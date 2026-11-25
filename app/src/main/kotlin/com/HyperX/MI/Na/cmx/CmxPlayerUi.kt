/*
 * Ported from CurrentMusicX (https://github.com/bileizhen/CurrentMusicX)
 * original package: io.github.currencortex.music.feature.player / ui.component
 * files: PlayerControls.kt, PlayerSheetControls.kt, PlayerSeekGlow.kt, ArtworkGlowPalette.kt
 * License: GPL-3.0 (upstream). Adapted for MINova (com.HyperX.MI.Na) — data wiring kept local.
 */
package com.HyperX.MI.Na.cmx

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.HyperX.MI.Na.R
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

// ─────────────────────────────────────────────────────────────────────────────
// Time / icons
// ─────────────────────────────────────────────────────────────────────────────

fun formatCmxTime(value: Long): String {
    val seconds = value.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

/** CMX PlayerIcon — the same symbol set drives header, function bar and transport. */
enum class CmxPlayerIcon { COLLAPSE, MORE, SHARE, EFFECTS, LYRICS, QUEUE, LIKE, COMMENT, ALARM, REPEAT, REPEAT_ONE, SHUFFLE }

private fun cmxIconRes(icon: CmxPlayerIcon, selected: Boolean) = when (icon) {
    CmxPlayerIcon.COLLAPSE -> R.drawable.player_symbol_keyboard_arrow_down
    CmxPlayerIcon.MORE -> R.drawable.player_symbol_more_horiz
    CmxPlayerIcon.SHARE -> R.drawable.player_symbol_share
    CmxPlayerIcon.EFFECTS -> R.drawable.player_symbol_graphic_eq
    CmxPlayerIcon.LYRICS -> R.drawable.player_symbol_lyrics
    CmxPlayerIcon.QUEUE -> R.drawable.player_symbol_queue_music
    CmxPlayerIcon.LIKE -> if (selected) R.drawable.player_symbol_favorite_fill1 else R.drawable.player_symbol_favorite
    CmxPlayerIcon.COMMENT -> R.drawable.player_symbol_chat_bubble
    CmxPlayerIcon.ALARM -> R.drawable.player_symbol_alarm
    CmxPlayerIcon.REPEAT -> R.drawable.player_symbol_repeat
    CmxPlayerIcon.REPEAT_ONE -> R.drawable.player_symbol_repeat_one
    CmxPlayerIcon.SHUFFLE -> R.drawable.player_symbol_shuffle
}

// ─────────────────────────────────────────────────────────────────────────────
// Buttons — 48dp targets, 24dp symbols, 65% idle ink, .88 press scale
// (PlayerControls.kt PlayerIconButton / PlayerTransportButton)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CmxPlayerIconButton(
    icon: CmxPlayerIcon,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val interactions = remember { MutableInteractionSource() }
    // 真液态（对齐排序芯片）：ElasticPress 拽动果冻 + 玻璃半透明白 .14 面 + 白 .26 细描边 + 按压高光
    val pressS = com.HyperX.MI.Na.nativeui.rememberElasticPressState()
    val jellyS = remember { Animatable(1f) }
    LaunchedEffect(pressS) {
        snapshotFlow { pressS.amount > .45f }.distinctUntilChanged().collect { down ->
            if (down) jellyS.animateTo(.92f, spring(dampingRatio = .62f, stiffness = 620f))
            else {
                jellyS.animateTo(1.06f, spring(dampingRatio = .38f, stiffness = 380f))
                jellyS.animateTo(1f, spring(dampingRatio = .55f, stiffness = 300f))
            }
        }
    }
    val opacity = animateFloatAsState(if (selected) 1f else .65f, tween(160), label = "cmx action selection")
    Box(
        modifier.size(48.dp).combinedClickable(
            enabled = enabled, role = Role.Button, interactionSource = interactions,
            indication = null, onClick = onClick,
        ).semantics {
            contentDescription = label
            if (!enabled) disabled()
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(48.dp).graphicsLayer {
                val amount = pressS.amount.coerceIn(0f, 1f)
                val net = jellyS.value / (1f + amount * .08f)
                scaleX = net; scaleY = net
                translationX = pressS.pull.x * .3f
                translationY = pressS.pull.y * .3f
            }
                .clip(CircleShape)
                .drawBehind { if (pressS.amount > 0f) drawCircle(Color.White.copy(alpha = .06f * pressS.amount.coerceIn(0f, 1f))) },
            contentAlignment = Alignment.Center,
        ) {
            val ink = (if (icon == CmxPlayerIcon.LIKE && selected) Color(0xFFFF647C) else Color.White)
                .copy(alpha = opacity.value * if (enabled) 1f else .4f)
            Image(
                painterResource(cmxIconRes(icon, selected)), null,
                Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(ink),
            )
        }
    }
}

