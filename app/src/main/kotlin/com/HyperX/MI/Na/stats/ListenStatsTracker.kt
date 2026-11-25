package com.HyperX.MI.Na.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.HyperX.MI.Na.PlayerViewModel
import kotlinx.coroutines.flow.combine
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 听歌时长采集器：把现有播放事件接到 ListenStatsStore 上。
 *
 * 接入点（只读，不改动播放链路本身）：
 *  - viewModel.currentSong    当前歌曲变化即结算上一首的未落账时长
 *  - viewModel.isPlaying      暂停即结算，避免把暂停时间算进去
 *  - viewModel.currentPosition 每 500ms 左右一次的进度回调，作为时长的增量来源
 *
 * 计时规则：只累计 1ms 到 2500ms 之间的正向进度差（跳过 seek 跳变），攒够 5 秒或
 * 暂停/切歌时结算落账。
 */
@Composable
fun ListenStatsBinder(viewModel: PlayerViewModel) {
    val appContext = LocalContext.current.applicationContext
    LaunchedEffect(viewModel) {
        ListenStatsStore.init(appContext)
        val songRef = AtomicReference<com.HyperX.MI.Na.Song?>(null)
        val lastPos = AtomicLong(-1L)
        val buffer = AtomicLong(0L)
        fun flushNow() {
            val s = songRef.get() ?: return
            val ms = buffer.getAndSet(0L)
            if (ms > 0) {
                ListenStatsStore.record(s.id, s.source, s.name, s.artist, s.picUrl, ms, System.currentTimeMillis())
            }
        }
        combine(viewModel.currentSong, viewModel.isPlaying, viewModel.currentPosition) { s, playing, pos ->
            Triple(s, playing, pos)
        }.collect { (s, playing, pos) ->
            val prev = songRef.get()
            if (s?.id != prev?.id || s?.source != prev?.source) {
                flushNow()
                songRef.set(s)
                lastPos.set(-1L)
            }
            if (s == null) {
                lastPos.set(-1L)
                return@collect
            }
            val last = lastPos.getAndUpdate { pos }
            if (playing && last >= 0) {
                val delta = pos - last
                if (delta in 1..2500) buffer.addAndGet(delta)
            }
            if (!playing || buffer.get() >= 5000) flushNow()
        }
    }
}
