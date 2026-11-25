// SPDX-License-Identifier: GPL-3.0-only
// GlassMaterial ported from CurrentMusicX ui/component/nativeui/GlassMaterial.kt
// (blob 0e6dfdbf, LiquidGlassStyle / liquid_glass.frag from chen08209's upstream).
// Package/import adaptation; optical pipeline and AGSL uniforms kept verbatim.
// Local addition: DeformableGlassShape feeds the drag-pull droplet into the
// shader's goo field, so the pressed glass surface is pulled out like jelly.
package com.HyperX.MI.Na.nativeui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.runtimeShaderEffect
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }
val LocalGlassBlurEnabled = staticCompositionLocalOf { true }
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }
val LiquidGlassOpticsKey = SemanticsPropertyKey<Boolean>("LiquidGlassOptics")

interface GlassOpticalShape : Shape {
    fun opticalGeometry(size: Size, density: Density, direction: LayoutDirection): GlassOpticalGeometry
}
data class GlassOpticalGeometry(val pane: Rect, val radius: Float,
    val droplet: Rect = Rect.Zero, val blend: Float = 0f,
    val extra: Rect = Rect.Zero, val extraBlend: Float = 0f)

/**
 * 拽动形变形状：pane 保持不动，droplet（goo 平滑最小化的第二个胶囊）跟随拖拽方向被
 * 拽出，blend 随按压量增大 —— 玻璃面像果冻一样被拉出形变，松手随 ElasticPress 弹簧回弹。
 */
class DeformableGlassShape(private val cornerRadius: Dp,
    private val pull: () -> androidx.compose.ui.geometry.Offset,
    private val amount: () -> Float) : GlassOpticalShape {
    override fun opticalGeometry(size: Size, density: Density, direction: LayoutDirection): GlassOpticalGeometry {
        val pane = Rect(0f, 0f, size.width, size.height)
        val r = with(density) { cornerRadius.toPx() }.coerceAtMost(size.minDimension / 2f)
        val p = pull()
        val a = amount().coerceIn(0f, 1f)
        val distance = p.getDistance()
        if (distance < .5f || a < .02f) return GlassOpticalGeometry(pane, r)
        val dir = androidx.compose.ui.geometry.Offset(p.x / distance, p.y / distance)
        val reach = min(distance, size.minDimension * .42f)
        val dropR = size.minDimension * .2f * a
        val cx = pane.center.x + dir.x * reach * .85f
        val cy = pane.center.y + dir.y * reach * .85f
        return GlassOpticalGeometry(pane, r,
            droplet = Rect(cx - dropR, cy - dropR, cx + dropR, cy + dropR),
            blend = size.minDimension * .34f * a)
    }
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        RoundedCornerShape(cornerRadius).createOutline(size, layoutDirection, density)
}

@Composable fun Modifier.glassSurface(backdrop: Backdrop?, shape: Shape, menu: Boolean = false,
    tint: Color? = null, glow: () -> Float = { 0f }, lens: Boolean = false,
    menuProgress: () -> Float = { if (menu) 1f else 0f }, frost: () -> Float = { if (menu) 12f else 2.5f },
    clipToShape: Boolean = true, strength: () -> Float = { 1f }, materialStrength: () -> Float = strength,
    alert: Boolean = false): Modifier {
    val dark = isSystemInDarkTheme()
    val glass = LocalLiquidGlassEnabled.current && Build.VERSION.SDK_INT >= 33
    val frosted = LocalGlassBlurEnabled.current
    val source = LocalContext.current.assets
    val shader = remember(source) { source.open("shaders/liquid_glass.agsl").bufferedReader().use { it.readText() } }
    val fill = tint ?: if (dark) Color(0xFF252528) else Color(0xFFFDFDFD)
    // Miuix's retained inner clip does not invalidate when only Shape changes. Clip outside
    // that node, so animated droplets and unfolding menus cannot retain a one-frame outline.
    val clipped = semantics { this[LiquidGlassOpticsKey] = glass && backdrop != null }
        .then(if (clipToShape) Modifier.graphicsLayerClip(shape)
            else Modifier)
    return if (backdrop != null && Build.VERSION.SDK_INT >= 33) clipped.drawBackdrop(backdrop,
        shape = { RectangleShape },
        effects = {
            val amount = strength().coerceIn(0f, 1f)
            val material = materialStrength().coerceIn(0f, 1f)
            val sigma = if (frosted && !lens) frost() * material else 0f
            // Frost may be reduced internally by Skia, but the optical pass and SDF mask
            // run at full resolution. Downsampling the finished pane softens its boundary.
            val sigmaPx = round(sigma * density * 16) / 16
            if (sigmaPx > 0f) blur(sigmaPx, sigmaPx)
            if (glass && size.width.isFinite() && size.height.isFinite() && size.width > 0f && size.height > 0f) {
                // 形变面（droplet 越界）需要更大的捕获 padding，避免拽出区域采样被钳制
                if (!clipToShape && padding < size.minDimension * .5f) padding = size.minDimension * .5f
                val geometry = (shape as? GlassOpticalShape)?.opticalGeometry(size, this, layoutDirection)
                    ?: when (val outline = shape.createOutline(size, layoutDirection, this)) {
                        is Outline.Rounded -> outline.roundRect.let {
                            GlassOpticalGeometry(Rect(it.left, it.top, it.right, it.bottom), it.topLeftCornerRadius.x)
                        }
                        else -> GlassOpticalGeometry(Rect(androidx.compose.ui.geometry.Offset.Zero, size), size.minDimension / 2)
                    }
                applyLiquidGlass(shader, geometry, fill, menuProgress().coerceIn(0f, 1f), dark, tint != null, glow(), lens, amount, material, alert)
            }
        },
        onDrawSurface = { if (!glass) drawOutline(shape.createOutline(size, layoutDirection, this), fill.copy(alpha = (if (menu) .92f else .82f) * strength().coerceIn(0f, 1f))) })
    else clipped.drawBehind { drawOutline(shape.createOutline(size, layoutDirection, this), fill.copy(alpha = .97f * strength().coerceIn(0f, 1f))) }
}

