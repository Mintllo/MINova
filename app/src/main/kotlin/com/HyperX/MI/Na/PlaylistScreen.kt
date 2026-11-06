package com.HyperX.MI.Na

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur as glassBlur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class MusicSource(val label: String) {
    NETEASE("网易云音乐"),
    QQ("QQ 音乐"),
    KUGOU("酷狗音乐"),
    KUWO("酷我音乐"),
    MIGU("咪咕音乐"),

    /** Internet Archive 免费曲库：公共领域 / CC 授权的录音，合法且可直接串流。 */
    ARCHIVE("免费曲库"),

    LOCAL("本地音乐"),
    OTHER("其他来源");

    companion object {
        fun fromName(n: String?): MusicSource = entries.firstOrNull { it.name.equals(n, ignoreCase = true) } ?: OTHER
    }
}

data class PlaylistSong(
    val id: String,
    val name: String,
    val artist: String,
    val picUrl: String = "",
    val album: String = "",
    val duration: Long = 0,
    val source: String = "",
)

data class PlaylistInfo(
    val id: String,
    val name: String,
    val source: MusicSource,
    val partition: String,
    val songs: List<PlaylistSong> = emptyList(),
)

/**
 * 去掉歌名结尾的音频文件后缀。
 *
 * 有些平台的歌单接口直接把文件名当歌名返回（"某某歌.mp3"、"某某.flac"），
 * 导入之后标题就永远带着后缀。这里统一在数据层清洗：
 * 读取旧数据时也会顺手修好，用户不用重新导入。
 */
internal fun cleanMediaTitle(raw: String): String {
    val src = raw.trim()
    if (src.isEmpty()) return raw
    val ext = Regex(
        "(?i)\\.(mp3|flac|m4a|wav|ape|ogg|aac|wma|mp4|mkv|ncm|kgm|kgma|qmc0|qmcflac)$"
    )
    var s = src
    var guard = 0
    while (guard++ < 3 && ext.containsMatchIn(s)) s = ext.replace(s, "").trim()
    return s.ifBlank { src }
}

object PlaylistStore {
    private const val PREF = "minova_playlists"
    private const val KEY = "playlists_json"
    const val PART_CREATED = "我创建的"
    const val PART_IMPORTED = "导入的歌单"

    val defaultPartitions = listOf(PART_CREATED, PART_IMPORTED)

    /**
     * 歌单按账号隔离：存储 key 带上当前登录账号（取自 filesDir/user_info.dat，退出登录会被删除）。
     *
     * 旧版本用的是全局 key（playlists_json），任何账号都能读到同一份歌单，
     * 换账号后就会看到别人的歌单。这里首次读取时把全局数据迁移给当前账号并删除全局 key。
     */
    private fun scopedKey(context: Context): String {
        val owner = try {
            val f = java.io.File(context.filesDir, "user_info.dat")
            if (f.exists()) {
                val jo = JSONObject(f.readText()); val u = jo.optLong("id", 0L).let { if (it > 0L) "id$it" else jo.optString("username", "") }
                if (u.isNotBlank()) "u_$u" else "guest"
            } else "guest"
        } catch (_: Exception) {
            "guest"
        }
        return KEY + "_" + owner
    }

    /**
     * 是否已登录账号。
     *
     * 未登录时歌单只会落进 guest 空间；导入歌单需要写入账号名下，所以导入前必须登录。
     */
    fun isLoggedIn(context: Context): Boolean = !scopedKey(context).endsWith("_guest")

    fun load(context: Context): List<PlaylistInfo> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val scoped = scopedKey(context)
        var raw = prefs.getString(scoped, null)
        if (raw == null) {
            // 旧版本的全局歌单只在「未登录空间」里迁移，绝不混进真实账号；
            // 否则第一个登录的账号会把全设备共用的旧歌单据为己有（表现为「歌单所有人可见」）。
            val legacy = prefs.getString(KEY, null)
            if (legacy != null) {
                if (scoped.endsWith("_guest")) {
                    prefs.edit().putString(scoped, legacy).remove(KEY).apply()
                    raw = legacy
                } else {
                    // 已登录：直接丢弃全局残留，避免串号
                    prefs.edit().remove(KEY).apply()
                }
            }
        }
        if (raw == null) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val songsArr = o.optJSONArray("songs") ?: JSONArray()
                val songs = (0 until songsArr.length()).mapNotNull { k ->
                    val so = songsArr.optJSONObject(k) ?: return@mapNotNull null
                    PlaylistSong(
                        id = so.optString("id"),
                        // 读取时就清掉 ".mp3" 之类的后缀，旧数据也能自动修好
                        name = cleanMediaTitle(so.optString("name")),
                        artist = so.optString("artist"),
                        picUrl = so.optString("picUrl", ""),
                        album = so.optString("album", ""),
                        duration = so.optLong("duration", 0),
                        source = so.optString("source", ""),
                    )
                }
                PlaylistInfo(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    source = MusicSource.fromName(o.optString("source")),
                    partition = o.optString("partition").ifEmpty { PART_CREATED },
                    songs = songs,
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, list: List<PlaylistInfo>) {
        val arr = JSONArray()
        list.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("source", p.source.name)
            o.put("partition", p.partition)
            val sa = JSONArray()
            p.songs.forEach { s -> sa.put(
                    JSONObject()
                        .put("id", s.id)
                        .put("name", s.name)
                        .put("artist", s.artist)
                        .put("picUrl", s.picUrl)
                        .put("album", s.album)
                        .put("duration", s.duration)
                        .put("source", s.source)
                ) }
            o.put("songs", sa)
            arr.put(o)
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(scopedKey(context), arr.toString()).apply()
    }