@Composable
fun CmxTransportButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    playing: Boolean = false,
    direction: Int = 0,
) {
    val interactions = remember { MutableInteractionSource() }
    // 真液态（对齐排序芯片）：ElasticPress 拽动果冻 + 玻璃半透明白 .14 面 + 白 .26 细描边 + 按压高光
    val pressS = com.HyperX.MI.Na.nativeui.rememberElasticPressState()
    val jellyS = remember { Animatable(1f) }
    LaunchedEffect(pressS) {
        snapshotFlow { pressS.amount > .45f }.distinctUntilChanged().collect { down ->
            if (down) jellyS.animateTo(.92f, spring(dampingRatio = .62f, stiffness = 620f))
            else {
                jellyS.animateTo(1.06f, spring(dampingRatio = .38f, stiffness = 380f))
                jellyS.animateTo(1f, spring(dampingRatio = .55f, stiffness = 300f))
            }
        }
    }
    val glyph = animateFloatAsState(if (playing) 1f else 0f, tween(180, easing = LinearEasing), label = "cmx play pause glyph")
    Box(
        modifier.size(68.dp).clickable(
            enabled = enabled, role = Role.Button, interactionSource = interactions,
            indication = null, onClick = onClick,
        ).semantics {
            contentDescription = label
            if (!enabled) disabled()
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(68.dp).graphicsLayer {
                val amount = pressS.amount.coerceIn(0f, 1f)
                val net = jellyS.value / (1f + amount * .08f)
                scaleX = net; scaleY = net
                translationX = pressS.pull.x * .3f
                translationY = pressS.pull.y * .3f
            }
                .clip(CircleShape)
                .drawBehind { if (pressS.amount > 0f) drawCircle(Color.White.copy(alpha = .06f * pressS.amount.coerceIn(0f, 1f))) },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(if (direction == 0) 40.dp else 30.dp).graphicsLayer {
                    translationX = direction * 3.dp.toPx() * pressS.amount.coerceIn(0f, 1f)
                }
            ) {
            val ink = Color.White.copy(alpha = if (enabled) 1f else .28f)
            if (direction == 0) {
                CmxSharedControlIcon(CmxSharedControl.TOGGLE, ink, Modifier.fillMaxSize(), playing = playing)
            } else Image(
                painterResource(if (direction < 0) R.drawable.player_symbol_skip_previous_fill1 else R.drawable.player_symbol_skip_next_fill1),
                null, Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(ink),
            )
            }
        }
    }
}

enum class CmxSharedControl { TOGGLE, QUEUE }

