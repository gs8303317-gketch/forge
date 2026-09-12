# Forge ProGuard rules
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.gketch.forge.playback.** { *; }
-keep class com.gketch.forge.MainActivity { *; }
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
-dontwarn androidx.datastore.**
-keep class com.gketch.forge.cast.** { *; }
-keep class com.gketch.forge.widget.** { *; }
-keep class com.google.android.gms.cast.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn androidx.mediarouter.**

-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**
-keep class androidx.appcompat.** { *; }
-keep class androidx.fragment.app.** { *; }
