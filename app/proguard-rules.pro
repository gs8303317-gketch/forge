# Forge ProGuard rules
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
