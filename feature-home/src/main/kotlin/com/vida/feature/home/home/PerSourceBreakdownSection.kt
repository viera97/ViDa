package com.vida.feature.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vida.domain.model.SourceType
import com.vida.feature.home.PerSource

/**
 * Per-source breakdown section (R4).
 *
 * Renders up to 5 wallet/card/stash rows with a left-side distinguishing icon
 * (wallet / card / stash), the source label, and the right-aligned formatted
 * balance. A "Ver todos →" link at the bottom navigates to the unified
 * [com.vida.app.ui.FuentesScreen] so the user can see every source (mirrors
 * the "Ver todos" affordance of [RecentExpensesList]). The whole section is
 * omitted when [perSource] is empty (S5).
 *
 * The icon-to-source mapping is the single source of truth in this file —
 * [iconFor]. Adding a new [SourceType] requires updating that map; the rest
 * of the composable stays untouched.
 *
 * @param perSource Non-zero balance sources, already capped by [HomeViewModel].
 * @param onNavigateToFuentes Callback for the "Ver todos" link.
 */
@Composable
fun PerSourceBreakdownSection(
    perSource: List<PerSource>,
    onNavigateToFuentes: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (perSource.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Fuentes",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        perSource.forEachIndexed { index, source ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = iconFor(source.sourceType),
                    contentDescription = null, // label below is descriptive enough
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = source.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = source.formatted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (index < perSource.lastIndex) {
                HorizontalDivider()
            }
        }
        TextButton(
            onClick = onNavigateToFuentes,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("Ver todos →")
        }
    }
}

/**
 * Maps a [SourceType] to the icon shown on the left of each row in
 * [PerSourceBreakdownSection]. Centralized so the visual mapping is the single
 * source of truth — adding a new [SourceType] requires updating this map; the
 * composable stays untouched.
 */
private fun iconFor(sourceType: SourceType): ImageVector = when (sourceType) {
    SourceType.WALLET -> Icons.Default.AccountBalanceWallet
    SourceType.CARD -> Icons.Default.CreditCard
    SourceType.STASH -> Icons.Default.Savings
}
