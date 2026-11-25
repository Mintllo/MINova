// ============================================================================
//  PlaylistVRSpace.kt —— 歌单详情「VR 展馆走廊」完全重构
//
//  相机站在一条 3D 展馆走廊里，视线朝走廊深处（灭点在屏幕中上部）。
//  歌曲 = 两侧墙上的展品：封面像画作挂在左右两墙，交错进深排列（左1 右2 左3 右4）。
//  向下滚动 = 相机沿走廊前进：展品从远处迎面放大逼近，到近处后从屏幕两侧滑出视野。
//
//  透视数学（每帧在 graphicsLayer 内读 scroll 快照，零重组）：
//    rel   = 展品与相机的相对进深（px）＝ item 自然偏移 + 引导进深
//    scale = near / (near + rel)        近大远小（针孔投影）
//    屏幕位置 = 灭点 + 墙面世界坐标 × scale   → 近处自然从两侧滑出
//    alpha = 雾效(fog) × 过镜(pass)：远小暗透、近大亮清、穿身后淡出
//    rotationY = ∓侧向角：左墙朝右倾（面向走廊中心），右墙反之
//
//  性能红线：
//    全部透视/雾效在 graphicsLayer 内逐帧读 LazyListState 快照，零重组；
//    LazyColumn 保留项回收；地面/墙面网格单 Canvas；无新依赖；无 blur；
//    同时大尺寸展品 ≤ 3 张（远处的由 GPU 小尺寸绘制 + 雾效压暗）。
// ============================================================================
package com.HyperX.MI.Na

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

// ---- 走廊参数 ----
private const val VrTwoPi = 6.2831853f
/** 相机每前进一格的进深（也 = LazyColumn item 高） */
private val VrSlotDp = 232.dp
/** 画框尺寸 */
private val VrCardW = 212.dp
private val VrCardH = 262.dp
/** 针孔相机近面距离（相对格深） */
private const val VrNearK = 1.15f
/** 首展品起始进深（相机与入口的间距） */
private const val VrLeadK = 0.85f
/** 墙面倾角：基础角 + 过镜加深 */
private const val VrTiltBase = 20f
private const val VrTiltPass = 14f
/** 合成波单色网格线 */
private val VrGridColor = Color(0xFF67D8F2)
/** 展品点亮/标牌描边 */
private val VrAccent = Color(0xFF8FE8FF)

/** 展品侧别：偶数序号挂左墙，奇数挂右墙（左1 右2 左3 右4） */
private fun vrSide(index: Int): Float = if (index % 2 == 0) -1f else 1f

private fun PlaylistSong.vrToSong(): Song = Song(
    id = id,
    name = name,
    artist = artist,
    album = album,
    duration = duration,
    picUrl = picUrl,
    source = source,
)

