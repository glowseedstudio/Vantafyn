package dev.vantafyn.core.media.music

enum class MusicStreamingQuality(
    val label: String,
    val shortLabel: String,
    val description: String,
    val maxBitrateBps: Int?, // null = Lossless / uncapped direct play
    val audioCodec: String,
    val container: List<String>,
) {
    Lossless(
        label = "Lossless (Original Master)",
        shortLabel = "Lossless",
        description = "Bit-perfect FLAC, ALAC, WAV & Hi-Res audio directly streamed without compression",
        maxBitrateBps = null,
        audioCodec = "flac,alac,wav,aac,mp3,opus,vorbis",
        container = listOf("flac", "alac", "wav", "m4a", "aac", "mp3", "opus", "ogg", "webma", "webm"),
    ),
    High(
        label = "High Quality (320 kbps)",
        shortLabel = "320 kbps",
        description = "Transparent high-fidelity sound, ideal for Bluetooth and mobile listening",
        maxBitrateBps = 320_000,
        audioCodec = "aac,mp3,opus,vorbis",
        container = listOf("mp3", "aac", "m4a", "opus", "ogg"),
    ),
    Medium(
        label = "Medium Quality (192 kbps)",
        shortLabel = "192 kbps",
        description = "Great balance of audio clarity and reduced data usage",
        maxBitrateBps = 192_000,
        audioCodec = "aac,mp3,opus,vorbis",
        container = listOf("mp3", "aac", "m4a", "opus", "ogg"),
    ),
    DataSaver(
        label = "Data Saver (128 kbps)",
        shortLabel = "128 kbps",
        description = "Minimal data consumption for low-bandwidth or tight mobile data caps",
        maxBitrateBps = 128_000,
        audioCodec = "aac,mp3,opus,vorbis",
        container = listOf("mp3", "aac", "m4a", "opus", "ogg"),
    );

    val maxBitrateKbps: Int?
        get() = maxBitrateBps?.let { it / 1000 }

    companion object {
        val DefaultWifi = Lossless
        val DefaultCellular = High
    }
}
