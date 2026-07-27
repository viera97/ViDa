package com.vida.feature.transfermanagement

import app.cash.turbine.test
import com.vida.domain.model.Card
import com.vida.domain.model.CardNumber
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Stash
import com.vida.domain.model.Wallet
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.transfer.RecordTransfer
import com.vida.domain.usecase.wallet.ListWallets
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Unit tests for [TransferFormViewModel] covering 23 spec scenarios
 * (SCN-TRF-001 through SCN-TRF-023) plus edge cases.
 *
 * Test infrastructure matches [com.vida.feature.expense.ExpenseFormViewModelTest]:
 * MockK + Turbine + UnconfinedTestDispatcher. No Hilt, no Android runtime.
 *
 * NOTE: With UnconfinedTestDispatcher, [TransferFormViewModel.init] completes synchronously
 * inside [createVm]. Turbine's first [awaitItem] receives the final init state
 * (Ready / EmptySourceList / Error), NOT Idle. Idle is only observable when
 * init is artificially delayed via [CompletableDeferred].
 */
class TransferFormViewModelTest {

    private lateinit var recordTransfer: RecordTransfer
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
        Card(
            id = 2L,
            number = CardNumber.fromFirst6Last4("654321", "7890"),
            bank = "Banco Popular",
            type = CardType.CREDIT,
            currency = "CUP",
            expirationDate = LocalDate.of(2027, 6, 15),
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
        Stash(
            id = 2L,
            name = "Efectivo",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            currency = Currency.CUP,
        ),
    )

    private val defaultWallet = Wallet(currency = "CUP")

    // Pre-constructed TransferSourceItem fixtures matching the sample data.
    //
    // The wallet entry uses the wallet's real row id (1L, matching the default
    // wallet fixture seeded in setup). The Transfer domain invariant requires
    // non-null fromId/toId for ALL source types — the wallet is no longer a
    // singleton special case.
    private val walletSource = TransferSourceItem(
        id = 1L, type = SourceType.WALLET, name = "Billetera",
        currency = Currency.CUP, icon = "\uD83D\uDCB0",
    )
    private val card1Source = TransferSourceItem(
        id = 1L, type = SourceType.CARD, name = "Banco kubo",
        currency = Currency.USD, icon = "\u2660\uFE0F", subtitle = "···3456",
    )
    private val card2Source = TransferSourceItem(
        id = 2L, type = SourceType.CARD, name = "Banco Popular",
        currency = Currency.CUP, icon = "\u2660\uFE0F", subtitle = "···7890",
    )
    private val stash1Source = TransferSourceItem(
        id = 1L, type = SourceType.STASH, name = "Ahorro vacaciones",
        currency = Currency.MLC, icon = "\uD83D\uDC8E",
    )
    private val stash2Source = TransferSourceItem(
        id = 2L, type = SourceType.STASH, name = "Efectivo",
        currency = Currency.CUP, icon = "\uD83D\uDC8E",
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        recordTransfer = mockk()
        listCards = mockk()
        listStashes = mockk()
        listWallets = mockk()

        // Default mocks: wallet + 2 cards + 2 stashes, recordTransfer succeeds
        every { listWallets() } returns flowOf(listOf(defaultWallet))
        every { listCards() } returns flowOf(sampleCards)
        every { listStashes() } returns flowOf(sampleStashes)
        coEvery { recordTransfer(any()) } returns 1L
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Factory ─────────────────────────────────────────────────────────────

    private fun createVm(): TransferFormViewModel = TransferFormViewModel(
        recordTransfer = recordTransfer,
        listCards = listCards,
        listStashes = listStashes,
        listWallets = listWallets,
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-001: Init → Ready with sources
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `init loads all sources and emits Ready`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as TransferFormUiState.Ready

            assertEquals(5, ready.sources.size) // wallet + 2 cards + 2 stashes

            // Wallet is first. id is the wallet's real row id — the wallet is no
            // longer a singleton special case (see Transfer domain invariant).
            assertEquals(SourceType.WALLET, ready.sources[0].type)
            assertEquals(1L, ready.sources[0].id)
            assertEquals("Billetera", ready.sources[0].name)
            assertEquals(Currency.CUP, ready.sources[0].currency)

            // Cards follow
            assertEquals(SourceType.CARD, ready.sources[1].type)
            assertEquals(1L, ready.sources[1].id)
            assertEquals(Currency.USD, ready.sources[1].currency)

            assertEquals(SourceType.CARD, ready.sources[2].type)
            assertEquals(2L, ready.sources[2].id)
            assertEquals(Currency.CUP, ready.sources[2].currency)

            // Stashes last
            assertEquals(SourceType.STASH, ready.sources[3].type)
            assertEquals(1L, ready.sources[3].id)
            assertEquals(Currency.MLC, ready.sources[3].currency)

            assertEquals(SourceType.STASH, ready.sources[4].type)
            assertEquals(2L, ready.sources[4].id)
            assertEquals(Currency.CUP, ready.sources[4].currency)

            // Defaults: no selection, empty fields
            assertNull(ready.deSource)
            assertNull(ready.aSource)
            assertEquals("", ready.amount)
            assertEquals("", ready.note)
        }
    }

