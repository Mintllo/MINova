package com.HyperX.MI.Na.cmx

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import kotlin.math.abs

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable internal fun PlaylistRecordShelf(songs: List<Song>, canPlay: Boolean,
    onPlay: (Int) -> Unit, onClose: () -> Unit,
    isSaved: (Song) -> Boolean = { false }, onSave: ((Song) -> Unit)? = null, saveEnabled: Boolean = true,
    onPreview: (Song?) -> Unit = {},
    previewMode: RecordShelfPreviewMode = RecordShelfPreviewMode.ONCE,
    onPreviewModeChange: (RecordShelfPreviewMode) -> Unit = {},
    fullPreview: Boolean = false,
    onFullPreviewChange: (Boolean) -> Unit = {},
    nextPreviewRequests: Flow<Song>? = null,
    collectionKey: String = "",
    // 播放状态接线：黑胶伸出 + 底部三控制钮 + 长按菜单
    playingSongId: String? = null,
    playing: Boolean = false,
    onPrevious: () -> Unit = {},
    onTogglePlay: () -> Unit = {},
    onNext: () -> Unit = {},
    onSongLongPress: (Song) -> Unit = {},
    vinylAccent: Color = Color.White,
    // 底部进度条（RulerSeekBar 刻度尺）接线：viewModel 播放状态 + seek
    positionMs: Long = 0L,
    durationMs: Long = 0L,
    onSeek: (Long) -> Unit = {},
    // 陈列模式：STACK = 原生纵向 3D 唱片架（RecordShelfView 原生 View，由 AndroidView 托载）。
    // FLOW 平铺模式已删除；SIMPLE 不路由到这里（PlaylistShelfScene 简洁模式走独立列表）。
    // 液态玻璃管线已整体拆除（任务 3）：本页不接收 backdrop，控件全部走简洁材质。
    shelfMode: PlaylistShelfMode = PlaylistShelfMode.STACK,
    playlistName: String = "",
    playlistCount: Int = -1) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val renderer = remember(context, collectionKey) { RecordShelfView(context) }
    val customTypeface = LocalCustomTypeface.current
    val pageActive by rememberUpdatedState(LocalMusicPageActive.current)
    val refreshRate = LocalRefreshRatePreference.current
    var selection by remember(renderer) { mutableIntStateOf(0) }
    var displayedSongs by remember(renderer) { mutableStateOf(songs) }
    var replacingResults by remember(renderer) { mutableStateOf(false) }
    var query by rememberSaveable(collectionKey) { mutableStateOf("") }
    var searchOpen by rememberSaveable(collectionKey) { mutableStateOf(false) }
    var optionsOpen by rememberSaveable(collectionKey) { mutableStateOf(false) }
    // 抽出观摩（任务 3 重做）：全程原生 RecordShelfView 画布内 3D，无 overlay。
    // Compose 侧只挂状态回声：renderer.onInspectMode → inspecting（隐藏底部控件 + 返回键接管）。
    var inspecting by remember(renderer) { mutableStateOf(false) }
    val inspectingNow by rememberUpdatedState(inspecting)
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val controlsAlpha by animateFloatAsState(if (appeared) 1f else 0f, tween(500), label = "shelf controls")
    val filtered = remember(songs, query) {
        songs.filter { query.isBlank() || listOf(it.name, it.artists, it.album).any { text -> text.contains(query.trim(), true) } }
    }
    val selected = displayedSongs.getOrNull(selection)
    // 死灰活化（任务 7）：封面取色氛围渐变 —— 顶部 primary 12% 洒在深底上，换卡 800ms 缓动
    val ambientPalette by rememberArtworkGlowPalette(selected?.cover.orEmpty())
    val ambientPrimary by androidx.compose.animation.animateColorAsState(
        Color(ambientPalette.primary), tween(800), label = "shelf ambient")
    fun openInspect() {
        if (selected == null) return
        // 抽出 + 观摩 + 插回全部在原生画布内完成（enterInspect/exitInspect）
        renderer.enterInspect()
    }
    val density = LocalDensity.current
    val top = WindowInsets.statusBars.getTop(density).toFloat()
    // 进架定位播放中歌：首帧 SideEffect 用 restoreSelection 直接落到 playingSongId（不穿架动画）；
    // 之后播放切歌由 LaunchedEffect(playingSongId, collectionKey) 让顶卡跟随重新定位。
    var positioned by remember(renderer) { mutableStateOf(false) }
    val activate by rememberUpdatedState<(Int) -> Unit>({ index ->
        if (canPlay && !replacingResults) displayedSongs.getOrNull(index)?.let { song ->
            songs.indexOfFirst { it.id == song.id && it.musicSource == song.musicSource }.takeIf { it >= 0 }?.let(onPlay)
        }
    })
    val currentPreview by rememberUpdatedState<(Int?) -> Unit>({ index -> onPreview(if (canPlay) index?.let(displayedSongs::getOrNull) else null) })
    val currentLongPress by rememberUpdatedState<(Int) -> Unit>({ index -> displayedSongs.getOrNull(index)?.let(onSongLongPress) })
    // 试听结束自动换卡：原生 pick（带落定动画），对齐上游唱片架
    val advancePreview by rememberUpdatedState<(Song) -> Unit>({ finished ->
        if (!replacingResults && previewMode in setOf(RecordShelfPreviewMode.NEXT, RecordShelfPreviewMode.RANDOM) && canPlay && pageActive &&
            displayedSongs.getOrNull(selection)?.let { it.id == finished.id && it.musicSource == finished.musicSource } == true) {
            nextRecordPreviewIndex(displayedSongs, selection, previewMode == RecordShelfPreviewMode.RANDOM)?.let(renderer::pick)
        }
    })
    LaunchedEffect(nextPreviewRequests) { nextPreviewRequests?.collect { advancePreview(it) } }
    // 换歌跟随：进架首帧定位由 SideEffect restoreSelection 负责；此后播放切歌，顶卡跟过去
    LaunchedEffect(playingSongId, collectionKey) {
        if (!positioned) return@LaunchedEffect
        val playingSong = songs.firstOrNull { it.id == playingSongId } ?: return@LaunchedEffect
        val index = displayedSongs.indexOfFirst { it.id == playingSong.id && it.musicSource == playingSong.musicSource }
        if (index >= 0 && index != selection) { selection = index; renderer.select(index) }
    }
    BackHandler(enabled = pageActive) { if (searchOpen) searchOpen = false else if (optionsOpen) optionsOpen = false else onClose() }
    // 观摩模式返回键优先收回（后注册的 enabled BackHandler 优先）：原生反向插回动画
    BackHandler(enabled = inspectingNow) { renderer.exitInspect() }
    DisposableEffect(renderer, owner) {
        renderer.onSelection = { selection = it }
        renderer.onSongsChanged = { displayedSongs = it }
        renderer.onResultsChangingChanged = { replacingResults = it }
        renderer.onPreview = { currentPreview(it) }
        renderer.onActivate = { activate(it) }
        renderer.onSongLongPress = { currentLongPress(it) }
        renderer.onInspectMode = { inspecting = it }
        val observer = LifecycleEventObserver { _, _ ->
            renderer.setRunning(pageActive && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); renderer.setRunning(false) }
    }
    DisposableEffect(renderer) { onDispose { renderer.dispose() } }
    SideEffect {
        renderer.setCustomTypeface(customTypeface)
        renderer.setRunning(pageActive && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        // Keep result changes independent of AndroidView layout updates. The renderer owns both
        // the outgoing and incoming phases, including updates delivered behind the search overlay.
        renderer.configure(filtered, searching = query.isNotBlank())
        if (!positioned && songs.isNotEmpty()) {
            // 进架定位：selection 初始 = playingSongId 下标（restoreSelection 直接落位，不穿架动画）
            songs.firstOrNull { it.id == playingSongId }?.let { playingSong ->
                val index = renderer.restoreSelection(playingSong.id, playingSong.musicSource.name)
                selection = index.coerceAtLeast(0)
            }
            positioned = true
        }
        // 黑胶同步播放状态：playingSongId 命中的唱片在播放时滑出黑胶
        renderer.setVinyl(playingSongId, playing, vinylAccent.toArgb())
        displayedSongs = renderer.displayedSongs
        replacingResults = renderer.replacingResults
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF102132)).testTag("playlist_record_shelf")) {
        val screenWidth = maxWidth
        val safeInsets = WindowInsets.safeDrawing
        val direction = LocalLayoutDirection.current
        val safeWidth = screenWidth - with(density) {
            (safeInsets.getLeft(density, direction) + safeInsets.getRight(density, direction)).toDp()
        }
        // Scale within a compact control group, not the full landscape/tablet width.
        val footerWidth = safeWidth.coerceAtMost(420.dp)
        val footerSize = (footerWidth * .132f).coerceIn(48.dp, 56.dp)
        // 底部控制区分组分层（任务 4）：进度条一行 + 三段式胶囊一行 + 次级钮一行，16dp 网格；
        // 唱片架 bottomInset 预留控制区真实总高，唱片不再被控件遮挡。
        val footerMargin = 16.dp
        val footerStack = 30.dp + 16.dp + footerSize + 12.dp + 36.dp + footerMargin
        val footerInset = with(density) { footerStack.toPx() } + safeInsets.getBottom(density)
        Box(Modifier.matchParentSize()) {
            androidx.compose.animation.Crossfade(selected?.cover.orEmpty(), Modifier.matchParentSize(), tween(600), label = "shelf cover background") { cover ->
                PlayerBackdrop(cover, Modifier.fillMaxSize())
            }
            // 封面取色氛围渐变（任务 7）：顶部 primary 12% → 透明，盖在深底上活色
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                listOf(ambientPrimary.copy(alpha = .12f), Color.Transparent))))
            // 卡片区：STACK = 原生纵向 3D 唱片架 —— RecordShelfView 由 AndroidView 托载。
            // 接线：songs 注入（configure）/ onSelection→selection / onActivate→播放 /
            // onSongLongPress→菜单 / setVinyl 播放状态 / 搜索过滤 setSongs / position 定位 playingSongId。
            key(renderer) {
                AndroidView(factory = { renderer }, update = {
                    it.topInset = top; it.bottomInset = footerInset; applyViewRefreshRate(it, refreshRate); it.invalidate()
                }, modifier = Modifier.matchParentSize().testTag("record_shelf_canvas").semantics {
                    contentDescription = "CD 唱片架，左右拖动换唱片，点击唱片抽出，再次点击播放，底部刻度可快速定位"
                    stateDescription = if (replacingResults) "正在切换唱片"
                        else selected?.let { "${selection + 1} / ${displayedSongs.size}，${it.name}，${it.artists}" } ?: "没有匹配的歌曲"
                    if (replacingResults) disabled()
                    onClick("播放当前歌曲") {
                        if (canPlay && selected != null && !replacingResults) { activate(selection); true } else false
                    }
                    scrollBy { x, y -> renderer.travel((x - y * .55f) / 70f); true }
                    scrollToIndex { index ->
                        if (!replacingResults && index in displayedSongs.indices) { renderer.select(index); true } else false
                    }
                    customActions = listOf(
                        CustomAccessibilityAction("上一张唱片") { renderer.travel(-1f); true },
                        CustomAccessibilityAction("下一张唱片") { renderer.travel(1f); true },
                        CustomAccessibilityAction("播放当前歌曲") { activate(selection); canPlay && selected != null },
                        CustomAccessibilityAction("返回列表") { onClose(); true },
                    )
                })
            }
        }
        // 头部（任务 4 排版高级化）：歌单名大字 + 副信息疏朗（曲数 · 当前曲目）；
        // 搜索/退出钮与标题保持安全距离（14dp 间距 + 5% 边距，不与标题叠），头部下方合计 24dp 留白。
        // zIndex(1f)：头部（搜索/退出 X）明确压在全屏 RecordShelfView 之上，命中测试优先，
        // 配合 RecordShelfView 头部死区让路，保证 X 退出钮点击必达 onClose。
        Row(Modifier.align(Alignment.TopCenter).zIndex(1f).graphicsLayer { alpha = controlsAlpha }
            .windowInsetsPadding(safeInsets.only(WindowInsetsSides.Horizontal)).statusBarsPadding()
            .padding(start = screenWidth * .05f, end = screenWidth * .05f, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ShelfCircle("搜索歌曲", "record_shelf_search", { searchOpen = true }) {
                Icon(Icons.Default.Search, null, Modifier.size(20.dp), tint = Color.White)
            }
            Column(Modifier.weight(1f).padding(top = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // 歌单名暖白渐变大字（暖白 → 暖黄纵向渐变 brush，沿用原项目高级感）
                Text(playlistName.ifBlank { selected?.name ?: "没有匹配的歌曲" },
                    fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold,
                    style = androidx.compose.ui.text.TextStyle(brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFF7E8), Color(0xFFF0D970)))),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("record_shelf_title"))
                val meta = buildString {
                    if (playlistCount >= 0) append("$playlistCount 首")
                    selected?.name?.takeIf(String::isNotBlank)?.let { if (isNotEmpty()) append(" · "); append(it) }
                }
                Text(meta, Modifier.padding(top = 8.dp), fontSize = 13.sp,
                    color = Color.White.copy(alpha = .6f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
            // 退出按钮直白化：X 圆钮 + 无障碍文案「退出歌单」（原唱片架图标不直观）
            ShelfCircle("退出歌单", "record_shelf_close", onClose) {
                ShelfGlyph("close", Modifier.size(21.dp))
            }
        }
        // 观摩中隐藏底部控件（避免干扰拖拽旋转），退出观摩淡入恢复（任务 3）
        androidx.compose.animation.AnimatedVisibility(
            visible = !inspecting, modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
        Column(Modifier.graphicsLayer { alpha = controlsAlpha }
            .windowInsetsPadding(safeInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(bottom = footerMargin).width(footerWidth)) {
            // 底部控制区（任务 4 高级化）分组分层：① 进度条独占一行 → ② 三段式胶囊居中一行 →
            // ③ 次级钮（收藏 / 抽出观察 / 选项）一行小尺寸 36dp；组间 16dp/12dp 网格，不再堆一行。
            RulerSeekBar(
                value = positionMs.toFloat(),
                onSeek = { onSeek(it.toLong()) },
                valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = footerWidth * .05f).height(30.dp),
            )
            Spacer(Modifier.height(16.dp))
            // 三段式播放控制：只留三个图标，居中独立一行
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ShelfTransportCapsule(width = footerWidth * .44f, height = footerSize,
                    enabled = true, playing = playing,
                    onPrevious = onPrevious, onTogglePlay = onTogglePlay, onNext = onNext)
            }
            Spacer(Modifier.height(12.dp))
            // 次级钮一行：小尺寸 36dp（图标 20dp），16dp 网格间距
            Row(Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                ShelfIconAction(if (selected?.let(isSaved) == true) "取消喜欢" else "喜欢当前歌曲", "record_shelf_save", {
                    selected?.let { onSave?.invoke(it) }
                }, enabled = selected != null && !selected.video && onSave != null && saveEnabled, size = 36.dp) {
                    Icon(if (selected?.let(isSaved) == true) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        null, Modifier.size(20.dp), tint = Color.White)
                }
                ShelfTextAction("抽出观察", "record_shelf_inspect", ::openInspect,
                    enabled = selected != null)
                ShelfIconAction("唱片架选项", "record_shelf_options", { optionsOpen = true }, size = 36.dp) {
                    ShelfGlyph("sliders", Modifier.size(20.dp))
                }
            }
        }
        }
    }
    if (optionsOpen) MusicDialog("唱片架", onDismiss = { optionsOpen = false }) {
        SettingsSwitch("整首试听", fullPreview, onFullPreviewChange,
            Modifier.testTag("record_shelf_preview_full"), summary = "从头播放整首歌曲，关闭后恢复高潮片段")
        SettingsSwitch(if (fullPreview) "循环试听整首歌" else "循环试听片段", previewMode == RecordShelfPreviewMode.LOOP,
            { onPreviewModeChange(if (it) RecordShelfPreviewMode.LOOP else RecordShelfPreviewMode.ONCE) },
            Modifier.testTag("record_shelf_preview_loop"), summary = if (fullPreview) "重复播放当前唱片的整首歌曲" else "重复当前唱片的试听片段")
        SettingsSwitch("试听结束自动下一首", previewMode in setOf(RecordShelfPreviewMode.NEXT, RecordShelfPreviewMode.RANDOM),
            { onPreviewModeChange(if (it) RecordShelfPreviewMode.NEXT else RecordShelfPreviewMode.ONCE) },
            Modifier.testTag("record_shelf_preview_next"), summary = if (previewMode == RecordShelfPreviewMode.RANDOM)
                "试听结束后随机抽出另一张唱片" else "按当前列表顺序试听，最后一首后回到开头")
        SettingsSwitch("自动随机切换", previewMode == RecordShelfPreviewMode.RANDOM,
            { onPreviewModeChange(if (it) RecordShelfPreviewMode.RANDOM else RecordShelfPreviewMode.NEXT) },
            Modifier.testTag("record_shelf_preview_random"), summary = "随机抽出另一张唱片并试听",
            enabled = previewMode in setOf(RecordShelfPreviewMode.NEXT, RecordShelfPreviewMode.RANDOM))
        MusicDialogButton("返回歌曲列表", onClick = { optionsOpen = false; onClose() })
    }

    // 搜索唱片（任务 1）：真 M3 ModalBottomSheet —— 液态质感面（半透明白 .14 + 白 .26 细描边）+
    // 28dp 顶圆角 + 拖拽手柄；内嵌 M3 OutlinedTextField（leadingIcon=Search，输入实时过滤唱片架），确认/清空按钮。
    if (searchOpen) {
        val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        // 新液态（任务 1）：MD3 结构保持（真 ModalBottomSheet + 28dp 顶圆角 + 拖拽手柄内移），
        // 表面材质换液态质感 —— 去掉 surfaceContainerLow 实色底，改半透明白 .14 面 + 白 .26 细描边。
        ModalBottomSheet(
            onDismissRequest = { searchOpen = false },
            sheetState = searchSheetState,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = null) {
            Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding()) {
                Column(Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.60f))
                    .border(1.dp, Color.White.copy(alpha = .26f),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(horizontal = 24.dp).padding(bottom = 18.dp)) {
                Box(Modifier.padding(top = 20.dp, bottom = 12.dp).width(36.dp).height(4.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .35f),
                        RoundedCornerShape(2.dp)))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().testTag("record_shelf_query"),
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    label = { Text("歌名、歌手或专辑") },
                    placeholder = { Text("输入关键词实时过滤唱片架") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                Text("${filtered.size} 首歌曲", Modifier.padding(top = 10.dp),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MusicDialogButton("查看唱片", onClick = { searchOpen = false })
                if (query.isNotBlank()) MusicDialogButton("清空搜索", onClick = { query = "" })
                }
            }
        }
    }
}

