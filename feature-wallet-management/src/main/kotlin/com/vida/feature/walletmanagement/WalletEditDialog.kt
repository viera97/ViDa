package com.vida.feature.walletmanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vida.domain.model.Currency

/**
 * AlertDialog for editing the singleton wallet's name and currency.
 *
 * Mirrors [com.vida.feature.stashmanagement.StashFormDialog] but without
 * the add/edit mode distinction — this dialog is always in edit mode since
 * the wallet is a singleton that either exists or is being upserted.
 *
 * Fields:
 * - Name ([OutlinedTextField], required, 1–100 chars, non-blank)
 * - Currency ([FilterChip] row: CUP / USD / MLC)
 * - Balance ([OutlinedTextField], optional, decimal, default 0.00). The user-facing
 *   label is "Balance" because the stored value IS the displayed balance — transfers
 *   no longer auto-update it (Option B).
 *
 * Save is disabled when [isSaving] is true, name is blank/whitespace-only,
 * or name exceeds 100 characters.
 *
 * @param initialName Pre-populated name (wallet name or "Billetera" default).
 * @param initialCurrency Default [Currency] selection.
 * @param balance Pre-populated balance string (decimal format).
 * @param isSaving Whether a save operation is in-flight (disables save button).
 * @param onDismiss Called when the dialog is dismissed.
 * @param onSave Called with (name, currency, balanceMinor) when the user confirms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletEditDialog(
    initialName: String = "Billetera",
    initialCurrency: Currency = Currency.CUP,
    balance: String = "",
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, currency: Currency, balanceMinor: Long) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var currency by remember { mutableStateOf(initialCurrency) }
    var balanceInput by remember { mutableStateOf(balance) }

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
        title = { Text("Editar billetera") },
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

                // Balance
                OutlinedTextField(
                    value = balanceInput,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) balanceInput = it },
                    label = { Text("Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minorUnits = balanceInput.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
                    onSave(name.trim(), currency, minorUnits)
                },
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
