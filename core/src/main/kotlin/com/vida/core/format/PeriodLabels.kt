package com.vida.core.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DAILY_MILLIS = 86_400_000L
private const val MONTHLY_MILLIS = 2_592_000_000L
private const val YEARLY_MILLIS = 31_536_000_000L

private val ES_LOCALE: Locale = Locale.forLanguageTag("es-ES")

private val dailyFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d MMM yyyy", ES_LOCALE)

private val monthlyFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", ES_LOCALE)

private val yearlyFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy", ES_LOCALE)

private val fallbackFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy", ES_LOCALE)

/**
 * Format-helper utilities for the Reports screen granularity labels.
 *
 * Produces a localized Spanish label for a given [Instant] based on the bucket size
 * used by [com.vida.domain.model.statistics.ReportsPeriod.bucketMillis].
 *
 * - Daily (`~1d`) → `EEEE d MMM yyyy` — e.g. "Lunes 29 jun 2026"
 * - Monthly (`~30d`) → `MMMM yyyy` — e.g. "Junio 2026"
 * - Yearly (`~365d`) → `yyyy` — e.g. "2026"
 * - Else → `dd MMM yyyy` fallback
 *
 * Uses [ZoneId.systemDefault] for display alignment (separate from the UTC-aligned
 * epoch-millis math in the DAO query).
 */
object PeriodLabels {

    /**
     * Returns the human-readable Spanish label for [instant] at the given [bucketMillis].
     *
     * @param instant UTC epoch millis of the bucket start.
     * @param bucketMillis Bucket size in millis (e.g. 86_400_000L for daily).
     */
    fun formatPeriodLabel(instant: Instant, bucketMillis: Long): String {
        val zone = ZoneId.systemDefault()
        val date = instant.atZone(zone)
        val formatter = when {
            bucketMillis <= DAILY_MILLIS + (DAILY_MILLIS / 2) -> dailyFormatter
            bucketMillis <= MONTHLY_MILLIS + (MONTHLY_MILLIS / 2) -> monthlyFormatter
            bucketMillis <= YEARLY_MILLIS + (YEARLY_MILLIS / 2) -> yearlyFormatter
            else -> fallbackFormatter
        }
        return formatter.format(date).replaceFirstChar { it.titlecase(ES_LOCALE) }
    }
}
