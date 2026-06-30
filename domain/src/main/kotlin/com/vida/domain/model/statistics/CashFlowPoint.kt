package com.vida.domain.model.statistics

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import java.time.Instant

/**
 * A single point in the expense/income time-series evolution.
 *
 * Each side is a map keyed by [Currency] because a period bucket (e.g. a day)
 * can contain transactions in multiple currencies. Maps are never empty — when
 * no data exists for a side the map is absent from the data structure (null).
 *
 * @property periodStart Start of the time bucket (daily, weekly, or monthly).
 * @property expenseTotal Aggregated expense amounts per currency, or null if no expenses.
 * @property incomeTotal Aggregated income amounts per currency, or null if no incomes.
 */
data class CashFlowPoint(
    val periodStart: Instant,
    val expenseTotal: Map<Currency, Money>?,
    val incomeTotal: Map<Currency, Money>?,
)
