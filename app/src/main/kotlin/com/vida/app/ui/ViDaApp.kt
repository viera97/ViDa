package com.vida.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vida.feature.expense.ExpenseFormScreen
import com.vida.feature.home.HomeScreen

/**
 * Root app composable. Sets up [NavHost] with two routes:
 * - "home" → [HomeScreen] (dashboard)
 * - "expense/new" → [ExpenseFormScreen] (expense recording form)
 *
 * Navigation Compose is a new dependency introduced for this feature.
 * Hardware/system back returns to the home screen.
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
            )
        }
        composable("expense/new") {
            ExpenseFormScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
