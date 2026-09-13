package dev.vantafyn.feature.home.scream

import java.util.UUID

data class ScreamMovie(
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

enum class ScreamSortMode(val label: String) {
    Release("Release Order"),
    Timeline("Timeline Order"),
}

enum class ScreamFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class ScreamWatchItemUi(
    val movie: ScreamMovie,
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

object ScreamMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    val movies: List<ScreamMovie> = listOf(
        ScreamMovie(
            id = "scream_1996",
            title = "Scream",
            releaseYear = 1996,
            releaseOrder = 1,
            timelineOrder = 1,
            timelineSetting = "1996 (Woodsboro)",
            era = "The Original Spree",
            tmdbId = 4232,
            imdbId = "tt0120082",
            fallbackPosterPath = "/lr9ZIrmuwVmZhpZuTCW8D9g0ZJe.jpg",
            overview = "A year after the murder of her mother, a teenage girl is terrorized by a masked killer who targets her and her friends using scary movies as part of a deadly game.",
        ),
        ScreamMovie(
            id = "scream_2_1997",
            title = "Scream 2",
            releaseYear = 1997,
            releaseOrder = 2,
            timelineOrder = 2,
            timelineSetting = "1998 (Windsor College)",
            era = "College Copycat",
            tmdbId = 4233,
            imdbId = "tt0120084",
            fallbackPosterPath = "/dORlVasiaDkJXTqt9bdH7nFNs6C.jpg",
            overview = "Two years after the first series of murders, a new Ghostface killer begins stalking Sidney Prescott and her friends at Windsor College.",
        ),
        ScreamMovie(
            id = "scream_3_2000",
            title = "Scream 3",
            releaseYear = 2000,
            releaseOrder = 3,
            timelineOrder = 3,
            timelineSetting = "2000 (Hollywood / Stab 3)",
            era = "Hollywood Finale",
            tmdbId = 4234,
            imdbId = "tt0134084",
            fallbackPosterPath = "/qpH8ToZVlFD1bakL04LkEKodyDI.jpg",
            overview = "Sidney Prescott has gone into hiding, but is drawn out when a killer begins murdering cast members of Stab 3 in production order.",
        ),
        ScreamMovie(
            id = "scream_4_2011",
            title = "Scream 4",
            releaseYear = 2011,
            releaseOrder = 4,
            timelineOrder = 4,
            timelineSetting = "2011 (Woodsboro 15th Anniversary)",
            era = "Reboot Generation",
            tmdbId = 41446,
            imdbId = "tt1262416",
            fallbackPosterPath = "/3cf8p2qw9qnsaaFWc64AG6U05eT.jpg",
            overview = "Ten years later, Sidney returns home to Woodsboro on her book tour, bringing about the return of Ghostface who targets the new high school generation.",
        ),
        ScreamMovie(
            id = "scream_2022",
            title = "Scream",
            releaseYear = 2022,
            releaseOrder = 5,
            timelineOrder = 5,
            timelineSetting = "2022 (Woodsboro Legacy)",
            era = "The Re-quel",
            tmdbId = 646385,
            imdbId = "tt11245972",
            fallbackPosterPath = "/nD4M4Bx457ryLuKYpxFwQ2IBJ5w.jpg",
            overview = "Twenty-five years after the original streak of brutal murders, a new killer dons the Ghostface mask to resurrect secrets from Woodsboro's deadly past.",
        ),
        ScreamMovie(
            id = "scream_vi_2023",
            title = "Scream VI",
            releaseYear = 2023,
            releaseOrder = 6,
            timelineOrder = 6,
            timelineSetting = "2023 (New York City)",
            era = "New York Nightmare",
            tmdbId = 934433,
            imdbId = "tt17663992",
            fallbackPosterPath = "/wDWwtvkRRlgTiUr6TyLSMX8FCuZ.jpg",
            overview = "The four survivors of the Woodsboro murders leave their hometown behind and start a fresh chapter in New York City, only to be hunted by Ghostface once more.",
        ),
        ScreamMovie(
            id = "scream_7_2026",
            title = "Scream 7",
            releaseYear = 2026,
            releaseOrder = 7,
            timelineOrder = 7,
            timelineSetting = "2026 (Sidney Prescott Returns)",
            era = "Bloodline",
            tmdbId = 1159559,
            imdbId = "tt27047903",
            fallbackPosterPath = "/jjyuk0edLiW8vOSnlfwWCCLpbh5.jpg",
            overview = "Sidney Prescott must confront the Ghostface legacy once again to protect her family in the seventh installment directed by Kevin Williamson.",
        ),
    )
}
