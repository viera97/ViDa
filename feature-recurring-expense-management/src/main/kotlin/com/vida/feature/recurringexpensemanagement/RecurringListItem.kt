package com.vida.feature.recurringexpensemanagement

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
/** Frequency chip colors. */
private val DailyColor = Color(0xFF1565C0)   // Blue
private val WeeklyColor = Color(0xFF2E7D32)  // Green
private val MonthlyColor = Color(0xFFE65100) // Orange
private val YearlyColor = Color(0xFF6A1B9A)  // Purple

/**
 * A single recurring expense row rendered as a Material3 [Card].
 *
 * Displays:
 * - Amount + currency as headline
 * - Category label
 * - Frequency badge (colored chip)
 * - Source type icon (💰/♠/💎)
 * - Next due date
 * - Active/inactive Switch toggle
 *
 * Long-press opens a [DropdownMenu] with "Editar" / "Eliminar" / "Generar".
 * "Generar" is hidden for inactive templates.
 *
 * @param item Pre-formatted display item.
 * @param onClick Invoked on tap (opens edit dialog — PR #2).
 * @param onEdit Invoked from context menu "Editar" (PR #2).
 * @param onDelete Invoked from context menu "Eliminar".
 * @param onGenerate Invoked from context menu "Generar" (PR #3).
 * @param onToggleActive Invoked when the isActive Switch is toggled.
 * @param modifier Optional [Modifier].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecurringListItem(
    item: RecurringDisplayItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onGenerate: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showContextMenu = true },
                )
                .padding(16.dp),
        ) {
            // Headline row: amount + isActive Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Amount + currency
                Text(
                    text = "${item.amountFormatted} ${item.currencyCode}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isActive)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )

                // isActive toggle
                Switch(
                    checked = item.isActive,
                    onCheckedChange = onToggleActive,
                )
            }

            Spacer(modifier = Modifier.padding(top = 4.dp))

            // Description
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.isActive)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.padding(top = 8.dp))

            // Badges row: category, frequency chip, sourceType icon, next due
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Category label
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = item.categoryName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }

                // Frequency badge (colored)
                FrequencyColorChip(label = item.frequencyLabel)

                // Source type icon
                Text(
                    text = item.sourceTypeIcon,
                    style = MaterialTheme.typography.bodySmall,
                )

                // Next due date
                Text(
                    text = item.nextDueFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Context menu ─────────────────────────────────────────────────
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Editar") },
                onClick = {
                    showContextMenu = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text("Eliminar") },
                onClick = {
                    showContextMenu = false
                    onDelete()
                },
            )
            // "Generar" hidden for inactive templates
            if (item.isActive) {
                DropdownMenuItem(
                    text = { Text("Generar") },
                    onClick = {
                        showContextMenu = false
                        onGenerate()
                    },
                )
            }
        }
    }
}

/**
 * Renders the frequency label in a colored chip.
 *
 * Colors:
 * - Diario → Blue
 * - Semanal → Green
 * - Mensual → Orange
 * - Anual → Purple
 */
@Composable
private fun FrequencyColorChip(label: String) {
    val color = when (label) {
        "Diario" -> DailyColor
        "Semanal" -> WeeklyColor
        "Mensual" -> MonthlyColor
        "Anual" -> YearlyColor
        else -> Color.Gray
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}
