package com.vida.feature.recurringexpensemanagement

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
import com.vida.domain.model.RecurringExpense

/**
 * Root composable for the recurring expense management screen.
 *
 * Layout (top to bottom):
 * - TopAppBar ("Gastos Recurrentes") with back button
 * - Content area: [LazyColumn] when [RecurringListUiState.Ready],
 *   centered message for [RecurringListUiState.Empty],
 *   error message + retry for [RecurringListUiState.Error],
 *   spinner for [RecurringListUiState.Loading]
 * - FAB ("+") → emits add dialog request via ViewModel
 *
 * Dialog management is owned by this composable via [mutableStateOf];
 * the ViewModel exposes [RecurringListViewModel.navEvents] for feedback.
 *
 * PR #2: form dialog (add/edit) wired. PR #3 will add generate flow.
 *
 * @param onNavigateBack Back navigation via toolbar arrow.
 * @param viewModel Injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecurringListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val stashes by viewModel.stashes.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Dialog state (owned by Compose, not ViewModel) ────────────────────
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntity by remember { mutableStateOf<RecurringExpense?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<RecurringDisplayItem?>(null) }

    // Observe one-shot navigation events.
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is RecurringNavEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is RecurringNavEvent.SaveSuccess -> {
                    showAddDialog = false
                    editingEntity = null
                }

                is RecurringNavEvent.ShowAddDialog -> {
                    showAddDialog = true
                }

                is RecurringNavEvent.ShowDeleteDialog -> {
                    showDeleteConfirm = event.item
                }

                is RecurringNavEvent.ShowEditDialog -> {
                    editingEntity = event.entity
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gastos Recurrentes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text(
                            text = "\u2190",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onFabClick() }) {
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
                is RecurringListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is RecurringListUiState.Ready -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = state.items,
                            key = { it.id },
                        ) { item ->
                            RecurringListItem(
                                item = item,
                                onClick = { viewModel.onOpenEditDialog(item) },
                                onEdit = { viewModel.onOpenEditDialog(item) },
                                onDelete = { viewModel.onRequestDelete(item) },
                                onGenerate = { viewModel.onGenerate(item.id) },
                                onToggleActive = { viewModel.onToggleActive(item.id) },
                            )
                        }
                    }
                }

                is RecurringListUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No hay plantillas",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tocá el botón + para crear tu primera plantilla de gasto recurrente",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.onFabClick() }) {
                                Text("Agregar plantilla")
                            }
                        }
                    }
                }

                is RecurringListUiState.Error -> {
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
                            Button(onClick = { viewModel.onRetry() }) {
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
        RecurringFormDialog(
            isEdit = false,
            isSaving = isSaving,
            categories = categories,
            cards = cards,
            stashes = stashes,
            onDismiss = { showAddDialog = false },
            onSave = { expense -> viewModel.onAdd(expense) },
        )
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────
    editingEntity?.let { entity ->
        RecurringFormDialog(
            initialAmount = entity.amount.amount.toPlainString(),
            initialCurrency = entity.currency,
            initialCategoryId = entity.categoryId,
            initialSourceType = entity.sourceType,
            initialSourceId = entity.sourceId,
            initialDescription = entity.description,
            initialFrequency = entity.frequency,
            initialStartDate = entity.startDate,
            initialEndDate = entity.endDate,
            initialIsActive = entity.isActive,
            isEdit = true,
            isSaving = isSaving,
            categories = categories,
            cards = cards,
            stashes = stashes,
            onDismiss = { editingEntity = null },
            onSave = { formExpense ->
                val merged = entity.copy(
                    amount = formExpense.amount,
                    currency = formExpense.currency,
                    categoryId = formExpense.categoryId,
                    sourceType = formExpense.sourceType,
                    sourceId = formExpense.sourceId,
                    description = formExpense.description,
                    frequency = formExpense.frequency,
                    startDate = formExpense.startDate,
                    endDate = formExpense.endDate,
                    isActive = formExpense.isActive,
                )
                viewModel.onEdit(merged)
            },
        )
    }

    // ── Delete confirmation AlertDialog ──────────────────────────────────────
    showDeleteConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Eliminar plantilla") },
            text = {
                Text("¿Eliminar \"${item.description} — ${item.amountFormatted} ${item.currencyCode}\"?")
            },
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
