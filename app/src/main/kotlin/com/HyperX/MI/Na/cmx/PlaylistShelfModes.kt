// 歌单详情两种陈列模式（双模式重设计）：堆叠（模式 1 = 原生纵向 3D 唱片架，RecordShelfView 原生 View）
// / 简洁（模式 3 = 原始 MD3 列表）。FLOW 平铺模式已按需求删除。
// 数据源不变：detail.songs；播放不变：viewModel.playExternalSong（整张歌单作队列）。
// STACK 的卡片层由 PlaylistRecordShelf 内 AndroidView 托载的原生 RecordShelfView 承担；
// 模式切换 = STACK ↔ SIMPLE 直接过渡（简单 fade，无聚回堆叠编舞）。
package com.HyperX.MI.Na.cmx

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.HyperX.MI.Na.PlaylistInfo
import com.HyperX.MI.Na.PlaylistSong
import com.HyperX.MI.Na.PlayerViewModel as PlayViewModel
import com.HyperX.MI.Na.apiSourceName
import com.HyperX.MI.Na.nativeui.ElasticPress
import com.HyperX.MI.Na.nativeui.rememberElasticPressState

/** 歌单详情陈列模式：STACK=3D 堆叠唱片架（模式 1，原生 RecordShelfView），SIMPLE=简洁列表（模式 3）。FLOW 平铺已删除。 */
internal enum class PlaylistShelfMode(val label: String) {
    STACK("堆叠"),
    SIMPLE("简洁");
}

/** 详情右上两态切换器（任务 6 简洁化）：两枚并排小图标钮 —— 选中 primary 底 / 非选中透明，
 *  圆角 12dp、40dp 高，轻按压缩放；下方一行当前模式名（唯一的模式名标注）。 */
@Composable
internal fun PlaylistShelfModeSwitcher(
    mode: PlaylistShelfMode,
    onMode: (PlaylistShelfMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PlaylistShelfMode.entries.forEach { m ->
                val selected = m == mode
                val interactions = remember { MutableInteractionSource() }
                // 真液态（对齐排序芯片）：ElasticPress 拽动果冻 + 玻璃半透明白 .14 面 + 白 .26 细描边 + 按压高光
                val pressS = rememberElasticPressState()
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
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 40.dp)
                        .graphicsLayer {
                            val amount = pressS.amount.coerceIn(0f, 1f)
                            val net = jellyS.value / (1f + amount * .08f)
                            scaleX = net; scaleY = net
                            translationX = pressS.pull.x * .3f
                            translationY = pressS.pull.y * .3f
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = .14f))
                        .drawBehind { if (pressS.amount > 0f) drawRect(Color.White.copy(alpha = .06f * pressS.amount.coerceIn(0f, 1f))) }
                        .border(1.dp, Color.White.copy(alpha = .26f), RoundedCornerShape(12.dp))
                        .clickable(interactionSource = interactions, indication = null) { onMode(m) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (m) {
                            PlaylistShelfMode.STACK -> Icons.Rounded.Layers
                            PlaylistShelfMode.SIMPLE -> Icons.Rounded.MusicNote
                        },
                        contentDescription = "陈列模式：${m.label}",
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Text(
            mode.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ============================================================================
//  统一陈列场景：STACK = 原生 3D 唱片架（stack 槽 → CmxPlaylistShelf → PlaylistRecordShelf
//  内 AndroidView 托载 RecordShelfView）；SIMPLE = 普通列表（PlaylistSimpleList）。
//  状态机简化：STACK ↔ SIMPLE 直接过渡（简单 fade，无聚回堆叠编舞）。
// ============================================================================

@Composable
internal fun PlaylistShelfScene(
    detail: PlaylistInfo,
    viewModel: PlayViewModel,
    mode: PlaylistShelfMode,
    onSongLongPress: (PlaylistSong) -> Unit,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    stack: @Composable () -> Unit,
) {
    Crossfade(
        targetState = mode == PlaylistShelfMode.SIMPLE,
        modifier = modifier,
        animationSpec = tween(280),
        label = "shelf scene fade",
    ) { simple ->
        if (simple) {
            PlaylistSimpleList(
                detail = detail,
                viewModel = viewModel,
                onSongLongPress = onSongLongPress,
                onClose = onClose,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize()) { stack() }
        }
    }
}

// ============================================================================
//  模式 3：简洁模式 —— 原始界面（无 3D、无套无边框）：普通 MD3 歌曲列表 封面 + 歌名 + 歌手行。
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistSimpleList(
    detail: PlaylistInfo,
    viewModel: PlayViewModel,
    onSongLongPress: (PlaylistSong) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourceKey = detail.source.apiSourceName() ?: "netease"
    // 播放映射与唱片架一致：每首歌自带 source 优先（空才兜底歌单来源）
    val playSongs = remember(detail.songs, sourceKey) { detail.songs.map { it.toPlaySong(sourceKey) } }
    val currentSong by viewModel.currentSong.collectAsState()
    val playingId = currentSong?.id
    val listState = rememberLazyListState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val collapsePx = with(density) { 96.dp.toPx() }
    // 折叠进度（任务 2）：大标题滚出视口的程度，0 = 顶部大标题，1 = 收成顶栏小标题
    val collapse by remember(collapsePx) {
        androidx.compose.runtime.derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapsePx).coerceIn(0f, 1f)
        }
    }
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        // 顶部栏（任务 1）：返回 X 钮 + 歌单名 + 播放全部胶囊（与其他模式一致；右上模式切换器由页级照常叠加）
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "返回歌单列表",
                    tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                detail.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 2.dp)
                    .graphicsLayer { alpha = collapse },
            )
            // 播放全部胶囊：整张歌单作队列，从第一首起播（与堆叠模式点唱片播放同一条链路）
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = .14f))
                    .clickable(enabled = playSongs.isNotEmpty()) {
                        playSongs.firstOrNull()?.let {
                            viewModel.playExternalSong(it, sourceKey, playSongs)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Rounded.PlayArrow, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp),
                )
                Text(
                    "播放全部",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (detail.songs.isEmpty()) {
            BoardHint("这张歌单还没有歌曲", Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
            ) {
                item(key = "simple_header") {
                    // 可折叠大标题（任务 2）：滚动时上移淡出 + 0.5x 视差（比列表滚得慢），
                    // 收成顶栏小标题；LargeTopAppBar 风格的轻量自制实现
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = listState.firstVisibleItemScrollOffset * .5f
                                alpha = 1f - collapse
                            }
                            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp),
                    ) {
                        Text(
                            detail.name,
                            fontSize = 28.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${detail.songs.size} 首",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                itemsIndexed(detail.songs) { index, ps ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    playSongs.getOrNull(index)?.let {
                                        viewModel.playExternalSong(it, sourceKey, playSongs)
                                    }
                                },
                                onLongClick = { onSongLongPress(ps) },
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = ps.picUrl,
                            contentDescription = ps.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(
                                ps.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (ps.id == playingId) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                listOf(ps.artist, ps.album).filter(String::isNotBlank).joinToString(" · "),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (ps.id == playingId) {
                            Icon(
                                Icons.Rounded.MusicNote, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp).size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.statusBarsPadding().padding(top = 96.dp),
        )
    }
}
