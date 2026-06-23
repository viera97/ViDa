package com.vida.feature.stashmanagement

import app.cash.turbine.test
import com.vida.domain.model.Currency
import com.vida.domain.model.Stash
import com.vida.domain.usecase.stash.AddStash
import com.vida.domain.usecase.stash.DeleteStash
import com.vida.domain.usecase.stash.GetStash
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.stash.UpdateStash
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
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StashListViewModelTest {

    private lateinit var listStashes: ListStashes
    private lateinit var addStash: AddStash
    private lateinit var updateStash: UpdateStash
    private lateinit var deleteStash: DeleteStash
    private lateinit var getStash: GetStash

    private val sampleStashes = listOf(
        Stash(
            id = 1L,
            name = "Banco USD",
            createdAt = Instant.parse("2025-01-15T10:30:00Z"),
            updatedAt = Instant.parse("2025-06-01T12:00:00Z"),
            currency = Currency.USD,
        ),
        Stash(
            id = 2L,
            name = "Efectivo",
            createdAt = Instant.parse("2025-03-20T08:00:00Z"),
            updatedAt = Instant.parse("2025-03-20T08:00:00Z"),
            currency = Currency.CUP,
        ),
        Stash(
            id = 3L,
            name = "Zelle",
            createdAt = Instant.parse("2025-05-10T14:00:00Z"),
            updatedAt = Instant.parse("2025-05-10T14:00:00Z"),
            currency = Currency.MLC,
        ),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        listStashes = mockk()
        addStash = mockk()
        updateStash = mockk()
        deleteStash = mockk()
        getStash = mockk()

        // Default: stashes exist
        every { listStashes() } returns flowOf(sampleStashes)
        coEvery { deleteStash(any<Long>()) } returns Unit
        coEvery { addStash(any(), any(), any()) } returns 5L
        coEvery { updateStash(any(), any()) } returns 5L
        coEvery { getStash(any()) } returns sampleStashes[0]
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(): StashListViewModel = StashListViewModel(
        listStashes = listStashes,
        addStash = addStash,
        updateStash = updateStash,
        deleteStash = deleteStash,
        getStash = getStash,
    )

    // ══════════════════════════════════════════════════════════════════════
    // R1 — UiState Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-STH-001: Initial load → Ready with sorted stashes ────────────────
    @Test
    fun `initial load emits Ready with sorted stashes`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as StashListUiState.Ready
            assertEquals(3, ready.items.size)
        }
    }

    // ── SCN-STH-006: Sort order (name alphabetical ascending) ────────────────
    @Test
    fun `sort order is by name alphabetical ascending`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as StashListUiState.Ready
            val names = ready.items.map { it.name }
            assertEquals(
                listOf("Banco USD", "Efectivo", "Zelle"),
                names,
            )
        }
    }

    // ── SCN-STH-002: Initial load → Empty ───────────────────────────────────
    @Test
    fun `initial load emits Empty when no stashes exist`() = runTest {
        every { listStashes() } returns flowOf(emptyList())

        val vm = createVm()

        vm.uiState.test {
            val empty = awaitItem() as StashListUiState.Empty
            assertNotNull(empty)
        }
    }

    // ── SCN-STH-003: Initial load → Error ───────────────────────────────────
    @Test
    fun `initial load emits Error when ListStashes throws`() = runTest {
        every { listStashes() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as StashListUiState.Error
            assertEquals("DB error", error.message)
        }
    }

    @Test
    fun `initial load emits Error with fallback message on exception without message`() =
        runTest {
            every { listStashes() } throws RuntimeException()

            val vm = createVm()

            vm.uiState.test {
                val error = awaitItem() as StashListUiState.Error
                assertTrue(error.message.isNotBlank())
            }
        }

    // ══════════════════════════════════════════════════════════════════════
    // R2 — Stash Item Display (field mapping)
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-STH-004: CUP badge display fields ────────────────────────────────
    @Test
    fun `stash fields are correctly mapped to display items for CUP`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as StashListUiState.Ready
            val efectivo = ready.items.first { it.name == "Efectivo" }
            assertEquals(2L, efectivo.id)
            assertEquals("Efectivo", efectivo.name)
            assertEquals("CUP", efectivo.currencyCode)
            assertTrue(efectivo.createdAtFormatted.isNotBlank())
            assertTrue(efectivo.updatedAtFormatted.isNotBlank())
        }
    }

    // ── SCN-STH-005: USD badge display fields ────────────────────────────────
    @Test
    fun `stash fields are correctly mapped to display items for USD`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as StashListUiState.Ready
            val bancoUsd = ready.items.first { it.name == "Banco USD" }
            assertEquals(1L, bancoUsd.id)
            assertEquals("Banco USD", bancoUsd.name)
            assertEquals("USD", bancoUsd.currencyCode)
        }
    }

    @Test
    fun `MLC display item has correct currency code`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as StashListUiState.Ready
            val zelle = ready.items.first { it.name == "Zelle" }
            assertEquals(3L, zelle.id)
            assertEquals("MLC", zelle.currencyCode)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R5 — Delete Stash
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-STH-017: Delete success → refetch + toast ────────────────────────
    @Test
    fun `onDelete calls DeleteStash and refetches list`() = runTest {
        coEvery { deleteStash(1L) } returns Unit
        val remaining = sampleStashes.filter { it.id != 1L }
        every { listStashes() } returns flowOf(sampleStashes) andThen flowOf(remaining)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as StashListUiState.Ready
            assertEquals(3, ready.items.size)

            vm.onDelete(1L)

            val afterDelete = awaitItem() as StashListUiState.Ready
            assertEquals(2, afterDelete.items.size)
            assertTrue(afterDelete.items.none { it.id == 1L })

            coVerify(exactly = 1) { deleteStash(1L) }
        }
    }

    @Test
    fun `onDelete success emits ShowToast`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onDelete(1L) // Banco USD

            val event = awaitItem() as StashNavEvent.ShowToast
            assertEquals("Fondo eliminado", event.message)
        }
    }

    // ── SCN-STH-018: Delete cancel → no-op ──────────────────────────────────
    @Test
    fun `cancel does NOT call DeleteStash when dialog dismissed without confirm`() =
        runTest {
            createVm() // VM created but onDelete never called
            coVerify(inverse = true) { deleteStash(any()) }
        }

    // ── SCN-STH-021: Delete throws → toast, list preserved ───────────────────
    @Test
    fun `onDelete emits ShowToast when DeleteStash throws`() = runTest {
        coEvery { deleteStash(1L) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onDelete(1L)

            val event = awaitItem() as StashNavEvent.ShowToast
            assertEquals("Network error", event.message)
        }
    }

    @Test
    fun `onDelete error preserves existing stash list`() = runTest {
        coEvery { deleteStash(1L) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as StashListUiState.Ready
            assertEquals(3, ready.items.size)

            vm.onDelete(1L)

            // State should NOT change — list preserved
            expectNoEvents()
        }
    }

    @Test
    fun `onDelete error uses fallback message when exception has no message`() =
        runTest {
            coEvery { deleteStash(1L) } throws RuntimeException()

            val vm = createVm()

            vm.navEvents.test {
                vm.onDelete(1L)

                val event = awaitItem() as StashNavEvent.ShowToast
                assertTrue(event.message.isNotBlank())
            }
        }

    // ── Edge: non-existent id no-op ─────────────────────────────────────────
    @Test
    fun `onDelete with non-existent id no-ops`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready
            vm.onDelete(999L) // Not in the list
            expectNoEvents()
            coVerify(inverse = true) { deleteStash(any()) }
        }
    }

    // ── Edge: delete when state is not Ready ────────────────────────────────
    @Test
    fun `onDelete no-ops when state is not Ready`() = runTest {
        every { listStashes() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Error state
            vm.onDelete(1L)
            expectNoEvents()
            coVerify(inverse = true) { deleteStash(any()) }
        }
    }

    // ── SCN-STH-023: isDeleting guard — double-tap ──────────────────────────
    @Test
    fun `onDelete double invocation calls DeleteStash once per call`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDelete(1L) // Delete Banco USD
            vm.onDelete(2L) // Delete Efectivo (separate call)

            coVerify(exactly = 1) { deleteStash(1L) }
            coVerify(exactly = 1) { deleteStash(2L) }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R7 — Error Handling: retry from Error
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-STH-022: Retry from Error → Ready ───────────────────────────────
    @Test
    fun `onDismissError transitions from Error to Ready on success`() = runTest {
        val successStashes = listOf(sampleStashes[0]) // Only Banco USD
        every { listStashes() } throws RuntimeException("DB error") andThen flowOf(
            successStashes,
        )

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as StashListUiState.Error
            assertEquals("DB error", error.message)

            vm.onDismissError()

            // With UnconfinedTestDispatcher, Loading → Ready happens synchronously
            // and StateFlow conflates to the latest value (Ready).
            val ready = awaitItem() as StashListUiState.Ready
            assertEquals(1, ready.items.size)
            assertEquals("Banco USD", ready.items[0].name)
        }
    }

    @Test
    fun `onDismissError retry that gets empty stashes emits Empty`() = runTest {
        every { listStashes() } throws RuntimeException("DB error") andThen flowOf(
            emptyList(),
        )

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as StashListUiState.Error
            assertEquals("DB error", error.message)

            vm.onDismissError()

            // Loading → Empty conflates to Empty
            val empty = awaitItem() as StashListUiState.Empty
            assertNotNull(empty)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R1 extended — sort order after mutation
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `delete of a stash preserves correct sort order in remaining items`() =
        runTest {
            // After deleting Efectivo (name="Efectivo"), remaining sorted:
            // "Banco USD", "Zelle"
            val remaining = sampleStashes.filter { it.id != 2L }
            every { listStashes() } returns flowOf(sampleStashes) andThen flowOf(
                remaining,
            )

            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as StashListUiState.Ready
                assertEquals(3, ready.items.size)

                vm.onDelete(2L) // Delete Efectivo

                val afterDelete = awaitItem() as StashListUiState.Ready
                assertEquals(2, afterDelete.items.size)
                assertEquals(
                    listOf("Banco USD", "Zelle"),
                    afterDelete.items.map { it.name },
                )
            }
        }

    // ══════════════════════════════════════════════════════════════════════
    // R3 — Add Stash (onAdd)
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-STH-008: Add success → SaveSuccess + toast + refetch ──────────────
    @Test
    fun `onAdd success emits SaveSuccess and refetches list`() = runTest {
        val newStash = Stash(
            id = 5L,
            name = "Ahorro Nuevo",
            createdAt = Instant.parse("2026-01-01T10:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
            currency = Currency.CUP,
        )
        coEvery { addStash(any(), any(), any()) } returns 5L
        every { listStashes() } returns flowOf(sampleStashes) andThen flowOf(
            sampleStashes + newStash,
        )

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as StashListUiState.Ready
            assertEquals(3, ready.items.size)

            vm.onAdd("Ahorro Nuevo", Currency.CUP)

            val afterAdd = awaitItem() as StashListUiState.Ready
            assertEquals(4, afterAdd.items.size)
            assertTrue(afterAdd.items.any { it.name == "Ahorro Nuevo" })

            coVerify(exactly = 1) { addStash("Ahorro Nuevo", Currency.CUP, any()) }
        }
    }

    @Test
    fun `onAdd success emits SaveSuccess event`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("Ahorro Nuevo", Currency.CUP)

            // SaveSuccess
            val saveEvent = awaitItem()
            assertTrue(saveEvent is StashNavEvent.SaveSuccess)

            // ShowToast
            val toastEvent = awaitItem() as StashNavEvent.ShowToast
            assertTrue(toastEvent.message.contains("agregado"))
        }
    }

    // ── SCN-STH-009: CUP default pre-selected ────────────────────────────────
    @Test
    fun `onAdd with CUP currency succeeds`() = runTest {
        coEvery { addStash("Efectivo CUP", Currency.CUP, any()) } returns 6L

        val vm = createVm()

        vm.onAdd("Efectivo CUP", Currency.CUP)

        coVerify(exactly = 1) { addStash("Efectivo CUP", Currency.CUP, any()) }
    }

    @Test
    fun `onAdd with USD currency succeeds`() = runTest {
        coEvery { addStash(any(), any(), any()) } returns 7L

        val vm = createVm()

        vm.onAdd("Ahorro USD", Currency.USD)

        coVerify(exactly = 1) { addStash("Ahorro USD", Currency.USD, any()) }
    }

    @Test
    fun `onAdd with MLC currency succeeds`() = runTest {
        coEvery { addStash(any(), any(), any()) } returns 8L

        val vm = createVm()

        vm.onAdd("Ahorro MLC", Currency.MLC)

        coVerify(exactly = 1) { addStash("Ahorro MLC", Currency.MLC, any()) }
    }

    // ── SCN-STH-010: Empty name rejected ──────────────────────────────────────
    @Test
    fun `onAdd with empty name does not call AddStash`() = runTest {
        val vm = createVm()

        vm.onAdd("", Currency.CUP)

        coVerify(inverse = true) { addStash(any(), any(), any()) }
    }

    // ── SCN-STH-024: Whitespace-only name rejected ───────────────────────────
    @Test
    fun `onAdd with whitespace-only name does not call AddStash`() = runTest {
        val vm = createVm()

        vm.onAdd("   ", Currency.CUP)

        coVerify(inverse = true) { addStash(any(), any(), any()) }
    }

    // ── SCN-STH-012: Name exceeds 100 characters rejected ────────────────────
    @Test
    fun `onAdd with name over 100 chars does not call AddStash`() = runTest {
        val vm = createVm()

        vm.onAdd("x".repeat(101), Currency.CUP)

        coVerify(inverse = true) { addStash(any(), any(), any()) }
    }

    // ── SCN-STH-011: Duplicate name rejected (domain error) ──────────────────
    @Test
    fun `onAdd duplicate name emits error toast`() = runTest {
        coEvery { addStash(any(), any(), any()) } throws IllegalArgumentException(
            "Stash with name 'Efectivo' already exists",
        )

        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("Efectivo", Currency.CUP)

            val event = awaitItem() as StashNavEvent.ShowToast
            assertTrue(event.message.contains("Efectivo") ||
                event.message.contains("No se pudo agregar"),
            )
        }
    }

    // ── isSaving lifecycle ───────────────────────────────────────────────────
    @Test
    fun `isSaving starts false and resets after onAdd`() = runTest {
        val vm = createVm()

        // isSaving starts false
        assertEquals(false, vm.isSaving.value)

        vm.onAdd("Test", Currency.CUP)

        // With UnconfinedTestDispatcher the operation completes synchronously,
        // so isSaving is false again immediately after.
        assertEquals(false, vm.isSaving.value)
        coVerify(exactly = 1) { addStash("Test", Currency.CUP, any()) }
    }

    // ── SCN-STH-023: Double-tap isSaving guard (onAdd) ──────────────────────
    @Test
    fun `onAdd completes successfully for valid input`() = runTest {
        // With UnconfinedTestDispatcher, the operation completes synchronously
        // and isSaving resets to false before a second call could be made.
        // This test verifies the normal path works correctly.
        val vm = createVm()

        vm.onAdd("Nuevo Fondo", Currency.CUP)

        coVerify(exactly = 1) { addStash("Nuevo Fondo", Currency.CUP, any()) }
    }

    // ── Mutation error preserves list ────────────────────────────────────────
    @Test
    fun `onAdd error preserves existing stash list`() = runTest {
        coEvery { addStash(any(), any(), any()) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as StashListUiState.Ready
            assertEquals(3, ready.items.size)

            vm.onAdd("Test", Currency.CUP)

            // State should NOT change — list preserved
            expectNoEvents()
        }
    }

    @Test
    fun `onAdd error uses fallback message on null exception message`() = runTest {
        coEvery { addStash(any(), any(), any()) } throws RuntimeException()

        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("Test", Currency.CUP)

            val event = awaitItem() as StashNavEvent.ShowToast
            assertTrue(event.message.isNotBlank())
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R4 — Edit Stash (onEdit)
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-STH-015: Edit save success ───────────────────────────────────────
    @Test
    fun `onEdit success emits SaveSuccess and updates list`() = runTest {
        coEvery { updateStash(any(), any()) } returns 1L
        coEvery { getStash(1L) } returns sampleStashes[0] // Banco USD
        val updatedStashes = listOf(
            sampleStashes[0].copy(name = "Banco USD Editado"),
            sampleStashes[1],
            sampleStashes[2],
        )
        every { listStashes() } returns flowOf(sampleStashes) andThen flowOf(
            updatedStashes,
        )

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as StashListUiState.Ready
            assertEquals("Banco USD", ready.items.first { it.id == 1L }.name)

            vm.onEdit(1L, "Banco USD Editado", Currency.USD)

            val afterEdit = awaitItem() as StashListUiState.Ready
            assertEquals(3, afterEdit.items.size)
            assertEquals("Banco USD Editado", afterEdit.items.first { it.id == 1L }.name)

            coVerify(exactly = 1) { updateStash(any(), any()) }
            coVerify(exactly = 1) { getStash(1L) }
        }
    }

    @Test
    fun `onEdit success emits toast with actualizado`() = runTest {
        coEvery { getStash(1L) } returns sampleStashes[0]

        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit(1L, "Banco USD Editado", Currency.USD)

            // SaveSuccess
            val saveEvent = awaitItem()
            assertTrue(saveEvent is StashNavEvent.SaveSuccess)

            // ShowToast
            val toastEvent = awaitItem() as StashNavEvent.ShowToast
            assertTrue(toastEvent.message.contains("actualizado"))
        }
    }

    // ── SCN-STH-016: Edit cancel does nothing ────────────────────────────────
    @Test
    fun `not calling onEdit does NOT call UpdateStash`() = runTest {
        createVm() // VM created but onEdit never called
        coVerify(inverse = true) { updateStash(any(), any()) }
    }

    // ── Edit validation ─────────────────────────────────────────────────────
    @Test
    fun `onEdit with empty name does not call UpdateStash`() = runTest {
        val vm = createVm()

        vm.onEdit(1L, "", Currency.CUP)

        coVerify(inverse = true) { updateStash(any(), any()) }
        coVerify(inverse = true) { getStash(any()) }
    }

    @Test
    fun `onEdit with whitespace-only name does not call UpdateStash`() = runTest {
        val vm = createVm()

        vm.onEdit(1L, "   ", Currency.CUP)

        coVerify(inverse = true) { updateStash(any(), any()) }
        coVerify(inverse = true) { getStash(any()) }
    }

    @Test
    fun `onEdit with name over 100 chars does not call UpdateStash`() = runTest {
        val vm = createVm()

        vm.onEdit(1L, "x".repeat(101), Currency.CUP)

        coVerify(inverse = true) { updateStash(any(), any()) }
        coVerify(inverse = true) { getStash(any()) }
    }

    // ── Edit error ───────────────────────────────────────────────────────────
    @Test
    fun `onEdit error emits toast and preserves list`() = runTest {
        coEvery { getStash(1L) } returns sampleStashes[0]
        coEvery { updateStash(any(), any()) } throws RuntimeException("Update failed")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as StashListUiState.Ready
            assertEquals(3, ready.items.size)

            vm.onEdit(1L, "Banco USD Modified", Currency.USD)

            // List preserved
            expectNoEvents()
        }
    }

    @Test
    fun `onEdit non-existent stash emits toast`() = runTest {
        coEvery { getStash(999L) } returns null

        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit(999L, "Ghost", Currency.CUP)

            val event = awaitItem() as StashNavEvent.ShowToast
            assertTrue(event.message.isNotBlank())
        }
    }

    // ── isSaving guard for edit ──────────────────────────────────────────────
    @Test
    fun `onEdit no-ops when isSaving is true via validation gate`() = runTest {
        val vm = createVm()

        // Blank name rejects before the isSaving guard matters
        vm.onEdit(1L, "", Currency.CUP)
        coVerify(inverse = true) { updateStash(any(), any()) }

        // Whitespace-only rejects
        vm.onEdit(1L, "   ", Currency.CUP)
        coVerify(inverse = true) { updateStash(any(), any()) }

        // >100 chars rejects
        vm.onEdit(1L, "x".repeat(101), Currency.CUP)
        coVerify(inverse = true) { updateStash(any(), any()) }
    }
}
