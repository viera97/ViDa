package com.vida.feature.recurringexpensemanagement

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
 * Pre-formatted display item for a single recurring expense row.
 *
 * @property id Template row id.
 * @property amountFormatted Amount with 2 decimal places (e.g. "500.00").
 * @property currencyCode Currency code string ("CUP", "USD", "MLC").
 * @property categoryName Category label for display.
 * @property frequencyLabel Spanish frequency label ("Diario", "Semanal", "Mensual", "Anual").
 * @property sourceTypeIcon Emoji icon for source type (💰/♠/💎).
 * @property nextDueFormatted Next due date as "dd/MM/yyyy".
 * @property description Template description.
 * @property isActive Whether the template is active.
 */
data class RecurringDisplayItem(
    val id: Long,
    val amountFormatted: String,
    val currencyCode: String,
    val categoryName: String,
    val frequencyLabel: String,
    val sourceTypeIcon: String,
    val nextDueFormatted: String,
    val description: String,
    val isActive: Boolean,
)

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
}
