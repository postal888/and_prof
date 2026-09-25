# Profconq release (R8)

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# App models used with Room (KSP generates keep rules; belt-and-suspenders for entities)
-keep @androidx.room.Entity class * { *; }
-keep class com.profconq.app.data.local.** { *; }

# Firebase / Google Sign-In
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Studio / Video lock-screen media session
-keep class com.profconq.app.studio.StudioPlaybackService { *; }
-keep class com.profconq.app.youtube.YouTubePlaybackService { *; }
-keep class android.support.v4.media.** { *; }

# YouTube player (embedded WebView)
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }
-keepclassmembers class * extends android.webkit.WebViewClient { *; }
-keepclassmembers class * extends android.webkit.WebChromeClient { *; }

# LAME encoder (JNI)
-keep class net.sourceforge.lame.** { *; }

# Kotlin coroutines / metadata
-dontwarn kotlinx.coroutines.**
-keep class kotlin.Metadata { *; }

# Compose (libraries ship consumer rules; silence common noise)
-dontwarn androidx.compose.**
