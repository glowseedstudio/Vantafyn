package dev.vantafyn.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.vantafyn.core.ui.VantafynSurface
import dev.vantafyn.core.ui.VantafynTheme
import dev.vantafyn.core.ui.tvScreenPadding
import dev.vantafyn.feature.home.LoginPlaceholderScreen
import dev.vantafyn.feature.home.ServerAddressScreen
import dev.vantafyn.feature.home.SplashScreen
import dev.vantafyn.feature.home.TvHomePlaceholder
import kotlinx.coroutines.delay

class TvMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VantafynTheme {
                VantafynSurface {
                    TvAppShell()
                }
            }
        }
    }
}

@Composable
private fun TvAppShell() {
    var route by remember { mutableStateOf(TvRoute.Splash) }

    LaunchedEffect(Unit) {
        delay(900)
        route = TvRoute.Server
    }

    Box(modifier = Modifier.padding(tvScreenPadding())) {
        when (route) {
            TvRoute.Splash -> SplashScreen()
            TvRoute.Server -> ServerAddressScreen(onContinue = { route = TvRoute.Login })
            TvRoute.Login -> LoginPlaceholderScreen(onContinue = { route = TvRoute.Home })
            TvRoute.Home -> TvHomePlaceholder()
        }
    }
}

private enum class TvRoute {
    Splash,
    Server,
    Login,
    Home,
}
