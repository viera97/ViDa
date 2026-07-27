package com.vida.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vida.domain.model.SourceType

private val walletColor = Color(0xFF1565C0)
private val cardColor = Color(0xFF2E7D32)
private val stashColor = Color(0xFFE65100)

/**
 * Modal bottom sheet displaying sources in a grid layout grouped by type.
 *
 * Each source group (Wallets, Cards, Stashes) renders as a section header
 * followed by a [LazyVerticalGrid] of [SourceChip]s. The chip shows a
 * type-appropriate icon, the source label, and the currency code on a colored
 * background. The currently selected source is highlighted.
 *
 * Layout matches the [CategorySheet] grid pattern used elsewhere in the app.
 *
 * @param sources Source items grouped by type (order: WALLET, CARD, STASH).
 * @param selectedSource The currently selected source, or null.
 * @param onDismiss Callback when the sheet is dismissed.
 * @param onSourceSelected Callback when a source is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSheet(
    sources: List<SourceItem>,
    selectedSource: SourceItem?,
    onDismiss: () -> Unit,
    onSourceSelected: (SourceItem) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        // --- Wallet section ---
        val wallets = sources.filter { it.type == SourceType.WALLET }
        if (wallets.isNotEmpty()) {
            SectionHeader("Billeteras")
            SourceGrid(
                sources = wallets,
                selectedSource = selectedSource,
                color = walletColor,
                icon = Icons.Rounded.AccountBalanceWallet,
                onSourceSelected = onSourceSelected,
            )
        }

        // --- Cards section ---
        val cards = sources.filter { it.type == SourceType.CARD }
        if (cards.isNotEmpty()) {
            SectionHeader("Tarjetas")
            SourceGrid(
                sources = cards,
                selectedSource = selectedSource,
                color = cardColor,
                icon = Icons.Rounded.CreditCard,
                onSourceSelected = onSourceSelected,
            )
        }

        // --- Stashes section ---
        val stashes = sources.filter { it.type == SourceType.STASH }
        if (stashes.isNotEmpty()) {
            SectionHeader("Ahorros")
            SourceGrid(
                sources = stashes,
                selectedSource = selectedSource,
                color = stashColor,
                icon = Icons.Rounded.Savings,
                onSourceSelected = onSourceSelected,
            )
        }
    }
}

/**
 * Section header label for a source group (Wallet, Cards, Stashes).
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * Adaptive grid of source chips for a single source type group.
 */
@Composable
private fun SourceGrid(
    sources: List<SourceItem>,
    selectedSource: SourceItem?,
    color: Color,
    icon: ImageVector,
    onSourceSelected: (SourceItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        items(
            count = sources.size,
            key = { "source-${sources[it].type}-${sources[it].id}" },
        ) { index ->
            val source = sources[index]
            SourceChip(
                label = source.label,
                currency = source.currency,
                color = color,
                icon = icon,
                selected = source == selectedSource,
                onClick = { onSourceSelected(source) },
            )
        }
    }
}

/**
 * Individual source chip rendered in a [SourceGrid].
 *
 * Shows a type-appropriate icon, the source label, and the currency code
 * stacked vertically. Modeled after [CategoryChip] for visual consistency.
 */
@Composable
private fun SourceChip(
    label: String,
    currency: String,
    color: Color,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) color else color.copy(alpha = 0.15f),
        tonalElevation = if (selected) 4.dp else 0.dp,
        modifier = Modifier.padding(4.dp),
    ) {
        Box(
            modifier = Modifier.size(width = 120.dp, height = 72.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = currency,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
