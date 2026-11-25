// 统一进场/退场动效：所有子页与弹窗共用同一套「折叠展开」（像纸张以顶边为轴展开）。
// 缓动取自 CurrentMusicX（GPL-3.0，_cmx_ref）：进场 sheetEasing 420ms，
// 退场反向折叠沿用 MusicOverlayDialog 的 cmxReturnEasing 300ms。
// 替换原先各入口不统一的 fade+scale / blur+进场，全站入口动作一致。
package com.HyperX.MI.Na

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

/** 进场折叠缓动，同全屏播放页展开（PlayerSheetTransition 的 sheetEasing）。 */
val foldEnterEasing = CubicBezierEasing(.2f, 0f, 0f, 1f)

/** 退场反向折叠缓动，同全局返回节奏（cmxReturnEasing）。 */
val foldExitEasing = CubicBezierEasing(.32f, 0f, .67f, .2f)

/**
 * 折叠动画播放中的全局计数（进场或退场任一播放时 >0）。
 * 液态玻璃面板在折叠转场期间采样 backdrop 不可靠（子页离屏合成 + 3D 变换会让
 * LayerBackdrop 的坐标/内容出现瞬时不一致），导航栏等玻璃面板据此切换静态玻璃色兜底。
 */
object FoldTransitionBus {
    private var count by mutableStateOf(0)

    val activeCount: Int get() = count

    fun begin() {
        count++
    }

    fun end() {
        count = (count - 1).coerceAtLeast(0)
    }
}

/**
 * 折叠效果：scaleY 从 0.1→1 + rotationX 从 -75°→0（顶部为轴，透视相机）+ alpha 渐入。
 * progress 0→1 为进场折叠展开，1→0 为退场反向折叠（同一曲线倒放）。
 */
fun Modifier.foldEnter(progress: Float): Modifier = graphicsLayer {
    applyFoldTransform(progress)
}

private fun GraphicsLayerScope.applyFoldTransform(progress: Float) {
    val p = progress.coerceIn(0f, 1f)
    scaleY = 0.1f + 0.9f * p
    rotationX = -75f * (1f - p)
    cameraDistance = 12f * density
    transformOrigin = TransformOrigin(0.5f, 0f)
    alpha = p
}

/**
 * 统一入口组件：visible 翻转时驱动折叠动画，替代各处 AnimatedVisibility 的不统一进出场。
 * 进场 tween(420, sheetEasing)，退场反向折叠 tween(300, cmxReturnEasing)。
 * 每次从不可见变可见（含弹窗首次组合）都播放折叠展开。
 * 动画进度在 graphicsLayer 内逐帧读取，不触发内容重组。
 */
@Composable
fun FoldEnterHost(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 进度一律从 0 起播：子页进场与弹窗（visible 恒 true 的包装）都在组合时展开折叠
    val progress = remember { Animatable(0f) }
    var mounted by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        if (visible) {
            mounted = true
            FoldTransitionBus.begin()
            try {
                progress.animateTo(1f, tween(420, easing = foldEnterEasing))
            } finally {
                FoldTransitionBus.end()
            }
        } else if (progress.value > 0f) {
            FoldTransitionBus.begin()
            try {
                progress.animateTo(0f, tween(300, easing = foldExitEasing))
            } finally {
                FoldTransitionBus.end()
            }
            mounted = false
        }
    }
    if (mounted) {
        Box(modifier.graphicsLayer { applyFoldTransform(progress.value) }) {
            content()
        }
    }
}

/**
 * 轻量入场组件：alpha + translationY 上浮（240ms）。
 * 与 FoldEnterHost 的整页折叠互补 —— 用于 tab 内容块 / 列表 item / 设置分组这类
 * 「静态出现」的小块：组合时从透明 + 上浮 offsetDp 落定，不阻塞滚动。
 *  - 动画由 item 局部 visible 状态驱动，LazyColumn 只在 item 首次组合时播一次。
 *  - index >= 0 时按全局 32ms 错峰（上限 10 项），超出上限不再累加延迟。
 *  - 节奏统一委托 listItemEnter（同一处定义时长/错峰），避免两套规格。
 */
@Composable
fun FadeSlideIn(
    modifier: Modifier = Modifier,
    index: Int = -1,
    content: @Composable () -> Unit
) {
    Box(modifier.listItemEnter(index)) {
        content()
    }
}

/**
 * 图片加载淡入：AsyncImage 的换图/首载闪变 → 载入完成前透明，Success 后 240ms 淡入。
 * 用 AsyncImage 原生 onState 回调感知加载结果，alpha 由 animateFloatAsState 驱动；
 * 换 model 时先淡出旧图再淡入新图，等效 Crossfade(240)。
 * 仅替换列表里的封面/头像等闪变明显的位置；全屏背景图保持原 AsyncImage 不动。
 */
@Composable
fun FadingAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center
) {
    var loaded by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (loaded) 1f else 0f,
        animationSpec = tween(240, easing = foldEnterEasing),
        label = "imageLoadFade"
    )
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        alignment = alignment,
        onState = { state -> loaded = state is AsyncImagePainter.State.Success },
        modifier = modifier.graphicsLayer { this.alpha = alpha }
    )
}
