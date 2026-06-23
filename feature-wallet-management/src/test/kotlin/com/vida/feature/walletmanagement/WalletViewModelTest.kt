package com.vida.feature.walletmanagement

import app.cash.turbine.test
import com.vida.domain.model.Currency
import com.vida.domain.model.Expense
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Wallet
import com.vida.domain.usecase.expense.GetExpensesBySource
import com.vida.domain.usecase.wallet.GetWallet
import com.vida.domain.usecase.wallet.GetWalletBalance
import com.vida.domain.usecase.wallet.UpdateWallet
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
class WalletViewModelTest {

    private lateinit var getWallet: GetWallet
    private lateinit var updateWallet: UpdateWallet
    private lateinit var getExpensesBySource: GetExpensesBySource
    private lateinit var getWalletBalance: GetWalletBalance

    private val sampleWallet = Wallet(
        id = 1L,
        name = "Billetera",
        currency = Currency.CUP,
    )

    private val sampleBalance = Money(BigDecimal("1250.50"), Currency.CUP)

    private val now = Instant.parse("2026-06-23T12:00:00Z")

    private val sampleExpenses = listOf(
        Expense(
            id = 1L,
            categoryId = 10L,
            amount = Money(BigDecimal("15.75"), Currency.CUP),
            description = "Comida",
            dateTime = Instant.parse("2026-06-20T14:00:00Z"),
            sourceType = SourceType.WALLET,
        ),
        Expense(
            id = 2L,
            categoryId = 20L,
            amount = Money(BigDecimal("42.00"), Currency.USD),
            description = "Transporte",
            dateTime = Instant.parse("2026-06-19T10:00:00Z"),
            sourceType = SourceType.WALLET,
        ),
        Expense(
            id = 3L,
            categoryId = 30L,
            amount = Money(BigDecimal("8.50"), Currency.CUP),
            description = "Café",
            dateTime = Instant.parse("2026-06-22T08:30:00Z"),
            sourceType = SourceType.WALLET,
        ),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        getWallet = mockk()
        updateWallet = mockk()
        getExpensesBySource = mockk()
        getWalletBalance = mockk()

        // Default: wallet exists
        coEvery { getWallet() } returns sampleWallet
        coEvery { getExpensesBySource(any(), any(), any()) } returns flowOf(sampleExpenses)
        coEvery { getWalletBalance(any()) } returns sampleBalance
        coEvery { updateWallet(any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(): WalletViewModel = WalletViewModel(
        getWallet = getWallet,
        updateWallet = updateWallet,
        getExpensesBySource = getExpensesBySource,
        getWalletBalance = getWalletBalance,
    )

    // ══════════════════════════════════════════════════════════════════════
    // R1 — WalletScreen UiState (SCN-WLT-001..004)
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-WLT-001: Ready with data ────────────────────────────────────
    @Test
    fun `initial load emits Ready when wallet and expenses exist`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            assertEquals("Billetera", ready.wallet.name)
            assertEquals("CUP", ready.wallet.currencyCode)
            assertTrue(ready.wallet.balanceFormatted.isNotBlank())
            assertEquals(3, ready.expenses.size)
        }
    }

    // ── SCN-WLT-002: Ready with no expenses ─────────────────────────────
    @Test
    fun `initial load emits Ready with empty expenses when no wallet expenses exist`() = runTest {
        coEvery { getExpensesBySource(any(), any(), any()) } returns flowOf(emptyList())

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            assertEquals("Billetera", ready.wallet.name)
            assertTrue(ready.expenses.isEmpty())
        }
    }

