package com.HyperX.MI.Na

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

/**
 * 覆盖层触摸拦截（引导页 / 风格页根容器专用）。
 *
 * 覆盖层根容器加上指针输入节点后，整块覆盖层成为命中目标，
 * 下层兄弟节点（底部导航栏等）在覆盖期间收不到触摸；
 * 同时只在 [PointerEventPass.Main] 消费「没有任何子节点消费过」的事件：
 * Main 的传递顺序是「叶子 → 根」，卡片 / 色点 / 按钮等子控件先拿到事件，
 * 页面自己的交互不会被抢；只有空白区域的触摸会被吞掉，不再透传到下层 Tab。
 */
internal fun Modifier.blockTouchesBehindOverlay(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            if (event.changes.none { it.isConsumed }) {
                event.changes.forEach { it.consume() }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("guide_prefs", Context.MODE_PRIVATE) }
    var guideDone by remember { mutableStateOf(prefs.getBoolean("guide_done", false)) }
    var splashDone by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    LaunchedEffect(splashDone, guideDone) {
        if (splashDone && guideDone) {
            onFinished()
        }
        if (splashDone && !guideDone) {
            showGuide = true
        }
    }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = "MINova",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(36.dp))
            LaunchedEffect(Unit) {
                delay(2000)
                splashDone = true
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .width(180.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Black.copy(alpha = 0.10f),
                strokeCap = StrokeCap.Round
            )
        }

        AnimatedVisibility(
            visible = showGuide,
            enter = fadeIn(tween(700, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(500, easing = FastOutSlowInEasing))
        ) {
            GuideScreen(
                onComplete = {
                    prefs.edit().putBoolean("guide_done", true).apply()
                    guideDone = true
                    showGuide = false
                }
            )
        }
    }
}
@Composable
private fun GuideScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var permissionDenied by remember { mutableStateOf(false) }
    // 4 步原有引导 + 第 5 步「风格切换（液态玻璃 / MD3）+ 导入歌单引导」
    val totalSteps = 5

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            step = 2
        } else {
            permissionDenied = true
        }
    }

    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            permissionDenied = false
            step = 3
        } else {
            permissionDenied = true
        }
    }

    val progress by animateFloatAsState(
        targetValue = (step + 1).toFloat() / totalSteps,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "GuideProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // 引导覆盖层：整屏拦截触摸，防止透传到下层底部导航（Tab 误切）
            .blockTouchesBehindOverlay()
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (fadeIn(tween(400, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(400, easing = FastOutSlowInEasing)) { if (forward) it / 8 else -it / 8 }) togetherWith
                        (fadeOut(tween(300, easing = FastOutSlowInEasing)) +
                                slideOutVertically(tween(300, easing = FastOutSlowInEasing)) { if (forward) -it / 8 else it / 8 })
            },
            label = "GuideStep",
            modifier = Modifier.weight(1f)
        ) { currentStep ->
            when (currentStep) {
                0 -> IntroScreen(onNext = { permissionDenied = false; step = 1 })
                1 -> NotificationPermissionScreen(
                    denied = permissionDenied,
                    onNext = {
                        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else true

                        if (granted) {
                            permissionDenied = false
                            step = 2
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                permissionDenied = false
                                step = 2
                            }
                        }
                    },
                    onSkip = { permissionDenied = false; step = 2 }
                )
                2 -> StoragePermissionScreen(
                    denied = permissionDenied,
                    onNext = {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
                        } else {
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        }
                        val allGranted = permissions.all {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }
                        if (allGranted) {
                            permissionDenied = false
                            step = 3
                        } else {
                            storageLauncher.launch(permissions)
                        }
                    },
                    onSkip = { permissionDenied = false; step = 3 }
                )
                3 -> AgreementScreen(onNext = { permissionDenied = false; step = 4 })
                // 第 5 项：风格切换 + 歌单导入入口（完成后才结束引导）
                4 -> GuideStyleStep(
                    onFinish = { onComplete() },
                    onImportNow = {
                        // 先请求打开导入面板，再结束引导（主界面此时才真正可见）
                        ImportBus.request()
                        onComplete()
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "${step + 1}/$totalSteps",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IntroScreen(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.PrivacyTip,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(76.dp)
        )
        Spacer(Modifier.height(28.dp))
        Text(
            "使用前请阅读",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(20.dp)
        ) {
            Text(
                "本应用性能开销比较大，最低支持骁龙 888 和安卓 13（液态玻璃效果必须），因此低于要求的设备无法使用（包括平板设备，不保证一定能使用）。我只是一个小开发者，能力并不大，请见谅。",
                fontSize = 14.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("下一步", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun NotificationPermissionScreen(
    denied: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Notifications,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(76.dp)
        )
        Spacer(Modifier.height(28.dp))
        Text(
            "通知权限",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "通知权限仅用于媒体播放控制，在锁屏或通知栏显示当前播放的音乐。",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        if (denied) {
            Spacer(Modifier.height(16.dp))
            Text(
                "权限被拒绝，请重新授予",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(if (denied) "重新申请" else "授予通知权限", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "暂不授予，跳过",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onSkip() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun StoragePermissionScreen(
    denied: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Storage,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(76.dp)
        )
        Spacer(Modifier.height(28.dp))
        Text(
            "存储权限",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "存储权限用于保存下载的音乐文件，以及读取本地音乐。",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        if (denied) {
            Spacer(Modifier.height(16.dp))
            Text(
                "权限被拒绝，请重新授予",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(if (denied) "重新申请" else "授予存储权限", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "暂不授予，跳过",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onSkip() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AgreementScreen(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Check,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(76.dp)
        )
        Spacer(Modifier.height(28.dp))
        Text(
            "用户协议",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(20.dp)
        ) {
            Text(
                "一、基础权益\n" +
                        "1. 您在使用本应用时，享有搜索、播放、下载、收藏音乐等基础功能。\n" +
                        "2. 本应用为个人学习作品，仅供学习交流使用，禁止用于商业用途。\n" +
                        "3. 您有权随时停止使用本应用并清除本地数据。\n\n" +
                        "二、使用规范\n" +
                        "1. 本应用不存储任何音乐资源，所有内容均来自第三方公开接口，版权归原作者所有。\n" +
                        "2. 请勿利用本应用从事任何违反国家法律法规的活动。\n" +
                        "3. 请勿恶意刷取接口、批量下载或对本应用进行逆向工程。\n" +
                        "4. 本应用为开源项目，遵循原开源许可证，允许学习、修改和分发，但禁止用于商业用途，且修改后不得冒充原作者。\n" +
                        "5. 因使用本应用产生的任何版权纠纷，由用户自行承担。\n\n" +
                        "三、隐私保护\n" +
                        "1. 我们会收集必要的设备信息用于改进服务。\n" +
                        "2. 我们不会主动收集您的个人隐私信息。\n" +
                        "3. 您的账号信息仅用于登录验证，不会泄露给第三方。\n\n" +
                        "四、法律效应\n" +
                        "1. 本协议自您点击「同意并继续」之时生效。\n" +
                        "2. 若您不同意本协议的任何条款，请立即停止使用本应用。\n" +
                        "3. 本协议的解释权归本应用作者所有。\n" +
                        "4. 本协议适用中华人民共和国法律。\n\n" +
                        "五、免责声明\n" +
                        "1. 本应用按「现状」提供，不提供任何形式的明示或暗示担保。\n" +
                        "2. 因使用本应用造成的任何直接或间接损失，作者不承担责任。\n" +
                        "3. 作者保留随时修改或终止本应用的权利。\n\n" +
                        "感谢您的使用与支持！",
                fontSize = 14.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("同意并继续", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}