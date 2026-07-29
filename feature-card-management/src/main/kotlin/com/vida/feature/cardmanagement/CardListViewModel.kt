package com.vida.feature.cardmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Card
import com.vida.domain.model.CardNumber
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.usecase.bank.ListBanks
import com.vida.domain.usecase.card.AddCard
import com.vida.domain.usecase.card.DeleteCard
import com.vida.domain.usecase.card.GetCard
import com.vida.domain.usecase.card.GetCardBalance
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.card.UpdateCard
import com.vida.domain.usecase.currency.ListCurrencies
import com.vida.feature.cardmanagement.cache.CardListCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the card list screen.
 *
 * Subscribes to the reactive [ListCards] flow and keeps the available cards
 * list in sync with the database — additions, deletions, and edits made
 * elsewhere in the app (e.g. another screen or another ViewModel instance)
 * are reflected in the UI immediately, no restart required.
 *
 * Mutations ([onAdd], [onEdit], [onDelete]) no longer need to manually
 * re-fetch the list: Room's reactive Flow auto-emits when the underlying
 * table changes, and [observeCards] updates the [uiState] accordingly.
 *
 * Exposes one-shot [CardNavEvent]s via a [Channel] for transient messages
 * (toasts, snackbars).
 */
@HiltViewModel
class CardListViewModel @Inject constructor(
    private val listCards: ListCards,
    private val addCard: AddCard,
    private val updateCard: UpdateCard,
    private val deleteCard: DeleteCard,
    private val getCard: GetCard,
    private val getCardBalance: GetCardBalance,
    private val listBanks: ListBanks,
    private val listCurrencies: ListCurrencies,
    private val cardListCache: CardListCache,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CardListUiState>(CardListUiState.Loading)
    val uiState: StateFlow<CardListUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<CardNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    /** True while a delete operation is in-flight (prevents double-tap). */
    private var isDeleting = false

    /** True while an add/edit operation is in-flight (prevents double-tap). */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Reactive list of available bank names for the card form dropdown. */
    val bankNames: StateFlow<List<String>> = listBanks()
        .map { banks -> banks.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Reactive list of currency codes for the card form dropdown. */
    val currencyCodes: StateFlow<List<String>> = listCurrencies()
        .map { currencies -> currencies.map { it.code } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Tracks the active card-observation coroutine so [observeCards] can replace it. */
    private var sourceObservationJob: Job? = null

    init {
        // Show cached data instantly, then start the reactive pipeline.
        // Ensures the Fuentes screen renders immediately on cold start.
        cardListCache.load()?.let { cached ->
            _uiState.value = CardListUiState.Ready(cards = cached)
        }
        observeCards()
    }

    // ── Public actions ───────────────────────────────────────────────────────

    /**
     * Deletes the card with [id].
     *
     * On success, the database change triggers the reactive [observeCards]
     * subscription which updates the list automatically.
     * On error, an error toast is shown.
     */
    fun onDelete(id: Long) {
        if (isDeleting) return

        val current = _uiState.value
        if (current !is CardListUiState.Ready) return

        val item = current.cards.find { it.id == id } ?: return

        viewModelScope.launch {
            isDeleting = true
            try {
                deleteCard(id)
                // Refresh the observation — in production, Room's reactive
                // Flow would already have emitted; this is a safety net for
                // tests using one-shot `flowOf` mocks and matches the
                // pre-reactive behavior for callers that expect synchronous
                // post-mutation refresh.
                observeCards(showLoading = false)
                _navEvents.send(
                    CardNavEvent.ShowToast("Tarjeta eliminada"),
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    CardNavEvent.ShowToast(
                        t.message ?: "No se pudo eliminar la tarjeta",
                    ),
                )
            } finally {
                isDeleting = false
            }
        }
    }

    /**
     * Adds a new card with the given field values.
     *
     * Validation occurs here before the domain use case is invoked.
     * On success the database change triggers the reactive [observeCards]
     * subscription which updates the list automatically, and
     * [CardNavEvent.SaveSuccess] is emitted (which closes the dialog).
     * On error a toast is shown.
     */
    fun onAdd(
        bank: String,
        first6: String,
        last4: String,
        type: CardType,
        currency: String,
        expiry: LocalDate,
        note: String?,
        balanceMinor: Long = 0L,
    ) {
        if (isSaving.value) return
        if (bank.isBlank()) return

        val number = try {
            CardNumber.fromFirst6Last4(first6, last4)
        } catch (_: IllegalArgumentException) {
            return
        }

        val trimmedNote = note?.trim()?.ifBlank { null }
        if (trimmedNote != null && trimmedNote.length > 200) return

        val currencyEnum = Currency.values().firstOrNull { it.code.equals(currency, ignoreCase = true) } ?: Currency.CUP
        val card = Card(
            number = number,
            bank = bank.trim(),
            type = type,
            currency = currency,
            expirationDate = expiry,
            note = trimmedNote,
            balance = Money(java.math.BigDecimal(balanceMinor).divide(java.math.BigDecimal(100), 2, java.math.RoundingMode.HALF_EVEN), currencyEnum),
        )

        viewModelScope.launch {
            _isSaving.value = true
            try {
                addCard(card)
                observeCards(showLoading = false)
                _navEvents.send(CardNavEvent.SaveSuccess)
                _navEvents.send(CardNavEvent.ShowToast("Tarjeta agregada"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    CardNavEvent.ShowToast(
                        t.message ?: "No se pudo agregar la tarjeta",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Updates an existing card with the given field values.
     *
     * Mirrors [onAdd] but reuses the card's existing [id] via [UpdateCard].
     * The reactive [observeCards] subscription updates the list automatically
     * on success.
     */
    fun onEdit(
        id: Long,
        bank: String,
        first6: String,
        last4: String,
        type: CardType,
        currency: String,
        expiry: LocalDate,
        note: String?,
        balanceMinor: Long = 0L,
    ) {
        if (isSaving.value) return
        if (bank.isBlank()) return

        val number = try {
            CardNumber.fromFirst6Last4(first6, last4)
        } catch (_: IllegalArgumentException) {
            return
        }

        val card = Card(
            id = id,
            number = number,
            bank = bank.trim(),
            type = type,
            currency = currency,
            expirationDate = expiry,
            note = note?.trim()?.ifBlank { null },
            balance = Money(java.math.BigDecimal(balanceMinor).divide(java.math.BigDecimal(100), 2, java.math.RoundingMode.HALF_EVEN), Currency.values().firstOrNull { it.code.equals(currency, ignoreCase = true) } ?: Currency.CUP),
        )

        viewModelScope.launch {
            _isSaving.value = true
            try {
                updateCard(card)
                observeCards(showLoading = false)
                _navEvents.send(CardNavEvent.SaveSuccess)
                _navEvents.send(CardNavEvent.ShowToast("Tarjeta actualizada"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    CardNavEvent.ShowToast(
                        t.message ?: "No se pudo actualizar la tarjeta",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** Re-initiates the card subscription from the [CardListUiState.Error] state. */
    fun onDismissError() {
        observeCards()
    }

    /**
     * Forces a re-fetch of the card list and per-card balances without going
     * through [CardListUiState.Loading]. Used by [com.vida.app.ui.FuentesScreen]
     * to refresh balances after a transfer is recorded from another ViewModel —
     * Room's reactive observation should already cover this, but the explicit
     * refresh is a safety net while the reactive chain is being validated.
     */
    fun refresh() {
        observeCards(showLoading = false)
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Subscribes to [listCards] and keeps [uiState] in sync with the latest
     * card list **and** each card's reactive balance. Replaces any previous
     * observation job so that [onDismissError] can restart the subscription
     * cleanly.
     *
     * The reactive shape is `listCards().flatMapLatest { combine(balances) }`:
     * whenever the source list changes OR any per-card balance flow emits
     * (e.g. after a transfer), the [combine] recomputes the display items and
     * pushes a fresh [CardListUiState.Ready] / [CardListUiState.Empty].
     *
     * - Empty list → [CardListUiState.Empty].
     * - Non-empty → [CardListUiState.Ready] with display items sorted by bank.
     * - Any exception → [CardListUiState.Error].
     *
     * @param showLoading When true (default), the state is briefly set to
     *   [CardListUiState.Loading] before the subscription starts. Mutation
     *   methods ([onAdd], [onEdit], [onDelete]) pass `false` to avoid a
     *   visible loading flash while keeping the subscription in sync with
     *   the database change they just performed.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeCards(showLoading: Boolean = true) {
        sourceObservationJob?.cancel()
        sourceObservationJob = viewModelScope.launch {
            if (showLoading) {
                // Don't flash Loading over cached data — the user sees stale
                // data only until the reactive pipeline emits fresh results.
                if (_uiState.value !is CardListUiState.Ready) {
                    _uiState.value = CardListUiState.Loading
                }
            }
            listCards()
                .flatMapLatest { cards ->
                    if (cards.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        combine(
                            cards
                                .sortedBy { it.bank.lowercase() }
                                .map { card ->
                                    getCardBalance(card.id)
                                        .map { balance -> card.toDisplayItem(balance) }
                                        .catch { emit(card.toDisplayItemError()) }
                                },
                        ) { it.toList() }
                    }
                }
                .catch { t ->
                    if (t is CancellationException) throw t
                    _uiState.value = CardListUiState.Error(
                        message = t.message ?: "No se pudieron cargar las tarjetas",
                    )
                }
                .collect { items ->
                    val state = if (items.isEmpty()) {
                        CardListUiState.Empty
                    } else {
                        CardListUiState.Ready(cards = items)
                    }
                    _uiState.value = state
                    // Update cache — Ready persists, Empty clears stale data.
                    when (state) {
                        is CardListUiState.Ready -> cardListCache.save(state.cards)
                        is CardListUiState.Empty -> cardListCache.clear()
                        is CardListUiState.Loading,
                        is CardListUiState.Error -> { /* no-op */ }
                    }
                }
        }
    }

    /**
     * Maps a domain [Card] to a pre-formatted [CardDisplayItem], formatting
     * [balance] via the locale-aware [NumberFormat] so the UI does not need
     * to re-format.
     *
     * - formattedNumber: "••••" + last 4 of masked number.
     * - first6: first 6 digits from masked number (for edit pre-population).
     * - last4: last 4 digits from masked number.
     * - expiry: raw [LocalDate] (for edit pre-population).
     * - expiryFormatted: "MM/YY" from [Card.expirationDate].
     * - balanceFormatted: formatted balance, or "—" on error.
     */
    private fun Card.toDisplayItem(balance: Money): CardDisplayItem {
        val first6 = number.masked.substring(0, 6)
        val last4 = number.masked.substring(12, 16)
        val month = expirationDate.monthValue.toString().padStart(2, '0')
        val year = expirationDate.year.toString().takeLast(2)

        val numberFormat = NumberFormat.getNumberInstance(Locale.US)
        numberFormat.minimumFractionDigits = 2
        numberFormat.maximumFractionDigits = 2
        val balanceFormatted = "${balance.currency.symbol} ${numberFormat.format(balance.amount)}"

        return CardDisplayItem(
            id = id,
            formattedNumber = "••••$last4",
            first6 = first6,
            last4 = last4,
            bank = bank,
            type = type,
            currency = currency,
            expiryFormatted = "$month/$year",
            expiry = expirationDate,
            note = note,
            balanceFormatted = balanceFormatted,
            balance = balance,
        )
    }

    /**
     * Maps a domain [Card] to a [CardDisplayItem] with an error-indicating balance.
     */
    private fun Card.toDisplayItemError(): CardDisplayItem {
        val first6 = number.masked.substring(0, 6)
        val last4 = number.masked.substring(12, 16)
        val month = expirationDate.monthValue.toString().padStart(2, '0')
        val year = expirationDate.year.toString().takeLast(2)

        return CardDisplayItem(
            id = id,
            formattedNumber = "••••$last4",
            first6 = first6,
            last4 = last4,
            bank = bank,
            type = type,
            currency = currency,
            expiryFormatted = "$month/$year",
            expiry = expirationDate,
            note = note,
            balanceFormatted = "${Currency.values().firstOrNull { it.code.equals(currency, ignoreCase = true) }?.symbol ?: currency} —",
            balance = balance,
        )
    }
}
