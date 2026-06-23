package com.vida.feature.expense

import com.vida.domain.model.Category
import com.vida.domain.model.Currency
import com.vida.domain.model.SourceType
import java.time.Instant

/**
 * UI state exposed by [ExpenseFormViewModel].
 *
 * State transitions:
 * ```
 * Loading → Ready | Error
 * Ready → Submitting (on valid submit)
 * Submitting → Success | Error (on AddExpense result)
 * ```
 */
sealed interface ExpenseFormUiState {

    /** Emitted on init while use case calls are in-flight. */
    data object Loading : ExpenseFormUiState

    /** Form is ready for user input with all data loaded. */
    data class Ready(
        val form: FormFields,
        val sources: List<SourceItem>,
        val categories: List<Category>,
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
 * Form field values. Defaults match the wallet source (the default source).
 *
 * @property amount Raw string input for decimal amount (e.g. "1250.50").
 * @property currency Selected currency — defaults to the selected source's currency.
 * @property description Expense description, must not be blank.
 * @property categoryId Selected category id, required before submission.
 * @property sourceType Which kind of source is selected (WALLET, CARD, STASH).
 * @property sourceId Entity id of the selected source; null for WALLET only.
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
    val dateTime: Instant = Instant.now(),
    val note: String = "",
)

/**
 * A single expense source (wallet, card, or stash) as displayed in the source picker.
 *
 * @property id Entity id; null only for WALLET (the singleton).
 * @property type Which kind of source this is.
 * @property label Display name (e.g. "Billetera", "Banco kubo", "Ahorro vacaciones").
 * @property subtitle Optional supplementary text (e.g. masked card number).
 * @property currency The source's native currency.
 */
data class SourceItem(
    val id: Long?,
    val type: SourceType,
    val label: String,
    val subtitle: String? = null,
    val currency: Currency,
)
