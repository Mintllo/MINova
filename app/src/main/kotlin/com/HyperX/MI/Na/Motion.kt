package com.HyperX.MI.Na

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 全应用统一的动效规范（Material Design 3 motion）。
 *
 * 为什么要统一：以前每个组件各自写 tween/spring（时长 200/240/260/300/380 混用、
 * 弹簧参数各不相同、错峰间隔 35ms 与 60ms 并存），相邻元素进出场节奏对不上，
 * 视觉上就会出现「衔接有痕迹」「不平滑」。
 *
 * 这里把规格收敛成一套：
 *  - 入场：300ms + FastOutSlowInEasing（减速进场，有「落定」感）
 *  - 退场：200ms + FastOutLinearInEasing（加速离场，干脆不拖沓）
 *  - 错峰：每项 32ms，最多 10 项（约 320ms 内铺满，不拖长整体时长）
 *  - 位移：入场 20dp 上浮；弹簧用低阻尼比让收尾自然减速
 */
object Motion {

    /** 入场时长/缓动 */
    const val ENTER_MS = 300
    /** 退场时长/缓动 */
    const val EXIT_MS = 200
    /** 错峰间隔 */
    const val STAGGER_MS = 32L
    /** 错峰上限（超过后不再累加延迟） */
    const val STAGGER_MAX = 10
    /** 入场上浮距离 */
    val ENTER_OFFSET: Dp = 20.dp

    val enterEasing = FastOutSlowInEasing
    val exitEasing = FastOutLinearInEasing
    val standardEasing = LinearOutSlowInEasing

    /** 入场：减速进场 */
    fun <T> enter() = tween<T>(durationMillis = ENTER_MS, easing = enterEasing)

    /** 退场：加速离场 */
    fun <T> exit() = tween<T>(durationMillis = EXIT_MS, easing = exitEasing)

    /** 尺寸/颜色等常态过渡 */
    fun <T> standard() = tween<T>(durationMillis = ENTER_MS, easing = standardEasing)

    /** 按压回弹（快、干脆） */
    fun <T> press() = spring<T>(dampingRatio = 0.62f, stiffness = 620f)

    /** 其它强调动效（进场弹簧，收尾自然） */
    fun <T> emphasized() = spring<T>(dampingRatio = 0.84f, stiffness = 380f)

    /** 强调弹簧（用于弹入/展开） */
    fun <T> bouncy() = spring<T>(dampingRatio = 0.7f, stiffness = 420f)

    /** 错峰延迟毫秒数 */
    fun staggerDelayMillis(index: Int): Long =
        if (index <= 0) 0L else (index.coerceAtMost(STAGGER_MAX) * STAGGER_MS)

    /** 不用弹簧的静默弹簧（占位，避免误用系统默认值） */
    fun <T> none() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
}

/**
 * 全应用统一的列表/分区入场动画：淡入 + 上浮，按 index 错峰。
 * DiscoverScreen / PlaylistScreen 等处的私有 enterAnimation 都委托到这里，
 * 保证跨页面节奏一致。
 */
@Composable
fun Modifier.motionEnter(index: Int = -1): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val d = Motion.staggerDelayMillis(index)
        if (d > 0L) delay(d)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = Motion.enter(),
        label = "motionEnterAlpha"
    )
    val dy by animateDpAsState(
        targetValue = if (visible) 0.dp else Motion.ENTER_OFFSET,
        animationSpec = Motion.emphasized(),
        label = "motionEnterDy"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        translationY = dy.toPx()
    }
}