/**
 * VR 展馆走廊主入口：替换原 PlaylistDetailImmersive。
 * 数据流不变（playExternalSong 播放 + 整张歌单作队列），交互全部保留。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlaylistVRSpace(
    detail: PlaylistInfo,
    viewModel: PlayerViewModel,
    downloadedSongs: List<Song>,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSongMenu: (PlaylistSong) -> Unit,
) {
    val songs = detail.songs
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playingSongId = currentSong?.id
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // 进页推进：相机从走廊口推进去（展品整体加一段进深 → 灭点前移感）
    val entry = remember { Animatable(0f) }
    val entryValue = { entry.value }
    LaunchedEffect(songs) {
        // 打开即按当前播放歌定位（相机直接停在那件展品前）
        val idx = songs.indexOfFirst { it.id == playingSongId }
        if (idx > 0) listState.scrollToItem(idx)
        entry.animateTo(1f, tween(760, easing = FastOutSlowInEasing))
    }

    // 悬浮相位：单实例无限过渡，graphicsLayer 内读取 → 只更新图层不重组
    val bob = rememberInfiniteTransition(label = "vrBob")
    val bobPhase = bob.animateFloat(
        initialValue = 0f,
        targetValue = VrTwoPi,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
        label = "vrBobPhase"
    )

    val slotPx = with(density) { VrSlotDp.toPx() }
    val cardHpx = with(density) { VrCardH.toPx() }
    val nearPx = slotPx * VrNearK
    val leadPx = slotPx * VrLeadK
    val fogDepth = slotPx * 3.2f

    // 前方展品序号：只有跨过中线才变化（底部页码/点按判定用）
    val nearestIdx by remember(slotPx) {
        derivedStateOf {
            val half = (slotPx * 0.5f).toInt()
            listState.firstVisibleItemIndex + if (listState.firstVisibleItemScrollOffset > half) 1 else 0
        }
    }

    fun playSong(song: PlaylistSong) {
        viewModel.playExternalSong(
            song.vrToSong(),
            song.source.ifBlank { detail.source.apiSourceName() ?: "netease" },
            // 整张歌单作为播放队列 → 播完自动续下一首
            songs.map { it.vrToSong() }
        )
    }
    val pickRandom: () -> Unit = {
        if (songs.isNotEmpty()) {
            val idx = Random.nextInt(songs.size)
            playSong(songs[idx])
            scope.launch { listState.animateScrollToItem(idx) }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wallXpx = with(density) { (maxWidth * 0.58f).toPx() }
        val vpYpx = with(density) { (maxHeight * 0.38f).toPx() }
        // 底部留白：让最后一件展品也能走到相机面前再滑出
        val endPad = (maxHeight - VrSlotDp * 0.75f).coerceAtLeast(0.dp)

        // —— 走廊本体（进页整体 0.7→1 推进）——
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = entry.value
                    val s = 0.7f + 0.3f * p
                    scaleX = s
                    scaleY = s
                }
        ) {
            // 地面 + 墙面网格：单 Canvas，滚动时网格向后流动
            VrCorridorCanvas(
                listState = listState,
                slotPx = slotPx,
                nearPx = nearPx,
                wallXpx = wallXpx,
                modifier = Modifier.fillMaxSize()
            )
            if (songs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("歌单是空的", fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "可以在搜索页长按歌曲，选择「加入歌单」",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 0.dp, bottom = endPad)
                ) {
                    itemsIndexed(songs, key = { _, s -> s.id }) { i, song ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(VrSlotDp)
                                // 深处展品先绘制：远处压在近处之下（正确的展厅遮挡）
                                .zIndex(-i.toFloat()),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            VrPainting(
                                index = i,
                                song = song,
                                isPlayingHere = playingSongId != null && playingSongId == song.id,
                                isPlaying = isPlaying,
                                downloaded = downloadedSongs.any { it.id == song.id },
                                listState = listState,
                                entryValue = entryValue,
                                bobPhase = bobPhase,
                                slotPx = slotPx,
                                cardHpx = cardHpx,
                                nearPx = nearPx,
                                leadPx = leadPx,
                                fogDepth = fogDepth,
                                wallXpx = wallXpx,
                                vpYpx = vpYpx,
                                onClick = {
                                    if (i == nearestIdx) playSong(song)
                                    // 点两侧半出/远处的展品 = 相机走到那件面前
                                    else scope.launch { listState.animateScrollToItem(i) }
                                },
                                onLongClick = { onSongMenu(song) }
                            )
                        }
                    }
                }
            }
        }

        // —— 入口上方立体标牌：进页从深处飞来定格 ——
        VrTitleSign(
            name = detail.name,
            count = songs.size,
            entryValue = entryValue,
            bobPhase = bobPhase,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 62.dp)
        )

        // —— 相机近处 HUD：返回 / 删除 / 播放全部（贴屏 + 滚动微视差）——
        VrHudBar(
            songsCount = songs.size,
            listState = listState,
            slotPx = slotPx,
            entryValue = entryValue,
            onBack = onBack,
            onDelete = onDelete,
            onPlayAll = { songs.firstOrNull()?.let { playSong(it) } }
        )

        // —— 底部：帮我挑一首 + 当前展品页码 ——
        if (songs.isNotEmpty()) {
            VrBottomHud(
                nearestIdx = nearestIdx,
                total = songs.size,
                listState = listState,
                slotPx = slotPx,
                onPick = pickRandom,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp)
            )
        }
    }
}

/**
 * 单张墙面展品：画框（封面满铺 + 底部歌名/歌手）+ 框下小铭牌（序号）。
 * 全部透视状态在单个 graphicsLayer 内逐帧计算（读 scroll 快照，不重组）：
 * scale = near/(near+rel)；雾效压暗压透；过镜后从两侧滑出并淡出；
 * 正在播放 = 展品点亮（描边变亮 + 压暗变浅 + 黑胶从右侧滑入 + 轻微悬浮）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VrPainting(
    index: Int,
    song: PlaylistSong,
    isPlayingHere: Boolean,
    isPlaying: Boolean,
    downloaded: Boolean,
    listState: LazyListState,
    entryValue: () -> Float,
    bobPhase: State<Float>,
    slotPx: Float,
    cardHpx: Float,
    nearPx: Float,
    leadPx: Float,
    fogDepth: Float,
    wallXpx: Float,
    vpYpx: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val side = vrSide(index)
    val glow by animateColorAsState(
        targetValue = if (isPlayingHere) VrAccent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.14f),
        animationSpec = tween(320),
        label = "vrGlow"
    )
    val scrim by animateFloatAsState(
        targetValue = if (isPlayingHere) 0.30f else 0.62f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "vrScrim"
    )
    val vinylSlide by animateFloatAsState(
        targetValue = if (isPlayingHere) 0f else 1f,
        animationSpec = tween(340, easing = FastOutSlowInEasing),
        label = "vrVinylSlide"
    )
    val spin = rememberInfiniteTransition(label = "vrVinylSpin")
    val vinylAng by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "vrVinylAng"
    )

    Box(
        Modifier
            .width(VrCardW)
            .height(VrCardH)
            .graphicsLayer {
                val camZ = listState.firstVisibleItemIndex * slotPx + listState.firstVisibleItemScrollOffset
                val naturalTop = index * slotPx - camZ
                var rel = naturalTop + leadPx
                // 进页推进：展品从更深处飞来
                rel += (1f - entryValue()) * slotPx * 2.4f
                val relC = rel.coerceAtLeast(-0.32f * slotPx)
                val s = (nearPx / (nearPx + relC)).coerceIn(0.16f, 1.26f)
                val fogT = (1f - rel / fogDepth).coerceIn(0f, 1f)
                val passT = ((rel + slotPx * 0.38f) / (slotPx * 0.38f)).coerceIn(0f, 1f)
                scaleX = s
                scaleY = s
                alpha = (0.16f + 0.84f * fogT) * passT
                // 左墙朝右倾（面向走廊中心），右墙反之；过镜时侧角加深
                rotationY = -side * (VrTiltBase + VrTiltPass * (1f - fogT))
                cameraDistance = 9f * density
                translationX = side * wallXpx * s
                translationY = vpYpx - (naturalTop + cardHpx / 2f) +
                    (if (isPlayingHere) 2.5.dp.toPx() * sin(bobPhase.value * 1.15f + index) else 0f)
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141034).copy(alpha = 0.55f))
            .border(1.5.dp, glow, RoundedCornerShape(14.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(7.dp)
    ) {
        Column {
            // 画作：封面满铺
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(VrCardW)
                    .clip(RoundedCornerShape(9.dp))
            ) {
                if (song.picUrl.isNotBlank()) {
                    AsyncImage(
                        model = song.picUrl,
                        contentDescription = song.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.fallback_cover),
                        contentDescription = song.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
                // 底部渐晕：播放中的展品「亮起」（压暗变浅）
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0.42f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.30f + 0.50f * scrim)
                            )
                        )
                )
                if (downloaded) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            "已下载",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPlayingHere && isPlaying) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = VrAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            song.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            song.artist.ifBlank { "未知歌手" },
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.66f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            // 框下小铭牌：序号
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "No.%02d".format(index + 1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (isPlayingHere) VrAccent else Color.White.copy(alpha = 0.72f),
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        // 黑胶：播放时从右侧滑入并旋转
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .graphicsLayer {
                    translationX = vinylSlide * 74.dp.toPx()
                    rotationZ = vinylAng
                }
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF0C0C16).copy(alpha = 0.92f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                val r = size.minDimension / 2f
                for (k in 1..3) {
                    drawCircle(
                        Color.White.copy(alpha = 0.10f),
                        radius = r * (0.40f + 0.16f * k),
                        center = c,
                        style = Stroke(1.dp.toPx())
                    )
                }
                drawCircle(VrAccent.copy(alpha = 0.85f), radius = r * 0.24f, center = c)
                drawCircle(Color(0xFF0C0C16), radius = r * 0.06f, center = c)
            }
        }
    }
}

/**
 * 走廊单 Canvas：合成波单色细线网格。
 * 横线（地面）按透视间距向观者流动；纵线汇聚到灭点；墙基线/墙顶线勾勒走廊轮廓。
 * draw lambda 内读 LazyListState 快照 → 只随滚动重绘，不重组。
 */
