package com.vida.feature.cardmanagement

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
 * Root composable for the card management screen.
 *
 * Layout (top to bottom):
 * - TopAppBar ("Tarjetas") with back button
 * - Content area: [LazyColumn] when [CardListUiState.Ready],
 *   centered message + "Agregar tarjeta" button for [CardListUiState.Empty],
 *   error message + retry for [CardListUiState.Error],
 *   spinner for [CardListUiState.Loading]
 * - FAB ("+") → opens [CardFormDialog] in add mode
 *
 * Dialog management is owned by this composable via [mutableStateOf];
 * the ViewModel exposes [CardListViewModel.navEvents] for feedback.
 *
 * @param onNavigateBack Back navigation via toolbar arrow.
 * @param viewModel Injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    onNavigateBack: () -> Unit,
    viewModel: CardListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Dialog state (owned by Compose, not ViewModel) ────────────────────
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CardDisplayItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<CardDisplayItem?>(null) }

    // Observe one-shot navigation events.
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is CardNavEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is CardNavEvent.SaveSuccess -> {
                    showAddDialog = false
                    editingCard = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tarjetas") },
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
                is CardListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is CardListUiState.Ready -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = state.cards,
                            key = { it.id },
                        ) { card ->
                            CardListItem(
                                card = card,
                                onClick = { editingCard = card },
                                onEdit = { editingCard = card },
                                onDelete = { showDeleteConfirm = card },
                            )
                        }
                    }
                }

                is CardListUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No hay tarjetas",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Las tarjetas que registres aparecerán aquí",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Text("Agregar tarjeta")
                            }
                        }
                    }
                }

                is CardListUiState.Error -> {
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
        CardFormDialog(
            isEdit = false,
            isSaving = isSaving,
            onDismiss = { showAddDialog = false },
            onSave = { bank, first6, last4, type, currency, expiry, note ->
                viewModel.onAdd(bank, first6, last4, type, currency, expiry, note)
            },
        )
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────
    editingCard?.let { card ->
        CardFormDialog(
            initialBank = card.bank,
            initialFirst6 = card.first6,
            initialLast4 = card.last4,
            initialType = card.type,
            initialCurrency = card.currency,
            initialExpiry = card.expiry,
            initialNote = card.note ?: "",
            isEdit = true,
            isSaving = isSaving,
            onDismiss = { editingCard = null },
            onSave = { bank, first6, last4, type, currency, expiry, note ->
                viewModel.onEdit(
                    card.id, bank, first6, last4, type, currency, expiry, note,
                )
            },
        )
    }

    // ── Delete confirmation AlertDialog ──────────────────────────────────────
    showDeleteConfirm?.let { card ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Eliminar tarjeta") },
            text = { Text("¿Estás seguro de eliminar la tarjeta de ${card.bank}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = null
                        viewModel.onDelete(card.id)
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
