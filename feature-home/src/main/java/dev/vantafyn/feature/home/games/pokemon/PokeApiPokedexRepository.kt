package dev.vantafyn.feature.home.games.pokemon

import android.content.Context
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession

/**
 * Retrieves canonical per-species metadata on demand and caches it on the device.
 * The bundled catalogue is deliberately retained as the offline/failure fallback.
 */
object PokeApiPokedexRepository {
    private const val cacheName = "pokeapi_pokedex_metadata_v1"

    suspend fun load(context: Context, speciesId: Int, fallback: SpeciesPokedexData, session: JellyfinSession?, repository: JellyfinPokemonRepository?): PokedexMetadata {
        val prefs = context.getSharedPreferences(cacheName, Context.MODE_PRIVATE)
        prefs.getString(speciesId.toString(), null)?.let { cached ->
            runCatching { return PokedexMetadata.fromJson(org.json.JSONObject(cached), fallback) }
        }
        if (session == null || repository == null) return PokedexMetadata.fromFallback(fallback)
        return repository.getDexMetadata(session, speciesId).mapCatching { dto ->
            val metadata = PokedexMetadata(dto.category, dto.flavorText, dto.heightMeters, dto.weightKg, dto.hp, dto.attack, dto.defense, dto.spAtk, dto.spDef, dto.speed, PokemonType.entries.firstOrNull { it.displayName.equals(dto.primaryType, true) }, PokemonType.entries.firstOrNull { it.displayName.equals(dto.secondaryType, true) })
            prefs.edit().putString(speciesId.toString(), metadata.toJson().toString()).apply()
            metadata
        }.getOrElse { PokedexMetadata.fromFallback(fallback) }
    }
}

data class PokedexMetadata(
    val category: String,
    val flavorText: String,
    val heightMeters: Float,
    val weightKg: Float,
    val hp: Int, val attack: Int, val defense: Int, val spAtk: Int, val spDef: Int, val speed: Int,
    val primaryType: PokemonType?, val secondaryType: PokemonType?,
) {
    fun applyTo(fallback: SpeciesPokedexData) = fallback.copy(category = category, flavorText = flavorText, heightMeters = heightMeters, weightKg = weightKg, hp = hp, attack = attack, defense = defense, spAtk = spAtk, spDef = spDef, speed = speed)
    fun toJson() = org.json.JSONObject().apply {
        put("category", category); put("flavorText", flavorText); put("height", heightMeters); put("weight", weightKg)
        put("hp", hp); put("attack", attack); put("defense", defense); put("spAtk", spAtk); put("spDef", spDef); put("speed", speed)
        put("primaryType", primaryType?.id); put("secondaryType", secondaryType?.id)
    }
    companion object {
        fun fromFallback(f: SpeciesPokedexData) = PokedexMetadata(f.category, f.flavorText, f.heightMeters, f.weightKg, f.hp, f.attack, f.defense, f.spAtk, f.spDef, f.speed, null, null)
        fun fromJson(json: org.json.JSONObject, f: SpeciesPokedexData) = PokedexMetadata(json.optString("category", f.category), json.optString("flavorText", f.flavorText), json.optDouble("height", f.heightMeters.toDouble()).toFloat(), json.optDouble("weight", f.weightKg.toDouble()).toFloat(), json.optInt("hp", f.hp), json.optInt("attack", f.attack), json.optInt("defense", f.defense), json.optInt("spAtk", f.spAtk), json.optInt("spDef", f.spDef), json.optInt("speed", f.speed), type(json, "primaryType"), type(json, "secondaryType"))
        fun fromApi(species: org.json.JSONObject, pokemon: org.json.JSONObject, f: SpeciesPokedexData): PokedexMetadata {
            val genus = (0 until species.getJSONArray("genera").length()).asSequence().map { species.getJSONArray("genera").getJSONObject(it) }.firstOrNull { it.getJSONObject("language").optString("name") == "en" }?.optString("genus").orEmpty().ifBlank { f.category }
            val flavors = species.getJSONArray("flavor_text_entries")
            val flavor = (flavors.length() - 1 downTo 0).asSequence().map { flavors.getJSONObject(it) }.firstOrNull { it.getJSONObject("language").optString("name") == "en" }?.optString("flavor_text")?.replace(Regex("[\\n\\f\\r]+"), " ")?.replace(Regex("\\s+"), " ")?.trim().orEmpty().ifBlank { f.flavorText }
            val stats = pokemon.getJSONArray("stats"); fun stat(name: String, fallback: Int) = (0 until stats.length()).asSequence().map { stats.getJSONObject(it) }.firstOrNull { it.getJSONObject("stat").optString("name") == name }?.optInt("base_stat") ?: fallback
            val types = pokemon.getJSONArray("types"); fun typeAt(index: Int) = if (index < types.length()) PokemonType.entries.firstOrNull { it.displayName.equals(types.getJSONObject(index).getJSONObject("type").optString("name"), true) } else null
            return PokedexMetadata(genus, flavor, pokemon.optDouble("height", f.heightMeters.toDouble() * 10).toFloat() / 10f, pokemon.optDouble("weight", f.weightKg.toDouble() * 10).toFloat() / 10f, stat("hp", f.hp), stat("attack", f.attack), stat("defense", f.defense), stat("special-attack", f.spAtk), stat("special-defense", f.spDef), stat("speed", f.speed), typeAt(0), typeAt(1))
        }
        private fun type(json: org.json.JSONObject, key: String) = if (json.has(key) && !json.isNull(key)) PokemonType.entries.firstOrNull { it.id == json.optInt(key) } else null
    }
}
