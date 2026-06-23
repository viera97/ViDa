package com.vida.feature.expenselist

import com.vida.domain.model.ExpenseFilter
import com.vida.domain.model.SourceType

/**
 * UI state exposed by [ExpenseListViewModel].
 *
 * State transitions:
 * ```
 * Loading → Ready | Empty | Error
 * Ready → Ready (pagination, refresh, filter changes)
 * ```
 */
sealed interface ExpenseListUiState {

    /** Emitted on init while the first page is being fetched. */
    data object Loading : ExpenseListUiState

    /** Expense list with optional pagination controls. */
    data class Ready(
        val items: List<ExpenseListItem>,
        val hasMore: Boolean,
        val filters: ExpenseFilter = ExpenseFilter(),
        val searchQuery: String = "",
        val isRefreshing: Boolean = false,
    ) : ExpenseListUiState

    /**
     * No expenses to display.
     *
     * @param noFiltersActive When `true`, there are genuinely no expenses at all.
     *   When `false`, the active filters matched zero results.
     * @param filters The active filter that produced this empty result (for the filter sheet).
     */
    data class Empty(
        val noFiltersActive: Boolean = true,
        val filters: ExpenseFilter = ExpenseFilter(),
    ) : ExpenseListUiState

    /** An error occurred — typically on initial load. */
    data class Error(val message: String) : ExpenseListUiState
}

/**
 * Pre-formatted display model for a single expense row in Compose.
 *
 * All text fields are already formatted by [ExpenseListViewModel] so the
 * composable can render them directly without calling formatters.
 *
 * @property id Row id — used for navigation on tap.
 * @property description Expense description, max 2 lines in UI.
 * @property amountFormatted Formatted amount with currency (e.g. "$1,500.00").
 * @property dateFormatted Relative date (e.g. "hace 2 días") for primary display.
 * @property absoluteDateFormatted Absolute date (e.g. "20 jun 2026") for secondary display.
 * @property categoryName Category display name.
 * @property categoryColor ARGB color int for the category dot.
 * @property sourceLabel Human-readable source label (e.g. "Billetera", "Banco kubo").
 * @property sourceType Which kind of source paid for this.
 * @property currencyCode The expense's currency code (CUP, USD, MLC).
 */
data class ExpenseListItem(
    val id: Long,
    val description: String,
    val amountFormatted: String,
    val dateFormatted: String,
    val absoluteDateFormatted: String,
    val categoryName: String,
    val categoryColor: Int,
    val sourceLabel: String,
    val sourceType: SourceType,
    val currencyCode: String,
)

/**
 * One-shot navigation event emitted by [ExpenseListViewModel] via a [Channel].
 */
sealed interface NavigationEvent {
    data class NavigateToDetail(val expenseId: Long) : NavigationEvent
}
