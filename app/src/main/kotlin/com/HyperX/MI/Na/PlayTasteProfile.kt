package com.HyperX.MI.Na

import android.content.Context
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * 行为口味画像（轻量 SharedPreferences 计数，纯本地存储）。
 *
 * 用户每播放一首歌 → 把该歌的歌手（可多名字，逗号/斜杠/& 分隔）与风格
 * （歌名/歌手命中 TasteOptions.GENRES 关键词）各 +1 记进计数表：
 *   key = "a|歌手小写" / "g|风格词小写"
 *   值  = { c: 累计次数, t: 最近一次播放时间戳 }
 *
 * 时效衰减：读取时 7 天没再听过的 key 有效权重减半（[effectiveWeight]），不删原始数据。
 *
 * 红线护栏：本画像只参与 For You / 相似推荐（DiscoverScreen rerankByPlayBehavior）的
 * 本地重排打分；「每日 30 首」的组成当天固定，绝不引用本画像，也不被播放重排触碰。
 */
internal object PlayTasteProfile {

    private const val PREF = "minova_play_taste"
    private const val KEY_DATA = "keys"
    private const val P_ARTIST = "a|"
    private const val P_GENRE = "g|"
    /** 计数表上限：超过时淘汰最久没听的 key，防止无限增长 */
    private const val MAX_KEYS = 300
    /** 7 天没听 → 有效权重减半 */
    private const val DECAY_AFTER_MS = 7L * 24L * 60L * 60L * 1000L

    private class Entry(var count: Int, var lastTs: Long)

    private val lock = Any()
    private val data = LinkedHashMap<String, Entry>()
    @Volatile private var loaded = false
    private val io = Executors.newSingleThreadExecutor()

    private fun sp(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun loadOnce(c: Context) {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            try {
                val jo = JSONObject(sp(c).getString(KEY_DATA, "{}") ?: "{}")
                val it = jo.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    val o = jo.optJSONObject(k) ?: continue
                    data[k] = Entry(o.optInt("c"), o.optLong("t"))
                }
            } catch (_: Exception) {
                // 损坏的旧数据不阻塞，从空画像开始
            }
            loaded = true
        }
    }

    private fun persistAsync(c: Context) {
        val snapshot: String = synchronized(lock) {
            // 淘汰最久没听的 key（先排序后删除，避免边遍历边改）
            val over = data.size - MAX_KEYS
            if (over > 0) {
                data.entries.sortedBy { it.value.lastTs }.take(over).map { it.key }
                    .forEach { data.remove(it) }
            }
            val jo = JSONObject()
            data.forEach { (k, e) -> jo.put(k, JSONObject().put("c", e.count).put("t", e.lastTs)) }
            jo.toString()
        }
        io.execute { runCatching { sp(c).edit().putString(KEY_DATA, snapshot).apply() } }
    }

    private fun bumpLocked(key: String, now: Long) {
        val e = data[key]
        if (e == null) data[key] = Entry(1, now) else { e.count++; e.lastTs = now }
    }

    /** 单 key 有效权重：累计次数 ×（7 天外 0.5 的时效衰减） */
    private fun weightLocked(key: String, nowMs: Long): Double {
        val e = data[key] ?: return 0.0
        return e.count.toDouble() * effectiveWeight(e.lastTs, nowMs)
    }

    /** 时效衰减系数：7 天内 1.0，超过 7 天 0.5（权重减半） */
    fun effectiveWeight(lastTs: Long, nowMs: Long): Double =
        if (nowMs - lastTs > DECAY_AFTER_MS) 0.5 else 1.0

    /**
     * 播放一首歌后调用：该歌手各名字 +1；歌名/歌手命中的风格词 +1。
     * 调用线程不限（组合内 LaunchedEffect 主线程即可），落盘在单线程 IO 执行器里。
     */
    fun recordPlay(context: Context, artist: String, name: String) {
        loadOnce(context)
        val now = System.currentTimeMillis()
        synchronized(lock) {
            artist.split(",", "，", "/", "&", "、")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() && it != "未知艺术家" }
                .forEach { a -> bumpLocked(P_ARTIST + a, now) }
            TasteOptions.GENRES.forEach { g ->
                val gl = g.lowercase()
                if (gl.length >= 2 &&
                    (name.lowercase().contains(gl) || artist.lowercase().contains(gl))
                ) bumpLocked(P_GENRE + gl, now)
            }
        }
        persistAsync(context)
    }

    /**
     * 歌手行为权重（主信号）：该歌的歌手名与画像里的歌手 key 精确/双向包含时，取最大有效权重。
     * （聚合源歌手字段常带后缀，如「周杰伦 (Jay Chou)」，双向包含能对上。）
     */
    fun artistWeight(artistNames: List<String>, nowMs: Long = System.currentTimeMillis()): Double {
        synchronized(lock) {
            val keys = data.keys
                .filter { it.startsWith(P_ARTIST) }
                .map { it.removePrefix(P_ARTIST) }
            if (keys.isEmpty()) return 0.0
            var best = 0.0
            for (raw in artistNames) {
                val n = raw.trim().lowercase()
                if (n.isBlank()) continue
                for (k in keys) {
                    if (k == n || n.contains(k) || k.contains(n)) {
                        val w = weightLocked(P_ARTIST + k, nowMs)
                        if (w > best) best = w
                    }
                }
            }
            return best
        }
    }

    /** 风格行为权重（主信号）：歌名 + 歌手命中的每个风格 key 有效权重求和。 */
    fun genreWeight(name: String, artistNames: List<String>, nowMs: Long = System.currentTimeMillis()): Double {
        synchronized(lock) {
            val keys = data.keys
                .filter { it.startsWith(P_GENRE) }
                .map { it.removePrefix(P_GENRE) }
            if (keys.isEmpty()) return 0.0
            val hay = (name + " " + artistNames.joinToString(" ")).lowercase()
            var sum = 0.0
            for (g in keys) if (hay.contains(g)) sum += weightLocked(P_GENRE + g, nowMs)
            return sum
        }
    }
}
