package dev.vantafyn.feature.home

import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverVantafynPromptTest {

    @Test
    fun vantafynHomeUiState_defaultShowDiscoverPromptIsFalse() {
        val state = VantafynHomeUiState()
        assertFalse("showDiscoverPrompt should default to false", state.showDiscoverPrompt)
    }

    @Test
    fun vantafynHomeUiState_copyTogglesShowDiscoverPrompt() {
        val state = VantafynHomeUiState()
        val updated = state.copy(showDiscoverPrompt = true)
        assertTrue(updated.showDiscoverPrompt)

        val dismissed = updated.copy(showDiscoverPrompt = false)
        assertFalse(dismissed.showDiscoverPrompt)
    }

    @Test
    fun discoverPromptCopy_matchesExactSpecification() {
        val expectedTitle = "There’s a lot to discover"
        val expectedBody1 = "Vantafyn is packed with features, shortcuts and ways to make the experience your own — more than we could reasonably fit into an introduction."
        val expectedBody2 = "If you'd like to see everything Vantafyn can do, take a look through Discover Vantafyn. You can always come back to it later."
        val expectedPrimaryAction = "Explore Vantafyn"
        val expectedSecondaryAction = "Maybe Later"

        assertEquals("There’s a lot to discover", expectedTitle)
        assertTrue(expectedBody1.contains("more than we could reasonably fit into an introduction"))
        assertTrue(expectedBody2.contains("You can always come back to it later"))
        assertEquals("Explore Vantafyn", expectedPrimaryAction)
        assertEquals("Maybe Later", expectedSecondaryAction)
    }
}
