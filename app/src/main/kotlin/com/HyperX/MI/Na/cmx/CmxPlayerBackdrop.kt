/*
 * Ported from CurrentMusicX (https://github.com/bileizhen/CurrentMusicX)
 * original package: io.github.currencortex.music.feature.player (PlayerBackdrop.kt)
 * License: GPL-3.0 (upstream). Adapted for MINova �?coil3 + our Song.picUrl wiring.
 * Three paths, mirroring upstream PlayerBackdrop:
 *   - custom image          �?scale + blur (upstream CustomPlayerBackground)
 *   - dynamic (default)     �?palette-driven flowing background. Upstream uses the
 *     MIUI BgEffectBackground runtime shader; MINova rebuilds the same look with
 *     standard Compose (base vertical gradient + drifting radial colour pools),
 *     palette comes from the C++ extractor (libminova nativeArtworkPalette).
 *   - frosted (fallback)    �?software-blurred 192px cover (blur in libminova,
 *     nativeBlurPixels; Kotlin loop stays as the no-native fallback).
 */
package com.HyperX.MI.Na.cmx

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.SuccessResult
import coil3.toBitmap
import com.HyperX.MI.Na.native.NativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Iterative box blur �?Kotlin fallback (kept bit-compatible with the C++ version:
 * separable horizontal �?vertical, 2 rounds, clamp edges).
 */
private fun blurBackgroundPixelsFallback(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
    val r = radius.coerceIn(1, 24)
    val div = 2 * r + 1
    var src = pixels.copyOf()
    var dst = IntArray(pixels.size)
    repeat(2) {
        // Horizontal pass: src �?dst
        for (y in 0 until height) {
            var sumA = 0; var sumR = 0; var sumG = 0; var sumB = 0
            val row = y * width
            for (x in -r..r) {
                val c = src[row + x.coerceIn(0, width - 1)]
                sumA += c ushr 24 and 255; sumR += c ushr 16 and 255; sumG += c ushr 8 and 255; sumB += c and 255
            }
            for (x in 0 until width) {
                val i = row + x
                dst[i] = ((sumA / div) shl 24) or ((sumR / div) shl 16) or ((sumG / div) shl 8) or (sumB / div)
                val co = src[row + (x - r).coerceIn(0, width - 1)]
                val ci = src[row + (x + r + 1).coerceIn(0, width - 1)]
                sumA += (ci ushr 24 and 255) - (co ushr 24 and 255)
                sumR += (ci ushr 16 and 255) - (co ushr 16 and 255)
                sumG += (ci ushr 8 and 255) - (co ushr 8 and 255)
                sumB += (ci and 255) - (co and 255)
            }
        }
        src = dst.also { dst = src }
        // Vertical pass: src �?dst
        for (x in 0 until width) {
            var sumA = 0; var sumR = 0; var sumG = 0; var sumB = 0
            for (y in -r..r) {
                val c = src[y.coerceIn(0, height - 1) * width + x]
                sumA += c ushr 24 and 255; sumR += c ushr 16 and 255; sumG += c ushr 8 and 255; sumB += c and 255
            }
            for (y in 0 until height) {
                val i = y * width + x
                dst[i] = ((sumA / div) shl 24) or ((sumR / div) shl 16) or ((sumG / div) shl 8) or (sumB / div)
                val co = src[(y - r).coerceIn(0, height - 1) * width + x]
                val ci = src[(y + r + 1).coerceIn(0, height - 1) * width + x]
                sumA += (ci ushr 24 and 255) - (co ushr 24 and 255)
                sumR += (ci ushr 16 and 255) - (co ushr 16 and 255)
                sumG += (ci ushr 8 and 255) - (co ushr 8 and 255)
                sumB += (ci and 255) - (co and 255)
            }
        }
        src = dst.also { dst = src }
    }
    return src
}

/** 盒式模糊：优�?libminova（nativeBlurPixels），失败无缝回退 Kotlin 循环�?*/
internal fun blurBackgroundPixels(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
    return NativeBridge.blurPixels(pixels, width, height, radius)
        ?: blurBackgroundPixelsFallback(pixels, width, height, radius)
}

/**
 * CMX PlayerBackdrop: warm dark base, then one of the three background paths,
 * then the shared vertical scrim (33%�?2% black).
 */
