package com.vida.feature.expenselist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.core.format.formatMoney
import com.vida.core.format.toRelativeDateString
import com.vida.domain.model.Category
import com.vida.domain.model.Expense
import com.vida.domain.model.ExpenseFilter
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.expense.SearchExpenses
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.GetWallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the expense list screen.
 *
 * Manages manual offset pagination (page size = 20), loads source/category
 * labels on init, exposes navigation events via a [Channel], and supports
 * combined filtering + debounced full-text search.
 */
@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val searchExpenses: SearchExpenses,
    private val listCategories: ListCategories,
    private val listCards: ListCards,
    private val listStashes: ListStashes,
    private val getWallet: GetWallet,
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    private val _uiState = MutableStateFlow<ExpenseListUiState>(ExpenseListUiState.Loading)
    val uiState: StateFlow<ExpenseListUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    /** One-shot error messages for snackbar (pagination/refresh/filter failures). */
    private val _snackbarEvents = Channel<String>(Channel.BUFFERED)
    val snackbarEvents = _snackbarEvents.receiveAsFlow()

    /** Current page offset for manual pagination. */
    private var currentOffset = 0

    /** Whether more pages exist (results.size == PAGE_SIZE). */
    private var hasMore = true

    /** Current filter state — all nulls by default (show all expenses). */
    private var currentFilter: ExpenseFilter = ExpenseFilter()

    /** Cache of all categories for label/color lookup and filter sheet. */
    private var categories: Map<Long, Category> = emptyMap()

    /** Source label cache: key is "sourceType:sourceId" → label string. */
    private var sourceLabels: Map<String, String> = emptyMap()

    /** Exposed for filter chip display — category name lookup by id. */
    val categoriesMap: Map<Long, Category> get() = categories

    /** Debounced search query input from the UI. */
    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _uiState.value = ExpenseListUiState.Loading
            try {
                // Load metadata in parallel.
                val cats = listCategories().first()
                getWallet()
                val cards = listCards().first()
                val stashes = listStashes().first()

                categories = cats.associateBy { it.id }

                sourceLabels = buildMap {
                    put("WALLET:null", "Billetera")
                    for (card in cards) put("CARD:${card.id}", card.bank)
                    for (stash in stashes) put("STASH:${stash.id}", stash.name)
                }

                fetchPage(offset = 0, merge = false)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = ExpenseListUiState.Error(
                    t.message ?: "No se pudieron cargar los gastos",
                )
            }
        }

        // Debounced search: emits after 300ms of no further input.
        // drop(1) skips the initial empty value so we don't refetch on setup.
        viewModelScope.launch {
            _searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .drop(1)
                .collect { query ->
                    val newFilter = currentFilter.copy(
                        searchQuery = query.ifBlank { null },
                    )
                    if (newFilter != currentFilter) {
                        applyFilter(newFilter)
                    }
                }
        }
    }

    // ── Public actions ───────────────────────────────────────────────────────

    /** Append the next page to the existing list. */
    fun onLoadMore() {
        val current = _uiState.value
        if (current !is ExpenseListUiState.Ready || !current.hasMore) return
        viewModelScope.launch {
            fetchPage(offset = currentOffset, merge = true)
        }
    }

    /** Reset to page 1 and refetch preserving current filters. */
    fun onRefresh() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current is ExpenseListUiState.Ready) {
                _uiState.value = current.copy(isRefreshing = true)
            }
            currentOffset = 0
            fetchPage(offset = 0, merge = false)
        }
    }

    /** Emit navigation event to the expense detail screen. */
    fun onExpenseTap(expenseId: Long) {
        viewModelScope.launch {
            _navigationEvents.send(NavigationEvent.NavigateToDetail(expenseId))
        }
    }

    // ── Filter / search actions ──────────────────────────────────────────────

    /**
     * Replace the current filter entirely (e.g. from the filter sheet) and
     * refetch page 1.
     */
    fun onFilterChanged(filter: ExpenseFilter) {
        if (filter == currentFilter) return
        applyFilter(filter)
    }

    /**
     * Push a new search query from the text field to the debounce pipeline.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Remove a single active filter identified by [key] and refetch.
     *
     * Valid keys:
     * - "date" → clears dateFrom/dateTo
     * - "currency" → clears currency
     * - "sourceType" → clears sourceType
     * - Any numeric string → clears that category id from categoryIds
     */
    fun onDismissFilterChip(key: String) {
        val newFilter = when (key) {
            "date" -> currentFilter.copy(dateFrom = null, dateTo = null)
            "currency" -> currentFilter.copy(currency = null)
            "sourceType" -> currentFilter.copy(sourceType = null)
            else -> {
                val catId = key.toLongOrNull() ?: return
                val updated = currentFilter.categoryIds
                    ?.minus(catId)
                    ?.takeIf { it.isNotEmpty() }
                currentFilter.copy(categoryIds = updated)
            }
        }
        if (newFilter != currentFilter) {
            applyFilter(newFilter)
        }
    }

    /** Clear ALL active filters and refetch. */
    fun onClearAllFilters() {
        if (currentFilter == ExpenseFilter()) return
        _searchQuery.value = ""
        applyFilter(ExpenseFilter())
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /** Apply a new filter, reset pagination, and refetch from page 1. */
    private fun applyFilter(filter: ExpenseFilter) {
        currentFilter = filter
        currentOffset = 0
        hasMore = true
        viewModelScope.launch {
            fetchPage(offset = 0, merge = false)
        }
    }

    /**
     * Fetches a page of expenses from [searchExpenses] and updates the UI state.
     *
     * @param offset Starting offset for this fetch.
     * @param merge When `true`, appends results to the existing list; otherwise replaces.
     */
    private suspend fun fetchPage(offset: Int, merge: Boolean) {
        val results = try {
            searchExpenses(currentFilter, limit = PAGE_SIZE, offset = offset)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            handleFetchError(merge)
            return
        }

        hasMore = results.size == PAGE_SIZE
        currentOffset = offset + results.size

        val items = results.map { it.toListItem() }
        val current = _uiState.value

        val mergedItems = if (merge && current is ExpenseListUiState.Ready) {
            current.items + items
        } else {
            items
        }

        _uiState.value = when {
            mergedItems.isEmpty() -> ExpenseListUiState.Empty(
                noFiltersActive = currentFilter == ExpenseFilter(),
                filters = currentFilter,
            )
            else -> ExpenseListUiState.Ready(
                items = mergedItems,
                hasMore = hasMore,
                filters = currentFilter,
                isRefreshing = false,
            )
        }
    }

    /**
     * Handles a fetch error. On initial load (no prior Ready state),
     * emits [ExpenseListUiState.Error]. On pagination/refresh/filter,
     * preserves the existing list and sends a snackbar event.
     */
    private fun handleFetchError(merge: Boolean) {
        val current = _uiState.value
        if (current is ExpenseListUiState.Ready) {
            if (!merge) {
                _uiState.value = current.copy(isRefreshing = false)
            }
            viewModelScope.launch {
                _snackbarEvents.send("No se pudieron cargar los gastos")
            }
        } else {
            _uiState.value = ExpenseListUiState.Error(
                "No se pudieron cargar los gastos",
            )
        }
    }

    /**
     * Maps a domain [Expense] to a pre-formatted [ExpenseListItem].
     */
    private fun Expense.toListItem(): ExpenseListItem {
        val cat = categories[categoryId]
        val sourceKey = "$sourceType:${sourceId}"
        val now = java.time.Clock.systemDefaultZone().instant()
        val relative = dateTime.toString().toRelativeDateString(now)
        val absolute = java.time.format.DateTimeFormatter
            .ofPattern("dd MMM yyyy", java.util.Locale("es", "ES"))
            .format(dateTime.atZone(java.time.ZoneId.systemDefault()).toLocalDate())

        return ExpenseListItem(
            id = id,
            description = description,
            amountFormatted = formatMoney(amount),
            dateFormatted = relative,
            absoluteDateFormatted = absolute,
            categoryName = cat?.name ?: "Sin categoría",
            categoryColor = cat?.color ?: 0xFF9E9E9E.toInt(),
            sourceLabel = sourceLabels[sourceKey] ?: sourceType.name,
            sourceType = sourceType,
            currencyCode = amount.currency.code,
        )
    }
}
