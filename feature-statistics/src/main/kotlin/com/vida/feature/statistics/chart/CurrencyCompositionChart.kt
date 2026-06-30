package com.vida.feature.statistics.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vida.core.format.formatMoney
import com.vida.domain.model.statistics.CurrencyComposition

/**
 * Column chart showing expense and income totals per currency.
 *
 * Each currency gets a grouped bar with expense (red) and income (primary color).
 * Amounts are displayed in their original currency — no cross-currency conversion.
 */
@Composable
fun CurrencyCompositionChart(
    composition: List<CurrencyComposition>,
    modifier: Modifier = Modifier,
) {
    if (composition.isEmpty()) return

    val maxValue = remember(composition) {
        composition.maxOf {
            maxOf(
                it.expenseTotal?.amount?.toDouble() ?: 0.0,
                it.incomeTotal?.amount?.toDouble() ?: 0.0,
            )
        }
    }

    val expenseColor = MaterialTheme.colorScheme.error
    val incomeColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            LegendItem(color = expenseColor, label = "Gastos")
            LegendItem(color = incomeColor, label = "Ingresos")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bars per currency
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            composition.forEach { entry ->
                val expenseValue = entry.expenseTotal?.amount?.toDouble() ?: 0.0
                val incomeValue = entry.incomeTotal?.amount?.toDouble() ?: 0.0

                Column {
                    Text(
                        text = entry.currency.code,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BarRow(
                        value = expenseValue,
                        maxValue = maxValue,
                        color = expenseColor,
                        label = entry.expenseTotal?.let { formatMoney(it) },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BarRow(
                        value = incomeValue,
                        maxValue = maxValue,
                        color = incomeColor,
                        label = entry.incomeTotal?.let { formatMoney(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun BarRow(
    value: Double,
    maxValue: Double,
    color: Color,
    label: String?,
) {
    val fraction = if (maxValue > 0.0) (value / maxValue).toFloat().coerceIn(0f, 1f) else 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(20.dp),
        ) {
            drawRoundRect(
                color = Color.LightGray.copy(alpha = 0.2f),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
            )
            if (fraction > 0f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset.Zero,
                    size = Size(size.width * fraction, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(120.dp),
            )
        }
    }
}
