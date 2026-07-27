package com.vida.feature.walletmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.Wallet
import com.vida.domain.usecase.currency.ListCurrencies
import com.vida.domain.usecase.wallet.DeleteWallet
import com.vida.domain.usecase.wallet.GetWallet
import com.vida.domain.usecase.wallet.GetWalletBalance
import com.vida.domain.usecase.wallet.ListWallets
import com.vida.domain.usecase.wallet.UpdateWallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the wallet list screen.
 *
 * On init, loads all wallets via [ListWallets], computes balance for each via
 * [GetWalletBalance], and emits [WalletListUiState].
 *
 * State transitions:
 * ```
 * Loading → Ready(wallets)  // wallets exist
 * Loading → Empty            // no wallets
 * Loading → Error(message)   // any exception
 * Ready   → Ready(wallets)   // after successful mutation + refetch
 * ```
 */
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val listWallets: ListWallets,
    private val deleteWallet: DeleteWallet,
    private val updateWallet: UpdateWallet,
    private val getWallet: GetWallet,
    private val getWalletBalance: GetWalletBalance,
    private val listCurrencies: ListCurrencies,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletListUiState>(WalletListUiState.Loading)
    val uiState: StateFlow<WalletListUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<WalletNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    /** True while an add/edit operation is in-flight (prevents double-tap). */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Reactive list of currency codes for the wallet edit dialog dropdown. */
    val currencyCodes: StateFlow<List<String>> = listCurrencies()
        .map { currencies -> currencies.map { it.code } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadWallets()
    }

    // ── Public actions ───────────────────────────────────────────────────────

    /**
     * Adds a new wallet with [name], [currencyCode], and optional [balanceMinor].
     *
     * Creates a [Wallet] with id=0 (upsert generates the real id) and calls
     * [updateWallet]. On success the list is refetched and [WalletNavEvent.SaveSuccess]
     * is emitted.
     */
    fun onAdd(name: String, currencyCode: String, balanceMinor: Long = 0L) {
        if (_isSaving.value) return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (trimmed.length > 100) return

        val currency = Currency.fromCode(currencyCode)

        viewModelScope.launch {
            _isSaving.value = true
            try {
                updateWallet(Wallet(id = 0L, name = trimmed, currency = currencyCode,
                    balance = Money(java.math.BigDecimal(balanceMinor).divide(java.math.BigDecimal(100), 2, java.math.RoundingMode.HALF_EVEN), currency)))
                loadWallets()
                _navEvents.send(WalletNavEvent.SaveSuccess)
                _navEvents.send(WalletNavEvent.ShowToast("Billetera agregada"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    WalletNavEvent.ShowToast(
                        t.message ?: "No se pudo agregar la billetera",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Updates an existing wallet identified by [id] with new [name], [currencyCode],
     * and optional [balanceMinor].
     *
     * On success the list is refetched and [WalletNavEvent.SaveSuccess] is emitted.
     */
    fun onEdit(id: Long, name: String, currencyCode: String, balanceMinor: Long = 0L) {
        if (_isSaving.value) return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (trimmed.length > 100) return

        val currency = Currency.fromCode(currencyCode)

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val existing = getWallet(id) ?: run {
                    _navEvents.send(WalletNavEvent.ShowToast("Billetera no encontrada"))
                    return@launch
                }
                updateWallet(existing.copy(name = trimmed, currency = currencyCode,
                    balance = Money(java.math.BigDecimal(balanceMinor).divide(java.math.BigDecimal(100), 2, java.math.RoundingMode.HALF_EVEN), currency)))
                loadWallets()
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
     * Deletes the wallet with [id].
     *
     * On success, the list is refetched and a success toast is emitted.
     */
    fun onDelete(id: Long) {
        viewModelScope.launch {
            try {
                deleteWallet(id)
                loadWallets()
                _navEvents.send(WalletNavEvent.ShowToast("Billetera eliminada"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    WalletNavEvent.ShowToast(
                        t.message ?: "No se pudo eliminar la billetera",
                    ),
                )
            }
        }
    }

    /** Re-initiates the wallet fetch from the [WalletListUiState.Error] state. */
    fun onDismissError() {
        loadWallets()
    }

    /**
     * Forces a re-fetch of the wallet list and per-wallet balances without
     * going through [WalletListUiState.Loading]. Used by
     * [com.vida.app.ui.FuentesScreen] to refresh balances after a transfer is
     * recorded from another ViewModel — Room's reactive observation should
     * already cover this, but the explicit refresh is a safety net while the
     * reactive chain is being validated.
     */
    fun refresh() {
        loadWallets(showLoading = false)
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Fetches wallets, computes balance for each in parallel, and emits the
     * appropriate [WalletListUiState] ([Ready], [Empty], or [Error]).
     *
     * @param showLoading When true (default), the state is briefly set to
     *   [WalletListUiState.Loading] before the subscription starts. The
     *   [refresh] method passes `false` to avoid a visible loading flash when
     *   re-fetching after an external mutation (e.g. a recorded transfer).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadWallets(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = WalletListUiState.Loading
            }
            try {
                listWallets().flatMapLatest { wallets ->
                    if (wallets.isEmpty()) {
                        flow { emit(emptyList<WalletDisplayItem>()) }
                    } else {
                        combineWalletBalances(wallets)
                    }
                }.collect { items ->
                    _uiState.value = if (items.isEmpty()) {
                        WalletListUiState.Empty
                    } else {
                        WalletListUiState.Ready(wallets = items)
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = WalletListUiState.Error(
                    message = t.message ?: "No se pudieron cargar las billeteras",
                )
            }
        }
    }

    /**
     * Combines balance flows for all wallets into a single flow of display items.
     *
     * Each wallet's balance is observed reactively via [GetWalletBalance]; the
     * `combine` re-emits whenever any individual balance flow emits, so the UI
     * stays in sync after a transfer or expense without manual refetch.
     */
    private fun combineWalletBalances(wallets: List<Wallet>) =
        combine(
            wallets.map { wallet ->
                getWalletBalance(wallet.id)
                    .map { balance -> wallet.toDisplayItem(balance) }
                    .catch { emit(wallet.toDisplayItemError()) }
            },
        ) { it.toList() }

    /**
     * Maps a domain [Wallet] to a pre-formatted [WalletDisplayItem].
     */
    private fun Wallet.toDisplayItem(balance: com.vida.domain.model.Money): WalletDisplayItem {
        val numberFormat = NumberFormat.getNumberInstance(Locale.US)
        numberFormat.minimumFractionDigits = 2
        numberFormat.maximumFractionDigits = 2
        val formattedAmount = numberFormat.format(balance.amount)
        return WalletDisplayItem(
            id = id,
            name = name,
            currencyCode = this.currency,
            balanceFormatted = formattedAmount,
            balance = balance,
            currency = this.currency,
        )
    }

    /**
     * Maps a domain [Wallet] to a [WalletDisplayItem] with an error-indicating balance.
     */
    private fun Wallet.toDisplayItemError(): WalletDisplayItem {
        return WalletDisplayItem(
            id = id,
            name = name,
            currencyCode = this.currency,
            balanceFormatted = "—",
            balance = balance,
            currency = this.currency,
        )
    }
}
