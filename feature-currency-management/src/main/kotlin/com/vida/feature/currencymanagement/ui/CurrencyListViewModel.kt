package com.vida.feature.currencymanagement.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.CurrencyInfo
import com.vida.domain.usecase.currency.AddCurrency
import com.vida.domain.usecase.currency.DeleteCurrency
import com.vida.domain.usecase.currency.GetCurrency
import com.vida.domain.usecase.currency.ListCurrencies
import com.vida.domain.usecase.currency.UpdateCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the currency list screen.
 *
 * On init, loads all currencies via [ListCurrencies], sorts them
 * (system first, then user; both alphabetical), and emits [CurrencyListUiState].
 *
 * Exposes one-shot [CurrencyNavEvent]s via a [Channel] for
 * transient messages (toasts, snackbars).
 */
@HiltViewModel
class CurrencyListViewModel @Inject constructor(
    private val listCurrencies: ListCurrencies,
    private val addCurrency: AddCurrency,
    private val updateCurrency: UpdateCurrency,
    private val deleteCurrency: DeleteCurrency,
    private val getCurrency: GetCurrency,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CurrencyListUiState>(CurrencyListUiState.Loading)
    val uiState: StateFlow<CurrencyListUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<CurrencyNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    /** True while a delete operation is in-flight (prevents double-tap). */
    private var isDeleting = false

    /** True while an add or edit save is in-flight (disables the form save button). */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadCurrencies()
    }

    // ── Public actions ───────────────────────────────────────────────────────

    /**
     * Deletes the currency with [id].
     *
     * System currencies are blocked: a [CurrencyNavEvent.ShowToast] is emitted
     * and no delete call is made. On success, the list is refetched.
     */
    fun onDelete(id: Long) {
        if (isDeleting) return

        val current = _uiState.value
        if (current !is CurrencyListUiState.Ready) return

        val item = current.currencies.find { it.id == id } ?: return

        if (item.isSystem) {
            viewModelScope.launch {
                _navEvents.send(
                    CurrencyNavEvent.ShowToast(
                        "Moneda del sistema — no se puede eliminar",
                    ),
                )
            }
            return
        }

        viewModelScope.launch {
            isDeleting = true
            try {
                deleteCurrency(id)
                loadCurrencies()
                _navEvents.send(
                    CurrencyNavEvent.ShowToast("Moneda eliminada"),
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    CurrencyNavEvent.ShowToast(
                        t.message ?: "No se pudo eliminar la moneda",
                    ),
                )
            } finally {
                isDeleting = false
            }
        }
    }

    /**
     * Creates a new user currency with [name] and [code],
     * then refetches the list.
     *
     * Validation (defence-in-depth — domain [CurrencyInfo.init] requires non-blank,
     * code ≤ 10 chars):
     * rejects blank names/codes before calling the use case.
     * On success emits [CurrencyNavEvent.SaveSuccess] so the screen can close the dialog.
     */
    fun onAdd(name: String, code: String) {
        if (_isSaving.value) return

        val trimmedName = name.trim()
        val trimmedCode = code.trim().uppercase()

        if (trimmedName.isEmpty() || trimmedName.length > 50) {
            viewModelScope.launch {
                _navEvents.send(
                    CurrencyNavEvent.ShowToast("El nombre debe tener entre 1 y 50 caracteres"),
                )
            }
            return
        }

        if (trimmedCode.isEmpty() || trimmedCode.length > 10) {
            viewModelScope.launch {
                _navEvents.send(
                    CurrencyNavEvent.ShowToast("El código debe tener entre 1 y 10 caracteres"),
                )
            }
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                addCurrency(
                    CurrencyInfo(name = trimmedName, code = trimmedCode),
                )
                loadCurrencies()
                _navEvents.send(CurrencyNavEvent.SaveSuccess)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    CurrencyNavEvent.ShowToast(
                        t.message ?: "No se pudo agregar la moneda",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Updates an existing currency's [name] and [code],
     * preserving its [CurrencyInfo.isSystem] flag.
     * Refetches the list on success.
     *
     * On success emits [CurrencyNavEvent.SaveSuccess].
     */
    fun onEdit(id: Long, name: String, code: String) {
        if (_isSaving.value) return

        val trimmedName = name.trim()
        val trimmedCode = code.trim().uppercase()

        if (trimmedName.isEmpty() || trimmedName.length > 50) {
            viewModelScope.launch {
                _navEvents.send(
                    CurrencyNavEvent.ShowToast("El nombre debe tener entre 1 y 50 caracteres"),
                )
            }
            return
        }

        if (trimmedCode.isEmpty() || trimmedCode.length > 10) {
            viewModelScope.launch {
                _navEvents.send(
                    CurrencyNavEvent.ShowToast("El código debe tener entre 1 y 10 caracteres"),
                )
            }
            return
        }

        // Read isSystem from the current display item so we don't overwrite it
        val currentList = (_uiState.value as? CurrencyListUiState.Ready)?.currencies
        val existingIsSystem = currentList?.find { it.id == id }?.isSystem ?: false

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val existing = getCurrency(id)
                updateCurrency(
                    CurrencyInfo(
                        id = id,
                        name = trimmedName,
                        code = trimmedCode,
                        isSystem = existingIsSystem,
                    ),
                )
                loadCurrencies()
                _navEvents.send(CurrencyNavEvent.SaveSuccess)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    CurrencyNavEvent.ShowToast(
                        t.message ?: "No se pudo actualizar la moneda",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** Re-initiates the currency fetch from the [Error] state. */
    fun onDismissError() {
        loadCurrencies()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Fetches currencies, sorts them, and emits the appropriate
     * [CurrencyListUiState] ([Ready], [Empty], or [Error]).
     */
    private fun loadCurrencies() {
        viewModelScope.launch {
            try {
                val currencies = listCurrencies().first()
                val sorted = currencies.sortedWith(
                    compareByDescending<CurrencyInfo> { it.isSystem }
                        .thenBy { it.code.lowercase() },
                )
                val items = sorted.map { it.toDisplayItem() }

                _uiState.value = if (items.isEmpty()) {
                    CurrencyListUiState.Empty
                } else {
                    CurrencyListUiState.Ready(currencies = items)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = CurrencyListUiState.Error(
                    message = t.message ?: "No se pudieron cargar las monedas",
                )
            }
        }
    }

    /**
     * Maps a domain [CurrencyInfo] to a pre-formatted [CurrencyDisplayItem].
     */
    private fun CurrencyInfo.toDisplayItem(): CurrencyDisplayItem = CurrencyDisplayItem(
        id = id,
        name = name,
        code = code,
        isSystem = isSystem,
    )
}
