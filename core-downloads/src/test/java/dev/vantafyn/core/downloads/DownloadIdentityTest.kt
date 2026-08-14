package dev.vantafyn.core.downloads

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadIdentityTest {
    @Test
    fun sameItemOnDifferentServersHasDifferentStableKey() {
        val first = DownloadIdentity(
            serverId = "server-a",
            userId = "user",
            itemId = "item",
            mediaSourceId = "source",
        )
        val second = DownloadIdentity(
            serverId = "server-b",
            userId = "user",
            itemId = "item",
            mediaSourceId = "source",
        )

        assertNotEquals(first.stableKey, second.stableKey)
    }

    @Test
    fun sameItemOnSameServerForDifferentUsersHasDifferentStableKey() {
        val first = DownloadIdentity(
            serverId = "server",
            userId = "user-a",
            itemId = "item",
            mediaSourceId = "source",
        )
        val second = DownloadIdentity(
            serverId = "server",
            userId = "user-b",
            itemId = "item",
            mediaSourceId = "source",
        )

        assertNotEquals(first.stableKey, second.stableKey)
    }

    @Test
    fun blankIdentityPartsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DownloadIdentity(
                serverId = "server",
                userId = "",
                itemId = "item",
                mediaSourceId = "source",
            )
        }
    }

    @Test
    fun terminalStatesCannotTransitionToCompleted() {
        assertTrue(DownloadState.Failed.isTerminal())
        assertTrue(!DownloadState.Failed.canTransitionTo(DownloadState.Completed))
        assertTrue(!DownloadState.Cancelled.canTransitionTo(DownloadState.Completed))
    }
}
