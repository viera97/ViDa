package com.vida.domain.model.statistics

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import java.time.Instant

/**
 * One period bucket of reportable data, output by [com.vida.domain.usecase.statistics.GetPeriodReports].
 *
 * All currency maps are non-null but may be empty when that side has no data in the bucket.
 * Net is computed per-currency inside the use case — never across currencies (Money.plus/minus
 * throw on currency mismatch).
 *
 * @property periodStart Start of the time bucket (UTC-aligned epoch millis).
 * @property categoryBreakdown Per-(category, currency) expense rows for this bucket; empty when
 *   the bucket has income only (no expenses).
 * @property incomeByCurrency Aggregated income per currency; empty when no income in the bucket.
 * @property expenseByCurrency Aggregated expense per currency; empty when no expense in the bucket.
 * @property netByCurrency Per-currency net (income - expense); only currencies with at least
 *   one side present are included.
 */
data class PeriodReportEntry(
    val periodStart: Instant,
    val categoryBreakdown: List<CategoryBreakdown>,
    val incomeByCurrency: Map<Currency, Money>,
    val expenseByCurrency: Map<Currency, Money>,
    val netByCurrency: Map<Currency, Money>,
)
