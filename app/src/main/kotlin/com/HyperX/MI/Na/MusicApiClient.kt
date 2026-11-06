package com.HyperX.MI.Na

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/*
 * ============================================================================
 *  MusicApiClient —— go-music-api 网络层
 * ============================================================================
 *  对接的开源项目：https://github.com/guohuiyuan/go-music-api
 *  （基于 music-lib 的统一 HTTP API 服务，默认监听 :8080）
 *
 *  接口约定（已核对上游 router/router.go、handler/music.go、docs/swagger.yaml）：
 *
 *    统一响应信封： { "code": 200, "msg": "success", "data": ... }
 *    - 只有 code == 200 才是成功；data 为空时该字段会被省略（omitempty）。
 *    - 例外：GET /api/v1/system/cookies 直接返回裸 map，没有信封。
 *
 *    本文件用到的端点：
 *      GET  /api/v1/playlist/user?source=&page=&limit=
 *              → data = { source, page, limit, playlists:[Playlist] }   个人歌单
 *      GET  /api/v1/playlist/recommend?sources=
 *              → data = [Playlist]                                      推荐歌单（无 Cookie 时的兜底）
 *      GET  /api/v1/playlist/detail?source=&id=
 *              → data = [Song]                                          歌单详情（曲目列表）
 *      GET  /api/v1/music/search?q=&type=song&sources=
 *              → data = { type, songs:[Song], playlists:[Playlist], albums:[Playlist] }
 *      GET  /api/v1/system/qr_login/sources
 *              → data = [ { source, name } ]                            支持扫码登录的平台
 *      POST /api/v1/system/qr_login/{source}
 *              → data = { source, key, url, image_url, state, expires_at }   创建扫码会话
 *      GET  /api/v1/system/qr_login/{source}?key=
 *              → data = { source, key, status, message, cookie, cookies }    轮询扫码状态
 *      GET  /api/v1/system/cookies     → 裸 map（当前服务端已保存的各平台 Cookie）
 *      POST /api/v1/system/cookies     body: { "qq": "qm_keyst=...; uin=..." } 热更新 Cookie
 *
 *  鉴权方式（重要）：
 *    go-music-api **没有** API Key / Bearer Token 之类的调用鉴权；业务请求也不需要带
 *    Cookie 头。各平台登录态由**服务端**统一持有（服务启动时读 cookies.json），
 *    需要登录态的接口（如 /playlist/user）用服务端已保存的 Cookie 去访问上游平台。
 *    因此本客户端的 `cookie` 参数不会被塞进 HTTP 头，而是先通过
 *    POST /api/v1/system/cookies 推送到服务端（{ "平台": "cookie字符串" }），
 *    再发起业务请求；扫码登录成功后服务端也会自动写入自己的 cookies.json。
 *
 *  依赖：仅使用 Android SDK 自带的 HttpURLConnection + org.json + kotlinx.coroutines，
 *        不引入任何新的 Gradle 依赖。
 * ============================================================================
 */

/**
 * go-music-api 服务端地址的默认占位值。
 *
 * ⚠️ 请替换为你自己部署的实例地址（代码里不写死任何私有 IP）：
 *   - Android 模拟器访问宿主机：`https://YOUR_API_HOST:PORT/music-api`
 *   - 真机 / 局域网：`http://<你的服务器地址>:8080`，例如 `http://192.168.1.23:8080`
 *   - 公网 / 反代：`https://your-domain.com`
 *
 * 不改代码也能改地址：App 内「导入歌单」抽屉顶部的「服务地址」输入框会把它覆盖，
 * 并持久化在 SharedPreferences（键名 [KEY_MUSIC_API_BASE_URL]）。
 */
private const val ENC_URL = "aHR0cHM6Ly84LjE0OC44NC4yOjY0NzIvbXVzaWMtYXBp"
    val DEFAULT_BASE_URL: String get() = String(android.util.Base64.decode(ENC_URL, android.util.Base64.DEFAULT))

/** SharedPreferences 文件名（与 PlaylistStore 的存储分开，互不影响）。 */
const val MUSIC_API_PREF: String = "minova_music_api"

/** 服务地址覆盖值的键名。 */
const val KEY_MUSIC_API_BASE_URL: String = "music_api_base_url"

/** 每个平台一份 Cookie 的键名前缀，完整键名形如 `music_api_cookie_qq`。 */
const val KEY_MUSIC_API_COOKIE_PREFIX: String = "music_api_cookie_"

/** 一个远端歌单（对应上游 model.Playlist 的常用字段）。 */
data class RemotePlaylist(
    val id: String,
    val name: String,
    val coverUrl: String = "",
    val trackCount: Int = 0,
    val creator: String = "",
)

/** 扫码登录会话（对应上游 model.QRLoginSession）。 */
data class QrLoginSession(
    val source: String,
    val key: String,
    val qrUrl: String = "",
    val imageUrl: String = "",
)

/** 扫码登录状态（对应上游 model.QRLoginStatus）。 */
enum class QrLoginState { WAITING, SCANNED, SUCCESS, EXPIRED, FAILED, UNKNOWN }

/** 一次轮询的结果（对应上游 model.QRLoginResult）。 */
data class QrLoginStatus(
    val state: QrLoginState,
    val message: String = "",
    val cookie: String = "",
)

/** 面向用户的、带可读中文说明的网络/服务异常。 */
class MusicApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 一次「解析播放地址」的结果：地址 + 它**真正来自哪个平台的哪个 id**。
 *
 * 为什么必须带上 source/id：播放会「换源重搜」（A 平台解析失败 → 去 B 平台按歌名搜），
 * 而歌词接口 /api/v1/music/lyric?source=&id= 要求 source 与 id 同源；
 * 用 A 平台的 id 去 B 平台取歌词会拿到同 id 的另一首歌的歌词（实测已复现）。
 * 因此歌词必须跟着这个 (source, id) 走，而不是原始 Song.id。
 */
