package com.vida.feature.recurringexpensemanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.foundation.text.KeyboardOptions
import com.vida.domain.model.Card
import com.vida.domain.model.Category
import com.vida.domain.model.Currency
import com.vida.domain.model.Frequency
import com.vida.domain.model.Money
import com.vida.domain.model.RecurringExpense
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
 * Form dialog for creating or editing a recurring expense template.
 *
 * Fields (10 in a scrollable Column):
 * - amount: [OutlinedTextField] with real-time BigDecimal validation
 * - currency: [FilterChip] row (CUP / USD / MLC)
 * - categoryId: dropdown via [Category] list
 * - sourceType + sourceId: combined picker via bottom sheet (wallets + cards only;
 *   stashes are intentionally not allowed for recurring expenses)
 * - description: [OutlinedTextField]
 * - frequency: [FilterChip] row (DAILY / WEEKLY / MONTHLY / YEARLY)
 * - startDate: [DatePickerDialog] (default today)
 * - endDate: [DatePickerDialog] (optional)
 * - isActive: [Switch] toggle (default ON)
 *
 * Save is disabled when [isSaving] is true or validation fails.
 *
 * @param initialAmount Pre-populated amount string (empty for add).
 * @param initialCurrency Default [Currency] selection.
 * @param initialCategoryId Pre-selected category id (null = none).
 * @param initialSourceType Default [SourceType] selection.
 * @param initialSourceId Pre-selected source id (null = none).
 * @param initialDescription Pre-populated description (empty for add).
 * @param initialFrequency Pre-selected frequency (null = none).
 * @param initialStartDate Start date for the template.
 * @param initialEndDate Optional end date.
 * @param initialIsActive Whether the template starts active.
 * @param isEdit Whether this is an edit form (affects title).
 * @param isSaving Whether a save operation is in-flight (disables save button).
 * @param categories Available categories for the dropdown.
 * @param cards Available cards for the source dropdown.
 * @param stashes Available stashes for the source dropdown.
 * @param onDismiss Called when the dialog is dismissed.
 * @param onSave Called with the constructed [RecurringExpense] when the user confirms.
 *               (id=0, lastGeneratedDate=null — caller merges for edits.)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringFormDialog(
    initialAmount: String = "",
    initialCurrency: Currency = Currency.CUP,
    initialCategoryId: Long? = null,
    initialSourceType: SourceType = SourceType.WALLET,
    initialSourceId: Long? = null,
    initialDescription: String = "",
    initialFrequency: Frequency? = null,
    initialStartDate: LocalDate = LocalDate.now(),
    initialEndDate: LocalDate? = null,
    initialIsActive: Boolean = true,
    isEdit: Boolean = false,
    isSaving: Boolean = false,
    categories: List<Category> = emptyList(),
    wallets: List<Wallet> = emptyList(),
    cards: List<Card> = emptyList(),
    stashes: List<Stash> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (RecurringExpense) -> Unit,
) {
    // ── Local form state ──────────────────────────────────────────────────────

    var amount by remember { mutableStateOf(initialAmount) }
    var currency by remember { mutableStateOf(initialCurrency) }
    var selectedCategoryId by remember { mutableStateOf(initialCategoryId) }
    var sourceType by remember { mutableStateOf(initialSourceType) }
    var selectedSourceId by remember { mutableStateOf(initialSourceId) }
    var description by remember { mutableStateOf(initialDescription) }
    var frequency by remember { mutableStateOf(initialFrequency) }
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var isActive by remember { mutableStateOf(initialIsActive) }

    // ── Dropdown toggles ──────────────────────────────────────────────────────

    var categoryExpanded by remember { mutableStateOf(false) }
    var sourceExpanded by remember { mutableStateOf(false) }
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
    val availableSources = remember(wallets, cards) {
        wallets.toWalletSourceItems() + cards.toCardSourceItems()
    }
    val selectedSource = remember(availableSources, sourceType, selectedSourceId) {
        availableSources.firstOrNull {
            it.id == selectedSourceId && it.type == sourceType
        }
    }
    val isCurrencyLocked = selectedSource != null
    LaunchedEffect(selectedSource?.currency) {
        selectedSource?.currency?.let { currency = it }
    }

    val isSaveEnabled = isAmountValid
        && isDescriptionValid
        && selectedCategoryId != null
        && frequency != null
        && isSourceIdValid
        && endDateError == null
        && !isSaving

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun buildExpense(): RecurringExpense {
        val parsedAmount = amountParseResult.getOrThrow()
        val money = Money(parsedAmount, currency)
        return RecurringExpense(
            id = 0L,
            amount = money,
            currency = currency,
            categoryId = selectedCategoryId ?: error("category not selected"),
            sourceType = sourceType,
            sourceId = selectedSourceId,
            description = description.trim(),
            frequency = frequency ?: error("frequency not selected"),
            startDate = startDate,
            endDate = endDate,
            lastGeneratedDate = null,
            isActive = isActive,
        )
    }

    // ── Date picker dialogs ──────────────────────────────────────────────────

    // These must be remembered at composable level, not inside conditionals.
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
        title = { Text(if (isEdit) "Editar Gasto" else "Agregar Gasto") },
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

                // ── Currency ────────────────────────────────────────────
                Text("Moneda", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Currency.entries.forEach { curr ->
                        FilterChip(
                            selected = currency == curr,
                            onClick = { currency = curr },
                            enabled = !isCurrencyLocked,
                            label = { Text(curr.code) },
                        )
                    }
                }

                // ── Category ────────────────────────────────────────────
                val selectedCategory = remember(categories, selectedCategoryId) {
                    categories.firstOrNull { it.id == selectedCategoryId }
                }
                RecurringCategorySelector(
                    selectedCategory = selectedCategory,
                    onShowSheet = { categoryExpanded = true },
                    error = null,
                )
                if (categoryExpanded) {
                    RecurringCategorySheet(
                        categories = categories,
                        selectedId = selectedCategoryId,
                        onDismiss = { categoryExpanded = false },
                        onCategorySelected = { id ->
                            selectedCategoryId = id
                            categoryExpanded = false
                        },
                    )
                }

                // ── Source (wallet or card) ──────────────────────────────
                // Stashes are intentionally not exposed for recurring expenses.
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
                Text("Frecuencia", style = MaterialTheme.typography.bodySmall)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Frequency.entries.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq.toSpanishLabel()) },
                        )
                    }
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
                    runCatching { buildExpense() }
                        .onSuccess { expense -> onSave(expense) }
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

/** Spanish label for [Frequency] enum values. */
private fun Frequency.toSpanishLabel(): String = when (this) {
    Frequency.DAILY -> "Diario"
    Frequency.WEEKLY -> "Semanal"
    Frequency.MONTHLY -> "Mensual"
    Frequency.YEARLY -> "Anual"
}
