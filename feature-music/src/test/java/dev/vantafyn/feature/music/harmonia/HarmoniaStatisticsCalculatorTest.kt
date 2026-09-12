package dev.vantafyn.feature.music.harmonia

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class HarmoniaStatisticsCalculatorTest {
    private val zone = ZoneId.of("Australia/Sydney")
    private val userId = UUID.fromString("10000000-0000-0000-0000-000000000001")
    private val otherUserId = UUID.fromString("10000000-0000-0000-0000-000000000002")
    private val trackOne = UUID.fromString("20000000-0000-0000-0000-000000000001")
    private val trackTwo = UUID.fromString("20000000-0000-0000-0000-000000000002")
    private val albumOne = UUID.fromString("30000000-0000-0000-0000-000000000001")

    @Test
    fun monthlyPeriodUsesLocalTimezoneBoundaries() {
        val clock = Clock.fixed(Instant.parse("2026-08-26T15:00:00Z"), zone)
        val period = HarmoniaPeriodCalculator(clock).currentMonth(zone)

        assertEquals(Instant.parse("2026-07-31T14:00:00Z"), period.start)
        assertEquals(Instant.parse("2026-08-31T14:00:00Z"), period.endExclusive)
    }

    @Test
    fun yearlyPeriodUsesCalendarYear() {
        val clock = Clock.fixed(Instant.parse("2026-08-26T15:00:00Z"), zone)
        val period = HarmoniaPeriodCalculator(clock).currentYear(zone)

        assertEquals(Instant.parse("2025-12-31T13:00:00Z"), period.start)
        assertEquals(Instant.parse("2026-12-31T13:00:00Z"), period.endExclusive)
    }

    @Test
    fun calculatesListeningTotalsAndTopContent() {
        val period = HarmoniaPeriodCalculator(Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), zone)).currentMonth(zone)
        val records = listOf(
            record("a", trackOne, "Song A", "Artist A", "Album A", albumOne, listOf("Pop"), "2026-08-02T01:00:00Z", 120_000),
            record("b", trackOne, "Song A", "Artist A", "Album A", albumOne, listOf("Pop"), "2026-08-02T02:00:00Z", 180_000),
            record("c", trackTwo, "Song B", "Artist B", "Album B", null, listOf("Rock"), "2026-08-03T03:00:00Z", 60_000),
        )

        val stats = HarmoniaStatisticsCalculator().calculate(records, period, zone)

        assertEquals(360_000L, stats.totalListeningTimeMs.value)
        assertEquals(3, stats.totalTracksPlayed.value)
        assertEquals(2, stats.uniqueTracks.value)
        assertEquals("Artist A", stats.topArtists.value!!.first().label)
        assertEquals("Song A", stats.topTracks.value!!.first().label)
        assertEquals("Album A", stats.topAlbums.value!!.first().label)
        assertEquals("Pop", stats.topGenres.value!!.first().label)
        assertEquals(2, stats.dailyHeatmap.value!!.size)
        assertEquals(24, stats.listeningByHour.value!!.size)
    }

    @Test
    fun returnsNotEnoughDataForEmptyOrTinyPeriods() {
        val period = HarmoniaPeriodCalculator(Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), zone)).currentMonth(zone)
        val stats = HarmoniaStatisticsCalculator().calculate(emptyList(), period, zone)

        assertEquals(HarmoniaAvailability.NotEnoughData, stats.totalTracksPlayed.availability)
        assertNull(stats.totalTracksPlayed.value)
    }

    @Test
    fun separatesMonthlyAndYearlyInputByPeriod() {
        val calculator = HarmoniaStatisticsCalculator(minCompletedTrackPlays = 1)
        val august = HarmoniaPeriodCalculator(Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), zone)).currentMonth(zone)
        val records = listOf(
            record("aug", trackOne, "Song A", "Artist A", "Album A", albumOne, listOf("Pop"), "2026-08-02T01:00:00Z", 120_000),
            record("sep", trackTwo, "Song B", "Artist B", "Album B", null, listOf("Rock"), "2026-09-02T01:00:00Z", 120_000),
        )

        val stats = calculator.calculate(records, august, zone)

        assertEquals(1, stats.totalTracksPlayed.value)
        assertEquals("Song A", stats.topTracks.value!!.first().label)
    }

    @Test
    fun generationIsIdempotentAndUserSpecific() = runBlocking {
        val store = InMemoryHarmoniaStore()
        val clock = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), zone)
        val generator = HarmoniaGenerator(
            historyRepository = store,
            recapRepository = store,
            periodCalculator = HarmoniaPeriodCalculator(clock),
            statisticsCalculator = HarmoniaStatisticsCalculator(minCompletedTrackPlays = 1),
            clock = clock,
        )
        val record = record("a", trackOne, "Song A", "Artist A", "Album A", albumOne, listOf("Pop"), "2026-08-02T01:00:00Z", 120_000)
        store.add(record)
        store.add(record.copy(id = "other", userId = otherUserId, listenedMs = 999_000))

        val first = generator.generateMonthly(userId, "server", "profile", zone, previousCompleted = false)
        val second = generator.generateMonthly(userId, "server", "profile", zone, previousCompleted = false)

        assertTrue(first is HarmoniaGenerationResult.Generated)
        assertTrue(second is HarmoniaGenerationResult.Generated)
        assertEquals((first as HarmoniaGenerationResult.Generated).recap.id, (second as HarmoniaGenerationResult.Generated).recap.id)
        assertEquals(1, store.recaps.size)
        assertEquals(120_000L, second.recap.statistics.totalListeningTimeMs.value)
    }

    @Test
    fun automaticGenerationSkipsUnchangedMutableRecapsAndRefreshesWhenHistoryChanges() = runBlocking {
        val store = InMemoryHarmoniaStore()
        val clock = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), zone)
        val generator = HarmoniaGenerator(
            historyRepository = store,
            recapRepository = store,
            periodCalculator = HarmoniaPeriodCalculator(clock),
            statisticsCalculator = HarmoniaStatisticsCalculator(minCompletedTrackPlays = 1),
            clock = clock,
        )
        store.add(record("a", trackOne, "Song A", "Artist A", "Album A", albumOne, listOf("Pop"), "2026-08-02T01:00:00Z", 120_000))

        val first = generator.generateEligibleRecaps(userId, "server", "profile", zone)
        val unchanged = generator.generateEligibleRecaps(userId, "server", "profile", zone)
        store.add(record("b", trackTwo, "Song B", "Artist B", "Album B", null, listOf("Rock"), "2026-08-03T01:00:00Z", 180_000))
        val refreshed = generator.generateEligibleRecaps(userId, "server", "profile", zone)

        assertEquals(1, first.size)
        assertEquals(0, unchanged.size)
        assertEquals(1, refreshed.size)
        assertEquals(1, store.recaps.size)
        assertEquals(300_000L, refreshed.first().recap.statistics.totalListeningTimeMs.value)
    }

    @Test
    fun automaticGenerationDoesNotOverwriteFinalizedYearlyRecaps() = runBlocking {
        val store = InMemoryHarmoniaStore()
        val clock = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), zone)
        val generator = HarmoniaGenerator(
            historyRepository = store,
            recapRepository = store,
            periodCalculator = HarmoniaPeriodCalculator(clock),
            statisticsCalculator = HarmoniaStatisticsCalculator(minCompletedTrackPlays = 1),
            clock = clock,
        )
        store.add(record("y1", trackOne, "Song A", "Artist A", "Album A", albumOne, listOf("Pop"), "2025-08-02T01:00:00Z", 120_000))

        val first = generator.generateEligibleRecaps(userId, "server", "profile", zone)
        store.add(record("y2", trackTwo, "Song B", "Artist B", "Album B", null, listOf("Rock"), "2025-08-03T01:00:00Z", 180_000))
        val second = generator.generateEligibleRecaps(userId, "server", "profile", zone)

        assertEquals(1, first.size)
        assertEquals(HarmoniaPeriod.YEARLY, first.first().recap.periodType)
        assertTrue(first.first().recap.isFinalized)
        assertEquals(0, second.size)
        assertEquals(120_000L, store.recaps.values.first().statistics.totalListeningTimeMs.value)
    }

    @Test
    fun calculatesTheDevoteePersonaWhenTopArtistDominates() {
        val period = HarmoniaPeriodCalculator(Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), zone)).currentMonth(zone)
        val records = listOf(
            record("a", trackOne, "Song A", "Artist A", "Album A", albumOne, listOf("Pop"), "2026-08-02T01:00:00Z", 200_000),
            record("b", trackOne, "Song A", "Artist A", "Album A", albumOne, listOf("Pop"), "2026-08-02T02:00:00Z", 200_000),
            record("c", trackTwo, "Song B", "Artist B", "Album B", null, listOf("Pop"), "2026-08-03T03:00:00Z", 50_000),
        )

        val stats = HarmoniaStatisticsCalculator().calculate(records, period, zone)

        assertEquals(HarmoniaAvailability.Available, stats.persona.availability)
        assertEquals("The Devotee", stats.persona.value?.title)
    }

    @Test
    fun calculatesGenreChameleonWhenListeningIsSpreadAcrossDiverseGenres() {
        val period = HarmoniaPeriodCalculator(Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), zone)).currentMonth(zone)
        val t3 = UUID.randomUUID()
        val t4 = UUID.randomUUID()
        val records = listOf(
            record("a", trackOne, "Song A", "Artist A", null, null, listOf("Pop"), "2026-08-02T02:00:00Z", 100_000),
            record("b", trackTwo, "Song B", "Artist B", null, null, listOf("Rock"), "2026-08-02T03:00:00Z", 100_000),
            record("c", t3, "Song C", "Artist C", null, null, listOf("Electronic"), "2026-08-02T04:00:00Z", 100_000),
            record("d", t4, "Song D", "Artist D", null, null, listOf("Jazz"), "2026-08-02T05:00:00Z", 100_000),
        )

        val stats = HarmoniaStatisticsCalculator().calculate(records, period, zone)

        assertEquals(HarmoniaAvailability.Available, stats.persona.availability)
        assertEquals("The Genre Chameleon", stats.persona.value?.title)
    }

    private fun record(
        id: String,
        trackId: UUID,
        title: String,
        artist: String,
        album: String?,
        albumId: UUID?,
        genres: List<String>,
        startedAt: String,
        listenedMs: Long,
    ): HarmoniaPlaybackRecord {
        val start = Instant.parse(startedAt)
        return HarmoniaPlaybackRecord(
            id = id,
            userId = userId,
            serverId = "server",
            profileId = "profile",
            trackId = trackId,
            trackTitle = title,
            artist = artist,
            album = album,
            albumId = albumId,
            genres = genres,
            startedAt = start,
            endedAt = start.plusMillis(listenedMs),
            listenedMs = listenedMs,
            durationMs = listenedMs,
        )
    }
}