@Composable private fun ShelfCircle(label: String, tag: String, onClick: () -> Unit,
    enabled: Boolean = true, size: androidx.compose.ui.unit.Dp = 48.dp, content: @Composable () -> Unit) {
    UpdateGlassButton(tag, label, onClick, enabled = enabled, buttonSize = size, content = content)
}

/** 底部圆钮（任务 3）：只留图标 —— 玻璃面/描边全删，白色 tint 由图标自带，
 *  按压缩放反馈保留（净缩放 0.9 回弹 1.0）。 */
@Composable private fun ShelfIconAction(label: String, tag: String, onClick: () -> Unit,
    enabled: Boolean = true, size: androidx.compose.ui.unit.Dp = 48.dp, content: @Composable () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    // 真液态（对齐排序芯片）：ElasticPress 拽动果冻 + 玻璃半透明白 .14 面 + 白 .26 细描边 + 按压高光
    val pressS = com.HyperX.MI.Na.nativeui.rememberElasticPressState()
    val jellyS = remember { Animatable(1f) }
    LaunchedEffect(pressS) {
        snapshotFlow { pressS.amount > .45f }.distinctUntilChanged().collect { down ->
            if (down) jellyS.animateTo(.92f, spring(dampingRatio = .62f, stiffness = 620f))
            else {
                jellyS.animateTo(1.06f, spring(dampingRatio = .38f, stiffness = 380f))
                jellyS.animateTo(1f, spring(dampingRatio = .55f, stiffness = 300f))
            }
        }
    }
    Box(Modifier.size(size).testTag(tag).semantics { contentDescription = label }
        .clickable(enabled = enabled, role = androidx.compose.ui.semantics.Role.Button,
            interactionSource = interactions, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).graphicsLayer {
            val amount = pressS.amount.coerceIn(0f, 1f)
            val net = jellyS.value / (1f + amount * .08f)
            scaleX = net; scaleY = net
            translationX = pressS.pull.x * .3f
            translationY = pressS.pull.y * .3f
        }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) .14f else .07f))
            .drawBehind { if (pressS.amount > 0f) drawCircle(Color.White.copy(alpha = .06f * pressS.amount.coerceIn(0f, 1f))) }
            .border(1.dp, Color.White.copy(alpha = if (enabled) .26f else .12f), CircleShape),
            contentAlignment = Alignment.Center) { content() }
    }
}

