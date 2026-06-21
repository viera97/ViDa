package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vida.data.db.entity.RefundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RefundDao {
    @Query("SELECT * FROM refunds ORDER BY date_time DESC")
    fun observeAll(): Flow<List<RefundEntity>>

    @Query("SELECT * FROM refunds WHERE id = :id")
    suspend fun getById(id: Long): RefundEntity?

    @Query("SELECT * FROM refunds WHERE original_expense_id = :expenseId")
    fun observeByOriginalExpense(expenseId: Long): Flow<List<RefundEntity>>

    /**
     * Inserts a new refund row. Uses ABORT so a duplicate `original_expense_id`
     * (UNIQUE index) throws [android.database.sqlite.SQLiteConstraintException],
     * which the repository re-throws as `IllegalStateException`. An explicit
     * [update] path handles edits of an existing refund by primary key.
     *
     * Note: `@Upsert` is intentionally avoided here because its internal
     * conflict-resolution (INSERT OR IGNORE/REPLACE fallback) can silently
     * swallow a non-PK UNIQUE conflict, which would break the one-refund-per-expense
     * guarantee the spec requires.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RefundEntity): Long

    @Update
    suspend fun update(entity: RefundEntity)

    @Query("DELETE FROM refunds WHERE id = :id")
    suspend fun delete(id: Long)
}
