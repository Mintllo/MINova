package com.HyperX.MI.Na

import androidx.compose.runtime.staticCompositionLocalOf

// 全局：当前是否使用 MD3 经典样式（组合本地键，供跨文件读取）
val LocalMd3Style = staticCompositionLocalOf { false }
