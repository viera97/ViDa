package com.vida.feature.categorymanagement

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
 * Root composable for the category management screen.
 *
 * Layout (top to bottom):
 * - TopAppBar ("Categorías") with back button
 * - Content area: [LazyColumn] when [CategoryListUiState.Ready],
 *   centered message + "Agregar categoría" button for [CategoryListUiState.Empty],
 *   error message + retry for [CategoryListUiState.Error],
 *   spinner for [CategoryListUiState.Loading]
 * - FAB ("+") → opens [CategoryFormDialog] in add mode
 *
 * Dialog management is owned by this composable via [mutableStateOf];
 * the ViewModel exposes [isSaving] and emits [CategoryNavEvent] for feedback.
 *
 * @param onNavigateBack Back navigation via toolbar arrow.
 * @param onNavigateToAdd Unused in PR #2 — FAB opens dialog directly.
 * @param onNavigateToEdit Unused in PR #2 — row tap opens dialog directly.
 * @param viewModel Injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: CategoryListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Dialog state (owned by Compose, not ViewModel) ────────────────────
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryDisplayItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<CategoryDisplayItem?>(null) }

    // Observe one-shot navigation events.
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is CategoryNavEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is CategoryNavEvent.SaveSuccess -> {
                    showAddDialog = false
                    editingCategory = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorías") },
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
                is CategoryListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is CategoryListUiState.Ready -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = state.categories,
                            key = { it.id },
                        ) { item ->
                            CategoryListItem(
                                item = item,
                                onClick = { editingCategory = item },
                                onDeleteClick = { showDeleteConfirm = item },
                            )
                        }
                    }
                }

                is CategoryListUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No hay categorías",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Las categorías que crees aparecerán aquí",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Text("Agregar categoría")
                            }
                        }
                    }
                }

                is CategoryListUiState.Error -> {
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

    // ── CategoryFormDialog (add mode) ─────────────────────────────────────
    if (showAddDialog) {
        CategoryFormDialog(
            isEdit = false,
            isSaving = isSaving,
            onDismiss = { showAddDialog = false },
            onSave = { name, color -> viewModel.onAdd(name, color) },
        )
    }

    // ── CategoryFormDialog (edit mode) ────────────────────────────────────
    editingCategory?.let { item ->
        CategoryFormDialog(
            initialName = item.name,
            initialColor = item.color,
            isEdit = true,
            isSaving = isSaving,
            onDismiss = { editingCategory = null },
            onSave = { name, color -> viewModel.onEdit(item.id, name, color) },
        )
    }

    // ── Delete confirmation AlertDialog ───────────────────────────────────
    showDeleteConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Eliminar categoría") },
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
