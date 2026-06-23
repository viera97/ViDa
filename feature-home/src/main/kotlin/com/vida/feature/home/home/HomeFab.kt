package com.vida.feature.home.home

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Home FAB — navigates to the expense recording form.
 *
 * Visible in all states (Loading, Ready, Empty, Error).
 *
 * @param onClick Called when the FAB is tapped (navigates to expense form).
 */
@Composable
fun HomeFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            text = "＋",
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}