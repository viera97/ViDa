package com.vida.feature.statistics.chart

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vida.feature.statistics.model.StatsPeriod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Horizontal row of period selector chips.
 *
 * Shows [StatsPeriod.presets] as [FilterChip] items plus a "Personalizado" chip
 * that opens a [DatePickerDialog] for custom range selection.
 *
 * @param selectedPeriod Currently active period.
 * @param onPeriodChanged Called when the user selects a new period.
 * @param modifier Modifier for the composable root.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSelector(
    selectedPeriod: StatsPeriod,
    onPeriodChanged: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Preset periods
        StatsPeriod.presets.filterNotNull().forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodChanged(period) },
                label = { Text(period.displayName) },
            )
        }

        // Custom period chip
        FilterChip(
            selected = selectedPeriod is StatsPeriod.Personalizado,
            onClick = { showDatePicker = true },
            label = { Text("Personalizado") },
        )
    }

    // ── Date range picker dialog ──────────────────────────────────────
    if (showDatePicker) {
        CustomDateRangeDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { desde, hasta ->
                onPeriodChanged(StatsPeriod.Personalizado(desde, hasta))
                showDatePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    var step by remember { mutableStateOf(0) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }

    val zoneId = ZoneId.systemDefault()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (step == 0) {
            LocalDate.now().minusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } else {
            LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        if (step == 0) {
                            startDate = date
                            step = 1
                        } else {
                            startDate?.let { start ->
                                val end = date
                                if (start <= end) {
                                    onConfirm(start, end)
                                }
                            }
                        }
                    }
                },
            ) {
                Text(if (step == 0) "Siguiente" else "Aceptar")
            }
        },
        dismissButton = {
            if (step == 1) {
                TextButton(onClick = { step = 0; startDate = null }) {
                    Text("Atrás")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (step == 0) "Selecciona fecha de inicio" else "Selecciona fecha de fin",
                style = MaterialTheme.typography.titleLarge,
            )
            DatePicker(state = datePickerState)
        }
    }
}
