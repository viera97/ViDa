package com.vida.feature.bankmanagement.ui

/**
 * UI state for the bank list screen.
 *
 * Transitions:
 * ```
 * Loading → Ready | Empty | Error
 * Ready   → Ready (after mutation + refetch)
 * Error   → Loading → ... (on retry)
 * ```
 */
sealed interface BankListUiState {

    /** Emitted while [com.vida.domain.usecase.bank.ListBanks] is in-flight. */
    data object Loading : BankListUiState

    /** Banks loaded and sorted. */
    data class Ready(val banks: List<BankDisplayItem>) : BankListUiState

    /** No banks exist in the database. */
    data object Empty : BankListUiState

    /** Initial load failed. Retry available via [BankListViewModel.onDismissError]. */
    data class Error(val message: String) : BankListUiState
}

/**
 * Pre-formatted display item for a single bank row.
 *
 * @property id Bank row id.
 * @property name Display name.
 * @property color ARGB color int for the color dot and card gradient.
 * @property isSystem Whether this is a seeded system bank.
 */
data class BankDisplayItem(
    val id: Long,
    val name: String,
    val color: Int,
    val isSystem: Boolean,
)

/**
 * One-shot navigation events emitted by [BankListViewModel].
 *
 * Consumed via [kotlinx.coroutines.channels.Channel] and observed in the
 * composable with [androidx.compose.runtime.LaunchedEffect].
 */
sealed class BankNavEvent {
    /** Show a transient toast / snackbar message. */
    data class ShowToast(val message: String) : BankNavEvent()

    /** Emitted after an add or edit operation completes successfully. */
    data object SaveSuccess : BankNavEvent()
}
