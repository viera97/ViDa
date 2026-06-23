package com.vida.feature.expenselist

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vida.domain.model.Category
import com.vida.domain.model.ExpenseFilter
import com.vida.domain.model.SourceType

/**
 * Root composable for the expense list screen with filtering, search, and pull-to-refresh.
 *
 * Layout (top to bottom):
 * - TopAppBar ("Gastos")
 * - Search bar (OutlinedTextField, debounced via ViewModel)
 * - Active filter chips row + "Filtrar" button
 * - LazyColumn with expense items + "Cargar más" / "No hay más gastos"
 * - Pull-to-refresh wrapping the LazyColumn
 * - Filter bottom sheet (modal)
 *
 * @param onNavigateToDetail Invoked when a row is tapped → navigates to "expense/{id}".
 * @param onNavigateBack Back navigation via toolbar arrow.
 * @param viewModel Injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilterSheet by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Collect navigation events.
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToDetail -> onNavigateToDetail(event.expenseId)
            }
        }
    }

    // Collect snackbar events (pagination/refresh/filter errors).
    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gastos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is ExpenseListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ExpenseListUiState.Ready -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // ── Search bar ──────────────────────────────────────
                        SearchBar(
                            query = state.filters.searchQuery ?: "",
                            onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        )

                        // ── Filter chips row ────────────────────────────────
                        FilterChipsRow(
                            filter = state.filters,
                            categories = viewModel.categoriesMap,
                            onDismissChip = { key -> viewModel.onDismissFilterChip(key) },
                            onOpenFilterSheet = { showFilterSheet = true },
                        )

                        // ── List with pull-to-refresh ───────────────────────
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = { viewModel.onRefresh() },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(
                                    items = state.items,
                                    key = { it.id },
                                ) { item ->
                                    ExpenseListItem(
                                        item = item,
                                        onClick = { viewModel.onExpenseTap(item.id) },
                                    )
                                }

                                // "Cargar más" button
                                item {
                                    if (state.hasMore) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Button(
                                                onClick = { viewModel.onLoadMore() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                ),
                                            ) {
                                                Text("Cargar más")
                                            }
                                        }
                                    } else if (state.items.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "No hay más gastos",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is ExpenseListUiState.Empty -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SearchBar(
                            query = state.filters.searchQuery ?: "",
                            onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        )
                        FilterChipsRow(
                            filter = state.filters,
                            categories = viewModel.categoriesMap,
                            onDismissChip = { key -> viewModel.onDismissFilterChip(key) },
                            onOpenFilterSheet = { showFilterSheet = true },
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (state.noFiltersActive) {
                                        "No hay gastos"
                                    } else {
                                        "Sin resultados para estos filtros"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (state.noFiltersActive) {
                                        "Los gastos que registres aparecerán aquí"
                                    } else {
                                        "Probá cambiando los filtros"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                is ExpenseListUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.onRefresh() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Filter modal bottom sheet ───────────────────────────────────────────
    if (showFilterSheet) {
        ExpenseFilterSheet(
            currentFilter = when (val s = uiState) {
                is ExpenseListUiState.Ready -> s.filters
                is ExpenseListUiState.Empty -> s.filters
                else -> ExpenseFilter()
            },
            categories = viewModel.categoriesMap.values.toList(),
            onApply = { filter ->
                viewModel.onFilterChanged(filter)
                showFilterSheet = false
            },
            onClear = {
                viewModel.onClearAllFilters()
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false },
            sheetState = filterSheetState,
        )
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Buscar gastos...") },
        leadingIcon = {
            Text(
                text = "🔍",
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        singleLine = true,
    )
}

@Composable
private fun FilterChipsRow(
    filter: ExpenseFilter,
    categories: Map<Long, Category>,
    onDismissChip: (String) -> Unit,
    onOpenFilterSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasActiveFilters = filter.dateFrom != null ||
        filter.dateTo != null ||
        !filter.categoryIds.isNullOrEmpty() ||
        filter.currency != null ||
        filter.sourceType != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(onClick = onOpenFilterSheet) {
            Text("Filtrar")
        }

        if (hasActiveFilters) {
            Spacer(modifier = Modifier.width(4.dp))

            // Date range chip
            if (filter.dateFrom != null || filter.dateTo != null) {
                val label = buildString {
                    if (filter.dateFrom != null) append("Desde")
                    if (filter.dateFrom != null && filter.dateTo != null) append(" - ")
                    if (filter.dateTo != null) append("Hasta")
                }
                DismissibleChip(
                    label = label,
                    onDismiss = { onDismissChip("date") },
                )
            }

            // Category chips
            filter.categoryIds?.forEach { catId ->
                val cat = categories[catId]
                DismissibleChip(
                    label = cat?.name ?: "Cat #$catId",
                    onDismiss = { onDismissChip(catId.toString()) },
                )
            }

            // Currency chip
            filter.currency?.let { currency ->
                DismissibleChip(
                    label = currency.code,
                    onDismiss = { onDismissChip("currency") },
                )
            }

            // Source type chip
            filter.sourceType?.let { sourceType ->
                val label = when (sourceType) {
                    SourceType.WALLET -> "Billetera"
                    SourceType.CARD -> "Tarjeta"
                    SourceType.STASH -> "Reserva"
                }
                DismissibleChip(
                    label = label,
                    onDismiss = { onDismissChip("sourceType") },
                )
            }
        }
    }
}

@Composable
private fun DismissibleChip(
    label: String,
    onDismiss: () -> Unit,
) {
    InputChip(
        selected = false,
        onClick = onDismiss,
        label = { Text(label) },
        trailingIcon = {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(18.dp),
            ) {
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
    )
}
