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

}  // extern "C"
