/*
 * Ported from CurrentMusicX (https://github.com/bileizhen/CurrentMusicX)
 * original packages (feature/lyrics 下的 model/domain/timeline/ui，upstream = Folia AGPL-3.0 ports):
 *   model/LyricsDocument.kt, domain/LyricsSynchronizer.kt, timeline/LyricsPlaybackClock.kt
 *   ui/MonetLyricsRail.kt, ui/MonetLyricMotion.kt, ui/LyricsLineView.kt, ui/LyricsAuxiliaryText.kt,
 *   ui/LyricsInterludeView.kt, ui/KaraokeText.kt, ui/LyricsTextRaster.kt, ui/MonetSweepInk.kt,
 *   ui/MonetHaloInk.kt, ui/LyricsEffects.kt, ui/LyricsClusters.kt, ui/LyricsScreen.kt (classic path)
 * License: AGPL-3.0-only (Folia-derived parts) / GPL-3.0 (CurrentMusicX glue). Adapted for MINova:
 *   - data source = MINova LRC (LyricData times/text/translations), line-level timing (no word cues,
 *     no romanization) → the word-timed / phonetic branches are compiled but never taken.
 *   - smoke / particles / Apple-Music styles are not ported (upstream defaults off; our player
 *     exposes the classic Monet rail only).
 *   - upstream custom font family → FontFamily.Default.
 */
package com.HyperX.MI.Na.cmx

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds

import android.icu.text.BreakIterator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.scrollToIndex
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// 模型（LyricsDocument.kt 精简：LRC 行级数据 + KaraokeText 需要的字段）
// ─────────────────────────────────────────────────────────────────────────────

data class CmxLyricWord(val text: String, val startTimeMs: Long, val endTimeMs: Long,
    val startOffset: Int, val endOffset: Int,
    val agent: String? = null, val roles: List<String> = emptyList())

@androidx.compose.runtime.Immutable
data class CmxLyricLine(
    val startTimeMs: Long, val endTimeMs: Long, val text: String,
    val words: List<CmxLyricWord> = emptyList(),
    val translation: String = "", val romanization: String = "",
    val isBackground: Boolean = false, val isDuet: Boolean = false,
    val backgroundVocals: List<CmxLyricLine> = emptyList(),
    val wordsEstimated: Boolean = false,
    val roles: List<String> = emptyList(),
    val key: String? = null,
)

@androidx.compose.runtime.Immutable
data class CmxLyricsDocument(val lines: List<CmxLyricLine> = emptyList(), val durationMs: Long = 0) {
    val hasWordTiming: Boolean get() = lines.any { it.words.isNotEmpty() }
}

/** MINova LRC（行级时间 + 可选翻译）→ Monet 文档。 */
fun cmxDocumentFromLrc(
    times: List<Long>,
    lyrics: Map<Long, String>,
    translations: Map<Long, String>,
    durationMs: Long,
): CmxLyricsDocument {
    if (times.isEmpty()) return CmxLyricsDocument(emptyList(), durationMs)
    val lines = times.mapIndexed { index, start ->
        val end = times.getOrNull(index + 1)?.takeIf { it > start } ?: (start + 6000)
        CmxLyricLine(
            startTimeMs = start,
            endTimeMs = end,
            text = lyrics[start].orEmpty(),
            translation = translations[start].orEmpty(),
        )
    }.filter { it.text.isNotBlank() }
    return CmxLyricsDocument(lines, durationMs)
}

// ─────────────────────────────────────────────────────────────────────────────
// 设置等价物（data/settings 的最小字段；经典视图只读这些）
// ─────────────────────────────────────────────────────────────────────────────

enum class CmxLyricsWeight(val isBold: (Boolean) -> Boolean) {
    CURRENT({ current -> current }), ALL({ _ -> true }),
}

data class CmxLyricsDisplay(
    val centered: Boolean = true,
    val glow: Boolean = true,
    val hideControls: Boolean = false,
    val fontWeight: Int = 500,
)

object CmxLyricsTypography {
    const val DEFAULT_SIZE = 18f
    const val MAX_SIZE = 64f
    fun normalize(size: Float): Float = size.coerceIn(8f, MAX_SIZE)
}

// ─────────────────────────────────────────────────────────────────────────────
// 字体（LyricsFont.kt：无自定义字体 → Default；权重合成策略照抄）
// ─────────────────────────────────────────────────────────────────────────────

val CmxLyricsFontFamily = FontFamily.Default
val LocalCmxLyricsFont = staticCompositionLocalOf<FontFamily> { CmxLyricsFontFamily }

@Composable
fun currentCmxLyricsFontFamily(): FontFamily = LocalCmxLyricsFont.current

fun cmxLyricsFontWeight(mode: CmxLyricsWeight, current: Boolean, strength: Int = 500) =
    if (mode.isBold(current)) FontWeight(strength.coerceIn(400, 900)) else FontWeight.Normal

fun cmxLyricsFontSynthesis(weight: FontWeight) = if (weight.weight > 500)
    FontSynthesis.Weight else FontSynthesis.None

// ─────────────────────────────────────────────────────────────────────────────
// 时间轴（LyricsSynchronizer.kt 逐行；backgroundVocals 在 LRC 数据下恒空）
// ─────────────────────────────────────────────────────────────────────────────

data class CmxLyricsInterlude(val startTimeMs: Long, val endTimeMs: Long?, val label: String)

