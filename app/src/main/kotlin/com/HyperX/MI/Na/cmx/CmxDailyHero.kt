/*
 * Ported from CurrentMusicX (https://github.com/bileizhen/CurrentMusicX)
 * original package: io.github.currencortex.music.feature.home (DailyRecommendationHero.kt)
 * components: RecommendationCoverFlow / VinylRecord / DeckArtwork / LoadingRecommendationHero
 * License: GPL-3.0 (upstream). Adapted for MINova — MINova Song + play callbacks;
 * the browsing gesture never changes the playback queue, only the explicit play action does.
 */
package com.HyperX.MI.Na.cmx

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.HyperX.MI.Na.Song
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import java.lang.Math.floorMod
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.tanh

/** 帧时钟驱动的封面旋转：播放时 30°/s，暂停保持角度，换歌归零（MiniPlayer.kt rememberCoverRotation）。 */
@Composable
fun rememberCmxCoverRotation(songId: String?, playing: Boolean): androidx.compose.runtime.State<Float> {
    val angle = remember(songId) { androidx.compose.runtime.mutableFloatStateOf(0f) }
    LaunchedEffect(songId, playing) {
        if (playing && songId != null) {
            var last = androidx.compose.animation.core.withInfiniteAnimationFrameNanos { it }
            while (true) {
                val frame = androidx.compose.animation.core.withInfiniteAnimationFrameNanos { it }
                angle.floatValue = (angle.floatValue + (frame - last).coerceAtMost(250_000_000L) / 1_000_000_000f * 30f) % 360f
                last = frame
            }
        }
    }
    return angle
}

/**
 * CMX 每日推荐 3D 滑动卡组：tanh/exp 深度公式 + 黑胶唱片滑出 + Crossfade 题注。
 * 数据/播放仍由 MINova 提供：onSelect 只改浏览选中，onPlaySelected(index) 才播放。
 */
