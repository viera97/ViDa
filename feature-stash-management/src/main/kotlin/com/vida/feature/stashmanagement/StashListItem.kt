package com.vida.feature.stashmanagement

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
import androidx.compose.material3.Surface
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

/**
 * Currency badge colors.
 */
private val CupColor = Color(0xFF1565C0)   // Blue
private val UsdColor = Color(0xFF2E7D32)   // Green
private val MlcColor = Color(0xFFE65100)   // Orange

/** Human-readable badge label per currency code. */
private val String.currencyLabel: String
    get() = when (this) {
        "CUP" -> "CUP"
        "USD" -> "USD"
        "MLC" -> "MLC"
        else -> this
    }

/** Surface color for the currency badge. */
private val String.currencyBadgeColor: Color
    get() = when (this) {
        "CUP" -> CupColor
        "USD" -> UsdColor
        "MLC" -> MlcColor
        else -> Color.Gray
    }

/**
 * A single stash row rendered as a Material3 [Card].
 *
 * Displays the stash name as a headline, a colored currency badge,
 * and the creation date.
 *
 * Long-press opens a [DropdownMenu] with "Editar" / "Eliminar" (no-ops in PR #1).
 *
 * @param stash Pre-formatted display item.
 * @param onClick Invoked on tap (opens edit dialog — PR #2).
 * @param onEdit Invoked from context menu "Editar" (PR #2).
 * @param onDelete Invoked from context menu "Eliminar". Opens delete confirmation.
 * @param modifier Optional [Modifier].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StashListItem(
    stash: StashDisplayItem,
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
            // Headline: stash name
            Text(
                text = stash.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.padding(top = 8.dp))

            // Badges row: currency badge, creation date
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Currency badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = stash.currencyCode.currencyBadgeColor,
                ) {
                    Text(
                        text = stash.currencyCode.currencyLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }

                // Creation date
                Text(
                    text = stash.createdAtFormatted,
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
