package com.HyperX.MI.Na

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically

import android.app.Activity
import android.content.Intent
import android.net.Uri

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.RoundedRectangle
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

/** 本项目的开源仓库地址。 */
private const val PROJECT_GITHUB_URL = "https://github.com/Mintllo/MINova"

/**
 * 用系统浏览器打开开源仓库。
 *
 * 加 FLAG_ACTIVITY_NEW_TASK 是必须的：这里拿到的 Context 不一定是 Activity，
 * 否则会在部分机型上抛 AndroidRuntimeException；用 runCatching 保证没有浏览器时也不会崩。
 */
private fun openProjectLink(ctx: android.content.Context) {
    runCatching {
        ctx.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(PROJECT_GITHUB_URL)
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
fun ProfileTab(
    viewModel: PlayerViewModel,
    backdrop: Backdrop,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {}
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var showLogin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (!isLoggedIn) { delay(500); viewModel.checkLoginReminder() } }
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            showLogin = false
            // 之前点过「立即前往导入」但被要求登录：现在登录好了，自动接着打开导入面板
            if (ImportBus.afterLogin) {
                ImportBus.afterLogin = false
                ImportBus.request()
            }
        }
    }

    AnimatedContent(
        targetState = showLogin,
        transitionSpec = {
            fadeIn(Motion.enter()) + scaleIn(initialScale = 0.92f, animationSpec = Motion.enter()) togetherWith
                    fadeOut(Motion.exit()) + scaleOut(targetScale = 0.92f, animationSpec = Motion.exit())
        },
        label = "LoginTransition"
    ) { isShowingLogin ->
        if (isShowingLogin) {
            // 全屏沉浸式登录 / 注册页自带顶部返回按钮与标题，这里不再叠一层 chrome
            LoginScreen(
                viewModel = viewModel,
                backdrop = backdrop,
                onBack = { showLogin = false }
            )
        } else {
            if (isLoggedIn) {
                LoggedInContent(viewModel, currentUser, backdrop, onNavigateToHistory, onNavigateToPlaylists)
            } else {
                UnloggedProfilePage(viewModel, backdrop, onLoginClick = { showLogin = true }, onNavigateToPlaylists = onNavigateToPlaylists)
            }
        }
    }
}

