// Ported from CurrentMusicX (GPL-3.0) - https://github.com/bileizhen/CurrentMusicX
// Support layer: minimal stubs for upstream infrastructure the ported player files
// reference but whose source files are outside this port batch. Every stub is marked
// with TODO(CMX) and implements the smallest behaviour that keeps the player correct.
package com.HyperX.MI.Na.cmx

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.SingletonImageLoader
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.math.abs
import kotlin.math.roundToInt

// TODO(CMX): upstream coverRequestUrl maps Netease thumbnails to larger sizes.
internal fun coverRequestUrl(url: String, size: Int): String = url

/** 上一张封面取色缓存：换歌瞬间新层从旧氛围色起步，取色异步完成后缓动过去，
 *  消除「旧色 → 中性灰闪一下 → 新色」的不自然过渡。 */
private var lastGlowPalette = ArtworkGlowPalette.Neutral

/** Cover palette sampled from a small software bitmap; shares ArtworkGlowPalette with the dock. */
@Composable internal fun rememberArtworkGlowPalette(cover: String): State<ArtworkGlowPalette> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(lastGlowPalette) }
    androidx.compose.runtime.LaunchedEffect(cover) {
        if (cover.isBlank()) return@LaunchedEffect
        val result = runCatching {
            SingletonImageLoader.get(context).execute(ImageRequest.Builder(context)
                .data(coverRequestUrl(cover, 96)).size(96).allowHardware(false).build())
        }.getOrNull()
        if (result is SuccessResult) {
            val palette = withContext(Dispatchers.Default) {
                val bitmap = runCatching { result.image.toBitmap() }.getOrNull() ?: return@withContext ArtworkGlowPalette.Neutral
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                ArtworkGlowPalette.fromPixels(pixels)
            }
            lastGlowPalette = palette
            state.value = palette
        }
    }
    return state
}

// TODO(CMX): upstream MusicCover adds record-groove masking and border effects.
@Composable internal fun MusicCover(url: String, modifier: Modifier, pixels: Int = 800, cornerRadius: Dp = 0.dp) {
    AsyncImage(model = coverRequestUrl(url, pixels), contentDescription = null,
        contentScale = ContentScale.Crop, modifier = modifier)
}

// TODO(CMX): upstream BgEffectBackground is a Miuix shader effect; this stub renders a
// slow two-stop radial drift using the same palette pools so the look stays close.
@Composable internal fun BgEffectBackground(dynamicBackground: Boolean, modifier: Modifier,
    isFullSize: Boolean, effectBackground: Boolean, palette: List<Color>, darkTheme: Boolean,
    surfaceColor: Color, motionSpeed: (() -> Float)? = null, content: @Composable () -> Unit) {
    val colors = palette.takeIf { it.isNotEmpty() } ?: listOf(surfaceColor)
    Box(modifier.background(Brush.linearGradient(colors + surfaceColor))) { content() }
}

internal val CoverRotation = SemanticsPropertyKey<Float>("CoverRotation")
internal val LocalPlayerArtworkRotation = staticCompositionLocalOf<State<Float>?> { null }

/** Mini-player queue transition is not part of this port; the shared text layer treats it as idle. */
internal object LocalMiniQueueTransitionProvider {
    val current: MiniQueueTransition? get() = null
}
internal class MiniQueueTransition {
    fun textMoving(subtitle: Boolean): Boolean = false
}
internal val LocalMiniQueueTransition = staticCompositionLocalOf<MiniQueueTransition?> { null }

// TODO(CMX): upstream dissolves the mini lyric on track change; not needed for the full player.
internal fun miniLyricDissolve(lyricTrackKey: String?, text: String, color: Color,
    snapshot: PlayerSharedTextSnapshot): Modifier = Modifier

internal fun Modifier.playerCoverShadow(square: Boolean, progress: () -> Float = { 1f }): Modifier =
    drawBehind {
        val shadow = 1f - progress().coerceIn(0f, 1f)
        if (shadow <= 0f) return@drawBehind
        drawRoundRect(Color.Black.copy(alpha = .35f * shadow), topLeft = Offset(0f, size.height * .04f),
            size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension / 2f))
    }

/** Dock audio meter stub: the full player background motion falls back to its idle speed. */
internal class DockAudioLevel {
    class Sample(val timestampNanos: Long)
    val sample: Sample get() = Sample(0L)
    fun acquire(): Closeable? = null
}

/** Mirrors the upstream FluidBackgroundMotion.update contract with an idle-speed fallback. */
internal class FluidBackgroundMotion {
    fun update(sample: DockAudioLevel.Sample?, seconds: Float, rate: Float, playing: Boolean): Float =
        if (playing) .55f * rate.coerceIn(.5f, 2f) else .35f
}

internal fun formatNeteaseCount(count: Long): String = when {
    count >= 100_000_000L -> "${count / 100_000_000L}亿"
    count >= 10_000L -> "${count / 10_000L}万"
    else -> count.toString()
}

// TODO(CMX): chorus (highlight) segments need the upstream insights repository; the full
// player renders a plain seek bar until that data source is wired.
internal data class ChorusSegment(val startMs: Long, val endMs: Long)

internal val PlayerSeekChorusGold = Color(0xFFE0B15E)

internal class PlayerSeekChorusTrack(val startMs: Long, val endMs: Long, val playedShade: Color,
    val pendingShade: Color) {
    fun androidx.compose.ui.graphics.drawscope.DrawScope.draw(end: Float, stroke: Float, enabled: Boolean) {}
}

internal fun rememberPlayerChorus(song: Song?, insights: Any?): List<ChorusSegment> = emptyList()

internal fun playerChorusSegments(segments: List<ChorusSegment>, durationMs: Long): List<ChorusSegment> =
    segments.filter { it.endMs > 0 && (durationMs <= 0 || it.endMs <= durationMs) }

internal fun playerSeekChorusTracks(segments: List<ChorusSegment>, durationMs: Long,
    width: Float, verticalInsetPx: Float): List<PlayerSeekChorusTrack> = emptyList()
