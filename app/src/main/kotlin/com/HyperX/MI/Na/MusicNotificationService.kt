package com.HyperX.MI.Na

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 播放控制桥：ViewModel 注册实现，通知栏按钮通过它回调。
 * 避免服务直接持有 ViewModel 引用。
 */
object PlayerControlBridge {
    @Volatile var onPrev: (() -> Unit)? = null
    @Volatile var onNext: (() -> Unit)? = null
    @Volatile var onToggle: (() -> Unit)? = null

    fun prev() = runCatching { onPrev?.invoke() }
    fun next() = runCatching { onNext?.invoke() }
    fun toggle() = runCatching { onToggle?.invoke() }
}

/**
 * 仅用于接收通知栏 action 的轻量服务，收到后转给 [PlayerControlBridge]，随即结束。
 */
class MusicNotificationService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PREV -> PlayerControlBridge.prev()
            ACTION_NEXT -> PlayerControlBridge.next()
            ACTION_TOGGLE -> PlayerControlBridge.toggle()
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_PREV = "com.HyperX.MI.Na.MEDIA_PREV"
        const val ACTION_NEXT = "com.HyperX.MI.Na.MEDIA_NEXT"
        const val ACTION_TOGGLE = "com.HyperX.MI.Na.MEDIA_TOGGLE"
    }
}
