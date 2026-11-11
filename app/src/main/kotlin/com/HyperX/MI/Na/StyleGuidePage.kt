package com.HyperX.MI.Na

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlinx.coroutines.CancellationException
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 引导页入场规格：时长 / 缓动 / 错峰全部取自 [Motion]，这里只负责把错峰延迟挂上，
 * 避免在页面里另立一套 300ms、280f 之类的散装数值。
 */
private fun <T> motionEnter(index: Int = 0): FiniteAnimationSpec<T> = tween(
    durationMillis = Motion.ENTER_MS,
    delayMillis = Motion.staggerDelayMillis(index).toInt(),
    easing = Motion.enterEasing
)

@Composable
fun StyleGuidePage(
    currentLiquid: Boolean,
    onSelect: (liquid: Boolean) -> Unit,
    onImportNow: () -> Unit,
    /** 「记住上次播放」开关的当前值（风格选定后即可调整） */
    rememberLast: Boolean = false,
    onRememberLastChange: (Boolean) -> Unit = {},
    /** 「稍后我再看」：为 null 时退回 onSelect（保持老调用点的行为）。 */
    onLater: ((liquid: Boolean) -> Unit)? = null,
    /** 主按钮「继续」：为 null 时退回 onSelect（引导里用它收尾结束引导）。 */
    onContinue: ((liquid: Boolean) -> Unit)? = null,
    currentKeyColor: Int = 0,
    onKeyColor: (Int) -> Unit = {},
) {
    // 液态玻璃风格依赖 RuntimeShader/AGSL（API 33+）：低版本禁选并自动回退兼容风格（MD3）
    val liquidSupported = liquidGlassSupported
    // 低版本直接预选 Material Design 3（列表里第一个不依赖 shader 的风格），液态玻璃卡片置灰不可选
    var choice by remember { mutableStateOf(if (liquidSupported) null else false) }
    var appeared by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { appeared = true }

    // 风格确认：选中后 2 秒倒计时，结束才允许继续
    LaunchedEffect(choice) {
        if (choice != null) {
            countdown = 2
            try {
                while (countdown > 0) {
                    delay(1000)
                    countdown -= 1
                }
            } catch (e: CancellationException) {
                // 正常取消，忽略
            }
        } else {
            countdown = 0
        }
    }
    val ready = choice != null && countdown == 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // 风格页覆盖层：整屏拦截触摸，防止透传到下层底部导航（Tab 误切）
            .blockTouchesBehindOverlay()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn(motionEnter(0)) +
                        slideInVertically(motionEnter(0)) { it / 8 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        "欢迎使用 MINova",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "我们提供两种风格，请选择你喜欢的样式",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "您可以在应用设置内自由调整风格",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn(motionEnter(1)) +
                        slideInVertically(motionEnter(1)) { it / 8 }
            ) {
                StyleOptionCard(
                    selected = choice == true,
                    icon = {
                        Icon(
                            Icons.Rounded.Waves, null,
                            tint = if (choice == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    title = "液态玻璃",
                    desc = if (liquidSupported) "全界面液态玻璃，更加新颖的观感"
                           else "$LIQUID_GLASS_REQUIRE_TEXT，当前设备不可选",
                    enabled = liquidSupported,
                    onClick = {
                        if (liquidSupported) choice = true
                        // 置灰仍可点：弹应用内提示（AppToast，非系统 Toast）
                        else AppToast.show("该风格$LIQUID_GLASS_REQUIRE_TEXT")
                    }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn(motionEnter(2)) +
                        slideInVertically(motionEnter(2)) { it / 8 }
            ) {
                StyleOptionCard(
                    selected = choice == false,
                    icon = {
                        Icon(
                            Icons.Rounded.Android, null,
                            tint = if (choice == false) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    title = "Material Design 3",
                    desc = "更加现代的界面设计",
                    onClick = { choice = false }
                )
            }

            // 主题色选择已从这里移除：色板统一放在「设置 → 主题色」里调
            Spacer(modifier = Modifier.height(28.dp))

                        val btnAlpha by animateFloatAsState(
                targetValue = if (ready) 1f else 0.45f,
                animationSpec = Motion.enter(),
                label = "GuideBtnAlpha"
            )
            val btnOffset by animateDpAsState(
                targetValue = if (ready) 0.dp else 6.dp,
                animationSpec = Motion.emphasized(),
                label = "GuideBtnOffset"
            )
            Button(
                onClick = {
                    val cont = onContinue
                    if (cont != null) cont(choice == true) else onSelect(choice == true)
                },
                enabled = ready,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                                        .padding(bottom = btnOffset.coerceAtLeast(0.dp))
                    .graphicsLayer { alpha = btnAlpha }
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    when {
                        choice == null -> "请先选择一种风格"
                        countdown > 0 -> "请稍候 " + countdown + "s"
                        else -> "继续"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // ── 导入歌单引导：裸图标 + 两行文案 + 主次分明的两个按钮，无卡片、无描边 ──
            AnimatedVisibility(
                // 风格一选定就出现（也就是那 2 秒倒计时期间），
                // 让用户在这段时间里顺手把「导入歌单」和「记住上次播放」决定掉。
                visible = choice != null,
                // 关键：必须带 expandVertically —— 否则容器高度是"瞬间"撑开的，
                // 页面整体垂直居中时就会被顶上去"跳一下"。
                // 三件事一起做：高度展开 + 内容自下而上抬起 + 淡入；阻尼比取 1（不过冲，避免"弹一下"）。
                enter = fadeIn(tween(240)) +
                        slideInVertically(
                            animationSpec = spring(dampingRatio = 1f, stiffness = 260f),
                            initialOffsetY = { it / 2 }
                        ) +
                        expandVertically(
                            animationSpec = spring(dampingRatio = 1f, stiffness = 260f),
                            expandFrom = Alignment.Top
                        ),
                exit = fadeOut(tween(150)) +
                        slideOutVertically(tween(150)) { it / 2 } +
                        shrinkVertically(
                            animationSpec = tween(150),
                            shrinkTowards = Alignment.Top
                        )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "导入你的歌单",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "一键搬入你音乐平台的歌单",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "需要先登录账号（用于保存你的歌单信息）",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            // 先完成与「继续」同样的风格收尾，再打开导入面板
                            onClick = { onSelect(choice == true); onImportNow() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("立即前往导入", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = {
                                val later = onLater
                                if (later != null) later(choice == true) else onSelect(choice == true)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                "稍后我再看",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // 「记住上次播放」开关：风格一选定就出现，外观跟随当前所选风格
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onRememberLastChange(!rememberLast) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "记住上次播放",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "下次打开应用继续上次的歌与进度",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = rememberLast,
                            onCheckedChange = onRememberLastChange,
                            // 滑块里带对号/叉号，和设置里的开关观感一致
                            thumbContent = {
                                Icon(
                                    if (rememberLast) Icons.Rounded.Check else Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    // 壁纸能力提示：液态玻璃风格支持自定义壁纸（主界面会绘制自定义背景），
                    // 仅 Material Design 3 暂不支持 —— 按当前选择给出准确提示
                    Text(
                        if (choice == false) "提示：Material Design 3 风格暂不支持自定义壁纸"
                        else "提示：当前风格支持自定义壁纸，可在「设置 → 主题与个性化」中更换",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleOptionCard(
    selected: Boolean,
    icon: @Composable () -> Unit,
    title: String,
    desc: String,
    onClick: () -> Unit,
    /** false = 该风格当前设备不可用（如液态玻璃需要 Android 13+）：内容置灰 + 右侧锁图标角标，仍可点击弹提示 */
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = Motion.press(),
        label = "StyleCardPress"
    )
    // 容器只用一层很淡的底，不带任何描边；选中时换成 primaryContainer
    val container by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = Motion.standard(),
        label = "StyleCardContainer"
    )
    val selectScale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = Motion.emphasized(),
        label = "StyleCardSelect"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale * selectScale
                scaleY = pressScale * selectScale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = if (enabled) 1f else 0.38f },
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(3.dp))
                Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
            }
        }
        if (!enabled) {
            // 角标：锁图标 + 「需要 Android 13+」小字（保持全不透明，保证可读）
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.Lock, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    LIQUID_GLASS_REQUIRE_TEXT,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
                AnimatedVisibility(
                visible = selected,
                enter = scaleIn(
                    animationSpec = Motion.bouncy(),
                    initialScale = 0.4f
                ) + fadeIn(Motion.enter()),
                exit = scaleOut(Motion.exit()) + fadeOut(Motion.exit())
            ) {
                Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun StyleNoticePage(onContinue: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn(tween(240)) +
                        slideInVertically(
                            initialOffsetY = { it / 8 },
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 260f)
                        ) +
                        scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 320f))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Tune,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        "设置随时可改",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "您稍后可以在设置更改导航栏与界面样式，我们提供多种样式可选",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            var buttonReady by remember { mutableStateOf(false) }
            LaunchedEffect(appeared) {
                if (appeared) {
                    delay(1000)
                    buttonReady = true
                }
            }
            val btnAlpha by animateFloatAsState(
                targetValue = if (appeared) 1f else 0f,
                animationSpec = tween(260, delayMillis = 120),
                label = "NoticeBtnAlpha"
            )
            Button(
                onClick = onContinue,
                enabled = buttonReady,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = btnAlpha }
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (buttonReady) "开始使用" else "请稍候…",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 启动引导新增一步：歌单导入提示。
 *
 * 用户点「去导入歌单」→ 进入歌单导入页（PlaylistImportPage / PlaylistImportSheet）；
 * 点「以后再说」→ 只记录一次，不再打扰。全部为矢量图标，无 emoji。
 */
@Composable
fun PlaylistImportGuidePage(
    onImport: () -> Unit,
    onSkip: () -> Unit,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn(tween(240)) +
                        slideInVertically(
                            initialOffsetY = { it / 8 },
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 260f)
                        ) +
                        scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 320f))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.LibraryMusic,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        "您可以将自己喜欢的歌单导入本应用",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "支持从网易云音乐、QQ 音乐、酷狗音乐导入歌单，导入后可继续在本应用播放与管理",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onImport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("去导入歌单", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    "以后再说",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================================
//  引导第 5 项：风格切换（液态玻璃 / MD3）+ 导入歌单引导
// ============================================================================

/**
 * 引导第 5 项与 App 主题的桥：
 * MainActivity 启动时注入 [apply]，把引导里选中的风格 / 主题色写进 ViewModel 并持久化。
 * 这样引导步骤（GuideScreen 在启动闪屏里）不需要持有 ViewModel。
 */
internal object GuideStyleBridge {
    var apply: (liquid: Boolean, keyColor: Int, finish: Boolean) -> Unit = { _, _, _ -> }
}

/**
 * 引导第 5 项：复用 [StyleGuidePage] 的两个风格卡片 + 导入歌单引导区。
 * 选中即时预览（写入 ViewModel 并持久化），三个按钮都会收尾：
 * 「继续」/「稍后我再看」→ 结束引导进主界面；「立即前往导入」→ 结束引导并请求打开导入面板。
 *
 * 主题色已不在这里选（页面精简为风格卡片 + 导入引导），
 * keyColor 仍然从偏好里读出并原样回写，避免覆盖用户在设置里选好的颜色。
 */
@Composable
internal fun GuideStyleStep(
    onFinish: () -> Unit,
    onImportNow: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var liquid by remember { mutableStateOf(prefs.getBoolean("useFloatingNavBar", true)) }
    var keyColor by remember { mutableIntStateOf(prefs.getInt("key_color", 0)) }

    // 「记住上次播放」：这里直接读写保存开关的那份偏好（和设置里的开关是同一个 key），
    // 所以引导页勾选后，应用启动时的恢复逻辑能读到。
    val resumePrefs = remember { context.getSharedPreferences("playback_resume", Context.MODE_PRIVATE) }
    var rememberLast by remember { mutableStateOf(resumePrefs.getBoolean("enabled", false)) }

    fun commit(style: Boolean, color: Int, finish: Boolean) {
        liquid = style
        keyColor = color
        GuideStyleBridge.apply(style, color, finish)
    }

    // 关键：让整页内容按「当前选中的风格」渲染。
    // 之前页面内部不读 LocalMd3Style，用的是外层环境的风格，
    // 所以选了另一个风格后，下面的导入区/开关都不跟着变。
    CompositionLocalProvider(LocalMd3Style provides !liquid) {
        StyleGuidePage(
            currentLiquid = liquid,
            currentKeyColor = keyColor,
            onSelect = { l -> commit(l, keyColor, finish = false) },
            onKeyColor = { cc -> commit(liquid, cc, finish = false) },
            onContinue = { l -> commit(l, keyColor, finish = true); onFinish() },
            onLater = { l -> commit(l, keyColor, finish = true); onFinish() },
            onImportNow = { commit(liquid, keyColor, finish = true); onImportNow() },
            rememberLast = rememberLast,
            onRememberLastChange = { on ->
                rememberLast = on
                resumePrefs.edit().putBoolean("enabled", on).apply()
            },
        )
    }
}
