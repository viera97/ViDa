package com.vida.feature.expense.form

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vida.feature.expense.SourceItem

/**
 * Trigger card for the source picker bottom sheet.
 *
 * Shows the selected source label and currency when a source is selected,
 * or a placeholder label otherwise. Tapping the card invokes [onShowSheet].
 *
 * @param selectedSource The currently selected source, or null.
 * @param onShowSheet Callback to open the source bottom sheet.
 * @param error Validation error message from `validationErrors["source"]`, or null.
 */
@Composable
fun SourceSelector(
    selectedSource: SourceItem?,
    onShowSheet: () -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onShowSheet,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            if (selectedSource != null) {
                Text(
                    text = selectedSource.label,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${selectedSource.currency.symbol})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Fuente de fondos",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
        }
    }
}
