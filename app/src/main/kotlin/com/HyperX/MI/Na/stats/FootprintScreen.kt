package com.HyperX.MI.Na.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.HyperX.MI.Na.motionEnter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 听歌足迹页（改版：先把「今天听了什么」说清楚，再看明细）。
 *
 * 呈现结构：
 *  - 今日概览卡：大字「今日听了 N 分钟，N 首」，副文本给出今日最长连续收听时段
 *  - 近 7 日趋势：Canvas 迷你柱状图，每天一根，高度按分钟数，今天高亮
 *  - 最爱歌手 Top3：按 perSong 聚合歌手，头像取该歌手听得最久一首的封面，展示播放次数
 *  - 时段分布：早/午/晚/夜 四段占比条
 *  - 其后保留：概览数据、最近在听封面墙、歌曲榜 Top30、足迹流水与口径说明
 *
 * 数据全部来自 ListenStatsStore（本地 SharedPreferences），不改动数据层，图表不引入任何新依赖。
 */
@Composable
fun FootprintScreen(onBack: () -> Unit) {
    val snap by ListenStatsStore.snapshot.collectAsState()
    val dayFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dayKeyFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayKey = remember { dayKeyFmt.format(Date()) }

    // 今日概览：今天听的总时长、听过的歌曲数、最长一段连续收听
    val todayMs = snap.perDay[todayKey] ?: 0L
    val todayEntries = remember(snap.footprint, todayKey) {
        snap.footprint.filter { dayKeyFmt.format(Date(it.ts)) == todayKey }
    }
    val todaySongs = todayEntries.distinctBy { it.key }.size
    val longestSessionMs = todayEntries.maxOfOrNull { it.ms } ?: 0L

    // 近 7 日趋势：从最旧到今天共 7 天，缺数据的天补 0（perDay 本身新日期在前）
    val last7Days = remember(snap.perDay, todayKey) {
        val cal = Calendar.getInstance()
        (6 downTo 0).map { back ->
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -back)
            val key = dayKeyFmt.format(cal.time)
            key to (snap.perDay[key] ?: 0L)
        }
    }

    // 最爱歌手 Top3：从 perSong 聚合，次数 = 播放次数之和，头像 = 该歌手听得最久一首的封面
    val topArtists3 = remember(snap.songs) {
        snap.songs.groupBy { it.artist.ifBlank { "未知歌手" } }
            .map { (name, list) -> Triple(name, list.sumOf { it.playCount }, list.first().cover) }
            .sortedByDescending { it.second }
            .take(3)
    }

    // 时段分布：把足迹流水按小时聚合，再归入 早/午/晚/夜 四段
    val hourMs = remember(snap.footprint) {
        LongArray(24).also { arr ->
            val cal = Calendar.getInstance()
            snap.footprint.forEach { fe ->
                if (fe.ms > 0) {
                    cal.timeInMillis = fe.ts
                    arr[cal.get(Calendar.HOUR_OF_DAY)] += fe.ms
                }
            }
        }
    }
    val dayparts = remember(hourMs) {
        listOf(
            "早" to (6..11).sumOf { hourMs[it] },
            "午" to (12..17).sumOf { hourMs[it] },
            "晚" to (18..23).sumOf { hourMs[it] },
            "夜" to (0..5).sumOf { hourMs[it] }
        )
    }
    val playCountTotal = remember(snap.songs) { snap.songs.sumOf { it.playCount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "返回",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("听歌足迹", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // —— 今日概览卡：大字一眼懂 ——
            item {
                OverviewCard {
                    Text("今日概览", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "今日听了 " + (todayMs / 60000) + " 分钟，" + todaySongs + " 首",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "最长连续收听 " + (longestSessionMs / 60000) + " 分钟",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // —— 近 7 日趋势：迷你柱状图，今天高亮 ——
            item {
                OverviewCard {
                    Text("近 7 日趋势", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    SevenDayTrendChart(last7Days)
                }
            }

            // —— 最爱歌手 Top3：头像 + 次数 ——
            if (topArtists3.isNotEmpty()) {
                item {
                    OverviewCard {
                        Text("最爱歌手 Top3", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(8.dp))
                        topArtists3.forEachIndexed { i, (name, count, cover) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    (i + 1).toString(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(20.dp)
                                )
                                AsyncImage(
                                    model = cover,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        name,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        count.toString() + " 次",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // —— 时段分布：早/午/晚/夜 四段占比条 ——
            item {
                OverviewCard {
                    Text("时段分布", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(12.dp))
                    DaypartBars(dayparts)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val total = dayparts.sumOf { it.second }
                        dayparts.forEachIndexed { i, (label, ms) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(daypartColor(i))
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    label + " " + (if (total > 0) (ms * 100 / total).toInt() else 0) + "%",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // —— 概览：听歌天数 / 歌曲数 / 播放次数 ——
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatChip("听歌天数", snap.perDay.size.toString() + " 天", Modifier.weight(1f))
                    StatChip("歌曲", snap.songs.size.toString() + " 首", Modifier.weight(1f))
                    StatChip("播放", playCountTotal.toString() + " 次", Modifier.weight(1f))
                }
            }

            // —— 最近在听封面墙 ——
            val wallCovers = snap.footprint.filter { it.cover.isNotBlank() }.distinctBy { it.key }.take(10)
            if (wallCovers.isNotEmpty()) {
                item { SectionTitle("最近在听") }
                item { CoverWall(wallCovers) }
            }

            // —— 歌曲榜 ——
            item { SectionTitle("歌曲榜") }
            items(snap.songs.take(30), key = { it.key }) { st ->
                SongRow(st.title, st.artist, st.cover, formatMs(st.totalMs), st.playCount.toString() + " 次")
            }
            if (snap.songs.isEmpty()) {
                item { EmptyHint("还没有听歌记录，播放一首歌试试") }
            }

            // —— 足迹流水 ——
            item { SectionTitle("足迹") }
            items(snap.footprint, key = { it.ts.toString() + it.key }) { fe ->
                SongRow(fe.title, fe.artist, fe.cover, dayFmt.format(Date(fe.ts)), formatMs(fe.ms))
            }

            item {
                Text(
                    "仅统计本机的实际播放时长，数据保存在设备本地",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

/** MD3 卡片容器：surfaceContainerLowest 背景，16dp 圆角 */
@Composable
private fun OverviewCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
    }
}

/** 近 7 日趋势：Canvas 自绘迷你柱状图，每天一根，高度按分钟数，最后一天（今天）高亮 */
@Composable
private fun SevenDayTrendChart(days: List<Pair<String, Long>>) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(top = 12.dp)
        ) {
            val n = days.size
            if (n > 0) {
                val gap = 6.dp.toPx()
                val barW = (size.width - gap * (n - 1)) / n
                val maxMs = days.maxOf { it.second }.coerceAtLeast(1L)
                days.forEachIndexed { i, (_, ms) ->
                    val h = if (ms <= 0L) 3.dp.toPx()
                    else (ms.toFloat() / maxMs * (size.height - 4.dp.toPx())).coerceAtLeast(5.dp.toPx())
                    drawRoundRect(
                        color = if (i == n - 1) primary else primary.copy(alpha = 0.28f),
                        topLeft = Offset(i * (barW + gap), size.height - h),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 3f, barW / 3f)
                    )
                }
            }
        }
        if (days.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(days.first().first.takeLast(5).replace('-', '/'), fontSize = 10.sp, color = onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("今天", fontSize = 10.sp, color = onSurfaceVariant)
            }
        }
    }
}

/** 时段分布四段占比条：同一 primary 的深浅区分 早/午/晚/夜 */
@Composable
private fun DaypartBars(parts: List<Pair<String, Long>>) {
    val total = parts.sumOf { it.second }.coerceAtLeast(1L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
    ) {
        parts.forEachIndexed { i, (_, ms) ->
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (total > 0) (ms.toFloat() / total).coerceIn(0f, 1f) else 0f)
                    .background(daypartColor(i))
            )
        }
    }
}

// 时段四段用同一色相的深浅阶梯，避免引入额外颜色 token
@Composable
private fun daypartColor(index: Int) = when (index) {
    0 -> MaterialTheme.colorScheme.primary
    1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.64f)
    2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
}

/** 最近在听封面墙：足迹流水去重后的封面横向叠放 */
@Composable
private fun CoverWall(coverEntries: List<ListenStatsStore.FootprintEntry>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(56.dp)
    ) {
        coverEntries.forEachIndexed { i, fe ->
            AsyncImage(
                model = fe.cover,
                contentDescription = null,
                modifier = Modifier
                    .offset(x = (i * 30).dp)
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun SongRow(title: String, subtitle: String, cover: String?, right: String, right2: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .motionEnter()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (cover != null) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(right, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            if (right2 != null) {
                Text(right2, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return when {
        h > 0 -> h.toString() + " 小时 " + m + " 分"
        m > 0 -> m.toString() + " 分 " + s + " 秒"
        else -> s.toString() + " 秒"
    }
}
