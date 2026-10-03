package dev.vantafyn.feature.player.games

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatterySaveImportNormalizerTest {
    @Test
    fun rawSavePassesThrough() {
        val bytes = ByteArray(32 * 1024) { 0x5A }

        val result = BatterySaveImportNormalizer.normalize("pokemon.sav", bytes, "gba", "gba")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().convertedFromDsv)
        assertArrayEquals(bytes, result.getOrThrow().bytes)
    }

    @Test
    fun validDesmumeSaveIsConvertedToRawBytes() {
        val raw = ByteArray(128 * 1024) { (it and 0xFF).toByte() }
        val dsv = raw + ByteArray(40)
        writeIntLe(dsv, raw.size, raw.size + 4)
        "|-DESMUME SAVE-|".encodeToByteArray().copyInto(dsv, dsv.size - 16)

        val result = BatterySaveImportNormalizer.normalize("pokemon.dsv", dsv, "nds", "desmume")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().convertedFromDsv)
        assertArrayEquals(raw, result.getOrThrow().bytes)
    }

    @Test
    fun rejectsSaveStatesAndMalformedDsvFiles() {
        val bytes = ByteArray(2048)

        assertTrue(BatterySaveImportNormalizer.normalize("pokemon.sgm", bytes, "gba", "gba").isFailure)
        assertTrue(BatterySaveImportNormalizer.normalize("pokemon.dsv", bytes, "nds", "desmume").isFailure)
    }

    private fun writeIntLe(bytes: ByteArray, value: Int, offset: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value ushr 8).toByte()
        bytes[offset + 2] = (value ushr 16).toByte()
        bytes[offset + 3] = (value ushr 24).toByte()
    }
}
