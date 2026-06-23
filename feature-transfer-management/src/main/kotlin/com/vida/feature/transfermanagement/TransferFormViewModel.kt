package com.vida.feature.transfermanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Transfer
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.transfer.RecordTransfer
import com.vida.domain.usecase.wallet.GetWallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for the transfer creation form.
 *
 * Loads all available sources (wallet + cards + stashes) on init via
 * [GetWallet], [ListCards], and [ListStashes] (provided by HomeModule/ExpenseModule
 * in the same [dagger.hilt.android.components.ViewModelComponent]).
 *
 * Submits valid transfers via [RecordTransfer] (provided by [di.TransferModule]).
 *
 * State transitions:
 * ```
 * Idle → Ready | EmptySourceList | Error   (loadSources)
 * Ready → Saved      (submit success)
 * Ready → Error      (submit failure)
 * Error → Idle → Ready | EmptySourceList   (retry)
 * ```
 *
 * Mutual exclusion: selecting a source for "De" removes it from "A" options
 * and vice versa, preventing self-transfer at the UI level.
 *
 * Cross-currency validation: rejected before submit if De and A currencies differ.
 *
 * @property uiState The main form state exposed to the UI.
 * @property isSaving Guard for the submit button — prevents double-tap.
 * @property navEvents One-shot navigation events (e.g. navigate back after save).
 */
