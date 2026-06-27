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
 * When the inverse rate (e.g. CUP → USD for a USD → CUP card) also exists,
 * it is rendered as a sub-section of the same card. Both rows share provider
 * and date (they're created as a pair when the primary is added).
 *
 * @property id Row id of the PRIMARY rate.
 * @property fromCurrency The "from" side of the pair (preserved for edit reconstruction).
 * @property toCurrency The "to" side of the pair (preserved for edit reconstruction).
 * @property pairLabel Human-readable pair label (e.g. "USD → CUP").
 * @property rate Raw [BigDecimal] rate value (for edit reconstruction).
 * @property rateFormatted Formatted rate string (e.g. "120.50").
 * @property provider The source of this rate (e.g. "Manual", "Banco Central").
 * @property updatedAt Raw [Instant] (for edit reconstruction).
 * @property updatedAtFormatted Date as "dd/MM/yyyy".
 * @property inverse The matching inverse rate (to → from), if it exists in storage.
 */
data class RateDisplayItem(
    val id: Long,
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val pairLabel: String,
    val rate: BigDecimal,
    val rateFormatted: String,
    val provider: String,
    val updatedAt: Instant,
    val updatedAtFormatted: String,
    val inverse: InverseRateDisplay? = null,
)

/**
 * Sub-section of a [RateDisplayItem] card showing the inverse rate (e.g. for
 * a USD → CUP primary, the inverse is CUP → USD with rate = 1 / 120.50).
 *
 * @property id Row id of the inverse rate.
 * @property fromCurrency Inverse "from" (same as the primary's "to").
 * @property toCurrency Inverse "to" (same as the primary's "from").
 * @property rate Raw [BigDecimal] of the inverse rate.
 * @property rateFormatted Formatted inverse rate string (e.g. "0.0083").
 */
data class InverseRateDisplay(
    val id: Long,
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val rate: BigDecimal,
    val rateFormatted: String,
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

    /** Emitted when user tries to add a rate that already exists. */
    data object DuplicateRate : RateNavEvent()
}
