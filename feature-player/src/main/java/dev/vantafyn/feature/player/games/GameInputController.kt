package dev.vantafyn.feature.player.games

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

enum class RetroButton(val id: String) {
    Up("up"),
    Down("down"),
    Left("left"),
    Right("right"),
    A("a"),
    B("b"),
    X("x"),
    Y("y"),
    L1("l1"),
    R1("r1"),
    L2("l2"),
    R2("r2"),
    Z("z"),
    CUp("c_up"),
    CDown("c_down"),
    CLeft("c_left"),
    CRight("c_right"),
    Start("start"),
    Select("select"),
    Menu("menu");
}

class GameInputController(
    private val onButtonEvent: (RetroButton, Boolean) -> Unit,
    private val onMenuTriggered: () -> Unit,
    private val onAxisEvent: ((String, Float) -> Unit)? = null,
) {
    private var lastHatX = 0f
    private var lastHatY = 0f
    private var lastStickX = 0f
    private var lastStickY = 0f

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val isDown = event.action == KeyEvent.ACTION_DOWN

        // Intercept Back button or Menu button to open Pause HUD
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_MENU) {
            if (isDown) onMenuTriggered()
            return true
        }

        val button = mapKeyCodeToRetroButton(event.keyCode)
        if (button != null) {
            onButtonEvent(button, isDown)
            return true
        }

        return false
    }

    fun handleMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) == 0 &&
            (event.source and InputDevice.SOURCE_GAMEPAD) == 0
        ) {
            return false
        }

        // Process D-pad Hat axes
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        processAxisPair(hatX, hatY, lastHatX, lastHatY)
        lastHatX = hatX
        lastHatY = hatY

        // Process Left Analog Stick
        val stickX = event.getAxisValue(MotionEvent.AXIS_X)
        val stickY = event.getAxisValue(MotionEvent.AXIS_Y)
        val deadzone = 0.35f

        onAxisEvent?.invoke("left_x", stickX)
        onAxisEvent?.invoke("left_y", stickY)

        val normX = when {
            stickX > deadzone -> 1f
            stickX < -deadzone -> -1f
            else -> 0f
        }
        val normY = when {
            stickY > deadzone -> 1f
            stickY < -deadzone -> -1f
            else -> 0f
        }

        processAxisPair(normX, normY, lastStickX, lastStickY)
        lastStickX = normX
        lastStickY = normY

        return true
    }

    private fun processAxisPair(currX: Float, currY: Float, prevX: Float, prevY: Float) {
        if (currX != prevX) {
            if (currX > 0.5f) onButtonEvent(RetroButton.Right, true)
            else if (prevX > 0.5f) onButtonEvent(RetroButton.Right, false)

            if (currX < -0.5f) onButtonEvent(RetroButton.Left, true)
            else if (prevX < -0.5f) onButtonEvent(RetroButton.Left, false)
        }

        if (currY != prevY) {
            if (currY > 0.5f) onButtonEvent(RetroButton.Down, true)
            else if (prevY > 0.5f) onButtonEvent(RetroButton.Down, false)

            if (currY < -0.5f) onButtonEvent(RetroButton.Up, true)
            else if (prevY < -0.5f) onButtonEvent(RetroButton.Up, false)
        }
    }

    private fun mapKeyCodeToRetroButton(keyCode: Int): RetroButton? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> RetroButton.Up
        KeyEvent.KEYCODE_DPAD_DOWN -> RetroButton.Down
        KeyEvent.KEYCODE_DPAD_LEFT -> RetroButton.Left
        KeyEvent.KEYCODE_DPAD_RIGHT -> RetroButton.Right

        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER -> RetroButton.A
        KeyEvent.KEYCODE_BUTTON_B -> RetroButton.B
        KeyEvent.KEYCODE_BUTTON_X -> RetroButton.X
        KeyEvent.KEYCODE_BUTTON_Y -> RetroButton.Y

        KeyEvent.KEYCODE_BUTTON_L1 -> RetroButton.L1
        KeyEvent.KEYCODE_BUTTON_R1 -> RetroButton.R1
        KeyEvent.KEYCODE_BUTTON_L2 -> RetroButton.L2
        KeyEvent.KEYCODE_BUTTON_R2 -> RetroButton.R2

        KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER -> RetroButton.Start
        KeyEvent.KEYCODE_BUTTON_SELECT -> RetroButton.Select
        KeyEvent.KEYCODE_BUTTON_MODE -> RetroButton.Menu

        else -> null
    }

    companion object {
        fun isGamepadConnected(): Boolean {
            val deviceIds = InputDevice.getDeviceIds()
            for (id in deviceIds) {
                val dev = InputDevice.getDevice(id) ?: continue
                val sources = dev.sources
                if ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                ) {
                    return true
                }
            }
            return false
        }
    }
}

@Composable
fun rememberGameInputController(
    onButtonEvent: (RetroButton, Boolean) -> Unit,
    onMenuTriggered: () -> Unit,
    onAxisEvent: ((String, Float) -> Unit)? = null,
): GameInputController = remember {
    GameInputController(onButtonEvent, onMenuTriggered, onAxisEvent)
}
