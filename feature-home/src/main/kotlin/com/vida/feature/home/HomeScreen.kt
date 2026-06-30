package com.vida.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
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

    var showExpenseDialog by remember { mutableStateOf(false) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inicio") },
                actions = {
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