package com.vida.feature.expenselist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vida.domain.model.Category
import com.vida.domain.model.Currency
import com.vida.domain.model.ExpenseFilter
import com.vida.domain.model.SourceType
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "ES"))

private val Currency.displayLabel: String get() = when (this) {
    Currency.CUP -> "CUP"
    Currency.USD -> "USD"
    Currency.MLC -> "MLC"
    Currency.EUR -> "EUR"
}

private val SourceType.displayLabel: String get() = when (this) {
    SourceType.WALLET -> "Billetera"
    SourceType.CARD -> "Tarjeta"
    SourceType.STASH -> "Reserva"
}

/**
 * Modal bottom sheet for building an [ExpenseFilter].
 *
 * Sections:
 * - Date range: "Desde" / "Hasta" → [DatePickerDialog]
 * - Categories: multi-select [FilterChip] flow row
 * - Currency: single-select row (CUP / USD / MLC + Todas)
 * - Source type: single-select row (Billetera / Tarjeta / Reserva + Todas)
 *
 * @param currentFilter Pre-populates the editing buffer.
 * @param categories All available categories to render as multi-select chips.
 * @param onApply Called with the edited filter on "Aplicar filtros".
 * @param onClear Called on "Limpiar filtros" (clears everything).
 * @param onDismiss Called on back gesture / scrim tap.
 * @param sheetState M3 bottom sheet state.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseFilterSheet(
    currentFilter: ExpenseFilter,
    categories: List<Category>,
    onApply: (ExpenseFilter) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    // Local editing buffer — reset when currentFilter changes.
    var dateFrom: Instant? by remember(currentFilter) { mutableStateOf(currentFilter.dateFrom) }
    var dateTo: Instant? by remember(currentFilter) { mutableStateOf(currentFilter.dateTo) }
    var selectedCategoryIds: Set<Long> by remember(currentFilter) {
        mutableStateOf(currentFilter.categoryIds ?: emptySet())
    }
    var selectedCurrency: Currency? by remember(currentFilter) {
        mutableStateOf(currentFilter.currency)
    }
    var selectedSourceType: SourceType? by remember(currentFilter) {
        mutableStateOf(currentFilter.sourceType)
    }

    // Which date field is being edited (null = none).
    var editingDateField: EditingDateField? by remember { mutableStateOf(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            // ── Title ───────────────────────────────────────────────────────
            Text(
                text = "Filtros",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ── Date range ──────────────────────────────────────────────────
            SectionHeader("Rango de fechas")
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { editingDateField = EditingDateField.FROM },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(formatDateLabel("Desde", dateFrom))
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { editingDateField = EditingDateField.TO },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(formatDateLabel("Hasta", dateTo))
                }
            }

            // ── Categories ──────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("Categorías")
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                categories.forEach { cat ->
                    val selected = cat.id in selectedCategoryIds
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedCategoryIds = if (selected) {
                                selectedCategoryIds - cat.id
                            } else {
                                selectedCategoryIds + cat.id
                            }
                        },
                        label = { Text(cat.name) },
                    )
                }
            }

            // ── Currency ────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("Moneda")
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedCurrency == null,
                    onClick = { selectedCurrency = null },
                    label = { Text("Todas") },
                )
                Currency.values().forEach { cur ->
                    FilterChip(
                        selected = selectedCurrency == cur,
                        onClick = { selectedCurrency = cur },
                        label = { Text(cur.displayLabel) },
                    )
                }
            }

            // ── Source type ─────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("Tipo de fuente")
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedSourceType == null,
                    onClick = { selectedSourceType = null },
                    label = { Text("Todas") },
                )
                SourceType.values().forEach { st ->
                    FilterChip(
                        selected = selectedSourceType == st,
                        onClick = { selectedSourceType = st },
                        label = { Text(st.displayLabel) },
                    )
                }
            }

            // ── Actions ─────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onClear) {
                    Text("Limpiar filtros")
                }
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        onApply(
                            ExpenseFilter(
                                dateFrom = dateFrom,
                                dateTo = dateTo,
                                categoryIds = selectedCategoryIds.takeIf { it.isNotEmpty() },
                                currency = selectedCurrency,
                                sourceType = selectedSourceType,
                                searchQuery = currentFilter.searchQuery,
                            )
                        )
                    },
                ) {
                    Text("Aplicar filtros")
                }
            }
        }
    }

    // ── Date picker dialog ──────────────────────────────────────────────────
    val editing = editingDateField
    if (editing != null) {
        val currentMillis = when (editing) {
            EditingDateField.FROM -> dateFrom?.toEpochMilli()
            EditingDateField.TO -> dateTo?.toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentMillis ?: System.currentTimeMillis(),
        )

        DatePickerDialog(
            onDismissRequest = { editingDateField = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        val instant = millis?.let { Instant.ofEpochMilli(it) }
                        when (editing) {
                            EditingDateField.FROM -> dateFrom = instant
                            EditingDateField.TO -> dateTo = instant
                        }
                        editingDateField = null
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { editingDateField = null }) {
                    Text("Cancelar")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ── Internal helpers ────────────────────────────────────────────────────────

private enum class EditingDateField { FROM, TO }

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun formatDateLabel(fallback: String, instant: Instant?): String {
    if (instant == null) return fallback
    val localDate = instant.atOffset(ZoneOffset.UTC).toLocalDate()
    return localDate.format(dateFormatter)
}
