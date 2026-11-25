package com.HyperX.MI.Na.cmx

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.HyperX.MI.Na.PlaylistInfo
import com.HyperX.MI.Na.PlaylistSong
import com.HyperX.MI.Na.PlayerViewModel as PlayViewModel
import com.HyperX.MI.Na.Song as PlaySong
import com.HyperX.MI.Na.apiSourceName

/**
 * 数据适配层（任务第 4 步）：本工程歌单存储（PlaylistInfo/PlaylistSong）
 * 到上游歌单 UI 模型（cmx Song/Playlist）的映射，播放仍走 viewModel.playExternalSong。
 * 播放引擎/解析/看门狗零改动。
 */

/** PlaylistSong（歌单存储）→ 上游 cmx Song（3D 卡组消费）。 */
internal fun PlaylistSong.toCmxSong(): Song = Song(
    id = id,
    name = name,
    artists = artist,
    cover = picUrl,
    album = album,
    durationMs = duration,
    musicSource = source.toCmxMusicSource(),
)

private fun String.toCmxMusicSource(): MusicSource = when (lowercase()) {
    "netease", "ncm" -> MusicSource.NETEASE
    "qq", "qqmusic" -> MusicSource.QQ_MUSIC
    else -> MusicSource.LOCAL
}

/** 上游 cmx Song → 播放引擎根包 Song。（internal：PlaylistShelfModes 三模式复用） */
internal fun Song.toPlaySong(sourceKey: String): PlaySong = PlaySong(
    id = id,
    name = name,
    artist = artists,
    album = "",
    duration = durationMs,
    picUrl = cover,
    // 回归修复：歌曲自带 source 有效就用自带（musicSource 精确映射），
    // 仅枚举表达不了的平台（LOCAL/EMBY，如 kuwo/migu）才回落歌单级 sourceKey；别名归一保留。
    source = normalizeSourceKey(musicSource.toSourceKey().orEmpty(), sourceKey),
)

/** cmx 枚举 → 解析用 source 名；LOCAL/EMBY 返回 null = 交回 fallback。 */
private fun MusicSource.toSourceKey(): String? = when (this) {
    MusicSource.NETEASE -> "netease"
    MusicSource.QQ_MUSIC -> "qq"
    else -> null
}

/**
 * PlaylistSong（歌单存储）→ 播放引擎根包 Song：source 用**每首歌自带平台**。
 *
 * 修「账号歌单大部分歌播放不了」：此前整批歌都盖歌单级 sourceKey（resolveFastUrl 的
 * knownSource 按错平台解析，服务器按错源拿直链必挂）。服务器歌单可能整首缺 source
 * 字段 → 兜底歌单来源，再兜底 netease（服务器歌单绝大多数是网易）。
 */
internal fun PlaylistSong.toPlaySong(fallbackSource: String): PlaySong = PlaySong(
    id = id,
    name = name,
    artist = artist,
    album = album,
    duration = duration,
    picUrl = picUrl,
    source = normalizeSourceKey(source, fallbackSource),
)

/** 解析用 source 归一化：歌曲自带 → 歌单来源 → netease；兼容常见别名。 */
internal fun normalizeSourceKey(raw: String, fallback: String): String {
    val s = raw.trim().lowercase()
    val f = fallback.trim().lowercase()
    val k = when {
        s.isNotBlank() -> s
        f.isNotBlank() -> f
        else -> "netease"
    }
    return when (k) {
        "ncm", "wangyi", "wyy", "163" -> "netease"
        "qqmusic" -> "qq"
        else -> k
    }
}

/** 上游 io.github.currencortex.music.data.library.Playlist 的最小等价模型（歌单列表页/分类用）。 */
internal data class Playlist(
    val id: Long = 0,
    val remoteId: String = "",
    val name: String = "",
    val source: String = "local",
    val cover: String = "",
    val count: Int = 0,
    val editableFlag: Boolean = true,
    val nativeLiked: Boolean = false,
    val nativeOwned: Boolean = false,
    val nativeSubscribed: Boolean = false,
    val nativeSubscriptionKnown: Boolean = true,
    val songs: List<Song> = emptyList(),
) {
    fun editable(accountId: Long): Boolean = editableFlag
}

/**
 * 歌单详情 3D 唱片架（照搬 CurrentMusicX PlaylistRecordShelf 的接入口）。
 * 替换原 PlaylistArchiveDeck 的调用位：数据流与交互入口不变
 * （playExternalSong 播放 + 整张歌单作队列；返回/删除/长按菜单由宿主保留）。
 *
 * 模式 1 = 原生纵向 3D 唱片架：PlaylistRecordShelf 内 AndroidView 托载原生 RecordShelfView
 * （songs 注入 / onSelection / onActivate / onSongLongPress / setVinyl / restoreSelection 接线）。
 * FLOW 平铺已删除；SIMPLE 不路由到这里（PlaylistShelfScene 简洁模式走独立列表）。
 */
