package com.HyperX.MI.Na

import android.content.Context

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.SliderColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.PI
import kotlin.math.sin

/** 应用动态色（KSU/materialkolor）—— 所有进度条默认跟随它。 */
@Composable
private fun dynamicAccent(): Color = MiuixTheme.colorScheme.primary

/**
 * 只读进度条 = Google Material 3 Expressive 官方波浪进度条。
 * 关键：波浪持续流动（waveSpeed）+ 小幅度（amplitude 2dp）+ 端点圆点（stopSize）。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TadpoleWaveProgress(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
    @Suppress("UNUSED_PARAMETER") strokeCap: StrokeCap = StrokeCap.Round
) {
    val act = if (color != Color.Unspecified) color else dynamicAccent()
    val track = if (trackColor != Color.Unspecified) trackColor else act.copy(alpha = 0.24f)
    LinearWavyProgressIndicator(
        progress = progress,
        modifier = modifier.fillMaxWidth(),
        color = act,
        trackColor = track,
        amplitude = { 2f },
        wavelength = 40.dp,
        waveSpeed = 26.dp,
        stopSize = 4.dp
    )
}

/**
 * 可拖动波浪滑杆 —— M3 Expressive 波浪滑杆：
 * 波浪轨道持续流动 + 圆形滑块 + 轨道端点圆点，幅度小、线条细。
 */
@Composable
fun TadpoleWaveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    activeColor: Color = Color.Unspecified,
    inactiveColor: Color = Color.Unspecified,
    thumbColor: Color = Color.Unspecified,
    waveAmplitude: Dp = 3.dp,
    waveLength: Dp = 34.dp,
    @Suppress("UNUSED_PARAMETER") colors: SliderColors? = null
) {
    val act = if (activeColor != Color.Unspecified) activeColor else dynamicAccent()
    val inact = if (inactiveColor != Color.Unspecified) inactiveColor else act.copy(alpha = 0.28f)
    val handle = if (thumbColor != Color.Unspecified) thumbColor else act

    // 波浪持续流动
    val transition = rememberInfiniteTransition(label = "wave_flow")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val fraction = remember(value, valueRange) {
        val span = valueRange.endInclusive - valueRange.start
        if (span <= 0f) 0f else ((value - valueRange.start) / span).coerceIn(0f, 1f)
    }

    fun snap(f: Float): Float {
        val raw = valueRange.start + f * (valueRange.endInclusive - valueRange.start)
        if (steps <= 0) return raw
        val stepSize = (valueRange.endInclusive - valueRange.start) / (steps + 1)
        val idx = ((raw - valueRange.start) / stepSize).toInt().coerceIn(0, steps + 1)
        return valueRange.start + idx * stepSize
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(enabled, valueRange, steps) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    onValueChange(snap((offset.x / size.width).coerceIn(0f, 1f)))
                    onValueChangeFinished?.invoke()
                }
            }
            .pointerInput(enabled, valueRange, steps) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        onValueChange(snap((offset.x / size.width).coerceIn(0f, 1f)))
                    },
                    onDragEnd = { onValueChangeFinished?.invoke() },
                    onDragCancel = { onValueChangeFinished?.invoke() },
                    onDrag = { change, _ ->
                        change.consume()
                        onValueChange(snap((change.position.x / size.width).coerceIn(0f, 1f)))
                    }
                )
            }
    ) {
        val midY = size.height / 2f
        val amp = waveAmplitude.toPx()
        val wl = waveLength.toPx()
        val strokeW = (amp * 1.1f).coerceAtLeast(3f)

        fun waveY(x: Float) = midY + amp * sin((x / wl) * 2.0 * PI + phase).toFloat()

        fun wavePath(): Path {
            val p = Path()
            if (size.width <= 0f) return p
            var x = 0f
            p.moveTo(x, waveY(x))
            x += 2f
            while (x <= size.width) {
                p.lineTo(x, waveY(x))
                x += 2f
            }
            return p
        }

        val wave = wavePath()
        drawPath(wave, inact, style = Stroke(width = strokeW, cap = StrokeCap.Round))
        if (fraction > 0f) {
            clipRect(right = size.width * fraction) {
                drawPath(wave, act, style = Stroke(width = strokeW, cap = StrokeCap.Round))
            }
        }

        // MD3 端点圆点
        drawCircle(
            inact,
            radius = strokeW,
            center = Offset(size.width - strokeW, waveY(size.width - strokeW))
        )

        // 圆形滑块
        val hx = size.width * fraction
        val hy = waveY(hx)
        drawCircle(Color.White.copy(alpha = 0.18f), radius = strokeW * 2.3f, center = Offset(hx, hy))
        drawCircle(handle, radius = strokeW * 1.45f, center = Offset(hx, hy))
    }
}


/** 播放器进度条样式：true = 蝌蚪波浪（M3 Expressive），false = 普通 MD3 进度条。 */
object ProgressStylePrefs {
    private const val PREF = "progress_style"
    private const val KEY = "wave_enabled"

    fun waveEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY, true)

    fun setWaveEnabled(ctx: Context, v: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY, v).apply()
    }
}
