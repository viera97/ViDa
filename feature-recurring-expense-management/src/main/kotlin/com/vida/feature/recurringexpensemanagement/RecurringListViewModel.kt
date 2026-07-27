package com.vida.feature.recurringexpensemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Card
import com.vida.domain.model.Category
import com.vida.domain.model.Frequency
import com.vida.domain.model.Income
import com.vida.domain.model.RecurringExpense
import com.vida.domain.model.RecurringIncome
import com.vida.domain.model.SourceType
import com.vida.domain.model.Expense
import com.vida.domain.model.Stash
import com.vida.domain.model.Wallet
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.currency.ListCurrencies
import com.vida.domain.usecase.expense.RecordExpense
import com.vida.domain.usecase.income.RecordIncome
import com.vida.domain.usecase.recurring.AddRecurringExpense
import com.vida.domain.usecase.recurring.AddRecurringIncome
import com.vida.domain.usecase.recurring.DeleteRecurringExpense
import com.vida.domain.usecase.recurring.DeleteRecurringIncome
import com.vida.domain.usecase.recurring.GenerateRecurringExpense
import com.vida.domain.usecase.recurring.GenerateRecurringIncome
import com.vida.domain.usecase.recurring.GetDueRecurringExpenses
import com.vida.domain.usecase.recurring.GetDueRecurringIncomes
import com.vida.domain.usecase.recurring.GetRecurringExpense
import com.vida.domain.usecase.recurring.GetRecurringIncome
import com.vida.domain.usecase.recurring.ListRecurringExpenses
import com.vida.domain.usecase.recurring.ListRecurringIncomes
import com.vida.domain.usecase.recurring.UpdateRecurringExpense
import com.vida.domain.usecase.recurring.UpdateRecurringIncome
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.ListWallets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for the recurring expense/income list screen.
 *
 * Collects [ListRecurringExpenses] and [ListRecurringIncomes] as reactive Flows,
 * merges them into a single sorted list.
 *
 * Sort: isActive DESC → frequency ordinal ASC (DAILY first) → startDate ASC.
 */
