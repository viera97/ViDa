package com.vida.feature.expense.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Optional multi-line note text input.
 *
 * Supports up to 3 visible lines with no validation requirements.
 * The note is a free-form field that can be left blank.
 *
 * @param value Current note string.
 * @param onChanged Callback when the text changes.
 */
@Composable
fun NoteInput(
    value: String,
    onChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChanged,
        label = { Text("Nota (opcional)") },
        singleLine = false,
        maxLines = 3,
        modifier = modifier.fillMaxWidth(),
    )
}