@Composable
private fun VrCorridorCanvas(
    listState: LazyListState,
    slotPx: Float,
    nearPx: Float,
    wallXpx: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val vpx = w / 2f
        val vpy = h * 0.38f
        val floorDrop = h - vpy
        val camZ = listState.firstVisibleItemIndex * slotPx + listState.firstVisibleItemScrollOffset
        val line = 1.dp.toPx()

        // 灭点辉光
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(VrGridColor.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(vpx, vpy),
                radius = w * 0.55f
            )
        )
        // 两侧墙幕压暗（走廊围合感）
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color(0xFF060514).copy(alpha = 0.5f), Color.Transparent)
            ),
            size = Size(w * 0.26f, h)
        )
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color(0xFF060514).copy(alpha = 0.5f))
            ),
            topLeft = Offset(w * 0.74f, 0f),
            size = Size(w * 0.26f, h)
        )
        // 地面横线：透视间距 + 随滚动向观者流动
        val grid = slotPx * 0.5f
        var d = grid - (camZ % grid)
        var n = 0
        while (n < 30) {
            val s = nearPx / (nearPx + d)
            val y = vpy + floorDrop * s
            drawLine(
                VrGridColor.copy(alpha = 0.04f + 0.26f * s),
                Offset(0f, y),
                Offset(w, y),
                strokeWidth = line * (0.5f + 1.1f * s)
            )
            d += grid
            n++
        }
        // 汇聚纵线
        for (j in -7..7) {
            val fade = 1f - abs(j) / 9f
            drawLine(
                VrGridColor.copy(alpha = 0.03f + 0.13f * fade),
                Offset(vpx + j * (w / 7.5f), h),
                Offset(vpx, vpy),
                strokeWidth = line * 0.8f
            )
        }
        // 墙基线与墙顶线
        val wallTop = floorDrop * 0.92f
        drawLine(VrGridColor.copy(alpha = 0.30f), Offset(vpx - wallXpx, h), Offset(vpx, vpy), line)
        drawLine(VrGridColor.copy(alpha = 0.30f), Offset(vpx + wallXpx, h), Offset(vpx, vpy), line)
        drawLine(VrGridColor.copy(alpha = 0.18f), Offset(vpx - wallXpx, vpy - wallTop), Offset(vpx, vpy), line)
        drawLine(VrGridColor.copy(alpha = 0.18f), Offset(vpx + wallXpx, vpy - wallTop), Offset(vpx, vpy), line)
    }
}

