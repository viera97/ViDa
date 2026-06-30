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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vida.core.format.formatMoney
import com.vida.domain.model.statistics.CategoryBreakdown
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Donut chart showing expense totals broken down by category.
 *
 * Each segment is a category with its color, name, total amount, and
 * percentage of the overall total.
 *
 * Uses Canvas to draw the donut arcs. The legend renders below the chart
 * with category color swatches, names, amounts, and percentages.
 *
 * @param breakdown Category expense data for the selected period.
 * @param modifier Modifier for the composable root.
 */
@Composable
fun CategoryDonutChart(
    breakdown: List<CategoryBreakdown>,
    modifier: Modifier = Modifier,
) {
    if (breakdown.isEmpty()) return

    val total = remember(breakdown) {
        breakdown.sumOf { it.total.amount.toDouble() }
    }

    val segments = remember(breakdown) {
        breakdown.map { cat ->
            val value = cat.total.amount.toDouble()
            val percentage = if (total > 0.0) (value / total) * 100.0 else 0.0
            CategoryDonutSegment(
                color = Color(cat.color),
                name = cat.categoryName,
                amountFormatted = formatMoney(cat.total),
                percentage = percentage,
            )
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Donut canvas ──────────────────────────────────────────────────────
        val strokeWidth = 48f
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .padding(8.dp),
        ) {
            val diameter = size.minDimension
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)
            val halfStroke = strokeWidth / 2f
            val innerArcSize = Size(
                diameter - strokeWidth,
                diameter - strokeWidth,
            )
            val innerTopLeft = Offset(
                topLeft.x + halfStroke,
                topLeft.y + halfStroke,
            )

            // Draw outer ring (covers the stroke width) then inner circle
            // to create the donut hole effect.
            var startAngle = -90f
            segments.forEach { seg ->
                val sweep = (seg.percentage / 100.0 * 360.0).toFloat()
                drawArc(
                    color = seg.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )
                startAngle += sweep
            }

            // Center hole (background color)
            drawArc(
                color = Color.Transparent,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerArcSize,
                style = Stroke(width = strokeWidth),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Legend ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            segments.forEach { seg ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Color swatch
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = seg.color)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Name + percentage
                    Text(
                        text = "${seg.name} (${"%.1f".format(seg.percentage)}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    // Amount
                    Text(
                        text = seg.amountFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

private data class CategoryDonutSegment(
    val color: Color,
    val name: String,
    val amountFormatted: String,
    val percentage: Double,
)