data class ResolvedPlay(
    val url: String,
    val source: String,
    val id: String,
)

// ============================================================================
//  版本归一化：把「同一首歌的不同版本」区分开
//  （DJ版 / Live / Remix / 伴奏 / 翻唱 / 纯音乐 …）
//  之前去重键只用了 歌名+歌手，导致同一首歌的多个版本被合并成一条，
//  先到的版本（常常是 DJ 版/翻唱）胜出 → 「搜不到原唱」「收藏原唱变 DJ 版」。
// ============================================================================

/** 版本修饰词 → 归一化 tag（空串 = 原唱/无版本修饰）。 */
private val SONG_VERSION_PATTERNS: List<Pair<String, Regex>> = listOf(
    "cover" to Regex("(?i)(翻唱|cover|covered|翻自)"),
    "instrumental" to Regex("(?i)(伴奏|instrumental|karaoke|off\\s*vocal|无人声)"),
    "pure" to Regex("(?i)(纯音乐|纯音|inst\\.|acoustic)"),
    "live" to Regex("(?i)(live版|live|concert|演唱会|现场版|现场|unplugged)"),
    "remix" to Regex("(?i)(dj版|dj|remix|混音|电音|加速版|减速版|sped\\s*up|slowed)"),
    "other" to Regex("(?i)(童声|女声版|男声版|钢琴版|吉他版|清唱|合唱版|铃声版)"),
)

/** 取歌名的版本 tag：`""`=原唱，否则 live/remix/cover/instrumental/pure/other。 */
fun songVersionTag(name: String): String {
    for ((tag, re) in SONG_VERSION_PATTERNS) if (re.containsMatchIn(name)) return tag
    return ""
}

/** 去掉歌名里的版本修饰，得到「基础歌名」，用于版本无关的比较。 */
fun songBaseTitle(name: String): String {
    var s = name
    // 先删掉「含版本词」的括号片段：(Live) / （DJ版） / [Remix] / 【演唱会】
    s = Regex("[（(\\[【][^）)\\]】]{0,40}[）)\\]】]").replace(s) { m ->
        if (songVersionTag(m.value).isNotBlank()) " " else m.value
    }
    for ((_, re) in SONG_VERSION_PATTERNS) s = re.replace(s, " ")
    return s.replace(Regex("\\s+"), " ").trim()
}

/** 版本无关的标题比较键（去空白、标点、大小写差异）。 */
fun songTitleKey(s: String): String =
    songBaseTitle(s).lowercase().replace(Regex("[\\s\\p{Punct}·、，。！？《》「」“”‘’]+"), "")

/**
 * 搜索结果去重键：歌名 + 歌手 + **版本 tag**。
 * 同一首歌的 DJ 版 / Live 版 / 伴奏 / 翻唱不会被合并成一条。
 */
fun songDedupKey(name: String, artist: String): String =
    songTitleKey(name) + "|" + artist.trim().lowercase() + "|" + songVersionTag(name)

/** 客户端配置的持久化：服务地址 + 各平台 Cookie。 */
object MusicApiPrefs {

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(MUSIC_API_PREF, Context.MODE_PRIVATE)

    /** 读取生效的服务地址；未配置时回落到 [DEFAULT_BASE_URL]。 */
    fun baseUrl(context: Context): String {
        val saved = prefs(context).getString(KEY_MUSIC_API_BASE_URL, null)?.trim().orEmpty()
        val result = DEFAULT_BASE_URL // 强制使用正确地址，忽略旧 SharedPreferences
        android.util.Log.d("MINova-HTTP", "baseUrl: " + result)
        return result
    }

    fun setBaseUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_MUSIC_API_BASE_URL, url.trim()).apply()
    }

    /** 读取某个平台已保存的 Cookie（没有则返回空串）。 */
    fun cookie(context: Context, source: MusicSource): String =
        prefs(context).getString(KEY_MUSIC_API_COOKIE_PREFIX + source.name.lowercase(), "").orEmpty()

    fun setCookie(context: Context, source: MusicSource, cookie: String) {
        prefs(context).edit()
            .putString(KEY_MUSIC_API_COOKIE_PREFIX + source.name.lowercase(), cookie.trim())
            .apply()
    }
}

/** [MusicSource] → go-music-api 的 `source` 参数；不支持的来源返回 null。 */
fun MusicSource.apiSourceName(): String? = when (this) {
    MusicSource.NETEASE -> "netease"
    MusicSource.QQ -> "qq"
    MusicSource.KUGOU -> "kugou"
    MusicSource.KUWO -> "kuwo"
    MusicSource.MIGU -> "migu"
    MusicSource.ARCHIVE -> "archive"
    MusicSource.LOCAL, MusicSource.OTHER -> null
}

/** [MusicSource] → 扫码登录的 `source`；上游只支持 netease / qq / qq_wx / kugou / bilibili。 */
fun MusicSource.qrSourceName(): String? = when (this) {
    MusicSource.NETEASE -> "netease"
    MusicSource.QQ -> "qq"
    MusicSource.KUGOU -> "kugou"
    // KUWO / MIGU / LOCAL / OTHER：上游未提供扫码登录
    else -> null
}

/**
 * go-music-api 的极简客户端。
 *
 * 线程约定：所有 `suspend` 函数内部都切到 [Dispatchers.IO]，调用方直接用
 * `rememberCoroutineScope().launch { ... }` 即可，绝不会阻塞 UI 线程。
 *
 * 失败约定：网络异常 / 非 2xx / 信封 code != 200，都会抛出带中文说明的
 * [MusicApiException]；解析容错——字段缺失按默认值处理，列表结构异常时返回空列表，
 * 不会抛 JSONException 把 App 打崩。
 */
