package com.vida.feature.cardmanagement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.vida.domain.model.CardType
import java.time.LocalDate

/** Format raw digits into chunks of 4 separated by dashes: "9723-2948-1234-5678". */
private fun formatCardNumber(raw: String): String =
    raw.chunked(4).joinToString("-")

/** Map a cursor position in raw digit-only text to the formatted text position
 *  (accounting for dashes inserted every 4 digits). */
private fun rawToFormattedCursor(rawCursor: Int): Int =
    if (rawCursor <= 0) 0 else rawCursor + (rawCursor - 1) / 4

/** Map a cursor position in formatted text (with dashes) back to the
 *  raw digit-only position by skipping dash characters. */
private fun formattedToRawCursor(formatted: String, formattedCursor: Int): Int {
    var raw = 0
    for (i in 0 until formattedCursor.coerceAtMost(formatted.length)) {
        if (formatted[i] != '-') raw++
    }
    return raw
}

/**
 * Form dialog for creating or editing a card.
 *
 * Fields:
 * - Bank name ([OutlinedTextField], required)
 * - First 6 digits ([OutlinedTextField], 6 digits, numeric keyboard)
 * - Last 4 digits ([OutlinedTextField], 4 digits, numeric keyboard)
 * - Card type ([FilterChip] row: Débito / Crédito / Prepago)
 * - Currency ([ExposedDropdownMenuBox], required, from user's currency list)
 * - Expiry date (year/month picker dialog → formatted as MM/YY; day-of-month is
 *   ignored since credit-card expiry only specifies month and year)
 * - Note ([OutlinedTextField], optional, max 200 chars)
 * - Balance ([OutlinedTextField], optional, decimal, default 0.00). The user-facing
 *   label is "Balance" because the stored value IS the displayed balance — transfers
 *   no longer auto-update it (Option B).
 *
 * Save is disabled when [isSaving] is true, bank is blank or whitespace-only,
 * first6 is not exactly 6 digits, or last4 is not exactly 4 digits, or no currency
 * is selected, or the saved currency code is orphaned (no longer in the user's list).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardFormDialog(
    initialBank: String = "",
    initialFirst6: String = "",
    initialLast4: String = "",
    initialType: CardType = CardType.DEBIT,
    initialCurrency: String = "CUP",
    initialExpiry: LocalDate? = null,
    initialNote: String = "",
    balanceStr: String = "",
    isEdit: Boolean = false,
    isSaving: Boolean = false,
    availableBanks: List<String> = emptyList(),
    availableCurrencies: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (bank: String, first6: String, last4: String, type: CardType, currency: String, expiry: LocalDate, note: String?, balanceMinor: Long) -> Unit,
) {
    var selectedBank by remember { mutableStateOf(
        initialBank.ifEmpty { availableBanks.firstOrNull() ?: "" }
    ) }
    var cardNumberState by remember { mutableStateOf(TextFieldValue(initialFirst6 + initialLast4)) }
    var type by remember { mutableStateOf(initialType) }
    var selectedCurrency by remember { mutableStateOf(
        if (initialCurrency.isNotEmpty()) {
            initialCurrency
        } else {
            availableCurrencies.firstOrNull() ?: "CUP"
        }
    ) }
    var expiry by remember { mutableStateOf(initialExpiry) }
    var note by remember { mutableStateOf(initialNote) }
    var balanceInput by remember { mutableStateOf(balanceStr) }
    var bankDropdownExpanded by remember { mutableStateOf(false) }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // ── Validation ───────────────────────────────────────────────────────────

    val isBankBlank = selectedBank.isBlank()
    val cardNumberValid = cardNumberState.text.all { it.isDigit() }
    val cardNumberError: String? = when {
        cardNumberState.text.isNotEmpty() && !cardNumberValid -> "Solo dígitos"
        cardNumberState.text.isNotEmpty() && cardNumberState.text.length < 10 -> "Mínimo 10 dígitos"
        cardNumberState.text.length > 16 -> "Máximo 16 dígitos"
        else -> null
    }

    val isOrphanCurrency = isEdit && initialCurrency.isNotBlank() && initialCurrency !in availableCurrencies

    val isSaveEnabled = !isBankBlank &&
        selectedBank.length <= 100 &&
        cardNumberState.text.length in 10..16 && cardNumberValid &&
        selectedCurrency.isNotBlank() &&
        !isOrphanCurrency &&
        !isSaving

    // ── Year/month picker dialog ────────────────────────────────────────────
    if (showDatePicker) {
        ExpiryYearMonthPickerDialog(
            initial = initialExpiry,
            onConfirm = { picked ->
                expiry = picked
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
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
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 200) note = it },
                    label = { Text("Nombre de tarjeta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = formatCardNumber(cardNumberState.text).let { formatted ->
                        val cursor = rawToFormattedCursor(cardNumberState.selection.start)
                            .coerceIn(0, formatted.length)
                        TextFieldValue(formatted, TextRange(cursor))
                    },
                    onValueChange = { newValue ->
                        val digits = newValue.text.filter { it.isDigit() }
                        if (digits.length <= 16) {
                            val rawCursor = formattedToRawCursor(newValue.text, newValue.selection.start)
                                .coerceIn(0, digits.length)
                            cardNumberState = TextFieldValue(digits, TextRange(rawCursor))
                        }
                    },
                    label = { Text("Número de tarjeta") },
                    isError = cardNumberError != null,
                    supportingText = cardNumberError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                // Bank — trigger field
                CardBankSelector(
                    selectedBank = selectedBank,
                    onShowSheet = { bankDropdownExpanded = true },
                )

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

                // Currency — trigger field
                CardCurrencySelector(
                    selectedCurrencyCode = selectedCurrency,
                    isError = isOrphanCurrency,
                    errorMessage = if (isOrphanCurrency) "La moneda asignada ya no existe. Seleccione otra." else null,
                    onShowSheet = { currencyDropdownExpanded = true },
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                ) {
                    OutlinedTextField(
                        value = expiry?.let { e ->
                            val month = e.monthValue.toString().padStart(2, '0')
                            val year = e.year.toString().takeLast(2)
                            "$month/$year"
                        } ?: "",
                        onValueChange = {},
                        label = { Text("Vencimiento (MM/YY)") },
                        readOnly = true,
                        enabled = false,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }

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
                    val safeExpiry = expiry ?: LocalDate.now().plusMonths(1)
                    val safeNote = note.trim().ifBlank { null }
                    val minorUnits = balanceInput.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
                    val extractedFirst6 = cardNumberState.text.take(6)
                    val extractedLast4 = cardNumberState.text.takeLast(4)
                    onSave(
                        selectedBank.trim(),
                        extractedFirst6,
                        extractedLast4,
                        type,
                        selectedCurrency,
                        safeExpiry,
                        safeNote,
                        minorUnits,
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

    // Bank bottom sheet — rendered after the AlertDialog so it overlays correctly.
    if (bankDropdownExpanded) {
        CardBankPickerSheet(
            availableBanks = availableBanks,
            selectedBank = selectedBank,
            onDismiss = { bankDropdownExpanded = false },
            onBankSelected = { bank ->
                selectedBank = bank
                bankDropdownExpanded = false
            },
        )
    }

    // Currency bottom sheet — rendered after the AlertDialog so it overlays correctly.
    if (currencyDropdownExpanded) {
        CardCurrencyPickerSheet(
            availableCurrencies = availableCurrencies,
            selectedCurrencyCode = selectedCurrency,
            onDismiss = { currencyDropdownExpanded = false },
            onCurrencySelected = { code ->
                selectedCurrency = code
                currencyDropdownExpanded = false
            },
        )
    }
}

@Composable
private fun ExpiryYearMonthPickerDialog(
    initial: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = LocalDate.now()
    var selectedYear by remember { mutableIntStateOf(initial?.year ?: today.year) }
    var selectedMonth by remember { mutableIntStateOf(initial?.monthValue ?: today.monthValue) }

    val minYear = today.year - 5
    val maxYear = today.year + 20

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vencimiento") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TextButton(
                        onClick = { if (selectedYear > minYear) selectedYear-- },
                        enabled = selectedYear > minYear,
                    ) {
                        Text("<")
                    }
                    Text(text = selectedYear.toString(), style = MaterialTheme.typography.headlineMedium)
                    TextButton(
                        onClick = { if (selectedYear < maxYear) selectedYear++ },
                        enabled = selectedYear < maxYear,
                    ) {
                        Text(">")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                val monthNames = listOf(
                    "Ene", "Feb", "Mar", "Abr",
                    "May", "Jun", "Jul", "Ago",
                    "Sep", "Oct", "Nov", "Dic",
                )
                for (r in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (c in 0..3) {
                            val m = r * 4 + c + 1
                            FilterChip(
                                selected = selectedMonth == m,
                                onClick = { selectedMonth = m },
                                label = { Text(monthNames[m - 1]) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalDate.of(selectedYear, selectedMonth, 1)) }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}