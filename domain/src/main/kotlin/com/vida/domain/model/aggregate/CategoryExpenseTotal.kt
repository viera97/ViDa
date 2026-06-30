package com.vida.domain.model.aggregate

/**
 * Result of expense aggregation grouped by category and currency.
 *
 * Returned by [com.vida.data.db.dao.ExpenseDao.getExpenseTotalsByCategory].
 * Used as input for [com.vida.domain.usecase.statistics.GetCategoryBreakdown].
 *
 * @property categoryId The category row id.
 * @property currency ISO-ish currency code (CUP, USD, MLC).
 * @property totalMinor Sum of (real_amount_minor ?: amount_minor) in minor units.
 */
data class CategoryExpenseTotal(
    val categoryId: Long,
    val currency: String,
    val totalMinor: Long,
)
