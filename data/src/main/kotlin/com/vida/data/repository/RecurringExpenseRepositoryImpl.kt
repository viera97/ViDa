package com.vida.data.repository

import com.vida.data.db.dao.RecurringExpenseDao
import com.vida.data.mapper.RecurringExpenseMapper
import com.vida.domain.model.RecurringExpense
import com.vida.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Room-backed [RecurringExpenseRepository]. Delegates reactive queries to
 * [RecurringExpenseDao] at SQL level. [getDue] delegates to `observeActive`
 * which returns active templates within the eligible date window; the domain
 * use case further filters by computing the next due date from `startDate` +
 * `frequency` + `lastGeneratedDate`.
 */
class RecurringExpenseRepositoryImpl @Inject constructor(
    private val dao: RecurringExpenseDao,
    private val mapper: RecurringExpenseMapper,
) : RecurringExpenseRepository {

    override fun getAll(): Flow<List<RecurringExpense>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): RecurringExpense? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun getDue(asOf: LocalDate): Flow<List<RecurringExpense>> =
        dao.observeActive(asOf.toEpochDay())
            .map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun upsert(recurring: RecurringExpense): Long =
        dao.upsert(mapper.toEntity(recurring))

    override suspend fun delete(id: Long) = dao.delete(id)
}
