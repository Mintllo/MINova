package com.HyperX.MI.Na

import android.content.Context

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.SliderColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.HyperX.MI.Na.native.NativeBridge
import kotlin.math.PI
import kotlin.math.sin
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 应用动态色（KSU/materialkolor），进度条默认跟随它。 */
@Composable
private fun dynamicAccent(): Color = MiuixTheme.colorScheme.primary

/**
 * 只读进度条 = **官方 Material 3 标准进度条**（LinearProgressIndicator）。
 * M3 规范：圆角活动轨道 + 4dp 间隙 + 端点指示圆点 + 圆角非活动轨道。
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
    // 振幅 0 = 标准 MD3 直条（官方组件自带 轨道 + 间隙 + 端点圆点）
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
 * 可拖动进度条 = **纯自绘 Canvas**（不再用官方波浪组件，整条线不再一起摆）：
 *
 * [起点] –静止段(≥12dp·10%playX)– [波浪区（仅此段波动）] –静止段(≥24dp·18%playX)– [竖条@playX] –未播直线(track)– [端点圆点]
 *
 * 波浪只在中间区段流动：振幅 1.2dp、波长 26dp，两端各留 14dp 渐变收为 0。
 * 两侧静止段随进度按比例放大（起点 ≥max(12dp, 10%·playX)，竖条前 ≥max(24dp, 18%·playX)），
 * 避免波浪占满整个已播区 —— 头和尾固定不动，只有中间波动。
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

    // 波浪相位：匀速流动（一个完整波形 1.5s 走 2π）
    val twoPi = (2.0 * PI).toFloat()
    val phase = rememberInfiniteTransition(label = "wavePhase").animateFloat(
        initialValue = 0f,
        targetValue = twoPi,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "wavePhase"
    ).value

    // native 波浪 y 值缓冲（remember 复用，绘制期原地扩容，不触发重组）。
    // 说明：phase 是逐帧动画值，「按量化 phase 缓存」会让波形按量化步长跳变
    // （违反「视觉必须不变」红线），因此不做相位量化缓存 —— native 每帧重算仅 µs 级，
    // 这里只缓存 FloatArray 缓冲本身，避免每帧分配。
    val waveBufHolder = remember { arrayOf(FloatArray(0)) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val midY = size.height / 2f
            val lineW = 3.dp.toPx()
            val barW = 3.dp.toPx()
            val barH = 16.dp.toPx()
            // 竖条位置 = 播放进度
            val playX = (barW / 2f + (size.width - barW) * fraction)
                .coerceIn(barW / 2f, size.width - barW / 2f)
            // 静止段随进度按比例放大：竖条前 ≥max(24dp, 18%·playX)，起点 ≥max(12dp, 10%·playX)。
            // 之前是固定 12dp/24dp，波浪几乎占满整个已播区（只有竖条前一点点不波动）。
            val headStatic = maxOf(24.dp.toPx(), playX * 0.18f)  // 竖条前静止段
            val tailStatic = maxOf(12.dp.toPx(), playX * 0.10f)  // 起点静止段
            val taper = 14.dp.toPx()        // 波浪两端渐变收 0 的区段
            val amp = 1.2.dp.toPx()         // 振幅（幅度适中偏小）
            val wavelength = 26.dp.toPx()
            val x0 = tailStatic
            val x1 = playX - headStatic

            // ① 起点静止段（头不动）
            val headEnd = minOf(x0, playX)
            if (headEnd > 0f) {
                drawLine(act, Offset(0f, midY), Offset(headEnd, midY), lineW, StrokeCap.Round)
            }
            // ② 波浪区：只有这段波动，两端 14dp 渐变收 0，与静止段平滑衔接
            if (x1 > x0) {
                clipRect(left = x0, right = x1) {
                    val wave = Path()
                    wave.moveTo(x0, midY)
                    // native 预计算（libminova.so）：数学与下方 Kotlin 循环逐位一致
                    // （同平台 libm sin、同 float 步进累加、C++ 侧 -ffp-contract=off 禁 FMA）。
                    // 返回 -1 = 不可用/失败 → 走原 Kotlin 循环，视觉完全不变。
                    val need = ((x1 - x0) / 2f).toInt() + 4
                    var buf = waveBufHolder[0]
                    if (buf.size < need) {
                        buf = FloatArray(need)
                        waveBufHolder[0] = buf
                    }
                    val n = NativeBridge.computeWave(
                        size.width.toInt(), midY, amp, wavelength, phase, x0, x1, taper, buf
                    )
                    if (n >= 0) {
                        var x = x0 + 2f
                        for (i in 0 until n) {
                            wave.lineTo(x, buf[i])
                            x += 2f
                        }
                    } else {
                        var x = x0 + 2f
                        while (x < x1) {
                            val up = ((x - x0) / taper).coerceIn(0f, 1f)
                            val down = ((x1 - x) / taper).coerceIn(0f, 1f)
                            val env = minOf(up, down)
                            val y = midY - env * amp * sin(twoPi * (x - x0) / wavelength + phase)
                            wave.lineTo(x, y)
                            x += 2f
                        }
                    }
                    wave.lineTo(x1, midY)
                    drawPath(wave, act, style = Stroke(width = lineW, cap = StrokeCap.Round))
                }
            }
            // ③ 竖条前静止段（尾不动）
            val tailStart = maxOf(x1, x0)
            if (playX > tailStart) {
                drawLine(act, Offset(tailStart, midY), Offset(playX, midY), lineW, StrokeCap.Round)
            }
            // ④ 未播段 + 端点圆点
            val dotR = 3.dp.toPx()
            val dotCx = size.width - dotR
            if (dotCx - dotR > playX) {
                drawLine(track, Offset(playX, midY), Offset(dotCx - dotR, midY), lineW, StrokeCap.Round)
            }
            drawCircle(track, radius = dotR, center = Offset(dotCx, midY))
            // ⑤ 播放头竖条（保留原有画法）
            drawRoundRect(
                color = if (thumbColor != Color.Unspecified) thumbColor else act,
                topLeft = Offset(playX - barW / 2f, midY - barH / 2f),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f)
            )
        }
    }
}

/** 播放器进度条样式：true = 波浪（M3 Expressive），false = 标准 MD3 进度条。 */
object ProgressStylePrefs {
    private const val PREF = "progress_style"
    private const val KEY = "wave_enabled"

    fun waveEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY, true)

    fun setWaveEnabled(ctx: Context, v: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY, v).apply()
    }
}
