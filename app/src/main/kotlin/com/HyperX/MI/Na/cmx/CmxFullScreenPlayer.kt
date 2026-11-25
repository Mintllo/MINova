package com.HyperX.MI.Na.cmx

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop

/**
 * 全屏播放器（控制台全屏态）。MainActivity 只传状态与回调，本组件不做任何播放决策。
 * 背景封面/毛玻璃由调用方渲染，这里只画前景内容。
 */
@Composable
fun CmxFullScreenPlayer(
    currentSong: com.HyperX.MI.Na.Song,
    isPlaying: Boolean,
    isLooping: Boolean,
    currentPosition: Long,
    duration: Long,
    lyrics: com.HyperX.MI.Na.LyricData,
    currentLyricIndex: Int,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleLoop: () -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
    backdrop: LayerBackdrop? = null,
    backgroundImageUri: String? = null,
    onAddToFavorites: () -> Unit = {},
    onAddToPlaylist: ((com.HyperX.MI.Na.Song) -> Unit)? = null,
    onDownload: () -> Unit = {},
    onShare: () -> Unit = {},
) {
    val safeDuration = if (duration > 0) duration else 1L
    val progress = (currentPosition.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val currentLine = lyrics.times.getOrNull(currentLyricIndex)?.let { lyrics.lyrics[it] }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 顶栏：关闭
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }

            Spacer(Modifier.height(6.dp))

            // 专辑封面
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (currentSong.picUrl.isNotBlank()) {
                    AsyncImage(
                        model = currentSong.picUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.08f)))
                }
            }

            // 歌曲信息
            Text(
                text = currentSong.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currentSong.artist,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 当前歌词行
            Text(
                text = currentLine ?: "♪",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // 进度
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress,
                    onValueChange = { onSeek((it * safeDuration).toLong()) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(currentPosition), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    Text(formatTime(duration), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }

            // 播放控制
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "上一首", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                IconButton(onClick = onPlayPause, modifier = Modifier.size(76.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "下一首", tint = Color.White, modifier = Modifier.size(34.dp))
                }
            }

            // 循环 + 收藏/歌单/下载/分享
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleLoop, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = if (isLooping) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = if (isLooping) "单曲循环" else "列表循环",
                        tint = if (isLooping) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onAddToFavorites, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.FavoriteBorder, contentDescription = "收藏", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
                }
                if (onAddToPlaylist != null) {
                    IconButton(onClick = { onAddToPlaylist(currentSong) }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Rounded.PlaylistAdd, contentDescription = "加入歌单", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
                    }
                }
                IconButton(onClick = onDownload, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.Download, contentDescription = "下载", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onShare, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.Share, contentDescription = "分享", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
