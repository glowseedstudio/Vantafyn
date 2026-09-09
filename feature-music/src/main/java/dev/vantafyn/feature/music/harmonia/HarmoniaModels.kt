package dev.vantafyn.feature.music.harmonia

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class HarmoniaPeriod {
    MONTHLY,
    YEARLY,
}

enum class HarmoniaAvailability {
    Available,
    NotEnoughData,
    Unavailable,
}

enum class HarmoniaConfidence {
    High,
    Medium,
    Low,
    Unavailable,
}

data class HarmoniaStatistic<T>(
    val value: T?,
    val source: String,
    val calculation: String,
    val availability: HarmoniaAvailability,
    val confidence: HarmoniaConfidence,
)

data class HarmoniaPeriodRange(
    val type: HarmoniaPeriod,
    val start: Instant,
    val endExclusive: Instant,
)

data class HarmoniaPlaybackRecord(
    val id: String,
    val userId: UUID,
    val serverId: String,
    val profileId: String,
    val trackId: UUID,
    val trackTitle: String,
    val artist: String,
    val album: String?,
    val albumId: UUID?,
    val genres: List<String>,
    val startedAt: Instant,
    val endedAt: Instant,
    val listenedMs: Long,
    val durationMs: Long?,
    val artworkUrl: String? = null,
)

data class HarmoniaRankedItem(
    val id: String,
    val label: String,
    val playCount: Int,
    val listeningTimeMs: Long,
    val subtitle: String? = null,
    val artworkUrl: String? = null,
)

data class HarmoniaDayListening(
    val date: LocalDate,
    val listeningTimeMs: Long,
    val playCount: Int,
)

data class HarmoniaHourListening(
    val hour: Int,
    val listeningTimeMs: Long,
    val playCount: Int,
)

data class HarmoniaMonthListening(
    val month: YearMonth,
    val listeningTimeMs: Long,
    val playCount: Int,
)

data class HarmoniaStatistics(
    val totalListeningTimeMs: HarmoniaStatistic<Long>,
    val totalTracksPlayed: HarmoniaStatistic<Int>,
    val uniqueTracks: HarmoniaStatistic<Int>,
    val uniqueArtists: HarmoniaStatistic<Int>,
    val uniqueAlbums: HarmoniaStatistic<Int>,
    val uniqueGenres: HarmoniaStatistic<Int>,
    val averageListeningSessionMs: HarmoniaStatistic<Long>,
    val longestListeningDay: HarmoniaStatistic<HarmoniaDayListening>,
    val mostActiveDay: HarmoniaStatistic<HarmoniaDayListening>,
    val mostActiveHour: HarmoniaStatistic<HarmoniaHourListening>,
    val listeningStreakDays: HarmoniaStatistic<Int>,
    val topArtists: HarmoniaStatistic<List<HarmoniaRankedItem>>,
    val topTracks: HarmoniaStatistic<List<HarmoniaRankedItem>>,
    val topAlbums: HarmoniaStatistic<List<HarmoniaRankedItem>>,
    val topGenres: HarmoniaStatistic<List<HarmoniaRankedItem>>,
    val listeningByMonth: HarmoniaStatistic<List<HarmoniaMonthListening>>,
    val listeningByDay: HarmoniaStatistic<List<HarmoniaDayListening>>,
    val listeningByHour: HarmoniaStatistic<List<HarmoniaHourListening>>,
    val dayOfWeekDistribution: HarmoniaStatistic<Map<String, Long>>,
    val dailyHeatmap: HarmoniaStatistic<Map<LocalDate, Long>>,
    val mostReplayedTrack: HarmoniaStatistic<HarmoniaRankedItem>,
    val mostPlayedArtist: HarmoniaStatistic<HarmoniaRankedItem>,
    val mostPlayedAlbum: HarmoniaStatistic<HarmoniaRankedItem>,
    val mostDominantGenre: HarmoniaStatistic<HarmoniaRankedItem>,
    val firstTimeArtists: HarmoniaStatistic<List<HarmoniaRankedItem>>,
    val firstTimeTracks: HarmoniaStatistic<List<HarmoniaRankedItem>>,
    val comparisonListeningDeltaMs: HarmoniaStatistic<Long>,
)

sealed interface HarmoniaGenerationResult {
    data class Generated(val recap: HarmoniaRecap) : HarmoniaGenerationResult
    data class NotEnoughData(val reason: String, val period: HarmoniaPeriodRange) : HarmoniaGenerationResult
}

data class HarmoniaRecap(
    val id: String,
    val userId: UUID,
    val periodType: HarmoniaPeriod,
    val periodStart: Instant,
    val periodEnd: Instant,
    val generatedAt: Instant,
    val dataVersion: Int,
    val statistics: HarmoniaStatistics,
    val recordCount: Int = 0,
    val historySignature: String = "",
    val isFinalized: Boolean = false,
    val isSaved: Boolean = false,
)

data class HarmoniaRecapPreview(
    val id: String,
    val userId: UUID,
    val periodType: HarmoniaPeriod,
    val periodStart: Instant,
    val periodEnd: Instant,
    val generatedAt: Instant,
    val dataVersion: Int,
    val totalListeningTimeMs: Long,
    val totalTracksPlayed: Int,
    val topArtist: String?,
    val topTrack: String?,
    val recordCount: Int = 0,
    val historySignature: String = "",
    val isFinalized: Boolean = false,
    val isSaved: Boolean = false,
    val artworkUrl: String? = null,
)
