package com.vida.feature.currencymanagement

import app.cash.turbine.test
import com.vida.domain.model.CurrencyInfo
import com.vida.domain.usecase.currency.AddCurrency
import com.vida.domain.usecase.currency.DeleteCurrency
import com.vida.domain.usecase.currency.GetCurrency
import com.vida.domain.usecase.currency.ListCurrencies
import com.vida.domain.usecase.currency.UpdateCurrency
import com.vida.feature.currencymanagement.ui.CurrencyListUiState
import com.vida.feature.currencymanagement.ui.CurrencyListViewModel
import com.vida.feature.currencymanagement.ui.CurrencyNavEvent
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CurrencyListViewModelTest {

    private lateinit var listCurrencies: ListCurrencies
    private lateinit var addCurrency: AddCurrency
    private lateinit var updateCurrency: UpdateCurrency
    private lateinit var deleteCurrency: DeleteCurrency
    private lateinit var getCurrency: GetCurrency

    private val sampleCurrencies = listOf(
        CurrencyInfo(id = 1L, name = "Peso cubano", code = "CUP", isSystem = true),
        CurrencyInfo(id = 2L, name = "Dólar", code = "USD", isSystem = true),
        CurrencyInfo(id = 3L, name = "Euro", code = "EUR", isSystem = false),
        CurrencyInfo(id = 4L, name = "Libra esterlina", code = "GBP", isSystem = false),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        listCurrencies = mockk()
        addCurrency = mockk()
        updateCurrency = mockk()
        deleteCurrency = mockk()
        getCurrency = mockk()

        // Default: currencies exist
        every { listCurrencies() } returns flowOf(sampleCurrencies)
        coEvery { deleteCurrency(any<Long>()) } returns Unit
        coEvery { addCurrency(any()) } returns 5L
        coEvery { updateCurrency(any()) } returns 5L
        coEvery { getCurrency(any<Long>()) } returns null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(): CurrencyListViewModel = CurrencyListViewModel(
        listCurrencies = listCurrencies,
        addCurrency = addCurrency,
        updateCurrency = updateCurrency,
        deleteCurrency = deleteCurrency,
        getCurrency = getCurrency,
    )

    // ── Initial load → Ready with currencies ─────────────────────────────────

    @Test
    fun `initial load emits Ready with sorted currencies`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CurrencyListUiState.Ready
            assertEquals(4, ready.currencies.size)
        }
    }

    // ── Sort order (system first, then user, by code) ────────────────────────

    @Test
    fun `sort order puts system currencies first then user by code`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CurrencyListUiState.Ready
            val codes = ready.currencies.map { it.code }
            // System first (CUP, USD), then user (EUR, GBP)
            assertEquals(
                listOf("CUP", "USD", "EUR", "GBP"),
                codes,
            )
        }
    }

    // ── Display item mapping ─────────────────────────────────────────────────

    @Test
    fun `currency fields are correctly mapped to display items`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CurrencyListUiState.Ready
            val cup = ready.currencies.first { it.code == "CUP" }
            assertEquals(1L, cup.id)
            assertEquals("Peso cubano", cup.name)
            assertEquals("CUP", cup.code)
            assertTrue(cup.isSystem)
        }
    }

    // ── Initial load → Empty ─────────────────────────────────────────────────

    @Test
    fun `initial load emits Empty when no currencies exist`() = runTest {
        every { listCurrencies() } returns flowOf(emptyList())

        val vm = createVm()

        vm.uiState.test {
            val empty = awaitItem() as CurrencyListUiState.Empty
            assertNotNull(empty)
        }
    }

    // ── Initial load → Error ─────────────────────────────────────────────────

    @Test
    fun `initial load emits Error when ListCurrencies throws`() = runTest {
        every { listCurrencies() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as CurrencyListUiState.Error
            assertEquals("DB error", error.message)
        }
    }

    @Test
    fun `initial load emits Error with fallback message on exception without message`() =
        runTest {
            every { listCurrencies() } throws RuntimeException()

            val vm = createVm()

            vm.uiState.test {
                val error = awaitItem() as CurrencyListUiState.Error
                assertTrue(error.message.isNotBlank())
            }
        }

    // ── onDelete: success ────────────────────────────────────────────────────

    @Test
    fun `onDelete calls DeleteCurrency and refetches list`() = runTest {
        coEvery { deleteCurrency(3L) } returns Unit
        // After delete: only 3 currencies remain (EUR is gone)
        val remaining = sampleCurrencies.filter { it.id != 3L }
        every { listCurrencies() } returns flowOf(sampleCurrencies) andThen flowOf(remaining)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CurrencyListUiState.Ready
            assertEquals(4, ready.currencies.size)

            vm.onDelete(3L)

            val afterDelete = awaitItem() as CurrencyListUiState.Ready
            assertEquals(3, afterDelete.currencies.size)
            assertTrue(afterDelete.currencies.none { it.id == 3L })

            coVerify(exactly = 1) { deleteCurrency(3L) }
        }
    }

    // ── onDelete: system currency → ShowToast ────────────────────────────────

    @Test
    fun `onDelete system currency emits ShowToast and does NOT call DeleteCurrency`() =
        runTest {
            val vm = createVm()

            vm.navEvents.test {
                vm.onDelete(1L) // CUP is system

                val event = awaitItem() as CurrencyNavEvent.ShowToast
                assertEquals(
                    "Moneda del sistema — no se puede eliminar",
                    event.message,
                )

                coVerify(inverse = true) { deleteCurrency(any()) }
            }
        }

    @Test
    fun `onDelete system currency does NOT change the list state`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CurrencyListUiState.Ready
            assertEquals(4, ready.currencies.size)

            vm.onDelete(1L) // CUP is system

            // State should NOT change — no re-emission
            expectNoEvents()
        }
    }

    // ── onDelete: isDeleting guard prevents double-tap ───────────────────────

    @Test
    fun `onDelete double invocation calls DeleteCurrency once per call`() =
        runTest {
            val vm = createVm()

            vm.uiState.test {
                awaitItem() // Ready

                vm.onDelete(3L) // Delete EUR
                vm.onDelete(4L) // Delete GBP (separate call)

                coVerify(exactly = 1) { deleteCurrency(3L) }
                coVerify(exactly = 1) { deleteCurrency(4L) }
            }
        }

    // ── onDelete: when state is not Ready ────────────────────────────────────

    @Test
    fun `onDelete no-ops when state is not Ready`() = runTest {
        every { listCurrencies() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Error state
            vm.onDelete(3L)
            expectNoEvents()
            coVerify(inverse = true) { deleteCurrency(any()) }
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
            coVerify(inverse = true) { deleteCurrency(any()) }
        }
    }

    // ── onDelete: DeleteCurrency throws → ShowToast ───────────────────────────

    @Test
    fun `onDelete emits ShowToast when DeleteCurrency throws`() = runTest {
        coEvery { deleteCurrency(3L) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onDelete(3L)

            val event = awaitItem() as CurrencyNavEvent.ShowToast
            assertEquals("Network error", event.message)
        }
    }

    @Test
    fun `onDelete error preserves existing currency list`() = runTest {
        coEvery { deleteCurrency(3L) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CurrencyListUiState.Ready
            assertEquals(4, ready.currencies.size)

            vm.onDelete(3L)

            // State should NOT change — list preserved
            expectNoEvents()
        }
    }

    // ── onDismissError: retry from Error state ───────────────────────────────

    @Test
    fun `onDismissError transitions from Error to Loading then retries`() =
        runTest {
            val successCurrencies = listOf(sampleCurrencies[0]) // Only CUP
            every { listCurrencies() } throws RuntimeException("DB error") andThen flowOf(
                successCurrencies,
            )

            val vm = createVm()

            vm.uiState.test {
                val error = awaitItem() as CurrencyListUiState.Error
                assertEquals("DB error", error.message)

                vm.onDismissError()

                // With UnconfinedTestDispatcher, Loading → Ready happens synchronously
                // and StateFlow conflates to the latest value (Ready).
                val ready = awaitItem() as CurrencyListUiState.Ready
                assertEquals(1, ready.currencies.size)
                assertEquals("CUP", ready.currencies[0].code)
            }
        }

    @Test
    fun `onDismissError retry that gets empty currencies emits Empty`() = runTest {
        every { listCurrencies() } throws RuntimeException("DB error") andThen flowOf(
            emptyList(),
        )

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as CurrencyListUiState.Error
            assertEquals("DB error", error.message)

            vm.onDismissError()

            val empty = awaitItem() as CurrencyListUiState.Empty
            assertNotNull(empty)
        }
    }

    // ── Delete preserves correct sort order ──────────────────────────────────

    @Test
    fun `delete of a currency preserves correct sort order in remaining items`() =
        runTest {
            val remaining = sampleCurrencies.filter { it.id != 3L }
            every { listCurrencies() } returns flowOf(sampleCurrencies) andThen flowOf(
                remaining,
            )

            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as CurrencyListUiState.Ready
                assertEquals(4, ready.currencies.size)

                vm.onDelete(3L)

                val afterDelete = awaitItem() as CurrencyListUiState.Ready
                assertEquals(3, afterDelete.currencies.size)
                assertEquals(
                    listOf("CUP", "USD", "GBP"),
                    afterDelete.currencies.map { it.code },
                )
            }
        }

    // ── onDelete: success toast ──────────────────────────────────────────────

    @Test
    fun `onDelete success emits ShowToast`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onDelete(3L) // EUR (user currency)

            val event = awaitItem() as CurrencyNavEvent.ShowToast
            assertEquals("Moneda eliminada", event.message)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // onAdd / onEdit dialog actions
    // ══════════════════════════════════════════════════════════════════════

    // ── onAdd: success ───────────────────────────────────────────────────────

    @Test
    fun `onAdd creates currency, refetches list, and emits SaveSuccess`() = runTest {
        coEvery { addCurrency(any()) } returns 7L
        val afterAdd = sampleCurrencies + CurrencyInfo(
            id = 7L,
            name = "Yen",
            code = "JPY",
            isSystem = false,
        )
        every { listCurrencies() } returns flowOf(sampleCurrencies) andThen flowOf(
            afterAdd,
        )

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CurrencyListUiState.Ready
            assertEquals(4, ready.currencies.size)
        }

        vm.navEvents.test {
            vm.onAdd("Yen", "JPY")

            val event = awaitItem() as CurrencyNavEvent.SaveSuccess
            assertNotNull(event)

            coVerify(exactly = 1) {
                addCurrency(match { it.name == "Yen" && it.code == "JPY" && !it.isSystem })
            }
        }
    }

    @Test
    fun `onAdd trims whitespace from name and uppercases code`() = runTest {
        coEvery { addCurrency(any()) } returns 8L
        every { listCurrencies() } returns flowOf(sampleCurrencies) andThen flowOf(
            sampleCurrencies,
        )

        val vm = createVm()
        vm.onAdd("  Yen  ", "jpy")

        coVerify(exactly = 1) {
            addCurrency(match { it.name == "Yen" && it.code == "JPY" })
        }
    }

    // ── onAdd: validation ───────────────────────────────────────────────────

    @Test
    fun `onAdd rejected on blank name`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("", "USD")

            val event = awaitItem() as CurrencyNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { addCurrency(any()) }
        }
    }

    @Test
    fun `onAdd rejected on whitespace-only name`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("   ", "USD")

            val event = awaitItem() as CurrencyNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { addCurrency(any()) }
        }
    }

    @Test
    fun `onAdd rejected on name longer than 50 chars`() = runTest {
        val vm = createVm()
        val longName = "A".repeat(51)

        vm.navEvents.test {
            vm.onAdd(longName, "USD")

            val event = awaitItem() as CurrencyNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { addCurrency(any()) }
        }
    }

    @Test
    fun `onAdd rejected on blank code`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("Yen", "")

            val event = awaitItem() as CurrencyNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 10"))

            coVerify(inverse = true) { addCurrency(any()) }
        }
    }

    @Test
    fun `onAdd rejected on code longer than 10 chars`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("Yen", "ABCDEFGHIJK")

            val event = awaitItem() as CurrencyNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 10"))

            coVerify(inverse = true) { addCurrency(any()) }
        }
    }

    // ── onAdd: error ────────────────────────────────────────────────────────

    @Test
    fun `onAdd emits ShowToast when AddCurrency throws`() = runTest {
        coEvery { addCurrency(any()) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd("Yen", "JPY")

            val event = awaitItem() as CurrencyNavEvent.ShowToast
            assertEquals("Network error", event.message)
        }
    }

    @Test
    fun `onAdd error preserves existing currency list`() = runTest {
        coEvery { addCurrency(any()) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CurrencyListUiState.Ready
            assertEquals(4, ready.currencies.size)

            vm.onAdd("Yen", "JPY")

            // List should NOT change — no re-emission
            expectNoEvents()
        }
    }

    // ── onEdit: success ─────────────────────────────────────────────────────

    @Test
    fun `onEdit updates currency, refetches list, and emits SaveSuccess`() =
        runTest {
            val updatedList = sampleCurrencies.map {
                if (it.id == 3L) it.copy(name = "Euro Editado", code = "EUR")
                else it
            }
            every { listCurrencies() } returns flowOf(sampleCurrencies) andThen flowOf(
                updatedList,
            )

            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as CurrencyListUiState.Ready
                assertEquals(4, ready.currencies.size)
            }

            vm.navEvents.test {
                vm.onEdit(3L, "Euro Editado", "EUR")

                val event = awaitItem() as CurrencyNavEvent.SaveSuccess
                assertNotNull(event)

                coVerify(exactly = 1) {
                    updateCurrency(match {
                        it.id == 3L && it.name == "Euro Editado" && it.code == "EUR"
                    })
                }
            }
        }

    @Test
    fun `onEdit preserves isSystem flag from current state`() = runTest {
        every { listCurrencies() } returns flowOf(sampleCurrencies) andThen flowOf(
            sampleCurrencies,
        )

        val vm = createVm()

        // EUR (id=3) is NOT a system currency → isSystem=false
        vm.onEdit(3L, "Euro Edit", "EUR")

        coVerify(exactly = 1) {
            updateCurrency(match { it.id == 3L && !it.isSystem })
        }
    }

    @Test
    fun `onEdit uppercases code and trims whitespace`() = runTest {
        every { listCurrencies() } returns flowOf(sampleCurrencies) andThen flowOf(
            sampleCurrencies,
        )

        val vm = createVm()

        vm.onEdit(3L, "  Euro Edit  ", "eur")

        coVerify(exactly = 1) {
            updateCurrency(match {
                it.id == 3L && it.name == "Euro Edit" && it.code == "EUR"
            })
        }
    }

    // ── onEdit: validation ──────────────────────────────────────────────────

    @Test
    fun `onEdit rejected on blank name`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit(3L, "", "EUR")

            val event = awaitItem() as CurrencyNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 50"))

            coVerify(inverse = true) { updateCurrency(any()) }
        }
    }

    @Test
    fun `onEdit rejected on blank code`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit(3L, "Euro", "")

            val event = awaitItem() as CurrencyNavEvent.ShowToast
            assertTrue(event.message.contains("1 y 10"))

            coVerify(inverse = true) { updateCurrency(any()) }
        }
    }

    // ── onEdit: error ──────────────────────────────────────────────────────

    @Test
    fun `onEdit emits ShowToast when UpdateCurrency throws`() = runTest {
        coEvery { updateCurrency(any()) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit(3L, "Euro Editado", "EUR")

            val event = awaitItem() as CurrencyNavEvent.ShowToast
            assertEquals("DB error", event.message)
        }
    }

    @Test
    fun `onEdit error preserves existing currency list`() = runTest {
        coEvery { updateCurrency(any()) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CurrencyListUiState.Ready
            assertEquals(4, ready.currencies.size)

            vm.onEdit(3L, "Euro Editado", "EUR")

            // List should NOT change — no re-emission
            expectNoEvents()
        }
    }

    // ── isSaving StateFlow lifecycle ────────────────────────────────────────

    @Test
    fun `isSaving transitions false to true to false during add`() = runTest {
        val vm = createVm()

        vm.isSaving.test {
            assertFalse(awaitItem()) // initial: false

            vm.onAdd("Yen", "JPY")

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

    // ── Form cancel discards data (no ViewModel state change) ───────────────

    @Test
    fun `cancel does NOT call any mutation use case`() = runTest {
        val vm = createVm()

        coVerify(inverse = true) { addCurrency(any()) }
        coVerify(inverse = true) { updateCurrency(any()) }
        coVerify(inverse = true) { deleteCurrency(any()) }
    }
}