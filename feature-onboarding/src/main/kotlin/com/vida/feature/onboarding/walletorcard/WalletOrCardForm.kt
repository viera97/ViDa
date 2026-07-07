package com.vida.feature.onboarding.walletorcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vida.domain.model.Currency
import com.vida.feature.onboarding.OnboardingCopy.WOC_CARD_EXPIRY_NOTE
import com.vida.feature.onboarding.OnboardingCopy.WOC_ERR_BALANCE_PARSE
import com.vida.feature.onboarding.OnboardingCopy.WOC_FIELD_BANK
import com.vida.feature.onboarding.OnboardingCopy.WOC_FIELD_CARD_BALANCE
import com.vida.feature.onboarding.OnboardingCopy.WOC_FIELD_CURRENCY
import com.vida.feature.onboarding.OnboardingCopy.WOC_FIELD_LAST4
import com.vida.feature.onboarding.OnboardingCopy.WOC_FIELD_NAME
import com.vida.feature.onboarding.OnboardingCopy.WOC_FIELD_WALLET_BALANCE

/**
 * Wallet form composable — real fields (T-WIZ-007). The submit button lives
 * in [WalletOrCardScreen] so the same dispatch (wallet vs card) stays in one
 * place; this composable is purely presentational.
 *
 * Fields:
 * - [OutlinedTextField] `WOC_FIELD_NAME` — required, 1–100 chars. Inline
 *   error if blank (after trim) or too long.
 * - Currency — [FilterChip] row over every [Currency] entry (CUP, USD,
 *   MLC, EUR in this codebase).
 * - [OutlinedTextField] `WOC_FIELD_WALLET_BALANCE` — decimal, optional,
 *   empty means 0.00. Inline error on parse failure.
 *
 * @param isSaving Whether a save is in-flight; disables every input.
 * @param onSubmit Reserved for callers that want to embed their own submit
 *   button. The default is a no-op because [WalletOrCardScreen] owns the
 *   outer "Continuar" button.
 */
@Composable
fun WalletForm(
    state: WalletOrCardUiState.EditingWallet,
    onNameChange: (String) -> Unit,
    onCurrencyChange: (Currency) -> Unit,
    onBalanceChange: (String) -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
    onSubmit: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text(WOC_FIELD_NAME) },
            isError = state.nameError != null,
            supportingText = state.nameError?.let { msg -> { Text(msg) } },
            enabled = !isSaving,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = WOC_FIELD_CURRENCY,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Currency.entries.forEach { curr ->
                FilterChip(
                    selected = state.currency == curr,
                    onClick = { onCurrencyChange(curr) },
                    enabled = !isSaving,
                    label = { Text(curr.code) },
                )
            }
        }

        OutlinedTextField(
            value = state.balance,
            onValueChange = { input ->
                // Accept only digits and a single '.'
                val sanitized = input.filter { it.isDigit() || it == '.' }
                val firstDot = sanitized.indexOf('.')
                val normalized = if (firstDot < 0) sanitized
                else sanitized.substring(0, firstDot + 1) +
                    sanitized.substring(firstDot + 1).replace(".", "")
                onBalanceChange(normalized)
            },
            label = { Text(WOC_FIELD_WALLET_BALANCE) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = state.balanceError != null,
            supportingText = state.balanceError?.let { msg -> { Text(msg) } }
                ?: state.balanceError?.let { { Text(WOC_ERR_BALANCE_PARSE) } },
            enabled = !isSaving,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Card form composable — real fields (T-WIZ-008). The submit button lives in
 * [WalletOrCardScreen] so the same dispatch (wallet vs card) stays in one
 * place; this composable is purely presentational.
 *
 * Fields:
 * - Banco — `OutlinedTextField`, required. The wizard pre-fills nothing;
 *   the user picks a bank on their own.
 * - Últimos 4 dígitos — `OutlinedTextField` with `KeyboardType.NumberPassword`,
 *   length-clamped to 4 digits in the screen-level handler that calls
 *   [onLast4Change] (the ViewModel further validates the exact 4-digit
 *   format on submit).
 * - Currency — `FilterChip` row mirroring `WalletForm`.
 * - Saldo — decimal, optional, empty means 0.00. Inline error on parse
 *   failure.
 *
 * A small grey [WOC_CARD_EXPIRY_NOTE] explains that the expiration date
 * lands 4 years in the future and is editable from the Card screen later;
 * `LocalDate.now().plusYears(4)` is set inside
 * [WalletOrCardViewModel.submitCard].
 *
 * @param isSaving Whether a save is in-flight; disables every input.
 */
@Composable
fun CardForm(
    state: WalletOrCardUiState.EditingCard,
    onBankChange: (String) -> Unit,
    onLast4Change: (String) -> Unit,
    onCurrencyChange: (Currency) -> Unit,
    onBalanceChange: (String) -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
    onSubmit: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        OutlinedTextField(
            value = state.bank,
            onValueChange = onBankChange,
            label = { Text(WOC_FIELD_BANK) },
            isError = state.bankError != null,
            supportingText = state.bankError?.let { msg -> { Text(msg) } },
            enabled = !isSaving,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.last4,
            onValueChange = { input ->
                // Hard-clamp length and digits at the input layer so the
                // ViewModel still double-checks the format on submit.
                if (input.length <= 4 && input.all { it.isDigit() }) {
                    onLast4Change(input)
                }
            },
            label = { Text(WOC_FIELD_LAST4) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = state.last4Error != null,
            supportingText = state.last4Error?.let { msg -> { Text(msg) } },
            enabled = !isSaving,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = WOC_FIELD_CURRENCY,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Currency.entries.forEach { curr ->
                FilterChip(
                    selected = state.currency == curr,
                    onClick = { onCurrencyChange(curr) },
                    enabled = !isSaving,
                    label = { Text(curr.code) },
                )
            }
        }

        OutlinedTextField(
            value = state.balance,
            onValueChange = { input ->
                val sanitized = input.filter { it.isDigit() || it == '.' }
                val firstDot = sanitized.indexOf('.')
                val normalized = if (firstDot < 0) sanitized
                else sanitized.substring(0, firstDot + 1) +
                    sanitized.substring(firstDot + 1).replace(".", "")
                onBalanceChange(normalized)
            },
            label = { Text(WOC_FIELD_CARD_BALANCE) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = state.balanceError != null,
            supportingText = state.balanceError?.let { msg ->
                { Text(if (msg.isBlank()) WOC_ERR_BALANCE_PARSE else msg) }
            },
            enabled = !isSaving,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = WOC_CARD_EXPIRY_NOTE,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