/** The same vector glyphs, crossfaded in a Canvas (PlayerSheetControls.kt PlayerControlGlyph). */
@Composable
fun CmxSharedControlIcon(kind: CmxSharedControl, color: Color, modifier: Modifier, playing: Boolean = false) {
    val glyph = animateFloatAsState(if (playing) 1f else 0f, tween(180, easing = LinearEasing), label = "cmx shared play pause")
    val play = painterResource(R.drawable.player_symbol_play_arrow_fill1)
    val pause = painterResource(R.drawable.player_symbol_pause_fill1)
    val queue = painterResource(R.drawable.player_symbol_queue_music)
    Canvas(modifier) {
        val tint = ColorFilter.tint(color)
        if (kind == CmxSharedControl.QUEUE) {
            with(queue) { draw(size, colorFilter = tint) }
        } else {
            with(play) { draw(size, alpha = 1f - glyph.value, colorFilter = tint) }
            with(pause) { draw(size, alpha = glyph.value, colorFilter = tint) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Function bar — 56dp row, 24dp gap or even spread (PlayerControls.kt PlayerFunctionBar)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CmxFunctionBar(
    modifier: Modifier = Modifier,
    actionCount: Int = 3,
    content: @Composable RowScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth().height(56.dp)) {
        val gap = if (actionCount <= 3) 24.dp else ((maxWidth - 48.dp * actionCount) / (actionCount - 1)).coerceIn(0.dp, 12.dp)
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MINova 自研「恒亮辉光」进度条视觉（非上游 PlayerSeekGlow 的高斯拖尾光束）：
// 整条轨道恒亮底光（Modifier.blur 发光层，本工程自己的实现）+ 已播段白黄辉光条 +
// 未播段暗轨道 + 播放头小圆点带径向光晕。无逐帧蒙版位图，无 Gaussian mask。
// ─────────────────────────────────────────────────────────────────────────────

/** 已播段白黄辉光条的暖金端（与全工程白黄主题一致的固定色，不随上游取色拖尾）。 */
private val SeekPlayedGlowGold = Color(0xFFFFD97A)
private val SeekPlayedGlowWhite = Color(0xFFFFFDF4)

@Composable
fun CmxSeekBar(
    value: Float,
    enabled: Boolean,
    onPreview: (Float) -> Unit,
    onSeek: (Float) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    bufferedValue: Float = 0f,
    tint: Color = Color.White,
) {
    val preview by rememberUpdatedState(onPreview)
    val seek by rememberUpdatedState(onSeek)
    val cancel by rememberUpdatedState(onCancel)
    val progress = value.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0f
    val currentProgress by rememberUpdatedState(progress)
    val buffer = bufferedValue.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0f
    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var target by remember { mutableFloatStateOf(progress) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    val glowTint by animateColorAsState(lerp(tint, Color.White, .25f), tween(600), label = "cmx seek tint")
    val thickness = animateFloatAsState(
        if (dragging) 6f else 4f,
        tween(if (dragging) 220 else 280, easing = LinearEasing), label = "cmx seek press",
    )
    // 整条轨道恒亮辉光：只要可用就常亮（拖动中也保持），是「恒亮」语言的核心
    val trackGlow = animateFloatAsState(
        if (enabled) 1f else 0f,
        tween(if (enabled) 280 else 220, easing = LinearEasing), label = "cmx seek track glow",
    )
    val buffered = animateFloatAsState(buffer, tween(180), label = "cmx audio buffer progress")
    Box(modifier.fillMaxWidth().height(34.dp)) {
        // 自研发光层：轨道形状单独画进一层，Modifier.blur(10dp) 真模糊，铺在清晰轨道底层。
        // 整条恒亮 + 已播段主光 + 播放头点光，一次 blur 层完成（区别于上游头部拖尾光束）。
        Canvas(Modifier.matchParentSize().blur(10.dp)) {
            if (trackGlow.value > 0f) {
                val y = size.height / 2
                val end = size.width * if (pressed) target else progress
                val stroke = thickness.value.dp.toPx()
                val alpha = trackGlow.value
                // 恒亮底光：整条轨道一层暖白微光
                drawLine(glowTint.copy(alpha = .35f * alpha), Offset(0f, y), Offset(size.width, y),
                    stroke * 2.6f, StrokeCapRound())
                // 已播段主光：白黄辉光条在模糊层加浓
                if (end > 0f) drawLine(SeekPlayedGlowGold.copy(alpha = .8f * alpha), Offset(0f, y),
                    Offset(end, y), stroke * 2.4f, StrokeCapRound())
                // 播放头点光：光晕中心跟随播放头
                if (enabled) drawCircle(SeekPlayedGlowWhite.copy(alpha = .85f * alpha), stroke * 1.6f,
                    Offset(end, y))
            }
        }
    Canvas(
        Modifier.matchParentSize().onSizeChanged { bounds = it }.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(if (pressed) target else progress, 0f..1f)
            contentDescription = "播放进度"
            stateDescription = "已加载 ${(buffer * 100).toInt()}%"
        }.pointerInput(enabled, bounds) {
            if (!enabled || size.width <= 0) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (down.isConsumed) return@awaitEachGesture
                val width = size.width.toFloat()
                val initial = currentProgress
                val slop = viewConfiguration.touchSlop
                var previousX = down.position.x
                var committed = false
                pressed = true
                target = initial
                preview(target)
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.isConsumed || event.changes.any { it.id != down.id && it.pressed }) break
                        val dx = change.position.x - down.position.x
                        val dy = change.position.y - down.position.y
                        if (!change.pressed) {
                            val quickTap = !dragging && change.uptimeMillis - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis &&
                                abs(dx) <= slop && abs(dy) <= slop &&
                                change.position.x in 0f..width && change.position.y in 0f..size.height.toFloat()
                            if (dragging || quickTap) {
                                change.consume()
                                committed = true
                                seek(if (dragging) target else (change.position.x / width).coerceIn(0f, 1f))
                            }
                            break
                        }
                        if (!dragging) {
                            if (abs(dy) > slop && abs(dy) >= abs(dx)) break
                            if (abs(dx) > slop) {
                                dragging = true
                                target = (initial + dx / width).coerceIn(0f, 1f)
                            } else if (change.position.x !in 0f..width || change.position.y !in 0f..size.height.toFloat()) break
                        } else {
                            target = (target + (change.position.x - previousX) / width).coerceIn(0f, 1f)
                        }
                        previousX = change.position.x
                        if (dragging) {
                            change.consume()
                            preview(target)
                        }
                        val final = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Final)
                        if (!dragging && final.changes.firstOrNull { it.id == down.id }?.isConsumed == true) break
                    }
                } finally {
                    pressed = false
                    dragging = false
                    if (!committed) cancel()
                }
            }
        },
    ) {
        val y = size.height / 2
        val end = size.width * if (pressed) target else progress
        val stroke = thickness.value.dp.toPx()
        // 清晰层：暗轨道 + 白黄辉光已播段 + 小圆点播放头（恒亮底光在 blur 层）
        drawLine(Color.White.copy(alpha = .14f), Offset(0f, y), Offset(size.width, y), stroke, StrokeCapRound())
        if (buffered.value > 0f) drawLine(
            Color.White.copy(alpha = .30f), Offset(0f, y),
            Offset(size.width * buffered.value, y), stroke, StrokeCapRound(),
        )
        if (end > 0f) {
            val playedBrush = Brush.horizontalGradient(
                listOf(SeekPlayedGlowWhite, SeekPlayedGlowGold),
                startX = 0f, endX = maxOf(end, 1f),
            )
            drawLine(playedBrush, Offset(0f, y), Offset(end, y), stroke, StrokeCapRound())
        }
        if (enabled) {
            // 播放头：径向光晕 + 白色小圆点（无拖尾、无蒙版）
            drawCircle(Brush.radialGradient(
                listOf(Color.White.copy(alpha = .5f), Color.Transparent), center = Offset(end, y),
                radius = stroke * 4f), radius = stroke * 4f, center = Offset(end, y))
            drawCircle(SeekPlayedGlowWhite, stroke * 1.1f, Offset(end, y))
        }
    }
    }
}