@Composable
internal fun CmxPlaylistShelf(
    detail: PlaylistInfo,
    viewModel: PlayViewModel,
    downloadedSongs: List<PlaySong>,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSongMenu: (PlaylistSong) -> Unit,
) {
    val sourceKey = detail.source.apiSourceName() ?: "netease"
    // ── 试听开关状态链（任务 2 修「开关点了没反应」）：
    // 此前 CmxPlaylistShelf 调 PlaylistRecordShelf 时没传 previewMode/fullPreview 及其回调，
    // 选项弹窗里的四个 SettingsSwitch 全挂在空实现默认值上（onCheckedChange = {} 恒不翻转）。
    // 现在：SharedPreferences 持久化（状态回写）+ rememberSaveable 即时重组（切换立即生效），
    // 真值下传 PlaylistRecordShelf —— 开关翻转、摘要文案、试听模式即时联动。──
    val context = LocalContext.current
    val previewPrefs = remember { context.getSharedPreferences("record_shelf_prefs", Context.MODE_PRIVATE) }
    var fullPreview by rememberSaveable {
        mutableStateOf(previewPrefs.getBoolean("full_preview", false))
    }
    var previewMode by rememberSaveable {
        mutableStateOf(previewPrefs.getString("preview_mode", null)
            ?.let { stored -> RecordShelfPreviewMode.values().firstOrNull { it.name == stored } }
            ?: RecordShelfPreviewMode.ONCE)
    }
    val songs = remember(detail.songs) { detail.songs.map { it.toCmxSong() } }
    // 播放映射改走「每首歌自带 source」（空则兜底歌单来源，再兜底 netease）。
    val playSongs = remember(detail.songs, sourceKey) { detail.songs.map { it.toPlaySong(sourceKey) } }
    val favorites by viewModel.favorites.collectAsState()
    // 播放状态接线：黑胶伸出 + 底部三控制钮（上一首/播放暂停/下一首）+ GlowSeekBar 进度
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlayingNow by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.currentPosition.collectAsState()
    val durationMs by viewModel.duration.collectAsState()
    val playingSongId = currentSong?.id
    val onSongMenuState by rememberUpdatedState(onSongMenu)
    PlaylistRecordShelf(
        songs = songs,
        canPlay = true,
        onPlay = { index ->
            playSongs.getOrNull(index)?.let { start ->
                viewModel.playExternalSong(start, sourceKey, playSongs)
            }
        },
        isSaved = { record -> favorites.any { it.id == record.id } },
        onSave = { record ->
            if (favorites.any { it.id == record.id }) viewModel.removeFromFavorites(record.id)
            // 收藏回归修复：优先取原 PlaylistSong 落库（自带 source 逐首保留，kuwo 等不丢），
            // 找不到原条目才退回 cmx 记录的 musicSource 映射。
            else viewModel.addToFavorites(
                detail.songs.firstOrNull { it.id == record.id }?.toPlaySong(sourceKey)
                    ?: record.toPlaySong(sourceKey)
            )
        },
        onClose = onBack,
        // 上游原版 PlaylistRecordShelf 无 onSongMenu 参数（选项菜单由上游内置 UI 承担），
        // 本参数保留以兼容调用方 PlaylistScreen 签名，但不转发。
        collectionKey = "playlist-${detail.id}",
        playingSongId = playingSongId,
        playing = isPlayingNow,
        // 底部进度条（RulerSeekBar 刻度尺）接线：真实 PlayerViewModel 的 position/duration/seekTo
        positionMs = positionMs,
        durationMs = durationMs,
        onSeek = { viewModel.seekTo(it) },
        // 陈列模式：模式 1 = 原生 3D 唱片架（卡片层由 PlaylistRecordShelf 内原生 RecordShelfView 承担）
        shelfMode = PlaylistShelfMode.STACK,
        playlistName = detail.name,
        playlistCount = detail.songs.size,
        // 试听开关接线（任务 2）：真状态 + 持久化回写 + 日志（logcat tag MINova-ShelfPreview 可验证）
        previewMode = previewMode,
        onPreviewModeChange = { mode ->
            previewMode = mode
            previewPrefs.edit().putString("preview_mode", mode.name).apply()
            android.util.Log.i("MINova-ShelfPreview", "mode -> $mode full=$fullPreview")
        },
        fullPreview = fullPreview,
        onFullPreviewChange = { full ->
            fullPreview = full
            previewPrefs.edit().putBoolean("full_preview", full).apply()
            android.util.Log.i("MINova-ShelfPreview", "full -> $full mode=$previewMode")
        },
        // 三段式键 → 真实 PlayerViewModel 播放控制（playPrevious/togglePlayPause/playNext，
        // 均为 MainActivity 真实现非 stub）；logcat tag MINova-ShelfBtn 可验证点击有输出。
        onPrevious = {
            android.util.Log.i("MINova-ShelfBtn", "prev -> playPrevious cur=${currentSong?.name}")
            viewModel.playPrevious()
        },
        onTogglePlay = {
            android.util.Log.i("MINova-ShelfBtn", "toggle -> togglePlayPause playing=$isPlayingNow cur=${currentSong?.name}")
            viewModel.togglePlayPause()
        },
        onNext = {
            android.util.Log.i("MINova-ShelfBtn", "next -> playNext cur=${currentSong?.name}")
            viewModel.playNext()
        },
        // 长按唱片 → 曲目菜单（PlaylistSongMenuSheet：收藏/加入歌单/下载/从歌单删除）
        onSongLongPress = { record ->
            detail.songs.firstOrNull { it.id == record.id }?.let(onSongMenuState)
        },
    )
}
