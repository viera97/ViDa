package com.vida.feature.expenselist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.core.format.formatMoney
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.expense.DeleteExpense
import com.vida.domain.usecase.expense.GetExpense
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
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
    val note: String?,
)

/** One-shot navigation event from the detail ViewModel. */
sealed interface DetailNavigationEvent {
    /** Emitted after successful deletion — navigate back. */
    data object NavigateBack : DetailNavigationEvent
    /** Placeholder for future edit navigation. */
    data class NavigateToEdit(val expenseId: Long) : DetailNavigationEvent
}

@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExpense: GetExpense,
    private val deleteExpense: DeleteExpense,
    private val listCategories: ListCategories,
) : ViewModel() {

    private val expenseId: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow<ExpenseDetailUiState>(ExpenseDetailUiState.Loading)
    val uiState: StateFlow<ExpenseDetailUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<DetailNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    init {
        if (expenseId <= 0L) {
            _uiState.value = ExpenseDetailUiState.Error("ID de gasto inválido")
        } else {
            viewModelScope.launch {
                _uiState.value = ExpenseDetailUiState.Loading
                try {
                    val expense = getExpense(expenseId)
                    if (expense == null) {
                        _uiState.value = ExpenseDetailUiState.Error("Gasto no encontrado")
                        return@launch
                    }

                    val categories = listCategories().first()
                    val cat = categories.find { it.id == expense.categoryId }
                    val sourceLabel = when {
                        expense.sourceType.name == "WALLET" -> "Billetera"
                        expense.sourceType.name == "CARD" -> "Tarjeta #${expense.sourceId}"
                        expense.sourceType.name == "STASH" -> "Reserva #${expense.sourceId}"
                        else -> expense.sourceType.name
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
                            note = expense.note,
                        )
                    )
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    _uiState.value = ExpenseDetailUiState.Error(
                        t.message ?: "No se pudo cargar el gasto",
                    )
                }
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

    fun onEdit() {
        viewModelScope.launch {
            _navigationEvents.send(DetailNavigationEvent.NavigateToEdit(expenseId))
        }
    }
}
