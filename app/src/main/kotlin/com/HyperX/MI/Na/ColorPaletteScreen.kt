package com.HyperX.MI.Na

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

@Composable
fun ColorPaletteScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToNavSettings: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val currentColorMode = ColorMode.fromValue(viewModel.colorMode)
    val currentKeyColor = viewModel.keyColor
    val colorStyle = try {
        PaletteStyle.valueOf(viewModel.colorStyle)
    } catch (_: Exception) {
        PaletteStyle.TonalSpot
    }
        val currentColorSpec = try {
        ColorSpec.SpecVersion.valueOf(viewModel.colorSpec)
    } catch (_: Exception) {
        ColorSpec.SpecVersion.SPEC_2025
    }
    val isDark = currentColorMode.isDark || (currentColorMode.isSystem && isSystemInDarkTheme())
    val isAmoled = currentColorMode.isAmoled

    Column(
        modifier = Modifier
            .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
                Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "主题与颜色",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

                ThemePreviewCard(
            keyColor = currentKeyColor,
            isDark = isDark,
            isAmoled = isAmoled,
            paletteStyle = colorStyle,
            colorSpec = currentColorSpec,
            isLiquid = viewModel.useFloatingNavBar,
        )

                LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ColorButtonMaterial(
                    color = Color.Unspecified,
                    isSelected = currentKeyColor == 0,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    paletteStyle = colorStyle,
                    colorSpec = currentColorSpec,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        viewModel.keyColor = 0
                        viewModel.savePreferences()
                    }
                )
            }
            items(keyColorOptions) { color ->
                ColorButtonMaterial(
                    color = Color(color),
                    isSelected = currentKeyColor == color,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    paletteStyle = colorStyle,
                    colorSpec = currentColorSpec,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        viewModel.keyColor = color
                        viewModel.savePreferences()
                    }
                )
            }
        }
        KsuSegColumn(title = "调色板") {
            val styles = PaletteStyle.entries
            KsuExpressiveDropdown(
                index = 0, count = 2,
                icon = Icons.Rounded.Style,
                title = "调色板风格",
                summary = "主题色生成算法（Material 色彩规范）",
                items = styles.map { it.name },
                selectedIndex = styles.indexOf(colorStyle).coerceAtLeast(0),
                valueText = colorStyle.name,
                onSelect = { i: Int ->
                    viewModel.colorStyle = styles[i].name
                    viewModel.savePreferences()
                }
            )

            val specs = ColorSpec.SpecVersion.entries
            KsuExpressiveDropdown(
                index = 1, count = 2,
                icon = Icons.Rounded.DesignServices,
                title = "色彩规范",
                summary = "Material 色彩规范的版本",
                items = specs.map { it.name },
                selectedIndex = specs.indexOf(currentColorSpec).coerceAtLeast(0),
                valueText = currentColorSpec.name,
                onSelect = { i: Int ->
                    viewModel.colorSpec = specs[i].name
                    viewModel.savePreferences()
                }
            )
        }

                KsuSegColumn(title = "明暗模式") {
            val options = listOf(
                listOf(ColorMode.SYSTEM, ColorMode.MONET_SYSTEM) to "跟随系统",
                listOf(ColorMode.LIGHT, ColorMode.MONET_LIGHT) to "浅色",
                listOf(ColorMode.DARK, ColorMode.MONET_DARK) to "深色",
                listOf(ColorMode.DARK_AMOLED) to "纯黑",
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.chunked(4).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        rowOptions.forEachIndexed { index, (modes, label) ->
                            ToggleButton(
                                checked = currentColorMode in modes,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        viewModel.colorMode = modes.first().value
                                        viewModel.savePreferences()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                colors = expressiveToggleButtonColors(),
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    rowOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                            ) {
                                Icon(
                                    imageVector = when (modes.first()) {
                                        ColorMode.SYSTEM -> Icons.Filled.Brightness4
                                        ColorMode.LIGHT -> Icons.Filled.Brightness7
                                        ColorMode.DARK -> Icons.Filled.Brightness3
                                        ColorMode.DARK_AMOLED -> Icons.Filled.Brightness1
                                        else -> Icons.Filled.Brightness4
                                    },
                                    contentDescription = label
                                )
                            }
                        }
                    }
                }
            }
        }

                KsuSegColumn {
            KsuExpressiveDropdown(
                index = 0, count = 1,
                icon = Icons.Rounded.Palette,
                title = "应用风格",
                summary = "全局组件风格，导航栏可独立设置",
                items = listOf("液态玻璃", "Material Design 3"),
                selectedIndex = if (viewModel.md3Style) 1 else 0,
                valueText = if (viewModel.md3Style) "Material Design 3" else "液态玻璃",
                onSelect = { i: Int ->
                    viewModel.md3Style = i == 1
                    viewModel.savePreferences()
                }
            )
        }

                        KsuSegColumn(title = "导航栏") {
            KsuSegSwitch(
                index = 0, count = 4,
                icon = Icons.Rounded.Layers,
                title = "液态玻璃导航栏",
                summary = "悬浮玻璃底栏",
                checked = viewModel.useFloatingNavBar,
                onCheckedChange = {
                    viewModel.useFloatingNavBar = it
                    viewModel.savePreferences()
                }
            )
            KsuSegSwitch(
                index = 1, count = 4,
                icon = Icons.Rounded.BlurOn,
                title = "玻璃模糊效果",
                summary = "半透明毛玻璃与边缘折射",
                checked = viewModel.navBarLiquidGlass,
                onCheckedChange = {
                    viewModel.navBarLiquidGlass = it
                    viewModel.savePreferences()
                }
            )
            KsuSegSwitch(
                index = 2, count = 4,
                icon = Icons.Rounded.Label,
                title = "显示文字标签",
                summary = "底栏图标下方显示文字",
                checked = viewModel.navBarShowLabels,
                onCheckedChange = {
                    viewModel.navBarShowLabels = it
                    viewModel.savePreferences()
                }
            )
                KsuExpressiveDropdown(
                    index = 3, count = 4,
                    icon = Icons.Rounded.WaterDrop,
                    title = "透明度",
                    summary = "磨砂 / 半透明 / 纯透明",
                    items = listOf("磨砂", "半透明", "纯透明"),
                    selectedIndex = viewModel.navBarTransparency.coerceIn(0, 2),
                    valueText = listOf("磨砂", "半透明", "纯透明")[viewModel.navBarTransparency.coerceIn(0, 2)],
                    onSelect = { i: Int ->
                        viewModel.navBarTransparency = i
                        viewModel.savePreferences()
                    }
                )
        }

    }
}

