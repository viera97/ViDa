package com.vida.feature.onboarding.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vida.feature.onboarding.OnboardingCopy.WELCOME_HEADLINE
import com.vida.feature.onboarding.OnboardingCopy.WELCOME_PRIMARY
import com.vida.feature.onboarding.OnboardingCopy.WELCOME_SKIP
import com.vida.feature.onboarding.OnboardingCopy.WELCOME_SUBHEAD
import kotlinx.coroutines.launch

/**
 * First wizard screen. Centered layout with the welcome copy and two
 * affordances: "Empezar" advances to the wallet-or-card step; "Saltar"
 * marks the wizard as completed and exits to home. Hardware back behaves
 * identically to "Saltar" so a user dismissing the wizard before reading
 * it still lands on the home screen instead of being kicked out of the app.
 *
 * @param onContinue Called when the user taps "Empezar".
 * @param onSkip Called after the wizard-completed flag is written — the
 *   parent NavController should pop the wizard and navigate to home.
 * @param viewModel Injected via Hilt; can be overridden in previews.
 */
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        scope.launch {
            viewModel.markCompleted()
            onSkip()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = WELCOME_HEADLINE,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = WELCOME_SUBHEAD,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onContinue) {
            Text(text = WELCOME_PRIMARY)
        }
        TextButton(
            onClick = {
                scope.launch {
                    viewModel.markCompleted()
                    onSkip()
                }
            },
        ) {
            Text(text = WELCOME_SKIP)
        }
    }
}
