package com.vida.feature.expense.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vida.domain.model.Currency

/**
 * Trigger field for the currency picker bottom sheet in the expense form.
 *
 * @param currency The currently selected currency.
 * @param enabled Whether the selector is interactive.
 * @param onShowSheet Callback to open the currency bottom sheet.
 */
@Composable
fun ExpenseCurrencySelector(
    currency: Currency,
    enabled: Boolean = true,
    onShowSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().then(
            if (enabled) Modifier.clickable(onClick = onShowSheet) else Modifier
        ),
    ) {
        OutlinedTextField(
            value = currency.code,
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
