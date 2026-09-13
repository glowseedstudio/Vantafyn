package dev.vantafyn.feature.home.mordor

import java.util.UUID

data class MiddleEarthMovie(
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

enum class MiddleEarthSortMode(val label: String) {
    Timeline("Timeline Order"),
    Release("Release Order"),
}

enum class MiddleEarthFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class MiddleEarthWatchItemUi(
    val movie: MiddleEarthMovie,
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

object MiddleEarthMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    val movies: List<MiddleEarthMovie> = listOf(
        MiddleEarthMovie(
            id = "the_war_of_the_rohirrim_2024",
            title = "The Lord of the Rings: The War of the Rohirrim",
            releaseYear = 2024,
            releaseOrder = 7,
            timelineOrder = 1,
            timelineSetting = "Third Age 2758 (183 Years Before LOTR)",
            era = "Helm Hammerhand",
            tmdbId = 839033,
            imdbId = "tt14824600",
            fallbackPosterPath = "/23WCoDo6wzBfzbX7BGTNwVUqZfi.jpg",
            overview = "A sudden attack by Wulf, a clever and ruthless Dunlending lord seeking vengeance for the death of his father, forces Helm Hammerhand and his people to make a daring last stand in the Hornburg fortress.",
        ),
        MiddleEarthMovie(
            id = "the_hobbit_an_unexpected_journey_2012",
            title = "The Hobbit: An Unexpected Journey",
            releaseYear = 2012,
            releaseOrder = 4,
            timelineOrder = 2,
            timelineSetting = "Third Age 2941 (Erebor Quest Begins)",
            era = "The Hobbit",
            tmdbId = 49051,
            imdbId = "tt0903624",
            fallbackPosterPath = "/yHA9Fc37VmpUA5UncTxxo3rTGVA.jpg",
            overview = "Bilbo Baggins is swept into an epic quest to reclaim the lost Kingdom of Erebor from the fearsome dragon Smaug, accompanied by thirteen dwarves and Gandalf the Grey.",
        ),
        MiddleEarthMovie(
            id = "the_hobbit_the_desolation_of_smaug_2013",
            title = "The Hobbit: The Desolation of Smaug",
            releaseYear = 2013,
            releaseOrder = 5,
            timelineOrder = 3,
            timelineSetting = "Third Age 2941 (Mirkwood to Erebor)",
            era = "The Hobbit",
            tmdbId = 57158,
            imdbId = "tt1170358",
            fallbackPosterPath = "/xQYiXsheRCDBA39DOrmaw1aSpbk.jpg",
            overview = "The Dwarves, Bilbo and Gandalf have successfully escaped the Misty Mountains, and Bilbo has gained the One Ring. They all continue their journey to get their gold back from the Dragon Smaug.",
        ),
        MiddleEarthMovie(
            id = "the_hobbit_the_battle_of_the_five_armies_2014",
            title = "The Hobbit: The Battle of the Five Armies",
            releaseYear = 2014,
            releaseOrder = 6,
            timelineOrder = 4,
            timelineSetting = "Third Age 2941 (The Battle of Five Armies)",
            era = "The Hobbit",
            tmdbId = 122917,
            imdbId = "tt2310332",
            fallbackPosterPath = "/xT98tLqatZPQApyRmlPL12LtiWp.jpg",
            overview = "Bilbo and company are forced to engage in a war against an array of combatants and keep the Lonely Mountain from falling into the hands of a rising darkness.",
        ),
        MiddleEarthMovie(
            id = "the_lord_of_the_rings_the_fellowship_of_the_ring_2001",
            title = "The Lord of the Rings: The Fellowship of the Ring",
            releaseYear = 2001,
            releaseOrder = 1,
            timelineOrder = 5,
            timelineSetting = "Third Age 3001–3018 (The Shire to Amon Hen)",
            era = "Lord of the Rings",
            tmdbId = 120,
            imdbId = "tt0120737",
            fallbackPosterPath = "/6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg",
            overview = "Young hobbit Frodo Baggins, after inheriting a mysterious ring from his uncle Bilbo, must journey with eight companions to Mount Doom to destroy it.",
        ),
        MiddleEarthMovie(
            id = "the_lord_of_the_rings_the_two_towers_2002",
            title = "The Lord of the Rings: The Two Towers",
            releaseYear = 2002,
            releaseOrder = 2,
            timelineOrder = 6,
            timelineSetting = "Third Age 3019 (Helm's Deep & Fangorn)",
            era = "Lord of the Rings",
            tmdbId = 121,
            imdbId = "tt0167261",
            fallbackPosterPath = "/5VTN0pR8gcqV3EPUHHfMGnJYN9L.jpg",
            overview = "Frodo and Sam continue toward Mordor with the treacherous Gollum, while Aragorn, Legolas and Gimli aid the kingdom of Rohan against Saruman's armies.",
        ),
        MiddleEarthMovie(
            id = "the_lord_of_the_rings_the_return_of_the_king_2003",
            title = "The Lord of the Rings: The Return of the King",
            releaseYear = 2003,
            releaseOrder = 3,
            timelineOrder = 7,
            timelineSetting = "Third Age 3019 (Battle of Pelennor & Mount Doom)",
            era = "Lord of the Rings",
            tmdbId = 122,
            imdbId = "tt0167260",
            fallbackPosterPath = "/rCzpDGLbOoPwLjy3OAm5NUPOTrC.jpg",
            overview = "Aragorn leads the World of Men against Sauron's overwhelming forces at Minas Tirith while Frodo and Sam reach the heart of Mordor to cast the Ring into Mount Doom.",
        ),
    )
}
