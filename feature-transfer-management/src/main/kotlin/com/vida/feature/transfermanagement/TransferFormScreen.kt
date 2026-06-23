package com.vida.feature.transfermanagement

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Root composable for the transfer creation form.
 *
 * Observes [TransferFormViewModel.uiState] via [collectAsStateWithLifecycle] and
 * renders the appropriate screen state inside a [Scaffold]:
 * - [TransferFormUiState.Idle] → centered [CircularProgressIndicator]
 * - [TransferFormUiState.Ready] → scrollable form with source pickers, amount,
 *   note, and submit button
 * - [TransferFormUiState.EmptySourceList] → centered informational message
 * - [TransferFormUiState.Saved] → navigates back via [LaunchedEffect]
 * - [TransferFormUiState.Error] → error message with retry button (initial load)
 *   or form content with snackbar (submit error)
 *
 * Source selection uses [AlertDialog] dialogs triggered by tapping the
 * De/A source cards.
 *
 * One-shot navigation events from [TransferFormViewModel.navEvents] are
 * collected via [LaunchedEffect] and trigger [onNavigateBack].
 *
 * @param onNavigateBack Callback invoked to navigate back (toolbar arrow,
 *   on save success, or nav event).
 * @param viewModel The [TransferFormViewModel], injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransferFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Cache the last Ready state so the form is visible during Error (submit failure).
    var lastReady by remember { mutableStateOf<TransferFormUiState.Ready?>(null) }
    (uiState as? TransferFormUiState.Ready)?.let { lastReady = it }

    // Source selection dialog visibility.
    var showDeDialog by remember { mutableStateOf(false) }
    var showADialog by remember { mutableStateOf(false) }

    // Observe one-shot navigation events.
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is TransferFormNavEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva transferencia") },
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
            when (uiState) {
                is TransferFormUiState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is TransferFormUiState.Ready -> {
                    TransferFormContent(
                        ready = uiState as TransferFormUiState.Ready,
                        isSaving = isSaving,
                        onDeClick = { showDeDialog = true },
                        onAClick = { showADialog = true },
                        onAmountChanged = viewModel::onAmountChanged,
                        onNoteChanged = viewModel::onNoteChanged,
                        onSubmit = viewModel::submit,
                    )
                }

                is TransferFormUiState.EmptySourceList -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No hay fuentes disponibles para crear una transferencia.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(32.dp),
                        )
                    }
                }

                is TransferFormUiState.Saved -> {
                    LaunchedEffect(Unit) { onNavigateBack() }
                }

                is TransferFormUiState.Error -> {
                    val error = uiState as TransferFormUiState.Error
                    if (lastReady != null) {
                        // Submit error — show form with snackbar.
                        TransferFormContent(
                            ready = lastReady!!,
                            isSaving = false,
                            onDeClick = { showDeDialog = true },
                            onAClick = { showADialog = true },
                            onAmountChanged = viewModel::onAmountChanged,
                            onNoteChanged = viewModel::onNoteChanged,
                            onSubmit = viewModel::submit,
                        )
                        LaunchedEffect(error.message) {
                            snackbarHostState.showSnackbar(error.message)
                        }
                    } else {
                        // Initial load error — show centered error with retry.
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = error.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.retry() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }

            // Source selection dialogs — rendered outside the when block
            // so they overlay all states.

            val currentReady = (uiState as? TransferFormUiState.Ready) ?: lastReady
            val sources = currentReady?.sources.orEmpty()

            if (showDeDialog) {
                SourceSelectionDialog(
                    title = "Origen",
                    sources = sources,
                    selectedSource = currentReady?.deSource,
                    excludedSource = currentReady?.aSource,
                    onDismiss = { showDeDialog = false },
                    onSourceSelected = { source ->
                        viewModel.onDeSelected(source)
                        showDeDialog = false
                    },
                )
            }

            if (showADialog) {
                SourceSelectionDialog(
                    title = "Destino",
                    sources = sources,
                    selectedSource = currentReady?.aSource,
                    excludedSource = currentReady?.deSource,
                    onDismiss = { showADialog = false },
                    onSourceSelected = { source ->
                        viewModel.onASelected(source)
                        showADialog = false
                    },
                )
            }
        }
    }
}

// ── Form content ─────────────────────────────────────────────────────────────

/**
 * Scrollable form content with source pickers, amount, note, and submit button.
 *
 * Extracted as a private composable to avoid duplication across
 * [TransferFormUiState.Ready] and [TransferFormUiState.Error] (submit failure)
 * branches.
 */
@Composable
private fun TransferFormContent(
    ready: TransferFormUiState.Ready,
    isSaving: Boolean,
    onDeClick: () -> Unit,
    onAClick: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // De ↔ A source selector section.
        Text(
            text = "Origen → Destino",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceCard(
                label = "De",
                source = ready.deSource,
                modifier = Modifier.weight(1f),
                onClick = onDeClick,
            )
            Text(
                text = "↔",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            SourceCard(
                label = "A",
                source = ready.aSource,
                modifier = Modifier.weight(1f),
                onClick = onAClick,
            )
        }

        // Cross-currency validation error.
        val currencyError = ready.validationErrors["currency"]
        if (currencyError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currencyError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Amount field.
        OutlinedTextField(
            value = ready.amount,
            onValueChange = onAmountChanged,
            label = { Text("Importe") },
            isError = ready.validationErrors.containsKey("amount"),
            supportingText = ready.validationErrors["amount"]?.let { error ->
                { Text(error) }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Note field.
        TextField(
            value = ready.note,
            onValueChange = onNoteChanged,
            label = { Text("Nota (opcional)") },
            supportingText = ready.validationErrors["note"]?.let { error ->
                { Text(error) }
            },
            isError = ready.validationErrors.containsKey("note"),
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Submit button.
        Button(
            onClick = onSubmit,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Guardar transferencia")
        }
    }
}

// ── Source card ──────────────────────────────────────────────────────────────

/**
 * A tappable card representing a source (De or A) in the transfer form.
 *
 * When a source is selected, displays its icon, name, subtitle, and currency.
 * Otherwise shows a "Seleccionar" placeholder.
 */
@Composable
private fun SourceCard(
    label: String,
    source: TransferSourceItem?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (source != null)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (source != null) {
                Text(
                    text = source.icon,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (source.subtitle != null) {
                    Text(
                        text = source.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = source.currency.code,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Seleccionar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Source selection dialog ──────────────────────────────────────────────────

/**
 * [AlertDialog] listing all available transfer sources.
 *
 * The currently selected source is highlighted. The [excludedSource]
 * (the counterpart in the De/A pair) is visually dimmed and non-clickable
 * to enforce mutual exclusion at the UI level.
 */
@Composable
private fun SourceSelectionDialog(
    title: String,
    sources: List<TransferSourceItem>,
    selectedSource: TransferSourceItem?,
    excludedSource: TransferSourceItem?,
    onDismiss: () -> Unit,
    onSourceSelected: (TransferSourceItem) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                sources.forEachIndexed { index, source ->
                    val isExcluded = source == excludedSource
                    val isSelected = source == selectedSource
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isExcluded) {
                                onSourceSelected(source)
                            },
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isSelected) 2.dp else 0.dp,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = source.icon,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isExcluded)
                                        MaterialTheme.colorScheme.onSurface
                                            .copy(alpha = 0.38f)
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                )
                                if (source.subtitle != null) {
                                    Text(
                                        text = source.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                text = source.currency.code,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                    if (index < sources.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
