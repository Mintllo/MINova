package com.HyperX.MI.Na
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.foundation.layout.ColumnScope
import com.HyperX.MI.Na.native.NativeBridge
import kotlin.math.roundToInt
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.clip


import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.WindowManager

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.key
import androidx.compose.ui.layout.onGloballyPositioned
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.google.gson.Gson
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.HyperX.MI.Na.liquid.MiuixBackdropAdapter
import top.yukonga.miuix.kmp.blur.layerBackdrop as miuixLayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop as miuixRememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.sign
import kotlin.time.Clock
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

val MD3SpringFloat = spring<Float>(dampingRatio = 0.8f, stiffness = 450f)
val MD3SpringDp = spring<Dp>(dampingRatio = 0.75f, stiffness = 400f)
val MD3SpringOffset = spring<IntOffset>(dampingRatio = 0.8f, stiffness = 450f)
val MD3SpringSize = spring<IntSize>(dampingRatio = 0.8f, stiffness = 450f)
val MD3SpringConsole = spring<Dp>(dampingRatio = 0.55f, stiffness = 220f)

@Composable
fun Modifier.bounceClick(
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(0.dp),
    onClick: () -> Unit = {}
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.94f else 1.0f
    val scaleFactor by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 450f),
        label = "JellyScale"
    )
    return this
        .graphicsLayer { scaleX = scaleFactor; scaleY = scaleFactor }
        .clip(shape)
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

data class Song(
    val id: String,
    val name: String,
    val artist: String,
    val album: String = "",
    val duration: Long = 0,
    val picUrl: String = "",
    /** 已下载歌曲在下载目录里的实际文件名（重命名后与 id 不再一致）；未下载为空串 */
    val fileName: String = "",
    /** 歌曲热度/播放量（由后端返回；0 表示未知，前端仅在 >0 时展示） */
    val heat: Long = 0,
    /**
     * 该歌曲 id 所属的平台（kuwo / migu / bilibili / netease / qq / kugou）。
     *
     * ⚠️ 这个字段是「歌词与歌曲错配」修复的关键：后端 `id` 只在同一个 platform 内唯一，
     * 用 A 平台的 id 去 B 平台取歌词会拿到**另一首歌**的歌词（实测 kuwo id 228908 是
     * 《晴天》，而 netease id 228908 是《人面桃花》）。空串表示来源未知。
     */
    val source: String = ""
)

data class LyricData(
    val lyrics: Map<Long, String> = emptyMap(),
    val translations: Map<Long, String> = emptyMap(),
    val times: List<Long> = emptyList(),
    /** LRC 头部 `[ti:...]`，用于校验「这份歌词是不是这首歌的」；没有该标签时为空 */
    val title: String = "",
    /** LRC 头部 `[ar:...]`，同上 */
    val artist: String = "",
    /**
     * 上游明确标注为纯音乐 / 无歌词（占位歌词命中）。
     * 只用于区分文案：纯音乐 →「纯音乐，请欣赏」，普通无词 →「暂无歌词」；
     * 前奏期间（有歌词但还没到第一句）两者都不对，应显示「等待前奏…」。
     */
    val instrumental: Boolean = false
)

/**
 * 不是真歌词的「占位内容」。上游把这类文案当歌词返回（尤其 netease / qq），
 * 命中即视为无歌词，不显示，避免用户以为「歌词错了」。
 */
private val PLACEHOLDER_LYRICS = listOf(
    "纯音乐，请欣赏", "纯音乐请欣赏", "暂无歌词", "暂无歌词，请欣赏",
    "此歌曲为没有填词的纯音乐", "该歌曲暂无歌词", "纯音乐", "没有填词",
    "此歌曲为纯音乐", "instrumental"
)

data class SearchResponse(val result: SearchResult? = null)
data class SearchResult(val songs: List<SongRaw>? = null)
data class SongRaw(
    val id: Long = 0,
    val name: String? = null,
    val artists: List<ArtistRaw>? = null,
    val album: AlbumRaw? = null,
    val duration: Long = 0
)
data class ArtistRaw(val name: String = "")
data class AlbumRaw(val name: String = "", val picUrl: String? = null)
data class PlayUrlResponse(val code: Int = 0, val data: PlayUrlData? = null)
data class PlayUrlData(val url: String = "")
data class LyricApiResponse(
    val lrc: LyricContainer? = null,
    val klyric: LyricContainer? = null,
    val tlyric: LyricContainer? = null,
    val nolyric: Boolean = false,
    val uncollected: Boolean = false
)
data class LyricContainer(val lyric: String? = null)

class MusicApiService {
    companion object {
        private const val SEARCH_URL = "http://music.163.com/api/search/get/web"
        private const val SONG_DETAIL_URL = "https://music.163.com/api/song/detail"
        private const val PLAY_URL = "https://yunzhiapi.cn/API/yyjhss.php"
        private const val LYRIC_URL = "http://music.163.com/api/song/lyric"
        private const val TOKEN = "YOUR_THIRD_PARTY_TOKEN"
        val LRC_REGEX = "\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\](.*)".toRegex()
        val gsonInstance = Gson()

        /** 全局共享实例：保留 searchCache，避免每次重组/切页都重建并重复请求后端 */
        val shared: MusicApiService by lazy { MusicApiService() }
    }

    private val searchCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, List<Song>>>()
    /** 空结果短缓存：防止「换一批」反复重打后端、越刷新越拿不到 */
    private val emptyCache = java.util.concurrent.ConcurrentHashMap<String, Long>()

    suspend fun search(
            keyword: String,
            sources: List<String> = listOf("netease"),
        ): Result<List<Song>> = withContext(Dispatchers.IO) {
        // 10 分钟内同关键词走缓存，避免重复请求造成限流假死
        val cacheKey = keyword + "#" + sources.hashCode()
        searchCache[cacheKey]?.let { (t, v) ->
            if (System.currentTimeMillis() - t < 10 * 60_000L) return@withContext Result.success(v)
        }
        // 优先自建免费聚合搜索（网易云/QQ/酷狗/B站）
        // 四个音源并行请求：串行时单个音源卡住会叠加成数十秒，
        // 正是「偶尔搜不到 / 推荐加载不出来」的根因；并行后总耗时约等于最慢的那个源。
        suspend fun fetchMerged(sources: List<String>): List<Song> = runCatching {
            val merged = java.util.Collections.synchronizedMap(LinkedHashMap<String, Song>())
            kotlinx.coroutines.coroutineScope {
                // 必须把「源名」和它的请求结果绑在一起 await：否则拿不到 srcName，
                // 无法把歌曲标记成它真正所属的平台（→ 歌词/播放会串源）。
                val jobs = sources.map { srcName ->
                    srcName to async(Dispatchers.IO) {
                        // 单个来源最多等 8 秒。
                        // 之前这里是"谁慢就一起等"：一个上游卡住，整次搜索就一直转圈，
                        // 表现就是"有时候秒出、有时候等一辈子"。限时后慢的源直接跳过，
                        // 其余源的结果照常返回。
                        kotlinx.coroutines.withTimeoutOrNull(8_000) {
                            runCatching {
                                MusicApiClient.searchSongs(keyword, MusicSource.fromName(srcName))
                            }.getOrNull().orEmpty()
                        }.orEmpty()
                    }
                }
                jobs.forEach { (srcName, job) ->
                    job.await().forEach { ps ->
                        // 去重键必须带版本标识：否则同一首歌的「原唱 / DJ版 / Live / 翻唱 / 伴奏」
                        // 会被合并成一条，先到的版本胜出（免 Cookie 源排在前面，DJ 版常常先到）
                        // →「搜不到原唱」「收藏的原唱点进去变成 DJ 版」。
                        val key = (ps.name + "|" + ps.artist).lowercase() +
                            "|" + (parseVersionTag(ps.name, ps.album) ?: "")
                        if (!merged.containsKey(key)) {
                            merged[key] = Song(id = ps.id, name = ps.name, artist = ps.artist,
                                album = ps.album, duration = ps.duration, picUrl = ps.picUrl,
                                source = ps.source.ifBlank { srcName })
                        }
                    }
                }
            }
            // 按「抓取到达顺序」原样返回：展示层统一做整体倒序（用户要求默认倒序、不可调整）。
            // 这里不再做「原唱优先」重排 —— sortSearchResults / isJunkVersion 保留但已不参与展示。
            merged.values.toList().filter { isLikelyMusic(it) }
        }.getOrNull().orEmpty()

        // 全空时自动重试一次：上游偶发抽风（限流/超时）时第二次往往就正常。
        // 这是「有人搜索音乐加载不出来」最有效的兜底。
        // 用户方案：搜索只走网易云（原曲目录最全、原唱命中最高）；
        // 点击播放时再由 resolveNextPlayUrl 去其他平台严格匹配同一首歌（快 CDN 优先）。
        var free = fetchMerged(sources)
        if (free.isEmpty()) {
            free = fetchMerged(listOf("kuwo", "migu", "qianqian", "bilibili", "qq"))
        }
        if (free.isEmpty()) {
            kotlinx.coroutines.delay(700L)
            free = fetchMerged(sources)
        }
        if (free.isNotEmpty()) {
            searchCache[cacheKey] = System.currentTimeMillis() to free
            return@withContext Result.success(free)
        }
        // 空结果也做 45 秒短缓存，避免「换一批」反复重打后端造成限流恶性循环
        val emptyAt = emptyCache[cacheKey]
        if (emptyAt != null && System.currentTimeMillis() - emptyAt < 45_000L) {
            return@withContext Result.success(emptyList())
        }
        emptyCache[cacheKey] = System.currentTimeMillis()
        try {
            val encoded = URLEncoder.encode(keyword, "UTF-8")
            val url = URL("$SEARCH_URL?s=$encoded&type=1&limit=50")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            val json = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val response = gsonInstance.fromJson(json, SearchResponse::class.java)
            val rawSongs = response.result?.songs ?: emptyList()

            if (rawSongs.isEmpty()) return@withContext Result.success(emptyList())

            val ids = rawSongs.map { it.id }.joinToString(",")
            val coverMap = fetchCovers(ids)

            val songs = rawSongs.map { raw ->
                Song(
                    id = raw.id.toString(),
                    name = raw.name ?: "未知歌曲",
                    artist = raw.artists?.joinToString(", ") { it.name } ?: "未知艺术家",
                    album = raw.album?.name ?: "",
                    duration = raw.duration,
                    picUrl = coverMap[raw.id] ?: "",
                    source = "netease"
                )
            }
            Result.success(songs)
        } catch (_: Exception) {
            Result.failure(Exception("Search failed"))
        }
    }

    /**
     * 加载更多：同一个关键词取**下一页**（后端 `/api/v1/music/search` 的 page 从 1 开始）。
     *
     * 只在 page > 1 时才把 `page` 拼进 query（page=1 与不传等价），因此对原有调用零影响。
     * 返回的是这一页的新歌；去重/合并交给调用方（ViewModel.loadMoreSearch）。
     */
    suspend fun searchPage(keyword: String, page: Int): List<Song> = withContext(Dispatchers.IO) {
        if (keyword.isBlank() || page < 2) return@withContext emptyList()
        val sources = listOf("netease")
        val merged = java.util.Collections.synchronizedMap(LinkedHashMap<String, Song>())
        kotlinx.coroutines.coroutineScope {
            val jobs = sources.map { srcName ->
                srcName to async(Dispatchers.IO) {
                    kotlinx.coroutines.withTimeoutOrNull(8_000) {
                        runCatching {
                            MusicApiClient.searchSongs(keyword, MusicSource.fromName(srcName), page)
                        }.getOrNull().orEmpty()
                    }.orEmpty()
                }
            }
            jobs.forEach { (srcName, job) ->
                job.await().forEach { ps ->
                    // 与 search() 同一套去重键（带版本标识），保证两个入口的口径一致
                    val key = (ps.name + "|" + ps.artist).lowercase() +
                        "|" + (parseVersionTag(ps.name, ps.album) ?: "")
                    if (!merged.containsKey(key)) {
                        merged[key] = Song(id = ps.id, name = ps.name, artist = ps.artist,
                            album = ps.album, duration = ps.duration, picUrl = ps.picUrl,
                            source = ps.source.ifBlank { srcName })
                    }
                }
            }
        }
        merged.values.toList()
    }

    private fun fetchCovers(ids: String): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        try {
            val url = URL("$SONG_DETAIL_URL?ids=[$ids]")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.setRequestProperty("Referer", "https://music.163.com/")
            val json = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val root = JSONObject(json)
            val songsArray = root.optJSONArray("songs") ?: return map
            for (i in 0 until songsArray.length()) {
                val item = songsArray.getJSONObject(i)
                val id = item.optLong("id", 0L)
                val album = item.optJSONObject("album")
                val picUrl = album?.optString("picUrl", "") ?: ""
                if (id > 0 && picUrl.isNotEmpty()) {
                    map[id] = picUrl.replace("http://", "https://")
                }
            }
        } catch (_: Exception) {}
        return map
    }

    suspend fun getPlayUrl(songId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$PLAY_URL?id=$songId&type=wy&token=$TOKEN")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            val json = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val response = gsonInstance.fromJson(json, PlayUrlResponse::class.java)
            if (response.code == 1 && response.data != null) {
                Result.success(response.data.url.replace("\\/", "/").replace("http://", "https://"))
            } else {
                Result.failure(Exception("API Error"))
            }
        } catch (_: Exception) {
            Result.failure(Exception("Play url failed"))
        }
    }

    suspend fun fetchLyrics(songId: String): Result<LyricData> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$LYRIC_URL?lv=1&kv=1&tv=1&id=$songId")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            val json = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val response = gsonInstance.fromJson(json, LyricApiResponse::class.java)
            if (response.nolyric || response.uncollected) return@withContext Result.success(LyricData())
            Result.success(parseLrc(response.lrc?.lyric ?: "", response.tlyric?.lyric ?: ""))
        } catch (_: Exception) {
            Result.failure(Exception("Lyric failed"))
        }
    }

    suspend fun fetchLyricsExternal(ctx: android.content.Context, songId: String, source: String): Result<LyricData> = withContext(Dispatchers.IO) {
        try {
            val base = MusicApiPrefs.baseUrl(ctx)
            val u = base.trimEnd('/') + "/api/v1/music/lyric?id=" + java.net.URLEncoder.encode(songId, "UTF-8") +
                    "&source=" + java.net.URLEncoder.encode(source.ifBlank { "netease" }, "UTF-8")
            val conn = URL(u).openConnection() as HttpURLConnection
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.setRequestProperty("Accept", "application/json")
            val text = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val o = org.json.JSONObject(text).optJSONObject("data")
            val lrc = o?.optString("lyric").orEmpty()
            if (lrc.isBlank()) Result.failure(Exception("no lyric")) else Result.success(parseLrc(lrc, ""))
        } catch (_: Exception) {
            Result.failure(Exception("lyric failed"))
        }
    }

    private fun parseLrc(lrcText: String, tlyricText: String): LyricData {
        val tagRe = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")
        // 同一时间戳上游常常打包多份文本：原文 / 罗马音 / 中文翻译（netease、kugou、qq 都这样）。
        // 先把"这个时间戳下的所有候选文本"按出现顺序收集，最后统一择优 —— 边收边覆盖会丢原文。
        val candidates = LinkedHashMap<Long, MutableList<String>>()
        val times = mutableListOf<Long>()

        fun feed(line: String, collectTimes: Boolean) {
            val tags = tagRe.findAll(line).toList()
            if (tags.isEmpty()) return
            val text = tagRe.replace(line, "").trim()
            if (text.isEmpty() || text == "//") return
            // kugou/qq 会给"每一个字"都打时间戳：[00:00.00]周[00:00.93]杰[00:01.86]伦
            // 照旧"每个时间戳都写一遍整行"会让同一句在每个字的时间点反复出现，
            // 开头几行作词/作曲信息不停重复、真正的歌词被挤掉。
            val segments = line.split(tagRe).map { it.trim() }.filter { it.isNotEmpty() }
            val charwise = tags.size >= 3 && segments.isNotEmpty() && segments.all { it.length <= 2 }
            // 增强型 LRC：行尾还有一个"结束时间"标签：[00:16.00]白昼夢 繋いでいて[00:20.00]
            // 这类行只能用第一个标签当歌词时间；否则同一句会被写到结束时间上，
            // 和下一小节的真正歌词抢同一个时间戳（表现为歌词错位/串行）。
            val endsWithTag = line.trimEnd().endsWith("]")
            val effective = if (charwise || (endsWithTag && tags.size >= 2)) listOf(tags.first()) else tags
            effective.forEach { mm ->
                val min = mm.groupValues[1].toLongOrNull() ?: return@forEach
                val sec = mm.groupValues[2].toLongOrNull() ?: return@forEach
                val fracRaw = mm.groupValues[3]
                val frac = when (fracRaw.length) {
                    0 -> 0L
                    1 -> fracRaw.toLong() * 100
                    2 -> fracRaw.toLong() * 10
                    else -> fracRaw.take(3).toLong()
                }
                val t = min * 60000 + sec * 1000 + frac
                val list = candidates.getOrPut(t) { mutableListOf() }
                if (!list.contains(text)) list.add(text)
                if (collectTimes && !times.contains(t)) times.add(t)
            }
        }

        // ── native 热点路径（libminova.so，可选）────────────────────────────
        // C++ 侧与 feed() 语义逐条一致：同一时间戳多份文本按出现顺序收集、
        // 逐字时间戳/增强型 LRC 只取首标签、空文本与 "//" 跳过、trim 对齐
        // Character.isWhitespace。输出按时间稳定排序的 "mm:ss.xx\t文本"。
        // native 不可用或失败 → null，走下方原 Kotlin feed 逻辑，行为一致。
        val nLrc = NativeBridge.parseLrc(lrcText)

        // "mm:ss.xx\t文本" → (毫秒, 文本)：与 native 时间规则互逆（百分秒 ×10 = 毫秒）
        fun entryTime(e: String): Pair<Long, String>? {
            val tab = e.indexOf('\t')
            if (tab <= 0) return null
            val hms = e.substring(0, tab).split(':')
            if (hms.size != 2) return null
            val mm = hms[0].toLongOrNull() ?: return null
            val dot = hms[1].indexOf('.')
            if (dot < 0) return null
            val ss = hms[1].substring(0, dot).toLongOrNull() ?: return null
            val cs = hms[1].substring(dot + 1)
            if (ss < 0L || cs.length != 2) return null
            val frac = cs.toLongOrNull() ?: return null
            return (mm * 60000 + ss * 1000 + frac * 10) to e.substring(tab + 1)
        }

        if (nLrc != null) {
            for (e in nLrc) entryTime(e)?.let { (t, text) ->
                val list = candidates.getOrPut(t) { mutableListOf() }
                if (!list.contains(text)) list.add(text)
                if (!times.contains(t)) times.add(t)
            }
            val nTlyric = NativeBridge.parseLrc(tlyricText)
            if (nTlyric != null) {
                // 翻译行不进 times（与 feed(it, collectTimes=false) 一致）：
                // 择优时只挂在 lrc 已有时间戳上，多出的翻译时间戳会被丢弃。
                for (e in nTlyric) entryTime(e)?.let { (t, text) ->
                    val list = candidates.getOrPut(t) { mutableListOf() }
                    if (!list.contains(text)) list.add(text)
                }
            } else {
                tlyricText.lines().forEach { feed(it, false) }
            }
        } else {
            lrcText.lines().forEach { feed(it, true) }
            tlyricText.lines().forEach { feed(it, false) }
        }
        times.sort()

        // ── 择优：原文优先（假名/谚文 > 汉字 > 其它）──
        // 旧逻辑是"同时间戳后到的那行直接覆盖"，于是日文歌里最后到达的中文翻译
        // 把日文原文顶掉，只剩罗马音/拼音（用户反馈「日文显示成拼音」）。
        // 现在：含假名/谚文的必是日/韩原文，优先当主行；翻译位优先放"另一种原生文字"。
        val kanaRe = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\uFF66-\\uFF9F\\uAC00-\\uD7AF\\u1100-\\u11FF\\u3130-\\u318F]")
        fun nativeCount(s: String) = s.count { isNativeText(it) }
        val lyrics = LinkedHashMap<Long, String>()
        val translations = LinkedHashMap<Long, String>()
        for (t in times) {
            val list = candidates[t]?.filter { it.isNotBlank() } ?: continue
            if (list.isEmpty()) continue
            val main = list.firstOrNull { kanaRe.containsMatchIn(it) }
                ?: list.firstOrNull { nativeCount(it) > 0 }
                ?: list.first()
            lyrics[t] = main
            val others = list.filter { it != main }
            val trans = others.firstOrNull { nativeCount(it) > 0 } ?: others.firstOrNull()
            if (trans != null) translations[t] = trans
        }

        // 头部元信息（[ti:歌名] / [ar:歌手]）：用来校验「这份歌词是不是这首歌的」。
        // 没有这两个标签时留空，此时不做校验（宁可显示，也不误杀正确歌词）。
        fun meta(key: String): String =
            Regex("\\[" + key + ":([^\\]]*)\\]").find(lrcText)?.groupValues?.get(1)?.trim().orEmpty()

        // 播放器/iTunes 常见的「纯音乐，请欣赏」「暂无歌词」占位歌词：
        // 这类内容不是真歌词，直接视为无歌词（不显示），避免误导用户。
        val joined = lyrics.values.joinToString(" ")
        val placeholderOnly = lyrics.size <= 2 && PLACEHOLDER_LYRICS.any { joined.contains(it) }
        if (placeholderOnly) {
            // 占位歌词不是真歌词，但它是"纯音乐/无词"的唯一可靠信号 → 记下来给 UI 用
            return LyricData(title = meta("ti"), artist = meta("ar"), instrumental = true)
        }

        return LyricData(lyrics, translations, times, meta("ti"), meta("ar"))
    }

    fun getFallbackUrl(songId: String): String = "https://music.163.com/song/media/outer/url?id=$songId.mp3"
}

// ============================================================================
//  液态玻璃能力门槛（产品层面限制）：RuntimeShader / AGSL 仅 Android 13（API 33）+ 可用。
//  低版本虽有逐点降级守卫，但这里从产品层面直接禁选：
//  - 液态玻璃风格（md3Style = false）禁止选择，已选中则启动时自动回退 Material Design 3；
//  - 液态玻璃导航栏（useFloatingNavBar）与玻璃模糊（navBarLiquidGlass）禁止开启，已开启则强制关闭。
//  风格 id ↔ 依赖对照：md3Style=false「液态玻璃」→ 依赖 liquid/ 包（Lens/Vibrancy/InnerShadow/
//  CombinedBackdrop，均基于 RuntimeShader）+ InteractiveHighlight（API 33 守卫）；
//  md3Style=true「Material Design 3」→ 不依赖 shader，是低版本的兼容回退风格。
// ============================================================================
/** 当前设备是否支持液态玻璃效果（RuntimeShader/AGSL 需要 Android 13 / API 33+）。 */
internal val liquidGlassSupported: Boolean
    get() = Build.VERSION.SDK_INT >= 33

