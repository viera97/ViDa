package com.vida.feature.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vida.feature.home.RecentExpenseItem

/**
 * Recent expenses list section (R5).
 *
 * Renders at most 5 [RecentExpenseItem] rows (newest first) with
 * category name, source label + relative date, and right-aligned amount.
 * A divider separates each row.
 * Section header is "Gastos recientes".
 * When the list is non-empty, a "Ver todos →" link navigates to the
 * full expense list.
 *
 * @param expenses Recent expense items (max 5).
 * @param onNavigateToExpenseList Callback for "Ver todos →" navigation.
 * @param modifier Optional modifier for this composable.
 */
@Composable
fun RecentExpensesList(
    expenses: List<RecentExpenseItem>,
    onNavigateToExpenseList: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (expenses.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = "Gastos recientes",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        expenses.forEachIndexed { index, expense ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.categoryName,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "${expense.sourceLabel} · ${expense.relativeDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = expense.formattedAmount,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (index < expenses.lastIndex) {
                HorizontalDivider()
            }
        }
        TextButton(
            onClick = onNavigateToExpenseList,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("Ver todos →")
        }
    }
}
