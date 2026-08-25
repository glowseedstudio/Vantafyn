package dev.vantafyn.tv.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector

enum class TvSettingsCategory(
    val label: String,
    val icon: ImageVector,
) {
    Appearance("Appearance", Icons.Rounded.AutoAwesome),
    Profile("Profile", Icons.Rounded.AccountCircle),
    Playback("Playback", Icons.Rounded.PlayCircle),
    Permissions("Permissions", Icons.Rounded.Security),
    Vantafyn("Vantafyn", Icons.Rounded.Tune),
    About("About", Icons.Rounded.Info),
}

fun TvSettingsCategory.toNavigationItem(): TvNavigationItem =
    TvNavigationItem(
        route = TvRoute.Settings,
        label = label,
        icon = icon,
    )
