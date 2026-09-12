package dev.vantafyn.core.media.autoeq

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

class AutoEqRepository(private val context: Context) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var cachedPresets: List<AutoEqPreset>? = null

    suspend fun loadPresets(): List<AutoEqPreset> = withContext(Dispatchers.IO) {
        cachedPresets?.let { return@withContext it }

        mutex.withLock {
            cachedPresets?.let { return@withLock it }

            val presets = runCatching {
                val jsonString = appContext.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)
                val list = ArrayList<AutoEqPreset>(jsonArray.length())

                val defaultFrequencies = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
                val seenIds = HashSet<String>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val freqArray = obj.optJSONArray("frequencies")
                    val frequencies = if (freqArray != null && freqArray.length() > 0) {
                        val freqs = ArrayList<Int>(freqArray.length())
                        for (f in 0 until freqArray.length()) {
                            freqs.add(freqArray.getInt(f))
                        }
                        freqs
                    } else {
                        defaultFrequencies
                    }

                    val gainsArray = obj.getJSONArray("gains")
                    val gains = ArrayList<Float>(gainsArray.length())
                    for (g in 0 until gainsArray.length()) {
                        gains.add(gainsArray.getDouble(g).toFloat())
                    }

                    val rawId = obj.getString("id")
                    var uniqueId = rawId
                    if (seenIds.contains(uniqueId)) {
                        val sourceSlug = obj.optString("source", "").lowercase().replace(Regex("[^a-z0-9]"), "_").trim('_')
                        if (sourceSlug.isNotEmpty() && !uniqueId.endsWith(sourceSlug)) {
                            uniqueId = "${rawId}_$sourceSlug"
                        }
                        var counter = 2
                        val baseId = uniqueId
                        while (seenIds.contains(uniqueId)) {
                            uniqueId = "${baseId}_$counter"
                            counter++
                        }
                    }
                    seenIds.add(uniqueId)

                    list.add(
                        AutoEqPreset(
                            id = uniqueId,
                            name = obj.getString("name"),
                            brand = obj.getString("brand"),
                            type = obj.optString("type", "Headphones"),
                            source = obj.optString("source", "AutoEQ"),
                            preamp = obj.optDouble("preamp", 0.0).toFloat(),
                            frequencies = frequencies,
                            gains = gains,
                        ),
                    )
                }
                list.sortedBy { it.name }
            }.getOrElse { e ->
                Log.e(TAG, "Failed to load AutoEQ presets from $ASSET_PATH", e)
                emptyList()
            }

            cachedPresets = presets
            presets
        }
    }

    suspend fun search(query: String): List<AutoEqPreset> {
        val all = loadPresets()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return all

        val lowerQuery = trimmed.lowercase()
        return all.filter {
            it.name.lowercase().contains(lowerQuery) ||
                it.brand.lowercase().contains(lowerQuery) ||
                it.type.lowercase().contains(lowerQuery)
        }
    }

    suspend fun getPresetById(id: String): AutoEqPreset? {
        return loadPresets().firstOrNull { it.id == id }
    }

    suspend fun findMatchesForDevice(deviceName: String): List<AutoEqPreset> {
        val all = loadPresets()
        return rankPresetsForDevice(all, deviceName)
    }

    companion object {
        private const val TAG = "AutoEqRepository"
        private const val ASSET_PATH = "autoeq/headphones.json"

        @Volatile
        private var instance: AutoEqRepository? = null

        fun get(context: Context): AutoEqRepository =
            instance ?: synchronized(this) {
                instance ?: AutoEqRepository(context).also { instance = it }
            }

        fun cleanDeviceName(deviceName: String): String {
            if (deviceName.isBlank()) return ""
            val cleaned = deviceName
                .replace(Regex("^LE[-_]", RegexOption.IGNORE_CASE), "")
                .replace(Regex("[’']s\\b", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\b(Hands-Free|Stereo|Bluetooth|BT|Wireless|Headset|Headphones)\\b", RegexOption.IGNORE_CASE), "")
                .replace(Regex("[^a-zA-Z0-9\\s-]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            return if (cleaned.isBlank()) deviceName.trim() else cleaned
        }

        fun rankPresetsForDevice(all: List<AutoEqPreset>, deviceName: String): List<AutoEqPreset> {
            if (deviceName.isBlank() || all.isEmpty()) return emptyList()

            val cleaned = cleanDeviceName(deviceName)
            val cleanedLower = cleaned.lowercase()
            val cleanedTokens = cleanedLower.split(Regex("[\\s-]+")).filter { it.length >= 2 }
            if (cleanedTokens.isEmpty()) return emptyList()

            val scored = all.mapNotNull { preset ->
                val presetNameLower = preset.name.lowercase()
                val presetBrandLower = preset.brand.lowercase()
                val fullPresetLower = "$presetBrandLower $presetNameLower"

                var score = 0

                if (presetNameLower == cleanedLower || fullPresetLower == cleanedLower) {
                    score += 150
                } else if (presetNameLower.contains(cleanedLower) || cleanedLower.contains(presetNameLower)) {
                    score += 100
                } else if (fullPresetLower.contains(cleanedLower) || cleanedLower.contains(fullPresetLower)) {
                    score += 80
                }

                var matchedTokens = 0
                for (token in cleanedTokens) {
                    if (presetNameLower.contains(token) || presetBrandLower.contains(token)) {
                        matchedTokens++
                        if (token.any { it.isDigit() }) {
                            score += 35
                        } else {
                            score += 12
                        }
                    }
                }

                if (score <= 0 && matchedTokens < cleanedTokens.size.coerceAtLeast(1)) {
                    return@mapNotNull null
                }

                when (preset.source.lowercase()) {
                    "oratory1990" -> score += 6
                    "crinacle" -> score += 5
                    "rtings" -> score += 4
                    "super review" -> score += 3
                }

                if (preset.name.contains("modded", ignoreCase = true) ||
                    preset.name.contains("earpads", ignoreCase = true) ||
                    preset.name.contains("filter", ignoreCase = true) ||
                    preset.name.contains("module", ignoreCase = true)
                ) {
                    score -= 20
                }

                preset to score
            }

            return scored
                .sortedByDescending { it.second }
                .map { it.first }
                .distinctBy { "${it.brand}_${it.name}" }
                .take(5)
        }
    }
}
