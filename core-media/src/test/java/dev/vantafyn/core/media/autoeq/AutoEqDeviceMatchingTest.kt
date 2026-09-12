package dev.vantafyn.core.media.autoeq

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoEqDeviceMatchingTest {

    private val samplePresets = listOf(
        AutoEqPreset(
            id = "sony_wh1000xm4_oratory",
            name = "WH-1000XM4",
            brand = "Sony",
            type = "over-ear",
            source = "oratory1990",
            preamp = -5.5f,
            frequencies = listOf(1000),
            gains = listOf(0f),
        ),
        AutoEqPreset(
            id = "sony_wh1000xm4_rtings",
            name = "WH-1000XM4",
            brand = "Sony",
            type = "over-ear",
            source = "rtings",
            preamp = -4.0f,
            frequencies = listOf(1000),
            gains = listOf(0f),
        ),
        AutoEqPreset(
            id = "sony_wh1000xm4_modded",
            name = "WH-1000XM4 (modded earpads)",
            brand = "Sony",
            type = "over-ear",
            source = "oratory1990",
            preamp = -6.0f,
            frequencies = listOf(1000),
            gains = listOf(0f),
        ),
        AutoEqPreset(
            id = "sony_wf1000xm4",
            name = "WF-1000XM4",
            brand = "Sony",
            type = "in-ear",
            source = "crinacle",
            preamp = -3.2f,
            frequencies = listOf(1000),
            gains = listOf(0f),
        ),
        AutoEqPreset(
            id = "apple_airpods_pro",
            name = "AirPods Pro",
            brand = "Apple",
            type = "in-ear",
            source = "oratory1990",
            preamp = -4.8f,
            frequencies = listOf(1000),
            gains = listOf(0f),
        ),
        AutoEqPreset(
            id = "bose_qc45",
            name = "QuietComfort 45",
            brand = "Bose",
            type = "over-ear",
            source = "rtings",
            preamp = -4.2f,
            frequencies = listOf(1000),
            gains = listOf(0f),
        ),
    )

    @Test
    fun cleanDeviceNameStripsPrefixesAndSuffixes() {
        assertEquals("WH-1000XM4", AutoEqRepository.cleanDeviceName("LE-WH-1000XM4"))
        assertEquals("WH-1000XM4", AutoEqRepository.cleanDeviceName("WH-1000XM4 Hands-Free"))
        assertEquals("Alex AirPods Pro", AutoEqRepository.cleanDeviceName("Alex's AirPods Pro"))
        assertEquals("Bose QC45", AutoEqRepository.cleanDeviceName("Bose QC45 Wireless Stereo"))
        assertEquals("", AutoEqRepository.cleanDeviceName(""))
    }

    @Test
    fun matchesBluetoothDeviceToExactModel() {
        val matches = AutoEqRepository.rankPresetsForDevice(samplePresets, "LE_WH-1000XM4")
        assertTrue("Matches should not be empty", matches.isNotEmpty())
        assertEquals("WH-1000XM4", matches.first().name)
        assertEquals("Sony", matches.first().brand)
    }

    @Test
    fun prioritizesPreferredSourcesAndDemotesModded() {
        val matches = AutoEqRepository.rankPresetsForDevice(samplePresets, "Sony WH-1000XM4")
        assertTrue("Matches should not be empty", matches.isNotEmpty())
        val top = matches.first()
        assertEquals("WH-1000XM4", top.name)
        assertEquals("oratory1990", top.source)
    }

    @Test
    fun matchesAirPodsProWithPossessiveName() {
        val matches = AutoEqRepository.rankPresetsForDevice(samplePresets, "Glowseed's AirPods Pro")
        assertTrue("Matches should not be empty", matches.isNotEmpty())
        assertEquals("AirPods Pro", matches.first().name)
        assertEquals("Apple", matches.first().brand)
    }

    @Test
    fun emptyOrUnmatchedReturnsEmptyList() {
        val emptyMatches = AutoEqRepository.rankPresetsForDevice(samplePresets, "")
        assertTrue(emptyMatches.isEmpty())

        val unmatched = AutoEqRepository.rankPresetsForDevice(samplePresets, "NonExistentModelX999")
        assertTrue(unmatched.isEmpty())
    }
}
