package com.vida.feature.cardmanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Form dialog for creating or editing a card.
 *
 * Fields:
 * - Bank name ([OutlinedTextField], required)
 * - First 6 digits ([OutlinedTextField], 6 digits, numeric keyboard)
 * - Last 4 digits ([OutlinedTextField], 4 digits, numeric keyboard)
 * - Card type ([FilterChip] row: Débito / Crédito / Prepago)
 * - Currency ([FilterChip] row: CUP / USD / MLC)
 * - Expiry date ([DatePickerDialog] → formatted as MM/YY)
 * - Note ([OutlinedTextField], optional, max 200 chars)
 *
 * Save is disabled when [isSaving] is true, bank is blank or whitespace-only,
 * first6 is not exactly 6 digits, or last4 is not exactly 4 digits.
 *
 * @param initialBank Pre-populated bank name (empty for add, existing value for edit).
 * @param initialFirst6 Pre-populated first 6 digits.
 * @param initialLast4 Pre-populated last 4 digits.
 * @param initialType Default [CardType] selection (DEBIT).
 * @param initialCurrency Default [Currency] selection (CUP).
 * @param initialExpiry Pre-populated expiry date (null for add, existing for edit).
 * @param initialNote Pre-populated note.
 * @param isEdit Whether this is an edit form (affects title and button text).
 * @param isSaving Whether a save operation is in-flight (disables save button).
 * @param onDismiss Called when the dialog is dismissed.
 * @param onSave Called with (bank, first6, last4, type, currency, expiry, note)
 *   when the user confirms. Note is null when blank.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardFormDialog(
    initialBank: String = "",
    initialFirst6: String = "",
    initialLast4: String = "",
    initialType: CardType = CardType.DEBIT,
    initialCurrency: Currency = Currency.CUP,
    initialExpiry: LocalDate? = null,
    initialNote: String = "",
    isEdit: Boolean = false,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (bank: String, first6: String, last4: String, type: CardType, currency: Currency, expiry: LocalDate, note: String?) -> Unit,
) {
    var bank by remember { mutableStateOf(initialBank) }
    var first6 by remember { mutableStateOf(initialFirst6) }
    var last4 by remember { mutableStateOf(initialLast4) }
    var type by remember { mutableStateOf(initialType) }
    var currency by remember { mutableStateOf(initialCurrency) }
    var expiry by remember { mutableStateOf(initialExpiry) }
    var note by remember { mutableStateOf(initialNote) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialExpiry
            ?.atStartOfDay(ZoneId.of("UTC"))
            ?.toInstant()
            ?.toEpochMilli(),
    )

    // ── Validation ───────────────────────────────────────────────────────────

    /** True when first6 is empty OR exactly 6 digits. */
    val first6Valid = first6.isEmpty() || (first6.length == 6 && first6.all { it.isDigit() })

    /** True when last4 is empty OR exactly 4 digits. */
    val last4Valid = last4.isEmpty() || (last4.length == 4 && last4.all { it.isDigit() })

    val isBankBlank = bank.isBlank()
    val first6Error: String? = if (!first6Valid && first6.isNotEmpty()) "Deben ser 6 dígitos" else null
    val last4Error: String? = if (!last4Valid && last4.isNotEmpty()) "Deben ser 4 dígitos" else null
    val noteError: String? = if (note.length > 200) "Máximo 200 caracteres" else null

    val isSaveEnabled = !isBankBlank &&
        bank.length <= 100 &&
        first6.length == 6 && first6Valid &&
        last4.length == 4 && last4Valid &&
        !isSaving

    // ── Date picker dialog ───────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = Instant.ofEpochMilli(millis)
                            val localDate = instant.atZone(ZoneId.of("UTC")).toLocalDate()
                            expiry = localDate
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

    // ── Main dialog ──────────────────────────────────────────────────────────
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (isEdit) "Editar tarjeta" else "Agregar tarjeta") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Bank name
                OutlinedTextField(
                    value = bank,
                    onValueChange = { if (it.length <= 100) bank = it },
                    label = { Text("Banco") },
                    isError = isBankBlank && bank.isNotEmpty(),
                    supportingText = if (isBankBlank && bank.isNotEmpty()) {
                        { Text("El banco es obligatorio") }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // First 6 digits
                OutlinedTextField(
                    value = first6,
                    onValueChange = { if (it.length <= 6) first6 = it },
                    label = { Text("Primeros 6 dígitos") },
                    isError = first6Error != null,
                    supportingText = first6Error?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Last 4 digits
                OutlinedTextField(
                    value = last4,
                    onValueChange = { if (it.length <= 4) last4 = it },
                    label = { Text("Últimos 4 dígitos") },
                    isError = last4Error != null,
                    supportingText = last4Error?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Card type — FilterChip row
                Text("Tipo")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardType.entries.forEach { cardType ->
                        FilterChip(
                            selected = type == cardType,
                            onClick = { type = cardType },
                            label = {
                                Text(
                                    when (cardType) {
                                        CardType.DEBIT -> "Débito"
                                        CardType.CREDIT -> "Crédito"
                                        CardType.PREPAID -> "Prepago"
                                    },
                                )
                            },
                        )
                    }
                }

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

                // Expiry date — text field that opens DatePickerDialog
                OutlinedTextField(
                    value = expiry?.let { e ->
                        val month = e.monthValue.toString().padStart(2, '0')
                        val year = e.year.toString().takeLast(2)
                        "$month/$year"
                    } ?: "",
                    onValueChange = {},
                    label = { Text("Vencimiento (MM/YY)") },
                    readOnly = true,
                    enabled = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                TextButton(onClick = { showDatePicker = true }) {
                    Text(
                        if (expiry != null) "Cambiar fecha" else "Seleccionar fecha",
                    )
                }

                // Note (optional)
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 200) note = it },
                    label = { Text("Nota (opcional)") },
                    isError = noteError != null,
                    supportingText = noteError?.let { { Text(it) } },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val safeExpiry = expiry ?: LocalDate.now().plusMonths(1)
                    val safeNote = note.trim().ifBlank { null }
                    onSave(
                        bank.trim(),
                        first6.trim(),
                        last4.trim(),
                        type,
                        currency,
                        safeExpiry,
                        safeNote,
                    )
                },
                enabled = isSaveEnabled,
            ) {
                Text(if (isEdit) "Guardar" else "Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar")
            }
        },
    )
}
