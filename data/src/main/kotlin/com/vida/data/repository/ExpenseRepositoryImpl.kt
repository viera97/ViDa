package com.vida.data.repository

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.vida.data.db.AppDatabase
import com.vida.data.db.dao.CardDao
import com.vida.data.db.dao.ExpenseDao
import com.vida.data.db.dao.StashDao
import com.vida.data.db.dao.WalletDao
import com.vida.data.mapper.ExpenseMapper
import com.vida.data.mapper.util.amountMinorUnits
import com.vida.data.mapper.util.toEpochMillis
import com.vida.domain.model.Expense
import com.vida.domain.model.ExpenseFilter
import com.vida.domain.model.SourceType
import com.vida.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

/**
 * Room-backed [ExpenseRepository].
 *
 * `upsert` is wrapped in [database.withTransaction] so that the INSERT into the
 * `expenses` table and the ledger delta applied to the source's `balance_minor`
 * (Option C — wallet/card) are atomic: a failure mid-way rolls back both steps.
 * Stash source expenses skip the balance update — stashes have no stored
 * balance column; their balance is computed from transfers at read time.
 */
class ExpenseRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val dao: ExpenseDao,
    private val walletDao: WalletDao,
    private val cardDao: CardDao,
    private val stashDao: StashDao,
    private val mapper: ExpenseMapper,
) : ExpenseRepository {

    override fun getAll(): Flow<List<Expense>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): Expense? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun getBySource(
        sourceType: SourceType,
        sourceId: Long?,
        asOf: Instant,
    ): Flow<List<Expense>> =
        dao.observeBySource(sourceType.name, sourceId, asOf.toEpochMillis())
            .map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getByCategory(categoryId: Long, asOf: Instant): Flow<List<Expense>> =
        dao.observeByCategory(categoryId, asOf.toEpochMillis())
            .map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getByDateRange(from: Instant, to: Instant): Flow<List<Expense>> =
        dao.observeByDateRange(from.toEpochMillis(), to.toEpochMillis())
            .map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun searchExpenses(
        filter: ExpenseFilter,
        limit: Int,
        offset: Int,
    ): List<Expense> {
        val query = buildSearchQuery(filter, limit, offset)
        return dao.searchExpenses(query).map { mapper.toDomain(it) }
    }

    override suspend fun upsert(expense: Expense): Long =
        database.withTransaction {
            val newId = dao.upsert(mapper.toEntity(expense))
            // Ledger delta (Option C): reduce source balance by the actual charged
            // amount. We prefer `realAmount` over `amount` because `amount` can be
            // pre-discount (declared/planned), while `realAmount` is the receipt's
            // truth — what actually left the source. Falls back to `amount` if the
            // user did not specify a real amount.
            //
            // Stash expenses skip the balance update — stash balance is computed
            // from transfers, not stored. The wallet is a singleton (id = 1L) per
            // `ExpenseEntity`'s schema comment; for CARD the source row id comes
            // from `expense.sourceId`.
            val charged = expense.realAmount ?: expense.amount
            val delta = -charged.amountMinorUnits()
            when (expense.sourceType) {
                SourceType.WALLET -> walletDao.adjustBalance(WALLET_SINGLETON_ID, delta)
                SourceType.CARD -> cardDao.adjustBalance(expense.sourceId!!, delta)
                SourceType.STASH -> Unit
            }
            newId
        }

    override suspend fun delete(id: Long) = dao.delete(id)

    private companion object {
        /**
         * The single wallet row id used for wallet-sourced expenses. The wallet
         * is a singleton in the current schema (see `ExpenseEntity` doc) — every
         * WALLET expense writes `source_wallet_id = 1L`.
         */
        const val WALLET_SINGLETON_ID: Long = 1L
    }

    // ── private helpers ─────────────────────────────────────────────────────

    private fun buildSearchQuery(
        filter: ExpenseFilter,
        limit: Int,
        offset: Int,
    ): SimpleSQLiteQuery {
        val sb = StringBuilder("SELECT * FROM expenses")
        val args = mutableListOf<Any>()

        val clauses = buildWhereClauses(filter, args)

        if (clauses.isNotEmpty()) {
            sb.append(" WHERE ")
            sb.append(clauses.joinToString(" AND "))
        }

        sb.append(" ORDER BY date_time DESC LIMIT ? OFFSET ?")
        args.add(limit)
        args.add(offset)

        return SimpleSQLiteQuery(sb.toString(), args.toTypedArray())
    }

    private fun buildWhereClauses(
        filter: ExpenseFilter,
        args: MutableList<Any>,
    ): List<String> {
        val clauses = mutableListOf<String>()

        val dateFrom = filter.dateFrom
        if (dateFrom != null) {
            clauses.add("date_time >= ?")
            args.add(dateFrom.toEpochMillis())
        }
        val dateTo = filter.dateTo
        if (dateTo != null) {
            clauses.add("date_time < ?")
            args.add(dateTo.toEpochMillis())
        }
        val categoryIds = filter.categoryIds
        if (categoryIds != null && categoryIds.isNotEmpty()) {
            val placeholders = categoryIds.joinToString(", ") { "?" }
            clauses.add("category_id IN ($placeholders)")
            args.addAll(categoryIds)
        }
        val currency = filter.currency
        if (currency != null) {
            clauses.add("amount_currency = ?")
            args.add(currency.code)
        }
        val sourceType = filter.sourceType
        if (sourceType != null) {
            clauses.add(sourceTypeClause(sourceType))
        }
        val searchQuery = filter.searchQuery
        if (!searchQuery.isNullOrBlank()) {
            clauses.add("description LIKE '%' || ? || '%'")
            args.add(searchQuery)
        }

        return clauses
    }

    private fun sourceTypeClause(sourceType: SourceType): String = when (sourceType) {
        SourceType.WALLET -> "source_wallet_id IS NOT NULL"
        SourceType.CARD -> "source_card_id IS NOT NULL"
        SourceType.STASH -> "source_stash_id IS NOT NULL"
    }
}
