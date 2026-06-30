package com.vida.domain.model.aggregate

/**
 * Result of expense aggregation grouped by period bucket and currency.
 *
 * Returned by [com.vida.data.db.dao.ExpenseDao.getExpenseTotalsByPeriod].
 * Used as input for [com.vida.domain.usecase.statistics.GetCashFlowTrend].
 *
 * @property periodStart Epoch millis of the period bucket start.
 * @property currency ISO-ish currency code (CUP, USD, MLC).
 * @property totalMinor Sum of (real_amount_minor ?: amount_minor) in minor units.
 */
data class PeriodExpenseTotal(
    val periodStart: Long,
    val currency: String,
    val totalMinor: Long,
)
