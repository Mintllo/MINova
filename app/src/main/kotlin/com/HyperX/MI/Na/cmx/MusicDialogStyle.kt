package com.HyperX.MI.Na.cmx

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.dropShadow
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.colorControls
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 照搬上游 _cmx_ref/MusicDialogStyle.kt 的液态玻璃系统（改包名）：
 * glassSurface（miuix blur drawBackdrop 管线：frost 模糊 + 降饱和 + 表面 wash）
 * + alertGlassRim（边缘光）+ dropShadow(29dp/8dp) + LayerBackdrop 页面捕获。
 * glassSurface 在上游位于 nativeui/GlassMaterial.kt，此处按同一行为在 miuix blur 上复刻。
 */

internal val MusicDialogShape = RoundedCornerShape(32.dp)
internal val LocalMusicDialogBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** 液态玻璃总开关（上游 nativeui.LocalLiquidGlassEnabled，本工程默认开启）。 */
internal val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }
internal val LocalGlassBlurEnabled = staticCompositionLocalOf { true }

internal fun musicDialogFallbackColor(dark: Boolean) = if (dark) Color(0xF22C2D30) else Color(0xDDF2F3F5)

/** 捕获页面内容：API 33+ 且支持 RuntimeShader 才提供真液态玻璃，否则返回 null 走回退。 */
@Composable internal fun rememberMusicDialogBackdrop(): LayerBackdrop? =
    if (Build.VERSION.SDK_INT >= 33 && LocalView.current.isHardwareAccelerated && isRuntimeShaderSupported())
        rememberLayerBackdrop() else null

/** 内容层挂捕获源（挂在页面内容上，而不是弹层自身，避免模糊自反馈）。 */
@Composable internal fun Modifier.musicDialogBackdropSource(): Modifier {
    val backdrop = LocalMusicDialogBackdrop.current
    return if (backdrop != null) then(Modifier.layerBackdrop(backdrop)) else this
}

/**
 * 液态玻璃表面：捕获 backdrop → frost 模糊 + 降饱和 → 表面提亮 wash。
 * menu/frost 参数与上游 glassSurface 对齐（frost=8f，本工程降噪版）。
 */
internal fun Modifier.glassSurface(
    backdrop: Backdrop?,
    shape: Shape,
    dark: Boolean,
    menu: Boolean = false,
    frost: () -> Float = { 8f },
): Modifier {
    if (backdrop == null || Build.VERSION.SDK_INT < 33 || !isRuntimeShaderSupported()) return this
    return drawBackdrop(
        backdrop,
        shape = { shape },
        effects = {
            blur(frost().dp.toPx(), frost().dp.toPx())
            colorControls(brightness = 0f, contrast = 1f, saturation = .42f)
        },
        onDrawSurface = {
            drawRect(if (dark) Color.White.copy(alpha = .08f) else Color.White.copy(alpha = .30f))
        },
    )
}

/** LiquidGlassRim.alert：更强的左右侧边、安静的平直段与独立的高光描边。 */
internal fun Modifier.alertGlassRim(shape: Shape, dark: Boolean): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val radius = (outline as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x
        ?: (size.minDimension / 2f)
    val across = (radius / size.width.coerceAtLeast(1f)).coerceIn(0f, .4f)
    val side = if (dark) .75f else .35f
    val corner = if (dark) .1f else .22f
    val flat = if (dark) .03f else .12f
    val rim = Brush.horizontalGradient(
        0f to Color.Black.copy(alpha = side), across / 4f to Color.Black.copy(alpha = corner),
        across to Color.Black.copy(alpha = flat), 1f - across to Color.Black.copy(alpha = flat),
        1f - across / 4f to Color.Black.copy(alpha = corner), 1f to Color.Black.copy(alpha = side))
    val stroke = Stroke(.67.dp.toPx())
    onDrawWithContent { drawContent(); drawOutline(outline, rim, style = stroke) }
}

/** 上游 musicDialogMaterial：dropShadow(29dp/8dp) + glassSurface(menu, frost 16f) + alertGlassRim。 */
@Composable internal fun Modifier.musicDialogMaterial(
    backdrop: Backdrop? = LocalMusicDialogBackdrop.current,
    shape: Shape = MusicDialogShape,
    drawFallback: Boolean = true,
): Modifier {
    val dark = isSystemInDarkTheme()
    val glass = LocalLiquidGlassEnabled.current && backdrop != null && Build.VERSION.SDK_INT >= 33
    if (glass) return then(Modifier
        .dropShadow(shape = shape, shadow = Shadow(
            radius = 29.dp, color = Color.Black, alpha = if (dark) .177f else .118f))
        .glassSurface(backdrop, shape, dark, menu = true, frost = { 8f })
        .alertGlassRim(shape, dark))
    val material = if (Build.VERSION.SDK_INT >= 33 && LocalGlassBlurEnabled.current && backdrop != null)
        frostedMusicDialog(backdrop, dark, shape)
    else if (drawFallback) Modifier.background(musicDialogFallbackColor(dark), shape) else Modifier
    return then(material)
        .border(.7.dp, Color.White.copy(alpha = if (dark) .12f else .65f), shape)
}

/** 无 LayerBackdrop 但可实时模糊时的毛玻璃回退（同上游 frostedMusicDialog）。 */
@androidx.annotation.RequiresApi(33)
private fun Modifier.frostedMusicDialog(backdrop: Backdrop, dark: Boolean, shape: Shape): Modifier =
    then(Modifier.drawBackdrop(backdrop, shape = { shape }, effects = {
        colorControls(brightness = 0f, contrast = 1f, saturation = .35f)
        blur(28f.dp.toPx(), 28f.dp.toPx())
    }, onDrawSurface = {
        drawRect(if (dark) Color(0xFF292A2D).copy(alpha = .86f) else Color(0xFFF3F4F6).copy(alpha = .68f))
    }))
