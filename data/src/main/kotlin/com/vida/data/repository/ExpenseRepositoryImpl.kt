package com.vida.data.repository

import com.vida.data.db.dao.ExpenseDao
import com.vida.data.mapper.ExpenseMapper
import com.vida.data.mapper.util.toEpochMillis
import com.vida.domain.model.Expense
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

    override suspend fun upsert(expense: Expense): Long =
        dao.upsert(mapper.toEntity(expense))

    override suspend fun delete(id: Long) = dao.delete(id)
}
