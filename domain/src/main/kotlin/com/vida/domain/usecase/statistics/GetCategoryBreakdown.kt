package com.vida.domain.usecase.statistics

import com.vida.domain.model.Category
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.statistics.CategoryBreakdown
import com.vida.domain.repository.CategoryRepository
import com.vida.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Computes the expense category breakdown for the given time range.
 *
 * Joins aggregated expense totals with category metadata (name, color, icon)
 * and maps the raw [CategoryExpenseTotal] rows into [CategoryBreakdown] domain models.
 */
class GetCategoryBreakdown(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(from: Instant, to: Instant): List<CategoryBreakdown> {
        val totals = expenseRepository.getExpenseTotalsByCategory(from, to)
        if (totals.isEmpty()) return emptyList()

        val categories: Map<Long, Category> =
            categoryRepository.getAll().first().associateBy { it.id }

        return totals.map { (categoryId, currencyCode, totalMinor) ->
            val category = categories[categoryId]
            CategoryBreakdown(
                categoryId = categoryId,
                categoryName = category?.name ?: "Unknown",
                color = category?.color ?: 0,
                icon = category?.icon,
                total = toMoney(totalMinor, currencyCode),
            )
        }
    }

    private fun toMoney(totalMinor: Long, currencyCode: String): Money {
        val currency = Currency.fromCode(currencyCode)
        return Money.fromMinorUnits(totalMinor, currency)
    }
}
