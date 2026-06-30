package com.vida.feature.home

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import java.math.BigDecimal

/**
 * UI state exposed by [HomeViewModel].
 *
 * @see <a href="spec:HomeUiState lifecycle">SCN-HOME-001–004</a>
 */
sealed interface HomeUiState {
    /** Emitted on init before data is available. */
    data object Loading : HomeUiState

    /**
     * Happy-path state with all data loaded.
     *
     * @property totalBalance CUP aggregate from [GetTotalBalance].
     * @property perCurrencySubtotals Only currencies with ≥1 non-zero source (S4).
     * @property perSource Wallet + each card + each stash with native balance.
     * @property recentExpenses At most 5 newest expenses (newest first).
     * @property recentIncomes At most 5 newest incomes (newest first).
     * @property rates Nullable — `null` means section is hidden (S3).
     */
    data class Ready(
        val totalBalance: Money,
        val perCurrencySubtotals: Map<Currency, Money>,
        val perSource: List<PerSource>,
        val recentExpenses: List<RecentExpenseItem>,
        val recentIncomes: List<RecentIncomeItem>,
        val rates: Map<String, BigDecimal>?,
    ) : HomeUiState

    /**
     * All-zero state (R8): total zero, no expenses, no rates.
     * No CTA, no onboarding.
     */
    data object Empty : HomeUiState

    /** Fail-fast error — thrown by balance use cases (S2). */
    data class Error(val message: String) : HomeUiState
}

/**
 * A single balance source (wallet / card / stash).
 *
 * @property label Display name, e.g. "Billetera", "Banco kubo ···1234".
 * @property balance Balance in the source's native currency.
 * @property formatted Pre-formatted balance string for rendering.
 * @property sourceType Which kind of source this row represents. The UI uses
 *   this to pick a distinguishing icon (wallet/card/stash) shown next to the
 *   label.
 * @property sourceId Entity id of the underlying wallet/card/stash row. Combined
 *   with [sourceType] it uniquely identifies the source — used by
 *   [HomeViewModel] to sort by last-use (the most recently transacted source
 *   appears first).
 */
data class PerSource(
    val label: String,
    val balance: Money,
    val formatted: String,
    val sourceType: SourceType,
    val sourceId: Long,
)

/**
 * A recent expense row in the Home dashboard.
 *
 * @property categoryName Category name, or "Sin categoría" if category lookup fails.
 * @property formattedAmount Pre-formatted amount string (e.g. "1,250.00 CUP").
 * @property sourceLabel Target name, e.g. "Efectivo", "Banco BPA", "Kubo ···1234", or stash name.
 * @property relativeDate Spanish relative date (e.g. "hace 2 días").
 */
data class RecentExpenseItem(
    val categoryName: String,
    val formattedAmount: String,
    val sourceLabel: String,
    val relativeDate: String,
)

/**
 * A recent income row in the Home dashboard.
 *
 * Mirrors [RecentExpenseItem] but without the category — incomes are not
 * categorized. The primary label is the income's free-text description
 * ("Salario", "Regalo"), with the destination source label + relative date
 * underneath.
 *
 * @property description Income description, e.g. "Salario", "Regalo".
 * @property formattedAmount Pre-formatted amount string (e.g. "5,000.00 CUP").
 * @property sourceLabel "Efectivo", "Banco BPA", or stash name — the destination.
 * @property relativeDate Spanish relative date (e.g. "hace 2 días").
 */
data class RecentIncomeItem(
    val description: String,
    val formattedAmount: String,
    val sourceLabel: String,
    val relativeDate: String,
)