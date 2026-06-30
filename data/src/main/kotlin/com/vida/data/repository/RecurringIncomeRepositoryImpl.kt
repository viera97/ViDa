package com.vida.data.repository

import com.vida.data.db.dao.RecurringIncomeDao
import com.vida.data.mapper.RecurringIncomeMapper
import com.vida.domain.model.RecurringIncome
import com.vida.domain.repository.RecurringIncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Room-backed [RecurringIncomeRepository]. Delegates reactive queries to
 * [RecurringIncomeDao] at SQL level. [getDue] delegates to `observeActive`
 * which returns active templates within the eligible date window; the domain
 * use case further filters by computing the next due date from `startDate` +
 * `frequency` + `lastGeneratedDate`.
 */
class RecurringIncomeRepositoryImpl @Inject constructor(
    private val dao: RecurringIncomeDao,
    private val mapper: RecurringIncomeMapper,
) : RecurringIncomeRepository {

    override fun getAll(): Flow<List<RecurringIncome>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): RecurringIncome? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun getDue(asOf: LocalDate): Flow<List<RecurringIncome>> =
        dao.observeActive(asOf.toEpochDay())
            .map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun upsert(recurring: RecurringIncome): Long =
        dao.upsert(mapper.toEntity(recurring))

    override suspend fun delete(id: Long) = dao.delete(id)
}
