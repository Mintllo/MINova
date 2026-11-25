/*
 * Ported from CurrentMusicX (https://github.com/bileizhen/CurrentMusicX)
 * original packages:
 *   ui.component.nativeui.GlassPopupMotion.kt  (spring/droplet motion math)
 *   ui.component.MusicOverlayDialog.kt / MusicDialog.kt / MusicDialogStyle.kt /
 *   MusicDialogLayout.kt / MusicDialogPress.kt (liquid panel + rows)
 * License: GPL-3.0 (upstream). Adapted for MINova: host rebuilt on the platform Dialog +
 * kyant0 backdrop material (same drawBackdrop pattern as MD3StyleBottomSheetConsole).
 */
package com.HyperX.MI.Na.cmx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// GlassPopupMotion math (verbatim port, renamed)
// ─────────────────────────────────────────────────────────────────────────────

data class CmxPopupFrame(
    val bounds: Rect, val radius: Float, val droplet: Rect,
    val content: Float, val glyph: Float, val progress: Float,
    val blend: Float = 0f, val frost: Float = 2.5f,
)

internal fun cmxGlassSpring(time: Float, stiffness: Float, damping: Float, start: Float = 0f,
    end: Float = 1f, velocity: Float = 0f): Float {
    val omega = sqrt(stiffness); val decay = damping * omega
    val frequency = omega * sqrt((1 - damping * damping).coerceAtLeast(.000001f))
    val offset = start - end
    return end + exp(-decay * time) * (offset * cos(frequency * time) +
        (velocity + decay * offset) / frequency * sin(frequency * time))
}

internal fun cmxGlassRiseAndFall(time: Float, peak: Float, sharpness: Float): Float {
    if (time <= 0f) return 0f
    val x = time / peak
    return Math.pow(x.toDouble(), sharpness.toDouble()).toFloat() * exp(sharpness * (1 - x))
}

internal fun cmxGlassLerp(a: Float, b: Float, t: Float) = a + (b - a) * t

internal fun cmxGlassLerp(a: Rect, b: Rect, t: Float) = Rect(
    cmxGlassLerp(a.left, b.left, t), cmxGlassLerp(a.top, b.top, t),
    cmxGlassLerp(a.right, b.right, t), cmxGlassLerp(a.bottom, b.bottom, t),
)

private fun cmxRectAt(center: Offset, width: Float, height: Float): Rect = Rect(
    center - Offset(width.coerceAtLeast(0f) / 2, height.coerceAtLeast(0f) / 2),
    Size(width.coerceAtLeast(0f), height.coerceAtLeast(0f)),
)

internal fun cmxGlassOpening(anchor: Rect, card: Rect, time: Float, density: Float): CmxPopupFrame {
    val p = cmxGlassSpring(time, 400f, .754f)
    val scale = cmxGlassLerp(.41f * min(anchor.width, anchor.height) / card.width, 1f, p)
    val height = cmxGlassLerp(card.width, card.height, p)
    val distance = card.center.y - anchor.center.y
    val dip = sign(distance) * (min(1f, abs(distance) / (30 * density)) * 23 * density + .124f * abs(distance)) *
        cmxGlassRiseAndFall(time, .098f, 3.3f)
    val center = anchor.center * (1 - p) + card.center * p + Offset(0f, dip)
    val corners = 24 * density + card.width * .48f * cmxGlassSpring(maxOf(0f, time - .07f), 324f, .75f, 1f, 0f)
    val extent = cmxGlassLerp(1f, .25f, p)
    val inner = card.deflate(maxOf(anchor.width, anchor.height) * .25f / 2 + (28 + 8) * density)
    val sunk = anchor.center * .75f + card.center * .25f
    val target = if (inner.isEmpty) card.center
    else Offset(sunk.x.coerceIn(inner.left, inner.right), sunk.y.coerceIn(inner.top, inner.bottom))
    return CmxPopupFrame(
        cmxRectAt(center, card.width * scale, height * scale),
        maxOf(0f, min(height / 2, corners) * scale),
        cmxRectAt(anchor.center * (1 - p) + target * p, anchor.width * extent, anchor.height * extent),
        p.coerceIn(0f, 1f), 1f - (p / .25f).coerceIn(0f, 1f), p, 28 * density, 12f,
    )
}

