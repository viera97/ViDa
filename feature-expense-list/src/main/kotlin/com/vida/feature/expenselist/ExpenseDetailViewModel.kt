package com.vida.feature.expenselist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.core.format.formatMoney
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.Refund
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.expense.DeleteExpense
import com.vida.domain.usecase.expense.GetExpense
import com.vida.domain.usecase.refund.AddRefund
import com.vida.domain.usecase.refund.DeleteRefund
import com.vida.domain.usecase.refund.GetRefundsByOriginalExpense
import com.vida.domain.usecase.refund.UpdateRefund
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.ListWallets
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

/** UI state for the expense detail screen. */
sealed interface ExpenseDetailUiState {
    data object Loading : ExpenseDetailUiState
    data class Ready(val expense: ExpenseDetailDisplay) : ExpenseDetailUiState
    data class Error(val message: String) : ExpenseDetailUiState
}

/** Pre-formatted display model for the detail screen. */
data class ExpenseDetailDisplay(
    val id: Long,
    val formattedAmount: String,
    val description: String,
    val categoryName: String,
    val categoryColor: Int,
    val formattedDate: String,
    val sourceLabel: String,
    val sourceType: com.vida.domain.model.SourceType,
    val note: String?,
)

/** One-shot navigation event from the detail ViewModel. */
sealed interface DetailNavigationEvent {
    /** Emitted after successful deletion — navigate back. */
    data object NavigateBack : DetailNavigationEvent
}

/** One-shot snackbar event for refund-operation errors. */
data class SnackbarEvent(val message: String)

/** Refund loading state, separate from [ExpenseDetailUiState]. */
sealed interface RefundUiState {
    data object Loading : RefundUiState
    data class Ready(val refund: RefundDisplay) : RefundUiState
    data object Empty : RefundUiState
    data class Error(val message: String) : RefundUiState
}

/** Pre-formatted display model for a refund. */
data class RefundDisplay(
    val id: Long,
    val amount: BigDecimal,
    val formattedAmount: String,
    val reason: String,
    val formattedDate: String,
    val note: String?,
)

