// Ported from CurrentMusicX (GPL-3.0) - https://github.com/bileizhen/CurrentMusicX
// original file: PlayerSheetDrag.kt (verbatim implementation)
package com.HyperX.MI.Na.cmx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

internal class PlayerSheetState {
    val animation = Animatable(0f)
    var draggedProgress by mutableStateOf<Float?>(null)
    val dragging get() = draggedProgress != null
    val value get() = draggedProgress ?: animation.value
    /** 松手瞬时速度（progress/s，负值 = 收起方向）。拖拽控制器写入，收起的
     *  LaunchedEffect 消费一次；保证 settle 从松手速度续跑，不从 0 重新加速。 */
    var pendingSettleVelocity: Float? = null
}

internal class PlayerSheetDragController(val state: PlayerSheetState, private val scope: CoroutineScope) {
    var onOpen: () -> Unit = {}
    var onClose: () -> Unit = {}
    var onSettled: () -> Unit = {}
    var distance: () -> Float = { 2000f }
    var flingThreshold = 2000f
    private var opening = false
    private var travel = 1f
    private var settle: Job? = null

    fun begin(fromMini: Boolean) {
        settle?.cancel()
        opening = fromMini
        state.pendingSettleVelocity = null
        state.draggedProgress = state.value
        scope.launch { state.animation.stop() }
        if (fromMini) onOpen()
        travel = distance().coerceAtLeast(1f)
    }
    fun drag(delta: Float) {
        state.draggedProgress?.let { state.draggedProgress = (it - delta / travel).coerceIn(0f, 1f) }
    }
    fun end(velocity: Float, cancelled: Boolean = false) {
        val p = state.draggedProgress ?: return
        val expand = if (cancelled) !opening else if (abs(velocity) > flingThreshold) velocity < 0
            else if (opening) p >= .14f else p > .82f
        // 松手速度换算成 progress/s（向上拖为正）。settle 带 initialVelocity 续跑，
        // 不再从 0 重新加速：此前 tween 会忽略速度，松手瞬间出现"停一下再走"的顿挫。
        // tween 规格本身不消费初速度，展开 settle 改用无过冲弹簧，曲线手感与
        // sheetEasing 的减速段一致，且时长随剩余距离/初速度自适应。
        val settleVelocity = -velocity / travel
        settle = scope.launch {
            state.animation.snapTo(p)
            state.draggedProgress = null
            if (expand) {
                state.animation.animateTo(1f, spring(dampingRatio = 1f, stiffness = 260f),
                    initialVelocity = settleVelocity.coerceAtLeast(0f))
                onSettled()
            } else {
                state.pendingSettleVelocity = settleVelocity.coerceAtMost(0f)
                onClose()
            }
        }
    }
}
internal val LocalPlayerSheetDrag = staticCompositionLocalOf<PlayerSheetDragController?> { null }

internal data class PlayerUpDrag(
    val begin: () -> Unit,
    val drag: (Float) -> Unit,
    val end: (Float, Boolean) -> Unit,
)

internal data class PlayerControlsDownDrag(val area: () -> LayoutCoordinates?, val gesture: PlayerUpDrag)

/** Direction locking leaves the mini player's horizontal song gesture and lyric scrolling intact. */
@Composable internal fun Modifier.playerSheetDrag(fromMini: Boolean,
    upDrag: PlayerUpDrag? = null,
    controlsDownDrag: PlayerControlsDownDrag? = null,
    enabled: Boolean = true,
    excludedArea: () -> List<LayoutCoordinates> = { emptyList() }): Modifier {
    val controller = LocalPlayerSheetDrag.current
    if (fromMini && (controller == null || !enabled)) return this
    val exclusion by rememberUpdatedState(excludedArea)
    val upward by rememberUpdatedState(upDrag)
    val downward by rememberUpdatedState(controlsDownDrag)
    val available by rememberUpdatedState(enabled)
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    return onGloballyPositioned { coordinates = it }.pointerInput(controller, fromMini) {
        val velocity = VelocityTracker()
        if (!fromMini) {
            // Claim vertical movement before the cover's scroll container, but leave the
            // entire lyric viewport and horizontal controls untouched from pointer down.
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (!available) return@awaitEachGesture
                val rootDown = coordinates?.localToRoot(down.position) ?: down.position
                val root = coordinates ?: return@awaitEachGesture
                if (exclusion().any { excluded ->
                    if (!excluded.isAttached) false else {
                        val point = excluded.localPositionOf(root, down.position)
                        point.x >= 0 && point.y >= 0 && point.x < excluded.size.width && point.y < excluded.size.height
                    }
                }) return@awaitEachGesture
                val controlsDrag = downward?.takeIf { action ->
                    action.area()?.takeIf { it.isAttached }?.let { area ->
                        val point = area.localPositionOf(root, down.position)
                        point.x >= 0 && point.y >= 0 && point.x < area.size.width && point.y < area.size.height
                    } == true
                }?.gesture
                velocity.resetTracking()
                velocity.addPosition(down.uptimeMillis, rootDown)
                var travel = Offset.Zero
                var started = false
                var routedDrag: PlayerUpDrag? = null
                fun finish(speed: Float, cancelled: Boolean = false) {
                    routedDrag?.end?.invoke(speed, cancelled) ?: controller?.end(speed, cancelled)
                }
                try {
                    while (true) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                            ?: break
                        velocity.addPosition(change.uptimeMillis, coordinates?.localToRoot(change.position) ?: change.position)
                        if (change.isConsumed) break
                        if (!change.pressed) {
                            if (started) { finish(velocity.calculateVelocity().y); started = false }
                            break
                        }
                        val delta = change.positionChange()
                        if (!started) {
                            travel += delta
                            if (abs(travel.x) > viewConfiguration.touchSlop && abs(travel.x) >= abs(travel.y)) break
                            if (abs(travel.y) <= viewConfiguration.touchSlop) continue
                            routedDrag = if (travel.y < 0) upward else controlsDrag
                            if (routedDrag == null && controller == null) break
                            routedDrag?.begin?.invoke() ?: controller?.begin(fromMini = false)
                            started = true
                            val amount = travel.y - sign(travel.y) * viewConfiguration.touchSlop
                            routedDrag?.drag?.invoke(amount) ?: controller?.drag(amount)
                        } else routedDrag?.drag?.invoke(delta.y) ?: controller?.drag(delta.y)
                        change.consume()
                    }
                } finally {
                    if (started) finish(0f, cancelled = true)
                }
            }
            return@pointerInput
        }
        if (controller == null) return@pointerInput
        detectDragGestures(orientationLock = Orientation.Vertical,
            onDragStart = { down, trigger, _ ->
                velocity.resetTracking()
                velocity.addPosition(down.uptimeMillis, coordinates?.localToRoot(down.position) ?: down.position)
                velocity.addPosition(trigger.uptimeMillis, coordinates?.localToRoot(trigger.position) ?: trigger.position)
                controller.begin(fromMini)
            },
            onDrag = { change, amount ->
                velocity.addPosition(change.uptimeMillis, coordinates?.localToRoot(change.position) ?: change.position)
                change.consume()
                controller.drag(amount.y)
            },
            onDragEnd = { change ->
                velocity.addPosition(change.uptimeMillis, coordinates?.localToRoot(change.position) ?: change.position)
                controller.end(velocity.calculateVelocity().y)
            },
            onDragCancel = { controller.end(0f, cancelled = true) })
    }
}