@HiltViewModel
class RecurringListViewModel @Inject constructor(
    private val listRecurringExpenses: ListRecurringExpenses,
    private val addRecurringExpense: AddRecurringExpense,
    private val updateRecurringExpense: UpdateRecurringExpense,
    private val deleteRecurringExpense: DeleteRecurringExpense,
    private val getRecurringExpense: GetRecurringExpense,
    private val getDueRecurringExpenses: GetDueRecurringExpenses,
    private val generateRecurringExpense: GenerateRecurringExpense,
    private val recordExpense: RecordExpense,
    private val listRecurringIncomes: ListRecurringIncomes,
    private val addRecurringIncome: AddRecurringIncome,
    private val updateRecurringIncome: UpdateRecurringIncome,
    private val deleteRecurringIncome: DeleteRecurringIncome,
    private val getRecurringIncome: GetRecurringIncome,
    private val getDueRecurringIncomes: GetDueRecurringIncomes,
    private val generateRecurringIncome: GenerateRecurringIncome,
    private val recordIncome: RecordIncome,
    private val listCategories: ListCategories,
    private val listCards: ListCards,
    private val listStashes: ListStashes,
    private val listWallets: ListWallets,
    private val listCurrencies: ListCurrencies,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecurringListUiState>(RecurringListUiState.Loading)
    val uiState: StateFlow<RecurringListUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<RecurringNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    /** True while any mutation (delete, toggle, add, edit) is in-flight (prevents double-tap). */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** True while a generate operation is in-flight (prevents double-tap). */
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // ── Reference data (collected reactively for form dropdowns) ──────────

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards.asStateFlow()

    private val _stashes = MutableStateFlow<List<Stash>>(emptyList())
    val stashes: StateFlow<List<Stash>> = _stashes.asStateFlow()

    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList())
    val wallets: StateFlow<List<Wallet>> = _wallets.asStateFlow()

    /** Reactive list of currency codes for recurring form dropdowns. */
    val currencyCodes: StateFlow<List<String>> = listCurrencies()
        .map { currencies -> currencies.map { it.code } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Holds the active recurring-templates collection job so retries don't stack. */
    private var recurringJob: Job? = null

    init {
        collectReferenceData()
    }

    // ── Public actions (expense) ──────────────────────────────────────────────

    /**
     * Emits [RecurringNavEvent.ShowAddDialog] so the screen opens the add dialog.
     */
    fun onFabClick() {
        viewModelScope.launch {
            _navEvents.send(RecurringNavEvent.ShowAddDialog)
        }
    }

    /**
     * Adds a new recurring expense template.
     */
    fun onAdd(expense: RecurringExpense) {
        if (_isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                addRecurringExpense(expense)
                _navEvents.send(RecurringNavEvent.SaveSuccess)
                _navEvents.send(RecurringNavEvent.ShowToast("Plantilla creada"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    RecurringNavEvent.ShowToast(
                        t.message ?: "No se pudo crear la plantilla",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Updates an existing recurring expense template.
     */
    fun onEdit(expense: RecurringExpense) {
        if (_isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                updateRecurringExpense(expense)
                _navEvents.send(RecurringNavEvent.SaveSuccess)
                _navEvents.send(RecurringNavEvent.ShowToast("Plantilla actualizada"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    RecurringNavEvent.ShowToast(
                        t.message ?: "No se pudo actualizar la plantilla",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    // ── Public actions (income) ───────────────────────────────────────────────

    /**
     * Emits [RecurringNavEvent.ShowAddIncomeDialog] so the screen opens the income add dialog.
     */
    fun onIncomeFabClick() {
        viewModelScope.launch {
            _navEvents.send(RecurringNavEvent.ShowAddIncomeDialog)
        }
    }

    /**
     * Adds a new recurring income template.
     */
    fun onAddIncome(income: RecurringIncome) {
        if (_isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                addRecurringIncome(income)
                _navEvents.send(RecurringNavEvent.SaveSuccess)
                _navEvents.send(RecurringNavEvent.ShowToast("Plantilla de ingreso creada"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    RecurringNavEvent.ShowToast(
                        t.message ?: "No se pudo crear la plantilla de ingreso",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Updates an existing recurring income template.
     */
    fun onEditIncome(income: RecurringIncome) {
        if (_isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                updateRecurringIncome(income)
                _navEvents.send(RecurringNavEvent.SaveSuccess)
                _navEvents.send(RecurringNavEvent.ShowToast("Plantilla de ingreso actualizada"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    RecurringNavEvent.ShowToast(
                        t.message ?: "No se pudo actualizar la plantilla de ingreso",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    // ── Public actions (dispatched by item type) ──────────────────────────────

    /**
     * Opens the edit dialog for the given item, dispatching to expense or income
     * use cases based on [RecurringDisplayItem.type].
     */
    fun onOpenEditDialog(item: RecurringDisplayItem) {
        viewModelScope.launch {
            when (item.type) {
                RecurringDisplayItem.ItemType.EXPENSE -> {
                    val entity = getRecurringExpense(item.id)
                    if (entity != null) {
                        _navEvents.send(RecurringNavEvent.ShowEditDialog(entity))
                    } else {
                        _navEvents.send(RecurringNavEvent.ShowToast("Plantilla no encontrada"))
                    }
                }
                RecurringDisplayItem.ItemType.INCOME -> {
                    val entity = getRecurringIncome(item.id)
                    if (entity != null) {
                        _navEvents.send(RecurringNavEvent.ShowIncomeEditDialog(entity))
                    } else {
                        _navEvents.send(RecurringNavEvent.ShowToast("Plantilla no encontrada"))
                    }
                }
            }
        }
    }

    /**
     * Emits [RecurringNavEvent.ShowDeleteDialog] so the screen shows
     * delete confirmation for the given [item].
     */
    fun onRequestDelete(item: RecurringDisplayItem) {
        viewModelScope.launch {
            _navEvents.send(RecurringNavEvent.ShowDeleteDialog(item))
        }
    }

    /**
     * Deletes the recurring template with the given [item], dispatching to
     * expense or income use cases based on type.
     */
    fun onDelete(item: RecurringDisplayItem) {
        if (_isSaving.value) return

        val current = _uiState.value
        if (current !is RecurringListUiState.Ready) return
        if (current.items.none { it.id == item.id && it.type == item.type }) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                when (item.type) {
                    RecurringDisplayItem.ItemType.EXPENSE -> deleteRecurringExpense(item.id)
                    RecurringDisplayItem.ItemType.INCOME -> deleteRecurringIncome(item.id)
                }
                _navEvents.send(RecurringNavEvent.ShowToast("Plantilla eliminada"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    RecurringNavEvent.ShowToast(
                        t.message ?: "No se pudo eliminar la plantilla",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Toggles the `isActive` flag on the template for the given [item],
     * dispatching to expense or income use cases based on type.
     */
    fun onToggleActive(item: RecurringDisplayItem) {
        if (_isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                when (item.type) {
                    RecurringDisplayItem.ItemType.EXPENSE -> {
                        val existing = getRecurringExpense(item.id)
                            ?: run {
                                _navEvents.send(RecurringNavEvent.ShowToast("Plantilla no encontrada"))
                                return@launch
                            }
                        val toggled = existing.copy(isActive = !existing.isActive)
                        updateRecurringExpense(toggled)
                    }
                    RecurringDisplayItem.ItemType.INCOME -> {
                        val existing = getRecurringIncome(item.id)
                            ?: run {
                                _navEvents.send(RecurringNavEvent.ShowToast("Plantilla no encontrada"))
                                return@launch
                            }
                        val toggled = existing.copy(isActive = !existing.isActive)
                        updateRecurringIncome(toggled)
                    }
                }
                _navEvents.send(
                    RecurringNavEvent.ShowToast("Plantilla actualizada"),
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    RecurringNavEvent.ShowToast(
                        t.message ?: "No se pudo actualizar la plantilla",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Two-step generate flow for the recurring template with the given [item]:
     * dispatches to expense or income use cases based on type.
     */
    fun onGenerate(item: RecurringDisplayItem) {
        if (_isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            try {
                when (item.type) {
                    RecurringDisplayItem.ItemType.EXPENSE -> {
                        generateRecurringExpense(item.id)
                        val template = getRecurringExpense(item.id)
                        if (template != null) {
                            val expense = Expense(
                                id = 0L,
                                categoryId = template.categoryId,
                                amount = template.amount,
                                realAmount = null,
                                description = template.description,
                                dateTime = (template.lastGeneratedDate
                                    ?: LocalDate.now())
                                    .atStartOfDay(ZoneOffset.UTC)
                                    .toInstant(),
                                sourceType = template.sourceType,
                                sourceId = template.sourceId,
                                note = null,
                            )
                            recordExpense(expense)
                        }
                    }
                    RecurringDisplayItem.ItemType.INCOME -> {
                        generateRecurringIncome(item.id)
                        val template = getRecurringIncome(item.id)
                        if (template != null) {
                            val income = Income(
                                id = 0L,
                                amount = template.amount,
                                description = template.description,
                                dateTime = (template.lastGeneratedDate
                                    ?: LocalDate.now())
                                    .atStartOfDay(ZoneOffset.UTC)
                                    .toInstant(),
                                sourceType = template.sourceType,
                                sourceId = template.sourceId,
                                note = null,
                            )
                            recordIncome(income)
                        }
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    RecurringNavEvent.ShowToast(
                        t.message ?: "No se pudo generar",
                    ),
                )
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /** Re-initiates the Flow collection from the [RecurringListUiState.Error] state. */
    fun onRetry() {
        collectRecurringTemplates()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Collects the reactive [ListRecurringExpenses] and [ListRecurringIncomes] Flows,
     * merges them, sorts, maps to display items, and emits the appropriate
     * [RecurringListUiState].
     *
     * Idempotent: cancels any previous collection job before starting a new one.
     */
    private fun collectRecurringTemplates() {
        recurringJob?.cancel()
        recurringJob = viewModelScope.launch {
            try {
                combine(
                    listRecurringExpenses(),
                    listRecurringIncomes(),
                ) { expenses, incomes ->
                    val categoriesById = _categories.value.associateBy { it.id }
                    val expenseItems = expenses.map { it.toDisplayItem(categoriesById) }
                    val incomeItems = incomes.map { it.toIncomeDisplayItem() }
                    (expenseItems + incomeItems)
                        .sortedWith(
                            compareByDescending<RecurringDisplayItem> { it.isActive }
                                .thenBy { it.frequencyOrdinal }
                                .thenBy { it.startDateEpochDay },
                        )
                }
                    .onStart { _uiState.value = RecurringListUiState.Loading }
                    .catch { t ->
                        if (t is CancellationException) throw t
                        _uiState.value = RecurringListUiState.Error(
                            message = t.message ?: "No se pudieron cargar las plantillas",
                        )
                    }
                    .collect { items ->
                        _uiState.value = if (items.isEmpty()) {
                            RecurringListUiState.Empty
                        } else {
                            RecurringListUiState.Ready(items = items)
                        }
                    }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = RecurringListUiState.Error(
                    message = t.message ?: "No se pudieron cargar las plantillas",
                )
            }
        }
    }

    /**
     * Collects reference data (categories, cards, stashes, wallets) reactively for
     * form dropdowns. These flows run once per ViewModel lifetime and auto-update
     * whenever the underlying Room tables change.
     *
     * The recurring templates collection is started from here, after the first
     * non-empty emission of categories. This avoids a race condition where the
     * templates Flow emits first and `categoriesById` is empty.
     */
    private fun collectReferenceData() {
        viewModelScope.launch {
            listCategories()
                .catch { /* categories unavailable — list stays empty */ }
                .collect { categories ->
                    val firstNonEmpty = _categories.value.isEmpty() && categories.isNotEmpty()
                    _categories.value = categories
                    if (firstNonEmpty) {
                        // Categories are now available — safe to start the
                        // recurring templates collection.
                        collectRecurringTemplates()
                    }
                }
        }
        viewModelScope.launch {
            listCards().catch { /* cards unavailable */ }
                .collect { _cards.value = it }
        }
        viewModelScope.launch {
            listStashes().catch { /* stashes unavailable */ }
                .collect { _stashes.value = it }
        }
        viewModelScope.launch {
            listWallets().catch { /* wallets unavailable */ }
                .collect { _wallets.value = it }
        }
    }

    /**
     * Maps a domain [RecurringExpense] to a pre-formatted [RecurringDisplayItem].
     */
    private fun RecurringExpense.toDisplayItem(
        categoriesById: Map<Long, Category> = emptyMap(),
    ): RecurringDisplayItem {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val scale = amount.amount.setScale(2, RoundingMode.HALF_EVEN)
        return RecurringDisplayItem(
            id = id,
            amountFormatted = scale.toPlainString(),
            currencyCode = currency,
            categoryName = categoriesById[categoryId]?.name ?: categoryId.toString(),
            frequencyLabel = frequency.toSpanishLabel(),
            sourceType = sourceType,
            sourceTypeIcon = sourceType.toIcon(),
            nextDueFormatted = nextDueDate()?.format(formatter) ?: "—",
            description = description,
            isActive = isActive,
            type = RecurringDisplayItem.ItemType.EXPENSE,
            frequencyOrdinal = frequency.ordinal,
            startDateEpochDay = startDate.toEpochDay(),
        )
    }

    /**
     * Maps a domain [RecurringIncome] to a pre-formatted [RecurringDisplayItem].
     */
    private fun RecurringIncome.toIncomeDisplayItem(): RecurringDisplayItem {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val scale = amount.amount.setScale(2, RoundingMode.HALF_EVEN)
        return RecurringDisplayItem(
            id = id,
            amountFormatted = scale.toPlainString(),
            currencyCode = currency,
            categoryName = "",
            frequencyLabel = frequency.toSpanishLabel(),
            sourceType = sourceType,
            sourceTypeIcon = sourceType.toIcon(),
            nextDueFormatted = nextDueDate()?.format(formatter) ?: "—",
            description = description,
            isActive = isActive,
            type = RecurringDisplayItem.ItemType.INCOME,
            frequencyOrdinal = frequency.ordinal,
            startDateEpochDay = startDate.toEpochDay(),
        )
    }

    /**
     * Computes the next due date for this recurring expense template.
     */
    private fun RecurringExpense.nextDueDate(): LocalDate? {
        val lastGen = lastGeneratedDate
        val candidate = if (lastGen != null) {
            when (frequency) {
                Frequency.DAILY -> lastGen.plusDays(1)
                Frequency.WEEKLY -> lastGen.plusWeeks(1)
                Frequency.MONTHLY -> lastGen.plusMonths(1)
                Frequency.YEARLY -> lastGen.plusYears(1)
            }
        } else {
            startDate
        }
        if (candidate.isBefore(startDate)) return null
        return candidate
    }

    /**
     * Computes the next due date for this recurring income template.
     */
    private fun RecurringIncome.nextDueDate(): LocalDate? {
        val lastGen = lastGeneratedDate
        val candidate = if (lastGen != null) {
            when (frequency) {
                Frequency.DAILY -> lastGen.plusDays(1)
                Frequency.WEEKLY -> lastGen.plusWeeks(1)
                Frequency.MONTHLY -> lastGen.plusMonths(1)
                Frequency.YEARLY -> lastGen.plusYears(1)
            }
        } else {
            startDate
        }
        if (candidate.isBefore(startDate)) return null
        return candidate
    }

    companion object {
        /** Spanish labels for [Frequency] enum values. */
        fun Frequency.toSpanishLabel(): String = when (this) {
            Frequency.DAILY -> "Diario"
            Frequency.WEEKLY -> "Semanal"
            Frequency.MONTHLY -> "Mensual"
            Frequency.YEARLY -> "Anual"
        }

        /** Emoji icon for [SourceType] values. */
        fun SourceType.toIcon(): String = when (this) {
            SourceType.WALLET -> "\uD83D\uDCB0"
            SourceType.CARD -> "\u2660"
            SourceType.STASH -> "\uD83D\uDC8E"
        }
    }
}
