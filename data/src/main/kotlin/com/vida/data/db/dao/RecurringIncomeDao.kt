package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vida.data.db.entity.RecurringIncomeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `recurring_incomes` table — templates for recurring incomes.
 *
 * [observeActive] returns active templates whose `start_date <= asOf` and whose
 * `end_date` (if set) is `>= asOf`. The caller (use case) further filters by
 * computing the next due date from `startDate` + `frequency` + `lastGeneratedDate`.
 */
@Dao
interface RecurringIncomeDao {

    @Query("SELECT * FROM recurring_incomes ORDER BY start_date ASC")
    fun observeAll(): Flow<List<RecurringIncomeEntity>>

    @Query("SELECT * FROM recurring_incomes WHERE id = :id")
    suspend fun getById(id: Long): RecurringIncomeEntity?

    /**
     * Active templates within the eligible date window as of [asOf] (epoch day):
     * `is_active = 1 AND start_date <= asOf AND (end_date IS NULL OR end_date >= asOf)`.
     */
    @Query(
        """
        SELECT * FROM recurring_incomes
        WHERE is_active = 1 AND start_date <= :asOf
            AND (end_date IS NULL OR end_date >= :asOf)
        ORDER BY start_date ASC
        """,
    )
    fun observeActive(asOf: Long): Flow<List<RecurringIncomeEntity>>

    @Upsert
    suspend fun upsert(entity: RecurringIncomeEntity): Long

    @Query("DELETE FROM recurring_incomes WHERE id = :id")
    suspend fun delete(id: Long)
}