private fun StrokeCapRound() = androidx.compose.ui.graphics.StrokeCap.Round

// ─────────────────────────────────────────────────────────────────────────────
// Artwork palette (ArtworkGlowPalette.kt) — cover-derived hue for seek tint
// ─────────────────────────────────────────────────────────────────────────────

data class CmxArtworkPalette(val primary: Color, val secondary: Color) {
    companion object {
        val Neutral = CmxArtworkPalette(Color(0xff9da9b6), Color(0xffb3a7b6))
    }
}

private fun paletteChroma(color: Int): Int {
    val channels = listOf(16, 8, 0).map { color ushr it and 255 }
    return channels.max() - channels.min()
}

private fun paletteFromPixels(pixels: IntArray): CmxArtworkPalette {
    data class CoverColour(val argb: Int, val population: Int)
    val counts = IntArray(4096)
    val red = LongArray(4096); val green = LongArray(4096); val blue = LongArray(4096)
    pixels.forEach { color ->
        if ((color ushr 24) < 128) return@forEach
        val r = color ushr 16 and 255; val g = color ushr 8 and 255; val b = color and 255
        val max = maxOf(r, g, b); val min = minOf(r, g, b)
        if (max < 28 || min > 225) return@forEach
        val bucket = (r / 16 shl 8) or (g / 16 shl 4) or (b / 16)
        counts[bucket]++; red[bucket] += r.toLong(); green[bucket] += g.toLong(); blue[bucket] += b.toLong()
    }
    val candidates = counts.indices.filter { counts[it] > 0 }.sortedByDescending { i ->
        val chroma = (maxOf(red[i], green[i], blue[i]) - minOf(red[i], green[i], blue[i])).toFloat() / (counts[i] * 255)
        counts[i] * (.25f + chroma)
    }.map { i ->
        val r = (red[i] / counts[i]).toInt(); val g = (green[i] / counts[i]).toInt(); val b = (blue[i] / counts[i]).toInt()
        val max = maxOf(r, g, b).toFloat()
        val spread = max - minOf(r, g, b)
        val saturationBoost = if (spread < 12f) 1f else 1.12f
        val brighten = maxOf(1f, 205f / max)
        fun channel(value: Int) = ((max - (max - value) * saturationBoost) * brighten).toInt().coerceIn(0, 255)
        CoverColour((255 shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b), counts[i])
    }
    val leading = candidates.firstOrNull() ?: return CmxArtworkPalette.Neutral
    val minimumAccentPopulation = maxOf(2, leading.population / 10)
    val dominant = if (paletteChroma(leading.argb) >= 32) leading else candidates.firstOrNull {
        it.population >= minimumAccentPopulation && paletteChroma(it.argb) >= 32
    } ?: leading
    val first = dominant.argb
    val minimumPopulation = maxOf(2, dominant.population / 10)
    val minimumChroma = if (paletteChroma(first) < 32) 0 else maxOf(32, paletteChroma(first) / 2)
    val second = candidates.firstOrNull { candidate ->
        val color = candidate.argb
        candidate.population >= minimumPopulation &&
            paletteChroma(color) >= minimumChroma &&
            listOf(16, 8, 0).sumOf { shift -> abs((color ushr shift and 255) - (first ushr shift and 255)) } > 110
    }?.argb ?: first
    return CmxArtworkPalette(Color(first), Color(second))
}