/** 液态玻璃类选项的统一角标/提示文案。 */
internal const val LIQUID_GLASS_REQUIRE_TEXT = "需要 Android 13+"

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = MusicApiService()
    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val tokenFile = File(application.filesDir, "user_token.dat")
    private val userFile = File(application.filesDir, "user_info.dat")
    private val historyFile = File(application.filesDir, "search_history.json")
    private val playCountsFile = File(application.filesDir, "play_counts.json")
    /** 收藏本地缓存：未登录也能收藏、离线/重启不丢；登录后与服务端取并集 */
    // 注意 get() 里不能直接用构造参数 application（只有初始化器能看见它），要用 getApplication()
    private val favoritesFile: File
        get() = File(getApplication<Application>().filesDir, "favorites_" + favoritesOwner() + ".json")

    /**
     * 收藏缓存的归属账号（和推荐/口味的做法一致）。
     * 必须按账号隔离：否则 A 退出、B 登录时，A 留在本地的收藏会被当成"本地新增"
     * 补传到 B 的账号上（串号 + 数据污染）。
     */
    private fun favoritesOwner(): String = try {
        if (userFile.exists()) {
            val jo = JSONObject(userFile.readText())
            val id = jo.optLong("id", 0L)
            if (id > 0L) "id$id" else jo.optString("username", "").ifBlank { "guest" }
        } else "guest"
    } catch (_: Exception) {
        "guest"
    }

    var mediaPlayer: MediaPlayer? = null
        private set
    private lateinit var mediaSession: MediaSessionCompat

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())

    /**
     * 当前播放队列：从哪个列表点的播放，就把那个列表记下来。
     *
     * 之前 playNext/playPrevious 只在搜索结果里找下一首，
     * 所以从歌单 / 每日 30 首 / 收藏里播放时找不到下一首，整张列表播完就停了。
     */
    // ────────────────────────── 记住上次播放 ──────────────────────────
    //
    // 记录最后一首歌与播放进度，重启应用后恢复。默认关闭，用户可在设置或首次引导里打开。
    // 恢复时不自动播放（避免一进应用就外放），点播放会从上次的进度继续。

    private val _rememberLast = MutableStateFlow(false)
    val rememberLast: StateFlow<Boolean> = _rememberLast

    /** 上次恢复的歌曲 id + 进度：只对这首歌生效，用户换歌不会被带过去 */
    private var pendingSeekSongId: String = ""
    private var pendingSeekMs: Long = 0L

    private fun resumePrefs(ctx: android.content.Context) =
        ctx.getSharedPreferences("playback_resume", android.content.Context.MODE_PRIVATE)

    /** 打开/关闭「记住上次播放」；关闭时把已存的记录一并清掉。 */
    fun setRememberLast(ctx: android.content.Context, on: Boolean) {
        _rememberLast.value = on
        resumePrefs(ctx).edit().putBoolean("enabled", on).apply()
        if (!on) {
            resumePrefs(ctx).edit().remove("song").remove("position").apply()
            pendingSeekMs = 0L
            pendingSeekSongId = ""
        }
    }

    /** 保存当前歌曲 + 进度（开关关闭时直接跳过，不写盘）。 */
    fun savePlaybackState() {
        if (!_rememberLast.value) return
        val s = _currentSong.value ?: return
        val ctx = getApplication<Application>()
        val o = org.json.JSONObject().apply {
            put("id", s.id)
            put("name", s.name)
            put("artist", s.artist)
            put("album", s.album)
            put("duration", s.duration)
            put("picUrl", s.picUrl)
            put("source", s.source)
            put("fileName", s.fileName)
        }
        resumePrefs(ctx).edit()
            .putString("song", o.toString())
            .putLong("position", _currentPosition.value)
            .apply()
    }

    /** 启动时恢复：把上次的歌与进度放回播放器（不自动播放）。 */
    fun restorePlaybackState(ctx: android.content.Context) {
        val p = resumePrefs(ctx)
        _rememberLast.value = p.getBoolean("enabled", false)
        if (!_rememberLast.value) return
        val rawSong = p.getString("song", null) ?: return
        val o = runCatching { org.json.JSONObject(rawSong) }.getOrNull() ?: return
        val id = o.optString("id")
        if (id.isBlank()) return
        val song = Song(
            id = id,
            name = o.optString("name"),
            artist = o.optString("artist"),
            album = o.optString("album", ""),
            duration = o.optLong("duration", 0L),
            picUrl = o.optString("picUrl", ""),
            fileName = o.optString("fileName", ""),
            source = o.optString("source", ""),
        )
        _currentSong.value = song
        _duration.value = song.duration * 1000L
        val pos = p.getLong("position", 0L)
        _currentPosition.value = pos
        pendingSeekSongId = song.id
        pendingSeekMs = pos
    }

    /** 播放器准备完毕时应用"上次进度"。只对上次那首歌生效。 */
    private fun applyPendingSeek(mp: MediaPlayer) {
        if (pendingSeekMs <= 0L) return
        if (_currentSong.value?.id != pendingSeekSongId) return
        runCatching { mp.seekTo(pendingSeekMs.toInt()) }
        _currentPosition.value = pendingSeekMs
        pendingSeekMs = 0L
    }

    private val _playQueue = MutableStateFlow<List<Song>>(emptyList())

    /** 连续播放失败次数：用来在整张歌单都要会员时停止"无限自动跳过"。 */
    private var playFailStreak = 0
    /** 当前这首歌是否已用过网易云 outer 兜底（只允许一次，避免 onError 死循环） */
    private var neteaseFallbackTried = false

    /**
     * 兜底播放队列：推荐页各分区点歌时没有显式传队列，
     * 用这份"当前页面歌曲集合"兜底，保证一首放完还能自动接下一首。
     */
    private val _fallbackQueue = MutableStateFlow<List<Song>>(emptyList())

    fun setFallbackQueue(list: List<Song>) {
        if (list.isNotEmpty()) _fallbackQueue.value = list
    }
    val searchResults: StateFlow<List<Song>> = _searchResults
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    // ── 「加载更多」分页状态 ──
    /** 当前关键词已经加载到第几页（后端 page 从 1 开始） */
    private var currentSearchPage = 1
    /** 连续「这一页没拿到新歌」的次数：避免无限翻页（表现为一直停在加载中） */
    private var emptyPageStreak = 0
    /** 当前搜索关键词：加载更多必须用同一个词 */
    private var currentSearchKeyword = ""
    private val _hasMoreSearch = MutableStateFlow(false)
    val hasMoreSearch: StateFlow<Boolean> = _hasMoreSearch
    /**
     * 加载更多：用同一个关键词取下一页并**追加**到现有结果（按 id+source 去重）。
     *
     * 后端 page 从 1 开始；page>=2 时上游会把"弱相关"的结果也带回来（放开每源条数后更明显），
     * 所以追加前先做**相关性过滤**：歌名或歌手（按 `/ 、 & , ; 空格` 拆词）里必须命中搜索词的
     * 至少一个 token（大小写不敏感、去标点后比较）；一条都不含关键词的直接丢弃。
     */
    fun loadMoreSearch() {
        val kw = currentSearchKeyword
        if (kw.isBlank() || _isLoadingMore.value || !_hasMoreSearch.value) return
        _isLoadingMore.value = true
        viewModelScope.launch {
            val next = currentSearchPage + 1
            // 15 秒超时兜底：无论上游多慢都会走到下面把「加载中」解锁，绝不卡住
            val raw = runCatching {
                kotlinx.coroutines.withTimeoutOrNull(15_000) { apiService.searchPage(kw, next) }
            }.getOrNull().orEmpty().filter { isLikelyMusic(it) }
            // 搜索词拆 token：去掉空白与标点（中英文都要处理）
            val tokens = kw.lowercase()
                .split(Regex("[\\s\\p{Punct}・、，。！？；：…—～「」『』（）()【】]+"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
            // 相关性过滤：歌名 + 歌手（先按分隔符拆词再拼起来比，避免"跨词拼出的假命中"影响判断）
            val relevant = if (tokens.isEmpty()) raw else raw.filter { s ->
                val words = (s.name + " " + s.artist).lowercase()
                    .split(Regex("[/、&,，;；|\\s]+"))
                    .filter { it.isNotBlank() }
                tokens.any { t -> words.any { w -> w.contains(t) } }
            }
            val existing = _searchResults.value
            val keys = existing.map { it.id + "|" + it.source }.toMutableSet()
            val added = relevant.filter { keys.add(it.id + "|" + it.source) }
            if (raw.isEmpty()) {
                // 后端这一页是空的 → 真的没有更多了
                _hasMoreSearch.value = false
            } else {
                if (added.isNotEmpty()) {
                    // 展示层是确定性排序（见 sortedSearchResults），新一页放在**前面**，
                    // 排完后新歌落在列表底部（用户当前视线下方，继续下滑即可看到）。
                    _searchResults.value = added + existing
                    emptyPageStreak = 0
                } else {
                    // 连续两页都没拿到新歌 → 判定到底，停止翻页。
                    // 否则 hasMore 一直为真会立刻再次触发，界面就永远停在「加载中」。
                    emptyPageStreak++
                    if (emptyPageStreak >= 2) _hasMoreSearch.value = false
                }
                currentSearchPage = next
                if (currentSearchPage >= 30) _hasMoreSearch.value = false
            }
            _isLoadingMore.value = false
        }
    }

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean> = _isLooping
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory
    private val _currentLyrics = MutableStateFlow<LyricData>(LyricData())
    val currentLyrics: StateFlow<LyricData> = _currentLyrics
    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex
    private val _favorites = MutableStateFlow<List<Song>>(emptyList())
    val favorites: StateFlow<List<Song>> = _favorites
    private val _announcement = MutableStateFlow<AnnouncementData?>(null)
    val announcement: StateFlow<AnnouncementData?> = _announcement
    private val _updateInfo = MutableStateFlow<UpdateData?>(null)
    val updateInfo: StateFlow<UpdateData?> = _updateInfo
    private val _historyList = MutableStateFlow<List<Song>>(emptyList())
    val historyList: StateFlow<List<Song>> = _historyList

    /** 服务端维护状态；null = 未知/正常（绝不因为维护检查本身挡住使用）。 */
    private val _maintenance = MutableStateFlow<MaintenanceInfo?>(null)
    val maintenance: StateFlow<MaintenanceInfo?> = _maintenance

    /** 本次会话内已确认的维护提示签名（level 1 横幅 / level 2 弹窗可关；level 3 永远不可关）。 */
    private val _maintenanceAcked = MutableStateFlow("")
    val maintenanceAcked: StateFlow<String> = _maintenanceAcked

    /**
     * 检查服务端维护状态：GET {API_BASE_URL}/api/v1/system/maintenance（普通 GET，不需要登录）。
     *
     * 返回 { code, msg, data:{ level, title, message, until, allowPlayback } }。
     * **任何失败都按「正常」处理**（state 置 null）——维护检查本身绝不能挡住用户使用。
     */
    fun checkMaintenance() {
        viewModelScope.launch(Dispatchers.IO) {
            // ⚠️ 这个接口挂在 go-music-api 上（带 /music-api 前缀），根路径会 404 ——
            // 实测：/api/v1/system/maintenance → 404，/music-api/api/v1/system/maintenance → 200。
            // （公告/版本接口反而只挂在根路径上，两者不是同一个服务，不能混用同一个前缀。）
            val candidates = listOf(
                MusicApiClient.baseUrl.trimEnd('/') + "/api/v1/system/maintenance",
                API_BASE_URL.trimEnd('/') + "/music-api/api/v1/system/maintenance",
            ).distinct()
            for (url in candidates) {
                try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.setRequestProperty("Accept", "application/json")
                    conn.setRequestProperty("User-Agent", "MINova-Android")
                    val code = conn.responseCode
                    val text = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    if (code !in 200..299) continue
                    val root = JSONObject(text)
                    val o = root.optJSONObject("data") ?: root
                    val level = o.optInt("level", 0).coerceIn(0, 3)
                    android.util.Log.d(
                        "MINova-MAINT",
                        "url=" + url + " level=" + level + " title=" + o.optString("title") +
                            " until=" + o.optString("until") +
                            " allowPlayback=" + o.optBoolean("allowPlayback", false)
                    )
                    _maintenance.value = MaintenanceInfo(
                        level = level,
                        title = o.optString("title"),
                        message = o.optString("message"),
                        until = o.optString("until"),
                        allowPlayback = o.optBoolean("allowPlayback", false),
                    )
                    return@launch
                } catch (e: Exception) {
                    android.util.Log.d("MINova-MAINT", "try failed url=" + url + " : " + e.message)
                }
            }
            // 全部失败：按「正常」处理，绝不因为维护检查挡住用户
            android.util.Log.d("MINova-MAINT", "all endpoints failed, treat as normal")
            _maintenance.value = null
        }
    }

    /** 用户关掉了本次维护提示（level 1 横幅 / level 2 弹窗）：本次会话内不再显示。 */
    fun ackMaintenance(info: MaintenanceInfo) {
        _maintenanceAcked.value = info.signature()
    }

    /** 本地真实播放次数：songId -> 次数。全网无公开的单曲播放量接口（已实测），
     *  因此这里统计用户在本机的实际播放次数，作为真实、可解释的「播放量」展示。 */
    private val _playCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playCounts: StateFlow<Map<String, Int>> = _playCounts

    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs: StateFlow<List<Song>> = _downloadedSongs
    private val downloadDir: File = run {
        val ext = application.getExternalFilesDir(null) ?: application.filesDir
        val newDir = File(ext, "downloads").apply { if (!exists()) mkdirs() }
        runCatching {
            val legacy = File(application.filesDir, "downloads")
            if (legacy.exists() && legacy.absolutePath != newDir.absolutePath) {
                legacy.listFiles()?.forEach { f ->
                    val dest = File(newDir, f.name)
                    if (!dest.exists()) runCatching { f.copyTo(dest, overwrite = false) }
                }
                if (newDir.listFiles()?.isNotEmpty() == true) legacy.listFiles()?.forEach { it.delete() }
            }
        }
        newDir
    }
    private val downloadInfoFile = File(application.filesDir, "downloahds_info.json")

    var bannerImageUri by mutableStateOf<Any?>(R.drawable.default_banner)
    var backgroundImageUri by mutableStateOf<Any?>(R.drawable.default_wallpaper)
    var customBackgroundUri by mutableStateOf<Uri?>(null)
    var hasLoadedLocalAssets = false
    var bannerBiasX by mutableFloatStateOf(prefs.getFloat("bannerBiasX", 0f))
    var bannerBiasY by mutableFloatStateOf(prefs.getFloat("bannerBiasY", 0f))
    var backgroundBiasX by mutableFloatStateOf(prefs.getFloat("backgroundBiasX", 0f))
    var backgroundBiasY by mutableFloatStateOf(prefs.getFloat("backgroundBiasY", 0f))
    var isCustomBackgroundEnabled by mutableStateOf(prefs.getBoolean("isCustomBackgroundEnabled", true))
    var isBlurEnabled by mutableStateOf(prefs.getBoolean("isBlurEnabled", false))
    var globalBlurRadius by mutableFloatStateOf(prefs.getFloat("globalBlurRadius", 25f))
    var blurMode by mutableIntStateOf(prefs.getInt("blurMode", 0))
    var themeIndex by mutableIntStateOf(prefs.getInt("themeIndex", 1))
    var cornerRadius by mutableFloatStateOf(prefs.getFloat("cornerRadius", 24f))
    var isConsoleExpanded by mutableStateOf(false)
    var isConsoleFullScreen by mutableStateOf(false)

    var isBlurSettingsExpanded by mutableStateOf(prefs.getBoolean("isBlurSettingsExpanded", true))
    var isCornerExpanded by mutableStateOf(prefs.getBoolean("isCornerExpanded", true))
    var isBannerExpanded by mutableStateOf(prefs.getBoolean("isBannerExpanded", true))
    var isBackgroundExpanded by mutableStateOf(prefs.getBoolean("isBackgroundExpanded", true))
    var isPaletteExpanded by mutableStateOf(prefs.getBoolean("isPaletteExpanded", true))

                var isForceDarkMode: Boolean
        get() = ColorMode.fromValue(colorMode).isDark
        set(value) { colorMode = if (value) 2 else 0 }

    var useFloatingNavBar by mutableStateOf(prefs.getBoolean("useFloatingNavBar", true))
            var md3Style by mutableStateOf(prefs.getBoolean("md3_style", false))
        var whiteTextMode by mutableStateOf(prefs.getBoolean("white_text", false))
        var colorSpec by mutableStateOf(prefs.getString("color_spec", "SPEC_2025") ?: "SPEC_2025")
            var colorMode by mutableIntStateOf(
        prefs.getInt("color_mode", if (prefs.getBoolean("isForceDarkMode", false)) 2 else 0)
    )
    var keyColor by mutableIntStateOf(prefs.getInt("key_color", 0))
    var colorStyle by mutableStateOf(prefs.getString("color_style", "TonalSpot") ?: "TonalSpot")
        var navBarTransparency by mutableIntStateOf(prefs.getInt("nav_bar_transparency", 0))
    var navBarLiquidGlass by mutableStateOf(prefs.getBoolean("navBarLiquidGlass", true))
    var navBarShowLabels by mutableStateOf(prefs.getBoolean("navBarShowLabels", true))
    var navBarIconSize by mutableFloatStateOf(prefs.getFloat("navBarIconSize", 24f))
    var navBarBlurAmount by mutableFloatStateOf(prefs.getFloat("navBarBlurAmount", 8f))
    var navBarLensEnabled by mutableStateOf(prefs.getBoolean("navBarLensEnabled", true))
    var navBarOffsetX by mutableFloatStateOf(prefs.getFloat("navBarOffsetX", 0f))
    var navBarOffsetY by mutableFloatStateOf(prefs.getFloat("navBarOffsetY", -20f))
    var navBarCornerRadius by mutableFloatStateOf(prefs.getFloat("navBarCornerRadius", 32f))
    var navBarIndicatorCornerRadius by mutableFloatStateOf(prefs.getFloat("navBarIndicatorCornerRadius", 28f))
    var navBarWidthFactor by mutableFloatStateOf(prefs.getFloat("navBarWidthFactor", 0.9f))
    var hasShownShakeGuide by mutableStateOf(false)
    var isConsoleMinimized by mutableStateOf(false)

    var lastLoginReminderTime by mutableLongStateOf(prefs.getLong("lastLoginReminderTime", 0L))
    var showAnnouncement by mutableStateOf(false)
    var showUpdate by mutableStateOf(false)

    data class UserInfo(val id: Long, val username: String, val nickname: String, val avatar: String = "", val quota: Int = 50, val resetTime: Long = 0)
    data class AnnouncementData(val title: String, val content: String, val time: String)
    data class UpdateData(val version: String, val versionCode: Int, val updateUrl: String, val updateLog: String, val forceUpdate: Boolean)

    /**
     * 服务端维护状态（GET {base}/api/v1/system/maintenance）。
     * level：0 正常 / 1 普通维护 / 2 重要维护 / 3 紧急停机。
     */
    data class MaintenanceInfo(
        val level: Int = 0,
        val title: String = "",
        val message: String = "",
        val until: String = "",
        val allowPlayback: Boolean = false,
        /** 展示样式，服务端可自定义：banner=顶部横幅 / fullscreen=整屏维护页 / dialog=居中弹窗；留空则按 level 自动选。 */
        val style: String = "",
    ) {
        /** 会话内「已确认」的签名：level 或文案变化后重新提示。 */
        fun signature(): String = level.toString() + "|" + title + "|" + message

        /** 接口文案为空时的默认标题。 */
        fun displayTitle(): String = title.ifBlank {
            when {
                level >= 3 -> "系统维护中，暂时无法使用"
                level == 2 -> "系统维护中，建议稍后再使用"
                else -> "系统维护中，部分功能可能不稳定"
            }
        }

        /** 接口文案为空时的默认正文。 */
        fun displayMessage(): String = message.ifBlank {
            when {
                level >= 3 -> "服务器正在紧急停机维护，服务暂时不可用，请稍后再试。"
                level == 2 -> "重要维护进行中，部分功能可能不可用，建议稍后再使用。"
                else -> "部分功能可能不稳定，正在处理中，感谢理解。"
            }
        }
    }

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser
    var authToken by mutableStateOf("")
        internal set
    var avatarUri by mutableStateOf<Uri?>(null)
        private set

    private var progressJob: Job? = null
    private var playJob: Job? = null

    private val adManager = AdRewardManager()
    private var lastAdWatchTime = 0L

    private val _remainingQuota = MutableStateFlow(50)
    val remainingQuota: StateFlow<Int> = _remainingQuota
    private val _nextResetTime = MutableStateFlow(0L)
    val nextResetTime: StateFlow<Long> = _nextResetTime

    companion object {
        private const val ENC_API = "aHR0cHM6Ly84LjE0OC44NC4yOjY0NzI="
    val API_BASE_URL: String get() = String(android.util.Base64.decode(ENC_API, android.util.Base64.DEFAULT))
        const val CLIENT_VERSION_CODE = 13

        /** 直链解析：同源同 id 快路径超时（ms）。一次请求，通常 <1s。 */
        private const val PLAY_FAST_TIMEOUT_MS = 4_000L

        /** 直链解析：跨源严格搜索的单源超时（ms）。超时视为没找到，继续下一个源/下一首，绝不卡住切歌。 */
        private const val PLAY_STRICT_TIMEOUT_MS = 4_000L

        /**
         * 跨源严格搜索的**总预算**（ms）。
         * 逐源 4s × 6 个源 = 24s 会串成"点了歌要等十几秒"，所以整段搜索必须再有总闸门：
         * 超了就按"没找到"处理，起播流程绝不为找歌无限等。
         */
        private const val PLAY_STRICT_TOTAL_BUDGET_MS = 6_000L

        /** 试听片段最多重试次数：超过就跳过该歌，避免「同一个源反复 prepare→再试听」的静默死循环。 */
        private const val PREVIEW_RETRY_LIMIT = 3

        /** 下一首预解析直链的保鲜期（ms）。 */
        private const val PREFETCH_TTL_MS = 5 * 60_000L
    }

    private fun sha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun saveTokenToFile(token: String) { try { tokenFile.writeText(token) } catch (_: Exception) {} }
    private fun loadTokenFromFile(): String { try { if (tokenFile.exists()) return tokenFile.readText() } catch (_: Exception) {}; return "" }
    private fun deleteTokenFile() { try { if (tokenFile.exists()) tokenFile.delete() } catch (_: Exception) {} }

    private fun saveUserInfoToFile(userInfo: UserInfo) {
        try { val json = JSONObject().apply { put("id", userInfo.id); put("username", userInfo.username); put("nickname", userInfo.nickname); put("avatar", userInfo.avatar); put("quota", userInfo.quota); put("resetTime", userInfo.resetTime) }; userFile.writeText(json.toString()) } catch (_: Exception) {}
    }

    private fun loadUserInfoFromFile(): UserInfo? {
        try { if (userFile.exists()) { val json = JSONObject(userFile.readText()); return UserInfo(json.getLong("id"), json.getString("username"), json.getString("nickname"), json.optString("avatar", ""), json.optInt("quota", 50), json.optLong("resetTime", 0)) } } catch (_: Exception) {}
        return null
    }

    private fun deleteUserInfoFile() { try { if (userFile.exists()) userFile.delete() } catch (_: Exception) {} }

    private fun saveHistoryToFile() { try { val gson = Gson(); historyFile.writeText(gson.toJson(_searchHistory.value)) } catch (_: Exception) {} }
    private fun loadHistoryFromFile(): List<String> { try { if (historyFile.exists()) { val gson = Gson(); val array = gson.fromJson(historyFile.readText(), Array<String>::class.java); return array.toList() } } catch (_: Exception) {}; return emptyList() }

    fun addToHistory(keyword: String) { if (keyword.isBlank()) return; val list = _searchHistory.value.toMutableList(); list.remove(keyword); list.add(0, keyword); if (list.size > 15) list.removeAt(list.size - 1); _searchHistory.value = list; saveHistoryToFile() }
    fun clearHistory() { _searchHistory.value = emptyList(); saveHistoryToFile() }

    fun searchOnline(keyword: String) { if (keyword.isBlank()) return; addToHistory(keyword); currentSearchKeyword = keyword; currentSearchPage = 1; emptyPageStreak = 0; _hasMoreSearch.value = true; _isLoadingMore.value = false; _isSearching.value = true; viewModelScope.launch { apiService.search(keyword).onSuccess { _searchResults.value = it; _hasMoreSearch.value = it.isNotEmpty() }.onFailure { _toastMessage.value = "网络异常" }; _isSearching.value = false } }

    private fun loadDownloadedSongs() {
        try {
            if (downloadInfoFile.exists()) {
                val json = downloadInfoFile.readText()
                val array = org.json.JSONArray(json)
                val list = mutableListOf<Song>()
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    list.add(Song(id = item.getString("id"), name = item.getString("name"), artist = item.getString("artist"), album = item.optString("album", ""), duration = item.optLong("duration", 0), picUrl = item.optString("picUrl", "")))
                }
                _downloadedSongs.value = list
            }
        } catch (_: Exception) {}
    }

    private fun loadPlayCounts() {
        try {
            if (playCountsFile.exists()) {
                val obj = JSONObject(playCountsFile.readText())
                val map = mutableMapOf<String, Int>()
                obj.keys().forEach { k -> map[k] = obj.optInt(k, 0) }
                _playCounts.value = map
            }
        } catch (_: Exception) {}
    }

    private fun savePlayCounts() {
        try {
            val obj = JSONObject()
            _playCounts.value.forEach { (k, v) -> obj.put(k, v) }
            playCountsFile.writeText(obj.toString())
        } catch (_: Exception) {}
    }

    private fun bumpPlayCount(song: Song) {
        if (song.id.isBlank()) return
        val cur = _playCounts.value
        _playCounts.value = cur + (song.id to ((cur[song.id] ?: 0) + 1))
        savePlayCounts()
    }

    private fun saveDownloadedSongs() {
        try {
            val array = org.json.JSONArray()
            _downloadedSongs.value.forEach { song ->
                val item = org.json.JSONObject()
                item.put("id", song.id); item.put("name", song.name); item.put("artist", song.artist)
                item.put("album", song.album); item.put("duration", song.duration); item.put("picUrl", song.picUrl)
                array.put(item)
            }
            downloadInfoFile.writeText(array.toString())
        } catch (_: Exception) {}
    }

    fun downloadSong(song: Song, onProgress: (Float) -> Unit = {}, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = run {
                    MusicApiClient.resolveUnblockUrl(song.id)?.let { if (it.startsWith("http")) return@run it }
                    val keys = listOf("kuwo", "migu", "bilibili", "netease", "qq")
                    for (k in keys) {
                        runCatching { MusicApiClient.resolveSongUrlByName(song.id, song.name, song.artist, k) }
                            .getOrNull()?.let { if (it.startsWith("http")) return@run it }
                    }
                    // 关键：这个兜底是网易云的 outer 接口，只能拿网易云自己的 id 用。
                    // 其他平台（B站/酷我/酷狗…）的 id 丢进去，网易云会返回"同 id 的另一首歌"，
                    // 于是歌名/封面/歌词都对、声音却完全不是那首歌。
                    if (song.source.equals("netease", ignoreCase = true)) {
                        apiService.getFallbackUrl(song.id)
                    } else {
                        throw IllegalStateException("NO_FREE_SOURCE")
                    }
                }
                val musicFile = File(downloadDir, "${song.id}.mp3")
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000; connection.readTimeout = 15000; connection.connect()
                val fileLength = connection.contentLength
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(musicFile)
                val buffer = ByteArray(8192)
                var totalRead = 0L; var read: Int
                var lastReported = -2f
                var lastReportAt = 0L
                while (inputStream.read(buffer).also { read = it } != -1) {
                    totalRead += read; outputStream.write(buffer, 0, read)
                    // 进度回调：
                    //  有 Content-Length → 按比例给 0..1
                    //  没有（分块传输，很多音乐 CDN 都这样）→ 给 -1，UI 显示"不确定进度"的转圈
                    // 之前只在 fileLength > 0 时回调，这类响应整个下载过程进度条纹丝不动，
                    // 直到下完才突然弹"下载完成"。
                    val now = System.currentTimeMillis()
                    if (now - lastReportAt >= 120) {
                        lastReportAt = now
                        val p = if (fileLength > 0) (totalRead.toFloat() / fileLength).coerceIn(0f, 1f) else -1f
                        if (p < 0f || p - lastReported >= 0.01f || p >= 1f) {
                            lastReported = p
                            // 不要 withContext 回主线程：那会让每个分块都同步等一次 UI 线程，
                            // 一次下载上千次往返，把速度拖垮。这里异步 post 即可。
                            viewModelScope.launch(Dispatchers.Main) { onProgress(p) }
                        }
                    }
                }
                outputStream.close(); inputStream.close(); connection.disconnect()
                val updatedList = _downloadedSongs.value.toMutableList()
                if (!updatedList.any { it.id == song.id }) { updatedList.add(0, song); _downloadedSongs.value = updatedList; saveDownloadedSongs() }
                withContext(Dispatchers.Main) { _toastMessage.value = "下载完成：${song.name}"; onComplete(true) }
            } catch (e: Exception) { withContext(Dispatchers.Main) { _toastMessage.value = "下载失败"; onComplete(false) } }
        }
    }

    fun deleteDownloadedSong(songId: String) {
        val musicFile = File(downloadDir, "$songId.mp3")
        if (musicFile.exists()) musicFile.delete()
        _downloadedSongs.value = _downloadedSongs.value.filter { it.id != songId }
        saveDownloadedSongs()
    }

    fun isSongDownloaded(songId: String): Boolean = _downloadedSongs.value.any { it.id == songId }

    fun playLocalSong(song: Song) {
        val musicFile = File(downloadDir, "${song.id}.mp3")
        if (!musicFile.exists()) { _toastMessage.value = "本地文件不存在"; return }
        playJob?.cancel(); playJob = viewModelScope.launch {
            _isPlaying.value = false; _currentSong.value = song; _currentPosition.value = 0
            _duration.value = song.duration; _currentLyrics.value = LyricData(); _currentLyricIndex.value = -1; releasePlayer()
            addPlayHistory(song)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                setOnPreparedListener { mp ->
                    applyPendingSeek(mp); mp.start(); _isPlaying.value = true; _duration.value = mp.duration.toLong(); startProgressTracking(); updateMediaSession(true) }
                setOnCompletionListener { if (!_isLooping.value) playNext() else { seekTo(0); it.start(); _isPlaying.value = true } }
                setOnErrorListener { _, _, _ -> _toastMessage.value = "播放失败"; _isPlaying.value = false; true }
                try { setDataSource(musicFile.absolutePath); prepareAsync() } catch (_: Exception) { _toastMessage.value = "无法播放本地文件" }
            }
            loadLyricsFor(song)?.let { _currentLyrics.value = it }
        }
    }

    fun clearAllDownloads() {
        if (downloadDir.exists()) downloadDir.listFiles()?.forEach { it.delete() }
        _downloadedSongs.value = emptyList(); saveDownloadedSongs()
    }

    fun getDownloadedFileSize(): Long {
        var totalSize = 0L
        if (downloadDir.exists()) downloadDir.listFiles()?.forEach { totalSize += it.length() }
        return totalSize
    }

    fun getCacheSize(): Long {
        var totalSize = 0L
        val cacheDir = getApplication<Application>().cacheDir
        cacheDir.listFiles()?.forEach { totalSize += it.length() }
        return totalSize
    }

    fun clearCache() {
        val cacheDir = getApplication<Application>().cacheDir
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun clearAllData() {
        clearAllDownloads(); clearCache(); clearHistory()
        _searchHistory.value = emptyList(); saveHistoryToFile()
    }

    fun playExternalSong(song: Song, sourceKey: String, queue: List<Song>? = null) {
        playStartedAt = System.currentTimeMillis()
        // 歌单里的歌也要记住队列，否则这首放完不知道该接哪一首
        // 未显式带队列时自动收编：点的是搜索结果里的歌 → 沿用搜索结果（按展示顺序）做队列，
        // 否则「下一首」会落进推荐页各分区混在一起的兜底池，表现为乱跳。
        var effectiveQueue = queue
        if ((effectiveQueue == null || effectiveQueue.isEmpty()) &&
            _searchResults.value.any { it.id == song.id }
        ) {
            effectiveQueue = _searchResults.value.asReversed()
        }
        if (!effectiveQueue.isNullOrEmpty() && _playQueue.value != effectiveQueue) {
            _playQueue.value = effectiveQueue
            prefetchedPlays.clear()
        }
        playJob?.cancel()
        playJob = viewModelScope.launch {
            _isPlaying.value = false
            _currentSong.value = song
            _currentPosition.value = 0
            _duration.value = song.duration * 1000L
            _currentLyrics.value = LyricData()
            _currentLyricIndex.value = -1
            previewRetryCount = 0
            playFailStreak = 0   // 手动点歌 = 重新给满重试额度

            releasePlayer()
            try {
                val url = run {
                    // ① 快路径：同源同 id 直取（一次请求，最快；同平台同 id 必然同一首）
                    val own = song.source.trim().lowercase()
                    if (own.isNotBlank() && song.id.isNotBlank()) {
                        val u0 = kotlinx.coroutines.withTimeoutOrNull(PLAY_FAST_TIMEOUT_MS) {
                            runCatching { MusicApiClient.resolveSongUrl(song.id, own) }.getOrNull()
                        }
                        if (!u0.isNullOrBlank() && u0.startsWith("http")) return@run u0
                    }
                    // ② 免费音源优先：酷我 / 咪咕 / B站 的免费曲库命中率最高，
                    // 放最前面能让能放的歌一秒出声，不用先绕一圈 VIP 源。
                    val keys = listOf("kuwo", "migu", "bilibili", song.source, sourceKey, "netease", "qq")
                        .filter { it.isNotBlank() }.distinct()
                    for (k in keys) {
                        // 单个音源最多等 8 秒：之前每源都可能拖十几秒，
                        // 8 个源串下来就是"点了半天没反应、多点几次也一样"。
                        val u = kotlinx.coroutines.withTimeoutOrNull(PLAY_STRICT_TIMEOUT_MS) {
                            runCatching {
                                MusicApiClient.resolveSongUrlByName(song.id, song.name, song.artist, k, song.duration)
                            }.getOrNull()
                        }
                        if (!u.isNullOrBlank() && u.startsWith("http")) return@run u
                    }
                    // 关键：这个兜底是网易云的 outer 接口，只能拿网易云自己的 id 用。
                    // 其他平台（B站/酷我/酷狗…）的 id 丢进去，网易云会返回"同 id 的另一首歌"，
                    // 于是歌名/封面/歌词都对、声音却完全不是那首歌。
                    if (song.source.equals("netease", ignoreCase = true)) {
                        apiService.getFallbackUrl(song.id)
                    } else {
                        throw IllegalStateException("NO_FREE_SOURCE")
                    }
                }
                // 与 playSong 统一：歌词走 loadLyricsFor（带归属校验，避免串词），
                // 与直链解析并行、不阻塞；回填前用「当前歌 id」守卫，防止慢响应回来串词。
                viewModelScope.launch {
                    loadLyricsFor(song, sourceKey)?.let { ld ->
                        if (_currentSong.value?.id == song.id) _currentLyrics.value = ld
                    }
                }
                updateMediaMetadata(song)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setOnPreparedListener { mp ->
                        applyPendingSeek(mp)
                        android.util.Log.i(
                            "MINova-Play",
                            "ready to play in " + (System.currentTimeMillis() - playStartedAt) + "ms (playlist) song=" + song.name
                        )
                        mp.start()
                        playFailStreak = 0
                        _isPlaying.value = true
                        _duration.value = mp.duration.toLong()
                        startProgressTracking()
                        updateMediaSession(true)
                        // 起播成功：试听计数归零，并预解析「下一首」直链（自动切歌零等待）
                        previewRetryCount = 0
                        prefetchNext(song)
                    }
                    setOnCompletionListener { if (!_isLooping.value) playNext() }
                    setOnErrorListener { player, _, _ ->
                        _isPlaying.value = false
                        playFailStreak++
                        if (playFailStreak >= 3) {
                            // 连续失败多半是"整张歌单都需要会员"，再跳下去没意义
                            _toastMessage.value = "连续多首都无法播放（多数是需要会员的歌曲），已停止自动跳过"
                        } else {
                            _toastMessage.value = "这首暂无免费音源（可能需要会员），已自动跳过"
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(1200)
                                if (_isPlaying.value.not()) playNext()
                            }
                        }
                        true
                    }
                    setDataSource(url)
                    prepareAsync()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 切歌时上个协程被取消属正常流程，不能当播放失败提示
                throw e
            } catch (e: Exception) {
                _toastMessage.value = if (e.message == "NO_FREE_SOURCE")
                    "这首在免费音源里没找到，已跳过"
                else "播放失败：" + (e.message ?: "未知错误")
                _isPlaying.value = false
            }
        }
    }

    /** 本次播放的候选音源顺序（VIP 歌换源用） */
    private var playSources: List<String> = listOf("qq", "bilibili", "netease")
    private var playSourceIndex = 0

    /** 「同源同 id」快路径是否已试过：换源时不要重复打同一个源。 */
    private var ownSourceTried = false

    /**
     * 实测「prepare 快」的 CDN：kuwo / kugou / migu / bilibili 的直链 0.4~1.4 秒就能出声；
     * 而网易云直链（m701.music.126.net）在模拟器上稳定 31~32 秒才 onPrepared。
     * 因此：① 同源同 id 快路径只对这几个源用；② 网易云的歌优先走**跨源严格搜索**（kuwo/kugou 在前），
     * 只有跨源都找不到时才退回它自己的直链。
     */
    private val FAST_CDN_SOURCES = setOf("kuwo", "migu", "bilibili")

    /** 最近一次成功解析实际命中的源（日志用；区别于 song.source） */
    private var lastResolvedSource: String = ""

    /** 预缓冲用的临时播放器：只 prepare 不 start，把 DNS/TLS/CDN 连接与首段缓冲先热起来。 */
    private var warmUpPlayer: MediaPlayer? = null

    /**
     * 「当前这首歌」的试听重试次数。
     *
     * 必须独立于 [playSourceIndex]：之前试听判定条件用的是 playSourceIndex，
     * 而解析走的是 for 循环不推进它 → 同一个源反复 startPrepare → onPrepared → 还是试听，
     * 静默死循环（表现为「点了没反应、一直不出声」）。每首歌起播时归零。
     */
    private var previewRetryCount = 0

    /** 「点歌」时刻（ms）：日志用，证明起播链路没有串行等待（tag = MINova-Play） */
    private var playStartedAt = 0L

    /** 下一首直链预解析缓存：key = id|source。自动切歌直接命中，消除「要等很久」。 */
    private data class PrefetchedPlay(val url: String, val source: String, val at: Long)
    private val prefetchedPlays = java.util.concurrent.ConcurrentHashMap<String, PrefetchedPlay>()
    private var prefetchJob: Job? = null

    private fun prefetchKey(s: Song): String = s.id + "|" + s.source.lowercase()

    /** 取预解析结果：过期（[PREFETCH_TTL_MS]）或非 http 一律作废。 */
    private fun takePrefetched(song: Song): PrefetchedPlay? {
        val p = prefetchedPlays.remove(prefetchKey(song)) ?: return null
        if (System.currentTimeMillis() - p.at > PREFETCH_TTL_MS) return null
        val hit = p.takeIf { it.url.startsWith("http") }
        android.util.Log.i("MINova-Play", "prefetch " + (if (hit != null) "HIT" else "MISS") +
            " key=" + prefetchKey(song) + " src=" + (hit?.source ?: "-"))
        return hit
    }

    /** 当前这轮播放的队列：显式队列 → 页面兜底队列 → 搜索结果。 */
    private fun currentQueue(): List<Song> =
        _playQueue.value.ifEmpty { _fallbackQueue.value }.ifEmpty { _searchResults.value }

    /** 队列里的下一首（取法与 [playNext] 完全一致）。 */
    private fun nextInQueue(cur: Song): Song? {
        val q = currentQueue()
        if (q.size < 2) return null
        val i = q.indexOfFirst { it.id == cur.id }
        return (if (i >= 0) q[(i + 1) % q.size] else q.firstOrNull())?.takeIf { it.id != cur.id }
    }

    /**
     * 预解析**下一首**的直链并缓存：当前歌一出声就发起。
     * 自动切歌时先查缓存 → 直接开播，不用再等网络（「自动切歌要等很久」的根治办法）。
     * 失败/超时静默忽略：这只是优化，自动切歌仍有正常解析兜底。
     */
    private fun prefetchNext(cur: Song) {
        val next = nextInQueue(cur) ?: return
        val key = prefetchKey(next)
        if (prefetchedPlays.containsKey(key)) return
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            val own = next.source.trim().lowercase()
            var src = own
            var url: String? = null
            // ① 只有"快 CDN"才直接用同源同 id；网易云这类慢源留给下面的跨源严格搜索
            if (own in FAST_CDN_SOURCES && next.id.isNotBlank()) {
                url = kotlinx.coroutines.withTimeoutOrNull(PLAY_FAST_TIMEOUT_MS) {
                    runCatching { MusicApiClient.resolveSongUrl(next.id, own) }.getOrNull()
                }
            }
            // ② 跨源严格搜索（kuwo/kugou 在前；歌名归一化相等 + 歌手交集或一方为空 + 时长差 ≤3s）
            if (url.isNullOrBlank()) {
                val rp = kotlinx.coroutines.withTimeoutOrNull(PLAY_STRICT_TOTAL_BUDGET_MS) {
                    runCatching {
                        MusicApiClient.resolveOrigin(next.id, next.name, next.artist, next.source, next.duration)
                    }.getOrNull()
                }
                if (rp != null) { url = rp.url; src = rp.source }
            }
            // ③ 最后手段：慢源自己的直链（跨源确实找不到时才用，避免"整首放不出来"）
            if (url.isNullOrBlank() && own.isNotBlank() && next.id.isNotBlank()) {
                url = kotlinx.coroutines.withTimeoutOrNull(PLAY_FAST_TIMEOUT_MS) {
                    runCatching { MusicApiClient.resolveSongUrl(next.id, own) }.getOrNull()
                }
                if (!url.isNullOrBlank()) src = own
            }
            val u = url
            if (!u.isNullOrBlank() && u.startsWith("http")) {
                if (prefetchedPlays.size > 6) prefetchedPlays.clear()
                prefetchedPlays[key] = PrefetchedPlay(u, src, System.currentTimeMillis())
                android.util.Log.i(
                    "MINova-Play",
                    "prefetched next src=" + src + " fastCdn=" + (src in FAST_CDN_SOURCES) + " song=" + next.name
                )
                // 预缓冲：只 prepare 不 start，把这条 URL 的连接 / 首段数据先热好（慢源不预热，省流量）
                if (src in FAST_CDN_SOURCES) warmUpUrl(next, u)
            }
        }
    }

    /**
     * 预缓冲下一首（不只是预解析 URL）：起一个**临时** MediaPlayer 只做 prepareAsync，
     * 不 start、不出声，让 CDN 连接 / DNS / TLS / 首段数据先落到本地缓冲；
     * 切歌时真正的播放器再 prepare 就会快很多（CDN 边缘与连接已热）。
     * prepare 完成或出错立刻 release —— 它是独立实例，不参与真正播放器的状态机。
     */
    private fun warmUpUrl(song: Song, url: String) {
        runCatching {
            warmUpPlayer?.let { runCatching { it.release() } }
            warmUpPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setOnPreparedListener { mp ->
                    android.util.Log.i("MINova-Play", "warmed up next (prepared) song=" + song.name)
                    runCatching { mp.release() }
                    if (warmUpPlayer === mp) warmUpPlayer = null
                }
                setOnErrorListener { mp, _, _ ->
                    runCatching { mp.release() }
                    if (warmUpPlayer === mp) warmUpPlayer = null
                    true
                }
                setDataSource(url)
                prepareAsync()
            }
        }
    }

    /**
     * 取下一个音源的播放地址；用尽返回 null。
     * 为什么要换源：netease 对 VIP/版权歌常返回 45 秒试听片段，
     * 必须等 MediaPlayer 报出真实时长后再判断，短了就换源。
     *
     * 顺序按「多快能出声」排，而不是按"哪个 id 最可信"排：
     *  ① **快 CDN** 的「同源同 id」直取（一次请求，通常 <1s）；
     *  ② 跨源严格搜索，源顺序 = [playSources]（kuwo/kugou 在前、网易云在最后），整段有总预算；
     *  ③ 最后手段：慢源（网易云）自己的直链 —— prepare 慢，但总比"放不出来"好。
     * ② 必须推进 [playSourceIndex]：试听换源要靠它继续往后走，否则会一直重试同一个源。
     */
    private suspend fun resolveNextPlayUrl(song: Song): String? {
        val own = song.source.trim().lowercase()
        // ① 只有快 CDN 才值得用同源同 id 直取（网易云直链实测要 31~32s 才出声）
        if (!ownSourceTried && own in FAST_CDN_SOURCES) {
            ownSourceTried = true
            resolveOwnSourceUrl(song, own)?.let { return it }
        }
        // ② 跨源严格搜索：逐源推进索引，带上时长指纹；整段有**总预算**（不会串成几十秒）
        val strict = kotlinx.coroutines.withTimeoutOrNull(PLAY_STRICT_TOTAL_BUDGET_MS) {
            while (playSourceIndex < playSources.size) {
                val k = playSources[playSourceIndex++]
                val u = kotlinx.coroutines.withTimeoutOrNull(PLAY_STRICT_TIMEOUT_MS) {
                    runCatching {
                        MusicApiClient.resolveSongUrlByName(song.id, song.name, song.artist, k, song.duration)
                    }.getOrNull()
                }
                if (!u.isNullOrBlank() && u.startsWith("http")) {
                    lastResolvedSource = k
                    return@withTimeoutOrNull u
                }
            }
            null
        }
        if (!strict.isNullOrBlank()) return strict
        // ③ 最后手段：慢源自己的直链（跨源确实找不到时才用）
        if (!ownSourceTried) {
            ownSourceTried = true
            resolveOwnSourceUrl(song, own)?.let { return it }
        }
        return null
    }

    /**
     * 同源同 id 直取：[MusicApiClient.resolveSongUrl] 一次 HTTP 拿地址，
     * 不按关键词搜索（比 resolveSongUrlByName 快好几倍）。同平台同 id 必然是同一首歌。
     */
    private suspend fun resolveOwnSourceUrl(song: Song, own: String): String? {
        if (own.isBlank() || song.id.isBlank()) return null
        val u = kotlinx.coroutines.withTimeoutOrNull(PLAY_FAST_TIMEOUT_MS) {
            runCatching { MusicApiClient.resolveSongUrl(song.id, own) }.getOrNull()
        }
        if (!u.isNullOrBlank() && u.startsWith("http")) {
            lastResolvedSource = own
            android.util.Log.i("MINova-Play", "resolved via own-source src=" + own + " song=" + song.name)
            return u
        }
        return null
    }

    /** 启动/重启播放器加载指定地址 */
    private fun startPrepare(url: String): Boolean = runCatching {
        mediaPlayer?.reset()
        mediaPlayer?.setDataSource(url)
        mediaPlayer?.prepareAsync()
    }.isSuccess
    /**
     * 按歌曲所属平台取歌词。
     * 关键：后端 id 只在同一平台内唯一 —— 用 kuwo 的 id 去 netease 取歌词会拿到另一首歌
     * （实测 kuwo id 228908 = 晴天，netease id 228908 = 人面桃花）。
     * 所以必须「同源取词」，只有歌曲本身就是 netease 时才回落到旧的网易直连接口。
     * 另外部分源（kuwo/migu）会把罗马音/翻译当主歌词返回（表现为拼音/英文），
     * 所以拿到歌词后做一次归属校验：[ar:] 与歌手对不上、且正文几乎全 ASCII → 判定为别人的词，丢弃。
     */
    private suspend fun loadLyricsFor(song: Song, sourceHint: String = ""): LyricData? {
        // sourceHint：调用方知道这首歌实际来自哪个平台时优先用它（歌单里点的歌 song.source 可能为空）
        val src = sourceHint.ifBlank { song.source }.ifBlank { "netease" }.lowercase()
        val tried = linkedSetOf(src, "kuwo", "migu", "netease")
        for (s in tried) {
            val ld = runCatching {
                kotlinx.coroutines.withTimeoutOrNull(6000) {
                    apiService.fetchLyricsExternal(getApplication(), song.id, s).getOrNull()
                }
            }.getOrNull()
            if (ld != null && ld.lyrics.isNotEmpty() && lyricsBelongTo(ld, song)) return ld
            // 只有 netease 源才回落旧的网易直连接口（id 是 netease 语义）
            if (s == "netease") {
                val legacy = runCatching { apiService.fetchLyrics(song.id) }.getOrNull()?.getOrNull()
                if (legacy != null && legacy.lyrics.isNotEmpty() && lyricsBelongTo(legacy, song)) return legacy
            }
        }
        return null
    }

    /** 歌词归属校验：[ar:] 命中歌手即通过；无标签但正文以 CJK 为主也放行；纯 ASCII 且 [ar:] 不匹配 → 拒绝。 */
    private fun lyricsBelongTo(ld: LyricData, song: Song): Boolean {
        val firstLines = ld.lyrics.entries.sortedBy { it.key }.take(12).map { it.value }.joinToString("\n")
        val arTag = Regex("\\[ar:([^\\]]+)\\]", RegexOption.IGNORE_CASE).find(firstLines)?.groupValues?.get(1)?.trim()
        val body = ld.lyrics.values.joinToString("\n")
        val cjk = body.count { isNativeText(it) }
        val asciiLetters = body.count { it.isLetter() && it.code < 0x80 }
        val asciiDominant = asciiLetters > 0 && asciiLetters > cjk * 3
        fun norm(t: String) = t.lowercase().replace(Regex("\\s|[（）()\\[\\]【】・.]"), "")
        val artistHit = arTag == null || listOf(song.artist, *song.artist.split(",", "/", "&", "、").toTypedArray())
            .any { a -> a.isNotBlank() && (norm(arTag).contains(norm(a)) || norm(a).contains(norm(arTag))) }
        if (artistHit) return true
        // [ar:] 不匹配时：若正文以中文为主，仍可能是没写 [ar:] 的正常词 → 放行
        return !asciiDominant && cjk > 0
    }

    /** 「自动点一下播放」兜底定时器：起播后若迟迟未出声，自动补一次手动播放的那一下 */
    private var autoPlayKickJob: kotlinx.coroutines.Job? = null

    fun playSong(song: Song, queue: List<Song>? = null) {
        playStartedAt = System.currentTimeMillis()
        // 记住这轮播放来自哪个列表，供自动切歌使用。
        // 队列真的换了 → 预解析缓存作废（缓存是按「当前队列的下一首」算出来的）。
        if (!queue.isNullOrEmpty() && _playQueue.value != queue) {
            _playQueue.value = queue
            prefetchedPlays.clear()
        }
        // 去重：连续点同一首歌时不要重建播放器 —— 否则会多次起播/叠音。
        // 播放中 → 直接忽略；暂停中 → 继续播放。
        if (_currentSong.value?.id == song.id) {
            val mp = mediaPlayer
            val playing = mp != null && runCatching { mp.isPlaying }.getOrDefault(false)
            if (playing) return
            if (mp != null) {
                runCatching { mp.start() }
                _isPlaying.value = true
                return
            }
        }
        // 立即反馈：先同步更新 UI，避免等待网络造成“点击无反应”
        _isPlaying.value = false
        _currentSong.value = song
        _currentPosition.value = 0
        _duration.value = song.duration * 1000L
        _currentLyrics.value = LyricData()
        _currentLyricIndex.value = -1
        // 试听重试计数只对「当前这首歌」生效：每首歌起播归零
        previewRetryCount = 0
        playJob?.cancel()
        playJob = viewModelScope.launch {
            releasePlayer()
            // 历史异步写入，不阻塞播放启动
            launch(Dispatchers.IO) { runCatching { addPlayHistory(song) } }
            // ── 自动切歌与手动点播共用这一条加载流程 ──
            // 歌词 + 通知栏元数据与直链解析**并行**发起，互不阻塞：
            // 之前这两件事排在直链解析之后，解析一慢/一失败就永远不执行 →「自动切到下一首后
            // UI 已经换了新歌，却没声音也没歌词，手动点一下播放才补上」。
            // 回填前用「当前歌 id 是否仍是这首歌」守卫，防止慢请求回来后串词。
            launch {
                loadLyricsFor(song)?.let { ld ->
                    if (_currentSong.value?.id == song.id) _currentLyrics.value = ld
                }
            }
            updateMediaMetadata(song)
            val isLoggedInAndHasQuota = _isLoggedIn.value && _remainingQuota.value > 0
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setOnPreparedListener { mp ->
                        applyPendingSeek(mp)
                    val actual = mp.duration.toLong()
                    val expect = song.duration
                    // 试听片段检测：真实时长明显短于歌曲原长（如 VIP 歌 45 秒试听）→ 自动换下一个源
                    val expectMs = if (expect > 0L) expect * 1000L else 0L   // song.duration 是秒，mp.duration 是毫秒
                    // 时长未知（expectMs<=0）时不再凭「短」就判试听：
                    // 不少源返回的 duration 是 0 或明显虚高，误判会触发「换源→重解析→再缓冲」
                    // 的连环重试，表现为自动播放干等几十秒。现在只有「已知原长且明显偏短」
                    // 才判试听；时长未知就直接播 —— 宁可播，不要干等。
                    val isPreview = expectMs > 0L && actual > 0L && actual < expectMs * 6 / 10
                    // 试听片段：用**独立计数**判断还能不能重试（最多 PREVIEW_RETRY_LIMIT 次）。
                    // 之前这里用的是 playSourceIndex，而解析循环不推进它 → 同一个源反复
                    // startPrepare→onPrepared→还是试听，静默死循环（表现为「点了没反应、一直不出声」）。
                    if (actual > 0L && isPreview && previewRetryCount < PREVIEW_RETRY_LIMIT) {
                        previewRetryCount++
                        // 关键：**先立刻把当前这段播出来**。
                        // 以前这里是「先去换源、换不到才播、换源期间不播」→ 表现为
                        // 「等半天没声音，手动点一下播放键才出声」。有声音永远优于干等。
                        runCatching { mp.start() }
                        _isPlaying.value = true
                        _duration.value = actual
                        startProgressTracking()
                        updateMediaSession(true)
                        // 不再后台换源：换源会 reset 播放器，一旦新链失败就变成
                        // 「播到一半弹没音源」。试听段播完后自然 playNext 换下一首。
                        return@setOnPreparedListener
                    }
                    // 重试到上限仍只拿到试听片段 → 跳到下一首并提示，绝不再原地重试
                    if (actual > 0L && isPreview) {
                        // 绝不再 playNext()：用户点的是这首歌，跳到别的歌就是「乱跳」。
                        // 播出来 + 明确提示，把选择权交给用户。
                        runCatching { mp.start() }
                        _isPlaying.value = true
                        _duration.value = actual
                        startProgressTracking()
                        updateMediaSession(true)
                        _toastMessage.value = "该歌曲为会员专享，当前音源仅提供约 ${actual / 1000} 秒试听"
                        prefetchNext(song)
                        return@setOnPreparedListener
                    }
                        // 单位修复后仍可能是试听：四个源都用尽却只拿到短片段，说明服务端 Cookie 对该曲无权限，                         // 明确提示用户，避免误以为「客户端又坏了 / 又只能听 30 秒」。                         if (actual > 0L && isPreview && playSourceIndex >= playSources.size) {                             _toastMessage.value = "该歌曲为会员专享，当前音源仅提供约 ${actual / 1000} 秒试听"                         }
                    android.util.Log.i(
                        "MINova-Play",
                        "ready to play in " + (System.currentTimeMillis() - playStartedAt) + "ms song=" + song.name
                    )
                    mp.start()
                    _isPlaying.value = true
                    _duration.value = actual
                    startProgressTracking()
                    updateMediaSession(true)
                    // 拿到完整版：计数归零，并为「下一首」预解析直链（自动切歌零等待）
                    previewRetryCount = 0
                    playFailStreak = 0
                    prefetchNext(song)
                }
                setOnCompletionListener {
                    if (!_isLooping.value) playNext()
                    else {
                        seekTo(0)
                        it.start()
                        _isPlaying.value = true
                    }
                }
                setOnErrorListener { player, _, _ ->
                    // 播放中流中断（URL 过期/网络抖动）：reset 会清掉播放位置，
                    // 先在 reset 之前记下断点，重解成功后从这里继续播。
                    val resumePos = _currentPosition.value
                    player.reset()
                    // 网易云的 outer 接口只能用网易云自己的 id。
                    // 别的平台（B站/酷我/酷狗…）的 id 丢进去，网易云会返回"同 id 的另一首歌"，
                    // 于是歌名/封面/歌词都对、声音却完全是另一首歌。非网易云一律跳过。
                    if (!song.source.equals("netease", ignoreCase = true) || neteaseFallbackTried) {
                        // 播放到一半的流错误（网络抖动）≠「没找到音源」：
                        // 静默重解同一首歌，拿到新直链就从断点续播；
                        // 失败只暂停并提示 —— 不 playNext、不计入 playFailStreak
                        //（那是「连续多首没音源」的计数；网络问题累积它，
                        //  会让用户重播同一首时被误判成「连续多首找不到可用音源」）。
                        viewModelScope.launch {
                            // 重置源遍历状态：playSourceIndex/ownSourceTried 只在 playSong() 里重置，
                            // 起播时已把索引消耗到末尾，不重置的话播放中重解必然直接返回 null。
                            playSourceIndex = 0
                            ownSourceTried = false
                            val u = runCatching {
                                kotlinx.coroutines.withTimeoutOrNull(8_000) { resolveNextPlayUrl(song) }
                            }.getOrNull()
                            if (!u.isNullOrBlank() && u.startsWith("http")) {
                                runCatching {
                                    // 断点交给挂起 seek：onPrepared 开头 applyPendingSeek(mp)
                                    // 会 seekTo(resumePos) 再 start → 从中断处继续，而不是从 0 重播。
                                    pendingSeekSongId = song.id
                                    pendingSeekMs = resumePos
                                    player.setDataSource(u)
                                    player.prepareAsync()
                                }
                            } else {
                                _toastMessage.value = "网络波动，播放已暂停"
                                _isPlaying.value = false
                            }
                        }
                        return@setOnErrorListener true
                    }
                    neteaseFallbackTried = true
                    try {
                        player.setDataSource(apiService.getFallbackUrl(song.id))
                        player.prepareAsync()
                    } catch (_: Exception) {
                        _toastMessage.value = "资源解析失败"
                        _isPlaying.value = false
                    }
                    true
                }
            }
            // ── 统一解析：只用自建 API 的多源聚合，拿不到再兜底 ──
            // 说明：后端（自建 go-music-api）用保存的账号 Cookie 取完整地址；
            // 旧的直连 music.163.com 路径没有 Cookie，对 VIP 歌只会返回 30 秒试听，
            // 因此这里把自建 API 的四个源放在前面，getFallbackUrl 只作为最后兜底。
            // 另外：某些源对特定歌也只给试听片段，所以真正的判定放在 onPrepared 里按真实时长校验。
            playSourceIndex = 0
            ownSourceTried = false
            // 源顺序 = 实测速度顺序：kuwo/kugou 先（prepare 亚秒级），网易云最后（31~32s）
            playSources = listOf("kuwo", "migu", "qianqian", "bilibili", "qq", "netease")
            // 优先用「上一首播放时预解析的下一首直链」：自动切歌零等待
            val cached = takePrefetched(song)
            if (cached != null) {
                // 缓存已经覆盖了这个源，换源时跳过它，避免拿同一个试听片段反复重试
                playSources = playSources.filter { it != cached.source.lowercase() }
                ownSourceTried = true
            }
            // 同上：非网易云绝不用网易云 outer 兜底，宁可判为"没找到"
            val firstUrl = cached?.url
                ?: resolveNextPlayUrl(song)
                ?: if (song.source.equals("netease", ignoreCase = true)) apiService.getFallbackUrl(song.id) else ""
            // 起播链路计时：cached=true 表示用的是上一首播放时预解析好的直链（0 网络等待）
            android.util.Log.i(
                "MINova-Play",
                "url ready in " + (System.currentTimeMillis() - playStartedAt) + "ms cached=" + (cached != null) +
                    " used=" + (if (cached != null) cached.source else lastResolvedSource) +
                    " origin=" + song.source + " song=" + song.name
            )
            val ok = firstUrl.isNotBlank() && startPrepare(firstUrl)
            if (firstUrl.isBlank()) {
                // 用户方案：这首找不到就继续切下一首（连续 3 首失败才停，避免狂跳）
                playFailStreak++
                if (playFailStreak < 3) {
                    _toastMessage.value = "这首在免费音源里没找到，已跳过"
                    playNext()
                } else {
                    _toastMessage.value = "连续多首找不到可用音源，已停止自动跳过"
                    _isPlaying.value = false
                }
                return@launch
            }
            if (!ok) {
                _toastMessage.value = "播放失败，该歌曲可能受版权限制"
                _isPlaying.value = false
            } else {
                // ── 邪修兜底：自动触发一遍播放 ──
                // 手动点播放「几秒就出声」，是因为它对已就绪的播放器执行了一次 mp.start()；
                // 自动路径偶发停在「已 prepared 但没真正出声」的状态。5 秒后检查一次：
                // 还没在播就自动补上手动播放的那一下（只补一次；歌已切走就不补）。
                autoPlayKickJob?.cancel()
                autoPlayKickJob = viewModelScope.launch {
                    for (attempt in 1..5) {
                        kotlinx.coroutines.delay(3_000L)
                        if (_currentSong.value?.id != song.id) return@launch
                        val mp0 = mediaPlayer ?: return@launch
                        val playing = runCatching { mp0.isPlaying }.getOrDefault(false)
                        if (playing) return@launch
                        // 无条件 start()：对已在播放的播放器是无害的 no-op，
                        // 对「已 prepared 却停在暂停态」的播放器，这正是手动点一次播放键的效果。
                        runCatching { mp0.start() }
                        _isPlaying.value = true
                        startProgressTracking()
                        updateMediaSession(true)
                        android.util.Log.i("MINova-Play", "auto-kick: attempt " + attempt + " song=" + song.name)
                    }
                }
                if (_isLoggedIn.value) {
                    if (_remainingQuota.value > 0) useQuota()
                    else _toastMessage.value = "额度已用完，请观看广告获取额度"
                }
            }
            // 歌词与通知元数据已在本协程开头与直链解析并行发起（见上），此处不再串行等待
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            _isPlaying.value = false
            progressJob?.cancel()
            updateMediaSession(false)
        } else if (_currentSong.value != null) {
            mp.start()
            _isPlaying.value = true
            startProgressTracking()
            updateMediaSession(true)
        }
    }

    fun playNext() {
        val cur = _currentSong.value ?: return
        // 队列优先级：① 当前队列里确实有这首歌 → 用它；
        //            ② 这首歌来自搜索结果 → 用搜索结果（按展示顺序）；
        //            ③ 才轮到推荐页的兜底池。
        // 之前第 ③ 步排在前面，导致「搜索结果里点歌、失败后却开始在推荐池里乱放」。
        val q = run {
            val pq = _playQueue.value
            when {
                pq.isNotEmpty() && pq.any { it.id == cur.id } -> pq
                _searchResults.value.any { it.id == cur.id } -> _searchResults.value.asReversed()
                else -> _fallbackQueue.value.ifEmpty { _searchResults.value }
            }
        }
        if (q.isEmpty()) return
        val i = q.indexOfFirst { it.id == cur.id }
        // 找不到就从头放；到末尾回到第一首（列表循环）
        val next = if (i >= 0) q[(i + 1) % q.size] else q.firstOrNull()
        next?.let { playSong(it) }
    }

    fun playPrevious() {
        val cur = _currentSong.value ?: return
        // 队列优先级：① 当前队列里确实有这首歌 → 用它；
        //            ② 这首歌来自搜索结果 → 用搜索结果（按展示顺序）；
        //            ③ 才轮到推荐页的兜底池。
        // 之前第 ③ 步排在前面，导致「搜索结果里点歌、失败后却开始在推荐池里乱放」。
        val q = run {
            val pq = _playQueue.value
            when {
                pq.isNotEmpty() && pq.any { it.id == cur.id } -> pq
                _searchResults.value.any { it.id == cur.id } -> _searchResults.value.asReversed()
                else -> _fallbackQueue.value.ifEmpty { _searchResults.value }
            }
        }
        if (q.isEmpty()) return
        val i = q.indexOfFirst { it.id == cur.id }
        val prev = if (i > 0) q[i - 1] else q.lastOrNull()
        prev?.let { playSong(it) }
    }

    fun toggleLooping() {
        _isLooping.value = !_isLooping.value
        mediaPlayer?.isLooping = _isLooping.value
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _currentPosition.value = position
        updateLyricIndex(position)
        updateMediaSession(true)
    }

    fun pauseTrackingForDrag() {
        progressJob?.cancel()
    }

    fun resumeTrackingAfterDrag(p: Long) {
        seekTo(p)
        if (_isPlaying.value) {
            startProgressTracking()
            updateMediaSession(true)
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition.toLong()
                        updateLyricIndex(it.currentPosition.toLong())
                    }
                }
                delay(250)
            }
        }
    }

    private fun updateLyricIndex(ms: Long) {
        val t = _currentLyrics.value.times
        if (t.isEmpty()) return
        var idx = -1
        for (i in t.indices) {
            if (ms >= t[i]) idx = i else break
        }
        _currentLyricIndex.value = idx
            run {
                val ts = _currentLyrics.value.times
                if (idx in ts.indices) {
                    FloatingLyricsState.currentLine = _currentLyrics.value.lyrics[ts[idx]] ?: ""
                    FloatingLyricsState.currentTranslation = _currentLyrics.value.translations[ts[idx]] ?: ""
                    val pIdx = idx - 1
                    val nIdx = idx + 1
                    FloatingLyricsState.prevLine =
                        if (pIdx >= 0 && pIdx in ts.indices) _currentLyrics.value.lyrics[ts[pIdx]] ?: "" else ""
                    FloatingLyricsState.nextLine =
                        if (nIdx in ts.indices) _currentLyrics.value.lyrics[ts[nIdx]] ?: "" else ""
                }
            }
    }

    private fun releasePlayer() {
        progressJob?.cancel()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            reset()
            release()
        }
        mediaPlayer = null
        if (::mediaSession.isInitialized) {
            mediaSession.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                    .build()
            )
        }
    }

    private fun updateMediaSession(playing: Boolean) {
        if (!::mediaSession.isInitialized) return
        val s = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(s, _currentPosition.value, 1f)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        )
        mediaSession.isActive = playing
        FloatingLyricsState.isPlaying = playing
        // 播放/暂停状态变化时刷新通知按钮
        lastNotifSong?.let { postMediaNotification(it, playing) }
    }

        private var notifChannelReady = false

    private fun ensureNotifChannel(): String {
        val id = "minova_playback"
        if (!notifChannelReady && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val mgr = getApplication<Application>().getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (mgr.getNotificationChannel(id) == null) {
                mgr.createNotificationChannel(
                    android.app.NotificationChannel(id, "音乐播放", android.app.NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
                )
            }
            notifChannelReady = true
        }
        return id
    }

    private fun notifAction(ctx: android.content.Context, code: Int, action: String): android.app.PendingIntent =
        android.app.PendingIntent.getService(
            ctx, code,
            android.content.Intent(ctx, MusicNotificationService::class.java).setAction(action),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

    private fun postMediaNotification(song: Song, playing: Boolean) {
        try {
            val ctx = getApplication<Application>()
            // 复用已有任务：缺少 SINGLE_TOP/CLEAR_TOP 时点通知会新建 Activity 实例（表现为「重进应用」）
            val openIntent = android.content.Intent(ctx, MainActivity::class.java).apply {
                action = android.content.Intent.ACTION_MAIN
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val open = android.app.PendingIntent.getActivity(
                ctx, 0, openIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val b = androidx.core.app.NotificationCompat.Builder(ctx, ensureNotifChannel())
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(song.name)
                .setContentText(song.artist.ifBlank { "MINova" })
                .setSubText(song.album.ifBlank { null })
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_TRANSPORT)
                .setContentIntent(open)
                .setOnlyAlertOnce(true)
                .setOngoing(playing)
                .setShowWhen(false)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            b.addAction(android.R.drawable.ic_media_previous, "上一首", notifAction(ctx, 1, MusicNotificationService.ACTION_PREV))
            b.addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "暂停" else "播放",
                notifAction(ctx, 2, MusicNotificationService.ACTION_TOGGLE)
            )
            b.addAction(android.R.drawable.ic_media_next, "下一首", notifAction(ctx, 3, MusicNotificationService.ACTION_NEXT))
            val mgr = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            mgr.notify(8802, b.build())
        } catch (_: Exception) { }
    }

    /** 最近一次用于通知的歌曲（切歌/暂停刷新通知用） */
    private var lastNotifSong: Song? = null

    private fun updateMediaMetadata(song: Song) {
        bumpPlayCount(song)
        FloatingLyricsState.songTitle = song.name
        FloatingLyricsState.songArtist = song.artist
        lastNotifSong = song
        // 每次切歌都刷新媒体通知（歌名/歌手/切歌按钮）
        postMediaNotification(song, true)
        if (!::mediaSession.isInitialized) return
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.name)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
                .build()
        )
    }

    fun clearToast() { _toastMessage.value = null }
    fun updateAvatar(uri: Uri?) { avatarUri = uri }

    fun checkLoginReminder() { /* 用户要求去掉「未登录」提醒弹窗：已改为不提示 */ }

    fun checkAnnouncement() { viewModelScope.launch(Dispatchers.IO) { try { val conn = URL("${API_BASE_URL}/api/v1/announcements").openConnection() as HttpURLConnection; conn.requestMethod = "GET"; conn.connectTimeout = 5000; conn.readTimeout = 5000; val text = conn.inputStream.bufferedReader().readText(); conn.disconnect(); val response = JSONObject(text); val item = response.optJSONArray("items")?.optJSONObject(0); if (item != null) { val t = item.optString("title"); if (t.isNotBlank()) { val at = item.optString("published_at"); _announcement.value = AnnouncementData(t, item.optString("content"), if (at.length >= 10) at.substring(0, 10) else at); showAnnouncement = true } } } catch (_: Exception) {} } }

    fun checkUpdate() { viewModelScope.launch(Dispatchers.IO) { try { val conn = URL("${API_BASE_URL}/api/v1/version/latest").openConnection() as HttpURLConnection; conn.requestMethod = "GET"; conn.connectTimeout = 5000; conn.readTimeout = 5000; val text = conn.inputStream.bufferedReader().readText(); conn.disconnect(); val response = JSONObject(text); val v = response.optJSONObject("version") ?: response; val serverVersionCode = v.optInt("version_code", 0); val apkVersionCode = try { getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0).versionCode } catch (_: Exception) { 0 }; val currentVersionCode = maxOf(apkVersionCode, CLIENT_VERSION_CODE); if (serverVersionCode > currentVersionCode) { _updateInfo.value = UpdateData(v.optString("version_name"), serverVersionCode, v.optString("download_url"), v.optString("changelog"), v.optBoolean("is_forced", false)); showUpdate = true } } catch (_: Exception) {} } }

    fun updateNickname(nickname: String) { if (!_isLoggedIn.value || _currentUser.value == null) return; viewModelScope.launch(Dispatchers.IO) { try { val json = JSONObject().apply { put("nickname", nickname) }; val conn = URL("${API_BASE_URL}/api/v1/me").openConnection() as HttpURLConnection; conn.requestMethod = "PATCH"; conn.setRequestProperty("Content-Type", "application/json"); conn.setRequestProperty("Authorization", "Bearer $authToken"); conn.doOutput = true; conn.connectTimeout = 10000; conn.readTimeout = 10000; conn.outputStream.use { it.write(json.toString().toByteArray()) }; if (conn.responseCode == 200) { _currentUser.value = _currentUser.value?.copy(nickname = nickname); _currentUser.value?.let { saveUserInfoToFile(it) }; withContext(Dispatchers.Main) { _toastMessage.value = "昵称已更新" } }; conn.disconnect() } catch (_: Exception) { withContext(Dispatchers.Main) { _toastMessage.value = "更新失败" } } } }

    fun login(username: String, password: String, onResult: (Boolean, String) -> Unit) { viewModelScope.launch(Dispatchers.IO) { try { val json = JSONObject().apply { put("username", username); put("password", password) }; val conn = URL("${API_BASE_URL}/api/v1/auth/login").openConnection() as HttpURLConnection; conn.requestMethod = "POST"; conn.setRequestProperty("Content-Type", "application/json"); conn.doOutput = true; conn.connectTimeout = 10000; conn.readTimeout = 10000; conn.outputStream.write(json.toString().toByteArray()); conn.outputStream.flush(); conn.outputStream.close(); val code = conn.responseCode; val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream); val text = stream.bufferedReader().readText(); conn.disconnect(); val response = JSONObject(text); if (code in 200..299) { authToken = response.getString("token"); saveTokenToFile(authToken); val u = response.getJSONObject("user"); val userInfo = UserInfo(u.getLong("id"), u.getString("username"), u.getString("nickname"), u.optString("avatar_url", "").ifBlank { u.optString("avatar", "") }, u.optInt("quota", 50), u.optLong("reset_time", 0) * 1000); _currentUser.value = userInfo; _remainingQuota.value = userInfo.quota; _nextResetTime.value = userInfo.resetTime; _isLoggedIn.value = true; saveUserInfoToFile(userInfo); MusicApiClient.refreshUserId(getApplication()); loadFavorites(); loadHistory(); syncPlaylists(); withContext(Dispatchers.Main) { onResult(true, "登录成功") } } else { val errText = conn.errorStream?.bufferedReader()?.readText() ?: text; val errJson = try { JSONObject(errText) } catch (_: Exception) { null }; val errMsg = (errJson?.optJSONObject("error")?.optString("message") ?: errJson?.optString("message") ?: errJson?.optString("error")).orEmpty().ifBlank { when (code) { 401 -> "用户名或密码错误"; 423, 429 -> "尝试过于频繁，请稍后再试"; else -> "登录失败，请稍后重试" } }; withContext(Dispatchers.Main) { onResult(false, errMsg) } } } catch (e: Exception) { withContext(Dispatchers.Main) { onResult(false, "连接失败，请检查网络后重试") } } } }

    fun register(username: String, password: String, nickname: String, onResult: (Boolean, String) -> Unit) { viewModelScope.launch(Dispatchers.IO) { try { val json = JSONObject().apply { put("username", username); put("password", password); put("nickname", nickname) }; val conn = URL("${API_BASE_URL}/api/v1/auth/register").openConnection() as HttpURLConnection; conn.requestMethod = "POST"; conn.setRequestProperty("Content-Type", "application/json"); conn.doOutput = true; conn.connectTimeout = 10000; conn.readTimeout = 10000; conn.outputStream.write(json.toString().toByteArray()); conn.outputStream.flush(); conn.outputStream.close(); val code = conn.responseCode; val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream); val text = stream.bufferedReader().readText(); conn.disconnect(); val response = JSONObject(text); if (code in 200..299) { authToken = response.getString("token"); saveTokenToFile(authToken); val u = response.getJSONObject("user"); val userInfo = UserInfo(u.getLong("id"), u.getString("username"), u.getString("nickname"), u.optString("avatar_url", "").ifBlank { u.optString("avatar", "") }, u.optInt("quota", 50), u.optLong("reset_time", 0) * 1000); _currentUser.value = userInfo; _remainingQuota.value = userInfo.quota; _nextResetTime.value = userInfo.resetTime; _isLoggedIn.value = true; saveUserInfoToFile(userInfo); MusicApiClient.refreshUserId(getApplication()); syncPlaylists(); withContext(Dispatchers.Main) { onResult(true, "注册成功") } } else { val errText = conn.errorStream?.bufferedReader()?.readText() ?: text; val errJson = try { JSONObject(errText) } catch (_: Exception) { null }; val errMsg = (errJson?.optJSONObject("error")?.optString("message") ?: errJson?.optString("message") ?: errJson?.optString("error")).orEmpty().ifBlank { when (code) { 401 -> "用户名或密码错误"; 409 -> "该用户名已被注册"; 423, 429 -> "尝试过于频繁，请稍后再试"; else -> "注册失败，请稍后重试" } }; withContext(Dispatchers.Main) { onResult(false, errMsg) } } } catch (e: Exception) { withContext(Dispatchers.Main) { onResult(false, "网络错误") } } } }

    fun uploadAvatar(context: Context, uri: Uri) { viewModelScope.launch(Dispatchers.IO) { try { val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch; val boundary = "----FormBoundary${System.currentTimeMillis()}"; val url = URL("${API_BASE_URL}/api/v1/me/avatar"); val conn = url.openConnection() as HttpURLConnection; conn.requestMethod = "POST"; conn.setRequestProperty("Authorization", "Bearer $authToken"); conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary"); conn.doOutput = true; conn.connectTimeout = 15000; conn.readTimeout = 15000; conn.outputStream.use { out -> out.write("--$boundary\r\n".toByteArray()); out.write("Content-Disposition: form-data; name=\"avatar\"; filename=\"avatar.jpg\"\r\n".toByteArray()); out.write("Content-Type: image/jpeg\r\n\r\n".toByteArray()); out.write(bytes); out.write("\r\n--$boundary--\r\n".toByteArray()); out.flush() }; if (conn.responseCode == 200) { val text = conn.inputStream.bufferedReader().readText(); val resp = JSONObject(text); avatarUri = uri; _currentUser.value = _currentUser.value?.copy(avatar = resp.getString("avatar")); _currentUser.value?.let { saveUserInfoToFile(it) }; withContext(Dispatchers.Main) { AppToast.show("上传成功") } }; conn.disconnect() } catch (e: Exception) { withContext(Dispatchers.Main) { AppToast.show("上传失败") } } } }

