package com.vida.feature.currencymanagement.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/**
 * Dialog for creating or editing a currency.
 *
 * Shows [OutlinedTextField]s for name and code (pre-filled when editing).
 * The code field is limited to 10 characters and forces uppercase.
 * The save button is disabled when name or code is blank, or a save operation
 * is in progress.
 *
 * @param initialName Pre-populated name (empty for new currencies).
 * @param initialCode Pre-populated code (empty for new currencies).
 * @param isEdit `true` to show "Editar moneda" title, `false` for "Nueva moneda".
 * @param isSaving When `true` the save button and inputs are disabled.
 * @param onDismiss Invoked on cancel or tap-outside.
 * @param onSave Invoked with `(name, code)` when the user taps "Guardar".
 */
@Composable
fun CurrencyFormDialog(
    initialName: String = "",
    initialCode: String = "",
    isEdit: Boolean = false,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, code: String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var code by remember { mutableStateOf(initialCode) }

    val isValid = name.isNotBlank() && code.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEdit) "Editar moneda" else "Nueva moneda")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { newValue ->
                        if (newValue.length <= 10) {
                            code = newValue.uppercase()
                        }
                    },
                    label = { Text("Código") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Máximo 10 caracteres — ej. CUP, USD, EUR")
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, code) },
                enabled = isValid && !isSaving,
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
