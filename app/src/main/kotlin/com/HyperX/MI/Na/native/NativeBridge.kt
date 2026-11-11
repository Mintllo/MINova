package com.HyperX.MI.Na.native

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MINova 原生加速桥（NDK / libminova.so，纯计算、无 Android 依赖）。
 *
 * 六条热点路径：
 *  - [computeWave] : 波浪进度条每 2px 步进的 y 值（WaveProgress.kt per-frame sin 循环的 C++ 版，
 *    两端 taper 收 0 的数学原样保留）
 *  - [parseLrc]    : LRC 解析，输出按时间排序的 "mm:ss.xx\t歌词行"
 *    （MainActivity.parseLrc feed() 的 C++ 版，含 [mm:ss.xx] 多时间标签行）
 *  - [bmHash]      : FNV-1a 64 快速指纹（歌曲去重 / 缓存 key 用）
 *  - [filterSongs] : 批量「是否像音乐」（MainActivity.isLikelyMusic 的 C++ 版，true=保留）
 *  - [junkFlags]   : 批量「是否垃圾版本」（MainActivity.isJunkVersion 的 C++ 版，true=垃圾，
 *    供排序键批量预计算）
 *  - [dedupKey]    : 搜索结果去重键（MainActivity 搜索合并处的 C++ 版）
 *
 * 兜底约定（红线）：libminova.so 缺失、加载失败或调用抛错时，[available] 为 false
 * 或包装方法返回 null / -1；**任何调用点必须先判 [available]（或按包装返回值），
 * 失败无缝走原 Kotlin 逻辑，行为保持一致。**
 *
 * 激活证据：每条 native 路径首次**成功**调用时打一行
 * `Log.i("MinovaNative", "native path active: <fn>")`（每个函数只打一次，release 也打），
 * logcat 抓到该行 = native 路径真实执行过的运行时证据。
 *
 * 首次访问本 object 时尝试 System.loadLibrary；加载失败在 init 内被捕获，
 * 类仍可用（loaded=false），不会抛 ExceptionInInitializerError。
 */
object NativeBridge {

    /** native 库是否可用。false 时所有调用点走原 Kotlin 兜底逻辑。 */
    val available: Boolean
        get() = loaded

    @Volatile
    private var loaded = false

    init {
        loaded = try {
            System.loadLibrary("minova")
            true
        } catch (_: Throwable) {
            false
        }
    }

    // ── 一次性激活日志（每个 native 路径首次成功调用打一行，release 也打）──
    private class Once(private val name: String) {
        private val fired = AtomicBoolean(false)
        fun fire() {
            if (fired.compareAndSet(false, true)) {
                Log.i("MinovaNative", "native path active: $name")
            }
        }
    }

    private val onceWave = Once("computeWave")
    private val onceLrc = Once("parseLrc")
    private val onceHash = Once("bmHash")
    private val onceFilter = Once("filterSongs")
    private val onceJunk = Once("junkFlags")
    private val onceDedup = Once("dedupKey")

    // ── JNI 原型（object 上的 external fun 编译为单例实例方法，JNI 侧以 jobject 接收）──
    // 请勿在兜底检查之外直接调用，一律走下方包装方法。

    /**
     * 波浪 y 值预计算：x 从 x0+2f 起每步 +2f，输出
     * `y = midY - env*amp*sin(2π*(x-x0)/wavelengthPx + phase)`（两端 taper 收 0），
     * 写入 [out] 并返回写入点数。
     */
    external fun nativeComputeWave(
        widthPx: Int, midY: Float, amp: Float, wavelengthPx: Float,
        phase: Float, x0: Float, x1: Float, taper: Float, out: FloatArray
    ): Int

    /** LRC 解析：输出按时间排序的 "mm:ss.xx\t歌词行"（同一时间戳内保持原文出现顺序）。 */
    external fun nativeParseLrc(lrc: String): Array<String>

