package com.vida.feature.onboarding.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Registers the three wizard routes inside the parent NavHost.
 *
 * Each composable uses the same `slideInHorizontally + fadeIn` transition
 * convention used by [com.vida.app.ui.ViDaApp]. Real screen content is
 * filled in by T-WIZ-004 (Welcome), T-WIZ-006 (Wallet-or-Card), and
 * T-WIZ-005 (Get-Started); for now each step renders a private [StubScreen]
 * with two buttons that satisfy the Continue / Skip contract so the parent
 * graph compiles.
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
        StubScreen(
            title = "Welcome",
            continueLabel = "Continuar",
            onContinue = { navController.navigate(OnboardingRoutes.WALLET_OR_CARD) },
            skipLabel = "Saltar",
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
        StubScreen(
            title = "WalletOrCard",
            continueLabel = "Continuar",
            onContinue = { navController.navigate(OnboardingRoutes.GET_STARTED) },
            skipLabel = "Saltar",
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
        StubScreen(
            title = "GetStarted",
            continueLabel = "Ir al inicio",
            onContinue = onFinished,
            skipLabel = "Saltar",
            onSkip = onFinished,
        )
    }
}

/**
 * Temporary placeholder rendered while the real wizard screens are being
 * implemented. Removed in T-WIZ-004 / T-WIZ-005 / T-WIZ-006.
 */
@Composable
private fun StubScreen(
    title: String,
    continueLabel: String,
    onContinue: () -> Unit,
    skipLabel: String,
    onSkip: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "TODO step: $title",
                style = MaterialTheme.typography.headlineSmall,
            )
            Button(onClick = onContinue) {
                Text(continueLabel)
            }
            TextButton(onClick = onSkip) {
                Text(skipLabel)
            }
        }
    }
}