private val cmxPopupEaseIn = CubicBezierEasing(.42f, 0f, 1f, 1f)
private val cmxGlyphEase = CubicBezierEasing(.25f, .1f, .25f, 1f)
private val cmxAbsorptionEase = CubicBezierEasing(.42f, 0f, .58f, 1f)

internal fun cmxGlassReturning(anchor: Rect, start: CmxPopupFrame, time: Float, density: Float): CmxPopupFrame {
    val ringing = 1 - cmxPopupEaseIn.transform(((time - .4f) / .1f).coerceIn(0f, 1f))
    val p = 1 + (cmxGlassSpring(time, 165f, .8f) - 1) * ringing
    val from = start.bounds
    val distance = from.center.y - anchor.center.y
    val dip = min(.21f * abs(distance), 22 * density) * cmxGlassRiseAndFall(time, .072f, 2.4f)
    val lift = min(.25f * abs(distance), 19 * density) * cmxGlassRiseAndFall(time, .176f, 4.9f)
    val scale = cmxGlassLerp(1f, .41f * min(anchor.width, anchor.height) / maxOf(from.width, from.height), p)
    val platter = cmxRectAt(
        from.center * (1 - p) + anchor.center * p + Offset(0f, sign(distance) * dip * ringing),
        from.width * scale, cmxGlassLerp(from.height, from.width, p) * scale,
    )
    return CmxPopupFrame(
        platter,
        maxOf(0f, cmxGlassLerp(start.radius, from.width / 2, p) * scale),
        cmxRectAt(
            start.droplet.center * (1 - p) + anchor.center * p - Offset(0f, sign(distance) * lift * ringing),
            cmxGlassLerp(start.droplet.width, anchor.width, p),
            cmxGlassLerp(start.droplet.height, anchor.height, p),
        ),
        start.content * (1 - time / .1f).coerceIn(0f, 1f),
        .2f + .8f * cmxGlyphEase.transform((time / .47f).coerceIn(0f, 1f)),
        1 - p,
        28 * density * min(1f, time / .05f) * (1 - cmxAbsorptionEase.transform(((time / .5f - .6f) / .3f).coerceIn(0f, 1f))),
        cmxGlassLerp(12f, 2.5f, cmxAbsorptionEase.transform(((time / .5f - .6f) / .3f).coerceIn(0f, 1f))),
    )
}

/** Captures the painted frame before retargeting (interrupted presses keep continuity). */
class CmxPopupMotion {
    val clock = Animatable(1f)
    var opening by mutableStateOf(false)
        private set
    private var from by mutableStateOf<CmxPopupFrame?>(null)
    var shown by mutableStateOf(false)
        private set

    fun frame(anchor: Rect, card: Rect, density: Float): CmxPopupFrame {
        val settled = CmxPopupFrame(card, 24 * density,
            cmxRectAt(anchor.center * .75f + card.center * .25f, anchor.width * .25f, anchor.height * .25f),
            1f, 0f, 1f, 28 * density, 12f)
        if (opening) {
            if (clock.value >= 1f) return settled
            return cmxGlassOpening(anchor, card, clock.value * .45f, density)
        }
        return cmxGlassReturning(anchor, from ?: settled, clock.value * .5f, density)
    }

