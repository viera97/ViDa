package com.vida.feature.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.feature.home.home.formatHomeMoney

/**
 * Per-currency subtotals row (R3).
 *
 * Displays non-zero currency subtotals in a horizontal [FlowRow].
 * Only currencies with non-zero sources are rendered (S4).
 * Entire section is omitted when [subtotals] is empty.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerCurrencySubtotals(
    subtotals: Map<Currency, Money>,
    modifier: Modifier = Modifier,
) {
    if (subtotals.isEmpty()) return

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        subtotals.forEach { (_, money) ->
            Text(
                text = formatHomeMoney(money),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