/** 入口上方立体标牌：进页从深处（小 + 后仰）飞来定格，落定后缓慢悬浮。 */
@Composable
private fun VrTitleSign(
    name: String,
    count: Int,
    entryValue: () -> Float,
    bobPhase: State<Float>,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .graphicsLayer {
                    val p = entryValue()
                    val settle = p * p * (3f - 2f * p)
                    val sc = 0.42f + 0.58f * settle
                    scaleX = sc
                    scaleY = sc
                    rotationX = -26f * (1f - settle)
                    alpha = settle.coerceIn(0f, 1f)
                    translationY = -(1f - settle) * 54.dp.toPx() +
                        2.5.dp.toPx() * sin(bobPhase.value * 0.9f + 1.3f)
                    cameraDistance = 12f * density
                }
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.38f))
                .border(1.dp, VrAccent.copy(alpha = 0.30f), RoundedCornerShape(50))
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.MusicNote, null, tint = VrAccent, modifier = Modifier.size(16.dp))
            Text(
                name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 190.dp)
            )
            Text("$count 首", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

/** 顶部 HUD：返回 / 删除 / 播放全部。玻璃胶囊贴屏，滚动时带轻微视差。 */
@Composable
private fun VrHudBar(
    songsCount: Int,
    listState: LazyListState,
    slotPx: Float,
    entryValue: () -> Float,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onPlayAll: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .graphicsLayer {
                val frac = ((listState.firstVisibleItemIndex * slotPx +
                    listState.firstVisibleItemScrollOffset) / slotPx) % 1f
                translationX = (0.5f - frac) * 2.2.dp.toPx()
                val p = entryValue()
                val e = p * p * (3f - 2f * p)
                val sc = 0.92f + 0.08f * e
                scaleX = sc
                scaleY = sc
                alpha = 0.3f + 0.7f * e
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.36f))
                .border(1.dp, Color.White.copy(alpha = 0.13f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.36f))
                .border(1.dp, Color.White.copy(alpha = 0.13f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDelete
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Delete, "删除", tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.36f))
                .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(50))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPlayAll
                )
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("播放全部", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (songsCount > 0) {
                Spacer(Modifier.width(4.dp))
                Text("($songsCount)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
            }
        }
    }
}

/** 底部 HUD：帮我挑一首 + 当前展品页码胶囊（滚动微视差）。 */
@Composable
private fun VrBottomHud(
    nearestIdx: Int,
    total: Int,
    listState: LazyListState,
    slotPx: Float,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (total <= 0) 0f else (nearestIdx + 1f) / total,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "vrPageProgress"
    )
    Column(
        modifier.graphicsLayer {
            val frac = ((listState.firstVisibleItemIndex * slotPx +
                listState.firstVisibleItemScrollOffset) / slotPx) % 1f
            translationY = (frac - 0.5f) * 2.2.dp.toPx()
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.38f))
                .border(1.dp, VrAccent.copy(alpha = 0.28f), RoundedCornerShape(50))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPick
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Shuffle, null, tint = VrAccent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("帮我挑一首", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.34f))
                .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${(nearestIdx + 1).coerceAtMost(total)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(" / $total", fontSize = 12.sp, color = Color.White.copy(alpha = 0.58f))
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(progress.coerceIn(0f, 1f))
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                )
            }
        }
    }
}
