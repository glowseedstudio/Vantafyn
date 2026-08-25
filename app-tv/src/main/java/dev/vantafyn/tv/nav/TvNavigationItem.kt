package dev.vantafyn.tv.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import dev.vantafyn.core.jellyfin.JellyfinLibrary

data class TvNavigationItem(
    val route: TvRoute,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
    val isVisible: Boolean = true,
    val onClick: (() -> Unit)? = null,
) {
    companion object {
        fun buildMainItems(
            libraries: List<JellyfinLibrary>,
            hasFavorites: Boolean = true,
            isAdmin: Boolean = false,
            hasRequests: Boolean = false,
        ): List<TvNavigationItem> {
            val list = mutableListOf<TvNavigationItem>()

            // 1. Home
            list.add(
                TvNavigationItem(
                    route = TvRoute.Home,
                    label = "Home",
                    icon = Icons.Rounded.Home,
                )
            )

            // 2. Favorites / My List
            if (hasFavorites) {
                list.add(
                    TvNavigationItem(
                        route = TvRoute.Favorites,
                        label = "Favorites",
                        icon = Icons.Rounded.Favorite,
                    )
                )
            }

            // 3. Dynamic Jellyfin Libraries (or standard categories)
            if (libraries.isNotEmpty()) {
                libraries.forEach { lib ->
                    val (icon, label) = resolveLibraryMeta(lib)
                    val route = if (lib.collectionType?.equals("music", ignoreCase = true) == true) {
                        TvRoute.Music
                    } else {
                        TvRoute.Library(parentId = lib.id, title = lib.name)
                    }
                    list.add(
                        TvNavigationItem(
                            route = route,
                            label = label,
                            icon = icon,
                        )
                    )
                }
            } else {
                list.add(
                    TvNavigationItem(
                        route = TvRoute.Library(),
                        label = "Library",
                        icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                    )
                )
                list.add(
                    TvNavigationItem(
                        route = TvRoute.Music,
                        label = "Music",
                        icon = Icons.Rounded.MusicNote,
                    )
                )
            }

            // 4. Requests (Gated)
            if (hasRequests || isAdmin) {
                list.add(
                    TvNavigationItem(
                        route = TvRoute.Requests,
                        label = "Requests",
                        icon = Icons.AutoMirrored.Rounded.Send,
                        isVisible = hasRequests || isAdmin,
                    )
                )
            }

            // 5. Admin (Admin only)
            if (isAdmin) {
                list.add(
                    TvNavigationItem(
                        route = TvRoute.Admin,
                        label = "Admin",
                        icon = Icons.Rounded.AdminPanelSettings,
                        isVisible = true,
                    )
                )
            }

            // 6. Settings (Placed at the bottom of the list with libraries)
            list.add(
                TvNavigationItem(
                    route = TvRoute.Settings,
                    label = "Settings",
                    icon = Icons.Rounded.Settings,
                )
            )

            return list.filter { it.isVisible }
        }

        fun buildBottomItems(
            hasUnseenNotifications: Boolean = false,
            onExitApp: () -> Unit,
        ): List<TvNavigationItem> = listOf(
            TvNavigationItem(
                route = TvRoute.Search,
                label = "Search",
                icon = Icons.Rounded.Search,
            ),
            TvNavigationItem(
                route = TvRoute.Notifications,
                label = "Notifications",
                icon = Icons.Rounded.Notifications,
                badgeCount = if (hasUnseenNotifications) 1 else 0,
            ),
            TvNavigationItem(
                route = TvRoute.Home,
                label = "Exit App",
                icon = Icons.Rounded.PowerSettingsNew,
                onClick = onExitApp,
            ),
        )

        private fun resolveLibraryMeta(lib: JellyfinLibrary): Pair<ImageVector, String> {
            val type = lib.collectionType?.lowercase().orEmpty()
            val name = lib.name.trim()
            val icon = when {
                type == "movies" || name.contains("movie", ignoreCase = true) -> Icons.Rounded.Movie
                type == "tvshows" || type == "series" || name.contains("show", ignoreCase = true) || name.contains("tv", ignoreCase = true) -> Icons.Rounded.Tv
                type == "boxsets" || name.contains("collection", ignoreCase = true) -> Icons.Rounded.VideoLibrary
                type == "music" || name.contains("music", ignoreCase = true) -> Icons.Rounded.MusicNote
                type == "livetv" || name.contains("live", ignoreCase = true) -> Icons.Rounded.LiveTv
                else -> Icons.Rounded.Folder
            }
            return Pair(icon, name)
        }
    }
}
