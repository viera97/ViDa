package com.vida.feature.expenselist

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.vida.domain.model.Category
import com.vida.domain.model.Currency
import com.vida.domain.model.Expense
import com.vida.domain.model.Money
import com.vida.domain.model.Refund
import com.vida.domain.model.SourceType
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.expense.DeleteExpense
import com.vida.domain.usecase.expense.GetExpense
import com.vida.domain.usecase.refund.AddRefund
import com.vida.domain.usecase.refund.DeleteRefund
import com.vida.domain.usecase.refund.GetRefundsByOriginalExpense
import com.vida.domain.usecase.refund.UpdateRefund
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
import java.math.BigDecimal
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseDetailViewModelTest {

    private lateinit var getExpense: GetExpense
    private lateinit var deleteExpense: DeleteExpense
    private lateinit var listCategories: ListCategories
    private lateinit var getRefunds: GetRefundsByOriginalExpense
    private lateinit var addRefund: AddRefund
    private lateinit var updateRefund: UpdateRefund
    private lateinit var deleteRefund: DeleteRefund

    private val sampleCategories = listOf(
        Category(id = 1L, name = "Comida", color = -43904),
        Category(id = 2L, name = "Transporte", color = -14614533),
    )

    private fun sampleExpense(
        id: Long = 1L,
        amount: Money = Money(BigDecimal("1500.00"), Currency.CUP),
    ): Expense = Expense(
        id = id,
        categoryId = 1L,
        amount = amount,
        description = "Almuerzo café",
        dateTime = Instant.now(),
        sourceType = SourceType.WALLET,
    )

    private fun sampleRefund(
        id: Long = 1L,
        originalExpenseId: Long = 1L,
        amount: Money = Money(BigDecimal("500.00"), Currency.CUP),
        reason: String = "Producto defectuoso",
        note: String? = "Reembolso parcial",
    ): Refund = Refund(
        id = id,
        originalExpenseId = originalExpenseId,
        amount = amount,
        reason = reason,
        dateTime = Instant.now(),
        note = note,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        getExpense = mockk()
        deleteExpense = mockk()
        listCategories = mockk()
        getRefunds = mockk()
        addRefund = mockk()
        updateRefund = mockk()
        deleteRefund = mockk()

        coEvery { getExpense(any()) } returns sampleExpense()
        every { listCategories() } returns flowOf(sampleCategories)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(id: String = "1"): ExpenseDetailViewModel = ExpenseDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("id" to id)),
        getExpense = getExpense,
        deleteExpense = deleteExpense,
        listCategories = listCategories,
        getRefunds = getRefunds,
        addRefund = addRefund,
        updateRefund = updateRefund,
        deleteRefund = deleteRefund,
    )

    // ── SCN-REFUND-001: Refund load emits Ready when refund exists ──────────

    @Test
    fun `refundState emits Ready when refund exists`() = runTest {
        coEvery { getRefunds(1L) } returns flowOf(listOf(sampleRefund()))

        val vm = createVm()

        vm.refundState.test {
            val state = awaitItem()
            if (state is RefundUiState.Loading) {
                val ready = awaitItem() as RefundUiState.Ready
                assertEquals(1L, ready.refund.id)
                assertEquals("Producto defectuoso", ready.refund.reason)
                assertTrue(ready.refund.formattedAmount.contains("500.00"))
            } else {
                assertTrue("Expected Ready, got $state", state is RefundUiState.Ready)
                assertEquals(1L, (state as RefundUiState.Ready).refund.id)
            }
        }
    }

    // ── SCN-REFUND-002: Refund load emits Empty when no refund exists ───────

    @Test
    fun `refundState emits Empty when no refund exists`() = runTest {
        coEvery { getRefunds(1L) } returns flowOf(emptyList())

        val vm = createVm()

        vm.refundState.test {
            val state = awaitItem()
            if (state is RefundUiState.Loading) {
                assertTrue(awaitItem() is RefundUiState.Empty)
            } else {
                assertTrue("Expected Empty, got $state", state is RefundUiState.Empty)
            }
        }
    }

    // ── SCN-REFUND-003: Refund load emits Error ─────────────────────────────

    @Test
    fun `refundState emits Error when GetRefunds throws`() = runTest {
        coEvery { getRefunds(1L) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.refundState.test {
            val state = awaitItem()
            if (state is RefundUiState.Loading) {
                val error = awaitItem() as RefundUiState.Error
                assertTrue(error.message.isNotBlank())
            } else {
                assertTrue("Expected Error, got $state", state is RefundUiState.Error)
            }
        }
    }

    // ── SCN-REFUND-004: Add refund success ──────────────────────────────────

    @Test
    fun `onAddRefund reloads refundState on success`() = runTest {
        coEvery { getRefunds(1L) } returnsMany listOf(
            flowOf(emptyList()),              // initial load
            flowOf(listOf(sampleRefund())),   // reload after add
        )
        coEvery { addRefund(any()) } returns 1L

        val vm = createVm()

        // Consume initial state first
        vm.refundState.test {
            // Skip initial Empty (or Loading then Empty)
            while (awaitItem() !is RefundUiState.Empty) { }

            vm.onAddRefund(BigDecimal("500.00"), "Producto defectuoso", "Reembolso parcial")

            // Should see Loading → Ready
            val postAdd = awaitItem()
            if (postAdd is RefundUiState.Loading) {
                val ready = awaitItem() as RefundUiState.Ready
                assertEquals("Producto defectuoso", ready.refund.reason)
            } else {
                assertTrue("Expected Ready, got $postAdd", postAdd is RefundUiState.Ready)
            }
        }
    }

    // ── SCN-REFUND-005: Add refund failure emits snackbar ───────────────────

    @Test
    fun `onAddRefund emits snackbar on failure`() = runTest {
        coEvery { getRefunds(1L) } returns flowOf(emptyList())
        coEvery { addRefund(any()) } throws RuntimeException("Duplicate refund")

        val vm = createVm()

        vm.snackbarEvents.test {
            vm.onAddRefund(BigDecimal("500.00"), "Producto defectuoso", null)
            val event = awaitItem()
            assertTrue(event.message.contains("Duplicate refund"))
        }
    }

    // ── SCN-REFUND-006: Edit refund success ─────────────────────────────────

    @Test
    fun `onEditRefund reloads refundState on success`() = runTest {
        val existingRefund = sampleRefund()
        coEvery { getRefunds(1L) } returnsMany listOf(
            flowOf(listOf(existingRefund)),     // initial load
            flowOf(listOf(existingRefund.copy(reason = "Editado"))), // reload
        )
        coEvery { updateRefund(any()) } returns 1L

        val vm = createVm()

        vm.refundState.test {
            // Skip to Ready
            while (awaitItem() !is RefundUiState.Ready) { }

            vm.onEditRefund(BigDecimal("600.00"), "Editado", null)

            val postEdit = awaitItem()
            if (postEdit is RefundUiState.Loading) {
                val ready = awaitItem() as RefundUiState.Ready
                assertEquals("Editado", ready.refund.reason)
            } else {
                assertTrue("Expected Ready, got $postEdit", postEdit is RefundUiState.Ready)
            }
        }
    }

    // ── SCN-REFUND-007: Edit refund failure emits snackbar ──────────────────

    @Test
    fun `onEditRefund emits snackbar on failure`() = runTest {
        coEvery { getRefunds(1L) } returns flowOf(listOf(sampleRefund()))
        coEvery { updateRefund(any()) } throws RuntimeException("Invalid amount")

        val vm = createVm()

        vm.snackbarEvents.test {
            vm.onEditRefund(BigDecimal("600.00"), "Invalid", null)
            val event = awaitItem()
            assertTrue("Got: ${event.message}", event.message.contains("Invalid amount"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── SCN-REFUND-008: Delete refund success ───────────────────────────────

    @Test
    fun `onDeleteRefund sets refundState to Empty on success`() = runTest {
        coEvery { getRefunds(1L) } returns flowOf(listOf(sampleRefund()))
        coEvery { deleteRefund(any()) } returns Unit

        val vm = createVm()

        vm.refundState.test {
            // Skip to Ready
            while (awaitItem() !is RefundUiState.Ready) { }

            vm.onDeleteRefund()
            assertTrue(awaitItem() is RefundUiState.Empty)
        }
    }

    // ── SCN-REFUND-009: Delete refund failure emits snackbar ────────────────

    @Test
    fun `onDeleteRefund emits snackbar on failure`() = runTest {
        coEvery { getRefunds(1L) } returns flowOf(listOf(sampleRefund()))
        coEvery { deleteRefund(any()) } throws RuntimeException("Delete failed")

        val vm = createVm()

        vm.snackbarEvents.test {
            vm.onDeleteRefund()
            val event = awaitItem()
            assertTrue(event.message.contains("Delete failed"))
        }
    }

    // ── SCN-REFUND-010: RefundDisplay carries correct fields ────────────────

    @Test
    fun `RefundDisplay maps refund fields correctly`() = runTest {
        val refund = sampleRefund(note = "Nota extra")
        coEvery { getRefunds(1L) } returns flowOf(listOf(refund))

        val vm = createVm()

        vm.refundState.test {
            var state = awaitItem()
            if (state is RefundUiState.Loading) state = awaitItem()
            val ready = state as RefundUiState.Ready
            val display = ready.refund

            assertEquals(1L, display.id)
            assertEquals(BigDecimal("500.00"), display.amount)
            assertTrue(display.formattedAmount.contains("500.00"))
            assertEquals("Producto defectuoso", display.reason)
            assertTrue(display.formattedDate.isNotBlank())
            assertEquals("Nota extra", display.note)
        }
    }

    // ── Expired ID emits Error in both states ───────────────────────────────

    @Test
    fun `invalid expenseId emits Error in both uiState and refundState`() = runTest {
        val vm = createVm(id = "0")

        vm.uiState.test {
            val ui = awaitItem()
            assertTrue(ui is ExpenseDetailUiState.Error)
            assertTrue((ui as ExpenseDetailUiState.Error).message.contains("ID"))
        }

        vm.refundState.test {
            val ref = awaitItem()
            assertTrue(ref is RefundUiState.Error)
        }
    }
}
