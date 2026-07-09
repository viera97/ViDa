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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import java.time.LocalDate

/**
 * Form dialog for creating or editing a card.
 *
 * Fields:
 * - Bank name ([OutlinedTextField], required)
 * - First 6 digits ([OutlinedTextField], 6 digits, numeric keyboard)
 * - Last 4 digits ([OutlinedTextField], 4 digits, numeric keyboard)
 * - Card type ([FilterChip] row: Débito / Crédito / Prepago)
 * - Currency ([FilterChip] row: CUP / USD / MLC)
 * - Expiry date (year/month picker dialog → formatted as MM/YY; day-of-month is
 *   ignored since credit-card expiry only specifies month and year)
 * - Note ([OutlinedTextField], optional, max 200 chars)
 * - Balance ([OutlinedTextField], optional, decimal, default 0.00). The user-facing
 *   label is "Balance" because the stored value IS the displayed balance — transfers
 *   no longer auto-update it (Option B).
 *
 * Save is disabled when [isSaving] is true, bank is blank or whitespace-only,
 * first6 is not exactly 6 digits, or last4 is not exactly 4 digits.
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
    balanceStr: String = "",
    isEdit: Boolean = false,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (bank: String, first6: String, last4: String, type: CardType, currency: Currency, expiry: LocalDate, note: String?, balanceMinor: Long) -> Unit,
) {
    val banks = listOf("Bandec", "BPA", "Metropolitano")
    val allBankOptions = banks + "Otros"
    val isCustom = initialBank.isNotBlank() && initialBank !in banks
    var selectedBank by remember { mutableStateOf(
        if (isCustom) "Otros"
        else initialBank.ifEmpty { "Bandec" }
    ) }
    var customBank by remember { mutableStateOf(if (isCustom) initialBank else "") }
    val effectiveBank = if (selectedBank == "Otros") customBank else selectedBank
    var cardNumber by remember { mutableStateOf(initialFirst6 + initialLast4) }
    var type by remember { mutableStateOf(initialType) }
    var currency by remember { mutableStateOf(initialCurrency) }
    var expiry by remember { mutableStateOf(initialExpiry) }
    var note by remember { mutableStateOf(initialNote) }
    var balanceInput by remember { mutableStateOf(balanceStr) }
    var bankDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // ── Validation ───────────────────────────────────────────────────────────

    val isBankBlank = effectiveBank.isBlank()
    val cardNumberValid = cardNumber.all { it.isDigit() }
    val cardNumberError: String? = when {
        cardNumber.isNotEmpty() && !cardNumberValid -> "Solo dígitos"
        cardNumber.isNotEmpty() && cardNumber.length < 10 -> "Mínimo 10 dígitos"
        else -> null
    }

    val isSaveEnabled = !isBankBlank &&
        effectiveBank.length <= 100 &&
        cardNumber.length >= 10 && cardNumberValid &&
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
                    value = cardNumber,
                    onValueChange = { if (it.length <= 19 && it.all { c -> c.isDigit() }) cardNumber = it },
                    label = { Text("Número de tarjeta") },
                    isError = cardNumberError != null,
                    supportingText = cardNumberError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = bankDropdownExpanded,
                    onExpandedChange = { bankDropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedBank.ifEmpty { "Otros" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Banco") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = bankDropdownExpanded,
                        onDismissRequest = { bankDropdownExpanded = false },
                    ) {
                        allBankOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedBank = option
                                    bankDropdownExpanded = false
                                },
                            )
                        }
                    }
                }
                if (selectedBank == "Otros") {
                    OutlinedTextField(
                        value = customBank,
                        onValueChange = { if (it.length <= 100) customBank = it },
                        label = { Text("Nombre de tarjeta") },
                        isError = effectiveBank.isBlank() && customBank.isNotEmpty(),
                        supportingText = if (effectiveBank.isBlank() && customBank.isNotEmpty()) {
                            { Text("El nombre es obligatorio") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

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
                    val extractedFirst6 = cardNumber.take(6)
                    val extractedLast4 = cardNumber.takeLast(4)
                    onSave(
                        effectiveBank.trim(),
                        extractedFirst6,
                        extractedLast4,
                        type,
                        currency,
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