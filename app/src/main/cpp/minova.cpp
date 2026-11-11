// ============================================================================
//  MINova native 热点路径（libminova.so）
//  纯计算、无 Android 依赖，由 com.HyperX.MI.Na.native.NativeBridge 持有。
//  三条入口：
//    nativeComputeWave : 波浪进度条每 2px 步进的 y 值
//                        （WaveProgress.kt TadpoleWaveSlider per-frame sin 循环的 C++ 版，
//                         两端 taper 收 0 的数学原样保留，与 Kotlin 逐位一致）
//    nativeParseLrc    : LRC 解析，输出按时间排序的 "mm:ss.xx\t歌词行"
//                        （MainActivity.parseLrc 的 feed() 语义 C++ 版，
//                         含 [mm:ss.xx] 多时间标签行 / 逐字时间戳 / 增强型 LRC 规则）
//    nativeBmHash      : FNV-1a 64 快速指纹（歌曲去重 / 缓存 key 用）
//
//  一致性要点：
//   - 浮点：Kotlin/ART 按字节码逐运算舍入（无 FMA），编译侧 -ffp-contract=off 对齐；
//     kotlin.math.sin(Float) = sin(Double).toFloat()，这里同样以 double 调 libm sin
//     再转 float（同平台 libm，结果逐位一致）。
//   - 字符串：按字节处理 JNI Modified UTF-8；空白判定对齐 Character.isWhitespace
//     （含 U+3000 全角空格、U+2000-200A 等，排除 U+00A0/U+2007/U+202F）。
//     Kotlin String.length 是 UTF-16 单元数 —— Modified UTF-8 中每个 1/2/3 字节序列
//     恰好对应一个 UTF-16 单元（增补字符按两个 3 字节代理序列编码，无 4 字节序列），
//     故"非续字节计数"即 UTF-16 长度。
// ============================================================================

#include <jni.h>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>
#include <algorithm>

