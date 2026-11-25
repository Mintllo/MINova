// Ported from CurrentMusicX (GPL-3.0) - https://github.com/bileizhen/CurrentMusicX
// Stub layer: minimal implementations of the upstream components the ported PlayerScreen
// references that are outside this port batch (dialogs, queue page, side pages, lyrics
// settings panel). Each stub is marked TODO(CMX) and renders a functional fallback so
// the player never crashes; visuals can be upgraded file by file later.
package com.HyperX.MI.Na.cmx

import com.HyperX.MI.Na.AudioQuality

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.yukonga.miuix.kmp.basic.Text

// ---------- Sheet-side pages (TODO(CMX): port PlayerSidePage / ListeningPage upstream) ----------

internal class PlayerSidePageState(private val scope: androidx.compose.runtime.RememberObserver? = null) {
    val active: Boolean get() = false
    val direction: Float get() = 0f
    val pagerDragging: Boolean get() = false
    fun close() {}
}

@Composable internal fun PlayerSidePageLayout(state: PlayerSidePageState, enabled: Boolean,
    openingDirection: () -> Float, pagerScrolling: () -> Boolean,
    excludedArea: () -> androidx.compose.ui.layout.LayoutCoordinates?,
    side: @Composable () -> Unit, content: @Composable () -> Unit) {
    content()
}

@Composable internal fun PlayerSidePageContent(song: Song?, navigation: PlayerSongNavigation,
    aiEnabled: Boolean, onAi: () -> Unit) {
    Box(Modifier.fillMaxSize())
}

@Composable internal fun PlayerListeningPage(container: CmxPlayerContainer, onPlay: () -> Unit,
    onBinding: () -> Unit, onListening: () -> Unit, onDialogActive: (Boolean) -> Unit) {
    Box(Modifier.fillMaxSize())
}

// ---------- Queue page (TODO(CMX): port PlaybackQueuePage upstream) ----------

internal class QueuePageMotion {
    val animation = androidx.compose.animation.core.Animatable(0f)
    var dragged by androidx.compose.runtime.mutableStateOf<Float?>(null)
    val progress: Float get() = dragged ?: animation.value
}

@Composable internal fun PlaybackQueuePage(vm: PlayerViewModel, motion: QueuePageMotion,
    expanded: Boolean, testTag: String, dismiss: () -> Unit,
    actions: (@Composable (Song) -> Unit)?) {
    PlayerDialogCard("播放列表", dismiss) {
        Text("播放列表暂未接入", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
    }
}

// ---------- Dialogs (TODO(CMX): port the upstream liquid dialog set) ----------

/** 底部抽屉弹窗容器：遮罩 + 上滑面板 + 顶部拖动条，所有播放页弹窗共用。 */
@Composable private fun PlayerDialogCard(title: String, dismiss: () -> Unit,
    content: @Composable () -> Unit) {
    val appear = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .45f))
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null) { dismiss() })
        AnimatedVisibility(visibleState = appear,
            enter = slideInVertically(tween(300, easing = CubicBezierEasing(.2f, 0f, .2f, 1f))) { it } +
                fadeIn(tween(200)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(180))) {
            // MD3 化：显式 surfaceContainerLow 根底（黑底根除）+ 28dp 顶圆角 + 主题化 dragHandle。
            Column(Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable(enabled = false) {}
                .padding(top = 8.dp, bottom = 6.dp)
                .navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .4f)))
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("完成", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
                        modifier = Modifier.clickable { dismiss() }.padding(6.dp))
                }
                Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    content()
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable internal fun MusicDialog(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    PlayerDialogCard(title, onDismiss, content)
}

@Composable internal fun MusicChoiceDialog(title: String, dismiss: () -> Unit, content: @Composable () -> Unit) {
    PlayerDialogCard(title, dismiss, content)
}

@Composable internal fun MusicDialogChoiceRow(label: String, selected: Boolean, modifier: Modifier = Modifier,
    summary: String? = null, enabled: Boolean = true, onClick: () -> Unit = {}) {
    // 尺寸对齐 MD3 设置行：padding ≤ 12dp、字号 13-14sp、行高紧凑，玻璃底不变。
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(enabled = enabled) { onClick() }
        .heightIn(min = 38.dp).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (summary != null) Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (selected) Text("已选", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
    }
}