    fun savePlaylist(context: android.content.Context, playlist: PlaylistInfo) {
        val all = load(context).toMutableList()
        val idx = all.indexOfFirst { it.id == playlist.id }
        if (idx >= 0) all[idx] = playlist else all.add(playlist)
        save(context, all)
    }
    fun create(context: Context, name: String, source: MusicSource, partition: String): PlaylistInfo {
        val p = PlaylistInfo("pl_" + System.currentTimeMillis(), name, source, partition.ifBlank { PART_CREATED })
        save(context, load(context) + p)
        return p
    }

    /**
     * 导入歌单用：一次性创建「带曲目」的歌单。
     *
     * 既有 [create] 不接受 songs，导入流程先用远端歌单名建一个空歌单再回写会多一次读写；
     * 这里直接建好带 songs 的歌单，分区默认落到「导入的歌单」（[PART_IMPORTED]）。
     */
    fun createWithSongs(
        context: Context,
        name: String,
        source: MusicSource,
        partition: String,
        songs: List<PlaylistSong>,
    ): PlaylistInfo {
        val p = PlaylistInfo(
            id = "pl_" + System.currentTimeMillis() + "_" + (System.nanoTime() % 100_000),
            name = name.ifBlank { "导入的歌单" },
            source = source,
            partition = partition.ifBlank { PART_IMPORTED },
            songs = songs,
        )
        save(context, load(context) + p)
        return p
    }

    fun delete(context: Context, id: String) {
        save(context, load(context).filterNot { it.id == id })
    }

    fun partitions(context: Context): List<String> =
        (defaultPartitions + load(context).map { it.partition }).distinct()
}

@Composable
private fun Modifier.enterAnimation(index: Int): Modifier = motionEnter(if (index > 0) index else -1)

@Composable
private fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

private val SheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
private val CardShape = RoundedCornerShape(22.dp)
private val TagShape = RoundedCornerShape(8.dp)

/** 导入抽屉里曲目预览最多渲染多少行（导入本身不受此上限影响）。 */
private const val PreviewSongLimit = 200

private fun sourceIcon(src: MusicSource): ImageVector = when (src) {
    MusicSource.NETEASE -> Icons.Rounded.Cloud
    MusicSource.QQ -> Icons.Rounded.MusicNote
    MusicSource.KUGOU -> Icons.Rounded.QueueMusic
    MusicSource.KUWO -> Icons.Rounded.LibraryMusic
    MusicSource.MIGU -> Icons.Rounded.Radio
    MusicSource.ARCHIVE -> Icons.Rounded.LibraryMusic
    MusicSource.LOCAL -> Icons.Rounded.Folder
    MusicSource.OTHER -> Icons.Rounded.MoreHoriz
}

private fun PlaylistSong.toSong(): Song = Song(
    id = id,
    name = name,
    artist = artist,
    album = album,
    duration = duration,
    picUrl = picUrl,
    // 带上来源平台：丢了它，播放/歌词会退化成"按歌名去别的源重搜"，
    // 导致自动切歌时播成 DJ 版或别人翻唱的同名歌。
    source = source,
)

// ============================================================================
//  文案安全网 —— 界面上永远不出现服务器 IP / URL
//  任何来自网络层的错误文案在展示前都过一遍 [uiSafeText]；
//  平台登录链接用 [hideServerAddress]（只抹掉本服务地址，保留平台链接）。
// ============================================================================

private val UrlRegex = Regex("https?://\\S+")
private val IpRegex = Regex("\\b\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?\\b")

private fun uiSafeText(raw: String?): String {
    val text = raw.orEmpty().trim()
    if (text.isEmpty()) return ""
    return text.replace(UrlRegex, "服务").replace(IpRegex, "服务")
}

private fun hideServerAddress(text: String, baseHost: String): String {
    if (text.isBlank()) return ""
    var out = text
    if (baseHost.isNotBlank()) out = out.replace(baseHost, "服务")
    return out
}

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 20.dp, bottom = 18.dp)
            .width(36.dp)
            .height(4.dp)
            .background(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(2.dp)
            )
    )
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = 10.dp)
    )
}

/** 玻璃风格下卡片是半透明玻璃，标签底色也跟着换成浅色叠加，MD3 下用 surfaceVariant。 */
@Composable
private fun tagContainerColor(): Color =
    if (LocalMd3Style.current) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    else Color.White.copy(alpha = 0.16f)

