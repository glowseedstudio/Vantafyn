package dev.vantafyn.feature.music.harmonia

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class HarmoniaStatisticsCalculator(
    private val minCompletedTrackPlays: Int = 3,
) {
    fun calculate(
        records: List<HarmoniaPlaybackRecord>,
        period: HarmoniaPeriodRange,
        zoneId: ZoneId,
        previousPeriodRecords: List<HarmoniaPlaybackRecord> = emptyList(),
    ): HarmoniaStatistics {
        val safeRecords = records
            .filter { it.listenedMs > 0L && it.startedAt >= period.start && it.startedAt < period.endExclusive }
            .sortedBy { it.startedAt }
        val hasEnoughData = safeRecords.size >= minCompletedTrackPlays
        val source = "Vantafyn local music playback history"

        fun <T> stat(value: T?, calculation: String, available: Boolean = hasEnoughData): HarmoniaStatistic<T> =
            HarmoniaStatistic(
                value = value.takeIf { available },
                source = source,
                calculation = calculation,
                availability = if (available) HarmoniaAvailability.Available else HarmoniaAvailability.NotEnoughData,
                confidence = if (available) HarmoniaConfidence.High else HarmoniaConfidence.Low,
            )

        fun <T> unavailable(calculation: String): HarmoniaStatistic<T> =
            HarmoniaStatistic(
                value = null,
                source = source,
                calculation = calculation,
                availability = HarmoniaAvailability.Unavailable,
                confidence = HarmoniaConfidence.Unavailable,
            )

        val totalMs = safeRecords.sumOf { it.listenedMs }
        val daily = safeRecords.groupBy { it.startedAt.atZone(zoneId).toLocalDate() }
            .map { (date, items) -> HarmoniaDayListening(date, items.sumOf { it.listenedMs }, items.size) }
            .sortedBy { it.date }
        val hourly = (0..23).map { hour ->
            val items = safeRecords.filter { it.startedAt.atZone(zoneId).hour == hour }
            HarmoniaHourListening(hour, items.sumOf { it.listenedMs }, items.size)
        }
        val monthly = safeRecords.groupBy { YearMonth.from(it.startedAt.atZone(zoneId)) }
            .map { (month, items) -> HarmoniaMonthListening(month, items.sumOf { it.listenedMs }, items.size) }
            .sortedBy { it.month }
        val dayOfWeek = safeRecords.groupBy { it.startedAt.atZone(zoneId).dayOfWeek.name }
            .mapValues { (_, items) -> items.sumOf { it.listenedMs } }

        val topTracks = safeRecords.rankBy(
            key = { it.trackId.toString() },
            label = { it.trackTitle },
            subtitle = { it.artist },
            artworkUrl = { it.artworkUrl },
        )
        val topArtists = safeRecords.rankBy(
            key = { it.artist.trim().lowercase() },
            label = { it.artist },
            subtitle = null,
            artworkUrl = { it.artworkUrl },
        )
        val topAlbums = safeRecords
            .filter { !it.album.isNullOrBlank() }
            .rankBy(
                key = { it.albumId?.toString() ?: "${it.artist.trim().lowercase()}|${it.album!!.trim().lowercase()}" },
                label = { it.album.orEmpty() },
                subtitle = { it.artist },
                artworkUrl = { it.artworkUrl },
            )
        val genericGenres = setOf("music", "audio", "general", "unknown", "other", "sound", "track")
        val genreRows = safeRecords.flatMap { record ->
            record.genres
                .distinct()
                .map { it.trim() }
                .filter { it.isNotBlank() && it.lowercase() !in genericGenres }
                .map { genre ->
                    record.copy(trackId = stableGenreUuid, trackTitle = genre, artist = genre, album = genre, listenedMs = record.listenedMs)
                }
        }
        val topGenres = genreRows.rankBy(
            key = { it.trackTitle.trim().lowercase() },
            label = { it.trackTitle },
        )
        val comparisonDelta = if (previousPeriodRecords.isNotEmpty()) {
            totalMs - previousPeriodRecords.sumOf { it.listenedMs }
        } else {
            null
        }

        return HarmoniaStatistics(
            totalListeningTimeMs = stat(totalMs, "Sum listenedMs for records in the period."),
            totalTracksPlayed = stat(safeRecords.size, "Count completed listening records in the period."),
            uniqueTracks = stat(safeRecords.map { it.trackId }.distinct().size, "Count distinct track ids played in the period."),
            uniqueArtists = stat(safeRecords.map { it.artist.trim().lowercase() }.filter { it.isNotBlank() }.distinct().size, "Count distinct artist labels in the period."),
            uniqueAlbums = stat(safeRecords.mapNotNull { it.albumId ?: it.album?.trim()?.lowercase() }.distinct().size, "Count distinct albums in the period."),
            uniqueGenres = if (genreRows.isNotEmpty()) {
                stat(genreRows.map { it.trackTitle.lowercase() }.distinct().size, "Count distinct Jellyfin genres present on played tracks.")
            } else {
                unavailable("Jellyfin did not expose genre values on recorded music tracks.")
            },
            averageListeningSessionMs = stat(safeRecords.averageDuration(), "Mean listenedMs per completed playback record."),
            longestListeningDay = stat(daily.maxByOrNull { it.listeningTimeMs }, "Day with highest total listenedMs."),
            mostActiveDay = stat(daily.maxWithOrNull(compareBy<HarmoniaDayListening> { it.playCount }.thenBy { it.listeningTimeMs }), "Day with highest track play count."),
            mostActiveHour = stat(hourly.maxWithOrNull(compareBy<HarmoniaHourListening> { it.playCount }.thenBy { it.listeningTimeMs }), "Hour of day with highest track play count."),
            listeningStreakDays = stat(daily.longestStreak(), "Longest run of consecutive local dates with at least one listening record."),
            topArtists = stat(topArtists, "Rank artists by total listenedMs, then play count."),
            topTracks = stat(topTracks, "Rank tracks by total listenedMs, then play count."),
            topAlbums = stat(topAlbums, "Rank albums by total listenedMs, then play count."),
            topGenres = if (topGenres.isNotEmpty()) stat(topGenres, "Rank Jellyfin genres by total listenedMs, then play count.") else unavailable("No genre values were available in recorded tracks."),
            listeningByMonth = stat(monthly, "Group period listening by local calendar month."),
            listeningByDay = stat(daily, "Group period listening by local calendar date."),
            listeningByHour = stat(hourly, "Group period listening by local hour of day."),
            dayOfWeekDistribution = stat(dayOfWeek, "Group listenedMs by local day of week."),
            dailyHeatmap = stat(daily.associate { it.date to it.listeningTimeMs }, "Map local calendar date to listenedMs."),
            mostReplayedTrack = stat(topTracks.firstOrNull(), "Top track by total listenedMs, then play count."),
            mostPlayedArtist = stat(topArtists.firstOrNull(), "Top artist by total listenedMs, then play count."),
            mostPlayedAlbum = stat(topAlbums.firstOrNull(), "Top album by total listenedMs, then play count."),
            mostDominantGenre = if (topGenres.isNotEmpty()) stat(topGenres.first(), "Top genre by listenedMs, then play count.") else unavailable("No genre values were available in recorded tracks."),
            firstTimeArtists = unavailable("Requires playback history before the period; not reliable until older local Harmonia history exists."),
            firstTimeTracks = unavailable("Requires playback history before the period; not reliable until older local Harmonia history exists."),
            comparisonListeningDeltaMs = if (comparisonDelta != null) {
                stat(comparisonDelta, "Current period total listenedMs minus previous matching period total listenedMs.")
            } else {
                unavailable("No prior matching completed period found for comparison.")
            },
        )
    }

    fun hasEnoughData(records: List<HarmoniaPlaybackRecord>): Boolean =
        records.count { it.listenedMs > 0L } >= minCompletedTrackPlays

    private fun List<HarmoniaPlaybackRecord>.rankBy(
        key: (HarmoniaPlaybackRecord) -> String,
        label: (HarmoniaPlaybackRecord) -> String,
        subtitle: ((HarmoniaPlaybackRecord) -> String?)? = null,
        artworkUrl: ((HarmoniaPlaybackRecord) -> String?)? = null,
    ): List<HarmoniaRankedItem> =
        mapNotNull { record ->
            val displayLabel = label(record).trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            key(record).takeIf { it.isNotBlank() }?.let { id -> id to record }
        }
            .groupBy({ it.first }, { it.second })
            .map { (id, records) ->
                val representative = records.first()
                val art = records.mapNotNull { artworkUrl?.invoke(it) ?: it.artworkUrl }.firstOrNull()
                HarmoniaRankedItem(
                    id = id,
                    label = label(representative).trim(),
                    playCount = records.size,
                    listeningTimeMs = records.sumOf { it.listenedMs },
                    subtitle = subtitle?.invoke(representative)?.trim()?.takeIf { it.isNotBlank() },
                    artworkUrl = art,
                )
            }
            .sortedWith(compareByDescending<HarmoniaRankedItem> { it.listeningTimeMs }.thenByDescending { it.playCount }.thenBy { it.label })

    private fun List<HarmoniaPlaybackRecord>.averageDuration(): Long =
        if (isEmpty()) 0L else sumOf { it.listenedMs } / size

    private fun List<HarmoniaDayListening>.longestStreak(): Int {
        val days = map { it.date }.distinct().sorted()
        var best = 0
        var current = 0
        var previous: LocalDate? = null
        days.forEach { date ->
            current = if (previous?.plusDays(1) == date) current + 1 else 1
            best = maxOf(best, current)
            previous = date
        }
        return best
    }
}

private val stableGenreUuid = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")
