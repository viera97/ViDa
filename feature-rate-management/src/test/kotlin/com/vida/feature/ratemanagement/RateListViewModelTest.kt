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

    // sampleRates uses pair groups: CUP↔USD (ids 1,2) and MLC↔CUP (id 3).
    // Inverse pairing is collapsed into a single display item.
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
            fromCurrency = Currency.USD,
            toCurrency = Currency.CUP,
            rate = BigDecimal("0.0083"),
            updatedAt = Instant.parse("2025-02-01T10:00:00Z"),
            provider = "Manual",
        ),
        CurrencyRate(
            id = 3L,
            fromCurrency = Currency.CUP,
            toCurrency = Currency.USD,
            rate = BigDecimal("119.00"),
            updatedAt = Instant.parse("2025-01-15T08:00:00Z"),
            provider = "Manual",
        ),
        CurrencyRate(
            id = 4L,
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
            assertEquals(Currency.CUP, cupUsd.fromCurrency)
            assertEquals(Currency.USD, cupUsd.toCurrency)
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
            from = Currency.MLC,
            to = Currency.USD,
            rate = BigDecimal("2.50"),
            updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
            provider = "Manual",
        )

        // Two inserts: primary (MLC→USD @ 2.50) and inverse (USD→MLC @ 0.4)
        coVerify(exactly = 1) { addCurrencyRate(match { it.fromCurrency == Currency.MLC && it.toCurrency == Currency.USD && it.rate.compareTo(BigDecimal("2.50")) == 0 }) }
        coVerify(exactly = 1) { addCurrencyRate(match { it.fromCurrency == Currency.USD && it.toCurrency == Currency.MLC && it.rate.compareTo(BigDecimal("0.4")) == 0 }) }
    }

    @Test
    fun `onAdd success emits SaveSuccess and ShowToast agregada`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd(
                from = Currency.MLC,
                to = Currency.USD,
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
            from = Currency.CUP,
            to = Currency.CUP,
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

    // ── Duplicate detection ──

    @Test
    fun `onAdd with duplicate rate (same from, to, provider) does NOT call AddCurrencyRate`() =
        runTest {
            val vm = createVm()

            // sampleRates[0] = CUP→USD, Manual
            vm.onAdd(
                from = Currency.CUP,
                to = Currency.USD,
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
                from = Currency.CUP,
                to = Currency.USD,
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
            from = Currency.CUP,
            to = Currency.USD,
            rate = BigDecimal("121.00"),
            updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
            provider = "BCV",
        )

        coVerify(exactly = 1) { addCurrencyRate(match { it.fromCurrency == Currency.CUP }) }
        coVerify(exactly = 1) { addCurrencyRate(match { it.fromCurrency == Currency.USD }) }
    }

    // ── Add error ──

    @Test
    fun `onAdd error preserves existing rate list`() = runTest {
        coEvery { addCurrencyRate(any()) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAdd(
                from = Currency.MLC,
                to = Currency.USD,
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
            from = Currency.CUP,
            to = Currency.USD,
            rate = BigDecimal("125.00"),
            updatedAt = Instant.parse("2025-02-02T10:00:00Z"),
            provider = "Manual",
        )

        // Primary update
        coVerify(exactly = 1) { updateCurrencyRate(match { it.id == 1L && it.rate.compareTo(BigDecimal("125.00")) == 0 }) }
        // Inverse update (id=2, USD→CUP, new rate = 1/125 = 0.008)
        coVerify(exactly = 1) { updateCurrencyRate(match { it.id == 2L && it.fromCurrency == Currency.USD && it.toCurrency == Currency.CUP && it.rate.compareTo(BigDecimal("0.008")) == 0 }) }
    }

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

    @Test
    fun `onEdit error preserves list`() = runTest {
        coEvery { updateCurrencyRate(any()) } throws RuntimeException("Update failed")

        val vm = createVm()

        vm.uiState.test {
            awaitItem()
            vm.onEdit(
                id = 1L,
                from = Currency.CUP,
                to = Currency.USD,
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
            from = Currency.MLC,
            to = Currency.USD,
            rate = BigDecimal("2.50"),
            updatedAt = Instant.now(),
            provider = "Manual",
        )

        // 1 / 2.50 = 0.4
        coVerify(exactly = 1) {
            addCurrencyRate(
                match {
                    it.fromCurrency == Currency.USD &&
                        it.toCurrency == Currency.MLC &&
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
    fun `getRateForConversion returns latest matching rate`() {
        val vm = createVm()

        val rate = vm.getRateForConversion(Currency.CUP, Currency.USD)
        assertNotNull(rate)
        // id=1 is the latest CUP→USD (2025-02-01 > 2025-01-15)
        assertEquals(1L, rate!!.id)
        assertEquals(BigDecimal("120.50"), rate.rate)
    }

    @Test
    fun `getRateForConversion returns null for missing pair`() {
        val vm = createVm()

        val rate = vm.getRateForConversion(Currency.USD, Currency.MLC)
        assertNull(rate)
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
}

/** Helper to read the inverse pair label as "${from} → ${to}" for assertions. */
private fun InverseRateDisplay.pairLabelFromCodes(): String =
    "${fromCurrency.code} → ${toCurrency.code}"
