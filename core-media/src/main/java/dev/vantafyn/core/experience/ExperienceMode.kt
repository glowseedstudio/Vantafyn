package dev.vantafyn.core.experience

import android.content.Context

enum class ExperienceMode(
    val id: String,
    val title: String,
    val description: String,
) {
    FullMedia(
        id = "full_media",
        title = "Full Media Experience",
        description = "Movies, TV Shows, Music, Live TV, and WatchParty powered by Jellyfin.",
    ),
    MusicOnly(
        id = "music_only",
        title = "Music Experience",
        description = "Dedicated music streaming powered by OpenSubsonic (Navidrome, Gonic, etc.) or Jellyfin Music.",
    );

    companion object {
        val Default = FullMedia

        fun fromId(id: String?): ExperienceMode =
            entries.firstOrNull { it.id == id || it.name.equals(id, ignoreCase = true) } ?: Default
    }
}

enum class MusicBackendType(
    val id: String,
    val title: String,
    val description: String,
) {
    OpenSubsonic(
        id = "opensubsonic",
        title = "OpenSubsonic / Navidrome",
        description = "Connect to Navidrome, Gonic, LMS, Subsonic, or any OpenSubsonic-compatible server.",
    ),
    Jellyfin(
        id = "jellyfin",
        title = "Jellyfin Music",
        description = "Connect to your Jellyfin server scoped exclusively to your music library.",
    );

    companion object {
        val Default = OpenSubsonic

        fun fromId(id: String?): MusicBackendType =
            entries.firstOrNull { it.id == id || it.name.equals(id, ignoreCase = true) } ?: Default
    }
}

object ExperiencePreferences {
    private const val PREFS_NAME = "vantafyn_app_preferences"
    private const val KEY_EXPERIENCE_MODE = "experience_mode"
    private const val KEY_MUSIC_BACKEND_TYPE = "music_backend_type"

    fun getExperienceMode(context: Context): ExperienceMode {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ExperienceMode.fromId(prefs.getString(KEY_EXPERIENCE_MODE, null))
    }

    fun setExperienceMode(context: Context, mode: ExperienceMode) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EXPERIENCE_MODE, mode.id)
            .apply()
    }

    fun getMusicBackendType(context: Context): MusicBackendType {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return MusicBackendType.fromId(prefs.getString(KEY_MUSIC_BACKEND_TYPE, null))
    }

    fun setMusicBackendType(context: Context, backend: MusicBackendType) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MUSIC_BACKEND_TYPE, backend.id)
            .apply()
    }
}
