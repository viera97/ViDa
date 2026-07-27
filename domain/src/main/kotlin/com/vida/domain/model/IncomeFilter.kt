package com.vida.domain.model

import java.time.Instant

/**
 * Filter criteria for the [com.vida.domain.usecase.income.SearchIncomes] use case.
 * Mirrors [ExpenseFilter] but drops `categoryIds` (incomes have no category).
 * All fields are nullable — null means "no filter on this dimension".
 *
 * @property dateFrom Inclusive start of the date range (date_time >= dateFrom)
 * @property dateTo Exclusive end of the date range (date_time < dateTo)
 * @property currency Currency code string (e.g. "CUP", "USD") — null means "all currencies"
 * @property sourceType Match incomes whose polymorphic destination column corresponds to this
 * @property searchQuery Free-text filter on description (LIKE '%term%')
 */
data class IncomeFilter(
    val dateFrom: Instant? = null,
    val dateTo: Instant? = null,
    val currency: String? = null,
    val sourceType: SourceType? = null,
    val searchQuery: String? = null,
)
