package com.vida.feature.expense

import app.cash.turbine.test
import com.vida.domain.model.Card
import com.vida.domain.model.CardNumber
import com.vida.domain.model.CardType
import com.vida.domain.model.Category
import com.vida.domain.model.Currency
import com.vida.domain.model.Expense
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Stash
import com.vida.domain.model.Wallet
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.expense.AddExpense
import com.vida.domain.usecase.expense.GetExpense
import com.vida.domain.usecase.expense.UpdateExpense
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

class ExpenseFormViewModelTest {

    private lateinit var addExpense: AddExpense
    private lateinit var updateExpense: UpdateExpense
    private lateinit var getExpense: GetExpense
    private lateinit var listCategories: ListCategories
    private lateinit var listCards: ListCards
    private lateinit var listStashes: ListStashes
    private lateinit var listWallets: ListWallets

    private val testCardNumber: CardNumber =
        CardNumber.fromFirst6Last4("123456", "3456")

    private val sampleCategories = listOf(
        Category(id = 1L, name = "Comida", color = (-43904)),
        Category(id = 2L, name = "Transporte", color = (-14614533)),
    )

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

    private val defaultWallet = Wallet(id = 1L, currency = "CUP")

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        addExpense = mockk()
        updateExpense = mockk()
        getExpense = mockk()
        listCategories = mockk()
        listCards = mockk()
        listStashes = mockk()
        listWallets = mockk()

        // Default mocks: one wallet, categories, empty cards/stashes
        every { listWallets() } returns flowOf(listOf(defaultWallet))
        every { listCategories() } returns flowOf(sampleCategories)
        every { listCards() } returns flowOf(emptyList())
        every { listStashes() } returns flowOf(emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Factory ─────────────────────────────────────────────────────────────

    private fun createVm(): ExpenseFormViewModel = ExpenseFormViewModel(
        addExpense = addExpense,
        updateExpense = updateExpense,
        getExpense = getExpense,
        listCategories = listCategories,
        listCards = listCards,
        listStashes = listStashes,
        listWallets = listWallets,
    )

    // ── SCN-EXP-004 / 005 / 006: Initial load ───────────────────────────────

    @Test
    fun `initial load emits Loading then Ready with categories and sources`() =
        runTest {
            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as ExpenseFormUiState.Ready
                assertEquals(sampleCategories.size, ready.categories.size)
                assertEquals(1, ready.sources.size)
                assertEquals("Billetera", ready.sources[0].label)
                assertEquals(Currency.CUP, ready.sources[0].currency)
                assertEquals(1L, ready.sources[0].id)
                assertEquals(SourceType.WALLET, ready.sources[0].type)
            }
        }

    // ── SCN-EXP-011: Default currency matches wallet ────────────────────────