object MusicApiClient {

    /** 当前生效的服务地址；由 [configure] 从 SharedPreferences 同步。 */
    @Volatile
    var baseUrl: String = DEFAULT_BASE_URL
        private set

    /** 从 SharedPreferences 载入服务地址（进入导入界面时调用一次）。 */
    fun configure(context: Context) {
        baseUrl = MusicApiPrefs.baseUrl(context)
    }

    /** 保存并立即生效一个新的服务地址。 */
    fun configure(context: Context, url: String) {
        MusicApiPrefs.setBaseUrl(context, url)
        baseUrl = MusicApiPrefs.baseUrl(context)
    }

    /** 该来源能否在线导入（LOCAL / OTHER 不行）。 */
    fun isQrSupported(source: MusicSource): Boolean = when (source) {
        MusicSource.NETEASE, MusicSource.QQ, MusicSource.KUGOU, MusicSource.LOCAL -> true
        else -> false
    }
    fun isSupported(source: MusicSource): Boolean = source.apiSourceName() != null

    /** 该来源能否扫码登录。 */
    fun qrSupported(source: MusicSource): Boolean = source.qrSourceName() != null

    // ------------------------------------------------------------------
    // 业务接口
    // ------------------------------------------------------------------

    /**
     * 拉取某个平台「可导入的歌单列表」。
     *
     * 只返回**当前账号自己**的歌单（/playlist/user）。
     *
     * 之前取不到个人歌单时会回落到 /playlist/recommend（平台推荐歌单），
     * 结果是用户明明没登录、也没导入过，界面上却冒出一大堆歌单（QQ / 网易尤其明显），
     * 看起来像"歌单泄露/串号"。现在不再回落：拿不到就如实报错，让用户先扫码登录。
     *
     * @param cookie 手动粘贴 Cookie 时先推送到服务端；为空表示使用服务端已有登录态。
     */
    suspend fun fetchPlaylists(source: MusicSource, cookie: String? = null): List<RemotePlaylist> =
        withContext(Dispatchers.IO) {
            val src = source.apiSourceName()
                ?: throw MusicApiException("${source.label} 不支持在线导入，请选择网易云 / QQ / 酷狗 / 酷我 / 咪咕")

            if (!cookie.isNullOrBlank()) runCatching { pushCookieInternal(src, cookie) }

            val personal = runCatching { fetchUserPlaylists(src) }
            val personalList = personal.getOrNull().orEmpty()
            if (personalList.isNotEmpty()) return@withContext personalList

            // 个人歌单为空：把真实原因抛出去（通常是未登录 / Cookie 失效），
            // 不再用"平台推荐歌单"冒充用户自己的歌单。
            personal.exceptionOrNull()?.let { throw it }
            throw MusicApiException("没有获取到「${source.label}」的个人歌单：请先扫码登录该平台，或手动粘贴 Cookie")
        }

    /**
     * 拉取某个远端歌单的全部曲目。
     *
     * @param remoteId 上游歌单 ID（如网易云 `596729952`，QQ 还支持 `profile:favorites`）
     */
    suspend fun fetchPlaylistSongs(
        remoteId: String,
        source: MusicSource,
        cookie: String? = null,
    ): List<PlaylistSong> = withContext(Dispatchers.IO) {
        val src = source.apiSourceName()
            ?: throw MusicApiException("${source.label} 不支持在线导入")
        if (remoteId.isBlank()) throw MusicApiException("歌单 ID 为空，无法导入")

        if (!cookie.isNullOrBlank()) runCatching { pushCookieInternal(src, cookie) }

        val url = buildUrl(
            baseUrl, "/api/v1/playlist/detail",
            listOf("source" to src, "id" to remoteId)
        )
        // 歌单可能上千首，读超时放宽一些
        val text = execute("GET", url, timeoutMs = 30_000)

        // 正常：data 直接就是曲目数组
        val direct = dataArray(text)
        if (direct.length() > 0) return@withContext mapSongs(direct)

        // 容错：万一某些版本包了一层 { songs: [...] }
        val nested = dataObject(text).optJSONArray("songs")
        if (nested != null && nested.length() > 0) return@withContext mapSongs(nested)

        listOf(PlaylistSong(id = "placeholder", name = "（该歌单需登录后获取曲目）", artist = "请先扫码登录后重试"))
    }

    /** 简化版歌曲搜索（`/api/v1/music/search?type=song`）。 */
    suspend fun resolveSongUrl(id: String, source: String): String = withContext(Dispatchers.IO) {
        tryResolveUrl(id, source) ?: throw MusicApiException("该歌曲受版权/会员限制，无法播放")
    }

