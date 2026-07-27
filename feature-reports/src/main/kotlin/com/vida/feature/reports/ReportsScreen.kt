package com.vida.feature.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vida.domain.model.statistics.ReportsPeriod
import com.vida.domain.model.statistics.ReportsPeriod.Personalizado
import com.vida.feature.reports.model.CategoryRow
import com.vida.feature.reports.model.MoneyRow
import com.vida.feature.reports.model.ReportListItem
import com.vida.feature.reports.model.ReportsUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Root composable for the Reports screen.
 *
 * Layout (top to bottom):
 * - TopAppBar ("Reportes") with back button
 * - Granularity chip row (Diario / Mensual / Anual / Personalizado)
 * - LazyColumn of [ReportListItem] cards, newest first
 *
 * @param onNavigateBack Back navigation via toolbar arrow (typically `popBackStack`).
 * @param viewModel Injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is ReportsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is ReportsUiState.Ready -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    item(key = "chips") {
                        GranularityChipRow(
                            selectedPeriod = state.period,
                            onPeriodChanged = viewModel::onPeriodChanged,
                        )
                    }
                    items(
                        items = state.entries,
                        key = { it.periodStart.toEpochMilli() },
                    ) { entry ->
                        ReportListCard(item = entry)
                    }
                    item(key = "bottom") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            is ReportsUiState.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Sin datos",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No hay transacciones en el período seleccionado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }

            is ReportsUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.onRetry() }) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

/**
 * Horizontal row of granularity selector chips.
 *
 * Mirrors [com.vida.feature.statistics.chart.PeriodSelector] exactly: the `Personalizado`
 * chip opens a two-step [DatePickerDialog] (see [CustomDateRangeDialog]).
 */
@Composable
private fun GranularityChipRow(
    selectedPeriod: ReportsPeriod,
    onPeriodChanged: (ReportsPeriod) -> Unit,
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
        ReportsPeriod.presets.filterNotNull().forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodChanged(period) },
                label = { Text(period.displayName) },
            )
        }
        FilterChip(
            selected = selectedPeriod is ReportsPeriod.Personalizado,
            onClick = { showDatePicker = true },
            label = { Text("Personalizado") },
        )
    }

    if (showDatePicker) {
        CustomDateRangeDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { desde, hasta ->
                onPeriodChanged(ReportsPeriod.Personalizado(desde, hasta))
                showDatePicker = false
            },
        )
    }
}

/**
 * Two-step date range picker dialog. Mirrors
 * `feature-statistics/.../chart/PeriodSelector.kt:87-149` 1:1 — see that file for the
 * canonical pattern. The "end date < start date" guard silently drops the confirm
 * (`if (start <= end) { onConfirm(start, end) }`) — matches Statistics behavior.
 */
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

/** Single bucket card with category breakdown and per-currency Income / Expense / Net sections. */
@Composable
private fun ReportListCard(item: ReportListItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.periodLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (item.categoryRows.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                item.categoryRows.forEach { row ->
                    CategoryBreakdownRow(row)
                }
            }
            if (item.incomeRows.isNotEmpty() || item.expenseRows.isNotEmpty() || item.netRows.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            if (item.incomeRows.isNotEmpty()) {
                SectionLabel("Ingresos")
                item.incomeRows.forEach { row ->
                    CurrencyAmountRow(row, MaterialTheme.colorScheme.primary)
                }
            }
            if (item.expenseRows.isNotEmpty()) {
                SectionLabel("Gastos")
                item.expenseRows.forEach { row ->
                    CurrencyAmountRow(row, MaterialTheme.colorScheme.error)
                }
            }
            if (item.netRows.isNotEmpty()) {
                SectionLabel("Neto")
                item.netRows.forEach { row ->
                    val color = when {
                        row.isNegative -> MaterialTheme.colorScheme.error
                        row.amountLabel == "0.00 ${row.currency.code}" -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.primary
                    }
                    CurrencyAmountRow(row, color)
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(row: CategoryRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = row.categoryName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = row.amountLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun CurrencyAmountRow(row: MoneyRow, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = row.currency.code,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = row.amountLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}
