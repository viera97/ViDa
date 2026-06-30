package com.vida.feature.reports.model

import com.vida.domain.model.Currency
import java.time.Instant

/**
 * Pure UI projection of [com.vida.domain.model.statistics.PeriodReportEntry].
 *
 * All labels are pre-formatted via [com.vida.core.format.formatMoney] and
 * [com.vida.core.format.PeriodLabels] — no domain `Money` arithmetic at render time.
 *
 * @property periodStart UTC start of the bucket (for LazyColumn key derivation).
 * @property periodLabel Localized Spanish label for the bucket header.
 * @property categoryRows One entry per (categoryId, currency) pair.
 * @property incomeRows Aggregated income per currency (primary tint).
 * @property expenseRows Aggregated expense per currency (error tint).
 * @property netRows Per-currency net; sign drives color (positive → primary,
 *   negative → error, zero → onSurfaceVariant).
 */
data class ReportListItem(
    val periodStart: Instant,
    val periodLabel: String,
    val categoryRows: List<CategoryRow>,
    val incomeRows: List<MoneyRow>,
    val expenseRows: List<MoneyRow>,
    val netRows: List<MoneyRow>,
)

/** Per-(category, currency) row inside [ReportListItem.categoryRows]. */
data class CategoryRow(
    val categoryName: String,
    val currency: Currency,
    val amountLabel: String,
)

/** Pre-formatted currency amount row (Ingresos / Gastos / Neto sections). */
data class MoneyRow(
    val currency: Currency,
    val amountLabel: String,
    val isNegative: Boolean = false,
)
