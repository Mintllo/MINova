package com.HyperX.MI.Na

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用内提示（替代系统 Toast）。
 *
 * 所有原来的 Toast.makeText(...) / _toastMessage 语义与文案保持不变，
 * 只是展示方式改为：Compose 自绘的底部居中悬浮胶囊（见 [InAppToastHost]）。
 * 单例 StateFlow：任何线程可调 [show]，主界面根部的 Host 负责渲染与自动消失。
 */
object AppToast {
    data class Msg(val key: Long, val text: String, val long: Boolean = false)

    private val _message = MutableStateFlow<Msg?>(null)
    val message: StateFlow<Msg?> = _message.asStateFlow()

    /** 替代 Toast.makeText(ctx, text, LENGTH_SHORT/LONG).show()；long=true 对应 LENGTH_LONG。 */
    fun show(text: String, long: Boolean = false) {
        _message.value = Msg(System.nanoTime(), text, long)
    }

    fun clear() {
        _message.value = null
    }
}

/**
 * 底部居中悬浮胶囊：MD3 深色 surface、圆角 50（胶囊）、白字、
 * 2.5 秒（long 时 3.5 秒）自动消失，淡入淡出 + 轻微上移动画。
 * 纯展示层（无点击语义），不拦截下方内容的触摸。
 */
@Composable
fun InAppToastHost() {
    val msg by AppToast.message.collectAsState()
    var current by remember { mutableStateOf<AppToast.Msg?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(msg) {
        val m = msg ?: return@LaunchedEffect
        current = m
        visible = true
        delay(if (m.long) 3500L else 2500L)
        visible = false
        delay(280) // 等退场动画播完再清空，避免文字提前跳走
        AppToast.clear()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible && current != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(240)) { it / 3 },
            exit = fadeOut(tween(260)) + slideOutVertically(tween(260)) { it / 3 },
            modifier = Modifier.padding(bottom = 92.dp)
        ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 320.dp)
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(18.dp), clip = false)
                .background(Color.Black, RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                text = current?.text.orEmpty(),
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        }
    }
}
