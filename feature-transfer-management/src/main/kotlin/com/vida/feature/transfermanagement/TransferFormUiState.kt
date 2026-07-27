package com.vida.feature.transfermanagement

import com.vida.domain.model.SourceType
import java.time.Instant

/**
 * UI state exposed by [TransferFormViewModel].
 *
 * State transitions:
 * ```
 * Idle → Ready | EmptySourceList | Error   (init load)
 * Ready → Saved                 (submit success)
 * Ready → Error                 (submit failure)
 * Error → Idle → Ready | EmptySourceList    (retry)
 * ```
 */
sealed interface TransferFormUiState {

    /** Initial state before first source load. Default value of the StateFlow. */
    data object Idle : TransferFormUiState

    /** Form is loaded and ready for user input. */
    data class Ready(
        val sources: List<TransferSourceItem>,
        val deSource: TransferSourceItem? = null,
        val aSource: TransferSourceItem? = null,
        val amount: String = "",
        val dateTime: Instant = Instant.now(),
        val note: String = "",
        val validationErrors: Map<String, String> = emptyMap(),
    ) : TransferFormUiState

    /** No sources exist — transfer cannot be created. Distinct from [Error]. */
    data object EmptySourceList : TransferFormUiState

    /** Transfer was recorded successfully. UI should navigate back. */
    data object Saved : TransferFormUiState

    /** An error occurred during source load or submission. */
    data class Error(val message: String) : TransferFormUiState
}

/**
 * A single source (wallet, card, or stash) as displayed in the transfer source pickers.
 *
 * Mirrors `com.vida.feature.expense.SourceItem` to keep the `:feature-transfer-management`
 * module independent (design decision B).
 *
 * @property id Entity id; the row id of the corresponding wallet/card/stash.
 * @property type Which kind of source this is.
 * @property name Display name (e.g. "Billetera", "Banco kubo", "Ahorro vacaciones").
 * @property currency The source's native currency.
 * @property icon Pre-computed icon character: "💰"(WALLET), "♠"(CARD), "💎"(STASH).
 * @property subtitle Optional supplementary text (e.g. masked card number).
 */
data class TransferSourceItem(
    val id: Long,
    val type: SourceType,
    val name: String,
    val currency: String,
    val icon: String,
    val subtitle: String? = null,
)

/**
 * One-shot navigation events emitted by [TransferFormViewModel] via a [Channel].
 *
 * The Screen composable collects these via [kotlinx.coroutines.flow.Flow] with
 * [kotlinx.coroutines.flow.receiveAsFlow] and triggers navigation accordingly.
 */
sealed class TransferFormNavEvent {
    /** Transfer saved successfully — navigate back to HomeScreen. */
    data object NavigateBack : TransferFormNavEvent()
}
