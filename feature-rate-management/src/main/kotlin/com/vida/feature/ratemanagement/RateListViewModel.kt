package com.vida.feature.ratemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Currency
import com.vida.domain.model.CurrencyRate
import com.vida.domain.usecase.rate.AddCurrencyRate
import com.vida.domain.usecase.rate.DeleteCurrencyRate
import com.vida.domain.usecase.rate.ListCurrencyRates
import com.vida.domain.usecase.rate.UpdateCurrencyRate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for the currency rate list screen.
 *
 * On init, loads all rates via [ListCurrencyRates], sorts them by pair
 * (from→to ascending) then updatedAt DESC, and emits [RateListUiState].
 *
 * Exposes one-shot [RateNavEvent]s via a [Channel] for transient messages
 * (toasts, snackbars).
 */
@HiltViewModel
class RateListViewModel @Inject constructor(
    private val listCurrencyRates: ListCurrencyRates,
    private val addCurrencyRate: AddCurrencyRate,
    private val updateCurrencyRate: UpdateCurrencyRate,
    private val deleteCurrencyRate: DeleteCurrencyRate,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RateListUiState>(RateListUiState.Loading)
    val uiState: StateFlow<RateListUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<RateNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    /** True while a delete operation is in-flight (prevents double-tap). */
    private var isDeleting = false

    /** True while an add/edit operation is in-flight (prevents double-tap). */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadRates()
    }

    // ── Public actions ───────────────────────────────────────────────────────

    /**
     * Adds a new currency rate.
     *
     * Validation: [from] != [to], [rate] > 0, [isSaving] guard.
     * On success the list is refetched and [RateNavEvent.SaveSuccess] is emitted
     * (which closes the dialog). On error a toast is shown and the list is preserved.
     */
    fun onAdd(from: Currency, to: Currency, rate: BigDecimal, updatedAt: Instant) {
        if (_isSaving.value) return
        if (from == to) return
        if (rate.signum() <= 0) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                addCurrencyRate(
                    CurrencyRate(
                        id = 0L,
                        fromCurrency = from,
                        toCurrency = to,
                        rate = rate,
                        updatedAt = updatedAt,
                    ),
                )
                loadRates()
                _navEvents.send(RateNavEvent.SaveSuccess)
                _navEvents.send(RateNavEvent.ShowToast("Tasa agregada"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    RateNavEvent.ShowToast(
                        t.message ?: "No se pudo agregar la tasa",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Updates an existing currency rate with [id].
     *
     * All four fields are editable; no [GetCurrencyRate] use case is needed
     * because no hidden fields exist. Reconstructs [CurrencyRate] from parameters.
     */
    fun onEdit(
        id: Long,
        from: Currency,
        to: Currency,
        rate: BigDecimal,
        updatedAt: Instant,
    ) {
        if (_isSaving.value) return
        if (from == to) return
        if (rate.signum() <= 0) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                updateCurrencyRate(
                    CurrencyRate(
                        id = id,
                        fromCurrency = from,
                        toCurrency = to,
                        rate = rate,
                        updatedAt = updatedAt,
                    ),
                )
                loadRates()
                _navEvents.send(RateNavEvent.SaveSuccess)
                _navEvents.send(RateNavEvent.ShowToast("Tasa actualizada"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    RateNavEvent.ShowToast(
                        t.message ?: "No se pudo actualizar la tasa",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Deletes the rate with [id].
     *
     * On success, the list is refetched and a success toast is emitted.
     * On error, the current list is preserved and an error toast is shown.
     */
    fun onDelete(id: Long) {
        if (isDeleting) return

        val current = _uiState.value
        if (current !is RateListUiState.Ready) return

        val item = current.items.find { it.id == id } ?: return

        viewModelScope.launch {
            isDeleting = true
            try {
                deleteCurrencyRate(id)
                loadRates()
                _navEvents.send(
                    RateNavEvent.ShowToast("Tasa eliminada"),
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    RateNavEvent.ShowToast(
                        t.message ?: "No se pudo eliminar la tasa",
                    ),
                )
            } finally {
                isDeleting = false
            }
        }
    }

    /** Re-initiates the rate fetch from the [RateListUiState.Error] state. */
    fun onDismissError() {
        loadRates()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Fetches rates, sorts them by pair (from→to ascending) then updatedAt DESC,
     * and emits the appropriate [RateListUiState].
     */
    private fun loadRates() {
        viewModelScope.launch {
            try {
                val rates = listCurrencyRates().first()
                val sorted = rates.sortedWith(
                    compareBy<CurrencyRate> {
                        "${it.fromCurrency.code}→${it.toCurrency.code}"
                    }.thenByDescending { it.updatedAt },
                )
                val items = sorted.map { it.toDisplayItem() }

                _uiState.value = if (items.isEmpty()) {
                    RateListUiState.Empty
                } else {
                    RateListUiState.Ready(items = items)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = RateListUiState.Error(
                    message = t.message ?: "No se pudieron cargar las tasas",
                )
            }
        }
    }

    /**
     * Maps a domain [CurrencyRate] to a pre-formatted [RateDisplayItem].
     *
     * - pairLabel: "CUP → USD" format using currency codes.
     * - rateFormatted: stripped trailing zeros, max 2 decimals (e.g. "120.50").
     * - updatedAtFormatted: "dd/MM/yyyy" from [CurrencyRate.updatedAt].
     */
    private fun CurrencyRate.toDisplayItem(): RateDisplayItem {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val zone = ZoneId.systemDefault()
        return RateDisplayItem(
            id = id,
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            pairLabel = "${fromCurrency.code} → ${toCurrency.code}",
            rate = rate,
            rateFormatted = rate.setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString(),
            updatedAt = updatedAt,
            updatedAtFormatted = formatter.format(updatedAt.atZone(zone)),
        )
    }
}
