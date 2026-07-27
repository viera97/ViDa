package com.vida.feature.currencymanagement.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
 * Root composable for the currency management screen.
 *
 * Layout (top to bottom):
 * - TopAppBar ("Monedas") with back button
 * - Content area: [LazyColumn] when [CurrencyListUiState.Ready],
 *   centered message + "Agregar moneda" button for [CurrencyListUiState.Empty],
 *   error message + retry for [CurrencyListUiState.Error],
 *   spinner for [CurrencyListUiState.Loading]
 * - FAB ("+") → opens [CurrencyFormDialog] in add mode
 *
 * Dialog management is owned by this composable via [mutableStateOf];
 * the ViewModel exposes [isSaving] and emits [CurrencyNavEvent] for feedback.
 *
 * @param onNavigateBack Back navigation via toolbar arrow.
 * @param viewModel Injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyListScreen(
    onNavigateBack: () -> Unit,
    viewModel: CurrencyListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Dialog state (owned by Compose, not ViewModel) ────────────────────
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCurrency by remember { mutableStateOf<CurrencyDisplayItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<CurrencyDisplayItem?>(null) }

    // Observe one-shot navigation events.
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is CurrencyNavEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is CurrencyNavEvent.SaveSuccess -> {
                    showAddDialog = false
                    editingCurrency = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monedas") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineSmall,
                )
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
                is CurrencyListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is CurrencyListUiState.Ready -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = state.currencies,
                            key = { it.id },
                        ) { item ->
                            CurrencyListItem(
                                item = item,
                                onClick = { editingCurrency = item },
                                onDeleteClick = { showDeleteConfirm = item },
                            )
                        }
                    }
                }

                is CurrencyListUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No hay monedas",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Las monedas que crees aparecerán aquí",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Text("Agregar moneda")
                            }
                        }
                    }
                }

                is CurrencyListUiState.Error -> {
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

    // ── CurrencyFormDialog (add mode) ─────────────────────────────────────
    if (showAddDialog) {
        CurrencyFormDialog(
            isEdit = false,
            isSaving = isSaving,
            onDismiss = { showAddDialog = false },
            onSave = { name, code -> viewModel.onAdd(name, code) },
        )
    }

    // ── CurrencyFormDialog (edit mode) ────────────────────────────────────
    editingCurrency?.let { item ->
        CurrencyFormDialog(
            initialName = item.name,
            initialCode = item.code,
            isEdit = true,
            isSaving = isSaving,
            onDismiss = { editingCurrency = null },
            onSave = { name, code -> viewModel.onEdit(item.id, name, code) },
        )
    }

    // ── Delete confirmation AlertDialog ───────────────────────────────────
    showDeleteConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Eliminar moneda") },
            text = { Text("¿Estás seguro?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = null
                        viewModel.onDelete(item.id)
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
}