@Composable
private fun MiniTag(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    container: Color = tagContainerColor(),
) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = tint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(container, TagShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/**
 * 双风格卡片：MD3 用 colorScheme.surfaceContainerLow，液态玻璃用 backdrop 玻璃，
 * 无边框、无阴影（不设 tonalElevation、不加 border）。
 *
 * 同时支持单击与长按（长按用于曲目行的功能菜单）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StyleAwareCard(
    backdrop: Backdrop,
    shape: Shape = CardShape,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val md3 = LocalMd3Style.current
    val interaction = remember { MutableInteractionSource() }
    val base = if (md3) {
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
    } else {
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    glassBlur(8.dp.toPx())
                    lens(14.dp.toPx(), 28.dp.toPx())
                },
                highlight = { Highlight.Plain },
            )
    }
    val touchModifier = when {
        onClick == null -> Modifier
        onLongClick == null -> Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick
        )
        else -> Modifier.combinedClickable(
            interactionSource = interaction,
            indication = null,
            onLongClick = onLongClick,
            onClick = onClick
        )
    }
    Row(
        modifier = base
            .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
            .then(modifier)
            .then(touchModifier)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(viewModel: PlayerViewModel, onBack: () -> Unit, backdrop: Backdrop) {
    val context = LocalContext.current
    var playlists by remember { mutableStateOf(PlaylistStore.load(context)) }
    var sheet by remember { mutableStateOf<PlaylistSheetKind?>(null) }
    // 「立即前往导入」请求：进入歌单页后自动打开导入面板
    val importRequested = ImportBus.pending.value
    var showImportLoginRequired by remember { mutableStateOf(false) }
    LaunchedEffect(importRequested) {
        if (importRequested) {
            // 导入歌单要落到账号名下，未登录时先提示去登录
            if (PlaylistStore.isLoggedIn(context)) {
                sheet = PlaylistSheetKind.IMPORT
            } else {
                showImportLoginRequired = true
            }
            ImportBus.consume()
        }
    }

    if (showImportLoginRequired) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showImportLoginRequired = false },
            title = { Text("登录后即可导入", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "导入的歌单会保存到你的账号下（卸载重装也能恢复）。\n\n" +
                        "点「去登录」，登录成功后会自动回来打开导入。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportLoginRequired = false
                    // 记住意图：登录成功后自动继续导入；返回上一层即账号页
                    ImportBus.afterLogin = true
                    onBack()
                }) { Text("去登录") }
            },
            dismissButton = {
                TextButton(onClick = { showImportLoginRequired = false }) { Text("取消") }
            }
        )
    }
    var detailId by remember { mutableStateOf<String?>(null) }
    var menuSong by remember { mutableStateOf<PlaylistSong?>(null) }
    var addTarget by remember { mutableStateOf<Song?>(null) }
    val isMd3 = LocalMd3Style.current

    fun refresh() {
        playlists = PlaylistStore.load(context)
        // 本地一变就顺手同步到云端（未登录时内部会直接跳过）
        viewModel.syncPlaylists()
    }
    val detail = playlists.firstOrNull { it.id == detailId }
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // 曲目长按 → 「加入歌单」走全局的 AddToPlaylistSheet（与搜索页同一套逻辑）
    addTarget?.let { target ->
        AddToPlaylistSheet(
            song = target,
            onDismiss = { addTarget = null },
            onAdded = { pl ->
                val ps = PlaylistSong(id = target.id, name = target.name, artist = target.artist, picUrl = target.picUrl)
                PlaylistStore.savePlaylist(context, pl.copy(songs = pl.songs + ps))
                addTarget = null
                refresh()
                android.widget.Toast.makeText(context, "已加入「${pl.name}」", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            detail?.name ?: "我的歌单",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (detail != null) detailId = null else onBack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回")
                        }
                    },
                    actions = {
                        if (detail != null) {
                            IconButton(onClick = {
                                PlaylistStore.delete(context, detail.id)
                                detailId = null
                                refresh()
                            }) {
                                Icon(Icons.Rounded.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (detail == null) {
                    // —— 两个主操作：新建 / 导入（同一个圆角容器，横向并排）——
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(
                                if (isMd3) MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
                                else Color.White.copy(alpha = 0.10f),
                                RoundedCornerShape(26.dp)
                            )
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SearchLiquidButton(
                                onClick = { sheet = PlaylistSheetKind.CREATE },
                                backdrop = backdrop,
                                shape = RoundedCornerShape(22.dp),
                                label = "新建歌单",
                                icon = Icons.Rounded.Add,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(1.dp)
                                .height(28.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            SearchLiquidButton(
                                // 导入要存到账号名下，未登录先提示登录
                                onClick = {
                                    if (PlaylistStore.isLoggedIn(context)) sheet = PlaylistSheetKind.IMPORT
                                    else showImportLoginRequired = true
                                },
                                backdrop = backdrop,
                                shape = RoundedCornerShape(22.dp),
                                label = "导入歌单",
                                icon = Icons.Rounded.CloudDownload,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    val grouped = PlaylistStore.partitions(context).map { part ->
                        part to playlists.filter { it.partition == part }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        grouped.forEach { (part, itemsList) ->
                            if (itemsList.isNotEmpty()) {
                                item(key = "h_$part") {
                                    Text(
                                        part,
                                        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 2.dp),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                itemsIndexed(itemsList, key = { _, it -> it.id }) { idx, pl ->
                                    StyleAwareCard(
                                        backdrop = backdrop,
                                        onClick = { detailId = pl.id },
                                        modifier = Modifier.animateItem().enterAnimation(idx)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Rounded.QueueMusic,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                pl.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(7.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                MiniTag(
                                                    text = pl.source.label,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                MiniTag(text = "${pl.songs.size} 首")
                                                MiniTag(
                                                    text = pl.partition,
                                                    modifier = Modifier.weight(0.8f, fill = false),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (playlists.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Rounded.QueueMusic,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "还没有歌单",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "新建一个歌单，或从音乐平台导入",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    Button(
                                        onClick = { sheet = PlaylistSheetKind.CREATE },
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("新建歌单", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            StyleAwareCard(backdrop = backdrop) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("来源应用", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        detail.source.label,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("所属分区", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        detail.partition,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        if (detail.songs.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Rounded.MusicNote,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text("歌单是空的", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "可以在搜索页长按歌曲，选择「加入歌单」",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        itemsIndexed(detail.songs, key = { index, it -> "${it.id}#$index" }) { idx, song ->
                            val downloaded = downloadedSongs.any { it.id == song.id }
                            StyleAwareCard(
                                backdrop = backdrop,
                                onClick = {
                                    viewModel.playExternalSong(
                                        song.toSong(),
                                        song.source.ifBlank { detail.source.apiSourceName() ?: "netease" },
                                        // 整张歌单作为播放队列 → 播完自动续下一首
                                        detail.songs.map { it.toSong() }
                                    )
                                },
                                onLongClick = { menuSong = song },
                                modifier = Modifier.animateItem().enterAnimation(idx)
                            ) {
                                PlaylistSongCover(song = song, size = 52.dp)
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        song.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        song.artist.ifBlank { "未知歌手" },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (downloaded) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            "已下载",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                }
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    "播放",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 曲目长按菜单：加入歌单 / 下载（含下载状态反馈）/ 收藏
    menuSong?.let { song ->
        PlaylistSongMenuSheet(
            song = song,
            viewModel = viewModel,
            onDismiss = { menuSong = null },
            onAddToPlaylist = { model ->
                menuSong = null
                addTarget = model
            }
        )
    }

    sheet?.let { kind ->
        when (kind) {
            // 新建歌单：保持原有「手动建空歌单」逻辑不变
            PlaylistSheetKind.LOCAL -> LocalImportSheet(onDismiss = { sheet = null })
            PlaylistSheetKind.CREATE -> PlaylistEditorSheet(
                kind = kind,
                onDismiss = { sheet = null },
                onConfirm = { name, source, partition ->
                    PlaylistStore.create(context, name, source, partition)
                    refresh()
                    sheet = null
                }
            )

            // 导入歌单：从 go-music-api 异步拉取远端歌单 → 选歌单/曲目/目标歌单/分区 → 写入本地
            PlaylistSheetKind.IMPORT -> PlaylistImportSheet(
                onDismiss = { sheet = null },
                onImported = {
                    refresh()
                    sheet = null
                }
            )
        }
    }
}

@Composable
private fun PlaylistSongCover(song: PlaylistSong, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        if (song.picUrl.isNotBlank()) {
            AsyncImage(
                model = song.picUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Rounded.MusicNote,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================================================
//  曲目长按菜单 —— 与搜索页 SongItem 的能力对齐：收藏 / 加入歌单 / 下载
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistSongMenuSheet(
    song: PlaylistSong,
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onAddToPlaylist: (Song) -> Unit,
) {
    val favorites by viewModel.favorites.collectAsState()
    val downloaded by viewModel.downloadedSongs.collectAsState()
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val model = remember(song) { song.toSong() }
    val isFavorite = favorites.any { it.id == song.id }
    val isDownloaded = downloaded.any { it.id == song.id }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = SheetShape,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PlaylistSongCover(song = song, size = 56.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist.ifBlank { "未知歌手" },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            MenuActionRow(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                tint = MaterialTheme.colorScheme.primary,
                text = if (isFavorite) "取消收藏" else "收藏到列表",
            ) {
                if (isFavorite) viewModel.removeFromFavorites(song.id) else viewModel.addToFavorites(model)
                onDismiss()
            }

            Spacer(Modifier.height(8.dp))

            MenuActionRow(
                icon = Icons.Rounded.PlaylistAdd,
                tint = MaterialTheme.colorScheme.primary,
                text = "加入歌单",
            ) { onAddToPlaylist(model) }

            Spacer(Modifier.height(8.dp))

            MenuActionRow(
                icon = when {
                    downloading -> null
                    isDownloaded -> Icons.Rounded.Delete
                    else -> Icons.Rounded.Download
                },
                tint = if (isDownloaded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                textColor = if (isDownloaded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                text = when {
                    downloading -> "下载中 ${(progress * 100).toInt()}%"
                    isDownloaded -> "删除下载"
                    else -> "下载音乐"
                },
                leading = if (downloading) {
                    {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null,
            ) {
                when {
                    isDownloaded -> {
                        viewModel.deleteDownloadedSong(song.id)
                        onDismiss()
                    }
                    !downloading -> {
                        downloading = true
                        viewModel.downloadSong(
                            song = model,
                            onProgress = { progress = it },
                            onComplete = { success ->
                                downloading = false
                                if (success) onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuActionRow(
    icon: ImageVector?,
    tint: Color,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
            } else if (icon != null) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(text, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = textColor)
        }
    }
}

enum class PlaylistSheetKind { CREATE, IMPORT, LOCAL }

/**
 * 「立即前往导入」的跨屏触发：
 * 引导第 5 项 / 风格页点按钮 → 置 true；
 * MainNavigationScreen 监听到后切到歌单页，PlaylistScreen 自动打开导入面板（[PlaylistSheetKind.IMPORT]）。
 */
internal object ImportBus {
    val pending = mutableStateOf(false)

    /**
     * 用户点了「立即前往导入」但还没登录时置位。
     * 登录成功后由账号页读取它，自动继续打开导入面板 —— 避免"点了导入却被弹窗告知要登录"的死胡同。
     */
    var afterLogin = false

    fun request() { pending.value = true }
    fun consume() { pending.value = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistEditorSheet(
    kind: PlaylistSheetKind,
    onDismiss: () -> Unit,
    onConfirm: (String, MusicSource, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(MusicSource.NETEASE) }
    var partition by remember {
        mutableStateOf(if (kind == PlaylistSheetKind.CREATE) PlaylistStore.PART_CREATED else PlaylistStore.PART_IMPORTED)
    }
    val context = LocalContext.current
    val isCreate = kind == PlaylistSheetKind.CREATE
    val partitions = PlaylistStore.partitions(context)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = SheetShape,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    if (isCreate) "新建歌单" else "导入歌单",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isCreate) "选择歌单来自哪个音乐应用，并指定它所属的分区"
                    else "指定歌单来自哪个音乐应用，并选择导入到哪个分区",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("歌单名称") },
                    placeholder = { Text(if (isCreate) "给歌单起个名字" else "例如：我的网易云收藏") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(Modifier.height(24.dp))
                SectionLabel(if (isCreate) "来自哪个音乐应用" else "来源应用")
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // 免费曲库（Internet Archive）没有"平台歌单"可导入，这里不列出来
                    MusicSource.entries.filter { it != MusicSource.ARCHIVE }.forEach { src ->
                        SheetPickRow(
                            label = src.label,
                            selected = src == source,
                            icon = sourceIcon(src),
                            onSelect = { source = src }
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
                SectionLabel(if (isCreate) "放到哪个分区" else "导入到分区")
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    partitions.forEach { part ->
                        SheetPickRow(
                            label = part,
                            selected = part == partition,
                            icon = Icons.Rounded.LibraryMusic,
                            onSelect = { partition = part }
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) { Text("取消", fontSize = 15.sp, fontWeight = FontWeight.Medium) }

                    Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onConfirm(name.trim(), source, partition)
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(if (isCreate) "创建" else "导入", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SheetPickRow(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onSelect: () -> Unit,
) {
    val rowBg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else Color.Transparent,
        animationSpec = tween(180),
        label = "pickBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "pickIcon"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(rowBg, RoundedCornerShape(16.dp))
            .clickable(interactionSource = null, indication = null, onClick = onSelect)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(Motion.enter()) + scaleIn(
                initialScale = 0.6f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f)
            ),
            exit = fadeOut(Motion.exit()) + scaleOut(targetScale = 0.6f, animationSpec = Motion.exit())
        ) {
            Icon(
                Icons.Rounded.Check,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun QrCodeImage(url: String, size: Int) {
    val bitmap = remember(url) {
        try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val hints = java.util.EnumMap<com.google.zxing.EncodeHintType, Any>(com.google.zxing.EncodeHintType::class.java)
            hints[com.google.zxing.EncodeHintType.MARGIN] = 1
            val matrix = writer.encode(url, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512, hints)
            val bmp = android.graphics.Bitmap.createBitmap(matrix.width, matrix.height, android.graphics.Bitmap.Config.ARGB_8888)
            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    bmp.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bmp
        } catch (_: Exception) { null }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "二维码",
            modifier = Modifier.size(size.dp)
        )
    }
}

// ============================================================================
//  「导入歌单」抽屉 —— 对接 go-music-api（网络层见 MusicApiClient.kt）
//
//  布局为清晰的三段式：
//    ① 顶部固定：标题「导入歌单」+ 服务连接状态（只显示「服务连接正常 / 失败」，不显示地址）
//    ② 中部滚动：来源平台 → 操作行（重试/扫码登录/测试连接）→ 扫码区（二维码 + 登录链接 + 可选 Cookie）
//                → 远端歌单列表 → 曲目预览（全选/反选/清空）→ 导入到歌单 → 导入到分区
//    ③ 底部固定：取消 + 导入到「目标歌单名」（始终可见，带 navigationBarsPadding + imePadding）
//
//  所有网络请求都在 MusicApiClient 内部切到 Dispatchers.IO，UI 线程不做网络。
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistImportSheet(
    onDismiss: () -> Unit,
    onImported: () -> Unit,
    /** 作为独立页面（引导流程）打开时，在标题下方显示「返回应用」 */
    standalone: Boolean = false,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // 只保留上游支持在线导入的三个平台
    val supportedSources = remember { listOf(MusicSource.NETEASE, MusicSource.QQ, MusicSource.KUGOU) }

    var source by remember { mutableStateOf(MusicSource.NETEASE) }
    var partition by remember { mutableStateOf(PlaylistStore.PART_IMPORTED) }
    var cookie by remember { mutableStateOf(MusicApiPrefs.cookie(context, MusicSource.NETEASE)) }
    var showCookiePanel by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var playlists by remember { mutableStateOf<List<RemotePlaylist>>(emptyList()) }
    var reloadToken by remember { mutableStateOf(0) }

    var selected by remember { mutableStateOf<RemotePlaylist?>(null) }
    var remoteSongs by remember { mutableStateOf<List<PlaylistSong>>(emptyList()) }
    var songsLoading by remember { mutableStateOf(false) }
    var songsHint by remember { mutableStateOf<String?>(null) }
    var selectedSongs by remember { mutableStateOf<Set<String>>(emptySet()) }

    var myPlaylists by remember { mutableStateOf(PlaylistStore.load(context)) }
    var targetPlaylistId by remember { mutableStateOf<String?>(null) }

    var importing by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }

    var qrSession by remember { mutableStateOf<QrLoginSession?>(null) }
    var qrBusy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var connOk by remember { mutableStateOf<Boolean?>(null) }
    var connMessage by remember { mutableStateOf<String?>(null) }

    val partitions = PlaylistStore.partitions(context)
    val canQr = MusicApiClient.qrSupported(source)
    // 只用于「抹掉可能出现的本服务地址」，绝不展示
    val baseHost = remember {
        MusicApiPrefs.baseUrl(context)
            .removePrefix("https://").removePrefix("http://")
            .substringBefore('/')
    }

    val targetPlaylist = myPlaylists.firstOrNull { it.id == targetPlaylistId }
    val targetName = (targetPlaylist?.name ?: selected?.name.orEmpty()).take(12)

    // 切换来源：载入该平台自己的 Cookie，并清掉上一次的选择/提示
    LaunchedEffect(source) {
        cookie = MusicApiPrefs.cookie(context, source)
        selected = null
        remoteSongs = emptyList()
        selectedSongs = emptySet()
        songsHint = null
        importError = null
        qrSession = null
        statusMessage = null
    }

    // 加载歌单列表：切来源、点「重试」都会重新加载
    LaunchedEffect(source, reloadToken) {
        MusicApiClient.configure(context)
        MusicApiPrefs.setCookie(context, source, cookie)
        loading = true
        loadError = null
        selected = null
        try {
            val list = MusicApiClient.fetchPlaylists(source, cookie.ifBlank { null })
            playlists = list
            if (list.isEmpty()) loadError = "没有获取到歌单，可先扫码登录再重试"
        } catch (e: Exception) {
            playlists = emptyList()
            loadError = uiSafeText(e.message).ifBlank { "加载失败，请稍后重试" }
        } finally {
            loading = false
        }
    }

    // 选中远端歌单 → 自动拉取曲目用于预览，默认全选
    LaunchedEffect(selected?.id, reloadToken) {
        val target = selected ?: return@LaunchedEffect
        remoteSongs = emptyList()
        selectedSongs = emptySet()
        songsHint = null
        songsLoading = true
        try {
            MusicApiClient.configure(context)
            val raw = MusicApiClient.fetchPlaylistSongs(
                remoteId = target.id,
                source = source,
                cookie = cookie.ifBlank { null }
            )
            // 网络层在拿不到曲目时会塞一条占位曲目，这里过滤掉并按「空」处理
            val songs = raw.filterNot { it.id == "placeholder" || it.name.startsWith("（该歌单需登录") }
            remoteSongs = songs
            if (songs.isEmpty()) {
                songsHint = "该歌单需登录后才能获取曲目，请先扫码登录"
            } else {
                selectedSongs = songs.map { it.id }.toSet()
            }
        } catch (e: Exception) {
            // 未登录 / 无权限时同样给「友好提示」而不是红色报错
            val msg = uiSafeText(e.message)
            val needsLogin = msg.isBlank() ||
                msg.contains("登录") || msg.contains("401") || msg.contains("403") ||
                msg.contains("cookie", true) || msg.contains("auth", true)
            songsHint = if (needsLogin) "该歌单需登录后才能获取曲目，请先扫码登录"
            else "曲目获取失败：$msg"
        } finally {
            songsLoading = false
        }
    }

    // 扫码登录轮询：拿到 key 后每 2.5s 问一次，成功则回填 Cookie 并刷新歌单
    LaunchedEffect(qrSession?.key, source) {
        val session = qrSession ?: return@LaunchedEffect
        if (session.key.isBlank()) {
            statusMessage = "服务端未返回登录 key，请重新获取二维码"
            qrSession = null
            return@LaunchedEffect
        }
        while (true) {
            delay(2500)
            val status = try {
                MusicApiClient.pollQrStatus(session.key, source)
            } catch (e: Exception) {
                statusMessage = uiSafeText(e.message).ifBlank { "轮询扫码状态失败" }
                qrSession = null
                return@LaunchedEffect
            }
            when (status.state) {
                QrLoginState.SUCCESS -> {
                    if (status.cookie.isNotBlank()) {
                        cookie = status.cookie
                        MusicApiPrefs.setCookie(context, source, status.cookie)
                    }
                    statusMessage = "登录成功，Cookie 已保存"
                    qrSession = null
                    reloadToken++
                    return@LaunchedEffect
                }
                QrLoginState.EXPIRED -> {
                    statusMessage = "二维码已过期，请重新获取"
                    qrSession = null
                    return@LaunchedEffect
                }
                QrLoginState.FAILED -> {
                    statusMessage = uiSafeText(status.message).ifBlank { "扫码登录失败" }
                    qrSession = null
                    return@LaunchedEffect
                }
                QrLoginState.SCANNED -> statusMessage = "已扫码，请在手机上确认"
                else -> statusMessage = "等待扫码…"
            }
        }
    }

    fun doImport() {
        val target = selected ?: return
        if (remoteSongs.isEmpty()) {
            songsHint = "该歌单需登录后才能获取曲目，请先扫码登录"
            return
        }
        val chosenSongs = remoteSongs.filter { selectedSongs.contains(it.id) }
        if (chosenSongs.isEmpty()) {
            importError = "请至少选择一首曲目"
            return
        }
        scope.launch {
            importing = true
            importError = null
            try {
                val chosen = targetPlaylist
                if (chosen != null) {
                    // 导入到已有歌单：按曲目 id 去重后合并
                    val exist = PlaylistStore.load(context).firstOrNull { it.id == chosen.id }
                    if (exist != null) {
                        val existed = exist.songs.map { it.id }.toSet()
                        PlaylistStore.savePlaylist(
                            context,
                            exist.copy(songs = exist.songs + chosenSongs.filterNot { existed.contains(it.id) })
                        )
                    } else {
                        PlaylistStore.createWithSongs(context, chosen.name, source, partition, chosenSongs)
                    }
                } else {
                    PlaylistStore.createWithSongs(
                        context = context,
                        name = target.name.ifBlank { "${source.label} 导入歌单" },
                        source = source,
                        partition = partition,
                        songs = chosenSongs,
                    )
                }
                myPlaylists = PlaylistStore.load(context)
                onImported()
            } catch (e: Exception) {
                importError = uiSafeText(e.message).ifBlank { "导入失败，请稍后重试" }
            } finally {
                importing = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = SheetShape,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // -------- ① 顶部固定：标题 + 服务连接状态 --------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 14.dp)
            ) {
                if (standalone) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                "返回",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "返回应用",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    "导入歌单",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                val connIcon: ImageVector = when (connOk) {
                    true -> Icons.Rounded.Check
                    false -> Icons.Rounded.Close
                    null -> Icons.Rounded.Refresh
                }
                val connTint: Color = when (connOk) {
                    true -> MaterialTheme.colorScheme.primary
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val connText: String = when (connOk) {
                    true -> "服务连接正常"
                    false -> connMessage ?: "服务连接失败，请稍后重试"
                    null -> if (loading) "正在加载歌单…" else "尚未加载歌单"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(connIcon, null, tint = connTint, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(connText, fontSize = 12.sp, color = connTint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // -------- ② 中部滚动区 --------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(18.dp))

                // 1. 来源平台
                SectionLabel("来源平台")
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    supportedSources.forEach { src ->
                        SheetPickRow(
                            label = src.label,
                            selected = src == source,
                            icon = sourceIcon(src),
                            onSelect = { if (src != source) source = src }
                        )
                    }
                }

                // 2. 操作行
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { reloadToken++ }, enabled = !loading) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("重试")
                    }
                    if (canQr) {
                        TextButton(
                            onClick = {
                                qrSession = null
                                statusMessage = "正在获取二维码…"
                                qrBusy = true
                                scope.launch {
                                    try {
                                        MusicApiClient.configure(context)
                                        qrSession = MusicApiClient.createQrCode(source)
                                        statusMessage = "请使用「${source.label}」客户端扫码登录"
                                    } catch (e: Exception) {
                                        statusMessage = uiSafeText(e.message).ifBlank { "获取二维码失败，请稍后重试" }
                                    } finally {
                                        qrBusy = false
                                    }
                                }
                            },
                            enabled = !qrBusy
                        ) { Text(if (qrSession == null) "扫码登录" else "刷新二维码") }
                    }
                }

                statusMessage?.let { msg ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        uiSafeText(msg),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. 扫码区（二维码 + 登录链接 + 可选 Cookie 登录）
                val session = qrSession
                if (session != null) {
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when {
                            session.imageUrl.isNotBlank() -> AsyncImage(
                                model = session.imageUrl,
                                contentDescription = "登录二维码",
                                modifier = Modifier.size(180.dp).clip(RoundedCornerShape(16.dp))
                            )
                            session.qrUrl.isNotBlank() -> Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .padding(10.dp)
                            ) {
                                QrCodeImage(url = session.qrUrl, size = 180)
                            }
                            else -> Text(
                                "服务端未返回二维码，可改用下方「手动粘贴 Cookie 登录」",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (session.qrUrl.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "登录链接（手机浏览器也可打开）",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                hideServerAddress(session.qrUrl, baseHost),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showCookiePanel = !showCookiePanel }) {
                        Text(if (showCookiePanel) "收起手动登录" else "手动粘贴 Cookie 登录")
                    }
                    Spacer(Modifier.weight(1f))
                    if (cookie.isNotBlank()) {
                        Text("已保存登录信息", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (showCookiePanel) {
                    OutlinedTextField(
                        value = cookie,
                        onValueChange = { cookie = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("登录 Cookie（可选）") },
                        placeholder = { Text("例如：MUSIC_U=xxx; __csrf=yyy;") },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { reloadToken++ },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(23.dp)
                    ) {
                        Text("保存并重新加载歌单", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // 4. 远端歌单列表
                Spacer(Modifier.height(20.dp))
                SectionLabel("选择歌单")
                when {
                    loading -> Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("正在加载歌单…", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    loadError != null -> Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            loadError!!,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "可点上方「重试」，或先扫码登录再试",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    playlists.isEmpty() -> Text(
                        "该平台暂时没有返回歌单",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    else -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        playlists.take(60).forEach { pl ->
                            SheetPickRow(
                                label = if (pl.trackCount > 0) "${pl.name}（${pl.trackCount} 首）" else pl.name,
                                selected = pl.id == selected?.id,
                                icon = Icons.Rounded.QueueMusic,
                                onSelect = {
                                    selected = pl
                                    importError = null
                                }
                            )
                        }
                        if (playlists.size > 60) {
                            Text(
                                "仅显示前 60 个，共 ${playlists.size} 个歌单",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                            )
                        }
                    }
                }

                // 5. 曲目预览（选中远端歌单后出现）
                if (selected != null) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "曲目预览",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "已选 ${selectedSongs.size} / ${remoteSongs.size} 首",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (remoteSongs.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { selectedSongs = remoteSongs.map { it.id }.toSet() }) {
                                Text("全选", fontSize = 13.sp)
                            }
                            TextButton(onClick = {
                                selectedSongs = remoteSongs.map { it.id }.toSet() - selectedSongs
                            }) {
                                Text("反选", fontSize = 13.sp)
                            }
                            TextButton(onClick = { selectedSongs = emptySet() }) {
                                Text("清空", fontSize = 13.sp)
                            }
                        }
                    }
                    when {
                        songsLoading -> Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("正在获取曲目…", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        songsHint != null -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                songsHint!!,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> Column(modifier = Modifier.fillMaxWidth()) {
                            remoteSongs.take(PreviewSongLimit).forEach { song ->
                                PreviewSongRow(
                                    song = song,
                                    checked = selectedSongs.contains(song.id),
                                    onToggle = {
                                        selectedSongs = if (selectedSongs.contains(song.id)) {
                                            selectedSongs - song.id
                                        } else {
                                            selectedSongs + song.id
                                        }
                                    }
                                )
                            }
                            if (remoteSongs.size > PreviewSongLimit) {
                                Text(
                                    "仅预览前 $PreviewSongLimit 首，导入仍包含全部 ${remoteSongs.size} 首",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 6. 导入到歌单（新建 or 已有）
                Spacer(Modifier.height(20.dp))
                SectionLabel("导入到歌单")
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SheetPickRow(
                        label = "新建歌单（用远端歌单名）",
                        selected = targetPlaylistId == null,
                        icon = Icons.Rounded.Add,
                        onSelect = { targetPlaylistId = null }
                    )
                    myPlaylists.forEach { pl ->
                        SheetPickRow(
                            label = pl.name + "（" + pl.songs.size + " 首）",
                            selected = targetPlaylistId == pl.id,
                            icon = Icons.Rounded.LibraryMusic,
                            onSelect = { targetPlaylistId = pl.id }
                        )
                    }
                }

                // 7. 导入到分区
                Spacer(Modifier.height(20.dp))
                SectionLabel("导入到分区")
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    partitions.forEach { part ->
                        SheetPickRow(
                            label = part,
                            selected = part == partition,
                            icon = Icons.Rounded.Folder,
                            onSelect = { partition = part }
                        )
                    }
                }
                if (targetPlaylist != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "已选择导入到「${targetPlaylist.name}」，分区以该歌单为准",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(Modifier.height(26.dp))
            }

            // -------- ③ 底部固定：取消 + 导入 --------
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Text(
                    if (selected == null) "请先选择要导入的歌单"
                    else if (targetPlaylist != null) "已选 ${selectedSongs.size} 首 · 合并到「${targetPlaylist.name}」"
                    else "已选 ${selectedSongs.size} 首 · 新建歌单",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (importError != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(importError!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text("取消", fontSize = 15.sp, fontWeight = FontWeight.Medium) }

                    Button(
                        onClick = { doImport() },
                        enabled = selected != null && !importing,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        if (importing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            when {
                                importing -> "导入中…"
                                targetName.isBlank() -> "导入"
                                else -> "导入到「$targetName」"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSongRow(
    song: PlaylistSong,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = null, indication = null, onClick = onToggle)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaylistSongCover(song = song, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.name,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist.ifBlank { "未知歌手" },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
@androidx.compose.material3.ExperimentalMaterial3Api
private fun LocalImportSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = SheetShape
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding()
        ) {
            Text("从本地导入音乐", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text("选择音频文件添加到歌单", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text("选择文件（即将支持）", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 引导流程使用的独立「歌单导入页」：全屏承载，复用 PlaylistImportSheet 的全部导入能力。
 * 下滑关闭、点返回都会回到调用方。
 */
@Composable
fun PlaylistImportPage(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PlaylistImportSheet(
            onDismiss = onBack,
            onImported = onBack,
            standalone = true
        )
    }
}
