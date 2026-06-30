package com.vida.domain.model.statistics

import com.vida.domain.model.Currency
import com.vida.domain.model.Money

/**
 * Expense and income totals grouped by currency for the selected period.
 *
 * @property currency The currency these totals are denominated in.
 * @property expenseTotal Aggregated expense amount in this currency, or null if no expenses.
 * @property incomeTotal Aggregated income amount in this currency, or null if no incomes.
 */
data class CurrencyComposition(
    val currency: Currency,
    val expenseTotal: Money?,
    val incomeTotal: Money?,
)
