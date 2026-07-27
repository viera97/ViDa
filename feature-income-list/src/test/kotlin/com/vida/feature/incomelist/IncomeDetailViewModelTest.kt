package com.vida.feature.incomelist

import app.cash.turbine.test
import com.vida.domain.model.Currency
import com.vida.domain.model.Income
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Wallet
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.income.DeleteIncome
import com.vida.domain.usecase.income.GetIncome
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

class IncomeDetailViewModelTest {

    private lateinit var getIncome: GetIncome
    private lateinit var deleteIncome: DeleteIncome
    private lateinit var listCards: ListCards
    private lateinit var listStashes: ListStashes
    private lateinit var listWallets: ListWallets

    private fun sampleIncome() = Income(
        id = 1L,
        amount = Money.of("5000.00", Currency.CUP),
        description = "Salario",
        dateTime = Instant.now(),
        sourceType = SourceType.WALLET,
        sourceId = 1L,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        getIncome = mockk()
        deleteIncome = mockk()
        listCards = mockk()
        listStashes = mockk()
        listWallets = mockk()

        coEvery { getIncome(1L) } returns sampleIncome()
        every { listWallets() } returns flowOf(listOf(Wallet(id = 1L, name = "Efectivo", currency = "CUP")))
        every { listCards() } returns flowOf(emptyList())
        every { listStashes() } returns flowOf(emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load emits Ready with income`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val state = awaitItem() as IncomeDetailUiState.Ready
            assertEquals("Salario", state.income.description)
            assertEquals("Efectivo", state.income.sourceLabel)
        }
    }

    @Test
    fun `delete emits NavigateBack`() = runTest {
        coEvery { deleteIncome(1L) } returns Unit
        val vm = createVm()

        vm.navigationEvents.test {
            vm.onDelete()
            assertEquals(IncomeDetailNavigationEvent.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createVm(): IncomeDetailViewModel = IncomeDetailViewModel(
        savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("id" to "1")),
        getIncome = getIncome,
        deleteIncome = deleteIncome,
        listCards = listCards,
        listStashes = listStashes,
        listWallets = listWallets,
    )
}
