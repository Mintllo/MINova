package com.HyperX.MI.Na.cmx

import android.graphics.Typeface
import android.os.Build
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.shadow.Shadow
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text as M3Text
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import com.HyperX.MI.Na.nativeui.glassSurface as nativeGlassSurface
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.min

/**
 * 上游 CurrentMusicX 库页所需但本工程未随带的底层符号。
 * 命名与上游一致，行为取最小等价实现（缺符号给 stub+TODO 的既定策略）。
 */

/** 上游 io.github.currencortex.music.data.settings.RecordShelfPreviewMode 照搬原值。 */
enum class RecordShelfPreviewMode { ONCE, LOOP, NEXT, RANDOM }

/** 页面是否处于前台可交互（上游由脚手架下发；本工程恒为 true）。 */
val LocalMusicPageActive = staticCompositionLocalOf { true }

/** 全局页脚安全垫（上游由底部栏测量下发；本工程暂为 0）。 */
val LocalMusicBottomInset = staticCompositionLocalOf { 0.dp }

/** 用户偏好的渲染帧率（上游接系统高刷设置；本工程暂不调整）。 */
val LocalRefreshRatePreference = staticCompositionLocalOf { 0 }

/** 全局自定义字体（上游接主题字体；本工程暂无）。 */
val LocalCustomTypeface = staticCompositionLocalOf<Typeface?> { null }

/** TODO 上游按帧率偏好调用 Display.getSupportedModes 调整 View 刷新率；本工程零改动策略下保持空实现。 */
fun applyViewRefreshRate(view: View, refreshRate: Int) {
}

/** 上游 feature.update 的玻璃背板句柄；直接复用 miuix blur 的 rememberLayerBackdrop。 */
@Composable
internal fun rememberUpdateBackdrop() = rememberLayerBackdrop()

/**
 * 上游 UpdateGlassButton（简化稳定版）：
 * 唱片架脚下的四个圆形按钮此前接入 nativeui 液态玻璃引擎后四连坏
 * （backdrop 捕获分辨率低 → 像素糊 / AGSL SDF 蒙版失配 → 方形底 /
 * 弹簧参数过激 → 乱抖 / onClick 未接线 → 点击无响应）。
 * 回退为纯 Compose 稳定实现：CircleShape 裁剪 + 半透明白玻璃面 +
 * ElasticPress 弹性缩放（graphicsLayer 内读值，不重组）+ drawBehind 按压高光，
 * clickable 直接挂在命中层，保证响应。无 backdrop 捕获，无 shader。
 */
@Composable
internal fun UpdateGlassButton(
    tag: String,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    buttonSize: Dp = 48.dp,
    content: @Composable () -> Unit,
) {
    val press = com.HyperX.MI.Na.nativeui.rememberElasticPressState()
    val interactions = remember { MutableInteractionSource() }
    // 果冻回弹（一眼可见）：按压净缩放压到 0.92，松手过冲到 1.06 再弹回落定 1.0
    val jelly = remember { Animatable(1f) }
    LaunchedEffect(press) {
        snapshotFlow { press.amount > .45f }.distinctUntilChanged().collect { down ->
            if (down) jelly.animateTo(.92f, spring(dampingRatio = .62f, stiffness = 620f))
            else {
                jelly.animateTo(1.06f, spring(dampingRatio = .38f, stiffness = 380f))
                jelly.animateTo(1f, spring(dampingRatio = .55f, stiffness = 300f))
            }
        }
    }
    com.HyperX.MI.Na.nativeui.ElasticPress(
        Modifier
            .size(buttonSize)
            .testTag(tag)
            .semantics { contentDescription = label }
            .clickable(
                enabled = enabled, role = androidx.compose.ui.semantics.Role.Button,
                interactionSource = interactions, indication = null, onClick = onClick,
            ),
        enabled = enabled, state = press,
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .alpha(if (enabled) 1f else .38f)
                .graphicsLayer {
                    // 按压形变加大：净缩放压到 0.92（抵消外层 ElasticPress 膨起），
                    // 松手 jelly 过冲回弹；全部在层属性内读值（零重组）
                    val amount = press.amount.coerceIn(0f, 1f)
                    val wrapperSwell = 1f + amount * min(.12f, 16.dp.toPx() / maxOf(size.width, size.height).coerceAtLeast(1f))
                    val net = jelly.value / wrapperSwell
                    scaleX = net; scaleY = net
                    translationX = press.pull.x * .35f
                    translationY = press.pull.y * .35f
                }
                .clip(CircleShape)
                // 纯净样式（任务 3）：去 drawBackdrop 液态管线，单层半透明白 .14 + 细描边；
                // 按压缩放（ElasticPress + 果冻回弹）保留。
                .background(Color.White.copy(alpha = .14f))
                .border(1.dp, Color.White.copy(alpha = .26f), CircleShape)
                .drawBehind {
                    // 按压高光：随 ElasticPress 抬升量增亮
                    if (press.amount > 0f) drawCircle(
                        Color.White.copy(alpha = .10f * press.amount.coerceIn(0f, 1f)))
                },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/** 上游 ui.component 的设置开关行 —— 真 MD3：M3 ListItem 排版（16dp 边距 token）+ M3 Switch。 */
@Composable
internal fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    ListItem(
        modifier = modifier.clickable(enabled = enabled) { onChange(!checked) },
        headlineContent = {
            M3Text(title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = if (summary != null) {
            {
                M3Text(summary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else null,
        trailingContent = {
            Switch(checked = checked, onCheckedChange = { onChange(it) }, enabled = enabled)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

/** 上游 ui.component 的对话框文字按钮 —— MD3 排版：TextButton + labelLarge token。 */
@Composable
internal fun MusicDialogButton(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary),
    ) {
        M3Text(text, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 上游 feature.lyrics.data.SongLyricsStore 的最小等价：歌曲同一性（定位动画用）。 */
internal object SongLyricsStore {
    fun identity(song: Song): String = "${song.id}:${song.musicSource.name}"
}

/**
 * 上游 LibraryViewModel.LibraryDetailState 的最小等价（歌单详情状态）。
 * albums/mv 为上游 catalog 字段的桩位（本工程未移植 catalog，TODO 接入时换成真实类型）。
 */
internal data class LibraryDetailState(
    val loading: Boolean = true,
    val title: String = "",
    val description: String = "",
    val cover: String = "",
    val songs: List<Song> = emptyList(),
    val playlist: Playlist? = null,
    val albums: List<String> = emptyList(),
    val more: Boolean = false,
    val moreAlbums: Boolean = false,
    val mv: String? = null,
    val error: String? = null,
    val loaded: Boolean = false,
    val refreshing: Boolean = false,
)
