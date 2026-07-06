package com.vida.feature.onboarding.walletorcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Currency
import com.vida.domain.usecase.card.AddCard
import com.vida.domain.usecase.wallet.UpdateWallet
import com.vida.feature.onboarding.preferences.WizardPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the wallet-or-card middle step. Holds the segmented chooser
 * state plus the active form's field strings, and exposes callbacks for each
 * field mutation. The real persistence lands in T-WIZ-007 (wallet submit)
 * and T-WIZ-008 (card submit).
 *
 * In this skeleton the [submitWallet] / [submitCard] callbacks emit
 * [WalletOrCardUiState.Saved] and [WalletOrCardNavEvent.Continue] without
 * touching the database — only the contracts are wired.
 */
@HiltViewModel
class WalletOrCardViewModel @Inject constructor(
    private val updateWallet: UpdateWallet,
    private val addCard: AddCard,
    private val wizardPreferences: WizardPreferences,
) : ViewModel() {

    private val _segment = MutableStateFlow(WizardSegment.WALLET)
    val segment: StateFlow<WizardSegment> = _segment.asStateFlow()

    private val _uiState =
        MutableStateFlow<WalletOrCardUiState>(WalletOrCardUiState.EditingWallet())
    val uiState: StateFlow<WalletOrCardUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<WalletOrCardNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    // ── Field mutations ──────────────────────────────────────────────────────

    /** Switches the segmented chooser and rebuilds the editing sub-state with defaults. */
    fun onSegmentChange(segment: WizardSegment) {
        _segment.value = segment
        _uiState.value = when (segment) {
            WizardSegment.WALLET -> WalletOrCardUiState.EditingWallet()
            WizardSegment.CARD -> WalletOrCardUiState.EditingCard()
        }
    }

    fun onNameChange(value: String) {
        val current = _uiState.value as? WalletOrCardUiState.EditingWallet ?: return
        _uiState.value = current.copy(name = value, nameError = null)
    }

    fun onBankChange(value: String) {
        val current = _uiState.value as? WalletOrCardUiState.EditingCard ?: return
        _uiState.value = current.copy(bank = value, bankError = null)
    }

    fun onLast4Change(value: String) {
        val current = _uiState.value as? WalletOrCardUiState.EditingCard ?: return
        _uiState.value = current.copy(last4 = value, last4Error = null)
    }

    fun onCurrencyChange(value: Currency) {
        _uiState.value = when (val current = _uiState.value) {
            is WalletOrCardUiState.EditingWallet -> current.copy(currency = value, balanceError = null)
            is WalletOrCardUiState.EditingCard -> current.copy(currency = value, balanceError = null)
            else -> current
        }
    }

    fun onBalanceChange(value: String) {
        _uiState.value = when (val current = _uiState.value) {
            is WalletOrCardUiState.EditingWallet -> current.copy(balance = value, balanceError = null)
            is WalletOrCardUiState.EditingCard -> current.copy(balance = value, balanceError = null)
            else -> current
        }
    }

    // ── Submission (skeleton — real logic in T-WIZ-007 / T-WIZ-008) ───────────

    /** Skeleton submit — no persistence yet. Real flow lands in T-WIZ-007. */
    fun submitWallet() {
        viewModelScope.launch {
            _uiState.value = WalletOrCardUiState.SavingWallet
            _uiState.value = WalletOrCardUiState.Saved
            _navEvents.send(WalletOrCardNavEvent.Continue)
        }
    }

    /** Skeleton submit — no persistence yet. Real flow lands in T-WIZ-008. */
    fun submitCard() {
        viewModelScope.launch {
            _uiState.value = WalletOrCardUiState.SavingCard
            _uiState.value = WalletOrCardUiState.Saved
            _navEvents.send(WalletOrCardNavEvent.Continue)
        }
    }

    /** Marks the wizard as completed — used by hardware-back and "Saltar" affordances. */
    fun markCompleted() {
        viewModelScope.launch {
            try {
                wizardPreferences.setWizardCompleted(true)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                // Best-effort write; flags staying false just means the wizard
                // would re-fire on next launch, which is acceptable.
            }
        }
    }
}
