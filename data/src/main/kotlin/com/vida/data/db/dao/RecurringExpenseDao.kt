package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vida.data.db.entity.RecurringExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `recurring_expenses` table — templates for recurring expenses.
 *
 * [observeActive] returns active templates whose `start_date <= asOf` and whose
 * `end_date` (if set) is `>= asOf`. The caller (use case) further filters by
 * computing the next due date from `startDate` + `frequency` + `lastGeneratedDate`.
 */
@Dao
interface RecurringExpenseDao {

    @Query("SELECT * FROM recurring_expenses ORDER BY start_date ASC")
    fun observeAll(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getById(id: Long): RecurringExpenseEntity?

    /**
     * Active templates within the eligible date window as of [asOf] (epoch day):
     * `is_active = 1 AND start_date <= asOf AND (end_date IS NULL OR end_date >= asOf)`.
     *
     * The domain `RecurringExpenseRepository.getDue` delegates to this query. The
     * actual "next due date" is computed in the domain layer (it requires
     * frequency-based date math not expressible in SQL).
     */
    @Query(
        """
        SELECT * FROM recurring_expenses
        WHERE is_active = 1 AND start_date <= :asOf
            AND (end_date IS NULL OR end_date >= :asOf)
        ORDER BY start_date ASC
        """,
    )
    fun observeActive(asOf: Long): Flow<List<RecurringExpenseEntity>>

    @Upsert
    suspend fun upsert(entity: RecurringExpenseEntity): Long

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE recurring_expenses SET is_active = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)
}
