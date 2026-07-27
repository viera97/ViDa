package com.vida.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Currency
import com.vida.domain.model.Expense
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.currency.ListCurrencies
import com.vida.domain.usecase.expense.AddExpense
import com.vida.domain.usecase.expense.GetExpense
import com.vida.domain.usecase.expense.UpdateExpense
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.ListWallets
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
 * ViewModel for the expense recording form.
 *
 * Loads categories and available sources (wallet + cards + stashes) on init,
 * then transitions to [ExpenseFormUiState.Ready]. Handles form field changes,
 * per-field validation, and submission via [AddExpense] (new expense) or
 * [UpdateExpense] (edit existing).
 *
 * Two modes:
 * - **Create** (default): called via [reset] — fresh blank form.
 * - **Edit**: called via [loadForEdit] — pre-fills the form from the existing
 *   expense row and routes [submit] through [UpdateExpense] preserving the row id.
 */
@HiltViewModel
class ExpenseFormViewModel @Inject constructor(
    private val addExpense: AddExpense,
    private val updateExpense: UpdateExpense,
    private val getExpense: GetExpense,
    private val listCategories: ListCategories,
    private val listCards: ListCards,
    private val listStashes: ListStashes,
    private val listWallets: ListWallets,
    private val listCurrencies: ListCurrencies,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExpenseFormUiState>(ExpenseFormUiState.Loading)
    val uiState: StateFlow<ExpenseFormUiState> = _uiState.asStateFlow()

    /** Cached last valid Ready state — used to recover from [ExpenseFormUiState.Error] on user edit. */
    private var lastReadyState: ExpenseFormUiState.Ready? = null

    /** Edit-mode state: `true` and [currentExpenseId] is non-null when [loadForEdit] ran. */
    private var isEditMode: Boolean = false
    private var currentExpenseId: Long? = null

    init {
        loadInitialData()
    }

    /**
     * Resets the ViewModel state to a fresh load (create mode).
     *
     * Called by the dialog composable on open so each session starts with
     * default form values and a clean Loading → Ready transition, regardless of
     * any previous Success / Submitting / Error terminal state left in the VM.
     */
    fun reset() {
        lastReadyState = null
        isEditMode = false
        currentExpenseId = null
        loadInitialData()
    }

    /**
     * Loads the expense identified by [expenseId] and pre-fills the form with
     * its current values. Emits [ExpenseFormUiState.Loading] synchronously,
     * then [ExpenseFormUiState.Ready] with the pre-filled form, or
     * [ExpenseFormUiState.Error] if the row is missing.
     *
     * Subsequent [submit] calls route through [UpdateExpense] preserving the row id.
     */
    fun loadForEdit(expenseId: Long) {
        require(expenseId > 0L) { "Expense id must be > 0 to edit" }
        lastReadyState = null
        isEditMode = true
        currentExpenseId = expenseId
        loadForEditData()
    }

    private fun loadForEditData() {
        _uiState.value = ExpenseFormUiState.Loading
        viewModelScope.launch {
            try {
                val categories = listCategories().first()
                listWallets().first() // ensure wallets are loaded for source-label display
                val cards = listCards().first()
                val stashes = listStashes().first()
                val currencies = listCurrencies().first()

                val sources = buildList {
                    for (wallet in listWallets().first()) {
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
                                currency = stash.currency.code,
                            ),
                        )
                    }
                }

                val existing = getExpense(currentExpenseId!!)
                if (existing == null) {
                    _uiState.value = ExpenseFormUiState.Error("Gasto no encontrado")
                    return@launch
                }

                val ready = ExpenseFormUiState.Ready(
                    form = FormFields(
                        amount = existing.amount.amount.toPlainString(),
                        currency = existing.amount.currency,
                        description = existing.description,
                        categoryId = existing.categoryId,
                        sourceType = existing.sourceType,
                        sourceId = existing.sourceId,
                        hasSourceSelected = true,
                        dateTime = existing.dateTime,
                        note = existing.note ?: "",
                    ),
                    sources = sources,
                    categories = categories,
                    availableCurrencies = currencies.map { Currency.fromCode(it.code) }.distinctBy { it.code },
                )
                lastReadyState = ready
                _uiState.value = ready
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = ExpenseFormUiState.Error(
                    t.message ?: "Error al cargar el gasto para editar",
                )
            }
        }
    }

    /**
     * Loads categories, wallets, cards, and stashes and emits the next state.
     *
     * Emits [ExpenseFormUiState.Loading] SYNCHRONOUSLY (before this function returns)
     * so callers observing the StateFlow can immediately see a non-terminal state and
     * don't briefly observe a stale terminal state (e.g. `Success` left over from a
     * previous dialog session). Then launches a coroutine that fetches data and emits
     * [ExpenseFormUiState.NoSources] / [ExpenseFormUiState.Ready] / [ExpenseFormUiState.Error].
     *
     * Both [init] and [reset] call this.
     */
    private fun loadInitialData() {
        _uiState.value = ExpenseFormUiState.Loading
        viewModelScope.launch {
            try {
                val categories = listCategories().first()
                val wallets = listWallets().first()
                val cards = listCards().first()
                val stashes = listStashes().first()
                val currencies = listCurrencies().first()

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
                                currency = stash.currency.code,
                            ),
                        )
                    }
                }

                if (sources.isEmpty()) {
                    _uiState.value = ExpenseFormUiState.NoSources
                    return@launch
                }

                val ready = ExpenseFormUiState.Ready(
                    form = FormFields(
                        currency = Currency.CUP,
                        hasSourceSelected = false,
                    ),
                    sources = sources,
                    categories = categories,
                    availableCurrencies = currencies.map { Currency.fromCode(it.code) }.distinctBy { it.code },
                )
                lastReadyState = ready
                _uiState.value = ready
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = ExpenseFormUiState.Error(
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

    fun onCategorySelected(id: Long) {
        updateForm(
            transform = { it.copy(categoryId = id) },
            newErrors = if (id <= 0L) mapOf("category" to "Selecciona una categoría") else emptyMap(),
            clearedFields = setOf("category"),
        )
    }

fun onSourceSelected(type: SourceType, id: Long?) {
        // Auto-update the currency to match the source's native currency so the
        // user doesn't have to re-pick it manually. The source IS the origin of
        // the expense — its currency is the natural default. The user can still
        // override via the Currency FilterChip row afterwards if needed (e.g.,
        // they want to record a currency mismatch scenario). Mismatch detection
        // is handled reactively by [computeMismatchError].
        val ready = _uiState.value as? ExpenseFormUiState.Ready
        val sourceCurrency = ready?.sources
            ?.find { it.type == type && it.id == id }
            ?.currency
        updateForm(
            { form ->
                form.copy(
                    sourceType = type,
                    sourceId = id,
                    hasSourceSelected = true,
                    currency = sourceCurrency?.let { Currency.fromCode(it) } ?: form.currency,
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
        val ready = _uiState.value as? ExpenseFormUiState.Ready ?: return null
        if (!ready.form.hasSourceSelected) return null
        val source = ready.sources.find {
            it.type == ready.form.sourceType && it.id == ready.form.sourceId
        } ?: return null
        // If the source uses a currency code unknown to the enum (custom/user-created
        // currency), bypass the mismatch check — the enum can't represent it and the
        // currency field is hidden when source is selected so the user can't fix it.
        if (Currency.values().none { it.code.equals(source.currency, ignoreCase = true) }) return null
        return if (source.currency != ready.form.currency.code) {
            "La moneda debe coincidir con la fuente (${source.currency})"
        } else null
    }

    /**
     * Returns all validation errors. Called by [submit]; not used for
     * reactive UI because the dialog should NOT show required-field errors
     * (e.g. "El importe es obligatorio") until the user has actually tried to
     * submit.
     */
    private fun computeFormErrors(): Map<String, String> {
        val ready = _uiState.value as? ExpenseFormUiState.Ready ?: return emptyMap()

        val errors = mutableMapOf<String, String>()
        errors.putAll(validateAmount(ready.form.amount))
        errors.putAll(validateDescription(ready.form.description))
        if (ready.form.categoryId == null) errors["category"] = "Selecciona una categoría"
        errors.putAll(validateSource(ready.form.sourceType, ready.form.sourceId, ready.form.hasSourceSelected))
        computeMismatchError()?.let { errors["amount"] = it }
        return errors
    }

    fun submit() {
        val ready = _uiState.value as? ExpenseFormUiState.Ready ?: return

        val errors = computeFormErrors()
        if (errors.isNotEmpty()) {
            _uiState.value = ready.copy(validationErrors = errors)
            return
        }

        _uiState.value = ExpenseFormUiState.Submitting

        viewModelScope.launch {
            try {
                // Preserve the row id when editing so UpdateExpense routes through
                // Room's @Upsert as an UPDATE rather than an INSERT. For create mode
                // the id defaults to 0L and AddExpense inserts a new row.
                val expense = Expense(
                    id = currentExpenseId ?: 0L,
                    categoryId = ready.form.categoryId!!,
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
                if (isEditMode && currentExpenseId != null) {
                    updateExpense(expense)
                } else {
                    addExpense(expense)
                }
                _uiState.value = ExpenseFormUiState.Success
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = ExpenseFormUiState.Error(
                    t.message ?: "Error al guardar el gasto",
                )
            }
        }
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    /**
     * Updates the form fields within the current [ExpenseFormUiState.Ready] state.
     *
     * Behavior:
     * - [newErrors] is the validation result for the field(s) just updated. Any keys
     *   present in [newErrors] replace the corresponding entries in
     *   [ExpenseFormUiState.Ready.validationErrors].
     * - [clearedFields] lists fields that were re-validated but are now valid. Their
     *   entries are removed from the errors map (so stale errors from a previous
     *   `submit()` don't linger when the user fixes the field).
     * - If the current state is [ExpenseFormUiState.Error], restores the last cached
     *   Ready state before applying the transform.
     */
    private fun updateForm(
        transform: (FormFields) -> FormFields,
        newErrors: Map<String, String> = emptyMap(),
        clearedFields: Set<String> = emptySet(),
    ) {
        val current = _uiState.value
        val ready: ExpenseFormUiState.Ready = when (current) {
            is ExpenseFormUiState.Ready -> current
            is ExpenseFormUiState.Error -> lastReadyState ?: return
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
