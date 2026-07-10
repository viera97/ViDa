package com.vida.feature.bankmanagement.ui

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
 * Root composable for the bank management screen.
 *
 * Layout (top to bottom):
 * - TopAppBar ("Bancos") with back button
 * - Content area: [LazyColumn] when [BankListUiState.Ready],
 *   centered message + "Agregar banco" button for [BankListUiState.Empty],
 *   error message + retry for [BankListUiState.Error],
 *   spinner for [BankListUiState.Loading]
 * - FAB ("+") → opens [BankFormDialog] in add mode
 *
 * Dialog management is owned by this composable via [mutableStateOf];
 * the ViewModel exposes [isSaving] and emits [BankNavEvent] for feedback.
 *
 * @param onNavigateBack Back navigation via toolbar arrow.
 * @param viewModel Injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankListScreen(
    onNavigateBack: () -> Unit,
    viewModel: BankListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Dialog state (owned by Compose, not ViewModel) ────────────────────
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBank by remember { mutableStateOf<BankDisplayItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<BankDisplayItem?>(null) }

    // Observe one-shot navigation events.
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is BankNavEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is BankNavEvent.SaveSuccess -> {
                    showAddDialog = false
                    editingBank = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bancos") },
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
                is BankListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is BankListUiState.Ready -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = state.banks,
                            key = { it.id },
                        ) { item ->
                            BankListItem(
                                item = item,
                                onClick = { editingBank = item },
                                onDeleteClick = { showDeleteConfirm = item },
                            )
                        }
                    }
                }

                is BankListUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No hay bancos",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Los bancos que crees aparecerán aquí",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Text("Agregar banco")
                            }
                        }
                    }
                }

                is BankListUiState.Error -> {
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

    // ── BankFormDialog (add mode) ─────────────────────────────────────
    if (showAddDialog) {
        BankFormDialog(
            isEdit = false,
            isSaving = isSaving,
            onDismiss = { showAddDialog = false },
            onSave = { name, color -> viewModel.onAdd(name, color) },
        )
    }

    // ── BankFormDialog (edit mode) ────────────────────────────────────
    editingBank?.let { item ->
        BankFormDialog(
            initialName = item.name,
            initialColor = item.color,
            isEdit = true,
            isSaving = isSaving,
            onDismiss = { editingBank = null },
            onSave = { name, color -> viewModel.onEdit(item.id, name, color) },
        )
    }

    // ── Delete confirmation AlertDialog ───────────────────────────────────
    showDeleteConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Eliminar banco") },
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
