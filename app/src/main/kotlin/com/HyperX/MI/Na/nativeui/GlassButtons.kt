// SPDX-License-Identifier: GPL-3.0-only
// GlassButtons ported from CurrentMusicX ui/component/nativeui/GlassButtons.kt
// (upstream TonalButton / LiquidGlassStyle, blob 7f33476a). Menu-trigger machinery
// is not carried over (本工程无 GlassMenu）；按钮本体 = ElasticPress 果冻物理 + 真液态
// 玻璃表面（AGSL 折射 + 按压拽动形变 + 高光随形变移动）。
package com.HyperX.MI.Na.nativeui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop

/** Circular glass control: press swells with a highlight, drag pulls the surface like jelly. */
@Composable fun GlassIconButton(onClick: () -> Unit, description: String, modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalGlassBackdrop.current, size: Dp = 44.dp, enabled: Boolean = true,
    surface: Boolean = true, content: @Composable () -> Unit) {
    val press = rememberElasticPressState()
    ElasticPress(modifier.size(size).semantics { contentDescription = description }
        .clickable(enabled = enabled, role = Role.Button,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null, onClick = onClick), enabled, press) {
        Box(Modifier.size(size)
            .then(if (surface) Modifier.glassSurface(backdrop,
                DeformableGlassShape(size / 2, { press.pull }, { press.amount }),
                glow = { press.amount }, clipToShape = false) else Modifier)
            .drawBehind { if (press.amount > 0f) drawCircle(Color.White.copy(alpha = .06f * press.amount.coerceIn(0f, 1f))) },
            contentAlignment = Alignment.Center) { content() }
    }
}

/** Shared filled/tonal action; the full pill stretches, while its hit target stays fixed. */
@Composable fun GlassActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    primary: Boolean = false, destructive: Boolean = false, enabled: Boolean = true,
    minHeight: Dp = 44.dp, backdrop: Backdrop? = LocalGlassBackdrop.current, fillWidth: Boolean = true,
    inkColor: Color = Color.Unspecified, fillColor: Color = Color.Unspecified) {
    val press = rememberElasticPressState()
    val fill = if (!fillColor.isSpecified) (if (destructive) Color(0xFFE5484D) else Color(0xFF4D8DFF)) else fillColor
    val ink = if (!inkColor.isSpecified) (if (destructive || primary) Color.White else Color(0xFF1C1D21)) else inkColor
    ElasticPress(modifier.clickable(enabled = enabled, role = Role.Button,
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null, onClick = onClick), enabled, press) {
        Box(Modifier.then(if (fillWidth) Modifier.fillMaxWidth() else Modifier).heightIn(min = minHeight)
            .then(if (primary || destructive) Modifier.glassSurface(backdrop,
                DeformableGlassShape(minHeight / 2, { press.pull }, { press.amount }),
                tint = fill, glow = { press.amount }, clipToShape = false)
            else Modifier.glassSurface(backdrop,
                DeformableGlassShape(minHeight / 2, { press.pull }, { press.amount }),
                glow = { press.amount }, clipToShape = false))
            .drawBehind { drawRect(ink.copy(alpha = .06f * press.amount.coerceIn(0f, 1f))) }
            .padding(horizontal = 16.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
            Text(label, color = ink.copy(alpha = if (enabled) 1f else .38f), fontSize = 15.sp,
                lineHeight = 20.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** One elastic capsule for related actions; children keep independent click targets. */
@Composable fun GlassButtonGroup(modifier: Modifier = Modifier, shape: Shape = CircleShape,
    backdrop: Backdrop? = LocalGlassBackdrop.current, content: @Composable RowScope.() -> Unit) {
    val press = rememberElasticPressState()
    ElasticPress(modifier, state = press) {
        androidx.compose.foundation.layout.Row(Modifier.glassSurface(backdrop,
            DeformableGlassShape(24.dp, { press.pull }, { press.amount }),
            glow = { press.amount }, clipToShape = false),
            verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

/** An action inside GlassButtonGroup: feedback and semantics without another glass outline. */
@Composable fun GlassGroupIconButton(onClick: () -> Unit, description: String, modifier: Modifier = Modifier,
    size: Dp = 44.dp, enabled: Boolean = true, ink: Color = Color(0xFF1C1D21), content: @Composable () -> Unit) {
    val press = rememberElasticPressState()
    ElasticPress(modifier.size(size).semantics { contentDescription = description }
        .clickable(enabled = enabled, role = Role.Button,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null, onClick = onClick), enabled, press) {
        Box(Modifier.size(size).drawBehind {
            drawCircle(ink.copy(alpha = .06f * press.amount.coerceIn(0f, 1f)))
        }, contentAlignment = Alignment.Center) { content() }
    }
}
