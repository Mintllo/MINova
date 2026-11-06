package com.HyperX.MI.Na

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 歌单云端同步。
 *
 * 背景：歌单以前只写在本机 SharedPreferences 里，卸载重装 / 换设备就全丢了，
 * 用户只能重新导入一遍。后端其实早就提供了完整的歌单接口（增删改查 + 歌曲批量导入），
 * 这里把它接起来：
 *
 *  - **上行**：本地有、云端没有的歌单 → 在云端建一个，再把曲目批量灌进去
 *  - **下行**：云端有、本地没有的歌单 → 还原到本地（卸载重装后靠这一步找回）
 *
 * 本地 id ↔ 云端 id 的映射单独存一份，避免每次同步都重复创建同名歌单。
 * 云端按「歌单名 + 分区」认领已存在的歌单，所以即使映射丢了也不会重复建。
 */
object PlaylistCloudSync {

    private const val PREF = "minova_playlist_cloud"
    private const val KEY_MAP = "local_to_cloud"

    /** 云端歌单的轻量视图，只保留同步需要的字段。 */
    private data class CloudPlaylist(
        val id: Long,
        val name: String,
        val sourceApp: String,
        val partition: String,
        val coverUrl: String,
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /**
     * 业务后端的根地址。
     *
     * 应用里配置的「服务地址」指向音乐聚合服务（形如 `https://host:port/music-api`），
     * 而账号/收藏/歌单这些业务接口挂在同一个 nginx 的根路径下，
     * 所以这里把 `/music-api` 后缀去掉即可。
     */
    private fun apiRoot(ctx: Context): String =
        MusicApiPrefs.baseUrl(ctx).trimEnd('/').removeSuffix("/music-api")

    private fun loadMap(ctx: Context): MutableMap<String, String> {
        val raw = prefs(ctx).getString(KEY_MAP, "") ?: ""
        val out = mutableMapOf<String, String>()
        if (raw.isBlank()) return out
        runCatching {
            val o = JSONObject(raw)
            o.keys().forEach { k -> out[k] = o.optString(k) }
        }
        return out
    }

    private fun saveMap(ctx: Context, map: Map<String, String>) {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v) }
        prefs(ctx).edit().putString(KEY_MAP, o.toString()).apply()
    }

    /**
     * 执行一次双向同步。
     *
     * @return 合并后的歌单列表；没有任何变化时返回 null（调用方据此跳过写盘）。
     */
    suspend fun sync(
        ctx: Context,
        token: String,
        local: List<PlaylistInfo>,
    ): List<PlaylistInfo>? = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext null
        val root = apiRoot(ctx)

        // 拉不到云端列表（未登录/网络问题）就直接放弃，绝不动本地数据
        val cloud = runCatching { listCloud(root, token) }.getOrNull() ?: return@withContext null

        val map = loadMap(ctx)
        val before = map.toMap()
        val restored = mutableListOf<PlaylistInfo>()

        // 云端按「名字+分区」建索引，用于认领已存在的歌单
        val cloudByKey = cloud.associateBy { it.name + "|" + it.partition }

        // ── ① 上行：本地 → 云端 ──
        for (p in local) {
            val cloudId = map[p.id]
                ?: cloudByKey[p.name + "|" + p.partition]?.id?.toString()
            if (cloudId == null) {
                val created = runCatching { createCloud(root, token, p) }.getOrNull() ?: continue
                map[p.id] = created
                if (p.songs.isNotEmpty()) {
                    uploadSongs(root, token, created, p.songs)
                }
            } else {
                map[p.id] = cloudId
                // 上次上行可能中断在"建了歌单但没灌完曲目"，这里补一次
                val remoteCount = runCatching { listCloudSongs(root, token, cloudId).size }.getOrNull()
                if (remoteCount != null && remoteCount < p.songs.size && p.songs.isNotEmpty()) {
                    uploadSongs(root, token, cloudId, p.songs)
                }
            }
        }

        // ── ② 下行：云端 → 本地（卸载重装后靠这一步恢复）──
        val knownCloudIds = map.values.toSet()
        val localKeys = local.map { it.name + "|" + it.partition }.toSet()
        for (c in cloud) {
            val cid = c.id.toString()
            if (knownCloudIds.contains(cid)) continue
            if (localKeys.contains(c.name + "|" + c.partition)) continue
            val songs = runCatching { listCloudSongs(root, token, cid) }.getOrNull().orEmpty()
            val pl = PlaylistInfo(
                id = "cloud_$cid",
                name = c.name,
                source = MusicSource.fromName(c.sourceApp),
                partition = c.partition.ifBlank { PlaylistStore.PART_IMPORTED },
                songs = songs,
            )
            restored += pl
            map[pl.id] = cid
        }

        if (map != before) saveMap(ctx, map)
        if (restored.isEmpty() && map == before) return@withContext null
        local + restored
    }

    // ---------------------------------------------------------------- HTTP

    private fun open(root: String, path: String, token: String, method: String): HttpURLConnection {
        val conn = URL(root.trimEnd('/') + path).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $token")
        if (method == "POST" || method == "PATCH") {
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
        }
        return conn
    }

    private fun readText(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
        val text = stream.bufferedReader().use { it.readText() }
        conn.disconnect()
        if (code !in 200..299) throw IllegalStateException("HTTP $code")
        return text
    }

    private fun listCloud(root: String, token: String): List<CloudPlaylist> {
        val text = readText(open(root, "/api/v1/playlists", token, "GET"))
        val o = JSONObject(text)
        // 兼容两种返回：{items:[...]} 或直接数组
        val arr: JSONArray = o.optJSONArray("items") ?: JSONArray(text)
        return (0 until arr.length()).mapNotNull { i ->
            val it = arr.optJSONObject(i) ?: return@mapNotNull null
            CloudPlaylist(
                id = it.optLong("id", 0L),
                name = it.optString("name"),
                sourceApp = it.optString("source_app"),
                partition = it.optString("partition"),
                coverUrl = it.optString("cover_url"),
            )
        }.filter { it.id > 0L && it.name.isNotBlank() }
    }

    private fun createCloud(root: String, token: String, p: PlaylistInfo): String {
        val body = JSONObject().apply {
            put("name", p.name)
            put("source_app", p.source.name.lowercase())
            put("partition", p.partition)
            put("cover_url", p.songs.firstOrNull()?.picUrl.orEmpty())
        }
        val conn = open(root, "/api/v1/playlists", token, "POST")
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val o = JSONObject(readText(conn))
        // 后端返回的是 {"playlist":{...},"songs":[]}（顶层直出，没有 data 包装），
        // 这里把几种可能的位置都试一遍，避免因为结构差异拿不到 id。
        val id = o.optJSONObject("playlist")?.optLong("id", 0L)
            ?: o.optJSONObject("data")?.optJSONObject("playlist")?.optLong("id", 0L)
            ?: o.optJSONObject("data")?.optLong("id", 0L)
            ?: o.optLong("id", 0L)
        if (id <= 0L) throw IllegalStateException("创建歌单未返回 id")
        return id.toString()
    }

    /**
     * 上传曲目：优先走批量接口，失败则逐首补。
     *
     * 后端 `/songs/bulk` 目前有 SQL 参数类型推断问题（PostgreSQL 42P08），
     * 批量会整批 500；单首添加走的是另一条 SQL，能成功。
     * 所以这里做成「先批量、失败逐首」，保证云端歌单的曲目能传上去。
     */
    private fun uploadSongs(root: String, token: String, cloudId: String, songs: List<PlaylistSong>) {
        val ok = runCatching { bulkAddSongs(root, token, cloudId, songs) }.isSuccess
        if (ok) return
        songs.forEachIndexed { i, s -> runCatching { addOneSong(root, token, cloudId, s, i) } }
    }

    /** 单首添加（`POST /api/v1/playlists/{id}/songs`）。 */
    private fun addOneSong(root: String, token: String, cloudId: String, s: PlaylistSong, position: Int) {
        val body = JSONObject().apply {
            put("song_id", s.id)
            put("song_name", s.name)
            put("artist", s.artist)
            put("album", s.album)
            put("cover_url", s.picUrl)
            put("source_app", s.source.ifBlank { MusicSource.OTHER.name.lowercase() })
            put("position", position)
        }
        val conn = open(root, "/api/v1/playlists/$cloudId/songs", token, "POST")
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        readText(conn)
    }

    private fun bulkAddSongs(root: String, token: String, cloudId: String, songs: List<PlaylistSong>) {
        val arr = JSONArray()
        songs.forEachIndexed { i, s ->
            arr.put(JSONObject().apply {
                put("song_id", s.id)
                put("song_name", s.name)
                put("artist", s.artist)
                put("album", s.album)
                put("cover_url", s.picUrl)
                put("source_app", s.source.ifBlank { MusicSource.OTHER.name.lowercase() })
                put("position", i)
            })
        }
        val body = JSONObject().put("songs", arr)
        val conn = open(root, "/api/v1/playlists/$cloudId/songs/bulk", token, "POST")
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        readText(conn)
    }

    private fun listCloudSongs(root: String, token: String, cloudId: String): List<PlaylistSong> {
        val text = readText(open(root, "/api/v1/playlists/$cloudId/songs", token, "GET"))
        val o = JSONObject(text)
        val arr: JSONArray = o.optJSONArray("items") ?: o.optJSONArray("songs") ?: JSONArray(text)
        return (0 until arr.length()).mapNotNull { i ->
            val it2 = arr.optJSONObject(i) ?: return@mapNotNull null
            val sid = it2.optString("song_id")
            if (sid.isBlank()) return@mapNotNull null
            PlaylistSong(
                id = sid,
                name = it2.optString("song_name"),
                artist = it2.optString("artist"),
                picUrl = it2.optString("cover_url"),
                album = it2.optString("album"),
                source = it2.optString("source_app"),
            )
        }
    }
}
