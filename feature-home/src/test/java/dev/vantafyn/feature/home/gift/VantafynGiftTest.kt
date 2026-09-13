package dev.vantafyn.feature.home.gift

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class VantafynGiftTest {

    @Test
    fun giftSerializationAndDeserialization_preservesAllFields() {
        val gift = VantafynCodeGift(
            id = "gift-12345",
            senderId = UUID.randomUUID(),
            senderName = "AdminGlowseed",
            senderAvatarUrl = "https://example.com/avatar.png",
            recipientUserId = UUID.randomUUID(),
            recipientName = "Alice",
            franchiseCode = "MORDOR",
            franchiseTitle = "Middle-earth: The Legendarium",
            franchiseBadge = "ONE RING | MIDDLE-EARTH",
            franchiseIcon = "💍",
            accentColorHex = 0xFFD4AF37,
            note = "Enjoy the journey to Mount Doom!",
            timestamp = 1773000000000L,
            isClaimed = false,
            posterUrl = "https://image.tmdb.org/t/p/w500/6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg",
        )

        val serialized = gift.toSerializedMessage()
        assertTrue(serialized.startsWith("[vantafyn_gift|"))
        assertTrue(serialized.endsWith("]"))

        val deserialized = VantafynCodeGift.fromSerializedMessage(serialized)
        assertNotNull(deserialized)
        assertEquals(gift.id, deserialized!!.id)
        assertEquals(gift.senderId, deserialized.senderId)
        assertEquals(gift.senderName, deserialized.senderName)
        assertEquals(gift.senderAvatarUrl, deserialized.senderAvatarUrl)
        assertEquals(gift.recipientUserId, deserialized.recipientUserId)
        assertEquals(gift.recipientName, deserialized.recipientName)
        assertEquals(gift.franchiseCode, deserialized.franchiseCode)
        assertEquals(gift.franchiseTitle, deserialized.franchiseTitle)
        assertEquals(gift.franchiseBadge, deserialized.franchiseBadge)
        assertEquals(gift.franchiseIcon, deserialized.franchiseIcon)
        assertEquals(gift.accentColorHex, deserialized.accentColorHex)
        assertEquals(gift.note, deserialized.note)
        assertEquals(gift.timestamp, deserialized.timestamp)
        assertEquals(gift.posterUrl, deserialized.posterUrl)
        assertFalse(deserialized.isClaimed)
    }

    @Test
    fun giftDeserialization_handlesInvalidPayloadsGracefully() {
        assertNull(VantafynCodeGift.fromSerializedMessage("hello world"))
        assertNull(VantafynCodeGift.fromSerializedMessage("[vantafyn_gift|"))
        assertNull(VantafynCodeGift.fromSerializedMessage("[vantafyn_gift|not-a-json]"))
        assertNull(VantafynCodeGift.fromSerializedMessage(""))
    }

    @Test
    fun giftFranchisesCatalog_containsAllSixteenFranchisesWithValidCodes() {
        val all = VantafynGiftFranchises.all
        assertEquals(16, all.size)

        // Unique codes
        val codes = all.map { it.code }.toSet()
        assertEquals(16, codes.size)

        // Validate key expected franchises
        val expectedCodes = listOf(
            "MCU", "SAW", "R-EVIL", "POTTER",
            "HUNGER", "SCREAM", "MATRIX", "JUMANJI",
            "JURASSIC", "PIRATES", "POKEMON", "SCARY",
            "TWILIGHT", "UNDERWORLD", "X-MEN", "MORDOR"
        )
        for (expected in expectedCodes) {
            assertTrue("Expected code $expected to be present", codes.contains(expected))
        }

        // Validate movie counts are positive, and poster paths/URLs are valid
        for (franchise in all) {
            assertTrue(franchise.movieCount > 0)
            assertTrue(franchise.title.isNotBlank())
            assertTrue(franchise.badge.isNotBlank())
            assertTrue(franchise.icon.isNotBlank())
            assertTrue(franchise.posterPath.isNotBlank())
            assertTrue(franchise.posterUrl.startsWith("https://image.tmdb.org/t/p/w500/"))
        }

        // Specifically verify MCU is at 60+ films (matches catalog size of 62)
        val mcu = all.first { it.code == "MCU" }
        assertEquals(dev.vantafyn.feature.home.mcu.McuMoviesCatalog.movies.size, mcu.movieCount)
        assertTrue("MCU film count should be 60+, got ${mcu.movieCount}", mcu.movieCount >= 60)
    }

    @Test
    fun giftPayloadInSocialSnippet_formatsToHumanFriendlyMessage() {
        val gift = VantafynCodeGift(
            id = "gift-mcu",
            senderId = UUID.randomUUID(),
            senderName = "Admin",
            senderAvatarUrl = null,
            recipientUserId = UUID.randomUUID(),
            recipientName = "Bob",
            franchiseCode = "MCU",
            franchiseTitle = "Marvel Cinematic Universe",
            franchiseBadge = "MARVEL | STUDIOS",
            franchiseIcon = "🛡️",
            accentColorHex = 0xFFE23636,
        )
        val snippet = dev.vantafyn.core.jellyfin.formatSocialSnippet(gift.toSerializedMessage())
        assertEquals("🎁 Special Gift: Marvel Cinematic Universe", snippet)
    }
}
