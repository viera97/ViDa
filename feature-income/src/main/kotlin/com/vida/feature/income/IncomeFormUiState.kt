package com.vida.feature.income

import com.vida.domain.model.Currency
import com.vida.domain.model.SourceType
import java.time.Instant

/**
 * UI state exposed by [IncomeFormViewModel].
 *
 * Mirrors [com.vida.feature.expense.ExpenseFormUiState] but drops the
 * `category` machinery — incomes are not categorized. The "what kind of
 * income" semantic lives in the free-text [IncomeFormFields.description]
 * field instead (e.g. "Salario", "Regalo", "Devolución").
 *
 * Reuses [com.vida.feature.expense.SourceItem] from the expense module for
 * the destination source picker — the shape (id, type, label, subtitle,
 * currency) is identical for both forms.
 *
 * State transitions:
 * ```
 * Loading → Ready | NoSources | Error
 * Ready → Submitting (on valid submit)
 * Submitting → Success | Error (on RecordIncome result)
 * ```
 */
sealed interface IncomeFormUiState {

    /** Emitted on init while use case calls are in-flight. */
    data object Loading : IncomeFormUiState

    /**
     * No sources are registered (no wallets, cards, or stashes).
     *
     * The dialog should show an empty-state message instead of the form,
     * since the user cannot select a destination to credit the income against.
     */
    data object NoSources : IncomeFormUiState

    /** Form is ready for user input with all data loaded. */
    data class Ready(
        val form: IncomeFormFields,
        val sources: List<com.vida.feature.expense.SourceItem>,
        val availableCurrencies: List<Currency> = Currency.entries.toList(),
        val validationErrors: Map<String, String> = emptyMap(),
    ) : IncomeFormUiState

    /** Submission is in-flight — submit button should be disabled. */
    data object Submitting : IncomeFormUiState

    /** Income was saved successfully. UI should navigate back. */
    data object Success : IncomeFormUiState

    /** An error occurred — either during init or submission. */
    data class Error(val message: String) : IncomeFormUiState
}

/**
 * Form field values for the income dialog.
 *
 * Unlike [com.vida.feature.expense.FormFields], has no `categoryId` — incomes
 * are not categorized.
 *
 * @property amount Raw string input for decimal amount (e.g. "1250.50").
 * @property currency Selected currency.
 * @property description Free-text label, not blank (e.g. "Salario").
 * @property sourceType Which kind of source receives the income.
 * @property sourceId Entity id of the selected source; null for WALLET only.
 * @property hasSourceSelected `true` once the user explicitly picks a source from
 *   the source sheet. Required for submission.
 * @property dateTime When the income occurred — defaults to [Instant.now].
 * @property note Optional free-form text, no validation.
 */
data class IncomeFormFields(
    val amount: String = "",
    val currency: Currency = Currency.CUP,
    val description: String = "",
    val sourceType: SourceType = SourceType.WALLET,
    val sourceId: Long? = null,
    val hasSourceSelected: Boolean = false,
    val dateTime: Instant = Instant.now(),
    val note: String = "",
)
