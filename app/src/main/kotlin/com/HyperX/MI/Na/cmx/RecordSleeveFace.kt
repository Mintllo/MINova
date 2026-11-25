// 共享「3D 歌碟套面」：与 RecordShelfView.drawSleeve 同一套视觉 token ——
// 米黄套 0xFFF6EFD8 / 深色插腔 0xFF15171B / 暖黄边框 0xFFF0D970 / 烫金文字 0xFFB8A45E，
// 封面 contain 不裁边，书脊竖排歌名歌手 + 左下专辑铭印。
// PlaylistShelfModes 三模式的 ShelfSceneCard 用它替换原抽象卡面：
// 堆叠/书架/环绕三种模式渲染同一套完整歌碟（套 + 白黄边框 + 封面 + 黑胶）。
package com.HyperX.MI.Na.cmx

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.HyperX.MI.Na.PlaylistSong as StorePlaylistSong

/**
 * 完整歌碟套面（GRID/RING 与 STACK 同款外观）。
 * @param coverUrl 封面地址（已带防盗链代理）
 * @param nameSpine 书脊文字（歌名 - 歌手）
 * @param albumImprint 左下专辑铭印
 * @param faceColor 套面底色（默认暖纸白；调用方可传「封面主色 lerp 暖纸白 .7」随封面微变）
 * @param frameColor 边框色（默认暖黄；调用方可传「封面主色 lerp 暖黄 .6」随封面微变）
 */
@Composable
internal fun RecordSleeveFace(
    coverUrl: String,
    nameSpine: String,
    albumImprint: String,
    modifier: Modifier = Modifier,
    faceColor: Color = Color(0xFFF6EFD8),
    frameColor: Color = Color(0xFFF0D970),
) {
    BoxWithConstraints(modifier) {
        val w = maxWidth
        val h = maxHeight
        // 套壳：米黄底 + 顶部一条浅金高光（原三段面简化，无 shader）；高光 = 套面与边框色混合
        val faceEdge = lerp(faceColor, frameColor, .45f)
        Box(Modifier.matchParentSize().background(faceColor))
        Box(
            Modifier
                .matchParentSize()
                .padding(bottom = h * .945f)
                .background(
                    Brush.verticalGradient(
                        listOf(faceEdge, faceColor),
                        startY = 0f, endY = Float.POSITIVE_INFINITY,
                    )
                ),
        )
        // 深色插腔（原 .041,.032 - .977,.975）：封面 contain 放进插腔，不裁边
        Box(
            Modifier
                .offset(x = w * .041f, y = h * .032f)
                .size(w * .936f, h * .943f)
                .background(Color(0xFF15171B)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = coverRequestUrl(coverUrl, 384),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // 边框：单色不透明一整块平滑（默认暖黄，可随封面主色微变），贴插腔（封面 + 统一小边距）
        Box(
            Modifier
                .offset(x = w * .041f, y = h * .032f)
                .size(w * .936f, h * .943f)
                .border(1.5.dp, frameColor),
        )
        // 书脊：左侧竖排（旋转 -90°）歌名/歌手，烫金色
        Text(
            nameSpine,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFB8A45E),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .graphicsLayer {
                    rotationZ = -90f
                    transformOrigin = TransformOrigin(0f, 1f)
                }
                .padding(start = h * .155f, bottom = w * .065f),
        )
        // 左下专辑铭印
        Text(
            albumImprint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 7.sp,
            color = Color(0xFFB8A45E),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = w * .13f, bottom = h * .012f),
        )
    }
}

/** 播放中的黑胶盘（画在套面之下，右缘探出月牙）：盘面 + 唱纹 + 标签 + 中孔。 */
@Composable
internal fun RecordVinylDisc(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF34363A), Color(0xFF121315), Color(0xFF292B2F)),
                center = center, radius = r,
            ),
            radius = r, center = center,
        )
        for (ring in 0..11) {
            drawCircle(
                Color.White.copy(alpha = if (ring % 3 == 0) .18f else .07f),
                radius = r * (.44f + ring * .042f),
                center = center,
                style = Stroke(width = r * .014f),
            )
        }
        drawCircle(accent, radius = r * .26f, center = center)
        drawCircle(Color(0xFF101113), radius = r * .055f, center = center)
    }
}

/** PlaylistSong（歌单存储）→ 套面文案：书脊（歌名 - 歌手）与专辑铭印。 */
internal fun StorePlaylistSong.sleeveTexts(): Pair<String, String> =
    listOf(name, artist).filter(String::isNotBlank).joinToString(" - ") to album

/** No.xx 铭牌（RecordShelfView 架面铭牌风格，三模式共用）。 */
@Composable
internal fun SleeveNoPlate(index: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        // 去白块：0.92 实色白 → 半透明主题底，贴合唱片架玻璃语言
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
    ) {
        Text(
            "No.%02d".format(index + 1),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 0.5.sp,
        )
    }
}
