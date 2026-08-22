package dev.vantafyn.core.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class JellyfinSocialRepositoryTest {

    @Test
    fun testSocialModelsInstantiation() {
        val userId = UUID.randomUUID()
        val friend = JellyfinFriend(
            userId = userId,
            username = "alex",
            displayName = "Alex M.",
            rankName = "Film Buff",
            rankTier = 3,
            currentScore = 450,
            isOnline = true,
            currentlyWatching = "Inception (2010)",
        )
        assertEquals("alex", friend.username)
        assertEquals("Alex M.", friend.displayName)
        assertEquals(3, friend.rankTier)
        assertTrue(friend.isOnline)
        assertEquals("Inception (2010)", friend.currentlyWatching)

        val messageId = UUID.randomUUID().toString()
        val message = JellyfinSocialMessage(
            messageId = messageId,
            conversationId = "conv-123",
            senderId = userId,
            senderName = "Alex M.",
            recipientId = UUID.randomUUID(),
            content = "You have to watch this!",
            isRead = false,
            isFromSelf = false,
        )
        assertEquals("conv-123", message.conversationId)
        assertEquals("You have to watch this!", message.content)
        assertFalse(message.isFromSelf)
        assertFalse(message.isRead)

        val conversation = JellyfinSocialConversation(
            conversationId = "conv-123",
            peerUserId = userId,
            peerName = "Alex M.",
            unreadCount = 2,
            lastMessageText = "You have to watch this!",
        )
        assertEquals(2, conversation.unreadCount)
        assertEquals(userId, conversation.peerUserId)
    }

    @Test
    fun testSocialSummaryDefaults() {
        val summary = JellyfinSocialSummary(
            friendCount = 5,
            incomingRequestCount = 2,
            outgoingRequestCount = 1,
            unreadMessageCount = 3,
        )
        assertEquals(5, summary.friendCount)
        assertEquals(2, summary.incomingRequestCount)
        assertEquals(1, summary.outgoingRequestCount)
        assertEquals(3, summary.unreadMessageCount)
    }
}
