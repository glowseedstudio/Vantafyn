package dev.vantafyn.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.vantafyn.core.ui.VantafynSurface
import dev.vantafyn.core.ui.VantafynTheme
import dev.vantafyn.feature.home.VantafynAppContent

class MobileMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VantafynTheme {
                VantafynSurface {
                    VantafynAppContent(tv = false)
                }
            }
        }
    }
}
