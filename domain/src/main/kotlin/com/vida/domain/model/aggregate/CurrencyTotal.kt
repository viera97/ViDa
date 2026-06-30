package com.vida.domain.model.aggregate

/**
 * Result of expense or income aggregation grouped by currency only.
 *
 * Returned by [com.vida.data.db.dao.ExpenseDao.getExpenseTotalsByCurrency]
 * and [com.vida.data.db.dao.IncomeDao.getIncomeTotalsByCurrency].
 * Used as input for [com.vida.domain.usecase.statistics.GetCurrencyComposition].
 *
 * @property currency ISO-ish currency code (CUP, USD, MLC).
 * @property totalMinor Sum of amounts in minor units.
 */
data class CurrencyTotal(
    val currency: String,
    val totalMinor: Long,
)