namespace {

// ---------------------------------------------------------------------------
// Character.isWhitespace 对齐的空白判定（Modified UTF-8 字节级）。
// 覆盖 isWhitespace==true 的 BMP 码点：0x09-0x0D、0x1C-0x1F、0x20、0x85(NEL)、
// 0x1680、0x2000-0x2006、0x2008-0x200A（0x2007 为 false）、0x2028、0x2029、
// 0x205F、0x3000。0xA0 / 0x202F / 0xFEFF 一律 false。
// ---------------------------------------------------------------------------
inline bool ktIsWhitespaceAt(const unsigned char* s, size_t i, size_t len, size_t* adv) {
    const unsigned char c = s[i];
    if (c == 0x09 || c == 0x0A || c == 0x0B || c == 0x0C || c == 0x0D ||
        c == 0x1C || c == 0x1D || c == 0x1E || c == 0x1F || c == 0x20) { *adv = 1; return true; }
    if (c == 0xC2 && i + 1 < len && s[i + 1] == 0x85) { *adv = 2; return true; }                    // U+0085
    if (c == 0xE1 && i + 2 < len && s[i + 1] == 0x9A && s[i + 2] == 0x80) { *adv = 3; return true; } // U+1680
    if (c == 0xE2 && i + 2 < len && s[i + 1] == 0x80) {
        const unsigned char t = s[i + 2];
        if (t >= 0x80 && t <= 0x8A && t != 0x87) { *adv = 3; return true; }                         // U+2000-200A
        if (t == 0xA8 || t == 0xA9) { *adv = 3; return true; }                                      // U+2028/2029
    }
    if (c == 0xE2 && i + 2 < len && s[i + 1] == 0x81 && s[i + 2] == 0x9F) { *adv = 3; return true; } // U+205F
    if (c == 0xE3 && i + 2 < len && s[i + 1] == 0x80 && s[i + 2] == 0x80) { *adv = 3; return true; } // U+3000
    return false;
}

// 等价 Kotlin String.trim()：去掉首尾 isWhitespace 字符。
inline std::string ktTrim(const unsigned char* s, size_t len) {
    size_t b = 0;
    while (b < len) {
        size_t adv = 0;
        if (ktIsWhitespaceAt(s, b, len, &adv)) b += adv; else break;
    }
    size_t e = len;
    while (e > b) {
        size_t st = e - 1;
        while (st > b && (s[st] & 0xC0) == 0x80) --st;  // 回退到字符首字节
        size_t adv = 0;
        if (ktIsWhitespaceAt(s, st, len, &adv) && st + adv == e) e = st; else break;
    }
    return std::string(reinterpret_cast<const char*>(s) + b, e - b);
}

// 等价 trimEnd + endsWith("]")。
inline bool ktTrimEndEndsWith(const std::string& s, char suffix) {
    const unsigned char* d = reinterpret_cast<const unsigned char*>(s.data());
    size_t e = s.size();
    while (e > 0) {
        size_t st = e - 1;
        while (st > 0 && (d[st] & 0xC0) == 0x80) --st;
        size_t adv = 0;
        if (ktIsWhitespaceAt(d, st, s.size(), &adv) && st + adv == e) e = st; else break;
    }
    return e > 0 && s[e - 1] == suffix;
}

// Kotlin String.length（UTF-16 code unit 数）：Modified UTF-8 中即"非续字节"数。
inline size_t ktUtf16Len(const std::string& s) {
    size_t n = 0;
    for (size_t i = 0; i < s.size(); ++i)
        if ((static_cast<unsigned char>(s[i]) & 0xC0) != 0x80) ++n;
    return n;
}

// ---------------------------------------------------------------------------
// LRC 时间标签：等价 Kotlin 正则 \[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\]
// （分钟 1-2 位、秒恰 2 位、小数 1-3 位、'.' 或 ':' 分隔，均无范围检查 —— 与正则一致）
// ---------------------------------------------------------------------------
struct LrcTag { long t; size_t start; size_t end; };

inline bool matchTagAt(const std::string& line, size_t pos, LrcTag* out) {
    const size_t n = line.size();
    size_t i = pos;
    if (i >= n || line[i] != '[') return false;
    ++i;
    int d1 = 0; long mm = 0;
    while (i < n && line[i] >= '0' && line[i] <= '9' && d1 < 2) {
        mm = mm * 10 + (line[i] - '0'); ++i; ++d1;
    }
    if (d1 == 0 || i >= n || line[i] != ':') return false;
    ++i;
    if (i + 1 >= n || line[i] < '0' || line[i] > '9' || line[i + 1] < '0' || line[i + 1] > '9') return false;
    const long ss = (line[i] - '0') * 10 + (line[i + 1] - '0');
    i += 2;
    long frac = 0;
    if (i < n && (line[i] == '.' || line[i] == ':')) {
        size_t j = i + 1; int fd = 0; long fv = 0;
        while (j < n && line[j] >= '0' && line[j] <= '9' && fd < 3) {
            fv = fv * 10 + (line[j] - '0'); ++j; ++fd;
        }
        if (fd == 0) return false;
        frac = (fd == 1) ? fv * 100 : (fd == 2) ? fv * 10 : fv;
        i = j;
    }
    if (i >= n || line[i] != ']') return false;
    out->t = mm * 60000 + ss * 1000 + frac;
    out->start = pos;
    out->end = i + 1;
    return true;
}

typedef std::pair<long, std::string> LrcEntry;

// 等价 MainActivity.parseLrc 的 feed(line, collectTimes=true)：
// 输出 (t, text) 条目，text 为"整行去标签 + trim"（与 Kotlin 的 tagRe.replace(line,"").trim() 一致）。
void feedLine(const std::string& line, std::vector<LrcEntry>* out) {
    // 1. 找出全部时间标签（findAll 语义：逐位置尝试，命中则跳到 ']' 之后）
    std::vector<LrcTag> tags;
    size_t p = 0;
    while (p < line.size()) {
        LrcTag tg;
        if (matchTagAt(line, p, &tg)) { tags.push_back(tg); p = tg.end; }
        else ++p;
    }
    if (tags.empty()) return;

    // 2. text = 去标签 + trim；空 或 "//" → 整行跳过
    std::string replaced;
    replaced.reserve(line.size());
    size_t last = 0;
    for (size_t k = 0; k < tags.size(); ++k) {
        replaced.append(line, last, tags[k].start - last);
        last = tags[k].end;
    }
    replaced.append(line, last, line.size() - last);
    const std::string text = ktTrim(reinterpret_cast<const unsigned char*>(replaced.data()), replaced.size());
    if (text.empty() || text == "//") return;

    // 3. charwise 判定：kugou/qq 逐字时间戳（≥3 个标签且所有非空段 UTF-16 长度 ≤ 2）
    //    → 只用第一个标签，否则同一句会在每个字的时间点反复出现。
    bool charwise = false;
    {
        bool any = false, allOk = true;
        size_t last2 = 0;
        for (size_t k = 0; k < tags.size(); ++k) {
            const std::string piece = ktTrim(
                reinterpret_cast<const unsigned char*>(line.data()) + last2, tags[k].start - last2);
            if (!piece.empty()) {
                any = true;
                if (ktUtf16Len(piece) > 2) allOk = false;
            }
            last2 = tags[k].end;
        }
        const std::string tail = ktTrim(
            reinterpret_cast<const unsigned char*>(line.data()) + last2, line.size() - last2);
        if (!tail.empty()) {
            any = true;
            if (ktUtf16Len(tail) > 2) allOk = false;
        }
        charwise = tags.size() >= 3 && any && allOk;
    }

    // 4. 增强型 LRC：行尾还有"结束时间"标签（trimEnd 后以 ']' 结尾且 ≥2 个标签）
    //    → 只用第一个标签，否则同一句会被写到结束时间上与下一句抢时间戳。
    const bool endsWithTag = ktTrimEndEndsWith(line, ']');
    const bool firstOnly = charwise || (endsWithTag && tags.size() >= 2);

    if (firstOnly) out->emplace_back(tags.front().t, text);
    else for (size_t k = 0; k < tags.size(); ++k) out->emplace_back(tags[k].t, text);
}

// 逐字节切割行：等价 Kotlin String.lines()（\n、\r\n、\r 均为换行）。
template <typename Fn>
void forEachLine(const std::string& s, Fn fn) {
    size_t start = 0, i = 0;
    const size_t n = s.size();
    while (i < n) {
        const char c = s[i];
        if (c == '\n' || c == '\r') {
            fn(s.substr(start, i - start));
            ++i;
            if (c == '\r' && i < n && s[i] == '\n') ++i;
            start = i;
        } else ++i;
    }
    fn(s.substr(start, n - start));  // 最后一行（可能为空，与 lines() 一致）
}

}  // namespace

