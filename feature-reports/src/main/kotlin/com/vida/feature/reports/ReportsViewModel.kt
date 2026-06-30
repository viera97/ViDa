package com.vida.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.core.format.PeriodLabels
import com.vida.core.format.formatMoney
import com.vida.domain.model.statistics.ReportsPeriod
import com.vida.domain.model.statistics.ReportsPeriod.Hoy
import com.vida.domain.usecase.statistics.GetPeriodReports
import com.vida.feature.reports.model.CategoryRow
import com.vida.feature.reports.model.MoneyRow
import com.vida.feature.reports.model.ReportListItem
import com.vida.feature.reports.model.ReportsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Reports screen.
 *
 * On init loads data for the default period ([ReportsPeriod.Hoy]). When the user
 * selects a different period via [onPeriodChanged], the list is re-fetched. Maps
 * the domain [com.vida.domain.model.statistics.PeriodReportEntry] list into UI
 * [ReportListItem] projections, then flips ASC → DESC for newest-first rendering.
 *
 * Follows the same pattern as
 * [com.vida.feature.statistics.StatisticsViewModel]:
 * [StateFlow] for UI state, [viewModelScope] for coroutines, [CancellationException]
 * re-thrown so structured concurrency stays intact.
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val getPeriodReports: GetPeriodReports,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportsUiState>(ReportsUiState.Loading)
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private var currentPeriod: ReportsPeriod = ReportsPeriod.Hoy

    init {
        loadData(currentPeriod)
    }

    /**
     * Called by the UI when the user picks a new period.
     * Triggers re-fetch when [period] differs from the current one.
     */
    fun onPeriodChanged(period: ReportsPeriod) {
        if (period == currentPeriod) return
        currentPeriod = period
        loadData(period)
    }

    /** Retry loading after an error, keeping the same period. */
    fun onRetry() {
        loadData(currentPeriod)
    }

    private fun loadData(period: ReportsPeriod) {
        viewModelScope.launch {
            _uiState.value = ReportsUiState.Loading
            try {
                val (from, to) = period.toDateRange()
                val bucketMillis = period.bucketMillis()

                val entries = getPeriodReports(from, to, bucketMillis)

                _uiState.value = if (entries.isEmpty()) {
                    ReportsUiState.Empty
                } else {
                    ReportsUiState.Ready(
                        period = period,
                        entries = entries.asReversed().map { it.toListItem(bucketMillis) },
                    )
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = ReportsUiState.Error(
                    message = t.message ?: "No se pudieron cargar los reportes",
                )
            }
        }
    }

    private fun com.vida.domain.model.statistics.PeriodReportEntry.toListItem(
        bucketMillis: Long,
    ): ReportListItem {
        val categoryRows = categoryBreakdown.map { cb ->
            CategoryRow(
                categoryName = cb.categoryName,
                currency = cb.total.currency,
                amountLabel = formatMoney(cb.total),
            )
        }
        val incomeRows = incomeByCurrency
            .toSortedMap(compareBy { it.code })
            .map { (c, m) -> MoneyRow(c, formatMoney(m), m.isNegative()) }
        val expenseRows = expenseByCurrency
            .toSortedMap(compareBy { it.code })
            .map { (c, m) -> MoneyRow(c, formatMoney(m), m.isNegative()) }
        val netRows = netByCurrency
            .toSortedMap(compareBy { it.code })
            .map { (c, m) ->
                MoneyRow(
                    currency = c,
                    amountLabel = formatMoney(m),
                    isNegative = m.isNegative(),
                )
            }
        return ReportListItem(
            periodStart = periodStart,
            periodLabel = PeriodLabels.formatPeriodLabel(periodStart, bucketMillis),
            categoryRows = categoryRows,
            incomeRows = incomeRows,
            expenseRows = expenseRows,
            netRows = netRows,
        )
    }
}
