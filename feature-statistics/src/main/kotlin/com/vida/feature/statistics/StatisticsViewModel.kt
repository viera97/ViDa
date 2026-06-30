package com.vida.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.usecase.statistics.GetCategoryBreakdown
import com.vida.domain.usecase.statistics.GetCashFlowTrend
import com.vida.domain.usecase.statistics.GetCurrencyComposition
import com.vida.feature.statistics.model.StatsPeriod
import com.vida.feature.statistics.model.StatsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Statistics screen.
 *
 * On init loads data for the default period (EsteMes). When the user selects
 * a different period via [onPeriodChanged], all three charts are recomputed.
 *
 * Follows the same pattern as [com.vida.feature.ratemanagement.RateListViewModel]:
 * [StateFlow] for UI state, [viewModelScope] for coroutines.
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getCategoryBreakdown: GetCategoryBreakdown,
    private val getCashFlowTrend: GetCashFlowTrend,
    private val getCurrencyComposition: GetCurrencyComposition,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    /** Currently selected period. */
    private var currentPeriod: StatsPeriod = StatsPeriod.EsteMes

    init {
        loadData(currentPeriod)
    }

    /**
     * Called by the UI when the user picks a new period.
     * Triggers re-computation of all three chart data sources.
     */
    fun onPeriodChanged(period: StatsPeriod) {
        if (period == currentPeriod) return
        currentPeriod = period
        loadData(period)
    }

    /**
     * Retry loading after an error, keeping the same period.
     */
    fun onRetry() {
        loadData(currentPeriod)
    }

    private fun loadData(period: StatsPeriod) {
        viewModelScope.launch {
            _uiState.value = StatsUiState.Loading
            try {
                val (from, to) = period.toDateRange()
                val bucketMillis = period.bucketMillis()

                val breakdown = getCategoryBreakdown(from, to)
                val cashFlow = getCashFlowTrend(from, to, bucketMillis)
                val composition = getCurrencyComposition(from, to)

                _uiState.value = if (breakdown.isEmpty() && cashFlow.isEmpty() && composition.isEmpty()) {
                    StatsUiState.Empty
                } else {
                    StatsUiState.Ready(
                        period = period,
                        categoryBreakdown = breakdown,
                        cashFlow = cashFlow,
                        currencyComposition = composition,
                    )
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = StatsUiState.Error(
                    message = t.message ?: "No se pudieron cargar las estadísticas",
                )
            }
        }
    }
}
