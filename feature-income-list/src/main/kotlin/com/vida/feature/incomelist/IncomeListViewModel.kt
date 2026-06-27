package com.vida.feature.incomelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.core.format.formatMoney
import com.vida.core.format.toRelativeDateString
import com.vida.domain.model.IncomeFilter
import com.vida.domain.model.Income
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.income.SearchIncomes
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.ListWallets
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the income list screen with search, filter, and pagination.
 *
 * Mirrors [com.vida.feature.expenselist.ExpenseListViewModel] but simpler:
 * no category filters. Uses manual offset pagination over [SearchIncomes]
 * (a @RawQuery one-shot), with debounced search and filter chips.
 */
@HiltViewModel
class IncomeListViewModel @Inject constructor(
    private val searchIncomes: SearchIncomes,
    private val listCards: ListCards,
    private val listStashes: ListStashes,
    private val listWallets: ListWallets,
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    private val _uiState = MutableStateFlow<IncomeListUiState>(IncomeListUiState.Loading)
    val uiState: StateFlow<IncomeListUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = Channel<String>(Channel.BUFFERED)
    val snackbarEvents = _snackbarEvents.receiveAsFlow()

    private var currentOffset = 0
    private var hasMore = true
    private var currentFilter: IncomeFilter = IncomeFilter()

    /** Source label cache: "WALLET:1" → "Efectivo", "CARD:2" → "Mi BPA", etc. */
    private var sourceLabels: Map<String, String> = emptyMap()

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _uiState.value = IncomeListUiState.Loading
            try {
                val wallets = listWallets().first()
                val cards = listCards().first()
                val stashes = listStashes().first()

                sourceLabels = buildMap {
                    for (wallet in wallets) put("WALLET:${wallet.id}", wallet.name)
                    for (card in cards) {
                        put("CARD:${card.id}", card.note?.takeIf { it.isNotBlank() } ?: card.bank)
                    }
                    for (stash in stashes) put("STASH:${stash.id}", stash.name)
                }

                fetchPage(offset = 0, merge = false)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = IncomeListUiState.Error(
                    t.message ?: "No se pudieron cargar los ingresos",
                )
            }
        }

        // Debounced search
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

    fun onLoadMore() {
        val current = _uiState.value
        if (current !is IncomeListUiState.Ready || !current.hasMore) return
        viewModelScope.launch {
            fetchPage(offset = currentOffset, merge = true)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current is IncomeListUiState.Ready) {
                _uiState.value = current.copy(isRefreshing = true)
            }
            currentOffset = 0
            fetchPage(offset = 0, merge = false)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChanged(filter: IncomeFilter) {
        applyFilter(filter)
    }

    fun onClearAllFilters() {
        applyFilter(IncomeFilter(searchQuery = currentFilter.searchQuery))
    }

    fun onDismissFilterChip(key: String) {
        val newFilter = when (key) {
            "date" -> currentFilter.copy(dateFrom = null, dateTo = null)
            "currency" -> currentFilter.copy(currency = null)
            "sourceType" -> currentFilter.copy(sourceType = null)
            else -> {
                // Unrecognised key — no-op.
                return
            }
        }
        applyFilter(newFilter)
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun applyFilter(filter: IncomeFilter) {
        currentFilter = filter
        currentOffset = 0
        viewModelScope.launch {
            fetchPage(offset = 0, merge = false)
        }
    }

    private suspend fun fetchPage(offset: Int, merge: Boolean) {
        try {
            val results = searchIncomes(currentFilter, limit = PAGE_SIZE, offset = offset)
            hasMore = results.size == PAGE_SIZE
            currentOffset = offset + results.size

            val current = _uiState.value
            val existingItems: List<IncomeListItem> = if (merge && current is IncomeListUiState.Ready) {
                current.items
            } else {
                emptyList()
            }

            val mapped = results.map { income ->
                val label = sourceLabels["${income.sourceType.name}:${income.sourceId}"]
                    ?: when (income.sourceType) {
                        com.vida.domain.model.SourceType.WALLET -> "Billetera"
                        com.vida.domain.model.SourceType.CARD -> "Tarjeta"
                        com.vida.domain.model.SourceType.STASH -> "Ahorro"
                    }
                val zone = ZoneId.systemDefault()
                IncomeListItem(
                    id = income.id,
                    description = income.description,
                    amountFormatted = formatMoney(income.amount),
                    dateFormatted = income.dateTime.toString().toRelativeDateString(zone = zone),
                    absoluteDateFormatted = income.dateTime.atZone(zone).toLocalDate()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "ES"))),
                    sourceLabel = label,
                    sourceType = income.sourceType,
                    currencyCode = income.amount.currency.code,
                )
            }

            val allItems = if (merge) existingItems + mapped else mapped

            if (allItems.isEmpty() && !merge) {
                _uiState.value = IncomeListUiState.Empty(
                    noFiltersActive = currentFilter == IncomeFilter(),
                    filter = currentFilter,
                )
            } else {
                _uiState.value = IncomeListUiState.Ready(
                    items = allItems,
                    hasMore = hasMore,
                    filter = currentFilter,
                    searchQuery = _searchQuery.value,
                    isRefreshing = false,
                )
            }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            if (merge) {
                _snackbarEvents.send("No se pudo cargar más")
            } else {
                _uiState.value = IncomeListUiState.Error(
                    t.message ?: "No se pudieron cargar los ingresos",
                )
            }
        }
    }
}
