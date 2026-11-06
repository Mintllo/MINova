# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# =====================================================================
# 修复 release(R8 混淆) 包启动崩溃
# 崩溃现象: java.lang.RuntimeException: Unable to get provider
#   androidx.startup.InitializationProvider:
#   Failed to create an instance of androidx.work.impl.WorkDatabase
# 原因: 本项目传递依赖 androidx.room:room-runtime:2.2.5 与
#   androidx.work:work-runtime:2.7.0。Room 通过
#   Class.forName("<Database>_Impl") 反射实例化数据库实现类，
#   R8 full mode 会将这些类裁剪/改名，导致运行时反射失败。
# 注意: debug 包不受影响（不混淆），只有 release 包会崩。
# =====================================================================

# ---------- Room：保留数据库实现类及其无参构造 ----------
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# ---------- WorkManager ----------
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker { public <init>(...); }
-keep class * extends androidx.work.Worker { public <init>(...); }
-keep class * extends androidx.work.InputMerger { <init>(...); }

# ---------- androidx.startup：InitializationProvider 反射加载 Initializer ----------
-keep class androidx.startup.** { *; }

# ---------- Gson：字段名即 JSON key，数据类不能被改名 ----------
# SearchResponse / PlayUrlResponse / LyricApiResponse 等均由 Gson 反射反序列化，
# 字段一旦被混淆，API 响应会静默解析成 null。
-keep class com.HyperX.MI.Na.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ---------- 常见噪音抑制 ----------
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**

# ---------- 保留行号，便于定位线上崩溃 ----------
-keepattributes SourceFile,LineNumberTable