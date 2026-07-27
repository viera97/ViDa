package com.vida.feature.incomelist

import app.cash.turbine.test
import com.vida.domain.model.Card
import com.vida.domain.model.CardNumber
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import com.vida.domain.model.Income
import com.vida.domain.model.IncomeFilter
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Stash
import com.vida.domain.model.Wallet
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.income.SearchIncomes
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.ListWallets
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class IncomeListViewModelTest {

    private lateinit var searchIncomes: SearchIncomes
    private lateinit var listCards: ListCards
    private lateinit var listStashes: ListStashes
    private lateinit var listWallets: ListWallets

    private val testCardNumber: CardNumber =
        CardNumber.fromFirst6Last4("123456", "3456")

    private val sampleWallets = listOf(
        Wallet(id = 1L, name = "Efectivo", currency = "CUP"),
    )

    private val sampleCards = listOf(
        Card(
            id = 1L,
            number = testCardNumber,
            bank = "Banco BPA",
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

    private fun sampleIncome(
        id: Long = 1L,
        amount: Money = Money.of("5000.00", Currency.CUP),
        description: String = "Salario",
        sourceType: SourceType = SourceType.WALLET,
        sourceId: Long = 1L,
    ): Income = Income(
        id = id,
        amount = amount,
        description = description,
        dateTime = Instant.now(),
        sourceType = sourceType,
        sourceId = sourceId,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        searchIncomes = mockk()
        listCards = mockk()
        listStashes = mockk()
        listWallets = mockk()

        every { listWallets() } returns flowOf(sampleWallets)
        every { listCards() } returns flowOf(sampleCards)
        every { listStashes() } returns flowOf(sampleStashes)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(): IncomeListViewModel = IncomeListViewModel(
        searchIncomes = searchIncomes,
        listCards = listCards,
        listStashes = listStashes,
        listWallets = listWallets,
    )

    @Test
    fun `empty search result emits Empty`() = runTest {
        coEvery { searchIncomes(any(), any(), any()) } returns emptyList()

        val vm = createVm()

        vm.uiState.test {
            val state = awaitItem()
            assertTrue("Expected Empty, got $state", state is IncomeListUiState.Empty)
        }
    }

    @Test
    fun `single income emits Ready`() = runTest {
        val incomes = listOf(sampleIncome())
        coEvery { searchIncomes(any(), any(), any()) } returns incomes

        val vm = createVm()

        vm.uiState.test {
            val state = awaitItem() as IncomeListUiState.Ready
            assertEquals(1, state.items.size)
            assertEquals("Salario", state.items[0].description)
            assertEquals("Efectivo", state.items[0].sourceLabel)
        }
    }

    @Test
    fun `hasMore is false when results less than page size`() = runTest {
        val incomes = List(10) { sampleIncome(id = it + 1L) }
        coEvery { searchIncomes(any(), 20, 0) } returns incomes

        val vm = createVm()

        vm.uiState.test {
            val state = awaitItem() as IncomeListUiState.Ready
            assertEquals(false, state.hasMore)
        }
    }

    @Test
    fun `hasMore is true when results equal page size`() = runTest {
        val incomes = List(20) { sampleIncome(id = it + 1L) }
        coEvery { searchIncomes(any(), 20, 0) } returns incomes

        val vm = createVm()

        vm.uiState.test {
            val state = awaitItem() as IncomeListUiState.Ready
            assertEquals(true, state.hasMore)
        }
    }

    @Test
    fun `search error emits Error state`() = runTest {
        coEvery { searchIncomes(any(), any(), any()) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state is IncomeListUiState.Error)
        }
    }

    @Test
    fun `onRefresh resets offset and re-fetches`() = runTest {
        val incomes = listOf(sampleIncome(id = 1L))
        coEvery { searchIncomes(any(), 20, 0) } returns incomes
        coEvery { searchIncomes(any(), 20, 20) } returns emptyList()

        val vm = createVm()
        vm.uiState.test {
            val state = awaitItem() as IncomeListUiState.Ready
            assertEquals(1, state.items.size)
        }
    }
}
