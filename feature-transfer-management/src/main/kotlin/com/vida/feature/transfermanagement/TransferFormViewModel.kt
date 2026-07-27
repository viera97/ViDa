package com.vida.feature.transfermanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Card
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Stash
import com.vida.domain.model.Transfer
import com.vida.domain.model.Wallet
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.transfer.RecordTransfer
import com.vida.domain.usecase.wallet.ListWallets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for the transfer creation form.
 *
 * Subscribes to the reactive [ListWallets], [ListCards], and [ListStashes]
 * flows and keeps the available sources list in sync — additions or deletions
 * made elsewhere in the app (e.g. a new card added from FuentesScreen) are
 * reflected in the form immediately, no restart required.
 *
 * Existing user selections are preserved across source updates as long as the
 * selected source still exists in the new list. If a selected source is
 * deleted, that selection is cleared silently.
 *
 * Submits valid transfers via [RecordTransfer] (provided by [di.TransferModule]).
 *
 * State transitions:
 * ```
 * Idle → Ready | EmptySourceList | Error   (observeSources)
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
    private val listWallets: ListWallets,
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

    /** Tracks the active source-observation coroutine so [retry] can replace it. */
    private var sourceObservationJob: Job? = null

    init {
        observeSources()
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
                    amount = Money(BigDecimal(ready.amount), Currency.fromCode(de.currency)),
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
     * Re-subscribes to the source flows and replaces any current state.
     *
     * Cancels any in-flight observation job and starts a fresh one. The form
     * data is reset — no fields are preserved across retry.
     */
    fun retry() {
        observeSources()
    }

    /**
     * Clears the form fields and restarts the source observation so the next
     * time the modal opens, the user starts with a fresh empty form.
     *
     * Behavior:
     * - If the current state is [TransferFormUiState.Ready], clears De/A
     *   selections, amount, note, and validation errors in-place. The
     *   current source list is preserved so the user does not see a loading
     *   spinner when the dialog is immediately reopened.
     * - Otherwise (Idle, Error, EmptySourceList, Saved), cancels the
     *   in-flight observation and starts a fresh one — this guarantees the
     *   dialog will receive the latest sources when reopened, even if the
     *   reactive flow has not emitted since the previous error.
     *
     * The reactive [observeSources] subscription picks up new emissions and
     * preserves the freshly-cleared form fields as long as selected sources
     * remain in the source list (which they will not — selections are null
     * after reset).
     */
    fun reset() {
        val currentReady = _uiState.value as? TransferFormUiState.Ready
        if (currentReady != null) {
            val fresh = currentReady.copy(
                deSource = null,
                aSource = null,
                amount = "",
                dateTime = Instant.now(),
                note = "",
                validationErrors = emptyMap(),
            )
            lastReadyState = fresh
            _uiState.value = fresh
        } else {
            lastReadyState = null
            // Re-trigger observation so the dialog does not show a stale
            // loading state when reopened.
            observeSources()
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Subscribes to [listWallets], [listCards], and [listStashes] flows and
     * keeps [uiState] in sync with the latest combined source list.
     *
     * Replaces any previous observation job so that [retry] can restart the
     * subscription cleanly. The first emission transitions the state from
     * [TransferFormUiState.Idle] to [TransferFormUiState.Ready] (or
     * [TransferFormUiState.EmptySourceList] / [TransferFormUiState.Error]).
     *
     * Any subsequent emission rebuilds the source list and preserves the
     * user's current selections as long as they still exist; selections whose
     * source has been deleted are cleared silently.
     */
    private fun observeSources() {
        sourceObservationJob?.cancel()
        sourceObservationJob = viewModelScope.launch {
            _uiState.value = TransferFormUiState.Idle
            // Wraps the combine() chain in try-catch so synchronous throws
            // from the use case providers (listWallets(), listCards(),
            // listStashes()) are also routed to Error. Flow's .catch only
            // covers errors during collection; throws from provider invoke
            // happen BEFORE .catch mounts and would otherwise leave the VM
            // stuck in Idle forever.
            try {
                combine(
                    listWallets(),
                    listCards(),
                    listStashes(),
                ) { wallets, cards, stashes ->
                    buildSourceList(wallets, cards, stashes)
                }
                    .catch { t ->
                        if (t is CancellationException) throw t
                        _uiState.value = TransferFormUiState.Error(
                            t.message ?: "Error al cargar el formulario",
                        )
                    }
                    .collect { sources ->
                        applySources(sources)
                    }
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                _uiState.value = TransferFormUiState.Error(
                    t.message ?: "Error al cargar el formulario",
                )
            }
        }
    }

    /**
     * Applies a fresh source list to [uiState], preserving user selections
     * (amount, note, datetime, validation errors) when they are still valid.
     *
     * - Empty [newSources] → [TransferFormUiState.EmptySourceList].
     * - Otherwise → [TransferFormUiState.Ready] with merged state.
     * - Selections whose source no longer exists are cleared silently.
     */
    private fun applySources(newSources: List<TransferSourceItem>) {
        if (newSources.isEmpty()) {
            lastReadyState = null
            _uiState.value = TransferFormUiState.EmptySourceList
            return
        }

        val currentReady = _uiState.value as? TransferFormUiState.Ready
        val preserved = lastReadyState

        // Preserve current user selections if the selected source still
        // exists in the new list; clear it otherwise.
        val preservedDe = (currentReady?.deSource ?: preserved?.deSource)?.takeIf { sel ->
            newSources.any { it.id == sel.id && it.type == sel.type }
        }
        val preservedA = (currentReady?.aSource ?: preserved?.aSource)?.takeIf { sel ->
            newSources.any { it.id == sel.id && it.type == sel.type }
        }

        val updated = TransferFormUiState.Ready(
            sources = newSources,
            deSource = preservedDe,
            aSource = preservedA,
            amount = currentReady?.amount ?: preserved?.amount ?: "",
            dateTime = currentReady?.dateTime ?: preserved?.dateTime ?: Instant.now(),
            note = currentReady?.note ?: preserved?.note ?: "",
            validationErrors = currentReady?.validationErrors
                ?: preserved?.validationErrors
                ?: emptyMap(),
        )
        lastReadyState = updated
        _uiState.value = updated
    }

    /**
     * Maps raw wallet/card/stash data into UI-ready [TransferSourceItem]s in
     * display order (wallets first, then cards, then stashes).
     */
    private fun buildSourceList(
        wallets: List<Wallet>,
        cards: List<Card>,
        stashes: List<Stash>,
    ): List<TransferSourceItem> = buildList {
        for (wallet in wallets) {
            add(
                TransferSourceItem(
                    id = wallet.id,
                    type = SourceType.WALLET,
                    name = wallet.name,
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
                    // Use the user-defined "Nombre de tarjeta" as the
                    // primary identifier (matches wallet behavior); fall
                    // back to the bank name when no custom name was set.
                    name = card.note ?: card.bank,
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
                    currency = stash.currency.code,
                    icon = "\uD83D\uDC8E",
                ),
            )
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