/** 抽出观察文字按钮（任务 4）：玻璃底胶囊 + 文字「抽出观察」+ 小唱片图标 + 按压缩放反馈。 */
@Composable private fun ShelfTextAction(label: String, tag: String, onClick: () -> Unit,
    enabled: Boolean = true) {
    val interactions = remember { MutableInteractionSource() }
    // 真液态（对齐排序芯片）：ElasticPress 拽动果冻 + 按压高光（玻璃面 .14 与描边原有保留）
    val pressS = com.HyperX.MI.Na.nativeui.rememberElasticPressState()
    val jellyS = remember { Animatable(1f) }
    LaunchedEffect(pressS) {
        snapshotFlow { pressS.amount > .45f }.distinctUntilChanged().collect { down ->
            if (down) jellyS.animateTo(.92f, spring(dampingRatio = .62f, stiffness = 620f))
            else {
                jellyS.animateTo(1.06f, spring(dampingRatio = .38f, stiffness = 380f))
                jellyS.animateTo(1f, spring(dampingRatio = .55f, stiffness = 300f))
            }
        }
    }
    Row(Modifier.height(36.dp).testTag(tag).semantics { contentDescription = label }
        .graphicsLayer {
            val amount = pressS.amount.coerceIn(0f, 1f)
            val net = jellyS.value / (1f + amount * .08f)
            scaleX = net; scaleY = net
            translationX = pressS.pull.x * .3f
            translationY = pressS.pull.y * .3f
        }
        .clip(RoundedCornerShape(50))
        .background(Color.White.copy(alpha = if (enabled) .14f else .07f))
        .drawBehind { if (pressS.amount > 0f) drawRect(Color.White.copy(alpha = .06f * pressS.amount.coerceIn(0f, 1f))) }
        .border(1.dp, Color.White.copy(alpha = if (enabled) .38f else .16f), RoundedCornerShape(50))
        .clickable(enabled = enabled, role = androidx.compose.ui.semantics.Role.Button,
            interactionSource = interactions, indication = null, onClick = onClick)
        .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)) {
        ShelfGlyph("disc", Modifier.size(13.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1,
            color = Color.White.copy(alpha = if (enabled) .95f else .45f))
    }
}

