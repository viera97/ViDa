package com.vida.feature.onboarding.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vida.feature.onboarding.getstarted.GetStartedScreen
import com.vida.feature.onboarding.walletorcard.WalletOrCardScreen
import com.vida.feature.onboarding.welcome.WelcomeScreen

/**
 * Registers the three wizard routes inside the parent NavHost.
 *
 * Each composable uses the same `slideInHorizontally + fadeIn` transition
 * convention used by [com.vida.app.ui.ViDaApp]. The three real screens
 * (Welcome, Wallet-or-Card, Get-Started) are wired by T-WIZ-004 / T-WIZ-005
 * / T-WIZ-006.
 *
 * @param navController The host NavController — used to navigate between
 *   wizard steps.
 * @param onFinished Callback invoked when the user explicitly finishes or
 *   skips the wizard. The parent should navigate to [OnboardingRoutes.HOME]
 *   and pop the wizard back stack.
 */
fun NavGraphBuilder.onboardingNavGraph(
    navController: NavController,
    onFinished: () -> Unit,
) {
    composable(
        OnboardingRoutes.WELCOME,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
    ) {
        WelcomeScreen(
            onContinue = { navController.navigate(OnboardingRoutes.WALLET_OR_CARD) },
            onSkip = onFinished,
        )
    }
    composable(
        OnboardingRoutes.WALLET_OR_CARD,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
    ) {
        WalletOrCardScreen(
            onContinue = { navController.navigate(OnboardingRoutes.GET_STARTED) },
            onSkip = onFinished,
        )
    }
    composable(
        OnboardingRoutes.GET_STARTED,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300)) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300)) },
    ) {
        GetStartedScreen(onFinish = onFinished)
    }
}
