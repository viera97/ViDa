package com.vida.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.vida.feature.expense.ExpenseFormDialog
import com.vida.feature.income.IncomeFormDialog
import com.vida.feature.home.home.EmptyStateContent
import com.vida.feature.home.home.ErrorStateContent
import com.vida.feature.home.home.HomeFab
import com.vida.feature.home.home.LoadingContent
import com.vida.feature.home.home.PerCurrencySubtotals
import com.vida.feature.home.home.PerSourceBreakdownSection
import com.vida.feature.home.home.RatesIndicator
import com.vida.feature.home.home.RecentExpensesList
import com.vida.feature.home.home.RecentIncomesList
import com.vida.feature.home.home.TotalBalanceCard
import com.vida.feature.home.update.UpdateUiState
import java.io.File
import java.util.Locale

/**
 * Root composable for the Home dashboard.
 *
 * Consumes [HomeViewModel.uiState] and dispatches to the appropriate state
 * renderer: [LoadingContent], [ReadyContent], [EmptyStateContent], or [ErrorStateContent].
 *
 * The expense FAB opens [ExpenseFormDialog] as a modal AlertDialog. The income
 * FAB opens [IncomeFormDialog] similarly. Both dialogs use shared ViewModels
 * (injected via Hilt) and call `reset()` on open so each session starts with
 * default form values.
 *
 * The update TopAppBar action drives [HomeViewModel.updateState] through the
 * state machine documented in [UpdateUiState]: transient messages (UpToDate /
 * Error) are surfaced through a snackbar hosted in the [Scaffold], while
 * longer-lived dialogs (UpdateAvailable / Downloading / ReadyToInstall) are
 * rendered as siblings of the Scaffold so they overlay the full screen.
 *
 * The FABs are visible in ALL states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExpenseList: () -> Unit = {},
    onNavigateToIncomeList: () -> Unit = {},
    onNavigateToFuentes: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Surface transient update messages (UpToDate / Error) through the
    // shared snackbar host. Once the snackbar dismisses, reset the state
    // machine back to Idle so the trigger button is enabled again.
    LaunchedEffect(updateState) {
        when (val s = updateState) {
            is UpdateUiState.UpToDate -> {
                snackbarHostState.showSnackbar("Ya tenés la última versión (v${s.currentVersion})")
                viewModel.dismissUpdateDialog()
            }
            is UpdateUiState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.dismissUpdateDialog()
            }
            else -> Unit
        }
    }

    var showExpenseDialog by remember { mutableStateOf(false) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Inicio") },
                actions = {
                    IconButton(
                        onClick = { viewModel.checkForUpdate() },
                        enabled = updateState !is UpdateUiState.Checking,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = "Actualizar aplicación",
                        )
                    }
                    IconButton(onClick = onNavigateToStats) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = "Estadísticas",
                        )
                    }
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Información",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            HomeFab(
                onExpenseClick = { showExpenseDialog = true },
                onIncomeClick = { showIncomeDialog = true },
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> LoadingContent()
                is HomeUiState.Ready -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
                        TotalBalanceCard(totalBalance = state.totalBalance)
                        Spacer(modifier = Modifier.height(16.dp))
                        PerCurrencySubtotals(subtotals = state.perCurrencySubtotals)
                        Spacer(modifier = Modifier.height(16.dp))
                        PerSourceBreakdownSection(
                            perSource = state.perSource,
                            onNavigateToFuentes = onNavigateToFuentes,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        RecentExpensesList(
                            expenses = state.recentExpenses,
                            onNavigateToExpenseList = onNavigateToExpenseList,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        RecentIncomesList(
                            incomes = state.recentIncomes,
                            onNavigateToIncomeList = onNavigateToIncomeList,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        RatesIndicator(rates = state.rates)
                    }
                }
                is HomeUiState.Empty -> EmptyStateContent()
                is HomeUiState.Error -> ErrorStateContent(state.message)
            }
        }
    }

    // ── Info dialog ──────────────────────────────────────────────────────
    if (showInfoDialog) {
        InfoDialog(onDismiss = { showInfoDialog = false })
    }

    // Form dialogs — rendered as siblings of the Scaffold so they overlay the
    // full screen (not just the inner Surface).
    if (showExpenseDialog) {
        ExpenseFormDialog(
            onDismiss = { showExpenseDialog = false },
        )
    }
    if (showIncomeDialog) {
        IncomeFormDialog(
            onDismiss = { showIncomeDialog = false },
        )
    }

    // ── Update flow dialogs ───────────────────────────────────────────────
    // Driven by HomeViewModel.updateState. Each branch renders a sibling
    // dialog so it overlays the whole screen (same pattern as the form
    // dialogs above).
    when (val s = updateState) {
        is UpdateUiState.UpdateAvailable -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                title = { Text("Nueva versión v${s.version}") },
                text = {
                    Text(
                        text = "Tamaño: ${formatBytes(s.sizeBytes)}",
                        textAlign = TextAlign.Start,
                    )
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("Descartar")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.startDownload(context) }) {
                        Text("Descargar")
                    }
                },
            )
        }
        is UpdateUiState.Downloading -> {
            AlertDialog(
                onDismissRequest = { /* not dismissable mid-download */ },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                ),
                icon = {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        strokeWidth = 2.dp,
                    )
                },
                title = { Text("Descargando actualización") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LinearProgressIndicator(
                            progress = { s.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "${(s.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {}, enabled = false) {
                        Text("Instalar")
                    }
                },
            )
        }
        is UpdateUiState.ReadyToInstall -> {
            UpdateReadyDialog(
                file = s.file,
                onInstall = { viewModel.installUpdate(s.file) },
                onCancel = { viewModel.dismissUpdateDialog() },
            )
        }
        else -> Unit
    }
}

@Composable
private fun UpdateReadyDialog(
    file: File,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text("Listo para instalar") },
        text = {
            Text(
                text = "La descarga terminó. Tocá Instalar para abrir el instalador del sistema.",
            )
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancelar") }
        },
        confirmButton = {
            TextButton(onClick = onInstall) { Text("Instalar") }
        },
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "tamaño desconocido"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return String.format(Locale.US, "%.1f %s", value, units[unitIndex])
}

@Composable
private fun InfoDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = "ViDa",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ViDa es una aplicación de gestión de finanzas personales diseñada pensando en las necesidades financieras de los cubanos.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                HorizontalDivider()
                Text(
                    text = "Únete a nuestro canal de Telegram para novedades, ayuda y comunidad:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { uriHandler.openUri("https://t.me/ViDaAppCuba") }) {
                    Text("t.me/ViDaAppCuba")
                }
                Text(
                    text = "Accede a nuestro perfil de GitHub:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { uriHandler.openUri("https://github.com/Viera97") }) {
                    Text("github.com/Viera97")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
    )
}