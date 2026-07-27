package com.vida.feature.currencymanagement.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A single currency row in the list.
 *
 * Renders the name, a code chip (like "CUP", "USD"), and a "(sistema)" badge
 * for system currencies. System currencies do not show a delete button.
 * Tap opens the edit dialog. Long-press shows a [DropdownMenu] with
 * "Editar" and "Eliminar".
 *
 * @param item Pre-formatted display item.
 * @param onClick Invoked on tap or "Editar" — opens the edit dialog.
 * @param onDeleteClick Invoked on "Eliminar" — triggers the delete confirmation.
 * @param modifier Optional [Modifier].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CurrencyListItem(
    item: CurrencyDisplayItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showContextMenu = true },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Name
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )

            // Code chip
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = item.code,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )

            Spacer(modifier = Modifier.width(8.dp))

            // System badge
            if (item.isSystem) {
                Text(
                    text = "(sistema)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Delete button (non-system only)
            if (!item.isSystem) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
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
                    onClick()
                },
            )
            if (!item.isSystem) {
                DropdownMenuItem(
                    text = { Text("Eliminar") },
                    onClick = {
                        showContextMenu = false
                        onDeleteClick()
                    },
                )
            }
        }
    }
}
