package com.vida.feature.ratemanagement

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A single currency rate row rendered as a Material3 [Card].
 *
 * Displays the pair label (e.g. "CUP → USD"), the formatted rate value,
 * and the last-updated date.
 *
 * Tap → opens edit dialog. Long-press → context menu with "Editar" / "Eliminar".
 *
 * @param rate Pre-formatted display item.
 * @param onClick Invoked on tap (opens edit dialog).
 * @param onEdit Invoked from context menu "Editar".
 * @param onDelete Invoked from context menu "Eliminar" (opens delete confirmation).
 * @param modifier Optional [Modifier].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RateListItem(
    rate: RateDisplayItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
            // Headline: pair label
            Text(
                text = rate.pairLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.padding(top = 8.dp))

            // Details row: rate value + date
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = rate.rateFormatted,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = rate.updatedAtFormatted,
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
        }
    }
}
