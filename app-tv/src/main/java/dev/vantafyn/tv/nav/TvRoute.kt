package dev.vantafyn.tv.nav

import java.util.UUID

sealed class TvRoute {
    data object Home : TvRoute()
    data object Favorites : TvRoute()
    data class Library(val parentId: UUID? = null, val title: String = "Library") : TvRoute()
    data object Search : TvRoute()
    data object Music : TvRoute()
    data object Requests : TvRoute()
    data object Admin : TvRoute()
    data object Settings : TvRoute()
    data object Notifications : TvRoute()
    data class Details(val itemId: UUID) : TvRoute()
    data class Player(val itemId: UUID) : TvRoute()
}