// ===========================================================================
// 歌曲过滤 / 去重 key（Kotlin 逻辑逐字移植）。规则来源（勿漂移）：
//   isLikelyMusic   = MainActivity.kt  isLikelyMusic()  （junk 词表 + 「第N章」正则）
//   isJunkVersion   = MainActivity.kt  isJunkVersion()  （parseVersionTag + 歌手拆词）
//   parseVersionTag = DiscoverScreen.kt parseVersionTag()（括号/破折号/专辑定位 + 词表）
//   去重 key        = MainActivity.kt  搜索合并处
//                     (name + "|" + artist).lowercase() + "|" + parseVersionTag(name, album)
// 字符串按 JNI Modified UTF-8 字节处理（与文件头既有约定一致）。
// lowercase 为 Kotlin invariant lowercase()（= toLowerCase(ROOT)）的覆盖子集：
// ASCII / Latin-1 / 全角 / 希腊 / 西里尔 / İ；希腊「词尾 ς」上下文规则未实现
// （Σ 恒映射 σ）——中文曲库不涉及；一致性抽查覆盖全部词表逐词验证。
// ===========================================================================
namespace {

inline size_t u8CharLen(unsigned char c) {
    return (c & 0xE0) == 0xE0 ? 3u : ((c & 0xC0) == 0xC0 ? 2u : 1u);
}

inline bool u8Eq(const std::string& s, size_t i, const char* lit, size_t n) {
    return i + n <= s.size() && std::memcmp(s.data() + i, lit, n) == 0;
}

inline bool u8Has(const std::string& hay, const char* needle) {
    return hay.find(needle) != std::string::npos;
}

// Kotlin String.lowercase() 覆盖子集（见上）。
std::string ktLowercase(const std::string& s) {
    std::string out;
    out.reserve(s.size() + 2);
    const size_t n = s.size();
    size_t i = 0;
    while (i < n) {
        const unsigned char c = static_cast<unsigned char>(s[i]);
        if (c >= 'A' && c <= 'Z') { out += static_cast<char>(c + 0x20); ++i; continue; }
        if (c == 0xC3 && i + 1 < n) {                       // Latin-1 U+00C0-00FF
            const unsigned char c2 = static_cast<unsigned char>(s[i + 1]);
            if (c2 >= 0x80 && c2 <= 0x9E && c2 != 0x97) {   // U+00C0-00DE（除 ×）
                out += static_cast<char>(0xC3);
                out += static_cast<char>(c2 + 0x20);
                i += 2; continue;
            }
        }
        if (c == 0xC4 && i + 1 < n &&                       // İ → i + U+0307
            static_cast<unsigned char>(s[i + 1]) == 0xB0) {
            out += 'i'; out += static_cast<char>(0xCC); out += static_cast<char>(0x87);
            i += 2; continue;
        }
        if (c == 0xCE && i + 1 < n) {                       // Greek
            const unsigned char c2 = static_cast<unsigned char>(s[i + 1]);
            unsigned char m = 0;
            if (c2 == 0x86) m = 0xAC;                         // Ά → ά
            else if (c2 >= 0x88 && c2 <= 0x8A) m = static_cast<unsigned char>(c2 + 0x24);
            else if (c2 == 0x8C) m = 0xCC;                    // Ό → ό
            else if (c2 >= 0x8E && c2 <= 0x8F) m = static_cast<unsigned char>(c2 + 0x3F);
            else if (c2 >= 0x91 && c2 <= 0xA1) m = static_cast<unsigned char>(c2 + 0x20);
            else if (c2 >= 0xA3 && c2 <= 0xAB) m = static_cast<unsigned char>(c2 + 0x20);
            if (m) {
                out += static_cast<char>(0xCE); out += static_cast<char>(m);
                i += 2; continue;
            }
        }
        if (c == 0xD0 && i + 1 < n) {                       // Cyrillic
            const unsigned char c2 = static_cast<unsigned char>(s[i + 1]);
            unsigned char m = 0;
            if (c2 >= 0x80 && c2 <= 0x8F) m = static_cast<unsigned char>(c2 + 0x50);
            else if (c2 >= 0x90 && c2 <= 0xAF) m = static_cast<unsigned char>(c2 + 0x20);
            if (m) {
                out += static_cast<char>(0xD0); out += static_cast<char>(m);
                i += 2; continue;
            }
        }
        if (c == 0xEF && i + 2 < n &&                       // Ａ-Ｚ → ａ-ｚ
            static_cast<unsigned char>(s[i + 1]) == 0xBC) {
            const unsigned char c2 = static_cast<unsigned char>(s[i + 2]);
            if (c2 >= 0xA1 && c2 <= 0xBA) {
                // U+FF21-FF3A → U+FF41-FF5A：跨 0x3F 边界 —— 中间字节 BC→BD、
                // 尾字节 -0x20（如 Ｌ=EF BC AC → ｌ=EF BD 8C；写错会变成 「，」）
                out += static_cast<char>(0xEF); out += static_cast<char>(0xBD);
                out += static_cast<char>(c2 - 0x20);
                i += 3; continue;
            }
        }
        out += s[i]; ++i;
    }
    return out;
}

// Kotlin String.isBlank()：全 isWhitespace（含 U+3000 等）或空。
inline bool ktIsBlank(const std::string& s) {
    size_t i = 0;
    while (i < s.size()) {
        size_t adv = 0;
        if (!ktIsWhitespaceAt(reinterpret_cast<const unsigned char*>(s.data()), i, s.size(), &adv))
            return false;
        i += adv;
    }
    return true;
}

// ---------------------------------------------------------------------------
// 等价 Regex("第\\s*[0-9零一二三四五六七八九十百千]{1,6}\\s*[章节集回期话卷]")
// .containsMatchIn(name)。注意 Java \s 仅 ASCII 空白（不含 U+3000/U+00A0）。
// ---------------------------------------------------------------------------
inline bool isRegexSpace(unsigned char c) {
    return c == 0x20 || (c >= 0x09 && c <= 0x0D);
}

inline bool isChapterNumAt(const std::string& s, size_t i) {
    const unsigned char c = static_cast<unsigned char>(s[i]);
    if (c < 0x80) return c >= '0' && c <= '9';
    static const char* const kNums[] = {
        "零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "百", "千"
    };
    for (const char* k : kNums) if (u8Eq(s, i, k, 3)) return true;
    return false;
}

inline bool isChapterUnitAt(const std::string& s, size_t i) {
    static const char* const kUnits[] = { "章", "节", "集", "回", "期", "话", "卷" };
    for (const char* k : kUnits) if (u8Eq(s, i, k, 3)) return true;
    return false;
}

bool chapterContains(const std::string& s) {
    static const char kDi[] = "第";
    size_t p = s.find(kDi);
    while (p != std::string::npos) {
        size_t i = p + 3;
        while (i < s.size() && isRegexSpace(static_cast<unsigned char>(s[i]))) ++i;
        for (int k = 1; k <= 6; ++k) {                  // {1,6}：存在性 → 逐个 k 试
            size_t j = i;
            bool ok = true;
            for (int c = 0; c < k; ++c) {
                if (j >= s.size() || !isChapterNumAt(s, j)) { ok = false; break; }
                j += u8CharLen(static_cast<unsigned char>(s[j]));
            }
            if (!ok) break;                             // 取不满 k 个 → 更大 k 也不可能
            while (j < s.size() && isRegexSpace(static_cast<unsigned char>(s[j]))) ++j;
            if (j < s.size() && isChapterUnitAt(s, j)) return true;
        }
        p = s.find(kDi, p + 3);
    }
    return false;
}

// ---------------------------------------------------------------------------
// 等价 DiscoverScreen.parseVersionTag(name, album)：返回 tag（"" = null）。
// ---------------------------------------------------------------------------
inline bool isBracketOpenAt(const std::string& s, size_t i, size_t* len) {
    const unsigned char c = static_cast<unsigned char>(s[i]);
    if (c == '(' || c == '[') { *len = 1; return true; }
    if (u8Eq(s, i, "（", 3)) { *len = 3; return true; }
    if (u8Eq(s, i, "【", 3)) { *len = 3; return true; }
    return false;
}

inline bool isBracketCloseAt(const std::string& s, size_t i) {
    const unsigned char c = static_cast<unsigned char>(s[i]);
    if (c == ')' || c == ']') return true;
    return u8Eq(s, i, "）", 3) || u8Eq(s, i, "】", 3);
}

std::string parseVersionTagCpp(const std::string& name, const std::string& album) {
    const std::string lower = ktLowercase(name);

    // 1) 括号内容：[（(\[【] ([^）)\]】]{1,24}) [）)\]】]，组1 join " "
    //    （{1,24} 计 UTF-16 单元 = Modified UTF-8 非续字节）
    std::string brackets;
    {
        const size_t n = lower.size();
        size_t i = 0;
        while (i < n) {
            size_t ol = 0;
            if (!isBracketOpenAt(lower, i, &ol)) { ++i; continue; }
            size_t j = i + ol;
            size_t units = 0;
            while (j < n && units < 24) {
                if (isBracketCloseAt(lower, j)) break;
                j += u8CharLen(static_cast<unsigned char>(lower[j]));
                ++units;
            }
            if (units >= 1 && j < n && isBracketCloseAt(lower, j)) {
                brackets.append(lower, i + ol, j - (i + ol));
                brackets += ' ';
                i = j + u8CharLen(static_cast<unsigned char>(lower[j]));
            } else {
                i += ol;  // 无匹配 → 前进一个单元重试（与 Java 逐位置语义一致）
            }
        }
    }

    // 2) tail：lower.substringAfterLast("-", "").substringAfterLast("–", "").trim()
    //    ⚠️ Kotlin 链式语义：第二段的 missingDelimiterValue 是 "" —— 最后一个 '-'
    //    之后若没有 en dash「–」，**整个 tail 丢弃为 ""**（不是保留 '-' 后的串）。
    //    tail 非空 ⟺ 存在 '-' 且其后还有 '–'，取最后一个 '–' 之后的 trim。
    std::string tail;
    {
        const size_t dash = lower.rfind('-');
        if (dash != std::string::npos) {
            const std::string s2 = lower.substr(dash + 1);
            const size_t end = s2.rfind("–");
            if (end != std::string::npos)
                tail = ktTrim(reinterpret_cast<const unsigned char*>(s2.data()) + end + 3,
                              s2.size() - end - 3);
        }
    }

    // 3) hay = (inBrackets + " " + tail + " " + album.lowercase()).trim()
    std::string hay = brackets;
    hay += ' '; hay += tail; hay += ' '; hay += ktLowercase(album);
    hay = ktTrim(reinterpret_cast<const unsigned char*>(hay.data()), hay.size());

    auto has = [&hay](std::initializer_list<const char*> keys) -> bool {
        for (const char* k : keys) if (u8Has(hay, k)) return true;
        return false;
    };
    if (has({"dj版", "dj 版", "remix", "dj"})) return "DJ版";
    if (has({"live", "演唱会", "concert"})) return "Live";
    if (has({"翻唱", "cover", "cover版"})) return "翻唱";
    if (has({"伴奏", "instrumental", "accompaniment", "off vocal"})) return "伴奏";
    if (has({"纯音乐"})) return "纯音乐";
    return "";
}

// 等价 Regex("(?i)(?<![a-z0-9])(dj|remix|cover|sped|slowed|8d)(?![a-z])")
// containsMatchIn(token)（token 已 lowercase；(?i) 对 ASCII 字母双向保守对齐）。
inline bool junkWordMatch(const std::string& t) {
    static const char* const kWords[] = { "dj", "remix", "cover", "sped", "slowed", "8d" };
    static const size_t kLens[] = { 2, 5, 5, 4, 6, 2 };
    const size_t n = t.size();
    for (size_t q = 0; q < n; ++q) {
        for (int w = 0; w < 6; ++w) {
            const size_t wl = kLens[w];
            if (q + wl > n || std::memcmp(t.data() + q, kWords[w], wl) != 0) continue;
            if (q > 0) {  // (?<![a-z0-9])：前一 UTF-16 单元是 ASCII 字母/数字 → 失败
                const unsigned char pc = static_cast<unsigned char>(t[q - 1]);
                if (pc < 0x80 && ((pc >= 'a' && pc <= 'z') || (pc >= 'A' && pc <= 'Z') ||
                                  (pc >= '0' && pc <= '9')))
                    continue;
            }
            if (q + wl < n) {  // (?![a-z])：后一单元是 ASCII 字母 → 失败（数字允许）
                const unsigned char nc = static_cast<unsigned char>(t[q + wl]);
                if (nc < 0x80 && ((nc >= 'a' && nc <= 'z') || (nc >= 'A' && nc <= 'Z')))
                    continue;
            }
            return true;
        }
    }
    return false;
}

// 等价 MainActivity.isLikelyMusic(song)
bool isLikelyMusicCpp(const std::string& name, const std::string& artist, const std::string& album) {
    static const char* const kMusicJunk[] = {
        "有声", "听书", "说书", "评书", "相声", "广播剧", "播客", "电台", "播音",
        "朗读", "诵读", "连载", "完结", "全集", "更新至", "精讲", "解读", "讲书",
        "拆书", "共读", "书评", "导读", "讲座", "公开课", "百家讲坛",
        "睡前故事", "儿童故事", "演播", "podcast"
    };
    std::string hay = name;
    hay += ' '; hay += artist; hay += ' '; hay += album;
    hay = ktLowercase(hay);
    for (const char* w : kMusicJunk)
        if (u8Has(hay, w)) return false;
    if (chapterContains(name)) return false;
    return true;
}

// 等价 MainActivity.isJunkVersion(song)
bool isJunkVersionCpp(const std::string& name, const std::string& artist, const std::string& album) {
    if (!parseVersionTagCpp(name, album).empty()) return true;
    const std::string art = ktLowercase(artist);
    if (ktIsBlank(art)) return false;
    static const char* const kJunkCn[] = {
        "混音", "翻唱", "加速", "减速", "降调", "升调", "抖音", "铃声",
        "伴奏", "纯音乐", "钢琴", "吉他", "童声", "女声版", "男声版",
        "合唱", "清唱", "戏腔", "串烧", "改版", "改编", "组曲", "重制"
    };
    // split('/', '、', '&', ',', '，', ';', '；', '-', '_', ' ', ' ') → trim → 非空
    const size_t n = art.size();
    size_t i = 0;
    while (i <= n) {
        size_t j = i;
        while (j < n) {
            const unsigned char c = static_cast<unsigned char>(art[j]);
            if (c == '/' || c == '&' || c == ',' || c == ';' || c == '-' || c == '_' || c == ' ')
                break;
            if (u8Eq(art, j, "、", 3) || u8Eq(art, j, "，", 3) || u8Eq(art, j, "；", 3))
                break;
            j += u8CharLen(c);
        }
        const std::string tok =
            ktTrim(reinterpret_cast<const unsigned char*>(art.data()) + i, j - i);
        if (!tok.empty()) {
            for (const char* w : kJunkCn)
                if (u8Has(tok, w)) return true;
            if (junkWordMatch(tok)) return true;
        }
        if (j >= n) break;
        i = j + u8CharLen(static_cast<unsigned char>(art[j]));
    }
    return false;
}

// 等价 MainActivity 搜索合并处的去重键：
// (name + "|" + artist).lowercase() + "|" + (parseVersionTag(name, album) ?: "")
std::string dedupKeyCpp(const std::string& name, const std::string& artist,
                        const std::string& album) {
    std::string k = name;
    k += '|'; k += artist;
    k = ktLowercase(k);
    k += '|';
    k += parseVersionTagCpp(name, album);
    return k;
}

// jobjectArray<String> → vector<string>（Modified UTF-8 字节；null 元素按 ""）
void fillStringVec(JNIEnv* env, jobjectArray arr, std::vector<std::string>* out) {
    const jsize n = env->GetArrayLength(arr);
    out->clear();
    out->reserve(static_cast<size_t>(n));
    for (jsize k = 0; k < n; ++k) {
        jstring js = static_cast<jstring>(env->GetObjectArrayElement(arr, k));
        if (js == nullptr) { out->emplace_back(); continue; }
        const char* raw = env->GetStringUTFChars(js, nullptr);
        const jsize blen = env->GetStringUTFLength(js);
        if (raw != nullptr) {
            out->emplace_back(raw, static_cast<size_t>(blen));
            env->ReleaseStringUTFChars(js, raw);
        } else {
            out->emplace_back();
        }
        env->DeleteLocalRef(js);
    }
}

}  // namespace