    /** FNV-1a 64 指纹（按无符号字节参与运算），供歌曲去重 / 缓存 key。 */
    external fun nativeBmHash(bytes: ByteArray): Long

    /**
     * 批量「是否像音乐」：names/artists/albums 等长平行数组，
     * 返回逐条 [MainActivity.isLikelyMusic] 结果（true=保留）。
     */
    external fun nativeFilterSongs(
        names: Array<String>, artists: Array<String>, albums: Array<String>
    ): BooleanArray

    /**
     * 批量「是否垃圾版本」：返回逐条 [MainActivity.isJunkVersion] 结果
     * （true=垃圾版本），供排序键批量预计算。
     */
    external fun nativeJunkFlags(
        names: Array<String>, artists: Array<String>, albums: Array<String>
    ): BooleanArray

    /** 搜索结果去重键：`(name + "|" + artist).lowercase() + "|" + parseVersionTag(name, album)`。 */
    external fun nativeDedupKey(name: String, artist: String, album: String): String

    // ── 安全包装（调用点使用这些）───────────────────────────────────────────

    /**
     * 波浪 y 值预计算。返回写入 out 的点数；native 不可用或失败返回 -1，
     * 调用点据此走原 Kotlin 循环（视觉与原实现逐位一致）。
     */
    fun computeWave(
        widthPx: Int, midY: Float, amp: Float, wavelengthPx: Float,
        phase: Float, x0: Float, x1: Float, taper: Float, out: FloatArray
    ): Int {
        if (!loaded || out.isEmpty()) return -1
        return try {
            val n = nativeComputeWave(widthPx, midY, amp, wavelengthPx, phase, x0, x1, taper, out)
            onceWave.fire()
            n
        } catch (_: Throwable) {
            -1
        }
    }

    /**
     * LRC 解析。null = native 不可用或失败，调用点走原 Kotlin feed 逻辑。
     * 条目格式 "mm:ss.xx\t文本"，已按时间稳定排序。
     */
    fun parseLrc(lrc: String): Array<String>? {
        if (!loaded) return null
        return try {
            val r = nativeParseLrc(lrc)
            onceLrc.fire()
            r
        } catch (_: Throwable) {
            null
        }
    }

    /** FNV-1a 64 指纹。null = native 不可用或失败，调用点走原逻辑。 */
    fun bmHash(bytes: ByteArray): Long? {
        if (!loaded) return null
        return try {
            val r = nativeBmHash(bytes)
            onceHash.fire()
            r
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 批量「是否像音乐」。null = native 不可用/失败/数组不等长，
     * 调用点走原 Kotlin 逐条 [com.HyperX.MI.Na.isLikelyMusic]，行为一致。
     */
    fun filterSongs(
        names: Array<String>, artists: Array<String>, albums: Array<String>
    ): BooleanArray? {
        if (!loaded || names.size != artists.size || names.size != albums.size) return null
        return try {
            val r = nativeFilterSongs(names, artists, albums)
            if (r.size != names.size) return null
            onceFilter.fire()
            r
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 批量「是否垃圾版本」。null = native 不可用/失败/数组不等长，
     * 调用点走原 Kotlin 逐条 [com.HyperX.MI.Na.isJunkVersion]，行为一致。
     */
    fun junkFlags(
        names: Array<String>, artists: Array<String>, albums: Array<String>
    ): BooleanArray? {
        if (!loaded || names.size != artists.size || names.size != albums.size) return null
        return try {
            val r = nativeJunkFlags(names, artists, albums)
            if (r.size != names.size) return null
            onceJunk.fire()
            r
        } catch (_: Throwable) {
            null
        }
    }

    /** 搜索结果去重键。null = native 不可用或失败，调用点走原 Kotlin 表达式。 */
    fun dedupKey(name: String, artist: String, album: String): String? {
        if (!loaded) return null
        return try {
            val r = nativeDedupKey(name, artist, album)
            onceDedup.fire()
            r
        } catch (_: Throwable) {
            null
        }
    }
}
