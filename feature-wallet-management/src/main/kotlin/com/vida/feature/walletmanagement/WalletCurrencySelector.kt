package com.vida.feature.walletmanagement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Trigger field for the currency picker bottom sheet.
 *
 * Renders as a disabled [OutlinedTextField] (transparent background, matching
 * the rest of the form fields) wrapped in a clickable [Box]. Tapping
 * the field invokes [onShowSheet].
 *
 * @param selectedCurrencyCode The currently selected currency code, or null.
 * @param onShowSheet Callback to open the currency bottom sheet.
 */
@Composable
fun WalletCurrencySelector(
    selectedCurrencyCode: String?,
    onShowSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().clickable(onClick = onShowSheet)) {
        OutlinedTextField(
            value = selectedCurrencyCode ?: "",
            onValueChange = {},
            label = { Text("Moneda") },
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