    suspend fun animate(open: Boolean, anchor: Rect, card: Rect, density: Float) {
        val previous = if (shown) frame(anchor, card, density) else null
        clock.stop()
        clock.snapTo(0f)
        from = previous
        opening = open
        shown = true
        clock.animateTo(1f, tween(if (open) 450 else 500, easing = LinearEasing))
        if (!open) shown = false
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Liquid dialog host — spring panel over a scrim, frosted material from kyant0
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CmxLiquidDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String?,
    backdrop: Backdrop? = null,
    maxHeight: Int = 420,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Retain content through the exit animation (upstream DialogLayout behaviour).
    var hostShown by remember { mutableStateOf(false) }
    var animateOpen by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) { hostShown = true; animateOpen = true } else animateOpen = false
    }
    if (!hostShown) return
    val dismiss = rememberUpdatedState(onDismiss)
    val motion = remember { CmxPopupMotion() }
    val density = LocalDensity.current
    var cardSize by remember { mutableStateOf(Size(1f, 1f)) }
    var cardTopLeft by remember { mutableStateOf(Offset.Zero) }
    val rootH = LocalView.current.height.toFloat()
    LaunchedEffect(hostShown, animateOpen) {
        if (animateOpen) {
            // 面板量到真实尺寸后才起弹簧，首帧不会从 (1,1) 的退化矩形起飞
            withFrameNanos { }
            val anchor = defaultAnchor(cardSize, cardTopLeft, rootH, density.density)
            motion.animate(true, anchor, Rect(cardTopLeft, cardSize), density.density)
        } else if (motion.shown) {
            val anchor = defaultAnchor(cardSize, cardTopLeft, rootH, density.density)
            motion.animate(false, anchor, Rect(cardTopLeft, cardSize), density.density)
            hostShown = false
        }
    }

    Dialog(
        onDismissRequest = { if (visible) dismiss.value() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val frame = motion.frame(defaultAnchor(cardSize, cardTopLeft, rootH, density.density), Rect(cardTopLeft, cardSize), density.density)
        val p = frame.progress.coerceIn(0f, 1f)
        val pressS = com.HyperX.MI.Na.nativeui.rememberElasticPressState()
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = .38f * p))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onClick = { if (visible) dismiss.value() }),
            contentAlignment = Alignment.Center,
        ) {
            com.HyperX.MI.Na.nativeui.ElasticPress(state = pressS) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        cardSize = Size(it.size.width.toFloat().coerceAtLeast(1f), it.size.height.toFloat().coerceAtLeast(1f))
                        cardTopLeft = it.boundsInWindow().topLeft
                    }
                    .graphicsLayer {
                        val card = Rect(cardTopLeft, cardSize)
                        val anchor = defaultAnchor(cardSize, cardTopLeft, rootH, density.density)
                        val f = motion.frame(anchor, card, density.density)
                        val sx = f.bounds.width / card.width
                        val sy = f.bounds.height / card.height
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                        translationX = f.bounds.left - card.left
                        translationY = f.bounds.top - card.top
                        scaleX = sx; scaleY = sy
                        alpha = f.content.coerceIn(0f, 1f)
                    }
                    .then(liquidDialogMaterial(backdrop, RoundedCornerShape(32.dp)))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                title?.let {
                    Text(
                        it, Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Column(
                    Modifier.fillMaxWidth().heightIn(max = maxHeight.dp).verticalScroll(rememberScrollState()),
                    content = content,
                )
            }
            }
        }
    }
}

private fun defaultAnchor(cardSize: Size, cardTopLeft: Offset, rootHeight: Float, density: Float): Rect {
    // Springs up from the bottom-centre trigger (mini-console origin).
    val cx = cardTopLeft.x + cardSize.width / 2
    val bottom = rootHeight.coerceAtLeast(cardTopLeft.y + cardSize.height)
    val half = 22f * density
    return Rect(cx - half, bottom - 2 * half, cx + half, bottom)
}

/** 新液态磨砂：backdrop 可用时叠真模糊，否则高 alpha 磨砂近似；无 lens/vibrancy/边缘高光。 */
@Composable
private fun liquidDialogMaterial(backdrop: Backdrop?, shape: Shape): Modifier {
    val frost = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.60f)
    return if (backdrop != null) Modifier.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = { blur(18f.dp.toPx()) },
        onDrawSurface = { drawRect(frost) },
    ) else Modifier
        .background(frost, shape)
        .border(1.dp, Color.White.copy(alpha = 0.26f), shape)
}

