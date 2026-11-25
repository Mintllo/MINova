package com.HyperX.MI.Na.stats

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 听歌时长统计与足迹（纯本地存储）。
 *
 * 数据结构（SharedPreferences "listen_stats"，三个 JSON 串持久化）：
 *  - perSong   按歌曲累计：key = source:id，含累计时长 totalMs、播放次数 playCount、最近播放 lastTs
 *  - perDay    按天累计："yyyy-MM-dd" -> 当天累计毫秒
 *  - footprint 足迹流水：每段播放合并成一条（同一首歌 10 分钟内自动并段），新记录在最前，上限 400 条
 *
 * key 用 source:id 的原因与 MainActivity.Song.source 注释一致：id 只在平台内唯一，
 * 不同平台的同名 id 不是同一首歌。
 */
object ListenStatsStore {

    data class SongStat(
        val key: String,
        val songId: String,
        val source: String,
        val title: String,
        val artist: String,
        val cover: String,
        val totalMs: Long,
        val playCount: Int,
        val lastTs: Long
    )

    data class FootprintEntry(
        val key: String,
        val title: String,
        val artist: String,
        val cover: String,
        val ts: Long,
        val ms: Long
    )

    data class Snapshot(
        val songs: List<SongStat> = emptyList(),
        val perDay: Map<String, Long> = emptyMap(),
        val footprint: List<FootprintEntry> = emptyList(),
        val totalMs: Long = 0L
    )

    private const val PREFS = "listen_stats"
    private const val KEY_SONGS = "per_song"
    private const val KEY_DAYS = "per_day"
    private const val KEY_FOOTPRINT = "footprint"
    private const val FOOTPRINT_CAP = 400
    /** 同一首歌在该时间窗内的记录合并为同一段足迹 */
    private const val MERGE_WINDOW_MS = 10 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private var prefs: SharedPreferences? = null
    private val songs = LinkedHashMap<String, SongStat>()
    private val perDay = LinkedHashMap<String, Long>()
    private val footprint = ArrayDeque<FootprintEntry>()

    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot

    fun init(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs != null) return
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            scope.launch { load() }
        }
    }

    private suspend fun load() {
        val p = prefs ?: return
        mutex.withLock {
            try {
                val sArr = JSONArray(p.getString(KEY_SONGS, "[]"))
                for (i in 0 until sArr.length()) {
                    val o = sArr.getJSONObject(i)
                    val key = o.optString("key")
                    if (key.isEmpty()) continue
                    songs[key] = SongStat(
                        key = key,
                        songId = o.optString("songId"),
                        source = o.optString("source"),
                        title = o.optString("title"),
                        artist = o.optString("artist"),
                        cover = o.optString("cover"),
                        totalMs = o.optLong("totalMs"),
                        playCount = o.optInt("playCount"),
                        lastTs = o.optLong("lastTs")
                    )
                }
                val dArr = JSONArray(p.getString(KEY_DAYS, "{}"))
                for (i in 0 until dArr.length()) {
                    val o = dArr.getJSONObject(i)
                    val day = o.optString("day")
                    if (day.isNotEmpty()) perDay[day] = o.optLong("ms")
                }
                val fArr = JSONArray(p.getString(KEY_FOOTPRINT, "[]"))
                for (i in 0 until fArr.length()) {
                    val o = fArr.getJSONObject(i)
                    footprint.addLast(
                        FootprintEntry(
                            key = o.optString("key"),
                            title = o.optString("title"),
                            artist = o.optString("artist"),
                            cover = o.optString("cover"),
                            ts = o.optLong("ts"),
                            ms = o.optLong("ms")
                        )
                    )
                }
                publishLocked()
            } catch (_: Exception) {
                // 损坏的旧数据不阻塞统计，直接从空数据开始
            }
        }
    }

    /**
     * 累计一段播放时长。由 ListenStatsTracker 在暂停/切歌/攒够 5 秒时调用，
     * 调用线程不限，内部统一在 IO 协程里落盘。
     */
    fun record(songId: String, source: String, title: String, artist: String, cover: String, ms: Long, ts: Long) {
        if (ms <= 0 || songId.isEmpty()) return
        scope.launch {
            mutex.withLock {
                val key = "$source:$songId"
                val day = dayFormat.format(Date(ts))
                val old = songs[key]
                val isNewSession = old == null || ts - old.lastTs > MERGE_WINDOW_MS
                songs[key] = SongStat(
                    key = key,
                    songId = songId,
                    source = source,
                    title = title,
                    artist = artist,
                    cover = cover,
                    totalMs = (old?.totalMs ?: 0L) + ms,
                    playCount = (old?.playCount ?: 0) + if (isNewSession) 1 else 0,
                    lastTs = ts
                )
                perDay[day] = (perDay[day] ?: 0L) + ms
                val head = footprint.firstOrNull()
                if (head != null && head.key == key && ts - head.ts <= MERGE_WINDOW_MS) {
                    footprint[0] = head.copy(ts = ts, ms = head.ms + ms)
                } else {
                    footprint.addFirst(FootprintEntry(key, title, artist, cover, ts, ms))
                    while (footprint.size > FOOTPRINT_CAP) footprint.removeLast()
                }
                publishLocked()
                persistLocked()
            }
        }
    }

    private fun publishLocked() {
        val total = perDay.values.sum()
        _snapshot.value = Snapshot(
            songs = songs.values.sortedByDescending { it.totalMs },
            perDay = perDay.toSortedMap(compareByDescending { it }),
            footprint = footprint.toList(),
            totalMs = total
        )
    }

    private fun persistLocked() {
        val p = prefs ?: return
        try {
            val sArr = JSONArray()
            songs.values.forEach { st ->
                sArr.put(JSONObject().apply {
                    put("key", st.key); put("songId", st.songId); put("source", st.source)
                    put("title", st.title); put("artist", st.artist); put("cover", st.cover)
                    put("totalMs", st.totalMs); put("playCount", st.playCount); put("lastTs", st.lastTs)
                })
            }
            val dArr = JSONArray()
            perDay.forEach { (day, ms) -> dArr.put(JSONObject().put("day", day).put("ms", ms)) }
            val fArr = JSONArray()
            footprint.forEach { fe ->
                fArr.put(JSONObject().apply {
                    put("key", fe.key); put("title", fe.title); put("artist", fe.artist)
                    put("cover", fe.cover); put("ts", fe.ts); put("ms", fe.ms)
                })
            }
            p.edit()
                .putString(KEY_SONGS, sArr.toString())
                .putString(KEY_DAYS, dArr.toString())
                .putString(KEY_FOOTPRINT, fArr.toString())
                .apply()
        } catch (_: Exception) {
        }
    }
}
