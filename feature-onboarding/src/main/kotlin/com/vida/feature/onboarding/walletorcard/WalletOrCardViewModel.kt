package com.vida.feature.onboarding.walletorcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.Wallet
import com.vida.domain.usecase.card.AddCard
import com.vida.domain.usecase.wallet.UpdateWallet
import com.vida.feature.onboarding.OnboardingCopy
import com.vida.feature.onboarding.preferences.WizardPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * ViewModel for the wallet-or-card middle step. Holds the segmented chooser
 * state plus the active form's field strings, and exposes callbacks for each
 * field mutation.
 *
 * Submission:
 * - [submitWallet] (T-WIZ-007): validates name + balance, calls
 *   [UpdateWallet] with `id = 0L` (upsert) and the parsed minor-units
 *   balance, then emits `Saved + Continue`. On validation failure sets the
 *   inline error field; on use-case failure emits a [WalletOrCardNavEvent.Snackbar].
 * - [submitCard] (T-WIZ-008): same shape, calls [AddCard] with the locked
 *   defaults (`expirationDate = now+4y`, `first6 = "000000"`, `type = DEBIT`,
 *   `note = ""`). Skeleton until T-WIZ-008.
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

    // ── Submission ──────────────────────────────────────────────────────────

    /**
     * Validates the wallet form, then calls [UpdateWallet] with `id = 0L`
     * (the singleton-upsert contract — see [UpdateWallet.invoke]).
     */
    fun submitWallet() {
        val s = _uiState.value as? WalletOrCardUiState.EditingWallet ?: return
        val name = s.name.trim()
        val nameError: String? = when {
            name.isEmpty() -> OnboardingCopy.WOC_ERR_NAME_BLANK
            name.length > 100 -> OnboardingCopy.WOC_ERR_NAME_LONG
            else -> null
        }
        if (nameError != null) {
            _uiState.value = s.copy(nameError = nameError)
            return
        }
        val balanceMinor = parseBalanceMinor(s.balance)
        if (balanceMinor == null) {
            _uiState.value = s.copy(balanceError = OnboardingCopy.WOC_ERR_BALANCE_PARSE)
            return
        }
        viewModelScope.launch {
            _uiState.value = WalletOrCardUiState.SavingWallet
            try {
                updateWallet(
                    Wallet(
                        id = 0L,
                        currency = s.currency,
                        name = name,
                        balance = Money.fromMinorUnits(balanceMinor, s.currency),
                    ),
                )
                _uiState.value = WalletOrCardUiState.Saved
                _navEvents.send(WalletOrCardNavEvent.Continue)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = WalletOrCardUiState.EditingWallet(
                    name = s.name,
                    currency = s.currency,
                    balance = s.balance,
                    nameError = t.message,
                )
                _navEvents.send(
                    WalletOrCardNavEvent.Snackbar(
                        t.message ?: "No se pudo guardar la billetera",
                    ),
                )
            }
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

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Parse a decimal balance string into minor units (2-decimal fixed-point).
     * Empty string is treated as zero. Returns `null` if the input is not a
     * valid decimal — the caller maps that to `WOC_ERR_BALANCE_PARSE`.
     */
    private fun parseBalanceMinor(text: String): Long? {
        if (text.isBlank()) return 0L
        return try {
            BigDecimal(text.trim()).movePointRight(2).longValueExact()
        } catch (_: NumberFormatException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }
}
