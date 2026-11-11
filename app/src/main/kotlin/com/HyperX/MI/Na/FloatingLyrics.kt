package com.HyperX.MI.Na

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
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
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
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

    /** 行间距（额外 dp，0..12）。默认 4dp 与旧版写死的 setPadding(0, 4dp, 0, 4dp) 一致 */
    fun lineSpacing(ctx: Context) = sp(ctx).getInt("line_spacing", 4)
    fun setLineSpacing(ctx: Context, v: Int) { sp(ctx).edit().putInt("line_spacing", v.coerceIn(0, 12)).apply() }

    /** 对齐：0 左 / 1 居中 / 2 右 */
    fun align(ctx: Context) = sp(ctx).getInt("align", 1)
    fun setAlign(ctx: Context, v: Int) { sp(ctx).edit().putInt("align", v.coerceIn(0, 2)).apply() }

    /** 背景不透明度 0..100（在不透明模式下按百分比缩放） */
    fun bgAlphaPercent(ctx: Context) = sp(ctx).getInt("bg_alpha_pct", 100)
    fun setBgAlphaPercent(ctx: Context, v: Int) { sp(ctx).edit().putInt("bg_alpha_pct", v.coerceIn(0, 100)).apply() }

    /** 卡片圆角 dp 0..32 */
    fun windowWidthDp(ctx: Context) = sp(ctx).getInt("win_w_dp", 280)
    fun setWindowWidthDp(ctx: Context, v: Int) { sp(ctx).edit().putInt("win_w_dp", v.coerceIn(280, 400)).apply() }
    fun windowHeightDp(ctx: Context) = sp(ctx).getInt("win_h_dp", 150)
    fun setWindowHeightDp(ctx: Context, v: Int) { sp(ctx).edit().putInt("win_h_dp", v.coerceIn(120, 200)).apply() }
    fun lyricColorIndex(ctx: Context) = sp(ctx).getInt("color_index", 1)
    fun setLyricColorIndex(ctx: Context, v: Int) { sp(ctx).edit().putInt("color_index", v).apply() }
    fun lyricBgIndex(ctx: Context) = sp(ctx).getInt("bg_index", 0)
    fun setLyricBgIndex(ctx: Context, v: Int) { sp(ctx).edit().putInt("bg_index", v).apply() }
    fun showBorder(ctx: Context) = sp(ctx).getBoolean("show_border", false)
    fun setShowBorder(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("show_border", v).apply() }
    fun cornerRadiusDp(ctx: Context) = sp(ctx).getInt("corner_dp", 19)
    fun setCornerRadiusDp(ctx: Context, v: Int) { sp(ctx).edit().putInt("corner_dp", v.coerceIn(0, 32)).apply() }

    /** 卡片宽度占屏幕百分比 60..100 */
    fun cardWidthPercent(ctx: Context) = sp(ctx).getInt("card_width_pct", 100)
    fun setCardWidthPercent(ctx: Context, v: Int) { sp(ctx).edit().putInt("card_width_pct", v.coerceIn(60, 100)).apply() }

    /** 阴影强度 0..100（100 = 预设阴影原强度，与旧版硬编码一致；按百分比缩放阴影色 alpha） */
    fun shadowAlphaPercent(ctx: Context) = sp(ctx).getInt("shadow_pct", 100)
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
    private var lyricTextViews: List<TextView> = emptyList()
    private var prevText: TextView? = null
    private var mainText: TextView? = null
    private var nextText: TextView? = null
    private var transText: TextView? = null
    private var ctrlRow: LinearLayout? = null
    private var playControlBtn: ImageView? = null

    /** 面板容器：常驻窗口内，内容原位替换；窗口根 view 绝不因内容切换而重建。 */
    private var panelHost: FrameLayout? = null

    /** 驱动「四行歌词同步切换」的唯一 animator（避免四行各自动画导致错位）。 */
    private var lineAnimator: android.animation.Animator? = null

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
    private fun applyLinesSmooth(items: List<Pair<TextView?, CharSequence>>) {
        // 照搬用户早期 Lua 项目的三行编排（main.lua updateLyricsWithAnimation）：
        //   第 1 行（上一句）：静止，alpha 0.7
        //   第 2 行（当前句）：自下方 30dp 滑入 + 缩放 0.8→1，400ms Overshoot(1.2) 回弹
        //   第 3 行（下一句）：自下方 15dp 轻滑入，350ms 减速，落定 alpha 0.7
        // 先统一换文案，再按行做各自的入场动画。
        // 变更检测（Lua 版原有、移植时被删掉）：refreshLyricText 在轮询/样式自愈里
        // 也会被调用（行未变化时同样走到这里），没有这个判断就会反复重播滑入动画
        // → 歌词持续抖动。文案没变就什么都不做。
        val changed = items.any { (tv, text) -> tv != null && tv.text?.toString() != text.toString() }
        if (!changed) return
        items.forEach { (tv, text) -> tv?.text = text }
        val main = mainText ?: return
        val prev = prevText
        val next = nextText

        lineAnimator?.cancel()
        val anims = ArrayList<android.animation.Animator>()

        // 上一行：静止淡色
        prev?.alpha = 0.7f
        prev?.translationY = 0f
        prev?.scaleX = 1f
        prev?.scaleY = 1f

        // 当前行：滑入 + 回弹 + 缩放
        val dyIn = dp(30).toFloat()
        main.alpha = 0f
        main.translationY = dyIn
        main.scaleX = 0.8f
        main.scaleY = 0.8f
        val mainSet = android.animation.AnimatorSet()
        mainSet.playTogether(listOf(
            ObjectAnimator.ofFloat(main, View.ALPHA, 0f, 1f),
            ObjectAnimator.ofFloat(main, View.TRANSLATION_Y, dyIn, 0f),
            ObjectAnimator.ofFloat(main, View.SCALE_X, 0.8f, 1f),
            ObjectAnimator.ofFloat(main, View.SCALE_Y, 0.8f, 1f)
        ))
        mainSet.duration = 400
        mainSet.interpolator = OvershootInterpolator(1.2f)
        anims.add(mainSet)

        // 下一行：轻滑入
        if (next != null && next.visibility == View.VISIBLE) {
            val dyN = dp(15).toFloat()
            next.alpha = 0f
            next.translationY = dyN
            val nextSet = android.animation.AnimatorSet()
            nextSet.playTogether(listOf(
                ObjectAnimator.ofFloat(next, View.ALPHA, 0f, 0.7f),
                ObjectAnimator.ofFloat(next, View.TRANSLATION_Y, dyN, 0f)
            ))
            nextSet.duration = 350
            nextSet.interpolator = DecelerateInterpolator()
            anims.add(nextSet)
        }

        lineAnimator = android.animation.AnimatorSet().apply {
            playTogether(anims)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (lineAnimator === animation) lineAnimator = null
                    next?.alpha = 0.7f
                    next?.translationY = 0f
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

    /** 照搬 main.lua 的歌词颜色预设（主行色 / 其他行半透明同色 / 阴影色） */
    private data class LyricColorSet(val name: String, val currentColor: Int, val otherColor: Int, val shadowColor: Int)
    private val lyricColorSets = listOf(
        LyricColorSet("粉色", 0xFFFF69B4.toInt(), 0x99FF69B4.toInt(), 0xFF8B008B.toInt()),
        LyricColorSet("蓝色", 0xFF1E90FF.toInt(), 0x991E90FF.toInt(), 0xFF00008B.toInt())
    )

    /** 照搬 main.lua 的背景颜色预设（含各自的边框色） */
    private data class LyricBgSet(val name: String, val color: Int, val borderColor: Int)
    private val lyricBgSets = listOf(
        LyricBgSet("深色", 0xCC1A1A1A.toInt(), 0x55FFFFFF.toInt()),
        LyricBgSet("白色", 0xCCFFFFFF.toInt(), 0x55000000.toInt()),
        LyricBgSet("雫蓝", 0xDDF5F9FF.toInt(), 0xDDB3D9FF.toInt()),
        LyricBgSet("芙兰", 0xDDF0F4F9.toInt(), 0xDDD1D5F2.toInt()),
        LyricBgSet("绒灰蓝", 0xDDF5F9FC.toInt(), 0xDD9DB5D6.toInt()),
        LyricBgSet("薰衣草紫", 0xDDFAF7FC.toInt(), 0xDDC3A6D2.toInt()),
        LyricBgSet("透明", 0x33000000.toInt(), 0x33FFFFFF.toInt())
    )

    private fun currentLyricColorSet(): LyricColorSet =
        lyricColorSets[FloatingLyricsPrefs.lyricColorIndex(this).coerceIn(0, lyricColorSets.size - 1)]

    private fun currentLyricBgSet(): LyricBgSet =
        lyricBgSets[FloatingLyricsPrefs.lyricBgIndex(this).coerceIn(0, lyricBgSets.size - 1)]

        private fun buildCardDrawable(): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()  // 标准矩形，小圆角
            setColor(Color.WHITE)  // 默认不透明白色
        }

    /** 当前行的实际文字色（跟随动态色时为强调色，否则为用户选色）。 */
    private fun effectiveMainColor(): Int =
        // 尊重用户在面板里选的文字颜色：不再被强调色覆盖（那就是「设置失效」的来源）。
        FloatingLyricsPrefs.textColor(this)

    private fun applyCardBackground() {
        val d = bgDrawable ?: return
        // 照搬 main.lua：背景 = 预设色（自带透明度）× 面板透明度百分比，12dp 圆角，1dp 边框可开关。
        // 自定义背景（lyricBgIndex == -1）不走预设：用用户选的 bgColor 的 RGB，
        // 透明度按现有逻辑（不透明白底）；之前索引 -1 被 clamp 成 0 → 永远显示预设色。
        val bi = FloatingLyricsPrefs.lyricBgIndex(this)
        val customBg = bi == -1
        val set = currentLyricBgSet()     // 预设分支的色源；自定义时仅沿用其边框色
        val alphaPct = FloatingLyricsPrefs.bgAlphaPercent(this)
        val base = if (customBg) FloatingLyricsPrefs.bgColor(this) else set.color
        // 背景 alpha = 「不透明」开关 × 不透明度百分比：
        //   开关开 → 255 × pct%（默认 100% → 255，与旧版视觉一致）
        //   开关关 → 0（完全透明，只留带阴影的文字 —— 设置页「背景不透明」开关说明文案即此语义）
        // 旧版这里硬编码 255，把「背景不透明」开关彻底锁死（真机反馈的根因）。
        val a = if (FloatingLyricsPrefs.isOpaque(this)) 255 * alphaPct.coerceIn(0, 100) / 100 else 0
        val rgb = base and 0xFFFFFF
        val corner = dp(FloatingLyricsPrefs.cornerRadiusDp(this)).toFloat()
        if (d.cornerRadius != corner) d.cornerRadius = corner
        if (root?.elevation != 0f) root?.elevation = 0f
        if (root?.translationZ != 0f) root?.translationZ = 0f
        adaptedBg = withAlpha(rgb or (a shl 24), a)

        when {
            showingPanel -> {
                // 面板自身永远是固定深色 MD3 surface：绝不跟随歌词卡片底色。
                // 否则用户把歌词底色设成白色后，面板里的白色文字会被底色吃掉、完全看不见
                // （用户实测反馈的问题）。
                d.setColor(0xF21B1B1F.toInt())
                d.setStroke(dp(1), Color.argb(38, 255, 255, 255))
            }
            a > 0 -> {
                d.setColor(rgb or (a shl 24))
                if (FloatingLyricsPrefs.showBorder(this)) d.setStroke(dp(1), set.borderColor)
                else d.setStroke(0, Color.TRANSPARENT)
            }
            else -> {
                d.setColor(Color.TRANSPARENT)
                d.setStroke(0, Color.TRANSPARENT)
            }
        }
        root?.background = d
    }
        /* ================= 视图构建 ================= */

    /** 窗口宽度：照搬 main.lua（默认 280dp，可调 280-400），再乘「卡片宽度」百分比（默认 100% 不变）。 */
    private fun cardFixedWidthPx(): Int =
        dp(FloatingLyricsPrefs.windowWidthDp(this)) *
            FloatingLyricsPrefs.cardWidthPercent(this).coerceIn(60, 100) / 100

    /** 单行高度（随字号） */
    private fun lyricRowHeightPx(): Int {
        val fs = FloatingLyricsPrefs.fontSize(this).toFloat()
        return (fs * resources.displayMetrics.scaledDensity * 1.55f).toInt().coerceAtLeast(dp(22))
    }

    /** 窗口高度：照搬 main.lua（默认 150dp，可调 120-200） */
    private fun cardFixedHeightPx(): Int = dp(FloatingLyricsPrefs.windowHeightDp(this))

    private fun buildLyricView(): LinearLayout {
        val cs = currentLyricColorSet()
        // 初始值与 restyleInPlace 读同一批偏好，构建后立即被 restyleInPlace 覆盖为最新值
        val padH = dp(FloatingLyricsPrefs.paddingHDp(this))
        val gap = dp(FloatingLyricsPrefs.lineSpacing(this).coerceIn(0, 12))
        val l = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(padH, dp(12), padH, dp(12))
            isClickable = true
            isFocusable = true
        }

        // 照搬 main.lua：三个单行 TextView，上下行间距按「行间距」偏好（0..12dp），minHeight 24dp；
        // 第 2 行字号 +2、粗体、当前色 + 4dp 阴影；第 1/3 行普通、半透明同色 + 3dp 阴影。
        lyricTextViews = (1..3).map { idx ->
            TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER
                setPadding(0, gap, 0, gap)   // 行间距跟偏好走（restyleInPlace 同步更新），旧版写死 4dp
                setSingleLine(true)
                ellipsize = TextUtils.TruncateAt.END
                minHeight = dp(24)
                isClickable = false
                isFocusable = false
                if (idx == 2) {
                    textSize = FloatingLyricsPrefs.fontSize(this@FloatingLyricsService) + 2f
                    setTypeface(Typeface.DEFAULT_BOLD)
                    setTextColor(cs.currentColor)
                    setShadowLayer(dp(4).toFloat(), 0f, 0f, cs.shadowColor)
                } else {
                    textSize = FloatingLyricsPrefs.fontSize(this@FloatingLyricsService).toFloat()
                    setTextColor(cs.otherColor)
                    setShadowLayer(dp(3).toFloat(), 0f, 0f, cs.shadowColor)
                }
            }
        }
        prevText = lyricTextViews[0]
        mainText = lyricTextViews[1]
        nextText = lyricTextViews[2]
        transText = null
        lyricTextViews.forEach { l.addView(it) }
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
            setOnClickListener {
                android.util.Log.e("FL-Panel", "onClick: chip \"$label\"")
                runCatching { onClick() }.onFailure { android.util.Log.e("FL-Panel", "onClick chip $label FAILED", it) }
            }
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
            setOnClickListener {
                android.util.Log.e("FL-Panel", "onClick: step " + if (iconRes == R.drawable.ic_fl_add) "+" else "-")
                runCatching { onClick() }.onFailure { android.util.Log.e("FL-Panel", "onClick step FAILED", it) }
            }
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
            setOnClickListener {
                // 排查「点了没反应」：确认进入 onClick；异常打日志而不是无声无息
                android.util.Log.e("FL-Panel", "onClick: colorDot selected=$selected")
                runCatching { onClick() }.onFailure { android.util.Log.e("FL-Panel", "onClick colorDot FAILED", it) }
            }
        }
    }

    /** 两态分段控件：不透明 | 透明。可传 icons 为每段配 16dp 矢量图标（图标 + 文字居中排列）。 */
    private fun segmentedTwoState(
        labels: List<String>,
        selectedIndex: Int,
        icons: List<Int>? = null,
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
                // 16dp 语义图标：颜色跟随选中/未选中态（与文字同色）
                if (icons != null && i < icons.size) {
                    addView(ImageView(this@FloatingLyricsService).apply {
                        setImageResource(icons[i])
                        setColorFilter(if (selected) readableOn(accent) else Color.WHITE)
                        layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)).apply { marginEnd = dp(5) }
                    })
                }
                addView(TextView(this@FloatingLyricsService).apply {
                    text = label
                    textSize = 13f
                    setTextColor(if (selected) readableOn(accent) else Color.WHITE)
                    setTypeface(typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
                    gravity = Gravity.CENTER
                })
                isClickable = true
                setOnClickListener {
                    android.util.Log.e("FL-Panel", "onClick: segment[$i] $label")
                    runCatching { onSelect(i) }.onFailure { android.util.Log.e("FL-Panel", "onClick segment[$i] FAILED", it) }
                }
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
            refreshLyricText()   // 立即应用到歌词行（翻译并入当前句），不等下次换行/收回面板
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
            if (FloatingLyricsPrefs.isOpaque(this)) 0 else 1,
            listOf(R.drawable.ic_fl_palette, R.drawable.ic_fl_remove)   // 语义图标：实色填充 / 无背景
        ) { i ->
            FloatingLyricsPrefs.setOpaque(this@FloatingLyricsService, i == 0)
            applyCardBackground()   // 立即生效，不等 250ms 自愈轮询/收回面板
            showPanel(animate = false)
        })

        // ---- 文字颜色 ----
        col.addView(sectionLabel("文字颜色"))
        val curC = FloatingLyricsPrefs.lyricColorIndex(this)
        val cRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        lyricColorSets.forEachIndexed { ci, cs ->
            cRow.addView(colorDot(cs.currentColor, curC == ci) {
                FloatingLyricsPrefs.setLyricColorIndex(this, ci)
                restyleInPlace()
                showPanel(animate = false)   // 同步面板选中态
            })
        }
        col.addView(cRow)

        // ---- 自定义文字颜色（用户自己调色）----
        col.addView(sectionLabel("自定义文字颜色"))
        val curT = FloatingLyricsPrefs.textColor(this)
        // 选中态与 restyleInPlace 同一判断：仅自定义模式（索引 -1）且颜色相等才高亮，
        // 避免预设与自定义点同时选中。
        val customTextActive = FloatingLyricsPrefs.lyricColorIndex(this) == -1
        val tScroll = android.widget.HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val tRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        listOf(
            0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFFFD54F.toInt(), 0xFF64B5F6.toInt(),
            0xFF81C784.toInt(), 0xFFFF8A80.toInt(), 0xFFFF69B4.toInt(), 0xFFB39DDB.toInt()
        ).forEach { argb ->
            tRow2.addView(colorDot(argb, customTextActive && curT == argb) {
                FloatingLyricsPrefs.setTextColor(this, argb)
                FloatingLyricsPrefs.setUseAccentForCurrent(this, false)
                FloatingLyricsPrefs.setLyricColorIndex(this, -1)   // 退出预设，改走自定义
                restyleInPlace()
                showPanel(animate = false)
            })
        }
        tScroll.addView(tRow2)
        col.addView(tScroll)

        col.addView(sectionLabel("背景颜色"))
        val curB = FloatingLyricsPrefs.lyricBgIndex(this)
        val bRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        lyricBgSets.forEachIndexed { bi, bs ->
            bRow.addView(colorDot(bs.color, curB == bi) {
                FloatingLyricsPrefs.setLyricBgIndex(this, bi)
                applyCardBackground()
                showPanel(animate = false)   // 同步面板选中态
            })
        }
        col.addView(bRow)

        // ---- 自定义背景颜色（用户自己调色）----
        col.addView(sectionLabel("自定义背景颜色"))
        val curB2 = FloatingLyricsPrefs.bgColor(this)
        // 选中态与 applyCardBackground 同一判断：仅自定义模式（索引 -1）且颜色相等才高亮。
        val customBgActive = FloatingLyricsPrefs.lyricBgIndex(this) == -1
        val bScroll = android.widget.HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val bRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        listOf(
            0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF424242.toInt(), 0xFF1A237E.toInt(),
            0xFF4A148C.toInt(), 0xFFBF360C.toInt(), 0xFFF5F9FF.toInt(), 0xFF1B1B1F.toInt()
        ).forEach { argb ->
            bRow2.addView(colorDot(argb, customBgActive && curB2 == argb) {
                FloatingLyricsPrefs.setBgColor(this, argb)
                FloatingLyricsPrefs.setLyricBgIndex(this, -1)      // 退出预设，改走自定义
                applyCardBackground()
                showPanel(animate = false)
            })
        }
        bScroll.addView(bRow2)
        col.addView(bScroll)

        // 窗口宽度 / 高度（照搬 Lua：280-400 / 120-200）
        col.addView(sectionLabel("窗口宽度: " + FloatingLyricsPrefs.windowWidthDp(this@FloatingLyricsService) + "dp"))
        val wRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        wRow.addView(stepButton(R.drawable.ic_fl_remove) {
            val v = FloatingLyricsPrefs.windowWidthDp(this@FloatingLyricsService) - 10
            FloatingLyricsPrefs.setWindowWidthDp(this@FloatingLyricsService, v)
            applyCardBackground(); applyWindow()
            showPanel(animate = false)   // 同步面板文案
        })
        wRow.addView(TextView(this).apply {
            text = FloatingLyricsPrefs.windowWidthDp(this@FloatingLyricsService).toString() + "dp"
            textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        wRow.addView(stepButton(R.drawable.ic_fl_add) {
            val v = FloatingLyricsPrefs.windowWidthDp(this@FloatingLyricsService) + 10
            FloatingLyricsPrefs.setWindowWidthDp(this@FloatingLyricsService, v)
            applyCardBackground(); applyWindow()
            showPanel(animate = false)   // 「+」之前漏了重建面板：值变了但标签不刷新，看起来像没反应
        })
        col.addView(wRow)

        col.addView(sectionLabel("窗口高度: " + FloatingLyricsPrefs.windowHeightDp(this@FloatingLyricsService) + "dp"))
        val hRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        hRow.addView(stepButton(R.drawable.ic_fl_remove) {
            val v = FloatingLyricsPrefs.windowHeightDp(this@FloatingLyricsService) - 10
            FloatingLyricsPrefs.setWindowHeightDp(this@FloatingLyricsService, v)
            applyWindow()
            showPanel(animate = false)   // 同步面板文案
        })
        hRow.addView(TextView(this).apply {
            text = FloatingLyricsPrefs.windowHeightDp(this@FloatingLyricsService).toString() + "dp"
            textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        hRow.addView(stepButton(R.drawable.ic_fl_add) {
            val v = FloatingLyricsPrefs.windowHeightDp(this@FloatingLyricsService) + 10
            FloatingLyricsPrefs.setWindowHeightDp(this@FloatingLyricsService, v)
            applyWindow()
            showPanel(animate = false)   // 「+」之前漏了重建面板：同宽度「+」
        })
        col.addView(hRow)

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
            restyleInPlace()   // 立即应用到歌词行，不等 250ms 轮询
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
            restyleInPlace()   // 立即应用到歌词行，不等 250ms 轮询
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
        // 关键：歌词视图必须**撑满固定尺寸的窗口**（MATCH_PARENT）。
        // 之前是 WRAP_CONTENT，内容贴着自身高度，root 的 Gravity.CENTER 就没有可居中的空间，
        // 表现就是「文字在长方体里不居中」。
        r.addView(
            lyricView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        panelHost = FrameLayout(this).apply {
            visibility = View.GONE
            alpha = 0f
            // 面板自带固定深色圆角背景：绝不跟随歌词卡片底色。
            // 否则用户把卡片底色设成白色后，面板变成白底白字，内容完全看不清。
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(0xF21B1B1F.toInt())
                setStroke(dp(1), Color.argb(38, 255, 255, 255))
            }
        }
        r.addView(
            panelHost,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
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
        // 三行结构：翻译并入当前句第二行（Spannable 缩小字号），行数恒为三行、尺寸恒定
        val show = FloatingLyricsPrefs.showTranslation(this) &&
            FloatingLyricsState.currentTranslation.isNotBlank()
        val curTxt = cur.ifBlank { "美好不期而遇" }
        val mainPayload: CharSequence = if (show) {
            val sp = android.text.SpannableString(curTxt + "\n" + FloatingLyricsState.currentTranslation)
            val st = curTxt.length + 1
            sp.setSpan(android.text.style.RelativeSizeSpan(0.62f), st, sp.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp
        } else curTxt
        applyLinesSmooth(listOfNotNull(
            prevText to prev,
            mainText to mainPayload,
            nextText to nxt
        ))
        restyleInPlace()
        applyWindow()
    }

    private fun computeFlags(): Int {
        var f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (FloatingLyricsPrefs.isLocked(this)) f = f or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return f
    }

    /** 目标窗口宽度：面板固定宽，歌词按内容自适应。 */
    private fun targetWidth(): Int =
        if (showingPanel) dp(PANEL_WIDTH_DP) else cardFixedWidthPx()

    /** 面板窗口高度：按真实内容高度，最多占屏幕 72%；歌词形态则自适应内容。 */
    private fun targetHeight(): Int {
        if (!showingPanel) return cardFixedHeightPx()
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
        // 照搬 main.lua：三行颜色完全由预设组决定（主行 +2 号粗体当前色，上下行半透明同色），
        // 阴影同样来自预设 —— 用户在面板切换「文字颜色」就是切换预设组。
        // 自定义色（lyricColorIndex == -1）不走预设：主行直接用用户选的 textColor，
        // 上下行同色 60% 透明，阴影按 textColor 对比度生成 —— 之前索引 -1 被 clamp 成 0，
        // 永远显示预设色，自定义色根本没应用（「点了不更新」的根因）。
        val ci = FloatingLyricsPrefs.lyricColorIndex(this)
        val custom = ci == -1
        val cs = currentLyricColorSet()   // 仅预设分支使用；-1 时被 clamp 的值不参与配色
        val userColor = FloatingLyricsPrefs.textColor(this)
        val mainColor = if (custom) userColor else cs.currentColor
        val otherColor = if (custom) withAlpha(userColor, 153) else cs.otherColor
        val shadowColor = if (custom) LyricContrast.shadowFor(userColor) else cs.shadowColor
        val fs = FloatingLyricsPrefs.fontSize(this).toFloat()
        // 排版偏好（设置页「排版」卡写入）：加粗 / 对齐 / 行间距 —— 旧版全部写死，设置因此不生效
        val bold = FloatingLyricsPrefs.bold(this)
        val lineGravity = when (FloatingLyricsPrefs.align(this).coerceIn(0, 2)) {
            0 -> Gravity.START
            2 -> Gravity.END
            else -> Gravity.CENTER
        }
        val gapPx = dp(FloatingLyricsPrefs.lineSpacing(this).coerceIn(0, 12))
        // 「阴影强度」0..100：按百分比缩放阴影色 alpha（100 = 预设原强度）
        val shadowPct = FloatingLyricsPrefs.shadowAlphaPercent(this).coerceIn(0, 100)
        val shadowColored = withAlpha(shadowColor, Color.alpha(shadowColor) * shadowPct / 100)
        lyricView?.gravity = Gravity.CENTER   // 垂直居中；水平对齐由各行 gravity 按偏好设置
        val padH = dp(FloatingLyricsPrefs.paddingHDp(this))
        lyricView?.setPadding(padH, dp(12), padH, dp(12))

        lyricTextViews.forEachIndexed { idx, tv ->
            tv.setPadding(0, gapPx, 0, gapPx)   // 行间距跟「行间距」偏好走（0dp = 三行紧贴）
            if (idx == 1) {
                tv.setTextSize(fs + 2f)
                tv.setTypeface(if (bold) Typeface.DEFAULT_BOLD else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))
                tv.setTextColor(mainColor)
                tv.setShadowLayer(dp(4).toFloat(), 0f, 0f, shadowColored)
            } else {
                tv.setTextSize(fs)
                tv.setTypeface(Typeface.create(tv.typeface, Typeface.NORMAL))
                tv.setTextColor(otherColor)
                tv.setShadowLayer(dp(3).toFloat(), 0f, 0f, shadowColored)
            }
            tv.gravity = lineGravity
        }
        val showPN = FloatingLyricsPrefs.showPrevNext(this)
        prevText?.visibility = if (showPN) View.VISIBLE else View.GONE
        nextText?.visibility = if (showPN) View.VISIBLE else View.GONE
        applyCardBackground()
    }

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
                            // 「松手吸附屏幕边缘」偏好：吸附到最近的左右边缘（默认关，行为不变）
                            if (FloatingLyricsPrefs.snapToEdge(this@FloatingLyricsService)) {
                                val w = (root?.width ?: 0).takeIf { it > 0 } ?: dp(240)
                                val right = (resources.displayMetrics.widthPixels - w).coerceAtLeast(0)
                                params.x = if (abs(params.x) <= abs(params.x - right)) 0 else right
                            }
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
                    FloatingLyricsPrefs.paddingHDp(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.windowWidthDp(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.windowHeightDp(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.cardWidthPercent(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.shadowAlphaPercent(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.lyricColorIndex(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.lyricBgIndex(this@FloatingLyricsService) +
                    FloatingLyricsPrefs.showBorder(this@FloatingLyricsService)
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
