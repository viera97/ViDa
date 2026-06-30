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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
 * Root composable for the wallet list screen.
 *
 * Layout (top to bottom):
 * - [TopAppBar] ("Billeteras") with back button.
 * - Content area:
 *   - [WalletListUiState.Loading] → centered [CircularProgressIndicator]
 *   - [WalletListUiState.Ready] → [LazyColumn] of wallet cards
 *   - [WalletListUiState.Empty] → message + prompt
 *   - [WalletListUiState.Error] → error message + "Reintentar" button
 * - FAB ("+") → opens [WalletEditDialog] in add mode
 *
 * Dialog state is owned by this composable via [mutableStateOf];
 * the [WalletViewModel] exposes [WalletViewModel.navEvents] for feedback.
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

    // ── Dialog state (owned by Compose, not ViewModel) ──────────────────────
    var showAddDialog by remember { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<WalletDisplayItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<WalletDisplayItem?>(null) }

    // Observe one-shot navigation events.
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is WalletNavEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is WalletNavEvent.SaveSuccess -> {
                    showAddDialog = false
                    editingWallet = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Billeteras") },
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
                is WalletListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is WalletListUiState.Ready -> {
                    if (state.wallets.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No hay billeteras registradas",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Las billeteras que registres aparecerán aquí",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = state.wallets,
                                key = { it.id },
                            ) { wallet ->
                                WalletListItem(
                                    wallet = wallet,
                                    onClick = { editingWallet = wallet },
                                    onDelete = { showDeleteConfirm = wallet },
                                )
                            }
                        }
                    }
                }

                is WalletListUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No hay billeteras registradas",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Las billeteras que registres aparecerán aquí",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                is WalletListUiState.Error -> {
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
                            TextButton(onClick = { viewModel.refresh() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Add dialog ──────────────────────────────────────────────────────────
    if (showAddDialog) {
        WalletEditDialog(
            initialName = "Billetera",
            initialCurrency = com.vida.domain.model.Currency.CUP,
            isSaving = isSaving,
            onDismiss = { showAddDialog = false },
            onSave = { name, currency, balanceMinor ->
                viewModel.onAdd(name, currency, balanceMinor)
            },
        )
    }

    // ── Edit dialog ─────────────────────────────────────────────────────────
    editingWallet?.let { wallet ->
        val balanceInput = wallet.balance.amount
            .setScale(2, java.math.RoundingMode.HALF_EVEN)
            .toPlainString()
        WalletEditDialog(
            initialName = wallet.name,
            initialCurrency = wallet.currency,
            balance = balanceInput,
            isSaving = isSaving,
            onDismiss = { editingWallet = null },
            onSave = { name, currency, balanceMinor ->
                viewModel.onEdit(wallet.id, name, currency, balanceMinor)
            },
        )
    }

    // ── Delete confirmation AlertDialog ─────────────────────────────────────
    showDeleteConfirm?.let { wallet ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Eliminar billetera") },
            text = { Text("¿Estás seguro de eliminar la billetera ${wallet.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = null
                        viewModel.onDelete(wallet.id)
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

// ── Wallet list item ─────────────────────────────────────────────────────────

/**
 * A single wallet row rendered as a Material3 [Card].
 *
 * Displays the wallet name, balance, currency badge, and a delete button.
 * Tap opens the edit dialog.
 */
@Composable
private fun WalletListItem(
    wallet: WalletDisplayItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(end = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = wallet.balanceFormatted,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
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
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar billetera",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}