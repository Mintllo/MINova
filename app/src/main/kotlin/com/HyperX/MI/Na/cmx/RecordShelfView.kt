package com.HyperX.MI.Na.cmx

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.LruCache
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.HyperX.MI.Na.native.NativeBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.*

/** One Android frame clock owns the shelf, its hit regions and the index ruler. */
internal class RecordShelfView(context: Context) : View(context) {
    var onSelection: (Int) -> Unit = {}
    var onPreview: (Int?) -> Unit = {}
    private var previewIndex: Int? = null
    private fun preview(index: Int?) {
        if (previewIndex != index) { previewIndex = index; onPreview(index) }
    }
    var onActivate: (Int) -> Unit = {}
    var onPickingChanged: (Boolean) -> Unit = {}
    var onSongsChanged: (List<Song>) -> Unit = {}
    var onResultsChangingChanged: (Boolean) -> Unit = {}
    /** 长按唱片 → 弹出该曲菜单（从歌单删除等）。 */
    var onSongLongPress: (Int) -> Unit = {}
    // ── 播放中黑胶：藏在该唱片套后面，播放时向右滑出（1.0r = 半张盘探出套外）并旋转 ──
    private var vinylSongId: String? = null
    private var vinylPlaying = false
    private var vinylAccentInt = 0xFFFFFFFF.toInt()
    private var vinylReveal = 0f
    private var vinylSpringV = 0f
    private var vinylAngle = 0f

    /** 由 Compose 侧同步播放状态：playingSongId 命中的唱片播放时滑出黑胶。 */
    fun setVinyl(songId: String?, playing: Boolean, accent: Int) {
        var changed = false
        if (vinylSongId != songId) { vinylSongId = songId; changed = true }
        if (vinylPlaying != playing) { vinylPlaying = playing; changed = true }
        if (vinylAccentInt != accent) { vinylAccentInt = accent; changed = true }
        if (changed) invalidate()
    }