fun useQuota(): Boolean {
        return true
    }

fun watchAd(activity: android.app.Activity) {
        AppToast.show("当前版本无广告")
    }

    fun checkAndResetQuota() { if (!_isLoggedIn.value) return; viewModelScope.launch(Dispatchers.IO) { try { val conn = URL("${API_BASE_URL}/api/v1/me").openConnection() as HttpURLConnection; conn.requestMethod = "GET"; conn.setRequestProperty("Authorization", "Bearer $authToken"); conn.connectTimeout = 5000; conn.readTimeout = 5000; val responseText = conn.inputStream.bufferedReader().use { it.readText() }; conn.disconnect(); val response = JSONObject(responseText); _remainingQuota.value = response.optInt("quota", 50); val resetTime = response.optLong("reset_time", 0) * 1000; _nextResetTime.value = resetTime; _currentUser.value = _currentUser.value?.copy(quota = _remainingQuota.value, resetTime = resetTime); _currentUser.value?.let { saveUserInfoToFile(it) } } catch (_: Exception) {} } }

    /**
     * 把本地歌单同步到云端，并还原云端独有的歌单。
     *
     * 触发时机：登录 / 注册成功、启动时已登录、以及歌单发生增删改之后。
     * 未登录时直接跳过（歌单是账号级数据）。
     */
    fun syncPlaylists() {
        if (!_isLoggedIn.value || authToken.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val local = PlaylistStore.load(ctx)
                val merged = PlaylistCloudSync.sync(ctx, authToken, local) ?: return@launch
                PlaylistStore.save(ctx, merged)
            } catch (_: Exception) {
                // 同步失败不影响本地使用，静默跳过
            }
        }
    }

    /**
     * 会话已失效（401/404）：清掉本地 token 与用户信息，并提示重新登录。
     *
     * 典型场景：服务端把账号删了或 token 过期，客户端却还保留着登录态 ——
     * 界面显示"已登录"，但收藏/历史/歌单全部默默失败。
     */
    fun onSessionInvalid() {
        if (!_isLoggedIn.value) return
        logout()
        _toastMessage.value = "登录状态已失效，请重新登录"
    }

    /** 启动时主动校验一次会话：token 无效就直接退出登录。 */
    fun checkSession() {
        val token = authToken
        if (token.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conn = URL("${API_BASE_URL}/api/v1/me").openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val code = conn.responseCode
                conn.disconnect()
                if (code == 401 || code == 404) {
                    withContext(Dispatchers.Main) { onSessionInvalid() }
                }
            } catch (_: Exception) {
                // 网络异常不代表会话失效，避免离线时被误登出
            }
        }
    }

    // ── 收藏的本地缓存 ─────────────────────────────────────────────
    /**
     * 把当前收藏落盘。作用：
     *  ① 未登录也能收藏（不再直接弹"请先登录"）；
     *  ② 离线/进程被杀/重启后立刻有内容，不必等服务端；
     *  ③ 登录时可把"服务端没有的本地条目"补传上去（见 [loadFavorites]）。
     */
    private fun saveFavoritesCache() {
        try {
            val arr = org.json.JSONArray()
            _favorites.value.forEach { s ->
                arr.put(
                    JSONObject()
                        .put("id", s.id).put("name", s.name).put("artist", s.artist)
                        .put("album", s.album).put("duration", s.duration).put("picUrl", s.picUrl)
                )
            }
            favoritesFile.writeText(arr.toString())
        } catch (_: Exception) {
        }
    }

    /** 启动时先读本地缓存，保证收藏页不空。 */
    private fun loadFavoritesCache() {
        try {
            if (!favoritesFile.exists()) return
            val arr = org.json.JSONArray(favoritesFile.readText())
            val list = mutableListOf<Song>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id")
                if (id.isBlank()) continue
                list.add(
                    Song(
                        id = id, name = o.optString("name"), artist = o.optString("artist"),
                        album = o.optString("album"), duration = o.optLong("duration"),
                        picUrl = o.optString("picUrl")
                    )
                )
            }
            if (list.isNotEmpty()) _favorites.value = list
        } catch (_: Exception) {
        }
    }

    /** 单条收藏上报服务端；成功返回 true。未登录调用返回 false。 */
    private suspend fun pushFavoriteToServer(song: Song): Boolean {
        if (authToken.isBlank()) return false
        return try {
            val json = JSONObject().apply {
                put("song_id", song.id); put("song_name", song.name)
                put("artist", song.artist); put("cover_url", song.picUrl)
            }
            val conn = URL("${API_BASE_URL}/api/v1/favorites").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $authToken")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.outputStream.use { it.write(json.toString().toByteArray()) }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    /** 从本地缓存里恢复收藏（启动即显示），未登录时这是唯一来源。 */
    fun restoreLocalFavorites() = loadFavoritesCache()

    fun loadFavorites() {
        if (!_isLoggedIn.value) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conn = URL("${API_BASE_URL}/api/v1/favorites").openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $authToken")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val code = conn.responseCode
                val text = if (code in 200..299) conn.inputStream.bufferedReader().readText()
                else conn.errorStream?.bufferedReader()?.readText() ?: ""
                conn.disconnect()
                if (code !in 200..299) {
                    if (code == 401 || code == 404) {
                        // token 失效 / 账号已被服务端删除：清掉登录态
                        withContext(Dispatchers.Main) { onSessionInvalid() }
                    } else {
                        withContext(Dispatchers.Main) { _toastMessage.value = "收藏加载失败，请稍后重试" }
                    }
                    return@launch
                }
                val rawObj = try { JSONObject(text) } catch (_: Exception) { null }
                val array = when {
                    rawObj != null && rawObj.has("items") -> rawObj.getJSONArray("items")
                    text.trimStart().startsWith("[") -> org.json.JSONArray(text)
                    else -> org.json.JSONArray()
                }
                val list = mutableListOf<Song>()
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    list.add(Song(id = item.getString("song_id"), name = item.optString("song_name", item.optString("name", "")), artist = item.optString("artist", ""), album = item.optString("album", ""), duration = item.optLong("duration", 0), picUrl = item.optString("cover_url", item.optString("pic_url", ""))))
                }
                // 服务端收藏是「删除应用重装 / 换机后」的唯一来源（本地文件随卸载消失）。
                // 本地缓存里服务端还没有的条目（离线收藏、未登录期间收藏）先补传，再取并集
                // —— 登录后两边都不会丢，也不会把服务端已有的收藏清掉。
                val serverIds = list.map { it.id }.toHashSet()
                val localOnly = _favorites.value.filter { it.id !in serverIds }
                localOnly.forEach { s -> runCatching { pushFavoriteToServer(s) } }
                _favorites.value = (list + localOnly).distinctBy { it.id }
                saveFavoritesCache()
            } catch (e: Exception) {
                android.util.Log.e("MINova", "loadFavorites error", e)
                withContext(Dispatchers.Main) { _toastMessage.value = "收藏加载失败: ${e.message}" }
            }
        }
    }

    fun addToFavorites(song: Song) {
        if (_favorites.value.any { it.id == song.id }) { _toastMessage.value = "已收藏过此歌曲"; return }
        // 云端优先：先带 token 上报服务端，云端成功后本地列表只作缓存镜像；
        // 失败（未登录 / 断网 / 服务异常）才落本地兜底，登录后 loadFavorites 会把本地新增补传上去。
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching { pushFavoriteToServer(song) }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                if (_favorites.value.none { it.id == song.id }) {
                    _favorites.value = listOf(song) + _favorites.value
                    saveFavoritesCache()
                }
                _toastMessage.value = if (ok) "已收藏" else "已收藏（本地，登录后同步）"
            }
        }
    }

    fun removeFromFavorites(songId: String) {
        // 本地立刻生效（离线/未登录也一致），登录时再同步删除服务端
        _favorites.value = _favorites.value.filter { it.id != songId }
        saveFavoritesCache()
        if (!_isLoggedIn.value || authToken.isBlank()) { _toastMessage.value = "已取消收藏"; return }
        viewModelScope.launch(Dispatchers.IO) {
            val ok = try {
                val encoded = java.net.URLEncoder.encode(songId, "UTF-8")
                val conn = URL("${API_BASE_URL}/api/v1/favorites/$encoded").openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.setRequestProperty("Authorization", "Bearer $authToken")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val code = conn.responseCode
                conn.disconnect()
                code in 200..299
            } catch (_: Exception) {
                false
            }
            withContext(Dispatchers.Main) {
                _toastMessage.value = if (ok) "已取消收藏" else "已取消收藏（云端稍后同步）"
            }
        }
    }

