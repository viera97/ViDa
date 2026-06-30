package com.vida.feature.expenselist

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vida.feature.expense.ExpenseFormDialog
import java.math.BigDecimal

/**
 * Expense detail screen — shows full expense info with edit (placeholder) and
 * delete actions. Also loads and displays refund information when available.
 *
 * Uses [ExpenseDetailViewModel] to load the expense by ID (from the nav route
 * argument) and handles deletion via [DeleteExpense] and refund CRUD.
 *
 * @param onNavigateBack Called after successful deletion or via toolbar back arrow.
 * @param viewModel Injected via Hilt with [SavedStateHandle] providing the "id" argument.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExpenseDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val refundState by viewModel.refundState.collectAsStateWithLifecycle()

    var showRefundDialog by remember { mutableStateOf(false) }
    var editingRefund by remember { mutableStateOf<RefundDisplay?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                DetailNavigationEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { event ->
            snackbarHostState.showSnackbar(event.message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del gasto") },
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is ExpenseDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ExpenseDetailUiState.Ready -> {
                    val detail = state.expense
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
                        // Amount — prominent
                        Text(
                            text = detail.formattedAmount,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Description
                        Text(
                            text = detail.description,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Info card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                DetailRow("Fecha", detail.formattedDate)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                // Category with color dot
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Categoría",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(detail.categoryColor)),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = detail.categoryName,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                DetailRow(
                                    label = "Fuente",
                                    value = detail.sourceLabel,
                                    leadingIcon = sourceTypeIcon(detail.sourceType),
                                )

                                if (!detail.note.isNullOrBlank()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    DetailRow("Nota", detail.note)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ── Refund section ──────────────────────────────────
                        when (val refState = refundState) {
                            is RefundUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }

                            is RefundUiState.Ready -> {
                                RefundCard(
                                    refund = refState.refund,
                                    onEdit = {
                                        editingRefund = refState.refund
                                        showRefundDialog = true
                                    },
                                    onDelete = { showDeleteConfirmation = true },
                                )
                            }

                            is RefundUiState.Empty -> {
                                OutlinedButton(
                                    onClick = {
                                        editingRefund = null
                                        showRefundDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Agregar reembolso")
                                }
                            }

                            is RefundUiState.Error -> {
                                Text(
                                    text = refState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.height(24.dp))

                        // Action buttons
                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Editar")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.onDelete() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text("Eliminar")
                        }
                    }
                }

                is ExpenseDetailUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onNavigateBack) {
                                Text("Volver")
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────
    if (showEditDialog) {
        ExpenseFormDialog(
            // Reusing the same dialog as create — `expenseIdToEdit` switches it
            // into EDIT mode (pre-fills fields + routes submit through
            // UpdateExpense). `onSuccess` triggers a re-fetch here so the detail
            // page reflects the updated values immediately.
            expenseIdToEdit = viewModel.expenseId,
            onDismiss = { showEditDialog = false },
            onSuccess = { viewModel.refresh() },
        )
    }

    if (showRefundDialog) {
        RefundFormDialog(
            editingRefund = editingRefund,
            onDismiss = {
                showRefundDialog = false
                editingRefund = null
            },
            onConfirm = { amount, reason, note ->
                if (editingRefund != null) {
                    viewModel.onEditRefund(amount, reason, note)
                } else {
                    viewModel.onAddRefund(amount, reason, note)
                }
                showRefundDialog = false
                editingRefund = null
            },
        )
    }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.onDeleteRefund()
                showDeleteConfirmation = false
            },
            onDismiss = { showDeleteConfirmation = false },
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null, // the value text describes it
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Refund info card with edit/delete actions. */
@Composable
private fun RefundCard(
    refund: RefundDisplay,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Reembolso",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = refund.formattedAmount,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = refund.reason,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = refund.formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!refund.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = refund.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onEdit) {
                    Text("Editar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Eliminar")
                }
            }
        }
    }
}

/** AlertDialog for adding or editing a refund. Pre-fills when [editingRefund] is non-null. */
@Composable
private fun RefundFormDialog(
    editingRefund: RefundDisplay?,
    onDismiss: () -> Unit,
    onConfirm: (amount: BigDecimal, reason: String, note: String?) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editingRefund) {
        if (editingRefund != null) {
            amountText = editingRefund.amount.toPlainString()
            reason = editingRefund.reason
            note = editingRefund.note ?: ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingRefund != null) "Editar reembolso" else "Agregar reembolso")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = { Text("Monto") },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedAmount = amountText.toBigDecimalOrNull()
                if (parsedAmount == null) {
                    amountError = "Monto inválido"
                    return@TextButton
                }
                if (reason.isBlank()) {
                    return@TextButton
                }
                onConfirm(parsedAmount, reason, note.ifBlank { null })
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

/**
 * Maps a [com.vida.domain.model.SourceType] to the icon shown next to the
 * "Fuente" row in the detail screen. Centralized so the visual mapping is the
 * single source of truth — adding a new SourceType only requires updating the
 * when branch here, matching the same pattern in
 * `PerSourceBreakdownSection.iconFor()` in feature-home.
 */
private fun sourceTypeIcon(sourceType: com.vida.domain.model.SourceType): androidx.compose.ui.graphics.vector.ImageVector =
    when (sourceType) {
        com.vida.domain.model.SourceType.WALLET -> Icons.Default.AccountBalanceWallet
        com.vida.domain.model.SourceType.CARD -> Icons.Default.CreditCard
        com.vida.domain.model.SourceType.STASH -> Icons.Default.Savings
    }

/** AlertDialog for confirming refund deletion. */
@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar reembolso") },
        text = { Text("¿Estás seguro de eliminar este reembolso? Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
