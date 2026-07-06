package com.vida.feature.onboarding.walletorcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Wallet form composable — skeleton. Real fields land in T-WIZ-007.
 */
@Composable
fun WalletForm(
    state: WalletOrCardUiState.EditingWallet,
    onNameChange: (String) -> Unit,
    onCurrencyChange: (com.vida.domain.model.Currency) -> Unit,
    onBalanceChange: (String) -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "TODO WalletForm (saving=$isSaving, name=${state.name}, currency=${state.currency.code}, balance=${state.balance})",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Card form composable — skeleton. Real fields land in T-WIZ-008.
 */
@Composable
fun CardForm(
    state: WalletOrCardUiState.EditingCard,
    onBankChange: (String) -> Unit,
    onLast4Change: (String) -> Unit,
    onCurrencyChange: (com.vida.domain.model.Currency) -> Unit,
    onBalanceChange: (String) -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "TODO CardForm (saving=$isSaving, bank=${state.bank}, last4=${state.last4}, currency=${state.currency.code}, balance=${state.balance})",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
