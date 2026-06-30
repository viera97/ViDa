package com.vida.feature.stashmanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vida.domain.model.Currency

/**
 * Form dialog for creating or editing a stash.
 *
 * Fields:
 * - Name ([OutlinedTextField], required, 1–100 chars, non-blank)
 * - Currency ([FilterChip] row: CUP / USD / MLC)
 *
 * Save is disabled when [isSaving] is true, name is blank/whitespace-only,
 * or name exceeds 100 characters.
 *
 * @param initialName Pre-populated name (empty for add, existing value for edit).
 * @param initialCurrency Default [Currency] selection (CUP).
 * @param isEdit Whether this is an edit form (affects title).
 * @param isSaving Whether a save operation is in-flight (disables save button).
 * @param onDismiss Called when the dialog is dismissed.
 * @param onSave Called with (name, currency) when the user confirms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StashFormDialog(
    initialName: String = "",
    initialCurrency: Currency = Currency.CUP,
    isEdit: Boolean = false,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, currency: Currency) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var currency by remember { mutableStateOf(initialCurrency) }

    // ── Validation ───────────────────────────────────────────────────────────

    val isNameBlank = name.isBlank()
    val nameError: String? = when {
        name.isNotEmpty() && isNameBlank -> "El nombre es obligatorio"
        name.length > 100 -> "Máximo 100 caracteres"
        else -> null
    }

    val isSaveEnabled = !isNameBlank && name.length <= 100 && !isSaving

    // ── Dialog ────────────────────────────────────────────────────────────────

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (isEdit) "Editar ahorro" else "Agregar ahorro") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Currency — FilterChip row
                Text("Moneda")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Currency.entries.forEach { curr ->
                        FilterChip(
                            selected = currency == curr,
                            onClick = { currency = curr },
                            label = { Text(curr.code) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), currency) },
                enabled = isSaveEnabled,
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar")
            }
        },
    )
}