fun loadHistory() {
        if (!_isLoggedIn.value) { _historyList.value = emptyList(); return }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conn = URL("${API_BASE_URL}/api/v1/me/history").openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $authToken")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val code = conn.responseCode
                val body = if (code in 200..299) conn.inputStream.bufferedReader().readText() else ""
                conn.disconnect()
                if (code !in 200..299) return@launch
                val arr = org.json.JSONObject(body).optJSONArray("items") ?: org.json.JSONArray()
                val list = (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    Song(
                        id = o.optString("song_id"),
                        name = o.optString("song_name"),
                        artist = o.optString("artist"),
                        album = "",
                        duration = 0,
                        picUrl = o.optString("cover_url", "")
                    )
                }
                _historyList.value = list
            } catch (_: Exception) {}
        }
    }

fun clearPlayHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conn = URL("${API_BASE_URL}/api/v1/me/history").openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.setRequestProperty("Authorization", "Bearer $authToken")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.doOutput = false
                val ok = conn.responseCode in 200..299
                conn.disconnect()
                if (ok) {
                    _historyList.value = emptyList()
                    withContext(Dispatchers.Main) { _toastMessage.value = "历史记录已清空" }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { _toastMessage.value = "清空失败" }
            }
        }
    }

private fun addPlayHistory(song: Song) {
        if (!_isLoggedIn.value) return
        val updated = _historyList.value.toMutableList()
        updated.removeAll { it.id == song.id }
        updated.add(0, song)
        _historyList.value = updated.take(200)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = org.json.JSONObject().apply {
                    put("song_id", song.id)
                    put("song_name", song.name)
                    put("artist", song.artist)
                    put("cover_url", song.picUrl)
                }
                val conn = URL("${API_BASE_URL}/api/v1/me/history").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $authToken")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.outputStream.use { it.write(json.toString().toByteArray()) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) {}
        }
    }

    fun logout() { authToken = ""; deleteTokenFile(); deleteUserInfoFile(); avatarUri = null; _isLoggedIn.value = false; _currentUser.value = null; _nextResetTime.value = 0L; _favorites.value = emptyList(); _historyList.value = emptyList(); MusicApiClient.refreshUserId(getApplication()) }

    override fun onCleared() { super.onCleared(); releasePlayer(); if (::mediaSession.isInitialized) mediaSession.release() }

    fun savePreferences() {
        prefs.edit()
            .putInt("themeIndex", themeIndex).putFloat("cornerRadius", cornerRadius)
            .putFloat("bannerBiasX", bannerBiasX).putFloat("bannerBiasY", bannerBiasY).putFloat("backgroundBiasX", backgroundBiasX).putFloat("backgroundBiasY", backgroundBiasY)
            .putBoolean("isCustomBackgroundEnabled", isCustomBackgroundEnabled).putBoolean("isBlurEnabled", isBlurEnabled)
            .putFloat("globalBlurRadius", globalBlurRadius).putInt("blurMode", blurMode)
            .putBoolean("isBlurSettingsExpanded", isBlurSettingsExpanded).putBoolean("isCornerExpanded", isCornerExpanded).putBoolean("isBannerExpanded", isBannerExpanded).putBoolean("isBackgroundExpanded", isBackgroundExpanded).putBoolean("isPaletteExpanded", isPaletteExpanded)
            .putBoolean("useFloatingNavBar", useFloatingNavBar).putBoolean("md3_style", md3Style).putBoolean("white_text", whiteTextMode).putString("color_spec", colorSpec).putInt("color_mode", colorMode).putInt("key_color", keyColor).putString("color_style", colorStyle).putInt("nav_bar_transparency", navBarTransparency).putBoolean("navBarLiquidGlass", navBarLiquidGlass).putBoolean("navBarShowLabels", navBarShowLabels).putFloat("navBarIconSize", navBarIconSize)
            .putFloat("navBarBlurAmount", navBarBlurAmount).putBoolean("navBarLensEnabled", navBarLensEnabled).putFloat("navBarOffsetX", navBarOffsetX).putFloat("navBarOffsetY", navBarOffsetY)
            .putFloat("navBarCornerRadius", navBarCornerRadius).putFloat("navBarIndicatorCornerRadius", navBarIndicatorCornerRadius).putFloat("navBarWidthFactor", navBarWidthFactor)
            .putLong("lastLoginReminderTime", lastLoginReminderTime)
            .putBoolean("isForceDarkMode", isForceDarkMode)
            .apply()
    }

    init {
        // ── 液态玻璃能力门槛（< Android 13 产品层面禁用）─────────────────────────
        // RuntimeShader/AGSL 仅 API 33+ 可用：低版本启动时把液态玻璃风格 / 玻璃导航栏
        // 直接回退到兼容组合（Material Design 3 + 经典导航栏），走既有 prefs setter 持久化，
        // 并通过 toastMessage → AppToast 恢复一次应用内提示（每个进程仅此一次）。
        if (!liquidGlassSupported) {
            var fallbackChanged = false
            if (!md3Style) { md3Style = true; fallbackChanged = true }
            if (useFloatingNavBar) { useFloatingNavBar = false; fallbackChanged = true }
            if (navBarLiquidGlass) { navBarLiquidGlass = false; fallbackChanged = true }
            if (fallbackChanged) {
                savePreferences()
                viewModelScope.launch {
                    // 延迟到闪屏/主界面就绪后再提示，避免被启动画面盖掉
                    delay(2500)
                    _toastMessage.value = "当前设备低于 Android 13，已自动切换到兼容风格（Material Design 3）"
                }
            }
        }
        _searchHistory.value = loadHistoryFromFile().toMutableList()
        loadDownloadedSongs()
        loadPlayCounts()
        // 收藏先读本地缓存（未登录/离线/刚启动也有内容）；登录后 loadFavorites() 再与服务端取并集
        restoreLocalFavorites()
        if (!downloadDir.exists()) downloadDir.mkdirs()
        viewModelScope.launch(Dispatchers.IO) { try { MusicApiService.gsonInstance.fromJson("{}", SearchResponse::class.java); MusicApiService.LRC_REGEX.find("[00:00.00]warmup") } catch (_: Exception) {} }
        val pb = PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SEEK_TO)
        
        PlayerControlBridge.onPrev = { playPrevious() }
        PlayerControlBridge.onNext = { playNext() }
        PlayerControlBridge.onToggle = { togglePlayPause() }
        mediaSession = MediaSessionCompat(application, "MusicPlayer"); mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS); mediaSession.setPlaybackState(pb.build())
        mediaSession.setCallback(object : MediaSessionCompat.Callback() { override fun onPlay() { togglePlayPause() }; override fun onPause() { togglePlayPause() }; override fun onSkipToNext() { playNext() }; override fun onSkipToPrevious() { playPrevious() }; override fun onSeekTo(pos: Long) { seekTo(pos) } })
        authToken = loadTokenFromFile()
        if (authToken.isNotBlank()) { val savedUser = loadUserInfoFromFile(); if (savedUser != null) { _currentUser.value = savedUser; _isLoggedIn.value = true; viewModelScope.launch { delay(2000); checkAndResetQuota(); checkSession(); loadFavorites(); loadHistory(); syncPlaylists() } } }
        viewModelScope.launch { delay(3000); checkLoginReminder() }

        // 「记住上次播放」：启动恢复一次，之后每 5 秒存一次进度（开关关闭时内部直接跳过）
        runCatching { restorePlaybackState(application) }
        viewModelScope.launch {
            while (true) {
                delay(5_000)
                runCatching { savePlaybackState() }
            }
        }
    }
}

class InteractiveHighlight(
    val animationScope: kotlinx.coroutines.CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {
    private val pressProgressAnimationSpec = spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val positionAnimation = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)
    private var startPosition = Offset.Zero
    val pressProgress: Float get() = pressProgressAnimation.value
    val offset: Offset get() = positionAnimation.value - startPosition
    val modifier: Modifier = Modifier.drawWithContent { drawContent() }
    val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
        awaitEachGesture {
            val down = awaitFirstDown()
            startPosition = down.position
            animationScope.launch {
                launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                launch { positionAnimation.snapTo(startPosition) }
            }
            waitForUpOrCancellation()
            animationScope.launch {
                launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
            }
        }
    }
}

class DampedDragAnimation(
    private val animationScope: kotlinx.coroutines.CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {
    private val valueAnimationSpec = spring<Float>(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec = spring<Float>(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec = spring<Float>(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring<Float>(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring<Float>(0.7f, 250f, 0.001f)
    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)
    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()
    val value: Float get() = valueAnimation.value
    val progress: Float get() = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        var longPressJob: Job? = null
        var isDragging = false
        awaitEachGesture {
            val down = awaitFirstDown()
            val downPos = down.position
            isDragging = false
            longPressJob = animationScope.launch { delay(200); onDragStarted(downPos); press() }
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (!change.pressed) {
                    longPressJob?.cancel()
                    if (isDragging) { onDragStopped(); release() } else { release() }
                    break
                }
                val currentPos = change.position
                if (!isDragging && (currentPos - downPos).getDistance() > 10f) {
                    isDragging = true
                    longPressJob?.cancel()
                    onDragStarted(downPos)
                    press()
                }
                if (isDragging) {
                    val dragAmount = change.position - change.previousPosition
                    onDrag(size, dragAmount)
                    change.consume()
                }
            }
        }
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    suspend fun awaitFrame() = delay(16)

    fun release() {
        animationScope.launch {
            awaitFrame()
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) { updateVelocity() } }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(Clock.System.now().toEpochMilliseconds(), Offset(value, 0f))
        val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}

internal val LocalLiquidBottomTabScale = staticCompositionLocalOf { { 1f } }

@Composable
fun RowScope.LiquidBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalLiquidBottomTabScale.current
    Column(
        modifier
            .clip(Capsule())
            .clickable(interactionSource = null, indication = null, role = Role.Tab, onClick = onClick)
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer { val s = scale(); scaleX = s; scaleY = s },
        verticalArrangement = Arrangement.spacedBy(2f.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    blurEnabled: Boolean = true,
    blurAmount: Dp = 8.dp,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        MaterialTheme.colorScheme.primary
            val containerColor =
        if (blurEnabled) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surfaceContainer

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex())
        }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(1f, 300f, 0.5f)
                        )
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    currentIndex = index
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, offset ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .layerBackdrop(tabsBackdrop)
        ) {
            Row(
                Modifier
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            if (blurEnabled) blur(blurAmount.toPx())
                            lens(28f.dp.toPx(), 28f.dp.toPx())
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = 0.5f)
                        },
                        shadow = { Shadow(alpha = 0.12f) },
                        layerBlock = {
                            val progress = dampedDragAnimation.pressProgress
                            val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                            scaleX = scale
                            scaleY = scale
                        },
                                                onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(64f.dp)
                    .fillMaxWidth()
                    .padding(4f.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )

            CompositionLocalProvider(
                LocalLiquidBottomTabScale provides {
                    lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                }
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .graphicsLayer {
                            translationX = panelOffset
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                vibrancy()
                                if (blurEnabled) blur(blurAmount.toPx())
                                lens(
                                    28f.dp.toPx() * progress,
                                    28f.dp.toPx() * progress
                                )
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Default.copy(alpha = progress * 0.7f)
                            }
                        )
                        .then(interactiveHighlight.modifier)
                        .height(56f.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 4f.dp)
                        .graphicsLayer { colorFilter = ColorFilter.tint(accentColor) },
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }

        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        vibrancy()
                        lens(
                            refractionHeight = 8f.dp.toPx() + 6f.dp.toPx() * progress,
                            refractionAmount = 14f.dp.toPx() + 10f.dp.toPx() * progress,
                            depthEffect = true,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = 0.3f + progress * 0.4f)
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                                                                                innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(radius = 8.dp * progress, alpha = progress)
                    },
                                                            onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            color = if (isLightTheme) Color.Black.copy(alpha = 0.10f)
                            else Color.White.copy(alpha = 0.12f),
                            alpha = 1f - progress * 0.5f
                        )
                    }
                )
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun MainNavigationScreen(viewModel: PlayerViewModel = viewModel()) {
    var currentTab by remember { mutableIntStateOf(0) }
    var activeSubPage by remember { mutableStateOf<String?>(null) }

    // 「立即前往导入」（引导第 5 项 / 风格页）→ 切到歌单页；
    // 导入面板由 PlaylistScreen 监听到同一个标记后自动打开。
    val importPending = ImportBus.pending.value
    val importHostCtx = LocalContext.current
    LaunchedEffect(importPending) {
        if (importPending) {
            if (!viewModel.isLoggedIn.value) {
                // 没登录就别把人扔进歌单页再弹"必须登录"——直接送去账号页登录，
                // 登录成功后账号页会自动继续打开导入面板（ImportBus.afterLogin）。
                ImportBus.afterLogin = true
                currentTab = 3
                activeSubPage = null
                ImportBus.consume()
                AppToast.show("登录后会自动打开歌单导入")
            } else {
                currentTab = 3
                // 从引导页跳过来时，主界面还没完成首次绘制，
                // 歌单页依赖的玻璃背景（backdrop）此刻是空白的 → 整页会透出后面的东西。
                // 先等两帧让主内容画出来，再进子页面。
                kotlinx.coroutines.delay(120)
                activeSubPage = "playlists"
            }
        }
    }

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLooping by viewModel.isLooping.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()

    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val gyroscope = remember { sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }
    var hasGyroscope by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("shake_guide", Context.MODE_PRIVATE) }
    var showShakeGuide by remember { mutableStateOf(false) }

    var guideButtonEnabled by remember { mutableStateOf(false) }
    var guideCountdown by remember { mutableIntStateOf(3) }

    LaunchedEffect(showShakeGuide) {
        if (showShakeGuide) {
            guideButtonEnabled = false
            guideCountdown = 3
            while (guideCountdown > 0) {
                delay(1000)
                guideCountdown--
            }
            guideButtonEnabled = true
        }
    }

    LaunchedEffect(Unit) { hasGyroscope = gyroscope != null }

    var lastGyroActionTime by remember { mutableLongStateOf(0L) }
    val gyroCooldown = 1000L

    if (hasGyroscope) {
        DisposableEffect(gyroscope) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                        val y = event.values[1]
                        val now = System.currentTimeMillis()
                        if (viewModel.currentSong.value != null && now - lastGyroActionTime > gyroCooldown) {
                            if (y > 2.5f && !viewModel.isConsoleMinimized) {
                                viewModel.isConsoleMinimized = true
                                lastGyroActionTime = now
                            } else if (y < -2.5f && viewModel.isConsoleMinimized) {
                                viewModel.isConsoleMinimized = false
                                lastGyroActionTime = now
                            }
                        }
                    }
                }
                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
            gyroscope?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    if (!hasGyroscope) {
        DisposableEffect(Unit) {
            val filter = android.content.IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            val receiver = object : android.content.BroadcastReceiver() {
                var last = 0L
                override fun onReceive(c: android.content.Context?, i: android.content.Intent?) {
                    val now = System.currentTimeMillis()
                    if (now - last > 500 && viewModel.currentSong.value != null) {
                        viewModel.isConsoleMinimized = !viewModel.isConsoleMinimized
                        last = now
                    }
                }
            }
            context.registerReceiver(receiver, filter)
            onDispose { context.unregisterReceiver(receiver) }
        }
    }

    val guideDismissed = prefs.getBoolean("guide_dismissed", false)
    LaunchedEffect(currentSong, guideDismissed) {
        if (currentSong != null && !guideDismissed) {
            delay(3000)
            showShakeGuide = true
        }
    }

    if (activeSubPage != null) BackHandler { activeSubPage = null }
    if (viewModel.isConsoleExpanded || viewModel.isConsoleFullScreen) {
        BackHandler {
            if (viewModel.isConsoleFullScreen) viewModel.isConsoleFullScreen = false
            else viewModel.isConsoleExpanded = false
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isTablet = configuration.screenWidthDp >= 600
        var railWidthPx by remember { mutableIntStateOf(0) }
    var showAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    // 推荐页长按歌曲 → 打开同一个「添加到歌单」面板
    val addToPlaylistReq = SongActionBus.pendingAddToPlaylist.value
    LaunchedEffect(addToPlaylistReq) {
        if (addToPlaylistReq != null) {
            showAddToPlaylist = addToPlaylistReq
            SongActionBus.consume()
        }
    }
    var showFloatLyrics by remember { mutableStateOf(false) }
    showAddToPlaylist?.let { targetSong ->
        val ctxAtp = LocalContext.current
        AddToPlaylistSheet(
            song = targetSong,
            onDismiss = { showAddToPlaylist = null },
            onAdded = { pl ->
                val psong = PlaylistSong(id = targetSong.id, name = targetSong.name, artist = targetSong.artist, picUrl = targetSong.picUrl)
                PlaylistStore.savePlaylist(ctxAtp, pl.copy(songs = pl.songs + psong))
                showAddToPlaylist = null
                AppToast.show("已加入「" + pl.name + "」")
            }
        )
    }
    val railWidth = with(LocalDensity.current) { railWidthPx.toDp() }
        val windowKey = "${configuration.screenWidthDp}x${configuration.screenHeightDp}"
    val backgroundBackdrop = key(windowKey) { rememberLayerBackdrop() }
    val fullBackdrop = key(windowKey) { rememberLayerBackdrop() }
                val ksuSurfaceColor = if (viewModel.md3Style) MaterialTheme.colorScheme.surfaceContainer
    else MaterialTheme.colorScheme.background
    val ksuBackdrop = key(windowKey) {
        miuixRememberLayerBackdrop {
            drawRect(ksuSurfaceColor)
            drawContent()
        }
    }

    // 统一动效：入场 300ms 减速 + 强调弹簧上浮，退场 200ms 加速 —— 与全局 Motion 规范一致，
    // 避免以前「入场 220ms 淡入+弹簧 / 退场 160ms+180ms」两套节奏对不上导致的衔接痕迹。
    val subPageEnter = fadeIn(Motion.enter()) +
            slideInVertically(initialOffsetY = { it / 18 }, animationSpec = Motion.emphasized())
    val subPageExit = fadeOut(Motion.exit()) +
            slideOutVertically(targetOffsetY = { it / 18 }, animationSpec = Motion.exit())

    fun dismissGuide(permanent: Boolean = false) {
        showShakeGuide = false
        if (permanent) {
            prefs.edit().putBoolean("guide_dismissed", true).apply()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GlassShaderWarmup(fullBackdrop)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        viewModel.md3Style -> MaterialTheme.colorScheme.surfaceContainer
                        viewModel.isCustomBackgroundEnabled -> MaterialTheme.colorScheme.surfaceContainerLowest
                        else -> MaterialTheme.colorScheme.background
                    }
                )
                .layerBackdrop(backgroundBackdrop)
        ) {
            if (viewModel.isCustomBackgroundEnabled && !viewModel.md3Style) {
                AsyncImage(
                    model = viewModel.customBackgroundUri ?: viewModel.backgroundImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(viewModel.backgroundBiasX, viewModel.backgroundBiasY),
                    modifier = Modifier.fillMaxSize().then(
                        if (viewModel.isBlurEnabled) Modifier.blur(
                            if (viewModel.blurMode == 0) viewModel.globalBlurRadius.dp
                            else (viewModel.globalBlurRadius * 2f).dp
                        )
                        else Modifier
                    )
                )
            }
        }

                        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!viewModel.md3Style) Modifier.layerBackdrop(fullBackdrop) else Modifier
                )
                .then(
                    if (viewModel.useFloatingNavBar && viewModel.navBarLiquidGlass) {
                        Modifier.miuixLayerBackdrop(ksuBackdrop)
                    } else Modifier
                )
        ) {
            if (viewModel.isCustomBackgroundEnabled && !viewModel.md3Style) {
                AsyncImage(
                    model = viewModel.customBackgroundUri ?: viewModel.backgroundImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(viewModel.backgroundBiasX, viewModel.backgroundBiasY),
                    modifier = Modifier.fillMaxSize().then(
                        if (viewModel.isBlurEnabled) Modifier.blur(
                            if (viewModel.blurMode == 0) viewModel.globalBlurRadius.dp
                            else (viewModel.globalBlurRadius * 2f).dp
                        )
                        else Modifier
                    )
                )
            }

            val mainPagerState = rememberPagerState(initialPage = currentTab, pageCount = { 4 })
            LaunchedEffect(currentTab) {
                if (mainPagerState.currentPage != currentTab) {
                    // 跨多页直接跳转：长动画在快速连点下会被反复取消而停在中间页；相邻页保留动画
                    if (kotlin.math.abs(mainPagerState.currentPage - currentTab) > 1) {
                        mainPagerState.scrollToPage(currentTab)
                    } else {
                        mainPagerState.animateScrollToPage(currentTab)
                    }
                }
            }
            LaunchedEffect(mainPagerState) {
                snapshotFlow { mainPagerState.settledPage }.collect { page ->
                    if (page != currentTab) currentTab = page
                }
            }
            HorizontalPager(
                state = mainPagerState,
                // 禁止左右滑动切 Tab：内层分区也有横向 Pager，滑到头会把手势冒泡上来，
                // 用户"滑着滑着"就跳到搜索页了。改为只能点底部导航切换。
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = if (isTablet && !viewModel.isConsoleFullScreen) railWidth else 0.dp)
            ) { page ->
                when (page) {
                    0 -> CommunityScreen(viewModel = viewModel, onBack = { currentTab = 1 })
                    1 -> MusicSearchTab(viewModel = viewModel, backdrop = backgroundBackdrop)
                    2 -> SettingsThemeTab(
                        viewModel = viewModel,
                        backdrop = backgroundBackdrop,
                        onNavigateToCustomCenter = { activeSubPage = "custom_center" },
                        onNavigateToColorPalette = { activeSubPage = "color_palette" }
                    )
                    3 -> ProfileTab(
                        viewModel = viewModel,
                        backdrop = backgroundBackdrop,
                        onNavigateToHistory = { activeSubPage = "history" }, onNavigateToPlaylists = { activeSubPage = "playlists" }
                    )
                }
            }

                        if (isTablet && !viewModel.isConsoleFullScreen) {
                MiuixTabletNavigationRail(
                    backdrop = backgroundBackdrop,
                    selectedTab = currentTab,
                    onTabSelected = { currentTab = it },
                    isDark = viewModel.isForceDarkMode || ColorMode.fromValue(viewModel.colorMode).isDark || (ColorMode.fromValue(viewModel.colorMode).isSystem && isSystemInDarkTheme()),
                    onWidthChange = { railWidthPx = it },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset(x = viewModel.navBarOffsetX.dp, y = viewModel.navBarOffsetY.dp)
        ) {
            AnimatedVisibility(
                visible = currentSong != null && !viewModel.isConsoleFullScreen && !viewModel.isConsoleMinimized,
                enter = fadeIn(animationSpec = spring(dampingRatio = 0.82f, stiffness = 240f)) +
                        slideInVertically(
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 200f),
                            initialOffsetY = { it / 3 }
                        ) +
                        scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = 0.80f, stiffness = 220f)),
                exit = fadeOut(animationSpec = spring(dampingRatio = 0.95f, stiffness = 120f)) +
                        slideOutVertically(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 160f),
                            targetOffsetY = { it / 4 }
                        ) +
                        scaleOut(targetScale = 0.94f, animationSpec = spring(dampingRatio = 0.90f, stiffness = 140f))
            ) {
                MD3StyleBottomSheetConsole(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    isLooping = isLooping,
                    currentPosition = currentPosition,
                    duration = duration,
                    lyrics = currentLyrics,
                    currentLyricIndex = currentLyricIndex,
                    viewModel = viewModel,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.playNext() },
                    onPrevious = { viewModel.playPrevious() },
                    onToggleLoop = { viewModel.toggleLooping() },
                    isExpanded = viewModel.isConsoleExpanded,
                    onExpandChange = { viewModel.isConsoleExpanded = it },
                    onFullScreen = { viewModel.isConsoleFullScreen = true },
                    isLandscape = isLandscape,
                    backdrop = fullBackdrop
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (!isTablet && viewModel.useFloatingNavBar && !viewModel.isConsoleFullScreen) {
                                val navIsDark = viewModel.isForceDarkMode || ColorMode.fromValue(viewModel.colorMode).isDark || (ColorMode.fromValue(viewModel.colorMode).isSystem && isSystemInDarkTheme())
                val miuixBackdrop = ksuBackdrop
                MiuixTheme(
                    controller = remember(navIsDark) {
                        ThemeController(if (navIsDark) ColorSchemeMode.Dark else ColorSchemeMode.Light)
                    }
                ) {
                    val navBottomPadding = WindowInsets.navigationBars
                        .asPaddingValues().calculateBottomPadding()
                        .let { inset -> if (inset != 0.dp) 8.dp + inset else 28.dp }
                    FloatingBottomBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .padding(start = 28.dp, end = 28.dp, bottom = navBottomPadding),
                        selectedIndex = currentTab,
                        onSelected = { currentTab = it },
                        backdrop = miuixBackdrop,
                        tabsCount = 4,
                        isBlurEnabled = viewModel.navBarLiquidGlass,
                        transparencyLevel = viewModel.navBarTransparency,
                    ) { activateTab ->
                        FloatingBottomBarItem(
                            selected = currentTab == 0,
                            onClick = { activateTab(0) },
                            modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                        ) {
                            MiuixIcon(imageVector = Icons.Rounded.Explore, contentDescription = "推荐")
                            MiuixText(text = "推荐", fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                        }
                        FloatingBottomBarItem(
                            selected = currentTab == 1,
                            onClick = { activateTab(1) },
                            modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                        ) {
                            MiuixIcon(imageVector = Icons.Rounded.Search, contentDescription = "搜索")
                            MiuixText(text = "搜索", fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                        }
                        FloatingBottomBarItem(
                            selected = currentTab == 2,
                            onClick = { activateTab(2) },
                            modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                        ) {
                            MiuixIcon(imageVector = Icons.Rounded.Settings, contentDescription = "设置")
                            MiuixText(text = "设置", fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                        }
                        FloatingBottomBarItem(
                            selected = currentTab == 3,
                            onClick = { activateTab(3) },
                            modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                        ) {
                            MiuixIcon(imageVector = Icons.Rounded.Person, contentDescription = "账号")
                            MiuixText(text = "账号", fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                        }
                    }
                }
            } else if (!isTablet && !viewModel.useFloatingNavBar && !viewModel.isConsoleFullScreen) {
                NavigationBar(
                                                            containerColor = if (LocalMd3Style.current) MaterialTheme.colorScheme.surfaceContainer
                    else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(if (currentTab == 0) Icons.Rounded.Explore else Icons.Outlined.Explore, null) },
                        label = { Text("推荐") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(if (currentTab == 1) Icons.Rounded.Search else Icons.Outlined.Search, null) },
                        label = { Text("搜索") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(if (currentTab == 2) Icons.Rounded.Settings else Icons.Outlined.Settings, null) },
                        label = { Text("设置") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(if (currentTab == 3) Icons.Rounded.Person else Icons.Outlined.Person, null) },
                        label = { Text("账号") }
                    )
                }
            }
        }

        if (viewModel.isConsoleFullScreen && currentSong != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                    FullScreenPlayer(
                currentSong = currentSong!!,
                isPlaying = isPlaying,
                isLooping = isLooping,
                currentPosition = currentPosition,
                duration = duration,
                lyrics = currentLyrics,
                currentLyricIndex = currentLyricIndex,
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onToggleLoop = { viewModel.toggleLooping() },
                onSeek = { viewModel.seekTo(it) },
                onDismiss = { viewModel.isConsoleFullScreen = false },
                backdrop = fullBackdrop,
                backgroundImageUri = viewModel.customBackgroundUri ?: viewModel.backgroundImageUri,
                onAddToFavorites = { viewModel.addToFavorites(currentSong!!) },
                onAddToPlaylist = { song -> showAddToPlaylist = song },
                onDownload = { viewModel.downloadSong(currentSong!!) },
                onShare = {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "我在 MINova 听 ${currentSong!!.name} - ${currentSong!!.artist}"
                        )
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "分享"))
                }
            )
            }
        }

        AnimatedVisibility(
            visible = showShakeGuide,
            enter = fadeIn(Motion.enter()) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 250f)
            ),
            exit = fadeOut(Motion.exit()) + scaleOut(
                targetScale = 0.92f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 250f)
            ) + slideOutVertically(Motion.exit()) { it / 8 }
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .animateContentSize(spring(dampingRatio = 0.7f, stiffness = 300f))
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // was(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(top = 40.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "tiltIcon")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )
                        Icon(
                            Icons.Rounded.RotateRight,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp).graphicsLayer { rotationZ = rotation }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            if (hasGyroscope) "陀螺仪控制" else "音量键控制",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            if (hasGyroscope) "倾斜手机即可隐藏或显示控制台\n无需触屏，让音乐播放更沉浸"
                            else "按音量键即可隐藏或显示控制台\n无需触屏，让音乐播放更沉浸",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 21.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (hasGyroscope) {
                                GuideStep("1", "正常竖屏握持手机")
                                GuideStep("2", "向左倾斜 → 隐藏控制台")
                                GuideStep("3", "向右倾斜 → 恢复控制台")
                                GuideStep("4", "操作后 1 秒内不会重复触发")
                            } else {
                                GuideStep("1", "按下音量键")
                                GuideStep("2", "控制台向下收缩隐藏")
                                GuideStep("3", "再次按下音量键")
                                GuideStep("4", "控制台恢复显示")
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { dismissGuide(permanent = true) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) { Text("不再提示", fontWeight = FontWeight.Medium, fontSize = 14.sp) }

                            Button(
                                onClick = { dismissGuide() },
                                enabled = guideButtonEnabled,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                AnimatedContent(
                                    targetState = guideButtonEnabled,
                                    transitionSpec = { fadeIn(Motion.enter()) togetherWith fadeOut(Motion.exit()) },
                                    label = "btnText"
                                ) { enabled ->
                                    if (enabled) Text("我知道了", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    else Text("${guideCountdown}s", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = activeSubPage == "custom_center", enter = subPageEnter, exit = subPageExit) {
            Box(modifier = Modifier.fillMaxSize().miuixLayerBackdrop(ksuBackdrop)) {
            ThemeAndPersonalizationScreen(
                viewModel = viewModel,
                backdrop = fullBackdrop,
                onBack = { activeSubPage = null }
            )
            }
        }
        AnimatedVisibility(visible = activeSubPage == "nav_settings", enter = subPageEnter, exit = subPageExit) {
            Box(modifier = Modifier.fillMaxSize().miuixLayerBackdrop(ksuBackdrop)) {
            NavigationBarSettingsScreen(
                viewModel = viewModel,
                onBack = { activeSubPage = null },
                backdrop = fullBackdrop
            )
            }
        }
                AnimatedVisibility(visible = activeSubPage == "color_palette", enter = subPageEnter, exit = subPageExit) {
            Box(modifier = Modifier.fillMaxSize().miuixLayerBackdrop(ksuBackdrop)) {
            ColorPaletteScreen(
                viewModel = viewModel,
                onBack = { activeSubPage = null },
                onNavigateToNavSettings = { activeSubPage = "nav_settings" }
            )
            }
        }
        AnimatedVisibility(visible = activeSubPage == "history", enter = subPageEnter, exit = subPageExit) {
            // 补一层主题底色：这两个页面自身是透明容器，只靠 backdrop 出效果，
            // backdrop 没就绪时会整页透明，加底色兜底最稳。
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).miuixLayerBackdrop(ksuBackdrop)) {
                HistoryScreen(viewModel = viewModel, backdrop = backgroundBackdrop, onBack = { activeSubPage = null })
            }
        }
        AnimatedVisibility(visible = activeSubPage == "playlists", enter = subPageEnter, exit = subPageExit) {
            // 同上：歌单页自身不画背景，这里兜底一层底色，避免"背景透明"
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).miuixLayerBackdrop(ksuBackdrop)) {
                PlaylistScreen(viewModel = viewModel, onBack = { activeSubPage = null }, backdrop = backgroundBackdrop)
            }
        }

    }
}

