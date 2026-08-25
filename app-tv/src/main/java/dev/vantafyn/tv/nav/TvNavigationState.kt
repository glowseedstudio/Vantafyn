package dev.vantafyn.tv.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class TvNavigationState(
    initialRoute: TvRoute = TvRoute.Home,
) {
    var currentRoute by mutableStateOf(initialRoute)
        private set

    var isSidebarExpanded by mutableStateOf(false)
        private set

    private val backStack = ArrayDeque<TvRoute>()

    fun navigateTo(route: TvRoute) {
        if (currentRoute == route) {
            isSidebarExpanded = false
            return
        }
        backStack.addLast(currentRoute)
        currentRoute = route
        isSidebarExpanded = false
    }

    /**
     * Navigates to a top-level route from the sidebar drawer.
     * Resets the backstack to Home so pressing Back from any top-level destination returns directly to Home.
     */
    fun navigateToFromDrawer(route: TvRoute) {
        if (currentRoute == route) {
            isSidebarExpanded = false
            return
        }
        backStack.clear()
        if (route != TvRoute.Home) {
            backStack.addLast(TvRoute.Home)
        }
        currentRoute = route
        isSidebarExpanded = false
    }

    fun goToHome() {
        backStack.clear()
        currentRoute = TvRoute.Home
        isSidebarExpanded = false
    }

    fun navigateBack(): Boolean {
        if (isSidebarExpanded) {
            isSidebarExpanded = false
            return true
        }
        if (backStack.isNotEmpty()) {
            currentRoute = backStack.removeLast()
            return true
        }
        if (currentRoute != TvRoute.Home) {
            currentRoute = TvRoute.Home
            return true
        }
        return false
    }

    fun expandSidebar() {
        isSidebarExpanded = true
    }

    fun collapseSidebar() {
        isSidebarExpanded = false
    }

    fun toggleSidebar() {
        isSidebarExpanded = !isSidebarExpanded
    }
}

@Composable
fun rememberTvNavigationState(
    initialRoute: TvRoute = TvRoute.Home,
): TvNavigationState = remember {
    TvNavigationState(initialRoute)
}
