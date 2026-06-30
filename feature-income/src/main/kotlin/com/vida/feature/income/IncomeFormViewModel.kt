package com.vida.feature.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Currency
import com.vida.domain.model.Income
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.income.AddIncome
import com.vida.domain.usecase.income.GetIncome
import com.vida.domain.usecase.income.UpdateIncome
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.ListWallets
import com.vida.feature.expense.SourceItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for the income recording form.
 *
 * Loads available sources (wallet + cards + stashes) on init, then transitions
 * to [IncomeFormUiState.Ready]. Handles form field changes, per-field validation,
 * and submission via [AddIncome]. Unlike [com.vida.feature.expense.ExpenseFormViewModel],
 * there is no category to pick — incomes are not categorized.
 *
 * The destination source's `balance_minor` is auto-updated on submit
 * (see [com.vida.domain.repository.IncomeRepository.upsert]) — this is the
 * opposite of expense, which uses Option B (no auto-update). The income flow
 * is the explicit "auto-update" path because users expect recording a salary
 * to immediately bump their wallet balance.
 */
@HiltViewModel
class IncomeFormViewModel @Inject constructor(
    private val addIncome: AddIncome,
    private val updateIncome: UpdateIncome,
    private val getIncome: GetIncome,
    private val listCards: ListCards,
    private val listStashes: ListStashes,
    private val listWallets: ListWallets,
) : ViewModel() {

    private val _uiState = MutableStateFlow<IncomeFormUiState>(IncomeFormUiState.Loading)
    val uiState: StateFlow<IncomeFormUiState> = _uiState.asStateFlow()

    /** Cached last valid Ready state — used to recover from [IncomeFormUiState.Error] on user edit. */
    private var lastReadyState: IncomeFormUiState.Ready? = null

    /** Edit-mode state. */
    private var isEditMode: Boolean = false
    private var currentIncomeId: Long? = null

    init {
        loadInitialData()
    }

    /**
     * Resets the ViewModel state to a fresh load.
     *
     * Called by the dialog composable on open so each session starts with
     * default form values and a clean Loading → Ready transition, regardless of
     * any previous Success / Submitting / Error terminal state left in the VM.
     */
    fun reset() {
        lastReadyState = null
        isEditMode = false
        currentIncomeId = null
        loadInitialData()
    }

    fun loadForEdit(incomeId: Long) {
        require(incomeId > 0L) { "Income id must be > 0 to edit" }
        lastReadyState = null
        isEditMode = true
        currentIncomeId = incomeId
        _uiState.value = IncomeFormUiState.Loading
        viewModelScope.launch {
            try {
                val wallets = listWallets().first()
                val cards = listCards().first()
                val stashes = listStashes().first()

                val sources = buildList {
                    for (wallet in wallets) add(SourceItem(id = wallet.id, type = SourceType.WALLET, label = wallet.name, subtitle = null, currency = wallet.currency))
                    for (card in cards) add(SourceItem(id = card.id, type = SourceType.CARD, label = card.note?.takeIf { it.isNotBlank() } ?: card.bank, subtitle = "···${card.number.masked.substring(12, 16)}", currency = card.currency))
                    for (stash in stashes) add(SourceItem(id = stash.id, type = SourceType.STASH, label = stash.name, subtitle = null, currency = stash.currency))
                }

                val existing = getIncome(currentIncomeId!!)
                if (existing == null) {
                    _uiState.value = IncomeFormUiState.Error("Ingreso no encontrado")
                    return@launch
                }

                val ready = IncomeFormUiState.Ready(
                    form = IncomeFormFields(
                        amount = existing.amount.amount.toPlainString(),
                        currency = existing.amount.currency,
                        description = existing.description,
                        sourceType = existing.sourceType,
                        sourceId = existing.sourceId,
                        hasSourceSelected = true,
                        dateTime = existing.dateTime,
                        note = existing.note ?: "",
                    ),
                    sources = sources,
                )
                lastReadyState = ready
                _uiState.value = ready
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = IncomeFormUiState.Error(t.message ?: "Error al cargar el ingreso para editar")
            }
        }
    }

    /**
     * Loads wallets, cards, and stashes and emits the next state.
     *
     * Emits [IncomeFormUiState.Loading] SYNCHRONOUSLY (before this function returns)
     * so callers observing the StateFlow can immediately see a non-terminal state and
     * don't briefly observe a stale terminal state (e.g. `Success` left over from a
     * previous dialog session). Then launches a coroutine that fetches data and emits
     * [IncomeFormUiState.NoSources] / [IncomeFormUiState.Ready] / [IncomeFormUiState.Error].
     *
     * Both [init] and [reset] call this.
     */
    private fun loadInitialData() {
        _uiState.value = IncomeFormUiState.Loading
        viewModelScope.launch {
            try {
                val wallets = listWallets().first()
                val cards = listCards().first()
                val stashes = listStashes().first()

                val sources = buildList {
                    for (wallet in wallets) {
                        add(
                            SourceItem(
                                id = wallet.id,
                                type = SourceType.WALLET,
                                label = wallet.name,
                                subtitle = null,
                                currency = wallet.currency,
                            ),
                        )
                    }
                    for (card in cards) {
                        add(
                            SourceItem(
                                id = card.id,
                                type = SourceType.CARD,
                                // The card's user-defined name is stored in `note`
                                // (the form's "Nombre de tarjeta" field binds to it).
                                // Fall back to the bank when no name was provided.
                                label = card.note?.takeIf { it.isNotBlank() } ?: card.bank,
                                subtitle = "···${card.number.masked.substring(12, 16)}",
                                currency = card.currency,
                            ),
                        )
                    }
                    for (stash in stashes) {
                        add(
                            SourceItem(
                                id = stash.id,
                                type = SourceType.STASH,
                                label = stash.name,
                                subtitle = null,
                                currency = stash.currency,
                            ),
                        )
                    }
                }

                if (sources.isEmpty()) {
                    _uiState.value = IncomeFormUiState.NoSources
                    return@launch
                }

                val defaultSource = sources.first() // first wallet
                val ready = IncomeFormUiState.Ready(
                    form = IncomeFormFields(
                        currency = defaultSource.currency,
                        sourceType = SourceType.WALLET,
                        sourceId = defaultSource.id,
                    ),
                    sources = sources,
                )
                lastReadyState = ready
                _uiState.value = ready
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = IncomeFormUiState.Error(
                    t.message ?: "Error al cargar el formulario",
                )
            }
        }
    }

    // ── Input handlers ──────────────────────────────────────────────────────

    fun onAmountChanged(value: String) {
        updateForm(
            transform = { it.copy(amount = value) },
            newErrors = validateAmount(value),
            clearedFields = setOf("amount"),
        )
    }

    fun onCurrencyChanged(currency: Currency) {
        // Clear the currency-vs-source mismatch error (keyed under "amount") so
        // it doesn't linger when the user picks a new currency.
        updateForm(
            transform = { it.copy(currency = currency) },
            clearedFields = setOf("amount"),
        )
    }

    fun onDescriptionChanged(value: String) {
        updateForm(
            transform = { it.copy(description = value) },
            newErrors = validateDescription(value),
            clearedFields = setOf("description"),
        )
    }

    fun onSourceSelected(type: SourceType, id: Long?) {
        // Note: currency is intentionally NOT updated here. The user may have
        // explicitly picked a currency before picking the source; auto-syncing
        // would silently overwrite that choice. If the chosen currency doesn't
        // match the source's currency, the dialog recomputes validation
        // reactively (see [computeFormErrors]) and disables Guardar with an
        // explicit error message; the user must explicitly resolve the mismatch.
        updateForm(
            { form ->
                form.copy(
                    sourceType = type,
                    sourceId = id,
                    hasSourceSelected = true,
                )
            },
            newErrors = validateSource(type, id, hasSourceSelected = true),
            clearedFields = setOf("source", "amount"),
        )
    }

    fun onDateTimeChanged(instant: Instant) {
        updateForm({ it.copy(dateTime = instant) })
    }

    fun onNoteChanged(value: String) {
        updateForm({ it.copy(note = value) })
    }

    // ── Submit ──────────────────────────────────────────────────────────────

    /**
     * Returns just the currency-vs-source mismatch message (or null if there's
     * no mismatch). This is checked REACTIVELY by the dialog to enable/disable
     * the submit button and show a top-of-form error message — separate from
     * the per-field required-field errors, which only show AFTER the user
     * tries to submit.
     */
    fun computeMismatchError(): String? {
        val ready = _uiState.value as? IncomeFormUiState.Ready ?: return null
        if (!ready.form.hasSourceSelected) return null
        val source = ready.sources.find {
            it.type == ready.form.sourceType && it.id == ready.form.sourceId
        } ?: return null
        return if (source.currency != ready.form.currency) {
            "La moneda debe coincidir con la fuente (${source.currency.code})"
        } else null
    }

    /**
     * Returns all validation errors. Called by [submit]; not used for
     * reactive UI because the dialog should NOT show required-field errors
     * (e.g. "El importe es obligatorio") until the user has actually tried to
     * submit.
     */
    private fun computeFormErrors(): Map<String, String> {
        val ready = _uiState.value as? IncomeFormUiState.Ready ?: return emptyMap()

        val errors = mutableMapOf<String, String>()
        errors.putAll(validateAmount(ready.form.amount))
        errors.putAll(validateDescription(ready.form.description))
        errors.putAll(validateSource(ready.form.sourceType, ready.form.sourceId, ready.form.hasSourceSelected))
        computeMismatchError()?.let { errors["amount"] = it }
        return errors
    }

    fun submit() {
        val ready = _uiState.value as? IncomeFormUiState.Ready ?: return

        val errors = computeFormErrors()
        if (errors.isNotEmpty()) {
            _uiState.value = ready.copy(validationErrors = errors)
            return
        }

        _uiState.value = IncomeFormUiState.Submitting

        viewModelScope.launch {
            try {
                val income = Income(
                    id = currentIncomeId ?: 0L,
                    amount = Money(
                        BigDecimal(ready.form.amount),
                        ready.form.currency,
                    ),
                    description = ready.form.description,
                    dateTime = ready.form.dateTime,
                    sourceType = ready.form.sourceType,
                    sourceId = ready.form.sourceId,
                    note = ready.form.note.ifBlank { null },
                )
                if (isEditMode && currentIncomeId != null) {
                    updateIncome(income)
                } else {
                    addIncome(income)
                }
                _uiState.value = IncomeFormUiState.Success
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = IncomeFormUiState.Error(
                    t.message ?: "Error al guardar el ingreso",
                )
            }
        }
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    /**
     * Updates the form fields within the current [IncomeFormUiState.Ready] state.
     *
     * Behavior:
     * - [newErrors] is the validation result for the field(s) just updated. Any keys
     *   present in [newErrors] replace the corresponding entries in
     *   [IncomeFormUiState.Ready.validationErrors].
     * - [clearedFields] lists fields that were re-validated but are now valid. Their
     *   entries are removed from the errors map (so stale errors from a previous
     *   `submit()` don't linger when the user fixes the field).
     * - If the current state is [IncomeFormUiState.Error], restores the last cached
     *   Ready state before applying the transform.
     */
    private fun updateForm(
        transform: (IncomeFormFields) -> IncomeFormFields,
        newErrors: Map<String, String> = emptyMap(),
        clearedFields: Set<String> = emptySet(),
    ) {
        val current = _uiState.value
        val ready: IncomeFormUiState.Ready = when (current) {
            is IncomeFormUiState.Ready -> current
            is IncomeFormUiState.Error -> lastReadyState ?: return
            else -> return
        }
        val mergedErrors = ready.validationErrors.toMutableMap().apply {
            // Drop stale errors for fields that have been re-validated.
            for (field in clearedFields) remove(field)
            // Apply the latest validation result.
            putAll(newErrors)
        }
        val updated = ready.copy(form = transform(ready.form), validationErrors = mergedErrors)
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

    /** Validates the description string. Returns a (possibly empty) errors map. */
    private fun validateDescription(value: String): Map<String, String> {
        return when {
            value.isBlank() -> mapOf("description" to "La descripción es obligatoria")
            value.length > 100 -> mapOf("description" to "Máximo 100 caracteres")
            else -> emptyMap()
        }
    }

    /** Validates the source selection. Returns a (possibly empty) errors map. */
    private fun validateSource(type: SourceType, id: Long?, hasSourceSelected: Boolean): Map<String, String> {
        if (!hasSourceSelected) return mapOf("source" to "Selecciona una fuente")
        return when {
            type == SourceType.CARD && id == null -> mapOf("source" to "Selecciona una tarjeta")
            type == SourceType.STASH && id == null -> mapOf("source" to "Selecciona un ahorro")
            else -> emptyMap()
        }
    }
}
