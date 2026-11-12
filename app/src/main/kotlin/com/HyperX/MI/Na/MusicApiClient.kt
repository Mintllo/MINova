package com.HyperX.MI.Na

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
 *   - Android 模拟器访问宿主机：`https://8.148.84.2:6472/music-api`
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

/**
 * 起播候选（[MusicApiClient.resolvePlayCandidates] 返回，已排序）：
 * UI 层按序解析地址起播；**起播后 5 秒内 onError = 候选 URL 秒死** → 轮换下一个候选（最多 3 个），
 * 而不是「重解同一首 → 同样秒死 → 切下一首」。
 */
data class PlayCandidate(
    val id: String,
    val source: String,
    val name: String = "",
)

/**
 * 一次「快速解析」的结果：胜出直链 + 候选列表（供起播后秒死轮换使用）。
 * cheapOwn=true 表示走了「自身源快 CDN 直取」便宜路径（1 次请求，无搜索）。
 */
data class FastResolve(
    val play: ResolvedPlay,
    val candidates: List<PlayCandidate>,
    val cheapOwn: Boolean = false,
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
    MusicSource.BILIBILI -> "bilibili"
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

    /**
     * strictResolve 第二轮「同歌不同版本」命中标记（一次性）：
     * 命中时置 true，UI 层在解析成功后调 [takeAlternateVersionHit] 取走并清除（用于 toast）。
     */
    @Volatile
    private var alternateVersionHit = false

    /** UI 层调用：取走并清除「第二轮命中」标记。true = 本次解析命中了同歌曲的其它版本。 */
    fun takeAlternateVersionHit(): Boolean {
        val v = alternateVersionHit
        alternateVersionHit = false
        return v
    }

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
        // 只用「指定来源 + 指定 id」解析（同平台同 id 必然是同一首歌）；
        // 失败就是失败，绝不回退到「按歌名去别家搜」或「拿 id 去别家试」。
        tryResolveUrl(id, source) ?: throw MusicApiException("NO_STRICT_MATCH")
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

    /**
     * 免 Cookie 可用源，按**实测 prepare 速度**排序（序号越小优先级越高）。
     *
     * 实测（Android 模拟器，同一台设备同一网络）：
     *   kuwo / kugou / migu / bilibili 直链 → onPrepared 0.4~1.4 秒
     *   网易云直链（m701.music.126.net）   → onPrepared 稳定 31~32 秒
     * 所以跨源候选同分时优先选 kuwo/kugou，网易云放最后（用户反馈"要等很久才自动播放"）。
     */
    private val PREFERRED_SOURCES = listOf("kuwo", "migu", "bilibili", "qq", "netease")

    /** 跨源匹配允许的最大时长差（秒）。 */
    private const val STRICT_DURATION_TOLERANCE_SEC = 3L

    /** 第二轮「同歌不同版本」允许的最大时长差（秒）：版本时长可能不同（Live/重制等），放宽到 10s。 */
    private const val VERSION_DURATION_TOLERANCE_SEC = 10L

    /** 源优先级：越靠前越小。 */
    private fun sourcePriority(src: String): Int =
        PREFERRED_SOURCES.indexOf(src).let { if (it < 0) PREFERRED_SOURCES.size else it }

    /**
     * 解析播放地址，**并返回它真正来自哪个平台的哪个 id**（[ResolvedPlay]）。
     *
     * 铁律：**绝不允许「播出来的声音是另一首歌」**。
     *
     * 顺序（宁缺毋滥）：
     *  1. 只用「歌曲**自身来源 + 自身 id**」解析直链。同平台的同一个 id 必然是同一首歌，成功即返回。
     *  2. 跨源只作最后手段，且候选必须**同时**通过三重校验（见 [isStrictSameSong]）：
     *       ① 歌名归一化后**完全相等**（[strictTitleKey]，已去掉版本修饰词/B 站风格的 "正标题/上传者"）
     *       ② 歌手**有交集**（按 `/ 、 & , ;` 拆分后逐词比较，双方都必须有非空歌手名）
     *       ③ 时长指纹：双方 duration 都 > 0 时差值必须 <= [STRICT_DURATION_TOLERANCE_SEC] 秒
     *     在全部合格候选里选「时长差最小」的；差值相同时选**源优先级更高**的。
     *  3. 没有任何候选通过 → 返回 null（**宁可跳过也不放错歌**）。
     *
     * 已彻底删除的两条危险路径：
     *  · 「按歌名去别的平台搜，取最像的一个」—— 同名/近名的另一首歌会被直接播放；
     *  · 「用原 id 去其它平台硬试」—— 不同平台的数字 id 会撞车，对方返回**合法但完全不同**的歌。
     *
     * 调用方必须用返回的 `source/id` 去取歌词，否则会出现「用 A 平台 id 取 B 平台歌词」。
     *
     * @param knownSource 歌曲自身所属平台（`Song.source`），空串表示未知。
     * @param durationSec 歌曲自身的时长（秒）。给了就作为时长指纹；没给则尝试从自身来源的
     *                    严格同名结果里补一个，补不到按"时长未知"处理（放宽但不优先）。
     */
    suspend fun resolveOrigin(
        songId: String,
        name: String,
        artist: String,
        knownSource: String,
        durationSec: Long = 0L,
    ): ResolvedPlay? = withContext(Dispatchers.IO) {
        val own = knownSource.trim().lowercase()
        // ① 自身来源 + 自身 id：同一首歌，唯一可以直接信任 id 的分支
        if (own.isNotBlank() && songId.isNotBlank()) {
            tryResolveUrl(songId, own)?.let { return@withContext ResolvedPlay(it, own, songId) }
        }
        // ② 跨源：必须过三重校验；过不了就当作没找到
        if (name.isNotBlank()) {
            val target = discoverTargetDurationSec(songId, name, artist, own, durationSec)
            val others = PREFERRED_SOURCES.filter { it != own }
            strictResolve(name, artist, target, others)?.let { return@withContext it }
        }
        // ③ 没有第三条路：绝不用别家的 id 去别家源硬试，也绝不退回"最像的一个"
        null
    }

    // ------------------------------------------------------------------
    //  严格「同一首歌」判定 —— 所有跨源播放的唯一入口
    // ------------------------------------------------------------------

    /** 括号片段（(…) （…） […] 【…】）：归一化时整段丢弃。 */
    private val BRACKET_SEGMENT_REGEX = Regex("[（(\\[【][^）)\\]】]{0,60}[）)\\]】]")

    /** 空白与标点（含中文标点）：严格比对键里全部丢掉。 */
    private val KEY_PUNCT_REGEX = Regex("[\\s\\p{Punct}·・、，。！？；：…—～《》「」『』“”‘’]+")

    /**
     * 版本修饰词 —— 归一化时整词删除。
     * ASCII 词要求两侧不是字母/数字（避免把 "alive" 里的 live 误删）；中文词直接删。
     */
    private val VERSION_DECORATOR_REGEXES: List<Regex> = listOf(
        Regex("(?i)(?<![a-z0-9])(dj\\s*版?|remix\\s*版?|混音\\s*版?|加速版|减速版|sped\\s*up|slowed)(?![a-z0-9])"),
        Regex("(?i)(?<![a-z0-9])(live\\s*版?|现场版|演唱会|现场|unplugged|concert)(?![a-z0-9])"),
        Regex("(?i)(?<![a-z0-9])(cover\\s*版?|翻唱\\s*版?|翻自)(?![a-z0-9])"),
        Regex("(?i)(?<![a-z0-9])(instrumental|karaoke|off\\s*vocal|伴奏\\s*版?|无人声)(?![a-z0-9])"),
        Regex("(?i)(?<![a-z0-9])(纯音乐\\s*版?|纯音\\s*版?|acoustic|inst\\.)(?![a-z0-9])"),
        Regex("(?i)(童声\\s*版?|女声版|男声版|钢琴版|吉他版|清唱|合唱版|铃声版|完整版|高清版|无损版)")
    )

    /**
     * 歌名清洗（保留词间空格，用于**搜索关键词**）：
     * 小写 → 去括号内容 → 去 "/" 之后的部分（B 站常见的 "正标题/上传者 (Cover)" 写法）
     * → 去掉版本修饰词（dj版/remix/live/翻唱/cover/伴奏/instrumental/纯音乐…）。
     */
    private fun strictCleanTitle(raw: String): String {
        var s = raw.lowercase()
        s = BRACKET_SEGMENT_REGEX.replace(s, " ")
        s = s.substringBefore("/")
        for (re in VERSION_DECORATOR_REGEXES) s = re.replace(s, " ")
        return s.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * 严格歌名比对键：**归一化后必须完全相等**才算同一首。
     * [strictCleanTitle] → 去掉所有空白与标点。
     */
    private fun strictTitleKey(raw: String): String =
        KEY_PUNCT_REGEX.replace(strictCleanTitle(raw), "")

    /** 歌手拆词：小写 → 按 `/ 、 & , ;` 拆分 → 去掉空白标点。 */
    private fun strictArtistTokens(raw: String): Set<String> {
        if (raw.isBlank()) return emptySet()
        return raw.lowercase()
            .split(Regex("[/、&,，;；]+"))
            .map { KEY_PUNCT_REGEX.replace(it, "") }
            .filter { it.isNotBlank() }
            .toSet()
    }

    /**
     * 歌手是否有交集：**双方都必须至少有一个非空歌手名**，
     * 且存在一对「完全相等」或「互相包含」（短词长度 < 2 时只认完全相等，避免 "a" 命中一切）。
     */
    private fun strictArtistIntersect(candArtist: String, targetArtist: String): Boolean {
        val a = strictArtistTokens(candArtist)
        val b = strictArtistTokens(targetArtist)
        if (a.isEmpty() || b.isEmpty()) return false
        return a.any { x ->
            b.any { y ->
                if (x == y) true
                else {
                    val short = if (x.length <= y.length) x else y
                    val long = if (x.length <= y.length) y else x
                    short.length >= 2 && long.contains(short)
                }
            }
        }
    }

    /** 时长差（秒）；任一侧 <= 0 表示上游没给时长 → null（未知，放宽但排序靠后）。 */
    private fun durationDeltaSeconds(candSec: Long, targetSec: Long): Long? {
        if (candSec <= 0L || targetSec <= 0L) return null
        return kotlin.math.abs(candSec - targetSec)
    }

    /**
     * 跨源候选的**唯一**准入判定：三重校验全部通过才算「同一首歌」。
     *   ① 歌名归一化后完全相等（[strictTitleKey]）
     *   ② 歌手有交集，**或其中一方 artist 为空**（很多源返回的 artist 是空的，
     *      以前这里直接拒绝 → 《精卫》这类歌被判"在免费歌源里没找到"，是误杀）
     *   ③ 双方 duration 都 > 0 时，差值 <= [STRICT_DURATION_TOLERANCE_SEC] 秒
     *      （任一侧为 0 视为"时长未知"，放宽但不优先，见 [durationDeltaSeconds]）
     * 任何一条不满足 → 直接丢弃（**不存在"仅按歌名相似就通过"的分支**）。
     */
    private fun isStrictSameSong(
        cand: PlaylistSong,
        targetName: String,
        targetArtist: String,
        targetDurationSec: Long,
    ): Boolean {
        val targetKey = strictTitleKey(targetName)
        if (targetKey.isBlank()) return false
        if (strictTitleKey(cand.name) != targetKey) return false
        // ② 歌手校验：任何一方没有可用的歌手名时不做歌手比对（只靠同名 + 时长兜底）
        val candArtistTokens = strictArtistTokens(cand.artist)
        val targetArtistTokens = strictArtistTokens(targetArtist)
        if (candArtistTokens.isNotEmpty() && targetArtistTokens.isNotEmpty() &&
            !strictArtistIntersect(cand.artist, targetArtist)
        ) return false
        val delta = durationDeltaSeconds(cand.duration, targetDurationSec)
        if (delta != null && delta > STRICT_DURATION_TOLERANCE_SEC) return false
        return true
    }

    /**
     * 第二轮「同歌不同版本」准入判定（比 [isStrictSameSong] 宽，但歌手校验更严）：
     *   ① 基准歌名键相等（[songTitleKey]，已剥离版本修饰词/含版本词的括号片段；
     *      B 站风格 "正标题/上传者" 先取 "/" 前段）
     *   ② 歌手交集**强制命中**（与第一轮不同：一方 artist 为空不再放行 —— 日文/小众歌
     *      的同名不同人极多，宁可不播也不错播）
     *   ③ 双方 duration 都 > 0 时，差值 <= [VERSION_DURATION_TOLERANCE_SEC]（版本时长可能不同）
     * 用途：第一轮严格同版本没命中时，跨平台找「同一首歌的其它版本」（如 Faded (即刻电音版) → 酷我的 Faded）。
     */
    private fun isSameSongOtherVersion(
        cand: PlaylistSong,
        targetArtist: String,
        targetDurationSec: Long,
        targetBaseKey: String,
    ): Boolean {
        if (targetBaseKey.isBlank()) return false
        if (songTitleKey(cand.name.substringBefore("/")) != targetBaseKey) return false
        val candArtistTokens = strictArtistTokens(cand.artist)
        val targetArtistTokens = strictArtistTokens(targetArtist)
        if (candArtistTokens.isEmpty() || targetArtistTokens.isEmpty()) return false
        if (!strictArtistIntersect(cand.artist, targetArtist)) return false
        val delta = durationDeltaSeconds(cand.duration, targetDurationSec)
        if (delta != null && delta > VERSION_DURATION_TOLERANCE_SEC) return false
        return true
    }

    /** 搜索关键词：从「精确」到「宽松」依次尝试（校验始终严格，关键词宽松不会放错歌）。 */
    private fun strictSearchKeywords(name: String, artist: String): List<String> {
        val clean = strictCleanTitle(name)
        val rawTitle = name.substringBefore("/").trim()
        val out = LinkedHashSet<String>()
        fun addTitleWithArtist(t: String) {
            if (t.isBlank()) return
            out.add(listOf(t, artist).filter { it.isNotBlank() }.joinToString(" "))
            out.add(t)
        }
        addTitleWithArtist(name.trim())
        if (rawTitle.isNotBlank() && rawTitle != name.trim()) addTitleWithArtist(rawTitle)
        if (clean.isNotBlank() && clean != rawTitle.lowercase().trim()) addTitleWithArtist(clean)
        return out.filter { it.isNotBlank() }.take(4)
    }

    /**
     * 目标歌曲的时长指纹（秒）。
     * 调用方给了就用；没给就回到「歌曲自身来源」按**严格同名 + 歌手交集**找一次元数据补上。
     * 补不到返回 0 = 未知（跨源时放宽，但排序上排在有指纹的候选之后）。
     */
    private suspend fun discoverTargetDurationSec(
        songId: String,
        name: String,
        artist: String,
        own: String,
        given: Long,
    ): Long {
        if (given > 0L) return given
        if (own.isBlank() || name.isBlank()) return 0L
        val key = strictTitleKey(name)
        if (key.isBlank()) return 0L
        for (kw in strictSearchKeywords(name, artist)) {
            val found = runCatching { searchSongs(kw, MusicSource.fromName(own)) }.getOrNull().orEmpty()
            found.firstOrNull {
                strictTitleKey(it.name) == key && strictArtistIntersect(it.artist, artist) && it.duration > 0L
            }?.let { return it.duration }
        }
        return 0L
    }

    /** 内部用：一个通过三重校验的跨源候选。 */
    private data class StrictCandidate(
        val song: PlaylistSong,
        val src: String,
        /** 时长差；null = 时长未知。 */
        val durationDelta: Long?,
    )

    /**
     * 跨源严格解析 —— **所有**跨源播放都必须走这里。
     *
     * 1. 在给定源里按关键词搜索（**只搜一次**，两轮共用同一份候选池）；
     * 2. 第一轮：只用 [isStrictSameSong] 过滤（三重校验，任一不满足直接丢）——最忠实原版；
     * 3. 第二轮（新增）：第一轮全败时，用 [isSameSongOtherVersion] 过滤「同歌不同版本」
     *    （基准歌名相等 + 歌手交集 + 时长差 <= 10s；跳过第一轮已试过的候选）；
     * 4. 两轮都用同一套排序：有明确时长指纹的优先 → 时长差小的优先 → 源优先级高的优先，
     *    按序尝试解析直链，第一个成功的即返回；全失败返回 null（宁可跳过）。
     * 第二轮命中时置 [alternateVersionHit]（UI 层读取后 toast「已为你匹配同歌曲其他版本」）。
     */
    private suspend fun strictResolve(
        name: String,
        artist: String,
        targetDurationSec: Long,
        sources: List<String>,
    ): ResolvedPlay? {
        val targetKey = strictTitleKey(name)
        if (targetKey.isBlank()) return null
        val keywords = strictSearchKeywords(name, artist)
        if (keywords.isEmpty()) return null

        // 基准歌名键（去版本修饰）：第二轮「同歌不同版本」的比较键
        val baseKey = songTitleKey(name.substringBefore("/"))

        // 只发一轮搜索请求：把所有源、所有关键词的结果收集成一份候选池，两轮过滤共用
        val pool = collectStrictCandidates(name, artist, targetDurationSec, sources, keywords)
        if (pool.isEmpty()) return null

        val tried = HashSet<String>()
        suspend fun attempt(cands: Collection<StrictCandidate>): ResolvedPlay? {
            for (cand in orderCandidates(cands)) {
                tried.add(cand.src + "|" + cand.song.id)
                tryResolveUrl(cand.song.id, cand.src)?.let { return ResolvedPlay(it, cand.src, cand.song.id) }
            }
            return null
        }

        // 第一轮：严格同版本（含版本修饰 + 时长差 <= 3s）
        attempt(pool.values.filter { isStrictSameSong(it.song, name, artist, targetDurationSec) })?.let {
            android.util.Log.i("MINova-HTTP", "strictResolve round=1(strict) hit src=" + it.source + " id=" + it.id)
            return it
        }
        // 第二轮：同歌不同版本（基准歌名 + 歌手交集 + 时长差 <= 10s；跳过第一轮已试过的候选）
        attempt(
            pool.values.filter {
                val k = it.src + "|" + it.song.id
                !tried.contains(k) && isSameSongOtherVersion(it.song, artist, targetDurationSec, baseKey)
            }
        )?.let {
            alternateVersionHit = true
            android.util.Log.i("MINova-HTTP", "strictResolve round=2(otherVersion) hit src=" + it.source + " id=" + it.id)
            return it
        }
        return null
    }

    /** 汇集候选池：给定源 × 关键词各搜一次，按 `src|id` 去重（两轮过滤共用，不重复发请求）。
     *  源之间**并行**搜索（互不依赖；串行时 5 源 × 4 词在慢后端下轻松超过起播预算）。 */
    private suspend fun collectStrictCandidates(
        name: String,
        artist: String,
        targetDurationSec: Long,
        sources: List<String>,
        keywords: List<String> = strictSearchKeywords(name, artist),
    ): LinkedHashMap<String, StrictCandidate> {
        val pool = LinkedHashMap<String, StrictCandidate>()
        if (keywords.isEmpty()) return pool
        val perSource: List<Pair<String, List<PlaylistSong>>> = kotlinx.coroutines.coroutineScope {
            sources.map { src ->
                async(Dispatchers.IO) {
                    val merged = ArrayList<PlaylistSong>()
                    for (kw in keywords) {
                        merged.addAll(runCatching { searchSongs(kw, MusicSource.fromName(src)) }.getOrNull().orEmpty())
                    }
                    src to merged
                }
            }.map { it.await() }
        }
        for ((src, list) in perSource) {
            for (c in list) {
                if (c.id.isBlank()) continue
                val k = src + "|" + c.id
                if (pool.containsKey(k)) continue
                pool[k] = StrictCandidate(c, src, durationDeltaSeconds(c.duration, targetDurationSec))
            }
        }
        return pool
    }

    /** 既定候选排序：有明确时长指纹的优先 → 时长差小的优先 → 源优先级高的优先。 */
    private fun orderCandidates(cands: Collection<StrictCandidate>): List<StrictCandidate> =
        cands.sortedWith(
            compareBy<StrictCandidate> { if (it.durationDelta == null) 1 else 0 }
                .thenBy { it.durationDelta ?: Long.MAX_VALUE }
                .thenBy { sourcePriority(it.src) }
        )

    /**
     * 并发抢答（Promise.any 语义）：所有候选**同时**解析直链（/music/url 直链，各 2.5s 预算），
     * 总预算 3s。快 CDN 源的结果先到先得立即胜出；非快 CDN 结果先暂存，
     * 快源全败时用第一个暂存结果（慢源自然被快源跑赢）。
     * 输家在独立 scope 里自然超时，不阻塞调用方。
     */
    private suspend fun raceFirstUrl(candidates: List<PlayCandidate>): ResolvedPlay? {
        if (candidates.isEmpty()) return null
        val ch = kotlinx.coroutines.channels.Channel<ResolvedPlay>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val raceScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
        candidates.forEach { c ->
            raceScope.launch {
                val u = runCatching {
                    kotlinx.coroutines.withTimeoutOrNull(2_500) { tryResolveUrl(c.id, c.source) }
                }.getOrNull()
                if (!u.isNullOrBlank() && u.startsWith("http")) ch.trySend(ResolvedPlay(u, c.source, c.id))
            }
        }
        var winner: ResolvedPlay? = null
        var slow: ResolvedPlay? = null
        val deadline = System.currentTimeMillis() + 3_000
        while (winner == null && System.currentTimeMillis() < deadline) {
            val left = (deadline - System.currentTimeMillis()).coerceAtLeast(1)
            val msg = kotlinx.coroutines.withTimeoutOrNull(left) { ch.receive() } ?: break
            if (msg.source in FAST_CDN_SOURCES) winner = msg else if (slow == null) slow = msg
        }
        ch.close()
        return winner ?: slow
    }

    /**
     * 快速解析（起播与预解析共用，1~2s 体感的根基）：
     *  ① 便宜路径：自身源快 CDN 同 id 直取（1 次请求、无搜索，通常 <1s）→ 命中即返回；
     *  ② 候选池（源间并行搜索，6s 预算）→ [raceFirstUrl] 并发抢答（2.5s/候选、3s 总预算）；
     *  ③ netease 收尾：抢答胜出是 netease 直链时**优先改走串流代理**（直链 prepare 慢 31s，
     *     代理服务端取流 prepare ~1s）；抢答全败也走串流代理；代理不可用才回退 netease 直链。
     * 返回值带完整候选列表，供 UI 层秒死轮换（cheapOwn 命中时候选列表只有自身源，
     * UI 层应在后台补全，见 MainActivity.playSong）。
     */
    suspend fun resolveFastUrl(
        songId: String,
        name: String,
        artist: String,
        knownSource: String,
        durationSec: Long = 0L,
    ): FastResolve? = withContext(Dispatchers.IO) {
        val own = knownSource.trim().lowercase()
        // ① 自身源快 CDN 直取（1 请求）
        if (own in FAST_CDN_SOURCES && songId.isNotBlank()) {
            val u = runCatching {
                kotlinx.coroutines.withTimeoutOrNull(2_500) { tryResolveUrl(songId, own) }
            }.getOrNull()
            if (!u.isNullOrBlank() && u.startsWith("http")) {
                return@withContext FastResolve(
                    ResolvedPlay(u, own, songId),
                    listOf(PlayCandidate(songId, own, name)),
                    cheapOwn = true,
                )
            }
        }
        // ①' 自身源 = netease：串流代理 URL **零请求即时返回**（服务端取流，prepare ~1s）。
        // 8 秒卡顿的根源是「候选池搜索 2~6s + 抢答 3s」排在起播前面 —— netease 歌不再等搜索，
        // 代理直接先播；跨源并发抢答移到后台补全（结果只作秒死轮换弹药）。
        if (own == "netease" && songId.isNotBlank()) {
            val proxy = streamProxyUrl(songId, "netease")
            if (proxy != null) {
                return@withContext FastResolve(
                    ResolvedPlay(proxy, "netease", songId),
                    listOf(PlayCandidate(songId, "netease", name)),
                    cheapOwn = true,
                )
            }
        }
        // ② 候选池 + 并发抢答
        val candidates = runCatching {
            kotlinx.coroutines.withTimeoutOrNull(6_000) {
                resolvePlayCandidates(songId, name, artist, knownSource, durationSec)
            }
        }.getOrNull().orEmpty()
        val list = candidates.ifEmpty {
            listOf(PlayCandidate(songId, own.ifBlank { "netease" }, name))
        }
        val raced = raceFirstUrl(list)
        // ③ netease 收尾：串流代理优先于 netease 直链
        val neteaseCand = list.firstOrNull { it.source.equals("netease", true) }
        val proxyUrl = neteaseCand?.let { streamProxyUrl(it.id, "netease") }
        when {
            raced != null && !raced.source.equals("netease", true) ->
                FastResolve(raced, list)
            proxyUrl != null ->
                FastResolve(ResolvedPlay(proxyUrl, "netease", neteaseCand.id), list)
            raced != null ->
                FastResolve(raced, list)
            else -> null
        }
    }

    /**
     * 起播候选列表（**已排序**，供 UI 层「起播 5 秒内 onError → 轮换下一个候选（最多 3 个）」）：
     *  ① 歌曲自身源：快 CDN 源（[FAST_CDN_SOURCES]）排最前同 id 直取；慢源（netease 等）排**最后**兜底；
     *  ② 跨源第一轮「严格同版本」候选（排序同 [orderCandidates]）；
     *  ③ 跨源第二轮「同歌不同版本」候选（同上；与第一轮去重）。
     * 只做搜索与过滤，**不解析直链** —— URL 由 UI 层按需经 [candidateUrl] 解析，
     * 坏候选秒死轮换时才花下一次请求。
     */
    suspend fun resolvePlayCandidates(
        songId: String,
        name: String,
        artist: String,
        knownSource: String,
        durationSec: Long = 0L,
    ): List<PlayCandidate> = withContext(Dispatchers.IO) {
        val out = ArrayList<PlayCandidate>()
        if (name.isBlank()) return@withContext out
        val own = knownSource.trim().lowercase()
        val seen = HashSet<String>()
        fun push(id: String, src: String, n: String) {
            if (id.isBlank() || src.isBlank()) return
            val k = src + "|" + id
            if (seen.add(k)) out.add(PlayCandidate(id, src, n))
        }
        // ① 自身源（快 CDN 直取排最前）
        if (own.isNotBlank() && songId.isNotBlank() && own in FAST_CDN_SOURCES) push(songId, own, name)
        val target = discoverTargetDurationSec(songId, name, artist, own, durationSec)
        val others = PREFERRED_SOURCES.filter { it != own }
        // 起播要快：候选池只用前 2 个关键词（全名+歌手 / 纯歌名），源间并行已覆盖大部分场景
        val keywords = strictSearchKeywords(name, artist).take(2)
        if (keywords.isNotEmpty()) {
            val pool = collectStrictCandidates(name, artist, target, others, keywords)
            val baseKey = songTitleKey(name.substringBefore("/"))
            val r1 = orderCandidates(pool.values.filter { isStrictSameSong(it.song, name, artist, target) })
            val r1Keys = HashSet<String>().also { s -> r1.forEach { s.add(it.src + "|" + it.song.id) } }
            val r2 = orderCandidates(
                pool.values.filter {
                    val k = it.src + "|" + it.song.id
                    k !in r1Keys && isSameSongOtherVersion(it.song, artist, target, baseKey)
                }
            )
            (r1 + r2).forEach { push(it.song.id, it.src, it.song.name) }
        }
        // 自身慢源（如 netease）排最后兜底：candidateUrl 对 netease 走串流代理优先
        if (own.isNotBlank() && songId.isNotBlank()) push(songId, own, name)
        out
    }

    /**
     * 在**单个源**内做严格匹配并解析地址。
     *
     * 注意：本方法**不再**"取最像的一个"。内部只有一条路径 —— [strictResolve]，
     * 即「歌名归一化完全相等 + 歌手交集 + （双方时长都 > 0 时）差 <= 3 秒」三重校验，
     * 全不通过就返回 null。旧实现里的"二级：只要求歌名对得上"已删除 ——
     * 那正是「搜到同名/近名的另一首歌就直接播放」的根源。
     */
    suspend fun resolveOriginByName(
        name: String,
        artist: String,
        source: String,
        durationSec: Long = 0L,
    ): ResolvedPlay? = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext null
        val src = source.trim().lowercase().ifBlank { "netease" }
        strictResolve(name, artist, durationSec, listOf(src))
    }

    /**
     * 兼容旧调用：只要地址。
     *
     * ⚠️ 调用方（MainActivity）是**按候选源逐个循环**调用本方法的：传进来的 `source`
     * 是"这次要试的源"，**不一定是这首歌自己的平台**；而 `id` 是这首歌在原平台的 id。
     * 跨平台拿 id 直连是禁止的（不同平台的数字 id 会撞车，对方会返回合法但完全不同的歌），
     * 所以这里**绝不直接信任 id**：只在 `source` 内按「歌名 + 歌手 + 时长」严格搜索，
     * 命中同一首歌才解析直链；找不到就抛 "NO_STRICT_MATCH"（上层提示后跳过）。
     */
    suspend fun resolveSongUrlByName(
        id: String,
        name: String,
        artist: String,
        source: String,
        durationSec: Long = 0L,
    ): String = withContext(Dispatchers.IO) {
        val src = source.trim().lowercase().ifBlank { "netease" }
        // 仅用于日志：能证明这条路径没有拿 id 去跨源直连
        android.util.Log.d("MINova-HTTP", "strictResolve src=" + src + " ignoreId=" + id)
        strictResolve(name, artist, durationSec, listOf(src))?.url
            ?: throw MusicApiException("NO_STRICT_MATCH")
    }

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

    /** 快 CDN 源（与 MainActivity.FAST_CDN_SOURCES 口径一致）：自身源同 id 直取排最前。 */
    private val FAST_CDN_SOURCES = setOf("kuwo", "migu", "bilibili")

    /**
     * 后端串流代理地址（swagger `/api/v1/music/stream`：服务端取流转发，解决防盗链/跨域，
     * 返回 audio/mpeg）。对网易电台/小众内容尤其关键：`/music/url` 给的直链常秒死，
     * 而代理在服务端拉流（实测同一首歌代理能稳定返回完整音频）。
     */
    private fun streamProxyUrl(id: String, source: String): String? {
        if (id.isBlank()) return null
        if (source.equals("archive", ignoreCase = true) || id.startsWith("ia:")) return null
        val src = source.ifBlank { "netease" }
        return buildUrl(baseUrl, "/api/v1/music/stream", listOf("id" to id, "source" to src))
    }

    /**
     * 候选起播地址：netease 走**串流代理优先**（客户端直连网易 CDN 慢、电台/小众直链秒死），
     * 其它源**直链优先**（快 CDN 亚秒出声）、代理兜底。两者都拿不到返回 null。
     */
    suspend fun candidateUrl(id: String, source: String): String? = withContext(Dispatchers.IO) {
        val src = source.trim().lowercase()
        if (src == "netease") {
            streamProxyUrl(id, src) ?: tryResolveUrl(id, src)
        } else {
            tryResolveUrl(id, src) ?: streamProxyUrl(id, src)
        }
    }

    suspend fun searchSongs(keyword: String, source: MusicSource, page: Int = 1): List<PlaylistSong> = withContext(Dispatchers.IO) {
        // 免费曲库走 Internet Archive 的公开接口，不经后端聚合服务
        if (source == MusicSource.ARCHIVE) return@withContext searchArchive(keyword)
        val src = source.apiSourceName() ?: throw MusicApiException("该平台不支持在线搜索")
        if (keyword.isBlank()) throw MusicApiException("搜索关键词不能为空")
        val params = mutableListOf("q" to keyword, "type" to "song", "sources" to src)
    // 只在翻页时带上 page（page=1 与不传等价，保证原有调用行为不变）
    if (page > 1) params.add("page" to page.toString())
    val url = buildUrl(baseUrl, "/api/v1/music/search", params)
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
