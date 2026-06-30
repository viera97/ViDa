package com.vida.feature.onboarding.nav

/**
 * String routes for the first-run wizard subgraph. Strings match the
 * conventions used everywhere else in [com.vida.app.ui.ViDaApp].
 *
 * - [WELCOME] / [WALLET_OR_CARD] / [GET_STARTED] are wizard destinations.
 * - [HOME] is the post-wizard destination and is shared with the
 *   root NavHost.
 * - [WIZARD_PREFIX] is used by [com.vida.app.ui.ViDaApp] to suppress the
 *   bottom [androidx.compose.material3.NavigationBar].
 */
object OnboardingRoutes {
    const val WELCOME = "wizard/welcome"
    const val WALLET_OR_CARD = "wizard/wallet-or-card"
    const val GET_STARTED = "wizard/get-started"
    const val HOME = "home"
    const val WIZARD_PREFIX = "wizard/"
}

/**
 * Sealed destination hierarchy used by the wizard composables. Concrete
 * steps wrap their corresponding [OnboardingRoutes] constant.
 */
sealed class OnboardingDestination(val route: String) {
    data object Welcome : OnboardingDestination(OnboardingRoutes.WELCOME)
    data object WalletOrCard : OnboardingDestination(OnboardingRoutes.WALLET_OR_CARD)
    data object GetStarted : OnboardingDestination(OnboardingRoutes.GET_STARTED)
}