@Composable
private fun GuideStep(number: String, text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(number, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayer(
    currentSong: Song,
    isPlaying: Boolean,
    isLooping: Boolean,
    currentPosition: Long,
    duration: Long,
    lyrics: LyricData,
    currentLyricIndex: Int,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleLoop: () -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
    backdrop: Backdrop,
    backgroundImageUri: Any?,
    onAddToFavorites: (() -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    var showLyrics by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val lyricHalf = screenHeight / 2

    LaunchedEffect(currentLyricIndex, showLyrics) {
        if (showLyrics && currentLyricIndex >= 0 && lyrics.times.isNotEmpty()) {
            listState.animateScrollToItem(
                index = maxOf(currentLyricIndex, 0),
                scrollOffset = 0
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (backgroundImageUri != null || currentSong.picUrl.isNotEmpty()) MaterialTheme.colorScheme.surfaceContainerLowest
                else MaterialTheme.colorScheme.background
            )
    ) {

        // 换源解析的瞬间 picUrl 可能短暂为空，用"稳定封面"避免背景闪空
        val stableCover = rememberStableCover(currentSong.picUrl)
        val bgModel = when {
            stableCover.isNotEmpty() -> stableCover
            backgroundImageUri != null -> backgroundImageUri
            else -> null
        }
        if (bgModel != null) {
            AsyncImage(
                model = bgModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(30.dp)
            )
        } else {
            // 无封面且无自定义背景：用统一封面占位图做模糊底（原为随机网图）
            Image(
                painter = painterResource(R.drawable.fallback_cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(30.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.10f))
        )
        // 竖向渐变：上下稍暗、中间透亮 —— 白字更清楚，画面也更有层次
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.30f),
                        0.30f to Color.Transparent,
                        0.70f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.46f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        "收起",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    if (showLyrics) "歌词" else "正在播放",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(44.dp)) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            "更多",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("收藏到列表") },
                            leadingIcon = { Icon(Icons.Rounded.FavoriteBorder, null) },
                            onClick = {
                                showMenu = false
                                onAddToFavorites?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("添加进歌单") },
                            leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                            onClick = {
                                showMenu = false
                                onAddToPlaylist?.invoke(currentSong)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("下载音乐") },
                            leadingIcon = { Icon(Icons.Rounded.Download, null) },
                            onClick = {
                                showMenu = false
                                onDownload?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("分享") },
                            leadingIcon = { Icon(Icons.Rounded.Share, null) },
                            onClick = {
                                showMenu = false
                                onShare?.invoke()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = showLyrics,
                transitionSpec = {
                    (fadeIn(Motion.enter()) + slideInHorizontally(Motion.enter()) { it / 8 }) togetherWith
                            (fadeOut(Motion.exit()) + slideOutHorizontally(Motion.exit()) { -it / 8 })
                },
                label = "contentSwitch"
            ) { isLyrics ->

                if (!isLyrics) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(12.dp))

                        // 封面：播放时轻微"呼吸"放大，配柔和投影与顶部玻璃高光
                        val coverScale by animateFloatAsState(
                            targetValue = if (isPlaying) 1f else 0.955f,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 220f),
                            label = "coverScale"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(1f)
                                .scale(coverScale)
                                .shadow(elevation = 26.dp, shape = RoundedCornerShape(28.dp), clip = false)
                                .clip(RoundedCornerShape(28.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (lyrics.times.isNotEmpty()) showLyrics = true
                                }
                        ) {
                            val coverModel = rememberStableCover(currentSong.picUrl)
                            if (coverModel.isNotEmpty()) {
                                AsyncImage(
                                    model = coverModel,
                                    contentDescription = "Album Art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // 无封面兜底：统一封面占位图（原为随机网图）
                                Image(
                                    painter = painterResource(R.drawable.fallback_cover),
                                    contentDescription = "Album Art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0f to Color.White.copy(alpha = 0.12f),
                                            0.45f to Color.Transparent,
                                            1f to Color.Black.copy(alpha = 0.20f)
                                        )
                                    )
                            )
                        }

                        Spacer(Modifier.height(28.dp))

                        Text(
                            text = currentSong.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = currentSong.artist,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            letterSpacing = 0.6.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(24.dp))

                        if (lyrics.times.isNotEmpty() && currentLyricIndex >= 0) {
                            val time = lyrics.times[currentLyricIndex]
                            val text = lyrics.lyrics[time] ?: ""
                            val trans = lyrics.translations[time]

                            AnimatedContent(
                                targetState = "$text|${trans ?: ""}",
                                transitionSpec = {
                                    (fadeIn(Motion.enter()) + slideInVertically(Motion.enter()) { it / 3 }) togetherWith
                                            (fadeOut(Motion.exit()) + slideOutVertically(Motion.exit()) { -it / 3 })
                                },
                                label = "lyricPreview"
                            ) { content ->
                                val parts = content.split("|")
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { showLyrics = true },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        parts.getOrNull(0) ?: "",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (parts.size > 1 && parts[1].isNotEmpty()) {
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            parts[1],
                                            fontSize = 13.sp,
                                            color = Color.White.copy(alpha = 0.65f),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else if (lyrics.times.isNotEmpty() || lyrics.lyrics.isNotEmpty()) {
                            // 有歌词、只是还没唱到第一句（前奏/间奏）→ 不是"纯音乐"
                            Text(
                                "等待前奏…",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                if (lyrics.instrumental) "纯音乐，请欣赏" else "暂无歌词",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        if (ProgressStylePrefs.waveEnabled(androidx.compose.ui.platform.LocalContext.current)) {
                            TadpoleWaveSlider(
                                                        value = currentPosition.toFloat(),
                                                        onValueChange = { onSeek(it.toLong()) },
                                                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        activeColor = Color.White,
                                                        inactiveColor = Color.White.copy(alpha = 0.25f),
                                                        thumbColor = Color.White
                                                    )
                        } else {
                            Slider(
                                                        value = currentPosition.toFloat(),
                                                        onValueChange = { onSeek(it.toLong()) },
                                                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.25f)),
                                                    )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                formatDuration(currentPosition),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                formatDuration(duration),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onToggleLoop, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    if (isLooping) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                    null,
                                    tint = if (isLooping) Color.White else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                                Icon(
                                    Icons.Rounded.SkipPrevious,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            FilledIconButton(
                                onClick = onPlayPause,
                                modifier = Modifier.size(72.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    null,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                                Icon(
                                    Icons.Rounded.SkipNext,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    Icons.Rounded.Shuffle,
                                    null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (lyrics.times.isNotEmpty()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item { Spacer(Modifier.height(lyricHalf)) }
                                itemsIndexed(lyrics.times) { index, time ->
                                    val isCurrent = index == currentLyricIndex
                                    val text = lyrics.lyrics[time] ?: ""
                                    val trans = lyrics.translations[time]

                                    val textColor by animateColorAsState(
                                        targetValue = if (isCurrent) Color.White
                                        else Color.White.copy(alpha = 0.32f),
                                        animationSpec = tween(500),
                                        label = "lyricColor"
                                    )
                                    val scale by animateFloatAsState(
                                        targetValue = if (isCurrent) 1.12f else 1f,
                                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 240f),
                                        label = "lyricScale"
                                    )
                                    val baseSize = 18f

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSeek(time) }
                                            .padding(vertical = 14.dp, horizontal = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = text,
                                                fontSize = baseSize.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = textColor,
                                                textAlign = TextAlign.Center,
                                                lineHeight = (baseSize * 1.4f).sp
                                            )
                                            if (!trans.isNullOrEmpty()) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = trans,
                                                    fontSize = if (isCurrent) 14.sp else 12.sp,
                                                    color = if (isCurrent) Color.White.copy(alpha = 0.7f)
                                                    else Color.White.copy(alpha = 0.25f),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                                item { Spacer(Modifier.height(lyricHalf)) }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (lyrics.instrumental) "纯音乐，请欣赏" else "暂无歌词",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onToggleLoop, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    if (isLooping) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                    null,
                                    tint = if (isLooping) Color.White else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                                Icon(
                                    Icons.Rounded.SkipPrevious,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            FilledIconButton(
                                onClick = onPlayPause,
                                modifier = Modifier.size(72.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    null,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                                Icon(
                                    Icons.Rounded.SkipNext,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    Icons.Rounded.Shuffle,
                                    null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MusicSearchTab(
    viewModel: PlayerViewModel,
    backdrop: Backdrop
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var lastKeyword by remember { mutableStateOf("") }
    var showAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    showAddToPlaylist?.let { targetSong ->
        val ctxAtp = LocalContext.current
        AddToPlaylistSheet(
            song = targetSong,
            onDismiss = { showAddToPlaylist = null },
            onAdded = { pl ->
                val psong = PlaylistSong(id = targetSong.id, name = targetSong.name, artist = targetSong.artist, picUrl = targetSong.picUrl)
                PlaylistStore.savePlaylist(ctxAtp, pl.copy(songs = pl.songs + psong))
                showAddToPlaylist = null
                AppToast.show("已加入「" + pl.name + "」")
            }
        )
    }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val cornerDp = viewModel.cornerRadius.dp

    LaunchedEffect(Unit) {
        if (!viewModel.hasLoadedLocalAssets) {
            // v2：旧版会往 filesDir 写 default_banner/background.jpg 且永不更新，导致换包后
            // 仍显示旧壁纸/旧横幅。改成带版本号的文件名，且仅当用户**主动设置过**自定义图
            // （存在 custom 标记）时才用本地副本，否则一律用 APK 内置新资源。
            val prefs = context.getSharedPreferences("minova_assets", Context.MODE_PRIVATE)
            val usesCustom = prefs.getBoolean("uses_custom_assets", false)
            if (usesCustom) {
                val localBanner = File(context.filesDir, "default_banner_v2.jpg")
                if (localBanner.exists()) viewModel.bannerImageUri = localBanner
                val localBg = File(context.filesDir, "default_background_v2.jpg")
                if (localBg.exists()) viewModel.backgroundImageUri = localBg
            }
            viewModel.hasLoadedLocalAssets = true
        }
        // 让后端请求带上账号标识：服务端据此把 Cookie / 歌单按账号分区存放
        MusicApiClient.refreshUserId(context)
        viewModel.checkLoginReminder()
    }

    fun performSearch() {
        if (searchText.isNotBlank() && searchText != lastKeyword) {
            lastKeyword = searchText
            viewModel.searchOnline(searchText)
            focusManager.clearFocus()
        }
    }

    if (showHistoryDialog) {
        Dialog(
            onDismissRequest = { showHistoryDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showHistoryDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .then(
                            if (LocalMd3Style.current)
                                Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(32.dp))
                            else Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedCornerShape(32.dp) },
                                effects = {
                                    vibrancy()
                                    blur(8f.dp.toPx())
                                    lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
                                },
                                highlight = { Highlight.Plain },
                                shadow = { Shadow(alpha = 0.25f) },
                                innerShadow = { InnerShadow(radius = 12f.dp, alpha = 0.06f) }
                            )
                        )
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 28.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.ManageSearch,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "历史搜索",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        if (searchHistory.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearHistory() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteSweep,
                                    null,
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    if (searchHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.SearchOff,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "暂无搜索历史",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .padding(horizontal = 16.dp)
                        ) {
                            itemsIndexed(searchHistory, key = { _, h -> h }) { _, historyKey ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    onClick = {
                                        searchText = historyKey
                                        lastKeyword = historyKey
                                        viewModel.searchOnline(historyKey)
                                        showHistoryDialog = false
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp, horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.History,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            historyKey,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            Icons.Rounded.CallMade,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalButton(
                            onClick = { showHistoryDialog = false },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("关闭", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

        val isTabletSearch = LocalConfiguration.current.screenWidthDp >= 600
        val searchLeftColumn: @Composable () -> Unit = {
        Card(
            modifier = Modifier.fillMaxWidth().height(190.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(cornerDp)
        ) {
            key(viewModel.bannerImageUri, viewModel.bannerBiasX, viewModel.bannerBiasY) {
                AsyncImage(model = viewModel.bannerImageUri, contentDescription = "Banner", contentScale = ContentScale.Crop, alignment = BiasAlignment(viewModel.bannerBiasX, viewModel.bannerBiasY), modifier = Modifier.fillMaxSize())
            }
        }

        val searchContainerPadding by animateDpAsState(targetValue = if (isSearchFocused) 12.dp else 16.dp, animationSpec = tween(250, easing = FastOutSlowInEasing))
        val searchCornerRadius by animateDpAsState(targetValue = if (isSearchFocused) 20.dp else cornerDp, animationSpec = tween(250, easing = FastOutSlowInEasing))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = searchContainerPadding, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .then(
                        if (LocalMd3Style.current)
                            Modifier.background(MaterialTheme.colorScheme.surfaceBright, RoundedCornerShape(searchCornerRadius))
                        else Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(searchCornerRadius) },
                            effects = {
                                vibrancy()
                                blur(if (isSearchFocused) 14f.dp.toPx() else 10f.dp.toPx())
                                lens(16f.dp.toPx(), 24f.dp.toPx())
                            },
                            highlight = {
                                Highlight.Default.copy(alpha = if (isSearchFocused) 0.7f else 0.45f)
                            },
                            shadow = { Shadow(alpha = 0.1f) }
                        )
                    )
                    .clip(RoundedCornerShape(searchCornerRadius))
            ) {
                BasicTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    singleLine = true,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxSize()
                        .onFocusChanged { isSearchFocused = it.isFocused },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                    decorationBox = { innerTextField ->
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable { showHistoryDialog = true }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.History, null, tint = if (isSearchFocused || showHistoryDialog) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchText.isEmpty()) Text("搜索歌曲、歌手", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 15.sp)
                                innerTextField()
                            }
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = "" }, modifier = Modifier.size(28.dp)) { Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = isSearchFocused || searchText.isNotBlank(),
                enter = fadeIn(Motion.enter()) + expandHorizontally(Motion.enter()),
                exit = fadeOut(Motion.exit()) + shrinkHorizontally(Motion.exit())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    SearchLiquidButton(
                        onClick = { performSearch() },
                        backdrop = backdrop,
                        shape = RoundedCornerShape(searchCornerRadius),
                        label = "搜索"
                    )
                }
            }
        }
    }

        val searchResultsPane: @Composable (Modifier) -> Unit = { resultModifier ->

        // ⚠️ 顺序必须是**确定的**：多音源是并行 async 抓取后合并的（`async` 谁先返回谁先入表），
        // 所以 searchResults 的原始顺序每次搜索都不一样。对随机顺序做 reversed() 依旧是随机顺序，
        // 用户看到的还是"随缘分布、好歌在下面"。因此这里完全按**显式排序键**排，不依赖合并顺序：
        //   ① 好版本优先（!isJunkVersion：歌手名里的 DJ / 翻唱 / 混音也认）
        //   ② 来源优先级固定：kuwo > migu > kugou > qq > netease > bilibili
        //   ③ 时长已知的优先
        //   ④ 歌名更短优先（更接近原版标题）
        //   ⑤ id + source 兜底 → 同一组搜索结果每次顺序完全一致
        // 排序方向：默认倒序，用户可在列表顶部的标签里手动切成正序。
        var searchSortDesc by remember { mutableStateOf(false) }
        val sortedSearchResults = remember(searchResults, searchSortDesc) {
            // 稳定排序：只按 垃圾版本 + 来源优先级 两个键，
            // 这样每个音源各自的相关度顺序会被保留（原唱通常在各源第一条）。
            val srcRank = mapOf(
                "kuwo" to 0, "migu" to 1, "kugou" to 2, "qq" to 3, "netease" to 4,
                "bilibili" to 5, "soda" to 6, "fivesing" to 7, "joox" to 8,
                "jamendo" to 9, "qianqian" to 10
            )
            val ordered = searchResults.sortedWith(
                compareBy(
                    { if (isJunkVersion(it)) 1 else 0 },
                    { srcRank[it.source.lowercase()] ?: 11 }
                )
            )
            if (searchSortDesc) ordered.asReversed() else ordered
        }

        Box(modifier = resultModifier) {
            val targetState = when {
                isSearching -> 0
                searchResults.isNotEmpty() -> 3
                else -> 2
            }

            AnimatedContent(
                targetState = targetState,
                transitionSpec = {
                    val spec = spring<Float>(dampingRatio = 0.75f, stiffness = 300f)
                    (fadeIn(spec) + scaleIn(initialScale = 0.94f, animationSpec = spec)) togetherWith
                    (fadeOut(spring(dampingRatio = 0.9f, stiffness = 200f)) + scaleOut(targetScale = 0.94f, animationSpec = spring(dampingRatio = 0.9f, stiffness = 200f)))
                },
                label = "MainListSwitch"
            ) { state ->
                when (state) {
                    0 -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                                Spacer(Modifier.height(16.dp))
                                Text("搜索中...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp)
                            }
                        }
                    }
                    2 -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    if (lastKeyword.isBlank()) "搜索你想听的音乐"
                                    else "没有找到「$lastKeyword」，可能是网络不稳",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                                if (lastKeyword.isNotBlank()) {
                                    Spacer(Modifier.height(16.dp))
                                    FilledTonalButton(
                                        onClick = { viewModel.searchOnline(lastKeyword) },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Text("重试", fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        val moreState = rememberLazyListState()
                        val moreNearBottom by remember {
                            derivedStateOf {
                                val info = moreState.layoutInfo
                                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                                last >= info.totalItemsCount - 3
                            }
                        }
                        val moreHas by viewModel.hasMoreSearch.collectAsState()
                        val moreLoading by viewModel.isLoadingMore.collectAsState()
                        // 使劲往上滑到底部 → 自动加载下一页（不需要点按钮）
                        LaunchedEffect(moreNearBottom, moreHas, moreLoading) {
                            if (moreNearBottom && moreHas && !moreLoading) viewModel.loadMoreSearch()
                        }
                        LazyColumn(
                            state = moreState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                // 排序标签：默认倒序，点一下可切正序（用户手动调整）
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("倒序" to true, "正序" to false).forEach { (label, desc) ->
                                        val on = searchSortDesc == desc
                                        val bg by animateColorAsState(
                                            targetValue = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            animationSpec = tween(220), label = "sortBg"
                                        )
                                        val fg by animateColorAsState(
                                            targetValue = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            animationSpec = tween(220), label = "sortFg"
                                        )
                                        val pad by animateDpAsState(
                                            targetValue = if (on) 14.dp else 12.dp,
                                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f), label = "sortPad"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(bg)
                                                .clickable { searchSortDesc = desc }
                                                .padding(horizontal = pad, vertical = 6.dp)
                                        ) {
                                            Text(label, fontSize = 12.sp, color = fg)
                                        }
                                    }
                                }
                            }
                            itemsIndexed(
                                items = sortedSearchResults,
                                // 去重键带上版本标识后，同一首歌的多个版本会同时出现在列表里，
                                // 只拿 id 当 key 可能撞车（LazyColumn 会直接崩）。这里用
                                // id+来源+歌名/歌手/专辑，与上面的去重键一一对应。
                                key = { _, s ->
                                    listOf(s.id, s.source, s.name, s.artist, s.album)
                                        .joinToString("\u0001").lowercase()
                                }
                            ) { index, song ->
                                SongItem(
                                    song = song,
                                    onAddToPlaylist = { s -> showAddToPlaylist = s },
                                    onLongPress = { selectedSong -> showAddToPlaylist = selectedSong },
                                    isCurrentSong = song.id == currentSong?.id,
                                    isPlaying = isPlaying,
                                    isFirst = index == 0,
                                    isLast = index == sortedSearchResults.size - 1,
                                    onClick = { viewModel.playSong(song) },
                                    viewModel = viewModel,
                                    backdrop = backdrop,
                                    isInFavorites = false,
                                    onRemoveFromFavorites = null
                                )
                            }
                            // ── 加载更多：同一关键词请求下一页并追加到列表末尾 ──
                            // 展示层是「整体倒序」，所以追加后新结果自动出现在列表最前面(?)——
                            // 实际是按倒序规则统一渲染，顺序始终一致。
                            item {
                                val hasMoreSearch by viewModel.hasMoreSearch.collectAsState()
                                val isLoadingMore by viewModel.isLoadingMore.collectAsState()
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 20.dp, vertical = 9.dp)
                                    ) {
                                        Text(
                                            when {
                                                isLoadingMore -> "加载中…"
                                                !hasMoreSearch -> "没有更多了"
                                                else -> "加载更多"
                                            },
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        if (isTabletSearch) {
                        Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(0.40f).fillMaxHeight()) {
                    searchLeftColumn()
                }
                Box(modifier = Modifier.weight(0.60f).fillMaxHeight()) {
                    searchResultsPane(Modifier.fillMaxSize())
                }
            }
        } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                searchLeftColumn()
                searchResultsPane(Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}
@Composable
fun SearchLiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    label: String = "搜索",
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Search,
    modifier: Modifier = Modifier
) {
        if (LocalMd3Style.current) {
        androidx.compose.material3.Button(
            onClick = onClick,
            shape = shape,
            modifier = modifier.height(52.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        return
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "SearchBtnPress"
    )

    val btnColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(12f.dp.toPx(), 24f.dp.toPx())
                },
                highlight = {
                    Highlight.Default.copy(alpha = if (isPressed) 0.9f else 0.6f)
                },
                shadow = { Shadow(alpha = 0.15f) },
                layerBlock = {
                    if (isPressed) {
                        scaleX = 0.98f
                        scaleY = 0.98f
                    }
                },
                onDrawSurface = {
                    drawRect(btnColor.copy(alpha = 0.85f))
                }
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = Color.White)
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
    }
}

@Composable
fun SettingsThemeTab(
    viewModel: PlayerViewModel,
    backdrop: Backdrop,
    onNavigateToCustomCenter: () -> Unit,
    onNavigateToColorPalette: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showFloatLyrics by remember { mutableStateOf(false) }
    // 音乐偏好入口：设置里随时可改（首启弹窗只在引导+风格选择完成后弹一次）
    var showTastePrefs by remember { mutableStateOf(false) }
    // 「更新日志」入口已按用户要求挪到「账号」页（ProfileTab 的 更新日志 → ChangelogScreen），
    // 设置页不再保留该入口；1.3.1 的条目由 changelogEntries() 统一提供。

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "个性化设置",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 2.dp)
        )

        if (LocalMd3Style.current) {

            val ctx = androidx.compose.ui.platform.LocalContext.current
            var waveProgress by remember { mutableStateOf(ProgressStylePrefs.waveEnabled(ctx)) }
                        KsuSegColumn {
                val rememberLastOn by viewModel.rememberLast.collectAsState()
                KsuSegClick(
                    index = 0, count = 10,
                    icon = Icons.Outlined.Image,
                    title = "主题与个性化",
                    summary = "此风格暂不支持自定义壁纸",
                    onClick = onNavigateToCustomCenter
                )
                KsuSegClick(
                    index = 1, count = 10,
                    icon = Icons.Rounded.Palette,
                    title = "主题与颜色",
                    summary = "可调整应用内大部分的主题设置与风格",
                    onClick = onNavigateToColorPalette
                )
                KsuSegSwitch(
                    index = 2, count = 10,
                    icon = if (viewModel.isForceDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    title = "暗色模式",
                    summary = "强制使用深色界面",
                    checked = viewModel.isForceDarkMode,
                    onCheckedChange = {
                        viewModel.isForceDarkMode = it
                        viewModel.savePreferences()
                    }
                )
            KsuSegSwitch(
                        index = 3, count = 10,
                        icon = Icons.Rounded.FormatColorText,
                        title = "文字变白",
                        summary = "仅文字转白，图标保持主题色，对抗暗色壁纸",
                        checked = viewModel.whiteTextMode,
                        onCheckedChange = {
                            viewModel.whiteTextMode = it
                            viewModel.savePreferences()
                        }
                    )
                KsuSegClick(
                    index = 4, count = 10,
                    icon = Icons.Rounded.Lyrics,
                    title = "悬浮歌词（测试版）",
                    summary = "桌面歌词：可穿透、可调色、可调透明度",
                    onClick = { showFloatLyrics = true }
                )
                if (showFloatLyrics) {
                    FloatingLyricsSettingsSheet(backdrop = backdrop, onDismiss = { showFloatLyrics = false })
                }

                KsuSegSwitch(
                    index = 5, count = 10,
                    icon = Icons.Rounded.MusicNote,
                    title = "波浪进度条",
                    summary = "进度条样式",
                    checked = waveProgress,
                    onCheckedChange = {
                        waveProgress = it
                        ProgressStylePrefs.setWaveEnabled(ctx, it)
                    }
                )
                KsuSegClick(
                    index = 6, count = 10,
                    icon = Icons.Rounded.Equalizer,
                    title = "音乐偏好",
                    summary = "音乐类型 关注艺人（参与推荐打分）",
                    onClick = { showTastePrefs = true }
                )
                // 与「暗色模式」同样的卡片样式
                KsuSegSwitch(
                    index = 7, count = 10,
                    icon = Icons.Rounded.Restore,
                    title = "记住上次播放",
                    summary = "下次打开应用继续上次的歌与进度",
                    checked = rememberLastOn,
                    onCheckedChange = { on -> viewModel.setRememberLast(ctx, on) }
                )
                // 音质切换：先占位 + 标注"开发中"，让用户有个期待
                KsuSegClick(
                    index = 8, count = 9,
                    icon = Icons.Rounded.Equalizer,
                    title = "音质切换（开发中）",
                    summary = "标准 / 高品 / 无损，正在开发中",
                    onClick = {
                        AppToast.show("音质切换正在开发中，敬请期待 🎧")
                    }
                )
                if (showTastePrefs) {
                    MusicTasteSettingsSheet(backdrop = backdrop, onDismiss = { showTastePrefs = false })
                }
            }
        } else {
                    SettingsGlassCard(modifier = Modifier.padding(horizontal = 16.dp), backdrop = backdrop, onClick = onNavigateToCustomCenter) {
            Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("主题与个性化", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                // 液态玻璃风格（md3Style = false）支持自定义壁纸：主界面会绘制自定义背景（isCustomBackgroundEnabled && !md3Style），
                // 此前误标「暂不支持」，已修正为支持文案
                Text("支持自定义壁纸与横幅，点击前往设置", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

                SettingsGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    backdrop = backdrop,
                    onClick = { showFloatLyrics = true }
                ) {
                    Icon(Icons.Rounded.Lyrics, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("悬浮歌词（测试版）", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("桌面歌词：可穿透、可调色、可调透明度", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (showFloatLyrics) {
                    FloatingLyricsSettingsSheet(backdrop = backdrop, onDismiss = { showFloatLyrics = false })
                }

                val ctx2 = LocalContext.current
                SettingsGlassCard(modifier = Modifier.padding(horizontal = 16.dp), backdrop = backdrop, onClick = {
                    AppToast.show("音质切换正在开发中，敬请期待 🎧")
                }) {
                    Icon(Icons.Rounded.Equalizer, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("音质切换（开发中）", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("标准 / 高品 / 无损，正在开发中", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                SettingsGlassCard(modifier = Modifier.padding(horizontal = 16.dp), backdrop = backdrop, onClick = {}) {
                    val rememberOn by viewModel.rememberLast.collectAsState()
                    val ctxL = LocalContext.current
                    Icon(Icons.Rounded.Restore, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("记住上次播放", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("下次打开应用继续上次的歌与进度", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LiquidToggleSwitch(checked = rememberOn, onCheckedChange = { on -> viewModel.setRememberLast(ctxL, on) }, backdrop = backdrop)
                }

                SettingsGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    backdrop = backdrop,
                    onClick = { showTastePrefs = true }
                ) {
                    Icon(Icons.Rounded.Equalizer, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("音乐偏好", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("音乐类型 关注艺人（参与推荐打分）", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (showTastePrefs) {
                    MusicTasteSettingsSheet(backdrop = backdrop, onDismiss = { showTastePrefs = false })
                }

                SettingsGlassCard(modifier = Modifier.padding(horizontal = 16.dp), backdrop = backdrop, onClick = onNavigateToColorPalette) {
            Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("主题与颜色", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("可调整应用内大部分的主题设置与风格", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SettingsGlassCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            backdrop = backdrop,
            onClick = {
                viewModel.isForceDarkMode = !viewModel.isForceDarkMode
                viewModel.savePreferences()
            }
        ) {
            Icon(
                if (viewModel.isForceDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("暗色模式", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("强制使用深色界面", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LiquidToggleSwitch(
                checked = viewModel.isForceDarkMode,
                onCheckedChange = {
                    viewModel.isForceDarkMode = it
                    viewModel.savePreferences()
                },
                backdrop = backdrop
            )
        }

            SettingsGlassCard(
            modifier = Modifier.padding(horizontal = 16.dp),
                backdrop = backdrop,
                onClick = {
                    viewModel.whiteTextMode = !viewModel.whiteTextMode
                    viewModel.savePreferences()
                }
            ) {
                Icon(Icons.Rounded.FormatColorText, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("文字变白", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("仅文字转白，图标保持主题色，对抗暗色壁纸", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LiquidToggleSwitch(
                    checked = viewModel.whiteTextMode,
                    onCheckedChange = {
                        viewModel.whiteTextMode = it
                        viewModel.savePreferences()
                    },
                    backdrop = backdrop
                )
            }
        }

            }
}

@Composable
fun LiquidToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop
) {
        if (LocalMd3Style.current) {
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        return
    }
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        MaterialTheme.colorScheme.primary
    val trackColor =
        MaterialTheme.colorScheme.surfaceVariant

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }

    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    onCheckedChange(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (checked) 0f else 1f
                    onCheckedChange(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = dragAmount.x / dragWidth
                fraction =
                    if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                    else (fraction - delta).fastCoerceIn(0f, 1f)
            }
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { fraction ->
                dampedDragAnimation.updateValue(fraction)
            }
    }
    LaunchedEffect(checked) {
        snapshotFlow { checked }
            .collectLatest { isSelected ->
                val target = if (isSelected) 1f else 0f
                if (target != fraction) {
                    fraction = target
                    dampedDragAnimation.animateToValue(target)
                }
            }
    }

    val trackBackdrop = rememberLayerBackdrop()

    Box(
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(Capsule())
                .drawBehind {
                    val fraction = dampedDragAnimation.value
                    drawRect(lerp(trackColor, accentColor, fraction))
                }
                .size(64.dp, 28.dp)
        )

        Box(
            Modifier
                .graphicsLayer {
                    val fraction = dampedDragAnimation.value
                    val padding = 2.dp.toPx()
                    translationX =
                        if (isLtr) lerp(padding, padding + dragWidth, fraction)
                        else lerp(-padding, -(padding + dragWidth), fraction)
                }
                .semantics {
                    role = Role.Switch
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 0.75f, progress)
                            val scaleY = lerp(0f, 0.75f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            5f.dp.toPx() * progress,
                            10f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40.dp, 24.dp)
        )
    }
}

/**
 * 「设置 → 音乐偏好」：双风格选择器。
 *  - MD3：Material3 分段排版 + FilterChip；
 *  - 液态玻璃：沿用设置页 SettingsGlassCard 的实底表面（solid = true 同款），避免透明穿透。
 * 保存后立刻让「每日 30 首」与旧歌池缓存失效，并通知推荐页刷新 —— 否则会出现
 * 「选完口味还是排行榜的歌 / 重进也没用」。
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun MusicTasteSettingsSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val md3 = LocalMd3Style.current
    var genres by remember { mutableStateOf(RecommendPrefs.genres(ctx)) }
    var artists by remember { mutableStateOf(RecommendPrefs.artists(ctx)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(top = 2.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 20.dp, top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "音乐偏好",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text("设置", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "用于「推荐」页打分与关键词：选得越准，首屏越贴近你的口味",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            if (!md3) {
                // 液态玻璃风格：实底卡片（solid = true 同款表面），不透明、不穿透
                SettingsGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    backdrop = backdrop,
                    solid = true,
                    onClick = { }
                ) {
                    Icon(Icons.Rounded.Equalizer, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "已选 " + genres.size + " 个类型 / " + artists.size + " 位艺人",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "保存在本机（未登录账号也能生效）",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            TastePickerBody(
                md3 = md3,
                genres = genres,
                artists = artists,
                onToggleGenre = { g -> genres = if (g in genres) genres - g else genres + g },
                onToggleArtist = { a -> artists = if (a in artists) artists - a else artists + a },
                modifier = Modifier.padding(horizontal = 16.dp),
                compact = !md3
            )
            FilledTonalButton(
                onClick = {
                    RecommendPrefs.saveAndInvalidate(ctx, genres, artists)
                    AppToast.show("已保存，推荐已按新偏好刷新")
                    onDismiss()
                },
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = genres.isNotEmpty() || artists.isNotEmpty()
            ) {
                Text("保存", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun FloatingLyricsSettingsSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val md3 = LocalMd3Style.current
    var enabled by remember { mutableStateOf(FloatingLyricsPrefs.isEnabled(ctx)) }
    var showTranslation by remember { mutableStateOf(FloatingLyricsPrefs.showTranslation(ctx)) }
    var locked by remember { mutableStateOf(FloatingLyricsPrefs.isLocked(ctx)) }
    var bgColor by remember { mutableStateOf(FloatingLyricsPrefs.bgColor(ctx)) }
    var opaque by remember { mutableStateOf(FloatingLyricsPrefs.isOpaque(ctx)) }
    var textColor by remember { mutableStateOf(FloatingLyricsPrefs.textColor(ctx)) }
    var fontSize by remember { mutableStateOf(FloatingLyricsPrefs.fontSize(ctx)) }
    var useAccent by remember { mutableStateOf(FloatingLyricsPrefs.useAccentForCurrent(ctx)) }
    // ── MD3 重构新增的调整项状态（每项都会立刻作用到悬浮窗）──
    var bold by remember { mutableStateOf(FloatingLyricsPrefs.bold(ctx)) }
    var showPN by remember { mutableStateOf(FloatingLyricsPrefs.showPrevNext(ctx)) }
    var lineGap by remember { mutableIntStateOf(FloatingLyricsPrefs.lineSpacing(ctx)) }
    var alignMode by remember { mutableIntStateOf(FloatingLyricsPrefs.align(ctx)) }
    var bgPct by remember { mutableIntStateOf(FloatingLyricsPrefs.bgAlphaPercent(ctx)) }
    var cornerDp by remember { mutableIntStateOf(FloatingLyricsPrefs.cornerRadiusDp(ctx)) }
    var widthPct by remember { mutableIntStateOf(FloatingLyricsPrefs.cardWidthPercent(ctx)) }
    var shadowPct by remember { mutableIntStateOf(FloatingLyricsPrefs.shadowAlphaPercent(ctx)) }
    var padHDp by remember { mutableIntStateOf(FloatingLyricsPrefs.paddingHDp(ctx)) }
    var snapEdge by remember { mutableStateOf(FloatingLyricsPrefs.snapToEdge(ctx)) }
    var needOverlayPerm by remember { mutableStateOf(false) }
    val hasOverlayPerm = FloatingLyricsState.canOverlay(ctx)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (enabled) FloatingLyricsService.start(ctx)
    }

    // 申请「显示在其他应用上层」权限
    val requestOverlay: () -> Unit = {
        runCatching {
            ctx.startActivity(
                android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + ctx.packageName)
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        Unit
    }

    // 启用开关（权限不足时回滚并提示）
    val applyEnabled: (Boolean) -> Unit = { on ->
        if (on) {
            if (FloatingLyricsState.canOverlay(ctx)) {
                enabled = true
                FloatingLyricsPrefs.setEnabled(ctx, true)
                FloatingLyricsService.start(ctx)
            } else {
                enabled = false
                FloatingLyricsPrefs.setEnabled(ctx, false)
                needOverlayPerm = true
            }
        } else {
            enabled = false
            FloatingLyricsPrefs.setEnabled(ctx, false)
            FloatingLyricsService.stop(ctx)
        }
    }

    val textColorOptions = listOf(
        Color.White, Color.Black,
        Color(0xFFFFD54F), Color(0xFF64B5F6),
        Color(0xFF81C784), Color(0xFFFF8A80)
    )
    val bgColorOptions = listOf(
        Color.Black, Color.White,
        Color(0xFF424242), Color(0xFF1A237E),
        Color(0xFF4A148C), Color(0xFFBF360C)
    )
    // 预览必须和悬浮窗真实效果一致：底色按文字色自动调对比度，文字带反向阴影
    val previewMainColor = if (useAccent) MaterialTheme.colorScheme.primary.toArgb() else textColor
    val previewBgColor = if (opaque) LyricContrast.readableBg(previewMainColor, bgColor, 4.5) else bgColor
    val previewShadow = androidx.compose.ui.graphics.Shadow(
        color = Color(LyricContrast.shadowFor(textColor)),
        offset = Offset(0f, 2f),
        blurRadius = 6f
    )
    val previewMainShadow = androidx.compose.ui.graphics.Shadow(
        color = Color(LyricContrast.shadowFor(previewMainColor)),
        offset = Offset(0f, 2f),
        blurRadius = 6f
    )

    // 色板一行（两种风格共用，圆角色块 + 选中描边）
    val swatchRow: @Composable (List<Color>, Int, (Int) -> Unit) -> Unit = { colors, current, onPick ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEachIndexed { i, c ->
                val selected = current == c.toArgb()
                val lum = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (md3) MaterialTheme.colorScheme.surfaceBright
                            else Color.White.copy(alpha = 0.10f)
                        )
                        .clickable { onPick(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                if (selected) 3.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape
                            )
                    )
                    if (selected) {
                        Icon(
                            Icons.Rounded.Check, null,
                            tint = if (lum > 0.6f) Color.Black else Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(top = 2.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 标题：与「个性化设置」页同款排版 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 20.dp, top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "悬浮歌词",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text("设置", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // ── 实时预览：与悬浮窗同款卡片（圆角 / 描边 / 阴影 / 动态色当前行） ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (opaque) Color(previewBgColor) else Color.Transparent)
                    .then(
                        if (opaque) Modifier.border(
                            1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp)
                        ) else Modifier.drawBehind {
                            // 透明态：斜纹底表示「无背景」
                            val gap = 14.dp.toPx()
                            val stroke = 5.dp.toPx()
                            var x = -size.height
                            while (x < size.width + size.height) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.06f),
                                    start = Offset(x, size.height),
                                    end = Offset(x + size.height, 0f),
                                    strokeWidth = stroke
                                )
                                x += gap
                            }
                        }
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "风把心事吹散在街角",
                        fontSize = (fontSize * 0.78f).sp,
                        color = Color(textColor).copy(alpha = 0.47f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(shadow = previewShadow)
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "美好不期而遇",
                        fontSize = fontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (useAccent) MaterialTheme.colorScheme.primary else Color(textColor),
                        maxLines = 1,
                        style = TextStyle(shadow = previewMainShadow)
                    )
                    if (showTranslation) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "All good things come unexpectedly",
                            fontSize = (fontSize * 0.68f).sp,
                            color = Color(textColor).copy(alpha = 0.67f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(shadow = previewShadow)
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "雨后的天空总会放晴",
                        fontSize = (fontSize * 0.78f).sp,
                        color = Color(textColor).copy(alpha = 0.47f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(shadow = previewShadow)
                    )
                }
            }

            // ── 悬浮窗权限提示 ──
            if (needOverlayPerm || (enabled && !hasOverlayPerm)) {
                if (md3) {
                    KsuSegColumn {
                        KsuSegClick(
                            index = 0, count = 1,
                            icon = Icons.Rounded.Warning,
                            title = "需要「显示在其他应用上层」权限",
                            summary = "点此前往系统设置授权，回来后重新打开开关",
                            onClick = requestOverlay
                        )
                    }
                } else {
                    SettingsGlassCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        backdrop = backdrop,
                        solid = true,
                        onClick = requestOverlay
                    ) {
                        Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "需要「显示在其他应用上层」权限",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "点此前往系统设置授权，回来后重新打开开关",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (md3) {
                // ══════════ MD3：与「主题与颜色」页完全一致的分段列表 ══════════
                KsuSegColumn(title = "歌词显示") {
                    KsuSegSwitch(
                        index = 0, count = 4,
                        icon = Icons.Rounded.Lyrics,
                        title = "启用悬浮歌词",
                        summary = "桌面显示歌词卡片，可拖动、可点按",
                        checked = enabled,
                        onCheckedChange = { applyEnabled(it) }
                    )
                    KsuSegSwitch(
                        index = 1, count = 4,
                        icon = Icons.Rounded.Palette,
                        title = "当前行跟随动态色",
                        summary = "当前行用 App 动态主题色高亮",
                        checked = useAccent,
                        onCheckedChange = {
                            useAccent = it
                            FloatingLyricsPrefs.setUseAccentForCurrent(ctx, it)
                        }
                    )
                    KsuSegSwitch(
                        index = 2, count = 4,
                        icon = Icons.Rounded.Translate,
                        title = "显示翻译",
                        summary = "当前行下方显示翻译行",
                        checked = showTranslation,
                        onCheckedChange = {
                            showTranslation = it
                            FloatingLyricsPrefs.setShowTranslation(ctx, it)
                        }
                    )
                    KsuSegSwitch(
                        index = 3, count = 4,
                        icon = Icons.Rounded.Lock,
                        title = "锁定歌词",
                        summary = "锁定 = 完全穿透且点击无效，可在通知中心解锁",
                        checked = locked,
                        onCheckedChange = {
                            locked = it
                            FloatingLyricsPrefs.setLocked(ctx, it)
                        }
                    )
                }

                KsuSegTitle("文字颜色")
                var lyricColorIndexState by remember { mutableIntStateOf(FloatingLyricsPrefs.lyricColorIndex(ctx)) }
                run {                 Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("粉色" to 0, "蓝色" to 1).forEach { (nm, idx) ->
                        val on = lyricColorIndexState == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    FloatingLyricsPrefs.setLyricColorIndex(ctx, idx); lyricColorIndexState = idx
                                    FloatingLyricsPrefs.setLyricColorIndex(ctx, idx)
                                    FloatingLyricsService.applyNow(ctx)
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(nm, fontSize = 13.sp, color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } }
                Text(
                    "底色会按文字色自动保证对比度：选白字自动压深底色，任何配色都看得清",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                KsuSegTitle("自定义文字颜色")
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0xFFFFFFFF.toInt() to "白", 0xFF000000.toInt() to "黑",
                        0xFFFFD54F.toInt() to "黄", 0xFF64B5F6.toInt() to "蓝",
                        0xFF81C784.toInt() to "绿", 0xFFFF8A80.toInt() to "红",
                        0xFFFF69B4.toInt() to "粉", 0xFFB39DDB.toInt() to "紫"
                    ).forEach { (argb, _) ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(argb))
                                .clickable {
                                    FloatingLyricsPrefs.setTextColor(ctx, argb)
                                    FloatingLyricsPrefs.setUseAccentForCurrent(ctx, false)
                                    FloatingLyricsService.applyNow(ctx)
                                }
                        )
                    }
                }

                KsuSegTitle("背景颜色")
                var lyricBgIndexState by remember { mutableIntStateOf(FloatingLyricsPrefs.lyricBgIndex(ctx)) }
                run {                 Row(modifier = Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("深色" to 0, "白色" to 1, "雫蓝" to 2, "芙兰" to 3, "绒灰蓝" to 4, "薰衣草紫" to 5, "透明" to 6).forEach { (nm, idx) ->
                        val on = lyricBgIndexState == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    FloatingLyricsPrefs.setLyricBgIndex(ctx, idx); lyricBgIndexState = idx
                                    FloatingLyricsPrefs.setLyricBgIndex(ctx, idx)
                                    FloatingLyricsService.applyNow(ctx)
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(nm, fontSize = 13.sp, color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } }

                KsuSegTitle("自定义背景颜色")
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF424242.toInt(), 0xFF1A237E.toInt(),
                        0xFF4A148C.toInt(), 0xFFBF360C.toInt(), 0xFFF5F9FF.toInt(), 0xFF1B1B1F.toInt()
                    ).forEach { argb ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(argb))
                                .clickable {
                                    FloatingLyricsPrefs.setBgColor(ctx, argb)
                                    FloatingLyricsService.applyNow(ctx)
                                }
                        )
                    }
                }

                KsuSegColumn(title = "卡片外观") {
                    KsuSegSwitch(
                        index = 0, count = 2,
                        icon = Icons.Rounded.WaterDrop,
                        title = "背景不透明",
                        summary = "关闭后背景完全透明，只留带阴影的文字",
                        checked = opaque,
                        onCheckedChange = {
                            opaque = it
                            FloatingLyricsPrefs.setOpaque(ctx, it)
                        }
                    )
                    KsuSegRow(
                        index = 1, count = 2,
                        icon = Icons.Rounded.FormatSize,
                        title = "字号",
                        summary = "当前 ${fontSize}sp",
                        onClick = null
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    fontSize = (fontSize - 2).coerceIn(10, 40)
                                    FloatingLyricsPrefs.setFontSize(ctx, fontSize)
                                },
                                modifier = Modifier.size(34.dp)
                            ) { Icon(Icons.Rounded.Remove, null) }
                            Text(
                                "${fontSize}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    fontSize = (fontSize + 2).coerceIn(10, 40)
                                    FloatingLyricsPrefs.setFontSize(ctx, fontSize)
                                },
                                modifier = Modifier.size(34.dp)
                            ) { Icon(Icons.Rounded.Add, null) }
                        }
                    }
                }
            } else {
                // ══════════ 液态玻璃：与「个性化设置」页同款玻璃卡片 ══════════
                SettingsGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    backdrop = backdrop,
                    solid = true,
                    onClick = { applyEnabled(!enabled) }
                ) {
                    Icon(Icons.Rounded.Lyrics, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("启用悬浮歌词", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("桌面显示歌词卡片，可拖动、可点按", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LiquidToggleSwitch(
                        checked = enabled,
                        onCheckedChange = { applyEnabled(it) },
                        backdrop = backdrop
                    )
                }

                SettingsGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    backdrop = backdrop,
                    solid = true,
                    onClick = {
                        useAccent = !useAccent
                        FloatingLyricsPrefs.setUseAccentForCurrent(ctx, useAccent)
                    }
                ) {
                    Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("当前行跟随动态色", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("当前行用 App 动态主题色高亮", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LiquidToggleSwitch(
                        checked = useAccent,
                        onCheckedChange = {
                            useAccent = it
                            FloatingLyricsPrefs.setUseAccentForCurrent(ctx, it)
                        },
                        backdrop = backdrop
                    )
                }

                SettingsGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    backdrop = backdrop,
                    solid = true,
                    onClick = {
                        showTranslation = !showTranslation
                        FloatingLyricsPrefs.setShowTranslation(ctx, showTranslation)
                    }
                ) {
                    Icon(Icons.Rounded.Translate, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("显示翻译", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("当前行下方显示翻译行", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LiquidToggleSwitch(
                        checked = showTranslation,
                        onCheckedChange = {
                            showTranslation = it
                            FloatingLyricsPrefs.setShowTranslation(ctx, it)
                        },
                        backdrop = backdrop
                    )
                }

                SettingsGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    backdrop = backdrop,
                    solid = true,
                    onClick = {
                        locked = !locked
                        FloatingLyricsPrefs.setLocked(ctx, locked)
                    }
                ) {
                    Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("锁定歌词", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("锁定 = 完全穿透且点击无效，可在通知中心解锁", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LiquidToggleSwitch(
                        checked = locked,
                        onCheckedChange = {
                            locked = it
                            FloatingLyricsPrefs.setLocked(ctx, it)
                        },
                        backdrop = backdrop
                    )
                }

                KsuSegTitle("文字颜色")
                var lyricColorIndexState by remember { mutableIntStateOf(FloatingLyricsPrefs.lyricColorIndex(ctx)) }
                run {                 Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("粉色" to 0, "蓝色" to 1).forEach { (nm, idx) ->
                        val on = lyricColorIndexState == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    FloatingLyricsPrefs.setLyricColorIndex(ctx, idx); lyricColorIndexState = idx
                                    FloatingLyricsPrefs.setLyricColorIndex(ctx, idx)
                                    FloatingLyricsService.applyNow(ctx)
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(nm, fontSize = 13.sp, color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } }
                Text(
                    "底色会按文字色自动保证对比度：选白字自动压深底色，任何配色都看得清",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                KsuSegTitle("自定义文字颜色")
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0xFFFFFFFF.toInt() to "白", 0xFF000000.toInt() to "黑",
                        0xFFFFD54F.toInt() to "黄", 0xFF64B5F6.toInt() to "蓝",
                        0xFF81C784.toInt() to "绿", 0xFFFF8A80.toInt() to "红",
                        0xFFFF69B4.toInt() to "粉", 0xFFB39DDB.toInt() to "紫"
                    ).forEach { (argb, _) ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(argb))
                                .clickable {
                                    FloatingLyricsPrefs.setTextColor(ctx, argb)
                                    FloatingLyricsPrefs.setUseAccentForCurrent(ctx, false)
                                    FloatingLyricsService.applyNow(ctx)
                                }
                        )
                    }
                }

                KsuSegTitle("背景颜色")
                var lyricBgIndexState by remember { mutableIntStateOf(FloatingLyricsPrefs.lyricBgIndex(ctx)) }
                run {                 Row(modifier = Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("深色" to 0, "白色" to 1, "雫蓝" to 2, "芙兰" to 3, "绒灰蓝" to 4, "薰衣草紫" to 5, "透明" to 6).forEach { (nm, idx) ->
                        val on = lyricBgIndexState == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    FloatingLyricsPrefs.setLyricBgIndex(ctx, idx); lyricBgIndexState = idx
                                    FloatingLyricsPrefs.setLyricBgIndex(ctx, idx)
                                    FloatingLyricsService.applyNow(ctx)
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(nm, fontSize = 13.sp, color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } }

                SettingsGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    backdrop = backdrop,
                    solid = true,
                    onClick = {
                        opaque = !opaque
                        FloatingLyricsPrefs.setOpaque(ctx, opaque)
                    }
                ) {
                    Icon(Icons.Rounded.WaterDrop, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("背景不透明", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("关闭后背景完全透明，只留带阴影的文字", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LiquidToggleSwitch(
                        checked = opaque,
                        onCheckedChange = {
                            opaque = it
                            FloatingLyricsPrefs.setOpaque(ctx, it)
                        },
                        backdrop = backdrop
                    )
                }

                SettingsGlassCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    backdrop = backdrop,
                    solid = true,
                    onClick = {}
                ) {
                    Icon(Icons.Rounded.FormatSize, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("字号", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("当前 ${fontSize}sp", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                fontSize = (fontSize - 2).coerceIn(10, 40)
                                FloatingLyricsPrefs.setFontSize(ctx, fontSize)
                            },
                            modifier = Modifier.size(34.dp)
                        ) { Icon(Icons.Rounded.Remove, null) }
                        Text(
                            "${fontSize}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                fontSize = (fontSize + 2).coerceIn(10, 40)
                                FloatingLyricsPrefs.setFontSize(ctx, fontSize)
                            },
                            modifier = Modifier.size(34.dp)
                        ) { Icon(Icons.Rounded.Add, null) }
                    }
                }
            }

            // ── 说明 ──
            Text(
                "修改立即生效：悬浮窗背景只有「不透明 / 透明」两态；锁定后歌词完全穿透，可在通知中心「解锁」或在此关闭。",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 2.dp)
            )
            // ── 排版（MD3）──
            LyricOptionCard("排版") {
                LyricSwitchRow("文字加粗", bold) {
                    bold = it; FloatingLyricsPrefs.setBold(ctx, it); FloatingLyricsService.applyNow(ctx)
                }
                LyricSwitchRow("显示上一句 / 下一句", showPN) {
                    showPN = it; FloatingLyricsPrefs.setShowPrevNext(ctx, it); FloatingLyricsService.applyNow(ctx)
                }
                LyricSliderRow("行间距", lineGap.toFloat(), 0f..12f, "${lineGap} dp") {
                    lineGap = it.roundToInt(); FloatingLyricsPrefs.setLineSpacing(ctx, lineGap); FloatingLyricsService.applyNow(ctx)
                }
                LyricSegRow("对齐方式", alignMode, listOf("左", "中", "右")) {
                    alignMode = it; FloatingLyricsPrefs.setAlign(ctx, it); FloatingLyricsService.applyNow(ctx)
                }
            }

            // ── 外观（MD3）──
            LyricOptionCard("外观") {
                LyricSliderRow("背景不透明度", bgPct.toFloat(), 0f..100f, "${bgPct}%") {
                    bgPct = it.roundToInt(); FloatingLyricsPrefs.setBgAlphaPercent(ctx, bgPct); FloatingLyricsService.applyNow(ctx)
                }
                LyricSliderRow("卡片圆角", cornerDp.toFloat(), 0f..32f, "${cornerDp} dp") {
                    cornerDp = it.roundToInt(); FloatingLyricsPrefs.setCornerRadiusDp(ctx, cornerDp); FloatingLyricsService.applyNow(ctx)
                }
                LyricSliderRow("卡片宽度", widthPct.toFloat(), 60f..100f, "${widthPct}%") {
                    widthPct = it.roundToInt(); FloatingLyricsPrefs.setCardWidthPercent(ctx, widthPct); FloatingLyricsService.applyNow(ctx)
                }
                LyricSliderRow("阴影强度", shadowPct.toFloat(), 0f..100f, "${shadowPct}%") {
                    shadowPct = it.roundToInt(); FloatingLyricsPrefs.setShadowAlphaPercent(ctx, shadowPct); FloatingLyricsService.applyNow(ctx)
                }
                LyricSliderRow("左右内边距", padHDp.toFloat(), 8f..40f, "${padHDp} dp") {
                    padHDp = it.roundToInt(); FloatingLyricsPrefs.setPaddingHDp(ctx, padHDp); FloatingLyricsService.applyNow(ctx)
                }
            }

            // ── 行为（MD3）──
            LyricOptionCard("行为") {
                LyricSwitchRow("松手吸附屏幕边缘", snapEdge) {
                    snapEdge = it; FloatingLyricsPrefs.setSnapToEdge(ctx, it); FloatingLyricsService.applyNow(ctx)
                }
            }
        }
    }
}

/* ================= 悬浮歌词设置面板的 MD3 组件 ================= */

/** MD3 设置分组卡：surfaceContainer 底 + 24dp 圆角 + titleSmall 分区标题 */
@Composable
private fun LyricOptionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
        )
        content()
    }
}

/** MD3 开关行：56dp 行高，标题 bodyLarge + Material3 Switch */
@Composable
private fun LyricSwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** MD3 滑块行：标题 + 右侧数值(labelLarge) + Material3 Slider */
@Composable
private fun LyricSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                display,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}

/** MD3 分段按钮行：SingleChoiceSegmentedButtonRow */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricSegRow(label: String, selected: Int, options: List<String>, onSelect: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, t ->
                SegmentedButton(
                    selected = selected == i,
                    onClick = { onSelect(i) },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size)
                ) {
                    Text(t, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
@Composable
private fun SettingsGlassCard(
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    solid: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "SettingsCardPress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .then(
                if (LocalMd3Style.current || solid)
                    Modifier.background(MaterialTheme.colorScheme.surfaceBright, RoundedRectangle(24f.dp))
                else Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(24f.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(16f.dp.toPx(), 32f.dp.toPx())
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = 0.5f)
                    }
                )
            )
            .clip(RoundedRectangle(24f.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop
) {
    val density = LocalDensity.current
    val trackColor = MaterialTheme.colorScheme.primary

    val trackWidth = with(density) { 52.dp.toPx() }
    val trackHeight = with(density) { 30.dp.toPx() }
    val knobSize = with(density) { 24.dp.toPx() }
    val padding = with(density) { 3.dp.toPx() }

    val offsetAnimation = remember { Animatable(if (checked) 1f else 0f) }
    LaunchedEffect(checked) {
        offsetAnimation.animateTo(
            targetValue = if (checked) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f)
        )
    }

    val knobOffsetPx = offsetAnimation.value * (trackWidth - knobSize - padding * 2)
    val knobOffsetDp = with(density) { knobOffsetPx.toDp() }
    val paddingDp = with(density) { padding.toDp() }

    Box(
        modifier = Modifier
            .width(52.dp)
            .height(30.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(6f.dp.toPx())
                    lens(10f.dp.toPx(), 16f.dp.toPx())
                },
                highlight = { Highlight.Default.copy(alpha = 0.5f) },
                shadow = { Shadow(alpha = 0.12f) },
                onDrawSurface = {
                    if (checked) {
                        drawRect(trackColor.copy(alpha = 0.85f))
                    }
                }
            )
            .clip(Capsule())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = paddingDp + knobOffsetDp, y = paddingDp)
                .size(24.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(3f.dp.toPx())
                        lens(6f.dp.toPx(), 10f.dp.toPx())
                    },
                    highlight = { Highlight.Default.copy(alpha = 0.8f) },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.85f))
                    }
                )
                .clip(Capsule())
        )
    }
}

@Composable
fun NavigationBarSettingsScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    backdrop: Backdrop
) {
    val scrollState = rememberScrollState()
    val cornerDp = viewModel.cornerRadius.dp
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, "返回", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("导航栏设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cornerDp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Layers, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("导航栏风格：液态玻璃", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Switch(
                                checked = viewModel.useFloatingNavBar,
                                enabled = liquidGlassSupported,
                                onCheckedChange = {
                                                                        viewModel.useFloatingNavBar = it
                                    viewModel.md3Style = !it
                                    viewModel.savePreferences()
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (viewModel.useFloatingNavBar) Icons.Rounded.Check else Icons.Rounded.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                        tint = if (viewModel.useFloatingNavBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (liquidGlassSupported) "关闭后使用 MD3 经典底部导航栏（Material Design 3）"
                            else "液态玻璃导航栏$LIQUID_GLASS_REQUIRE_TEXT，当前设备已回退经典导航栏",
                            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cornerDp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.WaterDrop, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("液态玻璃效果", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Switch(
                                checked = viewModel.navBarLiquidGlass,
                                enabled = liquidGlassSupported,
                                onCheckedChange = {
                                    viewModel.navBarLiquidGlass = it
                                    viewModel.savePreferences()
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (viewModel.navBarLiquidGlass) Icons.Rounded.Check else Icons.Rounded.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                        tint = if (viewModel.navBarLiquidGlass) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (liquidGlassSupported) "半透明毛玻璃导航栏效果"
                            else "玻璃模糊$LIQUID_GLASS_REQUIRE_TEXT，不可开启",
                            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cornerDp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Label, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("显示标签", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Switch(
                                checked = viewModel.navBarShowLabels,
                                onCheckedChange = {
                                    viewModel.navBarShowLabels = it
                                    viewModel.savePreferences()
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (viewModel.navBarShowLabels) Icons.Rounded.Check else Icons.Rounded.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                        tint = if (viewModel.navBarShowLabels) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cornerDp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Preview, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("实时预览", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        var previewTab by remember { mutableIntStateOf(0) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                                                        val miuixPreviewBackdrop = remember(backdrop) { MiuixBackdropAdapter(backdrop) }
                            AsyncImage(
                                model = viewModel.bannerImageUri ?: "https://picsum.photos/1200/600",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Column(
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            ) {
                                                                val navIsDark = viewModel.isForceDarkMode || ColorMode.fromValue(viewModel.colorMode).isDark || (ColorMode.fromValue(viewModel.colorMode).isSystem && isSystemInDarkTheme())
                                MiuixTheme(
                                    controller = remember(navIsDark) {
                                        ThemeController(if (navIsDark) ColorSchemeMode.Dark else ColorSchemeMode.Light)
                                    }
                                ) {
                                    FloatingBottomBar(
                                                                                modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentWidth(Alignment.CenterHorizontally)
                                            .padding(start = 8.dp, end = 8.dp),
                                        selectedIndex = previewTab,
                                        onSelected = { previewTab = it },
                                        backdrop = miuixPreviewBackdrop,
                                        tabsCount = 4,
                                        isBlurEnabled = viewModel.navBarLiquidGlass,
                        transparencyLevel = viewModel.navBarTransparency,
                                    ) { activateTab ->
                                        FloatingBottomBarItem(
                                            selected = previewTab == 0,
                                            onClick = { activateTab(0) },
                                            modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                                        ) {
                                            MiuixIcon(imageVector = Icons.Rounded.Explore, contentDescription = "推荐")
                                            MiuixText(text = "推荐", fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                                        }
                                        FloatingBottomBarItem(
                                            selected = previewTab == 1,
                                            onClick = { activateTab(1) },
                                            modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                                        ) {
                                            MiuixIcon(imageVector = Icons.Rounded.Search, contentDescription = "搜索")
                                            MiuixText(text = "搜索", fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                                        }
                                        FloatingBottomBarItem(
                                            selected = previewTab == 2,
                                            onClick = { activateTab(2) },
                                            modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                                        ) {
                                            MiuixIcon(imageVector = Icons.Rounded.Settings, contentDescription = "设置")
                                            MiuixText(text = "设置", fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                                        }
                                        FloatingBottomBarItem(
                                            selected = previewTab == 3,
                                            onClick = { activateTab(3) },
                                            modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                                        ) {
                                            MiuixIcon(imageVector = Icons.Rounded.Person, contentDescription = "账号")
                                            MiuixText(text = "账号", fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                                        }
                                    }
                                }
                            }
                        }
                        Text("点击图标预览切换效果", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp))
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cornerDp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.OpenWith, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("位置偏移", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("拖拽滑块调整导航栏位置", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("水平偏移", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Slider(
                            value = viewModel.navBarOffsetX,
                            onValueChange = {
                                viewModel.navBarOffsetX = it
                                viewModel.savePreferences()
                            },
                            valueRange = -40f..40f,
                            steps = 15,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("-40dp", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${viewModel.navBarOffsetX}dp", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("40dp", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("垂直偏移", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Slider(
                            value = viewModel.navBarOffsetY,
                            onValueChange = {
                                viewModel.navBarOffsetY = it
                                viewModel.savePreferences()
                            },
                            valueRange = -80f..80f,
                            steps = 15,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("-80dp", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${viewModel.navBarOffsetY}dp", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("80dp", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Button(
                    onClick = {
                        // 低版本不支持液态玻璃（RuntimeShader/AGSL）：恢复默认也不能把玻璃导航栏打开
                        viewModel.useFloatingNavBar = liquidGlassSupported
                        viewModel.navBarLiquidGlass = liquidGlassSupported
                        viewModel.navBarShowLabels = true
                        viewModel.navBarOffsetX = 0f
                        viewModel.navBarOffsetY = -20f
                        viewModel.savePreferences()
                        AppToast.show("已重置导航栏设置")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Icon(Icons.Rounded.Restore, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("恢复默认导航栏设置", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ThemeAndPersonalizationScreen(
    viewModel: PlayerViewModel,
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    var tempBannerUri by remember { mutableStateOf<Uri?>(null) }
    var tempBgUri by remember { mutableStateOf<Uri?>(null) }

    val cornerDp = viewModel.cornerRadius.dp
    val animatedCorner by animateFloatAsState(
        targetValue = viewModel.cornerRadius,
        animationSpec = tween(250)
    )
    val animatedBlur by animateFloatAsState(
        targetValue = viewModel.globalBlurRadius,
        animationSpec = tween(250)
    )

    LaunchedEffect(Unit) {
        if (viewModel.customBackgroundUri != null) {
            tempBgUri = viewModel.customBackgroundUri
        }
    }

    val bannerPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            tempBannerUri = it
            viewModel.bannerBiasX = 0f
            viewModel.bannerBiasY = 0f
            try {
                val bannerFile = File(context.filesDir, "default_banner.jpg")
                context.contentResolver.openInputStream(it)?.use { inStream ->
                    FileOutputStream(bannerFile).use { outStream -> inStream.copyTo(outStream) }
                }
                viewModel.bannerImageUri = it
                viewModel.savePreferences()
                AppToast.show("横幅已更新")
            } catch (_: Exception) {
                AppToast.show("横幅保存失败")
            }
        }
    }

    val backgroundPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            tempBgUri = it
            viewModel.backgroundBiasX = 0f
            viewModel.backgroundBiasY = 0f
            try {
                val bgFile = File(context.filesDir, "default_background.jpg")
                context.contentResolver.openInputStream(it)?.use { inStream ->
                    FileOutputStream(bgFile).use { outStream -> inStream.copyTo(outStream) }
                }
                viewModel.customBackgroundUri = it
                viewModel.backgroundImageUri = it
                viewModel.savePreferences()
                AppToast.show("背景已更新")
            } catch (_: Exception) {
                AppToast.show("背景保存失败")
            }
        }
    }

    val fixedColors = remember {
        listOf(
            "绿色" to Color(0xFF006C4C),
            "蓝色" to Color(0xFF4D5C92),
            "紫色" to Color(0xFF6750A4),
            "青色" to Color(0xFF3B73E6),
            "红色" to Color(0xFFB71C1C),
            "橙色" to Color(0xFFE65100),
            "粉色" to Color(0xFFFF4081),
            "深绿" to Color(0xFF004D40),
            "深蓝" to Color(0xFF01579B),
            "玫瑰" to Color(0xFF880E4F),
            "灰色" to Color(0xFF3E2723),
            "MaterialYOU" to Color.Transparent
        )
    }

    val previewHeight = screenWidth * (screenHeight / screenWidth) * 0.6f

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, "返回", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("主题与个性化", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cornerDp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    viewModel.isBlurSettingsExpanded = !viewModel.isBlurSettingsExpanded
                                    viewModel.savePreferences()
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.BlurOn, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("模糊效果", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    if (viewModel.isBlurEnabled) "已启用" else "已关闭",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (viewModel.isBlurSettingsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AnimatedVisibility(
                            visible = viewModel.isBlurSettingsExpanded,
                            enter = expandVertically(animationSpec = Motion.enter()) + fadeIn(Motion.enter()),
                            exit = shrinkVertically(animationSpec = Motion.exit()) + fadeOut(Motion.exit())
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Wallpaper, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("启用壁纸", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Switch(
                                        checked = viewModel.isCustomBackgroundEnabled,
                                        onCheckedChange = {
                                            viewModel.isCustomBackgroundEnabled = it
                                            viewModel.savePreferences()
                                        },
                                        thumbContent = {
                                            Icon(
                                                imageVector = if (viewModel.isCustomBackgroundEnabled) Icons.Rounded.Check else Icons.Rounded.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                                tint = if (viewModel.isCustomBackgroundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.BlurOn, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("壁纸模糊", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Switch(
                                        checked = viewModel.isBlurEnabled,
                                        onCheckedChange = {
                                            viewModel.isBlurEnabled = it
                                            viewModel.savePreferences()
                                        },
                                        enabled = viewModel.isCustomBackgroundEnabled,
                                        thumbContent = {
                                            Icon(
                                                imageVector = if (viewModel.isBlurEnabled) Icons.Rounded.Check else Icons.Rounded.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                                tint = if (viewModel.isBlurEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    )
                                }

                                Text("模糊模式", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = viewModel.blurMode == 0,
                                        onClick = { viewModel.blurMode = 0; viewModel.savePreferences() },
                                        label = { Text("毛玻璃") },
                                        leadingIcon = if (viewModel.blurMode == 0) {
                                            { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    FilterChip(
                                        selected = viewModel.blurMode == 1,
                                        onClick = { viewModel.blurMode = 1; viewModel.savePreferences() },
                                        label = { Text("高斯模糊") },
                                        leadingIcon = if (viewModel.blurMode == 1) {
                                            { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(50)
                                    )
                                }

                                Text("模糊强度", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                Slider(
                                    value = animatedBlur,
                                    onValueChange = {
                                        viewModel.globalBlurRadius = it
                                        viewModel.savePreferences()
                                    },
                                    valueRange = 0f..100f,
                                    steps = 9,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("0", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${animatedBlur}dp", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("100", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = viewModel.bannerImageUri ?: "https://picsum.photos/1200/600",
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(
                                                if (viewModel.isBlurEnabled && viewModel.isCustomBackgroundEnabled) Modifier.blur(
                                                    if (viewModel.blurMode == 0) animatedBlur.dp
                                                    else (animatedBlur * 2f).dp
                                                )
                                                else Modifier
                                            )
                                    )
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(12.dp),
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(35.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(35.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(20.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                CollapsibleCard(
                    title = "界面圆角",
                    subtitle = "拖拽滑块调节",
                    expanded = viewModel.isCornerExpanded,
                    onToggle = {
                        viewModel.isCornerExpanded = !viewModel.isCornerExpanded
                        viewModel.savePreferences()
                    },
                    cornerDp = cornerDp,
                    backdrop = backdrop
                ) {
                    Column {
                        Card(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(animatedCorner.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(animatedCorner.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    "${animatedCorner}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = viewModel.cornerRadius,
                            onValueChange = {
                                viewModel.cornerRadius = it
                                viewModel.savePreferences()
                            },
                            valueRange = 4f..32f,
                            steps = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("4dp", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("32dp", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                CollapsibleCard(
                    title = "横幅图片",
                    subtitle = "拖拽调整焦距",
                    expanded = viewModel.isBannerExpanded,
                    onToggle = {
                        viewModel.isBannerExpanded = !viewModel.isBannerExpanded
                        viewModel.savePreferences()
                    },
                    cornerDp = cornerDp,
                    backdrop = backdrop
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .pointerInput(Unit) {
                                coroutineScope {
                                    detectDragGestures(
                                        onDragEnd = { viewModel.savePreferences() }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        viewModel.bannerBiasX = (viewModel.bannerBiasX - dragAmount.x / 400f).coerceIn(-1f, 1f)
                                        viewModel.bannerBiasY = (viewModel.bannerBiasY - dragAmount.y / 400f).coerceIn(-1f, 1f)
                                    }
                                }
                            },
                        shape = RoundedCornerShape(cornerDp)
                    ) {
                        AsyncImage(
                            model = tempBannerUri ?: viewModel.bannerImageUri,
                            contentDescription = "Banner Preview",
                            contentScale = ContentScale.Crop,
                            alignment = BiasAlignment(viewModel.bannerBiasX, viewModel.bannerBiasY),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = {
                            bannerPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .bounceClick(shape = RoundedCornerShape(50)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("选择横幅", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                CollapsibleCard(
                    title = "背景壁纸",
                    subtitle = "启用背景后下方预览",
                    expanded = viewModel.isBackgroundExpanded,
                    onToggle = {
                        viewModel.isBackgroundExpanded = !viewModel.isBackgroundExpanded
                        viewModel.savePreferences()
                    },
                    cornerDp = cornerDp,
                    backdrop = backdrop
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(cornerDp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Wallpaper, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("启用背景", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Switch(
                                    checked = viewModel.isCustomBackgroundEnabled,
                                    onCheckedChange = {
                                        viewModel.isCustomBackgroundEnabled = it
                                        viewModel.savePreferences()
                                    },
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (viewModel.isCustomBackgroundEnabled) Icons.Rounded.Check else Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                            tint = if (viewModel.isCustomBackgroundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(previewHeight)
                                    .clip(RoundedCornerShape(12.dp))
                                    .pointerInput(Unit) {
                                        coroutineScope {
                                            detectDragGestures(
                                                onDragEnd = { viewModel.savePreferences() }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                viewModel.backgroundBiasX = (viewModel.backgroundBiasX - dragAmount.x / 400f).coerceIn(-1f, 1f)
                                                viewModel.backgroundBiasY = (viewModel.backgroundBiasY - dragAmount.y / 400f).coerceIn(-1f, 1f)
                                            }
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = tempBgUri ?: viewModel.customBackgroundUri ?: viewModel.backgroundImageUri,
                                    contentDescription = "Background Preview",
                                    contentScale = ContentScale.Crop,
                                    alignment = BiasAlignment(viewModel.backgroundBiasX, viewModel.backgroundBiasY),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        backgroundPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp).bounceClick(shape = RoundedCornerShape(50)),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("选择背景", fontSize = 12.sp)
                                }
                                if (viewModel.customBackgroundUri != null || tempBgUri != null) {
                                    FilledTonalButton(
                                        onClick = {
                                            tempBgUri = null
                                            viewModel.customBackgroundUri = null
                                            viewModel.backgroundBiasX = 0f
                                            viewModel.backgroundBiasY = 0f
                                            viewModel.savePreferences()
                                            AppToast.show("已移除自定义背景")
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp).bounceClick(shape = RoundedCornerShape(50)),
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("移除背景", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cornerDp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    viewModel.isPaletteExpanded = !viewModel.isPaletteExpanded
                                    viewModel.savePreferences()
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("主题色彩", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("选择配色方案", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = if (viewModel.isPaletteExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AnimatedVisibility(
                            visible = viewModel.isPaletteExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                fixedColors.chunked(2).forEachIndexed { chunkIndex, pairs ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        pairs.forEachIndexed { innerIndex, (name, color) ->
                                            val idx = chunkIndex * 2 + innerIndex
                                            val isSelected = viewModel.themeIndex == idx
                                            val itemBg = if (color == Color.Transparent) MaterialTheme.colorScheme.primary else color
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                                    )
                                                    .bounceClick(shape = RoundedCornerShape(14.dp)) {
                                                        viewModel.themeIndex = idx
                                                                                                                viewModel.keyColor = if (idx == 11) 0 else itemBg.toArgb()
                                                        viewModel.savePreferences()
                                                    }
                                                    .padding(10.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(itemBg))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = name,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun CollapsibleCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    cornerDp: Dp,
    backdrop: Backdrop,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
                        .then(
                if (LocalMd3Style.current)
                    Modifier.background(MaterialTheme.colorScheme.surfaceBright, RoundedRectangle(24f.dp))
                else Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(24f.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(16f.dp.toPx(), 32f.dp.toPx())
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = 0.5f)
                    }
                )
            )
            .clip(RoundedRectangle(24f.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onToggle() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = Motion.enter()) + fadeIn(Motion.enter()),
            exit = shrinkVertically(animationSpec = Motion.exit()) + fadeOut(Motion.exit())
        ) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
                content()
            }
        }
    }
}

/**
 * 歌曲长按操作面板。
 *
 * 搜索页（SongItem）与推荐页共用同一份实现，保证两处外观与行为完全一致：
 * 收藏 / 移出收藏、添加进歌单、下载（含进度与删除下载）。
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun SongLongPressSheet(
    song: Song,
    isInFavorites: Boolean,
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onRequestAddToPlaylist: (Song) -> Unit,
    onRemoveFromFavorites: (() -> Unit)? = null
) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    // 无封面兜底：统一封面占位图铺满（原为随机网图）
                    Image(
                        painter = painterResource(R.drawable.fallback_cover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (song.picUrl.isNotEmpty()) {
                        AsyncImage(
                            model = song.picUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    song.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (song.album.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        song.album,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(24.dp))

                if (isInFavorites && onRemoveFromFavorites != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        onClick = {
                            onRemoveFromFavorites()
                            onDismiss()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                "移出收藏",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        onClick = {
                            viewModel.addToFavorites(song)
                            onDismiss()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.FavoriteBorder,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                "收藏到列表",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        onClick = {
                            onDismiss()
                            onRequestAddToPlaylist(song)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlaylistAdd, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(14.dp))
                            Text("添加进歌单", fontWeight = FontWeight.Medium, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                var isDownloading by remember { mutableStateOf(false) }
                var downloadProgress by remember { mutableFloatStateOf(0f) }
                val isDownloaded = viewModel.isSongDownloaded(song.id)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    onClick = {
                        if (isDownloaded) {
                            viewModel.deleteDownloadedSong(song.id)
                            onDismiss()
                        } else if (!isDownloading) {
                            isDownloading = true
                            viewModel.downloadSong(
                                song = song,
                                onProgress = { progress -> downloadProgress = progress },
                                onComplete = { success ->
                                    isDownloading = false
                                    if (success) onDismiss()
                                }
                            )
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isDownloading) {
                            // downloadProgress < 0 表示"拿不到总大小"，用不确定进度转圈
                            if (downloadProgress < 0f) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                CircularProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Icon(
                                if (isDownloaded) Icons.Rounded.Delete else Icons.Rounded.Download,
                                null,
                                tint = if (isDownloaded) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            if (isDownloaded) "删除下载"
                            else if (isDownloading) {
                                if (downloadProgress < 0f) "下载中…"
                                else "下载中 ${(downloadProgress * 100).toInt()}%"
                            } else "下载音乐",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = if (isDownloaded) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongItem(
    song: Song,
    onLongPress: ((Song) -> Unit)? = null,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    viewModel: PlayerViewModel,
    backdrop: Backdrop,
    isInFavorites: Boolean = false,
    onRemoveFromFavorites: (() -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "SongPressScale"
    )

    val iconBgColor by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "IconBg"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.primary,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "IconTint"
    )

    key(song.id) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = pressedScale
                    scaleY = pressedScale
                }
                .md3OrGlassBackdrop(
                    backdrop = backdrop,
                    md3Shape = RoundedRectangle(24f.dp),
                    glassShape = { RoundedRectangle(24f.dp) },
                    glassEffects = {
                        vibrancy()
                        blur(6f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    glassHighlight = Highlight.Default.copy(alpha = 0.6f)
                )
                .clip(RoundedRectangle(24f.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showBottomSheet = true },
                        onTap = { onClick() }
                    )
                }
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedRectangle(16f.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    // Fallback layer: stays visible when the cover fails to load
                    // (Coil draws nothing on error), so the tile is never blank.
                    Image(
                        painter = painterResource(R.drawable.fallback_cover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (song.picUrl.isNotEmpty()) {
                        AsyncImage(
                            model = song.picUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        // 版本标签（原唱/翻唱/DJ版/Live/伴奏/纯音乐）已按用户要求**不再显示**：
                        // 它经常标错，反而误导。parseVersionTag 仍保留，仅用于排序（sortSearchResults）。
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.CenterEnd) {
                    AnimatedContent(
                        targetState = isCurrentSong && isPlaying,
                        transitionSpec = {
                            (fadeIn(Motion.enter()) +
                                    scaleIn(initialScale = 0.7f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f))) togetherWith
                                    (fadeOut(Motion.exit()) +
                                            scaleOut(targetScale = 0.7f, animationSpec = Motion.exit()))
                        },
                        label = "EqualizerSwitch"
                    ) { showBars ->
                        if (showBars) {
                            LiveAudioEqualizerBars(tintColor = MaterialTheme.colorScheme.primary)
                        } else {
                            if (song.duration > 0) {
                                Text(
                                    text = formatDuration(songDurationMillis(song.duration)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        SongLongPressSheet(
            song = song,
            isInFavorites = isInFavorites,
            viewModel = viewModel,
            onDismiss = { showBottomSheet = false },
            onRequestAddToPlaylist = {
                if (onAddToPlaylist != null) onAddToPlaylist.invoke(it) else showAddSheet = true
            },
            onRemoveFromFavorites = onRemoveFromFavorites
        )
    }

    if (showAddSheet) {
        val ctxA = LocalContext.current
        AddToPlaylistSheet(
            song = song,
            onDismiss = { showAddSheet = false },
            onAdded = { pl ->
                val ps = PlaylistSong(id = song.id, name = song.name, artist = song.artist, picUrl = song.picUrl)
                PlaylistStore.savePlaylist(ctxA, pl.copy(songs = pl.songs + ps))
                showAddSheet = false
                AppToast.show("已加入歌单")
            }
        )
    }
}

@Composable
fun LiveAudioEqualizerBars(tintColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "Equalizer")

    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(680, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = Modifier.size(width = 20.dp, height = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(bar1)
                .background(tintColor, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(bar2)
                .background(tintColor, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(bar3)
                .background(tintColor, RoundedCornerShape(3.dp))
        )
    }
}

@Composable
fun MD3StyleBottomSheetConsole(
    currentSong: Song?,
    isPlaying: Boolean,
    isLooping: Boolean,
    currentPosition: Long,
    duration: Long,
    lyrics: LyricData,
    currentLyricIndex: Int,
    viewModel: PlayerViewModel,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleLoop: () -> Unit,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onFullScreen: () -> Unit,
    isLandscape: Boolean,
    backdrop: Backdrop
) {
    val currentLyricText = remember(lyrics, currentLyricIndex) {
        if (currentLyricIndex >= 0 && currentLyricIndex < lyrics.times.size) {
            val time = lyrics.times[currentLyricIndex]
            val text = lyrics.lyrics[time] ?: ""
            val trans = lyrics.translations[time]
            if (!trans.isNullOrEmpty()) "$text\n$trans" else text
        } else if (lyrics.times.isNotEmpty() || lyrics.lyrics.isNotEmpty()) {
            // 有歌词、只是还没唱到第一句（前奏/间奏）→ 不是"纯音乐"
            "等待前奏…"
        } else if (lyrics.instrumental) {
            "纯音乐，请欣赏"
        } else {
            "暂无歌词"
        }
    }

    val maxProgressValue = if (duration > 0) duration.toFloat() else 1f
    val currentFraction = (currentPosition.toFloat() / maxProgressValue).coerceIn(0f, 1f)

    val consoleHeight by animateDpAsState(
        targetValue = if (isExpanded) 190.dp else 76.dp,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 320f),
        label = "ConsoleHeight"
    )

    val progressScale by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0.85f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 280f),
        label = "ProgressScale"
    )
    val progressAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isExpanded) 250 else 120,
            delayMillis = if (isExpanded) 80 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "ProgressAlpha"
    )

    // 容器圆角从 32dp 收到 24dp：76dp 高的胶囊配 32dp 圆角时，左右上下的圆角会"吃"进内容区，
    // 歌名/歌手会被遮掉一点（用户反馈）。同时内容再加一层内缩，保证文字完整。
    val consoleShape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isLandscape) 0.dp else 16.dp)
            .height(consoleHeight)
            .then(
                if (LocalMd3Style.current)
                    Modifier.background(MaterialTheme.colorScheme.surfaceBright, consoleShape)
                else Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { consoleShape },
                    effects = {
                        val minDimension = size.minDimension
                        vibrancy()
                        blur(6f.dp.toPx())
                        lens(
                            refractionHeight = 0.35f * minDimension * 0.5f,
                            refractionAmount = 0.6f * minDimension,
                            depthEffect = true,
                            chromaticAberration = false
                        )
                    },
                    highlight = { Highlight.Plain },
                    shadow = { Shadow(alpha = 0.15f) },
                    onDrawSurface = { }
                )
            )
            .clip(consoleShape)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    val cover = currentSong?.picUrl ?: ""
                    if (cover.isNotEmpty()) {
                        AsyncImage(
                            model = cover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // 无封面兜底：统一封面占位图铺满（原为 MusicNote 图标）
                        Image(
                            painter = painterResource(R.drawable.fallback_cover),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    // 标题块再内缩一点：圆角容器边缘不会压到歌名的首/末字
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onFullScreen() }
                        .padding(start = 12.dp, end = 10.dp, top = 2.dp, bottom = 2.dp)
                ) {
                    Text(
                        currentSong?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    AnimatedContent(
                        targetState = isExpanded,
                        transitionSpec = {
                            (fadeIn(Motion.enter()) +
                                    slideInVertically(
                                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 280f)
                                    ) { it / 3 }) togetherWith
                                    (fadeOut(Motion.exit()) +
                                            slideOutVertically(Motion.exit()) { -it / 3 })
                        },
                        label = "SubtitleSwitch"
                    ) { expanded ->
                        if (expanded) {
                            AnimatedContent(
                                targetState = currentLyricText,
                                transitionSpec = {
                                    fadeIn(Motion.enter()) togetherWith
                                            fadeOut(Motion.exit()) using
                                            SizeTransform(clip = false)
                                },
                                label = "LyricLine"
                            ) { text ->
                                Text(
                                    text = text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp
                                )
                            }
                        } else {
                            Text(
                                currentSong?.artist ?: "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (isLooping) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        null,
                        tint = if (isLooping)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onFullScreen, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Fullscreen, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onExpandChange(!isExpanded) }, modifier = Modifier.size(36.dp)) {
                    Icon(if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = spring(dampingRatio = 0.80f, stiffness = 200f)) +
                        expandVertically(
                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 300f),
                            expandFrom = Alignment.Top,
                            clip = false
                        ),
                exit = shrinkVertically(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                    shrinkTowards = Alignment.Top,
                    clip = false
                ) + fadeOut(Motion.exit())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clipToBounds()) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.SkipPrevious, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        val expandedPlayInteractionSource = remember { MutableInteractionSource() }
                        val expandedPlayIsPressed by expandedPlayInteractionSource.collectIsPressedAsState()
                        val expandedPlayScale by animateFloatAsState(
                            targetValue = if (expandedPlayIsPressed) 0.84f else 1f,
                            animationSpec = spring(dampingRatio = 0.35f, stiffness = 550f),
                            label = "ExpandedPlayPress"
                        )
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .graphicsLayer { scaleX = expandedPlayScale; scaleY = expandedPlayScale }
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable(interactionSource = expandedPlayInteractionSource, indication = null) { onPlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.SkipNext, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = progressScale
                                scaleY = progressScale
                                alpha = progressAlpha
                            }
                            .padding(start = 14.dp, end = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                        if (ProgressStylePrefs.waveEnabled(androidx.compose.ui.platform.LocalContext.current)) {
                            TadpoleWaveSlider(
                                                        value = currentPosition.toFloat(),
                                                        onValueChange = { viewModel.seekTo(it.toLong()) },
                                                        valueRange = 0f..maxProgressValue,
                                                        modifier = Modifier.fillMaxWidth().height(28.dp),
                                                        activeColor = MaterialTheme.colorScheme.primary,
                                                        inactiveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                                        thumbColor = MaterialTheme.colorScheme.primary
                                                    )
                        } else {
                            Slider(
                                                        value = currentPosition.toFloat(),
                                                        onValueChange = { viewModel.seekTo(it.toLong()) },
                                                        valueRange = 0f..maxProgressValue,
                                                        modifier = Modifier.fillMaxWidth().height(28.dp),
                                                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                                    )
                        }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatDuration(currentPosition), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f), fontWeight = FontWeight.Medium)
                            Text(formatDuration(duration), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 把 Song.duration 统一成毫秒。
 *
 * 后端返回的 duration 是**秒**（例如 269），而本地/播放器侧拿到的是毫秒；
 * 之前列表直接把它当毫秒交给 formatDuration，于是整列都显示成 00:00。
 * 用一个阈值区分：<=10 小时按秒处理，否则视为已经是毫秒。
 */
/**
 * 搜索结果排序。
 *
 * mode: 0 原唱优先（默认） / 1 热度 / 2 时长 / 3 默认（保持原顺序）
 *
 * 「原唱优先」的有序键（从强到弱，全部显式给出 → 结果确定、与并行抓取的到达顺序无关）：
 *  1) 版本修饰：parseVersionTag == null → 0，否则 1（无 DJ版/Live/翻唱/伴奏/纯音乐 的在前）
 *  2) 艺人命中：搜索词里包含该结果的歌手名 → 0，否则 1（用户可能在找这个歌手）
 *  3) 时长有效性：duration > 0 → 0，否则 1（0/未知通常是残缺项）
 *  4) 来源优先级：kuwo > migu > kugou > qq > netease > bilibili
 *  5) 热度降序
 *  6) 歌名更短（更接近原版标题）
 *  7) id 兜底，保证完全确定性
 *
 * 注意：故意**不再**用"和搜索词完全同名"当第一键 —— 实测翻唱版常常直接沿用原歌名，
 * 反而会被顶到原唱前面（用户反馈「好的歌永远只会在下面」）。
 */
/**
 * 是不是"垃圾版本"（DJ版 / 翻唱 / 混音 / 加速降调 / Live / 伴奏 …）。
 *
 * 为什么不能只看歌名：中文平台大量垃圾版本的版本信息写在**歌手名**里，
 * 例如 `虞兮叹 - 山河DJ`、`虞兮叹 - 混音太子`、`xxx - DJ阿智`。
 * 它们的歌名干干净净，只按歌名判定就会被当成"正常版本"顶在最上面，
 * 而真正的原唱被挤到最底下。所以歌手名也要拆词检查。
 */
/**
 * 过滤明显不是「音乐」的搜索结果。
 *
 * 网易云曲库会混入有声书 / 说书 / 评书 / 电台 / 播客 以及「第 1005 章」这类章节条目，
 * 用户搜的是音乐，这些必须挡掉（表现为「搜出来一堆说书的」）。
 */
fun isLikelyMusic(song: Song): Boolean {
    val hay = (song.name + " " + song.artist + " " + song.album).lowercase()
    val junk = listOf(
        "有声", "听书", "说书", "评书", "相声", "广播剧", "播客", "电台", "播音",
        "朗读", "诵读", "连载", "完结", "全集", "更新至", "精讲", "解读", "讲书",
        "拆书", "共读", "书评", "导读", "讲座", "公开课", "百家讲坛",
        "睡前故事", "儿童故事", "演播", "podcast"
    )
    if (junk.any { hay.contains(it) }) return false
    // 第123章 / 第12集 / 第3回 这类章节型条目
    if (Regex("第\\s*[0-9零一二三四五六七八九十百千]{1,6}\\s*[章节集回期话卷]")
            .containsMatchIn(song.name)) return false
    return true
}

fun isJunkVersion(song: Song): Boolean {
    // 歌名/专辑：沿用 parseVersionTag（只在"版本标注可能出现的位置"匹配，不会误伤 Believe/Discovery）
    if (parseVersionTag(song.name, song.album) != null) return true
    val artist = song.artist.lowercase()
    if (artist.isBlank()) return false
    // 中文版本/演绎标记。注意**不能**放"古风/国风/空灵/小曲"这类曲风词：
    // 它们是风格不是版本，歌手叫「古风XX」会被整体判成垃圾版本，反而把正常歌挤下去。
    val junkCn = listOf(
        "混音", "翻唱", "加速", "减速", "降调", "升调", "抖音", "铃声",
        "伴奏", "纯音乐", "钢琴", "吉他", "童声", "女声版", "男声版",
        "合唱", "清唱", "戏腔", "串烧", "改版", "改编", "组曲", "重制"
    )
    // 拉丁缩写必须按**词**匹配：之前的 t.contains("dj") 会把 Adjoa 之类正常歌手误杀。
    // 词首不能是字母/数字（山河DJ ✔、Adjoa ✘）；词尾允许数字（DJ2024 ✔）。
    val junkWordRe = Regex("(?i)(?<![a-z0-9])(dj|remix|cover|sped|slowed|8d)(?![a-z])")
    val tokens = artist.split('/', '、', '&', ',', '，', ';', '；', '-', '_', ' ', ' ')
        .map { it.trim() }.filter { it.isNotEmpty() }
    return tokens.any { t -> junkCn.any { t.contains(it) } || junkWordRe.containsMatchIn(t) }
}

fun sortSearchResults(list: List<Song>, mode: Int, keyword: String = ""): List<Song> {
    if (list.size <= 1) return list
    val srcRank = mapOf(
        "kuwo" to 0, "migu" to 1, "kugou" to 2, "qq" to 3, "netease" to 4, "bilibili" to 5
    )
    val kw = keyword.trim().lowercase()
    // 搜索词里是否点名了这位歌手（歌手可能是 "A/B"、"A、B"、"A & B" 这种多值）。
    fun artistHit(s: Song): Int {
        if (kw.isBlank()) return 1
        val hit = s.artist.split('/', '、', '&', ',', '，', ';', '；')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .any { kw.contains(it) }
        return if (hit) 0 else 1
    }
    return when (mode) {
        1 -> list.sortedWith(compareByDescending<Song> { it.heat }.thenBy { it.id })
        2 -> list.sortedWith(
            compareBy<Song> { if (it.duration > 0) it.duration else Long.MAX_VALUE }.thenBy { it.id }
        )
        3 -> list
        4 -> list.reversed()
        else -> list.sortedWith(
            compareBy(
                // 1) 无版本修饰的更可能是原唱/正常版本
                { if (!isJunkVersion(it)) 0 else 1 },
                // 2) 搜索词点名的歌手优先
                { artistHit(it) },
                // 3) 时长已知的优先
                { if (it.duration > 0) 0 else 1 },
                // 4) 来源优先级（kuwo/咪咕/酷狗的华语原唱覆盖最好）
                { srcRank[it.source.lowercase()] ?: 9 },
                // 5) 热度降序
                { -it.heat },
                // 6) 歌名更短（越短越接近原版标题）
                { it.name.length },
                // 7) 最终兜底，保证排序完全确定
                { it.id }
            )
        )
    }
}

fun songDurationMillis(duration: Long): Long =
    if (duration in 1..36_000L) duration * 1000L else duration

/**
 * 切歌时封面不要闪空。
 *
 * 换源解析/切下一首的瞬间，currentSong 的 picUrl 可能短暂为空，
 * 直接渲染就会"封面消失一下"。这里记住上一张非空封面兜底，
 * 等新封面到位再替换。
 */
@Composable
fun rememberStableCover(picUrl: String): String {
    var lastCover by remember { mutableStateOf(picUrl) }
    if (picUrl.isNotEmpty()) lastCover = picUrl
    return picUrl.ifEmpty { lastCover }
}

/**
 * 是不是"原文文字"。
 *
 * 之前只按 CJK 汉字（0x4E00-0x9FFF）判断，导致**日文假名不算原文**：
 * 日文歌的原文被当成"外来行"，罗马音反而胜出，歌词就变成了拼音。
 * 这里把假名（平假名/片假名）和 CJK 扩展 A 一起算作原文。
 *
 * 韩文同理：Hangul 音节 / Jamo / 兼容字母 也必须算原文，
 * 否则韩文歌会选到罗马音（RR）那一行。
 * 所有「原文判定」的地方（歌词择优、歌词归属校验、罗马音过滤）都走这个函数，
 * 因此只需在这里补齐区间即可全链路生效。
 */
internal fun isNativeText(ch: Char): Boolean {
    val c = ch.code
    return c in 0x4E00..0x9FFF ||   // CJK 汉字
            c in 0x3400..0x4DBF ||  // CJK 扩展 A
            c in 0x3040..0x309F ||  // 平假名
            c in 0x30A0..0x30FF ||  // 片假名
            c in 0xFF66..0xFF9F ||  // 半角片假名
            c in 0xAC00..0xD7AF ||  // 韩文 Hangul 音节（가～힣）
            c in 0x1100..0x11FF ||  // 韩文 Hangul Jamo（初声/中声/终声）
            c in 0x3130..0x318F     // 韩文兼容字母（ㄱ～ㅎ）
}

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
fun HistoryScreen(
    viewModel: PlayerViewModel,
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val scope = rememberCoroutineScope()

    val historyList by viewModel.historyList.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    val filteredHistory = remember(historyList, searchText) {
        if (searchText.isBlank()) historyList
        else historyList.filter { song ->
            song.name.contains(searchText, ignoreCase = true) ||
            song.artist.contains(searchText, ignoreCase = true) ||
            song.album.contains(searchText, ignoreCase = true)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (viewModel.isCustomBackgroundEnabled && viewModel.isBlurEnabled) {
                AsyncImage(
                    model = viewModel.customBackgroundUri ?: viewModel.backgroundImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(viewModel.backgroundBiasX, viewModel.backgroundBiasY),
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(
                            if (viewModel.blurMode == 0) viewModel.globalBlurRadius.dp
                            else (viewModel.globalBlurRadius * 2f).dp
                        )
                )
            }
            val collapseBehavior = androidx.compose.material3.TopAppBarDefaults
                .exitUntilCollapsedScrollBehavior(androidx.compose.material3.rememberTopAppBarState())
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                androidx.compose.material3.LargeTopAppBar(
                    title = {
                        Text("历史播放", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, "返回", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    scrollBehavior = collapseBehavior,
                    colors = androidx.compose.material3.TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )

                // 搜索框独立于列表存在性：清空历史时平滑收起，不再"瞬间消失"
                AnimatedVisibility(
                    visible = historyList.isNotEmpty(),
                    enter = fadeIn(Motion.enter()) +
                            expandVertically(Motion.enter(), expandFrom = Alignment.Top),
                    exit = fadeOut(Motion.exit()) +
                            shrinkVertically(Motion.exit(), shrinkTowards = Alignment.Top)
                ) {
                    GenericSearchBar(
                        searchText = searchText,
                        isSearchActive = isSearchActive,
                        onSearchTextChange = { searchText = it },
                        onSearchToggle = { isSearchActive = it },
                        placeholder = "搜索播放历史..."
                    )
                }

                if (historyList.isNotEmpty()) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    viewModel.clearPlayHistory()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Rounded.DeleteSweep,
                                null,
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "清空历史",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (filteredHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (searchText.isNotBlank()) Icons.Rounded.SearchOff else Icons.Rounded.History,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (searchText.isNotBlank()) "没有找到匹配的歌曲"
                                else "暂无播放历史",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().nestedScroll(collapseBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(
                            items = filteredHistory,
                            key = { index, s -> "history_${s.id}_$index" }
                        ) { index, song ->
                            SongItem(
                                song = song,
                                isCurrentSong = song.id == currentSong?.id,
                                isPlaying = isPlaying,
                                isFirst = index == 0,
                                isLast = index == filteredHistory.size - 1,
                                onClick = { viewModel.playSong(song) },
                                viewModel = viewModel,
                                backdrop = backdrop,
                                isInFavorites = false,
                                onRemoveFromFavorites = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GenericSearchBar(
    searchText: String,
    isSearchActive: Boolean,
    onSearchTextChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    placeholder: String = "搜索..."
) {
    val focusManager = LocalFocusManager.current

    val borderColor by animateColorAsState(
        targetValue = if (isSearchActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "searchBorderColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { onSearchTextChange(it) },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    onSearchToggle(focusState.isFocused || searchText.isNotEmpty())
                },
            label = {
                Text(
                    placeholder,
                    fontSize = if (isSearchActive) 12.sp else 14.sp,
                    color = if (isSearchActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Search,
                    null,
                    tint = if (isSearchActive)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = searchText.isNotEmpty(),
                    enter = scaleIn(animationSpec = Motion.enter()) + fadeIn(Motion.enter()),
                    exit = scaleOut(animationSpec = Motion.exit()) + fadeOut(Motion.exit())
                ) {
                    IconButton(
                        onClick = {
                            onSearchTextChange("")
                            onSearchToggle(false)
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            )
        )
    }
}

val LocalMd3Style = androidx.compose.runtime.staticCompositionLocalOf { false }
val LocalMd3Translucent = androidx.compose.runtime.staticCompositionLocalOf { false }
val LocalMd3Backdrop = androidx.compose.runtime.staticCompositionLocalOf<com.kyant.backdrop.Backdrop?> { null }

@androidx.compose.runtime.Composable
fun Modifier.md3OrGlassBackdrop(
    backdrop: com.kyant.backdrop.Backdrop,
    md3Shape: androidx.compose.ui.graphics.Shape,
    glassShape: () -> androidx.compose.ui.graphics.Shape,
    glassEffects: com.kyant.backdrop.BackdropEffectScope.() -> Unit,
    glassHighlight: com.kyant.backdrop.highlight.Highlight = com.kyant.backdrop.highlight.Highlight.Plain,
    glassShadow: (() -> com.kyant.backdrop.shadow.Shadow)? = null,
    glassOnDrawSurface: (androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit)? = null,
    md3Container: Color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceBright,
): Modifier =
    if (LocalMd3Style.current) {
        this.background(md3Container, md3Shape)
    } else {
        this.drawBackdrop(
            backdrop = backdrop,
            shape = glassShape,
            effects = glassEffects,
            highlight = { glassHighlight },
            shadow = glassShadow,
            onDrawSurface = glassOnDrawSurface,
        )
    }

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val conf = Configuration(newBase.resources.configuration)
        conf.fontScale = 1.0f                       // 忽略系统字体缩放，软件始终保持原大小
        val prefs = newBase.getSharedPreferences("app_display", Context.MODE_PRIVATE)
        val frozenDpi = prefs.getInt("density_dpi", -1)
        if (frozenDpi > 0) conf.densityDpi = frozenDpi  // 冻结显示密度（首次启动时由 App 捕获）
        applyOverrideConfiguration(conf)
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FloatingLyricsPrefs.isEnabled(this)) FloatingLyricsService.start(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        try {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        } catch (_: Exception) { }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        com.google.android.gms.ads.MobileAds.initialize(this)

        setContent {
    val viewModel: PlayerViewModel = viewModel()
        val ksColorMode = ColorMode.fromValue(viewModel.colorMode)
    val isDark = viewModel.isForceDarkMode || ksColorMode.isDark ||
            (ksColorMode.isSystem && isSystemInDarkTheme())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

            val ksPaletteStyle = try { PaletteStyle.valueOf(viewModel.colorStyle) } catch (_: Exception) { PaletteStyle.TonalSpot }
    val ksColorSpec = try { ColorSpec.SpecVersion.valueOf(viewModel.colorSpec) } catch (_: Exception) { ColorSpec.SpecVersion.SPEC_2025 }
    val baseColorScheme = rememberKernelSUColorScheme(
        seedColor = if (viewModel.keyColor == 0) Color.Unspecified else Color(viewModel.keyColor),
        isDark = isDark,
        isAmoled = ksColorMode.isAmoled,
        paletteStyle = ksPaletteStyle,
        colorSpec = ksColorSpec,
    )

    val customAnimatedColorSchemeBase = baseColorScheme.animateAsState()
        val customAnimatedColorScheme = if (viewModel.whiteTextMode && !viewModel.md3Style) {
        // 「只文字变白」：只改文本/描边类语义色，primary（图标 tint 主用色）保持原样
        customAnimatedColorSchemeBase.copy(
            onSurface = Color.White,
            onSurfaceVariant = Color.White.copy(alpha = 0.85f),
            onBackground = Color.White,
            onPrimaryContainer = Color.White,
            onSecondaryContainer = Color.White,
            onTertiaryContainer = Color.White,
            outline = Color.White.copy(alpha = 0.6f),
            outlineVariant = Color.White.copy(alpha = 0.35f),
        )
    } else customAnimatedColorSchemeBase

    // 把 App 动态颜色同步给悬浮歌词（进度条/悬浮窗跟随动态色）
    androidx.compose.runtime.LaunchedEffect(customAnimatedColorScheme.primary) {
        runCatching {
            val pc = customAnimatedColorScheme.primary
            FloatingLyricsPrefs.setAccentColor(
                context,
                android.graphics.Color.argb(
                    (pc.alpha * 255f).toInt().coerceIn(0, 255),
                    (pc.red * 255f).toInt().coerceIn(0, 255),
                    (pc.green * 255f).toInt().coerceIn(0, 255),
                    (pc.blue * 255f).toInt().coerceIn(0, 255)
                )
            )
        }
    }

    SideEffect {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(colorScheme = customAnimatedColorScheme, typography = KsuTypography) {
        var showSplash by remember { mutableStateOf(true) }

                val appPrefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
        var showStyleGuide by remember { mutableStateOf(false) }
        val guidePrefs = remember { context.getSharedPreferences("guide_prefs", android.content.Context.MODE_PRIVATE) }
        LaunchedEffect(Unit) {
            while (!guidePrefs.getBoolean("guide_done", false)) {
                kotlinx.coroutines.delay(300)
            }
            // 引导走完：把「引导已完成」同步给推荐页，音乐偏好选择器只在
            // 引导 + 风格都选完之后才弹（否则会盖在闪屏/引导上，用户反馈的一进应用就弹）
            OnboardingState.refresh(context)
            if (!appPrefs.getBoolean("style_guide_shown", false)) {
                showStyleGuide = true
            }
        }

        // 引导第 5 项（风格切换）：把选中的风格 / 主题色写进 ViewModel 并持久化。
        // 引导 UI 在启动闪屏里（logs.kt 的 GuideScreen），拿不到 ViewModel，所以从这里注入回调。
        LaunchedEffect(Unit) {
            GuideStyleBridge.apply = { liquid, keyColor, finish ->
                // 液态玻璃风格依赖 RuntimeShader/AGSL（API 33+）：低版本强制回退兼容风格
                val liquidAllowed = liquid && liquidGlassSupported
                viewModel.md3Style = !liquidAllowed
                if (liquidAllowed) viewModel.useFloatingNavBar = true
                else if (!liquidGlassSupported) {
                    // 低版本：玻璃导航栏一并禁用并持久化
                    viewModel.useFloatingNavBar = false
                    viewModel.navBarLiquidGlass = false
                }
                viewModel.keyColor = keyColor
                viewModel.savePreferences()
                if (finish) {
                    // 风格已在引导里选完 → 不再单独弹风格选择页
                    appPrefs.edit().putBoolean("style_guide_shown", true).apply()
                    OnboardingState.refresh(context)
                }
            }
        }

        // 风格页收尾（onSelect / 「稍后我再看」共用）：应用风格 + 标记已看过 + 通知推荐页
        val finishStyleGuide: (Boolean) -> Unit = { liquid ->
            // 液态玻璃风格依赖 RuntimeShader/AGSL（API 33+）：低版本强制回退兼容风格
            val liquidAllowed = liquid && liquidGlassSupported
            viewModel.md3Style = !liquidAllowed
            if (liquidAllowed) {
                viewModel.useFloatingNavBar = true
            } else if (!liquidGlassSupported) {
                // 低版本：玻璃导航栏一并禁用并持久化
                viewModel.useFloatingNavBar = false
                viewModel.navBarLiquidGlass = false
            }
            viewModel.savePreferences()
            appPrefs.edit().putBoolean("style_guide_shown", true).apply()
            OnboardingState.refresh(context)
            showStyleGuide = false
        }

        // 停服维护检查：进入主界面立刻查一次，之后每 5 分钟轮询一次。
        // 失败一律按正常处理（见 PlayerViewModel.checkMaintenance），绝不挡住使用。
        LaunchedEffect(Unit) {
            viewModel.checkMaintenance()
            while (true) {
                kotlinx.coroutines.delay(5 * 60 * 1000L)
                viewModel.checkMaintenance()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalMd3Style provides viewModel.md3Style,
                    LocalMd3Translucent provides false
                ) {
                    MainNavigationScreen(viewModel = viewModel)
                }
            }

                        androidx.compose.animation.AnimatedVisibility(
                visible = showSplash,
                enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(0)),
                exit = androidx.compose.animation.fadeOut(
                    androidx.compose.animation.core.tween(700, easing = FastOutSlowInEasing)
                )
            ) {
                SplashScreen(onFinished = {
                    showSplash = false
                    // 主界面可见后才允许弹音乐偏好选择器
                    OnboardingState.mainVisible.value = true
                })
            }

                        androidx.compose.animation.AnimatedVisibility(
                visible = showStyleGuide,
                enter = androidx.compose.animation.fadeIn(
                    androidx.compose.animation.core.tween(300, easing = FastOutSlowInEasing)
                ) + androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it / 12 },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 260f)
                ),
                exit = androidx.compose.animation.fadeOut(
                    androidx.compose.animation.core.tween(220, easing = FastOutSlowInEasing)
                ) + androidx.compose.animation.slideOutVertically(
                    targetOffsetY = { -it / 16 },
                    animationSpec = androidx.compose.animation.core.tween(220)
                )
            ) {
                StyleGuidePage(
                    currentLiquid = viewModel.useFloatingNavBar,
                    currentKeyColor = viewModel.keyColor,
                    onKeyColor = { c ->
                        viewModel.keyColor = c
                        viewModel.savePreferences()
                    },
                    onSelect = { liquid -> finishStyleGuide(liquid) },
                    // 「稍后我再看」：与 onSelect 同样的收尾，直接进主界面
                    onLater = { liquid -> finishStyleGuide(liquid) },
                    // 「立即前往导入」：StyleGuidePage 已先调用 onSelect 完成风格收尾，
                    // 这里补齐幂等收尾（标记已看过 + 通知推荐页），再请求打开歌单导入面板
                    onImportNow = {
                        appPrefs.edit().putBoolean("style_guide_shown", true).apply()
                        OnboardingState.refresh(context)
                        showStyleGuide = false
                        ImportBus.request()
                    },
                    rememberLast = viewModel.rememberLast.collectAsState().value,
                    onRememberLastChange = { on -> viewModel.setRememberLast(context, on) }
                )
            }


            // 应用内气泡提示：不再用系统 Toast，文案与语义保持不变
            val toastMessage by viewModel.toastMessage.collectAsState()
            LaunchedEffect(toastMessage) {
                toastMessage?.let { msg ->
                    AppToast.show(msg, long = true)
                    viewModel.clearToast()
                }
            }
            InAppToastHost()

            // 公告/更新的「何时弹」由下面两个 LaunchedEffect 用 onboardingDone 把关，
            // 所以这里只负责尽早把数据拉回来 —— 不要再等 onboardingDone：
            // 那是组合期捕获的常量，首次组合时恒为 false，会把请求硬推迟 60 秒，
            // 用户根本等不到（表现就是"后台发了公告/更新，客户端不显示"）。
            val onboardingDone = OnboardingState.guideDone.value && OnboardingState.styleChosen.value && OnboardingState.mainVisible.value
            LaunchedEffect(Unit) {
                delay(1000)
                viewModel.checkAnnouncement()
                viewModel.checkUpdate()
            }

            var showUpdateSheet by remember { mutableStateOf(false) }
            var showAnnouncementSheet by remember { mutableStateOf(false) }
            var isDownloading by remember { mutableStateOf(false) }
            var downloadProgress by remember { mutableFloatStateOf(0f) }

            // 串行处理两个弹层：先公告；用户关掉公告（showAnnouncement 置回 false）后再弹更新。
            // 拆成两个独立 LaunchedEffect 时它们会在同一帧一起触发、互相抢，导致更新把公告"顶掉"。
            LaunchedEffect(onboardingDone, viewModel.showAnnouncement, viewModel.showUpdate) {
                if (!onboardingDone) return@LaunchedEffect
                // 紧急停机（level >= 3）时不再自动弹公告 / 更新，避免盖在阻断页上面
                if ((viewModel.maintenance.value?.level ?: 0) >= 3) return@LaunchedEffect
                if (viewModel.showAnnouncement && viewModel.announcement.value != null) {
                    showAnnouncementSheet = true
                    return@LaunchedEffect
                }
                if (viewModel.showUpdate && viewModel.updateInfo.value != null) {
                    showUpdateSheet = true
                }
            }

            val backdrop = rememberLayerBackdrop { drawContent() }

            if (showAnnouncementSheet && viewModel.announcement.value != null) {
                val announcement = viewModel.announcement.value!!
                AppModalSheet(
                    onDismissRequest = {
                        showAnnouncementSheet = false
                        viewModel.showAnnouncement = false
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                            .navigationBarsPadding()
                    ) {
                        Icon(
                            Icons.Rounded.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            announcement.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            announcement.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                showAnnouncementSheet = false
                                viewModel.showAnnouncement = false
                            }) { Text("取消") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                showAnnouncementSheet = false
                                viewModel.showAnnouncement = false
                            }) { Text("我知道了") }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // ── 停服维护三档 UI（最上层）────────────────────────────────────
            // level 1：顶部细横幅（只占一条，不挡操作，可关，会话内不再显示）
            // level 2：居中弹窗（有关闭按钮 + 明显警告，可关，会话内不再显示）
            // level 3：全屏阻断页（不可关闭、不可操作）
            run {
                val maintenanceInfo by viewModel.maintenance.collectAsState()
                val ackedSignature by viewModel.maintenanceAcked.collectAsState()
                val info = maintenanceInfo
                if (info != null && info.level > 0) {
                    // 展示样式：服务端 maintenance.json 里的 style 说了算；
                    // 留空时按档位默认 —— level 1 = 顶部横幅，level >= 2 = 整屏维护页。
                    val style = info.style.trim().lowercase()
                    val wantBanner = style == "banner" || (style.isEmpty() && info.level == 1)
                    val wantDialog = style == "dialog"
                    when {
                        // 紧急停机：永远整屏、不可关，直到服务端降档
                        info.level >= 3 -> MaintenanceBlockScreen(info)
                        // 重要维护：默认整屏页（可返回/可关），也可被 style 强制成横幅或弹窗
                        wantBanner -> if (ackedSignature != info.signature()) {
                            MaintenanceTopBanner(info) { viewModel.ackMaintenance(info) }
                        }
                        wantDialog -> if (ackedSignature != info.signature()) {
                            MaintenanceNoticeDialog(info) { viewModel.ackMaintenance(info) }
                        }
                        else -> if (ackedSignature != info.signature()) {
                            MaintenanceBlockScreen(info, dismissible = true) {
                                viewModel.ackMaintenance(info)
                            }
                        }
                    }
                }
            }

            if (showUpdateSheet && viewModel.updateInfo.value != null) {
                val update = viewModel.updateInfo.value!!
                AppModalSheet(
                    onDismissRequest = {
                        if (!update.forceUpdate && !isDownloading) {
                            showUpdateSheet = false
                            viewModel.showUpdate = false
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    allowDismiss = !update.forceUpdate
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                            .navigationBarsPadding()
                    ) {
                        Icon(
                            Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = if (update.forceUpdate) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            if (update.forceUpdate) "重要安全更新" else "发现新版本",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "版本 ${update.version}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (update.forceUpdate) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                        )
                        if (update.forceUpdate) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    "此版本包含重要安全修复，必须更新后才能继续使用",
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            update.updateLog.replace("\\n", "\n"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        if (isDownloading) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TadpoleWaveProgress(
                                    progress = { downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "下载中 ${(downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!update.forceUpdate) {
                                    TextButton(onClick = {
                                        showUpdateSheet = false
                                        viewModel.showUpdate = false
                                    }) { Text("稍后再说") }
                                    Spacer(Modifier.width(8.dp))
                                }
                                Button(onClick = {
                                    isDownloading = true
                                    scope.launch(Dispatchers.IO) {
                                            try {
                                                val url = URL(update.updateUrl)
                                                val connection = url.openConnection() as HttpURLConnection
                                                connection.connect()
                                                val fileLength = connection.contentLength
                                                val inputStream = connection.inputStream
                                                val apkFile = File(cacheDir, "update.apk")
                                                val outputStream = apkFile.outputStream()
                                                val buffer = ByteArray(1024)
                                                var totalRead = 0L
                                                var read: Int
                                                while (inputStream.read(buffer).also { read = it } != -1) {
                                                    totalRead += read
                                                    outputStream.write(buffer, 0, read)
                                                    withContext(Dispatchers.Main) {
                                                        downloadProgress = if (fileLength > 0) totalRead.toFloat() / fileLength else 0f
                                                    }
                                                }
                                                outputStream.close()
                                                inputStream.close()
                                                connection.disconnect()
                                                withContext(Dispatchers.Main) {
                                                    isDownloading = false
                                                    showUpdateSheet = false
                                                    viewModel.showUpdate = false
                                                    try {
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                                        val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                            androidx.core.content.FileProvider.getUriForFile(
                                                                this@MainActivity,
                                                                "${packageName}.fileprovider",
                                                                apkFile
                                                            )
                                                        } else {
                                                            android.net.Uri.fromFile(apkFile)
                                                        }
                                                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        startActivity(intent)
                                                    } catch (e: Exception) {
                                                        AppToast.show("安装失败")
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    isDownloading = false
                                                    AppToast.show("下载失败")
                                                }
                                            }
                                    }
                                }) { Text("立即更新") }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
}
}

/* ============================================================================
 *  停服维护三档 UI
 *  level 1 = 顶部细横幅 / level 2 = 居中弹窗 / level 3 = 全屏阻断
 *  数据来自 GET {base}/api/v1/system/maintenance（见 PlayerViewModel.checkMaintenance）
 * ========================================================================== */

/** 紧急停机（level >= 3）：全屏阻断，不可关闭、不可操作、返回键无效。 */
@Composable
private fun MaintenanceBlockScreen(
    info: PlayerViewModel.MaintenanceInfo,
    dismissible: Boolean = false,
    onDismiss: () -> Unit = {},
) {
    BackHandler(enabled = dismissible) { onDismiss() }
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                // 吞掉所有点击，确保下面的界面完全不可操作
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    info.displayTitle(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    info.displayMessage(),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (info.until.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "预计恢复：" + info.until,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** 重要维护（level 2）：居中弹窗，有关闭按钮，但警告色明显。 */
@Composable
private fun MaintenanceNoticeDialog(
    info: PlayerViewModel.MaintenanceInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                info.displayTitle(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column {
                Text(
                    info.displayMessage(),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (info.until.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "预计恢复：" + info.until,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("我知道了") } }
    )
}

/** 普通维护（level 1）：顶部细横幅，只占一条，不遮挡也不拦截其它操作。 */
@Composable
private fun MaintenanceTopBanner(
    info: PlayerViewModel.MaintenanceInfo,
    onDismiss: () -> Unit
) {
    // 外层 Box 不处理任何手势 → 除横幅自身区域外，点击全部穿透到下面的界面
    Box(modifier = Modifier.fillMaxSize()) {
        // auto-hide：横幅只是提示，6 秒后自动消失，避免一直压住顶部的退出等按钮
        LaunchedEffect(info.signature()) {
            kotlinx.coroutines.delay(6_000)
            onDismiss()
        }

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 10.dp)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(info.displayTitle(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    val msg = info.displayMessage()
                    if (msg.isNotBlank()) {
                        Text(
                            msg,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (info.until.isNotBlank()) {
                        Text(
                            "预计恢复：" + info.until,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "关闭",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/* ============================================================================
 *  更新日志（入口在「账号」页 → 更新日志）
 * ========================================================================== */

/** 一条版本更新记录。public，供「账号」页的 ChangelogScreen（ProfileTab.kt）复用同一份数据。 */
data class ChangelogEntry(val version: String, val items: List<String>)

/** 各版本改动要点（只写项目里真实存在 / 真实修过的内容）。 */
private val CHANGELOG: List<ChangelogEntry> = listOf(
        ChangelogEntry(
        version = "1.3.2",
                items = listOf(
            "修复自动播放偶发等待数十秒才开始的问题：起播未响应时将自动触发一次播放",
            "修复部分歌曲被误判为试听片段导致反复换源、迟迟不播的问题",
            "修复搜索结果缺少原曲的问题：搜索改用网易云曲库，点击播放时再匹配其他平台音源",
            "修复推荐页点「下一首」跳到不相关歌曲的问题：播放队列跟随当前分区",
            "修复「每日 30 首」反复刷新的问题：同一天内保持固定",
            "修复搜索列表顺序无法调整的问题，现在可在倒序与正序之间切换（默认倒序）",
            "优化自动切歌的响应速度：播放器预热与预取进一步完善"
        )
    ),
    ChangelogEntry(
        version = "1.3.1",
        items = listOf(
            "日语 / 韩文歌词优先显示原文：不再把罗马音、拼音当主歌词（增强型 LRC 的行尾结束时间也已修正）",
            "起播更快：解析链路总预算 ≤6 秒，优先快 CDN（kuwo / kugou），并后台预解析 + 预缓冲下一首",
            "收藏增加本地缓存：未登录也能收藏，重装应用后登录即可与服务端自动合并回来",
            "歌曲前奏阶段显示「等待前奏…」；控制台不再出现「享受纯净音乐」这类占位文案",
            "搜索结果改为确定性排序：好版本（原唱 / 无 DJ 翻唱修饰）在最前，每次搜索顺序一致",
            "搜索列表上滑到底自动加载更多（后端 kuwo / kugou / bilibili 已支持翻页）",
            "跨源匹配放宽：歌手一方为空时不再误判「在免费歌源里没找到」",
            "新增停服维护提示：普通维护顶部横幅、重要维护弹窗、紧急停机全屏阻断",
            "「更新日志」入口位于「账号」页",
            "移除沉浸模式；开源协议为 GPL-3.0"
        )
    ),
    ChangelogEntry(
        version = "1.3.0 及更早",
        items = listOf(
            "搜索提速：多源并行聚合 + 结果去重",
            "推荐页自动续播；每日 30 首当天固定",
            "记住上次播放的歌与进度",
            "歌单导入与云端同步、各平台扫码登录",
            "悬浮歌词（测试版）：可穿透、可调色、可调透明度",
            "双风格界面：Material 3 与液态玻璃",
            "音质切换（开发中）"
        )
    )
)

/**
 * 更新日志数据源：最新版本排在最前。
 * 「账号」页的更新日志页（ProfileTab.kt 的 ChangelogScreen）直接用它渲染，
 * 保证「入口」和「内容」只有一个来源，不会出现"入口有了、条目还是旧的"。
 */
fun changelogEntries(): List<ChangelogEntry> = CHANGELOG

@Composable
fun ChangelogDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("更新日志", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                CHANGELOG.forEach { entry ->
                    Text(
                        "v" + entry.version,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    entry.items.forEach { line ->
                        Row(modifier = Modifier.padding(bottom = 5.dp)) {
                            Text(
                                "•",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                line,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun GlassShaderWarmup(backdrop: com.kyant.backdrop.Backdrop) {
    val shape = { androidx.compose.foundation.shape.RoundedCornerShape(16.dp) }
    Box(modifier = Modifier.size(1.dp)) {
        listOf(
            Triple(4f, 12f, 24f),
            Triple(6f, 12f, 24f),
            Triple(8f, 16f, 32f),
            Triple(10f, 16f, 32f),
            Triple(14f, 20f, 40f),
        ).forEach { (b, rh, ra) ->
            Box(
                Modifier
                    .size(1.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = shape,
                        effects = {
                            vibrancy()
                            blur(b.dp.toPx())
                            lens(rh.dp.toPx(), ra.dp.toPx())
                        }
                    )
            )
        }
    }
}

/**
 * Real ModalBottomSheet wrapper (drag-to-dismiss, back gesture, scrim tap).
 * allowDismiss=false blocks swipe-down and scrim tap (forced update).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AppModalSheet(
    onDismissRequest: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    allowDismiss: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value ->
            allowDismiss || value != androidx.compose.material3.SheetValue.Hidden
        }
    )
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = { if (allowDismiss) onDismissRequest() },
        sheetState = sheetState,
        containerColor = containerColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
            )
        },
        content = content
    )
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AddToPlaylistSheet(
    song: Song,
    onDismiss: () -> Unit,
    onAdded: (PlaylistInfo) -> Unit
) {
    val context = LocalContext.current
    var playlists by remember { mutableStateOf(PlaylistStore.load(context)) }
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "加入歌单",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                song.name,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (playlists.isEmpty()) {
                    Text(
                        "还没有歌单，先创建一个吧",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
                playlists.forEach { pl ->
                    val already = pl.songs.any { it.id == song.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { if (!already) onAdded(pl) else onDismiss() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.LibraryMusic,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pl.name, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(pl.songs.size.toString() + " 首", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (already) {
                            Text("已添加", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (creating) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("歌单名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { creating = false; newName = "" },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) { Text("取消") }
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                val pl = PlaylistStore.create(context, newName.trim(), MusicSource.OTHER, "我创建的")
                                onAdded(pl)
                                creating = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) { Text("创建并加入") }
                }
            } else {
                Button(
                    onClick = { creating = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("新建歌单并加入", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
