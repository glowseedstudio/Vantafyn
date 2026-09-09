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

                    list.add(
                        AutoEqPreset(
                            id = obj.getString("id"),
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

    companion object {
        private const val TAG = "AutoEqRepository"
        private const val ASSET_PATH = "autoeq/headphones.json"

        @Volatile
        private var instance: AutoEqRepository? = null

        fun get(context: Context): AutoEqRepository =
            instance ?: synchronized(this) {
                instance ?: AutoEqRepository(context).also { instance = it }
            }
    }
}