@Composable
internal fun KsuSegTitle(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
    )
}

@Composable
internal fun KsuSegColumn(title: String = "", content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (title.isNotEmpty()) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            content()
        }
    }
}

internal fun ksuSegShape(index: Int, count: Int): RoundedCornerShape {
    val outer = 16.dp
    val inner = 4.dp
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = bottom, bottomStart = bottom)
}

@Composable
internal fun KsuSegRow(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    summary: String?,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ksuSegShape(index, count))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = ksuSegShape(index, count)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (summary != null) {
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
internal fun KsuSegSwitch(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    summary: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
        SegmentedSwitchItem(
        index = index,
        count = count,
        icon = icon,
        title = title,
        summary = summary,
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

@Composable
internal fun KsuSegClick(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    summary: String?,
    onClick: () -> Unit,
) {
        SegmentedNavItem(
        index = index,
        count = count,
        icon = icon,
        title = title,
        summary = summary,
        onClick = onClick,
        trailing = { Icon(Icons.Rounded.ChevronRight, null) }
    )
}

@Composable
internal fun KsuExpressiveDropdown(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    summary: String?,
    items: List<String>,
    selectedIndex: Int,
    valueText: String,
    onSelect: (Int) -> Unit,
) {
    SegmentedDropdownItem(
        index = index,
        count = count,
        icon = icon,
        title = title,
        summary = summary,
        items = items,
        selectedIndex = selectedIndex,
        onItemSelected = onSelect
    )
}

@Composable
private fun KsuSectionTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 4.dp)
    )
}

@Composable
private fun KsuSwitchRow(
    icon: @Composable () -> Unit,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(summary, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun KsuNavRow(
    icon: @Composable () -> Unit,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(summary, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun expressiveToggleButtonColors(): ToggleButtonColors = ToggleButtonDefaults.colors(
    checkedContainerColor = MaterialTheme.colorScheme.primary,
    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor = MaterialTheme.colorScheme.onSurface,
)

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCard(
    keyColor: Int,
    isDark: Boolean,
    isAmoled: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    isLiquid: Boolean = true,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight

    val colorScheme = rememberKernelSUColorScheme(
        seedColor = if (keyColor == 0) Color.Unspecified else Color(keyColor),
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio),
            color = colorScheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color = colorScheme.outlineVariant)
        ) {
            val content: @Composable ColumnScope.() -> Unit = {
                                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "MINova",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }

                                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val showCards = maxHeight >= 100.dp
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                                                Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorScheme.secondaryContainer)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colorScheme.primary)
                                )
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Box(
                                        Modifier
                                            .size(width = 72.dp, height = 6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(colorScheme.onSecondaryContainer)
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Box(
                                        Modifier
                                            .size(width = 48.dp, height = 4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(colorScheme.onSecondaryContainer.copy(alpha = 0.5f))
                                    )
                                }
                            }
                            if (showCards) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                                )
                            }
                        }
                                                if (showCards) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.surfaceBright)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colorScheme.tertiaryContainer)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Box(
                                        Modifier
                                            .size(width = 64.dp, height = 6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(colorScheme.primaryContainer)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        Modifier
                                            .size(width = 40.dp, height = 4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column {
                content()

                if (isLiquid) {
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(colorScheme.surfaceContainer.copy(alpha = 0.65f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.primaryContainer)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.MusicNote, null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(12.dp))
                            }
                            Icon(Icons.Rounded.Settings, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                            Icon(Icons.Rounded.Forum, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                            Icon(Icons.Rounded.Person, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                        }
                    }
                } else {
                                        Surface(
                        color = colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .height(44.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(colorScheme.primaryContainer).padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Rounded.MusicNote, null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                            }
                            Icon(Icons.Rounded.Settings, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Icon(Icons.Rounded.Forum, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Icon(Icons.Rounded.Person, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorButtonMaterial(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    isAmoled: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = rememberKernelSUColorScheme(
        seedColor = color,
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(
                    color = colorScheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                drawArc(
                    color = colorScheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true
                )
            }

            val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1.0f)
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(16.dp)
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}

val paletteStyleDesc: Map<String, String> = mapOf(
    "TonalSpot" to "柔和色调（默认，最耐看）",
    "Neutral" to "中性灰（低饱和）",
    "Vibrant" to "鲜艳（高饱和）",
    "Expressive" to "表现力（活泼跳色）",
    "Fidelity" to "高保真（贴近源色）",
    "Content" to "内容色（跟随图片）",
    "Monochrome" to "单色（仅黑白灰）",
    "Rainbow" to "彩虹（多彩）",
    "FruitSalad" to "果缤纷（清新多彩）",
    "Flowers" to "花卉（柔美粉调）",
    "Assets" to "素材色",
)
