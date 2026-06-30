package com.vida.domain.model.aggregate

/**
 * Result of expense aggregation grouped by period bucket, category, and currency.
 *
 * Returned by [com.vida.data.db.dao.ExpenseDao.getExpenseCategoryTotalsByPeriod].
 * Used as input for [com.vida.domain.usecase.statistics.GetPeriodReports].
 *
 * @property periodStart Epoch millis of the period bucket start (UTC-aligned).
 * @property categoryId The category row id.
 * @property currency ISO-ish currency code (CUP, USD, MLC).
 * @property totalMinor Sum of (real_amount_minor ?: amount_minor) in minor units.
 */
data class PeriodCategoryExpenseTotal(
    val periodStart: Long,
    val categoryId: Long,
    val currency: String,
    val totalMinor: Long,
)
