// SPDX-License-Identifier: GPL-3.0-only
// ElasticPress ported from CurrentMusicX ui/component/nativeui/ElasticPress.kt
// (blob 25bf08db, adapted from chen08209 upstream lib/ui/navigation_dock.dart).
// Package/import adaptation only; physics constants kept verbatim.
package com.HyperX.MI.Na.nativeui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

@Immutable data class ElasticPressFrame(val lift: Float, val pull: Offset, val scaleX: Float, val scaleY: Float)
val ElasticPressGeometry = SemanticsPropertyKey<ElasticPressFrame>("ElasticPressGeometry")
val GlassPressLight = SemanticsPropertyKey<Float>("GlassPressLight")

/** ElasticLazyRow 的行绘制注册表（本工程暂无 ElasticLazyRow，恒为 null）。 */
val LocalElasticRowPaint = staticCompositionLocalOf<MutableMap<ElasticPressState, Unit>?> { null }

/** The upstream rubber band: asymptotically approaches limit rather than stopping the finger. */
fun elasticRubberBand(distance: Float, limit: Float): Float {
    if (limit <= 0f) return 0f
    return limit * (1f - 1f / (abs(distance) * .55f / limit + 1f)) * sign(distance)
}

@Stable class ElasticPressState internal constructor(private val scope: CoroutineScope) {
    private val lift = Animatable(0f, visibilityThreshold = .001f)
    private val pullX = Animatable(0f, visibilityThreshold = .01f)
    private val pullY = Animatable(0f, visibilityThreshold = .01f)
    private var releaseJob: Job? = null
    private var released = false
    private var origin = Offset.Zero
    var size by mutableStateOf(Size.Zero)
        internal set
    val amount get() = lift.value
    val pull: Offset get() = Offset(pullX.value, pullY.value)
    fun press(position: Offset) {
        origin = position
        released = false
        releaseJob?.cancel()
        scope.launch { lift.animateTo(1f, spring(.593f, 311f)) }
    }
    fun drag(position: Offset, pastEdges: Boolean = false) {
        val limit = size.minDimension * 7f / 32f
        val delta = if (pastEdges) position - Offset(position.x.coerceIn(0f, size.width), position.y.coerceIn(0f, size.height))
            else position - origin
        // 上游 2741.5568 的拖拽刚度在逐帧 animateTo 重启下肉眼乱抖；降到 900 并保持
        // 临界阻尼，橡皮筋手感不变、轨迹平滑（降 stiffness 保平滑，回弹交给 release）。
        scope.launch { pullX.animateTo(elasticRubberBand(delta.x, limit), spring(1f, 900f)) }
        scope.launch { pullY.animateTo(elasticRubberBand(delta.y, limit), spring(1f, 900f)) }
    }
    fun release() {
        if (released) return
        released = true
        // 回弹阻尼 .593 → .78：保留一点果冻余韵，但不再连续摆动（加 damping 抑抖）。
        scope.launch { pullX.animateTo(0f, spring(.78f, 311f)) }
        scope.launch { pullY.animateTo(0f, spring(.78f, 311f)) }
        // Keep the rise of an early release; the following coroutine owns its fall.
        releaseJob = scope.launch {
            snapshotFlow { lift.value }.first { it >= .975f || lift.targetValue == 0f }
            if (released) {
                lift.animateTo(0f, spring(.78f, 311f))
            }
        }
    }

    internal fun frame(maxGrowth: Float): ElasticPressFrame {
        val width = size.width.coerceAtLeast(1f); val height = size.height.coerceAtLeast(1f)
        // 膨起上限从 .25 收到 .12：小按钮上 25% 的形变叠加回弹摆动就是「乱抖」观感。
        val swell = 1f + lift.value * min(.12f, maxGrowth / maxOf(width, height))
        return ElasticPressFrame(lift.value, Offset(pullX.value, pullY.value),
            swell * (1 + abs(pullX.value) / width * .5f), swell * (1 + abs(pullY.value) / height * .5f))
    }
}

@Composable fun rememberElasticPressState(): ElasticPressState {
    val scope = rememberCoroutineScope()
    return remember(scope) { ElasticPressState(scope) }
}

/** Layout and hit targets stay at rest. Only the child's painted layer stretches and follows. */
@Composable fun ElasticPress(modifier: Modifier = Modifier, enabled: Boolean = true,
    state: ElasticPressState = rememberElasticPressState(), pullPastEdges: Boolean = false,
    draggable: Boolean = true,
    observePointer: Boolean = true, paintedHeight: Dp? = null,
    content: @Composable () -> Unit) {
    val growth = with(LocalDensity.current) { 16.dp.toPx() }
    val heightPx = with(LocalDensity.current) { paintedHeight?.toPx() }
    val currentHeight by rememberUpdatedState(heightPx)
    SideEffect { if (heightPx != null) state.size = state.size.copy(height = heightPx) }
    val rowPaint = LocalElasticRowPaint.current
    DisposableEffect(rowPaint, state) {
        rowPaint?.put(state, Unit)
        onDispose { rowPaint?.remove(state) }
    }
    val currentEnabled by rememberUpdatedState(enabled)
    LaunchedEffect(enabled) { if (!enabled) state.release() }
    val diagnostics = Modifier.semantics {
        this[ElasticPressGeometry] = state.frame(growth)
        this[GlassPressLight] = state.amount.coerceIn(0f, 1f)
    }
    Box(modifier.onSizeChanged { state.size = Size(it.width.toFloat(), currentHeight ?: it.height.toFloat()) }
        .then(diagnostics)
        .then(if (observePointer) Modifier.pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (!currentEnabled || currentHeight?.let { down.position.y !in 0f..it } == true) return@awaitEachGesture
                state.press(down.position)
                try {
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (event.changes.count { it.pressed } > 1) break
                        // draggable=false：正序/倒序等固定件只保留按压缩放，禁止拽动位移
                        if (change.pressed && draggable) state.drag(change.position, pullPastEdges)
                    } while (change.pressed)
                } finally { state.release() }
            }
        } else Modifier)) {
        Box(Modifier.graphicsLayer {
            val frame = state.frame(growth)
            transformOrigin = TransformOrigin(.5f, if (size.height > 0) (heightPx ?: size.height) / (2 * size.height) else .5f)
            scaleX = frame.scaleX; scaleY = frame.scaleY
            translationX = frame.pull.x; translationY = frame.pull.y
        }) { content() }
    }
}
