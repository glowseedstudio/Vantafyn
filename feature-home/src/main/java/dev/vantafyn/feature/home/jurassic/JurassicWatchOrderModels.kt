package dev.vantafyn.feature.home.jurassic

import java.util.UUID

data class JurassicMovie(
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

enum class JurassicSortMode(val label: String) {
    Release("Release Order"),
    Timeline("Timeline Order"),
}

enum class JurassicFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class JurassicWatchItemUi(
    val movie: JurassicMovie,
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

object JurassicMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    val movies: List<JurassicMovie> = listOf(
        JurassicMovie(
            id = "jurassic_park_1993",
            title = "Jurassic Park",
            releaseYear = 1993,
            releaseOrder = 1,
            timelineOrder = 1,
            timelineSetting = "1993 (Isla Nublar)",
            era = "Original Park",
            tmdbId = 329,
            imdbId = "tt0107290",
            fallbackPosterPath = "/63viWuPfYQjRYLSZSZNq7dglJP5.jpg",
            overview = "A pragmatic paleontologist touring an almost complete theme park on an island in Central America is tasked with protecting a couple of kids after a power failure causes the park's cloned dinosaurs to run loose.",
        ),
        JurassicMovie(
            id = "the_lost_world_jurassic_park_1997",
            title = "The Lost World: Jurassic Park",
            releaseYear = 1997,
            releaseOrder = 2,
            timelineOrder = 2,
            timelineSetting = "1997 (Isla Sorna / San Diego)",
            era = "Site B Incident",
            tmdbId = 330,
            imdbId = "tt0119567",
            fallbackPosterPath = "/7st3JW0xpMAkwB3dYfv3iqAwD8Y.jpg",
            overview = "A research team is sent to the Jurassic Park Site B island to study the dinosaurs there, while an InGen team approaches with another agenda.",
        ),
        JurassicMovie(
            id = "jurassic_park_iii_2001",
            title = "Jurassic Park III",
            releaseYear = 2001,
            releaseOrder = 3,
            timelineOrder = 3,
            timelineSetting = "2001 (Isla Sorna Rescue)",
            era = "Site B Return",
            tmdbId = 331,
            imdbId = "tt0163025",
            fallbackPosterPath = "/oQXj4NUfS3r3gHXtDOzcJgj1lLc.jpg",
            overview = "A decidedly odd couple with ulterior motives convince Dr. Alan Grant to go to Isla Sorna, resulting in an unexpected landing... and unexpected inhabitants on the island.",
        ),
        JurassicMovie(
            id = "jurassic_world_2015",
            title = "Jurassic World",
            releaseYear = 2015,
            releaseOrder = 4,
            timelineOrder = 4,
            timelineSetting = "2015 (Isla Nublar Resort)",
            era = "The Theme Park",
            tmdbId = 135397,
            imdbId = "tt0369610",
            fallbackPosterPath = "/rhr4y79GpxQF9IsfJItRXVaoGs4.jpg",
            overview = "Twenty-two years after the events of Jurassic Park, Isla Nublar now features a fully functioning dinosaur theme park, Jurassic World, as originally envisioned by John Hammond.",
        ),
        JurassicMovie(
            id = "jurassic_world_fallen_kingdom_2018",
            title = "Jurassic World: Fallen Kingdom",
            releaseYear = 2018,
            releaseOrder = 5,
            timelineOrder = 5,
            timelineSetting = "2018 (Isla Nublar / Lockwood Estate)",
            era = "Extinction & Escape",
            tmdbId = 351286,
            imdbId = "tt4881806",
            fallbackPosterPath = "/x2Us3jR6ToMJjbcPbLimYoxf6xr.jpg",
            overview = "When the island's dormant volcano begins roaring to life, Owen and Claire mount a campaign to rescue the remaining dinosaurs from this extinction-level event.",
        ),
        JurassicMovie(
            id = "jurassic_world_dominion_2022",
            title = "Jurassic World Dominion",
            releaseYear = 2022,
            releaseOrder = 6,
            timelineOrder = 6,
            timelineSetting = "2022 (Global Coexistence / BioSyn)",
            era = "Coexistence",
            tmdbId = 507086,
            imdbId = "tt8041270",
            fallbackPosterPath = "/jbAvCACjLf1ZG0unB2tdmx5HAf1.jpg",
            overview = "Four years after the destruction of Isla Nublar, dinosaurs now live and hunt alongside humans all over the world. This fragile balance will reshape the future.",
        ),
        JurassicMovie(
            id = "jurassic_world_rebirth_2025",
            title = "Jurassic World Rebirth",
            releaseYear = 2025,
            releaseOrder = 7,
            timelineOrder = 7,
            timelineSetting = "2025 (Equatorial Biosphere Mission)",
            era = "New Era",
            tmdbId = 1234821,
            imdbId = "tt31036941",
            fallbackPosterPath = "/1RICxzeoNCAO5NpcRMIgg1XT6fm.jpg",
            overview = "Five years after the events of Jurassic World Dominion, a covert operations team races to secure genetic material from the world's three most massive dinosaurs across land, sea, and air.",
        ),
    )
}
