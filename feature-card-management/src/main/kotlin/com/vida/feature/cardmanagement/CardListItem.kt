package com.vida.feature.cardmanagement

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
 * Layout mirrors [com.vida.app.ui.FuentesScreen.FullCardItem] so that
 * the card list view ("Todas las tarjetas") looks identical to the
 * preview shown on the Fuentes tab. The only difference is the top-right
 * icon: here it is a delete [IconButton] instead of a [CreditCard] icon.
 *
 * Long-press opens a [DropdownMenu] with "Editar" / "Eliminar".
 *
 * @param card Pre-formatted display item.
 * @param onClick Invoked on tap (opens edit dialog).
 * @param onEdit Invoked from context menu "Editar".
 * @param onDelete Invoked from delete button or context menu "Eliminar".
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
            .background(brush = gradientBrush, shape = cardShape)
    } else {
        modifier.fillMaxWidth()
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { showContextMenu = true },
                    )
                    .padding(16.dp),
            ) {
                // ── Top row: name on the left, delete button on the right ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = card.note ?: card.bank,
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

                Spacer(modifier = Modifier.height(4.dp))

                // ── Second row: bank name on the left, masked number on the right ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = card.bank,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = card.formattedNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Balance (prominent) ──
                Text(
                    text = card.balanceFormatted,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Badges row: type, currency, expiry ──
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                    Text(
                        text = card.currency,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = card.expiryFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Brand logo at bottom-right (Box overlay, same as FullCardItem) ──
            brand.logoDrawable?.let { drawable ->
                val tint = brand.logoTint
                Image(
                    painter = painterResource(drawable),
                    contentDescription = brand.logoContentDescription,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                        .size(36.dp),
                    colorFilter = tint?.let { ColorFilter.tint(it) },
                )
            }
        }
    }

    // ── Context menu (long-press) ──────────────────────────────────────
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
