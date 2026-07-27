package com.vida.feature.currencymanagement.ui

/**
 * UI state for the currency list screen.
 *
 * Transitions:
 * ```
 * Loading → Ready | Empty | Error
 * Ready   → Ready (after mutation + refetch)
 * Error   → Loading → ... (on retry)
 * ```
 */
sealed interface CurrencyListUiState {

    /** Emitted while [com.vida.domain.usecase.currency.ListCurrencies] is in-flight. */
    data object Loading : CurrencyListUiState

    /** Currencies loaded and sorted. */
    data class Ready(val currencies: List<CurrencyDisplayItem>) : CurrencyListUiState

    /** No currencies exist in the database. */
    data object Empty : CurrencyListUiState

    /** Initial load failed. Retry available via [CurrencyListViewModel.onDismissError]. */
    data class Error(val message: String) : CurrencyListUiState
}

/**
 * Pre-formatted display item for a single currency row.
 *
 * @property id Currency row id.
 * @property name Display name.
 * @property code ISO 4217-like abbreviation (e.g. "CUP", "USD").
 * @property isSystem Whether this is a seeded system currency.
 */
data class CurrencyDisplayItem(
    val id: Long,
    val name: String,
    val code: String,
    val isSystem: Boolean,
)

/**
 * One-shot navigation events emitted by [CurrencyListViewModel].
 *
 * Consumed via [kotlinx.coroutines.channels.Channel] and observed in the
 * composable with [androidx.compose.runtime.LaunchedEffect].
 */
sealed class CurrencyNavEvent {
    /** Show a transient toast / snackbar message. */
    data class ShowToast(val message: String) : CurrencyNavEvent()

    /** Emitted after an add or edit operation completes successfully. */
    data object SaveSuccess : CurrencyNavEvent()
}