extern "C" {

// ---------------------------------------------------------------------------
// nativeComputeWave(widthPx, midY, amp, wavelengthPx, phase, x0, x1, taper, out) -> 写入点数
// 与 WaveProgress.kt 原 Kotlin 循环逐位一致：
//   x 从 x0+2f 起、每步 +2f（float 累加）；包络 up=(x-x0)/taper、down=(x1-x)/taper 各收 0；
//   y = midY - env*amp*sin(twoPi*(x-x0)/wavelength + phase)，sin 以 double 计算后转 float。
// 返回写入 out 的 y 值个数（不超过 out.length）。
// ---------------------------------------------------------------------------
JNIEXPORT jint JNICALL
Java_com_HyperX_MI_Na_native_NativeBridge_nativeComputeWave(
        JNIEnv* env, jobject /*thiz*/,
        jint /*widthPx*/, jfloat midY, jfloat amp, jfloat wavelengthPx,
        jfloat phase, jfloat x0, jfloat x1, jfloat taper, jfloatArray jout) {
    if (jout == nullptr) return 0;
    const jsize cap = env->GetArrayLength(jout);
    if (cap <= 0) return 0;

    jfloat* out = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(jout, nullptr));
    if (out == nullptr) return 0;

    // 等价 Kotlin: val twoPi = (2.0 * PI).toFloat()
    const float twoPi = static_cast<float>(2.0 * 3.14159265358979323846);

    // 等价 Kotlin coerceIn(0f, 1f)：NaN 时两个比较都为 false → 原样返回（与 Kotlin 一致）。
    const float zero = 0.0f, one = 1.0f;
    jint count = 0;
    float x = x0 + 2.0f;
    while (x < x1 && count < cap) {
        float up = (x - x0) / taper;
        if (up < zero) up = zero; else if (up > one) up = one;
        float down = (x1 - x) / taper;
        if (down < zero) down = zero; else if (down > one) down = one;
        const float envv = (up <= down) ? up : down;  // Kotlin minOf(a,b) = if (a<=b) a else b
        const double sv = sin(static_cast<double>(twoPi * (x - x0) / wavelengthPx + phase));
        out[count++] = midY - envv * amp * static_cast<float>(sv);
        x += 2.0f;
    }
    env->ReleasePrimitiveArrayCritical(jout, out, 0);
    return count;
}

