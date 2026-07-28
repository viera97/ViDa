package com.vida.feature.home.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Home FAB — single "+" button that opens the add-expense-or-income menu.
 *
 * Mirrors the FuentesScreen FAB pattern: one FAB that triggers an option
 * dialog instead of separate buttons for each action.
 *
 * @param onClick Called when the FAB is tapped.
 */
@Composable
fun HomeFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Agregar",
        )
    }
}
