package com.vida.feature.home.home

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Home FAB — no-op in slice 1 (R7 S1).
 *
 * Visible in all states (Loading, Ready, Empty, Error) but click does
 * nothing. Future slices add expense/transfer recording.
 */
@Composable
fun HomeFab(
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = { /* no-op — R7 S1 */ },
        modifier = modifier,
    ) {
        Text(
            text = "＋",
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}