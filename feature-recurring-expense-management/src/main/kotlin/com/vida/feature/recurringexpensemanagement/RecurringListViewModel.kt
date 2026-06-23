package com.vida.feature.recurringexpensemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Frequency
import com.vida.domain.model.RecurringExpense
import com.vida.domain.model.SourceType
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.expense.RecordExpense
import com.vida.domain.usecase.recurring.AddRecurringExpense
import com.vida.domain.usecase.recurring.DeleteRecurringExpense
import com.vida.domain.usecase.recurring.GenerateRecurringExpense
import com.vida.domain.usecase.recurring.GetDueRecurringExpenses
import com.vida.domain.usecase.recurring.GetRecurringExpense
import com.vida.domain.usecase.recurring.ListRecurringExpenses
import com.vida.domain.usecase.recurring.UpdateRecurringExpense
import com.vida.domain.usecase.stash.ListStashes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for the recurring expense list screen.
 *
 * Collects [ListRecurringExpenses] as a reactive Flow — Room auto-re-emits
 * after mutations, so no manual refetch is needed.
 *
 * Sort: isActive DESC → frequency ordinal ASC (DAILY first) → startDate ASC.
 *
 * PR #1: list, delete, toggle active, retry.
 * PR #2 (future): add/edit with form dialog.
 * PR #3 (future): two-step generate flow.
 */
