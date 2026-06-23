package com.vida.feature.walletmanagement

import com.vida.domain.model.Currency

/**
 * UI state for the wallet management screen.
 *
 * Transitions:
 * ```
 * Loading → Ready | WalletNotFound | Error
 * Ready   → Ready (after mutation + refetch)
 * WalletNotFound → Ready (after upsert)
 * Error   → Loading → ... (on retry)
 * ```
 */
sealed interface WalletUiState {

    /** Emitted while the initial fetch is in-flight. */
    data object Loading : WalletUiState

    /** Wallet loaded with its info and the last 5 wallet-sourced expenses. */
    data class Ready(
        val wallet: WalletDisplayItem,
        val expenses: List<ExpenseDisplayItem>,
    ) : WalletUiState

    /**
     * No wallet row exists in the database — expected on first visit before
     * the seed expense is recorded. NOT an error. User gets an upsert affordance.
     */
    data object WalletNotFound : WalletUiState

    /** Initial load failed. Retry available via [WalletViewModel.onDismissError]. */
    data class Error(val message: String) : WalletUiState
}

/**
 * Pre-formatted display item for the wallet info card.
 *
 * @property name Wallet name (e.g. "Billetera", "Mi Billetera").
 * @property currencyCode Currency code string ("CUP", "USD", "MLC") for badge rendering.
 * @property balanceFormatted Formatted balance (e.g. "$1,250.50").
 * @property currency Domain [Currency] enum for dialog pre-selection.
 */
data class WalletDisplayItem(
    val name: String,
    val currencyCode: String,
    val balanceFormatted: String,
    val currency: Currency,
)

/**
 * Pre-formatted display item for a single expense row in the last-5 section.
 *
 * @property id Expense row id.
 * @property categoryName Category name (resolved from [com.vida.domain.model.Expense.description]
 *   as a pragmatic simplification; full category-name resolution would require
 *   an additional [com.vida.domain.repository.CategoryRepository] dependency).
 * @property amountFormatted Formatted amount (e.g. "$15.75", "USD 42.00", "MLC 100.00").
 * @property dateFormatted Date as "dd/MM/yyyy".
 */
data class ExpenseDisplayItem(
    val id: Long,
    val categoryName: String,
    val amountFormatted: String,
    val dateFormatted: String,
)

/**
 * One-shot navigation events emitted by [WalletViewModel].
 *
 * Consumed via [kotlinx.coroutines.channels.Channel] and observed in the
 * composable with [androidx.compose.runtime.LaunchedEffect].
 */
sealed class WalletNavEvent {
    /** Show a transient toast / snackbar message. */
    data class ShowToast(val message: String) : WalletNavEvent()

    /** Emitted after an edit operation completes successfully. */
    data object SaveSuccess : WalletNavEvent()
}
