package com.vida.feature.expense.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vida.domain.model.Currency

/**
 * Amount input with currency selector chips.
 *
 * Renders an [OutlinedTextField] for decimal amount entry alongside a row
 * of [FilterChip]s for [Currency.entries] (CUP, USD, MLC). Shows validation
 * error text below the text field.
 *
 * @param amount Current amount string value.
 * @param currency Currently selected currency.
 * @param amountError Validation error message from `validationErrors["amount"]`, or null.
 * @param onAmountChanged Callback when the amount text changes.
 * @param onCurrencyChanged Callback when a currency chip is selected.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmountSection(
    amount: String,
    currency: Currency,
    amountError: String?,
    onAmountChanged: (String) -> Unit,
    onCurrencyChanged: (Currency) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChanged,
            label = { Text("Importe") },
            isError = amountError != null,
            supportingText = amountError?.let { error -> { Text(error) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Currency.entries.forEach { entry ->
                FilterChip(
                    selected = entry == currency,
                    onClick = { onCurrencyChanged(entry) },
                    label = { Text(entry.symbol) },
                )
            }
        }
    }
}
