package com.vida.feature.incomelist

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vida.domain.model.IncomeFilter
import com.vida.domain.model.SourceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: IncomeListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilterSheet by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Collect snackbar events (pagination/refresh/filter errors).
    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Refresh the list when the screen returns to the foreground (e.g. after
    // deleting an income on the detail screen). Same pattern as
    // ExpenseListScreen — the query is one-shot (@RawQuery), not a Flow.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ingresos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
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
                is IncomeListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is IncomeListUiState.Ready -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        )
                        FilterChipsRow(
                            filter = state.filter,
                            onDismissChip = { key -> viewModel.onDismissFilterChip(key) },
                            onOpenFilterSheet = { showFilterSheet = true },
                        )
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
                                    IncomeListItem(
                                        item = item,
                                        onClick = { onNavigateToDetail(item.id) },
                                    )
                                }
                                item {
                                    if (state.hasMore) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp, vertical = 8.dp),
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
                                                text = "No hay más ingresos",
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

                is IncomeListUiState.Empty -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SearchBar(
                            query = "",
                            onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        )
                        FilterChipsRow(
                            filter = state.filter,
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
                                    text = if (state.noFiltersActive) "No hay ingresos"
                                    else "Sin resultados para estos filtros",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (state.noFiltersActive) "Los ingresos que registres aparecerán aquí"
                                    else "Probá cambiando los filtros",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                is IncomeListUiState.Error -> {
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

    if (showFilterSheet) {
        IncomeFilterSheet(
            currentFilter = when (val s = uiState) {
                is IncomeListUiState.Ready -> s.filter
                is IncomeListUiState.Empty -> s.filter
                else -> IncomeFilter()
            },
            availableCurrencies = viewModel.currencyCodes,
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
        placeholder = { Text("Buscar ingresos...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
            )
        },
        singleLine = true,
    )
}

@Composable
private fun FilterChipsRow(
    filter: IncomeFilter,
    onDismissChip: (String) -> Unit,
    onOpenFilterSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasActiveFilters = filter.dateFrom != null ||
        filter.dateTo != null ||
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

            if (filter.dateFrom != null || filter.dateTo != null) {
                val label = buildString {
                    if (filter.dateFrom != null) append("Desde")
                    if (filter.dateFrom != null && filter.dateTo != null) append(" - ")
                    if (filter.dateTo != null) append("Hasta")
                }
                DismissibleChip(label = label, onDismiss = { onDismissChip("date") })
            }

            filter.currency?.let { currency ->
                DismissibleChip(label = currency, onDismiss = { onDismissChip("currency") })
            }

            filter.sourceType?.let { sourceType ->
                val label = when (sourceType) {
                    SourceType.WALLET -> "Billetera"
                    SourceType.CARD -> "Tarjeta"
                    SourceType.STASH -> "Reserva"
                }
                DismissibleChip(label = label, onDismiss = { onDismissChip("sourceType") })
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