/** The four control silhouettes follow the reference, with native actions behind each one. */
@Composable private fun ShelfGlyph(kind: String, modifier: Modifier, color: Color = Color.White, filled: Boolean = false) {
    Canvas(modifier) {
        val u = size.width / 24f
        val stroke = 1.7f * u
        fun line(x: Float, y: Float, xx: Float, yy: Float) =
            drawLine(color, Offset(x * u, y * u), Offset(xx * u, yy * u), stroke, StrokeCap.Round)
        fun path(vararg p: Float, close: Boolean = false) {
            val shape = Path().apply {
                moveTo(p[0] * u, p[1] * u)
                for (i in 2 until p.size step 2) lineTo(p[i] * u, p[i + 1] * u)
                if (close) close()
            }
            drawPath(shape, color, style = Stroke(stroke, cap = StrokeCap.Round))
            if (filled && close) drawPath(shape, color.copy(alpha = .35f))
        }
        when (kind) {
            "bookmark" -> path(6f, 3f, 18f, 3f, 18f, 22f, 12f, 17f, 6f, 22f, close = true)
            "collection" -> {
                path(3f, 8f, 21f, 8f, 21f, 22f, 3f, 22f, close = true)
                line(5f, 5f, 19f, 5f); line(7f, 2f, 17f, 2f)
                line(7f, 8f, 7f, 22f); line(17f, 8f, 17f, 22f)
                for (y in listOf(12f, 17f)) { line(3f, y, 7f, y); line(17f, y, 21f, y) }
            }
            "sliders" -> for ((y, x) in listOf(5f to 15f, 12f to 8f, 19f to 16f)) {
                line(2f, y, x - 2.5f, y); line(x + 2.5f, y, 22f, y)
                drawCircle(color, 2.4f * u, Offset(x * u, y * u), style = Stroke(stroke))
            }
            "shuffle" -> {
                path(2f, 6f, 6f, 6f, 17f, 18f, 22f, 18f)
                path(2f, 18f, 6f, 18f, 17f, 6f, 22f, 6f)
                path(19f, 3f, 22f, 6f, 19f, 9f); path(19f, 15f, 22f, 18f, 19f, 21f)
            }
            "close" -> {
                line(6f, 6f, 18f, 18f)
                line(18f, 6f, 6f, 18f)
            }
            "disc" -> {
                drawCircle(color, 9f * u, Offset(12f * u, 12f * u), style = Stroke(stroke))
                drawCircle(color, 2.4f * u, Offset(12f * u, 12f * u), style = Stroke(stroke))
            }
        }
    }
}

