package dev.vantafyn.feature.home.matrix

import java.util.UUID

data class MatrixMovie(
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

enum class MatrixSortMode(val label: String) {
    Release("Release Order"),
    Timeline("Timeline Order"),
}

enum class MatrixFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class MatrixWatchItemUi(
    val movie: MatrixMovie,
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

object MatrixMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    val movies: List<MatrixMovie> = listOf(
        MatrixMovie(
            id = "the_matrix_1999",
            title = "The Matrix",
            releaseYear = 1999,
            releaseOrder = 1,
            timelineOrder = 1,
            timelineSetting = "c. 2199 (1999 Simulation)",
            era = "The One Awakens",
            tmdbId = 603,
            imdbId = "tt0133093",
            fallbackPosterPath = "/aOIuZAjPaRIE6CMzbazvcHuHXDc.jpg",
            overview = "A computer hacker learns from mysterious rebels about the true nature of his reality and his role in the war against its controllers.",
        ),
        MatrixMovie(
            id = "the_matrix_reloaded_2003",
            title = "The Matrix Reloaded",
            releaseYear = 2003,
            releaseOrder = 2,
            timelineOrder = 2,
            timelineSetting = "6 Months Later",
            era = "The Sixth Cycle",
            tmdbId = 604,
            imdbId = "tt0234215",
            fallbackPosterPath = "/aA5qHS0FbSXO8PxcxUIHbDrJyuh.jpg",
            overview = "Neo and the rebel leaders estimate that they have 72 hours until 250,000 probes discover Zion and destroy it. Neo must decide how he can save Trinity from a dark fate in his dreams.",
        ),
        MatrixMovie(
            id = "the_matrix_revolutions_2003",
            title = "The Matrix Revolutions",
            releaseYear = 2003,
            releaseOrder = 3,
            timelineOrder = 3,
            timelineSetting = "Immediately Following Reloaded",
            era = "The Machine War",
            tmdbId = 605,
            imdbId = "tt0242653",
            fallbackPosterPath = "/qEWiBXJGXK28jGBAm8oFKKTB0WD.jpg",
            overview = "The human city of Zion defends itself against the massive invasion of the machines as Neo fights to end the war at another front while also opposing the rogue Agent Smith.",
        ),
        MatrixMovie(
            id = "the_matrix_resurrections_2021",
            title = "The Matrix Resurrections",
            releaseYear = 2021,
            releaseOrder = 4,
            timelineOrder = 4,
            timelineSetting = "60 Years After Machine War",
            era = "The Resurgence",
            tmdbId = 624860,
            imdbId = "tt10838180",
            fallbackPosterPath = "/8c4a8kE7PizaGQQnditMmI1xbRp.jpg",
            overview = "Plagued by strange memories, Neo's life takes an unexpected turn when he finds himself back inside the Matrix.",
        ),
    )
}
