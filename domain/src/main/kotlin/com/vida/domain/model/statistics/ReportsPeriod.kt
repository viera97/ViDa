package com.vida.domain.model.statistics

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Granularity selector for the Reports screen.
 *
 * Sibling of [com.vida.feature.statistics.model.StatsPeriod] — both model time
 * ranges but with different UX semantics (Reports groups transactions by user-chosen
 * granularity; Statistics aggregates into chart buckets). If you change one, audit
 * the other.
 */
sealed class ReportsPeriod {

    /** Human-readable label shown in the granularity selector UI. */
    abstract val displayName: String

    /**
     * Returns the (from, to) instant range for this period.
     *
     * The `to` instant is exclusive — it represents the start of the day after the range,
     * matching the Room query convention `WHERE date_time >= :from AND date_time < :to`.
     */
    fun toDateRange(clock: Clock = Clock.systemDefaultZone()): Pair<Instant, Instant> {
        val today = LocalDate.now(clock)
        val zone = ZoneId.systemDefault()
        return when (this) {
            Hoy -> {
                val from = today.atStartOfDay(zone).toInstant()
                val to = today.plusDays(1).atStartOfDay(zone).toInstant()
                from to to
            }
            Semanal -> {
                val from = today.minusWeeks(12).atStartOfDay(zone).toInstant()
                val to = today.plusDays(1).atStartOfDay(zone).toInstant()
                from to to
            }
            Mensual -> {
                val from = today.minusMonths(12).atStartOfDay(zone).toInstant()
                val to = today.plusDays(1).atStartOfDay(zone).toInstant()
                from to to
            }
            Anual -> {
                val from = today.minusYears(5).atStartOfDay(zone).toInstant()
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
     * Bucket size in milliseconds for SQL `GROUP BY` aggregation.
     *
     * - [Hoy] → daily (86_400_000 ms)
     * - [Semanal] → weekly (604_800_000 ms)
     * - [Mensual] → monthly (~30d = 2_592_000_000 ms)
     * - [Anual] → yearly (~365d = 31_536_000_000 ms)
     * - [Personalizado] → daily if span ≤ 31 days, else monthly (matches [com.vida.feature.statistics.model.StatsPeriod.bucketMillis])
     */
    fun bucketMillis(): Long = when (this) {
        Hoy -> DAILY_MILLIS
        Semanal -> WEEKLY_MILLIS
        Mensual -> MONTHLY_MILLIS
        Anual -> YEARLY_MILLIS
        is Personalizado -> {
            val days = ChronoUnit.DAYS.between(desde, hasta)
            if (days <= 31) DAILY_MILLIS else MONTHLY_MILLIS
        }
    }

    data object Hoy : ReportsPeriod() {
        override val displayName: String = "Hoy"
    }

    data object Semanal : ReportsPeriod() {
        override val displayName: String = "Semanal"
    }

    data object Mensual : ReportsPeriod() {
        override val displayName: String = "Mensual"
    }

    data object Anual : ReportsPeriod() {
        override val displayName: String = "Anual"
    }

    data class Personalizado(
        val desde: LocalDate,
        val hasta: LocalDate,
    ) : ReportsPeriod() {
        override val displayName: String = "Personalizado"
    }

    companion object {
        /** All preset periods (excludes [Personalizado]). */
        val presets: List<ReportsPeriod> = listOf(Hoy, Semanal, Mensual, Anual)

        private const val DAILY_MILLIS = 86_400_000L
        private const val WEEKLY_MILLIS = 604_800_000L
        private const val MONTHLY_MILLIS = 2_592_000_000L
        private const val YEARLY_MILLIS = 31_536_000_000L
    }
}
