# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# WebSocket
-keep class org.java_websocket.** { *; }
-dontwarn org.java_websocket.**

# Gson
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
