package com.vida.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vida.feature.home.home.EmptyStateContent
import com.vida.feature.home.home.ErrorStateContent
import com.vida.feature.home.home.HomeFab
import com.vida.feature.home.home.LoadingContent
import com.vida.feature.home.home.PerCurrencySubtotals
import com.vida.feature.home.home.PerSourceBreakdownSection
import com.vida.feature.home.home.RatesIndicator
import com.vida.feature.home.home.RecentExpensesList
import com.vida.feature.home.home.TotalBalanceCard

/**
 * Root composable for the Home dashboard.
 *
 * Consumes [HomeViewModel.uiState] and dispatches to the appropriate state
 * renderer: [LoadingContent], [ReadyContent], [EmptyStateContent], or [ErrorStateContent].
 *
 * The FAB is visible in ALL states (R7 — no-op in slice 1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExpenseForm: () -> Unit = {},
    onNavigateToExpenseList: () -> Unit = {},
    onNavigateToCategoryManagement: () -> Unit = {},
    onNavigateToCardManagement: () -> Unit = {},
    onNavigateToStashManagement: () -> Unit = {},
    onNavigateToRateManagement: () -> Unit = {},
    onNavigateToWalletManagement: () -> Unit = {},
    onNavigateToTransferManagement: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inicio") },
                actions = {
                    IconButton(onClick = onNavigateToTransferManagement) {
                        Text(
                            text = "↔",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    IconButton(onClick = onNavigateToWalletManagement) {
                        Text(
                            text = "\uD83D\uDCB0",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    IconButton(onClick = onNavigateToCardManagement) {
                        Text(
                            text = "♠",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    IconButton(onClick = onNavigateToStashManagement) {
                        Text(
                            text = "\uD83D\uDC8E",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    IconButton(onClick = onNavigateToRateManagement) {
                        Text(
                            text = "\uD83D\uDCB1",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    IconButton(onClick = onNavigateToCategoryManagement) {
                        Text(
                            text = "⚙",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                },
            )
        },
        floatingActionButton = { HomeFab(onClick = onNavigateToExpenseForm) },
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
                        PerSourceBreakdownSection(perSource = state.perSource)
                        Spacer(modifier = Modifier.height(16.dp))
                        RecentExpensesList(
                            expenses = state.recentExpenses,
                            onNavigateToExpenseList = onNavigateToExpenseList,
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
}