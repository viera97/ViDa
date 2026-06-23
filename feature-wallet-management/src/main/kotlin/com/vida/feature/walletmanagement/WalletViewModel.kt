package com.vida.feature.walletmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Currency
import com.vida.domain.model.Expense
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Wallet
import com.vida.domain.usecase.expense.GetExpensesBySource
import com.vida.domain.usecase.wallet.GetWallet
import com.vida.domain.usecase.wallet.GetWalletBalance
import com.vida.domain.usecase.wallet.UpdateWallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the wallet management screen.
 *
 * The wallet is a singleton (id=1L). This screen shows the wallet info card
 * plus the last 5 wallet-sourced expenses. Edit is via an [AlertDialog] overlay.
 *
 * State transitions:
 * ```
 * Loading → Ready(wallet, expenses)  // wallet exists
 * Loading → WalletNotFound            // NoSuchElementException (first visit)
 * Loading → Error(message)            // any other exception
 * Ready   → Ready(wallet, expenses)   // after successful edit + refetch
 * ```
 */
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val getWallet: GetWallet,
    private val updateWallet: UpdateWallet,
    private val getExpensesBySource: GetExpensesBySource,
    private val getWalletBalance: GetWalletBalance,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<WalletNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    /** True while an edit operation is in-flight (prevents double-tap). */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadWallet()
    }

    // ── Public actions ───────────────────────────────────────────────────────

    /**
     * Updates the wallet name and/or currency.
     *
     * Validation:
     * - Blocked when [isSaving] is true (double-tap guard).
     * - Name trimmed length must be 1–100 (non-blank rejected).
     *
     * On success the wallet + expenses are refetched and [WalletNavEvent.SaveSuccess]
     * is emitted (which closes the dialog). On error a toast is shown and the
     * current state is preserved.
     */
    fun onEdit(name: String, currency: Currency) {
        if (_isSaving.value) return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (trimmed.length > 100) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                updateWallet(Wallet(id = 1L, name = trimmed, currency = currency))
                loadWallet()
                _navEvents.send(WalletNavEvent.SaveSuccess)
                _navEvents.send(WalletNavEvent.ShowToast("Billetera actualizada"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    WalletNavEvent.ShowToast(
                        t.message ?: "No se pudo actualizar la billetera",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Called from the [WalletUiState.WalletNotFound] state when the user taps
     * "Configurar billetera". The UI composable opens the edit dialog with
     * defaults (name = "Billetera", currency = CUP); the dialog save triggers
     * [onEdit] which upserts via [UpdateWallet].
     */
    fun onConfigureWallet() {
        // Dialog opening is managed by the composable, not the ViewModel.
        // This function exists for the UI to call as a semantic hook and for
        // testability (verify the action was triggered).
    }

    /** Re-initiates the wallet fetch from the [WalletUiState.Error] state. */
    fun onDismissError() {
        loadWallet()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Fetches the singleton wallet, its balance, and the last 5 wallet-sourced
     * expenses in parallel, then emits the appropriate [WalletUiState].
     *
     * - [NoSuchElementException] from [GetWallet] → [WalletUiState.WalletNotFound]
     * - Any other exception → [WalletUiState.Error]
     */
    private fun loadWallet() {
        viewModelScope.launch {
            _uiState.value = WalletUiState.Loading
            try {
                val wallet = getWallet()
                val now = Instant.now()

                val expensesDeferred = getExpensesBySource(SourceType.WALLET, null, now)
                val balanceDeferred = getWalletBalance(now)

                val expenses = expensesDeferred.first()
                    .sortedByDescending { it.dateTime }
                    .take(5)
                    .map { it.toDisplayItem() }

                val walletItem = wallet.toDisplayItem(balanceDeferred)

                _uiState.value = WalletUiState.Ready(
                    wallet = walletItem,
                    expenses = expenses,
                )
            } catch (e: NoSuchElementException) {
                _uiState.value = WalletUiState.WalletNotFound
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = WalletUiState.Error(
                    message = t.message ?: "No se pudo cargar la billetera",
                )
            }
        }
    }

    /**
     * Maps a domain [Wallet] to a pre-formatted [WalletDisplayItem].
     *
     * - [WalletDisplayItem.balanceFormatted] uses the currency symbol + grouped
     *   decimal formatting (e.g. "$1,250.50", "USD 1,250.50", "MLC 1,250.50").
     */
    private fun Wallet.toDisplayItem(balance: Money): WalletDisplayItem {
        val numberFormat = NumberFormat.getNumberInstance(Locale.US)
        numberFormat.minimumFractionDigits = 2
        numberFormat.maximumFractionDigits = 2
        val formattedAmount = numberFormat.format(balance.amount)
        return WalletDisplayItem(
            name = name,
            currencyCode = currency.code,
            balanceFormatted = "${currency.symbol} $formattedAmount",
            currency = currency,
        )
    }

    /**
     * Maps a domain [Expense] to a pre-formatted [ExpenseDisplayItem].
     *
     * - [ExpenseDisplayItem.categoryName] uses [Expense.description] as a
     *   pragmatic simplification (full category-name resolution would require a
     *   [com.vida.domain.repository.CategoryRepository] dependency).
     * - [ExpenseDisplayItem.amountFormatted] uses the currency symbol + amount
     *   (e.g. "$15.75", "USD 42.00", "MLC 100.00").
     * - [ExpenseDisplayItem.dateFormatted] is "dd/MM/yyyy" in system default zone.
     */
    private fun Expense.toDisplayItem(): ExpenseDisplayItem {
        val numberFormat = NumberFormat.getNumberInstance(Locale.US)
        numberFormat.minimumFractionDigits = 2
        numberFormat.maximumFractionDigits = 2
        val formattedAmount = "${amount.currency.symbol} ${numberFormat.format(amount.amount)}"

        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val zone = ZoneId.systemDefault()

        return ExpenseDisplayItem(
            id = id,
            categoryName = description,
            amountFormatted = formattedAmount,
            dateFormatted = dateFormatter.format(dateTime.atZone(zone)),
        )
    }
}
