package com.vida.feature.statistics.model

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Time range selector for statistics charts.
 *
 * Each preset computes a date range from today's date. [Personalizado] allows
 * the user to pick arbitrary start/end dates.
 */
sealed class StatsPeriod {

    /** Human-readable label shown in the period selector UI. */
    abstract val displayName: String

    /**
     * Returns the (from, to) instant range for this period.
     *
     * The `to` instant is exclusive — it represents the start of the next
     * day/period after the range, matching the Room query convention
     * `WHERE date_time >= :from AND date_time < :to`.
     */
    fun toDateRange(clock: Clock = Clock.systemDefaultZone()): Pair<Instant, Instant> {
        val today = LocalDate.now(clock)
        val zone = ZoneId.systemDefault()
        return when (this) {
            is EsteMes -> {
                val from = today.withDayOfMonth(1).atStartOfDay(zone).toInstant()
                val to = today.plusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant()
                from to to
            }
            is Ultimos3Meses -> {
                val from = today.minusMonths(3).atStartOfDay(zone).toInstant()
                val to = today.plusDays(1).atStartOfDay(zone).toInstant()
                from to to
            }
            is UltimoAnio -> {
                val from = today.minusYears(1).atStartOfDay(zone).toInstant()
                val to = today.plusDays(1).atStartOfDay(zone).toInstant()
                from to to
            }
            is Personalizado -> {
                val from = desde.atStartOfDay(zone).toInstant()
                val to = hasta.plusDays(1).atStartOfDay(zone).toInstant()
                from to to
            }
        }
    }

    /**
     * Bucket size in milliseconds for time-series aggregation.
     *
     * - ≤ 31 days: daily (86_400_000 ms)
     * - > 31 days: monthly (~30 days = 2_592_000_000 ms)
     *
     * [Ultimos3Meses] always returns weekly (604_800_000 ms).
     */
    fun bucketMillis(): Long = when (this) {
        EsteMes -> DAILY_MILLIS
        Ultimos3Meses -> WEEKLY_MILLIS
        UltimoAnio -> MONTHLY_MILLIS
        is Personalizado -> {
            val days = ChronoUnit.DAYS.between(desde, hasta)
            if (days <= 31) DAILY_MILLIS else MONTHLY_MILLIS
        }
    }

    data object EsteMes : StatsPeriod() {
        override val displayName: String = "Este mes"
    }

    data object Ultimos3Meses : StatsPeriod() {
        override val displayName: String = "Últimos 3 meses"
    }

    data object UltimoAnio : StatsPeriod() {
        override val displayName: String = "Último año"
    }

    data class Personalizado(
        val desde: LocalDate,
        val hasta: LocalDate,
    ) : StatsPeriod() {
        override val displayName: String = "Personalizado"
    }

    companion object {
        /** All preset periods (excludes [Personalizado]). */
        val presets: List<StatsPeriod> = listOf(EsteMes, Ultimos3Meses, UltimoAnio)

        private const val DAILY_MILLIS = 86_400_000L
        private const val WEEKLY_MILLIS = 604_800_000L
        private const val MONTHLY_MILLIS = 2_592_000_000L
    }
}