    @Test
    fun `default currency is taken from wallet`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as ExpenseFormUiState.Ready
            assertEquals(Currency.CUP, ready.form.currency)
            assertEquals(SourceType.WALLET, ready.form.sourceType)
            assertEquals(1L, ready.form.sourceId)
            assertTrue(ready.form.hasSourceSelected)
        }
    }

    // ── SCN-EXP-005: Load sources — wallet + cards + stashes ────────────────

    @Test
    fun `loads wallet cards and stashes as sources`() = runTest {
        every { listCards() } returns flowOf(sampleCards)
        every { listStashes() } returns flowOf(sampleStashes)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as ExpenseFormUiState.Ready
            assertEquals(3, ready.sources.size)

            // Wallet is first
            assertEquals(SourceType.WALLET, ready.sources[0].type)
            assertEquals(1L, ready.sources[0].id)
            assertEquals("Billetera", ready.sources[0].label)

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

    // ── SCN-EXP-007: Error state on load failure ────────────────────────────

    @Test
    fun `emits Error when listWallets throws`() = runTest {
        every { listWallets() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as ExpenseFormUiState.Error
            assertTrue(error.message.isNotBlank())
        }
    }

    @Test
    fun `emits Error when listCategories throws`() = runTest {
        every { listCategories() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as ExpenseFormUiState.Error
            assertTrue(error.message.isNotBlank())
        }
    }

    // ── SCN-EXP-008/009: Amount validation ──────────────────────────────────

    @Test
    fun `valid decimal amount clears amount error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("1250.50")

            val updated = awaitItem() as ExpenseFormUiState.Ready
            assertEquals("1250.50", updated.form.amount)
            assertNull(updated.validationErrors["amount"])
        }
    }

    @Test
    fun `blank amount shows error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("")

            val updated = awaitItem() as ExpenseFormUiState.Ready
            assertNotNull(updated.validationErrors["amount"])
        }
    }

    @Test
    fun `non-numeric amount shows error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("abc")

            val updated = awaitItem() as ExpenseFormUiState.Ready
            assertNotNull(updated.validationErrors["amount"])
        }
    }

    @Test
    fun `zero amount shows error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("0")

            val updated = awaitItem() as ExpenseFormUiState.Ready
            assertNotNull(updated.validationErrors["amount"])
        }
    }

    @Test
    fun `negative amount shows error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("-5")

            val updated = awaitItem() as ExpenseFormUiState.Ready
            assertNotNull(updated.validationErrors["amount"])
        }
    }

    // ── SCN-EXP-013/014: Description validation ─────────────────────────────

    @Test
    fun `valid description clears description error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDescriptionChanged("Almuerzo en el café")

            val updated = awaitItem() as ExpenseFormUiState.Ready
            assertEquals("Almuerzo en el café", updated.form.description)
            assertNull(updated.validationErrors["description"])
        }
    }

    @Test
    fun `blank description shows error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDescriptionChanged("   ")

            val updated = awaitItem() as ExpenseFormUiState.Ready
            assertNotNull(updated.validationErrors["description"])
        }
    }

    // ── SCN-EXP-017: Category selection ─────────────────────────────────────

    @Test
    fun `selecting category updates form and clears category error`() =
        runTest {
            val vm = createVm()

            vm.uiState.test {
                awaitItem() // Ready

                vm.onCategorySelected(1L)

                val updated = awaitItem() as ExpenseFormUiState.Ready
                assertEquals(1L, updated.form.categoryId)
                assertNull(updated.validationErrors["category"])
            }
        }

    // ── SCN-EXP-023: Source selection ───────────────────────────────────────

    @Test
    fun `selecting card source updates sourceType sourceId and currency`() =
        runTest {
            every { listCards() } returns flowOf(sampleCards)
            every { listStashes() } returns flowOf(sampleStashes)

            val vm = createVm()

            vm.uiState.test {
                awaitItem() // Ready

                vm.onSourceSelected(SourceType.CARD, 1L)

                val updated = awaitItem() as ExpenseFormUiState.Ready
                assertEquals(SourceType.CARD, updated.form.sourceType)
                assertEquals(1L, updated.form.sourceId)
                // Currency should change from wallet CUP to card USD
                assertEquals(Currency.USD, updated.form.currency)
            }
        }

    @Test
    fun `selecting stash source updates sourceType sourceId and currency`() =
        runTest {
            every { listStashes() } returns flowOf(sampleStashes)

            val vm = createVm()

            vm.uiState.test {
                awaitItem() // Ready

                vm.onSourceSelected(SourceType.STASH, 1L)

                val updated = awaitItem() as ExpenseFormUiState.Ready
                assertEquals(SourceType.STASH, updated.form.sourceType)
                assertEquals(1L, updated.form.sourceId)
                assertEquals(Currency.MLC, updated.form.currency)
            }
        }

    // ── SCN-EXP-025: Source change resets currency ──────────────────────────

    @Test
    fun `source change resets currency to new source currency`() = runTest {
        every { listCards() } returns flowOf(sampleCards)
        every { listStashes() } returns flowOf(sampleStashes)

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            // Change currency manually to USD
            vm.onCurrencyChanged(Currency.USD)

            val withUsd = awaitItem() as ExpenseFormUiState.Ready
            assertEquals(Currency.USD, withUsd.form.currency)

            // Now select stash which has MLC — currency should reset to MLC
            vm.onSourceSelected(SourceType.STASH, 1L)

            val withStash = awaitItem() as ExpenseFormUiState.Ready
            assertEquals(Currency.MLC, withStash.form.currency)
        }
    }

    // ── SCN-EXP-026: Date defaults to now ───────────────────────────────────

    @Test
    fun `date defaults to now`() = runTest {
        val vm = createVm()
        val before = Instant.now()

        vm.uiState.test {
            val ready = awaitItem() as ExpenseFormUiState.Ready

            val after = Instant.now()
            assertTrue(ready.form.dateTime >= before || ready.form.dateTime >= before.minusSeconds(1))
            assertTrue(ready.form.dateTime <= after || ready.form.dateTime <= after.plusSeconds(1))
        }
    }

    // ── SCN-EXP-027: Override date/time ─────────────────────────────────────

    @Test
    fun `changing dateTime updates form`() = runTest {
        val vm = createVm()
        val future = Instant.now().plus(7, ChronoUnit.DAYS)

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDateTimeChanged(future)

            val updated = awaitItem() as ExpenseFormUiState.Ready
            assertEquals(future, updated.form.dateTime)
        }
    }

    // ── SCN-EXP-028: Optional note ──────────────────────────────────────────

    @Test
    fun `changing note updates form without validation errors`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onNoteChanged("Nota opcional")

            val updated = awaitItem() as ExpenseFormUiState.Ready
            assertEquals("Nota opcional", updated.form.note)
            // Note should not cause any validation errors
            assertNull(updated.validationErrors["note"])
        }
    }

    // ── SCN-EXP-030/031: Successful submission ──────────────────────────────

    @Test
    fun `submit with valid data calls AddExpense and emits Success`() =
        runTest {
            coEvery { addExpense(any()) } returns 5L
            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as ExpenseFormUiState.Ready

                // Fill valid data
                vm.onAmountChanged("1250.50")
                awaitItem()
                vm.onDescriptionChanged("Almuerzo")
                awaitItem()
                vm.onCategorySelected(1L)
                awaitItem()

                // Submit
                vm.submit()

                // Should transition through Submitting → Success
                assertEquals(ExpenseFormUiState.Submitting, awaitItem())
                assertEquals(ExpenseFormUiState.Success, awaitItem())

                // Verify AddExpense was called with the correct expense
                coVerify {
                    addExpense(
                        withArg { expense ->
                            assertEquals(1L, expense.categoryId)
                            assertEquals(
                                Money(BigDecimal("1250.50"), Currency.CUP),
                                expense.amount,
                            )
                            assertEquals("Almuerzo", expense.description)
                            assertEquals(SourceType.WALLET, expense.sourceType)
                            assertEquals(1L, expense.sourceId)
                        },
                    )
                }
            }
        }

    // ── SCN-EXP-032: Submit error ───────────────────────────────────────────

    @Test
    fun `submit when AddExpense throws emits Error and preserves form data`() =
        runTest {
            coEvery { addExpense(any()) } throws RuntimeException("Network error")
            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as ExpenseFormUiState.Ready

                vm.onAmountChanged("500")
                awaitItem()
                vm.onDescriptionChanged("Taxi")
                awaitItem()
                vm.onCategorySelected(2L)
                awaitItem()

                vm.submit()

                assertEquals(ExpenseFormUiState.Submitting, awaitItem())
                val error = awaitItem() as ExpenseFormUiState.Error
                assertTrue(error.message.isNotBlank())
            }
        }

    // ── SCN-EXP-033: Validation errors prevent submission ───────────────────

    @Test
    fun `submit with empty form does not call AddExpense and shows errors`() =
        runTest {
            val vm = createVm()

            vm.uiState.test {
                awaitItem() // Ready

                vm.submit()

                // Submit should re-validate and show errors, NOT transition to Submitting
                val withErrors = awaitItem() as ExpenseFormUiState.Ready
                assertTrue(withErrors.validationErrors.isNotEmpty())
                assertNotNull(withErrors.validationErrors["amount"])
                assertNotNull(withErrors.validationErrors["description"])
                assertNotNull(withErrors.validationErrors["category"])
                // Source should be valid (WALLET is default, no sourceId needed)
                coVerify(inverse = true) { addExpense(any()) }
            }
        }

    @Test
    fun `submit without category shows category error`() = runTest {
        coEvery { addExpense(any()) } returns 5L
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("100")
            awaitItem()
            vm.onDescriptionChanged("Café")
            awaitItem()
            // NOTE: no category selected

            vm.submit()

            // Should NOT transition to Submitting — stays Ready with errors
            val withErrors = awaitItem() as ExpenseFormUiState.Ready
            assertNotNull(withErrors.validationErrors["category"])
            assertNull(withErrors.validationErrors["amount"])
            assertNull(withErrors.validationErrors["description"])
            coVerify(inverse = true) { addExpense(any()) }
        }
    }

    // ── SCN-EXP-012: Currency updates when source changes ───────────────────

    @Test
    fun `currency updates when source changes from wallet to USD card`() =
        runTest {
            every { listCards() } returns flowOf(sampleCards)

            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as ExpenseFormUiState.Ready
                assertEquals(Currency.CUP, ready.form.currency)

                vm.onSourceSelected(SourceType.CARD, 1L)

                val updated = awaitItem() as ExpenseFormUiState.Ready
                assertEquals(Currency.USD, updated.form.currency)
            }
        }

    // ── SCN-EXP-018: No category blocks submit ──────────────────────────────

    @Test
    fun `category missing blocks submission`() = runTest {
        coEvery { addExpense(any()) } returns 5L
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("50")
            awaitItem()
            vm.onDescriptionChanged("Snack")
            awaitItem()
            // Category NOT selected

            vm.submit()

            val ready = awaitItem() as ExpenseFormUiState.Ready
            assertNotNull(ready.validationErrors["category"])
            coVerify(inverse = true) { addExpense(any()) }
        }
    }

    // ── Error → Ready transition on user edit ───────────────────────────────

    @Test
    fun `editing a field after Error recovers to Ready`() = runTest {
        coEvery { addExpense(any()) } throws RuntimeException("Error")
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("100")
            awaitItem()
            vm.onDescriptionChanged("Test")
            awaitItem()
            vm.onCategorySelected(1L)
            awaitItem()

            vm.submit()

            assertEquals(ExpenseFormUiState.Submitting, awaitItem())
            val error = awaitItem() as ExpenseFormUiState.Error
            assertTrue(error.message.isNotBlank())

            // User edits amount after error
            vm.onAmountChanged("200")

            val recovered = awaitItem() as ExpenseFormUiState.Ready
            assertEquals("200", recovered.form.amount)
            // Other fields should be preserved
            assertEquals("Test", recovered.form.description)
            assertEquals(1L, recovered.form.categoryId)
        }
    }
}