    @Test
    fun `init emits Error when listCards throws`() = runTest {
        every { listCards() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as TransferFormUiState.Error
            assertTrue(error.message.isNotBlank())
        }
    }

    @Test
    fun `init emits Error when listStashes throws`() = runTest {
        every { listStashes() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as TransferFormUiState.Error
            assertTrue(error.message.isNotBlank())
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-004: Select De source, wallet disabled in A (mutual exclusion)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `selecting De updates deSource while keeping aSource null`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)

            val updated = awaitItem() as TransferFormUiState.Ready
            assertEquals(walletSource, updated.deSource)
            assertNull(updated.aSource)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-005: Swap selections — changing De re-enables previous in A
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `changing deSource when it matches aSource clears aSource`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()
            vm.onASelected(card2Source) // different source
            awaitItem()

            // Now change De to the same card that's selected for A
            vm.onDeSelected(card2Source)

            val updated = awaitItem() as TransferFormUiState.Ready
            assertEquals(card2Source, updated.deSource)
            assertNull(updated.aSource)
        }
    }

    @Test
    fun `changing aSource when it matches deSource clears deSource`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(card2Source)
            awaitItem()

            // Select same source for A
            vm.onASelected(card2Source)

            val updated = awaitItem() as TransferFormUiState.Ready
            assertEquals(card2Source, updated.aSource)
            assertNull(updated.deSource)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-006: Visual icons per SourceType
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `sources have correct icons per SourceType`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as TransferFormUiState.Ready

            val wallet = ready.sources.find { it.type == SourceType.WALLET }
            assertNotNull(wallet)
            assertEquals("\uD83D\uDCB0", wallet!!.icon)

            val card = ready.sources.find { it.type == SourceType.CARD }
            assertNotNull(card)
            assertEquals("\u2660\uFE0F", card!!.icon)

            val stash = ready.sources.find { it.type == SourceType.STASH }
            assertNotNull(stash)
            assertEquals("\uD83D\uDC8E", stash!!.icon)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-007: Cross-currency rejected
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `submit rejects cross-currency transfer`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)   // CUP
            awaitItem()
            vm.onASelected(card1Source)     // USD
            awaitItem()
            vm.onAmountChanged("100")
            awaitItem()

            vm.submit()

            val withErrors = awaitItem() as TransferFormUiState.Ready
            assertNotNull(withErrors.validationErrors["currency"])
            assertTrue(withErrors.validationErrors["currency"]!!.contains("moneda"))

            coVerify(inverse = true) { recordTransfer(any()) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-008: Same-currency accepted
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `submit accepts same-currency transfer`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)   // CUP
            awaitItem()
            vm.onASelected(card2Source)     // CUP
            awaitItem()
            vm.onAmountChanged("100")
            awaitItem()

            vm.submit()

            assertEquals(TransferFormUiState.Saved, awaitItem())

            coVerify(exactly = 1) { recordTransfer(any()) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-009: Picker prevents self-transfer (mutual exclusion at VM)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `selecting same source for both De and A clears the other`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()

            // Select same wallet for A
            vm.onASelected(walletSource)

            val updated = awaitItem() as TransferFormUiState.Ready
            assertEquals(walletSource, updated.aSource)
            assertNull(updated.deSource)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-010: Domain defense-in-depth (RecordTransfer rejects self-transfer)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `domain-level rejection emits Error`() = runTest {
        coEvery { recordTransfer(any()) } throws
            IllegalArgumentException("Cannot transfer from a source to itself")

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(card2Source)
            awaitItem()
            vm.onASelected(stash2Source) // Different source, same currency
            awaitItem()
            vm.onAmountChanged("50")
            awaitItem()

            vm.submit()


            val error = awaitItem() as TransferFormUiState.Error
            assertTrue(error.message.isNotBlank())
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-011: Valid positive amount
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `valid positive amount updates form`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onAmountChanged("150.75")

            val updated = awaitItem() as TransferFormUiState.Ready
            assertEquals("150.75", updated.amount)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-012: Zero rejected
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `submit rejects zero amount`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()
            vm.onASelected(card2Source)
            awaitItem()
            vm.onAmountChanged("0")
            awaitItem()

            vm.submit()

            val withErrors = awaitItem() as TransferFormUiState.Ready
            assertNotNull(withErrors.validationErrors["amount"])
            coVerify(inverse = true) { recordTransfer(any()) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-013: Negative rejected
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `submit rejects negative amount`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()
            vm.onASelected(card2Source)
            awaitItem()
            vm.onAmountChanged("-50")
            awaitItem()

            vm.submit()

            val withErrors = awaitItem() as TransferFormUiState.Ready
            assertNotNull(withErrors.validationErrors["amount"])
            coVerify(inverse = true) { recordTransfer(any()) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-014: Non-numeric rejected
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `submit rejects non-numeric amount`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()
            vm.onASelected(card2Source)
            awaitItem()
            vm.onAmountChanged("abc")
            awaitItem()

            vm.submit()

            val withErrors = awaitItem() as TransferFormUiState.Ready
            assertNotNull(withErrors.validationErrors["amount"])
            coVerify(inverse = true) { recordTransfer(any()) }
        }
    }

    @Test
    fun `submit rejects blank amount`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()
            vm.onASelected(card2Source)
            awaitItem()
            // amount left as default "" (blank)
            vm.submit()

            val withErrors = awaitItem() as TransferFormUiState.Ready
            assertNotNull(withErrors.validationErrors["amount"])
            coVerify(inverse = true) { recordTransfer(any()) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-015: Default to current datetime
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `dateTime defaults to now`() = runTest {
        val vm = createVm()
        val before = Instant.now()

        vm.uiState.test {
            val ready = awaitItem() as TransferFormUiState.Ready

            val after = Instant.now()
            assertTrue(
                ready.dateTime >= before || ready.dateTime >= before.minusSeconds(1),
            )
            assertTrue(
                ready.dateTime <= after || ready.dateTime <= after.plusSeconds(1),
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-016: User selects custom datetime
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `changing dateTime updates form`() = runTest {
        val vm = createVm()
        val future = Instant.now().plus(7, ChronoUnit.DAYS)

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDateTimeChanged(future)

            val updated = awaitItem() as TransferFormUiState.Ready
            assertEquals(future, updated.dateTime)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-017: Empty note accepted
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `submit with empty note passes validation`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()
            vm.onASelected(card2Source)
            awaitItem()
            vm.onAmountChanged("100")
            awaitItem()
            // note left as default ""

            vm.submit()

            assertEquals(TransferFormUiState.Saved, awaitItem())
            coVerify(exactly = 1) { recordTransfer(any()) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-018: Max length exceeded
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `submit rejects note over 500 characters`() = runTest {
        val vm = createVm()
        val longNote = "x".repeat(501)

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()
            vm.onASelected(card2Source)
            awaitItem()
            vm.onAmountChanged("100")
            awaitItem()
            vm.onNoteChanged(longNote)
            awaitItem()

            vm.submit()

            val withErrors = awaitItem() as TransferFormUiState.Ready
            assertNotNull(withErrors.validationErrors["note"])
            coVerify(inverse = true) { recordTransfer(any()) }
        }
    }

    @Test
    fun `submit accepts note exactly 500 characters`() = runTest {
        val vm = createVm()
        val exactNote = "x".repeat(500)

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()
            vm.onASelected(card2Source)
            awaitItem()
            vm.onAmountChanged("100")
            awaitItem()
            vm.onNoteChanged(exactNote)
            awaitItem()

            vm.submit()

            assertEquals(TransferFormUiState.Saved, awaitItem())
            coVerify(exactly = 1) { recordTransfer(any()) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-019: Submission success → Saved + NavEvent
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `successful submit calls RecordTransfer with correct args and emits Saved`() =
        runTest {
            val vm = createVm()

            vm.uiState.test {
                awaitItem() // Ready

                vm.onDeSelected(walletSource)
                awaitItem()
                vm.onASelected(card2Source)
                awaitItem()
                vm.onAmountChanged("1250.50")
                awaitItem()

                vm.submit()

                assertEquals(TransferFormUiState.Saved, awaitItem())

                coVerify {
                    recordTransfer(
                        withArg { transfer ->
                            assertEquals(SourceType.WALLET, transfer.fromType)
                            assertEquals(1L, transfer.fromId)
                            assertEquals(SourceType.CARD, transfer.toType)
                            assertEquals(2L, transfer.toId)
                            assertEquals(
                                Money(BigDecimal("1250.50"), Currency.CUP),
                                transfer.amount,
                            )
                            assertNull(transfer.note)
                        },
                    )
                }
            }

            // Nav event emitted after Saved
            vm.navEvents.test {
                assertEquals(TransferFormNavEvent.NavigateBack, awaitItem())
            }
        }

    @Test
    fun `successful submit emits NavEvent with a note`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()
            vm.onASelected(card2Source)
            awaitItem()
            vm.onAmountChanged("75")
            awaitItem()
            vm.onNoteChanged("Para el almuerzo")
            awaitItem()

            vm.submit()

            assertEquals(TransferFormUiState.Saved, awaitItem())

            coVerify {
                recordTransfer(
                    withArg { transfer ->
                        assertEquals("Para el almuerzo", transfer.note)
                    },
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-020: Double-tap guard
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `double-tap guard prevents duplicate submission`() = runTest {
        val recordDeferred = CompletableDeferred<Long>()
        coEvery { recordTransfer(any()) } coAnswers { recordDeferred.await() }

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()
            vm.onASelected(card2Source)
            awaitItem()
            vm.onAmountChanged("100")
            awaitItem()

            // First submit
            vm.submit()


            // isSaving should be true while recordTransfer is pending
            assertTrue(vm.isSaving.value)

            // Second submit should be a no-op (isSaving guard)
            vm.submit()
            assertTrue(vm.isSaving.value)

            // Complete the pending transfer
            recordDeferred.complete(1L)

            assertEquals(TransferFormUiState.Saved, awaitItem())
            assertFalse(vm.isSaving.value)

            // Only one recordTransfer call
            coVerify(exactly = 1) { recordTransfer(any()) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-021: RecordTransfer throws — Error with form preservation
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `submit when RecordTransfer throws emits Error`() = runTest {
        coEvery { recordTransfer(any()) } throws RuntimeException("Network error")
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()
            vm.onASelected(card2Source)
            awaitItem()
            vm.onAmountChanged("500")
            awaitItem()
            vm.onNoteChanged("Transferencia de prueba")
            awaitItem()

            vm.submit()

            val error = awaitItem() as TransferFormUiState.Error
            assertTrue(error.message.isNotBlank())

            // isSaving should be false after error
            assertFalse(vm.isSaving.value)
        }
    }

    @Test
    fun `editing a field after Error recovers to Ready with preserved data`() =
        runTest {
            coEvery { recordTransfer(any()) } throws RuntimeException("Save error")
            val vm = createVm()

            vm.uiState.test {
                awaitItem() // Ready

                vm.onDeSelected(walletSource)
                awaitItem()
                vm.onASelected(card2Source)
                awaitItem()
                vm.onAmountChanged("200")
                awaitItem()
                vm.onNoteChanged("Nota original")
                awaitItem()

                vm.submit()

                val error = awaitItem() as TransferFormUiState.Error
                assertTrue(error.message.isNotBlank())

                // Edit amount after error — should recover to Ready preserving data
                vm.onAmountChanged("300")

                val recovered = awaitItem() as TransferFormUiState.Ready
                assertEquals("300", recovered.amount)
                assertEquals(walletSource, recovered.deSource)
                assertEquals(card2Source, recovered.aSource)
                assertEquals("Nota original", recovered.note)
            }
        }

    @Test
    fun `form data preserved in Error state after submit failure`() = runTest {
        coEvery { recordTransfer(any()) } throws RuntimeException("DB error")
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()
            vm.onASelected(stash2Source)
            awaitItem()
            vm.onAmountChanged("1000")
            awaitItem()
            vm.onDateTimeChanged(Instant.parse("2026-03-15T14:30:00Z"))
            awaitItem()
            vm.onNoteChanged("Transferencia mensual")
            awaitItem()

            vm.submit()

            awaitItem() // Error

            // Edit note — should recover with all data preserved
            vm.onNoteChanged("Transferencia mensual modificada")

            val recovered = awaitItem() as TransferFormUiState.Ready
            assertEquals(walletSource, recovered.deSource)
            assertEquals(stash2Source, recovered.aSource)
            assertEquals("1000", recovered.amount)
            assertEquals(Instant.parse("2026-03-15T14:30:00Z"), recovered.dateTime)
            assertEquals("Transferencia mensual modificada", recovered.note)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCN-TRF-022: Init retry
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `submit without deSource selected shows validation error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onASelected(card2Source)
            awaitItem()
            vm.onAmountChanged("100")
            awaitItem()

            vm.submit()

            val withErrors = awaitItem() as TransferFormUiState.Ready
            assertNotNull(withErrors.validationErrors["deSource"])
            coVerify(inverse = true) { recordTransfer(any()) }
        }
    }

    @Test
    fun `submit without aSource selected shows validation error`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDeSelected(walletSource)
            awaitItem()
            vm.onAmountChanged("100")
            awaitItem()

            vm.submit()

            val withErrors = awaitItem() as TransferFormUiState.Ready
            assertNotNull(withErrors.validationErrors["aSource"])
            coVerify(inverse = true) { recordTransfer(any()) }
        }
    }

    @Test
    fun `onNoteChanged updates note field`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onNoteChanged("Nota de prueba")

            val updated = awaitItem() as TransferFormUiState.Ready
            assertEquals("Nota de prueba", updated.note)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Regression: wallet source item id contract
    //
    // TransferSourceItem.id is the row id of the corresponding
    // wallet/card/stash for ALL source types — the wallet is no longer a
    // singleton. Transfer's domain invariant requires non-null fromId/toId
    // for every source type. submit() passes de.id / a.id straight into
    // Transfer(...), which is correct now that id is always non-null.
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `submit with wallet as destination produces Transfer with toId=walletId and toType=WALLET`() =
        runTest {
            val vm = createVm()

            vm.uiState.test {
                awaitItem() // Ready

                vm.onDeSelected(card2Source) // CUP card → De (origin)
                awaitItem()
                vm.onASelected(walletSource) // wallet → A (destination)
                awaitItem()
                vm.onAmountChanged("100")
                awaitItem()

                vm.submit()

                assertEquals(TransferFormUiState.Saved, awaitItem())

                coVerify {
                    recordTransfer(
                        withArg { transfer ->
                            assertEquals(SourceType.CARD, transfer.fromType)
                            assertEquals(2L, transfer.fromId)
                            assertEquals(SourceType.WALLET, transfer.toType)
                            assertEquals(1L, transfer.toId)
                            assertEquals(
                                Money(BigDecimal("100"), Currency.CUP),
                                transfer.amount,
                            )
                        },
                    )
                }
            }
        }

    @Test
    fun `submit with wallet as origin produces Transfer with fromId=walletId and fromType=WALLET`() =
        runTest {
            val vm = createVm()

            vm.uiState.test {
                awaitItem() // Ready

                vm.onDeSelected(walletSource) // wallet → De (origin)
                awaitItem()
                vm.onASelected(card2Source)   // CUP card → A (destination)
                awaitItem()
                vm.onAmountChanged("250")
                awaitItem()

                vm.submit()

                assertEquals(TransferFormUiState.Saved, awaitItem())

                coVerify {
                    recordTransfer(
                        withArg { transfer ->
                            assertEquals(SourceType.WALLET, transfer.fromType)
                            assertEquals(1L, transfer.fromId)
                            assertEquals(SourceType.CARD, transfer.toType)
                            assertEquals(2L, transfer.toId)
                            assertEquals(
                                Money(BigDecimal("250"), Currency.CUP),
                                transfer.amount,
                            )
                        },
                    )
                }
            }
        }

    @Test
    fun `buildSourceList emits wallet TransferSourceItem with id=walletId`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as TransferFormUiState.Ready

            val walletItem = ready.sources.first { it.type == SourceType.WALLET }
            assertEquals(1L, walletItem.id)
        }
    }
}
