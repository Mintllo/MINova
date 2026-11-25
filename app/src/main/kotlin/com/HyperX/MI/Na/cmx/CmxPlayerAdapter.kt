// Ported from CurrentMusicX (GPL-3.0) - https://github.com/bileizhen/CurrentMusicX
// Adapter layer: maps MINova's PlayerViewModel data (currentSong / isPlaying / seekTo /
// onNext / onPrevious / lyrics) onto the upstream PlayerScreen's PlayerViewModel shape.
// Playback, parsing and watchdog logic stay untouched in MINova's MainActivity ViewModel;
// this adapter only mirrors state into the UI-facing flows the ported screen reads.
// TODO(CMX) markers flag surfaces whose upstream behaviour is approximated.
package com.HyperX.MI.Na.cmx

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HyperX.MI.Na.App
import com.HyperX.MI.Na.AudioQuality
import com.HyperX.MI.Na.LyricData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

internal enum class MusicSource { LOCAL, NETEASE, QQ_MUSIC, EMBY }

/** Upstream-shaped song entity consumed by the ported player composables. */
internal data class Song(
    val id: String,
    val name: String,
    val artists: String = "",
    val cover: String = "",
    val durationMs: Long = 0,
    val musicSource: MusicSource = MusicSource.LOCAL,
    val privateIdentity: String? = null,
    val privateLibrary: Boolean = false,
    val video: Boolean = false,
    val localUri: String? = null,
    val emby: PrivateSourceIdentity? = null,
    val navidrome: PrivateSourceIdentity? = null,
    val originalQualityLabel: String? = null,
    /** 专辑名（上游歌单 UI 需要；其余移植面不使用）。 */
    val album: String = "",
)

internal data class PrivateSourceIdentity(val id: String)

internal enum class PlayerMode { LOCAL, ROOM }
internal enum class PlaybackMode(val label: String) {
    LIST("列表循环"), ONE("单曲循环"), SHUFFLE("随机播放"), HEART("心动模式")
}

internal enum class LyricsStyle(val label: String) { CLASSIC("经典"), APPLE_MUSIC("Apple 风格") }
internal enum class LyricsWeight(val label: String) { REGULAR("常规"), MEDIUM("中等"), BOLD("加粗") }
internal enum class KaraokeScope(val label: String) { CURRENT("当前行"), ALL("可见行"), ALWAYS("全部行") }
internal enum class LyricsFont(val label: String) { DEFAULT("默认"), ROUNDED("圆体"), SERIF("衬线") }
internal enum class PlayerCoverStyle { STANDARD, IMMERSIVE, SQUARE }

internal data class LyricsDisplaySettings(
    val style: LyricsStyle = LyricsStyle.APPLE_MUSIC,
    val translation: Boolean = true,
    val romanization: Boolean = false,
    val wordAnimation: Boolean = true,
    val blur: Boolean = true,
    val karaokeScope: KaraokeScope = KaraokeScope.CURRENT,
    val perspective: Boolean = true,
    val hideControls: Boolean = false,
)

internal data class PlayerSettings(
    val server: String = "netease",
    val lyricsDisplay: LyricsDisplaySettings = LyricsDisplaySettings(),
    val lyricsWeight: LyricsWeight = LyricsWeight.REGULAR,
    val lyricsFontSize: Float = 20f,
    val lyricsFont: LyricsFont = LyricsFont.DEFAULT,
    val lyricsFontFile: String? = null,
    val lyricsOffsetMs: Long = 0,
) {
    fun qualityFor(source: MusicSource?, kind: NetworkKind): QualityOption =
        // 实时读取全局音质选择（AudioQuality.br 是 Compose 状态，这里在组合期读取 → 切换即刷新）。
        QualityOption(AudioQuality.label(), AudioQuality.br)
}

internal data class QualityOption(val label: String = "标准音质", val br: String = AudioQuality.BR_STANDARD) {
    fun qualityLabel(isQq: Boolean): String = label
}

internal enum class NetworkKind { WIFI, CELLULAR, OFFLINE }
internal data class NetworkStatus(val kind: NetworkKind = NetworkKind.WIFI)

internal data class AudioInfo(val label: String)

internal data class PlayerState(
    val song: Song? = null,
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val canControlPlayback: Boolean = true,
    val showPause: Boolean = false,
    val error: String? = null,
    val loading: Boolean = false,
    val resolving: Boolean = false,
    val mode: PlayerMode = PlayerMode.LOCAL,
    val audioInfo: AudioInfo? = null,
    val playbackSpeed: Float = 1f,
) {
    fun bufferedProgress(durationMs: Long): Float = 0f
}

