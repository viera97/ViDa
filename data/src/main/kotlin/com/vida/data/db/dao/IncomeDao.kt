package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import com.vida.data.db.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `incomes` table.
 *
 * Queries mirror [ExpenseDao] but without category-filtering and without the
 * "real amount" delta math — incomes don't have categories and the stored
 * amount IS the received amount.
 */
@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes ORDER BY date_time DESC")
    fun observeAll(): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE id = :id")
    suspend fun getById(id: Long): IncomeEntity?

    /**
     * Incomes whose polymorphic destination matches [sourceType]/[sourceId].
     * For WALLET, [sourceId] is ignored (the wallet is addressed by
     * `destination_wallet_id`). Only rows with `date_time <= asOf` are
     * returned, newest first.
     */
    @Query(
        """
        SELECT * FROM incomes WHERE
            ((destination_card_id IS NOT NULL AND destination_card_id = :sourceId AND :sourceType = 'CARD') OR
             (destination_stash_id IS NOT NULL AND destination_stash_id = :sourceId AND :sourceType = 'STASH') OR
             (destination_wallet_id IS NOT NULL AND :sourceType = 'WALLET'))
            AND date_time <= :asOf
        ORDER BY date_time DESC
        """,
    )
    fun observeBySource(sourceType: String, sourceId: Long?, asOf: Long): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE date_time >= :from AND date_time < :to ORDER BY date_time DESC")
    fun observeByDateRange(from: Long, to: Long): Flow<List<IncomeEntity>>

    /**
     * Dynamic parametric query for combined search + filter + pagination.
     * Build a [SimpleSQLiteQuery] with WHERE clauses matching non-null filter
     * fields, then ORDER BY date_time DESC LIMIT ? OFFSET ?. Mirrors
     * [ExpenseDao.searchExpenses] but for the `incomes` table, with
     * `destination_wallet_id`/`destination_card_id`/`destination_stash_id`
     * as the polymorphic source columns.
     */
    @RawQuery
    suspend fun searchIncomes(query: SupportSQLiteQuery): List<IncomeEntity>

    @Upsert
    suspend fun upsert(entity: IncomeEntity): Long

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun delete(id: Long)
}
