package com.HyperX.MI.Na

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.drawBackdrop

private val SegmentedOuterRadius = 16.dp
private val SegmentedInnerRadius = 4.dp
private val SegmentedGap = 2.dp

private const val SegmentedSpringDamping = 0.9f
private const val SegmentedSpringStiffness = 800f

private val LocalSegmentedRowGap = compositionLocalOf<Dp> { 0.dp }

private fun segmentedTopRadius(index: Int): Dp =
    if (index == 0) SegmentedOuterRadius else SegmentedInnerRadius

private fun segmentedBottomRadius(index: Int, count: Int): Dp =
    if (index == count - 1) SegmentedOuterRadius else SegmentedInnerRadius

internal fun segmentedShape(index: Int, count: Int): RoundedCornerShape {
    val top = segmentedTopRadius(index)
    val bottom = segmentedBottomRadius(index, count)
    return RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = bottom, bottomStart = bottom)
}

@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    title: String = "",
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
                        CompositionLocalProvider(LocalSegmentedRowGap provides SegmentedGap) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SegmentedRow(
    index: Int,
    count: Int,
    icon: ImageVector?,
    title: String,
    summary: String?,
    onClick: (() -> Unit)?,
    enabled: Boolean = true,
    trailing: @Composable () -> Unit,
) {
    // 液态模式（LocalMd3Backdrop 非空）：卡片本体真磨砂（backdrop blur + 半透明着色）
    val glass = LocalMd3Backdrop.current
    val container = if (glass == null) MaterialTheme.colorScheme.surfaceBright else Color.Transparent
    val colors = ListItemDefaults.segmentedColors(
        containerColor = container,
        disabledContainerColor = container,
        supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    val glassShape = RoundedCornerShape(
        topStart = if (index == 0) 16.dp else 4.dp,
        topEnd = if (index == 0) 16.dp else 4.dp,
        bottomEnd = if (index == count - 1) 16.dp else 4.dp,
        bottomStart = if (index == count - 1) 16.dp else 4.dp
    )
    val glassFrost = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.22f)
    val glassModifier = if (glass != null) Modifier.drawBackdrop(
        backdrop = glass,
        shape = { glassShape },
        effects = { blur(10f.dp.toPx()) },
        highlight = { com.kyant.backdrop.highlight.Highlight.Plain.copy(alpha = 0f) },
        onDrawSurface = { drawRect(glassFrost) }
    ) else Modifier
    val noop: () -> Unit = {}
    val clickAction: () -> Unit = if (enabled) (onClick ?: noop) else noop
    androidx.compose.material3.SegmentedListItem(
        onClick = clickAction,
        modifier = glassModifier,
        colors = colors,
        shapes = shapes,
        leadingContent = icon?.let { { Icon(it, null, modifier = Modifier.size(24.dp)) } },
        supportingContent = summary?.let { { Text(it) } },
        trailingContent = trailing,
        content = { Text(title) }
    )
}

@Composable
fun SegmentedSwitchItem(
    index: Int,
    count: Int,
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    SegmentedRow(
        index = index, count = count, icon = icon, title = title, summary = summary,
        enabled = enabled,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onCheckedChange(!checked)
        }
    ) {
                LiquidSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SegmentedNavItem(
    index: Int,
    count: Int,
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    SegmentedRow(
        index = index, count = count, icon = icon, title = title, summary = summary,
        onClick = onClick
    ) {
        trailing?.invoke()
    }
}

@Composable
fun SegmentedDropdownItem(
    index: Int,
    count: Int,
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    items: List<String>,
    enabled: Boolean = true,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    var anchorOffset by remember { mutableStateOf(IntOffset.Zero) }
    val hasItems = items.isNotEmpty()
    val safeIndex = if (hasItems) selectedIndex.coerceIn(0, items.lastIndex) else -1

    Box(modifier = Modifier.trackPressPosition { anchorOffset = it.round() }) {
        SegmentedRow(
            index = index, count = count, icon = icon, title = title, summary = summary,
            onClick = if (enabled) {
                {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    expanded = true
                }
            } else null
        ) {
            Text(
                text = if (hasItems && safeIndex >= 0) items[safeIndex] else "",
                textAlign = TextAlign.End,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OffsetAnchoredExpressiveMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            anchorOffset = anchorOffset,
        ) {
            items.forEachIndexed { i, text ->
                SelectableDropdownMenuItem(
                    text = { Text(text) },
                    selected = i == safeIndex,
                    onClick = {
                        if (i in items.indices) {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            onItemSelected(i)
                        }
                        expanded = false
                    },
                    shapes = MenuDefaults.itemShape(index = i, count = items.size),
                    selectedLeadingIcon = {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                        )
                    },
                )
            }
        }
    }
}


/** 液态风格开关：半透明胶囊轨道 + 白色玻璃旋钮，spring 弹性位移，无边框无高光。 */
@Composable
private fun LiquidSwitch(checked: Boolean, enabled: Boolean = true, onCheckedChange: ((Boolean) -> Unit)? = null) {
    val glass = LocalMd3Backdrop.current
    if (!LocalMd3Style.current && glass != null && onCheckedChange != null) {
        // 液态模式：悬浮歌词设置同款液态开关（可拖拽玻璃旋钮 + 彩色轨道）
        LiquidToggleSwitch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            backdrop = glass
        )
        return
    }
    // MD3 模式：原版 Material3 Switch
    Switch(
        checked = checked,
        enabled = enabled,
        onCheckedChange = { onCheckedChange?.invoke(it) },
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize)
            )
        },
        colors = SwitchDefaults.colors(
            checkedIconColor = MaterialTheme.colorScheme.primary,
            uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    )
}