package dev.vantafyn.feature.music.harmonia

import java.time.Clock
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId

class HarmoniaPeriodCalculator(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun currentMonth(zoneId: ZoneId = ZoneId.systemDefault()): HarmoniaPeriodRange {
        val month = YearMonth.now(clock.withZone(zoneId))
        return month.toRange(zoneId)
    }

    fun previousCompletedMonth(zoneId: ZoneId = ZoneId.systemDefault()): HarmoniaPeriodRange {
        val month = YearMonth.now(clock.withZone(zoneId)).minusMonths(1)
        return month.toRange(zoneId)
    }

    fun currentYear(zoneId: ZoneId = ZoneId.systemDefault()): HarmoniaPeriodRange {
        val year = Year.now(clock.withZone(zoneId))
        return year.toRange(zoneId)
    }

    fun previousCompletedYear(zoneId: ZoneId = ZoneId.systemDefault()): HarmoniaPeriodRange {
        val year = Year.now(clock.withZone(zoneId)).minusYears(1)
        return year.toRange(zoneId)
    }

    fun year(year: Int, zoneId: ZoneId = ZoneId.systemDefault()): HarmoniaPeriodRange =
        Year.of(year).toRange(zoneId)

    private fun YearMonth.toRange(zoneId: ZoneId): HarmoniaPeriodRange =
        HarmoniaPeriodRange(
            type = HarmoniaPeriod.MONTHLY,
            start = atDay(1).atStartOfDay(zoneId).toInstant(),
            endExclusive = plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant(),
        )

    private fun Year.toRange(zoneId: ZoneId): HarmoniaPeriodRange =
        HarmoniaPeriodRange(
            type = HarmoniaPeriod.YEARLY,
            start = LocalDate.of(value, 1, 1).atStartOfDay(zoneId).toInstant(),
            endExclusive = LocalDate.of(value + 1, 1, 1).atStartOfDay(zoneId).toInstant(),
        )
}
