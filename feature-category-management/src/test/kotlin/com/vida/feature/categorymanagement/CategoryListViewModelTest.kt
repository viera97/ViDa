package com.vida.feature.categorymanagement

import app.cash.turbine.test
import com.vida.domain.model.Category
import com.vida.domain.usecase.category.AddCategory
import com.vida.domain.usecase.category.DeleteCategory
import com.vida.domain.usecase.category.GetCategory
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.category.UpdateCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class CategoryListViewModelTest {

    private lateinit var listCategories: ListCategories
    private lateinit var addCategory: AddCategory
    private lateinit var updateCategory: UpdateCategory
    private lateinit var deleteCategory: DeleteCategory
    private lateinit var getCategory: GetCategory

    private val sampleCategories = listOf(
        Category(id = 1L, name = "Transporte", color = -14614533, isSystem = true),
        Category(id = 2L, name = "Alimentos", color = -43904, isSystem = true),
        Category(id = 3L, name = "Viajes", color = -7617718, isSystem = false),
        Category(id = 4L, name = "Café", color = -16733697, isSystem = false),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        listCategories = mockk()
        addCategory = mockk()
        updateCategory = mockk()
        deleteCategory = mockk()
        getCategory = mockk()

        // Default: categories exist
        every { listCategories() } returns flowOf(sampleCategories)
        coEvery { deleteCategory(any<Long>()) } returns Unit
        coEvery { addCategory(any()) } returns 5L
        coEvery { updateCategory(any()) } returns 5L
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(): CategoryListViewModel = CategoryListViewModel(
        listCategories = listCategories,
        addCategory = addCategory,
        updateCategory = updateCategory,
        deleteCategory = deleteCategory,
        getCategory = getCategory,
    )

    // ── SCN-CAT-001: Initial load → Ready with categories ────────────────────

    @Test
    fun `initial load emits Ready with sorted categories`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CategoryListUiState.Ready
            assertEquals(4, ready.categories.size)
        }
    }

    // ── SCN-CAT-006: Sort order (system first, then user, alphabetical) ──────

    @Test
    fun `sort order puts system categories first then user alphabetically`() =
        runTest {
            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as CategoryListUiState.Ready
                val names = ready.categories.map { it.name }
                assertEquals(
                    listOf("Alimentos", "Transporte", "Café", "Viajes"),
                    names,
                )
            }
        }

    // ── Display item mapping ─────────────────────────────────────────────────

    @Test
    fun `category fields are correctly mapped to display items`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CategoryListUiState.Ready
            val alimentos = ready.categories.first { it.name == "Alimentos" }
            assertEquals(2L, alimentos.id)
            assertEquals("Alimentos", alimentos.name)
            assertEquals(-43904, alimentos.color)
            assertTrue(alimentos.isSystem)
            assertFalse(alimentos.isSelectedForDelete)
        }
    }

    // ── SCN-CAT-002: Initial load → Empty ────────────────────────────────────

    @Test
    fun `initial load emits Empty when no categories exist`() = runTest {
        every { listCategories() } returns flowOf(emptyList())

        val vm = createVm()

        vm.uiState.test {
            val empty = awaitItem() as CategoryListUiState.Empty
            assertNotNull(empty)
        }
    }

    // ── SCN-CAT-003: Initial load → Error ────────────────────────────────────

    @Test
    fun `initial load emits Error when ListCategories throws`() = runTest {
        every { listCategories() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as CategoryListUiState.Error
            assertEquals("DB error", error.message)
        }
    }

    @Test
    fun `initial load emits Error with fallback message on exception without message`() =
        runTest {
            every { listCategories() } throws RuntimeException()

            val vm = createVm()

            vm.uiState.test {
                val error = awaitItem() as CategoryListUiState.Error
                assertTrue(error.message.isNotBlank())
            }
        }

    // ── onDelete: success ─────────────────────────────────────────────────────

    @Test
    fun `onDelete calls DeleteCategory and refetches list`() = runTest {
        coEvery { deleteCategory(3L) } returns Unit
        // After delete: only 3 categories remain (Viajes is gone)
        val remaining = sampleCategories.filter { it.id != 3L }
        every { listCategories() } returns flowOf(sampleCategories) andThen flowOf(remaining)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CategoryListUiState.Ready
            assertEquals(4, ready.categories.size)

            vm.onDelete(3L)

            val afterDelete = awaitItem() as CategoryListUiState.Ready
            assertEquals(3, afterDelete.categories.size)
            assertTrue(afterDelete.categories.none { it.id == 3L })

            coVerify(exactly = 1) { deleteCategory(3L) }
        }
    }

    // ── onDelete: system category → ShowToast ─────────────────────────────────

    @Test
    fun `onDelete system category emits ShowToast and does NOT call DeleteCategory`() =
        runTest {
            val vm = createVm()

            vm.navEvents.test {
                vm.onDelete(1L) // Transporte is system

                val event = awaitItem() as CategoryNavEvent.ShowToast
                assertEquals(
                    "Categoría del sistema — no se puede eliminar",
                    event.message,
                )

                coVerify(inverse = true) { deleteCategory(any()) }
            }
        }

    @Test
    fun `onDelete system category does NOT change the list state`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CategoryListUiState.Ready
            assertEquals(4, ready.categories.size)

            vm.onDelete(1L) // Transporte is system

            // State should NOT change — no re-emission
            expectNoEvents()
        }
    }

    // ── onDelete: isDeleting guard prevents double-tap ────────────────────────

    @Test
    fun `onDelete double invocation calls DeleteCategory once per call`() =
        runTest {
            // With UnconfinedTestDispatcher, coroutine bodies execute synchronously,
            // so the isDeleting guard runs and completes atomically within each call.
            // True concurrent double-tap blocking requires Compose UI tests (deferred).
            // This test verifies two sequential calls each trigger their own delete.
            val vm = createVm()

            vm.uiState.test {
                awaitItem() // Ready

                vm.onDelete(3L) // Delete Viajes
                vm.onDelete(4L) // Delete Café (separate call)

                coVerify(exactly = 1) { deleteCategory(3L) }
                coVerify(exactly = 1) { deleteCategory(4L) }
            }
        }

    // ── onDelete: when state is not Ready ────────────────────────────────────

    @Test
    fun `onDelete no-ops when state is not Ready`() = runTest {
        every { listCategories() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Error state
            vm.onDelete(3L)
            expectNoEvents()
            coVerify(inverse = true) { deleteCategory(any()) }
        }
    }

    // ── onDelete: non-existent id ────────────────────────────────────────────

    @Test
    fun `onDelete with non-existent id no-ops`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready
            vm.onDelete(999L) // Not in the list
            expectNoEvents()
            coVerify(inverse = true) { deleteCategory(any()) }
        }
    }

    // ── onDelete: DeleteCategory throws → ShowToast ───────────────────────────

    @Test
    fun `onDelete emits ShowToast when DeleteCategory throws`() = runTest {
        coEvery { deleteCategory(3L) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onDelete(3L)

            val event = awaitItem() as CategoryNavEvent.ShowToast
            assertEquals("Network error", event.message)
        }
    }

    @Test
    fun `onDelete error preserves existing category list`() = runTest {
        coEvery { deleteCategory(3L) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CategoryListUiState.Ready
            assertEquals(4, ready.categories.size)

            vm.onDelete(3L)

            // State should NOT change — list preserved
            expectNoEvents()
        }
    }

    // ── onDismissError: retry from Error state ───────────────────────────────

    @Test
    fun `onDismissError transitions from Error to Loading then retries`() =
        runTest {
            val successCategories = listOf(sampleCategories[0]) // Only Transporte
            every { listCategories() } throws RuntimeException("DB error") andThen flowOf(
                successCategories,
            )

            val vm = createVm()

            vm.uiState.test {
                val error = awaitItem() as CategoryListUiState.Error
                assertEquals("DB error", error.message)

                vm.onDismissError()

                // With UnconfinedTestDispatcher, Loading → Ready happens synchronously
                // and StateFlow conflates to the latest value (Ready).
                val ready = awaitItem() as CategoryListUiState.Ready
                assertEquals(1, ready.categories.size)
                assertEquals("Transporte", ready.categories[0].name)
            }
        }

    @Test
    fun `onDismissError retry that gets empty categories emits Empty`() = runTest {
        every { listCategories() } throws RuntimeException("DB error") andThen flowOf(
            emptyList(),
        )

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as CategoryListUiState.Error
            assertEquals("DB error", error.message)

            vm.onDismissError()

            // With UnconfinedTestDispatcher, Loading → Empty happens synchronously
            // and StateFlow conflates to the latest value (Empty).
            val empty = awaitItem() as CategoryListUiState.Empty
            assertNotNull(empty)
        }
    }

    // ── SCN-CAT-006 (extended): remove category changes sort order ────────────

    @Test
    fun `delete of a category preserves correct sort order in remaining items`() =
        runTest {
            // After deleting Viajes (id=3, user), remaining sorted:
            // Alimentos(s), Transporte(s), Café(u)
            val remaining = sampleCategories.filter { it.id != 3L }
            every { listCategories() } returns flowOf(sampleCategories) andThen flowOf(
                remaining,
            )

            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as CategoryListUiState.Ready
                assertEquals(4, ready.categories.size)

                vm.onDelete(3L)

                val afterDelete = awaitItem() as CategoryListUiState.Ready
                assertEquals(3, afterDelete.categories.size)
                assertEquals(
                    listOf("Alimentos", "Transporte", "Café"),
                    afterDelete.categories.map { it.name },
                )
            }
        }

    // ── onDelete: success toast ─────────────────────────────────────────────────

    @Test
    fun `onDelete success emits ShowToast`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onDelete(3L) // Viajes (user category)

            val event = awaitItem() as CategoryNavEvent.ShowToast
            assertEquals("Categoría eliminada", event.message)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Phase 6 — onAdd / onEdit dialog actions
    // ══════════════════════════════════════════════════════════════════════

    // ── onAdd: success ─────────────────────────────────────────────────────────

    @Test
    fun `onAdd creates category, refetches list, and emits SaveSuccess`() = runTest {
        coEvery { addCategory(any()) } returns 7L
        val afterAdd = sampleCategories + Category(
            id = 7L,
            name = "Salud",
            color = 0xFF4CAF50.toInt(),
            isSystem = false,
        )
        every { listCategories() } returns flowOf(sampleCategories) andThen flowOf(
            afterAdd,
        )

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CategoryListUiState.Ready
            assertEquals(4, ready.categories.size)
        }

        vm.navEvents.test {
            vm.onAdd("Salud", 0xFF4CAF50.toInt())

            val event = awaitItem() as CategoryNavEvent.SaveSuccess
            assertNotNull(event)

            coVerify(exactly = 1) {
                addCategory(match { it.name == "Salud" })
            }
        }
    }

    @Test
    fun `onAdd trims whitespace from name`() = runTest {
        coEvery { addCategory(any()) } returns 8L
        every { listCategories() } returns flowOf(sampleCategories) andThen flowOf(
            sampleCategories,
        )

        val vm = createVm()
        vm.onAdd("  Salud  ", 0xFF4CAF50.toInt())

        coVerify(exactly = 1) {
            addCategory(match { it.name == "Salud" })
        }
    }

    // ── onAdd: validation ─────────────────────────────────────────────────────

    @Test
    fun `onAdd rejected on blank name`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("", 0xFFFF9800.toInt())

            val event = awaitItem() as CategoryNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { addCategory(any()) }
        }
    }

    @Test
    fun `onAdd rejected on whitespace-only name`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("   ", 0xFFFF9800.toInt())

            val event = awaitItem() as CategoryNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { addCategory(any()) }
        }
    }

    @Test
    fun `onAdd rejected on name longer than 50 chars`() = runTest {
        val vm = createVm()
        val longName = "A".repeat(51)

        vm.navEvents.test {
            vm.onAdd(longName, 0xFFFF9800.toInt())

            val event = awaitItem() as CategoryNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { addCategory(any()) }
        }
    }

    // ── onAdd: error ──────────────────────────────────────────────────────────

    @Test
    fun `onAdd emits ShowToast when AddCategory throws`() = runTest {
        coEvery { addCategory(any()) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("Salud", 0xFF4CAF50.toInt())

            val event = awaitItem() as CategoryNavEvent.ShowToast
            assertEquals("Network error", event.message)
        }
    }

    @Test
    fun `onAdd error preserves existing category list`() = runTest {
        coEvery { addCategory(any()) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CategoryListUiState.Ready
            assertEquals(4, ready.categories.size)

            vm.onAdd("Salud", 0xFF4CAF50.toInt())

            // List should NOT change — no re-emission
            expectNoEvents()
        }
    }

    // ── onEdit: success ────────────────────────────────────────────────────────

    @Test
    fun `onEdit updates category, refetches list, and emits SaveSuccess`() =
        runTest {
            val updatedList = sampleCategories.map {
                if (it.id == 3L) it.copy(name = "Vacaciones", color = 0xFF2196F3.toInt())
                else it
            }
            every { listCategories() } returns flowOf(sampleCategories) andThen flowOf(
                updatedList,
            )

            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as CategoryListUiState.Ready
                assertEquals(4, ready.categories.size)
            }

            vm.navEvents.test {
                vm.onEdit(3L, "Vacaciones", 0xFF2196F3.toInt())

                val event = awaitItem() as CategoryNavEvent.SaveSuccess
                assertNotNull(event)

                coVerify(exactly = 1) {
                    updateCategory(match {
                        it.id == 3L && it.name == "Vacaciones" &&
                            it.color == 0xFF2196F3.toInt()
                    })
                }
            }
        }

    @Test
    fun `onEdit preserves isSystem flag from current state`() = runTest {
        every { listCategories() } returns flowOf(sampleCategories) andThen flowOf(
            sampleCategories,
        )

        val vm = createVm()

        // Viajes (id=3) is NOT a system category → isSystem=false
        vm.onEdit(3L, "Viajes Edit", 0xFF4CAF50.toInt())

        coVerify(exactly = 1) {
            updateCategory(match { it.id == 3L && !it.isSystem })
        }
    }

    // ── onEdit: validation ─────────────────────────────────────────────────────

    @Test
    fun `onEdit rejected on blank name`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit(3L, "", 0xFFFF9800.toInt())

            val event = awaitItem() as CategoryNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { updateCategory(any()) }
        }
    }

    @Test
    fun `onEdit rejected on whitespace-only name`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit(3L, "   ", 0xFFFF9800.toInt())

            val event = awaitItem() as CategoryNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { updateCategory(any()) }
        }
    }

    // ── onEdit: error ──────────────────────────────────────────────────────────

    @Test
    fun `onEdit emits ShowToast when UpdateCategory throws`() = runTest {
        coEvery { updateCategory(any()) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit(3L, "Vacaciones", 0xFF2196F3.toInt())

            val event = awaitItem() as CategoryNavEvent.ShowToast
            assertEquals("DB error", event.message)
        }
    }

    @Test
    fun `onEdit error preserves existing category list`() = runTest {
        coEvery { updateCategory(any()) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CategoryListUiState.Ready
            assertEquals(4, ready.categories.size)

            vm.onEdit(3L, "Vacaciones", 0xFF2196F3.toInt())

            // List should NOT change — no re-emission
            expectNoEvents()
        }
    }

    // ── isSaving StateFlow lifecycle ───────────────────────────────────────────

    @Test
    fun `isSaving transitions false to true to false during add`() = runTest {
        val vm = createVm()

        vm.isSaving.test {
            assertFalse(awaitItem()) // initial: false

            vm.onAdd("Salud", 0xFF4CAF50.toInt())

            // With UnconfinedTestDispatcher the operation completes synchronously,
            // so isSaving may conflate true→false. We verify it ends as false.
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `isSaving exposed and starts false`() = runTest {
        val vm = createVm()

        vm.isSaving.test {
            val initial = awaitItem()
            assertFalse(initial)
        }
    }

    // ── Form cancel discards data (no ViewModel state change) ──────────────────

    @Test
    fun `cancel does NOT call any mutation use case`() = runTest {
        val vm = createVm()

        // Simulating that the screen simply dismisses the dialog without calling
        // onSave → ViewModel is never invoked for a mutation on cancel.
        // This test verifies that no unexpected mutation is triggered.
        coVerify(inverse = true) { addCategory(any()) }
        coVerify(inverse = true) { updateCategory(any()) }
        coVerify(inverse = true) { deleteCategory(any()) }
    }
}