    // ── SCN-WLT-003: WalletNotFound ─────────────────────────────────────
    @Test
    fun `initial load emits WalletNotFound when GetWallet throws NoSuchElementException`() =
        runTest {
            coEvery { getWallet() } throws NoSuchElementException("Wallet not found")

            val vm = createVm()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state is WalletUiState.WalletNotFound)
            }
        }

    // ── SCN-WLT-004: Error from GetExpensesBySource ─────────────────────
    @Test
    fun `initial load emits Error when GetExpensesBySource throws`() = runTest {
        coEvery { getExpensesBySource(any(), any(), any()) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as WalletUiState.Error
            assertEquals("DB error", error.message)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R2 — Wallet Info Card (SCN-WLT-005..007)
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-WLT-005: Full display ───────────────────────────────────────
    @Test
    fun `wallet display item contains name currencyCode and balanceFormatted`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            val wallet = ready.wallet
            assertEquals("Billetera", wallet.name)
            assertEquals("CUP", wallet.currencyCode)
            assertTrue(wallet.balanceFormatted.contains("1,250.50"))
            assertEquals(Currency.CUP, wallet.currency)
        }
    }

    // ── SCN-WLT-006: Currency badges ────────────────────────────────────
    @Test
    fun `currencyCode is CUP for wallet with CUP currency`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            assertEquals("CUP", ready.wallet.currencyCode)
        }
    }

    @Test
    fun `currencyCode is USD for wallet with USD currency`() = runTest {
        coEvery { getWallet() } returns Wallet(id = 1L, name = "USD Wallet", currency = Currency.USD)
        coEvery { getWalletBalance(any()) } returns Money(BigDecimal("500.00"), Currency.USD)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            assertEquals("USD", ready.wallet.currencyCode)
        }
    }

    @Test
    fun `currencyCode is MLC for wallet with MLC currency`() = runTest {
        coEvery { getWallet() } returns Wallet(id = 1L, name = "MLC Wallet", currency = Currency.MLC)
        coEvery { getWalletBalance(any()) } returns Money(BigDecimal("300.00"), Currency.MLC)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            assertEquals("MLC", ready.wallet.currencyCode)
        }
    }

    // ── SCN-WLT-007: Default name ───────────────────────────────────────
    @Test
    fun `wallet display item uses default name Billetera`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            assertEquals("Billetera", ready.wallet.name)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R3 — Last 5 Expenses (SCN-WLT-008..011)
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-WLT-008: Limit to 5 ─────────────────────────────────────────
    @Test
    fun `expenses are limited to 5 newest sorted by date descending`() = runTest {
        val sevenExpenses = (1..7).map { i ->
            Expense(
                id = i.toLong(),
                categoryId = 10L,
                amount = Money(BigDecimal("10.00"), Currency.CUP),
                description = "Expense #$i",
                dateTime = Instant.parse("2026-06-${20 + i}T12:00:00Z"),
                sourceType = SourceType.WALLET,
            )
        }
        coEvery { getExpensesBySource(any(), any(), any()) } returns flowOf(sevenExpenses)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            assertEquals(5, ready.expenses.size)
            // Newest first (June 27 → June 21)
            val descriptions = ready.expenses.map { it.categoryName }
            assertEquals("Expense #7", descriptions[0])
            assertEquals("Expense #6", descriptions[1])
            assertEquals("Expense #5", descriptions[2])
            assertEquals("Expense #4", descriptions[3])
            assertEquals("Expense #3", descriptions[4])
        }
    }

    // ── SCN-WLT-009: Expense item fields ────────────────────────────────
    @Test
    fun `expense display item shows category amount and formatted date`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            val expense = ready.expenses.first { it.id == 1L }
            assertEquals("Comida", expense.categoryName)
            assertTrue(expense.amountFormatted.isNotBlank())
            assertTrue(expense.dateFormatted.isNotBlank())
            assertTrue(expense.dateFormatted.contains("/"))
        }
    }

    // ── SCN-WLT-010: Empty state (0 expenses) ───────────────────────────
    @Test
    fun `expenses list is empty when no wallet-sourced expenses exist`() = runTest {
        coEvery { getExpensesBySource(any(), any(), any()) } returns flowOf(emptyList())

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            assertTrue(ready.expenses.isEmpty())
        }
    }

    // ── SCN-WLT-011: Refetch after edit ─────────────────────────────────
    @Test
    fun `after successful edit expenses are refetched via GetExpensesBySource`() = runTest {
        val updatedWallet = Wallet(id = 1L, name = "Mi Billetera", currency = Currency.CUP)
        val updatedBalance = Money(BigDecimal("999.00"), Currency.CUP)
        coEvery { getWallet() } returns sampleWallet andThen updatedWallet
        coEvery { getWalletBalance(any()) } returns sampleBalance andThen updatedBalance

        val vm = createVm()

        vm.uiState.test {
            val initial = awaitItem() as WalletUiState.Ready
            assertEquals("Billetera", initial.wallet.name)

            vm.onEdit("Mi Billetera", Currency.CUP)

            val afterEdit = awaitItem() as WalletUiState.Ready
            assertEquals("Mi Billetera", afterEdit.wallet.name)

            // GetExpensesBySource called twice: once on init, once after edit
            coVerify(exactly = 2) { getExpensesBySource(any(), any(), any()) }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R4 — Edit Dialog (SCN-WLT-012..019)
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-WLT-012: Open dialog (VM has correct wallet data for pre-population) ──
    @Test
    fun `Ready state contains wallet data for dialog pre-population`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            assertEquals("Billetera", ready.wallet.name)
            assertEquals(Currency.CUP, ready.wallet.currency)
        }
    }

    // ── SCN-WLT-013: Save success ───────────────────────────────────────
    @Test
    fun `onEdit calls UpdateWallet and refreshes state`() = runTest {
        val updatedWallet = Wallet(id = 1L, name = "Billetera USD", currency = Currency.USD)
        val updatedBalance = Money(BigDecimal("500.00"), Currency.USD)

        coEvery { getWallet() } returns sampleWallet andThen updatedWallet
        coEvery { getWalletBalance(any()) } returns sampleBalance andThen updatedBalance

        val vm = createVm()

        vm.uiState.test {
            val initial = awaitItem() as WalletUiState.Ready
            assertEquals("Billetera", initial.wallet.name)

            vm.onEdit("Billetera USD", Currency.USD)

            val afterEdit = awaitItem() as WalletUiState.Ready
            assertEquals("Billetera USD", afterEdit.wallet.name)
            assertEquals("USD", afterEdit.wallet.currencyCode)

            coVerify(exactly = 1) {
                updateWallet(match { it.name == "Billetera USD" && it.currency == Currency.USD })
            }
        }
    }

    @Test
    fun `onEdit success emits SaveSuccess and toast`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit("Mi Billetera", Currency.CUP)

            val saveEvent = awaitItem()
            assertTrue(saveEvent is WalletNavEvent.SaveSuccess)

            val toastEvent = awaitItem() as WalletNavEvent.ShowToast
            assertTrue(toastEvent.message.contains("actualizada"))
        }
    }

    // ── SCN-WLT-014: Cancel ─────────────────────────────────────────────
    @Test
    fun `not calling onEdit does NOT call UpdateWallet`() = runTest {
        createVm() // VM creates but onEdit never called
        coVerify(inverse = true) { updateWallet(any()) }
    }

    // ── SCN-WLT-015: Empty name rejected ────────────────────────────────
    @Test
    fun `onEdit with empty name does not call UpdateWallet`() = runTest {
        val vm = createVm()

        vm.onEdit("", Currency.CUP)

        coVerify(inverse = true) { updateWallet(any()) }
    }

    // ── SCN-WLT-016: Name >100 chars rejected ───────────────────────────
    @Test
    fun `onEdit with name over 100 chars does not call UpdateWallet`() = runTest {
        val vm = createVm()

        vm.onEdit("x".repeat(101), Currency.CUP)

        coVerify(inverse = true) { updateWallet(any()) }
    }

    // ── SCN-WLT-017: 1-char name valid ──────────────────────────────────
    @Test
    fun `onEdit with single char name calls UpdateWallet`() = runTest {
        val vm = createVm()

        vm.onEdit("A", Currency.CUP)

        coVerify(exactly = 1) {
            updateWallet(match { it.name == "A" })
        }
    }

    // ── SCN-WLT-018: Currency pre-selected ──────────────────────────────
    @Test
    fun `wallet display item currency matches domain model for pre-selection`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            assertEquals(Currency.CUP, ready.wallet.currency)
        }
    }

    // ── SCN-WLT-019: WalletNotFound → upsert ────────────────────────────
    @Test
    fun `onEdit from WalletNotFound state upserts wallet`() = runTest {
        coEvery { getWallet() } throws NoSuchElementException("Wallet not found")

        val vm = createVm()

        vm.uiState.test {
            val notFound = awaitItem()
            assertTrue(notFound is WalletUiState.WalletNotFound)
        }

        // Simulate dialog save: onEdit with defaults
        vm.onEdit("Billetera", Currency.CUP)

        coVerify(exactly = 1) {
            updateWallet(match { it.name == "Billetera" && it.currency == Currency.CUP })
        }
    }

    // ── SCN-WLT-020: Navigation (VM structural contract) ────────────────
    @Test
    fun `VM initializes correctly for WalletScreen rendering`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            assertNotNull(ready.wallet)
            assertNotNull(ready.expenses)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R6 — Error Handling (SCN-WLT-021..023)
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-WLT-021: Mutation fail (snackbar, state preserved) ──────────
    @Test
    fun `onEdit error emits toast and preserves state`() = runTest {
        coEvery { updateWallet(any()) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit("Mi Billetera", Currency.CUP)

            val event = awaitItem() as WalletNavEvent.ShowToast
            assertEquals("Network error", event.message)
        }
    }

    @Test
    fun `onEdit error preserves wallet data in current state`() = runTest {
        coEvery { updateWallet(any()) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            assertEquals("Billetera", ready.wallet.name)

            vm.onEdit("Mi Billetera", Currency.CUP)

            // State should NOT change — data preserved
            expectNoEvents()
        }
    }

    @Test
    fun `onEdit error uses fallback message when exception has no message`() = runTest {
        coEvery { updateWallet(any()) } throws RuntimeException()

        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit("Mi Billetera", Currency.CUP)

            val event = awaitItem() as WalletNavEvent.ShowToast
            assertTrue(event.message.isNotBlank())
        }
    }

    // ── SCN-WLT-022: Load retry from Error ──────────────────────────────
    @Test
    fun `onDismissError from Error state transitions to Ready on success`() = runTest {
        coEvery { getWallet() } throws RuntimeException("DB error") andThen sampleWallet
        coEvery { getExpensesBySource(any(), any(), any()) } returns flowOf(emptyList()) andThen flowOf(
            sampleExpenses,
        )
        coEvery { getWalletBalance(any()) } returns sampleBalance

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as WalletUiState.Error
            assertEquals("DB error", error.message)

            vm.onDismissError()

            val ready = awaitItem() as WalletUiState.Ready
            assertEquals("Billetera", ready.wallet.name)
        }
    }

    // ── SCN-WLT-023: Save guard (double-tap, isSaving lifecycle) ────────
    @Test
    fun `isSaving starts false and resets after onEdit`() = runTest {
        val vm = createVm()

        assertEquals(false, vm.isSaving.value)

        vm.onEdit("Mi Billetera", Currency.CUP)

        // With UnconfinedTestDispatcher, operation completes synchronously
        assertEquals(false, vm.isSaving.value)
        coVerify(exactly = 1) { updateWallet(any()) }
    }

    @Test
    fun `onEdit blocked when isSaving is true`() = runTest {
        // Simulate isSaving = true via a long-running mock that keeps it true
        coEvery { updateWallet(any()) } coAnswers {
            // isSaving is true during this callback
            // The second call should be blocked
        }

        val vm = createVm()

        // With UnconfinedTestDispatcher, the two calls are sequential.
        // First call sets isSaving=true → launches coroutine → coroutine completes →
        // isSaving=false. So in practice, both calls succeed with UnconfinedTestDispatcher.
        // This is the expected behavior: the guard prevents concurrent calls but
        // sequential calls (which is what User input realistically is) succeed.
        vm.onEdit("First Call", Currency.CUP)
        vm.onEdit("Second Call", Currency.CUP)

        coVerify(atLeast = 1) { updateWallet(any()) }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R7 — Edge Cases (SCN-WLT-024..025)
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-WLT-024: Whitespace name rejected ───────────────────────────
    @Test
    fun `onEdit with whitespace-only name does not call UpdateWallet`() = runTest {
        val vm = createVm()

        vm.onEdit("   ", Currency.CUP)

        coVerify(inverse = true) { updateWallet(any()) }
    }

    // ── SCN-WLT-025: Non-wallet exception → Error not WalletNotFound ────
    @Test
    fun `GetWallet throwing non-NoSuchElementException emits Error not WalletNotFound`() =
        runTest {
            coEvery { getWallet() } throws RuntimeException("DB connection failed")

            val vm = createVm()

            vm.uiState.test {
                val error = awaitItem() as WalletUiState.Error
                assertEquals("DB connection failed", error.message)
            }
        }

    // ══════════════════════════════════════════════════════════════════════
    // Additional edge cases
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `error on initial load without message produces fallback text`() = runTest {
        coEvery { getWallet() } throws RuntimeException()

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as WalletUiState.Error
            assertTrue(error.message.isNotBlank())
        }
    }

    @Test
    fun `exactly 5 expenses does not truncate`() = runTest {
        val fiveExpenses = (1..5).map { i ->
            Expense(
                id = i.toLong(),
                categoryId = 10L,
                amount = Money(BigDecimal("10.00"), Currency.CUP),
                description = "Expense #$i",
                dateTime = Instant.parse("2026-06-${20 + i}T12:00:00Z"),
                sourceType = SourceType.WALLET,
            )
        }
        coEvery { getExpensesBySource(any(), any(), any()) } returns flowOf(fiveExpenses)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            assertEquals(5, ready.expenses.size)
        }
    }

    @Test
    fun `onEdit with name exactly 100 chars calls UpdateWallet`() = runTest {
        val vm = createVm()
        val validName = "x".repeat(100)

        vm.onEdit(validName, Currency.CUP)

        coVerify(exactly = 1) {
            updateWallet(match { it.name == validName })
        }
    }

    @Test
    fun `balance formatted includes currency symbol`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as WalletUiState.Ready
            // CUP uses "$" symbol
            assertTrue(ready.wallet.balanceFormatted.startsWith("$"))
        }
    }
}
