package com.vida.feature.cardmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Card
import com.vida.domain.model.CardNumber
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import com.vida.domain.usecase.card.AddCard
import com.vida.domain.usecase.card.DeleteCard
import com.vida.domain.usecase.card.GetCard
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.card.UpdateCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for the card list screen.
 *
 * On init, loads all cards via [ListCards], sorts them by bank name
 * alphabetically, and emits [CardListUiState].
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

    init {
        loadCards()
    }

    // ── Public actions ───────────────────────────────────────────────────────

    /**
     * Deletes the card with [id].
     *
     * On success, the list is refetched and a success toast is emitted.
     * On error, the current list is preserved and an error toast is shown.
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
                loadCards()
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
     * On success the list is refetched and [CardNavEvent.SaveSuccess] is emitted
     * (which closes the dialog). On error a toast is shown and the list is preserved.
     */
    fun onAdd(
        bank: String,
        first6: String,
        last4: String,
        type: CardType,
        currency: Currency,
        expiry: LocalDate,
        note: String?,
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

        val card = Card(
            number = number,
            bank = bank.trim(),
            type = type,
            currency = currency,
            expirationDate = expiry,
            note = trimmedNote,
        )

        viewModelScope.launch {
            _isSaving.value = true
            try {
                addCard(card)
                loadCards()
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
     */
    fun onEdit(
        id: Long,
        bank: String,
        first6: String,
        last4: String,
        type: CardType,
        currency: Currency,
        expiry: LocalDate,
        note: String?,
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
        )

        viewModelScope.launch {
            _isSaving.value = true
            try {
                updateCard(card)
                loadCards()
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

    /** Re-initiates the card fetch from the [Error] state. */
    fun onDismissError() {
        loadCards()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Fetches cards, sorts them by bank name alphabetically, and emits the
     * appropriate [CardListUiState] ([Ready], [Empty], or [Error]).
     */
    private fun loadCards() {
        viewModelScope.launch {
            try {
                val cards = listCards().first()
                val sorted = cards.sortedBy { it.bank.lowercase() }
                val items = sorted.map { it.toDisplayItem() }

                _uiState.value = if (items.isEmpty()) {
                    CardListUiState.Empty
                } else {
                    CardListUiState.Ready(cards = items)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = CardListUiState.Error(
                    message = t.message ?: "No se pudieron cargar las tarjetas",
                )
            }
        }
    }

    /**
     * Maps a domain [Card] to a pre-formatted [CardDisplayItem].
     *
     * - formattedNumber: "••••" + last 4 of masked number.
     * - first6: first 6 digits from masked number (for edit pre-population).
     * - last4: last 4 digits from masked number.
     * - expiry: raw [LocalDate] (for edit pre-population).
     * - expiryFormatted: "MM/YY" from [Card.expirationDate].
     */
    private fun Card.toDisplayItem(): CardDisplayItem {
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
        )
    }
}
