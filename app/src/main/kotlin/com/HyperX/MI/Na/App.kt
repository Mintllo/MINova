package com.HyperX.MI.Na

import android.app.Application
import android.content.Context

class App : Application() {
    companion object {
        lateinit var instance: App
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        // 首次运行时捕获系统显示密度并永久冻结（只存一次，后续不再更新）。
        // 之后用户再改系统「显示大小」也不影响应用；MainActivity.attachBaseContext 读取该值。
        val prefs = getSharedPreferences("app_display", Context.MODE_PRIVATE)
        if (!prefs.contains("density_dpi")) {
            val dpi = resources.configuration.densityDpi
            if (dpi > 0) prefs.edit().putInt("density_dpi", dpi).apply()
        }
    }
}
