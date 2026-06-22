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
import java.math.BigDecimal

/**
 * Currency rates indicator (R6).
 *
 * Renders "1 USD = X CUP" and "1 MLC = Y CUP" chips when rates are present.
 * Entirely hidden when [rates] is null or empty (S3 graceful degradation).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatesIndicator(
    rates: Map<String, BigDecimal>?,
    modifier: Modifier = Modifier,
) {
    if (rates.isNullOrEmpty()) return

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rates.forEach { (currency, rate) ->
            Text(
                text = "1 $currency = ${rate.toPlainString()} CUP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
