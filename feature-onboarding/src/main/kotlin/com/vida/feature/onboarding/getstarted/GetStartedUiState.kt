package com.vida.feature.onboarding.getstarted

/**
 * UI state for [GetStartedScreen]. The screen is stateless apart from the
 * nav events so a single `data object Ready` is enough — kept distinct
 * from `Loading` so future async data can land here without breaking
 * call sites.
 */
sealed interface GetStartedUiState {
    data object Ready : GetStartedUiState
}
