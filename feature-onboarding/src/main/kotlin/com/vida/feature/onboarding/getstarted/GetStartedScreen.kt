package com.vida.feature.onboarding.getstarted

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vida.feature.onboarding.OnboardingCopy.GS_BODY
import com.vida.feature.onboarding.OnboardingCopy.GS_HEADLINE
import com.vida.feature.onboarding.OnboardingCopy.GS_PRIMARY
import com.vida.feature.onboarding.OnboardingCopy.GS_SKIP
import kotlinx.coroutines.launch

/**
 * Third and final wizard screen. Centered layout summarizing what the user
 * can do now that they have at least one financial source registered.
 *
 * "Ir al inicio" and "Saltar" are equivalent: both write the
 * `wizard_completed` flag and pop the wizard back stack so `home` becomes
 * the next route. Hardware back behaves identically so a user dismissing
 * the final step still lands on the home screen instead of being kicked out
 * of the app or back into a previous wizard step.
 *
 * @param onFinish Called after the wizard-completed flag is written — the
 *   parent NavController should pop the wizard and navigate to home.
 * @param viewModel Injected via Hilt; can be overridden in previews.
 */
@Composable
fun GetStartedScreen(
    onFinish: () -> Unit,
    viewModel: GetStartedViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        scope.launch {
            viewModel.markCompleted()
            onFinish()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = GS_HEADLINE,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = GS_BODY,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Button(onClick = {
            scope.launch {
                viewModel.markCompleted()
                onFinish()
            }
        }) {
            Text(text = GS_PRIMARY)
        }
        TextButton(
            onClick = {
                scope.launch {
                    viewModel.markCompleted()
                    onFinish()
                }
            },
        ) {
            Text(text = GS_SKIP)
        }
    }
}
