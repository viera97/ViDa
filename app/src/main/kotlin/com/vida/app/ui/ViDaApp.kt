package com.vida.app.ui

import androidx.compose.runtime.Composable
import com.vida.feature.home.HomeScreen

/**
 * Root app composable. Delegates to [HomeScreen] which provides its own
 * Scaffold (TopAppBar + FAB).
 */
@Composable
fun ViDaApp() {
    HomeScreen()
}