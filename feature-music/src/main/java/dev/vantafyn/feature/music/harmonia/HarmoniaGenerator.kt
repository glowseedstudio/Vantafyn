package dev.vantafyn.feature.music.harmonia

import java.time.Clock
import java.time.ZoneId
import java.util.UUID

class HarmoniaGenerator(
    private val historyRepository: HarmoniaHistoryRepository,
    private val recapRepository: HarmoniaRecapRepository,
    private val periodCalculator: HarmoniaPeriodCalculator = HarmoniaPeriodCalculator(),
    private val statisticsCalculator: HarmoniaStatisticsCalculator = HarmoniaStatisticsCalculator(),
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend fun generateMonthly(
        userId: UUID,
        serverId: String,
        profileId: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        previousCompleted: Boolean = true,
    ): HarmoniaGenerationResult {
        val period = if (previousCompleted) periodCalculator.previousCompletedMonth(zoneId) else periodCalculator.currentMonth(zoneId)
        val previous = HarmoniaPeriodRange(
            type = HarmoniaPeriod.MONTHLY,
            start = period.start.atZone(zoneId).minusMonths(1).toInstant(),
            endExclusive = period.start,
        )
        return generate(
            userId = userId,
            serverId = serverId,
            profileId = profileId,
            period = period,
            zoneId = zoneId,
            previousPeriod = previous,
            isFinalized = previousCompleted,
            force = true,
        )
    }

    suspend fun generateYearly(
        userId: UUID,
        serverId: String,
        profileId: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): HarmoniaGenerationResult =
        generate(
            userId = userId,
            serverId = serverId,
            profileId = profileId,
            period = periodCalculator.currentYear(zoneId),
            zoneId = zoneId,
            previousPeriod = null,
            isFinalized = false,
            force = true,
        )

    suspend fun generateEligibleRecaps(
        userId: UUID,
        serverId: String,
        profileId: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<HarmoniaGenerationResult.Generated> {
        val results = mutableListOf<HarmoniaGenerationResult.Generated>()

        // 1. Current Month: generates on the second last day of the month
        if (periodCalculator.isCurrentMonthEligible(zoneId)) {
            val monthly = generateIfNeeded(
                userId = userId,
                serverId = serverId,
                profileId = profileId,
                period = periodCalculator.currentMonth(zoneId),
                zoneId = zoneId,
                previousPeriod = periodCalculator.previousCompletedMonth(zoneId),
                isFinalized = false,
            )
            if (monthly is HarmoniaGenerationResult.Generated) {
                results.add(monthly)
            }
        }

        // 2. Previous Completed Month: its generation date has passed, finalize if needed
        val previousMonthly = generateIfNeeded(
            userId = userId,
            serverId = serverId,
            profileId = profileId,
            period = periodCalculator.previousCompletedMonth(zoneId),
            zoneId = zoneId,
            previousPeriod = HarmoniaPeriodRange(
                type = HarmoniaPeriod.MONTHLY,
                start = periodCalculator.previousCompletedMonth(zoneId).start.atZone(zoneId).minusMonths(1).toInstant(),
                endExclusive = periodCalculator.previousCompletedMonth(zoneId).start,
            ),
            isFinalized = true,
        )
        if (previousMonthly is HarmoniaGenerationResult.Generated) {
            results.add(previousMonthly)
        }

        // 3. Current Year: generates on December 15 each year
        if (periodCalculator.isCurrentYearEligible(zoneId)) {
            val currentYearly = generateIfNeeded(
                userId = userId,
                serverId = serverId,
                profileId = profileId,
                period = periodCalculator.currentYear(zoneId),
                zoneId = zoneId,
                previousPeriod = null,
                isFinalized = false,
            )
            if (currentYearly is HarmoniaGenerationResult.Generated) {
                results.add(currentYearly)
            }
        }

        // 4. Previous Completed Year: its December 15 date has passed, finalize if needed
        val yearly = generateIfNeeded(
            userId = userId,
            serverId = serverId,
            profileId = profileId,
            period = periodCalculator.previousCompletedYear(zoneId),
            zoneId = zoneId,
            previousPeriod = null,
            isFinalized = true,
        )
        if (yearly is HarmoniaGenerationResult.Generated) {
            results.add(yearly)
        }

        return results
    }

    suspend fun loadRecap(
        preview: HarmoniaRecapPreview,
        serverId: String,
        profileId: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): HarmoniaGenerationResult {
        val period = HarmoniaPeriodRange(preview.periodType, preview.periodStart, preview.periodEnd)
        val previousPeriod = when (preview.periodType) {
            HarmoniaPeriod.MONTHLY -> HarmoniaPeriodRange(
                type = HarmoniaPeriod.MONTHLY,
                start = period.start.atZone(zoneId).minusMonths(1).toInstant(),
                endExclusive = period.start,
            )
            HarmoniaPeriod.YEARLY -> null
        }
        val records = historyRepository.recordsFor(preview.userId, serverId, profileId, period)
        if (!statisticsCalculator.hasEnoughData(records)) {
            return HarmoniaGenerationResult.NotEnoughData("Harmonia could not load enough playback records for this recap.", period)
        }
        val previousRecords = previousPeriod?.let { historyRepository.recordsFor(preview.userId, serverId, profileId, it) }.orEmpty()
        return HarmoniaGenerationResult.Generated(
            HarmoniaRecap(
                id = preview.id,
                userId = preview.userId,
                periodType = preview.periodType,
                periodStart = preview.periodStart,
                periodEnd = preview.periodEnd,
                generatedAt = preview.generatedAt,
                dataVersion = preview.dataVersion,
                statistics = statisticsCalculator.calculate(records, period, zoneId, previousRecords),
                recordCount = preview.recordCount,
                historySignature = preview.historySignature,
                isFinalized = preview.isFinalized,
            ),
        )
    }

    private suspend fun generateIfNeeded(
        userId: UUID,
        serverId: String,
        profileId: String,
        period: HarmoniaPeriodRange,
        zoneId: ZoneId,
        previousPeriod: HarmoniaPeriodRange?,
        isFinalized: Boolean,
    ): HarmoniaGenerationResult? {
        val records = historyRepository.recordsFor(userId, serverId, profileId, period)
        if (!statisticsCalculator.hasEnoughData(records)) return null

        val signature = records.historySignature()
        val existing = recapRepository.previewForPeriod(userId, serverId, profileId, period)
        if (existing != null) {
            if (existing.isFinalized) return null
            if (existing.historySignature == signature && existing.recordCount == records.size) return null
        }

        return generate(
            userId = userId,
            serverId = serverId,
            profileId = profileId,
            period = period,
            zoneId = zoneId,
            previousPeriod = previousPeriod,
            isFinalized = isFinalized,
            force = false,
            records = records,
            signature = signature,
        )
    }

    private suspend fun generate(
        userId: UUID,
        serverId: String,
        profileId: String,
        period: HarmoniaPeriodRange,
        zoneId: ZoneId,
        previousPeriod: HarmoniaPeriodRange?,
        isFinalized: Boolean,
        force: Boolean,
        records: List<HarmoniaPlaybackRecord>? = null,
        signature: String? = null,
    ): HarmoniaGenerationResult {
        if (!force && recapRepository.previewForPeriod(userId, serverId, profileId, period)?.isFinalized == true) {
            return HarmoniaGenerationResult.NotEnoughData("This finalized Harmonia recap is immutable unless explicitly regenerated.", period)
        }

        val periodRecords = records ?: historyRepository.recordsFor(userId, serverId, profileId, period)
        if (!statisticsCalculator.hasEnoughData(periodRecords)) {
            return HarmoniaGenerationResult.NotEnoughData("Harmonia needs at least three completed music playback records for this period.", period)
        }
        val previousRecords = previousPeriod?.let { historyRepository.recordsFor(userId, serverId, profileId, it) }.orEmpty()
        val recap = HarmoniaRecap(
            id = recapId(userId, serverId, profileId, period),
            userId = userId,
            periodType = period.type,
            periodStart = period.start,
            periodEnd = period.endExclusive,
            generatedAt = clock.instant(),
            dataVersion = HARMONIA_DATA_VERSION,
            statistics = statisticsCalculator.calculate(periodRecords, period, zoneId, previousRecords),
            recordCount = periodRecords.size,
            historySignature = signature ?: periodRecords.historySignature(),
            isFinalized = isFinalized,
        )
        recapRepository.upsert(recap, serverId, profileId)
        return HarmoniaGenerationResult.Generated(recap)
    }

    private fun recapId(userId: UUID, serverId: String, profileId: String, period: HarmoniaPeriodRange): String =
        listOf(serverId, profileId, userId, period.type.name, period.start.toEpochMilli(), HARMONIA_DATA_VERSION).joinToString(":")
}

const val HARMONIA_DATA_VERSION = 1

interface HarmoniaHistoryRepository {
    suspend fun add(record: HarmoniaPlaybackRecord)
    suspend fun recordsFor(userId: UUID, serverId: String, profileId: String, period: HarmoniaPeriodRange): List<HarmoniaPlaybackRecord>
    suspend fun recentRecords(userId: UUID, serverId: String, profileId: String, limit: Int = 50): List<HarmoniaPlaybackRecord>
}

interface HarmoniaRecapRepository {
    suspend fun upsert(recap: HarmoniaRecap, serverId: String, profileId: String)
    suspend fun latestPreviews(userId: UUID, serverId: String, profileId: String): List<HarmoniaRecapPreview>
    suspend fun previewForPeriod(userId: UUID, serverId: String, profileId: String, period: HarmoniaPeriodRange): HarmoniaRecapPreview?
    suspend fun setRecapSaved(recapId: String, isSaved: Boolean) {}
    suspend fun savedPreviews(userId: UUID, serverId: String, profileId: String): List<HarmoniaRecapPreview> = emptyList()
}

private fun List<HarmoniaPlaybackRecord>.historySignature(): String {
    val sorted = sortedWith(compareBy<HarmoniaPlaybackRecord> { it.startedAt }.thenBy { it.id })
    val latestEnded = sorted.maxOfOrNull { it.endedAt.toEpochMilli() } ?: 0L
    val totalListening = sorted.sumOf { it.listenedMs }
    val idsHash = sorted.fold(1) { hash, record -> 31 * hash + record.id.hashCode() }
    return "${sorted.size}:$latestEnded:$totalListening:$idsHash"
}
