package dev.vantafyn.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import dev.vantafyn.core.ui.VantafynSurface
import dev.vantafyn.core.ui.VantafynTheme
import dev.vantafyn.core.ui.tvScreenPadding
import dev.vantafyn.feature.home.VantafynAppContent

class TvMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VantafynTheme {
                VantafynSurface {
                    Box(modifier = androidx.compose.ui.Modifier.padding(tvScreenPadding())) {
                        VantafynAppContent(tv = true)
                    }
                }
            }
        }
    }
}
