// Bridge between MINova's playback console (MainActivity) and the CurrentMusicX
// PlayerSheetHost transition system (GPL-3.0, see _cmx_ref/PlayerSheetTransition.kt).
// The bridge owns the upstream sheet state machine (expansion, drag controller,
// artwork flight transfer) and exposes open/close requests so every entry point
// (tap, drag-up on console, system back, predictive back, drag-down on sheet)
// drives the exact upstream motion: tween 420ms sheetEasing open, 360ms
// collapseEasing close, quadratic element arc for the shared cover.
package com.HyperX.MI.Na.cmx

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal class CmxPlayerSheetBridge(
    val expansion: PlayerSheetState,
    val drag: PlayerSheetDragController,
    val artwork: PlayerArtworkTransition,
    private val scope: CoroutineScope,
) {
    var motion by mutableStateOf(PlayerSheetMotion.NONE)
    /** Console surface bounds (sheet expansion origin), reported by the console.
     *  转场飞行期间冻结（见 reportConsoleBounds）：展开/收起动画期间控制台必须
     *  静止在 sheet 背后，若逐帧追踪实时 bounds，sheet 的起点/落点会跟着控制台的
     *  退场动画跑，两条动画互相耦合、中途速度错位（顿挫的最主要根因）。 */
    var origin by mutableStateOf<PlayerSheetOrigin?>(null)
        private set
    /** Console cover bounds (shared artwork flight origin), reported by the console. */
    var artworkOrigin by mutableStateOf<PlayerArtworkOrigin?>(null)
    var viewport by mutableStateOf<Rect?>(null)
    /** Console tap-to-open area, used by the reopen hot zone during the close flight. */
    var consoleOpenBounds by mutableStateOf<Rect?>(null)
        private set

    /** 控制台上报自身 bounds。仅在静止（motion == NONE）时采纳：飞行期间控制台
     *  保持组合且静止，冻结起点让 sheet 展开几何与封面飞行起点全程稳定。 */
    fun reportConsoleBounds(origin: PlayerSheetOrigin?, openBounds: Rect?) {
        if (motion == PlayerSheetMotion.NONE) {
            this.origin = origin
            consoleOpenBounds = openBounds
        }
    }
    var openRequest: () -> Unit = {}
    var closeRequest: () -> Unit = {}

    /** Tap/drag entry: must set motion in the same snapshot as the open flag. */
    fun requestOpen() {
        // Upstream reopenNow pattern: UNDISPATCHED stop cancels the in-flight dock
        // animation on the calling frame, before the new open motion can start.
        scope.launch(start = CoroutineStart.UNDISPATCHED) { expansion.animation.stop() }
        motion = PlayerSheetMotion.OPEN
        openRequest()
    }

    fun requestClose() {
        motion = PlayerSheetMotion.CLOSE
        closeRequest()
    }
}

@Composable
internal fun rememberCmxPlayerSheetBridge(
    onOpenRequest: () -> Unit,
    onCloseRequest: () -> Unit,
): CmxPlayerSheetBridge {
    val expansion = remember { PlayerSheetState() }
    val scope = rememberCoroutineScope()
    val artwork = remember { PlayerArtworkTransition(expansion::value) }
    val bridge = remember(expansion) {
        CmxPlayerSheetBridge(expansion, PlayerSheetDragController(expansion, scope), artwork, scope)
    }
    val latestOpen by rememberUpdatedState(onOpenRequest)
    val latestClose by rememberUpdatedState(onCloseRequest)
    SideEffect {
        bridge.openRequest = latestOpen
        bridge.closeRequest = latestClose
        // Drag-up on the console opens, drag-down inside the player closes; both
        // funnel through the same motion flags as taps and system back.
        bridge.drag.onOpen = { bridge.requestOpen() }
        bridge.drag.onClose = { bridge.requestClose() }
        bridge.drag.onSettled = { bridge.motion = PlayerSheetMotion.NONE }
        bridge.drag.distance = {
            val vp = bridge.viewport
            val top = bridge.origin?.bounds?.top
            ((vp?.height ?: 2000f) - (top ?: 0f)).coerceAtLeast(1f)
        }
    }
    return bridge
}

/** Upstream PlayerSheetHost plus the drag CompositionLocal PlayerScreen consumes. */
@Composable
internal fun CmxFullScreenSheet(
    bridge: CmxPlayerSheetBridge,
    open: Boolean,
    artworkUrl: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    // 封面飞行起点接线：控制台 onCoverBounds 只上报到 bridge.artworkOrigin，而
    // PlayerArtworkFlight 只读 transfer.origin（PlayerArtworkTransition.origin），
    // 此前无人把两者接起来，origin 恒为空，封面从不起飞。这里在组合期同步。
    // 仅静止时同步：展开/收起动画期间控制台正在滑出/滑入，实时 bounds 会把
    // 起点拖偏；冻结后飞行始终从已落定的封面矩形起飞，并落回真实停靠位。
    val artworkOrigin = bridge.artworkOrigin
    SideEffect {
        // 展开时冻结起点：控制台正在滑出，实时 bounds 会把飞行起点拖偏。
        // 收起时必须反过来逐帧追踪：静止冻结值是上一轮退场动画尾帧的过时坐标
        // （控制台已随退场下移/缩放），用它当落点会让封面降到控制台下方。
        // 收起期间控制台正随入场动画滑入，每帧同步控制台实时上报的 bounds，
        // 进度归零时封面恰好贴合正在停靠的控制台封面（对齐 PlayerSheetDrag
        // 的 settle 语义：收起完成后封面精确落在控制台封面矩形上）。
        if (bridge.motion == PlayerSheetMotion.CLOSE ||
            (bridge.motion == PlayerSheetMotion.NONE && !bridge.artwork.moving)) {
            bridge.artwork.origin = artworkOrigin
        }
    }
    Box(Modifier.fillMaxSize().onGloballyPositioned { bridge.viewport = it.boundsInRoot() }) {
        CompositionLocalProvider(LocalPlayerSheetDrag provides bridge.drag) {
            PlayerSheetHost(
                open = open,
                motion = bridge.motion,
                expansion = bridge.expansion,
                origin = bridge.origin,
                viewport = bridge.viewport,
                artwork = bridge.artwork,
                artworkUrl = artworkUrl,
                predictiveBack = true,
                backEnabled = true,
                miniOpenBounds = bridge.consoleOpenBounds,
                onReopen = { bridge.requestOpen() },
                onBack = onBack,
                onSettled = { bridge.motion = PlayerSheetMotion.NONE },
            ) {
                content()
            }
        }
    }
}
