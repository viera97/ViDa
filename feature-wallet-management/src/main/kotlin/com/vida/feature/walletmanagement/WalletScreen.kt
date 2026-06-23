package com.vida.feature.walletmanagement

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vida.domain.model.Currency

// ── Currency badge colors (mirrored from StashListItem) ──────────────────────

private val CupColor = Color(0xFF1565C0)   // Blue
private val UsdColor = Color(0xFF2E7D32)   // Green
private val MlcColor = Color(0xFFE65100)   // Orange

private val String.currencyBadgeColor: Color
    get() = when (this) {
        "CUP" -> CupColor
        "USD" -> UsdColor
        "MLC" -> MlcColor
        else -> Color.Gray
    }

// ── Screen ───────────────────────────────────────────────────────────────────

/**
 * Root composable for the wallet management screen.
 *
 * Layout (top to bottom):
 * - [TopAppBar] ("Billetera") with back button and edit action.
 * - Content area:
 *   - [WalletUiState.Loading] → centered [CircularProgressIndicator]
 *   - [WalletUiState.Ready] → wallet info [Card] + last-5 expenses list
 *   - [WalletUiState.WalletNotFound] → message + "Configurar" button
 *   - [WalletUiState.Error] → error message + "Reintentar" button
 * - [WalletEditDialog] overlay when the edit action is triggered.
 *
 * Dialog state is owned by this composable via [mutableStateOf];
 * the [WalletViewModel] exposes [WalletViewModel.navEvents] for feedback.
 *
 * @param onNavigateBack Back navigation via toolbar arrow.
 * @param viewModel Injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onNavigateBack: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Dialog state (owned by Compose, not ViewModel) ────────────────────────
    var showEditDialog by remember { mutableStateOf(false) }

    // Observe one-shot navigation events.
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is WalletNavEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is WalletNavEvent.SaveSuccess -> {
                    showEditDialog = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Billetera") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                },
                actions = {
                    // Edit button — only visible in Ready state
                    if (uiState is WalletUiState.Ready) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Text(
                                text = "✎",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
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
                is WalletUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is WalletUiState.Ready -> WalletReadyContent(
                    wallet = state.wallet,
                    expenses = state.expenses,
                )

                is WalletUiState.WalletNotFound -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No hay billetera configurada",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.onConfigureWallet()
                                    showEditDialog = true
                                },
                            ) {
                                Text("Configurar")
                            }
                        }
                    }
                }

                is WalletUiState.Error -> {
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

    // ── Edit dialog ───────────────────────────────────────────────────────────

    if (showEditDialog) {
        val currentState = uiState
        when (currentState) {
            is WalletUiState.Ready -> WalletEditDialog(
                initialName = currentState.wallet.name,
                initialCurrency = currentState.wallet.currency,
                isSaving = isSaving,
                onDismiss = { showEditDialog = false },
                onSave = { name, currency ->
                    viewModel.onEdit(name, currency)
                },
            )

            is WalletUiState.WalletNotFound -> WalletEditDialog(
                initialName = "Billetera",
                initialCurrency = Currency.CUP,
                isSaving = isSaving,
                onDismiss = { showEditDialog = false },
                onSave = { name, currency ->
                    viewModel.onEdit(name, currency)
                },
            )

            else -> { /* Dialog should not be open in Loading/Error states */ }
        }
    }
}

// ── Ready content ────────────────────────────────────────────────────────────

/**
 * Content shown when the wallet is loaded ([WalletUiState.Ready]).
 *
 * Renders the wallet info card and a last-5 expenses section.
 */
@Composable
private fun WalletReadyContent(
    wallet: WalletDisplayItem,
    expenses: List<ExpenseDisplayItem>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Wallet info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                // Name + currency badge row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    // Currency badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = wallet.currencyCode.currencyBadgeColor,
                    ) {
                        Text(
                            text = wallet.currencyCode,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Balance
                Text(
                    text = wallet.balanceFormatted,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Last-5 expenses section
        Text(
            text = "Últimos gastos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (expenses.isEmpty()) {
            Text(
                text = "No hay gastos registrados",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                expenses.forEachIndexed { index, expense ->
                    ExpenseRow(expense = expense)
                    if (index < expenses.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

// ── Expense row ──────────────────────────────────────────────────────────────

/**
 * A single expense row in the last-5 section.
 *
 * Left side: category name (headline) + date (subtitle).
 * Right side: formatted amount.
 */
@Composable
private fun ExpenseRow(expense: ExpenseDisplayItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.categoryName,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = expense.dateFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = expense.amountFormatted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
