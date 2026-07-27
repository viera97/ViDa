package com.vida.feature.recurringexpensemanagement

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vida.domain.model.Card
import com.vida.domain.model.Currency
import com.vida.domain.model.Frequency
import com.vida.domain.model.Money
import com.vida.domain.model.RecurringIncome
import com.vida.domain.model.SourceType
import com.vida.domain.model.Stash
import com.vida.domain.model.Wallet
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.runCatching

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Form dialog for creating or editing a recurring income template.
 *
 * Fields (9 in a scrollable Column):
 * - amount: [OutlinedTextField] with real-time BigDecimal validation
 * - currency: [ExposedDropdownMenuBox] (dynamic from user's currency list)
 * - sourceType + sourceId: combined picker via bottom sheet (wallets + cards + stashes)
 * - description: [OutlinedTextField]
 * - frequency: [ExposedDropdownMenuBox] (Diario / Semanal / Mensual / Anual)
 * - startDate: [DatePickerDialog] (default today)
 * - endDate: [DatePickerDialog] (optional)
 * - isActive: [Switch] toggle (default ON)
 *
 * Unlike [RecurringFormDialog], this dialog has NO category field — incomes are
 * not categorized. Also unlike expenses, the source picker includes stashes.
 *
 * Save is disabled when [isSaving] is true or validation fails.
 *
 * @param initialAmount Pre-populated amount string (empty for add).
 * @param initialCurrencyCode Default currency code string.
 * @param initialSourceType Default [SourceType] selection.
 * @param initialSourceId Pre-selected source id (null = none).
 * @param initialDescription Pre-populated description (empty for add).
 * @param initialFrequency Pre-selected frequency (default MONTHLY).
 * @param initialStartDate Start date for the template.
 * @param initialEndDate Optional end date.
 * @param initialIsActive Whether the template starts active.
 * @param isEdit Whether this is an edit form (affects title).
 * @param isSaving Whether a save operation is in-flight (disables save button).
 * @param wallets Available wallets for the source dropdown.
 * @param cards Available cards for the source dropdown.
 * @param stashes Available stashes for the source dropdown.
 * @param availableCurrencies Currency codes for the currency dropdown.
 * @param onDismiss Called when the dialog is dismissed.
 * @param onSave Called with the constructed [RecurringIncome] when the user confirms.
 *               (id=0, lastGeneratedDate=null — caller merges for edits.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringIncomeFormDialog(
    initialAmount: String = "",
    initialCurrencyCode: String = "CUP",
    initialSourceType: SourceType = SourceType.WALLET,
    initialSourceId: Long? = null,
    initialDescription: String = "",
    initialFrequency: Frequency = Frequency.MONTHLY,
    initialStartDate: LocalDate = LocalDate.now(),
    initialEndDate: LocalDate? = null,
    initialIsActive: Boolean = true,
    isEdit: Boolean = false,
    isSaving: Boolean = false,
    wallets: List<Wallet> = emptyList(),
    cards: List<Card> = emptyList(),
    stashes: List<Stash> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (RecurringIncome) -> Unit,
) {
    // ── Local form state ──────────────────────────────────────────────────────

    var amount by remember { mutableStateOf(initialAmount) }
    var selectedCurrencyCode by remember {
        mutableStateOf(initialCurrencyCode.ifBlank { "CUP" })
    }
    var sourceType by remember { mutableStateOf(initialSourceType) }
    var selectedSourceId by remember { mutableStateOf(initialSourceId) }
    var description by remember { mutableStateOf(initialDescription) }
    var frequency by remember { mutableStateOf(initialFrequency) }
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var isActive by remember { mutableStateOf(initialIsActive) }

    // ── Dropdown toggles ──────────────────────────────────────────────────────

    var sourceExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // ── Validation ────────────────────────────────────────────────────────────

    val amountParseResult = remember(amount) {
        runCatching { BigDecimal(amount) }
    }
    val amountError: String? = when {
        amount.isEmpty() -> null
        amountParseResult.isFailure -> "Ingresá un número válido"
        amountParseResult.getOrNull()?.signum()?.let { it <= 0 } == true -> "El monto debe ser positivo"
        else -> null
    }
    val isAmountValid = amountParseResult.getOrNull()?.let { it.signum() > 0 } == true

    val descriptionError: String? = when {
        description.isNotEmpty() && description.isBlank() -> "La descripción es obligatoria"
        description.length > 200 -> "Máximo 200 caracteres"
        else -> null
    }
    val isDescriptionValid = description.isNotBlank() && description.length <= 200

    val isSourceIdValid = selectedSourceId != null

    val endDateError: String? = endDate?.let { ed ->
        if (ed.isBefore(startDate)) "La fecha fin no puede ser anterior al inicio" else null
    }

    // When a source is selected, lock the currency to the source's currency
    // (mirrors `feature-expense`'s amount section behavior).
    val availableSources = remember(wallets, cards, stashes) {
        wallets.toWalletSourceItems() + cards.toCardSourceItems() + stashes.toStashSourceItems()
    }
    val selectedSource = remember(availableSources, sourceType, selectedSourceId) {
        availableSources.firstOrNull {
            it.id == selectedSourceId && it.type == sourceType
        }
    }
    val isCurrencyLocked = selectedSource != null
    LaunchedEffect(selectedSource?.currency) {
        selectedSource?.currency?.let { selectedCurrencyCode = it }
    }

    val isSaveEnabled = isAmountValid
        && isDescriptionValid
        && isSourceIdValid
        && endDateError == null
        && !isSaving

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun buildIncome(): RecurringIncome {
        val parsedAmount = amountParseResult.getOrThrow()
        val money = Money(parsedAmount, Currency.fromCode(selectedCurrencyCode))
        return RecurringIncome(
            id = 0L,
            amount = money,
            currency = selectedCurrencyCode,
            sourceType = sourceType,
            sourceId = selectedSourceId,
            description = description.trim(),
            frequency = frequency,
            startDate = startDate,
            endDate = endDate,
            lastGeneratedDate = null,
            isActive = isActive,
        )
    }

    // ── Date picker dialogs ──────────────────────────────────────────────────

    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate.toEpochMillis(),
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = endDate?.toEpochMillis(),
    )

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let { millis ->
                        startDate = millis.toLocalDate()
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { millis ->
                        endDate = millis.toLocalDate()
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    // ── Dialog ────────────────────────────────────────────────────────────────

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (isEdit) "Editar Ingreso" else "Agregar Ingreso") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ── Amount ──────────────────────────────────────────────
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto") },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Currency is always locked to the selected source's currency.
                // No standalone dropdown needed — hidden from UI.

                // ── Source (wallet, card, or stash) ─────────────────────
                RecurringSourceSelector(
                    selectedSource = selectedSource,
                    onShowSheet = { sourceExpanded = true },
                    error = null,
                )
                if (sourceExpanded) {
                    RecurringSourceSheet(
                        sources = availableSources,
                        selectedSource = selectedSource,
                        onDismiss = { sourceExpanded = false },
                        onSourceSelected = { picked ->
                            sourceType = picked.type
                            selectedSourceId = picked.id
                            sourceExpanded = false
                        },
                    )
                }

                // ── Description ─────────────────────────────────────────
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    isError = descriptionError != null,
                    supportingText = descriptionError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Frequency ───────────────────────────────────────────
                RecurringFrequencySelector(
                    selectedFrequency = frequency,
                    onShowSheet = { frequencyExpanded = true },
                )
                if (frequencyExpanded) {
                    RecurringFrequencySheet(
                        selectedFrequency = frequency,
                        onDismiss = { frequencyExpanded = false },
                        onFrequencySelected = { selected ->
                            frequency = selected
                            frequencyExpanded = false
                        },
                    )
                }

                // ── Start date ──────────────────────────────────────────
                Text("Fecha de inicio", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(startDate.format(dateFormatter))
                }

                // ── End date (optional) ─────────────────────────────────
                Text("Fecha fin (opcional)", style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(endDate?.format(dateFormatter) ?: "Sin fecha límite")
                    }
                    if (endDate != null) {
                        TextButton(onClick = { endDate = null }) {
                            Text("Quitar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (endDateError != null) {
                    Text(
                        text = endDateError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // ── Active switch ───────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Activa")
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                    )
                }

                // Bottom spacer for scroll padding
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    runCatching { buildIncome() }
                        .onSuccess { income -> onSave(income) }
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

// ── Date conversion helpers ───────────────────────────────────────────────────

/** Converts [LocalDate] to epoch milliseconds in UTC. */
private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Converts epoch milliseconds to [LocalDate] in UTC. */
private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

// ── Display helpers ───────────────────────────────────────────────────────────

// Frequency.toSpanishLabel() is defined in RecurringFrequencySelector.kt
