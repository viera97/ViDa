package com.vida.feature.reports.model

import com.vida.domain.model.statistics.ReportsPeriod

/**
 * UI state for the Reports screen.
 *
 * Mirrors [com.vida.feature.statistics.model.StatsUiState] shape: four branches
 * (Loading, Ready, Empty, Error). `Ready` carries the active [ReportsPeriod] so the
 * active chip can be re-displayed after configuration change.
 */
sealed interface ReportsUiState {

    /** Initial load or period change in progress. */
    data object Loading : ReportsUiState

    /** Data loaded and ready to render. */
    data class Ready(
        val period: ReportsPeriod,
        val entries: List<ReportListItem>,
    ) : ReportsUiState

    /** No data for the selected period (all three sources empty). */
    data object Empty : ReportsUiState

    /** Something went wrong. */
    data class Error(val message: String) : ReportsUiState
}
