package com.vida.feature.cardmanagement

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vida.domain.model.CardType

/**
 * Type badge colors matching card type semantics.
 */
private val DebitColor = Color(0xFF1565C0)   // Blue
private val CreditColor = Color(0xFF2E7D32)  // Green
private val PrepaidColor = Color(0xFFE65100) // Orange

/** Human-readable badge label per [CardType]. */
private val CardType.label: String
    get() = when (this) {
        CardType.DEBIT -> "DÉBITO"
        CardType.CREDIT -> "CRÉDITO"
        CardType.PREPAID -> "PREPAGO"
    }

/** Surface color for the type badge. */
private val CardType.badgeColor: Color
    get() = when (this) {
        CardType.DEBIT -> DebitColor
        CardType.CREDIT -> CreditColor
        CardType.PREPAID -> PrepaidColor
    }

/**
 * A single card row rendered as a Material3 [Card].
 *
 * Displays the bank name as a headline, the masked number as a subtitle,
 * a colored type badge, the currency code, and the expiry date.
 *
 * Long-press opens a [DropdownMenu] with "Editar" / "Eliminar".
 *
 * @param card Pre-formatted display item.
 * @param onClick Invoked on tap (opens edit dialog).
 * @param onEdit Invoked from context menu "Editar". Opens edit dialog with this card.
 * @param onDelete Invoked from context menu "Eliminar". Opens delete confirmation.
 * @param modifier Optional [Modifier].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardListItem(
    card: CardDisplayItem,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showContextMenu by remember { mutableStateOf(false) }

    val brand = bankBrandFor(card.bank)
    val cardShape = RoundedCornerShape(12.dp)
    val gradientBrush: Brush? = brand.gradient
    val cardModifier = if (gradientBrush != null) {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(brush = gradientBrush, shape = cardShape)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    }
    val cardColors = if (brand.gradient != null) {
        CardDefaults.cardColors(containerColor = Color.Transparent)
    } else {
        CardDefaults.cardColors()
    }
    Card(
        modifier = cardModifier,
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = cardColors,
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
            // Top row: bank name on the left, delete affordance on the right.
            // Using SpaceBetween inside the same Column's combinedClickable so
            // the long-press context menu still works and tapping the icon
            // dispatches onDelete without competing pointer routing.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.bank,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.padding(top = 4.dp))

            // Subtitle: masked number
            Text(
                text = card.formattedNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.padding(top = 8.dp))

            // Badges row: type, currency, expiry
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Type badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = card.type.badgeColor,
                ) {
                    Text(
                        text = card.type.label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }

                // Currency code
                Text(
                    text = card.currency.code,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Expiry
                Text(
                    text = card.expiryFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Note indicator
            if (card.note != null) {
                Spacer(modifier = Modifier.padding(top = 4.dp))
                Text(
                    text = card.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            // Brand logo: rendered inline at the bottom-right of the Column
            // (no Box overlay) so the long-press combinedClickable on the
            // Column keeps full hit-test coverage.
            brand.logoDrawable?.let { drawable ->
                val tint = brand.logoTint
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Image(
                        painter = painterResource(drawable),
                        contentDescription = brand.logoContentDescription,
                        modifier = Modifier
                            .size(36.dp),
                        colorFilter = tint?.let { ColorFilter.tint(it) },
                    )
                }
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
