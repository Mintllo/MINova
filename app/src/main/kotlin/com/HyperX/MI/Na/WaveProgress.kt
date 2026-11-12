package com.HyperX.MI.Na

import android.content.Context

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.SliderColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 应用动态色（KSU/materialkolor），进度条默认跟随它。 */
@Composable
private fun dynamicAccent(): Color = MiuixTheme.colorScheme.primary

/**
 * 只读进度条 = 官方 M3 Expressive 波浪进度条（下载/更新用，用户认可的标准形态 ✓）。
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
 * 可拖动进度条 = 与更新进度条**完全同款**的官方 M3 Expressive 波浪组件
 * （同参数：振幅 2 / 波长 40 / 流速 26 / 端点 4）+ 透明手势层（点按/拖动 seek ✓）。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    @Suppress("UNUSED_PARAMETER") waveAmplitude: Dp = 3.dp,
    @Suppress("UNUSED_PARAMETER") waveLength: Dp = 34.dp,
    @Suppress("UNUSED_PARAMETER") colors: SliderColors? = null
) {
    val act = if (activeColor != Color.Unspecified) activeColor else dynamicAccent()
    val track = if (inactiveColor != Color.Unspecified) inactiveColor else act.copy(alpha = 0.28f)
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    fun snap(f: Float): Float {
        val raw = valueRange.start + f * (valueRange.endInclusive - valueRange.start)
        if (steps <= 0) return raw
        val stepSize = (valueRange.endInclusive - valueRange.start) / (steps + 1)
        val idx = ((raw - valueRange.start) / stepSize).toInt().coerceIn(0, steps + 1)
        return valueRange.start + idx * stepSize
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .clipToBounds()
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
        LinearWavyProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxSize(),
            color = act,
            trackColor = track,
            amplitude = { 0.5f },
            wavelength = 40.dp,
            waveSpeed = 26.dp,
            stopSize = 4.dp
        )
    }
}

/** 播放器进度条样式偏好。 */
object ProgressStylePrefs {
    private const val PREF = "progress_style"
    private const val KEY = "wave_enabled"

    fun waveEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY, true)

    fun setWaveEnabled(ctx: Context, v: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY, v).apply()
    }
}
