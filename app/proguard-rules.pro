# Forge ProGuard rules
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.gketch.forge.playback.** { *; }
-keep class com.gketch.forge.MainActivity { *; }
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
-dontwarn androidx.datastore.**
