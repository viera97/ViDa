package com.vida.feature.ratemanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vida.domain.model.Currency
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

/**
 * Form dialog for creating or editing a currency rate.
 *
 * Fields:
 * - From currency ([FilterChip] row: CUP / USD / MLC)
 * - To currency ([FilterChip] row: CUP / USD / MLC)
 * - Rate ([OutlinedTextField], BigDecimal, required, > 0)
 * - Provider ([OutlinedTextField], optional, defaults to "Manual")
 * - Date ([DatePickerDialog], default = today for new rates)
 *
 * Save is disabled when [isSaving] is true, rate is invalid (blank,
 * not a valid BigDecimal, zero, or negative), or from == to.
 *
 * @param initialFrom Default "from" currency (USD).
 * @param initialTo Default "to" currency (CUP).
 * @param initialRate Pre-populated rate string (empty for add).
 * @param initialProvider Pre-populated provider string ("Manual" for add).
 * @param initialDate Pre-populated date (Instant.now() for add).
 * @param isEdit Whether this is an edit form (affects title).
 * @param isSaving Whether a save operation is in-flight (disables save button).
 * @param onDismiss Called when the dialog is dismissed.
 * @param onSave Called with (from, to, rate, date, provider) when the user confirms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateFormDialog(
    initialFrom: Currency = Currency.USD,
    initialTo: Currency = Currency.CUP,
    initialRate: String = "",
    initialProvider: String = "Manual",
    initialDate: Instant = Instant.now(),
    isEdit: Boolean = false,
    isSaving: Boolean = false,
    duplicateError: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (from: Currency, to: Currency, rate: BigDecimal, date: Instant, provider: String) -> Unit,
) {
    var fromCurrency by remember { mutableStateOf(initialFrom) }
    var toCurrency by remember { mutableStateOf(initialTo) }
    var rateText by remember { mutableStateOf(initialRate) }
    var providerText by remember { mutableStateOf(initialProvider) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    // ── Rate Big Decimal validation ───────────────────────────────────────────
    val parsedRate: BigDecimal? = remember(rateText) {
        if (rateText.isBlank()) null
        else runCatching { BigDecimal(rateText) }.getOrNull()
    }

    val rateError: String? = when {
        rateText.isBlank() -> null
        parsedRate == null -> "Número inválido"
        parsedRate.signum() <= 0 -> "La tasa debe ser positiva"
        else -> null
    }

    val fromEqualsTo = fromCurrency == toCurrency

    val isSaveEnabled = parsedRate != null &&
        parsedRate.signum() > 0 &&
        !fromEqualsTo &&
        !isSaving

    // ── Date formatting ───────────────────────────────────────────────────────
    val dateText = remember(selectedDate) {
        val zone = ZoneId.systemDefault()
        java.time.format.DateTimeFormatter
            .ofPattern("dd/MM/yyyy")
            .format(selectedDate.atZone(zone))
    }

    // ── Dialog ────────────────────────────────────────────────────────────────
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (isEdit) "Editar tasa" else "Agregar tasa") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // From currency — FilterChip row
                Text(
                    text = "De",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Currency.entries.forEach { curr ->
                        FilterChip(
                            selected = fromCurrency == curr,
                            onClick = { fromCurrency = curr },
                            label = { Text(curr.code) },
                        )
                    }
                }

                // To currency — FilterChip row
                Text(
                    text = "A",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Currency.entries.forEach { curr ->
                        FilterChip(
                            selected = toCurrency == curr,
                            onClick = { toCurrency = curr },
                            label = { Text(curr.code) },
                        )
                    }
                }
                if (fromEqualsTo) {
                    Text(
                        text = "Las monedas deben ser diferentes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (duplicateError) {
                    Text(
                        text = "Esta tasa ya existe. Ábrela y edítala.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                // Rate
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Tasa") },
                    isError = rateError != null,
                    supportingText = rateError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Provider
                OutlinedTextField(
                    value = providerText,
                    onValueChange = { providerText = it },
                    label = { Text("Proveedor") },
                    placeholder = { Text("Manual") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Date picker
                Text(
                    text = "Fecha",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(dateText)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    parsedRate?.let { rate ->
                        val finalProvider = providerText.ifBlank { "Manual" }
                        onSave(fromCurrency, toCurrency, rate, selectedDate, finalProvider)
                    }
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

    // ── Date picker dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochMilli(),
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
