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
 * On init, loads all rates via [ListCurrencyRates], normalizes them into
 * [RateDisplayItem]s (each carrying its inverse rate when present), and emits
 * [RateListUiState].
 *
 * Inverse rate convention: whenever a rate X → Y is added or edited, the
 * matching Y → X rate (with `rate = 1 / rate`) is created/updated with the
 * same provider and date. Both are deleted together when either is removed.
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

    /** In-memory cache of all rates, kept up-to-date with [loadRates]. */
    private var cachedRates: List<CurrencyRate> = emptyList()

    /** Distinct providers present in [cachedRates], sorted alphabetically. */
    private val _availableProviders = MutableStateFlow<List<String>>(emptyList())
    val availableProviders: StateFlow<List<String>> = _availableProviders.asStateFlow()

    init {
        loadRates()
    }

    // ── Public actions ───────────────────────────────────────────────────────

    /**
     * Adds a new currency rate AND its inverse (to → from) for the same
     * provider and date.
     *
     * Validation: [from] != [to], [rate] > 0, [isSaving] guard. Duplicate
     * check is `(from, to, provider)` — if it fails, no insert is performed.
     * On success the list is refetched and [RateNavEvent.SaveSuccess] is
     * emitted. On error a toast is shown and the list is preserved.
     */
    fun onAdd(from: Currency, to: Currency, rate: BigDecimal, updatedAt: Instant, provider: String) {
        if (_isSaving.value) return
        if (from == to) return
        if (rate.signum() <= 0) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Duplicate check on the primary pair
                val existingRates = listCurrencyRates().first()
                val isDuplicate = existingRates.any { r ->
                    r.fromCurrency == from &&
                        r.toCurrency == to &&
                        r.provider == provider
                }
                if (isDuplicate) {
                    _navEvents.send(RateNavEvent.DuplicateRate)
                    return@launch
                }

                // 1. Insert primary
                addCurrencyRate(
                    CurrencyRate(
                        id = 0L,
                        fromCurrency = from,
                        toCurrency = to,
                        rate = rate,
                        updatedAt = updatedAt,
                        provider = provider,
                    ),
                )

                // 2. Insert inverse (rate = 1 / rate). Don't fail the whole
                //    operation if the inverse insert fails — the primary is
                //    already saved and the user can re-edit to fix the inverse.
                val inverseRate = inverseOf(rate)
                runCatching {
                    addCurrencyRate(
                        CurrencyRate(
                            id = 0L,
                            fromCurrency = to,
                            toCurrency = from,
                            rate = inverseRate,
                            updatedAt = updatedAt,
                            provider = provider,
                        ),
                    )
                }

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
     * Updates an existing currency rate (and its inverse, if any) with [id].
     *
     * The primary rate is always updated. The inverse is recomputed from
     * the new rate: if an inverse already exists in storage, it's updated;
     * if not, it's created. If the user changed the (from, to) pair, the
     * stale inverse (matching the OLD from/to) is also removed to keep
     * storage consistent.
     */
    fun onEdit(
        id: Long,
        from: Currency,
        to: Currency,
        rate: BigDecimal,
        updatedAt: Instant,
        provider: String,
    ) {
        if (_isSaving.value) return
        if (from == to) return
        if (rate.signum() <= 0) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val existing = cachedRates.firstOrNull { it.id == id }
                val oldFrom = existing?.fromCurrency
                val oldTo = existing?.toCurrency

                // 1. Update primary
                updateCurrencyRate(
                    CurrencyRate(
                        id = id,
                        fromCurrency = from,
                        toCurrency = to,
                        rate = rate,
                        updatedAt = updatedAt,
                        provider = provider,
                    ),
                )

                // 2. Refresh inverse: find existing one for the NEW pair, if any
                val current = listCurrencyRates().first()
                val newInverse = current.firstOrNull {
                    it.fromCurrency == to && it.toCurrency == from
                }
                val newInverseRate = inverseOf(rate)
                if (newInverse != null) {
                    runCatching {
                        updateCurrencyRate(
                            newInverse.copy(
                                rate = newInverseRate,
                                updatedAt = updatedAt,
                                provider = provider,
                            ),
                        )
                    }
                } else {
                    runCatching {
                        addCurrencyRate(
                            CurrencyRate(
                                id = 0L,
                                fromCurrency = to,
                                toCurrency = from,
                                rate = newInverseRate,
                                updatedAt = updatedAt,
                                provider = provider,
                            ),
                        )
                    }
                }

                // 3. If the (from, to) pair changed, the OLD inverse is now
                //    stale and must be removed.
                if (oldFrom != null && oldTo != null && (oldFrom != from || oldTo != to)) {
                    val staleInverse = current.firstOrNull {
                        it.fromCurrency == oldTo && it.toCurrency == oldFrom
                    }
                    staleInverse?.let { runCatching { deleteCurrencyRate(it.id) } }
                }

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
     * Deletes the rate with [id] AND its inverse (to → from) if present.
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
                // Delete primary
                deleteCurrencyRate(id)
                // Delete inverse if present
                item.inverse?.let { inv ->
                    runCatching { deleteCurrencyRate(inv.id) }
                }
                loadRates()
                _navEvents.send(RateNavEvent.ShowToast("Tasa eliminada"))
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

    /**
     * Returns the most recent [CurrencyRate] for the given pair, or null if
     * no rate has been configured.
     *
     * Used by [ConverterDialog] to perform live conversion without duplicating
     * the list logic.
     */
    fun getRateForConversion(
        from: Currency,
        to: Currency,
        provider: String = "Manual",
    ): CurrencyRate? {
        return cachedRates
            .filter {
                it.fromCurrency == from &&
                    it.toCurrency == to &&
                    it.provider == provider
            }
            .maxByOrNull { it.updatedAt }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Fetches rates, pairs each one with its inverse when present, and emits
     * the appropriate [RateListUiState].
     *
     * Each pair (X→Y) is rendered once in the grid; the inverse (Y→X) is
     * nested inside the same card. To avoid double-rendering, both
     * directions of the same pair land in the same [groupKey] bucket and we
     * only emit one card per bucket — the most-recently-updated direction
     * becomes the card headline (see [buildDisplayItem]).
     */
    private fun loadRates() {
        viewModelScope.launch {
            try {
                val rates = listCurrencyRates().first()
                cachedRates = rates
                _availableProviders.value = rates.map { it.provider }.distinct().sorted()

                // Group by normalized pair key + provider. The normalized pair
                // key collapses X→Y with Y→X into the same group; provider
                // keeps different sources separated as different cards.
                val byGroup = rates.groupBy { groupKey(it.fromCurrency, it.toCurrency, it.provider) }
                val items = byGroup.values
                    .map { group -> buildDisplayItem(group) }
                    .sortedWith(
                        // Most-recently updated rate first (matches the
                        // primary-direction rule above so the freshly
                        // created/edited card surfaces on top).
                        compareByDescending<RateDisplayItem> { it.updatedAt }
                            .thenBy { it.pairLabel }
                            .thenBy { it.provider },
                    )

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
     * Builds a single [RateDisplayItem] for a group of rates sharing the same
     * normalized pair and provider.
     *
     * - Primary direction: the one the user touched most recently (i.e. with
     *   the latest `updatedAt`). This means the card headline reflects the
     *   pair the user actually created/edited, instead of always picking the
     *   alphabetically smaller `fromCurrency.code` (which would flip CUP↔USD
     *   rates against user intent).
     * - If only one direction exists, it is the primary regardless.
     * - The inverse is the other direction (when present) for the same
     *   provider.
     */
    private fun buildDisplayItem(group: List<CurrencyRate>): RateDisplayItem {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val zone = ZoneId.systemDefault()

        // Within the group, keep only the most recent rate per direction.
        val byDirection = group.groupBy { it.fromCurrency to it.toCurrency }
        val latestPerDirection = byDirection.values.map { dir ->
            dir.maxByOrNull { it.updatedAt }!!
        }

        // Primary direction: the one with the latest updatedAt (the pair the
        // user last created or edited). Ties broken by pairLabel so the order
        // is deterministic.
        val primaryDirection = latestPerDirection.maxWithOrNull(
            compareBy<CurrencyRate> { it.updatedAt }
                .thenBy { "${it.fromCurrency.code} → ${it.toCurrency.code}" },
        )!!
        val inverseDirection = latestPerDirection.firstOrNull {
            it.fromCurrency == primaryDirection.toCurrency &&
                it.toCurrency == primaryDirection.fromCurrency
        }

        return RateDisplayItem(
            id = primaryDirection.id,
            fromCurrency = primaryDirection.fromCurrency,
            toCurrency = primaryDirection.toCurrency,
            pairLabel = "${primaryDirection.fromCurrency.code} → ${primaryDirection.toCurrency.code}",
            rate = primaryDirection.rate,
            rateFormatted = formatRate(primaryDirection.rate),
            provider = primaryDirection.provider,
            updatedAt = primaryDirection.updatedAt,
            updatedAtFormatted = formatter.format(primaryDirection.updatedAt.atZone(zone)),
            inverse = inverseDirection?.let { inv ->
                InverseRateDisplay(
                    id = inv.id,
                    fromCurrency = inv.fromCurrency,
                    toCurrency = inv.toCurrency,
                    rate = inv.rate,
                    rateFormatted = formatRate(inv.rate),
                )
            },
        )
    }

    /**
     * Normalized group key: collapses X→Y with Y→X into the same bucket,
     * but keeps different providers as separate buckets so BCV and Manual
     * rates for the same pair render as two distinct cards.
     */
    private fun groupKey(from: Currency, to: Currency, provider: String): String {
        val a = from.code
        val b = to.code
        val pair = if (a <= b) "$a-$b" else "$b-$a"
        return "$pair@$provider"
    }

    /** Inverse of a rate: 1 / rate, scaled to 10 decimals with HALF_UP. */
    private fun inverseOf(rate: BigDecimal): BigDecimal {
        return BigDecimal.ONE.divide(rate, 10, RoundingMode.HALF_UP)
    }

    /** Strip trailing zeros, preserving precision.
     *
     *  - For rates >= 1: at most 2 decimals (e.g. "120.5", "270").
     *  - For rates < 1: at most 6 significant digits (e.g. "0.0083") so the
     *    inverse of a 3-digit rate stays meaningful.
     */
    private fun formatRate(rate: BigDecimal): String {
        val absRate = rate.abs()
        val scaled = if (absRate >= BigDecimal.ONE) {
            rate.setScale(2, RoundingMode.HALF_UP)
        } else {
            // 6 significant digits total via MathContext
            rate.round(java.math.MathContext(6, RoundingMode.HALF_UP))
        }
        return scaled.stripTrailingZeros().toPlainString()
    }
}
