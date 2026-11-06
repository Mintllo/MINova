package com.HyperX.MI.Na

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** 悬浮歌词可调参数（SharedPreferences）。 */
object FloatingLyricsPrefs {
    private const val PREF = "floating_lyrics"
    private fun sp(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context) = sp(ctx).getBoolean("enabled", false)
    fun setEnabled(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("enabled", v).apply() }

    /**
     * 锁定 = 触摸穿透 + 不可拖动 + 点击无效。
     * 解锁途径：通知中心「解锁」按钮 / 设置页。
     */
    fun isLocked(ctx: Context) = sp(ctx).getBoolean("locked", false)
    fun setLocked(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("locked", v).apply() }

    fun textColor(ctx: Context) = sp(ctx).getInt("text_color", Color.WHITE)
    fun setTextColor(ctx: Context, v: Int) { sp(ctx).edit().putInt("text_color", v).apply() }

    fun bgColor(ctx: Context) = sp(ctx).getInt("bg_color", Color.WHITE)
    fun setBgColor(ctx: Context, v: Int) { sp(ctx).edit().putInt("bg_color", v).apply() }

    /** 一次性迁移到「默认不透明白色」 */
    fun migrateDefaultWhite(ctx: Context) {
        val sp = sp(ctx)
        if (!sp.getBoolean("bg_white_default", false)) {
            val e = sp.edit()
                .putInt("bg_color", Color.WHITE)
                .putBoolean("bg_opaque", true)
                .putBoolean("bg_white_default", true)
            // 全新安装的默认「白底 + 白字」对比度是 1:1（完全看不见）。
            // 只在用户从未主动选过文字色（磁盘上没有该键）时把默认文字色改成深色；
            // 已经选过色的用户保持原样（他们的组合由 LyricContrast 自动兜底）。
            if (!sp.contains("text_color")) {
                e.putInt("text_color", Color.argb(255, 27, 27, 31))
            }
            e.apply()
        }
    }

    /** 只提供两态：不透明(255) / 透明(0) */
    fun isOpaque(ctx: Context) = sp(ctx).getBoolean("bg_opaque", true)
    fun setOpaque(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("bg_opaque", v).apply() }

    fun bgAlpha(ctx: Context) = if (isOpaque(ctx)) 255 else 0
    fun setBgAlpha(ctx: Context, v: Int) {
        sp(ctx).edit().putBoolean("bg_opaque", v >= 128).apply()
    }

    fun fontSize(ctx: Context) = sp(ctx).getInt("font_size", 16)
    fun setFontSize(ctx: Context, v: Int) { sp(ctx).edit().putInt("font_size", v.coerceIn(10, 40)).apply() }

    /** 是否在歌词下方显示翻译行 */
    fun showTranslation(ctx: Context) = sp(ctx).getBoolean("show_translation", true)
    fun setShowTranslation(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("show_translation", v).apply() }

    /** 应用动态色（由 App 侧写入，悬浮窗跟随动态颜色） */
    fun accentColor(ctx: Context) = sp(ctx).getInt("accent_color", Color.argb(255, 124, 158, 255))
    fun setAccentColor(ctx: Context, v: Int) { sp(ctx).edit().putInt("accent_color", v).apply() }

    /** 当前行是否使用动态色高亮 */
    fun useAccentForCurrent(ctx: Context) = sp(ctx).getBoolean("current_accent", true)
    fun setUseAccentForCurrent(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("current_accent", v).apply() }

    fun x(ctx: Context) = sp(ctx).getInt("pos_x", -1)
    fun setX(ctx: Context, v: Int) { sp(ctx).edit().putInt("pos_x", v).apply() }

    fun y(ctx: Context) = sp(ctx).getInt("pos_y", -1)
    fun setY(ctx: Context, v: Int) { sp(ctx).edit().putInt("pos_y", v).apply() }

    /* ================= 新增调整项（MD3 重构，保证每项都真实生效） ================= */

    /** 当前行是否加粗 */
    fun bold(ctx: Context) = sp(ctx).getBoolean("bold", true)
    fun setBold(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("bold", v).apply() }

    /** 是否显示上一句 / 下一句 */
    fun showPrevNext(ctx: Context) = sp(ctx).getBoolean("show_prev_next", true)
    fun setShowPrevNext(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("show_prev_next", v).apply() }

    /** 行间距（额外 dp，0..12） */
    fun lineSpacing(ctx: Context) = sp(ctx).getInt("line_spacing", 2)
    fun setLineSpacing(ctx: Context, v: Int) { sp(ctx).edit().putInt("line_spacing", v.coerceIn(0, 12)).apply() }

    /** 对齐：0 左 / 1 居中 / 2 右 */
    fun align(ctx: Context) = sp(ctx).getInt("align", 1)
    fun setAlign(ctx: Context, v: Int) { sp(ctx).edit().putInt("align", v.coerceIn(0, 2)).apply() }

    /** 背景不透明度 0..100（在不透明模式下按百分比缩放） */
    fun bgAlphaPercent(ctx: Context) = sp(ctx).getInt("bg_alpha_pct", 100)
    fun setBgAlphaPercent(ctx: Context, v: Int) { sp(ctx).edit().putInt("bg_alpha_pct", v.coerceIn(0, 100)).apply() }

    /** 卡片圆角 dp 0..32 */
    fun cornerRadiusDp(ctx: Context) = sp(ctx).getInt("corner_dp", 19)
    fun setCornerRadiusDp(ctx: Context, v: Int) { sp(ctx).edit().putInt("corner_dp", v.coerceIn(0, 32)).apply() }

    /** 卡片宽度占屏幕百分比 60..100 */
    fun cardWidthPercent(ctx: Context) = sp(ctx).getInt("card_width_pct", 100)
    fun setCardWidthPercent(ctx: Context, v: Int) { sp(ctx).edit().putInt("card_width_pct", v.coerceIn(60, 100)).apply() }

    /** 阴影强度 0..100 */
    fun shadowAlphaPercent(ctx: Context) = sp(ctx).getInt("shadow_pct", 30)
    fun setShadowAlphaPercent(ctx: Context, v: Int) { sp(ctx).edit().putInt("shadow_pct", v.coerceIn(0, 100)).apply() }

    /** 左右内边距 dp 8..40 */
    fun paddingHDp(ctx: Context) = sp(ctx).getInt("padding_h", 26)
    fun setPaddingHDp(ctx: Context, v: Int) { sp(ctx).edit().putInt("padding_h", v.coerceIn(8, 40)).apply() }