class CmxLyricsSynchronizer(private val document: CmxLyricsDocument) {
    private data class Interval(val index: Int, val start: Long, val end: Long)
    private val intervals = document.lines.mapIndexed { index, line ->
        Interval(index, minOf(line.startTimeMs, line.backgroundVocals.minOfOrNull { it.startTimeMs } ?: line.startTimeMs),
            maxOf(line.endTimeMs, line.backgroundVocals.maxOfOrNull { it.endTimeMs } ?: line.endTimeMs))
    }.sortedBy { it.start }
    private val prefixEnd = LongArray(intervals.size).also { result ->
        intervals.forEachIndexed { index, row -> result[index] = maxOf(row.end, result.getOrElse(index - 1) { 0 }) }
    }
    private val prefixEndIndex = IntArray(intervals.size).also { result ->
        intervals.forEachIndexed { index, row ->
            result[index] = if (index == 0 || row.end >= prefixEnd[index - 1]) row.index else result[index - 1]
        }
    }
    private fun lastStarted(positionMs: Long): Int {
        var low = 0; var high = intervals.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (intervals[mid].start <= positionMs) low = mid + 1 else high = mid - 1
        }
        return high
    }
    fun activeLines(positionMs: Long): List<Int> {
        val active = mutableListOf<Int>()
        var index = lastStarted(positionMs)
        while (index >= 0 && prefixEnd[index] > positionMs) {
            if (intervals[index].end > positionMs) active.add(intervals[index].index)
            index--
        }
        return active.sorted()
    }
    fun findCurrentLine(positionMs: Long): Int = activeLines(positionMs).lastOrNull() ?: -1
    fun scrollTarget(positionMs: Long): Int = findCurrentLine(positionMs).takeIf { it >= 0 }
        ?: prefixEndIndex.getOrNull(lastStarted(positionMs)) ?: intervals.firstOrNull()?.index ?: 0
    fun interlude(positionMs: Long): CmxLyricsInterlude? {
        if (intervals.isEmpty() || activeLines(positionMs).isNotEmpty()) return null
        val previous = lastStarted(positionMs)
        val begin = prefixEnd.getOrElse(previous) { 0 }
        val end = intervals.getOrNull(previous + 1)?.start ?: document.durationMs.takeIf { it > 0 }
        if (end != null && (end - begin < 2000 || positionMs >= end)) return null
        return CmxLyricsInterlude(begin, end, if (previous < 0) "前奏" else if (previous == intervals.lastIndex) "尾奏" else "间奏")
    }
    fun findCurrentWord(line: CmxLyricLine, positionMs: Long): Int {
        var low = 0; var high = line.words.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (line.words[mid].startTimeMs <= positionMs) low = mid + 1 else high = mid - 1
        }
        return high.takeIf { it >= 0 && positionMs < line.words[it].endTimeMs } ?: -1
    }
    companion object {
        fun wordProgress(word: CmxLyricWord, positionMs: Long): Float {
            if (word.endTimeMs <= word.startTimeMs) return if (positionMs >= word.endTimeMs) 1f else 0f
            return ((positionMs.toDouble() - word.startTimeMs) / (word.endTimeMs.toDouble() - word.startTimeMs)).toFloat().coerceIn(0f, 1f)
        }
        fun lyricPosition(positionMs: Long, offsetMs: Long): Long = positionMs + offsetMs
        fun effectivePosition(positionMs: Long, offsetMs: Long): Long = lyricPosition(positionMs, offsetMs).coerceAtLeast(0)
        fun seekPosition(lineStartMs: Long, offsetMs: Long): Long = (lineStartMs - offsetMs).coerceAtLeast(0)
        fun seekPositionOrNull(lineStartMs: Long, offsetMs: Long, mediaDurationMs: Long): Long? =
            seekPosition(lineStartMs, offsetMs).takeUnless { mediaDurationMs > 0 && it >= mediaDurationMs }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 时钟（LyricsPlaybackClock.kt + PlaybackAnchor 逐行）
// ─────────────────────────────────────────────────────────────────────────────

data class CmxPlaybackAnchor(val positionMs: Long, val realtimeMs: Long, val playing: Boolean,
    val durationMs: Long = 0, val speed: Float = 1f, val revision: Long = 0) {
    companion object { const val MAX_EXTRAPOLATION_MS = 3000L }
    fun positionAt(nowMs: Long): Long {
        val elapsed = if (playing) ((nowMs - realtimeMs).coerceIn(0, MAX_EXTRAPOLATION_MS) * speed).toLong() else 0
        return (positionMs + elapsed).coerceIn(0, durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE)
    }
}

/** Smooths small sampling drift; seeks, track changes and pauses stay authoritative. */
class CmxLyricsPlaybackClock {
    private var songIdentity: Any? = null
    private var sample: CmxPlaybackAnchor? = null
    private var anchor = CmxPlaybackAnchor(0, 0, false)
    private var correctionMs = 0L
    private var correctionDurationMs = 300.0
    fun update(id: Any?, next: CmxPlaybackAnchor, fallbackRealtimeMs: Long = next.realtimeMs) {
        if (id == songIdentity && next == sample) return
        val timed = if (next.realtimeMs <= 0 && fallbackRealtimeMs != next.realtimeMs) next.copy(realtimeMs = fallbackRealtimeMs) else next
        val previous = positionAt(timed.realtimeMs)
        val snap = sample == null || id != songIdentity || next.playing != anchor.playing ||
            next.speed != anchor.speed || next.revision != anchor.revision || !next.playing || abs(next.positionMs - previous) > 180
        anchor = if (snap) timed else timed.copy(positionMs = previous)
        correctionMs = if (snap) 0 else next.positionMs - previous
        correctionDurationMs = maxOf(300.0, abs(correctionMs) * 1.6 / next.speed.coerceAtLeast(.01f))
        songIdentity = id; sample = next
    }
    private fun correctionAt(realtimeMs: Long): Long {
        val progress = ((realtimeMs - anchor.realtimeMs) / correctionDurationMs).coerceIn(0.0, 1.0)
        return (correctionMs * progress * progress * (3 - 2 * progress)).toLong()
    }
    private fun corrected(base: Long, correction: Long) =
        (base + correction).coerceIn(0, anchor.durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE)
    fun positionAt(realtimeMs: Long): Long = corrected(anchor.positionAt(realtimeMs), correctionAt(realtimeMs))
}

/**
 * 逐帧读头（rememberLyricsPosition 的 MINova 接线版：PlayerState → 我们的
 * currentPosition/isPlaying/duration/songId 参数）。
 */
@Composable
fun rememberCmxLyricsPosition(
    songId: String?, positionMs: Long, playing: Boolean, durationMs: Long,
): State<Long> {
    val identity = songId
    val realtime = remember(positionMs, playing) { android.os.SystemClock.elapsedRealtime() }
    val anchor = remember(identity, positionMs, playing, durationMs, realtime) {
        CmxPlaybackAnchor(positionMs, realtime, playing, durationMs)
    }
    val position = remember { mutableLongStateOf(positionMs) }
    val clock = remember { CmxLyricsPlaybackClock() }
    SideEffect {
        clock.update(identity, anchor)
        position.longValue = clock.positionAt(android.os.SystemClock.elapsedRealtime())
    }
    LaunchedEffect(identity, anchor.playing, anchor.realtimeMs) {
        position.longValue = clock.positionAt(android.os.SystemClock.elapsedRealtime())
        if (anchor.playing) while (true) {
            withFrameNanos { }
            position.longValue = clock.positionAt(android.os.SystemClock.elapsedRealtime())
        }
    }
    return position
}

// ─────────────────────────────────────────────────────────────────────────────
// 动效常量与曲线（MonetLyricMotion.kt 逐行）
// ─────────────────────────────────────────────────────────────────────────────

internal const val MONET_SCROLL_STIFFNESS = 142f / .82f
internal val MONET_SCROLL_DAMPING = 28f / (2f * sqrt(142f * .82f))
internal const val MONET_SCALE_STIFFNESS = 150f / .78f
internal val MONET_SCALE_DAMPING = 30f / (2f * sqrt(150f * .78f))
internal fun monetScaleSpring(): SpringSpec<Float> =
    spring(MONET_SCALE_DAMPING, MONET_SCALE_STIFFNESS, visibilityThreshold = .0001f)
internal const val MONET_BOUNCE_TOTAL_MS = 800L

internal fun monetViewportRadius(height: Float, fontPx: Float): Int =
    (ceil(height.coerceAtLeast(0f) * .54f / (fontPx * 1.1f).coerceAtLeast(1f)).toInt() + 1).coerceIn(4, 24)

internal data class MonetTone(val opacity: Float, val scale: Float, val blur: Float, val baseAlpha: Float)

internal fun monetTone(active: Boolean, offset: Int): MonetTone {
    if (active) return MonetTone(1f, 1f, 0f, .34f)
    val distance = abs(offset).coerceAtLeast(1)
    val waiting = offset >= 0
    return MonetTone(
        if (waiting) (.72f - (distance - 1) * .18f).coerceIn(.36f, .72f)
        else (.52f - (distance - 1) * .12f).coerceIn(.28f, .52f),
        (.72f * .9f.pow(distance - 1)).coerceIn(.68f, .92f),
        if (waiting) { if (distance == 1) .7f else 1.8f + (distance - 2) * .8f }
        else 1.1f + (distance - 1) * .7f,
        if (waiting) .46f else .36f)
}

internal const val MONET_RAIL_LIFT = .3f

internal fun monetRailTone(distance: Float, emphasis: Float = 0f, lift: Float = 0f): MonetTone {
    val ad = abs(distance)
    val waiting = distance >= 0f
    val baseOpacity = if (waiting) (.72f - (ad - 1f) * .18f).coerceIn(.36f, .72f)
        else (.52f - (ad - 1f) * .12f).coerceIn(.28f, .52f)
    val baseScale = (.72f * .9f.pow((ad - 1f).coerceAtLeast(0f))).coerceIn(.68f, .92f)
    val baseBlur = if (waiting) { if (ad <= 1f) .7f else 1.8f + (ad - 2f) * .8f } else 1.1f + (ad - 1f) * .7f
    val curveAlpha = if (waiting) .46f else .36f
    return MonetTone(baseOpacity + (1f - baseOpacity) * lift, baseScale + (1f - baseScale) * emphasis,
        baseBlur * (1f - lift), curveAlpha + (.34f - curveAlpha) * lift)
}

internal fun monetTrackPositions(heights: List<Float>, scales: List<Float>, anchor: Int,
    focusY: Float, fontPx: Float, active: Set<Int>): List<Float> {
    if (heights.isEmpty()) return emptyList()
    val y = MutableList(heights.size) { 0f }
    val safeAnchor = anchor.coerceIn(heights.indices)
    fun gap(a: Int, b: Int) = if (a in active || b in active) fontPx * .49f else fontPx * .38f
    y[safeAnchor] = focusY - heights[safeAnchor] * scales[safeAnchor] / 2f
    for (i in safeAnchor + 1..heights.lastIndex)
        y[i] = y[i - 1] + heights[i - 1] * scales[i - 1] + gap(i - 1, i)
    for (i in safeAnchor - 1 downTo 0)
        y[i] = y[i + 1] - heights[i] * scales[i] - gap(i, i + 1)
    return y
}

internal fun monetGlow(now: Long, start: Long, end: Long, lineEnd: Long): Float {
    if (now <= start) return 0f
    val rise = (end - start).coerceAtLeast(1) * 1.18f
    val peak = start + rise
    val tail = maxOf(lineEnd.toFloat(), end + 1050f)
    val value = if (now <= peak) (now - start) / rise
        else 1f - ((now - peak) / (tail - peak).coerceAtLeast(180f)).coerceIn(0f, 1f)
    return value.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
}

internal fun monetSweepSoftness(fontPx: Float): Float = (fontPx * .45f).coerceIn(6f, 16f)
internal fun monetSweepEnd(filled: Float, width: Float, softness: Float): Float =
    if (width <= 0f) 0f else filled + softness * (filled / width).coerceIn(0f, 1f)

internal fun monetFillWidth(progress: Float, widths: List<Float>): Float {
    if (widths.isEmpty()) return 0f
    val index = progress.coerceIn(0f, 1f) * widths.size
    val whole = index.toInt().coerceAtMost(widths.size)
    var width = 0f
    for (i in 0 until whole) width += widths[i]
    return width + if (whole < widths.size) widths[whole] * (index - whole) else 0f
}

internal fun monetDisplayWords(line: CmxLyricLine): List<CmxLyricWord> {
    if (line.wordsEstimated || line.words.isEmpty()) return emptyList()
    val words = line.words.filter { it.startOffset >= 0 && it.endOffset > it.startOffset &&
        it.endOffset <= line.text.length && line.text.substring(it.startOffset, it.endOffset) == it.text }
        .sortedBy { it.startOffset }
    if (words.isEmpty() || words.zipWithNext().any { (a, b) -> a.endOffset > b.startOffset }) return words
    val display = mutableListOf<CmxLyricWord>()
    var cursor = 0
    fun gap(until: Int, at: Long) {
        if (cursor < until) display += CmxLyricWord(line.text.substring(cursor, until), at, at,
            cursor, until, roles = listOf("x-display-gap"))
    }
    words.forEach { word ->
        gap(word.startOffset, word.startTimeMs)
        display += word
        cursor = word.endOffset
    }
    gap(line.text.length, words.maxOf { it.endTimeMs })
    return display
}

// ─────────────────────────────────────────────────────────────────────────────
// 逐字特效数学（LyricsEffects.kt 逐行）
// ─────────────────────────────────────────────────────────────────────────────

internal const val LYRIC_GLYPH_RISE_MS = 160L
internal fun lyricGlyphLift(now: Long, start: Long): Float =
    ((now - start).toFloat() / LYRIC_GLYPH_RISE_MS).coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }

