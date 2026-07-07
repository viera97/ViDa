package com.vida.feature.onboarding.walletorcard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vida.feature.onboarding.OnboardingCopy.WOC_HEADLINE
import com.vida.feature.onboarding.OnboardingCopy.WOC_PRIMARY
import com.vida.feature.onboarding.OnboardingCopy.WOC_SEGMENT_CARD
import com.vida.feature.onboarding.OnboardingCopy.WOC_SEGMENT_WALLET
import com.vida.feature.onboarding.OnboardingCopy.WOC_SKIP
import com.vida.feature.onboarding.OnboardingCopy.WOC_SUBHEAD
import kotlinx.coroutines.launch

/**
 * Wizard step 2 — segmented chooser between "Billetera" and "Tarjeta". The
 * form below swaps based on the active segment. "Saltar" or hardware-back
 * exits the wizard with the completion flag set.
 *
 * @param onContinue Navigate to the get-started step after a successful
 *   wallet or card submission.
 * @param onSkip Navigate to home with the wizard back stack popped.
 * @param viewModel Injected via Hilt; can be overridden in previews.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WalletOrCardScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    viewModel: WalletOrCardViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val segment by viewModel.segment.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = true) {
        scope.launch {
            viewModel.markCompleted()
            onSkip()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.navEvents.collect { event ->
            when (event) {
                WalletOrCardNavEvent.Continue -> onContinue()
                is WalletOrCardNavEvent.Snackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val isSaving = uiState is WalletOrCardUiState.SavingWallet ||
        uiState is WalletOrCardUiState.SavingCard

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = WOC_HEADLINE,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = WOC_SUBHEAD,
                style = MaterialTheme.typography.bodyMedium,
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                WizardSegment.entries.forEachIndexed { index, value ->
                    SegmentedButton(
                        selected = segment == value,
                        onClick = { viewModel.onSegmentChange(value) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = WizardSegment.entries.size,
                        ),
                    ) {
                        Text(
                            text = if (value == WizardSegment.WALLET) WOC_SEGMENT_WALLET
                            else WOC_SEGMENT_CARD,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(top = 8.dp))

            when (segment) {
                WizardSegment.WALLET -> {
                    val state = uiState as? WalletOrCardUiState.EditingWallet
                        ?: WalletOrCardUiState.EditingWallet()
                    WalletForm(
                        state = state,
                        onNameChange = viewModel::onNameChange,
                        onCurrencyChange = viewModel::onCurrencyChange,
                        onBalanceChange = viewModel::onBalanceChange,
                        isSaving = isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                WizardSegment.CARD -> {
                    val state = uiState as? WalletOrCardUiState.EditingCard
                        ?: WalletOrCardUiState.EditingCard()
                    CardForm(
                        state = state,
                        onBankChange = viewModel::onBankChange,
                        onLast4Change = viewModel::onLast4Change,
                        onCurrencyChange = viewModel::onCurrencyChange,
                        onBalanceChange = viewModel::onBalanceChange,
                        isSaving = isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.padding(top = 8.dp))

            Button(
                onClick = {
                    when (segment) {
                        WizardSegment.WALLET -> viewModel.submitWallet()
                        WizardSegment.CARD -> viewModel.submitCard()
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = WOC_PRIMARY)
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        viewModel.markCompleted()
                        onSkip()
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().align(Alignment.End),
            ) {
                Text(text = WOC_SKIP)
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}
