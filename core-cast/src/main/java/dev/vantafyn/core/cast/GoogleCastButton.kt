package dev.vantafyn.core.cast

import android.graphics.Color
import android.view.ContextThemeWrapper
import android.widget.ImageButton
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory

@Composable
fun GoogleCastRouteButton(modifier: Modifier = Modifier) {
    if (!VantafynCastFeatureFlags.googleCastEnabled) return
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val themedContext = ContextThemeWrapper(context, R.style.VantafynCastButtonTheme)
            runCatching {
                MediaRouteButton(themedContext).apply {
                    contentDescription = "Cast"
                    CastButtonFactory.setUpMediaRouteButton(themedContext, this)
                }
            }.getOrElse {
                ImageButton(context).apply {
                    contentDescription = "Cast"
                    setImageResource(R.drawable.ic_vantafyn_cast_24)
                    setColorFilter(Color.WHITE)
                    background = null
                    scaleType = ImageView.ScaleType.CENTER
                    setPadding(0, 0, 0, 0)
                }
            }
        },
    )
}
