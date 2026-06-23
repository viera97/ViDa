package com.vida.feature.ratemanagement

import com.vida.domain.model.Currency
import java.math.BigDecimal
import java.time.Instant

/**
 * UI state for the currency rate list screen.
 *
 * Transitions:
 * ```
 * Loading → Ready | Empty | Error
 * Ready   → Ready (after mutation + refetch)
 * Error   → Loading → ... (on retry)
 * ```
 */
sealed interface RateListUiState {

    /** Emitted while [com.vida.domain.usecase.rate.ListCurrencyRates] is in-flight. */
    data object Loading : RateListUiState

    /** Rates loaded and sorted (by pair then updatedAt DESC). */
    data class Ready(val items: List<RateDisplayItem>) : RateListUiState

    /** No currency rates exist in the database. */
    data object Empty : RateListUiState

    /** Initial load failed. Retry available via [RateListViewModel.onDismissError]. */
    data class Error(val message: String) : RateListUiState
}

/**
 * Pre-formatted display item for a single currency rate row.
 *
 * @property id Row id.
 * @property fromCurrency The "from" side of the pair (preserved for edit reconstruction).
 * @property toCurrency The "to" side of the pair (preserved for edit reconstruction).
 * @property pairLabel Human-readable pair label (e.g. "CUP → USD").
 * @property rate Raw [BigDecimal] rate value (for edit reconstruction).
 * @property rateFormatted Formatted rate string (e.g. "120.50").
 * @property updatedAt Raw [Instant] (for edit reconstruction).
 * @property updatedAtFormatted Date as "dd/MM/yyyy".
 */
data class RateDisplayItem(
    val id: Long,
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val pairLabel: String,
    val rate: BigDecimal,
    val rateFormatted: String,
    val updatedAt: Instant,
    val updatedAtFormatted: String,
)

/**
 * One-shot navigation events emitted by [RateListViewModel].
 *
 * Consumed via [kotlinx.coroutines.channels.Channel] and observed in the
 * composable with [androidx.compose.runtime.LaunchedEffect].
 */
sealed class RateNavEvent {
    /** Show a transient toast / snackbar message. */
    data class ShowToast(val message: String) : RateNavEvent()

    /** Emitted after an add or edit operation completes successfully. */
    data object SaveSuccess : RateNavEvent()
}
