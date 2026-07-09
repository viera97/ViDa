package com.vida.feature.onboarding.walletorcard

import com.vida.domain.model.Currency

/**
 * UI state for the wallet wizard step. The state machine models:
 *
 * - [Editing] — user is filling the form.
 *   Carries the field strings plus nullable inline error keys.
 * - [Saving] — submit is in-flight; submit button disabled.
 * - [Saved] — use case returned successfully; the screen emits [WalletOrCardNavEvent.Continue].
 * - [Failed] — use case threw. The screen restores editing with a snackbar error.
 */
sealed interface WalletOrCardUiState {
    data class Editing(
        val name: String = "",
        val currency: Currency = Currency.CUP,
        val balance: String = "",
        val nameError: String? = null,
        val balanceError: String? = null,
    ) : WalletOrCardUiState

    data object Saving : WalletOrCardUiState
    data object Saved : WalletOrCardUiState
    data class Failed(val message: String) : WalletOrCardUiState
}

/**
 * One-shot navigation events emitted by [WalletOrCardViewModel]. Surfaced via
 * a buffered Channel so the screen can react without holding a reference to
 * the NavController.
 */
sealed interface WalletOrCardNavEvent {
    data object Continue : WalletOrCardNavEvent
    data class Snackbar(val message: String) : WalletOrCardNavEvent
}