// ---------------------------------------------------------------------------
// nativeParseLrc(lrc) -> 按时间排序的 "mm:ss.xx\t歌词行" 数组
// 语义与 MainActivity.parseLrc 的 feed(lrcText, collectTimes=true) 一致；
// 稳定排序保证同一时间戳内多条文本保持原文出现顺序（Kotlin 侧据此重建候选列表）。
// ---------------------------------------------------------------------------
JNIEXPORT jobjectArray JNICALL
Java_com_HyperX_MI_Na_native_NativeBridge_nativeParseLrc(
        JNIEnv* env, jobject /*thiz*/, jstring jlrc) {
    std::string s;
    if (jlrc != nullptr) {
        const char* raw = env->GetStringUTFChars(jlrc, nullptr);
        if (raw != nullptr) {
            const jsize blen = env->GetStringUTFLength(jlrc);
            s.assign(raw, static_cast<size_t>(blen));
            env->ReleaseStringUTFChars(jlrc, raw);
        }
    }

    std::vector<LrcEntry> entries;
    forEachLine(s, [&](const std::string& line) { feedLine(line, &entries); });

    std::stable_sort(entries.begin(), entries.end(),
                     [](const LrcEntry& a, const LrcEntry& b) { return a.first < b.first; });

    jclass strCls = env->FindClass("java/lang/String");
    if (strCls == nullptr) return nullptr;
    jobjectArray arr = env->NewObjectArray(static_cast<jsize>(entries.size()), strCls, nullptr);
    if (arr == nullptr) return nullptr;

    char buf[32];
    for (size_t k = 0; k < entries.size(); ++k) {
        const long t = entries[k].first;
        const long mm = t / 60000;
        const long ss = (t / 1000) % 60;
        const long cs = (t % 1000) / 10;
        std::snprintf(buf, sizeof(buf), "%02ld:%02ld.%02ld\t", mm, ss, cs);
        std::string e = buf;
        e += entries[k].second;
        jstring js = env->NewStringUTF(e.c_str());
        if (js != nullptr) {
            env->SetObjectArrayElement(arr, static_cast<jsize>(k), js);
            env->DeleteLocalRef(js);
        }
    }
    return arr;
}

