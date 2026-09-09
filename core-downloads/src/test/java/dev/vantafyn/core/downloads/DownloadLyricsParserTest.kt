package dev.vantafyn.core.downloads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadLyricsParserTest {

    @Test
    fun parsesManifestJsonWithSyncedLyrics() {
        val manifest = DownloadOfflineManifest(
            itemId = "test-item",
            title = "Test Song",
            generatedAtMillis = 1000L,
            lyrics = DownloadOfflineLyrics(
                plainText = "Line one\nLine two",
                syncedLines = listOf(
                    DownloadOfflineLyricLine(startMs = 5000L, text = "Line one"),
                    DownloadOfflineLyricLine(startMs = 10500L, text = "Line two"),
                ),
            ),
        )
        val json = manifest.toJsonString()
        val parsed = parseOfflineLyrics(json)

        assertNotNull(parsed)
        assertEquals("Line one\nLine two", parsed?.plainText)
        assertEquals(2, parsed?.syncedLines?.size)
        assertEquals(5000L, parsed?.syncedLines?.get(0)?.startMs)
        assertEquals("Line one", parsed?.syncedLines?.get(0)?.text)
        assertEquals(10500L, parsed?.syncedLines?.get(1)?.startMs)
        assertEquals("Line two", parsed?.syncedLines?.get(1)?.text)
    }

    @Test
    fun parsesDirectLyricsJsonObject() {
        val json = """
            {
                "plainText": "First verse line",
                "syncedLines": [
                    {"startMs": 1200, "text": "First verse line"}
                ]
            }
        """.trimIndent()
        val parsed = parseOfflineLyrics(json)

        assertNotNull(parsed)
        assertEquals("First verse line", parsed?.plainText)
        assertEquals(1, parsed?.syncedLines?.size)
        assertEquals(1200L, parsed?.syncedLines?.first()?.startMs)
        assertEquals("First verse line", parsed?.syncedLines?.first()?.text)
    }

    @Test
    fun parsesStandardLrcLyrics() {
        val lrc = """
            [ti:Song Title]
            [ar:Artist Name]
            [00:04.50]Opening intro line
            [00:12.100]Second verse line
            [01:05.00]Chorus line starts here
        """.trimIndent()

        val parsed = parseOfflineLyrics(lrc)
        assertNotNull(parsed)
        val lines = parsed?.syncedLines.orEmpty()
        assertEquals(3, lines.size)

        assertEquals(4500L, lines[0].startMs)
        assertEquals("Opening intro line", lines[0].text)

        assertEquals(12100L, lines[1].startMs)
        assertEquals("Second verse line", lines[1].text)

        assertEquals(65000L, lines[2].startMs)
        assertEquals("Chorus line starts here", lines[2].text)

        assertTrue(parsed?.plainText?.contains("Opening intro line") == true)
        assertTrue(parsed?.plainText?.contains("Chorus line starts here") == true)
    }

    @Test
    fun handlesEmptyOrBlankInput() {
        assertNull(parseOfflineLyrics(""))
        assertNull(parseOfflineLyrics("   \n\t  "))
    }

    @Test
    fun returnsNullWhenManifestHasNoLyrics() {
        val json = """
            {"itemId":"5176b6ff-9d0d-f9a7-c79b-9fc33f7afc95","title":"2 Be Loved (Am I Ready)","generatedAtMillis":1788873242710,"chaptersAvailable":false,"trickplayAvailable":false,"subtitles":[],"segments":[],"lyrics":null}
        """.trimIndent()
        assertNull(parseOfflineLyrics(json))
    }
}
