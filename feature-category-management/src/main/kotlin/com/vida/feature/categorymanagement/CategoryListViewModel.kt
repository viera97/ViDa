package com.vida.feature.categorymanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Category
import com.vida.domain.usecase.category.AddCategory
import com.vida.domain.usecase.category.DeleteCategory
import com.vida.domain.usecase.category.GetCategory
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.category.UpdateCategory
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
 * ViewModel for the category list screen.
 *
 * On init, loads all categories via [ListCategories], sorts them
 * (system first, then user; both alphabetical), and emits [CategoryListUiState].
 *
 * Exposes one-shot [CategoryNavEvent]s via a [Channel] for
 * transient messages (toasts, snackbars).
 */
@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val listCategories: ListCategories,
    private val addCategory: AddCategory,
    private val updateCategory: UpdateCategory,
    private val deleteCategory: DeleteCategory,
    private val getCategory: GetCategory,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryListUiState>(CategoryListUiState.Loading)
    val uiState: StateFlow<CategoryListUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<CategoryNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    /** True while a delete operation is in-flight (prevents double-tap). */
    private var isDeleting = false

    /** True while an add or edit save is in-flight (disables the form save button). */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadCategories()
    }

    // ── Public actions ───────────────────────────────────────────────────────

    /**
     * Deletes the category with [id].
     *
     * System categories are blocked: a [CategoryNavEvent.ShowToast] is emitted
     * and no delete call is made. On success, the list is refetched.
     */
    fun onDelete(id: Long) {
        if (isDeleting) return

        val current = _uiState.value
        if (current !is CategoryListUiState.Ready) return

        val item = current.categories.find { it.id == id } ?: return

        if (item.isSystem) {
            viewModelScope.launch {
                _navEvents.send(
                    CategoryNavEvent.ShowToast(
                        "Categoría del sistema — no se puede eliminar",
                    ),
                )
            }
            return
        }

        viewModelScope.launch {
            isDeleting = true
            try {
                deleteCategory(id)
                loadCategories()
                _navEvents.send(
                    CategoryNavEvent.ShowToast("Categoría eliminada"),
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    CategoryNavEvent.ShowToast(
                        t.message ?: "No se pudo eliminar la categoría",
                    ),
                )
            } finally {
                isDeleting = false
            }
        }
    }

    /**
     * Creates a new user category with [name], [color], and optional [icon],
     * then refetches the list.
     *
     * Validation (defence-in-depth — domain [Category.init] requires 1..50 non-blank):
     * rejects blank/whitespace-only names before calling the use case.
     * On success emits [CategoryNavEvent.SaveSuccess] so the screen can close the dialog.
     */
    fun onAdd(name: String, color: Int, icon: String? = null) {
        if (_isSaving.value) return

        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 50) {
            viewModelScope.launch {
                _navEvents.send(
                    CategoryNavEvent.ShowToast("El nombre debe tener entre 1 y 50 caracteres"),
                )
            }
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                addCategory(Category(name = trimmed, color = color, icon = icon))
                loadCategories()
                _navEvents.send(CategoryNavEvent.SaveSuccess)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    CategoryNavEvent.ShowToast(
                        t.message ?: "No se pudo agregar la categoría",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Updates an existing category's [name], [color], and optional [icon],
     * preserving its [Category.isSystem] flag. If [icon] is null the existing
     * icon from the database is preserved. Refetches the list on success.
     *
     * On success emits [CategoryNavEvent.SaveSuccess].
     */
    fun onEdit(id: Long, name: String, color: Int, icon: String? = null) {
        if (_isSaving.value) return

        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 50) {
            viewModelScope.launch {
                _navEvents.send(
                    CategoryNavEvent.ShowToast("El nombre debe tener entre 1 y 50 caracteres"),
                )
            }
            return
        }

        // Read isSystem from the current display item so we don't overwrite it
        val currentList = (_uiState.value as? CategoryListUiState.Ready)?.categories
        val existingIsSystem = currentList?.find { it.id == id }?.isSystem ?: false

        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Fetch the existing category to preserve its icon when not changed
                val existing = getCategory(id)
                updateCategory(
                    Category(
                        id = id,
                        name = trimmed,
                        color = color,
                        icon = icon ?: existing?.icon,
                        isSystem = existingIsSystem,
                    ),
                )
                loadCategories()
                _navEvents.send(CategoryNavEvent.SaveSuccess)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _navEvents.send(
                    CategoryNavEvent.ShowToast(
                        t.message ?: "No se pudo actualizar la categoría",
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** Re-initiates the category fetch from the [Error] state. */
    fun onDismissError() {
        loadCategories()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Fetches categories, sorts them, and emits the appropriate
     * [CategoryListUiState] ([Ready], [Empty], or [Error]).
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = listCategories().first()
                val sorted = categories.sortedWith(
                    compareByDescending<Category> { it.isSystem }
                        .thenBy { it.name.lowercase() },
                )
                val items = sorted.map { it.toDisplayItem() }

                _uiState.value = if (items.isEmpty()) {
                    CategoryListUiState.Empty
                } else {
                    CategoryListUiState.Ready(categories = items)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.value = CategoryListUiState.Error(
                    message = t.message ?: "No se pudieron cargar las categorías",
                )
            }
        }
    }

    /**
     * Maps a domain [Category] to a pre-formatted [CategoryDisplayItem].
     */
    private fun Category.toDisplayItem(): CategoryDisplayItem = CategoryDisplayItem(
        id = id,
        name = name,
        color = color,
        icon = icon,
        isSystem = isSystem,
    )
}
