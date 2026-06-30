package com.vida.feature.recurringexpensemanagement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vida.domain.model.SourceType

/**
 * Modal bottom sheet displaying sources for a recurring expense.
 *
 * Groups: Wallets (always if any), Cards (if any). Stashes are intentionally
 * NOT shown — recurring expenses only support wallet and card sources.
 * Each source item shows label, optional subtitle (masked card number),
 * and a currency badge. The currently selected source is highlighted.
 *
 * @param sources Source items (order: WALLET, CARD).
 * @param selectedSource The currently selected source, or null.
 * @param onDismiss Callback when the sheet is dismissed.
 * @param onSourceSelected Callback when a source is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringSourceSheet(
    sources: List<RecurringSourceItem>,
    selectedSource: RecurringSourceItem?,
    onDismiss: () -> Unit,
    onSourceSelected: (RecurringSourceItem) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn {
            // --- Wallet section ---
            val wallets = sources.filter { it.type == SourceType.WALLET }
            if (wallets.isNotEmpty()) {
                item(key = "wallets-header") {
                    SectionHeader("Billeteras")
                }
                items(
                    items = wallets,
                    key = { "wallet-${it.id}" },
                ) { wallet ->
                    SourceRow(
                        source = wallet,
                        isSelected = wallet == selectedSource,
                        onClick = { onSourceSelected(wallet) },
                    )
                }
            }

            // --- Cards section ---
            val cards = sources.filter { it.type == SourceType.CARD }
            if (cards.isNotEmpty()) {
                item(key = "cards-divider") {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
                item(key = "cards-header") {
                    SectionHeader("Tarjetas")
                }
                items(
                    items = cards,
                    key = { "card-${it.id}" },
                ) { card ->
                    SourceRow(
                        source = card,
                        isSelected = card == selectedSource,
                        onClick = { onSourceSelected(card) },
                    )
                }
            }

            // --- Stashes section ---
            val stashes = sources.filter { it.type == SourceType.STASH }
            if (stashes.isNotEmpty()) {
                item(key = "stashes-divider") {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
                item(key = "stashes-header") {
                    SectionHeader("Alcancías")
                }
                items(
                    items = stashes,
                    key = { "stash-${it.id}" },
                ) { stash ->
                    SourceRow(
                        source = stash,
                        isSelected = stash == selectedSource,
                        onClick = { onSourceSelected(stash) },
                    )
                }
            }
        }
    }
}

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

@Composable
private fun SourceRow(
    source: RecurringSourceItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = source.label,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        supportingContent = source.subtitle?.let { subtitle ->
            {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        trailingContent = {
            Badge {
                Text(source.currency.code)
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = if (isSelected) {
            ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        } else {
            ListItemDefaults.colors()
        },
    )
}
