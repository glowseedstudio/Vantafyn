package dev.vantafyn.feature.home.saw

import java.util.UUID

data class SawMovie(
    val id: String,
    val title: String,
    val releaseYear: Int,
    val releaseOrder: Int,
    val timelineOrder: Int,
    val timelineSetting: String,
    val franchise: String,
    val tmdbId: Int?,
    val imdbId: String?,
    val fallbackPosterPath: String,
    val overview: String? = null,
) {
    val fallbackPosterUrl: String
        get() = if (fallbackPosterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$fallbackPosterPath" else ""
}

enum class SawSortMode(val label: String) {
    Release("Release Order"),
    Timeline("Timeline Order"),
}

enum class SawFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class SawWatchItemUi(
    val movie: SawMovie,
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

object SawMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    val movies: List<SawMovie> = listOf(
        SawMovie(
            id = "saw_2004",
            title = "Saw",
            releaseYear = 2004,
            releaseOrder = 1,
            timelineOrder = 1,
            timelineSetting = "2004",
            franchise = "Original",
            tmdbId = 176,
            imdbId = "tt0387564",
            fallbackPosterPath = "/qEFNjO3OAZP74bk3R55xtBQ4Pq2.jpg",
            overview = "Two men wake up chained in a decrepit bathroom with a corpse between them. The manipulative Jigsaw Killer has a new game for them to play.",
        ),
        SawMovie(
            id = "saw_2_2005",
            title = "Saw II",
            releaseYear = 2005,
            releaseOrder = 2,
            timelineOrder = 3,
            timelineSetting = "2005 (After Saw X)",
            franchise = "Original",
            tmdbId = 9619,
            imdbId = "tt0432291",
            fallbackPosterPath = "/8hpKLaTXbsHxqNUvF8cqHlL1Cuk.jpg",
            overview = "Detective Eric Matthews must play a new Jigsaw game after his son is trapped in a house with seven other victims — all slowly being poisoned.",
        ),
        SawMovie(
            id = "saw_3_2006",
            title = "Saw III",
            releaseYear = 2006,
            releaseOrder = 3,
            timelineOrder = 4,
            timelineSetting = "2006",
            franchise = "Original",
            tmdbId = 9622,
            imdbId = "tt0489270",
            fallbackPosterPath = "/6mJJt0DgRuY6bYJPaVpLDkqMKln.jpg",
            overview = "A dying Jigsaw kidnaps a surgeon to keep him alive while his apprentice Amanda arranges the most elaborate game yet — with a grieving father at its centre.",
        ),
        SawMovie(
            id = "saw_4_2007",
            title = "Saw IV",
            releaseYear = 2007,
            releaseOrder = 4,
            timelineOrder = 5,
            timelineSetting = "2006 (Concurrent with Saw III)",
            franchise = "Original",
            tmdbId = 9624,
            imdbId = "tt0890870",
            fallbackPosterPath = "/d07phJqCx6z5nILLFCRQ4goCHjQ.jpg",
            overview = "Even after Jigsaw's death, the games continue. SWAT Commander Rigg is forced to play while unravelling the origins of the Jigsaw Killer.",
        ),
        SawMovie(
            id = "saw_5_2008",
            title = "Saw V",
            releaseYear = 2008,
            releaseOrder = 5,
            timelineOrder = 6,
            timelineSetting = "2007",
            franchise = "Original",
            tmdbId = 14014,
            imdbId = "tt1132626",
            fallbackPosterPath = "/fwMfZfMv0OyFyMdJsTKFqBtRcYJ.jpg",
            overview = "Detective Strahm continues to uncover the conspiracy behind Jigsaw, while five new victims are put through a deadly game designed for one survivor.",
        ),
        SawMovie(
            id = "saw_6_2009",
            title = "Saw VI",
            releaseYear = 2009,
            releaseOrder = 6,
            timelineOrder = 7,
            timelineSetting = "2007",
            franchise = "Original",
            tmdbId = 18634,
            imdbId = "tt1259521",
            fallbackPosterPath = "/j5Wr8J4bHaOhEMGhBIPzHnICKFc.jpg",
            overview = "Agent Strahm is dead, and Detective Hoffman has emerged as the executor of Jigsaw's games. A health insurance executive becomes the next test subject.",
        ),
        SawMovie(
            id = "saw_3d_2010",
            title = "Saw 3D",
            releaseYear = 2010,
            releaseOrder = 7,
            timelineOrder = 8,
            timelineSetting = "2008 (The Final Chapter)",
            franchise = "Original",
            tmdbId = 44264,
            imdbId = "tt1233227",
            fallbackPosterPath = "/ioFuSk7JzQPdPOXC6gVoZ7y1UvF.jpg",
            overview = "As a deadly battle rages over Jigsaw's legacy, a group of survivors reform each other while the worst trap of all is unleashed.",
        ),
        SawMovie(
            id = "jigsaw_2017",
            title = "Jigsaw",
            releaseYear = 2017,
            releaseOrder = 8,
            timelineOrder = 9,
            timelineSetting = "~2017",
            franchise = "Legacy",
            tmdbId = 298250,
            imdbId = "tt4983590",
            fallbackPosterPath = "/7RwHxhdUNS996JPFNB9a7CJtlwR.jpg",
            overview = "Bodies begin turning up around the city, each having met a gruesome end. With all signs pointing to Jigsaw, detectives must figure out how a dead man is killing again.",
        ),
        SawMovie(
            id = "spiral_2021",
            title = "Spiral: From the Book of Saw",
            releaseYear = 2021,
            releaseOrder = 9,
            timelineOrder = 10,
            timelineSetting = "~2021",
            franchise = "Legacy",
            tmdbId = 587807,
            imdbId = "tt7642838",
            fallbackPosterPath = "/lA6P0bHRCnUBwWxJXFa9h21gUaS.jpg",
            overview = "A criminal mastermind unleashes a twisted form of justice in Spiral, a grisly, Jigsaw-inspired game targeting a corrupt police department.",
        ),
        SawMovie(
            id = "saw_x_2023",
            title = "Saw X",
            releaseYear = 2023,
            releaseOrder = 10,
            timelineOrder = 2,
            timelineSetting = "2004 (Between Saw & Saw II)",
            franchise = "Original",
            tmdbId = 951491,
            imdbId = "tt17009710",
            fallbackPosterPath = "/aQPeznSu7XDTrrdCtT5eLiu52Yu.jpg",
            overview = "John Kramer travels to Mexico for an experimental medical procedure, only to discover the treatment is a fraud. He devises a new game for those who preyed upon him.",
        ),
    )
}
