package com.HyperX.MI.Na

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.HyperX.MI.Na.liquid.MiuixBackdropAdapter
import com.HyperX.MI.Na.liquid.vibrancy
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.blur.Backdrop as MiuixBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

private data class RailTab(val label: String, val icon: ImageVector)

private val railTabs = listOf(
    RailTab("推荐", Icons.Rounded.Explore),
    RailTab("搜索", Icons.Rounded.Search),
    RailTab("设置", Icons.Rounded.Settings),
    RailTab("账号", Icons.Rounded.Person),
)

@Composable
fun MiuixTabletNavigationRail(
    backdrop: Backdrop,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isDark: Boolean,
    onWidthChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    MiuixTheme(
        controller = remember(isDark) {
            ThemeController(if (isDark) ColorSchemeMode.Dark else ColorSchemeMode.Light)
        }
    ) {
        val miuixBackdrop: MiuixBackdrop = remember(backdrop) { MiuixBackdropAdapter(backdrop) }
        val railState = rememberNavigationRailState(initialValue = NavigationRailValue.Collapsed)

                        val panelColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.30f)

        NavigationRail(
            state = railState,
            modifier = modifier
                .fillMaxHeight()
                .onGloballyPositioned { onWidthChange(it.size.width) }
                .drawBackdrop(
                    backdrop = miuixBackdrop,
                                        shape = { RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp) },
                    effects = {
                                                padding = maxOf(padding, 40.dp.toPx())
                        vibrancy()
                        blur(8.dp.toPx(), 8.dp.toPx())
                    },
                ),
            color = panelColor,
            expandContentDescription = "展开导航栏",
            collapseContentDescription = "收起导航栏",
        ) {
            railTabs.forEachIndexed { index, tab ->
                val selected = selectedTab == index
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                                                        .then(
                            if (selected) Modifier.drawBackdrop(
                                backdrop = miuixBackdrop,
                                shape = { RoundedCornerShape(16.dp) },
                                effects = {
                                    padding = maxOf(padding, 24.dp.toPx())
                                    vibrancy()
                                    blur(10.dp.toPx(), 10.dp.toPx())
                                },
                                                                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.22f)) },
                            ) else Modifier
                        )
                ) {
                    NavigationRailItem(
                                                                                                selected = false,
                        onClick = { onTabSelected(index) },
                        icon = tab.icon,
                        label = tab.label
                    )
                }
            }
        }
    }
}
