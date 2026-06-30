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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vida.domain.model.SourceType

/**
 * Root composable for the transfer creation form (full-screen route variant).
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
    showTopBar: Boolean = true,
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
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Nueva transferencia") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                            )
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = if (showTopBar) innerPadding.calculateBottomPadding() else 0.dp,
                ),
        ) {
            when (uiState) {
                is TransferFormUiState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is TransferFormUiState.Ready -> {
                    ScreenFormBody(
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
                        ScreenFormBody(
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

/**
 * Body of the screen-mode form: fields + a single "Transferir" button at the end.
 * Cancel is handled by the TopAppBar back arrow.
 */
@Composable
private fun ScreenFormBody(
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
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TransferFormFields(
            ready = ready,
            onDeClick = onDeClick,
            onAClick = onAClick,
            onAmountChanged = onAmountChanged,
            onNoteChanged = onNoteChanged,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onSubmit,
                enabled = !isSaving,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Transferir")
            }
        }
    }
}

// ── Reusable form fields ─────────────────────────────────────────────────────

/**
 * Reusable form fields for the transfer form (no action buttons).
 *
 * Composed by both [TransferFormScreen] (screen route) and [TransferFormDialog]
 * (modal variant). Buttons are owned by the parent so the dialog can use the
 * [AlertDialog] confirm/dismiss slots and the screen can use an inline row.
 */
@Composable
internal fun TransferFormFields(
    ready: TransferFormUiState.Ready,
    onDeClick: () -> Unit,
    onAClick: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
) {
    // De ↔ A source selector section.
    Text(
        text = "Origen → Destino",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceCard(
            label = "Origen",
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
            label = "Destino",
            source = ready.aSource,
            modifier = Modifier.weight(1f),
            onClick = onAClick,
        )
    }

    // Cross-currency validation error.
    val currencyError = ready.validationErrors["currency"]
    if (currencyError != null) {
        Text(
            text = currencyError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )

    // Note field.
    OutlinedTextField(
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
                Icon(
                    imageVector = when (source.type) {
                        SourceType.WALLET -> Icons.Default.AccountBalanceWallet
                        SourceType.CARD -> Icons.Default.CreditCard
                        SourceType.STASH -> Icons.Default.Savings
                    },
                    contentDescription = source.name,
                    modifier = Modifier.size(32.dp),
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
                            Icon(
                                imageVector = when (source.type) {
                                    SourceType.WALLET -> Icons.Default.AccountBalanceWallet
                                    SourceType.CARD -> Icons.Default.CreditCard
                                    SourceType.STASH -> Icons.Default.Savings
                                },
                                contentDescription = source.name,
                                modifier = Modifier.size(28.dp),
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
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = MaterialTheme.colorScheme.primary,
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

// ── Dialog variant ───────────────────────────────────────────────────────────

/**
 * Modal variant of the transfer form, mirroring the visual style of
 * [com.vida.feature.cardmanagement.CardFormDialog].
 *
 * Uses [AlertDialog] so the modal sizes itself to its content (with built-in
 * vertical scrolling when content exceeds the available height). This is the
 * recommended entry point when embedding the transfer form inside another
 * screen (e.g. from FuentesScreen), because wrapping [TransferFormScreen] in a
 * custom [androidx.compose.ui.window.Dialog] forces a `Scaffold` into a
 * dialog container, which leaves empty space below the buttons.
 *
 * @param onDismiss Called when the user cancels or the dialog should close
 *   (also fired automatically after a successful save via nav events).
 * @param onSaved Called only after a successful save (just before [onDismiss]),
 *   so the parent screen can react to the persisted transfer — typically by
 *   refreshing wallet/card lists whose reactive observation alone isn't enough.
 * @param viewModel The [TransferFormViewModel], injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFormDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    viewModel: TransferFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    // Cache the last Ready state so the form stays visible during Error (submit failure).
    var lastReady by remember { mutableStateOf<TransferFormUiState.Ready?>(null) }
    (uiState as? TransferFormUiState.Ready)?.let { lastReady = it }

    // Source selection dialog visibility.
    var showDeDialog by remember { mutableStateOf(false) }
    var showADialog by remember { mutableStateOf(false) }

    // Wrapper around onDismiss that clears the form before closing the
    // dialog — so reopening shows a fresh empty form, not the previously
    // filled data. Applied uniformly to every dismiss path: back press,
    // outside tap, Cancel button, and post-save dismissal.
    val handleDismiss: () -> Unit = {
        viewModel.reset()
        onDismiss()
    }

    // Observe one-shot navigation events.
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is TransferFormNavEvent.NavigateBack -> {
                    onSaved()
                    handleDismiss()
                }
            }
        }
    }

    // Current form data for the source picker dialogs (overlays).
    val currentReady = (uiState as? TransferFormUiState.Ready) ?: lastReady
    val sources = currentReady?.sources.orEmpty()

    // Compute enable state for the confirm button.
    val readyForSubmit = currentReady
    val canSubmit = readyForSubmit != null &&
        !isSaving &&
        readyForSubmit.deSource != null &&
        readyForSubmit.aSource != null &&
        readyForSubmit.amount.isNotBlank() &&
        readyForSubmit.validationErrors["de"] == null &&
        readyForSubmit.validationErrors["a"] == null &&
        readyForSubmit.validationErrors["amount"] == null

    AlertDialog(
        onDismissRequest = { if (!isSaving) handleDismiss() },
        title = { Text("Nueva transferencia") },
        text = {
            // The text slot is what AlertDialog scrolls internally; we just
            // hand it a content tree whose height fits the dialog's available
            // space. The Column uses verticalScroll so any overflow remains
            // reachable instead of pushing the dialog taller.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (val state = uiState) {
                    TransferFormUiState.Idle -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is TransferFormUiState.Ready -> {
                        TransferFormFields(
                            ready = state,
                            onDeClick = { showDeDialog = true },
                            onAClick = { showADialog = true },
                            onAmountChanged = viewModel::onAmountChanged,
                            onNoteChanged = viewModel::onNoteChanged,
                        )
                    }

                    TransferFormUiState.EmptySourceList -> {
                        Text(
                            text = "No hay fuentes disponibles para crear una transferencia.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Saved state dismissal is handled exclusively via the
                    // TransferFormNavEvent.NavigateBack collector above (line 607).
                    // A separate LaunchedEffect here would race with that path,
                    // closing the dialog before `onSaved()` fires — preventing the
                    // parent screen from refreshing source lists after the transfer.
                    TransferFormUiState.Saved -> Unit

                    is TransferFormUiState.Error -> {
                        if (lastReady != null) {
                            // Submit error — keep the form filled and surface
                            // the error inline (no snackbar inside a dialog).
                            TransferFormFields(
                                ready = lastReady!!,
                                onDeClick = { showDeDialog = true },
                                onAClick = { showADialog = true },
                                onAmountChanged = viewModel::onAmountChanged,
                                onNoteChanged = viewModel::onNoteChanged,
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            // Initial load error — show error + retry inside the dialog.
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Button(onClick = { viewModel.retry() }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.submit() },
                enabled = canSubmit,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Transferir")
            }
        },
        dismissButton = {
            TextButton(
                onClick = handleDismiss,
                enabled = !isSaving,
            ) {
                Text("Cancelar")
            }
        },
    )

    // Source selection dialogs (rendered as separate dialogs on top of the form).
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
