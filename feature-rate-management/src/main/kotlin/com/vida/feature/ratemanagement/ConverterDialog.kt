package com.vida.feature.ratemanagement

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import com.vida.domain.model.Currency
import com.vida.domain.model.CurrencyRate
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Modal dialog for converting an amount between two currencies using an
 * existing exchange rate.
 *
 * Fields:
 * - From currency ([FilterChip] row: USD default)
 * - To currency ([FilterChip] row: CUP default)
 * - Amount ([OutlinedTextField] with decimal keyboard)
 * - Result text — recomputed live as inputs change
 *
 * If [getRate] returns null for the selected pair, an inline message
 * "No hay tasa configurada para X → Y" is shown and the result field is hidden.
 *
 * Buttons:
 * - "Cerrar" — dismisses the dialog
 * - "Copiar" — copies the result string to the system clipboard (enabled only
 *   when a valid conversion is available)
 *
 * @param onDismiss Called when the dialog is dismissed.
 * @param getRate Lookup function returning the most recent [CurrencyRate] for
 *   the given pair, or null if none configured.
 */
@Composable
fun ConverterDialog(
    onDismiss: () -> Unit,
    getRate: (from: Currency, to: Currency) -> CurrencyRate?,
) {
    var fromCurrency by remember { mutableStateOf(Currency.USD) }
    var toCurrency by remember { mutableStateOf(Currency.CUP) }
    var amountText by remember { mutableStateOf("") }
    val context = LocalContext.current

    // ── Amount parsing ───────────────────────────────────────────────────
    val parsedAmount: BigDecimal? = remember(amountText) {
        if (amountText.isBlank()) null
        else runCatching { BigDecimal(amountText) }.getOrNull()
    }

    val fromEqualsTo = fromCurrency == toCurrency

    // ── Rate lookup ───────────────────────────────────────────────────────
    val rate: CurrencyRate? = if (fromEqualsTo) null else getRate(fromCurrency, toCurrency)

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
                // From currency
                Text(
                    text = "De",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Currency.entries.forEach { curr ->
                        FilterChip(
                            selected = fromCurrency == curr,
                            onClick = { fromCurrency = curr },
                            label = { Text(curr.code) },
                        )
                    }
                }

                // To currency
                Text(
                    text = "A",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Currency.entries.forEach { curr ->
                        FilterChip(
                            selected = toCurrency == curr,
                            onClick = { toCurrency = curr },
                            label = { Text(curr.code) },
                        )
                    }
                }

                if (fromEqualsTo) {
                    Text(
                        text = "Las monedas deben ser diferentes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (rate == null) {
                    Text(
                        text = "No hay tasa configurada para ${fromCurrency.code} → ${toCurrency.code}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
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
                        text = "Resultado: $resultText ${toCurrency.code}",
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
