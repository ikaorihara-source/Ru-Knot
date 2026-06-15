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

# ========================
# 数据模型 (Data Classes)
# ========================
# Retrofit 需要反射解析 JSON，Room 需要反射读写数据库
# 如果混淆了这些类，网络请求和数据库都会挂
-keep class com.ikaorihara.ruknot.alarm.** { *; }
-keep class com.ikaorihara.ruknot.data.** { *; }
-keep class com.ikaorihara.ruknot.network.** { *; }
-keep class com.ikaorihara.ruknot.streamer.** { *; }
-keep class com.ikaorihara.ruknot.utils.AppSettings { *; }
-keep class com.ikaorihara.ruknot.utils.BackupData { *; }
-keep class com.ikaorihara.ruknot.utils.UpdateInfo { *; }

# ========================
# 第三方库
# ========================
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*
-keepattributes Exceptions

# --- Gson ---
-keep class com.google.gson.stream.** { *; }

# --- OkHttp ---
-dontwarn okhttp3.**

# --- Retrofit ---
-dontwarn retrofit2.**

# --- Coil (图片加载) ---
-keep class coil.** { *; }

# --- Kotlin Coroutines (协程) ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- Jetpack Compose ---
# 官方通常建议保留这些，虽然 R8 现在很智能，但加上保险
-keep class androidx.compose.** { *; }