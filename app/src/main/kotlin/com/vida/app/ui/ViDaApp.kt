package com.vida.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vida.feature.cardmanagement.CardListScreen
import com.vida.feature.categorymanagement.CategoryListScreen
import com.vida.feature.ratemanagement.RateListScreen
import com.vida.feature.onboarding.nav.OnboardingRoutes
import com.vida.feature.onboarding.nav.onboardingNavGraph
import com.vida.feature.onboarding.preferences.WizardPreferences
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
import com.vida.core.crash.CurrentScreenTracker
import com.vida.app.ui.theme.ThemeMode
import com.vida.app.ui.theme.ViDaTheme
import com.vida.app.ui.theme.rememberThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

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
 * - "wizard/welcome" / "wizard/wallet-or-card" / "wizard/get-started"
 *   → first-run wizard (only reachable when [WizardPreferences.wizardCompleted]
 *   is `false`).
 *
 * The global [NavigationBar] is rendered at root level via [Scaffold], making it
 * persistent across all non-wizard screens. Each screen uses its own Scaffold
 * for top bars.
 *
 * `startDestination` is resolved at runtime from DataStore: on a fresh install
 * the first frame is `wizard/welcome`; on subsequent launches it is `home`.
 * While the flag is being read a [CircularProgressIndicator] placeholder fills
 * the screen.
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

    val appViewModel: ViDaAppViewModel = hiltViewModel()
    val startDestination by produceState(StartDestination.Loading) {
        value = if (appViewModel.wizardCompleted.first()) StartDestination.Home
                else StartDestination.Welcome
    }

    ViDaTheme(themeMode = themeMode) {
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            if (currentRoute?.startsWith(OnboardingRoutes.WIZARD_PREFIX) != true) {
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
            }
        },
    ) { innerPadding ->
        when (startDestination) {
            StartDestination.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            StartDestination.Home, StartDestination.Welcome -> {
                NavHost(
                    navController = navController,
                    startDestination = if (startDestination == StartDestination.Home) "home" else OnboardingRoutes.WELCOME,
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
                            onNavigateToCurrencies = { /* future: currencies screen */ },
                            onNavigateToBanks = { /* future: banks screen */ },
                            onNavigateToExportData = { /* future: premium export screen */ },
                            onNavigateToImportData = { /* future: premium import screen */ },
                            onNavigateToSecurity = { /* future: premium security screen */ },
                            onNavigateToTransfermovil = { /* future: premium transfermovil screen */ },
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
                    onboardingNavGraph(
                        navController = navController,
                        onFinished = {
                            navController.navigate("home") {
                                popUpTo(OnboardingRoutes.WELCOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
        }
    }
        // Crash/error dialog overlay — shown on top of all screens when a
        // pending report exists.
        CrashDialog()
    }
    }
}

/**
 * Decision states for [ViDaApp]'s `startDestination`. The graph reads from
 * DataStore on first composition; while the suspend read is in-flight the
 * screen renders a [CircularProgressIndicator] placeholder.
 */
private enum class StartDestination { Loading, Home, Welcome }

/**
 * ViewModel used by [ViDaApp] to expose the wizard-completed flag as a
 * suspending readable property. Kept tiny and stack-local to keep the
 * wiring change additive.
 */
@HiltViewModel
class ViDaAppViewModel @Inject constructor(
    private val wizardPreferences: WizardPreferences,
) : ViewModel() {
    val wizardCompleted: Flow<Boolean> = wizardPreferences.wizardCompleted
}