private fun Modifier.graphicsLayerClip(shape: Shape): Modifier = this.graphicsLayer {
    this.shape = shape; clip = true
}

private fun BackdropEffectScope.applyLiquidGlass(source: String, g: GlassOpticalGeometry, tint: Color,
    menu: Float, dark: Boolean, tinted: Boolean, glow: Float, lens: Boolean, amount: Float, material: Float, alert: Boolean) {
    val pad = padding
    val sf = downscaleFactor.coerceAtLeast(1).toFloat()
    val d = density
    val pane = g.pane
    val drop = g.droplet
    val extra = g.extra
    val r = g.radius.coerceIn(0f, pane.size.minDimension / 2)
    val pair = if (drop.isEmpty) pane else Rect(min(pane.left, drop.left), min(pane.top, drop.top),
        max(pane.right, drop.right), max(pane.bottom, drop.bottom))
    val union = if (extra.isEmpty) pair else Rect(min(pair.left, extra.left), min(pair.top, extra.top),
        max(pair.right, extra.right), max(pair.bottom, extra.bottom))
    val saturation = if (lens) 1f else if (tinted) 1.2f else glassLerp(if (dark) 2.84f else 2.08f, if (dark) 1.75f else 2f, menu)
    val brightness = if (lens && dark) 1.33f else glassLerp(1f, if (dark) 1.8f else 1.1f, menu)
    val opacity = if (lens) 0f else if (tinted) .82f else glassLerp(if (dark) .62f else .54f, if (dark) .63f else .55f, menu)
    // 边缘高光全清（用户要求）：rim / hairline / sideHairline 全部 0，玻璃边缘干净利落，只留普通细描边。
    val rim = 0f
    val hairline = 0f
    val sideHairline = 0f
    val halfWidth = pane.width / 2 / d; val halfHeight = pane.height / 2 / d; val roundR = r / d
    var lastTurn = 0f
    fun turn(along: Float, down: Float): Float {
        lastTurn = max(lastTurn, kotlin.math.atan2(halfWidth - min(along * roundR, halfWidth), halfHeight - min(down * roundR, halfHeight)))
        return lastTurn
    }
    val t1 = turn(1.53f, 0f); val t2 = turn(1f, 0f); val t3 = turn(.3f, .3f)
    val t4 = turn(0f, 1f); val t5 = turn(0f, 1.5f); val t6 = turn(0f, 2f)
    // Shader coordinates are dp, input samples are in the padded/downsampled capture's pixels.
    runtimeShaderEffect("LiquidGlassUltra", source, "content") {
        setFloatUniform("uFrame", -pad / d, -pad / d, sf / d, sf / d)
        setFloatUniform("uSample", -pad / d, -pad / d, sf / d, sf / d)
        setFloatUniform("uPane", pane.center.x / d, pane.center.y / d, pane.width / 2 / d, pane.height / 2 / d)
        setFloatUniform("uDrop", drop.center.x / d, drop.center.y / d, drop.width / 2 / d, drop.height / 2 / d)
        setFloatUniform("uExtra", extra.center.x / d, extra.center.y / d, extra.width / 2 / d, extra.height / 2 / d)
        setFloatUniform("uExtraBlend", g.extraBlend / d)
        setFloatUniform("uGeometry", r / d, g.blend / d, if (drop.width > .1f) 1f else 0f, sf / d)
        setFloatUniform("uHalfMinor", union.size.minDimension / 2 / d)
        setFloatUniform("uOptics", (if (lens) -4.3f else 60f) * amount,
            glassLerp(1f, saturation, material), glassLerp(1f, brightness, material), if (lens) 5.5f else 20f)
        setFloatUniform("uTint", tint.red, tint.green, tint.blue, opacity * material)
        setFloatUniform("uLight", rim * amount, if (lens) 1.1f else 1.2f, if (lens) 0f else .17f, if (lens) 0f else .38f)
        setFloatUniform("uEdge", hairline * amount,
            sideHairline * (if (lens) 1f else 1 - menu) * amount, (if (dark) .22f else .16f) * menu * amount,
            (if (lens) 0f else if (dark) glassLerp(-.4f, -.53f, menu) else .4f * (1 - menu)) * material)
        setFloatUniform("uLens", if (lens) 6.5f * amount else 0f, if (lens) 1.5f * amount else 0f, if (lens) .045f * amount else 0f)
        setFloatUniform("uGlow", pane.center.x / d, pane.center.y / d, glow.coerceIn(0f, 1f) * .16f * amount, pane.height / d / sqrt(2f))
        setFloatUniform("uBounds", (size.width + 2 * pad) / sf, (size.height + 2 * pad) / sf)
        setFloatUniform("uShineTurns0", 0f, t1, t2, t3)
        setFloatUniform("uShineTurns1", t4, t5, t6, PI.toFloat() / 2)
    }
}

private fun glassLerp(a: Float, b: Float, t: Float) = a + (b - a) * t
