package dev.vantafyn.feature.home.jumanji

import java.util.UUID

data class JumanjiMovie(
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

enum class JumanjiSortMode(val label: String) {
    Release("Release Order"),
    Timeline("Timeline Order"),
}

enum class JumanjiFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class JumanjiWatchItemUi(
    val movie: JumanjiMovie,
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

object JumanjiMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    val movies: List<JumanjiMovie> = listOf(
        JumanjiMovie(
            id = "jumanji_1995",
            title = "Jumanji",
            releaseYear = 1995,
            releaseOrder = 1,
            timelineOrder = 1,
            timelineSetting = "1969 / 1995 (Brantford, NH)",
            era = "The Board Game",
            tmdbId = 8844,
            imdbId = "tt0113497",
            fallbackPosterPath = "/bdHG5Mo83VPobeZZdlSz0Y7HQHB.jpg",
            overview = "When two children play an old board game they find in an attic, they unwittingly free Alan Parrish who was trapped in its jungle world for 26 years.",
        ),
        JumanjiMovie(
            id = "jumanji_welcome_to_the_jungle_2017",
            title = "Jumanji: Welcome to the Jungle",
            releaseYear = 2017,
            releaseOrder = 2,
            timelineOrder = 2,
            timelineSetting = "1996 / 2017 (The Video Game)",
            era = "Level One",
            tmdbId = 353486,
            imdbId = "tt2283362",
            fallbackPosterPath = "/pSgXKPU5h6U89ipF7HBYajvYt7j.jpg",
            overview = "Four high school kids discover an old video game console and are drawn into the game's jungle setting, literally becoming the adult avatars they chose.",
        ),
        JumanjiMovie(
            id = "jumanji_the_next_level_2019",
            title = "Jumanji: The Next Level",
            releaseYear = 2019,
            releaseOrder = 3,
            timelineOrder = 3,
            timelineSetting = "2019 (Glitch in the Game)",
            era = "Next Level",
            tmdbId = 512200,
            imdbId = "tt7975244",
            fallbackPosterPath = "/jyw8VKYEiM1UDzPB7NsisUgBeJ8.jpg",
            overview = "As the gang return to Jumanji to rescue one of their own, they discover that nothing is as they expect. The players will have to brave parts unknown from arid deserts to snowy mountains.",
        ),
    )
}
