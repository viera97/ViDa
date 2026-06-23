package com.vida.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vida.feature.cardmanagement.CardListScreen
import com.vida.feature.categorymanagement.CategoryListScreen
import com.vida.feature.ratemanagement.RateListScreen
import com.vida.feature.stashmanagement.StashListScreen
import com.vida.feature.expense.ExpenseFormScreen
import com.vida.feature.expenselist.ExpenseDetailScreen
import com.vida.feature.expenselist.ExpenseListScreen
import com.vida.feature.home.HomeScreen
import com.vida.feature.transfermanagement.TransferFormScreen
import com.vida.feature.walletmanagement.WalletScreen

/**
 * Root app composable. Sets up [NavHost] with routes:
 * - "home" → [HomeScreen] (dashboard)
 * - "expense/new" → [ExpenseFormScreen] (expense recording form)
 * - "transfer/new" → [TransferFormScreen] (transfer creation form)
 * - "expenses" → [ExpenseListScreen] (full expense list with filters)
 * - "expense/{id}" → [ExpenseDetailScreen] (placeholder for PR #3)
 * - "categories" → [CategoryListScreen] (category management)
 * - "cards" → [CardListScreen] (card management)
 * - "stashes" → [StashListScreen] (stash/savings management)
 * - "rates" → [RateListScreen] (currency rate management)
 * - "wallet" → [WalletScreen] (wallet view/edit)
 *
 * Navigation Compose is a new dependency introduced for this feature.
 * Hardware/system back returns to the previous screen.
 */
@Composable
fun ViDaApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToExpenseForm = {
                    navController.navigate("expense/new")
                },
                onNavigateToExpenseList = {
                    navController.navigate("expenses")
                },
                onNavigateToCategoryManagement = {
                    navController.navigate("categories")
                },
                onNavigateToCardManagement = {
                    navController.navigate("cards")
                },
                onNavigateToStashManagement = {
                    navController.navigate("stashes")
                },
                onNavigateToWalletManagement = {
                    navController.navigate("wallet")
                },
                onNavigateToRateManagement = {
                    navController.navigate("rates")
                },
                onNavigateToTransferManagement = {
                    navController.navigate("transfer/new")
                },
            )
        }
        composable("expense/new") {
            ExpenseFormScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("transfer/new") {
            TransferFormScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("expenses") {
            ExpenseListScreen(
                onNavigateToDetail = { id -> navController.navigate("expense/$id") },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("expense/{id}") {
            ExpenseDetailScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("categories") {
            CategoryListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAdd = { /* PR #2: navigate to add category */ },
                onNavigateToEdit = { /* PR #2: navigate to edit category */ },
            )
        }
        composable("cards") {
            CardListScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("stashes") {
            StashListScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("rates") {
            RateListScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("wallet") {
            WalletScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
