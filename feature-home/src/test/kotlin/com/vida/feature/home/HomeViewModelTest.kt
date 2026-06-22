package com.vida.feature.home

import app.cash.turbine.test
import com.vida.domain.model.Card
import com.vida.domain.model.CardNumber
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import com.vida.domain.model.CurrencyRate
import com.vida.domain.model.Expense
import com.vida.domain.model.Money
import com.vida.domain.model.Stash
import com.vida.domain.repository.CategoryRepository
import com.vida.domain.usecase.balance.GetTotalBalance
import com.vida.domain.usecase.card.GetCardBalance
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.expense.ListExpenses
import com.vida.domain.usecase.rate.GetCurrentRate
import com.vida.domain.usecase.stash.GetStashBalance
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.GetWalletBalance
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class HomeViewModelTest {

    private val testScope = TestScope()

    private lateinit var getTotalBalance: GetTotalBalance
    private lateinit var listExpenses: ListExpenses
    private lateinit var listCards: ListCards
    private lateinit var listStashes: ListStashes
    private lateinit var getWalletBalance: GetWalletBalance
    private lateinit var getCardBalance: GetCardBalance
    private lateinit var getStashBalance: GetStashBalance
    private lateinit var getCurrentRate: GetCurrentRate
    private lateinit var categoryRepository: CategoryRepository

    private val now: Instant = Instant.parse("2026-06-21T12:00:00Z")
    private val testCardNumber: CardNumber = CardNumber.fromFirst6Last4("123456", "3456")

    private val usdRate: CurrencyRate
        get() = CurrencyRate(
            fromCurrency = Currency.USD,
            toCurrency = Currency.CUP,
            rate = BigDecimal("150.00"),
            updatedAt = now,
        )

    private val mlcRate: CurrencyRate
        get() = CurrencyRate(
            fromCurrency = Currency.MLC,
            toCurrency = Currency.CUP,
            rate = BigDecimal("1.00"),
            updatedAt = now,
        )

    private fun createVm(
        expensesFlow: MutableSharedFlow<List<Expense>>,
        cardsFlow: MutableSharedFlow<List<Card>>,
        stashesFlow: MutableSharedFlow<List<Stash>>,
    ): HomeViewModel {
        every { listExpenses() } returns expensesFlow
        every { listCards() } returns cardsFlow
        every { listStashes() } returns stashesFlow

        return HomeViewModel(
            getTotalBalance = getTotalBalance,
            listExpenses = listExpenses,
            listCards = listCards,
            listStashes = listStashes,
            getWalletBalance = getWalletBalance,
            getCardBalance = getCardBalance,
            getStashBalance = getStashBalance,
            getCurrentRate = getCurrentRate,
            categoryRepository = categoryRepository,
        )
    }

    @Before
    fun setup() {
        getTotalBalance = mockk(relaxed = true)
        listExpenses = mockk(relaxed = true)
        listCards = mockk(relaxed = true)
        listStashes = mockk(relaxed = true)
        getWalletBalance = mockk(relaxed = true)
        getCardBalance = mockk(relaxed = true)
        getStashBalance = mockk(relaxed = true)
        getCurrentRate = mockk(relaxed = true)
        categoryRepository = mockk {
            every { getAll() } returns flowOf(emptyList())
        }
    }

    // SCN-HOME-001
    @Test
    fun `SCN-HOME-001 — Ready on non-zero data`() = testScope.runTest {
        val expense = Expense(1L, 1L, Money(BigDecimal("50.00"), Currency.CUP), null, "Café",
            now.minus(2, ChronoUnit.DAYS), com.vida.domain.model.SourceType.WALLET, null)
        val card = Card(1L, testCardNumber, "Banco kubo", CardType.DEBIT, Currency.USD, null, LocalDate.now().plusYears(1))
        val stash = Stash(1L, "Mi stash", now, now, Currency.CUP)

        coEvery { getTotalBalance() } returns Money(BigDecimal("5000.00"), Currency.CUP)
        coEvery { getWalletBalance(asOf = any()) } returns Money(BigDecimal("2000.00"), Currency.CUP)
        coEvery { getCardBalance(card.id, any()) } returns Money(BigDecimal("100.00"), Currency.USD)
        coEvery { getStashBalance(stash.id, any()) } returns Money(BigDecimal("500.00"), Currency.CUP)
        coEvery { getCurrentRate(Currency.USD, Currency.CUP, any()) } returns usdRate
        coEvery { getCurrentRate(Currency.MLC, Currency.CUP, any()) } returns mlcRate

        val expensesF = MutableSharedFlow<List<Expense>>(replay = 1)
        val cardsF = MutableSharedFlow<List<Card>>(replay = 1)
        val stashesF = MutableSharedFlow<List<Stash>>(replay = 1)

        expensesF.emit(listOf(expense))
        cardsF.emit(listOf(card))
        stashesF.emit(listOf(stash))

        val vm = createVm(expensesF, cardsF, stashesF)

        vm.uiState.test {
            skipItems(1) // Loading
            val state = awaitItem()
            assert(state is HomeUiState.Ready) { "expected Ready, got $state" }
            val ready = state as HomeUiState.Ready
            assert(ready.perSource.size == 3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // SCN-HOME-002
    @Test
    fun `SCN-HOME-002 — Empty on all-zero`() = testScope.runTest {
        coEvery { getTotalBalance() } returns Money.ZERO_CUP
        coEvery { getWalletBalance(asOf = any()) } returns Money.ZERO_CUP
        coEvery { getCurrentRate(any(), any(), any()) } throws NoSuchElementException("No rate")

        val expensesF = MutableSharedFlow<List<Expense>>(replay = 1)
        val cardsF = MutableSharedFlow<List<Card>>(replay = 1)
        val stashesF = MutableSharedFlow<List<Stash>>(replay = 1)

        expensesF.emit(emptyList())
        cardsF.emit(emptyList())
        stashesF.emit(emptyList())

        val vm = createVm(expensesF, cardsF, stashesF)

        vm.uiState.test {
            // May receive Loading + Empty; consume until Empty
            var state: HomeUiState = awaitItem()
            while (state is HomeUiState.Loading) {
                state = awaitItem()
            }
            assert(state is HomeUiState.Empty) { "expected Empty, got $state" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // SCN-HOME-003
    @Test
    fun `SCN-HOME-003 — Error when GetTotalBalance throws`() = testScope.runTest {
        coEvery { getTotalBalance() } throws RuntimeException("DB unavailable")
        coEvery { getWalletBalance(asOf = any()) } returns Money.ZERO_CUP

        val expensesF = MutableSharedFlow<List<Expense>>(replay = 1)
        val cardsF = MutableSharedFlow<List<Card>>(replay = 1)
        val stashesF = MutableSharedFlow<List<Stash>>(replay = 1)

        expensesF.emit(emptyList())
        cardsF.emit(emptyList())
        stashesF.emit(emptyList())

        val vm = createVm(expensesF, cardsF, stashesF)

        vm.uiState.test {
            var state: HomeUiState = awaitItem()
            while (state is HomeUiState.Loading) {
                state = awaitItem()
            }
            assert(state is HomeUiState.Error) { "expected Error, got $state" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // SCN-HOME-004 — Reactive re-derive on trigger emit
    @Test
    fun `SCN-HOME-004 — re-derives state when trigger flow emits again`() = testScope.runTest {
        coEvery { getTotalBalance() } returns Money(BigDecimal("1000.00"), Currency.CUP)
        coEvery { getWalletBalance(asOf = any()) } returns Money(BigDecimal("1000.00"), Currency.CUP)

        val expensesF = MutableSharedFlow<List<Expense>>(replay = 1)
        val cardsF = MutableSharedFlow<List<Card>>(replay = 1)
        val stashesF = MutableSharedFlow<List<Stash>>(replay = 1)

        expensesF.emit(emptyList())
        cardsF.emit(emptyList())
        stashesF.emit(emptyList())

        val vm = createVm(expensesF, cardsF, stashesF)

        vm.uiState.test {
            skipItems(1) // skip Loading
            val ready1 = awaitItem() as HomeUiState.Ready
            assert(ready1.totalBalance.amount.toPlainString() == "1000.00")

            // New emission triggers re-derive
            coEvery { getTotalBalance() } returns Money(BigDecimal("2000.00"), Currency.CUP)
            coEvery { getWalletBalance(asOf = any()) } returns Money(BigDecimal("2000.00"), Currency.CUP)
            expensesF.emit(emptyList())

            val ready2 = awaitItem() as HomeUiState.Ready
            assert(ready2.totalBalance.amount.toPlainString() == "2000.00")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // SCN-HOME-014
    @Test
    fun `SCN-HOME-014 — perSource has only wallet when no cards or stashes`() = testScope.runTest {
        coEvery { getTotalBalance() } returns Money(BigDecimal("500.00"), Currency.CUP)
        coEvery { getWalletBalance(asOf = any()) } returns Money(BigDecimal("500.00"), Currency.CUP)

        val expensesF = MutableSharedFlow<List<Expense>>(replay = 1)
        val cardsF = MutableSharedFlow<List<Card>>(replay = 1)
        val stashesF = MutableSharedFlow<List<Stash>>(replay = 1)

        expensesF.emit(emptyList())
        cardsF.emit(emptyList())
        stashesF.emit(emptyList())

        val vm = createVm(expensesF, cardsF, stashesF)

        vm.uiState.test {
            skipItems(1)
            val ready = awaitItem() as HomeUiState.Ready
            assert(ready.perSource.size == 1)
            assert(ready.perSource[0].label == "Billetera")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // SCN-HOME-015
    @Test
    fun `SCN-HOME-015 — per-source balance throw causes Error`() = testScope.runTest {
        val card = Card(1L, testCardNumber, "Kubo", CardType.DEBIT, Currency.USD, null, LocalDate.now().plusYears(1))

        coEvery { getTotalBalance() } returns Money(BigDecimal("600.00"), Currency.CUP)
        coEvery { getWalletBalance(asOf = any()) } returns Money(BigDecimal("500.00"), Currency.CUP)
        coEvery { getCardBalance(card.id, any()) } throws RuntimeException("Card DB error")

        val expensesF = MutableSharedFlow<List<Expense>>(replay = 1)
        val cardsF = MutableSharedFlow<List<Card>>(replay = 1)
        val stashesF = MutableSharedFlow<List<Stash>>(replay = 1)

        expensesF.emit(emptyList())
        cardsF.emit(listOf(card))
        stashesF.emit(emptyList())

        val vm = createVm(expensesF, cardsF, stashesF)

        vm.uiState.test {
            var state: HomeUiState = awaitItem()
            while (state is HomeUiState.Loading) {
                state = awaitItem()
            }
            assert(state is HomeUiState.Error) { "expected Error, got $state" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // SCN-HOME-024 — Rates throw → section hidden, not Error
    @Test
    fun `SCN-HOME-024 — rates throw hides rate section, no Error`() = testScope.runTest {
        coEvery { getTotalBalance() } returns Money(BigDecimal("500.00"), Currency.CUP)
        coEvery { getWalletBalance(asOf = any()) } returns Money(BigDecimal("500.00"), Currency.CUP)
        coEvery { getCurrentRate(any(), any(), any()) } throws NoSuchElementException("No rate")

        val expensesF = MutableSharedFlow<List<Expense>>(replay = 1)
        val cardsF = MutableSharedFlow<List<Card>>(replay = 1)
        val stashesF = MutableSharedFlow<List<Stash>>(replay = 1)

        expensesF.emit(emptyList())
        cardsF.emit(emptyList())
        stashesF.emit(emptyList())

        val vm = createVm(expensesF, cardsF, stashesF)

        vm.uiState.test {
            var state: HomeUiState = awaitItem()
            while (state is HomeUiState.Loading) {
                state = awaitItem()
            }
            assert(state is HomeUiState.Ready) { "expected Ready, got $state" }
            assert((state as HomeUiState.Ready).rates == null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // SCN-HOME-009
    @Test
    fun `SCN-HOME-009 — multi-currency sources include all non-zero currency subtotals`() = testScope.runTest {
        val stash = Stash(1L, "USD Stash", now, now, Currency.USD)

        coEvery { getTotalBalance() } returns Money(BigDecimal("2000.00"), Currency.CUP)
        coEvery { getWalletBalance(asOf = any()) } returns Money(BigDecimal("2000.00"), Currency.CUP)
        coEvery { getStashBalance(stash.id, any()) } returns Money(BigDecimal("100.00"), Currency.USD)

        val expensesF = MutableSharedFlow<List<Expense>>(replay = 1)
        val cardsF = MutableSharedFlow<List<Card>>(replay = 1)
        val stashesF = MutableSharedFlow<List<Stash>>(replay = 1)

        expensesF.emit(emptyList())
        cardsF.emit(emptyList())
        stashesF.emit(listOf(stash))

        val vm = createVm(expensesF, cardsF, stashesF)

        vm.uiState.test {
            skipItems(1)
            val ready = awaitItem() as HomeUiState.Ready
            assert(Currency.CUP in ready.perCurrencySubtotals)
            assert(Currency.USD in ready.perCurrencySubtotals)
            assert(Currency.MLC !in ready.perCurrencySubtotals)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // SCN-HOME-006
    @Test
    fun `SCN-HOME-006 — zero total with no expenses or rates emits Empty`() = testScope.runTest {
        coEvery { getTotalBalance() } returns Money.ZERO_CUP
        coEvery { getWalletBalance(asOf = any()) } returns Money.ZERO_CUP
        coEvery { getCurrentRate(any(), any(), any()) } throws NoSuchElementException("No rate")

        val expensesF = MutableSharedFlow<List<Expense>>(replay = 1)
        val cardsF = MutableSharedFlow<List<Card>>(replay = 1)
        val stashesF = MutableSharedFlow<List<Stash>>(replay = 1)

        expensesF.emit(emptyList())
        cardsF.emit(emptyList())
        stashesF.emit(emptyList())

        val vm = createVm(expensesF, cardsF, stashesF)

        vm.uiState.test {
            var state: HomeUiState = awaitItem()
            while (state is HomeUiState.Loading) {
                state = awaitItem()
            }
            assert(state is HomeUiState.Empty) { "expected Empty, got $state" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // SCN-HOME-028
    @Test
    fun `SCN-HOME-028 — partial data emits Ready not Empty`() = testScope.runTest {
        coEvery { getTotalBalance() } returns Money(BigDecimal("500.00"), Currency.CUP)
        coEvery { getWalletBalance(asOf = any()) } returns Money(BigDecimal("500.00"), Currency.CUP)

        val expensesF = MutableSharedFlow<List<Expense>>(replay = 1)
        val cardsF = MutableSharedFlow<List<Card>>(replay = 1)
        val stashesF = MutableSharedFlow<List<Stash>>(replay = 1)

        expensesF.emit(emptyList())
        cardsF.emit(emptyList())
        stashesF.emit(emptyList())

        val vm = createVm(expensesF, cardsF, stashesF)

        vm.uiState.test {
            skipItems(1)
            val state = awaitItem()
            assert(state is HomeUiState.Ready) { "expected Ready, got $state" }
            cancelAndIgnoreRemainingEvents()
        }
    }
}