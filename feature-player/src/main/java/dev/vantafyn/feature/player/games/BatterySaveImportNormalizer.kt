package dev.vantafyn.feature.player.games

/**
 * Converts only documented, lossless emulator battery-save wrappers to the raw bytes expected by
 * Libretro/EmulatorJS. It never attempts to interpret save states or compressed save formats.
 */
object BatterySaveImportNormalizer {
    private const val maxSaveBytes = 32 * 1024 * 1024
    private const val desmumeFooterBytes = 40
    private val desmumeCookie = "|-DESMUME SAVE-|".encodeToByteArray()
    private val noGbaHeader = "NocashGbaBackupMediaSavDataFile".encodeToByteArray()
    private val saveStateExtensions = setOf("state", "savestate", "ss0", "ss1", "sst", "slot", "zip", "sgm", "ds0", "ds1", "ds2", "ds3", "ds4", "ds5", "ds6", "ds7", "ds8", "ds9")

    data class Result(val bytes: ByteArray, val convertedFromDsv: Boolean)

    fun normalize(fileName: String?, input: ByteArray, systemId: String, core: String): kotlin.Result<Result> = runCatching {
        require(input.size in 512..maxSaveBytes) {
            "This is not a supported battery save. Choose a file between 512 bytes and 32 MB."
        }

        val extension = fileName.orEmpty().substringAfterLast('.', "").lowercase()
        require(extension !in saveStateExtensions) {
            "That looks like an emulator save state, not a battery save. Export the game's battery save instead."
        }
        require(!input.startsWith(noGbaHeader)) {
            "No${'$'}GBA compressed saves cannot be imported directly. Export a raw .sav file first."
        }
        require(extension != "duc") {
            "Action Replay .duc saves cannot be imported directly. Export a raw .sav file first."
        }

        val isDs = "$systemId $core".lowercase().let { it.contains("nds") || it.contains("nintendo ds") || it == "ds" }
        val isPsx = "$systemId $core".lowercase().let { it.contains("psx") || it.contains("pcsx") || it.contains("playstation") }

        when {
            hasDesmumeFooter(input) -> {
                require(isDs) { "A DeSmuME/DraStic .dsv save can only be imported for a Nintendo DS game." }
                val rawLength = readIntLe(input, input.size - desmumeFooterBytes + 4)
                require(rawLength in 512..(input.size - desmumeFooterBytes)) {
                    "This .dsv file has an invalid DeSmuME footer and was not imported."
                }
                Result(input.copyOfRange(0, rawLength), convertedFromDsv = true)
            }
            extension == "dsv" -> {
                throw IllegalArgumentException("This .dsv file is not a valid DeSmuME/DraStic battery save. Export a raw .sav file instead.")
            }
            extension in setOf("mcr", "mcd") -> {
                require(isPsx) { "Memory-card files (.mcr/.mcd) can only be imported for PlayStation games." }
                require(input.size == 128 * 1024) {
                    "This PlayStation memory card is not a standard 128 KB raw card. Export it as a raw .mcr or Libretro .srm file first."
                }
                Result(input, convertedFromDsv = false)
            }
            extension.isBlank() || extension in setOf("sav", "srm", "sram") -> {
                if (isDs && input.size > 16 * 1024 * 1024) {
                    throw IllegalArgumentException("That DS battery save is too large to load safely.")
                }
                Result(input, convertedFromDsv = false)
            }
            else -> throw IllegalArgumentException(
                "Unsupported save format .$extension. Choose a raw .sav, .srm, or .sram file" +
                    if (isDs) ", or a valid DeSmuME/DraStic .dsv file." else ".",
            )
        }
    }

    private fun hasDesmumeFooter(bytes: ByteArray): Boolean =
        bytes.size >= desmumeFooterBytes && bytes.copyOfRange(bytes.size - desmumeCookie.size, bytes.size).contentEquals(desmumeCookie)

    private fun readIntLe(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}
