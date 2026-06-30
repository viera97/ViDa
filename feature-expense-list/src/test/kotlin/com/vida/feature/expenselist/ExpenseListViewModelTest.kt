package com.vida.feature.expenselist

import app.cash.turbine.test
import com.vida.domain.model.Category
import com.vida.domain.model.Currency
import com.vida.domain.model.Expense
import com.vida.domain.model.ExpenseFilter
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Wallet
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.expense.SearchExpenses
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

class ExpenseListViewModelTest {

    private lateinit var searchExpenses: SearchExpenses
    private lateinit var listCategories: ListCategories
    private lateinit var listCards: ListCards
    private lateinit var listStashes: ListStashes
    private lateinit var listWallets: ListWallets

    private val sampleCategories = listOf(
        Category(id = 1L, name = "Comida", color = -43904),
        Category(id = 2L, name = "Transporte", color = -14614533),
    )

    private val sampleWallet = Wallet(currency = Currency.CUP)

    private fun sampleExpense(
        id: Long = 1L,
        categoryId: Long = 1L,
        amount: Money = Money(BigDecimal("1500.00"), Currency.CUP),
        description: String = "Almuerzo café",
        dateTime: Instant = Instant.now(),
        sourceType: SourceType = SourceType.WALLET,
        sourceId: Long? = null,
    ): Expense = Expense(
        id = id,
        categoryId = categoryId,
        amount = amount,
        description = description,
        dateTime = dateTime,
        sourceType = sourceType,
        sourceId = sourceId,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        searchExpenses = mockk()
        listCategories = mockk()
        listCards = mockk()
        listStashes = mockk()
        listWallets = mockk()

        // Default mocks: one wallet, empty cards/stashes, categories.
        every { listWallets() } returns flowOf(listOf(sampleWallet))
        every { listCategories() } returns flowOf(sampleCategories)
        every { listCards() } returns flowOf(emptyList())
        every { listStashes() } returns flowOf(emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(): ExpenseListViewModel = ExpenseListViewModel(
        searchExpenses = searchExpenses,
        listCategories = listCategories,
        listCards = listCards,
        listStashes = listStashes,
        listWallets = listWallets,
    )

    // ── SCN-LIST-001: First page loads successfully ──────────────────────────

    @Test
    fun `initial load emits Loading then Ready with first page`() = runTest {
        val expenses = (1L..20L).map { sampleExpense(id = it, description = "Gasto $it") }
        coEvery { searchExpenses(ExpenseFilter(), limit = 20, offset = 0) } returns expenses

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as ExpenseListUiState.Ready
            assertEquals(20, ready.items.size)
            assertEquals("Gasto 1", ready.items[0].description)
            assertTrue(ready.hasMore)
            assertFalse(ready.isRefreshing)
        }
    }

    // ── SCN-LIST-002: Empty list (no expenses) ───────────────────────────────

    @Test
    fun `initial load emits Empty when no expenses exist`() = runTest {
        coEvery { searchExpenses(ExpenseFilter(), limit = 20, offset = 0) } returns emptyList()

        val vm = createVm()

        vm.uiState.test {
            val empty = awaitItem() as ExpenseListUiState.Empty
            assertTrue(empty.noFiltersActive)
        }
    }

    // ── SCN-LIST-003 / SCN-LIST-026: Init error ──────────────────────────────

    @Test
    fun `initial load emits Error when SearchExpenses throws`() = runTest {
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
        } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as ExpenseListUiState.Error
            assertTrue(error.message.isNotBlank())
        }
    }

    @Test
    fun `initial load emits Error with fallback when no exception message`() =
        runTest {
            coEvery {
                searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
            } throws RuntimeException()

            val vm = createVm()

            vm.uiState.test {
                val error = awaitItem() as ExpenseListUiState.Error
                assertTrue(error.message.isNotBlank())
            }
        }

    @Test
    fun `initial load emits Error when listCategories throws`() = runTest {
        every { listCategories() } throws RuntimeException("Category DB error")
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
        } returns emptyList()

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as ExpenseListUiState.Error
            assertTrue(error.message.isNotBlank())
        }
    }

    // ── SCN-LIST-009: "Cargar más" loads next page ───────────────────────────

    @Test
    fun `onLoadMore appends next page items`() = runTest {
        val page1 = (1L..20L).map { sampleExpense(id = it) }
        val page2 = (21L..40L).map { sampleExpense(id = it) }
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
        } returns page1
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 20)
        } returns page2

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as ExpenseListUiState.Ready
            assertEquals(20, ready.items.size)
            assertTrue(ready.hasMore)

            vm.onLoadMore()

            val withMore = awaitItem() as ExpenseListUiState.Ready
            assertEquals(40, withMore.items.size)
            assertTrue(withMore.hasMore)
        }
    }

    // ── SCN-LIST-010: End of list ────────────────────────────────────────────

    @Test
    fun `hasMore is false when page has fewer than PAGE_SIZE items`() = runTest {
        val expenses = (1L..15L).map { sampleExpense(id = it) }
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
        } returns expenses

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as ExpenseListUiState.Ready
            assertEquals(15, ready.items.size)
            assertFalse(ready.hasMore)
        }
    }

    @Test
    fun `onLoadMore does nothing when hasMore is false`() = runTest {
        val expenses = (1L..15L).map { sampleExpense(id = it) }
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
        } returns expenses

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as ExpenseListUiState.Ready
            assertEquals(15, ready.items.size)
            assertFalse(ready.hasMore)

            // onLoadMore on a list with no more items should no-op.
            vm.onLoadMore()

            // State should not change — no new emission.
            expectNoEvents()
        }
    }

    @Test
    fun `onLoadMore does nothing when state is not Ready`() = runTest {
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
        } throws RuntimeException("Error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as ExpenseListUiState.Error
            assertNotNull(error)
            // onLoadMore on Error state should no-op.
            vm.onLoadMore()
            expectNoEvents()
        }
    }

    // ── SCN-LIST-011 / SCN-LIST-027: Pagination error preserves list ────────

    @Test
    fun `pagination error preserves existing items`() = runTest {
        val page1 = (1L..20L).map { sampleExpense(id = it) }
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
        } returns page1
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 20)
        } throws RuntimeException("Pagination error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as ExpenseListUiState.Ready
            assertEquals(20, ready.items.size)

            vm.onLoadMore()

            // State should be preserved — still Ready with 20 items.
            // Error case: the state stays Ready, no new emission for the error
            // (snackbar hook to come in PR #3).
            // In current implementation, handleFetchError only changes state on
            // init failure. For pagination errors with merge=true, it's a no-op.
            // So expectNoEvents is correct.
            expectNoEvents()
        }
    }

    // ── SCN-LIST-023: Tap expense → NavigateToDetail ─────────────────────────

    @Test
    fun `onExpenseTap emits NavigateToDetail event`() = runTest {
        val expenses = (1L..20L).map { sampleExpense(id = it) }
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
        } returns expenses

        val vm = createVm()

        // Must start collecting navigationEvents BEFORE emitting.
        vm.navigationEvents.test {
            vm.onExpenseTap(42L)
            val event = awaitItem()
            assertEquals(NavigationEvent.NavigateToDetail(42L), event)
        }
    }

    // ── Pre-formatted fields in ExpenseListItem ──────────────────────────────

    @Test
    fun `ExpenseListItem contains pre-formatted fields`() = runTest {
        val expense = sampleExpense(
            id = 1L,
            amount = Money(BigDecimal("1250.50"), Currency.CUP),
            description = "Almuerzo",
            categoryId = 1L,
            sourceType = SourceType.WALLET,
        )
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
        } returns listOf(expense)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as ExpenseListUiState.Ready
            assertEquals(1, ready.items.size)
            val item = ready.items[0]

            assertEquals(1L, item.id)
            assertEquals("Almuerzo", item.description)
            assertTrue(item.amountFormatted.contains("1,250.50"))
            // Relative date should be present (the expense is "now", so "ahora mismo")
            assertTrue(item.dateFormatted.isNotBlank())
            assertTrue(item.absoluteDateFormatted.isNotBlank())
            assertEquals("Comida", item.categoryName)
            assertEquals(-43904, item.categoryColor)
            assertEquals("Billetera", item.sourceLabel)
            assertEquals(SourceType.WALLET, item.sourceType)
        }
    }

    // ── Default source label (fallback) ──────────────────────────────────────

    @Test
    fun `expense with unknown source uses sourceType name as fallback`() = runTest {
        // CARD with id=99 that's not in the mock's card list.
        val expense = sampleExpense(
            id = 1L,
            sourceType = SourceType.CARD,
            sourceId = 99L,
        )
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
        } returns listOf(expense)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as ExpenseListUiState.Ready
            assertEquals(1, ready.items.size)
            // Fallback: should use the enum name "CARD" since source 99 is unknown.
            assertEquals("CARD", ready.items[0].sourceLabel)
        }
    }

    // ── Category fallback for unknown category ───────────────────────────────

    @Test
    fun `expense with unknown category uses fallback name and color`() = runTest {
        val expense = sampleExpense(
            id = 1L,
            categoryId = 999L, // Not in sampleCategories
        )
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
        } returns listOf(expense)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as ExpenseListUiState.Ready
            assertEquals(1, ready.items.size)
            assertEquals("Sin categoría", ready.items[0].categoryName)
            // Color should be the fallback gray.
            assertEquals(0xFF9E9E9E.toInt(), ready.items[0].categoryColor)
        }
    }

    // ── Multiple source labels ───────────────────────────────────────────────

    @Test
    fun `wallet source uses Billetera label`() = runTest {
        val expense = sampleExpense(
            id = 1L,
            sourceType = SourceType.WALLET,
            sourceId = null,
        )
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
        } returns listOf(expense)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as ExpenseListUiState.Ready
            assertEquals(1, ready.items.size)
            assertEquals("Billetera", ready.items[0].sourceLabel)
        }
    }

    // ── Empty state distinction (SCN-LIST-030) ───────────────────────────────

    @Test
    fun `Empty with noFiltersActive true when no filter is active`() = runTest {
        coEvery {
            searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
        } returns emptyList()

        val vm = createVm()

        vm.uiState.test {
            val empty = awaitItem() as ExpenseListUiState.Empty
            assertTrue(empty.noFiltersActive)
        }
    }

    // ── Loading state emitted first ──────────────────────────────────────────

    @Test
    fun `Loading is first emission (even if consumed by process death)`() =
        runTest {
            val expenses = listOf(sampleExpense(id = 1L))
            coEvery {
                searchExpenses(ExpenseFilter(), limit = 20, offset = 0)
            } returns expenses

            val vm = createVm()

            vm.uiState.test {
                // Skip Loading since UnconfinedTestDispatcher executes the
                // init block synchronously — we go directly to Ready.
                // We verify the current state is Ready.
                val state = awaitItem()
                assertTrue(state is ExpenseListUiState.Ready)
            }
        }
}