internal data class QueueState(
    val current: Song? = null,
    val songs: List<Song> = emptyList(),
    val mode: PlaybackMode = PlaybackMode.LIST,
)

internal data class SongActionsState(
    val songId: Any? = null,
    val error: String? = null,
    val liked: Boolean? = null,
    val likeCount: Long? = null,
    val likeCountLabel: String? = null,
    val commentCount: Long? = null,
)

internal data class LikePreviewState(val song: Song, val liked: Boolean)
internal data class LibraryStatus(val liked: Boolean = false)

internal data class SleepUiState(val enabled: Boolean = false, val notice: String? = null)

internal data class TogetherState(val active: Boolean = false, val busy: Boolean = false)
internal data class SharedQueueState(val mode: PlaybackMode = PlaybackMode.LIST, val songs: List<Song> = emptyList())

internal data class RoamingChoice(val code: String = "", val title: String = "")
internal data class RoamingState(val active: Boolean = false, val error: String? = null,
    val choice: RoamingChoice = RoamingChoice())

internal data class AppearanceSettings(
    val playerBackground: String = "",
    // MINova 定版：圆角正方形封面 + 封面毛玻璃背景（不走上游流动背景）。
    val playerCoverStyle: PlayerCoverStyle = PlayerCoverStyle.SQUARE,
    val playerDynamicBackground: Boolean = false,
    val playerBackgroundBlur: Float = 35f,
)

/** Mirrors the upstream lyrics UiState around MINova's LyricData via the Cmx rail document. */
internal class LyricsState(
    initial: CmxLyricsDocument = CmxLyricsDocument(),
) {
    var document by mutableStateOf(initial)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var songKey by mutableStateOf<Any?>(null)
        private set
    var songOffsetMs by mutableStateOf(0L)
        private set
    var songId by mutableStateOf<String?>(null)
        private set

    fun matches(song: Song?): Boolean = song != null && songId == song.id && document.lines.isNotEmpty()

    fun update(songId: String?, data: LyricData, durationMs: Long, settled: Boolean = true) {
        this.songId = songId
        songKey = songId
        // 三分状态：取词未结束 = 加载中；结束且无词 = 暂无歌词占位；有词 = 正常渲染
        loading = data.times.isEmpty() && !settled
        error = if (data.times.isEmpty() && settled) "暂无歌词" else null
        document = cmxDocumentFromLrc(data.times, data.lyrics, data.translations, durationMs)
    }
}

internal class PlayerAiController {
    fun close() {}
    fun draft(prompt: String) {}
}

internal data class AiCard(val type: String = "", val id: String = "")

internal class PlayerTransportController(
    private val onSeek: (Long) -> Unit,
    private val onNext: () -> Unit,
    private val onPrevious: () -> Unit,
) {
    fun seek(positionMs: Long) = onSeek(positionMs.coerceAtLeast(0))
    fun next() = onNext()
    fun previous() = onPrevious()
}

internal class SleepTimerFacade {
    val state: MutableStateFlow<SleepUiState> = MutableStateFlow(SleepUiState())
    fun consumeNotice() { state.value = state.value.copy(notice = null) }
}

internal class TogetherFacade {
    val state: MutableStateFlow<TogetherState> = MutableStateFlow(TogetherState())
    val queue: MutableStateFlow<SharedQueueState> = MutableStateFlow(SharedQueueState())
}

internal class ErrorNoticesStub {
    fun showMessage(message: String, longDuration: Boolean = false) {}
    fun show(message: String) {}
}

internal class PlayerBackgroundsStub {
    fun file(key: String): File? = null
}

internal class PlayerControllerStub {
    fun refreshSource() {}
}

internal class MusicRepositoryStub {
    val insights: Any? = null
}

/** Container stub mirroring the upstream AppContainer surface the player screen touches. */
internal class CmxPlayerContainer {
    val playbackNetworkStatus: MutableStateFlow<NetworkStatus> = MutableStateFlow(NetworkStatus())
    val settings = AppearanceSettingsHolder()
    val playerBackgrounds = PlayerBackgroundsStub()
    val errorNotices = ErrorNoticesStub()
    val musicRepository = MusicRepositoryStub()
    val playerController = PlayerControllerStub()
    val dockAudioLevel = DockAudioLevel()
    val activePersonalRoaming = RoamingHolder()