    /** 松手后自动吸附到屏幕左右边缘 */
    fun snapToEdge(ctx: Context) = sp(ctx).getBoolean("snap_edge", false)
    fun setSnapToEdge(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("snap_edge", v).apply() }
}

/**
 * 文字 / 底色对比度工具：保证「任意文字色 + 任意底色」组合下歌词都清晰可读。
 *
 * 规则：
 *  1. 浅色文字 → 底色自动压深；深色文字 → 底色自动提亮（只改亮度，尽量保留色相）。
 *  2. 文字额外加一层「与底色相反」的阴影，透明底色 / 半透明底色下也能看清。
 * 这样「选白色文字」不会再出现白字白底看不见的问题。
 */
object LyricContrast {

    /** 相对亮度（WCAG）。 */
    fun luminance(color: Int): Double {
        fun ch(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * ch(Color.red(color)) + 0.7152 * ch(Color.green(color)) + 0.0722 * ch(Color.blue(color))
    }

    /** 对比度（1.0 ~ 21.0）。 */
    fun ratio(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val hi = Math.max(la, lb)
        val lo = Math.min(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }

    fun isLight(color: Int): Boolean = luminance(color) > 0.55

    private fun mix(from: Int, to: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        fun ch(a: Int, b: Int) = Math.round(a + (b - a) * f)
        return Color.argb(
            Color.alpha(from),
            ch(Color.red(from), Color.red(to)),
            ch(Color.green(from), Color.green(to)),
            ch(Color.blue(from), Color.blue(to))
        )
    }

    /**
     * 把底色调整到与文字色对比度 >= [minRatio]（默认 4.5，WCAG AA）。
     * 已经达标时原样返回，不改变用户选择。
     */
    fun readableBg(fg: Int, bg: Int, minRatio: Double = 4.5): Int {
        val safeFg = Color.argb(255, Color.red(fg), Color.green(fg), Color.blue(fg))
        val safeBg = Color.argb(255, Color.red(bg), Color.green(bg), Color.blue(bg))
        if (ratio(safeFg, safeBg) >= minRatio) return bg
        val target = if (isLight(safeFg)) Color.BLACK else Color.WHITE
        var best = safeBg
        var bestRatio = ratio(safeFg, safeBg)
        for (step in 1..24) {
            val cand = mix(safeBg, target, step / 24f)
            val r = ratio(safeFg, cand)
            if (r > bestRatio) {
                bestRatio = r
                best = cand
            }
            if (r >= minRatio) break
        }
        return Color.argb(Color.alpha(bg), Color.red(best), Color.green(best), Color.blue(best))
    }

    /** 文字阴影色：浅色文字配深阴影，深色文字配浅阴影。 */
    fun shadowFor(fg: Int): Int =
        if (isLight(Color.argb(255, Color.red(fg), Color.green(fg), Color.blue(fg))))
            Color.argb(215, 0, 0, 0)
        else Color.argb(205, 255, 255, 255)
}

/** 歌词行/翻译行/播放状态的跨组件桥（ViewModel 写，悬浮服务/通知读）。 */
object FloatingLyricsState {
    @Volatile var prevLine: String = ""
    @Volatile var nextLine: String = ""
    @Volatile var currentLine: String = ""
    @Volatile var currentTranslation: String = ""
    @Volatile var songTitle: String = ""
    @Volatile var songArtist: String = ""
    @Volatile var isPlaying: Boolean = false

    fun canOverlay(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(ctx)
}

/**
 * 悬浮歌词前台服务。
 *
 * 形态一「歌词」：上一行淡出 / 当前行加粗高亮（动态色）/ 翻译行 / 下一行淡出，
 * 圆角 26dp 卡片，不透明时 1dp 描边，文字带阴影保证任意背景可读。
 * 形态二「面板」：点歌词弹出的快捷设置（MD3 图标胶囊按钮 + 分组标题 + 色板 + 字号 + 不透明|透明）。
 * 背景只有两态：不透明 / 透明（默认不透明），没有透明度滑杆。
 * 锁定：窗口完全穿透，点击无效，只能从通知中心「解锁」或设置页解除。
 * 全部按钮/图标均为矢量 drawable，绝不使用 emoji。
 * 内容切换（歌词 ⇄ 面板）只做 alpha/scale 过渡，不重建窗口根 view；
 * 尺寸变化走 updateViewLayout(root)；只有 flag（锁定/穿透）变化才 removeViewImmediate → re-add。
 */
class FloatingLyricsService : Service() {

    private lateinit var windowManager: WindowManager
    private var root: FrameLayout? = null
    private var lp: WindowManager.LayoutParams? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watchJob: Job? = null

    private var lyricView: LinearLayout? = null
    private var prevText: TextView? = null
    private var mainText: TextView? = null
    private var nextText: TextView? = null
    private var transText: TextView? = null
    private var ctrlRow: LinearLayout? = null
    private var playControlBtn: ImageView? = null

    /** 面板容器：常驻窗口内，内容原位替换；窗口根 view 绝不因内容切换而重建。 */
    private var panelHost: FrameLayout? = null

    /** 驱动「四行歌词同步切换」的唯一 animator（避免四行各自动画导致错位）。 */
    private var lineAnimator: ValueAnimator? = null

    /** 面板内容预量高度（px）：窗口高度按真实内容收放，底部控件不会被裁掉。 */
    private var panelContentHeight = 0

    private var showingPanel = false
    private var bgDrawable: GradientDrawable? = null
    private var lastFlags = 0

    /** 自动适配后的卡片底色（保证与文字色对比度达标）。 */
    private var adaptedBg = Color.WHITE

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        FloatingLyricsPrefs.migrateDefaultWhite(this)
        startForegroundInternal()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                FloatingLyricsPrefs.setEnabled(this, false)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_LOCK -> {
                val nv = !FloatingLyricsPrefs.isLocked(this)
                FloatingLyricsPrefs.setLocked(this, nv)
                showLyric()
                updateNotification()
                return START_STICKY
            }
            ACTION_APPLY -> {
                // 设置面板改了配置：立刻重排外观（无需等歌词刷新）
                restyleInPlace()
                applyWindow()
                return START_STICKY
            }
        }
        ensureOverlay()
        watch()
        return START_STICKY
    }

    private fun channelId(): String {
        val id = "minova_floating_lyrics"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(id) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(id, "悬浮歌词", NotificationManager.IMPORTANCE_MIN).apply {
                        setShowBadge(false); enableVibration(false)
                    }
                )
            }
        }
        return id
    }

    private fun buildServiceNotification(): Notification {
        val locked = FloatingLyricsPrefs.isLocked(this)
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(this, channelId())
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("悬浮歌词" + (if (locked) "（已锁定）" else "（使用中）"))
            .setContentText(
                if (locked) "锁定中：歌词点击穿透，点「解锁」恢复操作"
                else "点歌词可弹出快捷设置"
            )
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
        b.addAction(
            0,
            if (locked) "解锁" else "锁定",
            PendingIntent.getService(
                this, 11,
                Intent(this, FloatingLyricsService::class.java).setAction(ACTION_TOGGLE_LOCK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        b.addAction(
            0,
            "关闭",
            PendingIntent.getService(
                this, 12,
                Intent(this, FloatingLyricsService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        return b.build()
    }

    private fun startForegroundInternal() {
        val n = buildServiceNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIF_ID, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIF_ID, n)
            }
        } catch (_: Exception) {
            runCatching { startForeground(NOTIF_ID, n) }
        }
    }

    private fun updateNotification() {
        runCatching { startForeground(NOTIF_ID, buildServiceNotification()) }
    }

    /* ================= 尺寸 / 颜色工具 ================= */

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun withAlpha(c: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c))

    private fun blend(c: Int, target: Int, f: Float): Int {
        val r = (Color.red(c) + (Color.red(target) - Color.red(c)) * f).toInt().coerceIn(0, 255)
        val g = (Color.green(c) + (Color.green(target) - Color.green(c)) * f).toInt().coerceIn(0, 255)
        val b = (Color.blue(c) + (Color.blue(target) - Color.blue(c)) * f).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(c), r, g, b)
    }

    private fun lighten(c: Int, f: Float) = blend(c, Color.WHITE, f)
    private fun darken(c: Int, f: Float) = blend(c, Color.BLACK, f)

    /** 在给定底色上取可读的前景色（黑/白）。 */
    private fun readableOn(c: Int): Int {
        val lum = (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0
        return if (lum > 0.62) Color.BLACK else Color.WHITE
    }

    /**
     * 歌词行切换：旧句向上滑出、新句自下方滑入（上=上一句 / 中=当前句 / 下=下一句）。
     *
     * 关键：prev / main / trans / next 四行**由同一个 ValueAnimator 驱动**，
     * 同一时刻启动、同一时长、同一曲线，绝不会出现「主行已经切换、上一行还没动」的错位。
     * 前 40%（200ms，加速）旧句上滑淡出；后 60%（300ms，减速）新句自下方滑入；
     * 文案替换正好发生在两段的交界帧，四行在同一次回调里一起换字。
     */
    private fun applyLinesSmooth(items: List<Pair<TextView?, String>>) {
        val changed = items.mapNotNull { (tv, text) ->
            if (tv != null && tv.text?.toString() != text) tv to text else null
        }
        if (changed.isEmpty()) return
        lineAnimator?.cancel()
        val views = changed.map { it.first }
        // 清掉这些 view 上可能残留的单行动画，避免两套动画叠加出现错位
        views.forEach { it.animate().cancel() }

        // 四行统一位移量：按主行字号算，保证四行滑动距离一致（视觉最齐）
        val dy = ((mainText?.textSize ?: dp(16).toFloat()) * 0.85f).coerceAtLeast(dp(6).toFloat())
        val swapAt = 0.4f
        val accel = AccelerateInterpolator()
        val decel = DecelerateInterpolator()
        var swapped = false
        fun swapText() {
            if (swapped) return
            swapped = true
            changed.forEach { (tv, text) -> tv.text = text }
        }

        lineAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { a ->
                val p = a.animatedFraction
                if (p < swapAt) {
                    val e = accel.getInterpolation(p / swapAt)
                    views.forEach { v ->
                        v.alpha = 1f - e
                        v.translationY = -dy * e
                    }
                } else {
                    swapText()
                    val e = decel.getInterpolation((p - swapAt) / (1f - swapAt))
                    views.forEach { v ->
                        v.alpha = e
                        v.translationY = dy * (1f - e)
                    }
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    swapText()
                    views.forEach { v ->
                        v.alpha = 1f
                        v.translationY = 0f
                    }
                    if (lineAnimator === animation) lineAnimator = null
                }
            })
            start()
        }
    }

    /** 内容入场动画（淡入 + 轻微放大），切换面板/歌词更平滑。 */
    private fun animateIn(v: View) {
        v.animate().cancel()
        v.alpha = 0f
        v.scaleX = 0.965f
        v.scaleY = 0.965f
        v.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /* ================= 卡片背景（只有 不透明 / 透明 两态） ================= */

    private fun buildCardDrawable(): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()  // 标准矩形，小圆角
            setColor(Color.WHITE)  // 默认不透明白色
        }

    /** 当前行的实际文字色（跟随动态色时为强调色，否则为用户选色）。 */
    private fun effectiveMainColor(): Int =
        if (FloatingLyricsPrefs.useAccentForCurrent(this)) FloatingLyricsPrefs.accentColor(this)
        else FloatingLyricsPrefs.textColor(this)

    private fun applyCardBackground() {
        val d = bgDrawable ?: return
        val bg = FloatingLyricsPrefs.bgColor(this)
        val opaque = FloatingLyricsPrefs.isOpaque(this)

        // 圆角矩形（长方体）：统一纯色实底 —— 不用任何渐变，也不加阴影。
        // 播放中 / 暂停态外观完全一致，切换时不跳变。
        val corner = dp(FloatingLyricsPrefs.cornerRadiusDp(this)).toFloat()
        if (d.cornerRadius != corner) d.cornerRadius = corner
        val alphaPct = FloatingLyricsPrefs.bgAlphaPercent(this)
        fun scaled(a: Int) = (a * alphaPct / 100).coerceIn(0, 255)
        if (root?.elevation != 0f) root?.elevation = 0f
        if (root?.translationZ != 0f) root?.translationZ = 0f

        // 可读性兜底：底色按文字色亮度自动调深/调亮，任何配色都能看清。
        // （选白字 + 白底以前是对比度 1:1 完全看不见）
        adaptedBg = LyricContrast.readableBg(effectiveMainColor(), bg, 4.5)

        when {
            showingPanel -> {
                // 面板内部控件按「深色表面 + 白色文字/图标」设计，底色必须与白字形成对比，
                // 否则默认白底时面板会整片看不见。
                val panelBg = LyricContrast.readableBg(Color.WHITE, bg, 4.5)
                d.setColor(withAlpha(panelBg, scaled(246).coerceAtLeast(200)))
                d.setStroke(dp(1), Color.argb(48, 255, 255, 255))
            }
            opaque && alphaPct > 0 -> {
                d.setColor(withAlpha(adaptedBg, scaled(255)))
                d.setStroke(dp(1), Color.argb(40, 255, 255, 255))
            }
            else -> {
                d.setColor(Color.TRANSPARENT)
                d.setStroke(0, Color.TRANSPARENT)
            }
        }
        root?.background = d
    }
    /* ================= 视图构建 ================= */

    private fun buildLyricView(): LinearLayout {
        val l = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(26), dp(16), dp(26), dp(16))
            isClickable = true
            isFocusable = true
        }

        fun lineView(isMain: Boolean): TextView = TextView(this).apply {
            if (isMain) maxLines = 2 else maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
            includeFontPadding = false
            setLineSpacing(0f, 1.08f)
            isClickable = false
            isFocusable = false
            if (isMain) setTypeface(Typeface.create(typeface, Typeface.BOLD))
        }

        val prev = lineView(false).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(5) }
        }
        val main = lineView(true).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val trans = lineView(false).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(3) }
        }
        val next = lineView(false).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(5) }
        }
        l.addView(prev)
        l.addView(main)
        l.addView(trans)
        l.addView(next)
        prevText = prev
        mainText = main
        transText = trans
        nextText = next
        return l
    }

    /** MD3 带图标胶囊按钮（矢量图标，非 emoji）。 */
    private fun md3Chip(iconRes: Int, label: String, active: Boolean, onClick: () -> Unit): LinearLayout {
        val accent = FloatingLyricsPrefs.accentColor(this)
        val fg = if (active) readableOn(accent) else Color.WHITE
        val bg = GradientDrawable().apply {
            cornerRadius = dp(19).toFloat()
            setColor(if (active) accent else Color.argb(38, 255, 255, 255))
            if (!active) setStroke(dp(1), Color.argb(48, 255, 255, 255))
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = bg
            setPadding(dp(12), dp(8), dp(14), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(8) }
            addView(ImageView(this@FloatingLyricsService).apply {
                setImageResource(iconRes)
                setColorFilter(fg)
                layoutParams = LinearLayout.LayoutParams(dp(17), dp(17)).apply { marginEnd = dp(6) }
            })
            addView(TextView(this@FloatingLyricsService).apply {
                text = label
                textSize = 13f
                setTextColor(fg)
                setTypeface(typeface, if (active) Typeface.BOLD else Typeface.NORMAL)
            })
            isClickable = true
            setOnClickListener { onClick() }
        }
    }

    /** 圆形矢量图标按钮（收起 / 关闭）。 */
    private fun circleIconButton(iconRes: Int, tint: Int, onClick: () -> Unit): ImageView =
        ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(tint)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(38, 255, 255, 255))
                setStroke(dp(1), Color.argb(44, 255, 255, 255))
            }
            setPadding(dp(7), dp(7), dp(7), dp(7))
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(8) }
            isClickable = true
            setOnClickListener { onClick() }
        }

    /** 强调色圆形步进按钮（字号 ±）。 */
    private fun stepButton(iconRes: Int, onClick: () -> Unit): ImageView {
        val accent = FloatingLyricsPrefs.accentColor(this)
        return ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(readableOn(accent))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(withAlpha(accent, 235))
            }
            setPadding(dp(9), dp(9), dp(9), dp(9))
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
            isClickable = true
            setOnClickListener { onClick() }
        }
    }

    private fun sectionLabel(t: String): TextView =
        TextView(this).apply {
            text = t
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.argb(165, 255, 255, 255))
            setPadding(dp(2), dp(16), 0, dp(8))
        }

    private fun colorDot(color: Int, selected: Boolean, onClick: () -> Unit): View {
        val accent = FloatingLyricsPrefs.accentColor(this)
        val d = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(
                if (selected) dp(3) else dp(1),
                if (selected) accent else Color.argb(60, 255, 255, 255)
            )
        }
        return View(this).apply {
            background = d
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(11) }
            isClickable = true
            setOnClickListener { onClick() }
        }
    }

    /** 两态分段控件：不透明 | 透明。 */
    private fun segmentedTwoState(
        labels: List<String>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit
    ): LinearLayout {
        val accent = FloatingLyricsPrefs.accentColor(this)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(Color.argb(32, 255, 255, 255))
                setStroke(dp(1), Color.argb(40, 255, 255, 255))
            }
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        labels.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            row.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    cornerRadius = dp(18).toFloat()
                    setColor(if (selected) accent else Color.TRANSPARENT)
                }
                setPadding(dp(12), dp(7), dp(12), dp(7))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (i > 0) marginStart = dp(4)
                }
                addView(TextView(this@FloatingLyricsService).apply {
                    text = label
                    textSize = 13f
                    setTextColor(if (selected) readableOn(accent) else Color.WHITE)
                    setTypeface(typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
                    gravity = Gravity.CENTER
                })
                isClickable = true
                setOnClickListener { onSelect(i) }
            })
        }
        return row
    }

    private fun buildPanelView(): ScrollView {
        val accent = FloatingLyricsPrefs.accentColor(this)
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(20))
        }

        // ---- 标题栏：图标 + 标题 + 收起 + 关闭 ----
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        // 左上角音符图标：点它即收回歌词卡片。
        // 旧实现是纯装饰 ImageView（没有点击响应），且 head 的拖动监听在非按钮区域
        // ACTION_DOWN 就 return true 消费触摸、onTap 又是 null，所以「点了没反应」。
        // 这里把它做成真正的点击目标（34dp 触摸区 / 20dp 图形，视觉不变）。
        head.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_fl_lyrics_v2)
            setColorFilter(accent)
            setPadding(dp(7), dp(7), dp(7), dp(7))
            layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply { marginEnd = dp(9) }
            isClickable = true
            isFocusable = true
            contentDescription = "收起面板，回到歌词卡片"
            setOnClickListener { showLyric() }
        })
        head.addView(TextView(this).apply {
            text = "悬浮歌词"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        head.addView(circleIconButton(R.drawable.ic_fl_remove, Color.WHITE) { showLyric() })
        head.addView(circleIconButton(R.drawable.ic_fl_close, Color.WHITE) {
            FloatingLyricsPrefs.setEnabled(this@FloatingLyricsService, false)
            FloatingLyricsService.stop(this@FloatingLyricsService)
        })
        col.addView(head)

        // 面板可拖动（在标题栏上拖动移动窗口，正文区正常滚动）；
        // 标题栏上「未拖动 = 点击」同样收起面板，图标/标题区域都有明确反馈。
        lp?.let { p -> attachDrag(head, p) { showLyric() } }

        // ---- 显示 ----
        col.addView(sectionLabel("显示"))
        val opsRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        opsRow1.addView(md3Chip(R.drawable.ic_fl_lock, "锁定", FloatingLyricsPrefs.isLocked(this)) {
            FloatingLyricsPrefs.setLocked(this@FloatingLyricsService, true)
            updateNotification()
            showLyric()
        })
        opsRow1.addView(md3Chip(
            R.drawable.ic_fl_translate,
            if (FloatingLyricsPrefs.showTranslation(this)) "翻译 开" else "翻译 关",
            FloatingLyricsPrefs.showTranslation(this)
        ) {
            FloatingLyricsPrefs.setShowTranslation(
                this@FloatingLyricsService,
                !FloatingLyricsPrefs.showTranslation(this@FloatingLyricsService)
            )
            showPanel(animate = false)
        })
        col.addView(opsRow1)

        val opsRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        opsRow2.addView(md3Chip(
            R.drawable.ic_fl_palette,
            if (FloatingLyricsPrefs.useAccentForCurrent(this)) "动态色 开" else "动态色 关",
            FloatingLyricsPrefs.useAccentForCurrent(this)
        ) {
            FloatingLyricsPrefs.setUseAccentForCurrent(
                this@FloatingLyricsService,
                !FloatingLyricsPrefs.useAccentForCurrent(this@FloatingLyricsService)
            )
            showPanel(animate = false)
        })
        col.addView(opsRow2)

        // ---- 背景显示：只有两态 ----
        col.addView(sectionLabel("背景显示"))
        col.addView(segmentedTwoState(
            listOf("不透明", "透明"),
            if (FloatingLyricsPrefs.isOpaque(this)) 0 else 1
        ) { i ->
            FloatingLyricsPrefs.setOpaque(this@FloatingLyricsService, i == 0)
            showPanel(animate = false)
        })

        // ---- 文字颜色 ----
        col.addView(sectionLabel("文字颜色"))
        val tRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val tColors = listOf(
            Color.WHITE, Color.BLACK, Color.argb(255, 255, 213, 79),
            Color.argb(255, 100, 181, 246), Color.argb(255, 129, 199, 132), Color.argb(255, 255, 138, 128)
        )
        val curT = FloatingLyricsPrefs.textColor(this)
        tColors.forEach { c ->
            tRow.addView(colorDot(c, curT == c) {
                // 文字色板必须真正生效：选色后自动关闭「当前行跟随动态色」，
                // 否则 restyleInPlace 的 accent 会盖掉用户选的颜色。
                FloatingLyricsPrefs.setTextColor(this@FloatingLyricsService, c)
                FloatingLyricsPrefs.setUseAccentForCurrent(this@FloatingLyricsService, false)
                showPanel(animate = false)
            })
        }
        col.addView(tRow)

        // ---- 背景颜色 ----
        col.addView(sectionLabel("背景颜色"))
        val bRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val bColors = listOf(Color.BLACK, Color.WHITE)
        val curB = FloatingLyricsPrefs.bgColor(this)
        bColors.forEach { c ->
            bRow.addView(colorDot(c, curB == c) {
                FloatingLyricsPrefs.setBgColor(this@FloatingLyricsService, c)
                showPanel(animate = false)
            })
        }
        col.addView(bRow)

        // ---- 字号 ----
        col.addView(sectionLabel("字号"))
        val fsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        fsRow.addView(stepButton(R.drawable.ic_fl_remove) {
            FloatingLyricsPrefs.setFontSize(
                this@FloatingLyricsService,
                FloatingLyricsPrefs.fontSize(this@FloatingLyricsService) - 2
            )
            showPanel(animate = false)
        })
        fsRow.addView(TextView(this).apply {
            text = FloatingLyricsPrefs.fontSize(this@FloatingLyricsService).toString() + " sp"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        fsRow.addView(stepButton(R.drawable.ic_fl_add) {
            FloatingLyricsPrefs.setFontSize(
                this@FloatingLyricsService,
                FloatingLyricsPrefs.fontSize(this@FloatingLyricsService) + 2
            )
            showPanel(animate = false)
        })
        col.addView(fsRow)

        col.addView(TextView(this).apply {
            text = "拖动歌词或面板标题可移动位置；锁定后完全穿透、点击无效，可在通知中心或设置页解锁。"
            textSize = 11f
            setTextColor(Color.argb(150, 255, 255, 255))
            setLineSpacing(dp(3).toFloat(), 1.15f)
            setPadding(dp(2), dp(16), 0, 0)
        })

        scroll.addView(col)
        return scroll
    }

    /* ================= 窗口管理 ================= */

    private fun ensureOverlay() {
        if (!FloatingLyricsState.canOverlay(this)) return
        if (root != null) { restyleInPlace(); return }

        val r = FrameLayout(this).apply {
            isClickable = true
            isFocusable = true
        }
        root = r
        bgDrawable = buildCardDrawable()
        r.background = bgDrawable

        // 歌词与面板都常驻窗口内：切换只改 visibility/alpha，窗口根 view 不重建
        lyricView = buildLyricView()
        r.addView(
            lyricView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        panelHost = FrameLayout(this).apply {
            visibility = View.GONE
            alpha = 0f
        }
        r.addView(
            panelHost,
            FrameLayout.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        val sx = FloatingLyricsPrefs.x(this)
        val sy = FloatingLyricsPrefs.y(this)
        params.x = if (sx >= 0) sx else 40
        params.y = if (sy >= 0) sy else 300
        lp = params

        // 顺序很重要：先 addView(root)，再做窗口参数/尺寸调整
        try {
            windowManager.addView(r, params)
        } catch (_: Exception) {
            stopSelf(); return
        }
        lastFlags = params.flags
        showLyric(animate = false)
    }

    /**
     * 切换窗口内容（歌词 ⇄ 面板）。
     *
     * 关键：歌词与面板常驻窗口内，切换只做 alpha/scale 过渡，
     * 绝不 remove+add 窗口根 view —— 那是「卡一下、深一下」发暗闪烁的根因。
     */
    private fun switchContent(
        panel: Boolean,
        animate: Boolean,
        afterAttach: () -> Unit = {}
    ) {
        val inView: View = if (panel) (panelHost ?: return) else (lyricView ?: return)
        val outView: View? = if (panel) lyricView else panelHost
        if (showingPanel == panel && inView.visibility == View.VISIBLE && inView.alpha == 1f) {
            applyWindow()
            afterAttach()
            return
        }
        showingPanel = panel
        inView.animate().cancel()
        outView?.animate()?.cancel()

        // 先定尺寸（面板固定宽 / 歌词自适应），窗口参数原样更新，不重建窗口
        applyWindow()

        if (!animate) {
            outView?.animate()?.cancel()
            outView?.alpha = 1f
            outView?.scaleX = 1f
            outView?.scaleY = 1f
            outView?.visibility = View.GONE
            inView.alpha = 1f
            inView.scaleX = 1f
            inView.scaleY = 1f
            inView.visibility = View.VISIBLE
            applyWindow()
            afterAttach()
            return
        }

        inView.visibility = View.VISIBLE
        inView.alpha = 0f
        inView.scaleX = 0.97f
        inView.scaleY = 0.97f
        inView.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(190)
            .setInterpolator(DecelerateInterpolator())
            .start()

        if (outView != null && outView !== inView) {
            if (!panel) {
                // 回到歌词：面板立即让位，避免 WRAP_CONTENT 窗口被面板宽度撑住
                outView.alpha = 1f
                outView.scaleX = 1f
                outView.scaleY = 1f
                outView.visibility = View.GONE
                applyWindow()
            } else {
                outView.animate()
                    .alpha(0f).scaleX(0.97f).scaleY(0.97f)
                    .setDuration(110)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        // 迟到的回调不能把已经切回歌词的视图再藏起来
                        if (showingPanel) {
                            outView.alpha = 1f
                            outView.scaleX = 1f
                            outView.scaleY = 1f
                            outView.visibility = View.GONE
                        }
                    }
                    .start()
            }
        }
        afterAttach()
    }

    /** 切到歌词形态（纯歌词，可点弹面板）。 */
    private fun showLyric(animate: Boolean = true) {
        if (lyricView == null) lyricView = buildLyricView()
        switchContent(panel = false, animate = animate) {
            refreshLyricText()
            attachDragToLyrics()
        }
    }

    /** 切到快捷设置面板形态（面板内容原位替换，窗口不重建）。 */
    private fun showPanel(animate: Boolean = true) {
        if (FloatingLyricsPrefs.isLocked(this)) return
        val host = panelHost ?: return
        val fresh = buildPanelView()
        val old = if (host.childCount > 0) host.getChildAt(0) else null
        // 面板高度适配：先按固定宽度把内容量出来，窗口高度=min(内容高, 72%屏高)，
        // 这样面板既不会被裁掉底部控件，也不会在内容不高时留一大片空白。
        runCatching {
            fresh.measure(
                View.MeasureSpec.makeMeasureSpec(dp(PANEL_WIDTH_DP), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            panelContentHeight = fresh.measuredHeight
        }
        // 先加新内容再移除旧内容：同帧完成，不会出现空窗
        host.addView(
            fresh,
            FrameLayout.LayoutParams(dp(PANEL_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        if (old != null) host.removeView(old)
        if (showingPanel) {
            // 已展开：只原位刷新一次轻量淡入，不打断阅读、不动窗口
            fresh.alpha = 0.72f
            fresh.animate().alpha(1f).setDuration(130)
                .setInterpolator(DecelerateInterpolator()).start()
            applyWindow()
            return
        }
        switchContent(panel = true, animate = animate)
    }

    private fun refreshLyricText() {
        val prev = FloatingLyricsState.prevLine
        val cur = FloatingLyricsState.currentLine
        val nxt = FloatingLyricsState.nextLine
        prevText?.visibility = if (prev.isNotBlank() && FloatingLyricsPrefs.showPrevNext(this)) View.VISIBLE else View.GONE
        nextText?.visibility = if (nxt.isNotBlank() && FloatingLyricsPrefs.showPrevNext(this)) View.VISIBLE else View.GONE
        val show = FloatingLyricsPrefs.showTranslation(this) &&
            FloatingLyricsState.currentTranslation.isNotBlank()
        transText?.visibility = if (show) View.VISIBLE else View.GONE
        applyLinesSmooth(listOfNotNull(
            prevText to prev,
            mainText to cur.ifBlank { "美好不期而遇" },
            (if (show) transText else null) to FloatingLyricsState.currentTranslation,
            nextText to nxt
        ))
        restyleInPlace()
        // 文案/字号变化后强制窗口重新贴合内容（尺寸一律走 updateViewLayout）
        applyWindow()
    }

    private fun computeFlags(): Int {
        var f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (FloatingLyricsPrefs.isLocked(this)) f = f or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return f
    }

    /** 目标窗口宽度：面板固定宽，歌词按内容自适应。 */
    private fun targetWidth(): Int =
        if (showingPanel) dp(PANEL_WIDTH_DP) else {
            val pct = FloatingLyricsPrefs.cardWidthPercent(this)
            if (pct >= 100) WindowManager.LayoutParams.WRAP_CONTENT
            else (resources.displayMetrics.widthPixels * pct / 100)
        }

    /** 面板窗口高度：按真实内容高度，最多占屏幕 72%；歌词形态则自适应内容。 */
    private fun targetHeight(): Int {
        if (!showingPanel) return WindowManager.LayoutParams.WRAP_CONTENT
        val maxPanelHeight = (resources.displayMetrics.heightPixels * 0.72f).toInt()
        return if (panelContentHeight > 0) panelContentHeight.coerceAtMost(maxPanelHeight) else maxPanelHeight
    }

    /**
     * 面板贴边时把窗口位置收进屏幕内。
     * 必须用「真实窗口高度」夹取：旧代码用固定估值 PANEL_EST_HEIGHT_DP(560dp≈1470px)，
     * 而窗口实际高度是 72% 屏高(≈1745px)，两者差 275px → 面板底部会跑到屏幕外、
     * 底部控件（字号 ±、说明文字）点不到。
     */
    private fun clampForPanel(params: WindowManager.LayoutParams) {
        if (!showingPanel) return
        val w = dp(PANEL_WIDTH_DP)
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        params.x = params.x.coerceIn(0, (screenW - w).coerceAtLeast(0))
        val h = when {
            params.height > 0 -> params.height
            panelContentHeight > 0 -> panelContentHeight
            else -> dp(PANEL_EST_HEIGHT_DP)
        }.coerceAtMost(screenH)
        params.y = params.y.coerceIn(0, (screenH - h).coerceAtLeast(0))
    }

    /**
     * 更新窗口参数：
     * - 只有 flag（锁定/穿透）变化才 removeViewImmediate + addView（view 必须是窗口根 view）；
     * - 尺寸/内容变化只用 updateViewLayout(root, params)，不重建窗口，避免黑帧与整体发暗。
     */
    private fun applyWindow() {
        val r = root ?: return
        val params = lp ?: return
        val newFlags = computeFlags()
        val w = targetWidth()
        if (newFlags != lastFlags) {
            params.flags = newFlags
            params.width = w
            params.height = targetHeight()
            clampForPanel(params)
            val attached = r.isAttachedToWindow
            if (attached) {
                runCatching { windowManager.removeViewImmediate(r) }
            }
            val ok = runCatching { windowManager.addView(r, params) }.isSuccess
            if (!ok && attached) {
                runCatching { windowManager.addView(r, params) }
            }
            lastFlags = newFlags
            updateNotification()
            return
        }
        if (params.width != w) params.width = w
        // 面板高度：min(内容真实高度, 72%屏高)，歌词形态自适应内容
        val h = targetHeight()
        if (params.height != h) params.height = h
        clampForPanel(params)
        runCatching { windowManager.updateViewLayout(r, params) }
        r.requestLayout()
        updateNotification()
    }

    /** 兼容旧调用：等价于 applyWindow()（尺寸用 updateViewLayout，不再重建窗口）。 */
    private fun relayout() = applyWindow()

    /**
     * 淡出行（上一句 / 下一句 / 翻译行）在给定底色上的可读 alpha：
     * 从 baseAlpha 起逐步加深，直到与底色的对比度达标，避免「淡出 = 看不见」。
     */
    private fun dimOver(bg: Int, fg: Int, baseAlpha: Int, minRatio: Double = 3.0): Int {
        var a = baseAlpha.coerceIn(30, 255)
        while (a < 240) {
            val comp = blend(bg, fg, a / 255f)
            if (LyricContrast.ratio(comp, bg) >= minRatio) break
            a += 12
        }
        return withAlpha(fg, a)
    }

    /** 歌词文字样式：上一行/下一行淡出、当前行加粗高亮（动态色）、翻译行弱化。 */
    private fun restyleInPlace() {
        val userColor = FloatingLyricsPrefs.textColor(this)
        val accent = FloatingLyricsPrefs.accentColor(this)
        val fs = FloatingLyricsPrefs.fontSize(this).toFloat()
        val opaque = FloatingLyricsPrefs.isOpaque(this)
        val curColor = if (FloatingLyricsPrefs.useAccentForCurrent(this)) accent else userColor
        val dimAlpha = if (opaque) 120 else 152
        // 与 applyCardBackground 保持同一套「自动底色」，淡出行的 alpha 依据它计算
        adaptedBg = LyricContrast.readableBg(curColor, FloatingLyricsPrefs.bgColor(this), 4.5)
        val dimColor = if (opaque) dimOver(adaptedBg, userColor, dimAlpha) else withAlpha(userColor, dimAlpha)
        val transColor = if (opaque) dimOver(adaptedBg, userColor, 172, 2.5) else withAlpha(userColor, 172)

        // ── 新增可调项在此生效 ──
        val bold = FloatingLyricsPrefs.bold(this)
        val showPN = FloatingLyricsPrefs.showPrevNext(this)
        val lineGap = dp(FloatingLyricsPrefs.lineSpacing(this)).toFloat()
        val align = FloatingLyricsPrefs.align(this)

        // 对齐：0 左 / 1 居中 / 2 右（同时作用于文字与容器）
        val lineGravity = when (align) {
            0 -> Gravity.START
            2 -> Gravity.END
            else -> Gravity.CENTER
        }
        lyricView?.gravity = when (align) {
            0 -> Gravity.START
            2 -> Gravity.END
            else -> Gravity.CENTER_HORIZONTAL
        }
        val padH = dp(FloatingLyricsPrefs.paddingHDp(this))
        lyricView?.setPadding(padH, dp(16), padH, dp(16))

        prevText?.apply {
            setTextColor(dimColor)
            textSize = fs * 0.78f
            setTypeface(Typeface.create(typeface, Typeface.NORMAL))
            gravity = lineGravity
            setLineSpacing(lineGap, 1f)
        }
        nextText?.apply {
            setTextColor(dimColor)
            textSize = fs * 0.78f
            setTypeface(Typeface.create(typeface, Typeface.NORMAL))
            gravity = lineGravity
            setLineSpacing(lineGap, 1f)
        }
        mainText?.apply {
            setTextColor(curColor)
            textSize = fs
            setTypeface(Typeface.create(typeface, if (bold) Typeface.BOLD else Typeface.NORMAL))
            gravity = lineGravity
            setLineSpacing(lineGap, 1f)
        }
        val show = FloatingLyricsPrefs.showTranslation(this) &&
            FloatingLyricsState.currentTranslation.isNotBlank()
        transText?.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            transText?.apply {
                setTextColor(transColor)
                textSize = fs * 0.68f
                setTypeface(Typeface.create(typeface, Typeface.NORMAL))
                gravity = lineGravity
            }
        }
        // 上一句/下一句总开关
        if (!showPN) {
            prevText?.visibility = View.GONE
            nextText?.visibility = View.GONE
        }
        applyTextShadow(opaque, curColor, userColor, fs)
        applyCardBackground()
    }

    /**
     * 文字阴影（真正的可读性兜底）：
     * 浅色文字配深阴影、深色文字配浅阴影，透明/半透明底色下同样清晰。
     */
    private fun applyTextShadow(opaque: Boolean, mainColor: Int, userColor: Int, fontSize: Float) {
        // 不透明底色已经由 readableBg 保证对比度，阴影轻一点；透明底色完全靠阴影兜底，重一点。
        val radius = (fontSize * (if (opaque) 0.22f else 0.34f)).coerceIn(1.5f, 9f)
        val dy = (fontSize * 0.055f).coerceIn(0.5f, 2.5f)
        mainText?.setShadowLayer(radius, 0f, dy, LyricContrast.shadowFor(mainColor))
        val other = LyricContrast.shadowFor(userColor)
        prevText?.setShadowLayer(radius, 0f, dy, other)
        nextText?.setShadowLayer(radius, 0f, dy, other)
        transText?.setShadowLayer(radius, 0f, dy, other)
    }

    private fun attachDragToLyrics() {
        val lv = lyricView ?: return
        val params = lp ?: return
        if (FloatingLyricsPrefs.isLocked(this)) {
            lv.setOnTouchListener(null)
        } else {
            lv.isClickable = true
            attachDrag(lv, params) { if (!showingPanel) showPanel() }
        }
    }

    /**
     * 拖动移动窗口；未拖动（点击）时触发 onTap。
     *
     * 触摸分派顺序提醒：ViewGroup 会把 ACTION_DOWN 先给到子 View，
     * 只有子 View 不消费（例如纯装饰图标、标题文字）时才轮到这里的 OnTouchListener，
     * 因此本监听不会抢走 circleIconButton 之类的子按钮点击。
     * 为了点击更可靠，只有位移超过系统 touchSlop 才真正开始移动窗口：
     * 手指轻微抖动不会被误判成拖动，松手后仍能算作一次点击。
     */
    private fun attachDrag(
        v: View,
        params: WindowManager.LayoutParams,
        onTap: (() -> Unit)? = null
    ) {
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        v.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0
            private var downY = 0
            private var rawX = 0f
            private var rawY = 0f
            private var moved = false

            override fun onTouch(view: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = params.x; downY = params.y
                        rawX = e.rawX; rawY = e.rawY
                        moved = false
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - rawX).toInt()
                        val dy = (e.rawY - rawY).toInt()
                        if (!moved && (abs(dx) > slop || abs(dy) > slop)) moved = true
                        if (!moved) return true
                        params.x = downX + dx
                        params.y = downY + dy
                        // updateViewLayout 必须传窗口根 View
                        root?.let { r -> runCatching { windowManager.updateViewLayout(r, params) } }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (moved) {
                            clampIntoScreen(params)
                            root?.let { r -> runCatching { windowManager.updateViewLayout(r, params) } }
                            FloatingLyricsPrefs.setX(this@FloatingLyricsService, params.x)
                            FloatingLyricsPrefs.setY(this@FloatingLyricsService, params.y)
                        } else {
                            onTap?.invoke()
                        }
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> return true
                }
                return false
            }
        })
    }

    /** 松手后把窗口拉回屏幕内，避免拖出可视区域。 */
    private fun clampIntoScreen(params: WindowManager.LayoutParams) {
        val r = root ?: return
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val w = if (r.width > 0) r.width else dp(240)
        val h = if (r.height > 0) r.height else dp(120)
        val maxX = (screenW - w).coerceAtLeast(0)
        val maxY = (screenH - h).coerceAtLeast(0)
        params.x = params.x.coerceIn(0, maxX)
        params.y = params.y.coerceIn(0, maxY)
    }

    /** 轮询歌词/设置变化。 */
    private fun watch() {
        if (watchJob != null) return
        watchJob = scope.launch {
            var lastLine = "\u0000"
            var lastTrans = "\u0000"
            var lastSig = ""
            var lastLockSig = FloatingLyricsPrefs.isLocked(this@FloatingLyricsService)
            while (true) {
                playControlBtn?.setImageResource(
                    if (FloatingLyricsState.isPlaying) R.drawable.ic_fl_pause else R.drawable.ic_fl_play
                )
                val line = FloatingLyricsState.currentLine
                val tr = FloatingLyricsState.currentTranslation
                if (line != lastLine || tr != lastTrans) {
                    lastLine = line
                    lastTrans = tr
                    refreshLyricText()
                }
                val lockSig = FloatingLyricsPrefs.isLocked(this@FloatingLyricsService)
                if (lockSig != lastLockSig) {
                    lastLockSig = lockSig
                    showLyric()
                }
                // 外观签名：任何一项变化都在 250ms 内自愈（applyNow 失败也不会卡住外观）
                val sig = "" + FloatingLyricsPrefs.textColor(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.bgColor(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.bgAlpha(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.bgAlphaPercent(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.fontSize(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.showTranslation(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.useAccentForCurrent(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.accentColor(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.bold(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.showPrevNext(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.lineSpacing(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.align(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.cornerRadiusDp(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.paddingHDp(this@FloatingLyricsService)
                if (sig != lastSig) {
                    lastSig = sig
                    restyleInPlace()
                    refreshLyricText()
                }
                delay(250)
            }
        }
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        scope.cancel()
        root?.let { runCatching { windowManager.removeViewImmediate(it) } }
        root = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.HyperX.MI.Na.FLOATING_LYRICS_START"
        const val ACTION_STOP = "com.HyperX.MI.Na.FLOATING_LYRICS_STOP"
        const val ACTION_TOGGLE_LOCK = "com.HyperX.MI.Na.FLOATING_LYRICS_TOGGLE_LOCK"
        const val ACTION_APPLY = "com.HyperX.MI.Na.FLOATING_LYRICS_APPLY"

        /** 已存活的 Service 实例：applyNow 直接驱动它刷新，不依赖 startService 能否送达。 */
        @Volatile
        private var instance: FloatingLyricsService? = null
        private const val NOTIF_ID = 8801
        private const val PANEL_WIDTH_DP = 306
        private const val PANEL_EST_HEIGHT_DP = 560

        fun start(ctx: Context) {
            if (!FloatingLyricsState.canOverlay(ctx)) return
            val i = Intent(ctx, FloatingLyricsService::class.java).setAction(ACTION_START)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
                else ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            runCatching { ctx.stopService(Intent(ctx, FloatingLyricsService::class.java)) }
        }

    /**
     * 设置面板改动后调用：立刻按最新配置重排外观（不必等歌词刷新）。
     *
     * 首选「直接驱动已存活的 Service 实例」：不依赖 startForegroundService 能否送达
     * （Android 12+ 后台限制 / 服务已销毁都会让 startService 静默失败，这正是
     * 「改颜色像没反应」的原因之一）。实例不存在时，只有在用户已开启悬浮歌词的
     * 前提下才走 startService 兜底，避免「关着开关也被拉起一个悬浮窗」。
     */
    fun applyNow(ctx: Context) {
        val live = instance
        if (live != null) {
            live.root?.post {
                runCatching {
                    live.restyleInPlace()
                    live.applyWindow()
                }
            }
            return
        }
        if (!FloatingLyricsPrefs.isEnabled(ctx)) return
        runCatching {
            val i = Intent(ctx, FloatingLyricsService::class.java).setAction(ACTION_APPLY)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }
    }
    }
}