@Composable
fun CmxRecommendationDeck(
    songs: List<Song>,
    isPlaying: Boolean,
    playingSongId: String?,
    onPlaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dateLabel: String = "",
    onSongLongPress: ((Song) -> Unit)? = null,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedIndex = songs.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
    val selected = songs.getOrNull(selectedIndex)
    LaunchedEffect(selected?.id) { selectedId = selected?.id }
    val sameSong = selected != null && playingSongId != null && playingSongId == selected.id
    val rotation = rememberCmxCoverRotation(selected?.id, isPlaying && sameSong)

    Column(modifier.fillMaxWidth()) {
        if (dateLabel.isNotBlank()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Text(
                    dateLabel, fontSize = 11.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .8f),
                )
            }
        }
        if (songs.isNotEmpty()) {
            val playSelected = { onPlaySelected(selectedIndex) }
            key(songs.map { it.id }) {
                CmxRecommendationCoverFlow(
                    songs = songs,
                    selected = selectedIndex,
                    onSelect = { selectedId = songs[it].id },
                    showRecord = sameSong && isPlaying,
                    recordRotation = rotation.value,
                    onPlaySelected = playSelected,
                    onSongLongPress = onSongLongPress,
                )
            }
            Crossfade(
                selected!!,
                Modifier.fillMaxWidth().heightIn(min = 52.dp),
                animationSpec = tween(240, easing = FastOutSlowInEasing),
                label = "cmx recommendation caption",
            ) { song ->
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    androidx.compose.material3.Text(
                        song.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    )
                    androidx.compose.material3.Text(
                        song.artist.ifBlank { song.album },
                        Modifier.padding(top = 4.dp), fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = .55f),
                    )
                }
            }
        } else {
            Box(
                Modifier.fillMaxWidth().height(184.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Text(
                    "正在挑选…",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun CmxRecommendationCoverFlow(
    songs: List<Song>,
    selected: Int,
    onSelect: (Int) -> Unit,
    showRecord: Boolean,
    recordRotation: Float,
    onPlaySelected: () -> Unit,
    onSongLongPress: ((Song) -> Unit)? = null,
) {
    val select by rememberUpdatedState(onSelect)
    val scope = rememberCoroutineScope()
    var targetPosition by remember { mutableFloatStateOf(selected.toFloat()) }
    val position = remember { Animatable(selected.toFloat()) }
    var release by remember { mutableIntStateOf(0) }
    var releaseVelocity by remember { mutableStateOf<Float?>(null) }
    var dragPosition by remember { mutableFloatStateOf(selected.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    val renderedPosition = remember { derivedStateOf { if (dragging) dragPosition else position.value } }
    // 只有跨过中点才改变组合/绘制顺序；逐帧移动全部在 graphicsLayer。
    val foreground = remember { derivedStateOf { renderedPosition.value.roundToInt() } }
    LaunchedEffect(selected, release) {
        val from = floorMod(targetPosition.roundToInt(), songs.size)
        val difference = selected - from
        val delta = when {
            difference > songs.size / 2 -> difference - songs.size
            difference < -songs.size / 2 -> difference + songs.size
            else -> difference
        }
        targetPosition += delta
        if (!dragging) {
            val velocity = releaseVelocity ?: position.velocity
            releaseVelocity = null
            position.animateTo(
                targetPosition,
                spring(dampingRatio = .94f, stiffness = 135f, visibilityThreshold = .001f),
                initialVelocity = velocity,
            )
        }
    }
    val recordReveal = animateFloatAsState(
        if (showRecord) 1f else 0f,
        spring(dampingRatio = .94f, stiffness = 115f, visibilityThreshold = .001f),
        label = "cmx recommendation record",
    )
    val density = LocalDensity.current
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .semantics {
                stateDescription = "第 ${selected + 1} 首，共 ${songs.size} 首"
                customActions = listOf(
                    CustomAccessibilityAction("上一首推荐") { if (songs.size > 1) { onSelect(floorMod(selected - 1, songs.size)); true } else false },
                    CustomAccessibilityAction("下一首推荐") { if (songs.size > 1) { onSelect((selected + 1) % songs.size); true } else false },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val coverSize = (maxWidth / 2.38f).coerceIn(128.dp, 184.dp)
        val gestureDistance = with(density) { (coverSize * .65f).toPx() }
        val stage = Modifier
            .fillMaxWidth()
            .height(coverSize + 40.dp)
            .pointerInput(songs.map { it.id }, gestureDistance) {
                var start = 0f
                var pulled = 0f
                var nearest = selected
                val velocityTracker = VelocityTracker()
                fun finish(cancelled: Boolean) {
                    val released = dragPosition
                    val velocity = if (cancelled || songs.size <= 1) 0f else -velocityTracker.calculateVelocity().x / gestureDistance
                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        position.snapTo(released)
                        targetPosition = released.roundToInt().toFloat()
                        if (!cancelled) {
                            // 保留释放速度，最多多带一张卡的动量。
                            targetPosition = (released + velocity * .12f).coerceIn(released - 1f, released + 1f).roundToInt().toFloat()
                            select(floorMod(targetPosition.roundToInt(), songs.size))
                        }
                        releaseVelocity = velocity
                        dragging = false
                        release++
                    }
                }
                detectHorizontalDragGestures(
                    onDragStart = {
                        start = renderedPosition.value; pulled = 0f; dragPosition = start; dragging = true
                        nearest = start.roundToInt()
                        velocityTracker.resetTracking()
                        scope.launch(start = CoroutineStart.UNDISPATCHED) { position.stop() }
                    },
                    onDragCancel = { finish(true) }, onDragEnd = { finish(false) },
                ) { change, amount ->
                    change.consume()
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    pulled += amount
                    if (songs.size > 1) {
                        dragPosition = start - pulled / gestureDistance
                        val next = dragPosition.roundToInt()
                        if (next != nearest) {
                            nearest = next
                            select(floorMod(next, songs.size))
                        }
                    }
                }
            }
        Box(stage, contentAlignment = Alignment.Center) {
            val visible = minOf(songs.size, 7)
            val centre = foreground.value
            for (slot in centre - (visible - 1) / 2..centre + visible / 2) key(slot) {
                val index = floorMod(slot, songs.size)
                // 新液态（任务 2）：ElasticPress 跟手微拽（果冻物理，Initial pass 观察不劫持滑动）
                val pressS = com.HyperX.MI.Na.nativeui.rememberElasticPressState()
                com.HyperX.MI.Na.nativeui.ElasticPress(
                    modifier = Modifier.zIndex(10f - abs(slot - centre)), state = pressS,
                ) {
                Box(
                    Modifier.size(coverSize).graphicsLayer {
                        val relative = slot - renderedPosition.value
                        val depth = abs(relative)
                        val scale = exp(-depth * .20f)
                        translationX = tanh(relative * .70f) * coverSize.toPx() * .98f
                        translationY = (1f - scale) * coverSize.toPx() * .42f
                        scaleX = scale; scaleY = scale
                        rotationZ = tanh(relative * .55f) * 12f
                        alpha = (1f - (depth - 1f).coerceAtLeast(0f) * .14f).coerceIn(.35f, 1f)
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    },
                ) {
                    if (index == selected) CmxVinylRecord(
                        accent, songs[index].picUrl,
                        Modifier.size(coverSize * .9f).align(Alignment.Center).graphicsLayer {
                            translationX = coverSize.toPx() * .52f * recordReveal.value
                            rotationZ = recordRotation
                        },
                    )
                    // 修复上游 bug：卡片滑动/选中切换时封面圆角直接跳变 ——
                    // 前排卡 16dp / 侧卡 12dp，240ms FastOutSlowIn 补上过渡动画
                    val isFront = index == selected
                    val cornerRadius by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (isFront) 16.dp else 12.dp,
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                        label = "cmx deck corner",
                    )
                    val artwork = Modifier.fillMaxSize().clip(RoundedCornerShape(cornerRadius))
                        .semantics { stateDescription = songs[index].name }
                        .combinedClickable(
                            enabled = true,
                            role = androidx.compose.ui.semantics.Role.Button,
                            onClickLabel = if (index == selected) "播放推荐歌曲" else "浏览${songs[index].name}",
                            onLongClickLabel = "查看歌曲详情",
                            onLongClick = { onSongLongPress?.invoke(songs[index]) },
                        ) {
                            if (index == selected) onPlaySelected() else select(index)
                        }
                    CmxDeckArtwork(songs[index].picUrl, artwork)
                    // 液态玻璃面（新液态）：半透明白 .14 面 + 按压高光 + 白 .26 细描边（随卡片圆角）
                    Box(
                        Modifier.matchParentSize()
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(Color.White.copy(alpha = 0.58f))
                            .drawBehind { if (pressS.amount > 0f) drawRect(Color.White.copy(alpha = .06f * pressS.amount.coerceIn(0f, 1f))) }
                            .border(1.dp, Color.White.copy(alpha = .26f), RoundedCornerShape(cornerRadius))
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun CmxDeckArtwork(cover: String, modifier: Modifier) {
    val context = LocalContext.current
    Box(
        modifier,
        contentAlignment = Alignment.Center,
    ) {
        val placeholderInk = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = .16f)
        Canvas(Modifier.size(28.dp)) {
            // 封面占位：淡墨音符点（与上游 MusicIcons.List 占位视觉等效的极简形）
            drawCircle(placeholderInk, radius = size.minDimension * .16f)
        }
        // 修复上游 bug：封面 URL 切换是硬切 —— Crossfade 240ms 让换封面也有过渡
        Crossfade(
            targetState = cover,
            animationSpec = tween(240, easing = FastOutSlowInEasing),
            label = "cmx deck cover fade",
            modifier = Modifier.fillMaxSize(),
        ) { target ->
            if (target.isNotBlank()) {
                val model = remember(context, target) {
                    ImageRequest.Builder(context).data(target).size(480).build()
                }
                AsyncImage(model, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}

// internal：与 CmxCoverCarousel 共用同一块黑胶（藏在播放中封面背后滑出旋转），实现不再各写一份。
@Composable
internal fun CmxVinylRecord(accent: Color, cover: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(Color(0xFF34363A), Color(0xFF121315), Color(0xFF292B2F)), radius = radius))
            for (ring in 0..11) drawCircle(
                Color.White.copy(alpha = if (ring % 3 == 0) .12f else .045f),
                radius * (.44f + ring * .042f), style = Stroke(.7.dp.toPx()),
            )
            drawArc(
                Color.White.copy(alpha = .13f), 210f, 48f, false,
                topLeft = Offset(radius * .16f, radius * .16f), size = size * .84f, style = Stroke(2.dp.toPx()),
            )
            drawCircle(accent, radius * .26f)
            drawCircle(Color(0xFF101113), radius * .055f)
        }
        CmxDeckArtwork(cover, Modifier.fillMaxSize(.26f).clip(CircleShape))
        Canvas(Modifier.fillMaxSize(.05f)) { drawCircle(Color(0xFF101113)) }
    }
}
