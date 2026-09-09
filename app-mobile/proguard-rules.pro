# Vantafyn Mobile R8 / ProGuard Configuration

# Preserve line numbers and source file attributes for debugging and crash diagnostics
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ----------------------------------------------------------------------------------
# Kotlinx Serialization & Data Models
# ----------------------------------------------------------------------------------
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class org.jellyfin.sdk.model.** { *; }
-keep class org.jellyfin.sdk.api.** { *; }
-keep class org.jellyfin.sdk.core.** { *; }

-keep class dev.vantafyn.core.jellyfin.** { *; }
-keep class dev.vantafyn.core.media.** { *; }
-keep class dev.vantafyn.core.cast.** { *; }
-keep class dev.vantafyn.core.subsonic.** { *; }
-keep class dev.vantafyn.core.downloads.** { *; }
-keep class dev.vantafyn.core.ombi.** { *; }
-keep class dev.vantafyn.core.integrations.** { *; }

# ----------------------------------------------------------------------------------
# Google Play Services Cast Framework
# ----------------------------------------------------------------------------------
# VantafynCastOptionsProvider is instantiated via reflection by Play Services
-keep public class dev.vantafyn.core.cast.VantafynCastOptionsProvider {
    public <init>();
    public *;
}
-keep class com.google.android.gms.cast.** { *; }
-keep class androidx.mediarouter.app.** { *; }

# ----------------------------------------------------------------------------------
# Media3 / ExoPlayer
# ----------------------------------------------------------------------------------
-keep class androidx.media3.** { *; }
-keep class androidx.media3.ui.SubtitleView {
    private androidx.media3.ui.SubtitleView$Output output;
}
-keep class androidx.media3.ui.CanvasSubtitleOutput {
    private final java.util.List painters;
}
-keep class androidx.media3.ui.SubtitlePainter {
    private final float outlineWidth;
}

# ----------------------------------------------------------------------------------
# Android Glance & AppWidgets
# ----------------------------------------------------------------------------------
-keep class androidx.glance.** { *; }
-keep class dev.vantafyn.mobile.**Widget* { *; }
-keep class dev.vantafyn.mobile.**Receiver* { *; }

# ----------------------------------------------------------------------------------
# Reflection on R class (for dynamically resolved bundled avatars pp*)
# ----------------------------------------------------------------------------------
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ----------------------------------------------------------------------------------
# Standard Enums
# ----------------------------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ----------------------------------------------------------------------------------
# Warning Suppressions for Optional / Platform-specific classes
# ----------------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.protobuf.**