@Composable
fun CmxPlayerBackdrop(
    coverUrl: String,
    modifier: Modifier = Modifier,
    customImage: Any? = null,
    blurPercent: Float = 20f,
    dynamic: Boolean = false,
    active: Boolean = true,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // 封面 512px → 模糊 18dp → 缩放 0.92 → 直接铺满 ✓ 简洁通透 ✓
    val blurredBitmap by androidx.compose.runtime.key(coverUrl) {
        androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, coverUrl) {
            if (coverUrl.isBlank()) return@produceState
            kotlinx.coroutines.delay(60)
            val result = coil3.SingletonImageLoader.get(context).execute(
                coil3.request.ImageRequest.Builder(context)
                    .data(coverUrl).size(512).allowHardware(false).build())
            value = if (result is coil3.request.SuccessResult)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    val input = result.image.toBitmap()
                    android.graphics.Bitmap.createBitmap(
                        blurBackgroundPixels(
                            IntArray(input.width * input.height).also {
                                input.getPixels(it, 0, input.width, 0, 0, input.width, input.height)
                            },
                            input.width, input.height, 18
                        ),
                        input.width, input.height, android.graphics.Bitmap.Config.ARGB_8888
                    ).asImageBitmap()
                } else null
        }
    }
    Box(modifier.clipToBounds()) {
        // 暗色底
        Box(Modifier.matchParentSize().background(Color.White.copy(alpha = .08f)))
        // 封面模糊图 铺满 + 缩放 0.92
        blurredBitmap?.let { bmp ->
            Image(bmp, null, Modifier.matchParentSize()
                .graphicsLayer { scaleX = .92f; scaleY = .92f }
                .blur(6.dp), contentScale = ContentScale.Crop)
        }
        // 暗化渐变（保证文字可读 ✓ 但不过黑 ✓）
        Box(Modifier.matchParentSize().background(
            Brush.verticalGradient(listOf(
                Color.White.copy(alpha = .04f),
                Color.Transparent,
                Color.White.copy(alpha = .06f)))))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 动态流动背景（PlayerBackdrop.kt ArtworkPlayerBackground 的标�?Compose 等价）：
// 底层「深�?171719 × 封面�?辅色」垂直渐�?+ 4 个缓慢漂移的径向色池
// （上�?BgEffectBackground palette 池同构：主色 .85 / 辅色 .8 / 混合 .65 / 暗化 .9），
// 播放时光感增强、暂停时收敛。全�?GPU 渐变，无逐帧 JNI�?// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CmxDynamicBackground(coverUrl: String, active: Boolean, modifier: Modifier) {
    // 合并上游 FrostedArtworkBackground（封面模糊铺�?✓）+ ArtworkPlayerBackground（取色渐�?✓）
    val palette by rememberCmxArtworkPalette(coverUrl)
    val primary by animateColorAsState(palette.primary, tween(900), label = "cmx bg primary")
    val secondary by animateColorAsState(palette.secondary, tween(900), label = "cmx bg secondary")

    // 第一层：封面 192px 模糊图铺底（�?FrostedCmxBackground 同逻辑 ✓）
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap by androidx.compose.runtime.key(coverUrl) {
        androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, context, coverUrl) {
            if (coverUrl.isBlank()) return@produceState
            kotlinx.coroutines.delay(90)
            val result = coil3.SingletonImageLoader.get(context).execute(
                coil3.request.ImageRequest.Builder(context).data(coverRequestUrl(coverUrl, 192)).size(512).allowHardware(false).build())
            value = if (result is coil3.request.SuccessResult) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val input = result.image.toBitmap()
                val opaque = android.graphics.Bitmap.createBitmap(input.width, input.height, android.graphics.Bitmap.Config.ARGB_8888)
                android.graphics.Canvas(opaque).apply { drawColor(android.graphics.Color.rgb(66,64,63)); drawBitmap(input, 0f, 0f, null) }
                val pixels = IntArray(input.width * input.height)
                opaque.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
                opaque.recycle()
                android.graphics.Bitmap.createBitmap(
                    blurBackgroundPixels(pixels, input.width, input.height, 8),
                    input.width, input.height, android.graphics.Bitmap.Config.ARGB_8888
                ).asImageBitmap()
            } else null
        }
    }
    Box(modifier.background(Color.White.copy(alpha = .08f))) {
        // 封面模糊图铺�?        
        androidx.compose.animation.Crossfade(bitmap, animationSpec = androidx.compose.animation.core.tween(600), label = "cmx bg cover") { img ->
            if (img != null) androidx.compose.foundation.Image(img, null, Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        }
        // 第二层：封面取色渐变叠加（上�?ArtworkPlayerBackground 的色彩系�?✓）
        Box(Modifier.matchParentSize().background(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(
                androidx.compose.ui.graphics.lerp(Color(0xFF171719), primary, .6f),
                androidx.compose.ui.graphics.lerp(Color(0xFF171719), secondary, .35f)))))
    }
}



@Composable
private fun FrostedCmxBackground(coverUrl: String, percent: Float, modifier: Modifier) {
    val context = LocalContext.current
    val blur = if (percent.isFinite()) percent.coerceIn(0f, 100f).roundToInt() else 35
    val bitmap by key(coverUrl) {
        produceState<ImageBitmap?>(null, context, coverUrl, blur) {
            if (coverUrl.isBlank()) return@produceState
            delay(90)
            val result = runCatching {
                SingletonImageLoader.get(context).execute(
                    ImageRequest.Builder(context).data(coverUrl).size(512).allowHardware(false).build()
                )
            }.getOrNull() as? SuccessResult
            value = if (result != null) withContext(Dispatchers.Default) {
                runCatching {
                    val input = result.image.toBitmap()
                    // Flatten transparent artwork onto the warm base before filtering.
                    val opaque = android.graphics.Bitmap.createBitmap(
                        input.width, input.height, android.graphics.Bitmap.Config.ARGB_8888
                    )
                    android.graphics.Canvas(opaque).apply {
                        drawColor(android.graphics.Color.rgb(66, 64, 63))
                        drawBitmap(input, 0f, 0f, null)
                    }
                    val pixels = IntArray(input.width * input.height)
                    opaque.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
                    opaque.recycle()
                    android.graphics.Bitmap.createBitmap(
                        blurBackgroundPixels(pixels, input.width, input.height, (blur * .15f).roundToInt()),
                        input.width, input.height, android.graphics.Bitmap.Config.ARGB_8888,
                    ).asImageBitmap()
                }.getOrNull()
            } else null
        }
    }
    Box(modifier.background(Color.White.copy(alpha = .08f))) {
        Crossfade(bitmap, animationSpec = tween(600), label = "cmx frosted artwork") { image ->
            if (image != null) Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(Modifier.matchParentSize().background(Color(0xFFB5B1A8).copy(alpha = .08f)))
    }
}

@Composable
private fun CustomCmxBackground(file: Any?, percent: Float, modifier: Modifier) {
    val blur = if (percent.isFinite()) percent.coerceIn(0f, 100f) else 35f
    val scale = 1f + .3f * blur / 100f
    coil3.compose.AsyncImage(
        model = file,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }.blur((blur * .8f).dp),
    )
}