private class InMemoryHarmoniaStore : HarmoniaHistoryRepository, HarmoniaRecapRepository {
    private val records = mutableListOf<HarmoniaPlaybackRecord>()
    val recaps = linkedMapOf<String, HarmoniaRecap>()

    override suspend fun add(record: HarmoniaPlaybackRecord) {
        records.removeAll { it.id == record.id }
        records.add(record)
    }

    override suspend fun recordsFor(userId: UUID, serverId: String, profileId: String, period: HarmoniaPeriodRange): List<HarmoniaPlaybackRecord> =
        records.filter {
            it.userId == userId &&
                it.serverId == serverId &&
                it.profileId == profileId &&
                it.startedAt >= period.start &&
                it.startedAt < period.endExclusive
        }

    override suspend fun recentRecords(userId: UUID, serverId: String, profileId: String, limit: Int): List<HarmoniaPlaybackRecord> =
        records
            .filter { it.userId == userId && it.serverId == serverId && it.profileId == profileId }
            .sortedByDescending { it.startedAt }
            .take(limit)

    override suspend fun upsert(recap: HarmoniaRecap, serverId: String, profileId: String) {
        recaps[recap.id] = recap
    }

    override suspend fun latestPreviews(userId: UUID, serverId: String, profileId: String): List<HarmoniaRecapPreview> =
        recaps.values
            .filter { it.userId == userId && it.dataVersion == HARMONIA_DATA_VERSION }
            .sortedByDescending { it.periodStart }
            .distinctBy { it.periodType }
            .map { recap ->
                HarmoniaRecapPreview(
                    id = recap.id,
                    userId = recap.userId,
                    periodType = recap.periodType,
                    periodStart = recap.periodStart,
                    periodEnd = recap.periodEnd,
                    generatedAt = recap.generatedAt,
                    dataVersion = recap.dataVersion,
                    totalListeningTimeMs = recap.statistics.totalListeningTimeMs.value ?: 0L,
                    totalTracksPlayed = recap.statistics.totalTracksPlayed.value ?: 0,
                    topArtist = recap.statistics.topArtists.value?.firstOrNull()?.label,
                    topTrack = recap.statistics.topTracks.value?.firstOrNull()?.label,
                    recordCount = recap.recordCount,
                    historySignature = recap.historySignature,
                    isFinalized = recap.isFinalized,
                )
            }

