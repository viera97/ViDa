package com.vida.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vida.feature.categorymanagement.CategoryListScreen
import com.vida.feature.expense.ExpenseFormScreen
import com.vida.feature.expenselist.ExpenseDetailScreen
import com.vida.feature.expenselist.ExpenseListScreen
import com.vida.feature.home.HomeScreen

/**
 * Root app composable. Sets up [NavHost] with routes:
 * - "home" → [HomeScreen] (dashboard)
 * - "expense/new" → [ExpenseFormScreen] (expense recording form)
 * - "expenses" → [ExpenseListScreen] (full expense list with filters)
 * - "expense/{id}" → [ExpenseDetailScreen] (placeholder for PR #3)
 * - "categories" → [CategoryListScreen] (category management)
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
            )
        }
        composable("expense/new") {
            ExpenseFormScreen(
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
    }
}
