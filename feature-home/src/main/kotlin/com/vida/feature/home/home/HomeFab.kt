package com.vida.feature.home.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Home FAB cluster — two stacked floating buttons for the dashboard.
 *
 * - Primary (bottom): TrendingDown → opens the expense recording form.
 * - Secondary (top): TrendingUp → reserved for the future income feature.
 *   Wired as a no-op until that feature ships; the affordance is intentionally
 *   visible so the layout is final and the upcoming CTA has its slot.
 *
 * Stacking pattern (secondary on top, primary at the bottom) mirrors the
 * FuentesScreen FAB cluster.
 *
 * @param onExpenseClick Called when the expense FAB is tapped.
 * @param onIncomeClick Called when the income FAB is tapped. Defaults to a
 *   no-op so callers don't have to wire it until the income feature exists.
 */
@Composable
fun HomeFab(
    onExpenseClick: () -> Unit,
    modifier: Modifier = Modifier,
    onIncomeClick: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        SmallFloatingActionButton(onClick = onIncomeClick) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = "Ingresos (próximamente)",
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        FloatingActionButton(onClick = onExpenseClick) {
            Icon(
                imageVector = Icons.Default.TrendingDown,
                contentDescription = "Registrar gasto",
            )
        }
    }
}
