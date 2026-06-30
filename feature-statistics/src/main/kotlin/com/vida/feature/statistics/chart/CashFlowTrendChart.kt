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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vida.core.format.formatMoney
import com.vida.domain.model.Currency
import com.vida.domain.model.statistics.CashFlowPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Line chart showing expense and income totals over time.
 *
 * Dual-line series: red/orange for expenses, green/blue for income.
 * X-axis shows date labels, Y-axis shows monetary amounts.
 *
 * Bucket granularity (daily/weekly/monthly) is determined by the
 * [CashFlowPoint.periodStart] values returned from the use case.
 *
 * @param cashFlow Time-series data for the selected period.
 * @param modifier Modifier for the composable root.
 */
@Composable
fun CashFlowTrendChart(
    cashFlow: List<CashFlowPoint>,
    modifier: Modifier = Modifier,
) {
    if (cashFlow.isEmpty()) return

    val density = LocalDensity.current
    val textPaint = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = with(density) { 10.dp.toPx() }
            isAntiAlias = true
        }
    }

    // Flatten multi-currency to total per side for simplified visualization.
    // For a more detailed view, per-currency series could be added.
    val chartData = remember(cashFlow) {
        cashFlow.map { point ->
            TrendPoint(
                label = formatPeriodLabel(point.periodStart),
                expenseTotal = point.expenseTotal?.values?.sumOf {
                    it.amount.toDouble()
                } ?: 0.0,
                incomeTotal = point.incomeTotal?.values?.sumOf {
                    it.amount.toDouble()
                } ?: 0.0,
            )
        }
    }

    val maxValue = remember(chartData) {
        chartData.maxOf { maxOf(it.expenseTotal, it.incomeTotal) }
    }

    val expenseColor = MaterialTheme.colorScheme.error
    val incomeColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Legend ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            LegendItem(color = expenseColor, label = "Gastos")
            LegendItem(color = incomeColor, label = "Ingresos")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Chart canvas ──────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(start = 40.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        ) {
            val chartWidth = size.width
            val chartHeight = size.height
            val drawWidth = chartWidth
            val drawHeight = chartHeight
            val pointCount = chartData.size

            if (pointCount < 1 || maxValue <= 0.0) return@Canvas

            val stepX = if (pointCount > 1) drawWidth / (pointCount - 1).toFloat()
                        else drawWidth / 2f

            // ── Y-axis grid lines ─────────────────────────────────────
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = drawHeight * i / gridLines
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1f,
                )
            }

            // ── Data series ───────────────────────────────────────────
            if (pointCount >= 2) {
                // Line chart for 2+ points
                fun drawSeries(
                    data: List<TrendPoint>,
                    color: Color,
                    valueSelector: (TrendPoint) -> Double,
                ) {
                    val path = Path()
                    data.forEachIndexed { index, point ->
                        val x = stepX * index
                        val y = drawHeight * (1f - (valueSelector(point) / maxValue).toFloat())
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                drawSeries(chartData, expenseColor) { it.expenseTotal }
                drawSeries(chartData, incomeColor) { it.incomeTotal }
            } else {
                // Dot markers for single point
                val point = chartData.first()
                val cx = drawWidth / 2f

                fun drawDot(value: Double, color: Color) {
                    val cy = drawHeight * (1f - (value / maxValue).toFloat())
                    drawCircle(color = color, radius = 6f, center = Offset(cx, cy))
                }
                drawDot(point.expenseTotal, expenseColor)
                drawDot(point.incomeTotal, incomeColor)
            }

            // ── X-axis labels ─────────────────────────────────────────
            val maxLabels = (chartWidth / 60f).toInt().coerceAtLeast(2)
            val labelStep = (pointCount / maxLabels).coerceAtLeast(1)
            chartData.forEachIndexed { index, point ->
                if (index % labelStep == 0 || index == pointCount - 1) {
                    val x = stepX * index
                    drawContext.canvas.nativeCanvas.drawText(
                        point.label,
                        x - textPaint.measureText(point.label) / 2f,
                        size.height,
                        textPaint,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatPeriodLabel(instant: Instant): String {
    val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    return if (localDate.dayOfMonth == 1) {
        // Month label for monthly/weekly bucketing
        localDate.format(DateTimeFormatter.ofPattern("MMM"))
    } else {
        // Day label for daily bucketing
        localDate.format(DateTimeFormatter.ofPattern("d/M"))
    }
}

private data class TrendPoint(
    val label: String,
    val expenseTotal: Double,
    val incomeTotal: Double,
)
