package com.vida.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.vida.data.db.dao.RefundDao
import com.vida.data.mapper.RefundMapper
import com.vida.domain.model.Refund
import com.vida.domain.repository.RefundRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed [RefundRepository]. The `refunds` table has a UNIQUE index on
 * `original_expense_id`; a duplicate upsert surfaces as [SQLiteConstraintException],
 * which is re-thrown as [IllegalStateException] with a clear message per SCN-DATA-PR2-005.
 */
class RefundRepositoryImpl @Inject constructor(
    private val dao: RefundDao,
    private val mapper: RefundMapper,
) : RefundRepository {

    override fun getAll(): Flow<List<Refund>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): Refund? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun getByOriginalExpense(expenseId: Long): Flow<List<Refund>> =
        dao.observeByOriginalExpense(expenseId)
            .map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun upsert(refund: Refund): Long = try {
        if (refund.id == 0L) {
            dao.insert(mapper.toEntity(refund))
        } else {
            dao.update(mapper.toEntity(refund))
            refund.id
        }
    } catch (e: SQLiteConstraintException) {
        throw IllegalStateException(
            "Refund already exists for expense ${refund.originalExpenseId}", e,
        )
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}
