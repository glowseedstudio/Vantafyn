package dev.vantafyn.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.vantafyn.core.ui.VantafynSurface
import dev.vantafyn.core.ui.VantafynTheme
import dev.vantafyn.feature.home.LoginPlaceholderScreen
import dev.vantafyn.feature.home.MobileHomePlaceholder
import dev.vantafyn.feature.home.ServerAddressScreen
import dev.vantafyn.feature.home.SplashScreen
import kotlinx.coroutines.delay

class MobileMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VantafynTheme {
                VantafynSurface {
                    MobileAppShell()
                }
            }
        }
    }
}

@Composable
private fun MobileAppShell() {
    var route by remember { mutableStateOf(MobileRoute.Splash) }

    LaunchedEffect(Unit) {
        delay(700)
        route = MobileRoute.Server
    }

    when (route) {
        MobileRoute.Splash -> SplashScreen()
        MobileRoute.Server -> ServerAddressScreen(onContinue = { route = MobileRoute.Login })
        MobileRoute.Login -> LoginPlaceholderScreen(onContinue = { route = MobileRoute.Home })
        MobileRoute.Home -> MobileHomePlaceholder()
    }
}

private enum class MobileRoute {
    Splash,
    Server,
    Login,
    Home,
}
