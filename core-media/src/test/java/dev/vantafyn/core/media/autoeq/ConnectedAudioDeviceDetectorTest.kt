package dev.vantafyn.core.media.autoeq

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectedAudioDeviceDetectorTest {

    @Test
    fun carDeviceNames_classifiedAsCar() {
        val carNames = listOf(
            "Mazda BT",
            "Toyota Touch 2",
            "Ford SYNC",
            "SYNC",
            "Uconnect",
            "Audi MMI",
            "Honda BT",
            "CarAudio",
            "Car Audio",
            "My Car",
            "BMW iDrive",
            "Android Auto",
            "Zlink",
            "Tlink",
            "Subaru Starlink",
            "Chevy MyLink",
            "VW Bluetooth",
            "Nissan Connect",
            "Hyundai Tucson",
            "Kia Sportage",
            "Volvo Sensus",
            "Mercedes MBUX",
            "Tesla Model 3",
            "Lexus Multimedia",
            "BT_CAR",
            "CarPlay Box",
            "In-Car BT",
            "Autokit",
            "Carlinkit",
        )

        for (name in carNames) {
            assertTrue("Expected '$name' to be recognized as a car device", ConnectedAudioDeviceDetector.isCarDeviceName(name))
        }
    }

    @Test
    fun headphoneDeviceNames_notClassifiedAsCar() {
        val headphoneNames = listOf(
            "WH-1000XM4",
            "Sony WH-1000XM5",
            "AirPods Pro",
            "AirPods Max",
            "Bose QuietComfort 45",
            "Bose QC35 II",
            "Galaxy Buds2 Pro",
            "Sennheiser Momentum 4",
            "Sennheiser HD 650",
            "Moondrop Blessing 2",
            "Audio-Technica ATH-M50xBT2",
            "Pixel Buds Pro",
            "Anker Soundcore Space Q45",
            "LE-WH-1000XM4",
            "Nothing Ear (2)",
        )

        for (name in headphoneNames) {
            assertFalse("Expected '$name' NOT to be recognized as a car device", ConnectedAudioDeviceDetector.isCarDeviceName(name))
        }
    }

    @Test
    fun speakerDeviceNames_classifiedCorrectly() {
        val speakerNames = listOf(
            "JBL Flip 6 Speaker",
            "Bose SoundLink Mini",
            "Living Room Soundbar",
            "Portable Bluetooth Speaker",
            "Home Theater System",
        )

        for (name in speakerNames) {
            assertTrue("Expected '$name' to be recognized as a speaker device", ConnectedAudioDeviceDetector.isSpeakerDeviceName(name))
        }

        val nonSpeakerNames = listOf(
            "Sony WH-1000XM4",
            "AirPods Pro",
            "Bose QC45",
            "Sennheiser HD 600",
        )

        for (name in nonSpeakerNames) {
            assertFalse("Expected '$name' NOT to be recognized as a speaker device", ConnectedAudioDeviceDetector.isSpeakerDeviceName(name))
        }
    }
}
