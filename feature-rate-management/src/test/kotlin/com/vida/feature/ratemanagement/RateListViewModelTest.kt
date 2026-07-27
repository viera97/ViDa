package com.vida.feature.ratemanagement

import app.cash.turbine.test
import com.vida.domain.model.CurrencyRate
import com.vida.domain.usecase.currency.ListCurrencies
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RateListViewModelTest {

    private lateinit var listCurrencyRates: ListCurrencyRates
    private lateinit var addCurrencyRate: AddCurrencyRate
    private lateinit var updateCurrencyRate: UpdateCurrencyRate
    private lateinit var deleteCurrencyRate: DeleteCurrencyRate
    private lateinit var listCurrencies: ListCurrencies

    // sampleRates uses pair groups: CUP↔USD (ids 1,2) and MLC↔CUP (id 3).
    // Inverse pairing is collapsed into a single display item.
    private val sampleRates = listOf(
        CurrencyRate(
            id = 1L,
            fromCurrency = "CUP",
            toCurrency = "USD",
            rate = BigDecimal("120.50"),
            updatedAt = Instant.parse("2025-02-01T10:00:00Z"),
            provider = "Manual",
        ),
        CurrencyRate(
            id = 2L,
            fromCurrency = "USD",
            toCurrency = "CUP",
            rate = BigDecimal("0.0083"),
            updatedAt = Instant.parse("2025-02-01T10:00:00Z"),
            provider = "Manual",
        ),
        CurrencyRate(
            id = 3L,
            fromCurrency = "CUP",
            toCurrency = "USD",
            rate = BigDecimal("119.00"),
            updatedAt = Instant.parse("2025-01-15T08:00:00Z"),
            provider = "Manual",
        ),
        CurrencyRate(
            id = 4L,
            fromCurrency = "MLC",
            toCurrency = "CUP",
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
        listCurrencies = mockk()

        every { listCurrencyRates() } returns flowOf(sampleRates)
        coEvery { deleteCurrencyRate(any<Long>()) } returns Unit
        coEvery { addCurrencyRate(any<CurrencyRate>()) } returns 1L
        coEvery { updateCurrencyRate(any<CurrencyRate>()) } returns 1L
        every { listCurrencies() } returns flowOf(emptyList())
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
        listCurrencies = listCurrencies,
    )

    // ══════════════════════════════════════════════════════════════════════
    // R1 — UiState Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `initial load emits Ready with rates grouped by pair`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            // CUP↔USD (id 1, primary) and MLC→CUP (id 4) collapse to 2 cards
            assertEquals(2, ready.items.size)
        }
    }

    @Test
    fun `card primary is CUP to USD and its inverse is USD to CUP`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            val cupUsd = ready.items.first { it.pairLabel == "CUP → USD" }
            assertEquals("CUP → USD", cupUsd.pairLabel)
            assertNotNull(cupUsd.inverse)
            assertEquals("USD → CUP", cupUsd.inverse!!.pairLabelFromCodes())
        }
    }

    @Test
    fun `MLC to CUP card has no inverse in sampleRates`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            val mlcCup = ready.items.first { it.pairLabel == "MLC → CUP" }
            assertNull(mlcCup.inverse)
        }
    }

    @Test
    fun `initial load emits Empty when no rates exist`() = runTest {
        every { listCurrencyRates() } returns flowOf(emptyList())

        val vm = createVm()

        vm.uiState.test {
            val empty = awaitItem() as RateListUiState.Empty
            assertNotNull(empty)
        }
    }

    @Test
    fun `initial load emits Error when ListCurrencyRates throws`() = runTest {
        every { listCurrencyRates() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as RateListUiState.Error
            assertEquals("DB error", error.message)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R2 — Rate Item Display
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `rate display item has primary fields mapped correctly`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            val cupUsd = ready.items.first { it.id == 1L }
            assertEquals("CUP", cupUsd.fromCurrency)
            assertEquals("USD", cupUsd.toCurrency)
            assertEquals(BigDecimal("120.50"), cupUsd.rate)
            assertEquals("120.5", cupUsd.rateFormatted)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R3 — Add Rate (creates pair + inverse)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `onAdd inserts primary AND inverse rates`() = runTest {
        val vm = createVm()

        vm.onAdd(
            fromCode = "MLC",
            toCode = "USD",
            rate = BigDecimal("2.50"),
            updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
            provider = "Manual",
        )

        // Two inserts: primary (MLC→USD @ 2.50) and inverse (USD→MLC @ 0.4)
        coVerify(exactly = 1) { addCurrencyRate(match { it.fromCurrency == "MLC" && it.toCurrency == "USD" && it.rate.compareTo(BigDecimal("2.50")) == 0 }) }
        coVerify(exactly = 1) { addCurrencyRate(match { it.fromCurrency == "USD" && it.toCurrency == "MLC" && it.rate.compareTo(BigDecimal("0.4")) == 0 }) }
    }

    @Test
    fun `onAdd success emits SaveSuccess and ShowToast agregada`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd(
                fromCode = "MLC",
                toCode = "USD",
                rate = BigDecimal("2.50"),
                updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
                provider = "Manual",
            )

            assertTrue(awaitItem() is RateNavEvent.SaveSuccess)
            val toast = awaitItem() as RateNavEvent.ShowToast
            assertTrue(toast.message.contains("agregada"))
        }
    }

    @Test
    fun `onAdd with equal currencies does NOT call AddCurrencyRate`() = runTest {
        val vm = createVm()

        vm.onAdd(
            fromCode = "CUP",
            toCode = "CUP",
            rate = BigDecimal("1.00"),
            updatedAt = Instant.now(),
            provider = "Manual",
        )

        coVerify(inverse = true) { addCurrencyRate(any()) }
    }

    @Test
    fun `onAdd with zero rate does NOT call AddCurrencyRate`() = runTest {
        val vm = createVm()

        vm.onAdd(
            fromCode = "CUP",
            toCode = "USD",
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
            fromCode = "CUP",
            toCode = "USD",
            rate = BigDecimal("-5"),
            updatedAt = Instant.now(),
            provider = "Manual",
        )

        coVerify(inverse = true) { addCurrencyRate(any()) }
    }

    // ── Duplicate detection ──

    @Test
    fun `onAdd with duplicate rate (same from, to, provider) does NOT call AddCurrencyRate`() =
        runTest {
            val vm = createVm()

            // sampleRates[0] = CUP→USD, Manual
            vm.onAdd(
                fromCode = "CUP",
                toCode = "USD",
                rate = BigDecimal("120.50"),
                updatedAt = Instant.parse("2025-02-01T10:00:00Z"),
                provider = "Manual",
            )

            coVerify(inverse = true) { addCurrencyRate(any()) }
        }

    @Test
    fun `onAdd duplicate emits DuplicateRate and does NOT emit SaveSuccess`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd(
                fromCode = "CUP",
                toCode = "USD",
                rate = BigDecimal("120.50"),
                updatedAt = Instant.parse("2025-02-01T10:00:00Z"),
                provider = "Manual",
            )

            assertTrue(awaitItem() is RateNavEvent.DuplicateRate)
            expectNoEvents()
        }
    }

    @Test
    fun `onAdd with same pair but different provider succeeds`() = runTest {
        val vm = createVm()

        vm.onAdd(
            fromCode = "CUP",
            toCode = "USD",
            rate = BigDecimal("121.00"),
            updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
            provider = "BCV",
        )

        coVerify(exactly = 1) { addCurrencyRate(match { it.fromCurrency == "CUP" }) }
        coVerify(exactly = 1) { addCurrencyRate(match { it.fromCurrency == "USD" }) }
    }

    // ── Add error ──

    @Test
    fun `onAdd error preserves existing rate list`() = runTest {
        coEvery { addCurrencyRate(any()) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAdd(
                fromCode = "MLC",
                toCode = "USD",
                rate = BigDecimal("2.50"),
                updatedAt = Instant.now(),
                provider = "Manual",
            )

            expectNoEvents() // List preserved
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R4 — Edit Rate (updates pair + inverse)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `onEdit updates primary rate and recomputes inverse`() = runTest {
        val vm = createVm()

        vm.onEdit(
            id = 1L,
            fromCode = "CUP",
            toCode = "USD",
            rate = BigDecimal("125.00"),
            updatedAt = Instant.parse("2025-02-02T10:00:00Z"),
            provider = "Manual",
        )

        // Primary update
        coVerify(exactly = 1) { updateCurrencyRate(match { it.id == 1L && it.rate.compareTo(BigDecimal("125.00")) == 0 }) }
        // Inverse update (id=2, USD→CUP, new rate = 1/125 = 0.008)
        coVerify(exactly = 1) { updateCurrencyRate(match { it.id == 2L && it.fromCurrency == "USD" && it.toCurrency == "CUP" && it.rate.compareTo(BigDecimal("0.008")) == 0 }) }
    }

    @Test
    fun `onEdit with equal currencies does NOT call UpdateCurrencyRate`() = runTest {
        val vm = createVm()

        vm.onEdit(
            id = 1L,
            fromCode = "CUP",
            toCode = "CUP",
            rate = BigDecimal("1.00"),
            updatedAt = Instant.now(),
            provider = "Manual",
        )

        coVerify(inverse = true) { updateCurrencyRate(any()) }
    }

    @Test
    fun `onEdit error preserves list`() = runTest {
        coEvery { updateCurrencyRate(any()) } throws RuntimeException("Update failed")

        val vm = createVm()

        vm.uiState.test {
            awaitItem()
            vm.onEdit(
                id = 1L,
                fromCode = "CUP",
                toCode = "USD",
                rate = BigDecimal("125.00"),
                updatedAt = Instant.now(),
                provider = "Manual",
            )
            expectNoEvents()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R5 — Delete Rate (cascades to inverse)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `onDelete removes primary AND inverse rates`() = runTest {
        val vm = createVm()

        vm.onDelete(1L)

        coVerify(exactly = 1) { deleteCurrencyRate(1L) }
        coVerify(exactly = 1) { deleteCurrencyRate(2L) }
    }

    @Test
    fun `onDelete of card without inverse only removes primary`() = runTest {
        val vm = createVm()

        vm.onDelete(4L) // MLC→CUP, no inverse in sampleRates

        coVerify(exactly = 1) { deleteCurrencyRate(4L) }
        coVerify(exactly = 0) { deleteCurrencyRate(1L) }
        coVerify(exactly = 0) { deleteCurrencyRate(2L) }
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

    @Test
    fun `onDelete with non-existent id no-ops`() = runTest {
        val vm = createVm()

        vm.onDelete(999L)

        coVerify(inverse = true) { deleteCurrencyRate(any()) }
    }

    @Test
    fun `onDelete no-ops when state is not Ready`() = runTest {
        every { listCurrencyRates() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.onDelete(1L)
        coVerify(inverse = true) { deleteCurrencyRate(any()) }
    }

    @Test
    fun `onDelete emits ShowToast when DeleteCurrencyRate throws`() = runTest {
        coEvery { deleteCurrencyRate(any<Long>()) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onDelete(1L)
            val event = awaitItem() as RateNavEvent.ShowToast
            assertEquals("Network error", event.message)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R6 — Inverse rate computation
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `inverse rate is computed as one over primary rate`() = runTest {
        val vm = createVm()

        vm.onAdd(
            fromCode = "MLC",
            toCode = "USD",
            rate = BigDecimal("2.50"),
            updatedAt = Instant.now(),
            provider = "Manual",
        )

        // 1 / 2.50 = 0.4
        coVerify(exactly = 1) {
            addCurrencyRate(
                match {
                    it.fromCurrency == "USD" &&
                        it.toCurrency == "MLC" &&
                        it.rate.compareTo(BigDecimal("0.4")) == 0
                },
            )
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R7 — Error Handling
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `onDismissError transitions from Error to Ready`() = runTest {
        every { listCurrencyRates() } throws RuntimeException("DB error") andThen flowOf(
            sampleRates,
        )

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as RateListUiState.Error
            assertEquals("DB error", error.message)

            vm.onDismissError()

            val ready = awaitItem() as RateListUiState.Ready
            // 2 cards: CUP↔USD and MLC→CUP
            assertEquals(2, ready.items.size)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R8 — Edge Cases
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `empty state renders when no rates exist`() = runTest {
        every { listCurrencyRates() } returns flowOf(emptyList())

        val vm = createVm()

        vm.uiState.test {
            val empty = awaitItem() as RateListUiState.Empty
            assertNotNull(empty)
        }
    }

    @Test
    fun `getRateForConversion returns latest matching rate for given provider`() {
        val vm = createVm()

        val rate = vm.getRateForConversion("CUP", "USD", "Manual")
        assertNotNull(rate)
        // id=1 is the latest CUP→USD (2025-02-01 > 2025-01-15)
        assertEquals(1L, rate!!.id)
        assertEquals(BigDecimal("120.50"), rate.rate)
    }

    @Test
    fun `getRateForConversion defaults to Manual provider when omitted`() {
        val vm = createVm()

        val rate = vm.getRateForConversion("CUP", "USD")
        assertNotNull(rate)
        assertEquals("Manual", rate!!.provider)
    }

    @Test
    fun `getRateForConversion returns null for missing pair`() {
        val vm = createVm()

        val rate = vm.getRateForConversion("USD", "MLC", "Manual")
        assertNull(rate)
    }

    @Test
    fun `getRateForConversion filters by provider`() = runTest {
        // Add a BCV rate for the same pair so we can verify provider filter
        val ratesWithBcv = sampleRates + CurrencyRate(
            id = 10L,
            fromCurrency = "CUP",
            toCurrency = "USD",
            rate = BigDecimal("118.00"),
            updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
            provider = "BCV",
        )
        every { listCurrencyRates() } returns flowOf(ratesWithBcv)

        val vm = createVm()

        // Manual still resolves to id=1 (latest Manual rate)
        assertEquals(1L, vm.getRateForConversion("CUP", "USD", "Manual")!!.id)
        // BCV resolves to id=10
        assertEquals(10L, vm.getRateForConversion("CUP", "USD", "BCV")!!.id)
        // Unknown provider → null
        assertNull(vm.getRateForConversion("CUP", "USD", "Desconocido"))
    }

    @Test
    fun `availableProviders exposes distinct providers sorted`() = runTest {
        val vm = createVm()

        vm.availableProviders.test {
            // UnconfinedTestDispatcher runs init { loadRates() } before we
            // subscribe, so the first emission is the post-load value.
            val providers = awaitItem()
            assertEquals(listOf("Manual"), providers)
        }
    }

    @Test
    fun `availableProviders includes new providers after add`() = runTest {
        // Add a BCV rate
        val bcvRate = CurrencyRate(
            id = 20L,
            fromCurrency = "MLC",
            toCurrency = "USD",
            rate = BigDecimal("2.30"),
            updatedAt = Instant.now(),
            provider = "BCV",
        )
        // After init consumes the first flow, every subsequent invocation
        // returns the enriched list.
        every { listCurrencyRates() } returns flowOf(sampleRates + bcvRate)
        coEvery { addCurrencyRate(any<CurrencyRate>()) } returns 20L

        val vm = createVm()

        // After init + initial load, "BCV" and "Manual" are both present
        assertEquals(listOf("BCV", "Manual"), vm.availableProviders.value)

        // Adding a brand-new pair (CUP→MLC) with BCV should keep both providers
        vm.onAdd("CUP", "MLC", BigDecimal("2.30"), Instant.now(), "BCV")
        advanceUntilIdle()

        assertEquals(listOf("BCV", "Manual"), vm.availableProviders.value)
    }

    @Test
    fun `inverse rate formatted with correct precision`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            val cupUsd = ready.items.first { it.id == 1L }
            // sampleRates inverse id=2 has rate 0.0083 — small rates keep more precision
            assertEquals("0.0083", cupUsd.inverse!!.rateFormatted)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R9 — Provider-segregated cards (bug fix: same pair, different providers)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `same pair with different providers renders as two separate cards`() = runTest {
        // Add a BCV rate for the same CUP→USD pair as the existing Manual rate
        val ratesWithBcv = sampleRates + CurrencyRate(
            id = 10L,
            fromCurrency = "CUP",
            toCurrency = "USD",
            rate = BigDecimal("118.00"),
            updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
            provider = "BCV",
        ) + CurrencyRate(
            // Auto-created inverse of the BCV rate
            id = 11L,
            fromCurrency = "USD",
            toCurrency = "CUP",
            rate = BigDecimal("0.00847"),
            updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
            provider = "BCV",
        )
        every { listCurrencyRates() } returns flowOf(ratesWithBcv)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as RateListUiState.Ready
            // CUP↔USD Manual (1 card) + CUP↔USD BCV (1 card) + MLC→CUP Manual (1 card) = 3 cards
            assertEquals(3, ready.items.size)

            val manualCupUsd = ready.items.first { it.provider == "Manual" && it.pairLabel == "CUP → USD" }
            val bcvCupUsd = ready.items.first { it.provider == "BCV" && it.pairLabel == "CUP → USD" }

            // Manual card keeps its original rate
            assertEquals(BigDecimal("120.50"), manualCupUsd.rate)
            assertNotNull(manualCupUsd.inverse)
            assertEquals(2L, manualCupUsd.inverse!!.id)

            // BCV card has its own rate and inverse
            assertEquals(BigDecimal("118.00"), bcvCupUsd.rate)
            assertNotNull(bcvCupUsd.inverse)
            assertEquals(11L, bcvCupUsd.inverse!!.id)
            assertEquals("USD → CUP", bcvCupUsd.inverse.pairLabelFromCodes())
        }
    }

    @Test
    fun `deleting one provider card leaves the other untouched`() = runTest {
        // Setup: two providers for CUP↔USD
        val ratesWithBcv = sampleRates + CurrencyRate(
            id = 10L,
            fromCurrency = "CUP",
            toCurrency = "USD",
            rate = BigDecimal("118.00"),
            updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
            provider = "BCV",
        ) + CurrencyRate(
            id = 11L,
            fromCurrency = "USD",
            toCurrency = "CUP",
            rate = BigDecimal("0.00847"),
            updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
            provider = "BCV",
        )
        every { listCurrencyRates() } returns flowOf(ratesWithBcv)

        val vm = createVm()

        // Delete the Manual CUP→USD card (id=1, inverse id=2)
        vm.onDelete(1L)

        // Only Manual primary + inverse deleted, BCV untouched
        coVerify(exactly = 1) { deleteCurrencyRate(1L) }
        coVerify(exactly = 1) { deleteCurrencyRate(2L) }
        coVerify(exactly = 0) { deleteCurrencyRate(10L) }
        coVerify(exactly = 0) { deleteCurrencyRate(11L) }
    }
}

/** Helper to read the inverse pair label as "${from} → ${to}" for assertions. */
private fun InverseRateDisplay.pairLabelFromCodes(): String =
    "$fromCurrency → $toCurrency"
