package com.vida.feature.expense

import com.vida.core.ui.SourceItem
import com.vida.domain.model.Category
import com.vida.domain.model.Currency
import com.vida.domain.model.SourceType
import java.time.Instant

/**
 * UI state exposed by [ExpenseFormViewModel].
 *
 * State transitions:
 * ```
 * Loading → Ready | NoSources | Error
 * Ready → Submitting (on valid submit)
 * Submitting → Success | Error (on AddExpense result)
 * ```
 */
sealed interface ExpenseFormUiState {

    /** Emitted on init while use case calls are in-flight. */
    data object Loading : ExpenseFormUiState

    /**
     * No sources are registered (no wallets, cards, or stashes).
     *
     * The dialog should show an empty-state message instead of the form,
     * since the user cannot select a source to charge the expense against.
     */
    data object NoSources : ExpenseFormUiState

    /** Form is ready for user input with all data loaded. */
    data class Ready(
        val form: FormFields,
        val sources: List<SourceItem>,
        val categories: List<Category>,
        val availableCurrencies: List<Currency> = Currency.entries.toList(),
        val validationErrors: Map<String, String> = emptyMap(),
    ) : ExpenseFormUiState

    /** Submission is in-flight — submit button should be disabled. */
    data object Submitting : ExpenseFormUiState

    /** Expense was saved successfully. UI should navigate back. */
    data object Success : ExpenseFormUiState

    /** An error occurred — either during init or submission. */
    data class Error(val message: String) : ExpenseFormUiState
}

/**
 * Form field values. All fields default to "empty / not selected" so the user
 * must explicitly fill them in.
 *
 * @property amount Raw string input for decimal amount (e.g. "1250.50").
 * @property currency Selected currency.
 * @property description Expense description (optional).
 * @property categoryId Selected category id, required before submission.
 * @property sourceType Which kind of source is selected (WALLET, CARD, STASH).
 *   Defaults to [SourceType.WALLET] but only takes effect once the user makes
 *   an explicit pick — see [hasSourceSelected].
 * @property sourceId Entity id of the selected source; null for WALLET only.
 * @property hasSourceSelected `true` once the user explicitly picks a source from
 *   the source sheet. Required for submission. Without this flag, defaults like
 *   `sourceType = WALLET, sourceId = null` would look like a valid selection
 *   because the wallet is the singleton (always null id).
 * @property dateTime When the expense occurred — defaults to [Instant.now].
 * @property note Optional free-form text, no validation.
 */
data class FormFields(
    val amount: String = "",
    val currency: Currency = Currency.CUP,
    val description: String = "",
    val categoryId: Long? = null,
    val sourceType: SourceType = SourceType.WALLET,
    val sourceId: Long? = null,
    val hasSourceSelected: Boolean = false,
    val dateTime: Instant = Instant.now(),
    val note: String = "",
)