internal const val LYRIC_GLYPH_RETURN_MS = 240L
internal fun lyricGlyphLiftAndReturn(now: Long, start: Long, end: Long): Float {
    if (end <= start || now <= start) return 0f
    fun smooth(value: Float) = value.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
    val returnAt = maxOf(end, start + LYRIC_GLYPH_RISE_MS)
    return if (now < returnAt) smooth((now - start).toFloat() / LYRIC_GLYPH_RISE_MS)
        else 1f - smooth((now - returnAt).toFloat() / LYRIC_GLYPH_RETURN_MS)
}

private val vowelGroups = Regex("[aeiouy]+", RegexOption.IGNORE_CASE)
internal fun lyricGlowWeight(word: CmxLyricWord, lineRoles: List<String>): Float {
    if ((word.roles + lineRoles).any { it.lowercase() in setOf("chorus", "x-chorus", "emphasis", "x-emphasis") }) return 1f
    val cjk = word.text.count { it in '\u3400'..'\u9fff' || it in '\u3040'..'\u30ff' || it in '\uac00'..'\ud7af' }
    val syllables = (cjk + vowelGroups.findAll(word.text).count()).coerceAtLeast(1)
    val duration = (word.endTimeMs - word.startTimeMs).toFloat() / syllables
    return ((duration - 550f) / 700f).coerceIn(0f, 1f)
}

internal fun lyricGlowReveal(progress: Float): Float =
    (progress / .7f).coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }

internal fun lyricGlowStrength(now: Long, start: Long, end: Long): Float {
    if (end <= start || now <= start || now >= end) return 0f
    val progress = (now - start).toFloat() / (end - start)
    fun smooth(value: Float): Float = value.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
    return smooth(progress / .55f) * smooth((1f - progress) / .18f)
}

// ─────────────────────────────────────────────────────────────────────────────
// 字形聚类（LyricsClusters.kt 逐行）
// ─────────────────────────────────────────────────────────────────────────────

/** UTF-16 offsets into the shaped paragraph, never individual surrogate/code-unit glyphs. */
internal fun lyricsClusters(text: String): List<IntRange> {
    val iterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(text) }
    val ranges = mutableListOf<IntRange>()
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        ranges += start until end
        start = end; end = iterator.next()
    }
    return ranges
}

internal fun lyricsCanLift(text: String): Boolean = text.codePoints().allMatch { point ->
    Character.getType(point) !in setOf(Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt(), Character.ENCLOSING_MARK.toInt()) &&
        Character.UnicodeScript.of(point) !in setOf(Character.UnicodeScript.ARABIC, Character.UnicodeScript.DEVANAGARI,
            Character.UnicodeScript.BENGALI, Character.UnicodeScript.TAMIL, Character.UnicodeScript.THAI)
}

// ─────────────────────────────────────────────────────────────────────────────
// 段落光栅（LyricsTextRaster.kt 逐行）
// ─────────────────────────────────────────────────────────────────────────────

internal class LyricsTextRaster(private val layout: TextLayoutResult, private val density: Density,
    private val direction: LayoutDirection, private val glowRadiusScale: Float = 1f,
    private val monet: Boolean = false) {
    private val glyphHalos = mutableMapOf<Rect, Pair<ImageBitmap, Offset>>()
    private val wideHalos = mutableMapOf<Rect, Pair<ImageBitmap, Offset>>()
    private val untimedParagraphs = mutableMapOf<List<Rect>, ImageBitmap>()
    private val prepared = mutableSetOf<ImageBitmap>()
    fun prepareForDrawing(bitmap: ImageBitmap) {
        if (prepared.add(bitmap)) bitmap.asAndroidBitmap().prepareToDraw()
    }
    private val maskPaint by lazy { android.graphics.Paint().apply {
        maskFilter = android.graphics.BlurMaskFilter(if (monet) layout.layoutInput.style.fontSize.value * density.density * density.fontScale * .28f
            else 6f * density.density * glowRadiusScale, android.graphics.BlurMaskFilter.Blur.OUTER)
    } }
    private val widePaint by lazy { android.graphics.Paint().apply {
        maskFilter = android.graphics.BlurMaskFilter(layout.layoutInput.style.fontSize.value * density.density * density.fontScale * .65f,
            android.graphics.BlurMaskFilter.Blur.OUTER)
    } }
    val ink: ImageBitmap by lazy {
        ImageBitmap(layout.size.width.coerceAtLeast(1), layout.size.height.coerceAtLeast(1)).also { bitmap ->
            CanvasDrawScope().draw(density, direction, androidx.compose.ui.graphics.Canvas(bitmap), Size(bitmap.width.toFloat(), bitmap.height.toFloat())) {
                drawText(layout, color = Color.White, drawStyle = Fill)
            }
        }
    }
    fun untimedInk(boxes: List<Rect>): ImageBitmap = untimedParagraphs.getOrPut(boxes) {
        ink.asAndroidBitmap().copy(android.graphics.Bitmap.Config.ARGB_8888, true).also { bitmap ->
            val clear = android.graphics.Paint().apply { xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR) }
            val canvas = android.graphics.Canvas(bitmap)
            boxes.forEach { canvas.drawRect(it.left, it.top, it.right, it.bottom, clear) }
        }.asImageBitmap()
    }
    val lineHalo: Pair<ImageBitmap, Offset> by lazy {
        val offsets = IntArray(2)
        val alpha = ink.asAndroidBitmap().extractAlpha(maskPaint, offsets)
        alpha.asImageBitmap() to Offset(offsets[0].toFloat(), offsets[1].toFloat())
    }
    fun glyphHalo(box: Rect): Pair<ImageBitmap, Offset> = glyphHalos.getOrPut(box) { makeHalo(box, maskPaint) }
    fun wideGlyphHalo(box: Rect): Pair<ImageBitmap, Offset> = wideHalos.getOrPut(box) { makeHalo(box, widePaint) }
    private fun makeHalo(box: Rect, paint: android.graphics.Paint): Pair<ImageBitmap, Offset> {
        val left = floor(box.left).toInt()
        val top = floor(box.top).toInt()
        val glyph = android.graphics.Bitmap.createBitmap(
            (ceil(box.right).toInt() - left).coerceAtLeast(1),
            (ceil(box.bottom).toInt() - top).coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(glyph).apply {
            clipRect(box.left - left, box.top - top, box.right - left, box.bottom - top)
            drawBitmap(ink.asAndroidBitmap(), -left.toFloat(), -top.toFloat(), null)
        }
        val offsets = IntArray(2)
        val alpha = glyph.extractAlpha(paint, offsets)
        glyph.recycle()
        return alpha.asImageBitmap() to Offset((left + offsets[0]).toFloat(), (top + offsets[1]).toFloat())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 扫光与光晕（MonetSweepInk.kt + MonetHaloInk.kt 逐行）
// ─────────────────────────────────────────────────────────────────────────────

internal class MonetSweepInk(ink: ImageBitmap, color: Color) {
    private val mesh = MonetInkMesh(ink, color)
    fun begin() = mesh.begin()
    fun opaque(box: Rect) = mesh.quad(box, box, 1f, 1f)
    fun add(box: Rect, edgeEnd: Float, softness: Float, rtl: Boolean) {
        val left = if (rtl) box.right - edgeEnd else box.left + edgeEnd - softness
        val right = left + softness
        val middle = left + softness * if (rtl) .55f else .45f
        fun alpha(x: Float): Float {
            val p = ((x - left) / softness).coerceIn(0f, 1f).let { if (rtl) 1f - it else it }
            return if (p <= .45f) 1f - p / .45f * .08f else .92f * (1f - (p - .45f) / .55f)
        }
        var from = box.left
        fun piece(edge: Float) {
            val to = edge.coerceIn(box.left, box.right)
            if (to > from) {
                val rect = Rect(from, box.top, to, box.bottom)
                mesh.quad(rect, rect, alpha(from), alpha(to)); from = to
            }
        }
        piece(left); piece(middle); piece(right); piece(box.right)
    }
    fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope) = mesh.draw(scope)
}

internal class MonetInkMesh(ink: ImageBitmap, color: Color, capacity: Int = 1536) {
    private var vertices = FloatArray(capacity)
    private var texture = FloatArray(capacity)
    private var colors = IntArray(capacity / 2)
    private var count = 0
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
        shader = android.graphics.BitmapShader(ink.asAndroidBitmap(), android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
        colorFilter = android.graphics.PorterDuffColorFilter(color.toArgb(), android.graphics.PorterDuff.Mode.SRC_IN)
    }
    fun begin() { count = 0 }
    fun quad(box: Rect, uv: Rect, leftAlpha: Float, rightAlpha: Float) {
        if (leftAlpha <= 0f && rightAlpha <= 0f) return
        if (count + 12 > vertices.size) {
            vertices = vertices.copyOf(vertices.size * 2)
            texture = texture.copyOf(vertices.size)
            colors = colors.copyOf(vertices.size / 2)
        }
        fun vertex(x: Float, y: Float, u: Float, v: Float, alpha: Float) {
            vertices[count] = x; texture[count++] = u
            vertices[count] = y; texture[count++] = v
            colors[count / 2 - 1] = ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24) or 0x00FFFFFF
        }
        vertex(box.left, box.top, uv.left, uv.top, leftAlpha)
        vertex(box.right, box.top, uv.right, uv.top, rightAlpha)
        vertex(box.left, box.bottom, uv.left, uv.bottom, leftAlpha)
        vertex(box.left, box.bottom, uv.left, uv.bottom, leftAlpha)
        vertex(box.right, box.top, uv.right, uv.top, rightAlpha)
        vertex(box.right, box.bottom, uv.right, uv.bottom, rightAlpha)
    }
    fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope) {
        if (count > 0) scope.drawContext.canvas.nativeCanvas.drawVertices(
            android.graphics.Canvas.VertexMode.TRIANGLES, count,
            vertices, 0, texture, 0, colors, 0, null, 0, 0, paint)
    }
}

