package com.vida.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vida.data.db.AppDatabase
import com.vida.data.db.dao.CardDao
import com.vida.data.db.dao.ExpenseDao
import com.vida.data.db.dao.StashDao
import com.vida.data.db.dao.WalletDao
import com.vida.data.db.entity.CardEntity
import com.vida.data.db.entity.CategoryEntity
import com.vida.data.db.entity.ExpenseEntity
import com.vida.data.db.entity.StashEntity
import com.vida.data.db.entity.WalletEntity
import com.vida.data.mapper.ExpenseMapper
import com.vida.data.mapper.util.amountMinorUnits
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import com.vida.domain.model.Expense
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/**
 * Unit tests for [ExpenseRepositoryImpl].
 *
 * Read paths (`getAll`, `getById`, `getBySource`, etc.) use mocked DAOs and a
 * relaxed mock for `AppDatabase` (none of those paths touch `database.withTransaction`).
 *
 * `upsert` exercises a real in-memory Room database because Room 2.7+ declares
 * `withTransaction` as `inline`, which makes it unreliable to mock via
 * `mockkStatic`. Using a real DB (same pattern as `TransferOrchestratorIntegrationTest`)
 * gives us end-to-end verification of the INSERT + ledger UPDATE atomicity,
 * including the new behavior where wallet/card balances are auto-adjusted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExpenseRepositoryImplTest {
    private val dao = mockk<ExpenseDao>(relaxed = true)
    private val walletDao = mockk<WalletDao>(relaxed = true)
    private val cardDao = mockk<CardDao>(relaxed = true)
    private val stashDao = mockk<StashDao>(relaxed = true)
    private val mapper = ExpenseMapper
    private val database = mockk<AppDatabase>(relaxed = true)
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setUp() {
        repository = ExpenseRepositoryImpl(database, dao, walletDao, cardDao, stashDao, mapper)
    }

    @After
    fun tearDown() {
        // No-op: `database` is a relaxed mock; the real in-memory DBs used by the
        // upsert tests are created and closed inside each test method.
    }

    @Test
    fun `getAll maps entities to domain models`() = runTest {
        val entity = anEntity()
        coEvery { dao.observeAll() } returns flowOf(listOf(entity))

        repository.getAll().test {
            val expenses = awaitItem()
            assertEquals(1, expenses.size)
            assertEquals("Lunch", expenses[0].description)
            assertEquals(SourceType.CARD, expenses[0].sourceType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns mapped expense when found`() = runTest {
        coEvery { dao.getById(1L) } returns anEntity()

        val expense = repository.getById(1L)
        assertEquals("Lunch", expense!!.description)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getById(any()) } returns null
        assertNull(repository.getById(42L))
    }

    @Test
    fun `getBySource delegates to dao with source type name and epoch millis`() = runTest {
        coEvery { dao.observeBySource(any(), any(), any()) } returns flowOf(listOf(anEntity()))

        repository.getBySource(SourceType.CARD, 5L, Instant.ofEpochMilli(1_000L)).test {
            val expenses = awaitItem()
            assertEquals(1, expenses.size)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { dao.observeBySource("CARD", 5L, 1_000L) }
    }

    @Test
    fun `getByCategory delegates to dao with epoch millis`() = runTest {
        coEvery { dao.observeByCategory(any(), any()) } returns flowOf(listOf(anEntity()))

        repository.getByCategory(3L, Instant.ofEpochMilli(9_000L)).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { dao.observeByCategory(3L, 9_000L) }
    }

    @Test
    fun `getByDateRange delegates to dao with epoch millis`() = runTest {
        coEvery { dao.observeByDateRange(any(), any()) } returns flowOf(listOf(anEntity()))

        repository.getByDateRange(Instant.ofEpochMilli(100L), Instant.ofEpochMilli(200L)).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { dao.observeByDateRange(100L, 200L) }
    }

    @Test
    fun `upsert with CARD source inserts expense and reduces card balance`() = runTest {
        // Real in-memory Room DB so that the INSERT + ledger UPDATE run end-to-end.
        val realDb = newInMemoryDb()
        try {
            val categoryId = realDb.categoryDao().upsert(
                CategoryEntity(name = "Comida", color = 0, icon = null, isSystem = 0),
            )
            val cardId = realDb.cardDao().upsert(
                CardEntity(
                    maskedNumber = "123456******7890",
                    bank = "POP",
                    type = CardType.DEBIT,
                    currency = "USD",
                    note = null,
                    expirationDate = LocalDate.of(2028, 12, 31),
                    balanceMinor = 50_00L,
                ),
            )

            val realRepository = ExpenseRepositoryImpl(
                realDb,
                realDb.expenseDao(),
                realDb.walletDao(),
                realDb.cardDao(),
                realDb.stashDao(),
                mapper,
            )
            val expense = anExpense().copy(sourceId = cardId, categoryId = categoryId)

            val id = realRepository.upsert(expense)

            assertTrue("Expected positive row id from upsert", id > 0L)
            // Expense row was persisted.
            val persisted = realDb.expenseDao().getById(id)
            assertNotNull(persisted)
            assertEquals(expense.amount.amountMinorUnits(), persisted!!.amountMinor)
            // Card balance was reduced by the expense amount (ledger delta).
            val updatedCard = realDb.cardDao().getById(cardId)!!
            assertEquals(50_00L - 1234L, updatedCard.balanceMinor)
        } finally {
            realDb.close()
        }
    }

    @Test
    fun `upsert with WALLET source reduces the singleton wallet balance`() = runTest {
        val realDb = newInMemoryDb()
        try {
            val categoryId = realDb.categoryDao().upsert(
                CategoryEntity(name = "Comida", color = 0, icon = null, isSystem = 0),
            )
            realDb.walletDao().upsert(
                WalletEntity(id = 1L, currency = "USD", balanceMinor = 10_000L),
            )

            val realRepository = ExpenseRepositoryImpl(
                realDb,
                realDb.expenseDao(),
                realDb.walletDao(),
                realDb.cardDao(),
                realDb.stashDao(),
                mapper,
            )
            val expense = Expense(
                id = 0L,
                categoryId = categoryId,
                amount = Money.of("25.00", Currency.USD),
                description = "Coffee",
                dateTime = Instant.ofEpochMilli(1_000L),
                sourceType = SourceType.WALLET,
                sourceId = null,
            )

            val id = realRepository.upsert(expense)

            assertTrue("Expected positive row id from upsert", id > 0L)
            val wallet = realDb.walletDao().getById(1L)!!
            assertEquals(10_000L - 2_500L, wallet.balanceMinor)
        } finally {
            realDb.close()
        }
    }

    @Test
    fun `upsert with STASH source does not touch any balance column`() = runTest {
        val realDb = newInMemoryDb()
        try {
            val categoryId = realDb.categoryDao().upsert(
                CategoryEntity(name = "Comida", color = 0, icon = null, isSystem = 0),
            )
            val stashId = realDb.stashDao().upsert(
                StashEntity(
                    name = "Emergency",
                    createdAt = Instant.ofEpochMilli(0L),
                    updatedAt = Instant.ofEpochMilli(0L),
                    currency = Currency.USD,
                ),
            )

            val realRepository = ExpenseRepositoryImpl(
                realDb,
                realDb.expenseDao(),
                realDb.walletDao(),
                realDb.cardDao(),
                realDb.stashDao(),
                mapper,
            )
            val expense = Expense(
                id = 0L,
                categoryId = categoryId,
                amount = Money.of("15.00", Currency.USD),
                description = "Groceries",
                dateTime = Instant.ofEpochMilli(1_000L),
                sourceType = SourceType.STASH,
                sourceId = stashId,
            )

            val id = realRepository.upsert(expense)

            assertTrue("Expected positive row id from upsert", id > 0L)
            // No balance mutation expected — stash balance is computed at read time.
            // The expense row exists; that is the assertion.
            val persisted = realDb.expenseDao().getById(id)
            assertNotNull(persisted)
        } finally {
            realDb.close()
        }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(1L)
        coVerify { dao.delete(1L) }
    }

    private fun newInMemoryDb(): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

    private fun anEntity() = ExpenseEntity(
        id = 1L,
        categoryId = 2L,
        amountMinor = 1234L,
        amountCurrency = "USD",
        realAmountMinor = null,
        realAmountCurrency = null,
        description = "Lunch",
        dateTime = 1_000L,
        note = null,
        sourceWalletId = null,
        sourceCardId = 5L,
        sourceStashId = null,
    )

    private fun anExpense() = Expense(
        id = 0L,
        categoryId = 2L,
        amount = Money.of("12.34", Currency.USD),
        description = "Lunch",
        dateTime = Instant.ofEpochMilli(1_000L),
        sourceType = SourceType.CARD,
        sourceId = 5L,
    )
}