// ---------------------------------------------------------------------------
// nativeBmHash(bytes) -> FNV-1a 64 指纹（按无符号字节值参与运算，与 Kotlin
// `b.toLong() and 0xFF` 等价）。供歌曲去重 / 缓存 key。
// ---------------------------------------------------------------------------
JNIEXPORT jlong JNICALL
Java_com_HyperX_MI_Na_native_NativeBridge_nativeBmHash(
        JNIEnv* env, jobject /*thiz*/, jbyteArray jbytes) {
    if (jbytes == nullptr) return 0;
    const jsize len = env->GetArrayLength(jbytes);
    jbyte* b = static_cast<jbyte*>(env->GetPrimitiveArrayCritical(jbytes, nullptr));
    if (b == nullptr) return 0;
    uint64_t h = 14695981039346656037ULL;          // FNV offset basis
    for (jsize i = 0; i < len; ++i) {
        h ^= static_cast<uint64_t>(static_cast<uint8_t>(b[i]));
        h *= 1099511628211ULL;                     // FNV prime
    }
    env->ReleasePrimitiveArrayCritical(jbytes, b, 0);
    return static_cast<jlong>(h);
}

// ---------------------------------------------------------------------------
// nativeFilterSongs(names, artists, albums) -> 逐条 isLikelyMusic（true=保留）
// 与 MainActivity.isLikelyMusic 逐字一致（词表 29 条 + 「第N章」状态机）。
// ---------------------------------------------------------------------------
JNIEXPORT jbooleanArray JNICALL
Java_com_HyperX_MI_Na_native_NativeBridge_nativeFilterSongs(
        JNIEnv* env, jobject /*thiz*/,
        jobjectArray jnames, jobjectArray jartists, jobjectArray jalbums) {
    if (jnames == nullptr || jartists == nullptr || jalbums == nullptr) return nullptr;
    std::vector<std::string> names, artists, albums;
    fillStringVec(env, jnames, &names);
    fillStringVec(env, jartists, &artists);
    fillStringVec(env, jalbums, &albums);
    const jsize n = static_cast<jsize>(names.size());
    if (static_cast<jsize>(artists.size()) != n || static_cast<jsize>(albums.size()) != n)
        return nullptr;

    jbooleanArray outArr = env->NewBooleanArray(n);
    if (outArr == nullptr) return nullptr;
    if (n > 0) {
        std::vector<jboolean> flags(static_cast<size_t>(n));
        for (jsize k = 0; k < n; ++k)
            flags[static_cast<size_t>(k)] = isLikelyMusicCpp(names[static_cast<size_t>(k)],
                                                             artists[static_cast<size_t>(k)],
                                                             albums[static_cast<size_t>(k)])
                                                 ? JNI_TRUE : JNI_FALSE;
        env->SetBooleanArrayRegion(outArr, 0, n, flags.data());
    }
    return outArr;
}