    // ── 观摩模式（任务 3 重做）：全程原生画布内 3D，零层切换。
    // enterInspect()：其他唱片淡出滑走（inspectFade 沿用），选中唱片从「当前真实 quad」
    //   插值抽出至屏幕中央并放大 —— 全程同一套 setPolyToPoly 透视渲染，还是那张立体卡。
    // 观摩手势（onTouchEvent）：横向拖 = 绕 Y 轴 360°；纵向拖 = 绕 X 轴翻转（四面滑动）；
    //   松手指数衰减惯性 + 弹簧回最近正角度；点击卡片外空白 → exitInspect() 原位插回。──
    private var inspectingShelf = false
    private var inspectFade = 0f
    private var inspectTravel = 0f          // 抽出行程：0 = 原槽位，1 = 屏幕中央
    private var inspectTravelTarget = 0f
    private var inspectRotY = 0f            // 绕 Y 轴角度（度，可累计任意圈）
    private var inspectRotX = 0f            // 绕 X 轴角度（度）
    private var inspectSpinV = 0f           // Y 角速度（度/秒）
    private var inspectFlipV = 0f           // X 角速度（度/秒）
    private var inspectSpringing = false    // 惯性衰减结束后进入弹簧回正阶段
    private val inspectRestRotY = -18f      // 观摩默认 3D 姿态：初始 rotationY 微侧 18°（厚度透视），松手缓回该基准
    private var inspectStartQuad: FloatArray? = null
    // ── 抽出/放回单一 tween（任务 2/3）：抽出 700ms、放回 550ms，FastOutSlowIn(0.4,0,0.2,1) 曲线。
    // 放回 = 单一组合动画：角度归一到最近等效正角（不按做过的旋转倒转重放）后与行程
    // 共用同一条曲线并行插值，angle→0 与 quad→原位同时出发同时落定，全程无 snap/急促段。
    private var inspectTweenT = -1f          // -1 = 无 tween 进行中；0..1 = 进度
    private var inspectTweenDur = .7f
    private var inspectTweenExit = false
    private var inspectFromTravel = 0f
    private var inspectFromRotY = 0f
    private var inspectFromRotX = 0f
    private val inspectEasing = android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)
    // 收观摩退场淡回（修复「剩余唱片瞬现」）：放回落定后 inspectFade 从当前值 300ms 动画回 0，
    // 其余唱片平滑淡回，淡回落定后才清观摩标志。-1 = 无 tween；0..1 = 进度。
    private var inspectExitFadeT = -1f
    private var inspectExitFadeFrom = 0f
    /** Compose 侧同步：进入/退出观摩（用于隐藏底部控件、返回键接管）。 */
    var onInspectMode: (Boolean) -> Unit = {}

    /** 进入观摩：选中的唱片从原位动画抽出到屏幕中央（全程原生 3D 渲染，无 overlay）。 */
    fun enterInspect() {
        if (inspectingShelf || replacingResults || songs.isEmpty() || selected !in songs.indices) return
        preview(null); cancelPick()
        // 起点 = 选中卡当前在屏上的真实 quad（含斜切）——原位插值，首帧零跳变
        val q = FloatArray(8)
        val live = visible.firstOrNull { it.index == selected }
        if (live != null) System.arraycopy(live.quad, 0, q, 0, 8) else geometry().quad(0f, 1f, q)
        inspectStartQuad = q
        inspectingShelf = true
        inspectTravel = 0f; inspectTravelTarget = 1f
        inspectRotY = 0f; inspectRotX = 0f
        inspectSpinV = 0f; inspectFlipV = 0f; inspectSpringing = false
        // 抽出（任务 3）：单一 700ms FastOutSlowIn tween，取代原指数 approach（体感 ~500ms）
        inspectTweenExit = false; inspectTweenDur = .7f; inspectTweenT = 0f; inspectFromTravel = 0f
        inspectExitFadeT = -1f; inspectExitFadeFrom = 0f
        touching = false; dragging = false; scrubbing = false
        onInspectMode(true)
        invalidate()
    }

    /** 退出观摩：单一组合动画插回原槽位（角度→0 与行程→0 并行 550ms，落定后恢复常态绘制）。 */
    fun exitInspect() {
        if (!inspectingShelf) return
        // 已在退出流程（放回 tween / 退场淡回）中：忽略重复退出，避免重启动画
        if (inspectTweenExit && (inspectTweenT >= 0f || inspectExitFadeT >= 0f)) return
        // 已到中央才重取当前槽位 quad（中途返回则沿用进入时的起点，避免行程中跳位）；
        // 观摩期间即使列表定位变化，也插回此刻真实槽位
        if (inspectTravel > .95f) {
            val q = FloatArray(8)
            val live = visible.firstOrNull { it.index == selected }
            if (live != null) System.arraycopy(live.quad, 0, q, 0, 8) else geometry().quad(0f, 1f, q)
            inspectStartQuad = q
        }
        // 放回（任务 2 + 3）= 单一组合动画：角度先归一到最近等效正角（禁止按做过的旋转
        // 倒转重放一遍），再与行程共用同一条 550ms FastOutSlowIn 曲线并行插值 ——
        // angle→0 与 quad→原位同时出发、同时落定，无 snap/急促段（不再走弹簧回正）。
        inspectFromTravel = inspectTravel
        inspectFromRotY = inspectRotY - (inspectRotY / 360f).roundToInt() * 360f
        inspectFromRotX = inspectRotX - (inspectRotX / 360f).roundToInt() * 360f
        inspectRotY = inspectFromRotY; inspectRotX = inspectFromRotX
        inspectTweenExit = true; inspectTweenDur = .55f; inspectTweenT = 0f
        inspectTravelTarget = 0f
        inspectSpringing = false; inspectSpinV = 0f; inspectFlipV = 0f
        touching = false; dragging = false
        invalidate()
    }
    internal val displayedSongs get() = songs
    internal var replacingResults = false
        private set
    var topInset = 0f
    var bottomInset = 0f
    internal var position = 0f
        private set
    internal var renderedFrames = 0L
        private set
    private var songs = emptyList<Song>()
    private var configured = false
    private var pendingSongs: List<Song>? = null
    internal var resultsExitTime = -1f
        private set
    private var searchActive = false
    private var resultsPosition = 0f
    private data class ResultMotion(val song: Song, val from: FloatArray,
        val fromDistance: Float, val toDistance: Float?, val opacity: Float) {
        val sleeve = Sleeve().also { it.song = song; it.index = -1 }
    }
    private var resultsMotion = emptyList<ResultMotion>()
    private var target = 0f
    private var extraction = 0f
    private var velocity = 0f
    internal var running = false
        private set
    private var disposed = false
    private var lastFrame = 0L
    private var selected = -1
    private var touching = false
    private var dragging = false
    private var scrubbing = false
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var tracker: VelocityTracker? = null
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var longPressFired = false
    private val longPressRunnable = Runnable {
        if (touching && !dragging && !scrubbing && !replacingResults && songs.isNotEmpty()) {
            longPressFired = true
            preview(null)
            performHapticFeedbackSafe()
            onSongLongPress(Math.floorMod(position.roundToInt(), songs.size))
        }
    }
    private fun performHapticFeedbackSafe() {
        runCatching { performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS) }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val permits = Semaphore(4)
    private val requests = mutableMapOf<String, Job>()
    private val failed = mutableSetOf<String>()
    private class Texture(val image: Bitmap)
    private val textures = object : LruCache<String, Texture>(20 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Texture) = value.image.allocationByteCount
    }
    // ── 封面主色 → 卡片套面/边框着色（白黄基调随封面微变，用户想法）：
    // C++ nativeArtworkPalette 取主色；套面 = 主色 lerp 暖纸白 .7，边框 = 主色 lerp 暖黄 .6；
    // 异步提取（Dispatchers.Default），失败回默认白黄，缓存按 coverUrl。──
    private class SleeveTint(val face: Int, val frame: Int)
    private val defaultTint = SleeveTint(0xFFF6EFD8.toInt(), 0xFFF0D970.toInt())
    private val tints = LruCache<String, SleeveTint>(64)
    private val tintJobs = mutableSetOf<String>()
    private fun sleeveTint(url: String): SleeveTint = tints.get(url) ?: defaultTint
    private fun requestSleeveTint(url: String, bitmap: Bitmap) {
        if (url.isBlank() || tints.get(url) != null || !tintJobs.add(url)) return
        scope.launch(Dispatchers.Default) {
            val tint = runCatching {
                val w = 32; val h = 32
                val small = Bitmap.createScaledBitmap(bitmap, w, h, true)
                val pixels = IntArray(w * h)
                small.getPixels(pixels, 0, w, 0, 0, w, h)
                if (small !== bitmap) small.recycle()
                val dominant = NativeBridge.artworkPalette(pixels)?.first ?: vividAverage(pixels)
                    ?: return@runCatching null
                SleeveTint(
                    face = blendArgb(dominant, 0xFFF6EFD8.toInt(), .7f),
                    frame = blendArgb(dominant, 0xFFF0D970.toInt(), .6f))
            }.getOrNull()
            if (tint != null) tints.put(url, tint)
            tintJobs.remove(url)
            postInvalidateOnAnimation()
        }
    }
    /** C++ 取色不可用时的兜底：忽略近黑/近白像素后取均色；无有效像素返回 null（回默认白黄）。 */
    private fun vividAverage(pixels: IntArray): Int? {
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        for (c in pixels) {
            if (c ushr 24 < 128) continue
            val cr = c ushr 16 and 255; val cg = c ushr 8 and 255; val cb = c and 255
            val max = maxOf(cr, cg, cb); val min = minOf(cr, cg, cb)
            if (max < 28 || min > 225) continue
            r += cr; g += cg; b += cb; n++
        }
        if (n == 0L) return null
        return 0xFF000000.toInt() or ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
    }
    private fun blendArgb(from: Int, to: Int, amount: Float): Int {
        val a = amount.coerceIn(0f, 1f)
        fun ch(shift: Int) = ((from ushr shift and 255) * (1f - a) + (to ushr shift and 255) * a).roundToInt()
        return 0xFF000000.toInt() or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    fun setCustomTypeface(value: Typeface?) {
        val face = value ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
        if (textPaint.typeface != face) { textPaint.typeface = face; invalidate() }
    }
    private val matrix = Matrix()
    private val unit = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
    private class Sleeve { var index = 0; lateinit var song: Song; var distance = 0f; var entryBlur = 0f; var entryOpacity = 1f; val quad = FloatArray(8) }
    private val sleeves = Array(29) { Sleeve() }
    private val visible = ArrayList<Sleeve>(29)
    private val depthOfField = if (Build.VERSION.SDK_INT >= 31) ShelfDepthOfField() else null
    // 科技纹路来自 RecordShelfLighting AGSL 着色器（金属棱线/罗纹铰链）。按需求整体禁用：
    // 唱片边框只保留平滑黄白相间分段环，不再调用任何 shader。
    internal val hasLitMaterial get() = false
    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, 1f,
            intArrayOf(0x52080D12, 0x30080D12, 0x0C080D12, 0x00080D12),
            floatArrayOf(0f, .3f, .65f, 1f), Shader.TileMode.CLAMP)
    }
    private val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, 1f,
            intArrayOf(0x9206090C.toInt(), 0x4706090C, 0x0006090C),
            floatArrayOf(0f, .4f, 1f), Shader.TileMode.CLAMP)
    }
    private val rulerPointGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, 1f,
            intArrayOf(0x8CFFFFFF.toInt(), 0x3DFFFFFF, 0x10FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, .28f, .6f, 1f), Shader.TileMode.CLAMP)
    }
    private val maskPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
    // ── 黑胶唱片画笔（单位坐标：半径 1 的圆盘）──
    private val vinylDiscPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, 1f,
            intArrayOf(0xFF34363A.toInt(), 0xFF121315.toInt(), 0xFF292B2F.toInt()),
            floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
    }
    private val vinylGroovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = 0x14FFFFFF.toInt(); strokeWidth = .014f
    }
    private val vinylArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = 0x26FFFFFF.toInt(); strokeWidth = .035f
    }
    private val vinylLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val vinylHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF101113.toInt() }
    private val vinylBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val vinylClipPath = Path()
    // 软模糊保底（任务 0）：离屏小 Bitmap + BlurMaskFilter，跨帧复用
    private var softBlurBitmap: Bitmap? = null
    private var softBlurCanvas: android.graphics.Canvas? = null
    private val softBlurPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var requestedWindow = IntRange.EMPTY
    internal var introTime = 0f
        private set
    private var pickTime = -1f
    private var pickStart = 0f
    private var pickTarget = 0f
    private val edgePath = Path()
    private fun ease(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
    private fun cancelPick() {
        if (pickTime >= 0f) { pickTime = -1f; onPickingChanged(false) }
    }
    fun pick(index: Int) {
        if (replacingResults || index !in songs.indices) return
        preview(null)
        if (!ValueAnimator.areAnimatorsEnabled()) { select(index, true); onPickingChanged(false); return }
        pickStart = position
        pickTarget = index + ((position - index) / songs.size).roundToInt() * songs.size.toFloat()
        pickTime = 0f; onPickingChanged(true); invalidate()
    }
    private val density get() = resources.displayMetrics.density
    private val rulerY get() = height - bottomInset - 10f * density
    private fun geometry(): ShelfGeometry {
        val top = topInset + 76f * density
        return ShelfGeometry(width.toFloat(), top, maxOf(100f * density, rulerY - top - 24f * density), height.toFloat())
    }

    init { isClickable = true; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    fun configure(value: List<Song>, searching: Boolean = false) {
        val clearingSearch = searchActive && !searching
        searchActive = searching
        if (clearingSearch) {
            preview(null); cancelPick(); applySongs(value)
            return
        }
        if (pendingSongs == value) return
        if (pendingSongs != null) {
            beginResultsMotion(value)
            return
        }
        if (songs == value) return
        val sameRecords = songs.size == value.size && songs.indices.all {
            songs[it].id == value[it].id && songs[it].musicSource == value[it].musicSource
        }
        if (sameRecords) {
            songs = value; onSongsChanged(value); invalidate()
            return
        }
        preview(null)
        cancelPick()
        if (songs.isNotEmpty() && running && isAttachedToWindow && ValueAnimator.areAnimatorsEnabled()) {
            beginResultsMotion(value)
            target = position; velocity = 0f; touching = false; dragging = false; scrubbing = false
            tracker?.recycle(); tracker = null
            setResultsChanging(true)
            invalidate()
        } else applySongs(value)
    }

    private fun sameSong(a: Song, b: Song) = a.id == b.id && a.musicSource == b.musicSource

    /** Snapshot the actual on-screen poses, so typing again never restarts from a stale rack. */
    private fun beginResultsMotion(value: List<Song>) {
        val identity = songs.getOrNull(selected)
        val retained = value.indexOfFirst { identity != null && sameSong(it, identity) }
        resultsPosition = (if (retained >= 0) retained.toFloat() else position)
            .coerceIn(0f, maxOf(0, value.lastIndex).toFloat())
        val geometry = geometry()
        val destination = geometry.window(resultsPosition, value.size).map { logical ->
            value[Math.floorMod(logical, value.size)] to logical - resultsPosition
        }
        // Repeated query edits can interrupt an earlier replacement. Keep its nearest
        // poses, rather than accumulating another outgoing set on every keystroke.
        val old = visible.filter { sleeve -> sleeve.entryOpacity > .02f && sleeve.quad[0] < width && sleeve.quad[2] > 0f }
            .sortedBy { abs(it.distance) }.take(29)
        val motions = old.map { sleeve ->
            // A matching record remains on the shelf, even if its new slot is beyond the window.
            val targetDistance = destination.firstOrNull { sameSong(it.first, sleeve.song) }?.second
                ?: value.indexOfFirst { sameSong(it, sleeve.song) }.takeIf { it >= 0 }?.let {
                    val raw = it - resultsPosition
                    raw + ((sleeve.distance - raw) / value.size).roundToInt() * value.size
                }
            ResultMotion(sleeve.song, sleeve.quad.copyOf(), sleeve.distance, targetDistance, sleeve.entryOpacity)
        }.toMutableList()
        destination.filter { (song, _) -> old.none { sameSong(it.song, song) } }.forEach { (song, distance) ->
            val q = FloatArray(8)
            geometry.quad(distance, 0f, q)
            val offset = geometry.entryOffset(0f, distance, q[0])
            for (corner in 0..3) {
                q[corner * 2] += offset
                q[corner * 2 + 1] += geometry.entryRise(offset)
            }
            motions += ResultMotion(song, q, distance, distance, .12f)
        }
        resultsMotion = motions
        pendingSongs = value; resultsExitTime = 0f
        requestTextureUrls(motions.sortedBy { abs(it.toDistance ?: it.fromDistance) }.map { it.song.cover })
        setResultsChanging(true); invalidate()
    }

    private fun setResultsChanging(value: Boolean) {
        if (replacingResults == value) return
        replacingResults = value; onResultsChangingChanged(value)
    }

    private fun applySongs(value: List<Song>, enter: Boolean = true) {
        val animateResults = configured && value.isNotEmpty() && ValueAnimator.areAnimatorsEnabled()
        configured = true
        val identity = songs.getOrNull(selected)
        songs = value
        pendingSongs = null; resultsExitTime = -1f; resultsMotion = emptyList()
        visible.clear()
        introTime = if (enter && ValueAnimator.areAnimatorsEnabled()) 0f else 1.85f
        extraction = 0f
        val retained = value.indexOfFirst { it.id == identity?.id && it.musicSource == identity.musicSource }
        position = (if (retained >= 0) retained.toFloat() else position).coerceIn(0f, maxOf(0, songs.lastIndex).toFloat())
        target = position; velocity = 0f; selected = -1; requestedWindow = IntRange.EMPTY
        requests.values.toList().forEach(Job::cancel); requests.clear(); failed.clear()
        onSongsChanged(value)
        setResultsChanging(animateResults)
        notifySelection(); invalidate()
    }

    fun setRunning(value: Boolean) {
        if (running == value) return
        running = value; lastFrame = 0L
        if (!value) {
            preview(null); touching = false; velocity = 0f; tracker?.recycle(); tracker = null
            pendingSongs?.let { applySongs(it) }
        }
        else invalidate()
    }

    fun dispose() { preview(null); disposed = true; running = false; scope.cancel(); requests.clear(); textures.evictAll(); tracker?.recycle(); tracker = null
        softBlurCanvas = null; softBlurBitmap?.recycle(); softBlurBitmap = null }
    override fun onDetachedFromWindow() { lastFrame = 0L; super.onDetachedFromWindow() }

    /** Restore identity before the first frame; do not animate through or audition index zero. */
    internal fun restoreSelection(songId: String, source: String): Int {
        if (songs.isEmpty()) return -1
        val index = songs.indexOfFirst { it.id == songId && it.musicSource.name == source }.coerceAtLeast(0)
        preview(null)
        cancelPick()
        position = index.toFloat(); target = position; velocity = 0f
        selected = -1
        notifySelection(); invalidate()
        return index
    }

    fun select(index: Int, immediate: Boolean = false) {
        if (replacingResults || index !in songs.indices) return
        preview(null)
        cancelPick()
        velocity = 0f
        target = index + ((position - index) / songs.size).roundToInt() * songs.size.toFloat()
        if (immediate || !ValueAnimator.areAnimatorsEnabled()) { position = target; extraction = 1f; notifySelection() }
        invalidate()
    }

    fun travel(delta: Float) {
        if (replacingResults || songs.isEmpty()) return
        preview(null)
        cancelPick(); target += delta.roundToInt(); velocity = 0f
        if (!ValueAnimator.areAnimatorsEnabled()) { position = target; notifySelection() }
        invalidate()
    }

    private fun notifySelection() {
        if (songs.isEmpty()) return
        val index = Math.floorMod(position.roundToInt(), songs.size)
        if (index != selected) { selected = index; onSelection(index) }
    }

    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (inspectingShelf) return onTouchInspect(event)
        // 头部死区让路（修唱片架右上 X 退出钮点击无效）：触摸起点落在唱片区顶缘
        // （topInset + 76dp，同 geometry 卡区起点）之上时本 View 不消费该手势，
        // 让 Compose 头部的搜索/退出圆钮完整收到点击。此前恒 return true 吞掉
        // 全屏手势，头部两颗玻璃圆钮在 pointer 派发竞争下收不到落点。
        if (event.actionMasked == MotionEvent.ACTION_DOWN && event.y < topInset + 76f * density) return false
        if (replacingResults) return true
        if (songs.isEmpty()) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelPick()
                parent?.requestDisallowInterceptTouchEvent(true)
                touching = true; dragging = false; velocity = 0f
                longPressFired = false
                scrubbing = abs(event.y - rulerY) < 32f * density
                downX = event.x; downY = event.y; lastX = event.x; lastY = event.y
                tracker?.recycle(); tracker = VelocityTracker.obtain().also { it.addMovement(event) }
                if (scrubbing) scrub(event.x)
                else longPressHandler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                tracker?.addMovement(event)
                if (hypot(event.x - downX, event.y - downY) > slop) {
                    dragging = true; preview(null)
                    longPressHandler.removeCallbacks(longPressRunnable)
                }
                if (scrubbing) scrub(event.x)
                else if (dragging) {
                    position += (-(event.x - lastX) + (event.y - lastY) * .55f) / (width * .13f)
                    target = position.roundToInt().toFloat(); notifySelection()
                }
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                if (event.actionMasked == MotionEvent.ACTION_UP && !scrubbing && !longPressFired) {
                    if (dragging) {
                        tracker?.computeCurrentVelocity(1000)
                        velocity = ((-(tracker?.xVelocity ?: 0f) + (tracker?.yVelocity ?: 0f) * .55f) / (width * .13f)).coerceIn(-22f, 22f)
                        target = (position + velocity * .12f).roundToInt().toFloat()
                    } else {
                        performClick()
                        visible.asReversed().firstOrNull { contains(it.quad, event.x, event.y) }?.let { if (it.index == selected && extraction > .95f) onActivate(it.index) else select(it.index) }
                    }
                }
                touching = false; dragging = false; scrubbing = false
                tracker?.recycle(); tracker = null; parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        invalidate(); return true
    }

    private fun scrub(x: Float) {
        preview(null)
        position = ((x / width - .08f) / .84f).coerceIn(0f, 1f) * songs.lastIndex
        target = position.roundToInt().toFloat(); notifySelection()
    }

    private fun contains(q: FloatArray, x: Float, y: Float): Boolean {
        var sign = 0f
        for (i in 0..3) {
            val j = (i + 1) % 4
            val cross = (q[j * 2] - q[i * 2]) * (y - q[i * 2 + 1]) - (q[j * 2 + 1] - q[i * 2 + 1]) * (x - q[i * 2])
            if (i == 0) sign = cross else if (sign * cross < 0) return false
        }
        return true
    }

    // ── 观摩手势（任务 3）：横向拖 = 绕 Y 轴 360°，纵向拖 = 绕 X 轴翻转（四面滑动）；
    // 松手指数衰减惯性 → 低速并轨弹簧回最近正角度；点击卡片投影外空白 = 插回原槽位。──
    private fun onTouchInspect(event: MotionEvent): Boolean {
        // 放回 tween / 退场淡回阶段不接观摩手势（避免淡回途中再旋转/再触发退出）
        if (inspectTweenExit) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (abs(inspectTravel - inspectTravelTarget) > .02f) return true   // 进出场动画中不接手势
                parent?.requestDisallowInterceptTouchEvent(true)
                touching = true; dragging = false
                inspectSpringing = false; inspectSpinV = 0f; inspectFlipV = 0f
                downX = event.x; downY = event.y; lastX = event.x; lastY = event.y
                tracker?.recycle(); tracker = VelocityTracker.obtain().also { it.addMovement(event) }
            }
            MotionEvent.ACTION_MOVE -> {
                tracker?.addMovement(event)
                if (!dragging && hypot(event.x - downX, event.y - downY) > slop) dragging = true
                if (dragging) {
                    // 度/像素系数：整屏拖动 ≈ 360°+，纵横向同系数（四面都能翻到位）
                    inspectRotY += (event.x - lastX) * .42f
                    inspectRotX += (event.y - lastY) * .42f
                    inspectSpringing = false
                }
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (event.actionMasked == MotionEvent.ACTION_UP && !dragging) {
                    // 点击空白处（卡片投影外，含旋转后的投影）→ 原封不动插回原槽位
                    val travel = FloatArray(8); inspectCardQuad(travel)
                    val projected = FloatArray(8)
                    projectInspectQuad(inspectRotY, inspectRotX, travel, projected)
                    if (!contains(projected, event.x, event.y)) {
                        touching = false
                        tracker?.recycle(); tracker = null
                        exitInspect(); invalidate(); return true
                    }
                }
                if (dragging && ValueAnimator.areAnimatorsEnabled()) {
                    tracker?.computeCurrentVelocity(1000)
                    // 惯性（度/秒）：帧循环内 exponentialDecay 积分，低速自动并轨弹簧
                    inspectSpinV = (tracker?.xVelocity ?: 0f) * .42f
                    inspectFlipV = (tracker?.yVelocity ?: 0f) * .42f
                    inspectSpringing = false
                } else {
                    inspectSpringing = true; inspectSpinV = 0f; inspectFlipV = 0f
                }
                touching = false; dragging = false
                tracker?.recycle(); tracker = null
            }
        }
        invalidate()
        return true
    }

    /** 观摩物理步进：惯性 exponentialDecay（摩擦 2.2/s）→ 低速并轨弹簧（k=150，ζ≈.9）
     *  回最近正角度（180° 倍数，不停侧棱）；插回行程中角度一律弹回 0（原样插回）。
     *  返回 true = 角度仍在动（帧钟续帧）。 */
    private fun stepInspectPhysics(dt: Float, animate: Boolean): Boolean {
        val zeroing = inspectTravelTarget == 0f
        val ty = if (zeroing) 0f else ((inspectRotY - inspectRestRotY) / 180f).roundToInt() * 180f + inspectRestRotY
        val tx = if (zeroing) 0f else (inspectRotX / 180f).roundToInt() * 180f
        if (!animate) {
            if (inspectSpringing || zeroing) {
                inspectRotY = ty; inspectRotX = tx
                inspectSpinV = 0f; inspectFlipV = 0f; inspectSpringing = false
            }
            return false
        }
        if (!inspectSpringing && (inspectSpinV != 0f || inspectFlipV != 0f)) {
            inspectRotY += inspectSpinV * dt; inspectRotX += inspectFlipV * dt
            val decay = exp(-2.2f * dt)
            inspectSpinV *= decay; inspectFlipV *= decay
            if (abs(inspectSpinV) < 26f && abs(inspectFlipV) < 26f) inspectSpringing = true
            return true
        }
        if (inspectSpringing) {
            val k = 150f; val c = 2f * .9f * sqrt(k)
            val ay = -k * (inspectRotY - ty) - c * inspectSpinV
            val ax = -k * (inspectRotX - tx) - c * inspectFlipV
            inspectSpinV += ay * dt; inspectFlipV += ax * dt
            inspectRotY += inspectSpinV * dt; inspectRotX += inspectFlipV * dt
            if (abs(inspectRotY - ty) < .06f && abs(inspectSpinV) < .06f) { inspectRotY = ty; inspectSpinV = 0f }
            if (abs(inspectRotX - tx) < .06f && abs(inspectFlipV) < .06f) { inspectRotX = tx; inspectFlipV = 0f }
            return inspectSpinV != 0f || inspectFlipV != 0f
        }
        return false
    }

    /** 观摩行程 quad：原槽位 quad（含真实斜切）→ 屏幕中央放大正矩形，ease 插值。 */
    private fun inspectCardQuad(out: FloatArray) {
        val g = geometry()
        val start = inspectStartQuad
        if (start == null) { g.quad(0f, 1f, out); return }
        val stageH = maxOf(100f * density, rulerY - g.top - 24f * density)
        val cy = g.top + stageH * .5f
        val w = g.caseScale * .58f
        val h = w * (.45f / .435f)
        val l = width * .5f - w * .5f; val t = cy - h * .5f
        val target = floatArrayOf(l, t, l + w, t, l + w, t + h, l, t + h)
        val p = ease(inspectTravel)
        for (i in 0..7) out[i] = start[i] + (target[i] - start[i]) * p
    }

    /** 观摩卡 3D 投影：卡面四角（相对卡中心）绕 Y/X 旋转 + 透视除法，输出 setPolyToPoly
     *  目标角点（顺序 = unit 的 TL,TR,BR,BL）。rot=0 时 s=1 → 输出与输入 quad 逐点相等
     *  （含槽位斜切），入场首帧零跳变；透视焦距 = 5 倍卡半径，翻面/侧棱连续无跳变。 */
    private fun projectInspectQuad(rotYDeg: Float, rotXDeg: Float, quad: FloatArray, out: FloatArray) {
        val ry = Math.toRadians(rotYDeg.toDouble()); val rx = Math.toRadians(rotXDeg.toDouble())
        val cosY = cos(ry).toFloat(); val sinY = sin(ry).toFloat()
        val cosX = cos(rx).toFloat(); val sinX = sin(rx).toFloat()
        val cx = (quad[0] + quad[2] + quad[4] + quad[6]) * .25f
        val cy = (quad[1] + quad[3] + quad[5] + quad[7]) * .25f
        var extent = 1f
        for (c in 0..3) {
            extent = maxOf(extent, abs(quad[c * 2] - cx), abs(quad[c * 2 + 1] - cy))
        }
        val focal = extent * 5f
        for (c in 0..3) {
            val lx = quad[c * 2] - cx; val ly = quad[c * 2 + 1] - cy
            val x1 = lx * cosY                    // Ry：x' = lx cosY + lz sinY（lz = 0）
            val z1 = -lx * sinY                   //      z' = -lx sinY
            val y2 = ly * cosX - z1 * sinX        // Rx：y' = ly cosX - z' sinX
            val z2 = ly * sinX + z1 * cosX        //      z''= ly sinX + z' cosX
            val s = focal / (focal + z2).coerceAtLeast(focal * .18f)   // 透视除法（防除零）
            out[c * 2] = cx + x1 * s
            out[c * 2 + 1] = cy + y2 * s
        }
    }

    /** 观摩卡渲染：行程 quad + 3D 旋转投影，同一张立体卡（套 + 右缘黑胶月牙一体）。
     *  正面 = 黑胶月牙垫底、套面在上；背面 = 套背在上、月牙翻到左缘盖在其上（物理翻转）。 */
    private fun drawInspectRecord(canvas: Canvas) {
        if (selected !in songs.indices) return
        val quad = FloatArray(8)
        inspectCardQuad(quad)
        val song = songs[selected]
        val texture = textures.get(song.cover)
        val g = geometry()
        // 卡下软影：贴投影 quad 底缘，随行程淡入
        val midX = (quad[0] + quad[2] + quad[4] + quad[6]) * .25f
        val bottom = maxOf(quad[5], quad[7])
        val shadow = canvas.save()
        canvas.translate(midX + g.caseScale * .012f, bottom - g.caseScale * .012f)
        canvas.scale(g.caseScale * .30f, g.caseScale * .028f)
        floorPaint.alpha = (105f * ease(inspectTravel)).roundToInt()
        canvas.drawCircle(0f, 0f, 1f, floorPaint)
        canvas.restoreToCount(shadow)
        val projected = FloatArray(8)
        projectInspectQuad(inspectRotY, inspectRotX, quad, projected)
        if (!matrix.setPolyToPoly(unit, 0, projected, 0, 4)) return
        val facingFront = cos(Math.toRadians(inspectRotY.toDouble())).toFloat() *
            cos(Math.toRadians(inspectRotX.toDouble())).toFloat() > 0f
        val tint = sleeveTint(song.cover)
        // 黑胶月牙随行程滑出：入场从架面原状态（播放中=已伸出）平滑到全伸出，退出反向收回
        val crescent = maxOf(if (vinylHost(song)) vinylReveal else 0f, ease(inspectTravel))
        val save = canvas.save()
        canvas.concat(matrix)
        if (facingFront) {
            drawVinyl(canvas, song, texture, g.caseScale, projected, selectedCard = true, reveal = crescent)
            drawSleeve(canvas, song, texture, true, g.caseScale, tint)
        } else {
            drawSleeveBack(canvas, song, tint)
            drawVinyl(canvas, song, texture, g.caseScale, projected, selectedCard = true, reveal = crescent)
        }
        // 光照随角度变化：偏离正对角度越大表面越暗（0° 全亮，90° 最暗），强化 3D 体积感
        val litDeg = (abs(inspectRotY) % 180f).let { if (it > 90f) 180f - it else it }
        val shadeAlpha = (litDeg / 90f * 88f).roundToInt().coerceIn(0, 88)
        if (shadeAlpha > 0) {
            paint.shader = null; paint.style = Paint.Style.FILL
            paint.color = Color.BLACK; paint.alpha = shadeAlpha
            canvas.drawRect(0f, 0f, 1f, 1f, paint)
            paint.alpha = 255
        }
        canvas.restoreToCount(save)
    }

    /** 观摩卡背面：素套背 + 内衬铭牌（曲名/歌手/专辑）+ 同规格边框。
     *  投影矩阵翻面时内容已镜像，这里先镜像单元空间一次，背面文字保持可读。 */
    private fun drawSleeveBack(canvas: Canvas, song: Song, tint: SleeveTint) {
        canvas.translate(1f, 0f); canvas.scale(-1f, 1f)
        paint.shader = null; paint.style = Paint.Style.FILL
        paint.color = tint.face
        canvas.drawRect(0f, 0f, 1f, 1f, paint)
        paint.color = 0xFF15171B.toInt()
        canvas.drawRect(.041f, .032f, .977f, .975f, paint)
        paint.color = blendArgb(tint.face, 0xFF15171B.toInt(), .35f)
        canvas.drawRect(.075f, .075f, .935f, .93f, paint)
        val label = canvas.save()
        canvas.scale(1f / 1024f, 1f / 1024f)
        textPaint.textSize = 46f; textPaint.color = 0xFFB8A45E.toInt()
        fun fit(text: String, length: Float): String {
            val count = textPaint.breakText(text, true, length, null)
            return if (count < text.length) text.take(maxOf(0, count - 1)) + "…" else text
        }
        canvas.drawText(fit(song.name, 820f), 102f, 430f, textPaint)
        textPaint.textSize = 30f
        canvas.drawText(fit(song.artists, 820f), 102f, 505f, textPaint)
        textPaint.textSize = 25f
        canvas.drawText(fit(song.album, 820f), 102f, 570f, textPaint)
        canvas.restoreToCount(label)
        paint.style = Paint.Style.STROKE
        paint.color = tint.frame; paint.strokeWidth = .020f
        canvas.drawRect(.041f, .032f, .977f, .975f, paint)
        paint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (disposed || width == 0 || height == 0) return
        renderedFrames++
        val now = System.nanoTime()
        val dt = if (lastFrame == 0L) 1f / 60f else ((now - lastFrame) / 1e9f).coerceIn(0f, .05f)
        lastFrame = now
        val previousPosition = position
        val animate = ValueAnimator.areAnimatorsEnabled()
        if (pendingSongs != null) {
            resultsExitTime += dt
            if (!animate || resultsExitTime >= .52f) {
                position = resultsPosition
                applySongs(requireNotNull(pendingSongs), enter = false)
            }
        } else {
            introTime = if (animate) minOf(1.85f, introTime + dt) else 1.85f
            if (!animate || introTime >= 1.4f && extraction >= .98f) setResultsChanging(false)
        }
        if (pickTime >= 0f) {
            pickTime += dt
            if (pickTime >= .18f) {
                position = pickStart + (pickTarget - pickStart) * ease((pickTime - .18f) / .88f)
                target = position
            }
            if (pickTime >= 1.52f) { target = pickTarget; position = target; pickTime = -1f; onPickingChanged(false) }
        } else if (!touching) position = if (animate) shelfApproach(position, target, dt, 9f) else target
        notifySelection()
        val extracted = if (pendingSongs != null || introTime < 1.25f || touching && dragging ||
            abs(position - target) > .035f || pickTime in 0f..1.06f) 0f else 1f
        extraction = if (pendingSongs != null) 0f
            else if (animate) shelfApproach(extraction, extracted, dt, 10.5f) else extracted
        preview(selected.takeIf { running && !replacingResults && it in songs.indices && extracted == 1f && extraction > .12f && !scrubbing })
        // ── 播放中黑胶：等唱片架完全停稳（滚动/弹簧结束 + 唱片抽出完成）才滑出；
        // 滚动中、结果替换中、pick 动画中一律保持收起（用户实测「没停稳就开始抽」）。
        // 滑出本身 = settle 完成后启动物理弹簧（任务 5，替换原指数 approach）：
        // 临界阻尼 k=170 / ζ≈.85，起滑带轻微弹性衔接；滚动/拖拽立即把目标归 0，
        // 同一条弹簧平滑收回，不与滚动抢帧（帧钟只在弹簧未落定时续帧）。──
        val vinylTarget = if (vinylPlaying && extracted == 1f && extraction > .98f &&
            pendingSongs == null && pickTime < 0f && !touching && !dragging) 1f else 0f
        if (!animate) { vinylReveal = vinylTarget; vinylSpringV = 0f } else {
            val k = 170f; val c = 2f * .85f * sqrt(k)
            val a = -k * (vinylReveal - vinylTarget) - c * vinylSpringV
            vinylSpringV += a * dt
            vinylReveal += vinylSpringV * dt
            if (vinylTarget == 0f && vinylReveal <= 0f && vinylSpringV <= 0f) { vinylReveal = 0f; vinylSpringV = 0f }
            if (vinylTarget == 1f && vinylReveal >= 1f && vinylSpringV >= 0f) { vinylReveal = 1f; vinylSpringV = 0f }
        }
        if (vinylPlaying && vinylReveal > 0f) vinylAngle = (vinylAngle + 30f * dt) % 360f
        // ── 抽出观摩让位淡出（任务 4）：随帧接近目标，退出时反向淡回 ──
        // 退场淡回 tween 进行中由 tween 精确驱动 inspectFade→0，不再叠加 approach
        val inspectTarget = if (inspectingShelf) 1f else 0f
        if (inspectExitFadeT < 0f) inspectFade = if (animate) shelfApproach(inspectFade, inspectTarget, dt, 8f) else inspectTarget
        // ── 观摩帧步进（任务 3 重做）：抽出行程插值 + 惯性衰减/弹簧回正，全程同一帧钟 ──
        var inspectAnimating = false
        if (inspectingShelf) {
            if (inspectTweenT >= 0f) {
                // 抽出/放回单一 tween（任务 2/3）：一条 FastOutSlowIn 曲线同时驱动行程与角度，
                // 按帧钟推进（与刷新率无关），落定即静帧
                inspectTweenT = if (animate) minOf(1f, inspectTweenT + dt / inspectTweenDur) else 1f
                val p = inspectEasing.getInterpolation(inspectTweenT)
                inspectTravel = inspectFromTravel + (inspectTravelTarget - inspectFromTravel) * p
                if (inspectTweenExit) {
                    inspectRotY = inspectFromRotY * (1f - p)
                    inspectRotX = inspectFromRotX * (1f - p)
                } else {
                    // 抽出途中同步立起默认 3D 姿态：抵达中央时 rotationY = -18°（微侧、有厚度透视）
                    inspectRotY = inspectRestRotY * p
                }
                inspectAnimating = inspectTweenT < 1f
                if (inspectTweenT >= 1f) inspectTweenT = -1f
            } else if (inspectExitFadeT >= 0f) {
                // 收观摩退场淡回（修复「剩余唱片瞬现」）：其余唱片沿 FastOutSlowIn 300ms
                // 平滑淡回，帧钟续帧；落定后由下方落定块清观摩标志（复位改动画，
                // 不再一帧强推 inspectFade=0 造成瞬现）
                inspectExitFadeT = if (animate) minOf(1f, inspectExitFadeT + dt / .3f) else 1f
                inspectFade = inspectExitFadeFrom * (1f - inspectEasing.getInterpolation(inspectExitFadeT))
                inspectAnimating = inspectExitFadeT < 1f
                if (inspectExitFadeT >= 1f) { inspectExitFadeT = -1f; inspectFade = 0f }
            } else {
                inspectTravel = if (animate) shelfApproach(inspectTravel, inspectTravelTarget, dt, 9f) else inspectTravelTarget
                inspectAnimating = abs(inspectTravel - inspectTravelTarget) > 1e-4f
                inspectAnimating = stepInspectPhysics(dt, animate) or inspectAnimating
            }
            // 插回落定（行程归 0 且角度静止）：先启动 300ms 退场淡回（复位改动画，
            // 其余唱片平滑淡回），淡回落定后才结束观摩会话并清全部 inspect 残留（无缝交接）
            if (!inspectAnimating && inspectTravelTarget == 0f && abs(inspectTravel) < 1e-4f) {
                if (animate && inspectFade > .003f) {
                    inspectExitFadeFrom = inspectFade; inspectExitFadeT = 0f; inspectAnimating = true
                } else {
                    inspectingShelf = false
                    inspectStartQuad = null
                    inspectFade = 0f
                    inspectSpinV = 0f; inspectFlipV = 0f; inspectSpringing = false
                    inspectExitFadeT = -1f; inspectExitFadeFrom = 0f
                    onInspectMode(false)
                    postInvalidate()
                }
            }
        }
        if (songs.isEmpty()) return
        val geometry = geometry()
        val window = geometry.window(position, songs.size)
        visible.clear()
        val stageTop = geometry.top
        val stageWidth = geometry.caseScale
        if (pendingSongs != null) {
            val progress = ease(resultsExitTime / .52f)
            for (motion in resultsMotion) {
                val sleeve = motion.sleeve
                val destination = motion.toDistance
                sleeve.distance = motion.fromDistance + ((destination ?: motion.fromDistance) - motion.fromDistance) * progress
                if (destination != null) {
                    geometry.quad(destination, 0f, sleeve.quad)
                    for (corner in 0..3) {
                        for (axis in 0..1) {
                            val i = corner * 2 + axis
                            sleeve.quad[i] = motion.from[i] + (sleeve.quad[i] - motion.from[i]) * progress
                        }
                    }
                    sleeve.entryOpacity = motion.opacity + (1f - motion.opacity) * progress
                } else {
                    val offset = geometry.exitOffset(resultsExitTime, motion.fromDistance, motion.from[0])
                    for (corner in 0..3) {
                        sleeve.quad[corner * 2] = motion.from[corner * 2] + offset
                        sleeve.quad[corner * 2 + 1] = motion.from[corner * 2 + 1] + geometry.entryRise(offset)
                    }
                    // 滑出淡出起点延后：前 40% 位移保持可见，接近完全出界才开始淡出
                    sleeve.entryOpacity = motion.opacity * (1f - ((progress - .4f) / .6f).coerceIn(0f, 1f))
                }
                visible.add(sleeve)
            }
            visible.sortByDescending { it.distance }
        } else for ((slot, logical) in window.reversed().withIndex()) {
            val index = Math.floorMod(logical, songs.size)
            val sleeve = sleeves[slot]; sleeve.index = index; sleeve.song = songs[index]; sleeve.distance = logical - position
            geometry.quad(sleeve.distance, if (index == selected) extraction else 0f, sleeve.quad)
            fun offsetAt(time: Float): Float = geometry.entryOffset(time, sleeve.distance, sleeve.quad[0])
            val motionTime = introTime
            val entranceX = offsetAt(motionTime)
            val entranceY = geometry.entryRise(entranceX)
            val previousX = offsetAt((motionTime - dt).coerceAtLeast(0f))
            sleeve.entryBlur = abs(previousX - entranceX) * .16f
            sleeve.entryOpacity = (1f - entranceX / geometry.viewportWidth).coerceIn(.12f, 1f)
            for (corner in 0..3) {
                sleeve.quad[corner * 2] += entranceX
                sleeve.quad[corner * 2 + 1] += entranceY
            }
            // 抽出观摩让位（任务 2 重做）：其他唱片先摞回堆（淡出+外滑），选中唱片原位保持
            //（exempt），随后由 overlay 整只抽出 —— 三段式：摞回 300ms → 抽出 → 拆解观摩。
            if (inspectFade > .003f && sleeve.index != selected) {
                val dir = if (sleeve.distance > 0f) 1f else -1f
                val drift = dir * inspectFade * stageWidth * (.22f + .045f * abs(sleeve.distance))
                for (corner in 0..3) {
                    sleeve.quad[corner * 2] += drift
                    sleeve.quad[corner * 2 + 1] += inspectFade * stageWidth * .015f
                }
                sleeve.entryOpacity *= (1f - inspectFade)
            }
            visible.add(sleeve)
        }
        // 验证 selected 与 distance 对应（复查「模糊反向」用）：每约 90 帧打一行中心卡对应关系，
        // tag = MINova-DOF；正常停稳时 centerIdx 必须等于 selected 且 centerDist≈0。
        if (renderedFrames % 90 == 0L) {
            val center = visible.firstOrNull { abs(it.distance) <= .5f }
            android.util.Log.d("MINova-DOF",
                "selected=$selected pos=${"%.2f".format(position)} " +
                    "centerIdx=${center?.index} centerDist=${center?.let { "%.2f".format(it.distance) }}")
        }
        // A case slides within its own plane. Keep the rack's back-to-front order:
        // nearer sleeves still cover its left edge until it clears their right edge.
        val entryMask = ease((introTime - .85f) / .35f)
        val maskBottom = (height - bottomInset * .2f) * (1f - entryMask) + rulerY * entryMask
        val layerTop = if (geometry.wide) 0f else stageTop - 20f * density
        val layerBottom = if (geometry.wide) height.toFloat() else maskBottom
        val shelfLayer = canvas.saveLayer(0f, layerTop, width.toFloat(), layerBottom, null)
        // Leave room for the 48 px blur and case shadows when rejecting off-screen work.
        val cullPadding = maxOf(stageWidth * .12f, 144f)
        fun onScreen(sleeve: Sleeve): Boolean {
            val q = sleeve.quad
            return maxOf(q[2], q[4]) + cullPadding > 0f && minOf(q[0], q[6]) - cullPadding < width &&
                maxOf(q[5], q[7]) + cullPadding > layerTop && minOf(q[1], q[3]) - cullPadding < layerBottom
        }
        // Shadows lie on the shared supporting plane, below the standing case edges.
        // Drawing them before the cases makes the foreground correctly occlude distant shadows.
        for (sleeve in visible) {
            if (!onScreen(sleeve)) continue
            // 观摩中：选中唱片由观摩卡渲染接管（行程 quad + 3D 投影），架面停画避免双层
            if (inspectingShelf && sleeve.index == selected) continue
            val q = sleeve.quad
            val pull = if (sleeve.index == selected) extraction else 0f
            val middleX = (q[4] + q[6]) * .5f
            val middleY = (q[5] + q[7]) * .5f
            val edgeAngle = Math.toDegrees(atan2((q[5] - q[7]).toDouble(), (q[4] - q[6]).toDouble())).toFloat()
            val save = canvas.save()
            canvas.translate(middleX + stageWidth * (.012f + pull * .009f),
                middleY + stageWidth * (.012f + pull * .010f))
            canvas.rotate(edgeAngle)
            canvas.scale(stageWidth * (.245f + pull * .018f), stageWidth * (.025f + pull * .016f))
            floorPaint.alpha = ((105f + pull * 105f) * sleeve.entryOpacity).roundToInt()
            canvas.drawCircle(0f, 0f, 1f, floorPaint)
            canvas.restoreToCount(save)
            val contact = canvas.save()
            canvas.translate(middleX + stageWidth * .004f, middleY + stageWidth * .004f)
            canvas.rotate(edgeAngle)
            canvas.scale(stageWidth * .233f, stageWidth * (.007f + pull * .002f))
            contactPaint.alpha = ((150f + pull * 50f) * sleeve.entryOpacity).roundToInt()
            canvas.drawCircle(0f, 0f, 1f, contactPaint)
            canvas.restoreToCount(contact)
        }
        for ((slot, sleeve) in visible.withIndex()) {
            if (!onScreen(sleeve)) continue
            // 观摩中：选中唱片由观摩卡渲染接管（行程 quad + 3D 投影），架面停画避免双层
            if (inspectingShelf && sleeve.index == selected) continue
            if (!matrix.setPolyToPoly(unit, 0, sleeve.quad, 0, 4)) continue
            val song = sleeve.song
            // 模糊/雾化的唯一基准 = distance 绝对值（sleeve.index 在结果替换动画中恒为 -1，
            // 作判据会让替换期间中心卡误吃 3dp 模糊而两边清晰 = 用户实测「中间糊两边清」根因）：
            // 中心（distance≈0）0 模糊；紧邻 ±1 张 3dp 轻模糊；更远的卡 0 模糊（清晰，
            // 按进深缩小 quad 已带），只叠加雾化 alpha。
            val slotDist = abs(sleeve.distance)
            // 焦外模糊（任务 0 真修 = 恢复原项目 defocus 曲线，替换掉只给 ±1 张 3dp 的缩水版）：
            // 近侧（distance<0）从 2 张起、远侧从 3.5 张起按 1.45 次幂增长，近侧系数 1.3 / 远侧 .9，
            // 单卡上限 18 * stageWidth/724，总上限 48px —— 与原项目同参数，观感明显可辨。
            // 选中卡恒 0（保持清晰）。
            val nearSide = sleeve.distance < 0f
            val outsideFocus = (slotDist - if (nearSide) 2f else 3.5f).coerceAtLeast(0f)
            val defocus = if (sleeve.index == selected) 0f else
                (outsideFocus.pow(1.45f) * if (nearSide) 1.3f else .9f).coerceAtMost(18f) * stageWidth / 724f
            // 进深雾化：地板提到 0.95 —— 几乎不透明，只有极深的卡才轻微退后
            val fog = (1f - (slotDist - 1.5f) * .01f).coerceAtLeast(.95f).coerceAtMost(1f)
            val drawOpacity = sleeve.entryOpacity * fog
            val motionBlur = if (extraction < .1f) abs(position - previousPosition) * stageWidth * .012f else 0f
            val blur = maxOf(defocus, motionBlur.coerceAtMost(12f), sleeve.entryBlur)
                .roundToInt().coerceAtMost(48)
            if (renderedFrames % 90L == 0L && sleeve.index % 3 == 0) android.util.Log.d(
                "MINova-DOF",
                "idx=" + sleeve.index + " dist=" + ("%.2f".format(sleeve.distance)) +
                    " slotDist=" + ("%.2f".format(slotDist)) + " defocusPx=" + ("%.1f".format(defocus)) +
                    " blur=" + blur + " hw=" + canvas.isHardwareAccelerated + " api=" + Build.VERSION.SDK_INT +
                    " fog=" + ("%.2f".format(fog)) + " pos=" + ("%.2f".format(position)))
            if (Build.VERSION.SDK_INT >= 31 && canvas.isHardwareAccelerated && blur > 0) {
                val q = sleeve.quad
                val padding = ceil(maxOf(stageWidth * .095f, blur * 3f)).toInt()
                // 黑胶右缘探出套外（1.0r 滑出 ≈ .19 套宽）：DOF 节点右界必须把它包住，
                // 否则 Android 12+ 上播放中唱片的盘缘会被 RenderNode 右缘裁掉（回看被截的根因之一）。
                val vinylPad = if (vinylHost(song)) ceil(stageWidth * .24f).toInt() else 0
                val left = maxOf(-padding, floor(minOf(q[0], q[6]) - padding).toInt())
                val right = minOf(width + padding + vinylPad, ceil(maxOf(q[2], q[4]) + padding + vinylPad).toInt())
                val upper = maxOf(-padding, floor(minOf(q[1], q[3]) - padding).toInt())
                val lower = minOf(height + padding, ceil(maxOf(q[5], q[7]) + padding).toInt())
                if (right <= left || lower <= upper) continue
                val dofVinyl = vinylHost(song)
                depthOfField?.draw(canvas, slot, left, upper, right, lower, blur, matrix, drawOpacity) { recording ->
                    if (dofVinyl) drawVinyl(recording, song, textures.get(song.cover), stageWidth, sleeve.quad, sleeve.index == selected)
                    drawSleeve(recording, song, textures.get(song.cover), false, stageWidth, sleeveTint(song.cover))
                }
            } else if (blur > 0) {
                // 保底软模糊（任务 0）：RenderEffect 不可用（API<31 / 软渲染）时，
                // 套面内容离屏渲染到 1/4 尺寸 Bitmap，再以 BlurMaskFilter 放大回绘 ——
                // 全版本保证侧卡模糊可见，不再静默降级成无模糊直绘。
                val q = sleeve.quad
                val pad = blur * 3f + stageWidth * .06f
                val vinylPad = if (vinylHost(song)) stageWidth * .24f else 0f
                val left = (minOf(q[0], q[6]) - pad).toInt().coerceAtLeast(0)
                val top = (minOf(q[1], q[3]) - pad).toInt().coerceAtLeast(0)
                val right = (maxOf(q[2], q[4]) + pad + vinylPad).toInt().coerceAtMost(width)
                val bottom = (maxOf(q[5], q[7]) + pad).toInt().coerceAtMost(height)
                if (right > left && bottom > top) {
                    val w = right - left; val h = bottom - top
                    val smallW = maxOf(8, w / 4); val smallH = maxOf(8, h / 4)
                    var bmp = softBlurBitmap
                    if (bmp == null || bmp.width < smallW || bmp.height < smallH) {
                        softBlurBitmap?.recycle()
                        bmp = Bitmap.createBitmap(smallW, smallH, Bitmap.Config.ARGB_8888)
                        softBlurBitmap = bmp
                        softBlurCanvas = android.graphics.Canvas(bmp)
                    }
                    val scale = minOf(smallW.toFloat() / w, smallH.toFloat() / h)
                    val off = requireNotNull(softBlurCanvas)
                    bmp.eraseColor(Color.TRANSPARENT)
                    val offSave = off.save()
                    off.scale(scale, scale)
                    off.translate(-left.toFloat(), -top.toFloat())
                    off.concat(matrix)
                    if (vinylHost(song)) drawVinyl(off, song, textures.get(song.cover), stageWidth, sleeve.quad, sleeve.index == selected)
                    drawSleeve(off, song, textures.get(song.cover), sleeve.index == selected, stageWidth, sleeveTint(song.cover))
                    off.restoreToCount(offSave)
                    softBlurPaint.maskFilter = BlurMaskFilter(blur * scale, BlurMaskFilter.Blur.NORMAL)
                    softBlurPaint.alpha = (drawOpacity * 255).roundToInt()
                    canvas.drawBitmap(bmp, null,
                        RectF(left.toFloat(), top.toFloat(), left + smallW / scale, top + smallH / scale), softBlurPaint)
                }
            } else {
                val q = sleeve.quad
                val padding = stageWidth * .095f
                // 非 DOF 路径（blur=0 / API<31）的半透明层同样要为盘缘扩右界：
                // 探出量≈.19 套宽 > padding，drawOpacity<.999 时盘缘会被层界裁平 ——
                // 与 DOF 节点同规格的 vinylPad，任何 tint/路径下盘缘都完整。
                val vinylPad = if (vinylHost(song)) stageWidth * .24f else 0f
                val save = if (drawOpacity >= .999f) canvas.save() else canvas.saveLayerAlpha(
                    minOf(q[0], q[6]) - padding, minOf(q[1], q[3]) - padding,
                    maxOf(q[2], q[4]) + padding + vinylPad, maxOf(q[5], q[7]) + padding,
                    (drawOpacity * 255).roundToInt())
                canvas.concat(matrix)
                // 黑胶画在该唱片套之下：套盖住盘心，滑出右缘的部分露出（同 Deck 的藏盘效果）
                if (vinylHost(song)) drawVinyl(canvas, song, textures.get(song.cover), stageWidth, sleeve.quad, sleeve.index == selected)
                drawSleeve(canvas, song, textures.get(song.cover), sleeve.index == selected, stageWidth, sleeveTint(song.cover))
                canvas.restoreToCount(save)
            }
        }
        // Wide racks extend behind the scene to both edges; portrait retains its depth fade.
        maskPaint.shader = if (geometry.wide) LinearGradient(0f, 0f, width.toFloat(), 0f,
            intArrayOf(Color.TRANSPARENT, Color.BLACK, Color.BLACK, Color.TRANSPARENT),
            floatArrayOf(0f, .02f, .98f, 1f), Shader.TileMode.CLAMP)
        else LinearGradient(0f, stageTop - 20f * density, 0f, maskBottom,
            intArrayOf(Color.TRANSPARENT, Color.BLACK, Color.BLACK, Color.TRANSPARENT),
            floatArrayOf(0f, .055f, .93f, 1f), Shader.TileMode.CLAMP)
        canvas.drawPaint(maskPaint)
        canvas.restoreToCount(shelfLayer)
        // ── 观摩态（任务 3 重做）：背景轻微暗化 + 观摩卡，全部画在本视图画布内（零层切换）──
        if (inspectingShelf && inspectTravel > 0f) {
            paint.shader = null; paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.alpha = (78f * ease(inspectTravel)).roundToInt()
            canvas.drawPaint(paint)
            paint.alpha = 255
        }
        if (inspectingShelf && inspectStartQuad != null) drawInspectRecord(canvas)
        // 底部刻度尺已由 Compose 侧 GlowSeekBar（灰光横条）替代，原生刻度不再绘制。
        if (pendingSongs == null && requestedWindow != window) { requestedWindow = window; requestTextures(window) }
        if (running && animate && (pendingSongs != null || introTime < 1.85f || pickTime >= 0f || position != target ||
                extraction != extracted || vinylReveal != vinylTarget || abs(vinylSpringV) > .002f || inspectFade != inspectTarget ||
                // 观摩动画续帧（任务 3）：抽出行程 / 惯性 / 弹簧任一在动就继续喂帧
                inspectAnimating ||
                // 播放中黑胶持续旋转（任务 5）：reveal 已落定也要继续喂帧，否则 angle 冻结；
                // 暂停时 vinylPlaying=false 条件自动断开，帧钟停摆 = 角度保持
                (vinylPlaying && vinylReveal > .005f))) postInvalidateOnAnimation()
    }

    /** 该唱片是否正挂着播放中的黑胶（按 id 匹配，与浏览选中无关；观摩中随让位淡出，
     *  选中卡的观摩渲染用显式 reveal 画自己的黑胶，互不叠加）。 */
    private fun vinylHost(song: Song): Boolean =
        vinylSongId != null && song.id == vinylSongId && vinylReveal > .005f

    companion object {
        /** 播放态黑胶滑出量（单位半径 r 的倍数）：1.0r = 盘心移出套面一个半径，半张盘可见（任务 6）。 */
        const val VINYL_SLIDE_OUT = 1.0f
    }

    /** 单位坐标画黑胶：圆盘 + 唱纹 + 高光弧 + 标签（封面小圆随盘旋转）。
     *  圆心 = 边框（套）垂直中线：封面 contain 实际矩形 + 9dp 边距 = 边框矩形，取其水平中线；
     *  高度对齐套垂直中心（unit 0.5）；滑出方向右。selectedCard = 该卡是当前选中（抽出态）卡。 */
    private fun drawVinyl(canvas: Canvas, song: Song, texture: Texture?, caseScale: Float, quad: FloatArray,
        selectedCard: Boolean = false, reveal: Float = vinylReveal) {
        if (reveal <= .005f) return
        val r = .42f
        var coverL = .333f; var coverR = .976f
        texture?.let {
            val artWidth = .643f; val artHeight = .939f
            val bitmapRatio = it.image.width.toFloat() / it.image.height
            val physicalRatio = .435f / .45f
            val drawWidth = if (bitmapRatio > artWidth / artHeight * physicalRatio) artWidth
                else artHeight * bitmapRatio / physicalRatio
            coverL = .333f + (artWidth - drawWidth) / 2
            coverR = .333f + (artWidth + drawWidth) / 2
        }
        val marginX = 9f * density / (caseScale * .435f)
        val frameL = (coverL - marginX).coerceAtLeast(0f)
        val frameR = (coverR + marginX).coerceAtMost(1f)
        val frameCenterX = (frameL + frameR) / 2f
        // 屏幕右缘钳制（修「回看播放中的唱片，右侧伸出的黑胶被截断」）：
        // 该卡的 quad 左右缘都是竖直线 → 屏幕 x = quad[0] + u * 套宽（精确线性）。
        // 盘右缘 u = 盘心 + r；若越出视图右缘则按比例收敛滑出量 —— 播放中的唱片
        // 被抽出到最靠右的卡位时，月牙恰好完整收在页框内，任何卡位都不再被裁。
        val sleeveW = (quad[2] - quad[0]).coerceAtLeast(1f)
        // 页缘留白同时盖住宽架 mask 的右缘淡出（wide 横向渐变在 .98w 后开始变透明）。
        val pad = maxOf(2f * density, width * .025f)
        val uMax = (width - pad - quad[0]) / sleeveW
        // 钳制判据必须用「当前滑出量的实际盘缘」frameCenterX + 1.0r*reveal + r；
        // 旧判据 frameCenterX + r 少了滑出量 —— 播放卡停在 distance≈+4..+6 的卡位时
        // 盘缘已越界却不再收敛，月牙被视图右缘裁掉（截断回归的根因）。
        // 选中（抽出态）卡不做收敛（任务 1）：抽出位 x=.487W + 1.0r 月牙本就越出页框，
        // 收敛公式把 effReveal 压到 ~.1 = 用户实测「选中唱片黑胶不可见、未选中的反而可见」根因。
        // 选中卡整 1.0r 滑出：套盖盘心、右缘露出带标签/盘纹的月牙，越出页框部分由视图裁剪。
        val effReveal = if (!selectedCard && frameCenterX + VINYL_SLIDE_OUT * r * reveal + r > uMax) {
            (((uMax - frameCenterX - r) / (VINYL_SLIDE_OUT * r)).coerceIn(0f, 1f)).coerceAtMost(reveal)
        } else reveal
        if (effReveal <= .005f) return
        val save = canvas.save()
        // 标准滑出量 1.0r（任务 6，原 0.62r 月牙太含蓄）：盘心右移一个半径 =
        // 半张盘探出套外，一眼可见（右缘钳制收敛仍生效，任何卡位都不被裁）。
        canvas.translate(frameCenterX + VINYL_SLIDE_OUT * r * effReveal, .5f)
        canvas.rotate(vinylAngle)
        canvas.scale(r, r)
        canvas.drawCircle(0f, 0f, 1f, vinylDiscPaint)
        for (ring in 0..11) {
            vinylGroovePaint.alpha = if (ring % 3 == 0) 46 else 18
            canvas.drawCircle(0f, 0f, .44f + ring * .042f, vinylGroovePaint)
        }
        canvas.drawArc(-.84f, -.84f, .84f, .84f, 210f, 48f, false, vinylArcPaint)
        vinylLabelPaint.color = vinylAccentInt
        canvas.drawCircle(0f, 0f, .26f, vinylLabelPaint)
        texture?.let {
            vinylClipPath.reset()
            vinylClipPath.addCircle(0f, 0f, .24f, Path.Direction.CW)
            val clip = canvas.save()
            canvas.clipPath(vinylClipPath)
            canvas.drawBitmap(it.image, null, RectF(-.24f, -.24f, .24f, .24f), vinylBitmapPaint)
            canvas.restoreToCount(clip)
        }
        canvas.drawCircle(0f, 0f, .055f, vinylHolePaint)
        canvas.restoreToCount(save)
    }

    private fun drawSleeve(canvas: Canvas, song: Song, texture: Texture?, active: Boolean, caseScale: Float,
        tint: SleeveTint = defaultTint) {
        paint.shader = null; paint.style = Paint.Style.FILL
        // Back, top and side faces are smooth planes behind the cover. No AGSL lighting shader,
        // no bevels, no ribs: the user asked for a plain smooth shell.
        fun face(vararg points: Float) {
            edgePath.reset(); edgePath.moveTo(points[0], points[1])
            for (i in 2 until points.size step 2) edgePath.lineTo(points[i], points[i + 1])
            edgePath.close(); canvas.drawPath(edgePath, paint)
        }
        // 套面随封面主色微变（仍以暖纸白为基调）：边缘金晕 = 套面与边框色按原比例混合
        val faceEdge = blendArgb(tint.face, tint.frame, .45f)
        paint.color = tint.face
        canvas.drawRect(.045f, -.055f, 1.045f, .945f, paint)
        paint.shader = LinearGradient(0f, -.03f, 0f, .012f,
            intArrayOf(faceEdge, tint.face), null, Shader.TileMode.CLAMP)
        face(0f, 0f, .045f, -.055f, 1.045f, -.055f, 1f, 0f)
        paint.shader = LinearGradient(1f, 0f, 1.045f, 0f,
            intArrayOf(faceEdge, tint.face), null, Shader.TileMode.CLAMP)
        face(1f, 0f, 1.045f, -.055f, 1.045f, .945f, 1f, 1f)
        paint.shader = null
        // Smooth front face base colour. 全不透明：藏在套下的黑胶不再透出污染封面，
        // 黑胶只从套右缘露出月牙（用户要求封面完整、黑胶只露右缘）。
        paint.color = tint.face
        canvas.drawRect(0f, 0f, 1f, 1f, paint)
        // 封面实际渲染矩形（contain 后）：边框必须紧贴它，而不是大壳外框
        var coverL = .333f; var coverT = .034f; var coverR = .976f; var coverB = .973f
        // 封面腔深色内衬（原金底插槽）：contain 留白贴腔填充，不再裸露空容器
        paint.color = 0xFF15171B.toInt(); canvas.drawRect(.041f, .032f, .977f, .975f, paint)
        texture?.let {
            // 封面完整显示：等比缩小放进插入框（contain，不裁任何一边）。旧实现把封面按
            // 插入框高度撑满再左右裁切，方形封面只见中间约 2/3（实测「封面只显示一半」根因）。
            val save = canvas.save()
            canvas.clipRect(.333f, .034f, .976f, .973f)
            val artWidth = .643f; val artHeight = .939f
            val bitmapRatio = it.image.width.toFloat() / it.image.height
            // 单位坐标各向异性（x .435 / y .45）：绘制矩形按 physicalRatio 修正才不变形。
            val physicalRatio = .435f / .45f
            val drawWidth: Float; val drawHeight: Float
            if (bitmapRatio > artWidth / artHeight * physicalRatio) {
                // 宽图：宽度撑满，上下居中留白（留白处露出已画的金底插袋）
                drawWidth = artWidth; drawHeight = artWidth * physicalRatio / bitmapRatio
            } else {
                // 方图/高图：高度撑满，左右居中留白
                drawHeight = artHeight; drawWidth = artHeight * bitmapRatio / physicalRatio
            }
            paint.color = Color.WHITE
            coverL = .333f + (artWidth - drawWidth) / 2
            coverT = .034f + (artHeight - drawHeight) / 2
            coverR = .333f + (artWidth + drawWidth) / 2
            coverB = .034f + (artHeight + drawHeight) / 2
            canvas.drawBitmap(it.image, null, RectF(coverL, coverT, coverR, coverB), paint)
            canvas.restoreToCount(save)
        } ?: run {
            paint.shader = LinearGradient(.33f, 0f, 1f, 1f, 0xFFE8D48A.toInt(), 0xFFB8A45E.toInt(), Shader.TileMode.CLAMP)
            canvas.drawRect(.333f, .034f, .976f, .973f, paint); paint.shader = null
            textPaint.color = 0x88FFFFFF.toInt(); textPaint.textSize = .19f
            canvas.drawText("♪", .58f, .57f, textPaint)
        }
        // The printed insert is recessed under the cover: the lip occludes its upper/left edge.
        paint.shader = LinearGradient(0f, .029f, 0f, .066f, 0xA6000000.toInt(), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(.075f, .029f, .976f, .066f, paint)
        paint.shader = LinearGradient(.073f, 0f, .113f, 0f, 0x90000000.toInt(), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(.073f, .029f, .113f, .975f, paint); paint.shader = null
        val spine = canvas.save(); canvas.translate(.195f, .80f); canvas.rotate(-90f)
        // Android shapes glyphs at a real font size before the projected canvas scales them down.
        // Sub-pixel font sizes in unit coordinates quantize advances and collapse the lettering.
        canvas.scale(1f / 1024f, 1f / 1024f)
        textPaint.textSize = 53f; textPaint.color = 0xFFB8A45E.toInt()
        fun fit(text: String, length: Float): String {
            val count = textPaint.breakText(text, true, length, null)
            return if (count < text.length) text.take(maxOf(0, count - 1)) + "…" else text
        }
        canvas.drawText(fit(song.name, 700f), 0f, 0f, textPaint)
        textPaint.textSize = 31f; textPaint.color = 0xFFB8A45E.toInt()
        canvas.drawText(fit(song.artists, 700f), 0f, 68f, textPaint); canvas.restoreToCount(spine)
        val imprint = canvas.save(); canvas.scale(1f / 1024f, 1f / 1024f)
        textPaint.textSize = 25f; textPaint.color = 0xFFB8A45E.toInt()
        canvas.drawText(fit(song.album, 200f), 128f, 958f, textPaint); canvas.restoreToCount(imprint)
        // 边框：单色不透明 0xFFF0D970（暖黄）一整块平滑——无双环、无分段、无渐变；
        // 外圈只保留一圈柔和暖黄辉光（用户原话：不透明 不要任何条纹 就纯纯一整块平滑）。
        // 边框：紧贴封面实际渲染矩形（contain 后尺寸 + 统一小边距 ≈9dp），随封面实际大小走。
        // 材质 = 单色暖黄不透明主框（现有）+ 一层非常柔和的 glow，平滑无任何纹理/条纹。
        val marginX = 9f * density / (caseScale * .435f)
        val marginY = 9f * density / (caseScale * .45f)
        val frameL = (coverL - marginX).coerceAtLeast(0f)
        val frameT = (coverT - marginY).coerceAtLeast(0f)
        val frameR = (coverR + marginX).coerceAtMost(1f)
        val frameB = (coverB + marginY).coerceAtMost(1f)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.BUTT
        // 边框随封面主色微变（暖黄基调）：柔光圈用同色低透明版本
        // 选中态描边+辉光增强（任务 7）：选中卡外圈加一层更宽的暖黄辉光圈
        if (active) {
            paint.color = 0x2E shl 24 or (tint.frame and 0xFFFFFF)
            paint.strokeWidth = .095f
            canvas.drawRect(frameL, frameT, frameR, frameB, paint)
        }
        paint.color = ((if (active) 0x5C else 0x2E) shl 24 or (tint.frame and 0xFFFFFF))
        paint.strokeWidth = if (active) .052f else .045f
        canvas.drawRect(frameL, frameT, frameR, frameB, paint)
        paint.color = tint.frame; paint.strokeWidth = .020f
        canvas.drawRect(frameL, frameT, frameR, frameB, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawRuler(canvas: Canvas) {
        val normalized = ((position % songs.size) + songs.size) % songs.size
        val fraction = if (songs.size <= 1) .5f else (normalized / songs.lastIndex).coerceIn(0f, 1f)
        val left = width * .08f; val span = width * .84f
        for (tick in 0..88) {
            val f = tick / 88f
            val emphasis = exp(-((f - fraction) / .095f).pow(2))
            val h = width * (.009f + emphasis * .054f + if (tick % 5 == 0) .006f else 0f)
            paint.color = Color.WHITE; paint.alpha = (65f + emphasis * 150f).roundToInt(); paint.strokeWidth = density * .7f
            canvas.drawLine(left + f * span, rulerY, left + f * span, rulerY - h, paint)
        }
        val x = left + fraction * span
        val tipY = rulerY - width * .113f
        val pointGlow = canvas.save()
        canvas.translate(x, tipY); canvas.scale(7f * density, 7f * density)
        canvas.drawCircle(0f, 0f, 1f, rulerPointGlow)
        canvas.restoreToCount(pointGlow)
        paint.alpha = 255; paint.strokeWidth = density
        canvas.drawLine(x, rulerY, x, tipY, paint)
        canvas.drawCircle(x, tipY, 2f * density, paint)
    }

    private fun requestTextures(window: IntRange) {
        requestTextureUrls(window.sortedBy { abs(it - position) }.map { songs[Math.floorMod(it, songs.size)].cover })
    }

    private fun requestTextureUrls(urls: List<String>) {
        val wanted = urls.filter(String::isNotBlank).toSet()
        requests.keys.toList().filter { it !in wanted }.forEach { requests.remove(it)?.cancel() }
        // Nearest sleeves load first, including when jumping across thousands of songs.
        for (url in wanted) {
            if (url.isBlank() || textures.get(url) != null || url in requests || url in failed) continue
            requests[url] = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    permits.withPermit {
                        val result = SingletonImageLoader.get(context).execute(ImageRequest.Builder(context)
                            .data(coverRequestUrl(url, 384)).size(384).allowHardware(false).build())
                        if (result is SuccessResult) {
                            val texture = withContext(Dispatchers.Default) {
                                val image = result.image.toBitmap()
                                Texture(image)
                            }
                            textures.put(url, texture); requestSleeveTint(url, texture.image); invalidate()
                        } else failed.add(url)
                    }
                } finally { requests.remove(url) }
            }.also { it.start() }
        }
    }
}

/** Kept behind an API guard so Android 8-11 never resolve RenderEffect or allocate RenderNode arrays. */
@androidx.annotation.RequiresApi(31)
private class ShelfDepthOfField {
    private val nodes = MutableList(29) { RenderNode("record-sleeve-$it") }
    private val effects = Array(49) { radius ->
        if (radius == 0) null else RenderEffect.createBlurEffect(radius.toFloat(), radius.toFloat(), Shader.TileMode.DECAL)
    }
    fun draw(canvas: Canvas, slot: Int, left: Int, top: Int, right: Int, bottom: Int,
        blur: Int, matrix: Matrix, opacity: Float, content: (Canvas) -> Unit) {
        while (nodes.size <= slot) nodes.add(RenderNode("record-sleeve-${nodes.size}"))
        val node = nodes[slot]
        node.setPosition(left, top, right, bottom); node.setRenderEffect(effects[blur]); node.alpha = opacity
        val recording = node.beginRecording(right - left, bottom - top)
        recording.translate(-left.toFloat(), -top.toFloat()); recording.concat(matrix)
        content(recording); node.endRecording(); canvas.drawRenderNode(node)
    }
}
