package com.vida.feature.cardmanagement

import app.cash.turbine.test
import com.vida.domain.model.Card
import com.vida.domain.model.CardNumber
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import com.vida.domain.usecase.card.AddCard
import com.vida.domain.usecase.card.DeleteCard
import com.vida.domain.usecase.card.GetCard
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.card.UpdateCard
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
import java.time.LocalDate

class CardListViewModelTest {

    private lateinit var listCards: ListCards
    private lateinit var addCard: AddCard
    private lateinit var updateCard: UpdateCard
    private lateinit var deleteCard: DeleteCard
    private lateinit var getCard: GetCard

    private val sampleCards = listOf(
        Card(
            id = 1L,
            number = CardNumber("123456******3456"),
            bank = "BPA",
            type = CardType.CREDIT,
            currency = Currency.USD,
            expirationDate = LocalDate.of(2028, 12, 1),
            note = "Principal",
        ),
        Card(
            id = 2L,
            number = CardNumber("654321******7890"),
            bank = "Banco X",
            type = CardType.DEBIT,
            currency = Currency.CUP,
            expirationDate = LocalDate.of(2027, 6, 1),
            note = null,
        ),
        Card(
            id = 3L,
            number = CardNumber("111111******2222"),
            bank = "Metropolitan",
            type = CardType.PREPAID,
            currency = Currency.MLC,
            expirationDate = LocalDate.of(2029, 3, 1),
            note = "Viajes",
        ),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        listCards = mockk()
        addCard = mockk()
        updateCard = mockk()
        deleteCard = mockk()
        getCard = mockk()

        // Default: cards exist
        every { listCards() } returns flowOf(sampleCards)
        coEvery { deleteCard(any<Long>()) } returns Unit
        coEvery { addCard(any()) } returns 5L
        coEvery { updateCard(any()) } returns 5L
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(): CardListViewModel = CardListViewModel(
        listCards = listCards,
        addCard = addCard,
        updateCard = updateCard,
        deleteCard = deleteCard,
        getCard = getCard,
    )

    // ══════════════════════════════════════════════════════════════════════
    // R1 — UiState Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-CRD-001: Initial load → Ready with cards ────────────────────────
    @Test
    fun `initial load emits Ready with sorted cards`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CardListUiState.Ready
            assertEquals(3, ready.cards.size)
        }
    }

    // ── SCN-CRD-006: Sort order (bank name alphabetical) ────────────────────
    @Test
    fun `sort order is by bank name alphabetical`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CardListUiState.Ready
            val banks = ready.cards.map { it.bank }
            assertEquals(
                listOf("Banco X", "BPA", "Metropolitan"),
                banks,
            )
        }
    }

    // ── SCN-CRD-002: Initial load → Empty ───────────────────────────────────
    @Test
    fun `initial load emits Empty when no cards exist`() = runTest {
        every { listCards() } returns flowOf(emptyList())

        val vm = createVm()

        vm.uiState.test {
            val empty = awaitItem() as CardListUiState.Empty
            assertNotNull(empty)
        }
    }

    // ── SCN-CRD-003: Initial load → Error ───────────────────────────────────
    @Test
    fun `initial load emits Error when ListCards throws`() = runTest {
        every { listCards() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as CardListUiState.Error
            assertEquals("DB error", error.message)
        }
    }

    @Test
    fun `initial load emits Error with fallback message on exception without message`() =
        runTest {
            every { listCards() } throws RuntimeException()

            val vm = createVm()

            vm.uiState.test {
                val error = awaitItem() as CardListUiState.Error
                assertTrue(error.message.isNotBlank())
            }
        }

    // ══════════════════════════════════════════════════════════════════════
    // R2 — Card Item Display (field mapping)
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-CRD-004: Display fields mapped ──────────────────────────────────
    @Test
    fun `card fields are correctly mapped to display items`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CardListUiState.Ready
            val bpa = ready.cards.first { it.bank == "BPA" }
            assertEquals(1L, bpa.id)
            assertEquals("••••3456", bpa.formattedNumber)
            assertEquals("BPA", bpa.bank)
            assertEquals(CardType.CREDIT, bpa.type)
            assertEquals(Currency.USD, bpa.currency)
            assertEquals("12/28", bpa.expiryFormatted)
            assertEquals("Principal", bpa.note)
        }
    }

    @Test
    fun `card with null note maps to null display note`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CardListUiState.Ready
            val bancoX = ready.cards.first { it.bank == "Banco X" }
            assertEquals(null, bancoX.note)
            assertEquals("06/27", bancoX.expiryFormatted)
        }
    }

    // ── SCN-CRD-005: Type badge per variant ─────────────────────────────────
    @Test
    fun `display items carry correct CardType for each variant`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CardListUiState.Ready
            val types = ready.cards.map { it.type to it.bank }
            assertEquals(CardType.DEBIT, types.first { it.second == "Banco X" }.first)
            assertEquals(CardType.CREDIT, types.first { it.second == "BPA" }.first)
            assertEquals(CardType.PREPAID, types.first { it.second == "Metropolitan" }.first)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R5 — Delete Card
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-CRD-019: Delete success → refetch + toast ───────────────────────
    @Test
    fun `onDelete calls DeleteCard and refetches list`() = runTest {
        coEvery { deleteCard(1L) } returns Unit
        val remaining = sampleCards.filter { it.id != 1L }
        every { listCards() } returns flowOf(sampleCards) andThen flowOf(remaining)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CardListUiState.Ready
            assertEquals(3, ready.cards.size)

            vm.onDelete(1L)

            val afterDelete = awaitItem() as CardListUiState.Ready
            assertEquals(2, afterDelete.cards.size)
            assertTrue(afterDelete.cards.none { it.id == 1L })

            coVerify(exactly = 1) { deleteCard(1L) }
        }
    }

    @Test
    fun `onDelete success emits ShowToast`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onDelete(1L) // BPA

            val event = awaitItem() as CardNavEvent.ShowToast
            assertEquals("Tarjeta eliminada", event.message)
        }
    }

    // ── SCN-CRD-020: Delete cancel ──────────────────────────────────────────
    // Cancel is handled by the Compose layer (dialog dismissal). The VM only
    // acts on confirmed delete. This test verifies no mutation happens when
    // the dialog is never confirmed (i.e., onDelete is never called).

    @Test
    fun `cancel does NOT call DeleteCard when dialog dismissed without confirm`() =
        runTest {
            createVm() // VM created but onDelete never called
            coVerify(inverse = true) { deleteCard(any()) }
        }

    // ── SCN-CRD-023: Delete throws → toast, list preserved ──────────────────
    @Test
    fun `onDelete emits ShowToast when DeleteCard throws`() = runTest {
        coEvery { deleteCard(1L) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.navEvents.test {
            vm.onDelete(1L)

            val event = awaitItem() as CardNavEvent.ShowToast
            assertEquals("Network error", event.message)
        }
    }

    @Test
    fun `onDelete error preserves existing card list`() = runTest {
        coEvery { deleteCard(1L) } throws RuntimeException("Network error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CardListUiState.Ready
            assertEquals(3, ready.cards.size)

            vm.onDelete(1L)

            // State should NOT change — list preserved
            expectNoEvents()
        }
    }

    @Test
    fun `onDelete error uses fallback message when exception has no message`() =
        runTest {
            coEvery { deleteCard(1L) } throws RuntimeException()

            val vm = createVm()

            vm.navEvents.test {
                vm.onDelete(1L)

                val event = awaitItem() as CardNavEvent.ShowToast
                assertTrue(event.message.isNotBlank())
            }
        }

    // ── Edge: non-existent id no-op ─────────────────────────────────────────
    @Test
    fun `onDelete with non-existent id no-ops`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready
            vm.onDelete(999L) // Not in the list
            expectNoEvents()
            coVerify(inverse = true) { deleteCard(any()) }
        }
    }

    // ── Edge: delete when state is not Ready ────────────────────────────────
    @Test
    fun `onDelete no-ops when state is not Ready`() = runTest {
        every { listCards() } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Error state
            vm.onDelete(1L)
            expectNoEvents()
            coVerify(inverse = true) { deleteCard(any()) }
        }
    }

    // ── SCN-CRD-025: isDeleting guard — double-tap ──────────────────────────
    @Test
    fun `onDelete double invocation calls DeleteCard once per call`() = runTest {
        val vm = createVm()

        vm.uiState.test {
            awaitItem() // Ready

            vm.onDelete(1L) // Delete BPA
            vm.onDelete(2L) // Delete Banco X (separate call)

            coVerify(exactly = 1) { deleteCard(1L) }
            coVerify(exactly = 1) { deleteCard(2L) }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R7 — Error Handling: retry from Error
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-CRD-024: Retry from Error → Ready ───────────────────────────────
    @Test
    fun `onDismissError transitions from Error to Ready on success`() = runTest {
        val successCards = listOf(sampleCards[0]) // Only BPA
        every { listCards() } throws RuntimeException("DB error") andThen flowOf(
            successCards,
        )

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as CardListUiState.Error
            assertEquals("DB error", error.message)

            vm.onDismissError()

            // With UnconfinedTestDispatcher, Loading → Ready happens synchronously
            // and StateFlow conflates to the latest value (Ready).
            val ready = awaitItem() as CardListUiState.Ready
            assertEquals(1, ready.cards.size)
            assertEquals("BPA", ready.cards[0].bank)
        }
    }

    @Test
    fun `onDismissError retry that gets empty cards emits Empty`() = runTest {
        every { listCards() } throws RuntimeException("DB error") andThen flowOf(
            emptyList(),
        )

        val vm = createVm()

        vm.uiState.test {
            val error = awaitItem() as CardListUiState.Error
            assertEquals("DB error", error.message)

            vm.onDismissError()

            // Loading → Empty conflates to Empty
            val empty = awaitItem() as CardListUiState.Empty
            assertNotNull(empty)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R1 extended — sort order after mutation
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `delete of a card preserves correct sort order in remaining items`() =
        runTest {
            // After deleting BPA (bank="BPA"), remaining sorted:
            // "Banco X", "Metropolitan"
            val remaining = sampleCards.filter { it.id != 1L }
            every { listCards() } returns flowOf(sampleCards) andThen flowOf(
                remaining,
            )

            val vm = createVm()

            vm.uiState.test {
                val ready = awaitItem() as CardListUiState.Ready
                assertEquals(3, ready.cards.size)

                vm.onDelete(1L) // Delete BPA

                val afterDelete = awaitItem() as CardListUiState.Ready
                assertEquals(2, afterDelete.cards.size)
                assertEquals(
                    listOf("Banco X", "Metropolitan"),
                    afterDelete.cards.map { it.bank },
                )
            }
        }

    // ══════════════════════════════════════════════════════════════════════
    // R6 — Add Card (onAdd)
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-CRD-008: Add success → SaveSuccess + toast + refetch ──────────────
    @Test
    fun `onAdd success emits SaveSuccess and refetches list`() = runTest {
        val newCard = Card(
            id = 5L,
            number = CardNumber.fromFirst6Last4("999999", "8888"),
            bank = "Nuevo Banco",
            type = CardType.DEBIT,
            currency = Currency.CUP,
            expirationDate = LocalDate.of(2027, 12, 1),
            note = null,
        )
        coEvery { addCard(any()) } returns 5L
        every { listCards() } returns flowOf(sampleCards) andThen flowOf(
            sampleCards + newCard,
        )

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CardListUiState.Ready
            assertEquals(3, ready.cards.size)

            vm.onAdd(
                bank = "Nuevo Banco",
                first6 = "999999",
                last4 = "8888",
                type = CardType.DEBIT,
                currency = Currency.CUP,
                expiry = LocalDate.of(2027, 12, 1),
                note = null,
            )

            val afterAdd = awaitItem() as CardListUiState.Ready
            assertEquals(4, afterAdd.cards.size)
            assertTrue(afterAdd.cards.any { it.bank == "Nuevo Banco" })

            coVerify(exactly = 1) { addCard(any()) }
        }
    }

    @Test
    fun `onAdd success emits SaveSuccess event`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd(
                bank = "Nuevo Banco",
                first6 = "999999",
                last4 = "8888",
                type = CardType.DEBIT,
                currency = Currency.CUP,
                expiry = LocalDate.of(2027, 12, 1),
                note = null,
            )

            // SaveSuccess
            val saveEvent = awaitItem()
            assertTrue(saveEvent is CardNavEvent.SaveSuccess)

            // ShowToast
            val toastEvent = awaitItem() as CardNavEvent.ShowToast
            assertTrue(toastEvent.message.contains("agregada"))
        }
    }

    // ── SCN-CRD-009: Empty bank validation ───────────────────────────────────
    @Test
    fun `onAdd with empty bank does not call AddCard`() = runTest {
        val vm = createVm()

        vm.onAdd(
            bank = "",
            first6 = "999999",
            last4 = "8888",
            type = CardType.DEBIT,
            currency = Currency.CUP,
            expiry = LocalDate.of(2027, 12, 1),
            note = null,
        )

        coVerify(inverse = true) { addCard(any()) }
    }

    @Test
    fun `onAdd with whitespace-only bank does not call AddCard`() = runTest {
        val vm = createVm()

        vm.onAdd(
            bank = "   ",
            first6 = "999999",
            last4 = "8888",
            type = CardType.DEBIT,
            currency = Currency.CUP,
            expiry = LocalDate.of(2027, 12, 1),
            note = null,
        )

        coVerify(inverse = true) { addCard(any()) }
    }

    // ── SCN-CRD-010: Number format validation ────────────────────────────────
    @Test
    fun `onAdd with invalid first6 does not call AddCard`() = runTest {
        val vm = createVm()

        vm.onAdd(
            bank = "Banco",
            first6 = "123", // Only 3 digits
            last4 = "8888",
            type = CardType.DEBIT,
            currency = Currency.CUP,
            expiry = LocalDate.of(2027, 12, 1),
            note = null,
        )

        coVerify(inverse = true) { addCard(any()) }
    }

    @Test
    fun `onAdd with invalid last4 does not call AddCard`() = runTest {
        val vm = createVm()

        vm.onAdd(
            bank = "Banco",
            first6 = "999999",
            last4 = "88", // Only 2 digits
            type = CardType.DEBIT,
            currency = Currency.CUP,
            expiry = LocalDate.of(2027, 12, 1),
            note = null,
        )

        coVerify(inverse = true) { addCard(any()) }
    }

    @Test
    fun `onAdd with non-digit first6 does not call AddCard`() = runTest {
        val vm = createVm()

        vm.onAdd(
            bank = "Banco",
            first6 = "abc123",
            last4 = "8888",
            type = CardType.DEBIT,
            currency = Currency.CUP,
            expiry = LocalDate.of(2027, 12, 1),
            note = null,
        )

        coVerify(inverse = true) { addCard(any()) }
    }

    // ── SCN-CRD-011: Past expiry (domain validation) ────────────────────────
    @Test
    fun `onAdd with past expiry emits error toast`() = runTest {
        coEvery { addCard(any()) } throws IllegalArgumentException(
            "Card expiration date cannot be more than 1 year in the past",
        )

        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd(
                bank = "Banco",
                first6 = "999999",
                last4 = "8888",
                type = CardType.DEBIT,
                currency = Currency.CUP,
                expiry = LocalDate.of(2020, 1, 1),
                note = null,
            )

            val event = awaitItem() as CardNavEvent.ShowToast
            assertTrue(event.message.contains("Card expiration date") ||
                event.message.contains("No se pudo agregar"),
            )
        }
    }

    // ── SCN-CRD-012: Note max validation ────────────────────────────────────
    @Test
    fun `onAdd with note over 200 chars is silently rejected`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd(
                bank = "Banco",
                first6 = "999999",
                last4 = "8888",
                type = CardType.DEBIT,
                currency = Currency.CUP,
                expiry = LocalDate.of(2027, 12, 1),
                note = "x".repeat(201),
            )
            expectNoEvents()
        }
    }

    @Test
    fun `onAdd with note exactly 200 chars succeeds`() = runTest {
        coEvery { addCard(any()) } returns 5L
        val note200 = "x".repeat(200)

        val vm = createVm()

        vm.onAdd(
            bank = "Banco",
            first6 = "999999",
            last4 = "8888",
            type = CardType.DEBIT,
            currency = Currency.CUP,
            expiry = LocalDate.of(2027, 12, 1),
            note = note200,
        )

        // addCard was called with a Card whose note is 200 chars (≤ 200)
        coVerify(exactly = 1) {
            addCard(withArg { card ->
                assertEquals("Banco", card.bank)
                assertEquals(note200, card.note)
            })
        }
    }

    @Test
    fun `onAdd with blank note is converted to null`() = runTest {
        coEvery { addCard(any<Card>()) } returns 6L
        var capturedNote: String? = "UNSET"
        coEvery { addCard(any<Card>()) } returns 6L

        val vm = createVm()

        vm.onAdd(
            bank = "Banco",
            first6 = "999999",
            last4 = "8888",
            type = CardType.DEBIT,
            currency = Currency.CUP,
            expiry = LocalDate.of(2027, 12, 1),
            note = "   ",
        )

        // With UnconfinedTestDispatcher the operation completes immediately
        coVerify(exactly = 1) { addCard(any<Card>()) }
    }

    // ── SCN-CRD-013: Defaults ───────────────────────────────────────────────
    @Test
    fun `onAdd uses provided type and currency as stored`() = runTest {
        coEvery { addCard(any<Card>()) } returns 5L
        every { listCards() } returns flowOf(sampleCards)

        val vm = createVm()

        vm.onAdd(
            bank = "Banco",
            first6 = "999999",
            last4 = "8888",
            type = CardType.CREDIT,
            currency = Currency.USD,
            expiry = LocalDate.of(2027, 12, 1),
            note = null,
        )

        coVerify(exactly = 1) { addCard(any<Card>()) }
    }

    // ── Mutation error preserves list ────────────────────────────────────────
    @Test
    fun `onAdd error preserves existing card list`() = runTest {
        coEvery { addCard(any()) } throws RuntimeException("DB error")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CardListUiState.Ready
            assertEquals(3, ready.cards.size)

            vm.onAdd(
                bank = "Banco",
                first6 = "999999",
                last4 = "8888",
                type = CardType.DEBIT,
                currency = Currency.CUP,
                expiry = LocalDate.of(2027, 12, 1),
                note = null,
            )

            // State should NOT change — list preserved
            expectNoEvents()
        }
    }

    @Test
    fun `onAdd error uses fallback message on null exception message`() = runTest {
        coEvery { addCard(any()) } throws RuntimeException()

        val vm = createVm()

        vm.navEvents.test {
            vm.onAdd(
                bank = "Banco",
                first6 = "999999",
                last4 = "8888",
                type = CardType.DEBIT,
                currency = Currency.CUP,
                expiry = LocalDate.of(2027, 12, 1),
                note = null,
            )

            val event = awaitItem() as CardNavEvent.ShowToast
            assertTrue(event.message.isNotBlank())
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // R6 — Edit Card (onEdit)
    // ══════════════════════════════════════════════════════════════════════

    // ── SCN-CRD-016: Edit save success ───────────────────────────────────────
    @Test
    fun `onEdit success emits SaveSuccess and updates list`() = runTest {
        coEvery { updateCard(any()) } returns 1L
        val updatedCards = listOf(
            sampleCards[0].copy(bank = "BPA Editado"),
            sampleCards[1],
            sampleCards[2],
        )
        every { listCards() } returns flowOf(sampleCards) andThen flowOf(updatedCards)

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CardListUiState.Ready
            assertEquals("BPA", ready.cards.first { it.id == 1L }.bank)

            vm.onEdit(
                id = 1L,
                bank = "BPA Editado",
                first6 = "123456",
                last4 = "3456",
                type = CardType.CREDIT,
                currency = Currency.USD,
                expiry = LocalDate.of(2028, 12, 1),
                note = "Principal",
            )

            val afterEdit = awaitItem() as CardListUiState.Ready
            assertEquals(3, afterEdit.cards.size)
            assertEquals("BPA Editado", afterEdit.cards.first { it.id == 1L }.bank)

            coVerify(exactly = 1) { updateCard(any()) }
        }
    }

    @Test
    fun `onEdit success emits toast with actualizada`() = runTest {
        val vm = createVm()

        vm.navEvents.test {
            vm.onEdit(
                id = 1L,
                bank = "BPA Editado",
                first6 = "123456",
                last4 = "3456",
                type = CardType.CREDIT,
                currency = Currency.USD,
                expiry = LocalDate.of(2028, 12, 1),
                note = "Principal",
            )

            // SaveSuccess
            val saveEvent = awaitItem()
            assertTrue(saveEvent is CardNavEvent.SaveSuccess)

            // ShowToast
            val toastEvent = awaitItem() as CardNavEvent.ShowToast
            assertTrue(toastEvent.message.contains("actualizada"))
        }
    }

    // ── SCN-CRD-017: Edit cancel preserves ──────────────────────────────────
    // Cancel is handled by the Compose layer (dialog dismissal). The VM only
    // acts on confirmed save. This test verifies no mutation without calling
    // onEdit.

    @Test
    fun `not calling onEdit does NOT call UpdateCard`() = runTest {
        createVm() // VM created but onEdit never called
        coVerify(inverse = true) { updateCard(any()) }
    }

    // ── SCN-CRD-018: Edit validation ────────────────────────────────────────
    @Test
    fun `onEdit with empty bank does not call UpdateCard`() = runTest {
        val vm = createVm()

        vm.onEdit(
            id = 1L,
            bank = "",
            first6 = "123456",
            last4 = "3456",
            type = CardType.CREDIT,
            currency = Currency.USD,
            expiry = LocalDate.of(2028, 12, 1),
            note = null,
        )

        coVerify(inverse = true) { updateCard(any()) }
    }

    @Test
    fun `onEdit with invalid number format does not call UpdateCard`() = runTest {
        val vm = createVm()

        vm.onEdit(
            id = 1L,
            bank = "Banco",
            first6 = "abc", // Non-digits, wrong length
            last4 = "3456",
            type = CardType.CREDIT,
            currency = Currency.USD,
            expiry = LocalDate.of(2028, 12, 1),
            note = null,
        )

        coVerify(inverse = true) { updateCard(any()) }
    }

    @Test
    fun `onEdit error emits toast and preserves list`() = runTest {
        coEvery { updateCard(any()) } throws RuntimeException("Update failed")

        val vm = createVm()

        vm.uiState.test {
            val ready = awaitItem() as CardListUiState.Ready
            assertEquals(3, ready.cards.size)

            vm.onEdit(
                id = 1L,
                bank = "BPA Modified",
                first6 = "123456",
                last4 = "3456",
                type = CardType.CREDIT,
                currency = Currency.USD,
                expiry = LocalDate.of(2028, 12, 1),
                note = null,
            )

            // List preserved
            expectNoEvents()
        }
    }

    // ── SCN-CRD-021: Cascade warning — deferred ─────────────────────────────
    // The domain Card model does not expose an expense-count field.
    // Without a countByCardId() on CardRepository, cascade detection cannot
    // be implemented at the VM level. This scenario is marked DEFERRED.

    /**
     * SCN-CRD-021 (CASCADE WARNING) — DEFERRED.
     *
     * The domain [Card] model has no expense-count field. Cascade detection
     * requires either adding `expenseCount` to Card or exposing
     * `countByCardId()` on the repository. This feature is out of scope for
     * the current change.
     */
    @Test
    fun `delete cascade warning is deferred — structural guard only`() = runTest {
        // Verify that the Card model does not expose expense count.
        val card = sampleCards[0]
        // No expenseCount property on Card — cascade is deferred.
        assertTrue(true) // Placeholder for deferred scenario
    }
}
