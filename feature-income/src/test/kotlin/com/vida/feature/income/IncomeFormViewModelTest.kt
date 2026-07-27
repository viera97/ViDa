package com.vida.feature.income

import app.cash.turbine.test
import com.vida.domain.model.Card
import com.vida.domain.model.CardNumber
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import com.vida.domain.model.Income
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Stash
import com.vida.domain.model.Wallet
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.income.AddIncome
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.ListWallets
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
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class IncomeFormViewModelTest {

    private lateinit var addIncome: AddIncome
    private lateinit var listCards: ListCards
    private lateinit var listStashes: ListStashes
    private lateinit var listWallets: ListWallets

    private val testCardNumber: CardNumber =
        CardNumber.fromFirst6Last4("123456", "3456")

    private val sampleCards = listOf(
        Card(
            id = 1L,
            number = testCardNumber,
            bank = "Banco kubo",
            type = CardType.DEBIT,
            currency = "USD",
            expirationDate = LocalDate.of(2028, 12, 31),
        ),
    )

    private val sampleStashes = listOf(
        Stash(
            id = 1L,
            name = "Ahorro vacaciones",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            currency = Currency.MLC,
        ),
    )

    private val sampleWallets = listOf(
        Wallet(id = 1L, name = "Efectivo", currency = "CUP"),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        addIncome = mockk()
        listCards = mockk()
        listStashes = mockk()
        listWallets = mockk()

        // Default mocks: a CUP wallet, empty cards and stashes
        every { listWallets() } returns flowOf(sampleWallets)
        every { listCards() } returns flowOf(emptyList())
        every { listStashes() } returns flowOf(emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(): IncomeFormViewModel = IncomeFormViewModel(
        addIncome = addIncome,
        listCards = listCards,
        listStashes = listStashes,
        listWallets = listWallets,
    )

    // ── Initial load ─────────────────────────────────────────────────────────

    @Test
    fun `initial load emits Loading then Ready with wallet as first source`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as IncomeFormUiState.Ready
            assertEquals(1, ready.sources.size)
            assertEquals(SourceType.WALLET, ready.sources[0].type)
            assertEquals("Efectivo", ready.sources[0].label)
            assertEquals(Currency.CUP, ready.sources[0].currency)
        }
    }

    @Test
    fun `default currency is taken from first wallet`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as IncomeFormUiState.Ready
            assertEquals(Currency.CUP, ready.form.currency)
            assertEquals(SourceType.WALLET, ready.form.sourceType)
            assertEquals(1L, ready.form.sourceId)
            // hasSourceSelected defaults to false so the SourceSelector shows its placeholder
            assertTrue(!ready.form.hasSourceSelected)
        }
    }

    @Test
    fun `loads wallet cards and stashes as sources`() = runTest {
        every { listCards() } returns flowOf(sampleCards)
        every { listStashes() } returns flowOf(sampleStashes)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as IncomeFormUiState.Ready
            assertEquals(3, ready.sources.size)

            // Wallet is first
            assertEquals(SourceType.WALLET, ready.sources[0].type)
            assertEquals(1L, ready.sources[0].id)
            assertEquals("Efectivo", ready.sources[0].label)

            // Card is second
            assertEquals(SourceType.CARD, ready.sources[1].type)
            assertEquals(1L, ready.sources[1].id)
            assertEquals("Banco kubo", ready.sources[1].label)
            assertEquals("···3456", ready.sources[1].subtitle)

            // Stash is third
            assertEquals(SourceType.STASH, ready.sources[2].type)
            assertEquals(1L, ready.sources[2].id)
            assertEquals("Ahorro vacaciones", ready.sources[2].label)
        }
    }

    @Test
    fun `emits Error when listWallets throws`() = runTest {
        every { listWallets() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as IncomeFormUiState.Error
            assertTrue(error.message.isNotBlank())
        }
    }

    @Test
    fun `emits NoSources when no wallets cards or stashes exist`() = runTest {
        every { listWallets() } returns flowOf(emptyList())

        val vm = createVm()

        vm.uiState.test {
            assertEquals(IncomeFormUiState.NoSources, awaitItem())
        }
    }

    // ── Amount validation ────────────────────────────────────────────────────

    @Test
    fun `valid decimal amount clears amount error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("5000.00")

            val updated = awaitItem() as IncomeFormUiState.Ready
            assertEquals("5000.00", updated.form.amount)
            assertNull(updated.validationErrors["amount"])
        }
    }

    @Test
    fun `blank amount shows error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("")

            val updated = awaitItem() as IncomeFormUiState.Ready
            assertNotNull(updated.validationErrors["amount"])
        }
    }

    @Test
    fun `zero amount shows error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("0")

            val updated = awaitItem() as IncomeFormUiState.Ready
            assertNotNull(updated.validationErrors["amount"])
        }
    }

    // ── Description validation ───────────────────────────────────────────────

    @Test
    fun `valid description clears description error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDescriptionChanged("Salario")

            val updated = awaitItem() as IncomeFormUiState.Ready
            assertEquals("Salario", updated.form.description)
            assertNull(updated.validationErrors["description"])
        }
    }

    @Test
    fun `blank description shows error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDescriptionChanged("   ")

            val updated = awaitItem() as IncomeFormUiState.Ready
            assertNotNull(updated.validationErrors["description"])
        }
    }

    // ── Source selection ─────────────────────────────────────────────────────

    @Test
    fun `selecting card source does NOT auto-change currency`() = runTest {
        // Unlike expense, currency is NOT auto-changed on source selection —
        // the user must explicitly resolve any mismatch via the reactive
        // validation feedback (mirrors ExpenseFormViewModel pattern).
        every { listCards() } returns flowOf(sampleCards)

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onSourceSelected(SourceType.CARD, 1L)

            val updated = awaitItem() as IncomeFormUiState.Ready
            assertEquals(SourceType.CARD, updated.form.sourceType)
            assertEquals(1L, updated.form.sourceId)
            // Currency stays at the default (CUP) — mismatch is detected reactively
            assertEquals(Currency.CUP, updated.form.currency)
        }
    }

    @Test
    fun `mismatch error appears when currency differs from selected source currency`() = runTest {
        every { listCards() } returns flowOf(sampleCards)

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onSourceSelected(SourceType.CARD, 1L) // card is USD, form is CUP

            val updated = awaitItem() as IncomeFormUiState.Ready
            assertEquals(SourceType.CARD, updated.form.sourceType)

            // Reactive mismatch check
            val mismatch = vm.computeMismatchError()
            assertNotNull(mismatch)
            assertTrue(mismatch!!.contains("USD"))
        }
    }

    @Test
    fun `no mismatch when currency matches selected source`() = runTest {
        every { listCards() } returns flowOf(sampleCards)

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onSourceSelected(SourceType.CARD, 1L)
            awaitItem()
            vm.onCurrencyChanged(Currency.USD) // match the card

            val updated = awaitItem() as IncomeFormUiState.Ready

            assertNull(vm.computeMismatchError())
            assertEquals(Currency.USD, updated.form.currency)
        }
    }

    // ── Date / Note ──────────────────────────────────────────────────────────

    @Test
    fun `date defaults to now`() = runTest {
        val vm = createVm()
        val before = Instant.now()

        vm.uiState.test {
            val ready = awaitItem() as IncomeFormUiState.Ready

            val after = Instant.now()
            assertTrue(ready.form.dateTime >= before.minusSeconds(1))
            assertTrue(ready.form.dateTime <= after.plusSeconds(1))
        }
    }

    @Test
    fun `changing dateTime updates form`() = runTest {
        val vm = createVm()
        val future = Instant.now().plus(7, ChronoUnit.DAYS)

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDateTimeChanged(future)

            val updated = awaitItem() as IncomeFormUiState.Ready
            assertEquals(future, updated.form.dateTime)
        }
    }

    @Test
    fun `changing note updates form without validation errors`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onNoteChanged("aguinaldo")

            val updated = awaitItem() as IncomeFormUiState.Ready
            assertEquals("aguinaldo", updated.form.note)
            assertNull(updated.validationErrors["note"])
        }
    }

    // ── Submission ───────────────────────────────────────────────────────────

    @Test
    fun `submit with valid data calls AddIncome and emits Success`() = runTest {
        coEvery { addIncome(any()) } returns 5L
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as IncomeFormUiState.Ready

            vm.onAmountChanged("5000.00")
            awaitItem()
            vm.onDescriptionChanged("Salario")
            awaitItem()
            vm.onSourceSelected(SourceType.WALLET, 1L)
            awaitItem()

            vm.submit()

            assertEquals(IncomeFormUiState.Submitting, awaitItem())
            assertEquals(IncomeFormUiState.Success, awaitItem())

            coVerify {
                addIncome(
                    withArg { income ->
                        assertEquals(Money(BigDecimal("5000.00"), Currency.CUP), income.amount)
                        assertEquals("Salario", income.description)
                        assertEquals(SourceType.WALLET, income.sourceType)
                        assertEquals(1L, income.sourceId)
                    },
                )
            }
        }
    }

    @Test
    fun `submit without source selected adds source validation error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as IncomeFormUiState.Ready

            vm.onAmountChanged("5000.00")
            awaitItem()
            vm.onDescriptionChanged("Salario")
            awaitItem()

            vm.submit()

            // No Submitting transition — validation failed
            val updated = expectMostRecentItem()
            assertTrue(updated is IncomeFormUiState.Ready)
            assertNotNull((updated as IncomeFormUiState.Ready).validationErrors["source"])

            // AddIncome should NOT have been called
            coVerify(exactly = 0) { addIncome(any()) }
        }
    }

    @Test
    fun `submit with currency mismatch blocks submission`() = runTest {
        every { listCards() } returns flowOf(sampleCards)
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("5000.00")
            awaitItem()
            vm.onDescriptionChanged("Salario")
            awaitItem()
            vm.onSourceSelected(SourceType.CARD, 1L) // USD card, form is CUP
            awaitItem()

            vm.submit()

            val updated = expectMostRecentItem() as IncomeFormUiState.Ready
            assertNotNull(updated.validationErrors["amount"]) // mismatch error keyed under "amount"

            coVerify(exactly = 0) { addIncome(any()) }
        }
    }

    @Test
    fun `submit error transitions to Error state`() = runTest {
        coEvery { addIncome(any()) } throws RuntimeException("DB write failed")
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("5000.00")
            awaitItem()
            vm.onDescriptionChanged("Salario")
            awaitItem()
            vm.onSourceSelected(SourceType.WALLET, 1L)
            awaitItem()

            vm.submit()

            assertEquals(IncomeFormUiState.Submitting, awaitItem())
            val error = awaitItem() as IncomeFormUiState.Error
            assertTrue(error.message.isNotBlank())
        }
    }

    @Test
    fun `blank note is stored as null on submit`() = runTest {
        coEvery { addIncome(any()) } returns 5L
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("5000.00")
            awaitItem()
            vm.onDescriptionChanged("Salario")
            awaitItem()
            vm.onSourceSelected(SourceType.WALLET, 1L)
            awaitItem()

            vm.submit()

            assertEquals(IncomeFormUiState.Submitting, awaitItem())
            assertEquals(IncomeFormUiState.Success, awaitItem())

            coVerify {
                addIncome(
                    withArg { income ->
                        assertNull(income.note)
                    },
                )
            }
        }
    }
}
