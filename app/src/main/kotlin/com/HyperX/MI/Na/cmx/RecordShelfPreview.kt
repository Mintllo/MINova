package com.HyperX.MI.Na.cmx

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 唱片架试听控制器：照搬上游 RecordShelfPreview 的对外 API。
 * 上游用 ExoPlayer 播放高潮片段；本工程播放引擎零改动策略下暂无 media3 依赖，
 * 试听播放桩化（TODO：接入我们的 playExternalSong 试听或引入 media3）。
 */
internal class RecordShelfPreview(context: Context) {
    val nextRequests = MutableSharedFlow<Song>(extraBufferCapacity = 8)

    fun setActive(active: Boolean) {
    }

    fun setFullLength(full: Boolean) {
    }

    fun setMode(mode: RecordShelfPreviewMode) {
    }

    fun select(song: Song?) {
    }

    fun commit(onReady: () -> Unit) {
        onReady()
    }

    fun close() {
    }
}
