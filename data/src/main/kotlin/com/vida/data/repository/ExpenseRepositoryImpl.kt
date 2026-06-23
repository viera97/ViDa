package com.vida.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.vida.data.db.dao.ExpenseDao
import com.vida.data.mapper.ExpenseMapper
import com.vida.data.mapper.util.toEpochMillis
import com.vida.domain.model.Expense
import com.vida.domain.model.ExpenseFilter
import com.vida.domain.model.SourceType
import com.vida.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao,
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
        dao.upsert(mapper.toEntity(expense))

    override suspend fun delete(id: Long) = dao.delete(id)

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
