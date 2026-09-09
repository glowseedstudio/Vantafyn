# Vantafyn TV R8 / ProGuard Configuration

-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlinx Serialization & Data Models
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
-keep class dev.vantafyn.core.subsonic.** { *; }

# Media3 / ExoPlayer
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

# Standard Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Warning Suppressions
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.protobuf.**
