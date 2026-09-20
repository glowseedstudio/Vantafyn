package dev.vantafyn.core.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePostureTest {

    @Test
    fun isWideTabletop_returnsFalseWhenNotTabletop() {
        val state = DevicePostureState(
            isTabletop = false,
            screenWidthDp = 800.dp,
        )
        assertFalse(state.isWideTabletop)
    }

    @Test
    fun isWideTabletop_returnsFalseForNarrowFlipClamshell() {
        val state = DevicePostureState(
            isTabletop = true,
            screenWidthDp = 412.dp,
        )
        assertFalse(state.isWideTabletop)
    }

    @Test
    fun isWideTabletop_returnsTrueForWideFoldDisplay() {
        val state = DevicePostureState(
            isTabletop = true,
            screenWidthDp = 760.dp,
        )
        assertTrue(state.isWideTabletop)
    }

    @Test
    fun isWideTabletop_returnsTrueAtThreshold() {
        val state = DevicePostureState(
            isTabletop = true,
            screenWidthDp = 600.dp,
        )
        assertTrue(state.isWideTabletop)
    }
}