@Composable
private fun dialogFallbackColor(): Color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f)

// ─────────────────────────────────────────────────────────────────────────────
// Rows / buttons (MusicDialogStyle.kt + MusicDialogLayout.kt)
// ─────────────────────────────────────────────────────────────────────────────

internal class CmxDialogPress(val source: MutableInteractionSource, val amount: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>)

internal fun Modifier.cmxDialogRowPress(feedback: CmxDialogPress): Modifier = pointerInput(feedback) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val press = PressInteraction.Press(down.position)
        feedback.source.tryEmit(press)
        val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
        feedback.source.tryEmit(if (up != null) PressInteraction.Release(press) else PressInteraction.Cancel(press))
    }
}

@Composable
internal fun rememberCmxDialogPress(): CmxDialogPress {
    val feedback = remember { CmxDialogPress(MutableInteractionSource(), Animatable(0f)) }
    LaunchedEffect(feedback) {
        var pulse: Job? = null
        feedback.source.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pulse?.cancel()
                    pulse = launch { feedback.amount.animateTo(1f, tween(65)) }
                }
                is PressInteraction.Release -> {
                    pulse?.cancel()
                    pulse = launch {
                        feedback.amount.animateTo(1f, tween(35))
                        delay(35)
                        feedback.amount.animateTo(0f, tween(180))
                    }
                }
                is PressInteraction.Cancel -> {
                    pulse?.cancel()
                    pulse = launch { feedback.amount.animateTo(0f, tween(100)) }
                }
            }
        }
    }
    return feedback
}

/** Full-panel-width hit target, press wash, 24dp inset, hairline divider. */
@Composable
fun CmxDialogActionRow(
    label: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    onClick: () -> Unit = {},
) {
    val press = rememberCmxDialogPress()
    val ink = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    Row(
        modifier.fillMaxWidth().drawBehind {
            drawRect(ink.copy(alpha = .06f * press.amount.value))
            if (showDivider) drawLine(
                ink.copy(alpha = .06f),
                Offset(24.dp.toPx(), size.height), Offset(size.width - 24.dp.toPx(), size.height), .5.dp.toPx(),
            )
        }
            .then(if (enabled) Modifier.cmxDialogRowPress(press) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, interactionSource = press.source, indication = null, onClick = onClick)
            .heightIn(min = 48.dp).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            androidx.compose.material3.Text(
                label, color = ink.copy(alpha = if (enabled) 1f else .4f),
                fontSize = 15.sp, fontWeight = FontWeight.Medium,
            )
            summary?.let {
                androidx.compose.material3.Text(
                    it, color = ink.copy(alpha = .55f), fontSize = 12.sp, lineHeight = 18.sp,
                )
            }
        }
        androidx.compose.material3.Text(
            "›", color = ink.copy(alpha = .35f), fontSize = 18.sp, fontWeight = FontWeight.Medium,
        )
    }
}

/** Pill button with the upstream press wash (MusicDialogButton). */
@Composable
fun CmxDialogButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, destructive: Boolean = false) {
    val press = rememberCmxDialogPress()
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    val ink = if (destructive) scheme.error else scheme.onSurface
    Box(
        modifier.fillMaxWidth().heightIn(min = 44.dp)
            .graphicsLayer { scaleX = 1f - .025f * press.amount.value; scaleY = scaleX }
            .clip(CircleShape).background(scheme.onSurface.copy(alpha = .08f))
            .drawBehind { drawRect(ink.copy(alpha = .055f * press.amount.value)) }
            .clickable(role = Role.Button, interactionSource = press.source, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text, color = ink, textAlign = TextAlign.Center,
            fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium,
        )
    }
}
