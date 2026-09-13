package dev.vantafyn.feature.home.pirates

import java.util.UUID

data class PiratesMovie(
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

enum class PiratesSortMode(val label: String) {
    Release("Release Order"),
    Timeline("Timeline Order"),
}

enum class PiratesFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class PiratesWatchItemUi(
    val movie: PiratesMovie,
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

object PiratesMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    val movies: List<PiratesMovie> = listOf(
        PiratesMovie(
            id = "pirates_of_the_caribbean_the_curse_of_the_black_pearl_2003",
            title = "Pirates of the Caribbean: The Curse of the Black Pearl",
            releaseYear = 2003,
            releaseOrder = 1,
            timelineOrder = 1,
            timelineSetting = "c. 1720 (Port Royal & Isla de Muerta)",
            era = "The Aztec Curse",
            tmdbId = 22,
            imdbId = "tt0325980",
            fallbackPosterPath = "/poHwCZeWzJCShH7tOjg8RIoyjcw.jpg",
            overview = "Blacksmith Will Turner teams up with eccentric pirate \"Captain\" Jack Sparrow to save his love, the governor's daughter, from Jack's former pirate allies who are undead.",
        ),
        PiratesMovie(
            id = "pirates_of_the_caribbean_dead_mans_chest_2006",
            title = "Pirates of the Caribbean: Dead Man's Chest",
            releaseYear = 2006,
            releaseOrder = 2,
            timelineOrder = 2,
            timelineSetting = "c. 1721 (The Flying Dutchman)",
            era = "Davy Jones' Locker",
            tmdbId = 58,
            imdbId = "tt0383574",
            fallbackPosterPath = "/uXEqmloGyP7UXAiphJUu2v2pcuE.jpg",
            overview = "Jack Sparrow races to recover the heart of Davy Jones to avoid enslaving his soul to Jones' service, as other friends and foes seek the heart for their own agenda.",
        ),
        PiratesMovie(
            id = "pirates_of_the_caribbean_at_worlds_end_2007",
            title = "Pirates of the Caribbean: At World's End",
            releaseYear = 2007,
            releaseOrder = 3,
            timelineOrder = 3,
            timelineSetting = "c. 1721 (World's End & Brethren Court)",
            era = "Calypso's Wrath",
            tmdbId = 285,
            imdbId = "tt0449088",
            fallbackPosterPath = "/jGWpG4YhpQwVmjyHEGkxEkeRf0S.jpg",
            overview = "Captain Barbossa, Will Turner and Elizabeth Swann must sail off the edge of the map, navigate treachery and find Jack Sparrow to make a final alliance against Cutler Beckett.",
        ),
        PiratesMovie(
            id = "pirates_of_the_caribbean_on_stranger_tides_2011",
            title = "Pirates of the Caribbean: On Stranger Tides",
            releaseYear = 2011,
            releaseOrder = 4,
            timelineOrder = 4,
            timelineSetting = "c. 1750 (The Fountain of Youth)",
            era = "Fountain of Youth",
            tmdbId = 1865,
            imdbId = "tt1298650",
            fallbackPosterPath = "/keGfSvCmYj7CvdRx36OdVrAEibE.jpg",
            overview = "Jack Sparrow and Barbossa embark on a quest to find the elusive fountain of youth, only to discover that Blackbeard and his daughter are after it too.",
        ),
        PiratesMovie(
            id = "pirates_of_the_caribbean_dead_men_tell_no_tales_2017",
            title = "Pirates of the Caribbean: Dead Men Tell No Tales",
            releaseYear = 2017,
            releaseOrder = 5,
            timelineOrder = 5,
            timelineSetting = "c. 1755 (Trident of Poseidon)",
            era = "Poseidon's Trident",
            tmdbId = 166426,
            imdbId = "tt1790809",
            fallbackPosterPath = "/6lAPOAFYFWIO3SQRemEY2wInQMC.jpg",
            overview = "Captain Jack Sparrow is pursued by old rival Armando Salazar, who escaped from the Devil's Triangle with his ghost crew determined to kill every pirate at sea.",
        ),
    )
}