/**
 * 底部刻度尺拖动条（可拖动版）：
 * 横向细条 + 每 10% 一个均匀灰白小刻度点 + 已播段白→暖黄渐变线 +
 * 菱形播放头（旋转 45° 方块，非圆点）；无灰晕（恒亮辉光层已删，只留清晰细条）。
 * 手势：awaitEachGesture 完整拖动 —— 按下即预览、拖动跟手、松手 seek（与 CmxSeekBar 语义一致）。
 */
@Composable private fun RulerSeekBar(value: Float, onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f) {
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-6f)
    // 拖动预览值：非空时播放头跟手指走，松手才真正 seek
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val shown = dragValue ?: value
    val fraction = ((shown - valueRange.start) / span).coerceIn(0f, 1f)
    val seek by rememberUpdatedState(onSeek)
    Box(modifier.fillMaxWidth().semantics {
        progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
        contentDescription = "播放进度"
    }
        .pointerInput(enabled, valueRange) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val width = size.width.coerceAtLeast(1).toFloat()
                val startX = down.position.x
                var moved = false
                // 按下即预览落点（跟 GlowSeekBar「点按直达」一致，先预览不生效）
                dragValue = valueRange.start + (startX / width).coerceIn(0f, 1f) * span
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        // 松手：拖过（或原地点按）都按最终位置 seek
                        change.consume()
                        dragValue?.let(seek)
                        dragValue = null
                        break
                    }
                    val dx = change.position.x - startX
                    if (!moved && abs(dx) > viewConfiguration.touchSlop) moved = true
                    if (moved) {
                        change.consume()
                        dragValue = valueRange.start + (change.position.x / width).coerceIn(0f, 1f) * span
                    }
                }
            }
        }) {
        // 清晰层：细条 + 刻度点 + 白黄渐变线 + 菱形播放头（灰晕 blur 发光层已按需求删除）
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height / 2f
            val end = size.width * fraction
            val stroke = 2.dp.toPx()
            // 未播段基线细条（任务 7 底部控件提亮：白 .32）
            drawLine(Color.White.copy(alpha = .32f), Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
            // 已播段白→暖黄渐变线
            if (end > 0f) drawLine(Brush.horizontalGradient(
                listOf(Color.White, Color(0xFFF0D970)), startX = 0f, endX = maxOf(end, 1f)),
                Offset(0f, y), Offset(end, y), stroke, StrokeCap.Round)
            // 均匀刻度点：每 10% 一个（任务 7 底部控件提亮：未播 .5 / 已播 .8）
            for (tick in 0..10) {
                val x = size.width * tick / 10f
                drawCircle(Color.White.copy(alpha = if (x <= end) .8f else .5f), 1.2.dp.toPx(), Offset(x, y))
            }
            // 菱形播放头：45° 旋转方块，暖黄体 + 白描边
            val head = 4.6.dp.toPx()
            val diamond = Path().apply {
                moveTo(end, y - head); lineTo(end + head, y)
                lineTo(end, y + head); lineTo(end - head, y); close()
            }
            drawPath(diamond, Color(0xFFF0D970))
            drawPath(diamond, Color.White, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

/** 三段式播放控制（任务 3）：只留三个图标 —— 胶囊底（半透明白面 + 描边 + 按压高光）全删；
 *  按压反馈保留：任一钮按下整块微缩 + 图标自身轻缩。 */
@Composable private fun ShelfTransportCapsule(width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp, enabled: Boolean, playing: Boolean,
    onPrevious: () -> Unit, onTogglePlay: () -> Unit, onNext: () -> Unit) {
    val prevKey = remember { MutableInteractionSource() }
    val toggleKey = remember { MutableInteractionSource() }
    val nextKey = remember { MutableInteractionSource() }
    val p1 by prevKey.collectIsPressedAsState()
    val p2 by toggleKey.collectIsPressedAsState()
    val p3 by nextKey.collectIsPressedAsState()
    // 任一钮被按压 → 整块微缩（不再有胶囊面，反馈走纯缩放）
    val dent by animateFloatAsState(if (p1 || p2 || p3) 1f else 0f, tween(120), label = "capsule dent")
    Box(Modifier.width(width).height(height)
        .graphicsLayer { val s = 1f - .035f * dent; scaleX = s; scaleY = s },
        contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            CapsuleKey(prevKey, enabled, Modifier.weight(1f), onPrevious) {
                // 三段式按键图标 = 全屏页面同款（任务 4）：player_symbol_skip_previous_fill1
                Image(painterResource(com.HyperX.MI.Na.R.drawable.player_symbol_skip_previous_fill1), null,
                    Modifier.size(height * .46f), colorFilter = ColorFilter.tint(Color.White))
            }
            CapsuleKey(toggleKey, enabled, Modifier.weight(1.2f), onTogglePlay) {
                Box(Modifier.size(height * .78f).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.White), contentAlignment = Alignment.Center) {
                    // 同款播放/暂停矢量（play_arrow_fill1 / pause_fill1，180ms 交叉淡入）
                    ShelfToggleGlyph(playing, height * .5f, Color(0xFF0E1420))
                }
            }
            CapsuleKey(nextKey, enabled, Modifier.weight(1f), onNext) {
                Image(painterResource(com.HyperX.MI.Na.R.drawable.player_symbol_skip_next_fill1), null,
                    Modifier.size(height * .46f), colorFilter = ColorFilter.tint(Color.White))
            }
        }
    }
}

/** 播放/暂停同款矢量（PlayerControls 同一 drawables，交叉淡入与全屏页一致）。 */
@Composable private fun ShelfToggleGlyph(playing: Boolean, iconSize: androidx.compose.ui.unit.Dp, color: Color) {
    val play = painterResource(com.HyperX.MI.Na.R.drawable.player_symbol_play_arrow_fill1)
    val pause = painterResource(com.HyperX.MI.Na.R.drawable.player_symbol_pause_fill1)
    val glyph by animateFloatAsState(if (playing) 1f else 0f,
        tween(180, easing = androidx.compose.animation.core.LinearEasing), label = "shelf toggle")
    Canvas(Modifier.size(iconSize)) {
        val tint = ColorFilter.tint(color)
        with(play) { draw(size, alpha = 1f - glyph, colorFilter = tint) }
        with(pause) { draw(size, alpha = glyph, colorFilter = tint) }
    }
}

/** 胶囊内单键：图标按压轻缩（视觉反馈），点击回调给真实播放控制。 */
@Composable private fun CapsuleKey(interactions: MutableInteractionSource, enabled: Boolean,
    modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    // 真液态（对齐排序芯片）：ElasticPress 拽动果冻（胶囊玻璃面由父级提供）
    val pressS = com.HyperX.MI.Na.nativeui.rememberElasticPressState()
    val jellyS = remember { Animatable(1f) }
    LaunchedEffect(pressS) {
        snapshotFlow { pressS.amount > .45f }.distinctUntilChanged().collect { down ->
            if (down) jellyS.animateTo(.92f, spring(dampingRatio = .62f, stiffness = 620f))
            else {
                jellyS.animateTo(1.06f, spring(dampingRatio = .38f, stiffness = 380f))
                jellyS.animateTo(1f, spring(dampingRatio = .55f, stiffness = 300f))
            }
        }
    }
    Box(modifier.fillMaxHeight().graphicsLayer {
        val amount = pressS.amount.coerceIn(0f, 1f)
        val net = jellyS.value / (1f + amount * .08f)
        scaleX = net; scaleY = net
        translationX = pressS.pull.x * .3f
        translationY = pressS.pull.y * .3f
    }
        .clickable(enabled = enabled, role = androidx.compose.ui.semantics.Role.Button,
            interactionSource = interactions, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center) { content() }
}

