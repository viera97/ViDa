package com.vida.feature.income

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vida.feature.expense.form.AmountSection
import com.vida.feature.expense.form.DateSelector
import com.vida.feature.expense.form.DescriptionInput
import com.vida.feature.expense.form.NoteInput
import com.vida.feature.expense.form.SourceSelector
import com.vida.feature.expense.form.SourceSheet
import com.vida.feature.expense.form.TimeSelector
import java.time.ZoneId

/**
 * Modal dialog for recording a new income.
 *
 * Mirrors [com.vida.feature.expense.ExpenseFormDialog] but drops the category
 * field — incomes are not categorized the way expenses are. Reuses the
 * `feature-expense` form sections ([AmountSection], [DescriptionInput],
 * [SourceSelector], [SourceSheet], [DateSelector], [TimeSelector],
 * [NoteInput]) so the UI stays consistent with the expense dialog.
 *
 * State handling is identical to [com.vida.feature.expense.ExpenseFormDialog]:
 * - [IncomeFormUiState.Loading] → centered progress indicator.
 * - [IncomeFormUiState.NoSources] → empty-state message ("Sin fuentes") with a
 *   single "Cerrar" button.
 * - [IncomeFormUiState.Ready] → scrollable form with all field sections.
 * - [IncomeFormUiState.Submitting] / [IncomeFormUiState.Error] → last Ready
 *   state remains visible (cached locally so the form doesn't disappear).
 * - [IncomeFormUiState.Success] → invokes [onDismiss] and the parent closes the dialog.
 *
 * On submit, the destination source's stored `balance_minor` is auto-updated
 * via [com.vida.domain.repository.IncomeRepository.upsert] (handled by the data
 * layer, not the dialog).
 *
 * @param onDismiss Callback to close the dialog (cancel, tap-outside, or submit success).
 * @param viewModel The [IncomeFormViewModel], injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeFormDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {},
    expenseIdToEdit: Long? = null,
    viewModel: IncomeFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditMode = expenseIdToEdit != null

    // Cache last Ready state so the form stays visible during Submitting / Error.
    var lastReady by remember { mutableStateOf<IncomeFormUiState.Ready?>(null) }
    (uiState as? IncomeFormUiState.Ready)?.let { lastReady = it }

    // Bottom sheet visibility flag.
    var showSourceSheet by remember { mutableStateOf(false) }

    // Reset/load VM on open and observe Success to auto-dismiss.
    LaunchedEffect(Unit) {
        if (isEditMode) {
            viewModel.loadForEdit(expenseIdToEdit!!)
        } else {
            viewModel.reset()
        }
        viewModel.uiState.collect { state ->
            if (state is IncomeFormUiState.Success) {
                onSuccess()
                onDismiss()
            }
        }
    }

    val ready = (uiState as? IncomeFormUiState.Ready) ?: lastReady
    val isSubmitting = uiState is IncomeFormUiState.Submitting
    val submitError = (uiState as? IncomeFormUiState.Error)?.message
    val isNoSources = uiState is IncomeFormUiState.NoSources

    val selectedSource = if (ready != null && ready.form.hasSourceSelected) {
        ready.sources.find {
            it.type == ready.form.sourceType && it.id == ready.form.sourceId
        }
    } else null

    // Reactive mismatch check (currency vs source's currency).
    val mismatchError = remember(ready?.form, ready?.sources) {
        viewModel.computeMismatchError()
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                when {
                    isNoSources -> "Sin fuentes"
                    isEditMode -> "Editar ingreso"
                    else -> "Nuevo ingreso"
                },
            )
        },
        text = {
            when {
                isNoSources -> {
                    Text(
                        text = "No hay fuentes registradas. Para registrar un ingreso primero agregá una billetera, tarjeta o ahorro desde la sección Fuentes.",
                    )
                }
                ready == null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (mismatchError != null) {
                            Text(
                                text = mismatchError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else if (submitError != null && uiState is IncomeFormUiState.Error) {
                            Text(
                                text = submitError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        AmountSection(
                            amount = ready.form.amount,
                            currency = ready.form.currency,
                            amountError = ready.validationErrors["amount"],
                            onAmountChanged = viewModel::onAmountChanged,
                            onCurrencyChanged = viewModel::onCurrencyChanged,
                        )
                        DescriptionInput(
                            value = ready.form.description,
                            error = ready.validationErrors["description"],
                            onChanged = viewModel::onDescriptionChanged,
                        )
                        SourceSelector(
                            selectedSource = selectedSource,
                            onShowSheet = { showSourceSheet = true },
                            error = ready.validationErrors["source"],
                        )
                        val zone = ZoneId.systemDefault()
                        val currentDate = remember(ready.form.dateTime) {
                            ready.form.dateTime.atZone(zone).toLocalDate()
                        }
                        val currentTime = remember(ready.form.dateTime) {
                            ready.form.dateTime.atZone(zone).toLocalTime()
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            DateSelector(
                                date = currentDate,
                                onChanged = { newDate ->
                                    viewModel.onDateTimeChanged(
                                        newDate.atTime(currentTime).atZone(zone).toInstant(),
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                            TimeSelector(
                                time = currentTime,
                                onChanged = { newTime ->
                                    viewModel.onDateTimeChanged(
                                        currentDate.atTime(newTime).atZone(zone).toInstant(),
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        NoteInput(
                            value = ready.form.note,
                            onChanged = viewModel::onNoteChanged,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            if (isNoSources) {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            } else {
                TextButton(
                    onClick = { viewModel.submit() },
                    enabled = ready != null &&
                        mismatchError == null &&
                        !isSubmitting,
                ) {
                    Text(if (isSubmitting) "Guardando…" else "Guardar")
                }
            }
        },
        dismissButton = {
            if (!isNoSources) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                ) {
                    Text("Cancelar")
                }
            }
        },
    )

    if (showSourceSheet) {
        if (ready != null) {
            SourceSheet(
                sources = ready.sources,
                selectedSource = if (ready.form.hasSourceSelected) {
                    ready.sources.find {
                        it.type == ready.form.sourceType && it.id == ready.form.sourceId
                    }
                } else null,
                onDismiss = { showSourceSheet = false },
                onSourceSelected = { source ->
                    viewModel.onSourceSelected(source.type, source.id)
                    showSourceSheet = false
                },
            )
        }
    }
}
