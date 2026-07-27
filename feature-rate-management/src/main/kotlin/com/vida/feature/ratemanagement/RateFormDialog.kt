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
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Form dialog for creating or editing a currency rate.
 *
 * Fields:
 * - From currency ([ExposedDropdownMenuBox], from user's currency list)
 * - To currency ([ExposedDropdownMenuBox], from user's currency list)
 * - Rate ([OutlinedTextField], BigDecimal, required, > 0)
 * - Provider ([OutlinedTextField], optional, defaults to "Manual")
 * - Date ([DatePickerDialog], default = today for new rates)
 *
 * Save is disabled when [isSaving] is true, rate is invalid (blank,
 * not a valid BigDecimal, zero, or negative), or from == to.
 *
 * @param initialFrom Default "from" currency code ("USD").
 * @param initialTo Default "to" currency code ("CUP").
 * @param initialRate Pre-populated rate string (empty for add).
 * @param initialProvider Pre-populated provider string ("Manual" for add).
 * @param initialDate Pre-populated date (Instant.now() for add).
 * @param isEdit Whether this is an edit form (affects title).
 * @param isSaving Whether a save operation is in-flight (disables save button).
 * @param availableCurrencies Currency codes available for selection.
 * @param onDismiss Called when the dialog is dismissed.
 * @param onSave Called with (fromCode, toCode, rate, date, provider) when the user confirms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateFormDialog(
    initialFrom: String = "USD",
    initialTo: String = "CUP",
    initialRate: String = "",
    initialProvider: String = "Manual",
    initialDate: Instant = Instant.now(),
    isEdit: Boolean = false,
    isSaving: Boolean = false,
    duplicateError: Boolean = false,
    availableCurrencies: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (fromCode: String, toCode: String, rate: BigDecimal, date: Instant, provider: String) -> Unit,
) {
    val defaultFrom = if (availableCurrencies.contains(initialFrom)) initialFrom
        else availableCurrencies.firstOrNull() ?: "USD"

    val defaultTo = if (availableCurrencies.contains(initialTo) && defaultFrom != initialTo) initialTo
        else availableCurrencies.firstOrNull { it != defaultFrom } ?: "CUP"

    var fromCurrency by remember { mutableStateOf(defaultFrom) }
    var toCurrency by remember { mutableStateOf(defaultTo) }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
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
                // From currency — dropdown
                CurrencySelector(
                    selectedCurrencyCode = fromCurrency,
                    label = "De",
                    onShowSheet = { fromExpanded = true },
                )
                if (fromExpanded) {
                    CurrencyPickerSheet(
                        availableCurrencies = availableCurrencies,
                        selectedCurrencyCode = fromCurrency,
                        onDismiss = { fromExpanded = false },
                        onCurrencySelected = { code ->
                            fromCurrency = code
                            // If "To" equals the new "From", auto-switch "To"
                            if (toCurrency == code) {
                                toCurrency = availableCurrencies.firstOrNull { it != code } ?: "CUP"
                            }
                            fromExpanded = false
                        },
                    )
                }

                // To currency — dropdown
                CurrencySelector(
                    selectedCurrencyCode = toCurrency,
                    label = "A",
                    onShowSheet = { toExpanded = true },
                )
                if (toExpanded) {
                    CurrencyPickerSheet(
                        availableCurrencies = availableCurrencies,
                        selectedCurrencyCode = toCurrency,
                        onDismiss = { toExpanded = false },
                        onCurrencySelected = { code ->
                            toCurrency = code
                            toExpanded = false
                        },
                    )
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
                            val localDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            selectedDate = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
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