// ---------------------------------------------------------------------------
// nativeJunkFlags(names, artists, albums) -> 逐条 isJunkVersion（true=垃圾版本）
// 与 MainActivity.isJunkVersion 逐字一致（parseVersionTag 定位 + 歌手拆词 +
// 中文词表 23 条 + 拉丁缩写词级匹配）。供排序键批量预计算。
// ---------------------------------------------------------------------------
JNIEXPORT jbooleanArray JNICALL
Java_com_HyperX_MI_Na_native_NativeBridge_nativeJunkFlags(
        JNIEnv* env, jobject /*thiz*/,
        jobjectArray jnames, jobjectArray jartists, jobjectArray jalbums) {
    if (jnames == nullptr || jartists == nullptr || jalbums == nullptr) return nullptr;
    std::vector<std::string> names, artists, albums;
    fillStringVec(env, jnames, &names);
    fillStringVec(env, jartists, &artists);
    fillStringVec(env, jalbums, &albums);
    const jsize n = static_cast<jsize>(names.size());
    if (static_cast<jsize>(artists.size()) != n || static_cast<jsize>(albums.size()) != n)
        return nullptr;

    jbooleanArray outArr = env->NewBooleanArray(n);
    if (outArr == nullptr) return nullptr;
    if (n > 0) {
        std::vector<jboolean> flags(static_cast<size_t>(n));
        for (jsize k = 0; k < n; ++k)
            flags[static_cast<size_t>(k)] = isJunkVersionCpp(names[static_cast<size_t>(k)],
                                                             artists[static_cast<size_t>(k)],
                                                             albums[static_cast<size_t>(k)])
                                                 ? JNI_TRUE : JNI_FALSE;
        env->SetBooleanArrayRegion(outArr, 0, n, flags.data());
    }
    return outArr;
}

// ---------------------------------------------------------------------------
// nativeDedupKey(name, artist, album) -> 搜索结果去重键
// = (name + "|" + artist).lowercase() + "|" + parseVersionTag(name, album)
// 与 MainActivity 搜索合并处（两处调用点）逐字一致。
// ---------------------------------------------------------------------------
JNIEXPORT jstring JNICALL
Java_com_HyperX_MI_Na_native_NativeBridge_nativeDedupKey(
        JNIEnv* env, jobject /*thiz*/,
        jstring jname, jstring jartist, jstring jalbum) {
    auto getStr = [env](jstring js) -> std::string {
        if (js == nullptr) return std::string();
        const char* raw = env->GetStringUTFChars(js, nullptr);
        const jsize blen = env->GetStringUTFLength(js);
        std::string s;
        if (raw != nullptr) {
            s.assign(raw, static_cast<size_t>(blen));
            env->ReleaseStringUTFChars(js, raw);
        }
        return s;
    };
    const std::string key = dedupKeyCpp(getStr(jname), getStr(jartist), getStr(jalbum));
    return env->NewStringUTF(key.c_str());
}

}  // extern "C"
