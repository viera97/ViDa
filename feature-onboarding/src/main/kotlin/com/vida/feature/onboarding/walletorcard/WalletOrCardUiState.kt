package com.vida.feature.onboarding.walletorcard

import com.vida.domain.model.Currency

/** Which form the middle step currently shows. Switched via the segmented chooser. */
enum class WizardSegment { WALLET, CARD }

/**
 * UI state for the wallet-or-card wizard step. The state machine models:
 *
 * - [EditingWallet] / [EditingCard] — user is filling the form.
 *   Each carries the field strings plus nullable inline error keys. Errors
 *   rendered next to the offending field, not as a snackbar.
 * - [SavingWallet] / [SavingCard] — submit is in-flight; submit button disabled.
 * - [Saved] — use case returned successfully; the screen emits `Continue`
 *   via [WalletOrCardNavEvent.Continue] and pops to the get-started step.
 * - [Failed] — use case threw. The screen restores the prior editing state
 *   with a single error message surfaced through [WalletOrCardNavEvent.Snackbar].
 */
sealed interface WalletOrCardUiState {
    data class EditingWallet(
        val name: String = "",
        val currency: Currency = Currency.CUP,
        val balance: String = "",
        val nameError: String? = null,
        val balanceError: String? = null,
    ) : WalletOrCardUiState

    data class EditingCard(
        val bank: String = "",
        val last4: String = "",
        val currency: Currency = Currency.CUP,
        val balance: String = "",
        val bankError: String? = null,
        val last4Error: String? = null,
        val balanceError: String? = null,
    ) : WalletOrCardUiState

    data object SavingWallet : WalletOrCardUiState
    data object SavingCard : WalletOrCardUiState
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
