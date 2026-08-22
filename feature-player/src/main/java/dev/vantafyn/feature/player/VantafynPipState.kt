package dev.vantafyn.feature.player

import android.app.PictureInPictureParams
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object VantafynPipState {
    private val _isInPictureInPicture = MutableStateFlow(false)
    val isInPictureInPicture: StateFlow<Boolean> = _isInPictureInPicture

    var isActive by mutableStateOf(false)
        private set

    fun update(active: Boolean) {
        _isInPictureInPicture.value = active
        isActive = active
    }
}
