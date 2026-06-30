package com.vida.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vida.feature.cardmanagement.CardListScreen
import com.vida.feature.categorymanagement.CategoryListScreen
import com.vida.feature.ratemanagement.RateListScreen
import com.vida.app.ui.SettingsScreen
import com.vida.feature.recurringexpensemanagement.RecurringListScreen
import com.vida.feature.stashmanagement.StashListScreen
import com.vida.feature.expense.ExpenseFormScreen
import com.vida.feature.expenselist.ExpenseDetailScreen
import com.vida.feature.expenselist.ExpenseListScreen
import com.vida.feature.home.HomeScreen
import com.vida.feature.reports.ReportsScreen
import com.vida.feature.statistics.StatisticsScreen
import com.vida.feature.incomelist.IncomeDetailScreen
import com.vida.feature.incomelist.IncomeListScreen
import com.vida.feature.transfermanagement.TransferFormScreen
import com.vida.feature.walletmanagement.WalletScreen
import com.vida.app.ui.theme.ThemeMode
import com.vida.app.ui.theme.ViDaTheme
import com.vida.app.ui.theme.rememberThemeMode

/**
 * Root app composable. Sets up [NavHost] with routes:
 * - "home" → [HomeScreen] (dashboard)
 * - "expense/new" → [ExpenseFormScreen] (expense recording form)
 * - "transfer/new" → [TransferFormScreen] (transfer creation form)
 * - "expenses" → [ExpenseListScreen] (full expense list with filters)
 * - "expense/{id}" → [ExpenseDetailScreen] (placeholder for PR #3)
 * - "settings" → [SettingsScreen] (app configuration hub)
 * - "categories" → [CategoryListScreen] (category management, from settings)
 * - "cards" → [CardListScreen] (card management)
 * - "stashes" → [StashListScreen] (stash/savings management)
 * - "rates" → [RateListScreen] (currency rate management)
 * - "recurring" → [RecurringListScreen] (recurring expense management)
 * - "wallet" → [WalletScreen] (wallet view/edit)
 * - "fuentes" → [FuentesScreen] (unified wallet + cards view)
 *
 * The global [NavigationBar] is rendered at root level via [Scaffold], making it
 * persistent across all screens. Each screen uses its own Scaffold for top bars.
 *
 * Navigation Compose is a new dependency introduced for this feature.
 * Hardware/system back returns to the previous screen.
 */
@Composable
fun ViDaApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val themeModeState = rememberThemeMode()
    val themeMode by themeModeState
    val onThemeModeChange: (ThemeMode) -> Unit = { themeModeState.value = it }

    ViDaTheme(themeMode = themeMode) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Inicio",
                        )
                    },
                    selected = currentRoute == "home",
                    onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = "Fuentes",
                        )
                    },
                    selected = currentRoute == "fuentes",
                    onClick = {
                        navController.navigate("fuentes") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.CurrencyExchange,
                            contentDescription = "Tasas",
                        )
                    },
                    selected = currentRoute == "rates",
                    onClick = {
                        navController.navigate("rates") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Recurrentes",
                        )
                    },
                    selected = currentRoute == "recurring",
                    onClick = {
                        navController.navigate("recurring") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configuración",
                        )
                    },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
        ) {
            composable(
                "home",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                HomeScreen(
                    onNavigateToExpenseList = { navController.navigate("expenses") },
                    onNavigateToIncomeList = { navController.navigate("incomes") },
                    onNavigateToFuentes = { navController.navigate("fuentes") },
                    onNavigateToStats = { navController.navigate("stats") },
                )
            }
            composable(
                "recurring",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                RecurringListScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                "expense/new",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                ExpenseFormScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                "transfer/new",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                TransferFormScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                "expenses",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                ExpenseListScreen(
                    onNavigateToDetail = { id -> navController.navigate("expense/$id") },
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                "expense/{id}",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                ExpenseDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                "incomes",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                IncomeListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { id -> navController.navigate("income/$id") },
                )
            }
            composable(
                "income/{id}",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                IncomeDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                "settings",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    onNavigateToCategories = { navController.navigate("categories") },
                )
            }
            composable(
                "categories",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                CategoryListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAdd = { /* PR #2: navigate to add category */ },
                    onNavigateToEdit = { /* PR #2: navigate to edit category */ },
                )
            }
            composable(
                "cards",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                CardListScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                "stashes",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                StashListScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                "rates",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                RateListScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                "wallet",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                WalletScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                "fuentes",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                FuentesScreen(
                    onNavigateToTransfer = { navController.navigate("transfer/new") },
                    onNavigateToWallet = { navController.navigate("wallet") },
                    onNavigateToCards = { navController.navigate("cards") },
                )
            }
            composable(
                "stats",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                StatisticsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToReports = { navController.navigate("reports") },
                )
            }
            composable(
                "reports",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
            ) {
                ReportsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
    }
}
