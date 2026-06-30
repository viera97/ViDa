package com.vida.feature.statistics.model

import com.vida.domain.model.statistics.CategoryBreakdown
import com.vida.domain.model.statistics.CashFlowPoint
import com.vida.domain.model.statistics.CurrencyComposition

/**
 * UI state for the Statistics screen.
 *
 * Following the existing project convention of sealed interface UiStates
 * (see [com.vida.feature.ratemanagement.RateListUiState]).
 */
sealed interface StatsUiState {

    /** Initial load in progress. */
    data object Loading : StatsUiState

    /** Data loaded and ready to render. */
    data class Ready(
        val period: StatsPeriod,
        val categoryBreakdown: List<CategoryBreakdown>,
        val cashFlow: List<CashFlowPoint>,
        val currencyComposition: List<CurrencyComposition>,
    ) : StatsUiState

    /** No data for the selected period. */
    data object Empty : StatsUiState

    /** Something went wrong. */
    data class Error(val message: String) : StatsUiState
}
