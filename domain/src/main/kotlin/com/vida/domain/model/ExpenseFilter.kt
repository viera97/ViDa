package com.vida.domain.model

import java.time.Instant

/**
 * Filter criteria for the [SearchExpenses] use case. All fields are nullable — null
 * means "no filter on this dimension". The dynamic WHERE builder in `:data` includes
 * only non-null fields in the generated SQL.
 *
 * @property dateFrom Inclusive start of the date range (date_time >= dateFrom)
 * @property dateTo Exclusive end of the date range (date_time < dateTo)
 * @property categoryIds Set of category IDs to include (category_id IN (...))
 * @property currency Match expenses where amount.currency equals this
 * @property sourceType Match expenses whose polymorphic source column corresponds to this
 * @property searchQuery Free-text filter on description (LIKE '%term%')
 */
data class ExpenseFilter(
    val dateFrom: Instant? = null,
    val dateTo: Instant? = null,
    val categoryIds: Set<Long>? = null,
    val currency: Currency? = null,
    val sourceType: SourceType? = null,
    val searchQuery: String? = null,
)
