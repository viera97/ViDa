package com.vida.feature.recurringexpensemanagement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * Trigger field for the recurring expense source picker bottom sheet.
 *
 * Renders as a disabled [OutlinedTextField] (transparent background, matching
 * the rest of the recurring form fields) wrapped in a clickable [Box]. Tapping
 * the field invokes [onShowSheet].
 *
 * - When [selectedSource] is non-null, displays "label (CURRENCY)".
 * - Otherwise displays a "Origen" placeholder.
 *
 * Note: unlike `feature-expense`'s SourceSelector, this picker shows wallets
 * AND cards but NOT stashes (recurring expenses are not allowed to be sourced
 * from a stash).
 *
 * @param selectedSource The currently selected source, or null.
 * @param onShowSheet Callback to open the source bottom sheet.
 * @param error Validation error message, or null.
 */
@Composable
fun RecurringSourceSelector(
    selectedSource: RecurringSourceItem?,
    onShowSheet: () -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().clickable(onClick = onShowSheet)) {
        OutlinedTextField(
            value = selectedSource?.label ?: "",
            onValueChange = {},
            label = {
                Text(
                    text = if (selectedSource != null) {
                        "${selectedSource.label} (${selectedSource.currency.code})"
                    } else {
                        "Origen"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            isError = error != null,
            supportingText = error?.let { msg -> { Text(msg) } },
            readOnly = true,
            enabled = false,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledSupportingTextColor = MaterialTheme.colorScheme.error,
            ),
        )
    }
}
