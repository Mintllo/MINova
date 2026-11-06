package com.HyperX.MI.Na

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/* ============================================================================
 * 需求 3：ffmpeg 音频格式转换器
 *
 * 引擎：com.arthenica:ffmpeg-kit-full:6.0-2（官方仓库 2025 归档，制品仍在 Maven Central
 *       的镜像仓库，见 settings.gradle.kts 中走到阿里云 public 的兜底仓库）
 * 编码器可用性由 ffmpeg-kit full 变体保证：lame(mp3) / vorbis / libopus / twolame(mp2) /
 *       wmav2 / opencore-amr(amr) / ac3 / pcm(wav,aiff) / flac / aac
 *
 * MIDI：ffmpeg 没有「音频→MIDI」的复音转谱能力，MusicXML/MIDI 写谱也不在其 Muxer 列表中，
 *       因此本模块把 MIDI 明确标记为 unsupported，UI 会显示原因而不会假装转换成功。
 *       是否支持「.mid 作为输入源」由运行期探测（ffmpeg -demuxers）决定，探测结果如实展示。
 * ========================================================================== */

/** 目标音频格式定义。 */
data class AudioFormat(
    val id: String,
    val label: String,
    val ext: String,
    val mime: String,
    /** 有损格式才需要码率选项 */
    val lossy: Boolean,
    /** true = 当前引擎无法实现，UI 必须给出说明而不是假装支持 */
    val unsupported: Boolean = false,
    val note: String? = null,
)

object AudioFormats {
    /** MP3 → MIDI 需要旋律识别（复音转谱），ffmpeg 不具备该能力。 */
    const val MIDI_NOTICE = "MP3 转 MIDI 需要旋律识别，暂不支持"

    val ALL: List<AudioFormat> = listOf(
        AudioFormat("mp3", "MP3", "mp3", "audio/mpeg", lossy = true),
        AudioFormat("wav", "WAV (PCM)", "wav", "audio/wav", lossy = false),
        AudioFormat("flac", "FLAC", "flac", "audio/flac", lossy = false),
        AudioFormat("m4a", "AAC (M4A)", "m4a", "audio/mp4", lossy = true),
        AudioFormat("ogg", "OGG (Vorbis)", "ogg", "audio/ogg", lossy = true),
        AudioFormat("opus", "OPUS", "opus", "audio/opus", lossy = true),
        AudioFormat("wma", "WMA", "wma", "audio/x-ms-wma", lossy = true),
        AudioFormat("amr", "AMR", "amr", "audio/amr", lossy = true),
        AudioFormat("ac3", "AC3", "ac3", "audio/ac3", lossy = true),
        AudioFormat("aiff", "AIFF", "aiff", "audio/aiff", lossy = false),
        AudioFormat("mp2", "MP2", "mp2", "audio/mpeg", lossy = true),
        AudioFormat(
            "midi", "MIDI", "mid", "audio/midi", lossy = false,
            unsupported = true, note = MIDI_NOTICE
        ),
    )

    val BITRATES = listOf("128k", "192k", "320k")
    val SAMPLE_RATES = listOf("保持原样", "8000", "16000", "22050", "32000", "44100", "48000")

    fun byId(id: String): AudioFormat = ALL.firstOrNull { it.id == id } ?: ALL.first()

    fun fromFileName(name: String): AudioFormat? {
        val ext = name.substringAfterLast('.', "")
        if (ext.isEmpty()) return null
        return ALL.firstOrNull { it.ext.equals(ext, true) }
    }
}

/** 当前批处理进度（null = 空闲）。 */
data class ConversionState(
    val currentIndex: Int,
    val total: Int,
    val fraction: Float,
    val indeterminate: Boolean,
    val sourceName: String,
    val targetLabel: String,
    val outputName: String? = null,
    val cancelling: Boolean = false,
)

data class ConversionResult(
    val sourceName: String,
    val output: File?,
    val success: Boolean,
    val message: String?,
)

/**
 * 转换任务管理器。
 *
 * 生命周期与进程一致（object 单例），Compose 侧只用 collectAsState 观察，
 * 因此旋转屏幕/离开页面都不会丢失进度。ffmpeg 本身跑在 ffmpeg-kit 自己的
 * 后台线程池里（executeAsync），Kotlin 协程只负责串行调度与状态汇总。
 */
object AudioConversionManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _convertedFiles = MutableStateFlow<List<File>>(emptyList())
    val convertedFiles: StateFlow<List<File>> = _convertedFiles

    private val _state = MutableStateFlow<ConversionState?>(null)
    val state: StateFlow<ConversionState?> = _state

    private val _results = MutableStateFlow<List<ConversionResult>>(emptyList())
    val results: StateFlow<List<ConversionResult>> = _results

    /** null = 尚未探测；false = 当前 ffmpeg 构建不支持 MIDI 输入；true = 支持 */
    private val _midiInputSupported = MutableStateFlow<Boolean?>(null)
    val midiInputSupported: StateFlow<Boolean?> = _midiInputSupported

    @Volatile
    private var convertedDir: File? = null

    @Volatile
    private var sessionId: Long = -1L

    private val cancelRequested = AtomicBoolean(false)
    private var batchJob: Job? = null

    val isRunning: Boolean get() = batchJob?.isActive == true

    fun attach(dir: File) {
        convertedDir = dir
        if (!dir.exists()) runCatching { dir.mkdirs() }
        refresh()
    }

    fun outputDirectory(): File? = convertedDir

    fun refresh() {
        val dir = convertedDir ?: return
        _convertedFiles.value = dir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun convertedTotalSize(): Long = _convertedFiles.value.sumOf { it.length() }

    fun delete(file: File): Boolean {
        val ok = runCatching { file.delete() }.getOrDefault(false)
        refresh()
        return ok
    }

    fun clearAll(): Int {
        var n = 0
        convertedDir?.listFiles()?.forEach { if (it.isFile && runCatching { it.delete() }.getOrDefault(false)) n++ }
        refresh()
        return n
    }

    /**
     * 探测当前 ffmpeg 构建是否带 MIDI Demuxer。
     * 结果只用于「.mid 作为输入源」的可用性提示，绝不用于假装支持音频→MIDI。
     */
    fun probeMidiInputSupport() {
        if (_midiInputSupported.value != null) return
        scope.launch {
            val supported = try {
                val session = FFmpegKit.execute("-hide_banner -demuxers")
                val out = session.output ?: ""
                out.lineSequence().any { line ->
                    val parts = line.trim().split(Regex("\\s+"))
                    parts.size >= 2 && parts[1].equals("midi", true)
                }
            } catch (_: Throwable) {
                false
            }
            _midiInputSupported.value = supported
        }
    }

    /**
     * 串行转换多个源文件。整个过程在 IO 线程 + ffmpeg 原生异步线程里执行，
     * 不阻塞 UI；[cancel] 可随时中断。
     */
    fun convert(
        sources: List<File>,
        format: AudioFormat,
        bitrate: String,
        sampleRate: String,
        onFinished: (List<ConversionResult>) -> Unit = {},
    ) {
        if (sources.isEmpty()) return
        if (format.unsupported) {
            val blocked = sources.map { ConversionResult(it.name, null, false, format.note ?: "该格式暂不支持") }
            _results.value = blocked
            onFinished(blocked)
            return
        }
        if (batchJob?.isActive == true) return
        val dir = convertedDir
        if (dir == null) {
            val failed = sources.map { ConversionResult(it.name, null, false, "输出目录不可用") }
            _results.value = failed
            onFinished(failed)
            return
        }
        if (!dir.exists()) runCatching { dir.mkdirs() }

        cancelRequested.set(false)
        _results.value = emptyList()
        batchJob = scope.launch {
            val collected = ArrayList<ConversionResult>()
            for ((index, src) in sources.withIndex()) {
                if (cancelRequested.get()) {
                    collected.add(ConversionResult(src.name, null, false, "已取消"))
                    continue
                }
                val out = uniqueOutputFile(dir, src, format)
                _state.value = ConversionState(
                    currentIndex = index + 1,
                    total = sources.size,
                    fraction = 0f,
                    indeterminate = true,
                    sourceName = src.name,
                    targetLabel = format.label,
                    outputName = out.name
                )
                val totalMs = durationMs(src)
                if (totalMs > 0) {
                    _state.value = _state.value?.copy(indeterminate = false)
                }
                val result = runOne(src, out, format, bitrate, sampleRate, totalMs)
                collected.add(result)
                _results.value = collected.toList()
            }
            _state.value = null
            sessionId = -1L
            refresh()
            val snapshot = collected.toList()
            withContext(Dispatchers.Main) { onFinished(snapshot) }
        }
    }

    /** 取消当前批处理：中断正在跑的 ffmpeg 会话，并让后续文件不再开始。 */
    fun cancel() {
        cancelRequested.set(true)
        _state.value = _state.value?.copy(cancelling = true)
        val id = sessionId
        if (id > 0) runCatching { FFmpegKit.cancel(id) }
    }

    fun clearResults() {
        _results.value = emptyList()
    }

    // ------------------------------------------------------------------ 内部

    private suspend fun runOne(
        src: File,
        dst: File,
        format: AudioFormat,
        bitrate: String,
        sampleRate: String,
        totalDurationMs: Long,
    ): ConversionResult {
        if (!src.exists() || src.length() <= 0L) {
            return ConversionResult(src.name, null, false, "源文件不存在或为空")
        }
        val command = buildCommand(src, dst, format, bitrate, sampleRate)
        var lastFraction = -1f
        var lastEmitAt = 0L

        val outcome: Pair<Boolean, String?> = suspendCancellableCoroutine { cont ->
            try {
                val session = FFmpegKit.executeAsync(
                    command,
                    { finished ->
                        val rc = finished.returnCode
                        val success = rc != null && ReturnCode.isSuccess(rc)
                        val message = when {
                            success -> null
                            rc != null && ReturnCode.isCancel(rc) -> "已取消"
                            else -> firstErrorLine(finished.output)
                                ?: "ffmpeg 退出码 ${rc?.value ?: -1}"
                        }
                        if (cont.isActive) cont.resume(success to message)
                    },
                    { /* 逐条日志：错误信息已在 complete 回调里从 output 提取，这里不刷状态 */ },
                    { stats ->
                        if (totalDurationMs > 0) {
                            val f = (stats.time / totalDurationMs.toDouble()).toFloat().coerceIn(0f, 1f)
                            val now = System.currentTimeMillis()
                            if (f - lastFraction >= 0.01f || now - lastEmitAt > 300L) {
                                lastFraction = f
                                lastEmitAt = now
                                _state.value = _state.value?.copy(fraction = f, indeterminate = false)
                            }
                        }
                    }
                )
                sessionId = session.sessionId
                cont.invokeOnCancellation {
                    runCatching { FFmpegKit.cancel(session.sessionId) }
                }
            } catch (t: Throwable) {
                if (cont.isActive) cont.resume(false to (t.message ?: "无法启动 ffmpeg"))
            }
        }

        return if (outcome.first && dst.exists() && dst.length() > 0L) {
            ConversionResult(src.name, dst, true, null)
        } else {
            if (dst.exists()) runCatching { dst.delete() }
            ConversionResult(src.name, null, false, outcome.second ?: "转换失败")
        }
    }

    private fun buildCommand(
        src: File,
        dst: File,
        format: AudioFormat,
        bitrate: String,
        sampleRate: String,
    ): String {
        val sb = StringBuilder()
        sb.append("-y -hide_banner -loglevel warning")
        sb.append(" -i ").append(quote(src.absolutePath))
        sb.append(" -vn")
        when (format.id) {
            "mp3" -> sb.append(" -c:a libmp3lame -b:a ").append(bitrate)
            "wav" -> sb.append(" -c:a pcm_s16le")
            "flac" -> sb.append(" -c:a flac -compression_level 5")
            "m4a" -> sb.append(" -c:a aac -b:a ").append(bitrate)
            "ogg" -> sb.append(" -c:a libvorbis -q:a 5")
            "opus" -> sb.append(" -c:a libopus -b:a ").append(bitrate)
            "wma" -> sb.append(" -c:a wmav2 -b:a ").append(bitrate)
            // AMR-NB 规范只允许 8kHz 单声道 + 固定码率，不能套用用户选择
            "amr" -> sb.append(" -c:a libopencore_amrnb -b:a 12.2k -ar 8000 -ac 1")
            "ac3" -> sb.append(" -c:a ac3 -b:a ").append(bitrate)
            "aiff" -> sb.append(" -c:a pcm_s16be")
            "mp2" -> sb.append(" -c:a libtwolame -b:a ").append(bitrate)
            else -> sb.append(" -c:a copy")
        }
        // libopus 只接受 8/12/16/24/48kHz，AMR 已锁定 8kHz，其余格式允许用户选择
        if (sampleRate != AudioFormats.SAMPLE_RATES.first() && format.id != "amr" && format.id != "opus") {
            sb.append(" -ar ").append(sampleRate)
        }
        sb.append(" -f ").append(muxerFor(format))
        sb.append(' ').append(quote(dst.absolutePath))
        return sb.toString()
    }

    private fun muxerFor(format: AudioFormat): String = when (format.id) {
        "m4a" -> "ipod"
        "wma" -> "asf"
        else -> format.id
    }

    private fun quote(path: String): String = "\"" + path.replace("\"", "\\\"") + "\""

    private fun firstErrorLine(output: String?): String? = output
        ?.lineSequence()
        ?.map { it.trim() }
        ?.firstOrNull { it.isNotEmpty() }
        ?.take(200)

    private fun uniqueOutputFile(dir: File, src: File, format: AudioFormat): File {
        val raw = src.nameWithoutExtension
        val base = sanitize(raw).ifBlank { "audio" }.take(60)
        var candidate = File(dir, "$base.${format.ext}")
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base($i).${format.ext}")
            i++
        }
        return candidate
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_").trim()

    fun durationMs(file: File): Long {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(file.absolutePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Throwable) {
            0L
        } finally {
            runCatching { mmr.release() }
        }
    }

    /** 把用户通过 SAF 选中的任意音频（含 .mid）拷到 cache 再交给 ffmpeg。 */
    fun importSource(context: Context, uri: Uri): File? {
        return try {
            val display = displayName(context, uri) ?: "picked_${System.currentTimeMillis()}"
            val ext = display.substringAfterLast('.', "").lowercase(Locale.ROOT)
            val base = sanitize(display.substringBeforeLast('.', display)).ifBlank { "picked" }.take(60)
            val dir = File(context.cacheDir, "convert_in")
            if (!dir.exists()) dir.mkdirs()
            val out = File(dir, if (ext.isEmpty()) base else "$base.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            if (out.length() > 0L) out else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun displayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    } catch (_: Throwable) {
        null
    }
}
