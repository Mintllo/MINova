/*
 * Ported from CurrentMusicX (https://github.com/bileizhen/CurrentMusicX)
 * original package: io.github.currencortex.music.feature.player (PlayerScreen.kt and its
 * player-file dependencies, ported verbatim into this package).
 * License: GPL-3.0 (upstream). This entry keeps MINova's CmxFullScreenPlayer signature and
 * funnels MINova's playback data (currentSong / isPlaying / seekTo / onNext / onPrevious)
 * into the upstream-shaped PlayerViewModel adapter (CmxPlayerAdapter.kt). All playback,
 * parsing and watchdog logic stays in MINova's MainActivity ViewModel, untouched.
 */
package com.HyperX.MI.Na.cmx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.HyperX.MI.Na.LyricData
import com.HyperX.MI.Na.Song
import com.HyperX.MI.Na.cmx.Song as CmxSong
import com.kyant.backdrop.Backdrop

/**
 * CMX 全屏播放页：完整移植上游 PlayerScreen（头部 / 封面歌词双页 Pager / 共享封面动效 /
 * 光晕进度条 / 液态弹窗组），数据经 CmxPlayerAdapter 接到 MINova 的播放逻辑。
 */
@Composable
fun CmxFullScreenPlayer(
    currentSong: Song,
    isPlaying: Boolean,
    isLooping: Boolean,
    currentPosition: Long,
    duration: Long,
    lyrics: LyricData,
    lyricsSettled: Boolean = true,
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
    onShare: (() -> Unit)? = null,
) {
    val vm = remember { PlayerViewModel(onSeek = onSeek, onNext = onNext, onPrevious = onPrevious,
        onToggleLoop = onToggleLoop) }
    vm.push(currentSong, isPlaying, isLooping, currentPosition, duration, lyrics, lyricsSettled)
    val actions: @Composable (CmxSong) -> Unit = { _ ->
        if (onAddToFavorites != null) MusicDestinationRow("收藏到我喜欢", onClick = onAddToFavorites)
        if (onAddToPlaylist != null) MusicDestinationRow("添加到歌单", onClick = { onAddToPlaylist(currentSong) })
        if (onDownload != null) MusicDestinationRow("下载歌曲", onClick = onDownload)
        if (onShare != null) MusicDestinationRow("分享歌曲", onClick = onShare)
    }
    PlayerScreen(vm, onBack = onDismiss, onToggle = onPlayPause,
        actions = if (onAddToFavorites != null || onAddToPlaylist != null || onDownload != null || onShare != null) actions else null,
        onLike = onAddToFavorites)
}
