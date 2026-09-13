package dev.vantafyn.feature.home.twilight

import java.util.UUID

data class TwilightMovie(
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

enum class TwilightSortMode(val label: String) {
    Release("Release Order"),
    Timeline("Timeline Order"),
}

enum class TwilightFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class TwilightWatchItemUi(
    val movie: TwilightMovie,
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

object TwilightMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    val movies: List<TwilightMovie> = listOf(
        TwilightMovie(
            id = "twilight_2008",
            title = "Twilight",
            releaseYear = 2008,
            releaseOrder = 1,
            timelineOrder = 1,
            timelineSetting = "Spring - Autumn 2005",
            era = "Forks Arrival",
            tmdbId = 8966,
            imdbId = "tt1099212",
            fallbackPosterPath = "/3Gkb6jm6962ADUPaCBqzz9CTbn9.jpg",
            overview = "When Bella Swan moves to a small town in the Pacific Northwest, she falls in love with Edward Cullen, a mysterious classmate who reveals himself to be a vampire.",
        ),
        TwilightMovie(
            id = "the_twilight_saga_new_moon_2009",
            title = "The Twilight Saga: New Moon",
            releaseYear = 2009,
            releaseOrder = 2,
            timelineOrder = 2,
            timelineSetting = "Autumn 2005 - Spring 2006",
            era = "Volturi Threat",
            tmdbId = 18239,
            imdbId = "tt1259571",
            fallbackPosterPath = "/k2qTooPlHffgNABNWxeJdGMglPK.jpg",
            overview = "Fearing for Bella's safety, Edward leaves town, plunging her into depression until she finds solace in a deep friendship with Jacob Black.",
        ),
        TwilightMovie(
            id = "the_twilight_saga_eclipse_2010",
            title = "The Twilight Saga: Eclipse",
            releaseYear = 2010,
            releaseOrder = 3,
            timelineOrder = 3,
            timelineSetting = "Summer 2006",
            era = "Newborn Army",
            tmdbId = 24021,
            imdbId = "tt1325004",
            fallbackPosterPath = "/dK4Gi1UdMiHzHc7r7CZQG4IQ9Sr.jpg",
            overview = "Bella is once again surrounded by danger as Seattle is ravaged by a string of mysterious killings and a malicious vampire continues her quest for revenge.",
        ),
        TwilightMovie(
            id = "the_twilight_saga_breaking_dawn_part_1_2011",
            title = "The Twilight Saga: Breaking Dawn – Part 1",
            releaseYear = 2011,
            releaseOrder = 4,
            timelineOrder = 4,
            timelineSetting = "Autumn 2006",
            era = "Wedding & Renesmee",
            tmdbId = 50619,
            imdbId = "tt1324999",
            fallbackPosterPath = "/qs8LsHKYlVRmJbFUiSUhhRAygwj.jpg",
            overview = "The new love, intimacy, and marriage between Bella and Edward are tragically cut short by a series of events involving a perilous pregnancy.",
        ),
        TwilightMovie(
            id = "the_twilight_saga_breaking_dawn_part_2_2012",
            title = "The Twilight Saga: Breaking Dawn – Part 2",
            releaseYear = 2012,
            releaseOrder = 5,
            timelineOrder = 5,
            timelineSetting = "Winter 2006",
            era = "Final Stand",
            tmdbId = 50620,
            imdbId = "tt1673434",
            fallbackPosterPath = "/2K5nZ5wTzG2U3pM6g5p0w0q.jpg",
            overview = "After the birth of Renesmee, the Cullens gather other vampire clans in order to protect the child from a false allegation that puts the family in front of the Volturi.",
        ),
    )
}
