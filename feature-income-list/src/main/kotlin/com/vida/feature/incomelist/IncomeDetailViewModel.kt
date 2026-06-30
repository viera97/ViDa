package com.vida.feature.incomelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.core.format.formatMoney
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.income.DeleteIncome
import com.vida.domain.usecase.income.GetIncome
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
import javax.inject.Inject

sealed interface IncomeDetailUiState {
    data object Loading : IncomeDetailUiState
    data class Ready(val income: IncomeDetailDisplay) : IncomeDetailUiState
    data class Error(val message: String) : IncomeDetailUiState
}

data class IncomeDetailDisplay(
    val id: Long,
    val formattedAmount: String,
    val description: String,
    val formattedDate: String,
    val sourceLabel: String,
    val sourceType: com.vida.domain.model.SourceType,
    val note: String?,
)

sealed interface IncomeDetailNavigationEvent {
    data object NavigateBack : IncomeDetailNavigationEvent
}

@HiltViewModel
class IncomeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getIncome: GetIncome,
    private val deleteIncome: DeleteIncome,
    private val listCards: ListCards,
    private val listStashes: ListStashes,
    private val listWallets: ListWallets,
) : ViewModel() {

    val incomeId: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow<IncomeDetailUiState>(IncomeDetailUiState.Loading)
    val uiState: StateFlow<IncomeDetailUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<IncomeDetailNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    init {
        if (incomeId <= 0L) {
            _uiState.value = IncomeDetailUiState.Error("ID de ingreso inválido")
        } else {
            viewModelScope.launch { loadIncomeData() }
        }
    }

    fun refresh() {
        if (incomeId <= 0L) return
        viewModelScope.launch { loadIncomeData() }
    }

    private suspend fun loadIncomeData() {
        _uiState.value = IncomeDetailUiState.Loading
        try {
            val income = getIncome(incomeId)
            if (income == null) {
                _uiState.value = IncomeDetailUiState.Error("Ingreso no encontrado")
                return
            }

            val wallets = listWallets().first()
            val cards = listCards().first()
            val stashes = listStashes().first()
            val sourceLabel = when (income.sourceType) {
                com.vida.domain.model.SourceType.WALLET ->
                    wallets.find { it.id == income.sourceId }?.name ?: "Billetera"
                com.vida.domain.model.SourceType.CARD ->
                    cards.find { it.id == income.sourceId }
                        ?.let { it.note?.takeIf { n -> n.isNotBlank() } ?: it.bank }
                        ?: "Tarjeta"
                com.vida.domain.model.SourceType.STASH ->
                    stashes.find { it.id == income.sourceId }?.name ?: "Reserva"
            }

            val formatter = java.time.format.DateTimeFormatter
                .ofPattern("dd MMM yyyy, HH:mm", java.util.Locale("es", "ES"))
            val formattedDate = income.dateTime
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
                .format(formatter)

            _uiState.value = IncomeDetailUiState.Ready(
                IncomeDetailDisplay(
                    id = income.id,
                    formattedAmount = formatMoney(income.amount),
                    description = income.description,
                    formattedDate = formattedDate,
                    sourceLabel = sourceLabel,
                    sourceType = income.sourceType,
                    note = income.note,
                )
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            _uiState.value = IncomeDetailUiState.Error(
                t.message ?: "No se pudo cargar el ingreso",
            )
        }
    }

    fun onDelete() {
        viewModelScope.launch {
            try {
                deleteIncome(incomeId)
                _navigationEvents.send(IncomeDetailNavigationEvent.NavigateBack)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = IncomeDetailUiState.Error(
                    t.message ?: "No se pudo eliminar el ingreso",
                )
            }
        }
    }
}
