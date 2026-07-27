package com.vida.feature.ratemanagement

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vida.domain.model.CurrencyRate
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Modal dialog for converting an amount between two currencies using an
 * existing exchange rate.
 *
 * Fields:
 * - From currency ([ExposedDropdownMenuBox], defaults to "USD")
 * - To currency ([ExposedDropdownMenuBox], defaults to "CUP")
 * - Amount ([OutlinedTextField] with decimal keyboard)
 * - Result text — recomputed live as inputs change
 *
 * If [getRate] returns null for the selected pair/provider, an inline message
 * "No hay tasa configurada para X → Y (proveedor)" is shown and the result
 * field is hidden.
 *
 * The provider selector above the amount defaults to "Manual" when present
 * in [availableProviders]; otherwise it defaults to the first available.
 *
 * Buttons:
 * - "Cerrar" — dismisses the dialog
 * - "Copiar" — copies the result string to the system clipboard (enabled only
 *   when a valid conversion is available)
 *
 * @param onDismiss Called when the dialog is dismissed.
 * @param getRate Lookup function returning the most recent [CurrencyRate] for
 *   the given pair and provider, or null if none configured.
 * @param availableProviders Distinct providers that have at least one rate
 *   configured. The dropdown shows these options.
 * @param availableCurrencies Currency codes available for the from/to dropdowns.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterDialog(
    onDismiss: () -> Unit,
    getRate: (fromCode: String, toCode: String, provider: String) -> CurrencyRate?,
    availableProviders: List<String>,
    availableCurrencies: List<String> = emptyList(),
) {
    val defaultFrom = availableCurrencies.firstOrNull() ?: "USD"
    val defaultTo = availableCurrencies.firstOrNull { it != defaultFrom } ?: "CUP"

    var fromCurrency by remember { mutableStateOf(defaultFrom) }
    var toCurrency by remember { mutableStateOf(defaultTo) }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }

    // Default provider: "Manual" if available, else first entry, else "Manual".
    val defaultProvider = availableProviders.firstOrNull { it == "Manual" }
        ?: availableProviders.firstOrNull()
        ?: "Manual"
    var selectedProvider by remember(availableProviders) { mutableStateOf(defaultProvider) }
    var providerMenuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // ── Amount parsing ───────────────────────────────────────────────────
    val parsedAmount: BigDecimal? = remember(amountText) {
        if (amountText.isBlank()) null
        else runCatching { BigDecimal(amountText) }.getOrNull()
    }

    val fromEqualsTo = fromCurrency == toCurrency

    // ── Rate lookup ───────────────────────────────────────────────────────
    val rate: CurrencyRate? = if (fromEqualsTo) null
    else getRate(fromCurrency, toCurrency, selectedProvider)

    val amountError: String? = when {
        amountText.isBlank() -> null
        parsedAmount == null -> "Número inválido"
        parsedAmount.signum() < 0 -> "La cantidad debe ser positiva"
        else -> null
    }

    // ── Result computation ───────────────────────────────────────────────
    val resultText: String? = when {
        fromEqualsTo -> null
        rate == null -> null
        amountText.isBlank() -> null
        parsedAmount == null -> null
        parsedAmount.signum() < 0 -> null
        else -> {
            val raw = parsedAmount.multiply(rate.rate)
            raw.setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        }
    }

    val canCopy = resultText != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Convertir") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // From currency — dropdown
                CurrencySelector(
                    selectedCurrencyCode = fromCurrency,
                    label = "De",
                    onShowSheet = { fromExpanded = true },
                )
                if (fromExpanded) {
                    CurrencyPickerSheet(
                        availableCurrencies = availableCurrencies,
                        selectedCurrencyCode = fromCurrency,
                        onDismiss = { fromExpanded = false },
                        onCurrencySelected = { code ->
                            fromCurrency = code
                            if (toCurrency == code) {
                                toCurrency = availableCurrencies.firstOrNull { it != code } ?: "CUP"
                            }
                            fromExpanded = false
                        },
                    )
                }

                // To currency — dropdown
                CurrencySelector(
                    selectedCurrencyCode = toCurrency,
                    label = "A",
                    onShowSheet = { toExpanded = true },
                )
                if (toExpanded) {
                    CurrencyPickerSheet(
                        availableCurrencies = availableCurrencies,
                        selectedCurrencyCode = toCurrency,
                        onDismiss = { toExpanded = false },
                        onCurrencySelected = { code ->
                            toCurrency = code
                            toExpanded = false
                        },
                    )
                }

                if (fromEqualsTo) {
                    Text(
                        text = "Las monedas deben ser diferentes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (rate == null) {
                    Text(
                        text = "No hay tasa configurada para $fromCurrency → $toCurrency ($selectedProvider)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                // Provider selector — above Cantidad
                CurrencySelector(
                    selectedCurrencyCode = selectedProvider,
                    label = "Proveedor",
                    onShowSheet = { providerMenuExpanded = true },
                )
                if (providerMenuExpanded) {
                    ProviderPickerSheet(
                        availableProviders = availableProviders,
                        selectedProvider = selectedProvider,
                        onDismiss = { providerMenuExpanded = false },
                        onProviderSelected = { provider ->
                            selectedProvider = provider
                            providerMenuExpanded = false
                        },
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Cantidad") },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Result
                if (resultText != null) {
                    Text(
                        text = "Resultado: $resultText $toCurrency",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (rate != null && amountText.isNotBlank() && amountError == null && !fromEqualsTo) {
                    // Rate exists but amount is invalid — already handled above
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    resultText?.let { copyToClipboard(context, it) }
                },
                enabled = canCopy,
            ) {
                Text("Copiar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
    )
}

/**
 * Copies [text] to the system clipboard. Shows a toast confirming the copy.
 */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Conversión", text)
    clipboard.setPrimaryClip(clip)
    android.widget.Toast.makeText(context, "Copiado", android.widget.Toast.LENGTH_SHORT).show()
}
