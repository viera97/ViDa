package com.vida.feature.incomelist

import com.vida.domain.model.IncomeFilter
import com.vida.domain.model.SourceType

/**
 * UI state exposed by [IncomeListViewModel].
 *
 * Mirrors [com.vida.feature.expenselist.ExpenseListUiState] but without
 * category filtering. Supports manual offset pagination, debounced search,
 * and filter chips.
 *
 * State transitions:
 * ```
 * Loading → Ready | Empty | Error
 * Ready → Ready (pagination, refresh, filter changes)
 * ```
 */
sealed interface IncomeListUiState {

    data object Loading : IncomeListUiState

    data class Ready(
        val items: List<IncomeListItem>,
        val hasMore: Boolean,
        val filter: IncomeFilter = IncomeFilter(),
        val searchQuery: String = "",
        val isRefreshing: Boolean = false,
    ) : IncomeListUiState

    /**
     * No incomes to display.
     *
     * @param noFiltersActive When `true`, there are genuinely no incomes at all.
     *   When `false`, the active filters matched zero results.
     * @param filter The active filter (for the filter sheet).
     */
    data class Empty(
        val noFiltersActive: Boolean = true,
        val filter: IncomeFilter = IncomeFilter(),
    ) : IncomeListUiState

    data class Error(val message: String) : IncomeListUiState
}

/**
 * Pre-formatted display model for a single income row in Compose.
 *
 * Mirrors [com.vida.feature.expenselist.ExpenseListItem] minus category fields
 * (incomes have no category).
 */
data class IncomeListItem(
    val id: Long,
    val description: String,
    val amountFormatted: String,
    val dateFormatted: String,
    val absoluteDateFormatted: String,
    val sourceLabel: String,
    val sourceType: SourceType,
    val currencyCode: String,
)
