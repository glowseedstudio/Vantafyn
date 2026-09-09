# Vantafyn Mobile R8 / ProGuard Configuration

# Preserve line numbers and source file attributes for debugging and crash diagnostics
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ----------------------------------------------------------------------------------
# Kotlin & Kotlinx Serialization
# ----------------------------------------------------------------------------------
# Keep Kotlin Metadata so reflection-based and KType dynamic serializer resolution works
-keep class kotlin.Metadata { *; }

# Keep Companion object static fields of serializable classes
-keepclassmembers class * {
    @kotlin.jvm.Transient static <fields>;
}

# Keep serializer() method in Companion objects
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep classes implementing KSerializer and their constructors/methods
-keep class * implements kotlinx.serialization.KSerializer {
    public <init>(...);
    *;
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    public <init>(...);
    *;
}

# Keep all generated $$serializer classes and their members
-keep class **$$serializer {
    *;
}
-keepclassmembers class **$$serializer {
    *;
}

# Keep synthetic serialization constructor and write$Self methods
-keepclassmembers class * {
    public synthetic <init>(int, ..., kotlinx.serialization.internal.SerializationConstructorMarker);
    public static void write$Self(...);
}

# Keep SerialName and other serialization annotations on fields
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep kotlinx.serialization core internals
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ----------------------------------------------------------------------------------
# Jellyfin SDK
# ----------------------------------------------------------------------------------
-keep class org.jellyfin.sdk.** { *; }
-keepclassmembers class org.jellyfin.sdk.** { *; }
-dontwarn org.jellyfin.sdk.**

# ----------------------------------------------------------------------------------
# Vantafyn Core Models & Repositories
# ----------------------------------------------------------------------------------
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
