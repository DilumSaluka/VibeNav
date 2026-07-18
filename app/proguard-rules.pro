-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-dontwarn org.osmdroid.**
-keep class org.osmdroid.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
