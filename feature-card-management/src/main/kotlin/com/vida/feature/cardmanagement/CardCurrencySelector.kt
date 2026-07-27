package com.vida.feature.cardmanagement

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
 * @param selectedCurrencyCode The currently selected currency code, or null.
 * @param isError Whether to show error state (orphan currency).
 * @param errorMessage Error message to display, or null.
 * @param onShowSheet Callback to open the currency bottom sheet.
 */
@Composable
fun CardCurrencySelector(
    selectedCurrencyCode: String?,
    isError: Boolean = false,
    errorMessage: String? = null,
    onShowSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().clickable(onClick = onShowSheet)) {
        OutlinedTextField(
            value = selectedCurrencyCode ?: "",
            onValueChange = {},
            label = { Text("Moneda") },
            isError = isError,
            supportingText = errorMessage?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error) } },
            readOnly = true,
            enabled = false,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledSupportingTextColor = MaterialTheme.colorScheme.error,
            ),
        )
    }
}
