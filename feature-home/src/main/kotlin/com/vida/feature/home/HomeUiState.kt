package com.vida.feature.home

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
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
     * @property rates Nullable — `null` means section is hidden (S3).
     */
    data class Ready(
        val totalBalance: Money,
        val perCurrencySubtotals: Map<Currency, Money>,
        val perSource: List<PerSource>,
        val recentExpenses: List<RecentExpenseItem>,
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
 */
data class PerSource(
    val label: String,
    val balance: Money,
    val formatted: String,
)

/**
 * A recent expense row in the Home dashboard.
 *
 * @property categoryName Category name, or "Sin categoría" if category lookup fails.
 * @property formattedAmount Pre-formatted amount string (e.g. "1,250.00 CUP").
 * @property sourceLabel "Billetera", "Kubo ···1234", or stash name.
 * @property relativeDate Spanish relative date (e.g. "hace 2 días").
 */
data class RecentExpenseItem(
    val categoryName: String,
    val formattedAmount: String,
    val sourceLabel: String,
    val relativeDate: String,
)