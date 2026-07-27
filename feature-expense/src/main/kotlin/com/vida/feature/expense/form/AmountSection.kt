package com.vida.feature.expense.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * Amount input field.
 *
 * The currency is determined by the selected source — it auto-applies when
 * a source is picked, so no standalone currency selector is shown here.
 *
 * @param amount Current amount string value.
 * @param amountError Validation error message, or null.
 * @param onAmountChanged Callback when the amount text changes.
 */
@Composable
fun AmountSection(
    amount: String,
    amountError: String?,
    onAmountChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = amount,
        onValueChange = onAmountChanged,
        label = { Text("Importe") },
        isError = amountError != null,
        supportingText = amountError?.let { error -> { Text(error) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}
