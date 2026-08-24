package dev.vantafyn.tv.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class TvNavigationItem(
    val route: TvRoute,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
    val isVisible: Boolean = true,
) {
    companion object {
        fun defaultItems(
            isAdmin: Boolean = false,
            hasRequests: Boolean = false,
        ): List<TvNavigationItem> = listOf(
            TvNavigationItem(
                route = TvRoute.Home,
                label = "Home",
                icon = Icons.Rounded.Home,
            ),
            TvNavigationItem(
                route = TvRoute.Library(),
                label = "Library",
                icon = Icons.AutoMirrored.Rounded.LibraryBooks,
            ),
            TvNavigationItem(
                route = TvRoute.Search,
                label = "Search",
                icon = Icons.Rounded.Search,
            ),
            TvNavigationItem(
                route = TvRoute.Music,
                label = "Music",
                icon = Icons.Rounded.MusicNote,
            ),
            TvNavigationItem(
                route = TvRoute.Requests,
                label = "Requests",
                icon = Icons.AutoMirrored.Rounded.Send,
                isVisible = hasRequests,
            ),
            TvNavigationItem(
                route = TvRoute.Admin,
                label = "Admin",
                icon = Icons.Rounded.AdminPanelSettings,
                isVisible = isAdmin,
            ),
            TvNavigationItem(
                route = TvRoute.Settings,
                label = "Settings",
                icon = Icons.Rounded.Settings,
            ),
        ).filter { it.isVisible }
    }
}