    override suspend fun previewForPeriod(
        userId: UUID,
        serverId: String,
        profileId: String,
        period: HarmoniaPeriodRange,
    ): HarmoniaRecapPreview? =
        recaps.values
            .filter {
                it.userId == userId &&
                    it.periodType == period.type &&
                    it.periodStart == period.start &&
                    it.dataVersion == HARMONIA_DATA_VERSION
            }
            .maxByOrNull { it.generatedAt }
            ?.let { recap ->
                HarmoniaRecapPreview(
                    id = recap.id,
                    userId = recap.userId,
                    periodType = recap.periodType,
                    periodStart = recap.periodStart,
                    periodEnd = recap.periodEnd,
                    generatedAt = recap.generatedAt,
                    dataVersion = recap.dataVersion,
                    totalListeningTimeMs = recap.statistics.totalListeningTimeMs.value ?: 0L,
                    totalTracksPlayed = recap.statistics.totalTracksPlayed.value ?: 0,
                    topArtist = recap.statistics.topArtists.value?.firstOrNull()?.label,
                    topTrack = recap.statistics.topTracks.value?.firstOrNull()?.label,
                    recordCount = recap.recordCount,
                    historySignature = recap.historySignature,
                    isFinalized = recap.isFinalized,
                )
            }
}
