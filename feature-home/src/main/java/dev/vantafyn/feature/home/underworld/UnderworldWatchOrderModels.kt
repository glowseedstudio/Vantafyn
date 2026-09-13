package dev.vantafyn.feature.home.underworld

import java.util.UUID

data class UnderworldMovie(
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

enum class UnderworldSortMode(val label: String) {
    Timeline("Timeline Order"),
    Release("Release Order"),
}

enum class UnderworldFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class UnderworldWatchItemUi(
    val movie: UnderworldMovie,
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

object UnderworldMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    val movies: List<UnderworldMovie> = listOf(
        UnderworldMovie(
            id = "underworld_rise_of_the_lycans_2009",
            title = "Underworld: Rise of the Lycans",
            releaseYear = 2009,
            releaseOrder = 3,
            timelineOrder = 1,
            timelineSetting = "15th Century (Dark Ages)",
            era = "The Rebellion",
            tmdbId = 12437,
            imdbId = "tt1186727",
            fallbackPosterPath = "/yW9gF7rGn8EoV8B8rxOx1xjxVZf.jpg",
            overview = "A prequel tracing the origins of the centuries-old blood feud between the aristocratic vampires and their former slaves, the Lycans, led by Lucian.",
        ),
        UnderworldMovie(
            id = "underworld_2003",
            title = "Underworld",
            releaseYear = 2003,
            releaseOrder = 1,
            timelineOrder = 2,
            timelineSetting = "c. 2003 (Modern Night)",
            era = "Death Dealer",
            tmdbId = 277,
            imdbId = "tt0320691",
            fallbackPosterPath = "/zsnQ41UZ3jo1wEeemF0eA9cAIU0.jpg",
            overview = "Selene, a vampire death dealer, becomes entrenched in a war between vampires and werewolves while protecting Michael, a human hunted by the Lycans.",
        ),
        UnderworldMovie(
            id = "underworld_evolution_2006",
            title = "Underworld: Evolution",
            releaseYear = 2006,
            releaseOrder = 2,
            timelineOrder = 3,
            timelineSetting = "Immediately Following Underworld",
            era = "Corvinus Bloodline",
            tmdbId = 834,
            imdbId = "tt0401855",
            fallbackPosterPath = "/oJaQG353uOzOqffQ5K2hg03k4Vp.jpg",
            overview = "Selene and Michael fight together against the first true Vampire, Marcus, and seek to prevent the release of his imprisoned werewolf brother, William.",
        ),
        UnderworldMovie(
            id = "underworld_awakening_2012",
            title = "Underworld: Awakening",
            releaseYear = 2012,
            releaseOrder = 4,
            timelineOrder = 4,
            timelineSetting = "12 Years Later (The Purge)",
            era = "Human Cleansing",
            tmdbId = 52520,
            imdbId = "tt1496025",
            fallbackPosterPath = "/jN0uuc8U6M3sTg9zEaliJV60Stf.jpg",
            overview = "Having escaped human captivity after mankind learned of the existence of both species, Selene discovers she has a daughter who holds the key to Lycan supremacy.",
        ),
        UnderworldMovie(
            id = "underworld_blood_wars_2016",
            title = "Underworld: Blood Wars",
            releaseYear = 2016,
            releaseOrder = 5,
            timelineOrder = 5,
            timelineSetting = "Modern Climax",
            era = "Final War",
            tmdbId = 346672,
            imdbId = "tt3215888",
            fallbackPosterPath = "/v1ciDCWMG47gdT4kMyjyQbnLQQn.jpg",
            overview = "Selene fights to end the eternal war between the Lycan clan and the Vampire faction that betrayed her, aided only by David and his father Thomas.",
        ),
    )
}