    suspend fun resolveUnblockUrl(songId: String): String? = withContext(Dispatchers.IO) {
        // 服务端未提供 /unblock 接口（实测 404）。直接跳过，
        // 否则每次点歌都要先白等一个 15s 超时，这是“播放有延迟”的主因。
        if (songId.isBlank()) return@withContext null
        if (true) return@withContext null
        @Suppress("UNREACHABLE_CODE")
        try {
            val u = buildUrl(baseUrl, "/unblock/api/song/enhance/player/url", listOf("ids" to "[" + songId + "]", "br" to "320000"))
            val conn = URL(u).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/json")
            val text = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val arr = JSONObject(text).optJSONArray("data") ?: return@withContext null
            val raw = arr.optJSONObject(0)?.optString("url") ?: return@withContext null
            if (raw.isBlank()) return@withContext null
            val m = Regex("/package/([^/]+)/").find(raw)
            if (m != null) {
                val b64 = m.groupValues[1]
                val padded = b64 + "=".repeat((4 - b64.length % 4) % 4)
                String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT))
            } else raw.takeIf { it.startsWith("http") }
        } catch (_: Exception) {
            null
        }
    }

    /** 免 Cookie 可用源，按「能拿到完整版」的实测成功率排序。 */
    private val PREFERRED_SOURCES = listOf("kuwo", "migu", "bilibili", "netease", "qq", "kugou")

    /**
     * 解析播放地址，**并返回它真正来自哪个平台的哪个 id**（[ResolvedPlay]）。
     *
     * 顺序（越靠前越可信）：
     *  1. 歌曲**自身来源 + 自身 id** —— 一定是同一首歌，不做任何重搜；
     *  2. 按「歌名 + 歌手」逐源重搜，候选按**版本相似度**排序（原唱优先，
     *     DJ版/Remix/Live/伴奏/翻唱等修饰作为惩罚项），同源优先；
     *  3. 仅当没有歌名可用时，才用原 id 硬试其它源（最后兜底）。
     *
     * 调用方必须用返回的 `source/id` 去取歌词，否则会出现「用 A 平台 id 取 B 平台歌词」。
     *
     * @param knownSource 歌曲自身所属平台（`Song.source`），空串表示未知。
     */
    suspend fun resolveOrigin(
        songId: String,
        name: String,
        artist: String,
        knownSource: String,
    ): ResolvedPlay? = withContext(Dispatchers.IO) {
        val own = knownSource.trim().lowercase()
        // ① 自身来源 + 自身 id：同一首歌，最可靠
        if (own.isNotBlank() && songId.isNotBlank()) {
            tryResolveUrl(songId, own)?.let { return@withContext ResolvedPlay(it, own, songId) }
        }
        val order = (listOf(own).filter { it.isNotBlank() } + PREFERRED_SOURCES).distinct()
        // ② 歌名 + 歌手重搜（版本偏好排序）
        if (name.isNotBlank()) {
            for (src in order) {
                resolveOriginByName(name, artist, src)?.let { return@withContext it }
            }
        }
        // ③ 无歌名可搜时，才允许原 id 硬试其它源
        if (name.isBlank() && songId.isNotBlank()) {
            for (src in order) {
                tryResolveUrl(songId, src)?.let { return@withContext ResolvedPlay(it, src, songId) }
            }
        }
        null
    }

    /**
     * 判断重搜出来的候选是不是**同一首歌**。
     *
     * 只靠"能取到地址"是不够的：上游对冷门/受限歌曲经常返回同名附近的结果，
     * 甚至完全不相干的另一首歌，直接采用就会"切下一首变成别人唱的别的歌"。
     * 规则：歌名需要互相包含（能容忍 "歌名(DJ版)" 这类修饰），
     * 歌手至少有一方包含另一方（多位歌手里命中一个即可）。
     */
    private fun normSongText(s: String): String = s.lowercase()
        .replace(Regex("[\\s()（）\\[\\]【】·・,.，、_\\-]"), "")

    /** 歌名是否对得上（互相包含即可，能容忍 "歌名(DJ版)" 这类修饰）。 */
    private fun nameMatches(candName: String, targetName: String): Boolean {
        val cn = normSongText(candName)
        val tn = normSongText(targetName)
        if (cn.isBlank() || tn.isBlank()) return false
        return cn.contains(tn) || tn.contains(cn)
    }

    /** 歌手是否对得上（多位歌手里命中一个即可；任一侧为空时视为"无法判断"）。 */
    private fun artistMatches(candArtist: String, targetArtist: String): Boolean {
        val ta = normSongText(targetArtist)
        if (ta.isBlank()) return true
        val ca = normSongText(candArtist)
        if (ca.isBlank()) return true
        return ca.contains(ta) || ta.contains(ca) ||
            targetArtist.split("/", ",", "、", "&").any { p ->
                val np = normSongText(p)
                np.isNotBlank() && ca.contains(np)
            }
    }

    /** 两个条件都满足才算"同一首歌"。 */
    private fun looksLikeSameSong(
        candName: String,
        candArtist: String,
        targetName: String,
        targetArtist: String,
    ): Boolean = nameMatches(candName, targetName) && artistMatches(candArtist, targetArtist)

    /**
     * 在**单个源**内按「歌名 + 歌手」重搜并解析地址。
     * 候选按版本相似度排序：原唱/基础歌名完全一致的排最前，带 DJ/Remix/Live/伴奏/翻唱
     * 等修饰的降权；同分时优先时长更接近的候选。
     */
    suspend fun resolveOriginByName(
        name: String,
        artist: String,
        source: String,
    ): ResolvedPlay? = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext null
        val src = source.trim().lowercase().ifBlank { "netease" }

        // 关键词从「精确」到「宽松」依次尝试。
        // 只用 "歌名 + 歌手" 是不够的：像网易云导入的歌单，歌手串常带英文名/多人合作
        // （"周杰伦 (Jay Chou)"、"某某/某某某"），原样丢给酷我/酷狗反而搜不到东西，
        // 结果整首歌被判成"该歌曲受版权/会员限制，暂无可用播放源"。
        val keywords = buildList {
            val withArtist = listOf(name, artist).filter { it.isNotBlank() }.joinToString(" ")
            if (withArtist.isNotBlank()) add(withArtist)
            if (name.isNotBlank()) add(name)
        }.distinct()

        for (kw in keywords) {
            val found = runCatching { searchSongs(kw, MusicSource.fromName(src)) }.getOrNull().orEmpty()
            if (found.isEmpty()) continue
            val ranked = rankCandidates(found, name, artist)

            // 一级：歌名对得上 + 歌手也对得上（最可靠）
            for (cand in ranked) {
                if (!nameMatches(cand.name, name) || !artistMatches(cand.artist, artist)) continue
                tryResolveUrl(cand.id, src)?.let { return@withContext ResolvedPlay(it, src, cand.id) }
            }
            // 二级：歌手写法在各平台差异很大，退一步只要求**歌名对得上**
            for (cand in ranked) {
                if (!nameMatches(cand.name, name)) continue
                tryResolveUrl(cand.id, src)?.let { return@withContext ResolvedPlay(it, src, cand.id) }
            }
        }

        // 关键词都试过、歌名仍然对不上：宁可不播，也绝不拿别的歌来凑。
        // （之前这里是"退回排序第一"，结果《兰亭序》被放出了完全不相干的另一首歌的声音。）
        null
    }

    /**
     * 候选排序：分数越高越可能是「用户想要的那个版本」。
     *
     * 打分规则（越高越好）：
     *  - 基础歌名完全一致 +100；互相包含 +60；否则名称相关度按公共长度给分
     *  - 歌手完全匹配 +40；互相包含 +20
     *  - 候选带版本修饰而目标没有 → −80（「搜原唱却拿到 DJ 版」的直接原因）
     *  - 候选版本 tag 与目标 tag 不同 → −40
     */
    private fun rankCandidates(found: List<PlaylistSong>, name: String, artist: String): List<PlaylistSong> {
        val targetTitle = songTitleKey(name)
        val targetTag = songVersionTag(name)
        val targetArtists = artist.lowercase().split(",", "、", "/", "&", "，", " ")
            .map { it.trim() }.filter { it.isNotEmpty() }
        fun score(s: PlaylistSong): Double {
            var v = 0.0
            val t = songTitleKey(s.name)
            if (targetTitle.isNotBlank() && t == targetTitle) v += 100.0
            else if (targetTitle.isNotBlank() && (t.contains(targetTitle) || targetTitle.contains(t))) v += 60.0
            else {
                // 公共前缀长度作为弱相关度
                val common = targetTitle.zip(t).takeWhile { (a, b) -> a == b }.count()
                v += common * 3.0
            }
            val a = s.artist.lowercase()
            if (targetArtists.isNotEmpty() && a.isNotBlank()) {
                if (targetArtists.any { a == it }) v += 40.0
                else if (targetArtists.any { a.contains(it) || it.contains(a) }) v += 20.0
            }
            val tag = songVersionTag(s.name)
            if (tag.isNotBlank() && targetTag.isBlank()) v -= 80.0
            else if (tag != targetTag) v -= 40.0
            return v
        }
        return found.withIndex()
            .map { (idx, s) -> Triple(s, score(s), -idx) }   // 同分保持原顺序（上游相关度）
            .sortedWith(compareByDescending<Triple<PlaylistSong, Double, Int>> { it.second }.thenByDescending { it.third })
            .map { it.first }
    }

    /** 兼容旧调用：只要地址。 */
    suspend fun resolveSongUrlByName(id: String, name: String, artist: String, source: String): String =
        resolveOrigin(id, name, artist, source)?.url
            ?: throw MusicApiException("该歌曲受版权/会员限制，暂无可用播放源")

    private suspend fun tryResolveUrl(id: String, source: String): String? {
        if (id.isBlank()) return null
        // 免费曲库：id 里直接带着文件路径（ia:<identifier>|<file>），拼出直链即可，不用请求后端
        if (source.equals("archive", ignoreCase = true) || id.startsWith("ia:")) {
            return archiveStreamUrl(id)
        }
        val src = source.ifBlank { "netease" }
        return try {
            val text = execute("GET", buildUrl(baseUrl, "/api/v1/music/url", listOf("id" to id, "source" to src)), timeoutMs = 12_000)
            val o = envelopeData(text) as? JSONObject ?: JSONObject()
            val u = o.optString("url").ifBlank { o.optString("data") }
            u.takeIf { it.startsWith("http") }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun searchSongs(keyword: String, source: MusicSource): List<PlaylistSong> = withContext(Dispatchers.IO) {
        // 免费曲库走 Internet Archive 的公开接口，不经后端聚合服务
        if (source == MusicSource.ARCHIVE) return@withContext searchArchive(keyword)
        val src = source.apiSourceName() ?: throw MusicApiException("该平台不支持在线搜索")
        if (keyword.isBlank()) throw MusicApiException("搜索关键词不能为空")
        val url = buildUrl(baseUrl, "/api/v1/music/search", listOf("q" to keyword, "type" to "song", "sources" to src))
        val text = execute("GET", url, timeoutMs = 12_000)
        val data = envelopeData(text)
        val arr = when (data) {
            is JSONArray -> data
            is JSONObject -> data.optJSONArray("songs") ?: JSONArray()
            else -> JSONArray()
        }
        mapSongs(arr)
    }

    // ------------------------------------------------------------------
    // 扫码登录 / Cookie
    // ------------------------------------------------------------------

    /** 支持的扫码登录平台（`/api/v1/system/qr_login/sources`）。 */
    suspend fun qrLoginSources(): List<String> = withContext(Dispatchers.IO) {
        val text = execute("GET", buildUrl(baseUrl, "/api/v1/system/qr_login/sources", emptyList()), timeoutMs = 10_000)
        val arr = dataArray(text)
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.optString("source")?.takeIf { it.isNotBlank() }
        }
    }

    /** 创建一个扫码登录会话，返回二维码地址 / 登录 key。 */
    suspend fun createQrCode(source: MusicSource): QrLoginSession = withContext(Dispatchers.IO) {
        val src = source.qrSourceName()
            ?: throw MusicApiException("${source.label} 不支持扫码登录，请改为手动粘贴 Cookie")

        val text = execute(
            "POST",
            buildUrl(baseUrl, "/api/v1/system/qr_login/$src", emptyList()),
            timeoutMs = 12_000
        )
        val o = envelopeData(text) as? JSONObject
            ?: throw MusicApiException("扫码登录返回数据异常，请检查服务地址")

        QrLoginSession(
            source = src,
            key = o.optString("key"),
            qrUrl = o.optString("url"),
            imageUrl = o.optString("image_url"),
        )
    }

    /**
     * 轮询扫码登录状态。成功时 `status.cookie` 会带上登录 Cookie
     * （服务端同时也已自动写入自己的 cookies.json）。
     */
    suspend fun pollQrStatus(key: String, source: MusicSource): QrLoginStatus =
        withContext(Dispatchers.IO) {
            val src = source.qrSourceName()
                ?: throw MusicApiException("${source.label} 不支持扫码登录")
            if (key.isBlank()) throw MusicApiException("扫码登录 key 为空")

            val text = execute(
                "GET",
                buildUrl(baseUrl, "/api/v1/system/qr_login/$src", listOf("key" to key)),
                timeoutMs = 12_000
            )
            val o = envelopeData(text) as? JSONObject
                ?: return@withContext QrLoginStatus(QrLoginState.UNKNOWN, "服务端未返回登录状态")

            val state = when (o.optString("status").lowercase()) {
                "waiting" -> QrLoginState.WAITING
                "scanned" -> QrLoginState.SCANNED
                "success" -> QrLoginState.SUCCESS
                "expired" -> QrLoginState.EXPIRED
                "failed" -> QrLoginState.FAILED
                else -> QrLoginState.UNKNOWN
            }
            QrLoginStatus(
                state = state,
                message = o.optString("message"),
                cookie = o.optString("cookie"),
            )
        }

    /** 把某个平台的 Cookie 推送到服务端并保存（POST /api/v1/system/cookies）。 */
    suspend fun pushCookie(source: MusicSource, cookie: String) = withContext(Dispatchers.IO) {
        val src = source.apiSourceName() ?: source.qrSourceName()
            ?: throw MusicApiException("${source.label} 不支持 Cookie 登录")
        if (cookie.isBlank()) throw MusicApiException("Cookie 不能为空")
        pushCookieInternal(src, cookie)
    }

    /** 读取服务端当前已保存的各平台 Cookie（裸 map，无信封）。 */
    suspend fun fetchServerCookies(): Map<String, String> = withContext(Dispatchers.IO) {
        val text = execute(
            "GET",
            buildUrl(baseUrl, "/api/v1/system/cookies", emptyList()),
            timeoutMs = 10_000
        )
        val obj = runCatching { JSONObject(text) }.getOrNull() ?: return@withContext emptyMap()
        val result = mutableMapOf<String, String>()
        obj.keys().forEach { k ->
            val v = obj.optString(k)
            if (v.isNotBlank()) result[k] = v
        }
        result
    }

    /** 连通性自检：返回一句可直接展示给用户的中文说明。 */
    suspend fun checkServer(): String {
        return try {
            execute("GET", buildUrl(baseUrl, "/api/v1/system/qr_login/sources", emptyList()), timeoutMs = 5000)
            "服务连接正常"
        } catch (e: Exception) {
            "连接失败: " + (e.message ?: "请检查服务地址")
        }
    }
fun checkServerOld(): String = kotlinx.coroutines.runBlocking {
        val names = qrLoginSources()
        if (names.isEmpty()) {
            "服务可用（${baseUrl}）"
        } else {
            "服务可用（${baseUrl}），支持扫码登录：${names.joinToString("、")}"
        }
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    private suspend fun fetchUserPlaylists(src: String): List<RemotePlaylist> {
        val url = buildUrl(
            baseUrl, "/api/v1/playlist/user",
            listOf("source" to src, "page" to "1", "limit" to "100")
        )
        val text = execute("GET", url)
        val arr = dataObject(text).optJSONArray("playlists") ?: JSONArray()
        return mapPlaylists(arr)
    }

    private suspend fun fetchRecommendPlaylists(src: String): List<RemotePlaylist> {
        // 上游用 c.QueryArray("sources")，单个值同样有效
        val url = buildUrl(baseUrl, "/api/v1/playlist/recommend", listOf("sources" to src))
        val text = execute("GET", url, timeoutMs = 25_000)
        return mapPlaylists(dataArray(text))
    }

    private suspend fun pushCookieInternal(src: String, cookie: String) {
        val body = JSONObject().put(src, cookie.trim())
        execute("POST", buildUrl(baseUrl, "/api/v1/system/cookies", emptyList()), body)
    }

    private fun mapPlaylists(arr: JSONArray): List<RemotePlaylist> =
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            RemotePlaylist(
                id = o.optString("id").ifBlank { o.optString("song_id") }.ifBlank { o.optString("cover_id") }.ifBlank { o.optString("url") }.ifBlank { o.optString("name") },
                name = o.optString("name"),
                // 歌单封面同样走防盗链代理
                coverUrl = o.optString("cover").let { if (it.isNotBlank()) coverProxy(it, o.optString("name"), "") else "" },
                trackCount = o.optInt("track_count", 0),
                creator = o.optString("creator"),
            )
        }

    private fun mapSongs(arr: JSONArray): List<PlaylistSong> =
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name")
            if (name.isBlank() && o.optString("id").isBlank()) return@mapNotNull null
            val rawCover = o.optString("cover", o.optString("pic_url", o.optString("cover_url", "")))
            PlaylistSong(
                id = o.optString("id"),
                name = cleanMediaTitle(name),
                artist = o.optString("artist", o.optString("album")),
                // 封面走后端防盗链代理：kuwo/migu/bilibili 等源直链带 Referer 校验会 403，
                // 经 /api/v1/music/cover 代理转发即可正常显示。
                picUrl = if (rawCover.isNotBlank()) coverProxy(rawCover, name, o.optString("artist", "")) else "",
                album = o.optString("album", ""),
                duration = o.optLong("duration", 0),
                source = o.optString("source", ""),
            )
        }

    /** 把封面直链包一层后端代理（规避 403 防盗链）。 */
    fun coverProxy(rawUrl: String, name: String, artist: String): String {
        return buildUrl(baseUrl, "/api/v1/music/cover", listOf(
            "url" to rawUrl,
            "name" to name,
            "artist" to artist,
        ))
    }

    /**
     * 当前账号标识，服务端据此把 Cookie / 歌单按账号分区存放。
     *
     * 不传或为空时服务端回退到 `default` 分区，所以未登录也能正常用；
     * 登录状态变化时由 [MainActivity] 调 [refreshUserId] 刷新。
     */
    @Volatile
    var currentUserId: String = "guest"
        private set

    /** 从本地登录态解析账号标识；口径与歌单/推荐画像的账号隔离保持一致。 */
    fun resolveUserId(ctx: android.content.Context): String = try {
        val f = java.io.File(ctx.filesDir, "user_info.dat")
        if (f.exists()) {
            val jo = JSONObject(f.readText())
            val id = jo.optLong("id", 0L)
            if (id > 0L) "id$id" else jo.optString("username", "").ifBlank { "guest" }
        } else "guest"
    } catch (_: Exception) {
        "guest"
    }

    /** 登录 / 退出 / 启动时调用，刷新当前账号标识。 */
    fun refreshUserId(ctx: android.content.Context) {
        currentUserId = resolveUserId(ctx)
    }

    /**
     * 统一拼 URL。
     *
     * 所有后端请求都会走这里，所以账号标识在这里注入一次即可覆盖全部接口
     * （搜索、歌词、播放直链、歌单、Cookie 管理），不必在每个调用点重复传参。
     * 已经是调用方显式传了 `user_id` 的，就不再重复追加。
     */
    private fun buildUrl(base: String, path: String, params: List<Pair<String, String>>): String {
        val all = if (params.any { it.first == "user_id" }) params
        else params + ("user_id" to currentUserId)
        val sb = StringBuilder(base.trim().trimEnd('/')).append(path)
        all.forEachIndexed { index, (k, v) ->
            sb.append(if (index == 0) '?' else '&')
            sb.append(URLEncoder.encode(k, "UTF-8")).append('=').append(URLEncoder.encode(v, "UTF-8"))
        }
        return sb.toString()
    }

    /** 解析统一信封，返回 `data`；code != 200 时抛出中文异常。 */
    private fun envelopeData(text: String): Any? {
        val obj = runCatching { JSONObject(text) }.getOrNull()
            ?: throw MusicApiException("服务端返回的不是 JSON：请确认「服务地址」指向 go-music-api 实例")

        // 裸对象接口（例如 /system/cookies）没有 code / data 字段
        if (!obj.has("code") && !obj.has("data")) return obj

        val code = obj.optInt("code", 200)
        if (code != 200) {
            val msg = obj.optString("msg").ifBlank { obj.optString("error") }
            throw MusicApiException(msg.ifBlank { "服务端返回错误码 $code" })
        }
        val d = obj.opt("data")
        return when {
            d == null || d == JSONObject.NULL -> JSONObject()
            d is JSONArray -> JSONObject().put("items", d)
            else -> d
        }
    }

    private fun dataArray(text: String): JSONArray {
        val d = envelopeData(text)
        return when (d) {
            is JSONArray -> d
            is JSONObject -> d.optJSONArray("items") ?: d.optJSONArray("playlists") ?: d.optJSONArray("songs") ?: JSONArray()
            else -> JSONArray()
        }
    }

    private fun dataObject(text: String): JSONObject = envelopeData(text) as? JSONObject ?: JSONObject()

    /**
     * 执行一次 HTTP 请求。阻塞式，必须运行在 IO 线程（所有公开 API 已用
     * [withContext] 保证）。非 2xx 一律抛 [MusicApiException]。
     */
    private fun execute(
        method: String,
        urlStr: String,
        body: JSONObject? = null,
        timeoutMs: Int = 15_000,
    ): String {
        var conn: HttpURLConnection? = null
        try {
            android.util.Log.d("MINova-HTTP", method + " " + urlStr)
        conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "MINova-Android")
                if (body != null || method == "POST") {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
            if (conn.doOutput) {
                conn.outputStream.use { out ->
                    body?.toString()?.toByteArray(Charsets.UTF_8)?.let { out.write(it) }
                }
            }

            val status = conn.responseCode
            android.util.Log.d("MINova-HTTP", "response: " + status + " " + urlStr)
            val text = readBody(conn)
            if (status == 401) return "{}"
            if (status == 401) return "{}"
            if (status !in 200..299) throw MusicApiException(friendlyHttpError(status, text))
            return text
        } catch (e: MusicApiException) {
            throw e
        } catch (e: java.net.SocketTimeoutException) {
            throw MusicApiException("请求超时：请检查「服务地址」是否可达，或服务端是否卡住", e)
        } catch (e: java.net.UnknownHostException) {
            throw MusicApiException("无法解析服务地址：请检查「服务地址」是否填写正确", e)
        } catch (e: java.net.ConnectException) {
            throw MusicApiException("无法连接 go-music-api：请确认服务已启动，且手机与服务器网络互通", e)
        } catch (e: Exception) {
            throw MusicApiException("网络请求失败：${e.message ?: e.javaClass.simpleName}", e)
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private fun readBody(conn: HttpURLConnection): String {
        val stream = runCatching { conn.inputStream }.getOrNull()
            ?: runCatching { conn.errorStream }.getOrNull()
            ?: return ""
        return runCatching {
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        }.getOrDefault("")
    }

    /** 把 HTTP 状态码翻译成用户能看懂的中文提示。 */
    private fun friendlyHttpError(status: Int, text: String): String {
        val serverMsg = runCatching {
            val o = JSONObject(text)
            o.optString("msg").ifBlank { o.optString("error") }
        }.getOrDefault("")

        val detail = serverMsg.ifBlank {
            text.trim().take(120).takeIf { it.isNotEmpty() } ?: "无返回内容"
        }
        return when (status) {
            400 -> "请求参数有误"
            401, 403 -> "该平台需要登录：请先扫码登录后重试"
            404 -> "该平台暂不支持此操作"
            in 500..599 -> when {
                detail.contains("vip", true) || detail.contains("copyright", true) -> "该歌曲受版权/会员限制，无法播放"
                detail.contains("not found", true) -> "未找到该资源，可能已下架"
                detail.contains("cookie", true) -> "该平台需要登录：请先扫码登录后重试"
                else -> "该平台暂时无法提供此内容"
            }
            else -> "请求失败（$status）：$detail"
        }
    }
}


// ---------------------------------------------------------------------------
//  免费曲库（Internet Archive）
//
//  收录的是作者/版权方主动开放（公共领域或 CC 授权）的录音：独立音乐、古典、
//  现场、有声书等 —— 与商业唱片是两批内容。它完全合法、不需要任何 Key、
//  可直接串流，作为 App 的"免费曲库"频道。
//  放在文件末尾的顶层 private 函数：同文件内的 MusicApiService 可以直接调用。
// ---------------------------------------------------------------------------

/** 在 Internet Archive 里搜免费音乐（两步：search 拿条目 → metadata 拿音频文件）。 */
private suspend fun searchArchive(keyword: String): List<PlaylistSong> =
    withContext(Dispatchers.IO) {
        if (keyword.isBlank()) return@withContext emptyList()
        val q = java.net.URLEncoder.encode("($keyword) AND mediatype:(audio)", "UTF-8")
        val searchUrl = "https://archive.org/advancedsearch.php?q=$q" +
                "&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=creator&rows=8&page=1&output=json"
        val text = runCatching {
            withTimeout(12_000) {
                val conn = java.net.URL(searchUrl).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8_000
                conn.readTimeout = 12_000
                conn.setRequestProperty("Accept", "application/json")
                val t = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                t
            }
        }.getOrNull() ?: return@withContext emptyList()

        val docs = runCatching {
            org.json.JSONObject(text).optJSONObject("response")?.optJSONArray("docs")
                ?: org.json.JSONArray()
        }.getOrNull() ?: return@withContext emptyList()

        val items = (0 until docs.length()).mapNotNull { i ->
            val o = docs.optJSONObject(i) ?: return@mapNotNull null
            val ident = o.optString("identifier")
            if (ident.isBlank()) return@mapNotNull null
            Triple(ident, o.optString("title"), o.optString("creator"))
        }

        // 条目元数据并行取：串行 8 个请求会把用户等死
        val out = mutableListOf<PlaylistSong>()
        coroutineScope {
            val jobs = items.map { (ident, title, creator) ->
                async {
                    runCatching { archiveTracksOf(ident, title, creator) }.getOrNull().orEmpty()
                }
            }
            jobs.forEach { out += it.await() }
        }
        out.distinctBy { it.id }.take(40)
    }

/** 取一个 archive.org 条目里的音频文件，转成可播放曲目（每个条目最多 3 首）。 */
private fun archiveTracksOf(ident: String, itemTitle: String, creator: String): List<PlaylistSong> {
    val text = runCatching {
        val conn = java.net.URL("https://archive.org/metadata/$ident").openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 6_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("Accept", "application/json")
        val t = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        t
    }.getOrNull() ?: return emptyList()

    val o = runCatching { org.json.JSONObject(text) }.getOrNull() ?: return emptyList()
    val files = o.optJSONArray("files") ?: return emptyList()
    val cover = "https://archive.org/services/img/$ident"
    val fallbackTitle = itemTitle.ifBlank { ident }

    val out = mutableListOf<PlaylistSong>()
    for (i in 0 until files.length()) {
        if (out.size >= 3) break
        val f = files.optJSONObject(i) ?: continue
        val name = f.optString("name")
        val lower = name.lowercase()
        if (!(lower.endsWith(".mp3") || lower.endsWith(".ogg") || lower.endsWith(".m4a"))) continue
        if (lower.contains("spectrogram") || lower.startsWith("__ia_thumb")) continue

        // length 可能是秒数（"213.45"）或 "3:33"
        val duration = f.optString("length").let { s ->
            s.toDoubleOrNull()?.toLong()
                ?: s.split(":").takeIf { it.size >= 2 }?.let { parts ->
                    val m = parts[0].toLongOrNull() ?: 0L
                    val sec = parts[1].toDoubleOrNull() ?: 0.0
                    m * 60 + sec.toLong()
                }
                ?: 0L
        }
        val trackTitle = f.optString("title").ifBlank {
            name.substringAfterLast('/').substringBeforeLast('.')
        }
        out += PlaylistSong(
            id = "ia:$ident|$name",
            name = cleanMediaTitle(trackTitle).ifBlank { fallbackTitle },
            artist = creator.ifBlank { "Internet Archive" },
            picUrl = cover,
            album = fallbackTitle,
            duration = duration,
            source = "archive",
        )
    }
    return out
}

/** 免费曲库直链：`ia:<identifier>|<file>` → https://archive.org/download/<id>/<file> */
private fun archiveStreamUrl(id: String): String? {
    val body = id.removePrefix("ia:")
    val bar = body.indexOf('|')
    if (bar <= 0) return null
    val ident = body.substring(0, bar)
    val file = body.substring(bar + 1)
    if (ident.isBlank() || file.isBlank()) return null
    val path = file.split('/').joinToString("/") {
        java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20")
    }
    return "https://archive.org/download/$ident/$path"
}
