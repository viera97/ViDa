package com.vida.feature.recurringexpensemanagement

import com.vida.domain.model.RecurringExpense
import com.vida.domain.model.RecurringIncome
import com.vida.domain.model.SourceType

/**
 * UI state for the recurring expense list screen.
 *
 * Transitions:
 * ```
 * Loading → Ready | Empty | Error
 * Ready   → Ready (mutations trigger reactive Flow re-emission)
 * Error   → Loading → ... (on retry)
 * ```
 */
sealed interface RecurringListUiState {

    /** Emitted while [com.vida.domain.usecase.recurring.ListRecurringExpenses] Flow is collected. */
    data object Loading : RecurringListUiState

    /** Templates loaded and sorted (isActive DESC → frequency ASC → startDate ASC). */
    data class Ready(val items: List<RecurringDisplayItem>) : RecurringListUiState

    /** No templates exist in the database. */
    data object Empty : RecurringListUiState

    /** Initial load failed. Retry available via [RecurringListViewModel.onRetry]. */
    data class Error(val message: String) : RecurringListUiState
}

/**
 * Pre-formatted display item for a single recurring template row (expense or income).
 *
 * @property id Template row id.
 * @property amountFormatted Amount with 2 decimal places (e.g. "500.00").
 * @property currencyCode Currency code string ("CUP", "USD", "MLC").
 * @property categoryName Category label for display (empty for incomes).
 * @property frequencyLabel Spanish frequency label ("Diario", "Semanal", "Mensual", "Anual").
 * @property sourceType Source type — used by the UI to render the right Material icon.
 * @property sourceTypeIcon Emoji icon for source type (💰/♠/💎). Kept for backwards
 *   compatibility with any consumer; the item composable now uses [sourceType]
 *   to render a proper Material icon.
 * @property nextDueFormatted Next due date as "dd/MM/yyyy".
 * @property description Template description.
 * @property isActive Whether the template is active.
 * @property type Whether this is an EXPENSE or INCOME item.
 * @property frequencyOrdinal Ordinal for sorting (matches [Frequency.ordinal]).
 * @property startDateEpochDay Start date epoch day for sorting.
 */
data class RecurringDisplayItem(
    val id: Long,
    val amountFormatted: String,
    val currencyCode: String,
    val categoryName: String,
    val frequencyLabel: String,
    val sourceType: SourceType,
    val sourceTypeIcon: String,
    val nextDueFormatted: String,
    val description: String,
    val isActive: Boolean,
    val type: ItemType = ItemType.EXPENSE,
    val frequencyOrdinal: Int = 0,
    val startDateEpochDay: Long = 0L,
) {
    enum class ItemType { EXPENSE, INCOME }
}

/**
 * One-shot navigation events emitted by [RecurringListViewModel].
 *
 * Consumed via [kotlinx.coroutines.channels.Channel] and observed in the
 * composable with [androidx.compose.runtime.LaunchedEffect].
 */
sealed class RecurringNavEvent {
    /** Show a transient toast / snackbar message. */
    data class ShowToast(val message: String) : RecurringNavEvent()

    /** Emitted after an add or edit operation completes successfully. */
    data object SaveSuccess : RecurringNavEvent()

    /** Emitted when the FAB is tapped, requesting the add dialog to open. */
    data object ShowAddDialog : RecurringNavEvent()

    /** Emitted when delete confirmation is requested for a template. */
    data class ShowDeleteDialog(val item: RecurringDisplayItem) : RecurringNavEvent()

    /** Emitted when edit is requested for a template, carrying the full entity. */
    data class ShowEditDialog(val entity: RecurringExpense) : RecurringNavEvent()

    // ── Income-specific events ───────────────────────────────────────────────

    /** Emitted when the income FAB is tapped, requesting the add dialog to open. */
    data object ShowAddIncomeDialog : RecurringNavEvent()

    /** Emitted when edit is requested for an income template, carrying the full entity. */
    data class ShowIncomeEditDialog(val entity: RecurringIncome) : RecurringNavEvent()
}
