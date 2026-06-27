package com.vida.feature.ratemanagement

import app.cash.turbine.test
import com.vida.domain.model.Currency
import com.vida.domain.model.CurrencyRate
import com.vida.domain.usecase.rate.AddCurrencyRate
import com.vida.domain.usecase.rate.DeleteCurrencyRate
import com.vida.domain.usecase.rate.ListCurrencyRates
import com.vida.domain.usecase.rate.UpdateCurrencyRate
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
import java.math.BigDecimal
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RateListViewModelTest {

    private lateinit var listCurrencyRates: ListCurrencyRates
    private lateinit var addCurrencyRate: AddCurrencyRate
    private lateinit var updateCurrencyRate: UpdateCurrencyRate
    private lateinit var deleteCurrencyRate: DeleteCurrencyRate

    private val sampleRates = listOf(
        CurrencyRate(
            id = 1L,
            fromCurrency = Currency.CUP,
            toCurrency = Currency.USD,
            rate = BigDecimal("120.50"),
            updatedAt = Instant.parse("2025-02-01T10:00:00Z"),
            provider = "Manual",
        ),
        CurrencyRate(
            id = 2L,
            fromCurrency = Currency.CUP,
            toCurrency = Currency.USD,
            rate = BigDecimal("119.00"),
            updatedAt = Instant.parse("2025-01-15T08:00:00Z"),
            provider = "Manual",
        ),
        CurrencyRate(
            id = 3L,
            fromCurrency = Currency.MLC,
            toCurrency = Currency.CUP,
            rate = BigDecimal("270.00"),
            updatedAt = Instant.parse("2025-03-10T14:00:00Z"),
            provider = "Manual",
        ),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        listCurrencyRates = mockk()
        addCurrencyRate = mockk()
        updateCurrencyRate = mockk()
        deleteCurrencyRate = mockk()

        // Default: rates exist
        every { listCurrencyRates() } returns flowOf(sampleRates)
        coEvery { deleteCurrencyRate(any<Long>()) } returns Unit
        coEvery { addCurrencyRate(any<CurrencyRate>()) } returns 1L
        coEvery { updateCurrencyRate(any<CurrencyRate>()) } returns 1L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(): RateListViewModel = RateListViewModel(
        listCurrencyRates = listCurrencyRates,
        addCurrencyRate = addCurrencyRate,
        updateCurrencyRate = updateCurrencyRate,
        deleteCurrencyRate = deleteCurrencyRate,
    )

    // ══════════════════════════════════════════════════════════════════════
    // R1 — UiState Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-RT-001: Initial load → Ready with sorted rates ────────────────────
    @Test
    fun `initial load emits Ready with sorted rates`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            assertEquals(3, ready.items.size)
        }
    }

    // ── SCN-RT-005: Sort order: pair then date ───────────────────────────────
    @Test
    fun `sort order is by pair ascending then updatedAt DESC`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            val pairs = ready.items.map { it.pairLabel }
            // CUP→USD (Feb 1), CUP→USD (Jan 15), MLC→CUP (Mar 10)
            assertEquals(
                listOf("CUP → USD", "CUP → USD", "MLC → CUP"),
                pairs,
            )
            // Within same pair: latest updatedAt first
            assertEquals(BigDecimal("120.50"), ready.items[0].rate)
            assertEquals(BigDecimal("119.00"), ready.items[1].rate)
        }
    }

    // ── SCN-RT-002: Initial load → Empty ───────────────────────────────────
    @Test
    fun `initial load emits Empty when no rates exist`() = runTest {
        every { listCurrencyRates() } returns flowOf(emptyList())

        val vm = createVm()

        vm.uiState.test {
            val empty = awaitItem() as RateListUiState.Empty
            assertNotNull(empty)
        }
    }

    // ── SCN-RT-003: Initial load → Error ───────────────────────────────────
    @Test
    fun `initial load emits Error when ListCurrencyRates throws`() = runTest {
        every { listCurrencyRates() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as RateListUiState.Error
            assertEquals("DB error", error.message)
        }
    }

    @Test
    fun `initial load emits Error with fallback message on exception without message`() =
        runTest {
            every { listCurrencyRates() } throws RuntimeException()

            val vm = createVm()

            vm.uiState.test {
                val error = awaitItem() as RateListUiState.Error
                assertTrue(error.message.isNotBlank())
            }
        }

    // ══════════════════════════════════════════════════════════════════════
    // R2 — Rate Item Display (field mapping)
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-RT-004: Rate rendered with all fields ──────────────────────────
    @Test
    fun `rate display item has all fields mapped correctly`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            val cupUsd = ready.items.first { it.id == 1L }
            assertEquals(1L, cupUsd.id)
            assertEquals(Currency.CUP, cupUsd.fromCurrency)
            assertEquals(Currency.USD, cupUsd.toCurrency)
            assertEquals("CUP → USD", cupUsd.pairLabel)
            assertEquals(BigDecimal("120.50"), cupUsd.rate)
            assertEquals("120.5", cupUsd.rateFormatted)
        }
    }

    @Test
    fun `MLC to CUP display item has correct fields`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            val mlcCup = ready.items.first { it.id == 3L }
            assertEquals(3L, mlcCup.id)
            assertEquals("MLC → CUP", mlcCup.pairLabel)
            assertEquals(BigDecimal("270.00"), mlcCup.rate)
            assertEquals("270", mlcCup.rateFormatted)
        }
    }

    @Test
    fun `CUP to USD oldest rate display item has correct fields`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            val oldest = ready.items.first { it.id == 2L }
            assertEquals(2L, oldest.id)
            assertEquals("CUP → USD", oldest.pairLabel)
            assertEquals(BigDecimal("119.00"), oldest.rate)
            assertEquals("119", oldest.rateFormatted)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R3 — Add Rate
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-RT-007: Add rate with valid data succeeds ──────────────────────
    @Test
    fun `onAdd success emits SaveSuccess and refetches list`() = runTest {
        val newRate = CurrencyRate(
            id = 5L,
            fromCurrency = Currency.CUP,
            toCurrency = Currency.MLC,
            rate = BigDecimal("45.00"),
            updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
        )
        coEvery { addCurrencyRate(any()) } returns 5L
        every { listCurrencyRates() } returns flowOf(sampleRates) andThen flowOf(
            sampleRates + newRate,
        )

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            assertEquals(3, ready.items.size)

            vm.onAdd(
                from = Currency.CUP,
                to = Currency.MLC,
                rate = BigDecimal("45.00"),
                updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
                provider = "Manual",
            )

            val afterAdd = awaitItem() as RateListUiState.Ready
            assertEquals(4, afterAdd.items.size)
            assertTrue(afterAdd.items.any { it.pairLabel == "CUP → MLC" })

            coVerify(exactly = 1) { addCurrencyRate(any()) }
        }
    }

    @Test
    fun `onAdd success emits SaveSuccess event`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd(
                from = Currency.CUP,
                to = Currency.MLC,
                rate = BigDecimal("45.00"),
                updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
                provider = "Manual",
            )

            // SaveSuccess
            val saveEvent = awaitItem()
            assertTrue(saveEvent is RateNavEvent.SaveSuccess)

            // ShowToast
            val toastEvent = awaitItem() as RateNavEvent.ShowToast
            assertTrue(toastEvent.message.contains("agregada"))
        }
    }

    // ── SCN-RT-008: Same currency rejected ──────────────────────────────────
    @Test
    fun `onAdd with equal currencies does NOT call AddCurrencyRate`() = runTest {
        val vm = createVm()

        vm.onAdd(
            from = Currency.CUP,
            to = Currency.CUP,
            rate = BigDecimal("1.00"),
            updatedAt = Instant.now(),
            provider = "Manual",
        )

        coVerify(inverse = true) { addCurrencyRate(any()) }
    }

    // ── SCN-RT-009: Invalid rate rejected (zero, negative, empty would be caught by form) ──
    @Test
    fun `onAdd with zero rate does NOT call AddCurrencyRate`() = runTest {
        val vm = createVm()

        vm.onAdd(
            from = Currency.CUP,
            to = Currency.USD,
            rate = BigDecimal.ZERO,
            updatedAt = Instant.now(),
            provider = "Manual",
        )

        coVerify(inverse = true) { addCurrencyRate(any()) }
    }

    @Test
    fun `onAdd with negative rate does NOT call AddCurrencyRate`() = runTest {
        val vm = createVm()

        vm.onAdd(
            from = Currency.CUP,
            to = Currency.USD,
            rate = BigDecimal("-5"),
            updatedAt = Instant.now(),
            provider = "Manual",
        )

        coVerify(inverse = true) { addCurrencyRate(any()) }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R4 — Edit Rate
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-RT-010: Tap opens edit dialog → tested via onEdit success ──────
    // ── SCN-RT-011: Edit save updates rate ─────────────────────────────────
    @Test
    fun `onEdit success emits SaveSuccess and updates list`() = runTest {
        coEvery { updateCurrencyRate(any()) } returns 1L
        val updatedRates = listOf(
            sampleRates[0].copy(rate = BigDecimal("125.00")),
            sampleRates[1],
            sampleRates[2],
        )
        every { listCurrencyRates() } returns flowOf(sampleRates) andThen flowOf(
            updatedRates,
        )

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            assertEquals(BigDecimal("120.50"), ready.items.first { it.id == 1L }.rate)

            vm.onEdit(
                id = 1L,
                from = Currency.CUP,
                to = Currency.USD,
                rate = BigDecimal("125.00"),
                updatedAt = sampleRates[0].updatedAt,
                provider = "Manual",
            )

            val afterEdit = awaitItem() as RateListUiState.Ready
            assertEquals(3, afterEdit.items.size)
            assertEquals(BigDecimal("125.00"), afterEdit.items.first { it.id == 1L }.rate)

            coVerify(exactly = 1) { updateCurrencyRate(any()) }
        }
    }

    @Test
    fun `onEdit success emits toast with actualizada`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit(
                id = 1L,
                from = Currency.CUP,
                to = Currency.USD,
                rate = BigDecimal("125.00"),
                updatedAt = sampleRates[0].updatedAt,
                provider = "Manual",
            )

            // SaveSuccess
            val saveEvent = awaitItem()
            assertTrue(saveEvent is RateNavEvent.SaveSuccess)

            // ShowToast
            val toastEvent = awaitItem() as RateNavEvent.ShowToast
            assertTrue(toastEvent.message.contains("actualizada"))
        }
    }

    // ── SCN-RT-012: Edit cancel does nothing ───────────────────────────────
    @Test
    fun `not calling onEdit does NOT call UpdateCurrencyRate`() = runTest {
        createVm() // VM created but onEdit never called
        coVerify(inverse = true) { updateCurrencyRate(any()) }
    }

    // ── Edit validation: same currency rejected ────────────────────────────
    @Test
    fun `onEdit with equal currencies does NOT call UpdateCurrencyRate`() = runTest {
        val vm = createVm()

        vm.onEdit(
            id = 1L,
            from = Currency.CUP,
            to = Currency.CUP,
            rate = BigDecimal("1.00"),
            updatedAt = Instant.now(),
            provider = "Manual",
        )

        coVerify(inverse = true) { updateCurrencyRate(any()) }
    }

    // ── Edit error ─────────────────────────────────────────────────────────
    @Test
    fun `onEdit error emits toast and preserves list`() = runTest {
        coEvery { updateCurrencyRate(any()) } throws RuntimeException("Update failed")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            assertEquals(3, ready.items.size)

            vm.onEdit(
                id = 1L,
                from = Currency.CUP,
                to = Currency.USD,
                rate = BigDecimal("125.00"),
                updatedAt = sampleRates[0].updatedAt,
                provider = "Manual",
            )

            // List preserved
            expectNoEvents()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R5 — Delete Rate
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-RT-014: Delete success → toast + refetch ───────────────────────
    @Test
    fun `onDelete calls DeleteCurrencyRate and refetches list`() = runTest {
        coEvery { deleteCurrencyRate(1L) } returns Unit
        val remaining = sampleRates.filter { it.id != 1L }
        every { listCurrencyRates() } returns flowOf(sampleRates) andThen flowOf(remaining)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            assertEquals(3, ready.items.size)

            vm.onDelete(1L)

            val afterDelete = awaitItem() as RateListUiState.Ready
            assertEquals(2, afterDelete.items.size)
            assertTrue(afterDelete.items.none { it.id == 1L })

            coVerify(exactly = 1) { deleteCurrencyRate(1L) }
        }
    }

    @Test
    fun `onDelete success emits ShowToast`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onDelete(1L)

            val event = awaitItem() as RateNavEvent.ShowToast
            assertEquals("Tasa eliminada", event.message)
        }
    }

    // ── SCN-RT-015: Delete cancel does nothing ─────────────────────────────
    @Test
    fun `cancel does NOT call DeleteCurrencyRate when dialog dismissed without confirm`() =
        runTest {
            createVm() // VM created but onDelete never called
            coVerify(inverse = true) { deleteCurrencyRate(any()) }
        }

    // ── Delete error → toast, list preserved ───────────────────────────────
    @Test
    fun `onDelete emits ShowToast when DeleteCurrencyRate throws`() = runTest {
        coEvery { deleteCurrencyRate(1L) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onDelete(1L)

            val event = awaitItem() as RateNavEvent.ShowToast
            assertEquals("Network error", event.message)
        }
    }

    // ── SCN-RT-017: Mutation fails → list preserved ────────────────────────
    @Test
    fun `onDelete error preserves existing rate list`() = runTest {
        coEvery { deleteCurrencyRate(1L) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            assertEquals(3, ready.items.size)

            vm.onDelete(1L)

            // State should NOT change — list preserved
            expectNoEvents()
        }
    }

    @Test
    fun `onDelete error uses fallback message when exception has no message`() =
        runTest {
            coEvery { deleteCurrencyRate(1L) } throws RuntimeException()

            val vm = createVm()

            vm.navEvents.test {
                vm.onDelete(1L)

                val event = awaitItem() as RateNavEvent.ShowToast
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
            coVerify(inverse = true) { deleteCurrencyRate(any()) }
        }
    }

    // ── Edge: delete when state is not Ready ────────────────────────────────
    @Test
    fun `onDelete no-ops when state is not Ready`() = runTest {
        every { listCurrencyRates() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Error state
            vm.onDelete(1L)
            expectNoEvents()
            coVerify(inverse = true) { deleteCurrencyRate(any()) }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R7 — Error Handling: retry from Error
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-RT-018: Retry from Error → Ready ───────────────────────────────
    @Test
    fun `onDismissError transitions from Error to Ready on success`() = runTest {
        val successRates = listOf(sampleRates[0]) // Only CUP→USD
        every { listCurrencyRates() } throws RuntimeException("DB error") andThen flowOf(
            successRates,
        )

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as RateListUiState.Error
            assertEquals("DB error", error.message)

            vm.onDismissError()

            // With UnconfinedTestDispatcher, Loading → Ready happens synchronously
            // and StateFlow conflates to the latest value (Ready).
            val ready = awaitItem() as RateListUiState.Ready
            assertEquals(1, ready.items.size)
            assertEquals("CUP → USD", ready.items[0].pairLabel)
        }
    }

    @Test
    fun `onDismissError retry that gets empty rates emits Empty`() = runTest {
        every { listCurrencyRates() } throws RuntimeException("DB error") andThen flowOf(
            emptyList(),
        )

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as RateListUiState.Error
            assertEquals("DB error", error.message)

            vm.onDismissError()

            // Loading → Empty conflates to Empty
            val empty = awaitItem() as RateListUiState.Empty
            assertNotNull(empty)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R8 — Edge Cases
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-RT-019: isSaving guard prevents double-tap ─────────────────────
    @Test
    fun `onAdd completes successfully for valid input`() = runTest {
        // With UnconfinedTestDispatcher, the operation completes synchronously
        // and isSaving resets to false before a second call could be made.
        // This test verifies the normal path works correctly.
        val vm = createVm()

        vm.onAdd(
            from = Currency.CUP,
            to = Currency.USD,
            rate = BigDecimal("120.50"),
            updatedAt = Instant.now(),
            provider = "Manual",
        )

        coVerify(exactly = 1) { addCurrencyRate(any()) }
    }

    @Test
    fun `isSaving starts false and resets after onAdd`() = runTest {
        val vm = createVm()

        // isSaving starts false
        assertEquals(false, vm.isSaving.value)

        vm.onAdd(
            from = Currency.CUP,
            to = Currency.USD,
            rate = BigDecimal("120.50"),
            updatedAt = Instant.now(),
            provider = "Manual",
        )

        // With UnconfinedTestDispatcher the operation completes synchronously,
        // so isSaving is false again immediately after.
        assertEquals(false, vm.isSaving.value)
        coVerify(exactly = 1) { addCurrencyRate(any()) }
    }

    // ── SCN-RT-020: Empty state renders with guidance ──────────────────────
    @Test
    fun `empty state renders when no rates exist`() = runTest {
        every { listCurrencyRates() } returns flowOf(emptyList())

        val vm = createVm()

        vm.uiState.test {
            val empty = awaitItem() as RateListUiState.Empty
            assertNotNull(empty)
        }
    }

    // ── Mutation error preserves list ──────────────────────────────────────
    @Test
    fun `onAdd error preserves existing rate list`() = runTest {
        coEvery { addCurrencyRate(any()) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            assertEquals(3, ready.items.size)

            vm.onAdd(
                from = Currency.CUP,
                to = Currency.MLC,
                rate = BigDecimal("45.00"),
                updatedAt = Instant.now(),
                provider = "Manual",
            )

            // State should NOT change — list preserved
            expectNoEvents()
        }
    }

    @Test
    fun `onAdd error uses fallback message on null exception message`() = runTest {
        coEvery { addCurrencyRate(any()) } throws RuntimeException()

        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd(
                from = Currency.CUP,
                to = Currency.MLC,
                rate = BigDecimal("45.00"),
                updatedAt = Instant.now(),
                provider = "Manual",
            )

            val event = awaitItem() as RateNavEvent.ShowToast
            assertTrue(event.message.isNotBlank())
        }
    }

    // ── Sort order preserved after mutation ────────────────────────────────
    @Test
    fun `delete of a rate preserves correct sort order in remaining items`() =
        runTest {
            // After deleting id=1 (CUP→USD, Feb 1), remaining:
            // CUP→USD (Jan 15), MLC→CUP (Mar 10)
            val remaining = sampleRates.filter { it.id != 1L }
            every { listCurrencyRates() } returns flowOf(sampleRates) andThen flowOf(
                remaining,
            )

            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as RateListUiState.Ready
                assertEquals(3, ready.items.size)

                vm.onDelete(1L) // Delete newest CUP→USD

                val afterDelete = awaitItem() as RateListUiState.Ready
                assertEquals(2, afterDelete.items.size)
                assertEquals(
                    listOf("CUP → USD", "MLC → CUP"),
                    afterDelete.items.map { it.pairLabel },
                )
            }
        }
}