internal class MonetHaloInk(halos: List<Pair<Int, Pair<ImageBitmap, Offset>>>, color: Color) {
    private data class Patch(val word: Int, val box: Rect, val uv: Rect)
    private val patches: List<Patch>
    private val mesh: MonetInkMesh
    init {
        val width = maxOf(1024, halos.maxOfOrNull { it.second.first.width } ?: 1)
        var x = 0; var y = 0; var row = 0
        val origins = halos.map { (_, halo) ->
            val bitmap = halo.first
            if (x + bitmap.width > width) { x = 0; y += row + 2; row = 0 }
            val origin = x to y
            x += bitmap.width + 2; row = maxOf(row, bitmap.height)
            origin
        }
        val atlas = android.graphics.Bitmap.createBitmap(width, (y + row).coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(atlas)
        patches = halos.mapIndexed { index, (word, halo) ->
            val bitmap = halo.first
            val (left, top) = origins[index]
            canvas.drawBitmap(bitmap.asAndroidBitmap(), left.toFloat(), top.toFloat(), null)
            Patch(word, Rect(halo.second.x, halo.second.y, halo.second.x + bitmap.width, halo.second.y + bitmap.height),
                Rect(left.toFloat(), top.toFloat(), (left + bitmap.width).toFloat(), (top + bitmap.height).toFloat()))
        }
        atlas.prepareToDraw()
        mesh = MonetInkMesh(atlas.asImageBitmap(), color, maxOf(12, patches.size * 12))
    }
    fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope, strengths: FloatArray) {
        mesh.begin()
        patches.forEach { patch ->
            val alpha = (strengths[patch.word] * .88f).coerceIn(0f, 1f)
            if (alpha > .001f) mesh.quad(patch.box, patch.uv, alpha, alpha)
        }
        mesh.draw(scope)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 逐字/逐行文本引擎（KaraokeText.kt 逐行；smoke/粒子分支未移植 → 参数不存在）
// ─────────────────────────────────────────────────────────────────────────────

private data class TimedSegment(val box: Rect, val rtl: Boolean, val ink: Boolean)
private data class WordGeometry(val segments: List<TimedSegment>, val width: Float, val complete: Path,
    val glyphs: List<GlyphGeometry>)
private data class GlyphGeometry(val box: Rect, val wordIndex: Int, val widthOffset: Float, val start: Long, val rtl: Boolean)

@Composable
fun CmxKaraokeText(line: CmxLyricLine, position: State<Long>, animateWords: Boolean, modifier: Modifier = Modifier,
    fontSize: Float = CmxLyricsTypography.DEFAULT_SIZE, fontWeight: FontWeight = FontWeight.Normal, textAlign: TextAlign = TextAlign.Start,
    forceLineAnimation: Boolean = false, reservedFontWeight: FontWeight = fontWeight,
    color: Color = Color.White, lineHeightRatio: Float = 1.2f, exposeText: Boolean = true,
    glow: Boolean = false, wordLift: Boolean = false, glowOpacity: State<Float>? = null,
    baseOpacity: State<Float>? = null, accurateWordLift: Boolean = false,
    outline: Boolean = false, onLayout: ((TextLayoutResult) -> Unit)? = null,
    glowColor: Color = color, glowRadiusScale: Float = 1f, untimedLineGlow: Boolean = false,
    effectPadding: androidx.compose.ui.unit.Dp = 0.dp, singleLine: Boolean = false, prepareDrawResources: Boolean = false,
    prewarmGlow: Boolean = false, monet: Boolean = false,
    wordLiftReturns: Boolean = false, alignFirstBaseline: Boolean = false) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val size = if (fontSize.isFinite()) fontSize.coerceIn(8f, CmxLyricsTypography.MAX_SIZE) else CmxLyricsTypography.DEFAULT_SIZE
    val synthesis = cmxLyricsFontSynthesis(fontWeight)
    val normalTypeface by LocalFontFamilyResolver.current.resolve(currentCmxLyricsFontFamily(), fontWeight = FontWeight.Normal,
        fontSynthesis = cmxLyricsFontSynthesis(FontWeight.Normal))
    val emphasisTypeface by LocalFontFamilyResolver.current.resolve(currentCmxLyricsFontFamily(), fontWeight = reservedFontWeight,
        fontSynthesis = cmxLyricsFontSynthesis(reservedFontWeight))
    BoxWithConstraints(modifier) {
        val padding = with(density) { effectPadding.value.takeIf(Float::isFinite)?.coerceIn(0f, 24f)?.dp?.roundToPx() ?: 0 }
        val width = (with(density) { maxWidth.roundToPx() } - padding * 2).coerceAtLeast(1)
        val style = TextStyle(fontFamily = currentCmxLyricsFontFamily(), fontSynthesis = synthesis,
            fontSize = size.sp, lineHeight = (size * lineHeightRatio).sp, fontWeight = fontWeight, textAlign = textAlign)
        val layouts = remember(line.text, width, density.density, density.fontScale, size, reservedFontWeight,
            normalTypeface, emphasisTypeface, textAlign, lineHeightRatio, direction, singleLine) {
            val constraints = Constraints(minWidth = if (textAlign == TextAlign.End || textAlign == TextAlign.Center) width else 0,
                maxWidth = width)
            listOf(FontWeight.Normal, reservedFontWeight).map { weight ->
                measurer.measure(AnnotatedString(line.text), style.copy(fontWeight = weight,
                    fontSynthesis = cmxLyricsFontSynthesis(weight)), constraints = constraints,
                    softWrap = !singleLine, maxLines = if (singleLine) 1 else Int.MAX_VALUE)
            }
        }
        val layoutIndex = if (fontWeight == FontWeight.Normal) 0 else 1
        val layout = layouts[layoutIndex]
        val firstBaseline = if (alignFirstBaseline) layouts.maxOf { it.firstBaseline } else layout.firstBaseline
        val baselineShift = firstBaseline - layout.firstBaseline
        val paragraphHeight = if (alignFirstBaseline)
            ceil(firstBaseline + layouts.maxOf { it.size.height - it.firstBaseline }).toInt()
        else layouts.maxOf { it.size.height }
        SideEffect { onLayout?.invoke(layout) }
        val clusters = remember(line.text) { lyricsClusters(line.text) }
        val words = remember(line, forceLineAnimation, monet) {
            if (line.words.isNotEmpty() && !line.wordsEstimated) {
                if (monet) monetDisplayWords(line) else line.words
            }
            else if (forceLineAnimation && line.text.isNotBlank() && line.endTimeMs > line.startTimeMs)
                listOf(CmxLyricWord(line.text, line.startTimeMs, line.endTimeMs, 0, line.text.length))
            else emptyList()
        }
        val geometries = remember(words, layouts, clusters, accurateWordLift) { layouts.map { layout -> words.mapIndexed { wordIndex, word ->
            val segments = clusters.filter { it.first >= word.startOffset && it.first < word.endOffset }.mapNotNull { range ->
                val boxes = range.map { layout.getBoundingBox(it) }.filter { it.width > 0 }
                if (boxes.isEmpty()) null else {
                    val box = boxes.reduce { a, b -> Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom)) }
                    TimedSegment(box, layout.getBidiRunDirection(range.first) == ResolvedTextDirection.Rtl,
                        line.text.substring(range).any { !it.isWhitespace() })
                }
            }
            val total = segments.sumOf { it.box.width.toDouble() }.toFloat().coerceAtLeast(1f)
            var offset = 0f
            val glyphs = segments.mapNotNull { segment ->
                val from = offset; offset += segment.box.width
                if (!segment.ink) return@mapNotNull null
                GlyphGeometry(segment.box, wordIndex, from,
                    if (accurateWordLift) word.startTimeMs else word.startTimeMs + ((word.endTimeMs - word.startTimeMs) * from / total).toLong(),
                    segment.rtl)
            }
            WordGeometry(segments, total, Path().apply { segments.forEach { addRect(it.box) } }, glyphs)
        } } }
        val geometry = geometries[layoutIndex]
        val timed = animateWords && words.isNotEmpty()
        val safeRaster = layout.size.width.toLong() * layout.size.height <= 4_000_000 && layout.size.height < 8192 && layout.size.width < 8192
        val liftTimed = wordLift && safeRaster && line.words.isNotEmpty() && lyricsCanLift(line.text)
        val glyphs = remember(geometry) { geometry.flatMap { it.glyphs } }
        val liftedInk = remember(glyphs) { Path().apply { glyphs.forEach { addRect(it.box) } } }
        val lifts = remember(glyphs) { FloatArray(glyphs.size) }
        val glowWeights = remember(words, line.roles, monet) { words.map {
            if (monet) { if (it.endTimeMs > it.startTimeMs) 1f else 0f } else lyricGlowWeight(it, line.roles)
        } }
        val wordGlows = remember(words) { FloatArray(words.size) }
        val glowTimed = glow && safeRaster && line.words.isNotEmpty() && glowWeights.any { it > 0f }
        val glowLine = untimedLineGlow && safeRaster && line.words.isEmpty() && !forceLineAnimation
        val liftHeight = 2f * density.density
        val radius = glowRadiusScale.takeIf(Float::isFinite)?.coerceIn(.5f, 2f) ?: 1f
        val rasters = remember(layouts, density, direction, radius, monet) {
            layouts.map { LyricsTextRaster(it, density, direction, radius, monet) }
        }
        val raster = rasters[layoutIndex]
        val untimedInk = remember(liftTimed, raster, glyphs, clusters, words) {
            if (liftTimed && clusters.any { range ->
                line.text.substring(range).any { !it.isWhitespace() } &&
                    words.none { range.first >= it.startOffset && range.first < it.endOffset }
            }) raster.untimedInk(glyphs.map { it.box }) else null
        }
        val tint = remember(color) { ColorFilter.tint(color) }
        val haloTint = remember(glowColor) { ColorFilter.tint(glowColor) }
        if (liftTimed) {
            if (prepareDrawResources) rasters.forEach { it.prepareForDrawing(it.ink) }
            else raster.ink
        }
        LaunchedEffect(prewarmGlow, safeRaster, geometries, rasters, glowWeights, monet) {
            if (prewarmGlow && safeRaster && line.words.isNotEmpty()) {
                geometries.forEachIndexed { index, shapes ->
                    shapes.filterIndexed { wordIndex, _ -> glowWeights[wordIndex] > 0f }.forEach { shape ->
                        shape.glyphs.forEach { glyph ->
                            withFrameNanos { }
                            val halo = rasters[index].glyphHalo(glyph.box).first
                            if (prepareDrawResources) rasters[index].prepareForDrawing(halo)
                            if (monet) {
                                val wide = rasters[index].wideGlyphHalo(glyph.box).first
                                if (prepareDrawResources) rasters[index].prepareForDrawing(wide)
                            }
                        }
                    }
                }
            }
        }
        if (glowLine) raster.lineHalo.first.let { if (prepareDrawResources) raster.prepareForDrawing(it) }
        if (glowTimed) glyphs.filter { glowWeights[it.wordIndex] > 0f }.forEach {
            val halo = raster.glyphHalo(it.box).first
            if (prepareDrawResources) raster.prepareForDrawing(halo)
            if (monet) raster.wideGlyphHalo(it.box).let { wide -> if (prepareDrawResources) raster.prepareForDrawing(wide.first) }
        }
        val monetSweeps = if (monet && safeRaster) remember(rasters, color) { rasters.map { MonetSweepInk(it.ink, color) } } else emptyList()
        val monetHalo = if (monet && glowTimed) remember(raster, glyphs, glowColor) {
            MonetHaloInk(glyphs.filter { glowWeights[it.wordIndex] > 0f }.flatMap { glyph -> listOf(
                glyph.wordIndex to raster.glyphHalo(glyph.box),
                glyph.wordIndex to raster.wideGlyphHalo(glyph.box)) }, glowColor)
        } else null
        val monetWidths = remember(geometry) { geometry.map { shape -> shape.segments.map { it.box.width } } }
        val begin = words.minOfOrNull { it.startTimeMs } ?: 0
        val finish = maxOf(if (monet && glowTimed) maxOf(line.endTimeMs, (words.maxOfOrNull { it.endTimeMs } ?: 0) + 1050)
            else words.maxOfOrNull { it.endTimeMs } ?: 0,
            if (liftTimed && wordLiftReturns) maxOf(words.maxOfOrNull { it.endTimeMs } ?: 0,
                (glyphs.maxOfOrNull { it.start } ?: 0) + LYRIC_GLYPH_RISE_MS) + LYRIC_GLYPH_RETURN_MS
            else if (liftTimed) (glyphs.maxOfOrNull { it.start } ?: 0) + LYRIC_GLYPH_RISE_MS else 0)
        val phase by remember(begin, finish, position, timed, glowTimed, liftTimed, monet) { derivedStateOf {
            when {
                !monet && !timed && !glowTimed && !liftTimed -> 2
                position.value < begin -> 0
                position.value >= finish -> 2
                else -> 1
            }
        } }
        val sung = remember { Path() }
        Canvas(Modifier.fillMaxWidth().clipToBounds().height(with(density) { (paragraphHeight + padding * 2).toDp() })
            .layout { measurable, constraints ->
                val child = measurable.measure(constraints)
                layout(child.width, child.height, alignmentLines = mapOf(
                    FirstBaseline to (padding + firstBaseline).toInt(),
                    LastBaseline to (padding + layout.lastBaseline + baselineShift).toInt())) {
                    child.placeRelative(0, 0)
                }
            }
            .semantics {
                if (exposeText) {
                    text = AnnotatedString(line.text)
                    getTextLayoutResult { it.add(layout); true }
                }
            }) {
            translate(left = padding.toFloat(), top = padding + baselineShift) {
                if (monet && safeRaster && words.isNotEmpty()) {
                    val now = when (phase) { 0 -> begin - 1; 1 -> position.value; else -> Long.MAX_VALUE }
                    val sweep = monetSweeps[layoutIndex]
                    val softness = monetSweepSoftness(size * density.fontScale) * density.density
                    val base = baseOpacity?.value ?: .34f
                    if (monetHalo != null) {
                        words.forEachIndexed { index, word ->
                            wordGlows[index] = if (glowWeights[index] > 0f)
                                monetGlow(now, word.startTimeMs, word.endTimeMs, line.endTimeMs) else 0f
                        }
                        monetHalo.draw(this, wordGlows)
                    }
                    drawImage(raster.ink, alpha = base, colorFilter = tint)
                    sweep.begin()
                    words.forEachIndexed { wordIndex, word ->
                        val shape = geometry[wordIndex]
                        val progress = CmxLyricsSynchronizer.wordProgress(word, now)
                        val filled = monetFillWidth(progress, monetWidths[wordIndex])
                        val edgeEnd = monetSweepEnd(filled, shape.width, softness)
                        if (now >= word.endTimeMs) {
                            shape.segments.forEach { sweep.opaque(it.box) }
                        } else if (timed && now > word.startTimeMs) {
                            var offset = 0f
                            shape.segments.forEach { segment ->
                                sweep.add(segment.box, edgeEnd - offset, softness, segment.rtl)
                                offset += segment.box.width
                            }
                        }
                    }
                    sweep.draw(this)
                    return@translate
                }
                if (glowLine) {
                    val halo = raster.lineHalo
                    drawImage(halo.first, topLeft = halo.second,
                        alpha = (.9f * (glowOpacity?.value ?: 1f)).coerceIn(0f, 1f), colorFilter = haloTint)
                }
                if (outline) drawText(layout, color = (if (color.luminance() < .35f) Color.White else Color.Black).copy(alpha = .85f), drawStyle = Stroke(width = 1.6f * density.density))
                if (phase == 2 && liftTimed) {
                    if (wordLiftReturns) {
                        drawImage(raster.ink, colorFilter = tint)
                        return@translate
                    }
                    clipPath(liftedInk, ClipOp.Difference) { drawImage(raster.ink, colorFilter = tint) }
                    translate(top = -liftHeight) {
                        clipPath(liftedInk) { drawImage(raster.ink, colorFilter = tint) }
                    }
                    return@translate
                }
                val baseAlpha = if (timed && phase < 2) baseOpacity?.value ?: .32f else 1f
                val glowAlpha = glowOpacity?.value ?: 1f
                val now = if (phase == 1) position.value else 0L
                if (phase == 1 && liftTimed) glyphs.forEachIndexed { index, glyph ->
                    lifts[index] = (if (wordLiftReturns) lyricGlyphLiftAndReturn(now, glyph.start,
                        words[glyph.wordIndex].endTimeMs) else lyricGlyphLift(now, glyph.start)) * liftHeight
                }
                if (phase == 1 && glowTimed) {
                    words.forEachIndexed { index, word ->
                        wordGlows[index] = glowWeights[index] * lyricGlowStrength(now, word.startTimeMs, word.endTimeMs)
                    }
                    glyphs.forEachIndexed { index, glyph ->
                        val intensity = wordGlows[glyph.wordIndex] * glowAlpha
                        if (intensity <= .001f) return@forEachIndexed
                        val shape = geometry[glyph.wordIndex]
                        val progress = CmxLyricsSynchronizer.wordProgress(words[glyph.wordIndex], now)
                        val filled = (shape.width * progress - glyph.widthOffset).coerceIn(0f, glyph.box.width)
                        if (filled <= 0f) return@forEachIndexed
                        val halo = raster.glyphHalo(glyph.box)
                        val reveal = lyricGlowReveal(filled / glyph.box.width)
                        translate(top = if (liftTimed) -lifts[index] else 0f) {
                            drawImage(halo.first, topLeft = halo.second, alpha = (.75f * intensity * reveal).coerceIn(0f, 1f), colorFilter = haloTint)
                        }
                    }
                }
                if (phase == 1 && liftTimed) {
                    untimedInk?.let { drawImage(it, alpha = baseAlpha, colorFilter = tint) }
                    glyphs.forEachIndexed { index, glyph ->
                        val shape = geometry[glyph.wordIndex]
                        val progress = CmxLyricsSynchronizer.wordProgress(words[glyph.wordIndex], now)
                        val filled = (shape.width * progress - glyph.widthOffset).coerceIn(0f, glyph.box.width)
                        translate(top = -lifts[index]) {
                            val box = glyph.box
                            clipRect(box.left, box.top, box.right, box.bottom) {
                                if (timed && (progress >= 1f || filled >= box.width)) {
                                    drawImage(raster.ink, colorFilter = tint)
                                } else {
                                    drawImage(raster.ink, alpha = baseAlpha, colorFilter = tint)
                                    if (timed && filled > 0f) clipRect(if (glyph.rtl) box.right - filled else box.left, box.top,
                                        if (glyph.rtl) box.right else box.left + filled, box.bottom) {
                                        drawImage(raster.ink, colorFilter = tint)
                                    }
                                }
                            }
                        }
                    }
                    return@translate
                }
                drawText(layout, color = color.copy(alpha = color.alpha * baseAlpha), drawStyle = Fill)
                if (phase == 1) {
                    sung.reset()
                    words.forEachIndexed { index, word ->
                        val progress = CmxLyricsSynchronizer.wordProgress(word, now)
                        val shape = geometry[index]
                        if (progress >= 1f) {
                            sung.addPath(shape.complete)
                            return@forEachIndexed
                        }
                        if (progress <= 0f) return@forEachIndexed
                        var remaining = shape.width * progress
                        shape.segments.forEach { segment ->
                            val box = segment.box
                            val filled = remaining.coerceIn(0f, box.width)
                            if (filled > 0) {
                                val rect = if (segment.rtl) Rect(box.right - filled, box.top, box.right, box.bottom)
                                    else Rect(box.left, box.top, box.left + filled, box.bottom)
                                sung.addRect(rect)
                            }
                            remaining -= box.width
                        }
                    }
                    if (timed) clipPath(sung) { drawText(layout, color = color, drawStyle = Fill) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 辅助行（LyricsAuxiliaryText.kt 逐行）
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CmxLyricsAuxiliaryText(text: String, position: State<Long>, visible: Boolean,
    size: Float, weight: FontWeight, align: TextAlign, tag: String, opacity: Float,
    reserveSpace: Boolean = false, animateHeight: Boolean = true) {
    if (text.isBlank()) return
    val inkAlpha = animateFloatAsState(if (visible) 1f else 0f,
        tween(if (visible) 180 else 110, delayMillis = if (visible) 60 else 0), label = "$tag opacity")
    val height = if (!animateHeight) rememberUpdatedState(if (visible) 1f else 0f) else if (reserveSpace) remember { mutableFloatStateOf(1f) } else animateFloatAsState(if (visible) 1f else 0f,
        tween(if (visible) 280 else 220, delayMillis = if (visible) 0 else 70, easing = FastOutSlowInEasing), label = "$tag space")
    val auxiliary = remember(text) { CmxLyricLine(0, Long.MAX_VALUE, text) }
    CmxKaraokeText(auxiliary, position, false, Modifier.fillMaxWidth()
        .layout { measurable, constraints ->
            val child = measurable.measure(constraints.copy(minHeight = 0))
            layout(child.width, if (reserveSpace) child.height else (child.height * height.value).roundToInt()) {
                child.placeRelative(0, 0)
            }
        }.graphicsLayer {
            alpha = inkAlpha.value
            translationY = (1f - inkAlpha.value) * 3.dp.toPx()
        }.padding(top = 4.dp).then(if (visible) Modifier.testTag(tag) else Modifier),
        fontSize = size, fontWeight = weight, reservedFontWeight = weight, textAlign = align,
        color = Color.White.copy(alpha = opacity), lineHeightRatio = 1.2f, exposeText = visible)
}

// ─────────────────────────────────────────────────────────────────────────────
// 间奏指示（LyricsInterludeView.kt 逐行，字体→Default）
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CmxLyricsInterludeView(interval: CmxLyricsInterlude, position: State<Long>, modifier: Modifier) {
    Row(modifier.testTag("lyrics_interlude"), verticalAlignment = Alignment.CenterVertically) {
        BasicText(interval.label, style = TextStyle(color = Color.White.copy(alpha = .65f), fontSize = 13.sp,
            fontFamily = currentCmxLyricsFontFamily()))
        Canvas(Modifier.padding(start = 10.dp).size(48.dp, 20.dp)) {
            val end = interval.endTimeMs
            val progress = if (end != null) ((position.value - interval.startTimeMs).toDouble() / (end - interval.startTimeMs).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
                else ((position.value - interval.startTimeMs).coerceAtLeast(0) % 3000) / 3000f
            repeat(3) { index ->
                drawCircle(Color.White.copy(alpha = if (progress * 3 > index) .8f else .25f),
                    radius = 3.dp.toPx(), center = Offset(size.width * (index + .5f) / 3, size.height / 2))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 行视图（LyricsLineView.kt 逐行；罗马音分支因 LRC 无音节标注恒走普通路径）
// ─────────────────────────────────────────────────────────────────────────────

internal data class CmxClassicPhoneticLine(val cells: List<IntRange>, val fallbackRomanization: String)

internal fun cmxClassicLyricPhonetics(line: CmxLyricLine): CmxClassicPhoneticLine =
    CmxClassicPhoneticLine(emptyList(), line.romanization)

@Composable
private fun CmxLyricsLineView(line: CmxLyricLine, index: Int, position: State<Long>, focused: Boolean,
    resting: Boolean, distance: Int, canSeek: Boolean, onSeek: (Long) -> Unit, translation: Boolean,
    romanization: Boolean, wordAnimation: Boolean, effects: Boolean, fontSize: Float, weightMode: CmxLyricsWeight,
    display: CmxLyricsDisplay, monetTone: MonetTone, browsing: Boolean = false) {
    val mainFocused by remember(line, position) {
        derivedStateOf { position.value >= line.startTimeMs && position.value < line.endTimeMs }
    }
    Box(Modifier.fillMaxWidth().testTag("lyric_line_$index").semantics { selected = focused }
        .clip(RoundedCornerShape(12.dp))
        .clickable(enabled = canSeek, role = Role.Button) { onSeek(line.startTimeMs) }) {
        Column(Modifier.fillMaxWidth().layout { measurable, constraints ->
            val width = (constraints.maxWidth / 1.12f).roundToInt()
            val child = measurable.measure(constraints.copy(minWidth = minOf(constraints.minWidth, width), maxWidth = width))
            val height = ceil(child.height * 1.12f).toInt()
            layout(constraints.maxWidth, height) {
                child.placeRelative(if (display.centered) (constraints.maxWidth - child.width) / 2
                    else if (line.isDuet) constraints.maxWidth - child.width else 0, (height - child.height) / 2)
            }
        }.graphicsLayer {
            scaleX = 1.12f; scaleY = 1.12f
            transformOrigin = TransformOrigin(if (display.centered) .5f else if (line.isDuet) 1f else 0f, .5f)
        }) {
            CmxLyricsVocalView(line, position, mainFocused, resting, translation, romanization, wordAnimation,
                if (line.isBackground) fontSize * .75f else fontSize, weightMode, display, effects, distance,
                vocalModifier = Modifier.testTag("lyric_vocal_$index"), monetTone = monetTone,
                reserveAuxiliarySpace = line.backgroundVocals.isNotEmpty())
            line.backgroundVocals.forEachIndexed { bgIndex, bg ->
                val bgFocused by remember(bg, position) { derivedStateOf { position.value >= bg.startTimeMs && position.value < bg.endTimeMs } }
                val bgVisible = browsing || bgFocused
                val bgAlpha = animateFloatAsState(if (browsing) .55f else if (bgFocused) 1f else 0f,
                    tween(180), label = "background vocal opacity")
                Column(Modifier.padding(top = 10.dp).testTag("lyric_bg_${index}_$bgIndex")
                    .then(if (bgVisible) Modifier else Modifier.clearAndSetSemantics {})
                    .semantics { selected = bgFocused }
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = canSeek && bgVisible, role = Role.Button) { onSeek(bg.startTimeMs) }
                    .graphicsLayer { alpha = bgAlpha.value }) {
                    CmxLyricsVocalView(bg, position, bgFocused, false, translation, romanization, wordAnimation, fontSize * .75f, weightMode, display, effects, distance,
                        monetTone = monetTone, reserveAuxiliarySpace = true)
                }
            }
        }
    }
}

@Composable
private fun CmxLyricsVocalView(line: CmxLyricLine, position: State<Long>, focused: Boolean,
    resting: Boolean, translation: Boolean, romanization: Boolean, wordAnimation: Boolean, size: Float, weightMode: CmxLyricsWeight,
    display: CmxLyricsDisplay, effects: Boolean, distance: Int, vocalModifier: Modifier = Modifier,
    monetTone: MonetTone, reserveAuxiliarySpace: Boolean = false) {
    val weight = cmxLyricsFontWeight(weightMode, focused || resting, display.fontWeight)
    val reservedWeight = cmxLyricsFontWeight(CmxLyricsWeight.ALL, true, display.fontWeight)
    val auxiliaryWeight = cmxLyricsFontWeight(weightMode, true, display.fontWeight)
    val align = if (display.centered) TextAlign.Center else if (line.isDuet) TextAlign.End else TextAlign.Start
    val beforeStart by remember(line, position) { derivedStateOf { position.value < line.startTimeMs } }
    val preview = resting && beforeStart
    val baseOpacity = animateFloatAsState(if (preview) .55f else monetTone.baseAlpha, tween(220), label = "lyric preview ink")
    val glowOpacity = animateFloatAsState(if (display.glow && focused) 1f else 0f,
        tween(220), label = "lyric glow")
    val glowing by remember(glowOpacity) { derivedStateOf { glowOpacity.value > .001f } }
    val blur = if ((monetTone.blur == 0f) && effects && android.os.Build.VERSION.SDK_INT >= 31 && !focused && !resting)
        Modifier.blur(if (distance <= 1) 1.4.dp else 2.2.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded) else Modifier
    val phonetics = remember(line, romanization) {
        if (romanization) cmxClassicLyricPhonetics(line) else CmxClassicPhoneticLine(emptyList(), line.romanization)
    }
    val aligned = phonetics.cells.isNotEmpty()
    val hasRomanization = phonetics.fallbackRomanization.isNotBlank()
    val showTranslation = translation && !(romanization && hasRomanization)
    val sourceWordTiming = line.words.isNotEmpty() && !line.wordsEstimated
    val vocalText: @Composable (CmxLyricLine, Modifier, TextAlign) -> Unit = { vocal, modifier, textAlign ->
        CmxKaraokeText(vocal, position, wordAnimation && (focused || preview || (!resting && true)),
            modifier, size, weight, textAlign, forceLineAnimation = !aligned && false,
            reservedFontWeight = reservedWeight, glow = display.glow && (!aligned || sourceWordTiming) &&
                (focused || glowing || (!beforeStart && distance <= 4)),
            wordLift = false, monet = wordAnimation, glowOpacity = glowOpacity, baseOpacity = baseOpacity,
            prepareDrawResources = true, prewarmGlow = display.glow && (!aligned || sourceWordTiming) && beforeStart && distance == 1,
            alignFirstBaseline = aligned)
    }
    Column(Modifier.fillMaxWidth().then(blur)) {
        if (line.text.isNotBlank()) {
            vocalText(line, Modifier.fillMaxWidth().then(vocalModifier), align)
        }
        CmxLyricsAuxiliaryText(line.translation, position, showTranslation && focused, size * .6f,
            auxiliaryWeight, align, "lyric_translation", .65f,
            reserveSpace = reserveAuxiliarySpace && showTranslation, animateHeight = false)
        CmxLyricsAuxiliaryText(phonetics.fallbackRomanization, position, romanization && focused, size * .53f,
            auxiliaryWeight, align, "lyric_romanization", .5f,
            reserveSpace = reserveAuxiliarySpace && romanization, animateHeight = false)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 轨道（MonetLyricsRail.kt 逐行）
// ─────────────────────────────────────────────────────────────────────────────

private class MonetTrack(val entering: Boolean) {
    var target by mutableFloatStateOf(Float.NaN)
    var position: State<Float>? = null
}

@Composable
internal fun MonetLyricsRail(document: CmxLyricsDocument, target: Int, active: Set<Int>,
    modifier: Modifier = Modifier, fontSize: Float, centered: Boolean, minimal: Boolean,
    perspective: Boolean, effects: Boolean, followRequest: Int, onBrowse: (Int?) -> Unit,
    expandWindow: Boolean = false, playbackTarget: Int = target,
    content: @Composable (index: Int, browsing: Boolean, tone: MonetTone, focused: Boolean, focusIndex: Int) -> Unit) {
    val unit = LocalDensity.current.density
    val fontScale = LocalDensity.current.fontScale
    var viewportHeight by remember { mutableFloatStateOf(0f) }
    val radius by remember(expandWindow, fontSize, unit, fontScale) { derivedStateOf {
        if (expandWindow) monetViewportRadius(viewportHeight, fontSize * unit * fontScale) else 4
    } }
    val effectInset = (fontSize * fontScale * 2f + 6f).dp
    var manualAnchor by remember(document) { mutableStateOf<Int?>(null) }
    var gestureRevision by remember(document) { mutableIntStateOf(0) }
    var dragging by remember(document) { mutableStateOf(false) }
    var dragResidual by remember(document) { mutableFloatStateOf(0f) }
    var bouncing by remember(document) { mutableStateOf(false) }
    val scrubbing = dragging || bouncing
    val anchor = (manualAnchor ?: target).coerceIn(0, document.lines.lastIndex.coerceAtLeast(0))
    val latestPlayback = rememberUpdatedState(playbackTarget)
    val latestDragging = rememberUpdatedState(dragging)
    val latestAnchor = rememberUpdatedState(anchor)
    val browsing = manualAnchor != null
    fun isFocusActive(index: Int) = if (browsing) !scrubbing && index == anchor else index in active
    val focusIndex = if (browsing) anchor else target
    SideEffect { onBrowse(manualAnchor) }
    val start = (anchor - radius).coerceAtLeast(0)
    val desired = if (document.lines.isEmpty()) emptyList() else
        (start..minOf(anchor + radius, document.lines.lastIndex)).toList()
    var previousWindow by remember(document) { mutableStateOf(desired) }
    var retiring by remember(document) { mutableStateOf(emptyList<Int>()) }
    SideEffect {
        if (previousWindow != desired) {
            retiring = if (bouncing) emptyList() else previousWindow.filter { it !in desired }
            previousWindow = desired
        }
    }
    LaunchedEffect(retiring) { if (retiring.isNotEmpty()) { delay(200); retiring = emptyList() } }
    val indices = (desired + retiring).distinct().sorted()
    val firstWindow = remember(document) { desired.toSet() }
    val tracks = remember(document) { mutableMapOf<Int, MonetTrack>() }
    val keepKeys = (indices + previousWindow).toSet()
    SideEffect { tracks.keys.retainAll(keepKeys) }
    val residualSettle = remember { Animatable(0f) }
    LaunchedEffect(dragResidual) { if (scrubbing) residualSettle.snapTo(dragResidual) }
    LaunchedEffect(scrubbing) {
        if (!scrubbing) residualSettle.animateTo(0f, tween(150, easing = FastOutSlowInEasing))
    }
    val visualResidual = if (scrubbing) dragResidual else residualSettle.value
    val pitchFallback = fontSize * unit * fontScale * 1.3f
    fun railPitch(index: Int): Float {
        val here = tracks[index]?.target?.takeIf { it.isFinite() } ?: return pitchFallback
        val ahead = tracks[index + 1]?.target?.takeIf { it.isFinite() }
        val behind = tracks[index - 1]?.target?.takeIf { it.isFinite() }
        val aheadGap = if (ahead != null) abs(ahead - here) else 0f
        val behindGap = if (behind != null) abs(here - behind) else 0f
        return (if (aheadGap > 0f) aheadGap else behindGap).takeIf { it > 0f } ?: pitchFallback
    }
    suspend fun bounceToPlayback() {
        val from = manualAnchor ?: return
        bouncing = true
        try {
            val maxIndex = document.lines.lastIndex.coerceAtLeast(0)
            var to = latestPlayback.value
            val totalMs = MONET_BOUNCE_TOTAL_MS.coerceAtLeast(abs(to - from) * 24L)
            var startFrame = -1L
            while (true) {
                val frame = withFrameNanos { it }
                if (latestDragging.value) return
                if (startFrame < 0) startFrame = frame
                to = latestPlayback.value
                val t = ((frame - startFrame) / 1_000_000f / totalMs).coerceIn(0f, 1f)
                val pos = from + (to - from) * t
                val a = floor(pos).toInt().coerceIn(0, maxIndex)
                if (manualAnchor != a) manualAnchor = a
                dragResidual = (pos - a) * railPitch(a)
                if (t >= 1f) break
            }
            dragResidual = 0f
            manualAnchor = null
        } finally {
            bouncing = false
            if (!latestDragging.value) dragResidual = 0f
        }
    }
    LaunchedEffect(followRequest) { if (followRequest > 0) bounceToPlayback() }
    val focusPos = if (browsing) anchor + visualResidual / railPitch(anchor) else target.toFloat()
    val tones = indices.map {
        if (browsing) monetRailTone(it - focusPos, lift = (1f - abs(it - focusPos)).coerceIn(0f, 1f) * MONET_RAIL_LIFT)
        else monetTone(it in active || active.isEmpty() && it == target,
            if (expandWindow) (it - target).coerceIn(-4, 4) else it - target)
    }
    Layout(modifier = modifier.testTag("lyrics_list").onSizeChanged { viewportHeight = it.height.toFloat() }
        .semantics {
            verticalScrollAxisRange = ScrollAxisRange({ anchor.toFloat() }, { document.lines.lastIndex.coerceAtLeast(0).toFloat() })
            scrollBy { _, y ->
                if (y == 0f || document.lines.isEmpty()) false else {
                    manualAnchor = (anchor + if (y > 0f) 1 else -1).coerceIn(0, document.lines.lastIndex)
                    gestureRevision++; true
                }
            }
            scrollToIndex { index ->
                if (index !in document.lines.indices) false else { manualAnchor = index; gestureRevision++; true }
            }
        }
        .pointerInput(document) {
            var accumulator = 0f
            detectVerticalDragGestures(onDragStart = { dragging = true; manualAnchor = latestAnchor.value; accumulator = 0f; dragResidual = 0f },
                onDragEnd = { dragging = false; dragResidual = 0f; gestureRevision++ },
                onDragCancel = { dragging = false; dragResidual = 0f; gestureRevision++ }) { change, amount ->
                change.consume()
                if (accumulator * amount > 0) accumulator = 0f
                accumulator -= amount
                val maxIndex = document.lines.lastIndex.coerceAtLeast(0)
                while (true) {
                    val current = manualAnchor ?: latestAnchor.value
                    val dir = if (accumulator > 0) 1 else -1
                    val pitch = railPitch(current)
                    if (abs(accumulator) < pitch) break
                    val next = (current + dir).coerceIn(0, maxIndex)
                    if (next == current) { accumulator = 0f; break }
                    manualAnchor = next
                    accumulator -= dir * pitch
                }
                dragResidual = accumulator
                gestureRevision++
            }
        }
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithCache {
            val verticalFade = Brush.verticalGradient(0f to Color.Transparent, .11f to Color.Black,
                .88f to Color.Black, 1f to Color.Transparent)
            val edge = (8.dp.toPx() / size.width.coerceAtLeast(1f)).coerceAtMost(.49f)
            val horizontalFade = Brush.horizontalGradient(0f to Color.Transparent, edge to Color.Black,
                (1f - edge) to Color.Black, 1f to Color.Transparent)
            onDrawWithContent {
                drawContent()
                drawRect(verticalFade, blendMode = BlendMode.DstIn)
                drawRect(horizontalFade, blendMode = BlendMode.DstIn)
            }
        }, content = {
            indices.forEachIndexed { slot, index -> key(index, document.lines[index].startTimeMs) {
                val track = tracks.getOrPut(index) { MonetTrack(index !in firstWindow) }
                val exiting = index !in desired
                val targetY = track.target
                val y = if (targetY.isNaN()) null else {
                    // MINova fix: entering lines slide in (alpha 0→1 + translationY) even while
                    // browsing; only settled lines snap to the finger during a scrub.
                    val motion = remember { Animatable(targetY + if (track.entering) (if (index >= anchor) 34f else -34f) * unit else 0f) }
                    // MINova fix: 手势期间（拖动/回弹）一律 snapTo 直接跟手（等效 scrollBy），
                    // 弹簧不再与手势抢滚动；松手时 motion 已对齐 target，渲染从 target 切回
                    // motion 不再跳（此前 entering 行一直走弹簧，松手即弹跳）。
                    LaunchedEffect(targetY, exiting, scrubbing) {
                        if (exiting) motion.animateTo(targetY + (if (index < anchor) -38f else 38f) * unit,
                            tween(200, easing = CubicBezierEasing(.32f, .72f, 0f, 1f)))
                        else if (scrubbing) motion.snapTo(targetY)
                        else motion.animateTo(targetY, spring(MONET_SCROLL_DAMPING, MONET_SCROLL_STIFFNESS, .1f))
                    }
                    motion.asState()
                }
                track.position = y
                val tone = tones[slot]
                val scale = animateFloatAsState(tone.scale,
                    if (scrubbing) snap() else monetScaleSpring(), label = "Monet scale")
                val alpha = remember { Animatable(if (track.entering) 0f else tone.opacity) }
                LaunchedEffect(tone.opacity, exiting, scrubbing) {
                    if (exiting) alpha.animateTo(0f, tween(200, easing = CubicBezierEasing(.32f, .72f, 0f, 1f)))
                    else if (scrubbing && !track.entering) alpha.snapTo(tone.opacity)
                    else alpha.animateTo(tone.opacity, tween(280, easing = CubicBezierEasing(.32f, .72f, 0f, 1f)))
                }
                // MINova fix: keep the tone blur while browsing too (monetRailTone already
                // softens it via lift), so a manual drag no longer flattens the depth blur;
                // exiting browsing animates straight back to the playback tone.
                val blur = animateFloatAsState(if (effects) { if (exiting) 6f else tone.blur } else 0f,
                    tween(320, easing = CubicBezierEasing(.32f, .72f, 0f, 1f)), label = "Monet blur")
                val renderBlur = remember(blur, unit) { derivedStateOf {
                    if (android.os.Build.VERSION.SDK_INT >= 31 && blur.value > .01f)
                        BlurEffect(blur.value * unit, blur.value * unit, TileMode.Decal) else null
                } }
                Box(Modifier.fillMaxWidth().zIndex(if (isFocusActive(index)) 4f else -abs(index - focusPos).toFloat())
                    .layout { measurable, constraints ->
                        val inset = effectInset.roundToPx()
                        val child = measurable.measure(constraints.offset(inset * 2, inset * 2))
                        layout(child.width - inset * 2, child.height - inset * 2) { child.place(-inset, -inset) }
                    }.graphicsLayer {
                        translationY = if (scrubbing) track.target.takeIf { it.isFinite() } ?: y?.value ?: 0f
                        else y?.value ?: 0f
                        val right = document.lines[index].isDuet
                        val pivot = if (centered) .5f else if (right) 1f else 0f
                        val inset = effectInset.roundToPx().toFloat()
                        transformOrigin = TransformOrigin((inset + (size.width - inset * 2) * pivot) / size.width,
                            inset / size.height)
                        scaleX = scale.value; scaleY = scale.value
                        this.alpha = if (y == null) 0f else alpha.value
                        renderEffect = renderBlur.value
                    }.padding(effectInset)) { content(index, browsing, tone, isFocusActive(index), focusIndex) }
            } }
        }) { measurables, constraints ->
        val inset = 8.dp.roundToPx()
        val rowWidth = (constraints.maxWidth - inset * 2).coerceAtLeast(1)
        val rows = measurables.map { it.measure(Constraints.fixedWidth(rowWidth)) }
        val desiredSlots = desired.map { indices.indexOf(it) }
        val positions = monetTrackPositions(desiredSlots.map { rows[it].height.toFloat() },
            desiredSlots.map { tones[it].scale }, desired.indexOf(anchor),
            0f,
            fontSize * density * fontScale, desired.mapIndexedNotNull { slot, i -> if (isFocusActive(i)) slot else null }.toSet())
        desired.forEachIndexed { slot, i -> tracks.getValue(i).target = positions[slot] }
        layout(constraints.maxWidth, constraints.maxHeight) {
            val focus = ((constraints.maxHeight * if (minimal) .5f else .46f) - visualResidual).roundToInt()
            val halo = effectInset.roundToPx()
            rows.forEachIndexed { slot, row ->
                val track = tracks.getValue(indices[slot])
                val y = if (scrubbing) track.target.takeIf { it.isFinite() } ?: track.position?.value
                else track.position?.value
                if (y == null || (focus + y < constraints.maxHeight + halo && focus + y + row.height > -halo))
                    row.placeRelative(inset, focus)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 歌词屏（LyricsScreen.kt 经典路径逐行；Apple/烟雾样式未移植）
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CmxLyricsScreen(document: CmxLyricsDocument, position: State<Long>, onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier, canSeek: Boolean = true, translation: Boolean = true,
    wordAnimation: Boolean = true, effects: Boolean = true,
    fontSize: Float = CmxLyricsTypography.DEFAULT_SIZE, weightMode: CmxLyricsWeight = CmxLyricsWeight.CURRENT,
    lyricsOffsetMs: Long = 0, display: CmxLyricsDisplay = CmxLyricsDisplay(), minimal: Boolean = false,
    perspective: Boolean = false, cover: String = "", mediaDurationMs: Long = 0, playbackRunning: Boolean = true) {
    val size = CmxLyricsTypography.normalize(fontSize)
    val timeline = remember(document) { CmxLyricsSynchronizer(document) }
    val effectivePosition = remember(position, lyricsOffsetMs) { derivedStateOf { CmxLyricsSynchronizer.lyricPosition(position.value, lyricsOffsetMs) } }
    var browseAnchor by remember(document) { mutableStateOf<Int?>(null) }
    val visualPosition = remember(document, effectivePosition) { derivedStateOf {
        val anchor = browseAnchor
        if (anchor != null && anchor in document.lines.indices) document.lines[anchor].startTimeMs + 1
        else effectivePosition.value
    } }
    val activeLines by remember(timeline, visualPosition) { derivedStateOf { timeline.activeLines(visualPosition.value) } }
    val target by remember(timeline, visualPosition) { derivedStateOf { timeline.scrollTarget(visualPosition.value) } }
    val playbackTarget by remember(timeline, effectivePosition) { derivedStateOf { timeline.scrollTarget(effectivePosition.value) } }
    val interlude by remember(timeline, effectivePosition) { derivedStateOf { timeline.interlude(effectivePosition.value) } }
    var followRequest by remember(document) { mutableIntStateOf(0) }
    val browsingOffStage = remember { mutableLongStateOf(-1L) }
    Box(modifier.testTag("lyrics_panel")) {
        MonetLyricsRail(document, target, activeLines.toSet(), Modifier.fillMaxSize(), size,
            display.centered, minimal, perspective, effects, followRequest,
            onBrowse = { browseAnchor = it },
            expandWindow = display.hideControls && !minimal, playbackTarget = playbackTarget) { index, browsing, tone, focused, focusIndex ->
            val line = document.lines[index]
            CmxLyricsLineView(line, index, if (browsing) browsingOffStage else visualPosition, focused,
                !browsing && activeLines.isEmpty() && index == target, kotlin.math.abs(index - focusIndex),
                canSeek, {
                    CmxLyricsSynchronizer.seekPositionOrNull(it, lyricsOffsetMs, mediaDurationMs)?.let { seekTarget ->
                        onSeek(seekTarget); followRequest++
                    }
                }, translation, false, wordAnimation, effects, size, weightMode, display, monetTone = tone,
                browsing = browsing)
        }
        if (!minimal && interlude != null && browseAnchor == null)
            CmxLyricsInterludeView(interlude!!, effectivePosition, Modifier.align(Alignment.TopStart).padding(16.dp))
        if (!minimal && browseAnchor != null) BasicText("回到当前歌词",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).testTag("lyrics_follow")
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = .14f), RoundedCornerShape(24.dp))
                .clickable(role = Role.Button) { followRequest++ }.padding(horizontal = 20.dp, vertical = 14.dp),
            style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = currentCmxLyricsFontFamily()))
    }
}