    class AppearanceSettingsHolder {
        val state: MutableStateFlow<AppearanceSettings> = MutableStateFlow(AppearanceSettings())
    }
    class RoamingHolder {
        val state: MutableStateFlow<RoamingState> = MutableStateFlow(RoamingState())
    }
    val activePersonalRoamingIsQq: Boolean get() = false
}

internal object NeteaseSongActionsRepository {
    fun songId(song: Song?): Long? = null
}

/**
 * Upstream-shaped PlayerViewModel fed from MINova's playback callbacks. Only the visual
 * layer consumes it; every playback action delegates straight back to MINova's handlers.
 */
internal class PlayerViewModel(
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleLoop: () -> Unit,
) : ViewModel() {
    val container = CmxPlayerContainer()
    val player = PlayerTransportController(onSeek, onNext, onPrevious)
    val sleepTimer = SleepTimerFacade()
    val together = TogetherFacade()
    val ai = PlayerAiController()

    val state = MutableStateFlow(PlayerState())
    val queue = MutableStateFlow(QueueState())
    val settings = MutableStateFlow(PlayerSettings())
    val actions = MutableStateFlow(SongActionsState())
    val comments = MutableStateFlow<Any?>(null)
    val commentComposer = MutableStateFlow<Any?>(null)
    val likePreview = MutableStateFlow<LikePreviewState?>(null)
    val libraryStatuses = MutableStateFlow<Map<String, LibraryStatus>>(emptyMap())
    val usesNeteaseLibrary = MutableStateFlow(false)
    val currentSong = MutableStateFlow<Song?>(null)
    val lyrics = LyricsState()

    fun loadComments(more: Boolean = false) {}
    fun onCommentEdited() {}
    fun replyTo() {}
    fun sendComment() {}
    fun selectCommentSort() {}
    fun loadCommentReplies() {}
    fun likeComment() {}
    fun deleteComment() {}
    fun openAi() {}
    fun playAiCard(card: AiCard) {}
    fun setPlaybackMode(mode: PlaybackMode) { if (mode == PlaybackMode.ONE || mode == PlaybackMode.LIST) onToggleLoopRef?.invoke() }
    fun cyclePlaybackMode() { onToggleLoopRef?.invoke() }
    fun quality(option: QualityOption) {
        // 音质弹窗选择 → 写入全局音质（持久化到 SP），下一首歌解析 /music/url 即带新 br。
        AudioQuality.update(App.instance, option.br)
    }
    fun togglePrimaryLike() {}
    fun lyricsWeight(weight: LyricsWeight) { settings.value = settings.value.copy(lyricsWeight = weight) }
    fun lyricsDisplay(transform: (LyricsDisplaySettings) -> LyricsDisplaySettings) {
        settings.value = settings.value.copy(lyricsDisplay = transform(settings.value.lyricsDisplay))
    }
    fun lyricsFontSize(size: Float) { settings.value = settings.value.copy(lyricsFontSize = size) }
    fun lyricsFont(font: LyricsFont) { settings.value = settings.value.copy(lyricsFont = font) }
    fun lyricsFontFile(path: String?) { settings.value = settings.value.copy(lyricsFontFile = path) }

    internal var onToggleLoopRef: (() -> Unit)? = onToggleLoop

    /** Push MINova's latest playback snapshot into the UI-facing flows. */
    fun push(appSong: com.HyperX.MI.Na.Song, isPlaying: Boolean, isLooping: Boolean,
        currentPosition: Long, duration: Long, data: LyricData, lyricsSettled: Boolean = true) {
        val mapped = Song(
            id = appSong.id,
            name = appSong.name,
            artists = appSong.artist,
            cover = appSong.picUrl,
            durationMs = if (duration > 0) duration else appSong.duration,
            musicSource = when (appSong.source.lowercase()) {
                "qq" -> MusicSource.QQ_MUSIC
                "netease" -> MusicSource.NETEASE
                else -> MusicSource.LOCAL
            },
        )
        val knownDuration = if (duration > 0) duration else appSong.duration
        state.value = PlayerState(
            song = mapped, playing = isPlaying, positionMs = currentPosition, durationMs = knownDuration,
            canControlPlayback = true, showPause = isPlaying, mode = PlayerMode.LOCAL,
        )
        queue.value = QueueState(current = mapped, songs = listOf(mapped),
            mode = if (isLooping) PlaybackMode.ONE else PlaybackMode.LIST)
        currentSong.value = mapped
        lyrics.update(appSong.id, data, knownDuration, lyricsSettled)
    }
}
