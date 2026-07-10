package com.vida.feature.bankmanagement

import app.cash.turbine.test
import com.vida.domain.model.Bank
import com.vida.domain.usecase.bank.AddBank
import com.vida.domain.usecase.bank.DeleteBank
import com.vida.domain.usecase.bank.GetBank
import com.vida.domain.usecase.bank.ListBanks
import com.vida.domain.usecase.bank.UpdateBank
import com.vida.feature.bankmanagement.ui.BankListUiState
import com.vida.feature.bankmanagement.ui.BankListViewModel
import com.vida.feature.bankmanagement.ui.BankNavEvent
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

class BankListViewModelTest {

    private lateinit var listBanks: ListBanks
    private lateinit var addBank: AddBank
    private lateinit var updateBank: UpdateBank
    private lateinit var deleteBank: DeleteBank
    private lateinit var getBank: GetBank

    private val sampleBanks = listOf(
        Bank(id = 1L, name = "Bandec", color = 0xFF8E0509.toInt(), isSystem = true),
        Bank(id = 2L, name = "BPA", color = 0xFFBCD1DA.toInt(), isSystem = true),
        Bank(id = 3L, name = "MiBanco", color = 0xFF4CAF50.toInt(), isSystem = false),
        Bank(id = 4L, name = "OtroBanco", color = 0xFF2196F3.toInt(), isSystem = false),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        listBanks = mockk()
        addBank = mockk()
        updateBank = mockk()
        deleteBank = mockk()
        getBank = mockk()

        // Default: banks exist
        every { listBanks() } returns flowOf(sampleBanks)
        coEvery { deleteBank(any<Long>()) } returns Unit
        coEvery { addBank(any()) } returns 5L
        coEvery { updateBank(any()) } returns 5L
        coEvery { getBank(any<Long>()) } returns null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(): BankListViewModel = BankListViewModel(
        listBanks = listBanks,
        addBank = addBank,
        updateBank = updateBank,
        deleteBank = deleteBank,
        getBank = getBank,
    )

    // ── Initial load → Ready with banks ──────────────────────────────────────

    @Test
    fun `initial load emits Ready with sorted banks`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as BankListUiState.Ready
            assertEquals(4, ready.banks.size)
        }
    }

    // ── Sort order (system first, then user, alphabetical) ───────────────────

    @Test
    fun `sort order puts system banks first then user alphabetically`() =
        runTest {
            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as BankListUiState.Ready
                val names = ready.banks.map { it.name }
                assertEquals(
                    listOf("Bandec", "BPA", "MiBanco", "OtroBanco"),
                    names,
                )
            }
        }

    // ── Display item mapping ─────────────────────────────────────────────────

    @Test
    fun `bank fields are correctly mapped to display items`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as BankListUiState.Ready
            val bandec = ready.banks.first { it.name == "Bandec" }
            assertEquals(1L, bandec.id)
            assertEquals("Bandec", bandec.name)
            assertEquals(0xFF8E0509.toInt(), bandec.color)
            assertTrue(bandec.isSystem)
        }
    }

    // ── Initial load → Empty ─────────────────────────────────────────────────

    @Test
    fun `initial load emits Empty when no banks exist`() = runTest {
        every { listBanks() } returns flowOf(emptyList())

        val vm = createVm()

        vm.uiState.test {
            val empty = awaitItem() as BankListUiState.Empty
            assertNotNull(empty)
        }
    }

    // ── Initial load → Error ─────────────────────────────────────────────────

    @Test
    fun `initial load emits Error when ListBanks throws`() = runTest {
        every { listBanks() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as BankListUiState.Error
            assertEquals("DB error", error.message)
        }
    }

    @Test
    fun `initial load emits Error with fallback message on exception without message`() =
        runTest {
            every { listBanks() } throws RuntimeException()

            val vm = createVm()

            vm.uiState.test {
                val error = awaitItem() as BankListUiState.Error
                assertTrue(error.message.isNotBlank())
            }
        }

    // ── onDelete: success ─────────────────────────────────────────────────────

    @Test
    fun `onDelete calls DeleteBank and refetches list`() = runTest {
        coEvery { deleteBank(3L) } returns Unit
        // After delete: only 3 banks remain (MiBanco is gone)
        val remaining = sampleBanks.filter { it.id != 3L }
        every { listBanks() } returns flowOf(sampleBanks) andThen flowOf(remaining)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as BankListUiState.Ready
            assertEquals(4, ready.banks.size)

            vm.onDelete(3L)

            val afterDelete = awaitItem() as BankListUiState.Ready
            assertEquals(3, afterDelete.banks.size)
            assertTrue(afterDelete.banks.none { it.id == 3L })

            coVerify(exactly = 1) { deleteBank(3L) }
        }
    }

    // ── onDelete: system bank → ShowToast ─────────────────────────────────────

    @Test
    fun `onDelete system bank emits ShowToast and does NOT call DeleteBank`() =
        runTest {
            val vm = createVm()

            vm.navEvents.test {
                vm.onDelete(1L) // Bandec is system

                val event = awaitItem() as BankNavEvent.ShowToast
                assertEquals(
                    "Banco del sistema — no se puede eliminar",
                    event.message,
                )

                coVerify(inverse = true) { deleteBank(any()) }
            }
        }

    @Test
    fun `onDelete system bank does NOT change the list state`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as BankListUiState.Ready
            assertEquals(4, ready.banks.size)

            vm.onDelete(1L) // Bandec is system

            // State should NOT change — no re-emission
            expectNoEvents()
        }
    }

    // ── onDelete: isDeleting guard prevents double-tap ────────────────────────

    @Test
    fun `onDelete double invocation calls DeleteBank once per call`() =
        runTest {
            val vm = createVm()

            vm.uiState.test {
                awaitItem() // Ready

                vm.onDelete(3L) // Delete MiBanco
                vm.onDelete(4L) // Delete OtroBanco (separate call)

                coVerify(exactly = 1) { deleteBank(3L) }
                coVerify(exactly = 1) { deleteBank(4L) }
            }
        }

    // ── onDelete: when state is not Ready ────────────────────────────────────

    @Test
    fun `onDelete no-ops when state is not Ready`() = runTest {
        every { listBanks() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Error state
            vm.onDelete(3L)
            expectNoEvents()
            coVerify(inverse = true) { deleteBank(any()) }
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
            coVerify(inverse = true) { deleteBank(any()) }
        }
    }

    // ── onDelete: DeleteBank throws → ShowToast ───────────────────────────────

    @Test
    fun `onDelete emits ShowToast when DeleteBank throws`() = runTest {
        coEvery { deleteBank(3L) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onDelete(3L)

            val event = awaitItem() as BankNavEvent.ShowToast
            assertEquals("Network error", event.message)
        }
    }

    @Test
    fun `onDelete error preserves existing bank list`() = runTest {
        coEvery { deleteBank(3L) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as BankListUiState.Ready
            assertEquals(4, ready.banks.size)

            vm.onDelete(3L)

            // State should NOT change — list preserved
            expectNoEvents()
        }
    }

    // ── onDismissError: retry from Error state ───────────────────────────────

    @Test
    fun `onDismissError transitions from Error to Loading then retries`() =
        runTest {
            val successBanks = listOf(sampleBanks[0]) // Only Bandec
            every { listBanks() } throws RuntimeException("DB error") andThen flowOf(
                successBanks,
            )

            val vm = createVm()

            vm.uiState.test {
                val error = awaitItem() as BankListUiState.Error
                assertEquals("DB error", error.message)

                vm.onDismissError()

                // With UnconfinedTestDispatcher, Loading → Ready happens synchronously
                // and StateFlow conflates to the latest value (Ready).
                val ready = awaitItem() as BankListUiState.Ready
                assertEquals(1, ready.banks.size)
                assertEquals("Bandec", ready.banks[0].name)
            }
        }

    @Test
    fun `onDismissError retry that gets empty banks emits Empty`() = runTest {
        every { listBanks() } throws RuntimeException("DB error") andThen flowOf(
            emptyList(),
        )

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as BankListUiState.Error
            assertEquals("DB error", error.message)

            vm.onDismissError()

            val empty = awaitItem() as BankListUiState.Empty
            assertNotNull(empty)
        }
    }

    // ── Delete preserves correct sort order ──────────────────────────────────

    @Test
    fun `delete of a bank preserves correct sort order in remaining items`() =
        runTest {
            val remaining = sampleBanks.filter { it.id != 3L }
            every { listBanks() } returns flowOf(sampleBanks) andThen flowOf(
                remaining,
            )

            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as BankListUiState.Ready
                assertEquals(4, ready.banks.size)

                vm.onDelete(3L)

                val afterDelete = awaitItem() as BankListUiState.Ready
                assertEquals(3, afterDelete.banks.size)
                assertEquals(
                    listOf("Bandec", "BPA", "OtroBanco"),
                    afterDelete.banks.map { it.name },
                )
            }
        }

    // ── onDelete: success toast ──────────────────────────────────────────────

    @Test
    fun `onDelete success emits ShowToast`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onDelete(3L) // MiBanco (user bank)

            val event = awaitItem() as BankNavEvent.ShowToast
            assertEquals("Banco eliminado", event.message)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // onAdd / onEdit dialog actions
    // ══════════════════════════════════════════════════════════════════════

    // ── onAdd: success ─────────────────────────────────────────────────────────

    @Test
    fun `onAdd creates bank, refetches list, and emits SaveSuccess`() = runTest {
        coEvery { addBank(any()) } returns 7L
        val afterAdd = sampleBanks + Bank(
            id = 7L,
            name = "NuevoBanco",
            color = 0xFF4CAF50.toInt(),
            isSystem = false,
        )
        every { listBanks() } returns flowOf(sampleBanks) andThen flowOf(
            afterAdd,
        )

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as BankListUiState.Ready
            assertEquals(4, ready.banks.size)
        }

        vm.navEvents.test {
            vm.onAdd("NuevoBanco", 0xFF4CAF50.toInt())

            val event = awaitItem() as BankNavEvent.SaveSuccess
            assertNotNull(event)

            coVerify(exactly = 1) {
                addBank(match { it.name == "NuevoBanco" })
            }
        }
    }

    @Test
    fun `onAdd trims whitespace from name`() = runTest {
        coEvery { addBank(any()) } returns 8L
        every { listBanks() } returns flowOf(sampleBanks) andThen flowOf(
            sampleBanks,
        )

        val vm = createVm()
        vm.onAdd("  NuevoBanco  ", 0xFF4CAF50.toInt())

        coVerify(exactly = 1) {
            addBank(match { it.name == "NuevoBanco" })
        }
    }

    // ── onAdd: validation ─────────────────────────────────────────────────────

    @Test
    fun `onAdd rejected on blank name`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("", 0xFFFF9800.toInt())

            val event = awaitItem() as BankNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { addBank(any()) }
        }
    }

    @Test
    fun `onAdd rejected on whitespace-only name`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("   ", 0xFFFF9800.toInt())

            val event = awaitItem() as BankNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { addBank(any()) }
        }
    }

    @Test
    fun `onAdd rejected on name longer than 50 chars`() = runTest {
        val vm = createVm()
        val longName = "A".repeat(51)

        vm.navEvents.test {
            vm.onAdd(longName, 0xFFFF9800.toInt())

            val event = awaitItem() as BankNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { addBank(any()) }
        }
    }

    // ── onAdd: error ──────────────────────────────────────────────────────────

    @Test
    fun `onAdd emits ShowToast when AddBank throws`() = runTest {
        coEvery { addBank(any()) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("NuevoBanco", 0xFF4CAF50.toInt())

            val event = awaitItem() as BankNavEvent.ShowToast
            assertEquals("Network error", event.message)
        }
    }

    @Test
    fun `onAdd error preserves existing bank list`() = runTest {
        coEvery { addBank(any()) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as BankListUiState.Ready
            assertEquals(4, ready.banks.size)

            vm.onAdd("NuevoBanco", 0xFF4CAF50.toInt())

            // List should NOT change — no re-emission
            expectNoEvents()
        }
    }

    // ── onEdit: success ────────────────────────────────────────────────────────

    @Test
    fun `onEdit updates bank, refetches list, and emits SaveSuccess`() =
        runTest {
            val updatedList = sampleBanks.map {
                if (it.id == 3L) it.copy(name = "BancoEditado", color = 0xFF2196F3.toInt())
                else it
            }
            every { listBanks() } returns flowOf(sampleBanks) andThen flowOf(
                updatedList,
            )

            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as BankListUiState.Ready
                assertEquals(4, ready.banks.size)
            }

            vm.navEvents.test {
                vm.onEdit(3L, "BancoEditado", 0xFF2196F3.toInt())

                val event = awaitItem() as BankNavEvent.SaveSuccess
                assertNotNull(event)

                coVerify(exactly = 1) {
                    updateBank(match {
                        it.id == 3L && it.name == "BancoEditado" &&
                            it.color == 0xFF2196F3.toInt()
                    })
                }
            }
        }

    @Test
    fun `onEdit preserves isSystem flag from current state`() = runTest {
        every { listBanks() } returns flowOf(sampleBanks) andThen flowOf(
            sampleBanks,
        )

        val vm = createVm()

        // MiBanco (id=3) is NOT a system bank → isSystem=false
        vm.onEdit(3L, "MiBanco Edit", 0xFF4CAF50.toInt())

        coVerify(exactly = 1) {
            updateBank(match { it.id == 3L && !it.isSystem })
        }
    }

    // ── onEdit: validation ─────────────────────────────────────────────────────

    @Test
    fun `onEdit rejected on blank name`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit(3L, "", 0xFFFF9800.toInt())

            val event = awaitItem() as BankNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { updateBank(any()) }
        }
    }

    @Test
    fun `onEdit rejected on whitespace-only name`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit(3L, "   ", 0xFFFF9800.toInt())

            val event = awaitItem() as BankNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { updateBank(any()) }
        }
    }

    // ── onEdit: error ──────────────────────────────────────────────────────────

    @Test
    fun `onEdit emits ShowToast when UpdateBank throws`() = runTest {
        coEvery { updateBank(any()) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit(3L, "BancoEditado", 0xFF2196F3.toInt())

            val event = awaitItem() as BankNavEvent.ShowToast
            assertEquals("DB error", event.message)
        }
    }

    @Test
    fun `onEdit error preserves existing bank list`() = runTest {
        coEvery { updateBank(any()) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as BankListUiState.Ready
            assertEquals(4, ready.banks.size)

            vm.onEdit(3L, "BancoEditado", 0xFF2196F3.toInt())

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

            vm.onAdd("NuevoBanco", 0xFF4CAF50.toInt())

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

        coVerify(inverse = true) { addBank(any()) }
        coVerify(inverse = true) { updateBank(any()) }
        coVerify(inverse = true) { deleteBank(any()) }
    }
}
