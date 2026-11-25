/*
 * MINova 3D 封面轮播（CoverFlow）：收藏列表 / 歌单详情顶部的「3D 选集区域」。
 * 由 CmxRecommendationDeck（卡片堆叠）换为 HorizontalPager 封面流：
 * 当前页大且正，相邻页缩小 + Y 轴旋转的镜像透视；侧页点击切换，当前页点击播放。
 * 缩放/透明度沿用 CmxDailyHero 已有的 3D 逻辑（exp(-depth*.20)、逐张 .14 淡出、
 * spring(.94,135) 落位），翻页动量由 Pager 自带的 VelocityTracker fling 提供。
 * 浏览手势只改题注，不换播放队列；播放仍由 onPlaySelected 显式触发。
 * 黑胶唱片从 CmxDailyHero Deck 搬运：正在播放的那首歌，唱片盘藏在其封面背后，
 * 播放时向右滑出边缘（spring .94/115 露出）并随帧时钟旋转（rememberCmxCoverRotation 30°/s）。
 */
package com.HyperX.MI.Na.cmx

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.HyperX.MI.Na.Song
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp

@Composable
fun CmxCoverCarousel(
    songs: List<Song>,
    isPlaying: Boolean,
    playingSongId: String?,
    onPlaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dateLabel: String = "",
) {
    Column(modifier.fillMaxWidth()) {
        if (dateLabel.isNotBlank()) {
            Text(
                dateLabel,
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .8f),
            )
        }
        if (songs.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(184.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("暂无歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        } else {
            val scope = rememberCoroutineScope()
            // 初始定位到当前播放歌：打开即视觉居中（contentPadding 两侧露出邻页，含第一首）。
            val initialIndex = songs.indexOfFirst { it.id == playingSongId }.takeIf { it >= 0 } ?: 0
            val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { songs.size })
            var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
            // 列表变化（搜索过滤 / 刷新 / 重排）后按当前播放歌重定位，避免“页码还在、歌换了”。
            LaunchedEffect(songs) {
                val index = songs.indexOfFirst { it.id == playingSongId }.takeIf { it >= 0 } ?: 0
                selectedId = songs[index].id
                if (pagerState.currentPage != index) pagerState.scrollToPage(index)
            }
            // 滑动浏览：越过中点（currentPage 变化）即切换底部题注；逐帧移动全在 graphicsLayer。
            LaunchedEffect(pagerState, songs) {
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    songs.getOrNull(page)?.let { if (it.id != selectedId) selectedId = it.id }
                }
            }
            val selectedIndex = songs.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
            val selected = songs.getOrNull(selectedIndex) ?: songs.first()
            val sameSong = playingSongId != null && playingSongId == selected.id

            BoxWithConstraints(
                Modifier.fillMaxWidth().semantics {
                    stateDescription = "第 ${pagerState.currentPage + 1} 首，共 ${pagerState.pageCount} 首"
                    customActions = listOf(
                        CustomAccessibilityAction("上一首") {
                            if (pagerState.pageCount > 1) {
                                val target = if (pagerState.currentPage == 0) pagerState.pageCount - 1 else pagerState.currentPage - 1
                                scope.launch { pagerState.animateScrollToPage(target) }
                                true
                            } else false
                        },
                        CustomAccessibilityAction("下一首") {
                            if (pagerState.pageCount > 1) {
                                val target = (pagerState.currentPage + 1) % pagerState.pageCount
                                scope.launch { pagerState.animateScrollToPage(target) }
                                true
                            } else false
                        },
                    )
                },
            ) {
                // 封面宽度约屏宽一半，两侧留出邻页透视（间距/缩放参考 CmxDailyHero 封面流）。
                val coverWidth = (maxWidth * .52f).coerceIn(150.dp, 208.dp)
                val pagerHeight = (coverWidth * .82f).coerceIn(126.dp, 168.dp)
                val sidePadding = ((maxWidth - coverWidth) / 2f).coerceAtLeast(0.dp)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(pagerHeight),
                    pageSize = PageSize.Fixed(coverWidth),
                    pageSpacing = 12.dp,
                    contentPadding = PaddingValues(horizontal = sidePadding),
                    beyondViewportPageCount = 1,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        // 与 Deck 同款落位弹簧：松手后带回弹吸附中页。
                        snapAnimationSpec = spring(dampingRatio = .94f, stiffness = 135f, visibilityThreshold = .001f),
                    ),
                ) { page ->
                    val song = songs[page]
                    val isCenter = page == pagerState.currentPage
                    // 修复封面流圆角跳变：前排 16dp / 侧页 12dp，240ms 补过渡（同 Deck）。
                    val corner by animateDpAsState(
                        targetValue = if (isCenter) 16.dp else 12.dp,
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                        label = "cmx carousel corner",
                    )
                    // 黑胶挂在「正在播放」的那一页（浏览离开后唱片跟着歌走，同 Deck 的 showRecord）。
                    val playingHere = playingSongId != null && playingSongId == song.id
                    val recordReveal by animateFloatAsState(
                        targetValue = if (playingHere && isPlaying) 1f else 0f,
                        animationSpec = spring(dampingRatio = .94f, stiffness = 115f, visibilityThreshold = .001f),
                        label = "cmx carousel record",
                    )
                    Box(
                        Modifier.fillMaxSize()
                            // 页框圆角底：封面圆角外不再露出下层内容（修复四角发黑）；
                            // background(带形状)只画底不裁剪子级，唱片仍能探出页框右缘。
                            
                            // 唱片盘滑出页框，会被右侧页盖住：播放中的页抬到邻页之上（同 Deck 的 zIndex）。
                            .zIndex(if (playingHere) 1f else 0f)
                            .graphicsLayer {
                                // >0 页在当前页左侧，<0 在右侧；深度截断到 ±1 页。
                                val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                    .coerceIn(-1f, 1f)
                                val depth = abs(offset)
                                val scale = exp(-depth * .20f)
                                scaleX = scale; scaleY = scale
                                rotationY = -offset * 30f
                                translationY = (1f - scale) * pagerHeight.toPx() * .35f
                                alpha = (1f - (depth - 1f).coerceAtLeast(0f) * .14f).coerceIn(.35f, 1f)
                                compositingStrategy = CompositingStrategy.ModulateAlpha
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        // 换歌时旧唱片不瞬间消失：reveal 弹簧滑回归位（藏回封面后）再移除
                        if (playingHere || recordReveal > .01f) {
                            // Deck 同款黑胶：藏在封面背后，播放时滑出右缘并旋转；暂停保持角度滑回。
                            // 盘径取页高（页框宽>高，避免唱片纵向溢出 Pager 视口）。
                            val recordRotation = rememberCmxCoverRotation(song.id, isPlaying)
                            CmxVinylRecord(
                                MaterialTheme.colorScheme.primary, song.picUrl,
                                Modifier.size(pagerHeight).align(Alignment.Center).graphicsLayer {
                                    translationX = pagerHeight.toPx() * .52f * recordReveal
                                    rotationZ = recordRotation.value
                                },
                            )
                        }
                        CmxCarouselArtwork(
                            song.picUrl,
                            // 圆角/点击收在封面上（同 Deck）：页框不再 clip，唱片才探得出页框右缘。
                            Modifier.fillMaxSize()
                                .clip(RoundedCornerShape(corner))
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = if (isCenter) "播放${song.name}" else "切换到${song.name}",
                                ) {
                                    if (isCenter) onPlaySelected(page)
                                    else scope.launch { pagerState.animateScrollToPage(page) }
                                }
                                .semantics { contentDescription = song.name },
                        )
                    }
                }
            }
            Crossfade(
                targetState = selectedIndex,
                animationSpec = tween(240, easing = FastOutSlowInEasing),
                label = "cmx carousel caption",
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { captionIndex ->
                val song = songs.getOrNull(captionIndex) ?: selected
                val playing = playingSongId != null && playingSongId == song.id && isPlaying
                Column(
                    Modifier.fillMaxWidth().padding(top = 6.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (playing) {
                            Icon(
                                Icons.Rounded.GraphicEq, null,
                                Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            song.name,
                            fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        song.artist.ifBlank { song.album },
                        Modifier.padding(top = 3.dp),
                        fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CmxCarouselArtwork(cover: String, modifier: Modifier = Modifier) {
    Box(
        modifier,
        contentAlignment = Alignment.Center,
    ) {
        val placeholderInk = MaterialTheme.colorScheme.onSurface.copy(alpha = .16f)
        Canvas(Modifier.size(28.dp)) {
            // 封面占位：淡墨音符点（与 CmxDeckArtwork 占位视觉一致）。
            drawCircle(placeholderInk, radius = size.minDimension * .16f)
        }
        if (cover.isNotBlank()) {
            val context = LocalContext.current
            val model = remember(context, cover) {
                ImageRequest.Builder(context).data(cover).size(480).build()
            }
            AsyncImage(model, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}