@HiltViewModel
class TransferFormViewModel @Inject constructor(
    private val recordTransfer: RecordTransfer,
    private val listCards: ListCards,
    private val listStashes: ListStashes,
    private val getWallet: GetWallet,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<TransferFormUiState>(TransferFormUiState.Idle)
    val uiState: StateFlow<TransferFormUiState> = _uiState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _navEvents = Channel<TransferFormNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    /** Cached last valid Ready state — used to recover form data from Error. */
    private var lastReadyState: TransferFormUiState.Ready? = null

    init {
        loadSources()
    }

    // ── Input handlers ──────────────────────────────────────────────────────

    /**
     * Selects the "De" (origin) source for the transfer.
     *
     * Mutual exclusion: if [source] is currently selected as "A", clears "A".
     */
    fun onDeSelected(source: TransferSourceItem) {
        updateReady { ready ->
            val newASource = if (ready.aSource == source) null else ready.aSource
            ready.copy(
                deSource = source,
                aSource = newASource,
                validationErrors = emptyMap(),
            )
        }
    }

    /**
     * Selects the "A" (destination) source for the transfer.
     *
     * Mutual exclusion: if [source] is currently selected as "De", clears "De".
     */
    fun onASelected(source: TransferSourceItem) {
        updateReady { ready ->
            val newDeSource = if (ready.deSource == source) null else ready.deSource
            ready.copy(
                aSource = source,
                deSource = newDeSource,
                validationErrors = emptyMap(),
            )
        }
    }

    /** Updates the raw amount string in the form. */
    fun onAmountChanged(value: String) {
        updateReady { it.copy(amount = value, validationErrors = emptyMap()) }
    }

    /** Overrides the transfer date/time. */
    fun onDateTimeChanged(instant: Instant) {
        updateReady { it.copy(dateTime = instant, validationErrors = emptyMap()) }
    }

    /** Updates the optional note text. */
    fun onNoteChanged(value: String) {
        updateReady { it.copy(note = value, validationErrors = emptyMap()) }
    }

    // ── Submit ──────────────────────────────────────────────────────────────

    /**
     * Validates the form and, if valid, calls [RecordTransfer] atomically.
     *
     * Guards:
     * - Returns early if [isSaving] is true (double-tap prevention).
     * - Returns early if [uiState] is not [TransferFormUiState.Ready].
     *
     * Validation:
     * - De and A sources must be selected.
     * - Amount must be a positive decimal.
     * - Note must be ≤ 500 characters.
     * - De and A must share the same currency.
     *
     * On validation failure: emits [TransferFormUiState.Ready] with
     * [TransferFormUiState.Ready.validationErrors].
     * On success: emits [TransferFormUiState.Saved] and navigates back.
     * On [RecordTransfer] failure: emits [TransferFormUiState.Error] while
     * preserving form data via [lastReadyState].
     */
    fun submit() {
        if (_isSaving.value) return
        val ready = _uiState.value as? TransferFormUiState.Ready ?: return

        val errors = mutableMapOf<String, String>()
        if (ready.deSource == null) errors["deSource"] = "Selecciona el origen"
        if (ready.aSource == null) errors["aSource"] = "Selecciona el destino"
        errors.putAll(validateAmount(ready.amount))
        if (ready.note.length > 500) errors["note"] = "Máximo 500 caracteres"
        if (ready.deSource != null && ready.aSource != null &&
            ready.deSource!!.currency != ready.aSource!!.currency
        ) {
            errors["currency"] = "Origen y destino deben usar la misma moneda"
        }

        if (errors.isNotEmpty()) {
            _uiState.value = ready.copy(validationErrors = errors)
            return
        }

        _isSaving.value = true

        viewModelScope.launch {
            try {
                val de = ready.deSource!!
                val a = ready.aSource!!
                val transfer = Transfer(
                    fromType = de.type,
                    fromId = de.id,
                    toType = a.type,
                    toId = a.id,
                    amount = Money(BigDecimal(ready.amount), de.currency),
                    dateTime = ready.dateTime,
                    note = ready.note.ifBlank { null },
                )
                recordTransfer(transfer)
                _uiState.value = TransferFormUiState.Saved
                _navEvents.send(TransferFormNavEvent.NavigateBack)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = TransferFormUiState.Error(
                    t.message ?: "Error al guardar la transferencia",
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    // ── Error recovery ──────────────────────────────────────────────────────

    /**
     * Re-loads all sources from the current [TransferFormUiState.Error] state.
     *
     * Resets the form entirely — no form data is preserved across retry.
     */
    fun retry() {
        loadSources()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Fetches wallet + cards + stashes and builds the source list.
     *
     * - [NoSuchElementException] from [GetWallet] is NOT treated as an error;
     *   it means no wallet has been seeded, which can be combined with empty
     *   cards/stashes to produce [TransferFormUiState.EmptySourceList].
     * - Any other exception during fetch → [TransferFormUiState.Error].
     * - If total sources == 0 → [TransferFormUiState.EmptySourceList].
     * - Otherwise → [TransferFormUiState.Ready] with the full source list.
     */
    private fun loadSources() {
        viewModelScope.launch {
            _uiState.value = TransferFormUiState.Idle
            try {
                val wallet = try {
                    getWallet()
                } catch (e: NoSuchElementException) {
                    null
                }
                val cards = listCards().first()
                val stashes = listStashes().first()

                val sources = buildList {
                    if (wallet != null) {
                        add(
                            TransferSourceItem(
                                id = null,
                                type = SourceType.WALLET,
                                name = "Billetera",
                                currency = wallet.currency,
                                icon = "\uD83D\uDCB0",
                            ),
                        )
                    }
                    for (card in cards) {
                        add(
                            TransferSourceItem(
                                id = card.id,
                                type = SourceType.CARD,
                                name = card.bank,
                                currency = card.currency,
                                icon = "\u2660\uFE0F",
                                subtitle = "···${card.number.masked.substring(12, 16)}",
                            ),
                        )
                    }
                    for (stash in stashes) {
                        add(
                            TransferSourceItem(
                                id = stash.id,
                                type = SourceType.STASH,
                                name = stash.name,
                                currency = stash.currency,
                                icon = "\uD83D\uDC8E",
                            ),
                        )
                    }
                }

                if (sources.isEmpty()) {
                    _uiState.value = TransferFormUiState.EmptySourceList
                } else {
                    val ready = TransferFormUiState.Ready(sources = sources)
                    lastReadyState = ready
                    _uiState.value = ready
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = TransferFormUiState.Error(
                    t.message ?: "Error al cargar el formulario",
                )
            }
        }
    }

    /**
     * Applies [transform] to the current [TransferFormUiState.Ready] and emits
     * the updated state.
     *
     * If the current state is [TransferFormUiState.Error], recovers from
     * [lastReadyState] so that filled form fields are preserved.
     * If the current state is anything else (e.g. Idle, Saving), this is a no-op.
     *
     * Updates [lastReadyState] on every successful transformation so that
     * Error→Ready recovery always uses the most recent valid form data.
     */
    private fun updateReady(
        transform: (TransferFormUiState.Ready) -> TransferFormUiState.Ready,
    ) {
        val current = _uiState.value
        val ready: TransferFormUiState.Ready = when (current) {
            is TransferFormUiState.Ready -> current
            is TransferFormUiState.Error -> lastReadyState ?: return
            else -> return
        }
        val updated = transform(ready)
        lastReadyState = updated
        _uiState.value = updated
    }

    /** Validates the amount string. Returns a (possibly empty) errors map. */
    private fun validateAmount(value: String): Map<String, String> {
        return when {
            value.isBlank() -> mapOf("amount" to "El importe es obligatorio")
            value.toBigDecimalOrNull() == null -> mapOf("amount" to "Importe inválido")
            value.toBigDecimal() <= BigDecimal.ZERO -> mapOf("amount" to "Debe ser mayor que 0")
            else -> emptyMap()
        }
    }
}
