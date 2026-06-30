package com.vida.feature.onboarding.welcome

/**
 * UI state for [WelcomeScreen]. The screen is stateless apart from the
 * nav events so a single `data object Ready` is enough — kept distinct
 * from `Loading` so future async data can land here without breaking
 * call sites.
 */
sealed interface WelcomeUiState {
    data object Ready : WelcomeUiState
}