@Composable internal fun MusicDestinationRow(label: String, modifier: Modifier = Modifier,
    summary: String? = null, enabled: Boolean = true, onClick: () -> Unit = {}) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(enabled = enabled) { onClick() }
        .heightIn(min = 38.dp).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (summary != null) Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable internal fun MusicPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest))
}

@Composable internal fun SleepTimerSheet(timer: SleepTimerFacade, dismiss: () -> Unit) {
    PlayerDialogCard("定时关闭", dismiss) {
        Text("定时关闭暂未接入", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

internal fun sleepSummary(state: SleepUiState): String = if (state.enabled) "计时中" else "未开启"

@Composable internal fun AudioQualitySheet(quality: QualityOption, onQuality: (QualityOption) -> Unit,
    dismiss: () -> Unit, local: Boolean, onNetwork: (() -> Unit)? = null, song: Song? = null,
    insights: Any? = null, audioInfo: AudioInfo? = null, onRefresh: (() -> Unit)? = null,
    container: CmxPlayerContainer? = null, onEffects: (() -> Unit)? = null) {
    // 已接通后端 /music/url 的 br 参数：标准=320000，高（无损）=999000。
    // 点击 → vm.quality() 写入 AudioQuality（持久化），下一首歌解析即生效。
    PlayerDialogCard("音质选择", dismiss) {
        val lossless = quality.br == AudioQuality.BR_HIGH
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(6.dp))
            Text(if (lossless) "当前生效：无损 999K" else "当前生效：标准 320K",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        AudioQualityCard("标准音质", "320K", "省流量，音质均衡，适合日常聆听",
            selected = !lossless,
            onClick = { onQuality(QualityOption("标准音质", AudioQuality.BR_STANDARD)) })
        Spacer(Modifier.height(8.dp))
        AudioQualityCard("无损音质", "999K", "CD 品质，还原更多音乐细节",
            selected = lossless,
            onClick = { onQuality(QualityOption("高音质", AudioQuality.BR_HIGH)) })
        Spacer(Modifier.height(8.dp))
        Text("切换后下一首生效", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
    }
}

/** 音质大卡片：选中态主题色描边 + 对勾弹出，按压缩放微弹。 */
@Composable
private fun AudioQualityCard(title: String, bitrate: String, description: String,
    selected: Boolean, onClick: () -> Unit) {
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val ring by animateColorAsState(
        if (selected) accent else Color.Transparent, animationSpec = tween(240), label = "qualityRing")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f), label = "qualityScale")
    val check by animateFloatAsState(if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f), label = "qualityCheck")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHighest
                else MaterialTheme.colorScheme.surfaceContainer)
            .border(1.5.dp, ring, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Text(bitrate, color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Box(Modifier.size(20.dp).graphicsLayer { alpha = check; scaleX = check; scaleY = check }
            .clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable internal fun PlayerCommentsDialog(comments: Any?, dismiss: () -> Unit, reload: () -> Unit,
    loadMore: () -> Unit, composer: Any?, onEdited: () -> Unit, replyTo: () -> Unit, send: () -> Unit,
    selectSort: () -> Unit, loadReplies: () -> Unit, like: () -> Unit, delete: () -> Unit,
    title: String = "评论", fallbackUser: String = "用户") {
    PlayerDialogCard(title, dismiss) {
        Text("评论区暂未接入", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable internal fun PlayerAiDialog(ai: PlayerAiController, dismiss: () -> Unit,
    onCard: (AiCard) -> Unit) {
    PlayerDialogCard("问 AI", dismiss) {
        Text("AI 暂未接入", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable internal fun SongShareDialog(song: Song, dismiss: () -> Unit) {
    PlayerDialogCard("分享歌曲", dismiss) {
        MusicDestinationRow(song.name, summary = song.artists, onClick = dismiss)
    }
}

@Composable internal fun PersonalRoamingDialog(container: CmxPlayerContainer, dismiss: () -> Unit,
    onBinding: () -> Unit) {
    PlayerDialogCard("私人漫游", dismiss) {
        Text("私人漫游暂未接入", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable internal fun PlayerBackgroundDialog(container: CmxPlayerContainer, dismiss: () -> Unit) {
    PlayerDialogCard("播放背景", dismiss) {
        Text("播放背景设置暂未接入", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable internal fun PlayerLyricsManagementDialog(container: CmxPlayerContainer, song: Song,
    dismiss: () -> Unit) {
    PlayerDialogCard("歌词管理", dismiss) {
        Text("歌词管理暂未接入", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable internal fun RoamingCoverPuzzle(cover: String, songId: String?, showCaption: Boolean = true) {
    MusicCover(cover, Modifier.fillMaxSize())
}

// ---------- Song menu (TODO(CMX): port LocalSongMenu host upstream) ----------

internal class SongMenu(val song: Song, onClose: () -> Unit = {}, onDismiss: () -> Unit = {},
    onFallback: () -> Unit = {}, val extra: (@Composable () -> Unit)? = null, val transport: Boolean = true)

internal val LocalSongMenu = staticCompositionLocalOf<((SongMenu) -> Unit)?> { null }

// ---------- Song navigation (TODO(CMX): needs the upstream repository navigation) ----------

internal class PlayerSongNavigation(
    val artistsEnabled: Boolean = false,
    val albumEnabled: Boolean = false,
    val albumLoading: Boolean = false,
    val openArtists: (Boolean) -> Unit = {},
    val openAlbum: () -> Unit = {},
    val dialogActive: Boolean = false,
)

internal fun rememberPlayerSongNavigation(song: Song?, repository: MusicRepositoryStub,
    server: String, onNavigate: (String) -> Unit): PlayerSongNavigation =
    PlayerSongNavigation()

// ---------- Lyrics panel bridge onto the already-ported Cmx lyrics rail ----------

@Composable internal fun rememberLyricsPosition(player: PlayerState): State<Long> =
    rememberCmxLyricsPosition(player.song?.id, player.positionMs, player.playing, player.durationMs)

@Composable internal fun LyricsScreen(document: CmxLyricsDocument, position: State<Long>,
    onSeek: (Long) -> Unit, modifier: Modifier, canControl: Boolean, translation: Boolean,
    romanization: Boolean, wordAnimation: Boolean, blur: Boolean, fontSize: Float,
    weight: LyricsWeight, offsetMs: Long, display: LyricsDisplaySettings,
    minimal: Boolean = false, perspective: Boolean = false, cover: String = "",
    mediaDurationMs: Long = 0, playbackRunning: Boolean = true) {
    CmxLyricsScreen(document, position, onSeek, modifier, canControl, translation, wordAnimation,
        effects = blur, fontSize = fontSize,
        weightMode = if (weight == LyricsWeight.BOLD) CmxLyricsWeight.ALL else CmxLyricsWeight.CURRENT,
        lyricsOffsetMs = offsetMs,
        display = CmxLyricsDisplay(hideControls = display.hideControls),
        minimal = minimal, perspective = perspective, cover = cover,
        mediaDurationMs = mediaDurationMs, playbackRunning = playbackRunning)
}

// TODO(CMX): the lyrics display settings panel needs the upstream slider set.
@Composable internal fun LyricsDisplaySettingsPanel(fontSize: Float, onFontSize: (Float) -> Unit,
    weight: LyricsWeight, onWeight: () -> Unit, display: LyricsDisplaySettings,
    onDisplay: ((LyricsDisplaySettings) -> LyricsDisplaySettings) -> Unit, onKaraoke: () -> Unit,
    font: LyricsFont, fontFile: String?, onFont: (LyricsFont) -> Unit,
    onFontFile: (String?) -> Unit, currentLyric: Any?) {
    Text("歌词显示设置暂未接入", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

internal fun currentSettingsLyric(lyrics: LyricsState, state: PlayerState, song: Song?,
    offsetMs: Long): Any? = null

/** Fades a control block with the lyrics-page reveal/hide progress. */
@Composable internal fun PlayerControlsRegion(visibility: () -> Float,
    modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val alpha by rememberUpdatedState(visibility())
    Box(modifier.graphicsLayer { this.alpha = alpha }) { content() }
}

// TODO(CMX): the cover-page lyric preview needs the upstream line tracker; the full
// player shows the current LRC line beneath the artwork until then.
@Composable internal fun CoverLyricPreview(vm: PlayerViewModel, modifier: Modifier, active: Boolean) {
    if (!active) { Box(modifier); return }
    val lyrics = vm.lyrics
    val player by vm.state.collectAsStateWithLifecycle()
    val position = rememberLyricsPosition(player)
    val line = remember(position.value) {
        lyrics.document.lines.lastOrNull { it.startTimeMs <= position.value }
    }
    Box(modifier.height(64.dp), contentAlignment = Alignment.Center) {
        Text(line?.text.orEmpty(), color = Color.White.copy(alpha = .72f), fontSize = 15.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp))
    }
}
