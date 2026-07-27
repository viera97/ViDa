package com.vida.feature.ratemanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

/**
 * Root composable for the currency rate management screen.
 *
 * Layout (top to bottom):
 * - TopAppBar ("Tasas") with back button
 * - Content area: [LazyColumn] when [RateListUiState.Ready],
 *   centered message for [RateListUiState.Empty],
 *   error message + retry for [RateListUiState.Error],
 *   spinner for [RateListUiState.Loading]
 * - FAB ("+") → opens add dialog
 *
 * Dialog management is owned by this composable via [mutableStateOf];
 * the ViewModel exposes [RateListViewModel.navEvents] for feedback.
 *
 * @param onNavigateBack Back navigation via toolbar arrow.
 * @param viewModel Injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateListScreen(
    onNavigateBack: () -> Unit,
    viewModel: RateListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val availableProviders by viewModel.availableProviders.collectAsStateWithLifecycle()
    val currencyCodes by viewModel.currencyCodes.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Dialog state (owned by Compose, not ViewModel) ────────────────────
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddDuplicateError by remember { mutableStateOf(false) }
    var editingRate by remember { mutableStateOf<RateDisplayItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<RateDisplayItem?>(null) }
    var showConverter by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }

    // Observe one-shot navigation events.
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is RateNavEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is RateNavEvent.SaveSuccess -> {
                    showAddDialog = false
                    showAddDuplicateError = false
                    editingRate = null
                }

                is RateNavEvent.DuplicateRate -> {
                    showAddDuplicateError = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasas") },
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                FloatingActionButton(onClick = { showProDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = "Sincronización automática",
                    )
                }
                FloatingActionButton(onClick = { showConverter = true }) {
                    Icon(
                        imageVector = Icons.Filled.Calculate,
                        contentDescription = "Convertidor",
                    )
                }
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is RateListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is RateListUiState.Ready -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = state.items,
                            key = { it.id },
                        ) { rate ->
                            RateListItem(
                                rate = rate,
                                onClick = { editingRate = rate },
                                onEdit = { editingRate = rate },
                                onDelete = { showDeleteConfirm = rate },
                            )
                        }
                    }
                }

                is RateListUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No hay tasas registradas",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Las tasas de cambio que registres aparecerán aquí",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                is RateListUiState.Error -> {
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
                            Button(onClick = { viewModel.onDismissError() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Add dialog ────────────────────────────────────────────────────────────
    if (showAddDialog) {
        RateFormDialog(
            isEdit = false,
            isSaving = isSaving,
            duplicateError = showAddDuplicateError,
            availableCurrencies = currencyCodes,
            onDismiss = {
                showAddDialog = false
                showAddDuplicateError = false
            },
            onSave = { fromCode, toCode, rate, date, provider ->
                viewModel.onAdd(fromCode, toCode, rate, date, provider)
            },
        )
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────
    editingRate?.let { rate ->
        RateFormDialog(
            initialFrom = rate.fromCurrency,
            initialTo = rate.toCurrency,
            initialRate = rate.rateFormatted,
            initialProvider = rate.provider,
            initialDate = rate.updatedAt,
            isEdit = true,
            isSaving = isSaving,
            availableCurrencies = currencyCodes,
            onDismiss = { editingRate = null },
            onSave = { fromCode, toCode, rateVal, date, provider ->
                viewModel.onEdit(rate.id, fromCode, toCode, rateVal, date, provider)
            },
        )
    }

    // ── Delete confirmation AlertDialog ──────────────────────────────────────
    showDeleteConfirm?.let { rate ->
        val inverseLabel = rate.inverse?.let { inv ->
            " y ${inv.fromCurrency} → ${inv.toCurrency}"
        } ?: ""
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Eliminar tasa") },
            text = { Text("¿Eliminar la tasa ${rate.pairLabel}$inverseLabel?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = null
                        viewModel.onDelete(rate.id)
                    },
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancelar")
                }
            },
        )
    }

    // ── Pro placeholder (sync) ────────────────────────────────────────────────
    if (showProDialog) {
        AlertDialog(
            onDismissRequest = { showProDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = { Text("ViDa Pro") },
            text = {
                Text("La sincronización automática de tasas es una funcionalidad premium.")
            },
            confirmButton = {
                TextButton(onClick = { showProDialog = false }) {
                    Text("Cerrar")
                }
            },
        )
    }

    // ── Converter dialog ──────────────────────────────────────────────────────
    if (showConverter) {
        ConverterDialog(
            onDismiss = { showConverter = false },
            getRate = { fromCode, toCode, provider ->
                viewModel.getRateForConversion(fromCode, toCode, provider)
            },
            availableProviders = availableProviders,
            availableCurrencies = currencyCodes,
        )
    }
}
