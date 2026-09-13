package dev.vantafyn.feature.home.residentevil

import java.util.UUID

data class ResidentEvilMovie(
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

enum class ResidentEvilSortMode(val label: String) {
    Release("Release Order"),
    Timeline("Timeline Order"),
}

enum class ResidentEvilFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class ResidentEvilWatchItemUi(
    val movie: ResidentEvilMovie,
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

object ResidentEvilMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    val movies: List<ResidentEvilMovie> = listOf(
        ResidentEvilMovie(
            id = "resident_evil_2002",
            title = "Resident Evil",
            releaseYear = 2002,
            releaseOrder = 1,
            timelineOrder = 1,
            timelineSetting = "2002",
            era = "The Hive",
            tmdbId = 1576,
            imdbId = "tt0120804",
            fallbackPosterPath = "/1UKNef590A0ZaMnxsscIcWuK1Em.jpg",
            overview = "A special military unit fights a powerful, out-of-control supercomputer and hundreds of scientists who have mutated into flesh-eating creatures after a laboratory accident.",
        ),
        ResidentEvilMovie(
            id = "resident_evil_apocalypse_2004",
            title = "Resident Evil: Apocalypse",
            releaseYear = 2004,
            releaseOrder = 2,
            timelineOrder = 2,
            timelineSetting = "2004",
            era = "Raccoon City",
            tmdbId = 1577,
            imdbId = "tt0318627",
            fallbackPosterPath = "/way9dOm4dM2sm9UMcu2PEXMTX0q.jpg",
            overview = "Alice awakes in Raccoon City, now overrun by zombies and Umbrella bio-weapons, and must escape before a nuclear strike destroys the city.",
        ),
        ResidentEvilMovie(
            id = "resident_evil_extinction_2007",
            title = "Resident Evil: Extinction",
            releaseYear = 2007,
            releaseOrder = 3,
            timelineOrder = 3,
            timelineSetting = "2007",
            era = "The Wasteland",
            tmdbId = 7737,
            imdbId = "tt0432021",
            fallbackPosterPath = "/6yaLr7Ymg5cvbtSVi5hHwBKx35I.jpg",
            overview = "Years after the Raccoon City catastrophe, Alice joins a convoy traveling across the Nevada desert striving to reach sanctuary in Alaska while evading Umbrella forces.",
        ),
        ResidentEvilMovie(
            id = "resident_evil_afterlife_2010",
            title = "Resident Evil: Afterlife",
            releaseYear = 2010,
            releaseOrder = 4,
            timelineOrder = 4,
            timelineSetting = "2010",
            era = "Arcadia",
            tmdbId = 35791,
            imdbId = "tt1220634",
            fallbackPosterPath = "/qZHDnt8bnsD2cSJfIbjWbCkIq3G.jpg",
            overview = "Alice searches for survivors in Los Angeles following the virus outbreak, leading them toward a mysterious haven known as Arcadia.",
        ),
        ResidentEvilMovie(
            id = "resident_evil_retribution_2012",
            title = "Resident Evil: Retribution",
            releaseYear = 2012,
            releaseOrder = 5,
            timelineOrder = 5,
            timelineSetting = "2012",
            era = "Simulation",
            tmdbId = 71679,
            imdbId = "tt1855325",
            fallbackPosterPath = "/ohdUDWVlcbuWphaLu6wS91xdJ73.jpg",
            overview = "Alice awakens in the heart of Umbrella's clandestine operations facility and journeys across virtual cities to escape with the help of allies old and new.",
        ),
        ResidentEvilMovie(
            id = "resident_evil_the_final_chapter_2016",
            title = "Resident Evil: The Final Chapter",
            releaseYear = 2016,
            releaseOrder = 6,
            timelineOrder = 6,
            timelineSetting = "2016",
            era = "The End",
            tmdbId = 173897,
            imdbId = "tt2592614",
            fallbackPosterPath = "/7glPlA0xPpxPxBu0TnY4ulQVCV1.jpg",
            overview = "Alice returns to where the nightmare began—The Hive in Raccoon City—for a final confrontation against the Umbrella Corporation to release the airborne antivirus.",
        ),
    )
}
