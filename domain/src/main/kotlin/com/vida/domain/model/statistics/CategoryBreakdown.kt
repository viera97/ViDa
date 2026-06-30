package com.vida.domain.model.statistics

import com.vida.domain.model.Money

/**
 * Expense totals broken down by category for the selected period.
 *
 * @property categoryId The category row id.
 * @property categoryName Display name of the category.
 * @property color ARGB color int for UI tinting.
 * @property icon Material icon resource name (nullable).
 * @property total Aggregated expense amount in the original currency.
 */
data class CategoryBreakdown(
    val categoryId: Long,
    val categoryName: String,
    val color: Int,
    val icon: String?,
    val total: Money,
)
