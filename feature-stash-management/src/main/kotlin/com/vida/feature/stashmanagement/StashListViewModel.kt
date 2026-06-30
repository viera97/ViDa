package com.vida.feature.stashmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Currency
import com.vida.domain.model.Stash
import com.vida.domain.usecase.stash.AddStash
import com.vida.domain.usecase.stash.DeleteStash
import com.vida.domain.usecase.stash.GetStash
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.stash.UpdateStash
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for the stash list screen.
 *
 * On init, loads all stashes via [ListStashes], sorts them by name
 * alphabetically, and emits [StashListUiState].
 *
 * Exposes one-shot [StashNavEvent]s via a [Channel] for transient messages
 * (toasts, snackbars).
 *
 * PR #1: list, delete, retry. PR #2 adds onAdd/onEdit with form dialog.
 */
@HiltViewModel
class StashListViewModel @Inject constructor(
    private val listStashes: ListStashes,
    private val addStash: AddStash,
    private val updateStash: UpdateStash,
    private val deleteStash: DeleteStash,
    private val getStash: GetStash,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StashListUiState>(StashListUiState.Loading)
    val uiState: StateFlow<StashListUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<StashNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    /** True while a delete operation is in-flight (prevents double-tap). */
    private var isDeleting = false

    /** True while an add/edit operation is in-flight (prevents double-tap). */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadStashes()
    }

    // ── Public actions ───────────────────────────────────────────────────────

    /**
     * Deletes the stash with [id].
     *
     * On success, the list is refetched and a success toast is emitted.
     * On error, the current list is preserved and an error toast is shown.
     */
    fun onDelete(id: Long) {
        if (isDeleting) return

        val current = _uiState.value
        if (current !is StashListUiState.Ready) return

        val item = current.items.find { it.id == id } ?: return

        viewModelScope.launch {
            isDeleting = true
            try {
                deleteStash(id)
                loadStashes()
                _navEvents.send(
                    StashNavEvent.ShowToast("Fondo eliminado"),
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    StashNavEvent.ShowToast(
                        t.message ?: "No se pudo eliminar el fondo",
                    ),
                )
            } finally {
                isDeleting = false
            }
        }
    }

    /**
     * Adds a new stash with the given [name] and [currency].
     *
     * Validation occurs here before the domain use case is invoked.
     * On success the list is refetched and [StashNavEvent.SaveSuccess] is emitted
     * (which closes the dialog). On error a toast is shown and the list is preserved.
     */
    fun onAdd(name: String, currency: Currency) {
        if (isSaving.value) return
        if (name.isBlank()) return
        if (name.length > 100) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                addStash(name, currency)
                loadStashes()
                _navEvents.send(StashNavEvent.SaveSuccess)
                _navEvents.send(StashNavEvent.ShowToast("Fondo agregado"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    StashNavEvent.ShowToast(
                        t.message ?: "No se pudo agregar el fondo",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Updates an existing stash with the given [id], [name], and [currency].
     *
     * Fetches the existing stash via [GetStash] to preserve [Stash.createdAt],
     * then calls [UpdateStash]. Mirrors [onAdd] for the success/error flow.
     */
    fun onEdit(id: Long, name: String, currency: Currency) {
        if (isSaving.value) return
        if (name.isBlank()) return
        if (name.length > 100) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val existing = getStash(id)
                if (existing == null) {
                    _navEvents.send(StashNavEvent.ShowToast("Fondo no encontrado"))
                    return@launch
                }
                val stash = Stash(
                    id = id,
                    name = name.trim(),
                    createdAt = existing.createdAt,
                    updatedAt = existing.updatedAt,
                    currency = currency,
                )
                updateStash(stash)
                loadStashes()
                _navEvents.send(StashNavEvent.SaveSuccess)
                _navEvents.send(StashNavEvent.ShowToast("Fondo actualizado"))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    StashNavEvent.ShowToast(
                        t.message ?: "No se pudo actualizar el fondo",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** Re-initiates the stash fetch from the [StashListUiState.Error] state. */
    fun onDismissError() {
        loadStashes()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Fetches stashes, sorts them by name alphabetically, and emits the
     * appropriate [StashListUiState] ([Ready], [Empty], or [Error]).
     */
    private fun loadStashes() {
        viewModelScope.launch {
            try {
                val stashes = listStashes().first()
                val sorted = stashes.sortedBy { it.name.lowercase() }
                val items = sorted.map { it.toDisplayItem() }

                _uiState.value = if (items.isEmpty()) {
                    StashListUiState.Empty
                } else {
                    StashListUiState.Ready(items = items)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = StashListUiState.Error(
                    message = t.message ?: "No se pudieron cargar los fondos",
                )
            }
        }
    }

    /**
     * Maps a domain [Stash] to a pre-formatted [StashDisplayItem].
     *
     * - currencyCode: [com.vida.domain.model.Currency.code] from the domain model.
     * - createdAtFormatted: "dd/MM/yyyy" from [Stash.createdAt].
     * - updatedAtFormatted: "dd/MM/yyyy" from [Stash.updatedAt].
     */
    private fun Stash.toDisplayItem(): StashDisplayItem {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val zone = ZoneId.systemDefault()
        return StashDisplayItem(
            id = id,
            name = name,
            currencyCode = currency.code,
            createdAtFormatted = formatter.format(createdAt.atZone(zone)),
            updatedAtFormatted = formatter.format(updatedAt.atZone(zone)),
        )
    }
}
