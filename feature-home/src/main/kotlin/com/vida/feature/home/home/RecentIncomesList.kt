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
import com.vida.feature.home.RecentIncomeItem

/**
 * Recent incomes list section (R5' — mirror of [RecentExpensesList]).
 *
 * Renders at most 5 [RecentIncomeItem] rows (newest first) with the income
 * description, destination source label + relative date, and right-aligned
 * amount. A divider separates each row.
 * Section header is "Ingresos recientes".
 * When the list is non-empty, a "Ver todos →" link navigates to the full
 * income list.
 *
 * @param incomes Recent income items (max 5).
 * @param onNavigateToIncomeList Callback for "Ver todos →" navigation.
 * @param modifier Optional modifier for this composable.
 */
@Composable
fun RecentIncomesList(
    incomes: List<RecentIncomeItem>,
    onNavigateToIncomeList: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (incomes.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = "Ingresos recientes",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        incomes.forEachIndexed { index, income ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = income.description,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "${income.sourceLabel} · ${income.relativeDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = income.formattedAmount,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (index < incomes.lastIndex) {
                HorizontalDivider()
            }
        }
        TextButton(
            onClick = onNavigateToIncomeList,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("Ver todos →")
        }
    }
}