@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExpense: GetExpense,
    private val deleteExpense: DeleteExpense,
    private val listCategories: ListCategories,
    private val listWallets: ListWallets,
    private val listCards: ListCards,
    private val listStashes: ListStashes,
    private val getRefunds: GetRefundsByOriginalExpense,
    private val addRefund: AddRefund,
    private val updateRefund: UpdateRefund,
    private val deleteRefund: DeleteRefund,
) : ViewModel() {

    /** The expense row id from the nav route arg — exposed so the screen can
     *  pass it to the edit modal via [com.vida.feature.expense.ExpenseFormDialog]. */
    val expenseId: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: 0L

    private var expenseCurrency: Currency = Currency.CUP

    private val _uiState = MutableStateFlow<ExpenseDetailUiState>(ExpenseDetailUiState.Loading)
    val uiState: StateFlow<ExpenseDetailUiState> = _uiState.asStateFlow()

    private val _refundState = MutableStateFlow<RefundUiState>(RefundUiState.Loading)
    val refundState: StateFlow<RefundUiState> = _refundState.asStateFlow()

    private val _navigationEvents = Channel<DetailNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    private val _snackbarEvents = Channel<SnackbarEvent>(Channel.BUFFERED)
    val snackbarEvents = _snackbarEvents.receiveAsFlow()

    init {
        if (expenseId <= 0L) {
            _uiState.value = ExpenseDetailUiState.Error("ID de gasto inválido")
            _refundState.value = RefundUiState.Error("ID de gasto inválido")
        } else {
            viewModelScope.launch { loadExpenseData() }
        }
    }

    /**
     * Re-runs the expense + refund load.
     *
     * Called by the screen after a successful edit (the dialog emits `Success`
     * and closes) so the detail page reflects the updated values without
     * requiring a full navigation away-and-back. Mirrors the on-resume refresh
     * pattern used in the list screen.
     */
    fun refresh() {
        if (expenseId <= 0L) return
        viewModelScope.launch { loadExpenseData() }
    }

    private suspend fun loadExpenseData() {
        _uiState.value = ExpenseDetailUiState.Loading
        try {
            val expense = getExpense(expenseId)
            if (expense == null) {
                _uiState.value = ExpenseDetailUiState.Error("Gasto no encontrado")
                _refundState.value = RefundUiState.Error("Gasto no encontrado")
                return
            }

            expenseCurrency = expense.amount.currency

            val categories = listCategories().first()
            val cat = categories.find { it.id == expense.categoryId }

            // Load source entities so we can show their NAMES (e.g.
            // "Efectivo", "Mi BPA", "Ahorro vacaciones") instead of the
            // hardcoded "Billetera" / "Tarjeta #N" / "Reserva #N" labels.
            val wallets = listWallets().first()
            val cards = listCards().first()
            val stashes = listStashes().first()
            val sourceLabel = when (expense.sourceType) {
                com.vida.domain.model.SourceType.WALLET ->
                    wallets.find { it.id == expense.sourceId }?.name ?: "Billetera"
                com.vida.domain.model.SourceType.CARD ->
                    cards.find { it.id == expense.sourceId }
                        ?.let { it.note?.takeIf { n -> n.isNotBlank() } ?: it.bank }
                        ?: "Tarjeta"
                com.vida.domain.model.SourceType.STASH ->
                    stashes.find { it.id == expense.sourceId }?.name ?: "Reserva"
            }

            val formatter = java.time.format.DateTimeFormatter
                .ofPattern("dd MMM yyyy, HH:mm", java.util.Locale("es", "ES"))
            val formattedDate = expense.dateTime
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
                .format(formatter)

            _uiState.value = ExpenseDetailUiState.Ready(
                ExpenseDetailDisplay(
                    id = expense.id,
                    formattedAmount = formatMoney(expense.amount),
                    description = expense.description,
                    categoryName = cat?.name ?: "Sin categoría",
                    categoryColor = cat?.color ?: 0xFF9E9E9E.toInt(),
                    formattedDate = formattedDate,
                    sourceLabel = sourceLabel,
                    sourceType = expense.sourceType,
                    note = expense.note,
                )
            )

            loadRefund()
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            _uiState.value = ExpenseDetailUiState.Error(
                t.message ?: "No se pudo cargar el gasto",
            )
            _refundState.value = RefundUiState.Error("No se pudo cargar el reembolso")
        }
    }

    private suspend fun loadRefund() {
        _refundState.value = RefundUiState.Loading
        try {
            val refunds = getRefunds(expenseId).first()
            if (refunds.isEmpty()) {
                _refundState.value = RefundUiState.Empty
            } else {
                val refund = refunds.first()
                val fmt = java.time.format.DateTimeFormatter
                    .ofPattern("dd MMM yyyy, HH:mm", java.util.Locale("es", "ES"))
                _refundState.value = RefundUiState.Ready(
                    RefundDisplay(
                        id = refund.id,
                        amount = refund.amount.amount,
                        formattedAmount = formatMoney(refund.amount),
                        reason = refund.reason,
                        formattedDate = refund.dateTime
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime()
                            .format(fmt),
                        note = refund.note,
                    )
                )
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            _refundState.value = RefundUiState.Error(
                e.message ?: "No se pudo cargar el reembolso",
            )
        }
    }

    fun onAddRefund(amount: BigDecimal, reason: String, note: String?) {
        viewModelScope.launch {
            try {
                val money = Money(amount, expenseCurrency)
                val refund = Refund(
                    originalExpenseId = expenseId,
                    amount = money,
                    reason = reason,
                    dateTime = Instant.now(),
                    note = note,
                )
                addRefund(refund)
                loadRefund()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _snackbarEvents.send(
                    SnackbarEvent(e.message ?: "No se pudo agregar el reembolso"),
                )
            }
        }
    }

    fun onEditRefund(amount: BigDecimal, reason: String, note: String?) {
        viewModelScope.launch {
            try {
                val current = (_refundState.value as? RefundUiState.Ready)?.refund
                    ?: return@launch
                val money = Money(amount, expenseCurrency)
                val refund = Refund(
                    id = current.id,
                    originalExpenseId = expenseId,
                    amount = money,
                    reason = reason,
                    dateTime = Instant.now(),
                    note = note,
                )
                updateRefund(refund)
                loadRefund()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _snackbarEvents.send(
                    SnackbarEvent(e.message ?: "No se pudo editar el reembolso"),
                )
            }
        }
    }

    fun onDeleteRefund() {
        viewModelScope.launch {
            try {
                val current = (_refundState.value as? RefundUiState.Ready)?.refund
                    ?: return@launch
                deleteRefund(current.id)
                _refundState.value = RefundUiState.Empty
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _snackbarEvents.send(
                    SnackbarEvent(e.message ?: "No se pudo eliminar el reembolso"),
                )
            }
        }
    }

    fun onDelete() {
        viewModelScope.launch {
            try {
                deleteExpense(expenseId)
                _navigationEvents.send(DetailNavigationEvent.NavigateBack)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                // Stay on screen; the error will be surfaced via UI.
                _uiState.value = ExpenseDetailUiState.Error(
                    t.message ?: "No se pudo eliminar el gasto",
                )
            }
        }
    }
}
