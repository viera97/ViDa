package com.vida.feature.bankmanagement.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Bank
import com.vida.domain.usecase.bank.AddBank
import com.vida.domain.usecase.bank.DeleteBank
import com.vida.domain.usecase.bank.GetBank
import com.vida.domain.usecase.bank.ListBanks
import com.vida.domain.usecase.bank.UpdateBank
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
 * ViewModel for the bank list screen.
 *
 * On init, loads all banks via [ListBanks], sorts them
 * (system first, then user; both alphabetical), and emits [BankListUiState].
 *
 * Exposes one-shot [BankNavEvent]s via a [Channel] for
 * transient messages (toasts, snackbars).
 */
@HiltViewModel
class BankListViewModel @Inject constructor(
    private val listBanks: ListBanks,
    private val addBank: AddBank,
    private val updateBank: UpdateBank,
    private val deleteBank: DeleteBank,
    private val getBank: GetBank,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BankListUiState>(BankListUiState.Loading)
    val uiState: StateFlow<BankListUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<BankNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    /** True while a delete operation is in-flight (prevents double-tap). */
    private var isDeleting = false

    /** True while an add or edit save is in-flight (disables the form save button). */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadBanks()
    }

    // ── Public actions ───────────────────────────────────────────────────────

    /**
     * Deletes the bank with [id].
     *
     * System banks are blocked: a [BankNavEvent.ShowToast] is emitted
     * and no delete call is made. On success, the list is refetched.
     */
    fun onDelete(id: Long) {
        if (isDeleting) return

        val current = _uiState.value
        if (current !is BankListUiState.Ready) return

        val item = current.banks.find { it.id == id } ?: return

        if (item.isSystem) {
            viewModelScope.launch {
                _navEvents.send(
                    BankNavEvent.ShowToast(
                        "Banco del sistema — no se puede eliminar",
                    ),
                )
            }
            return
        }

        viewModelScope.launch {
            isDeleting = true
            try {
                deleteBank(id)
                loadBanks()
                _navEvents.send(
                    BankNavEvent.ShowToast("Banco eliminado"),
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    BankNavEvent.ShowToast(
                        t.message ?: "No se pudo eliminar el banco",
                    ),
                )
            } finally {
                isDeleting = false
            }
        }
    }

    /**
     * Creates a new user bank with [name] and [color],
     * then refetches the list.
     *
     * Validation (defence-in-depth — domain [Bank.init] requires 1..50 non-blank):
     * rejects blank/whitespace-only names before calling the use case.
     * On success emits [BankNavEvent.SaveSuccess] so the screen can close the dialog.
     */
    fun onAdd(name: String, color: Int) {
        if (_isSaving.value) return

        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 50) {
            viewModelScope.launch {
                _navEvents.send(
                    BankNavEvent.ShowToast("El nombre debe tener entre 1 y 50 caracteres"),
                )
            }
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                addBank(Bank(name = trimmed, color = color))
                loadBanks()
                _navEvents.send(BankNavEvent.SaveSuccess)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    BankNavEvent.ShowToast(
                        t.message ?: "No se pudo agregar el banco",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Updates an existing bank's [name] and [color],
     * preserving its [Bank.isSystem] flag.
     * Refetches the list on success.
     *
     * On success emits [BankNavEvent.SaveSuccess].
     */
    fun onEdit(id: Long, name: String, color: Int) {
        if (_isSaving.value) return

        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 50) {
            viewModelScope.launch {
                _navEvents.send(
                    BankNavEvent.ShowToast("El nombre debe tener entre 1 y 50 caracteres"),
                )
            }
            return
        }

        // Read isSystem from the current display item so we don't overwrite it
        val currentList = (_uiState.value as? BankListUiState.Ready)?.banks
        val existingIsSystem = currentList?.find { it.id == id }?.isSystem ?: false

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val existing = getBank(id)
                updateBank(
                    Bank(
                        id = id,
                        name = trimmed,
                        color = color,
                        isSystem = existingIsSystem,
                    ),
                )
                loadBanks()
                _navEvents.send(BankNavEvent.SaveSuccess)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    BankNavEvent.ShowToast(
                        t.message ?: "No se pudo actualizar el banco",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** Re-initiates the bank fetch from the [Error] state. */
    fun onDismissError() {
        loadBanks()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Fetches banks, sorts them, and emits the appropriate
     * [BankListUiState] ([Ready], [Empty], or [Error]).
     */
    private fun loadBanks() {
        viewModelScope.launch {
            try {
                val banks = listBanks().first()
                val sorted = banks.sortedWith(
                    compareByDescending<Bank> { it.isSystem }
                        .thenBy { it.name.lowercase() },
                )
                val items = sorted.map { it.toDisplayItem() }

                _uiState.value = if (items.isEmpty()) {
                    BankListUiState.Empty
                } else {
                    BankListUiState.Ready(banks = items)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = BankListUiState.Error(
                    message = t.message ?: "No se pudieron cargar los bancos",
                )
            }
        }
    }

    /**
     * Maps a domain [Bank] to a pre-formatted [BankDisplayItem].
     */
    private fun Bank.toDisplayItem(): BankDisplayItem = BankDisplayItem(
        id = id,
        name = name,
        color = color,
        isSystem = isSystem,
    )
}
