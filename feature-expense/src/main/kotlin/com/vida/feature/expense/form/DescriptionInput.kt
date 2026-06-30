package com.vida.feature.expense.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Single-line description text input.
 *
 * Enforces a maximum of 100 characters. Shows validation error text below
 * the field when [error] is non-null.
 *
 * @param value Current description string.
 * @param error Validation error message from `validationErrors["description"]`, or null.
 * @param onChanged Callback when the text changes.
 */
@Composable
fun DescriptionInput(
    value: String,
    error: String?,
    onChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 100) onChanged(it) },
        label = { Text("Descripción") },
        isError = error != null,
        supportingText = error?.let { err -> { Text(err) } },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}