/** Loads a small software cover and extracts the two dominant artwork hues. */
@Composable
fun rememberCmxArtworkPalette(coverUrl: String): State<CmxArtworkPalette> {
    val context = LocalContext.current
    return produceState(CmxArtworkPalette.Neutral, context, coverUrl) {
        if (coverUrl.isBlank()) return@produceState
        delay(60)
        val result = runCatching {
            SingletonImageLoader.get(context).execute(
                ImageRequest.Builder(context).data(coverUrl).size(192).allowHardware(false).build()
            )
        }.getOrNull() as? SuccessResult
        if (result != null) {
            value = withContext(Dispatchers.Default) {
                runCatching {
                    val input = result.image.toBitmap()
                    val pixels = IntArray(input.width * input.height)
                    input.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
                    // C++ 下沉：调色板分桶/排序在 libminova 内完成，失败回退 Kotlin 循环。
                    val native = com.HyperX.MI.Na.native.NativeBridge.artworkPalette(pixels)
                    if (native != null) {
                        CmxArtworkPalette(Color(native.first), Color(native.second))
                    } else {
                        paletteFromPixels(pixels)
                    }
                }.getOrDefault(CmxArtworkPalette.Neutral)
            }
        }
    }
}

@Composable
fun rememberCmxSeekTint(coverUrl: String): State<Color> {
    val palette by rememberCmxArtworkPalette(coverUrl)
    return animateColorAsState(softenSeekTint(palette.primary), tween(600), label = "cmx seek artwork tint")
}

/** 封面取色直接用会太暗/太艳：向暖白靠拢 + 亮度/饱和度整流，得到柔和耐看的进度条色调。 */
private fun softenSeekTint(c: Color): Color {
    val lum = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
    var out = lerp(c, Color(0xFFF2F0EB), 0.42f)
    if (lum < 0.30f) out = lerp(out, Color.White, 0.22f)
    val mx = maxOf(out.red, out.green, out.blue)
    val mn = minOf(out.red, out.green, out.blue)
    val sat = if (mx > 0f) (mx - mn) / mx else 0f
    if (sat > 0.55f) out = lerp(out, Color.White, (sat - 0.55f) * 0.7f)
    return out.copy(alpha = 1f)
}
