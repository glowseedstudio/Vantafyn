package dev.vantafyn.feature.home.scary

import java.util.UUID

data class ScaryMovie(
    val id: String,
    val title: String,
    val releaseYear: Int,
    val releaseOrder: Int,
    val timelineOrder: Int,
    val timelineSetting: String,
    val era: String,
    val tmdbId: Int?,
    val imdbId: String?,
    val fallbackPosterPath: String,
    val overview: String? = null,
) {
    val fallbackPosterUrl: String
        get() = if (fallbackPosterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$fallbackPosterPath" else ""
}

enum class ScaryMovieSortMode(val label: String) {
    Release("Release Order"),
    Timeline("Timeline Order"),
}

enum class ScaryMovieFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class ScaryMovieWatchItemUi(
    val movie: ScaryMovie,
    val isOnServer: Boolean = false,
    val isPlayed: Boolean = false,
    val mediaItemId: UUID? = null,
    val serverImageUrl: String? = null,
    val serverBackdropUrl: String? = null,
    val playbackPositionPercentage: Float? = null,
) {
    val displayPosterUrl: String
        get() = serverImageUrl?.takeIf { it.isNotBlank() } ?: movie.fallbackPosterUrl
}

object ScaryMovieMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    val movies: List<ScaryMovie> = listOf(
        ScaryMovie(
            id = "scary_movie_2000",
            title = "Scary Movie",
            releaseYear = 2000,
            releaseOrder = 1,
            timelineOrder = 1,
            timelineSetting = "High School Parody (2000)",
            era = "Wayans Era",
            tmdbId = 4247,
            imdbId = "tt0175142",
            fallbackPosterPath = "/fVQFPRuw3yWXojYDJvA5EoFjUOY.jpg",
            overview = "A familiar-looking masked killer stalks a group of teenagers after they accidentally hit a man with their car, spoofing popular 90s slasher films.",
        ),
        ScaryMovie(
            id = "scary_movie_2_2001",
            title = "Scary Movie 2",
            releaseYear = 2001,
            releaseOrder = 2,
            timelineOrder = 2,
            timelineSetting = "Hell House (2001)",
            era = "Wayans Era",
            tmdbId = 4248,
            imdbId = "tt0257106",
            fallbackPosterPath = "/7Eb1JWK0Cb0rbfsYjwfc9g0PbQH.jpg",
            overview = "Four teens are duped into visiting a haunted mansion for a school project, parodying The Haunting, The Exorcist, and Poltergeist.",
        ),
        ScaryMovie(
            id = "scary_movie_3_2003",
            title = "Scary Movie 3",
            releaseYear = 2003,
            releaseOrder = 3,
            timelineOrder = 3,
            timelineSetting = "Crop Circles & Tape (2003)",
            era = "Zucker Era",
            tmdbId = 4256,
            imdbId = "tt0306047",
            fallbackPosterPath = "/8dLsax5KSwlCY5uWXETKjud5k6U.jpg",
            overview = "Cindy Campbell discovers a mysterious videotape that causes death in seven days while investigating crop circles, spoofing The Ring and Signs.",
        ),
        ScaryMovie(
            id = "scary_movie_4_2006",
            title = "Scary Movie 4",
            releaseYear = 2006,
            releaseOrder = 4,
            timelineOrder = 4,
            timelineSetting = "Alien Invasion (2006)",
            era = "Zucker Era",
            tmdbId = 4257,
            imdbId = "tt0362120",
            fallbackPosterPath = "/dEwlu8S0z1AibuX1weLwUyiRWFl.jpg",
            overview = "Cindy finds love with Tom Ryan while fighting off a terrifying invasion of alien triPods, parodying War of the Worlds, The Grudge, and Saw.",
        ),
        ScaryMovie(
            id = "scary_movie_5_2013",
            title = "Scary Movie 5",
            releaseYear = 2013,
            releaseOrder = 5,
            timelineOrder = 5,
            timelineSetting = "Paranormal House (2013)",
            era = "Paranormal Era",
            tmdbId = 4258,
            imdbId = "tt0795461",
            fallbackPosterPath = "/vBqLLxE6GaAPhO6v9EFvFbLZ7Ap.jpg",
            overview = "A happily-married couple begins to notice bizarre activity inside their home after they bring home their newborn infant, parodying Mama and Paranormal Activity.",
        ),
        ScaryMovie(
            id = "scary_movie_6_2026",
            title = "Scary Movie 6",
            releaseYear = 2026,
            releaseOrder = 6,
            timelineOrder = 6,
            timelineSetting = "Core Four Reunion (2026)",
            era = "Wayans Revival",
            tmdbId = 1273221,
            imdbId = "tt32093575",
            fallbackPosterPath = "/znHT8peERZRWG1ME3r0Db0EV8k8.jpg",
            overview = "Twenty-six years after outrunning a suspiciously familiar masked killer, the Core Four are back in the killer's crosshairs and no horror movie IP is safe.",
        ),
    )
}
