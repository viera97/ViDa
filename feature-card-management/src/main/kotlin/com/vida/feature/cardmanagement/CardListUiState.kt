package com.vida.feature.cardmanagement

import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import java.time.LocalDate

/**
 * UI state for the card list screen.
 *
 * Transitions:
 * ```
 * Loading → Ready | Empty | Error
 * Ready   → Ready (after mutation + refetch)
 * Error   → Loading → ... (on retry)
 * ```
 */
sealed interface CardListUiState {

    /** Emitted while [com.vida.domain.usecase.card.ListCards] is in-flight. */
    data object Loading : CardListUiState

    /** Cards loaded and sorted (by bank name). */
    data class Ready(val cards: List<CardDisplayItem>) : CardListUiState

    /** No cards exist in the database. */
    data object Empty : CardListUiState

    /** Initial load failed. Retry available via [CardListViewModel.onDismissError]. */
    data class Error(val message: String) : CardListUiState
}

/**
 * Pre-formatted display item for a single card row.
 *
 * @property id Card row id.
 * @property formattedNumber Masked number like "••••3456".
 * @property first6 First 6 digits (for edit pre-population).
 * @property last4 Last 4 digits (for edit pre-population).
 * @property bank Bank name.
 * @property type Card type (DEBIT, CREDIT, PREPAID).
 * @property currency Currency enum for badge rendering.
 * @property expiryFormatted Expiry date as "MM/YY".
 * @property expiry Raw expiry [LocalDate] (for edit pre-population).
 * @property note Optional card note.
 * @property balanceFormatted Formatted balance (e.g. "$ 1,250.50") or "—" on error.
 * @property balance Raw stored [Money] balance. Used by edit dialogs to
 *   pre-populate the input field without parsing the locale-aware formatted string.
 */
data class CardDisplayItem(
    val id: Long,
    val formattedNumber: String,
    val first6: String,
    val last4: String,
    val bank: String,
    val type: CardType,
    val currency: Currency,
    val expiryFormatted: String,
    val expiry: LocalDate,
    val note: String?,
    val balanceFormatted: String = "—",
    val balance: Money = Money.ZERO_CUP,
)

/**
 * One-shot navigation events emitted by [CardListViewModel].
 *
 * Consumed via [kotlinx.coroutines.channels.Channel] and observed in the
 * composable with [androidx.compose.runtime.LaunchedEffect].
 */
sealed class CardNavEvent {
    /** Show a transient toast / snackbar message. */
    data class ShowToast(val message: String) : CardNavEvent()

    /** Emitted after an add or edit operation completes successfully. */
    data object SaveSuccess : CardNavEvent()
}
