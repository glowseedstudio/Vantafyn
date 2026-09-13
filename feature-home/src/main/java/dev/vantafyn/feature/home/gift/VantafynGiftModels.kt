package dev.vantafyn.feature.home.gift

import android.content.Context
import androidx.compose.ui.graphics.Color
import dev.vantafyn.feature.home.hunger.HungerGamesMoviesCatalog
import dev.vantafyn.feature.home.jumanji.JumanjiMoviesCatalog
import dev.vantafyn.feature.home.jurassic.JurassicMoviesCatalog
import dev.vantafyn.feature.home.matrix.MatrixMoviesCatalog
import dev.vantafyn.feature.home.mcu.McuMoviesCatalog
import dev.vantafyn.feature.home.mordor.MiddleEarthMoviesCatalog
import dev.vantafyn.feature.home.pirates.PiratesMoviesCatalog
import dev.vantafyn.feature.home.pokemon.PokemonMoviesCatalog
import dev.vantafyn.feature.home.potter.HarryPotterMoviesCatalog
import dev.vantafyn.feature.home.residentevil.ResidentEvilMoviesCatalog
import dev.vantafyn.feature.home.saw.SawMoviesCatalog
import dev.vantafyn.feature.home.scary.ScaryMovieMoviesCatalog
import dev.vantafyn.feature.home.scream.ScreamMoviesCatalog
import dev.vantafyn.feature.home.twilight.TwilightMoviesCatalog
import dev.vantafyn.feature.home.underworld.UnderworldMoviesCatalog
import dev.vantafyn.feature.home.xmen.XMenMoviesCatalog
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class VantafynCodeGift(
    val id: String = UUID.randomUUID().toString(),
    val senderId: UUID,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val recipientUserId: UUID,
    val recipientName: String,
    val franchiseCode: String,
    val franchiseTitle: String,
    val franchiseBadge: String,
    val franchiseIcon: String,
    val accentColorHex: Long,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isClaimed: Boolean = false,
    val posterUrl: String? = null,
) {
    fun toSerializedMessage(): String {
        val json = JSONObject().apply {
            put("id", id)
            put("senderId", senderId.toString())
            put("senderName", senderName)
            put("senderAvatarUrl", senderAvatarUrl ?: "")
            put("recipientUserId", recipientUserId.toString())
            put("recipientName", recipientName)
            put("franchiseCode", franchiseCode)
            put("franchiseTitle", franchiseTitle)
            put("franchiseBadge", franchiseBadge)
            put("franchiseIcon", franchiseIcon)
            put("accentColorHex", accentColorHex)
            put("note", note ?: "")
            put("timestamp", timestamp)
            put("isClaimed", isClaimed)
            put("posterUrl", posterUrl ?: "")
        }
        return "[vantafyn_gift|${json}]"
    }

    companion object {
        fun fromSerializedMessage(raw: String): VantafynCodeGift? {
            val trimmed = raw.trim()
            if (!trimmed.startsWith("[vantafyn_gift|") || !trimmed.endsWith("]")) return null
            val body = trimmed.removePrefix("[vantafyn_gift|").removeSuffix("]")
            return try {
                val json = JSONObject(body)
                fromJsonObject(json)
            } catch (_: Exception) {
                null
            }
        }

        fun fromJsonObject(json: JSONObject): VantafynCodeGift? {
            return try {
                val code = json.getString("franchiseCode")
                val franchise = VantafynGiftFranchises.all.firstOrNull { it.code.equals(code, ignoreCase = true) }
                VantafynCodeGift(
                    id = json.optString("id", UUID.randomUUID().toString()),
                    senderId = UUID.fromString(json.getString("senderId")),
                    senderName = json.getString("senderName"),
                    senderAvatarUrl = json.optString("senderAvatarUrl").takeIf { it.isNotBlank() },
                    recipientUserId = UUID.fromString(json.getString("recipientUserId")),
                    recipientName = json.getString("recipientName"),
                    franchiseCode = code,
                    franchiseTitle = json.optString("franchiseTitle", franchise?.title ?: code),
                    franchiseBadge = json.optString("franchiseBadge", franchise?.badge ?: "FRANCHISE"),
                    franchiseIcon = json.optString("franchiseIcon", franchise?.icon ?: "🎁"),
                    accentColorHex = json.optLong("accentColorHex", franchise?.accentColorHex ?: 0xFFD4AF37),
                    note = json.optString("note").takeIf { it.isNotBlank() },
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    isClaimed = json.optBoolean("isClaimed", false),
                    posterUrl = json.optString("posterUrl").takeIf { it.isNotBlank() } ?: franchise?.posterUrl,
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class VantafynGiftFranchise(
    val code: String,
    val title: String,
    val badge: String,
    val icon: String,
    val movieCount: Int,
    val eraSubtitle: String,
    val accentColor: Color,
    val accentColorHex: Long,
    val posterPath: String = "",
) {
    val posterUrl: String
        get() = if (posterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$posterPath" else ""
}

object VantafynGiftFranchises {
    val all: List<VantafynGiftFranchise> = listOf(
        VantafynGiftFranchise(
            code = "MCU",
            title = "Marvel Cinematic Universe",
            badge = "MARVEL | STUDIOS",
            icon = "🛡️",
            movieCount = McuMoviesCatalog.movies.size,
            eraSubtitle = "Road to Doomsday",
            accentColor = Color(0xFFE23636),
            accentColorHex = 0xFFE23636,
            posterPath = "/or06FN3Dka5tukK1e9sl16pB3iy.jpg",
        ),
        VantafynGiftFranchise(
            code = "SAW",
            title = "SAW: The Complete Guide",
            badge = "JIGSAW | SAW",
            icon = "🪚",
            movieCount = SawMoviesCatalog.movies.size,
            eraSubtitle = "All 10 Games & Traps",
            accentColor = Color(0xFF8B0000),
            accentColorHex = 0xFF8B0000,
            posterPath = "/rLNSOudrayDBo1uqXjrhxcjODIC.jpg",
        ),
        VantafynGiftFranchise(
            code = "R-EVIL",
            title = "Resident Evil: Alice Saga",
            badge = "UMBRELLA | RESIDENT EVIL",
            icon = "☣️",
            movieCount = ResidentEvilMoviesCatalog.movies.size,
            eraSubtitle = "All 6 Milla Jovovich Films",
            accentColor = Color(0xFFB01C2E),
            accentColorHex = 0xFFB01C2E,
            posterPath = "/1UKNef590A0ZaMnxsscIcWuK1Em.jpg",
        ),
        VantafynGiftFranchise(
            code = "POTTER",
            title = "Harry Potter & Wizarding World",
            badge = "HOGWARTS | WIZARDING WORLD",
            icon = "⚡",
            movieCount = HarryPotterMoviesCatalog.movies.size,
            eraSubtitle = "8 HP Films + 3 Fantastic Beasts",
            accentColor = Color(0xFFD4AF37),
            accentColorHex = 0xFFD4AF37,
            posterPath = "/wuMc08IPKEatf9rnMNXvIDxqP4W.jpg",
        ),
        VantafynGiftFranchise(
            code = "HUNGER",
            title = "The Hunger Games",
            badge = "MOCKINGJAY | PANEM",
            icon = "🏹",
            movieCount = HungerGamesMoviesCatalog.movies.size,
            eraSubtitle = "All 5 Films (including Prequel)",
            accentColor = Color(0xFFE67E22),
            accentColorHex = 0xFFE67E22,
            posterPath = "/apa5G43Hha7kH7wJG0gkkHT7FA9.jpg",
        ),
        VantafynGiftFranchise(
            code = "SCREAM",
            title = "Scream: Slasher Guide",
            badge = "GHOSTFACE | WOODSBORO",
            icon = "😱",
            movieCount = ScreamMoviesCatalog.movies.size,
            eraSubtitle = "All 7 Slasher Films",
            accentColor = Color(0xFFC0392B),
            accentColorHex = 0xFFC0392B,
            posterPath = "/lr9ZIrmuwVmZhpZuTCW8D9g0ZJe.jpg",
        ),
        VantafynGiftFranchise(
            code = "MATRIX",
            title = "The Matrix Reality Guide",
            badge = "SYSTEM | THE MATRIX",
            icon = "💊",
            movieCount = MatrixMoviesCatalog.movies.size,
            eraSubtitle = "All 4 Wachowski Sci-Fi Films",
            accentColor = Color(0xFF00AA33),
            accentColorHex = 0xFF00AA33,
            posterPath = "/aOIuZAjPaRIE6CMzbazvcHuHXDc.jpg",
        ),
        VantafynGiftFranchise(
            code = "JUMANJI",
            title = "Jumanji: Adventure Guide",
            badge = "JUNGLE | JUMANJI",
            icon = "🎲",
            movieCount = JumanjiMoviesCatalog.movies.size,
            eraSubtitle = "Board Game to Video Game",
            accentColor = Color(0xFF27AE60),
            accentColorHex = 0xFF27AE60,
            posterPath = "/bdHG5Mo83VPobeZZdlSz0Y7HQHB.jpg",
        ),
        VantafynGiftFranchise(
            code = "JURASSIC",
            title = "Jurassic Park & World",
            badge = "INGEN | JURASSIC",
            icon = "🦖",
            movieCount = JurassicMoviesCatalog.movies.size,
            eraSubtitle = "All 7 Prehistoric Films",
            accentColor = Color(0xFFE74C3C),
            accentColorHex = 0xFFE74C3C,
            posterPath = "/63viWuPfYQjRYLSZSZNq7dglJP5.jpg",
        ),
        VantafynGiftFranchise(
            code = "PIRATES",
            title = "Pirates of the Caribbean",
            badge = "BLACK PEARL | PIRATES",
            icon = "🏴‍☠️",
            movieCount = PiratesMoviesCatalog.movies.size,
            eraSubtitle = "All 5 High Seas Voyages",
            accentColor = Color(0xFFD4AF37),
            accentColorHex = 0xFFD4AF37,
            posterPath = "/poHwCZeWzJCShH7tOjg8RIoyjcw.jpg",
        ),
        VantafynGiftFranchise(
            code = "POKEMON",
            title = "Pokémon Movies Collection",
            badge = "TRAINER | POKÉMON",
            icon = "⚡",
            movieCount = PokemonMoviesCatalog.movies.size,
            eraSubtitle = "23 Films + Detective Pikachu",
            accentColor = Color(0xFFE3350D),
            accentColorHex = 0xFFE3350D,
            posterPath = "/6YPzBcMH0aPNTvdXNCDLY0zdE1g.jpg",
        ),
        VantafynGiftFranchise(
            code = "SCARY",
            title = "Scary Movie Spoof Guide",
            badge = "DIMENSION | SCARY MOVIE",
            icon = "👻",
            movieCount = ScaryMovieMoviesCatalog.movies.size,
            eraSubtitle = "All 6 Spoof Films (incl. 2026)",
            accentColor = Color(0xFF00E676),
            accentColorHex = 0xFF00E676,
            posterPath = "/fVQFPRuw3yWXojYDJvA5EoFjUOY.jpg",
        ),
        VantafynGiftFranchise(
            code = "TWILIGHT",
            title = "The Twilight Saga",
            badge = "FORKS | TWILIGHT",
            icon = "🍎",
            movieCount = TwilightMoviesCatalog.movies.size,
            eraSubtitle = "Complete Bella & Edward Saga",
            accentColor = Color(0xFF9C27B0),
            accentColorHex = 0xFF9C27B0,
            posterPath = "/3Gkb6jm6962ADUPaCBqzz9CTbn9.jpg",
        ),
        VantafynGiftFranchise(
            code = "UNDERWORLD",
            title = "Underworld Chronicles",
            badge = "COVEN | UNDERWORLD",
            icon = "🦇",
            movieCount = UnderworldMoviesCatalog.movies.size,
            eraSubtitle = "Vampires vs Lycans War",
            accentColor = Color(0xFF1E88E5),
            accentColorHex = 0xFF1E88E5,
            posterPath = "/zsnQ41UZ3jo1wEeemF0eA9cAIU0.jpg",
        ),
        VantafynGiftFranchise(
            code = "X-MEN",
            title = "X-Men: Complete Mutant Saga",
            badge = "MUTANT | X-MEN",
            icon = "🧬",
            movieCount = XMenMoviesCatalog.movies.size,
            eraSubtitle = "First Class to Logan",
            accentColor = Color(0xFFF59E0B),
            accentColorHex = 0xFFF59E0B,
            posterPath = "/bRDAc4GogyS9ci3ow7UnInOcriN.jpg",
        ),
        VantafynGiftFranchise(
            code = "MORDOR",
            title = "Middle-earth: The Legendarium",
            badge = "ONE RING | MIDDLE-EARTH",
            icon = "💍",
            movieCount = MiddleEarthMoviesCatalog.movies.size,
            eraSubtitle = "Rohirrim, Hobbit & LOTR",
            accentColor = Color(0xFFD4AF37),
            accentColorHex = 0xFFD4AF37,
            posterPath = "/6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg",
        ),
    )
}

object VantafynGiftStorage {
    private const val PREFS_NAME = "vantafyn_server_gifts"
    private const val KEY_PENDING_GIFTS = "pending_gifts"
    private const val KEY_CLAIMED_IDS = "claimed_gift_ids"

    @Synchronized
    fun enqueueGift(context: Context, serverUrl: String, gift: VantafynCodeGift) {
        val claimedIds = getClaimedGiftIds(context)
        if (gift.id in claimedIds || gift.isClaimed) return
        val existing = getPendingGifts(context, serverUrl, gift.recipientUserId).toMutableList()
        existing.removeAll { it.id == gift.id }
        existing.add(gift)
        savePendingGifts(context, serverUrl, gift.recipientUserId, existing)
    }

    @Synchronized
    fun getPendingGifts(context: Context, serverUrl: String, userId: UUID): List<VantafynCodeGift> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val claimedIds = getClaimedGiftIds(context)
        val raw = prefs.getString("${KEY_PENDING_GIFTS}_${serverUrl}_$userId", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<VantafynCodeGift>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val gift = VantafynCodeGift.fromJsonObject(obj)
                if (gift != null && !gift.isClaimed && gift.id !in claimedIds) {
                    list.add(gift)
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun markGiftClaimed(context: Context, serverUrl: String, userId: UUID, giftId: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val claimed = getClaimedGiftIds(context).toMutableSet()
        claimed.add(giftId)
        prefs.edit().putStringSet(KEY_CLAIMED_IDS, claimed).apply()

        val pending = getPendingGifts(context, serverUrl, userId).filter { it.id != giftId }
        savePendingGifts(context, serverUrl, userId, pending)
    }

    private fun getClaimedGiftIds(context: Context): Set<String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_CLAIMED_IDS, emptySet()) ?: emptySet()
    }

    private fun savePendingGifts(context: Context, serverUrl: String, userId: UUID, gifts: List<VantafynCodeGift>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (g in gifts) {
            val json = JSONObject().apply {
                put("id", g.id)
                put("senderId", g.senderId.toString())
                put("senderName", g.senderName)
                put("senderAvatarUrl", g.senderAvatarUrl ?: "")
                put("recipientUserId", g.recipientUserId.toString())
                put("recipientName", g.recipientName)
                put("franchiseCode", g.franchiseCode)
                put("franchiseTitle", g.franchiseTitle)
                put("franchiseBadge", g.franchiseBadge)
                put("franchiseIcon", g.franchiseIcon)
                put("accentColorHex", g.accentColorHex)
                put("note", g.note ?: "")
                put("timestamp", g.timestamp)
                put("isClaimed", g.isClaimed)
                put("posterUrl", g.posterUrl ?: "")
            }
            array.put(json)
        }
        prefs.edit().putString("${KEY_PENDING_GIFTS}_${serverUrl}_$userId", array.toString()).apply()
    }
}
