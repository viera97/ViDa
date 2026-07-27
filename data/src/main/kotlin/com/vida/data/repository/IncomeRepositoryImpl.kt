package com.vida.data.repository

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.vida.data.db.AppDatabase
import com.vida.data.db.dao.CardDao
import com.vida.data.db.dao.IncomeDao
import com.vida.data.db.dao.StashDao
import com.vida.data.db.dao.WalletDao
import com.vida.data.mapper.IncomeMapper
import com.vida.data.mapper.util.amountMinorUnits
import com.vida.data.mapper.util.toEpochMillis
import com.vida.domain.model.Income
import com.vida.domain.model.IncomeFilter
import com.vida.domain.model.SourceType
import com.vida.domain.model.aggregate.CurrencyTotal
import com.vida.domain.model.aggregate.PeriodIncomeTotal
import com.vida.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

/**
 * Room-backed [IncomeRepository].
 *
 * `upsert` is wrapped in [database.withTransaction] so that the INSERT into the
 * `incomes` table and the ledger delta applied to the destination's
 * `balance_minor` are atomic: a failure mid-way rolls back both steps.
 *
 * **Income auto-updates balances** (opposite of expense):
 * - WALLET / CARD destination → `walletDao.adjustBalance(id, +delta)` /
 *   `cardDao.adjustBalance(id, +delta)` (positive delta).
 * - STASH destination → no stored column to adjust. The income row IS the
 *   ledger entry; [com.vida.data.db.dao.BalanceDao] adds it as a positive term
 *   to the stash balance SQL at read time, the same way it does for transfers.
 */
class IncomeRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val dao: IncomeDao,
    private val walletDao: WalletDao,
    private val cardDao: CardDao,
    private val stashDao: StashDao,
    private val mapper: IncomeMapper,
) : IncomeRepository {

    override fun getAll(): Flow<List<Income>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): Income? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun getBySource(
        sourceType: SourceType,
        sourceId: Long?,
        asOf: Instant,
    ): Flow<List<Income>> =
        dao.observeBySource(sourceType.name, sourceId, asOf.toEpochMillis())
            .map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getByDateRange(from: Instant, to: Instant): Flow<List<Income>> =
        dao.observeByDateRange(from.toEpochMillis(), to.toEpochMillis())
            .map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun upsert(income: Income): Long =
        database.withTransaction {
            val newId = dao.upsert(mapper.toEntity(income))
            // Credit the destination's stored balance (Option C). Stash side is
            // skipped — the income row itself acts as the ledger entry, picked
            // up by BalanceDao SQL at read time.
            val delta = income.amount.amountMinorUnits()
            when (income.sourceType) {
                SourceType.WALLET -> walletDao.adjustBalance(income.sourceId!!, delta)
                SourceType.CARD -> cardDao.adjustBalance(income.sourceId!!, delta)
                SourceType.STASH -> Unit
            }
            newId
        }

    // ── Aggregation methods for statistics ─────────────────────────────────

    override suspend fun getIncomeTotalsByPeriod(
        from: Instant,
        to: Instant,
        bucketMillis: Long,
    ): List<PeriodIncomeTotal> =
        dao.getIncomeTotalsByPeriod(from.toEpochMillis(), to.toEpochMillis(), bucketMillis)

    override suspend fun getIncomeTotalsByCurrency(from: Instant, to: Instant): List<CurrencyTotal> =
        dao.getIncomeTotalsByCurrency(from.toEpochMillis(), to.toEpochMillis())

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun searchIncomes(
        filter: IncomeFilter,
        limit: Int,
        offset: Int,
    ): List<Income> {
        val query = buildSearchQuery(filter, limit, offset)
        return dao.searchIncomes(query).map { mapper.toDomain(it) }
    }

    // ── private helpers ─────────────────────────────────────────────────────

    private fun buildSearchQuery(
        filter: IncomeFilter,
        limit: Int,
        offset: Int,
    ): SimpleSQLiteQuery {
        val sb = StringBuilder("SELECT * FROM incomes")
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
        filter: IncomeFilter,
        args: MutableList<Any>,
    ): List<String> {
        val clauses = mutableListOf<String>()

        filter.dateFrom?.let {
            clauses.add("date_time >= ?")
            args.add(it.toEpochMillis())
        }
        filter.dateTo?.let {
            clauses.add("date_time < ?")
            args.add(it.toEpochMillis())
        }
        filter.currency?.takeIf { it.isNotBlank() }?.let {
            clauses.add("amount_currency = ?")
            args.add(it)
        }
        filter.sourceType?.let {
            clauses.add(
                when (it) {
                    SourceType.WALLET -> "destination_wallet_id IS NOT NULL"
                    SourceType.CARD -> "destination_card_id IS NOT NULL"
                    SourceType.STASH -> "destination_stash_id IS NOT NULL"
                },
            )
        }
        filter.searchQuery?.let {
            if (it.isNotBlank()) {
                clauses.add("description LIKE '%' || ? || '%'")
                args.add(it)
            }
        }

        return clauses
    }
}
