// Ported from CurrentMusicX (GPL-3.0) - https://github.com/bileizhen/CurrentMusicX
// original file: PlayerSongActionsBar.kt (verbatim implementation)
package com.HyperX.MI.Na.cmx

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable internal fun PlayerSongActionsBar(vm: PlayerViewModel, onComments: () -> Unit, onQueue: () -> Unit,
    onSleep: () -> Unit, onLike: (() -> Unit)?, onEffects: (() -> Unit)? = null, onOptions: (() -> Unit)? = null) {
    val queue by vm.queue.collectAsStateWithLifecycle()
    val actions by vm.actions.collectAsStateWithLifecycle()
    // MINova 定版：爱心即收藏，选中态直接读应用已有的收藏列表。
    val appVm = androidx.lifecycle.viewmodel.compose.viewModel<com.HyperX.MI.Na.PlayerViewModel>()
    val favorites by appVm.favorites.collectAsStateWithLifecycle()
    val liked = favorites.any { it.id == queue.current?.id }
    PlayerFunctionBar(actionCount = 5, transportAligned = onOptions != null) {
        val middleSlot = if (onOptions != null) Modifier.weight(1f) else Modifier
        PlayerIconButton(PlayerIcon.LIKE, if (liked) "取消喜欢" else "喜欢",
            onLike ?: { vm.togglePrimaryLike() },
            Modifier.testTag("player_like"), selected = liked, enabled = queue.current != null)
        if (onOptions != null) {
            Box(middleSlot, contentAlignment = Alignment.Center) {}
            PlayerIconButton(PlayerIcon.MORE, "更多操作", onOptions, Modifier.testTag("lyrics_options"))
        } else {
            Box(middleSlot, contentAlignment = Alignment.Center) {}
            PlayerQueueButton(vm, onQueue)
        }
    }
}

@Composable internal fun PlayerPlaybackModeButton(vm: PlayerViewModel) {
    val queue by vm.queue.collectAsStateWithLifecycle()
    val player by vm.state.collectAsStateWithLifecycle()
    val together by vm.together.state.collectAsStateWithLifecycle()
    val shared by vm.together.queue.collectAsStateWithLifecycle()
    val mode = if (together.active) shared.mode else queue.mode
    PlayerIconButton(when (mode) {
        PlaybackMode.ONE -> PlayerIcon.REPEAT_ONE
        PlaybackMode.SHUFFLE -> PlayerIcon.SHUFFLE
        PlaybackMode.HEART -> PlayerIcon.HEART_MODE
        PlaybackMode.LIST -> PlayerIcon.REPEAT
    }, mode.label, vm::cyclePlaybackMode, Modifier.testTag("player_cycle_mode"),
        selected = mode == PlaybackMode.HEART, enabled = if (together.active) !together.busy else player.mode == PlayerMode.LOCAL,
        onLongClick = if (!together.active && player.mode == PlayerMode.LOCAL &&
            NeteaseSongActionsRepository.songId(queue.current) != null) {
            { vm.setPlaybackMode(PlaybackMode.HEART) }
        } else null, onLongClickLabel = "启用心动模式")
}

@Composable internal fun PlayerQueueButton(vm: PlayerViewModel, onQueue: () -> Unit) {
    val queue by vm.queue.collectAsStateWithLifecycle()
    val together by vm.together.state.collectAsStateWithLifecycle()
    val shared by vm.together.queue.collectAsStateWithLifecycle()
    PlayerIconButton(PlayerIcon.QUEUE, if (together.active) "一起听歌单，${shared.songs.size} 首" else "播放列表，${queue.songs.size} 首", onQueue, Modifier.testTag("open_player_queue"))
}
