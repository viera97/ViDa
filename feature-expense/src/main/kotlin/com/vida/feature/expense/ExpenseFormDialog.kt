package com.vida.feature.expense

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
import com.vida.feature.expense.form.CategorySelector
import com.vida.feature.expense.form.CategorySheet
import com.vida.feature.expense.form.DateSelector
import com.vida.feature.expense.form.DescriptionInput
import com.vida.feature.expense.form.NoteInput
import com.vida.feature.expense.form.SourceSelector
import com.vida.feature.expense.form.SourceSheet
import com.vida.feature.expense.form.TimeSelector
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Modal dialog for recording a new expense.
 *
 * Mirrors the pattern of [com.vida.feature.cardmanagement.CardFormDialog] but uses the
 * shared [ExpenseFormViewModel] (via `hiltViewModel()`) so submit success navigates via
 * the existing [ExpenseFormUiState.Success] event and field-level validation lives in
 * the ViewModel.
 *
 * State handling:
 * - [ExpenseFormUiState.Loading] → centered progress indicator.
 * - [ExpenseFormUiState.NoSources] → empty-state message ("Sin fuentes") with a single
 *   "Cerrar" button. Replaces the previous infinite-spinner behavior when the user
 *   has no wallets, cards, or stashes registered yet.
 * - [ExpenseFormUiState.Ready] → scrollable form with all field sections.
 * - [ExpenseFormUiState.Submitting] / [ExpenseFormUiState.Error] → last Ready state
 *   remains visible (cached locally so the form doesn't disappear).
 * - [ExpenseFormUiState.Success] → invokes [onDismiss] and the parent closes the dialog.
 * - [ExpenseFormUiState.Error] from submit → top-level inline error banner.
 *
 * The dialog is expected to be hosted under a per-session [androidx.lifecycle.ViewModelStoreOwner]
 * (e.g. provided by [com.vida.feature.home.HomeScreen] via `LocalViewModelStoreOwner`), so
 * each open gets a brand-new `ExpenseFormViewModel`. The VM's `init` runs fresh and the
 * dialog starts in [ExpenseFormUiState.Loading] — no risk of stale terminal state from
 * a previous session triggering an auto-dismiss.
 *
 * Bottom sheets for category and source selection are managed by local
 * [mutableStateOf] flags and rendered after the AlertDialog so they overlay it.
 *
 * @param onDismiss Callback to close the dialog (cancel, tap-outside, or submit success).
 * @param onSuccess Optional callback fired only on submit success — distinct from
 *   [onDismiss] so the caller can refresh host-screen data (e.g. the expense
 *   detail page re-fetching the updated row) without doing it on cancel. Defaults
 *   to a no-op.
 * @param expenseIdToEdit When non-null, opens the form in EDIT mode: pre-fills
 *   the fields from the existing expense and routes submit through
 *   [com.vida.domain.usecase.expense.UpdateExpense]. When null, opens in CREATE
 *   mode with blank fields (default).
 * @param viewModel The [ExpenseFormViewModel], injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {},
    expenseIdToEdit: Long? = null,
    viewModel: ExpenseFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditMode = expenseIdToEdit != null

    // Cache last Ready state so the form stays visible during Submitting / Error.
    var lastReady by remember { mutableStateOf<ExpenseFormUiState.Ready?>(null) }
    (uiState as? ExpenseFormUiState.Ready)?.let { lastReady = it }

    // Bottom sheet visibility flags.
    var showCategorySheet by remember { mutableStateOf(false) }
    var showSourceSheet by remember { mutableStateOf(false) }

    // Reset/load VM and observe its StateFlow directly.
    //
    // Important: we read from `viewModel.uiState` (the StateFlow) instead of the
    // `uiState` State<T> above. The State<T> lags by one frame because the
    // collector that backs it must receive the new value before it propagates.
    // If we observed `uiState` here, a stale `Success` left over from a previous
    // session could still be observed on the first composition and dismiss the
    // dialog before reset() takes effect.
    //
    // Calling reset()/loadForEdit() FIRST (synchronously transitions to Loading)
    // and then subscribing means the first emission we see is `Loading`, never
    // the stale `Success` from a previous session. A real submit Success is
    // still observed because the load has long completed by the time the user
    // fills and submits.
    LaunchedEffect(Unit) {
        if (isEditMode) {
            viewModel.loadForEdit(expenseIdToEdit!!)
        } else {
            viewModel.reset()
        }
        viewModel.uiState.collect { state ->
            if (state is ExpenseFormUiState.Success) {
                onSuccess()
                onDismiss()
            }
        }
    }

    val ready = (uiState as? ExpenseFormUiState.Ready) ?: lastReady
    val isSubmitting = uiState is ExpenseFormUiState.Submitting
    val submitError = (uiState as? ExpenseFormUiState.Error)?.message
    val isNoSources = uiState is ExpenseFormUiState.NoSources

    // `selectedSource` is null both when there's no Ready state and when the user
    // hasn't picked a source yet (`hasSourceSelected = false`). The SourceSelector
    // shows its placeholder in that case instead of pre-selecting a wallet.
    val selectedSource = if (ready != null && ready.form.hasSourceSelected) {
        ready.sources.find {
            it.type == ready.form.sourceType && it.id == ready.form.sourceId
        }
    } else null

    // Reactive mismatch check (currency vs source's currency). Disables Guardar
    // immediately when the user picks a currency that doesn't match the source.
    // Required-field errors (e.g. "El importe es obligatorio") are NOT reactive —
    // they only show after the user attempts to submit (see `ready.validationErrors`).
    val mismatchError = remember(ready?.form, ready?.sources) {
        viewModel.computeMismatchError()
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                when {
                    isNoSources -> "Sin fuentes"
                    isEditMode -> "Editar gasto"
                    else -> "Nuevo gasto"
                },
            )
        },
        text = {
            when {
                isNoSources -> {
                    Text(
                        text = "No hay fuentes registradas. Para registrar un gasto primero agregá una billetera, tarjeta o ahorro desde la sección Fuentes.",
                    )
                }
                ready == null -> {
                    // Still loading and no cached Ready state.
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    val selectedCategory = ready.categories.find { it.id == ready.form.categoryId }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Mismatch error (currency vs source) is reactive — show
                        // it as a top-of-form banner as soon as it's detected,
                        // without waiting for a submit attempt.
                        if (mismatchError != null) {
                            Text(
                                text = mismatchError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        // Top-level submit error banner (only when in Error state and we have a cached form).
                        else if (submitError != null && uiState is ExpenseFormUiState.Error) {
                            Text(
                                text = submitError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        AmountSection(
                            amount = ready.form.amount,
                            amountError = ready.validationErrors["amount"],
                            onAmountChanged = viewModel::onAmountChanged,
                        )
                        DescriptionInput(
                            value = ready.form.description,
                            error = ready.validationErrors["description"],
                            onChanged = viewModel::onDescriptionChanged,
                        )
                        CategorySelector(
                            selectedCategory = selectedCategory,
                            onShowSheet = { showCategorySheet = true },
                            error = ready.validationErrors["category"],
                        )
                        SourceSelector(
                            selectedSource = selectedSource,
                            onShowSheet = { showSourceSheet = true },
                            error = ready.validationErrors["source"],
                        )
                        // Date and time as separate fields side by side. Each
                        // emits its own half; the dialog combines them back into
                        // an Instant and forwards to the VM.
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
                    // Guardar is disabled reactively only when there's a
                    // currency/source mismatch. Required-field errors are NOT
                    // reactive — they only show after the user attempts to
                    // submit and the Save button stays enabled so the user can
                    // trigger the validation feedback.
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

    // Category bottom sheet — rendered after the AlertDialog so it overlays correctly.
    if (showCategorySheet) {
        if (ready != null) {
            CategorySheet(
                categories = ready.categories,
                selectedId = ready.form.categoryId,
                onDismiss = { showCategorySheet = false },
                onCategorySelected = { id ->
                    viewModel.onCategorySelected(id)
                    showCategorySheet = false
                },
            )
        }
    }

    // Source bottom sheet — rendered after the AlertDialog so it overlays correctly.
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

    // Currency is auto-determined by the source — no standalone currency picker.
}