@HiltViewModel
class RecurringListViewModel @Inject constructor(
    private val listRecurringExpenses: ListRecurringExpenses,
    private val addRecurringExpense: AddRecurringExpense,
    private val updateRecurringExpense: UpdateRecurringExpense,
    private val deleteRecurringExpense: DeleteRecurringExpense,
    private val getRecurringExpense: GetRecurringExpense,
    private val getDueRecurringExpenses: GetDueRecurringExpenses,
    private val generateRecurringExpense: GenerateRecurringExpense,
    private val recordExpense: RecordExpense,
    private val listCategories: ListCategories,
    private val listCards: ListCards,
    private val listStashes: ListStashes,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecurringListUiState>(RecurringListUiState.Loading)
    val uiState: StateFlow<RecurringListUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<RecurringNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    /** True while any mutation (delete, toggle) is in-flight (prevents double-tap). */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        collectRecurringExpenses()
    }

    // ── Public actions ───────────────────────────────────────────────────────

    /**
     * Emits [RecurringNavEvent.ShowAddDialog] so the screen opens the add dialog.
     * (Form dialog implementation arrives in PR #2.)
     */
    fun onFabClick() {
        viewModelScope.launch {
            _navEvents.send(RecurringNavEvent.ShowAddDialog)
        }
    }

    /**
     * Emits [RecurringNavEvent.ShowDeleteDialog] so the screen shows
     * delete confirmation for the given [item].
     */
    fun onRequestDelete(item: RecurringDisplayItem) {
        viewModelScope.launch {
            _navEvents.send(RecurringNavEvent.ShowDeleteDialog(item))
        }
    }

    /**
     * Deletes the recurring expense template with [id].
     *
     * On success the reactive Flow re-emits and a success toast is shown.
     * On error the current list is preserved and an error toast is shown.
     */
    fun onDelete(id: Long) {
        if (_isSaving.value) return

        val current = _uiState.value
        if (current !is RecurringListUiState.Ready) return

        if (current.items.none { it.id == id }) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                deleteRecurringExpense(id)
                // Flow auto-re-emits — no manual refetch needed.
                _navEvents.send(
                    RecurringNavEvent.ShowToast("Plantilla eliminada"),
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    RecurringNavEvent.ShowToast(
                        t.message ?: "No se pudo eliminar la plantilla",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Toggles the `isActive` flag on the template with [id].
     *
     * Fetches the full entity via [GetRecurringExpense], flips [RecurringExpense.isActive],
     * and persists via [UpdateRecurringExpense]. The reactive Flow re-emits automatically.
     */
    fun onToggleActive(id: Long) {
        if (_isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val existing = getRecurringExpense(id)
                    ?: run {
                        _navEvents.send(
                            RecurringNavEvent.ShowToast("Plantilla no encontrada"),
                        )
                        return@launch
                    }
                val toggled = existing.copy(isActive = !existing.isActive)
                updateRecurringExpense(toggled)
                // Flow auto-re-emits.
                _navEvents.send(
                    RecurringNavEvent.ShowToast(
                        if (toggled.isActive) "Plantilla activada" else "Plantilla desactivada",
                    ),
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    RecurringNavEvent.ShowToast(
                        t.message ?: "No se pudo actualizar la plantilla",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** Re-initiates the Flow collection from the [RecurringListUiState.Error] state. */
    fun onRetry() {
        collectRecurringExpenses()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Collects the reactive [ListRecurringExpenses] Flow, sorts, maps to display
     * items, and emits the appropriate [RecurringListUiState].
     */
    private fun collectRecurringExpenses() {
        viewModelScope.launch {
            try {
                listRecurringExpenses()
                    .onStart { _uiState.value = RecurringListUiState.Loading }
                    .catch { t ->
                        if (t is CancellationException) throw t
                        _uiState.value = RecurringListUiState.Error(
                            message = t.message ?: "No se pudieron cargar las plantillas",
                        )
                    }
                    .collect { templates ->
                        val sorted = templates.sortedWith(
                            compareByDescending<RecurringExpense> { it.isActive }
                                .thenBy { it.frequency.ordinal }
                                .thenBy { it.startDate },
                        )
                        val items = sorted.map { it.toDisplayItem() }

                        _uiState.value = if (items.isEmpty()) {
                            RecurringListUiState.Empty
                        } else {
                            RecurringListUiState.Ready(items = items)
                        }
                    }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = RecurringListUiState.Error(
                    message = t.message ?: "No se pudieron cargar las plantillas",
                )
            }
        }
    }

    /**
     * Maps a domain [RecurringExpense] to a pre-formatted [RecurringDisplayItem].
     *
     * - amountFormatted: "###.00" from [RecurringExpense.amount].
     * - currencyCode: [com.vida.domain.model.Currency.code].
     * - categoryName: (placeholder — category lookup arrives in PR #2 via ListCategories).
     *   For now we display the categoryId as a fallback.
     * - frequencyLabel: Spanish label via [Frequency.toSpanishLabel].
     * - sourceTypeIcon: emoji via [SourceType.toIcon].
     * - nextDueFormatted: "dd/MM/yyyy" derived from lastGeneratedDate + 1 period,
     *   falling back to startDate.
     */
    private fun RecurringExpense.toDisplayItem(): RecurringDisplayItem {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val scale = amount.amount.setScale(2, RoundingMode.HALF_EVEN)
        return RecurringDisplayItem(
            id = id,
            amountFormatted = scale.toPlainString(),
            currencyCode = currency.code,
            categoryName = categoryId.toString(), // PR #2: replace with actual category name
            frequencyLabel = frequency.toSpanishLabel(),
            sourceTypeIcon = sourceType.toIcon(),
            nextDueFormatted = nextDueDate()?.format(formatter) ?: "—",
            description = description,
            isActive = isActive,
        )
    }

    /**
     * Computes the next due date for this template.
     *
     * - If [RecurringExpense.lastGeneratedDate] is non-null, the next due date
     *   is `lastGeneratedDate + 1 period` (using the template's frequency).
     * - Otherwise falls back to [RecurringExpense.startDate].
     * - Returns null when startDate hasn't been reached yet.
     */
    private fun RecurringExpense.nextDueDate(): LocalDate? {
        val lastGen = lastGeneratedDate
        val candidate = if (lastGen != null) {
            when (frequency) {
                Frequency.DAILY -> lastGen.plusDays(1)
                Frequency.WEEKLY -> lastGen.plusWeeks(1)
                Frequency.MONTHLY -> lastGen.plusMonths(1)
                Frequency.YEARLY -> lastGen.plusYears(1)
            }
        } else {
            startDate
        }
        // startDate not yet reached — treat as "not eligible yet".
        if (candidate.isBefore(startDate)) return null
        return candidate
    }

    companion object {
        /** Spanish labels for [Frequency] enum values. */
        fun Frequency.toSpanishLabel(): String = when (this) {
            Frequency.DAILY -> "Diario"
            Frequency.WEEKLY -> "Semanal"
            Frequency.MONTHLY -> "Mensual"
            Frequency.YEARLY -> "Anual"
        }

        /** Emoji icon for [SourceType] values. */
        fun SourceType.toIcon(): String = when (this) {
            SourceType.WALLET -> "\uD83D\uDCB0"
            SourceType.CARD -> "\u2660"
            SourceType.STASH -> "\uD83D\uDC8E"
        }
    }
}
