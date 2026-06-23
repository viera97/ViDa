package com.vida.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Currency
import com.vida.domain.model.Expense
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.expense.AddExpense
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.GetWallet
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
 * per-field validation, and submission via [AddExpense].
 */
@HiltViewModel
class ExpenseFormViewModel @Inject constructor(
    private val addExpense: AddExpense,
    private val listCategories: ListCategories,
    private val listCards: ListCards,
    private val listStashes: ListStashes,
    private val getWallet: GetWallet,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExpenseFormUiState>(ExpenseFormUiState.Loading)
    val uiState: StateFlow<ExpenseFormUiState> = _uiState.asStateFlow()

    /** Cached last valid Ready state — used to recover from [ExpenseFormUiState.Error] on user edit. */
    private var lastReadyState: ExpenseFormUiState.Ready? = null

    init {
        viewModelScope.launch {
            _uiState.value = ExpenseFormUiState.Loading
            try {
                val categories = listCategories().first()
                val wallet = getWallet()
                val cards = listCards().first()
                val stashes = listStashes().first()

                val sources = buildList {
                    add(
                        SourceItem(
                            id = null,
                            type = SourceType.WALLET,
                            label = "Billetera",
                            subtitle = null,
                            currency = wallet.currency,
                        ),
                    )
                    for (card in cards) {
                        add(
                            SourceItem(
                                id = card.id,
                                type = SourceType.CARD,
                                label = card.bank,
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

                val defaultSource = sources.first() // always wallet
                val ready = ExpenseFormUiState.Ready(
                    form = FormFields(
                        currency = defaultSource.currency,
                        sourceType = SourceType.WALLET,
                        sourceId = null,
                    ),
                    sources = sources,
                    categories = categories,
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
        updateForm({ it.copy(amount = value) }, validateAmount(value))
    }

    fun onCurrencyChanged(currency: Currency) {
        updateForm({ it.copy(currency = currency) })
    }

    fun onDescriptionChanged(value: String) {
        updateForm({ it.copy(description = value) }, validateDescription(value))
    }

    fun onCategorySelected(id: Long) {
        updateForm(
            { it.copy(categoryId = id) },
            if (id <= 0L) mapOf("category" to "Selecciona una categoría") else emptyMap(),
        )
    }

    fun onSourceSelected(type: SourceType, id: Long?) {
        val source = (_uiState.value as? ExpenseFormUiState.Ready)?.sources
            ?.find { it.type == type && it.id == id }
        updateForm(
            { form ->
                form.copy(
                    sourceType = type,
                    sourceId = id,
                    currency = source?.currency ?: form.currency,
                )
            },
            validateSource(type, id),
        )
    }

    fun onDateTimeChanged(instant: Instant) {
        updateForm({ it.copy(dateTime = instant) })
    }

    fun onNoteChanged(value: String) {
        updateForm({ it.copy(note = value) })
    }

    // ── Submit ──────────────────────────────────────────────────────────────

    fun submit() {
        val ready = _uiState.value as? ExpenseFormUiState.Ready ?: return

        val errors = mutableMapOf<String, String>()
        errors.putAll(validateAmount(ready.form.amount))
        errors.putAll(validateDescription(ready.form.description))
        if (ready.form.categoryId == null) errors["category"] = "Selecciona una categoría"
        errors.putAll(validateSource(ready.form.sourceType, ready.form.sourceId))

        if (errors.isNotEmpty()) {
            _uiState.value = ready.copy(validationErrors = errors)
            return
        }

        _uiState.value = ExpenseFormUiState.Submitting

        viewModelScope.launch {
            try {
                val expense = Expense(
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
                addExpense(expense)
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
     * Updates the form fields within the current [ExpenseFormUiState.Ready] state,
     * optionally merging [extraErrors] into the validation errors map.
     * If the current state is [ExpenseFormUiState.Error], restores the last cached
     * Ready state before applying the transform. Emits exactly once.
     */
    private fun updateForm(
        transform: (FormFields) -> FormFields,
        extraErrors: Map<String, String> = emptyMap(),
    ) {
        val current = _uiState.value
        val ready: ExpenseFormUiState.Ready = when (current) {
            is ExpenseFormUiState.Ready -> current
            is ExpenseFormUiState.Error -> lastReadyState ?: return
            else -> return
        }
        val mergedErrors = if (extraErrors.isNotEmpty()) {
            ready.validationErrors + extraErrors
        } else {
            ready.validationErrors
        }
        val updated = if (mergedErrors == ready.validationErrors) {
            ready.copy(form = transform(ready.form))
        } else {
            ready.copy(form = transform(ready.form), validationErrors = mergedErrors)
        }
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
    private fun validateSource(type: SourceType, id: Long?): Map<String, String> {
        return when {
            type == SourceType.CARD && id == null -> mapOf("source" to "Selecciona una tarjeta")
            type == SourceType.STASH && id == null -> mapOf("source" to "Selecciona un ahorro")
            else -> emptyMap()
        }
    }
}
