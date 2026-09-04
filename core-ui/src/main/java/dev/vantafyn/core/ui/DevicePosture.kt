package dev.vantafyn.core.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.collectLatest

@Immutable
data class DevicePostureState(
    val isTabletop: Boolean = false,
    val isBookMode: Boolean = false,
    val isSeparating: Boolean = false,
    val hingeBounds: Rect = Rect(),
    val hingeThicknessDp: Dp = 0.dp,
    val topHalfHeightDp: Dp = 0.dp,
    val bottomHalfHeightDp: Dp = 0.dp,
)

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun rememberDevicePosture(): DevicePostureState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var postureState by remember { mutableStateOf(DevicePostureState()) }

    if (activity == null) {
        return postureState
    }

    LaunchedEffect(activity, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val tracker = WindowInfoTracker.getOrCreate(activity)
            tracker.windowLayoutInfo(activity).collectLatest { layoutInfo ->
                val foldingFeature = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()

                if (foldingFeature == null) {
                    postureState = DevicePostureState()
                    return@collectLatest
                }

                val isHalfOpened = foldingFeature.state == FoldingFeature.State.HALF_OPENED
                val isSeparating = foldingFeature.isSeparating
                val isHorizontal = foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL
                val isVertical = foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL

                val isTabletop = (isHalfOpened || isSeparating) && isHorizontal
                val isBookMode = (isHalfOpened || isSeparating) && isVertical

                val bounds = foldingFeature.bounds
                val thicknessDp = with(density) {
                    if (isHorizontal) bounds.height().toDp() else bounds.width().toDp()
                }

                val topHeightDp = with(density) { bounds.top.toDp() }
                val screenHeightDp = configuration.screenHeightDp.dp
                val bottomHeightDp = (screenHeightDp - with(density) { bounds.bottom.toDp() }).coerceAtLeast(0.dp)

                postureState = DevicePostureState(
                    isTabletop = isTabletop,
                    isBookMode = isBookMode,
                    isSeparating = isSeparating,
                    hingeBounds = bounds,
                    hingeThicknessDp = thicknessDp,
                    topHalfHeightDp = if (topHeightDp > 0.dp) topHeightDp else screenHeightDp / 2,
                    bottomHalfHeightDp = if (bottomHeightDp > 0.dp) bottomHeightDp else screenHeightDp / 2,
                )
            }
        }
    }

    return postureState
}
