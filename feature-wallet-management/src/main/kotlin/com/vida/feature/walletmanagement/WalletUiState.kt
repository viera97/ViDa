package com.vida.feature.walletmanagement

import com.vida.domain.model.Currency
import com.vida.domain.model.Money

/**
 * UI state for the wallet list screen.
 *
 * Transitions:
 * ```
 * Loading → Ready | Empty | Error
 * Ready   → Ready (after mutation + refetch)
 * Error   → Loading → ... (on retry)
 * ```
 */
sealed interface WalletListUiState {

    /** Emitted while the initial fetch is in-flight. */
    data object Loading : WalletListUiState

    /** Wallets loaded with pre-formatted display items. */
    data class Ready(val wallets: List<WalletDisplayItem>) : WalletListUiState

    /** No wallets exist in the database. User gets an add affordance. */
    data object Empty : WalletListUiState

    /** Initial load failed. Retry available via [WalletViewModel.onDismissError]. */
    data class Error(val message: String) : WalletListUiState
}

/**
 * Pre-formatted display item for a single wallet row.
 *
 * @property id Wallet row id.
 * @property name Wallet name (e.g. "Billetera", "Mi Billetera").
 * @property currencyCode Currency code string ("CUP", "USD", "MLC") for badge rendering.
 * @property balanceFormatted Formatted balance (e.g. "$1,250.50").
 * @property balance Raw stored [Money] balance. Used by edit dialogs to
 *   pre-populate the input field without parsing the locale-aware formatted string.
 * @property currency Domain [Currency] enum for dialog pre-selection.
 */
data class WalletDisplayItem(
    val id: Long,
    val name: String,
    val currencyCode: String,
    val balanceFormatted: String,
    val balance: Money,
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

    /** Emitted after an add or edit operation completes successfully. */
    data object SaveSuccess : WalletNavEvent()
}
