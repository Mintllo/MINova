package com.HyperX.MI.Na

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import com.google.gson.Gson

/** 从歌名/专辑解析出版本标签：Live / DJ版 / 翻唱 / 伴奏 / 纯音乐 */
fun parseVersionTag(name: String, album: String = ""): String? {
    val lower = name.lowercase()
    // 只在"版本标注"可能出现的位置匹配：括号内、破折号之后、以及专辑名。
    // 之前直接 lower.contains("live") / contains("cover") 会误伤大量正常歌名：
    //   Be**live**ve → 被标 Live；Dis**cover**y → 被标 翻唱；任何含 "DJ " 的也被标 DJ版。
    val inBrackets = Regex("[（(\\[【]([^）)\\]】]{1,24})[）)\\]】]")
        .findAll(lower).map { it.groupValues[1] }.joinToString(" ")
    val tail = lower.substringAfterLast("-", "").substringAfterLast("–", "").trim()
    val hay = (inBrackets + " " + tail + " " + album.lowercase()).trim()
    fun has(vararg keys: String) = keys.any { hay.contains(it) }
    return when {
        has("dj版", "dj 版", "remix", "dj") -> "DJ版"
        has("live", "演唱会", "concert") -> "Live"
        has("翻唱", "cover", "cover版") -> "翻唱"
        has("伴奏", "instrumental", "accompaniment", "off vocal") -> "伴奏"
        has("纯音乐") -> "纯音乐"
        else -> null
    }
}

/**
 * Enter animation: fade in + rise. staggerIndex > 0 staggers by index (max 5 steps).
 * Implemented as a Modifier extension to avoid touching brace structure.
 */