@Composable
fun UnloggedProfilePage(
    viewModel: PlayerViewModel,
    backdrop: Backdrop,
    onLoginClick: () -> Unit,
    onNavigateToPlaylists: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = if (isLandscape) 80.dp else 20.dp
    val maxWidth = if (isLandscape) 600.dp else Dp.Unspecified
    var activePage by remember { mutableStateOf<String?>(null) }
        val isMd3Style = LocalMd3Style.current

    AnimatedContent(
        targetState = activePage,
        transitionSpec = {
            fadeIn(Motion.enter()) + scaleIn(initialScale = 0.95f, animationSpec = Motion.enter()) togetherWith
                    fadeOut(Motion.exit()) + scaleOut(targetScale = 0.95f, animationSpec = Motion.exit())
        },
        label = "UnloggedPage"
    ) { page ->
        when (page) {
            "playlists" -> PlaylistScreen(viewModel = viewModel, onBack = { activePage = null }, backdrop = backdrop)
            "changelog" -> ChangelogScreen(backdrop = backdrop, onBack = { activePage = null })
            "about" -> AboutScreen(backdrop = backdrop, onBack = { activePage = null })
            "storage_manage" -> StorageManageScreen(viewModel = viewModel, backdrop = backdrop, onBack = { activePage = null })
            "feedback" -> FeedbackScreen(viewModel = viewModel, backdrop = backdrop, onBack = { activePage = null })
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = horizontalPadding, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight().widthIn(max = maxWidth),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("欢迎使用MINova", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(16.dp))
                                SearchLiquidButton(
                                    onClick = onLoginClick,
                                    backdrop = backdrop,
                                    shape = Capsule(),
                                    label = "立即注册 / 登录"
                                )
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            val ctx = LocalContext.current
                            ProfileGlassCard(backdrop = backdrop, onClick = { openProjectLink(ctx) }) {
                                Icon(Icons.Rounded.Code, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("前往Github开源地址", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Rounded.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (isMd3Style) {
                                                        item {
                                SegmentedColumn(title = "更多操作") {
                                    SegmentedNavItem(index = 0, count = 7, icon = Icons.Rounded.Favorite, title = "我的收藏", summary = null, onClick = { onLoginClick() }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                                    SegmentedNavItem(index = 1, count = 7, icon = Icons.Rounded.History, title = "历史播放", summary = null, onClick = { onLoginClick() }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                                    SegmentedNavItem(index = 2, count = 7, icon = Icons.Rounded.QueueMusic, title = "歌单", summary = null, onClick = { activePage = "playlists" }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                                    SegmentedNavItem(index = 3, count = 7, icon = Icons.Rounded.EventNote, title = "更新日志", summary = null, onClick = { activePage = "changelog" }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                                    SegmentedNavItem(index = 4, count = 7, icon = Icons.Rounded.Info, title = "关于本应用", summary = null, onClick = { activePage = "about" }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                                    SegmentedNavItem(index = 5, count = 7, icon = Icons.Rounded.Storage, title = "管理本地存储", summary = null, onClick = { activePage = "storage_manage" }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                                    SegmentedNavItem(index = 6, count = 7, icon = Icons.Rounded.Feedback, title = "意见反馈", summary = null, onClick = { activePage = "feedback" }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                                }
                            }
                        } else {
                            item {
                                Text(
                                    "更多操作",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                                )
                            }
                            item { ProfileMenuItemCard(Icons.Rounded.Favorite, "我的收藏", backdrop) { onLoginClick() } }
                            item { ProfileMenuItemCard(Icons.Rounded.History, "历史播放", backdrop) { onLoginClick() } }
                            item { ProfileMenuItemCard(Icons.Rounded.QueueMusic, "歌单", backdrop) { activePage = "playlists" } }
                                                        item { ProfileMenuItemCard(Icons.Rounded.EventNote, "更新日志", backdrop) { activePage = "changelog" } }
                            item { ProfileMenuItemCard(Icons.Rounded.Info, "关于本应用", backdrop) { activePage = "about" } }
                            item { ProfileMenuItemCard(Icons.Rounded.Storage, "管理本地存储", backdrop) { activePage = "storage_manage" } }
                            item { ProfileMenuItemCard(Icons.Rounded.Feedback, "意见反馈", backdrop) { activePage = "feedback" } }
                        }
                        item { Spacer(Modifier.height(30.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun LoggedInContent(
    viewModel: PlayerViewModel,
    currentUser: PlayerViewModel.UserInfo?,
    backdrop: Backdrop,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {}
) {
    var activePage by remember { mutableStateOf<String?>(null) }
    AnimatedContent(
        targetState = activePage,
        transitionSpec = {
            fadeIn(Motion.enter()) + scaleIn(initialScale = 0.95f, animationSpec = Motion.enter()) togetherWith
                    fadeOut(Motion.exit()) + scaleOut(targetScale = 0.95f, animationSpec = Motion.exit())
        },
        label = "ProfilePage"
    ) { page ->
        when (page) {
            "usage_limit" -> UsageLimitScreen(viewModel, onBack = { activePage = null })
            "favorites" -> FavoritesScreen(viewModel, backdrop, onBack = { activePage = null })
            "account_manage" -> AccountManageScreen(viewModel, currentUser, onBack = { activePage = null })
            "playlists" -> PlaylistScreen(viewModel = viewModel, onBack = { activePage = null }, backdrop = backdrop)
            "changelog" -> ChangelogScreen(backdrop = backdrop, onBack = { activePage = null })
            "about" -> AboutScreen(backdrop = backdrop, onBack = { activePage = null })
            "storage_manage" -> StorageManageScreen(viewModel, backdrop, onBack = { activePage = null })
            "feedback" -> FeedbackScreen(viewModel, backdrop, onBack = { activePage = null })
            else -> MainProfilePage(viewModel, currentUser, backdrop, onNavigate = { page ->
                if (page == "history") onNavigateToHistory()
                else activePage = page
            }, onNavigateToPlaylists = onNavigateToPlaylists)
        }
    }
}

@Composable
fun MainProfilePage(
    viewModel: PlayerViewModel,
    currentUser: PlayerViewModel.UserInfo?,
    backdrop: Backdrop,
    onNavigate: (String) -> Unit,
    onNavigateToPlaylists: () -> Unit = {}
) {
    val context = LocalContext.current
    val avatarUri = viewModel.avatarUri
    val avatarUrl = currentUser?.avatar
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.updateAvatar(it)
            viewModel.uploadAvatar(context, it)
        }
    }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = if (isLandscape) 80.dp else 20.dp
    val maxWidth = if (isLandscape) 600.dp else Dp.Unspecified
        val isMd3Style = LocalMd3Style.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = horizontalPadding, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxHeight().widthIn(max = maxWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .clickable {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            avatarUri != null -> AsyncImage(avatarUri, "头像", Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            !avatarUrl.isNullOrEmpty() -> AsyncImage(avatarModelUrl(avatarUrl!!), "头像", Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            else -> Text((currentUser?.nickname ?: "?").take(1), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("${currentUser?.nickname ?: ""}，您好！", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = { onNavigate("account_manage") },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Rounded.ManageAccounts, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("管理账号", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
            item {
                val ctx = LocalContext.current
                ProfileGlassCard(backdrop = backdrop, onClick = { openProjectLink(ctx) }) {
                    Icon(Icons.Rounded.Code, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("前往Github开源地址", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            if (isMd3Style) {
                                item {
                    SegmentedColumn(title = "更多操作") {
                        SegmentedNavItem(index = 0, count = 7, icon = Icons.Rounded.Favorite, title = "我的收藏", summary = null, onClick = { onNavigate("favorites") }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                        SegmentedNavItem(index = 1, count = 7, icon = Icons.Rounded.History, title = "历史播放", summary = null, onClick = { onNavigate("history") }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                        SegmentedNavItem(index = 2, count = 7, icon = Icons.Rounded.QueueMusic, title = "歌单", summary = null, onClick = { onNavigate("playlists") }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                        SegmentedNavItem(index = 3, count = 7, icon = Icons.Rounded.EventNote, title = "更新日志", summary = null, onClick = { onNavigate("changelog") }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                        SegmentedNavItem(index = 4, count = 7, icon = Icons.Rounded.Info, title = "关于本应用", summary = null, onClick = { onNavigate("about") }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                        SegmentedNavItem(index = 5, count = 7, icon = Icons.Rounded.Storage, title = "管理本地存储", summary = null, onClick = { onNavigate("storage_manage") }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                        SegmentedNavItem(index = 6, count = 7, icon = Icons.Rounded.Feedback, title = "意见反馈", summary = null, onClick = { onNavigate("feedback") }, trailing = { Icon(Icons.Rounded.ChevronRight, null) })
                    }
                }
            } else {
                item {
                    Text(
                        "更多操作",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                    )
                }
                item { ProfileMenuItemCard(Icons.Rounded.Favorite, "我的收藏", backdrop) { onNavigate("favorites") } }
                item { ProfileMenuItemCard(Icons.Rounded.History, "历史播放", backdrop) { onNavigate("history") } }
                item { ProfileMenuItemCard(Icons.Rounded.QueueMusic, "歌单", backdrop) { onNavigate("playlists") } }
                                item { ProfileMenuItemCard(Icons.Rounded.EventNote, "更新日志", backdrop) { onNavigate("changelog") } }
                item { ProfileMenuItemCard(Icons.Rounded.Info, "关于本应用", backdrop) { onNavigate("about") } }
                item { ProfileMenuItemCard(Icons.Rounded.Storage, "管理本地存储", backdrop) { onNavigate("storage_manage") } }
                item { ProfileMenuItemCard(Icons.Rounded.Feedback, "意见反馈", backdrop) { onNavigate("feedback") } }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

@Composable
fun ProfileGlassCard(
    backdrop: Backdrop,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "ProfileCardPress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .then(
                if (LocalMd3Style.current)
                    Modifier.background(MaterialTheme.colorScheme.surfaceBright, RoundedRectangle(24f.dp))
                else Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(24f.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(16f.dp.toPx(), 32f.dp.toPx())
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = 0.5f)
                    }
                )
            )
            .clip(RoundedRectangle(24f.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManageScreen(
    viewModel: PlayerViewModel,
    currentUser: PlayerViewModel.UserInfo?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val avatarUri = viewModel.avatarUri
    val avatarUrl = currentUser?.avatar
    var editingNickname by remember { mutableStateOf(false) }
    var newNickname by remember { mutableStateOf(currentUser?.nickname ?: "") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(editingNickname) {
        if (editingNickname) {
            delay(100)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.updateAvatar(it)
            viewModel.uploadAvatar(context, it)
        }
    }
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(8.dp))
            Text("管理账号", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(viewModel.cornerRadius.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp).copy(alpha = 0.55f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .clickable { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                avatarUri != null -> AsyncImage(avatarUri, "头像", Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                !avatarUrl.isNullOrEmpty() -> AsyncImage(avatarModelUrl(avatarUrl!!), "头像", Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                else -> Icon(Icons.Rounded.CameraAlt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("点击更换头像", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(20.dp))
                        AnimatedContent(
                            targetState = editingNickname,
                            transitionSpec = {
                                (fadeIn(Motion.enter()) + scaleIn(initialScale = 0.9f, animationSpec = Motion.enter())) togetherWith
                                        (fadeOut(Motion.exit()) + scaleOut(targetScale = 0.9f, animationSpec = Motion.exit()))
                            },
                            label = "nicknameEdit"
                        ) { isEditing ->
                            if (isEditing) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    OutlinedTextField(
                                        value = newNickname, onValueChange = { newNickname = it }, singleLine = true,
                                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester), shape = RoundedCornerShape(14.dp),
                                        label = { Text("新昵称") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            if (newNickname.isNotBlank()) { viewModel.updateNickname(newNickname); editingNickname = false; focusManager.clearFocus() }
                                        })
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        FilledTonalButton(onClick = {
                                            if (newNickname.isNotBlank()) { viewModel.updateNickname(newNickname); editingNickname = false; focusManager.clearFocus() }
                                        }, shape = RoundedCornerShape(12.dp)) {
                                            Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("保存", fontWeight = FontWeight.Medium)
                                        }
                                        OutlinedButton(onClick = { newNickname = currentUser?.nickname ?: ""; editingNickname = false; focusManager.clearFocus() }, shape = RoundedCornerShape(12.dp)) {
                                            Text("取消", fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(currentUser?.nickname ?: "", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.height(2.dp))
                                    Text("@${currentUser?.username ?: ""}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(8.dp))
                                    FilledTonalButton(onClick = { newNickname = currentUser?.nickname ?: ""; editingNickname = true }, shape = RoundedCornerShape(12.dp)) {
                                        Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("修改昵称", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(viewModel.cornerRadius.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp).copy(alpha = 0.55f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("账号信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("用户ID", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.width(80.dp))
                            Text("${currentUser?.id ?: ""}", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("用户名", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.width(80.dp))
                            Text(currentUser?.username ?: "", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            item {
                Button(onClick = { viewModel.logout(); onBack() }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Rounded.Logout, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("退出登录", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

@Composable
fun FavoritesScreen(
    viewModel: PlayerViewModel,
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    val favorites by viewModel.favorites.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    LaunchedEffect(Unit) { if (isLoggedIn) viewModel.loadFavorites() }

    val filteredFavorites = remember(favorites, searchText) {
        if (searchText.isBlank()) favorites
        else favorites.filter { song ->
            song.name.contains(searchText, ignoreCase = true) || song.artist.contains(searchText, ignoreCase = true) || song.album.contains(searchText, ignoreCase = true)
        }
    }

    val collapseBehavior = androidx.compose.material3.TopAppBarDefaults
        .exitUntilCollapsedScrollBehavior(androidx.compose.material3.rememberTopAppBarState())
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        androidx.compose.material3.LargeTopAppBar(
            title = { Text("我的收藏", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = MaterialTheme.colorScheme.primary) }
            },
            scrollBehavior = collapseBehavior,
            colors = androidx.compose.material3.TopAppBarDefaults.largeTopAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )

        if (!isLoggedIn) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Login, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("请先登录后使用", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }
        } else {
            // 搜索框跟着「有没有收藏」出现/消失：必须带 expand/shrink，
            // 否则删掉最后一首歌时它会"啪"地瞬间消失，观感很突兀。
            AnimatedVisibility(
                visible = favorites.isNotEmpty(),
                enter = fadeIn(Motion.enter()) +
                        expandVertically(Motion.enter(), expandFrom = Alignment.Top),
                exit = fadeOut(Motion.exit()) +
                        shrinkVertically(Motion.exit(), shrinkTowards = Alignment.Top)
            ) {
                GenericSearchBar(searchText = searchText, isSearchActive = isSearchActive, onSearchTextChange = { searchText = it }, onSearchToggle = { isSearchActive = it }, placeholder = "搜索收藏的歌曲...")
            }

            if (filteredFavorites.isEmpty() && favorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (searchText.isNotBlank()) Icons.Rounded.SearchOff else Icons.Rounded.FavoriteBorder, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        Spacer(Modifier.height(16.dp))
                        Text(if (searchText.isNotBlank()) "没有找到匹配的歌曲" else "暂无收藏歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 14.sp)
                        if (searchText.isBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("长按歌曲可添加到收藏", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), fontSize = 12.sp)
                        }
                    }
                }
            } else if (filteredFavorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("没有找到匹配的歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().nestedScroll(collapseBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(
                        items = filteredFavorites,
                        key = { index, s -> "fav_${s.id}_$index" }
                    ) { index, song ->
                        var isRemoving by remember { mutableStateOf(false) }
                        AnimatedVisibility(
                            visible = !isRemoving,
                            enter = fadeIn(Motion.enter()) + scaleIn(initialScale = 0.95f, animationSpec = Motion.enter()),
                            exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit()) + scaleOut(targetScale = 0.9f, animationSpec = Motion.exit())
                        ) {
                            SongItem(
                                song = song,
                                isCurrentSong = song.id == currentSong?.id,
                                isPlaying = isPlaying,
                                isFirst = index == 0,
                                isLast = index == filteredFavorites.size - 1,
                                onClick = { viewModel.playSong(song, filteredFavorites) },
                                viewModel = viewModel,
                                backdrop = backdrop,
                                isInFavorites = true,
                                onRemoveFromFavorites = { isRemoving = true }
                            )
                        }
                        LaunchedEffect(isRemoving) {
                            if (isRemoving) {
                                delay(300)
                                viewModel.removeFromFavorites(song.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageLimitScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val remainingQuota by viewModel.remainingQuota.collectAsState()
    val nextResetTime by viewModel.nextResetTime.collectAsState()
    val totalQuota = 50
    val progress = if (remainingQuota > 0) remainingQuota / totalQuota.toFloat() else 0f
    val isLowQuota = remainingQuota <= 10
    val indicatorColor = if (isLowQuota) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val trackColor = if (isLowQuota) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val activity = LocalContext.current as Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = if (isLandscape) 80.dp else 20.dp
    val maxWidth = if (isLandscape) 600.dp else Dp.Unspecified

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("API限额", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (!isLoggedIn) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Login, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("请先登录后使用", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = horizontalPadding), contentAlignment = Alignment.TopCenter) {
                LazyColumn(modifier = Modifier.fillMaxSize().widthIn(max = maxWidth), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp).copy(alpha = 0.55f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                            Column(Modifier.padding(vertical = 28.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("剩余可用额度", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                                    Text("$remainingQuota", style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp), color = indicatorColor)
                                    Text(" / $totalQuota", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp), fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(24.dp))
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape), color = indicatorColor, trackColor = trackColor)
                                Spacer(Modifier.height(16.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.DataUsage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("已用 ${totalQuota - remainingQuota} 次", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Schedule, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("${formatResetTime(nextResetTime)} 重置", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp).copy(alpha = 0.55f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                            Column(Modifier.padding(20.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("使用规则说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(Modifier.height(16.dp))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("每次解析或播放在线VIP歌曲将消耗 1 额度", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Rounded.Loop, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("额度将在首次使用后12小时自动循环重置为 50 次", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                                }
                            }
                        }
                    }
                    item {
                        Button(onClick = { viewModel.watchAd(activity) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
                            Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("观看视频广告获取更多额度", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    item { Text(text = "当前维护压力巨大，额度方案实属无奈，望多加理解", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("更新日志", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
        var showOldLogs by remember { mutableStateOf(false) }
            // v1.3.1 的条目统一来自 MainActivity 的 changelogEntries()（设置页入口已下线，
            // 更新日志入口只在「账号」页；内容与数据源保持同一份，避免"入口有、条目旧"）。
                        changelogEntries().forEachIndexed { idx, entry ->
                ChangelogItem(
                    version = "v" + entry.version + if (idx == 0) " 正式版" else "",
                    date = mapOf("1.3.4" to "2026年11月12日 01:27", "1.3.3" to "2026年11月11日 19:41", "1.3.2" to "2026年11月8日 21:30", "1.3.1" to "2026年11月8日 18:00")[entry.version] ?: "",
                    changes = entry.items,
                    isLatest = idx == 0,
                    backdrop = backdrop
                )
            }
            ChangelogItem(
                version = "v1.3.0 正式版", date = "2026年11月3日",
                changes = listOf(
                    "##播放",
                    "修复 VIP 歌曲只能播放 30 秒：时长单位毫秒/秒不一致，同时补上多音源自动换源",
                    "修复歌词与歌曲错配：歌词改为按同源平台 id 获取并做归属校验，拼音、外文误匹配直接拦掉",
                    "修复部分歌曲封面加载不出来：封面统一走后端防盗链代理",
                    "##悬浮歌词",
                    "悬浮歌词重构：19 项可调，纯色无阴影圆角卡片，文字颜色改完立即生效，底色按对比度自动适配，行切换动画顺滑",
                    "##每日 30 首与推荐",
                    "每日 30 首同一天内固定、隔天刷新，数量稳定 30 首，详情页显示时长与播放律动条",
                    "推荐算法改为加权打分（听歌次数 / 搜索热度 / 收藏 / 口味画像）并加同歌手限流，选完口味立即生效",
                    "音乐偏好选择器移入设置，两种风格都已适配",
                    "##引导",
                    "启动引导新增第 5 步：风格选择 + 导入歌单引导，进度 5/5",
                    "修复引导层触摸透传，引导时点底部导航不再误切页面",
                    "##界面",
                    "应用图标修正：自适应图标按安全区留边，不再放大截断",
                    "新增默认壁纸 / 横幅；登录注册按钮支持液态玻璃效果",
                    "全局动效统一到 Motion 规范，清除所有渐变，全组件改为动态取色",
                    "移除「未登录」提醒弹窗，解除必须登录才能使用的限制提示"
                ),
                isLatest = false,
                backdrop = backdrop
            )
            ChangelogItem(
                version = "v1.2.0 正式版", date = "2026年10月27日",
                changes = listOf(
                    "##全新双风格体系",
                    "新增「MD3 Expressive」与「液态玻璃」两套完整视觉风格，覆盖首页、播放器、设置、列表、弹层、导航栏全部页面",
                    "液态玻璃全链路接入真实背景模糊、高光透镜与动态取色，玻璃卡片可采样壁纸",
                    "启动引导新增风格选择页，含 2 秒确认倒计时；MD3 说明文案明确「不含液态效果，仅液态玻璃导航栏」",
                    "玻璃模式下关闭壁纸不再回退黑屏，正确回落主题底色",
                    "##底部导航栏",
                    "选中项图标与文字跟随应用动态主题色实时变化",
                    "修复导航栏点击无响应、切换标签不跳转页面",
                    "液态导航栏新增独立强调色通道，不再使用固定配色",
                    "导航顺序调整：推荐移至首屏，原「音乐」更名为「搜索」",
                    "新增左右滑动切换页面",
                    "修复跨页切换标签时滑块卡在中间页、界面无响应",
                    "##播放",
                    "修复全平台歌曲无法播放、点击报错",
                    "修复点歌延迟（原每次播放需空等 15 秒超时）",
                    "修复切歌时误报「StandaloneCoroutine was cancelled」",
                    "播放启动不再被历史记录网络请求阻塞，点击即时响应",
                    "播放地址改为自建免费多源解析（网易云 / QQ / 酷狗 / B站），彻底移除第三方付费额度依赖",
                    "##搜索",
                    "搜索切换至自建免费多源聚合，跨平台合并去重",
                    "新增 10 分钟结果缓存，避免重复请求触发限流",
                    "##推荐",
                    "推荐页全新版式：双大卡（猜你喜欢 / 每日 30 首）+ 分区歌曲列表，均带真实封面",
                    "点大卡进入对应歌曲列表，点播放键直接播放",
                    "歌曲列表改为每排 3 个的网格：左侧小封面、右侧歌曲信息，纵向滚动",
                    "新增本地播放次数统计并展示",
                    "修复长分区标题折行、末尾图标被挤到单独一行",
                    "推荐算法基于历史与收藏生成种子，无数据时回落热门默认推荐",
                    "支持一键换一批，分区可单独隐藏",
                    "##意见反馈",
                    "修复提交必然失败的问题",
                    "提交按钮不再被导航栏遮挡，且始终可点击（内容不足时给出提示）",
                    "失败时展示服务端真实原因",
                    "删除设置页重复的「意见反馈」入口，保留账号页入口",
                    "##公告与更新",
                    "修复证书固定（pin-set）失效导致登录 / 公告 / 更新 / 收藏 / 历史全部 TLS 握手失败的问题",
                    "修复后台发布公告后客户端不显示",
                    "修复后台发布新版本后客户端不提示",
                    "公告与更新弹层互斥，不再重叠",
                    "弹层统一为 Material 3 底部抽屉，新增无背景的公告 / 更新图标",
                    "底部抽屉拖拽条改为圆角样式",
                    "##界面与交互",
                    "全局点击水波纹统一为圆角，消除直角反馈",
                    "历史播放、我的收藏改用折叠大标题栏，滚动自动收起",
                    "修复设置页文案，进度条样式统一为「进度条样式」",
                    "默认启用蝌蚪波浪进度条",
                    "##悬浮歌词",
                    "移除文字阴影与描边，改为纯色文字",
                    "卡片改为不透明白底 + 8dp 圆角",
                    "移除卡片上的上一首／暂停／下一首按钮及「测试版」徽标",
                    "修复文字颜色调整无效（选色后自动关闭「当前行跟随动态色」）",
                    "##安装包与图标",
                    "安装包体积由 142 MB 降至 51 MB：移除未使用的视频编解码器、精简 CPU 架构、开启代码与资源压缩",
                    "应用图标更换为 Minova 标识",
                    "冷启动优化（正式版实测约 0.9 秒）"
                ),
                backdrop = backdrop
            )

            // 以往的测试版更新（默认折叠，点击展开）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showOldLogs = !showOldLogs }
                    .padding(vertical = 14.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "以往的测试版更新",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (showOldLogs) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = "展开或收起",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = showOldLogs) {
                Column {
            ChangelogItem(
                version = "1.1.1 beta", date = "2026年9月16日",
                changes = listOf(
                    "全面引入液态玻璃效果，覆盖导航栏、控制台、设置卡片、个人信息卡片等....",
                    "音乐列表新增歌曲封面显示",
                    "新增自适应圆角（G2 圆角）",
                    "全新升级全屏歌曲页面",
                    "修复若干bug",
                    "暂时移除环绕声"
                ),
                backdrop = backdrop
            )
            ChangelogItem(
                version = "1.1.0 beta", date = "2026年7月29日",
                changes = listOf(
                    "新增陀螺仪控制音乐控制台",
                    "支持音量键控制（无陀螺仪设备）",
                    "优化液态玻璃导航栏效果",
                    "新增音乐下载功能",
                    "新增本地存储管理页面",
                    "修复搜索列表滑动崩溃问题",
                    "优化歌词滚动动画",
                    "进度条支持拖动控制播放进度",
                    "修复底部控制台与导航栏间距问题",
                    "优化引导弹窗动画与交互",
                    "新增首次使用引导提示",
                ),
                backdrop = backdrop
            )
            ChangelogItem(
                version = "1.0.9 beta", date = "2026年7月27日",
                changes = listOf(
                    "新增社区页面（WebView浏览）",
                    "新增下载音乐到本地功能",
                    "新增本地存储管理页面",
                    "删除导航栏多余设置项",
                    "简化导航栏设置，保留核心开关",
                    "优化未登录状态下账号页面显示",
                    "优化液态导航栏显示效果",
                    "修复搜索列表滑动崩溃问题",
                ),
                backdrop = backdrop
            )
            ChangelogItem(
                version = "1.0.8 beta", date = "2026年6月28日",
                changes = listOf("新增历史播放记录功能，支持云端同步", "收藏列表新增搜索", "历史播放列表新增搜索", "新增背景模糊效果支持", "修复历史记录无法同步到云端的问题", "修复历史播放页面背景透明问题"),
                backdrop = backdrop
            )
            ChangelogItem(
                version = "1.0.7 beta", date = "2026年6月28日",
                changes = listOf("新增环绕声功能（虚拟环绕/杜比全景/哈曼卡顿）", "新增自定义均衡器（五段频率调节）", "修复液态导航栏浅色模式图标显示问题", "优化音乐控制台深色模式下的显示效果"),
                backdrop = backdrop
            )
            ChangelogItem(
                version = "1.0.6 beta", date = "2026年6月28日",
                changes = listOf("新增云端更新功能", "新增系统公告功能", "新增更新日志与关于页面", "新增管理账号页面", "优化头像加载缓存"),
                backdrop = backdrop
            )
            ChangelogItem(
                version = "1.0.5 beta", date = "2026年6月27日",
                changes = listOf("新增深色模式", "支持 Material YOU", "引入液态玻璃导航栏", "新增账号系统", "新增收藏功能", "新增历史搜索并持久化"),
                backdrop = backdrop
            )
            ChangelogItem(
                version = "1.0.4 beta", date = "2026年6月22日",
                changes = listOf("新增背景模糊调整", "新增毛玻璃与高斯模糊", "支持各组件独立设置模糊"),
                backdrop = backdrop
            )
            ChangelogItem(
                version = "1.0.3 beta", date = "2026年6月21日",
                changes = listOf("新增背景模糊效果", "支持高度自定义模糊强度", "优化壁纸显示逻辑"),
                backdrop = backdrop
            )
            ChangelogItem(
                version = "1.0.2 beta", date = "2026年6月19日",
                changes = listOf("新增动态颜色主题", "新增基础背景与横幅", "支持自定义横幅和背景"),
                backdrop = backdrop
            )
            ChangelogItem(
                version = "1.0.1 beta", date = "2026年6月18日",
                changes = listOf("增加界面过渡动画", "搜索框改为自定义 Material 样式", "优化整体交互体验"),
                backdrop = backdrop
            )
            ChangelogItem(
                version = "1.0.0 beta", date = "2026年6月18日",
                changes = listOf("首个测试版本发布", "采用完整 Material Design 3 设计", "实现基础音乐搜索与播放功能"),
                isFirst = true,
                backdrop = backdrop
            )
                }
            }
            Spacer(Modifier.height(150.dp))
        }
    }
}

@Composable
fun ChangelogItem(
    version: String,
    date: String,
    changes: List<String>,
    backdrop: Backdrop,
    isLatest: Boolean = false,
    isFirst: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(IntrinsicSize.Min)) {
        Column(modifier = Modifier.width(40.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(if (isLatest) 14.dp else 10.dp).clip(CircleShape).background(if (isLatest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
            if (!isFirst) {
                Box(modifier = Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .then(
                    if (LocalMd3Style.current)
                        Modifier.background(MaterialTheme.colorScheme.surfaceBright, RoundedRectangle(18f.dp))
                    else Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(18f.dp) },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(14f.dp.toPx(), 28f.dp.toPx())
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = if (isLatest) 0.7f else 0.5f)
                        }
                    )
                )
                .clip(RoundedRectangle(18f.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(version, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isLatest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                if (isLatest) {
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary) {
                        Text("最新", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            changes.forEach { change ->
                if (change.startsWith("##")) {
                    Text(
                        change.removePrefix("##").trim(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                } else {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(14.dp))
                        Text(change, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(backdrop: Backdrop, onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = if (isLandscape) 80.dp else 20.dp
    val maxWidth = if (isLandscape) 600.dp else Dp.Unspecified

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val images = remember { listOf(R.drawable.minya_ui, R.drawable.minya_ui1, R.drawable.minya_ui2) }
    var currentImageIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("关于", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding)
                .widthIn(max = maxWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        currentImageIndex = (currentImageIndex + 1) % images.size
                    },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = images[currentImageIndex],
                        animationSpec = tween(400),
                        label = "imageSwitch"
                    ) { imageRes ->
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = "MINova Logo",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (LocalMd3Style.current)
                            Modifier.background(MaterialTheme.colorScheme.surfaceBright, RoundedRectangle(24f.dp))
                        else Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(24f.dp) },
                            effects = {
                                vibrancy()
                                blur(10f.dp.toPx())
                                lens(16f.dp.toPx(), 32f.dp.toPx())
                            },
                            highlight = { Highlight.Default.copy(alpha = 0.5f) }
                        )
                    )
                    .clip(RoundedRectangle(24f.dp))
                    .padding(16.dp)
            ) {
                DetailRow(Icons.Rounded.Info, "软件名称", "MINova")
                DetailRow(Icons.Rounded.SystemUpdate, "应用版本", "1.3.0")
                DetailRow(Icons.Rounded.CalendarMonth, "发布日期", "2026年9月16日")
                DetailRow(Icons.Rounded.Android, "最低运行配置", "骁龙888 / 天玑1100")
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("开发团队", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (LocalMd3Style.current)
                            Modifier.background(MaterialTheme.colorScheme.surfaceBright, RoundedRectangle(24f.dp))
                        else Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(24f.dp) },
                            effects = {
                                vibrancy()
                                blur(10f.dp.toPx())
                                lens(16f.dp.toPx(), 32f.dp.toPx())
                            },
                            highlight = { Highlight.Default.copy(alpha = 0.5f) }
                        )
                    )
                    .clip(RoundedRectangle(24f.dp))
                    .padding(8.dp)
            ) {
                DevCard("https://q1.qlogo.cn/g?b=qq&nk=3662589606&s=640&t=${System.currentTimeMillis()}", "Mintllo", "项目主要维护") { openQQ(context, "3662589606") }
                DevCard("https://q1.qlogo.cn/g?b=qq&nk=0&s=640&t=${System.currentTimeMillis()}", "不愿透露任何信息", "项目bug找寻及意见提及") { openQQ(context, "0") }
                DevCard("https://q1.qlogo.cn/g?b=qq&nk=1951078485&s=640&t=${System.currentTimeMillis()}", "五指拳心剑", "界面优化及功能意见") { openQQ(context, "1951078485") }
                DevCard("https://q1.qlogo.cn/g?b=qq&nk=1322043642&s=640&t=${System.currentTimeMillis()}", "ㅤ", "长期支持") { openQQ(context, "1322043642") }
                DevCard("", "deepseek", "", isAI = true, onClick = {})
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("© 2026 MINova Team", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("All Rights Reserved", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 头像可访问地址。
 * 服务端可能返回相对路径（/uploads/xxx.jpg）也可能返回完整 URL（https://...），
 * 旧代码无条件拼 API_BASE_URL，遇到完整 URL 时会拼成坏地址 —— 表现为「退出重进头像就没了」。
 * 同时兼容已经带 query 的情况，避免拼出两个问号。
 */
fun avatarModelUrl(path: String): String {
    val base = PlayerViewModel.API_BASE_URL.trimEnd('/')
    val full = when {
        path.startsWith("http://") || path.startsWith("https://") -> path
        path.startsWith("/") -> base + path
        else -> "$base/$path"
    }
    return full + (if (full.contains("?")) "&t=" else "?t=") + System.currentTimeMillis()
}
@Composable
fun DevCard(avatarUrl: String, name: String, role: String, isAI: Boolean = false, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), onClick = onClick) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isAI) {
                Icon(Icons.Rounded.SmartToy, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer))
            } else {
                AsyncImage(model = avatarUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                if (role.isNotBlank()) {
                    Text(role, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * 保留原有函数名对外可用；实现体已切换到 AuthScreens.kt 的全屏沉浸式登录 / 注册页。
 */
@Composable
fun LoginScreen(viewModel: PlayerViewModel, backdrop: Backdrop, onBack: () -> Unit = {}) {
    AuthFlowScreen(
        viewModel = viewModel,
        backdrop = backdrop,
        onBack = onBack,
        onAuthSuccess = onBack
    )
}

@Composable
fun ProfileMenuItemCard(
    icon: ImageVector,
    title: String,
    backdrop: Backdrop,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "MenuItemPress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .then(
                if (LocalMd3Style.current)
                    Modifier.background(MaterialTheme.colorScheme.surfaceBright, RoundedRectangle(20f.dp))
                else Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(20f.dp) },
                    effects = {
                        vibrancy()
                        blur(6f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = 0.5f)
                    }
                )
            )
            .clip(RoundedRectangle(20f.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

fun formatResetTime(timestamp: Long): String {
    if (timestamp == 0L) return "首次使用后"
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

fun openQQ(context: android.content.Context, qq: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=$qq")))
    } catch (e: Exception) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/cgi-bin/qm/qr?k=$qq"))) } catch (_: Exception) { AppToast.show("无法打开QQ") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManageScreen(viewModel: PlayerViewModel, backdrop: Backdrop, onBack: () -> Unit) {
    val context = LocalContext.current
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val conversionState by AudioConversionManager.state.collectAsState()
    val conversionResults by AudioConversionManager.results.collectAsState()
    val convertedFiles by AudioConversionManager.convertedFiles.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = if (isLandscape) 80.dp else 20.dp
    val maxWidth = if (isLandscape) 600.dp else Dp.Unspecified

    // 下载目录与 PlayerViewModel 内部逻辑保持一致（外部专属目录 /downloads，未迁移完成时回落内部存储）
    val downloadDir = remember {
        val ext = context.getExternalFilesDir(null)
        File(ext ?: context.filesDir, "downloads").apply { if (!exists()) mkdirs() }
    }

    // 目录快照：下载列表、转换器 refresh（convertedFiles）或 dirTick 变化时重新扫描
    var dirTick by remember { mutableIntStateOf(0) }
    val dirFiles = remember(downloadedSongs, convertedFiles, dirTick) {
        (downloadDir.listFiles()?.filter { it.isFile } ?: emptyList()).sortedByDescending { it.lastModified() }
    }
    val downloadedNames = remember(downloadedSongs) { downloadedSongs.map { "${it.id}.mp3" }.toSet() }
    val convertedOnly = remember(dirFiles, downloadedNames) { dirFiles.filter { it.name !in downloadedNames } }
    val songSizes = remember(downloadedSongs, dirFiles) {
        downloadedSongs.associate { song ->
            song.id to (dirFiles.firstOrNull { f -> f.name == "${song.id}.mp3" }?.length() ?: 0L)
        }
    }
    val downloadSize = songSizes.values.sum()
    val convertedSize = convertedOnly.sumOf { it.length() }
    val totalSize = downloadSize + convertedSize

    // 多选模式
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    fun exitSelection() { selectionMode = false; selectedIds.clear() }
    fun toggleSelect(songId: String) {
        if (songId in selectedIds) selectedIds.remove(songId) else selectedIds.add(songId)
    }
    BackHandler(enabled = selectionMode) { exitSelection() }

    // 格式转换抽屉
    var showConvertSheet by remember { mutableStateOf(false) }
    var convertSources by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedFormatId by remember { mutableStateOf<String?>(null) }
    var selectedBitrate by remember { mutableStateOf("192k") }

    fun songFile(song: Song): File = File(downloadDir, "${song.id}.mp3")

    fun startConvert(files: List<File>) {
        val existing = files.filter { it.exists() }
        if (existing.isEmpty()) {
            AppToast.show("本地文件不存在")
            return
        }
        convertSources = existing
        selectedFormatId = null
        AudioConversionManager.clearResults()
        showConvertSheet = true
    }

    fun shareFiles(files: List<File>) = shareAudioFiles(context, files)

    var showClearAllDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        // object 单例状态保留：离开页面不做清理
        AudioConversionManager.attach(downloadDir)
        onDispose { }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp)) },
            title = { Text("全部删除", fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Text(
                    "将删除全部 ${downloadedSongs.size} 首下载歌曲和 ${convertedOnly.size} 个转换文件，共 ${formatFileSize(totalSize)}，此操作不可恢复。",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllDownloads()
                        AudioConversionManager.refresh()
                        AudioConversionManager.clearResults()
                        dirTick++
                        exitSelection()
                        showClearAllDialog = false
                        AppToast.show("已删除全部文件")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("全部删除", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        )
    }

    if (showBatchDeleteDialog) {
        val batchCount = selectedIds.size
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp)) },
            title = { Text("批量删除", fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Text(
                    "将删除已选的 $batchCount 首歌曲及其本地文件，此操作不可恢复。",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ids = selectedIds.toList()
                        ids.forEach { viewModel.deleteDownloadedSong(it) }
                        AudioConversionManager.refresh()
                        dirTick++
                        exitSelection()
                        showBatchDeleteDialog = false
                        AppToast.show("已删除 $batchCount 首歌曲")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        )
    }

    if (showConvertSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConvertSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            ConvertSheetContent(
                sources = convertSources,
                running = conversionState != null,
                state = conversionState,
                results = conversionResults,
                selectedFormat = selectedFormatId?.let { AudioFormats.byId(it) },
                onFormatSelect = { selectedFormatId = it.id },
                selectedBitrate = selectedBitrate,
                onBitrateSelect = { selectedBitrate = it },
                onStart = { format ->
                    AudioConversionManager.convert(convertSources, format, selectedBitrate, AudioFormats.SAMPLE_RATES.first())
                    AppToast.show("开始转换 ${convertSources.size} 个文件")
                },
                onCancel = { AudioConversionManager.cancel() },
                onShare = { shareAudioFiles(context, listOf(it)) }
            )
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("已选 ${selectedIds.size} 首", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = { exitSelection() }) {
                            Icon(Icons.Rounded.Close, "退出多选")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedIds.clear()
                            selectedIds.addAll(downloadedSongs.map { it.id })
                        }) { Icon(Icons.Rounded.DoneAll, "全选") }
                        IconButton(onClick = {
                            val inverted = downloadedSongs.map { it.id }.toSet() - selectedIds.toSet()
                            selectedIds.clear()
                            selectedIds.addAll(inverted)
                        }) { Icon(Icons.Rounded.SwapHoriz, "反选") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            } else {
                LargeTopAppBar(
                    title = { Text("管理本地存储", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回")
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectionMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                StorageBatchBar(
                    selectedCount = selectedIds.size,
                    onDelete = { if (selectedIds.isNotEmpty()) showBatchDeleteDialog = true },
                    onConvert = {
                        val files = downloadedSongs.filter { it.id in selectedIds }.map { songFile(it) }.filter { it.exists() }
                        if (files.isNotEmpty()) startConvert(files)
                        else AppToast.show("所选歌曲没有本地文件")
                    },
                    onShare = {
                        val files = downloadedSongs.filter { it.id in selectedIds }.map { songFile(it) }.filter { it.exists() }
                        if (files.isNotEmpty()) shareFiles(files)
                    }
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = horizontalPadding)
                .widthIn(max = maxWidth),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "stats") {
                StoragePanel(backdrop = backdrop) {
                    StorageStatsContent(
                        totalSize = totalSize,
                        downloadSize = downloadSize,
                        convertedSize = convertedSize,
                        songCount = downloadedSongs.size,
                        convertedCount = convertedOnly.size,
                        onClearAll = { showClearAllDialog = true }
                    )
                }
            }

            if (downloadedSongs.isEmpty()) {
                item(key = "empty") {
                    StoragePanel(backdrop = backdrop) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.CloudDownload, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), modifier = Modifier.size(56.dp))
                            Text("暂无下载歌曲", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("在歌曲上长按即可下载", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
                        }
                    }
                }
            } else {
                item(key = "dl_header") {
                    StorageSectionHeader(
                        title = "已下载歌曲",
                        subtitle = "${downloadedSongs.size} 首 · ${formatFileSize(downloadSize)}",
                        hint = if (!selectionMode) "长按歌曲进入批量管理" else null
                    )
                }
                itemsIndexed(downloadedSongs, key = { _, song -> "song_${song.id}" }) { _, song ->
                    DownloadedSongRow(
                        song = song,
                        fileSize = songSizes[song.id] ?: 0L,
                        selectionMode = selectionMode,
                        selected = song.id in selectedIds,
                        backdrop = backdrop,
                        onClick = { if (selectionMode) toggleSelect(song.id) else viewModel.playLocalSong(song) },
                        onLongClick = {
                            if (!selectionMode) {
                                selectionMode = true
                                selectedIds.add(song.id)
                            } else {
                                toggleSelect(song.id)
                            }
                        },
                        onShare = { shareFiles(listOf(songFile(song))) },
                        onConvert = { startConvert(listOf(songFile(song))) },
                        onDelete = {
                            viewModel.deleteDownloadedSong(song.id)
                            AudioConversionManager.refresh()
                            dirTick++
                            AppToast.show("已删除「${song.name}」")
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            if (conversionState != null) {
                item(key = "conv_progress") {
                    ConversionProgressCard(
                        state = conversionState!!,
                        onCancel = { AudioConversionManager.cancel() },
                        backdrop = backdrop
                    )
                }
            }

            if (convertedOnly.isNotEmpty()) {
                item(key = "cv_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("转换结果", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "${convertedOnly.size} 个文件 · ${formatFileSize(convertedSize)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                val n = AudioConversionManager.clearAll()
                                dirTick++
                                AppToast.show("已清空 $n 个转换文件")
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("清空", fontSize = 13.sp) }
                    }
                }
                itemsIndexed(convertedOnly, key = { _, file -> "conv_${file.absolutePath}" }) { _, file ->
                    ConvertedFileRow(
                        file = file,
                        backdrop = backdrop,
                        onShare = { shareFiles(listOf(file)) },
                        onDelete = {
                            AudioConversionManager.delete(file)
                            dirTick++
                            AppToast.show("已删除「${file.name}」")
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            item(key = "spacer") { Spacer(Modifier.height(28.dp)) }
        }
    }
}

/** 双风格面板：MD3 用 surfaceContainerLow，液态玻璃用 backdrop 毛玻璃；无边框、无阴影。 */
@Composable
private fun StoragePanel(
    backdrop: Backdrop,
    corner: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    val base = if (LocalMd3Style.current) {
        Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
    } else {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(8f.dp.toPx())
                lens(16f.dp.toPx(), 32f.dp.toPx())
            },
            highlight = { Highlight.Default.copy(alpha = 0.5f) }
        )
    }
    Column(modifier = base.fillMaxWidth(), content = content)
}

/** 统计卡内容：总占用 + 已下载 N 首 + 转换文件占用 + 全部删除入口。 */
@Composable
private fun StorageStatsContent(
    totalSize: Long,
    downloadSize: Long,
    convertedSize: Long,
    songCount: Int,
    convertedCount: Int,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text("存储空间", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Text(formatFileSize(totalSize), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            if (totalSize > 0) "已下载 $songCount 首歌曲 · 转换文件 $convertedCount 个" else "已下载 0 首歌曲",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        StorageStatRow(icon = Icons.Rounded.MusicNote, label = "下载歌曲", value = formatFileSize(downloadSize), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        StorageStatRow(icon = Icons.Rounded.Audiotrack, label = "转换文件", value = formatFileSize(convertedSize), tint = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.height(18.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
            onClick = onClearAll
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("全部删除", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StorageStatRow(icon: ImageVector, label: String, value: String, tint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

@Composable
private fun StorageSectionHeader(title: String, subtitle: String, hint: String? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp)) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        hint?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

/** 已下载歌曲行：56dp 封面 + 歌名 + 歌手 + 文件大小；多选模式显示 Checkbox；溢出菜单提供播放/分享/转换/删除。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadedSongRow(
    song: Song,
    fileSize: Long,
    selectionMode: Boolean,
    selected: Boolean,
    backdrop: Backdrop,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShare: () -> Unit,
    onConvert: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "StorageRowPress"
    )

    val base = if (LocalMd3Style.current) {
        Modifier.background(
            if (selectionMode && selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
            shape
        )
    } else {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(6f.dp.toPx())
                lens(12f.dp.toPx(), 24f.dp.toPx())
            },
            highlight = { Highlight.Default.copy(alpha = 0.5f) }
        )
    }

    Row(
        modifier = base
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressedScale; scaleY = pressedScale }
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
            Spacer(Modifier.width(4.dp))
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            if (song.picUrl.isNotBlank()) {
                AsyncImage(model = song.picUrl, contentDescription = "封面", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                // 无封面兜底：统一封面占位图铺满（原为 MusicNote 图标）
                Image(
                    painter = painterResource(R.drawable.fallback_cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(song.artist.ifBlank { "未知歌手" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (fileSize > 0) formatFileSize(fileSize) else "--",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        if (!selectionMode) {
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.MoreVert, "更多操作", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("播放") },
                        leadingIcon = { Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp)) },
                        onClick = { menuOpen = false; onClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("分享") },
                        leadingIcon = { Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp)) },
                        onClick = { menuOpen = false; onShare() }
                    )
                    DropdownMenuItem(
                        text = { Text("转换格式") },
                        leadingIcon = { Icon(Icons.Rounded.Transform, null, modifier = Modifier.size(18.dp)) },
                        onClick = { menuOpen = false; onConvert() }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

/** 转换结果文件行：可分享 / 删除。 */
@Composable
private fun ConvertedFileRow(
    file: File,
    backdrop: Backdrop,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val base = if (LocalMd3Style.current) {
        Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
    } else {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(6f.dp.toPx())
                lens(12f.dp.toPx(), 24f.dp.toPx())
            },
            highlight = { Highlight.Default.copy(alpha = 0.5f) }
        )
    }
    Row(
        modifier = base.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Audiotrack, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatFileSize(file.length())} · ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Rounded.Share, "分享", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Rounded.Close, "删除", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        }
    }
}

/** 转换进行中的进度卡（抽屉关闭时页面上也能看到进度）。 */
@Composable
private fun ConversionProgressCard(state: ConversionState, onCancel: () -> Unit, backdrop: Backdrop) {
    StoragePanel(backdrop = backdrop, corner = 18.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Transform, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "正在转换 ${state.currentIndex}/${state.total}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                if (state.cancelling) {
                    Text("正在取消…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Cancel, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("取消", fontSize = 12.sp)
                    }
                }
            }
            if (state.indeterminate) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
            } else {
                LinearProgressIndicator(progress = { state.fraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    strokeCap = StrokeCap.Round
                )
            }
            Text(
                "${state.sourceName} → ${state.targetLabel}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 多选模式底部批量操作栏：批量删除 / 批量转换 / 批量分享。 */
@Composable
private fun StorageBatchBar(selectedCount: Int, onDelete: () -> Unit, onConvert: () -> Unit, onShare: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onDelete,
                enabled = selectedCount > 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("删除", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            FilledTonalButton(
                onClick = onConvert,
                enabled = selectedCount > 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Icon(Icons.Rounded.Transform, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("转换", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            FilledTonalButton(
                onClick = onShare,
                enabled = selectedCount > 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("分享", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** 格式转换抽屉内容：选格式（不支持项置灰并给原因）→ 选码率 → 开始转换；转换中显示进度；完成后展示结果。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConvertSheetContent(
    sources: List<File>,
    running: Boolean,
    state: ConversionState?,
    results: List<ConversionResult>,
    selectedFormat: AudioFormat?,
    onFormatSelect: (AudioFormat) -> Unit,
    selectedBitrate: String,
    onBitrateSelect: (String) -> Unit,
    onStart: (AudioFormat) -> Unit,
    onCancel: () -> Unit,
    onShare: (File) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Transform, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("格式转换", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (sources.size == 1) sources.first().name else "已选 ${sources.size} 个文件",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (running && state != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "正在转换 ${state.currentIndex}/${state.total}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.weight(1f))
                    if (state.cancelling) {
                        Text("正在取消…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Rounded.Cancel, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("取消", fontSize = 13.sp)
                        }
                    }
                }
                if (state.indeterminate) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                } else {
                    LinearProgressIndicator(progress = { state.fraction },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        strokeCap = StrokeCap.Round
                    )
                }
                Text(
                    "${state.sourceName} → ${state.targetLabel}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Text("目标格式", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AudioFormats.ALL.chunked(4).forEach { rowFormats ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowFormats.forEach { format ->
                        StorageFormatCell(
                            format = format,
                            selected = selectedFormat?.id == format.id,
                            onClick = { onFormatSelect(format) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(4 - rowFormats.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
            val unsupportedNotes = AudioFormats.ALL.filter { it.unsupported }
                .joinToString("\n") { "· ${it.label}：${it.note ?: "暂不支持"}" }
            if (unsupportedNotes.isNotBlank()) {
                Text(
                    unsupportedNotes,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(4.dp))
            Text("码率（仅对有损格式生效）", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AudioFormats.BITRATES.forEach { bitrate ->
                    FilterChip(
                        selected = selectedBitrate == bitrate,
                        onClick = { onBitrateSelect(bitrate) },
                        label = { Text(bitrate, fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { selectedFormat?.let(onStart) },
                enabled = selectedFormat != null && sources.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Transform, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "开始转换${if (sources.size > 1) " ${sources.size} 个文件" else ""}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }

        if (results.isNotEmpty()) {
            Text("转换结果", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                results.forEach { result ->
                    ConversionResultRow(result = result, onShare = onShare)
                }
            }
        }
    }
}

/** 格式选择格：unsupported 的置灰并展示「不支持」，原因统一列在网格下方。 */
@Composable
private fun StorageFormatCell(format: AudioFormat, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val background = when {
        format.unsupported -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val contentColor = when {
        format.unsupported -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .then(if (format.unsupported) Modifier else Modifier.clickable(onClick = onClick))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(format.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
            Text(if (format.unsupported) "不支持" else format.ext, fontSize = 10.sp, color = contentColor.copy(alpha = 0.75f))
        }
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(12.dp)
            )
        }
    }
}

@Composable
private fun ConversionResultRow(result: ConversionResult, onShare: (File) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (result.success) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            null,
            tint = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(result.sourceName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (result.success) "已生成 ${result.output?.name ?: ""}" else (result.message ?: "转换失败"),
                fontSize = 11.sp,
                color = if (result.success) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (result.success && result.output != null) {
            TextButton(
                onClick = { onShare(result.output) },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Rounded.Share, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("分享", fontSize = 12.sp)
            }
        }
    }
}

/** 通过 FileProvider 分享音频文件（单个用 ACTION_SEND，多个用 ACTION_SEND_MULTIPLE）。 */
private fun shareAudioFiles(context: android.content.Context, files: List<File>) {
    val existing = files.filter { it.exists() }
    if (existing.isEmpty()) {
        AppToast.show("文件不存在")
        return
    }
    try {
        val uris = ArrayList(
            existing.map { FileProvider.getUriForFile(context, context.packageName + ".fileprovider", it) }
        )
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = AudioFormats.fromFileName(existing.first().name)?.mime ?: "audio/*"
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "分享音频文件"))
    } catch (_: Exception) {
        AppToast.show("未找到可分享的应用")
    }
}

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "${size}B"
        size < 1024 * 1024 -> "${size / 1024}KB"
        size < 1024 * 1024 * 1024 -> "${"%.1f".format(size / (1024.0 * 1024.0))}MB"
        else -> "${"%.2f".format(size / (1024.0 * 1024.0 * 1024.0))}GB"
    }
}

// ============================================================================
//  意见反馈（原「配置导出导入」入口，POST /api/v1/feedback）
// ============================================================================

private val FEEDBACK_TYPES = listOf("功能建议", "问题反馈", "播放异常", "其他")

/** 自动附带的设备信息：仅机型/系统/版本/屏幕/语言，不含账号或任何硬件标识。 */
private fun feedbackDeviceInfo(context: android.content.Context): String {
    val dm = context.resources.displayMetrics
    val pkg = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    val version = pkg?.versionName ?: "unknown"
    val code = if (android.os.Build.VERSION.SDK_INT >= 28) {
        pkg?.longVersionCode?.toString() ?: ""
    } else {
        @Suppress("DEPRECATION")
        pkg?.versionCode?.toString() ?: ""
    }
    return "机型: " + android.os.Build.BRAND + " " + android.os.Build.MODEL +
        " | 系统: Android " + android.os.Build.VERSION.RELEASE +
        " (API " + android.os.Build.VERSION.SDK_INT + ")" +
        " | 应用: " + version + (if (code.isNotBlank()) " (" + code + ")" else "") +
        " | 屏幕: " + dm.widthPixels + "x" + dm.heightPixels + "/" + dm.densityDpi + "dpi" +
        " | 语言: " + java.util.Locale.getDefault()
}

@Composable
private fun FeedbackTypeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val container by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        label = "FeedbackChipContainer"
    )
    val textColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "FeedbackChipText"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: PlayerViewModel,
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf(FEEDBACK_TYPES.first()) }
    var content by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var showDeviceInfo by remember { mutableStateOf(false) }
    val deviceInfo = remember { feedbackDeviceInfo(context) }
    val canSubmit = !submitting && content.trim().length >= 5

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            LargeTopAppBar(
                title = { Text("意见反馈", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "遇到问题或有新想法？告诉我们，我们会尽快处理。提交时会自动附带设备信息，便于定位问题。",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(4.dp))
            Text("反馈类型", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FEEDBACK_TYPES.chunked(2).forEach { rowTypes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowTypes.forEach { t ->
                            FeedbackTypeChip(
                                label = t,
                                selected = type == t,
                                modifier = Modifier.weight(1f),
                                onClick = { type = t }
                            )
                        }
                        if (rowTypes.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("反馈内容", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= 1000) content = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                placeholder = {
                    Text("请描述你遇到的问题或建议（至少 5 个字）", fontSize = 13.sp)
                },
                shape = RoundedCornerShape(16.dp)
            )
            Text(
                content.length.toString() + " / 1000",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            Text("联系方式", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = contact,
                onValueChange = { if (it.length <= 100) contact = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("邮箱 / QQ（选填，便于我们回复你）", fontSize = 13.sp) },
                shape = RoundedCornerShape(16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showDeviceInfo = !showDeviceInfo }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Android,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "已自动附带设备信息",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (showDeviceInfo) "收起" else "查看",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = showDeviceInfo) {
                Text(
                    deviceInfo,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }

            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    if (!canSubmit) {
                        AppToast.show("请至少输入 5 个字的反馈内容")
                        return@Button
                    }
                    submitting = true
                    scope.launch {
                        val errMsg = withContext(Dispatchers.IO) {
                            runCatching {
                                val json = org.json.JSONObject().apply {
                                    put("type", type)
                                    put("content", content.trim())
                                    put("contact", contact.trim())
                                    put("device_info", deviceInfo)
                                }
                                val conn = java.net.URL(PlayerViewModel.API_BASE_URL + "/api/v1/feedback")
                                    .openConnection() as java.net.HttpURLConnection
                                conn.requestMethod = "POST"
                                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                                if (viewModel.authToken.isNotBlank()) {
                                    conn.setRequestProperty("Authorization", "Bearer " + viewModel.authToken)
                                }
                                conn.doOutput = true
                                conn.connectTimeout = 10000
                                conn.readTimeout = 15000
                                conn.outputStream.use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }
                                val code = conn.responseCode
                                val serverMsg = if (code in 200..299) "" else runCatching {
                                    val t = conn.errorStream?.bufferedReader()?.readText().orEmpty()
                                    org.json.JSONObject(t).optJSONObject("error")?.optString("message")
                                        ?.takeIf { it.isNotBlank() } ?: "HTTP $code"
                                }.getOrDefault("HTTP $code")
                                conn.disconnect()
                                serverMsg
                            }.getOrDefault("网络错误，请检查网络连接")
                        }
                        submitting = false
                        if (errMsg.isEmpty()) {
                            submitted = true
                            content = ""
                            contact = ""
                            AppToast.show("反馈已提交，感谢你的建议", long = true)
                        } else {
                            AppToast.show("提交失败：$errMsg", long = true)
                        }
                    }
                },
                enabled = !submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("提交中…", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("提交反馈", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            AnimatedVisibility(visible = submitted) {
                Text(
                    "已收到你的反馈，我们会在 3 个工作日内处理。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}