@Composable
private fun Modifier.enterAnimation(staggerIndex: Int = -1): Modifier = motionEnter(if (staggerIndex > 0) staggerIndex else -1)
/**
 * 「推荐」页（发现页）
 *
 * 版式对齐参考图：
 *  - 顶部两张并列大卡（For You / Daily 30）：大标题 + 右上封面 + 底部歌名/歌手 + 白色圆形播放键
 *      点「卡面」→ 进入该卡的歌曲列表（大卡点进去才显示列表）
 *      点「播放键」→ 直接播放第一首
 *  - 分区标题：大号加粗 + 可选 💗 + 黑色实心播放键 + 右侧灰色 X（隐藏该分区）
 *  - 歌曲行：左封面 / 歌名(粗) + 歌手 / 右侧心形+计数 / 再右侧三列小封面（后续待播）
 *  - 适配双风格：MD3 用 surfaceContainer，液态玻璃用半透明观感
 *  - 数据源：自建免费聚合服务（多源合并）
 */
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
fun CommunityScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val api = MusicApiService.shared
    val ctx = LocalContext.current
    val history by viewModel.historyList.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val playCounts by viewModel.playCounts.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val currentSongNow by viewModel.currentSong.collectAsState()
    val isPlayingNow by viewModel.isPlaying.collectAsState()
    val isMd3 = LocalMd3Style.current
    // ── 音乐偏好（音乐类型 / 关注艺人）──
    // 入口在「设置 → 音乐偏好」；首启弹窗时机 = 引导走完 + 应用风格选完 + 用户还没选过口味。
    // 之前是「一进推荐页就弹」（甚至盖在启动闪屏 / 引导上面），用户明确不接受。
    val guideReady = OnboardingState.guideDone.value &&
        OnboardingState.styleChosen.value &&
        OnboardingState.mainVisible.value
    val tasteVersion = TasteBus.version.intValue
    var showTastePicker by remember { mutableStateOf(false) }
    var tasteGenres by remember { mutableStateOf(RecommendPrefs.genres(ctx)) }
    var tasteArtists by remember { mutableStateOf(RecommendPrefs.artists(ctx)) }
    var tasteTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { OnboardingState.refresh(ctx) }
    // 设置里改完口味（TasteBus.bump）或引导状态变化时重新同步偏好，并决定是否弹首启窗
    LaunchedEffect(guideReady, tasteVersion) {
        tasteGenres = RecommendPrefs.genres(ctx)
        tasteArtists = RecommendPrefs.artists(ctx)
        // 选过 或 说过「不再提醒」就不再自动弹
        if (guideReady && !RecommendPrefs.picked(ctx) && !RecommendPrefs.neverRemind(ctx)) {
            showTastePicker = true
        }
    }

    if (showTastePicker) {
        TastePickerSheet(
            md3 = isMd3,
            genres = tasteGenres,
            artists = tasteArtists,
            required = true,
            onToggleGenre = { g -> tasteGenres = if (g in tasteGenres) tasteGenres - g else tasteGenres + g },
            onToggleArtist = { a -> tasteArtists = if (a in tasteArtists) tasteArtists - a else tasteArtists + a },
            onConfirm = {
                // 保存 + 让「每日 30 首」与旧歌池缓存失效，否则重进还是上一批排行榜歌
                RecommendPrefs.saveAndInvalidate(ctx, tasteGenres, tasteArtists)
                showTastePicker = false
                tasteTick++
            },
            // 点空白处 / 返回键关掉：视为「别再提醒」，以后不再自动弹
            onDismiss = {
                showTastePicker = false
                RecommendPrefs.setNeverRemind(ctx)
            },
            onNeverRemind = {
                showTastePicker = false
                RecommendPrefs.setNeverRemind(ctx)
            }
        )
    }

    var daily by remember { mutableStateOf<List<Song>>(emptyList()) }
    var forYou by remember { mutableStateOf<List<Song>>(emptyList()) }
    var everyone by remember { mutableStateOf<List<Song>>(emptyList()) }
    var similar by remember { mutableStateOf<List<Song>>(emptyList()) }
    var repeated by remember { mutableStateOf<List<Song>>(emptyList()) }
    var interest by remember { mutableStateOf<List<Song>>(emptyList()) }
    var seedTitle by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }
    var lastLoadAt by remember { mutableStateOf(0L) }
    var firstLoadDone by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf("") }

    // 被 X 关掉的分区
    var hiddenSections by remember { mutableStateOf(emptySet<String>()) }
    // 点进大卡后的详情页（null = 停留在推荐首页）
    var openedCard by remember { mutableStateOf<String?>(null) }

    // 各分区歌曲汇总成"兜底播放队列"：点分区里的歌时没显式传队列，
    // 有了它一首放完仍能自动接下一首（之前推荐页放完就停住）。
    LaunchedEffect(daily, forYou, similar, everyone, repeated, interest) {
        viewModel.setFallbackQueue(
            (daily + forYou + similar + everyone + repeated + interest).distinctBy { it.id }
        )
    }

    val favoriteIds = remember(favorites) { favorites.map { it.id }.toHashSet() }

    // 长按歌曲 → 操作面板：收藏 / 加入歌单 / 下载到本地
    // 长按歌曲 → 与搜索页**完全相同**的操作面板（共用 SongLongPressSheet）
    SongActionBus.pendingActions.value?.let { actionSong ->
        SongLongPressSheet(
            song = actionSong,
            isInFavorites = actionSong.id in favoriteIds,
            viewModel = viewModel,
            onDismiss = { SongActionBus.consumeActions() },
            onRequestAddToPlaylist = {
                SongActionBus.consumeActions()
                SongActionBus.requestAddToPlaylist(it)
            }
        )
    }

    LaunchedEffect(history, favorites, searchHistory, tick, tasteTick) {
        // 刷新冷却：连续点击「换一批」不再叠加请求风暴（那正是「重复刷新反而拿不到」的原因）
        val now = System.currentTimeMillis()
        if (tick > 0 && now - lastLoadAt < 4000L) return@LaunchedEffect
        lastLoadAt = now
        loading = true
        loadError = ""

        // 首次进入错峰 600ms，避免和启动时的公告/版本/登录态请求抢带宽与后端配额
        if (tick == 0 && !firstLoadDone) delay(600L)

        // ── 推荐种子：最近 5~10 首播放记录（含从搜索结果点播的歌）为主，叠加收藏与最近搜索词 ──
        val recentPlays = history.take(10)
        val recentFavorites = favorites.take(10)
        val seedSongs = (recentPlays + recentFavorites).distinctBy { it.name }
        val seedArtists = seedSongs
            .map { it.artist }
            .filter { it.isNotBlank() && it != "未知艺术家" }
            .flatMap { it.split(",", "/", "&", "、") }
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "未知艺术家" }
            .distinct()
            .take(5)
        val recentKeywords = searchHistory.take(4).filter { it.isNotBlank() }
        seedTitle = seedSongs.firstOrNull()?.name ?: ""

        // ── 口味画像：每次都从磁盘重新读（设置里刚改完也要立刻生效，不能吃 remember 的旧值）──
        val tasteG = RecommendPrefs.genres(ctx)
        val tasteA = RecommendPrefs.artists(ctx)
        val tasteOn = tasteG.isNotEmpty() || tasteA.isNotEmpty()
        val tasteKeywordSet = (tasteG + tasteA).map { it.lowercase() }.filter { it.isNotBlank() }.toHashSet()
        // 由口味关键词搜出来的歌：歌名 / 歌手里不一定带口味词，单独记账再加权
        val tasteHitKeys = java.util.Collections.synchronizedSet(HashSet<String>())
        fun songKey(s: Song) = (s.name + "|" + s.artist).lowercase()

        fun distribute(list: List<Song>, rndSeed: Long) {
            val rnd = java.util.Random(rndSeed)

            // ── 加权打分推荐（可解释）──
            // 用户画像来源：最近播放、收藏、**搜索最多的关键词**、**听歌次数**。
            val seedArtistSet = seedArtists.map { it.lowercase() }.filter { it.isNotBlank() }.toSet()
            val seedNameSet = seedSongs.map { it.name.lowercase() }.toSet()
            val favArtistSet = recentFavorites.map { it.artist.lowercase() }.filter { it.isNotBlank() }.toSet()
            val favNameSet = recentFavorites.map { it.name.lowercase() }.toSet()
            val historyNameCount = recentPlays.groupingBy { it.name.lowercase() }.eachCount()

            // 搜索热度：同一关键词搜得越多权重越高
            val searchFreq = HashMap<String, Int>()
            searchHistory.forEach { k ->
                val key = k.trim().lowercase()
                if (key.length >= 2) searchFreq[key] = (searchFreq[key] ?: 0) + 1
            }

            fun score(s: Song): Double {
                var v = 0.0
                val a = s.artist.lowercase()
                val n = s.name.lowercase()
                // 1) 种子/收藏的歌手与歌名
                seedArtistSet.forEach { sa -> if (a.contains(sa) || sa.contains(a)) v += 6.0 }
                favArtistSet.forEach { fa -> if (a.contains(fa)) v += 2.5 }
                if (seedNameSet.contains(n)) v += 5.0
                if (favNameSet.contains(n)) v += 2.0
                // 2) 历史重复播放
                v += (historyNameCount[n] ?: 0) * 1.5
                // 3) 听歌次数（按 id）——「听的最多」直接进打分
                v += (playCounts[s.id] ?: 0) * 1.2
                // 4) 搜索最多：关键词命中歌手或歌名
                searchFreq.forEach { (k, f) ->
                    if (k.length >= 2 && (a.contains(k) || n.contains(k))) v += f * 1.8
                }
                // 5) 口味画像（设置里选的类型 / 艺人）：命中歌手或歌名的权重最高
                tasteA.forEach { ta -> val t = ta.lowercase(); if (t.isNotBlank() && a.contains(t)) v += 9.0 }
                tasteG.forEach { tg -> val t = tg.lowercase(); if (t.isNotBlank() && (a.contains(t) || n.contains(t))) v += 6.0 }
                // 6) 由口味关键词整批搜出来的歌：直接加权，保证「首屏就有口味内容」
                if (tasteHitKeys.contains(songKey(s))) v += 5.0
                // 7) 有画像时，非口味来源（榜单 / 热门）降权，避免排行榜歌曲继续霸榜
                else if (tasteOn) v -= 2.0
                if (s.picUrl.isNotBlank()) v += 0.4
                v += rnd.nextDouble() * 1.2
                return v
            }

            val scored = list.map { it to score(it) }
                .sortedByDescending { it.second }
                .map { it.first }

            // 多样性约束：同一歌手最多 5 首
            fun diversify(src: List<Song>, limit: Int): List<Song> {
                val perArtist = HashMap<String, Int>()
                val out = ArrayList<Song>(limit)
                for (s in src) {
                    val key = s.artist.lowercase().ifBlank { "?" }
                    val c = perArtist[key] ?: 0
                    if (c >= 8) continue
                    perArtist[key] = c + 1
                    out.add(s)
                    if (out.size >= limit) break
                }
                return out
            }

            val seeded = if (seedArtistSet.isEmpty()) emptyList() else list.filter { s ->
                seedArtistSet.any { sa -> s.artist.lowercase().contains(sa) }
            }
            val listened = seedSongs.mapNotNull { seed -> list.firstOrNull { it.name == seed.name } }

            // ── 每日 30 首：同一天内保持不变，隔天才重新生成 ──
            // 刷新按钮只刷新其它分区；daily 由日期决定，避免用户刷新后「每日 30 首」也变了。
            val ranked = diversify(scored, 240)
            val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
            val savedDate = RecommendPrefs.dailyDate(ctx)
            val byId = list.associateBy { it.id }
            // 「每日 30 首」必须当天稳定：优先用本地缓存的歌曲本体（不依赖本次候选池）。
            // 之前只存 id、靠实时池子还原 —— 池子一变就还原不出来，于是每次进来都重新生成，
            // 用户看到的就是「老刷新」。现在生成后把歌曲列表整个持久化（saveDailyCache）。
            val cachedDailyAll = RecommendPrefs.getDailyCache(ctx)
            val dailyPick: List<Song> = when {
                savedDate == today && cachedDailyAll.isNotEmpty() -> cachedDailyAll
                savedDate == today -> {
                    val restored = RecommendPrefs.dailyIds(ctx).mapNotNull { byId[it] }
                    if (restored.size >= 5) restored
                    else diversify(ranked, minOf(30, maxOf(6, ranked.size / 2))).also {
                        RecommendPrefs.saveDaily(ctx, today, it.map { s -> s.id })
                        RecommendPrefs.saveDailyCache(ctx, it)
                    }
                }
                else -> diversify(ranked, minOf(30, maxOf(6, ranked.size / 2))).also {
                    RecommendPrefs.saveDaily(ctx, today, it.map { s -> s.id })
                    RecommendPrefs.saveDailyCache(ctx, it)
                }
            }
            // 不足 30 首时用候选池补足（diversify 同歌手上限会减员，导致 28/29 首）
            val dailyList = run {
                var pick = dailyPick.ifEmpty { RecommendPrefs.getDailyCache(ctx) }
                if (pick.size < 30) {
                    val have = pick.map { it.id }.toHashSet()
                    val extra = ranked.filter { it.id !in have }.take(30 - pick.size)
                    if (extra.isNotEmpty()) {
                        pick = pick + extra
                        RecommendPrefs.saveDaily(ctx, today, pick.map { s -> s.id })
                        RecommendPrefs.saveDailyCache(ctx, pick)
                    }
                }
                pick
            }
            // 关键修复：池子为空（网络失败）时不要把已显示的每日 30 覆盖成空列表 ——
            // 之前每次 distribute 都无条件赋值，一次搜索失败首页「每日 30 首」就变 0。
            if (dailyList.isNotEmpty()) {
                daily = dailyList
            }
            val dailyIds = dailyList.map { it.id }.toHashSet()
            val rest = ranked.filter { it.id !in dailyIds }
            val restThenAll = (rest + ranked).distinctBy { it.id }

            // 跨分区去重：按顺序构建，每个分区排除前面分区已用掉的歌（此前后两列大量重复）
            val sectionUsed = HashSet<String>().apply {
                addAll(dailyList.map { it.id })
            }
            // 关键：每个分区**按上限取用**，且只把「真正取走」的 id 记入 sectionUsed。
            // 之前扫描时就标记整个池子为已用 → 第一个分区吃掉全部，后面四个分区全空
            // （界面表现即「只有两个大卡片，下面一片空白」）。
            fun takeFresh(src: List<Song>, limit: Int): List<Song> {
                val out = ArrayList<Song>(limit)
                for (s in src) {
                    if (out.size >= limit) break
                    if (sectionUsed.add(s.id)) out.add(s)
                }
                return out
            }
            forYou = diversify(takeFresh(rest + listened + seeded, 40), 40)
            similar = diversify(takeFresh(seeded + restThenAll, 40), 40)
            repeated = diversify(takeFresh(listened + recentFavorites + restThenAll, 40), 40)
            everyone = diversify(takeFresh(restThenAll.shuffled(rnd), 40), 40)
            interest = diversify(takeFresh(rest + ranked.reversed(), 40), 40)
        }

        // 先用上次成功的结果垫底：进入时不会白屏，网络抽风也还有内容
        if (daily.isEmpty()) {
            val cached = RecommendCache.load(ctx)
            if (cached.isNotEmpty()) distribute(cached, 0L)
        }

        // ── 关键词分批并行：每批 3 个，避免一次打出几十个上游请求把后端/上游打到限流 ──
        // 有口味画像时砍掉大部分通用榜单词：以前 8 个榜单词排在前面，take(18) 常把口味词整段截掉，
        // 结果就是「选完口味还是排行榜的歌」。
        val baseKeywords = if (tasteOn) listOf("热门", "新歌榜")
        else listOf(
            "热歌榜", "热门", "新歌榜", "华语流行",
            "经典老歌", "欧美流行", "日韩流行", "抖音热歌"
        )
        // 口味词放最前面，保证一定进 take(18)
        val allKeywords = (tasteA + tasteG + baseKeywords + seedArtists + recentKeywords).distinct().take(18)

        suspend fun fetchPool(keywords: List<String>): List<Song> {
            val bucket = java.util.Collections.synchronizedList(ArrayList<Song>())
            kotlinx.coroutines.coroutineScope {
                keywords.chunked(3).forEach { batch ->
                    val jobs = batch.map { kw ->
                        async(Dispatchers.IO) {
                            // 推荐池必须用免费源：之前误用网易云专属搜索，
                            // 池子全是热门 VIP 歌，四列重复且全部没有免费音源。
                            val res: List<Song> = kotlinx.coroutines.withTimeoutOrNull(20_000) {
                                api.search(kw, listOf("kuwo", "migu", "qianqian", "qq", "bilibili"))
                                    .getOrDefault(emptyList())
                            } ?: emptyList()
                            // 记录「这批歌是口味关键词搜出来的」，打分时直接加权
                            if (tasteKeywordSet.contains(kw.trim().lowercase())) {
                                res.forEach { s -> tasteHitKeys.add(songKey(s)) }
                            }
                            res
                        }
                    }
                    jobs.map { it.await() }.forEach { list -> bucket.addAll(list) }
                }
            }
            return bucket.toList()
        }

        fun dedup(list: List<Song>): List<Song> {
            val map = LinkedHashMap<String, Song>()
            list.forEach { s ->
                val key = (s.name + "|" + s.artist).lowercase()
                if (!map.containsKey(key)) map[key] = s
            }
            return map.values.toList()
        }

        var allSongs = dedup(fetchPool(allKeywords))
        // 兜底：全空时等 1.2s 用最核心的两个词再试一次
        if (allSongs.isEmpty()) {
            delay(1200L)
            allSongs = dedup(fetchPool(listOf("热歌榜", "热门")))
        }
        firstLoadDone = true

        if (allSongs.isNotEmpty()) {
            RecommendCache.save(ctx, allSongs)
            distribute(allSongs, seedSongs.size.toLong() * 31 + tick)
            loadError = ""
        } else {
            val cached = RecommendCache.load(ctx)
            if (cached.isNotEmpty()) {
                distribute(cached, seedSongs.size.toLong() * 31)
                loadError = "网络不稳，已为你显示上次的推荐"
            } else {
                loadError = "暂时获取不到歌曲，请点右上角刷新重试"
            }
        }
        loading = false
    }

    // ── 大卡详情页（点进去才显示列表）──
    val cardKey = openedCard
    if (cardKey != null) {
        val detail = if (cardKey == "foryou") {
            Triple("猜你喜欢", "For You 沉浸刷歌", forYou)
        } else {
            Triple("每日 30 首", "Daily 30 为你挑选", daily)
        }
        BackHandler { openedCard = null }
        CardDetailPage(
            title = detail.first,
            subtitle = detail.second,
            songs = detail.third,
            favoriteIds = favoriteIds,
            playCounts = playCounts,
            currentSongId = currentSongNow?.id,
            isPlaying = isPlayingNow,
            onBack = { openedCard = null },
            onPlayAll = { detail.third.firstOrNull()?.let { s -> viewModel.playSong(s, detail.third) } },
            onPlay = { viewModel.playSong(it, detail.third) }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        // ── 标题 ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth().enterAnimation(0).padding(start = 20.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "推荐", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (seedTitle.isBlank()) "为你精选 大家都爱听" else "根据你的《$seedTitle》推荐",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { tick++ }) {
                    Icon(Icons.Rounded.Refresh, "换一批", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ── 双大卡 ──
        item {

            Row(
                modifier = Modifier.fillMaxWidth().enterAnimation(1).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BigCard(
                    modifier = Modifier.weight(1f),
                    badge = "For\nYou",
                    title = "猜你喜欢",
                    subtitle = forYou.firstOrNull()?.let { "${it.name} - ${it.artist}" } ?: "沉浸刷歌",
                    cover = forYou.firstOrNull()?.picUrl.orEmpty(),
                    colors = listOf(MaterialTheme.colorScheme.primaryContainer),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { openedCard = "foryou" },
                    onPlay = { forYou.firstOrNull()?.let { viewModel.playSong(it, forYou) } }
                )

                BigCard(
                    modifier = Modifier.weight(1f),
                    badge = "Daily\n30",
                    title = "每日 30 首",
                    subtitle = if (loading) "正在挑选…" else "共 ${daily.size} 首",
                    cover = daily.firstOrNull()?.picUrl.orEmpty(),
                    colors = listOf(MaterialTheme.colorScheme.secondaryContainer),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { openedCard = "daily" },
                    onPlay = { daily.firstOrNull()?.let { viewModel.playSong(it, daily) } }
                )
            }
        }

        // ── 听《X》也会喜欢（参考图：听「X」也会喜欢💗）──

        // 一首都没拿到时给出明确提示，而不是空白页
        if (!loading && daily.isEmpty()) {
            item {
                Text(
                    loadError.ifBlank { "暂时获取不到歌曲，请点右上角刷新重试" },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
        if (similar.isNotEmpty() && "similar" !in hiddenSections) {
            item {
                SectionHeader(
                    title = if (seedTitle.isBlank()) "为你精选💗" else "听「$seedTitle」也会喜欢💗",
                    enterIndex = 2,
                    onPlayAll = { similar.firstOrNull()?.let { viewModel.playSong(it, similar) } },
                    onHide = { hiddenSections = hiddenSections + "similar" }
                )
            }
            item {
                SectionPager(
                    songs = similar,
                    favoriteIds = favoriteIds,
                    playCounts = playCounts,
                    onPlay = { viewModel.playSong(it, similar) },
                    onToggleFavorite = { s ->
                        if (s.id in favoriteIds) viewModel.removeFromFavorites(s.id)
                        else viewModel.addToFavorites(s)
                    }
                )
            }
        }

        // ── 大家都在听 ──
        if (everyone.isNotEmpty() && "everyone" !in hiddenSections) {
            item {
                SectionHeader(
                    title = "大家都在听",
                    enterIndex = 3,
                    onPlayAll = { everyone.firstOrNull()?.let { viewModel.playSong(it, everyone) } },
                    onHide = { hiddenSections = hiddenSections + "everyone" }
                )
            }
            item {
                SectionPager(
                    songs = everyone,
                    favoriteIds = favoriteIds,
                    playCounts = playCounts,
                    onPlay = { viewModel.playSong(it, everyone) },
                    onToggleFavorite = { s ->
                        if (s.id in favoriteIds) viewModel.removeFromFavorites(s.id)
                        else viewModel.addToFavorites(s)
                    }
                )
            }
        }

        // ── 这些反复听过的歌一定值得收藏 ──
        if (repeated.isNotEmpty() && "repeated" !in hiddenSections) {
            item {
                SectionHeader(
                    title = "这些反复听过的歌一定值得收藏",
                    enterIndex = 4,
                    onPlayAll = { repeated.firstOrNull()?.let { viewModel.playSong(it, repeated) } },
                    onHide = { hiddenSections = hiddenSections + "repeated" }
                )
            }
            item {
                SectionPager(
                    songs = repeated,
                    favoriteIds = favoriteIds,
                    playCounts = playCounts,
                    onPlay = { viewModel.playSong(it, repeated) },
                    onToggleFavorite = { s ->
                        if (s.id in favoriteIds) viewModel.removeFromFavorites(s.id)
                        else viewModel.addToFavorites(s)
                    }
                )
            }
        }

        // ── 你可能感兴趣 ──
        if (interest.isNotEmpty() && "interest" !in hiddenSections) {
            item {
                SectionHeader(
                    title = "你可能感兴趣",
                    enterIndex = 5,
                    onPlayAll = { interest.firstOrNull()?.let { viewModel.playSong(it) } },
                    onHide = { hiddenSections = hiddenSections + "interest" }
                )
            }
            item {
                SectionPager(
                    songs = interest,
                    favoriteIds = favoriteIds,
                    playCounts = playCounts,
                    onPlay = { viewModel.playSong(it, similar) },
                    onToggleFavorite = { s ->
                        if (s.id in favoriteIds) viewModel.removeFromFavorites(s.id)
                        else viewModel.addToFavorites(s)
                    }
                )
            }
        }
    }
}

/**
 * 大卡详情页：点大卡后进入，才显示该卡的歌曲列表。
 */
@Composable
private fun CardDetailPage(
    title: String,
    subtitle: String,
    songs: List<Song>,
    favoriteIds: Set<String>,
    playCounts: Map<String, Int>,
    currentSongId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onPlay: (Song) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ArrowBack, "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "还没有挑到歌曲，稍后再试试",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 130.dp)
            ) {
                // ── 榜单头图：大封面 + 渐变遮罩 + 播放全部（不再是直白列表）──
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(190.dp)
                            .clip(RoundedCornerShape(28.dp))
                    ) {
                        val heroCover = songs.firstOrNull()?.picUrl.orEmpty()
                        // 无封面兜底：统一封面占位图铺满（上方有黑色遮罩压字）
                        Image(
                            painter = painterResource(R.drawable.fallback_cover),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (heroCover.isNotBlank()) {
                            AsyncImage(
                                model = heroCover,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Color.Black.copy(alpha = 0.52f)
                            )
                        )
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.88f))
                            Spacer(Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = onPlayAll,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("播放全部", fontSize = 13.sp)
                            }
                        }
                    }
                }
                // ── 名次列表：序号徽标（前三名特殊配色）+ 错峰入场动画 ──
                itemsIndexed(songs, key = { _, s -> "rank_${s.id}" }) { idx, song ->
                    val appear = remember { Animatable(0f) }
                    LaunchedEffect(song.id) {
                        delay((idx.coerceAtMost(14) * 32).toLong())
                        appear.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 340f))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 3.dp)
                            .graphicsLayer {
                                alpha = appear.value
                                translationY = (1f - appear.value) * 42f
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onPlay(song) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val badgeColor = when (idx) {
                            0 -> Color(0xFFFFC107)
                            1 -> Color(0xFFB0BEC5)
                            2 -> Color(0xFFD08B5B)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Box(
                            modifier = Modifier.size(26.dp).clip(CircleShape).background(badgeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${idx + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (idx < 3) Color.Black.copy(alpha = 0.82f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // 无封面兜底：统一封面占位图
                            Image(
                                painter = painterResource(R.drawable.fallback_cover),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            AsyncImage(
                                model = song.picUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                song.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val heat = playCounts[song.id] ?: 0
                            Text(
                                song.artist + if (heat > 0) " ${formatHeat(heat.toLong())}" else "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    Spacer(Modifier.width(8.dp))
                    // 版本标签（原唱/翻唱/DJ版/Live/伴奏/纯音乐）已按用户要求**不再显示**；
                    // parseVersionTag 仍保留，仅用于排序。

                                                // 与搜索列表一致：播放中显示律动条，否则显示时长
                                                androidx.compose.animation.AnimatedContent(
                                                    targetState = song.id == currentSongId && isPlaying,
                                                    transitionSpec = {
                                                        fadeIn(Motion.exit()) togetherWith fadeOut(Motion.exit())
                                                    },
                                                    label = "detailRowTrailing"
                                                ) { showBars ->
                                                    if (showBars) {
                                                        LiveAudioEqualizerBars(tintColor = MaterialTheme.colorScheme.primary)
                                                    } else {
                                                        Text(
                                                            fmtDuration(songDurationMillis(song.duration)),
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                    }
                }
            }
        }
    }
}

/** 大卡（带封面 + 播放键）；点卡面进列表，点播放键直接播放 */
@Composable
private fun BigCard(
    modifier: Modifier = Modifier,
    badge: String,
    title: String,
    subtitle: String,
    cover: String,
    colors: List<Color>,
    contentColor: Color,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    // 按压回弹：大卡按下时轻微缩放，与 MD3 触感一致
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 520f),
        label = "bigCardPress"
    )
    Box(
        modifier = modifier
            .height(196.dp)
            .clip(RoundedCornerShape(24.dp))
            // 纯色实底：不使用任何渐变
            .background(colors.first())
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        // 右上封面
        if (cover.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
            ) {
                CoverBox(url = cover, name = title, size = 92.dp, corner = 16.dp)
            }
        }

        // 左上大标题
        Text(
            badge,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            lineHeight = 30.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 14.dp)
        )

        // 底部：歌名/歌手 + 播放键
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle, fontSize = 11.sp, color = contentColor.copy(alpha = 0.92f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 分区标题：大号加粗 + 播放全部（实心圆）+ 右侧灰色 X（隐藏分区） */
@Composable
private fun SectionHeader(
    title: String,
    enterIndex: Int = -1,
    onPlayAll: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .enterAnimation(enterIndex)
            .padding(start = 20.dp, end = 12.dp, top = 26.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

/** 实心圆形播放键（跟随主题取反色） */
@Composable
private fun RoundPlayButton(size: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.PlayArrow, "播放全部",
            tint = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(size * 0.62f)
        )
    }
}

/**
 * 歌曲行：封面 + 歌名/歌手 + 右侧心形计数 + 三列小封面（后续待播预览）。
 */
/**
 * Grid cell for the recommendation page: square cover + title/artist.
 * Three per row, the whole grid scrolls vertically. The favourite heart sits on
 * the cover so the cell stays compact, and the cell itself is clipped to a
 * rounded shape so the ripple follows the corner radius.
 */
/**
 * 分区横向分页：每页 4 行 × 4 列 = 16 首，固定最多 10 页。
 * 右侧留出 56dp 露出下一页的封面与部分歌名，提示可以横向切换。
 */
@Composable
private fun SectionPager(
    songs: List<Song>,
    favoriteIds: Set<String>,
    playCounts: Map<String, Int>,
    onPlay: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    val pages = remember(songs) { songs.chunked(8).take(10) }
    if (pages.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(start = 16.dp, end = 56.dp),
            pageSpacing = 10.dp,
            modifier = Modifier.fillMaxWidth().height(258.dp)
        ) { pageIndex ->
            val rows = pages[pageIndex].chunked(2)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rows.forEach { rowSongs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowSongs.forEach { song ->
                            SongGridCell(
                                modifier = Modifier.weight(1f),
                                song = song.copy(heat = maxOf(song.heat, (playCounts[song.id] ?: 0).toLong())),
                                isFavorite = song.id in favoriteIds,
                                onClick = { onPlay(song) },
                                onToggleFavorite = { onToggleFavorite(song) }
                            )
                        }
                        repeat(2 - rowSongs.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                repeat(4 - rows.size) { Spacer(Modifier.height(60.dp)) }
            }
        }

        // 页码指示：直观看到共有多少页
        if (pages.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { i ->
                    val active = i == pagerState.currentPage
                    // 当前页平滑变成长条胶囊，而不是"啪"地跳一下
                    val dotWidth by animateDpAsState(
                        targetValue = if (active) 16.dp else 5.dp,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
                        label = "dotWidth$i"
                    )
                    val dotColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        animationSpec = tween(220),
                        label = "dotColor$i"
                    )
                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}
/**
 * 推荐页 → 主界面的动作总线。
 *
 * 推荐页长按歌曲时要和搜索页一样弹出「添加到歌单」面板，
 * 但那个面板挂在主界面的组合作用域里（状态在 MainActivity），
 * 所以用这条极简总线把请求递过去。
 */
internal object SongActionBus {
    /** 请求主界面打开「添加到歌单」面板。 */
    val pendingAddToPlaylist = androidx.compose.runtime.mutableStateOf<Song?>(null)

    /** 请求推荐页弹出长按操作面板（收藏 / 加入歌单 / 下载）。 */
    val pendingActions = androidx.compose.runtime.mutableStateOf<Song?>(null)

    fun requestAddToPlaylist(song: Song) { pendingAddToPlaylist.value = song }
    fun consume() { pendingAddToPlaylist.value = null }

    fun requestActions(song: Song) { pendingActions.value = song }
    fun consumeActions() { pendingActions.value = null }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun SongGridCell(
    song: Song,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(60.dp)
            .enterAnimation()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                // 与搜索页保持一致：长按直接开「添加到歌单」面板
                onLongClick = { SongActionBus.requestActions(song) }
            )
            .padding(horizontal = 2.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(9.dp))
            ) {
                // 无封面兜底：统一封面占位图铺满（原为渐变底 + 首字）
                Image(
                    painter = painterResource(R.drawable.fallback_cover),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (song.picUrl.isNotBlank()) {
                    AsyncImage(
                        model = song.picUrl,
                        contentDescription = song.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.40f))
                    .clickable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FavoriteBorder, "favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        Spacer(Modifier.width(5.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.name, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp
            )
            Text(
                if (song.heat > 0) song.artist + " \u00B7 " + formatHeat(song.heat) else song.artist,
                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun SongRow(
    song: Song,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .enterAnimation()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                // 与搜索页一致：长按弹「添加到歌单」
                onLongClick = { SongActionBus.requestActions(song) }
            )
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverBox(url = song.picUrl, name = song.name, size = 54.dp, corner = 12.dp)
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.name, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist, fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }

        // 心形 + 计数（heat 为后端真实播放量；未知时只显示心形）
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onToggleFavorite)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.FavoriteBorder, "收藏",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(19.dp)
                )
                if (song.heat > 0) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        formatHeat(song.heat),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }


    }
}

/**
 * 封面方块：无图 / 加载失败时兜底为统一的封面占位图（fallback_cover）。
 * 兜底层始终绘制在图片下方，图片加载失败（Coil 不绘制）时自然露出，零额外依赖。
 */
@Composable
private fun CoverBox(url: String, name: String, size: Dp, corner: Dp = 12.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.fallback_cover),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/** 播放量展示：>=10000 用「w+」，避免长数字把行挤爆 */
private fun formatHeat(count: Long): String = when {
    count >= 100_000_000L -> "${count / 100_000_000L}亿+"
    count >= 10_000L -> "${count / 10_000L}w+"
    else -> count.toString()
}

/**
 * 每日推荐的持久化缓存：把上次成功拉到的歌单落盘。
 * 网络抖动/上游限流时用它垫底，避免整页空白 —— 这是「有时候加载不出来」的兜底。
 */
private object RecommendCache {
    private const val PREF = "minova_recommend"
    private const val KEY = "pool_json"

    /**
     * 推荐结果按账号隔离：缓存 key 带上当前登录账号（取自 filesDir/user_info.dat）。
     * 否则同一台设备换账号后会直接命中上一个账号的推荐，看起来「推荐不是按人算的」。
     */
    private fun scopedKey(context: android.content.Context): String {
        val owner = try {
            val f = java.io.File(context.filesDir, "user_info.dat")
            if (f.exists()) {
                val jo = org.json.JSONObject(f.readText())
                val id = jo.optLong("id", 0L)
                if (id > 0L) "id$id" else jo.optString("username", "").ifBlank { "guest" }
            } else "guest"
        } catch (_: Exception) {
            "guest"
        }
        return KEY + "_" + owner
    }

    fun load(context: android.content.Context): List<Song> = try {
        val raw = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
            .getString(scopedKey(context), null) ?: return emptyList()
        val arr = org.json.JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name")
            if (name.isBlank()) return@mapNotNull null
            Song(
                id = o.optString("id"),
                name = name,
                artist = o.optString("artist"),
                album = o.optString("album", ""),
                duration = o.optLong("duration", 0),
                picUrl = o.optString("picUrl", "")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun save(context: android.content.Context, songs: List<Song>) {
        try {
            val arr = org.json.JSONArray()
            songs.take(80).forEach { s ->
                arr.put(
                    org.json.JSONObject()
                        .put("id", s.id)
                        .put("name", s.name)
                        .put("artist", s.artist)
                        .put("album", s.album)
                        .put("duration", s.duration)
                        .put("picUrl", s.picUrl)
                )
            }
            context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
                .edit().putString(scopedKey(context), arr.toString()).apply()
        } catch (_: Exception) {
        }
    }

    /** 口味变化后彻底丢弃旧歌池，避免把上一个画像的排行榜歌曲读回来。 */
    fun clear(context: android.content.Context) {
        try {
            context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
                .edit().remove(scopedKey(context)).apply()
        } catch (_: Exception) {
        }
    }
}

/**
 * 推荐偏好（音乐类型 / 关注艺人）：按账号存储，直接参与推荐打分与关键词扩展。
 * 入口：设置 → 音乐偏好（首启时在引导 + 风格选择完成后弹出一次）。
 */
internal object RecommendPrefs {
    private const val PREF = "minova_taste"

    private fun sp(c: android.content.Context) = c.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)

    /** 按账号隔离：同一设备换账号不应共用口味画像 */
    private fun owner(c: android.content.Context): String = try {
        val f = java.io.File(c.filesDir, "user_info.dat")
        if (f.exists()) {
            val jo = org.json.JSONObject(f.readText())
            val id = jo.optLong("id", 0L)
            if (id > 0L) "id$id" else jo.optString("username", "").ifBlank { "guest" }
        } else "guest"
    } catch (_: Exception) {
        "guest"
    }

    private fun k(c: android.content.Context, base: String) = base + "_" + owner(c)

    /* ---------- 每日 30 首的「当天固定」缓存 ---------- */

    fun dailyDate(c: android.content.Context): String =
        sp(c).getString(k(c, "daily_date"), "") ?: ""

    fun dailyIds(c: android.content.Context): List<String> =
        (sp(c).getString(k(c, "daily_ids"), "") ?: "")
            .split(",").map { it.trim() }.filter { it.isNotBlank() }

    fun saveDaily(c: android.content.Context, date: String, ids: List<String>) {
        sp(c).edit()
            .putString(k(c, "daily_date"), date)
            .putString(k(c, "daily_ids"), ids.joinToString(","))
            .apply()
    }

    private val gson = Gson()

    fun saveDailyCache(c: android.content.Context, songs: List<Song>) {
        val json = gson.toJson(songs)
        sp(c).edit().putString(k(c, "daily_cache"), json).apply()
    }

    fun getDailyCache(c: android.content.Context): List<Song> {
        val json = sp(c).getString(k(c, "daily_cache"), "") ?: ""
        if (json.isBlank()) return emptyList()
        return try {
            // 必须传 Type 本身。之前写成 SongListType::class.java，Gson 会把「匿名 TypeToken 类」
            // 当成目标类型去解析，于是抛 Expected BEGIN_OBJECT but was BEGIN_ARRAY，
            // 直接把首页打崩（历史脏缓存更容易触发）。这里同时做兜底：解析失败就清掉这条缓存。
            (gson.fromJson(json, SongListType) as? List<Song>).orEmpty()
        } catch (e: Exception) {
            runCatching { sp(c).edit().remove(k(c, "daily_cache")).apply() }
            emptyList()
        }
    }

    private val SongListType = object : com.google.gson.reflect.TypeToken<List<Song>>() {}.type

    fun picked(c: android.content.Context) = sp(c).getBoolean(k(c, "picked"), false)

    /**
     * 用户是否已表示「别再提醒我选偏好」。
     *
     * 两种情形都会置位：点空白处/返回键关掉了选择器，或主动点了「不再提醒」。
     * 置位后不再自动弹出（设置里的「音乐偏好」入口仍然可用，随时能手动进去选）。
     */
    fun neverRemind(c: android.content.Context) = sp(c).getBoolean(k(c, "taste_never_remind"), false)

    fun setNeverRemind(c: android.content.Context) {
        sp(c).edit().putBoolean(k(c, "taste_never_remind"), true).apply()
    }

    fun genres(c: android.content.Context): List<String> =
        sp(c).getStringSet(k(c, "genres"), emptySet())?.toList() ?: emptyList()
    fun artists(c: android.content.Context): List<String> =
        sp(c).getStringSet(k(c, "artists"), emptySet())?.toList() ?: emptyList()

    fun save(c: android.content.Context, genres: List<String>, artists: List<String>) {
        sp(c).edit()
            .putBoolean(k(c, "picked"), true)
            .putStringSet(k(c, "genres"), genres.toSet())
            .putStringSet(k(c, "artists"), artists.toSet())
            .apply()
    }

    /** 清掉「每日 30 首」的当天固定结果，让新画像立刻重新生成。 */
    fun clearDaily(c: android.content.Context) {
        sp(c).edit().remove(k(c, "daily_date")).remove(k(c, "daily_ids")).apply()
    }

    /**
     * 保存口味 + 让所有相关缓存失效 + 通知推荐页刷新。
     * 「选完口味还是排行榜的歌 / 重进也没用」的根因就是旧歌池与当天固定的每日 30 没失效。
     */
    fun saveAndInvalidate(c: android.content.Context, genres: List<String>, artists: List<String>) {
        save(c, genres, artists)
        clearDaily(c)
        RecommendCache.clear(c)
        TasteBus.bump()
    }
}

/* ==================== 音乐偏好选择器（首启弹窗 + 设置页共用） ==================== */

/** 可选的音乐类型 / 关注艺人（首启弹窗与设置页共用同一份）。 */
internal object TasteOptions {
    val GENRES = listOf(
        "古风", "京剧", "DJ", "口水旋律", "Funk", "摇滚", "民谣", "电音",
        "说唱", "爵士", "轻音乐", "粤语", "日系", "欧美", "古典"
    )
    val ARTISTS = listOf(
        "虚拟歌姬", "周杰伦", "林俊杰", "陈奕迅", "薛之谦", "邓紫棋",
        "毛不易", "李荣浩", "周深", "华晨宇", "五月天", "Taylor Swift"
    )
}

/** 设置页改完口味后通知推荐页立即刷新（Compose 可观察的版本号）。 */
internal object TasteBus {
    val version = mutableIntStateOf(0)
    fun bump() { version.intValue = version.intValue + 1 }
}

/**
 * 首启引导 / 应用风格选择的进度：只有「引导走完 + 风格选完」之后才允许弹音乐偏好选择器，
 * 避免选择器盖在闪屏 / 引导 / 权限弹窗上面（用户反馈「一进应用就弹」）。
 */
internal object OnboardingState {
    val guideDone = mutableStateOf(false)
    val styleChosen = mutableStateOf(false)

    /** 启动闪屏消失、主界面真正可见（避免弹窗盖在闪屏上）。 */
    val mainVisible = mutableStateOf(false)

    fun refresh(ctx: android.content.Context) {
        val g = ctx.getSharedPreferences("guide_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("guide_done", false)
        val s = ctx.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("style_guide_shown", false)
        guideDone.value = g
        styleChosen.value = s
    }
}

/**
 * 音乐偏好选择器（双风格适配）：
 *  - MD3：Material3 FilterChip + 常规 Column；
 *  - 液态玻璃：沿用设置页 SettingsGlassCard 的实底表面（surfaceBright，solid=true 同款），
 *    不用半透明玻璃，避免底下的内容透上来把字冲糊。
 */
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
internal fun TastePickerBody(
    md3: Boolean,
    genres: List<String>,
    artists: List<String>,
    onToggleGenre: (String) -> Unit,
    onToggleArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val body: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        Text(
            "音乐类型",
            fontSize = if (md3) 19.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "选得越准，推荐越懂你（随时可在设置里改）",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TasteOptions.GENRES.forEach { g ->
                FilterChip(
                    selected = g in genres,
                    onClick = { onToggleGenre(g) },
                    label = { Text(g, fontSize = 13.sp) }
                )
            }
        }
        Text(
            "关注艺人",
            fontSize = if (md3) 17.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
        )
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TasteOptions.ARTISTS.forEach { a ->
                FilterChip(
                    selected = a in artists,
                    onClick = { onToggleArtist(a) },
                    label = { Text(a, fontSize = 13.sp) }
                )
            }
        }
    }

    if (md3) {
        Column(modifier = modifier.fillMaxWidth()) { body() }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(if (compact) 14.dp else 18.dp)
        ) { body() }
    }
}

/**
 * 音乐偏好选择器弹窗。[required] = true 表示首启必须选完才能进推荐（不可划掉）。
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
internal fun TastePickerSheet(
    md3: Boolean,
    genres: List<String>,
    artists: List<String>,
    required: Boolean,
    onToggleGenre: (String) -> Unit,
    onToggleArtist: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onNeverRemind: () -> Unit = onDismiss
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            TastePickerBody(
                md3 = md3,
                genres = genres,
                artists = artists,
                onToggleGenre = onToggleGenre,
                onToggleArtist = onToggleArtist,
                compact = !md3
            )
            Spacer(Modifier.height(20.dp))
            FilledTonalButton(
                onClick = onConfirm,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = genres.isNotEmpty() || artists.isNotEmpty()
            ) {
                Text(if (required) "开始为我推荐" else "保存", fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (required) "不再提醒" else "取消",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNeverRemind() }
                    .padding(vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/** 毫秒 → mm:ss；未知时长显示 --:-- */
fun fmtDuration(ms: Long): String {
    if (ms <= 0L) return "--:--"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
