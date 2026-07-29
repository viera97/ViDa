package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import com.vida.data.db.entity.ExpenseEntity
import com.vida.domain.model.aggregate.CategoryExpenseTotal
import com.vida.domain.model.aggregate.CurrencyTotal
import com.vida.domain.model.aggregate.PeriodCategoryExpenseTotal
import com.vida.domain.model.aggregate.PeriodExpenseTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date_time DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    /**
     * Expenses whose polymorphic source matches [sourceType]/[sourceId]. For WALLET,
     * [sourceId] is ignored (the wallet is a singleton addressed by `source_wallet_id`).
     * Only rows with `date_time <= asOf` are returned, newest first.
     */
    @Query(
        """
        SELECT * FROM expenses WHERE
            ((source_card_id IS NOT NULL AND source_card_id = :sourceId AND :sourceType = 'CARD') OR
             (source_stash_id IS NOT NULL AND source_stash_id = :sourceId AND :sourceType = 'STASH') OR
             (source_wallet_id IS NOT NULL AND :sourceType = 'WALLET'))
            AND date_time <= :asOf
        ORDER BY date_time DESC
        """,
    )
    fun observeBySource(sourceType: String, sourceId: Long?, asOf: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE category_id = :categoryId AND date_time <= :asOf ORDER BY date_time DESC")
    fun observeByCategory(categoryId: Long, asOf: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date_time >= :from AND date_time < :to ORDER BY date_time DESC")
    fun observeByDateRange(from: Long, to: Long): Flow<List<ExpenseEntity>>

    /**
     * Dynamic parametric query. Build a [SupportSQLiteQuery] (typically [SimpleSQLiteQuery])
     * with WHERE clauses matching non-null filter fields, then ORDER BY date_time DESC.
     * The caller appends `LIMIT ? OFFSET ?` at the end of the SQL and binds the respective
     * integer parameters.
     */
    @RawQuery
    suspend fun searchExpenses(query: SupportSQLiteQuery): List<ExpenseEntity>

    // ── Aggregation queries for statistics ─────────────────────────────────

    @Query(
        """
        SELECT category_id AS categoryId,
               amount_currency AS currency,
               COALESCE(SUM(COALESCE(real_amount_minor, amount_minor)), 0) AS totalMinor
        FROM expenses
        WHERE date_time >= :from AND date_time < :to
        GROUP BY category_id, amount_currency
        """,
    )
    suspend fun getExpenseTotalsByCategory(from: Long, to: Long): List<CategoryExpenseTotal>

    @Query(
        """
        SELECT (date_time / :bucketMillis * :bucketMillis) AS periodStart,
               amount_currency AS currency,
               COALESCE(SUM(COALESCE(real_amount_minor, amount_minor)), 0) AS totalMinor
        FROM expenses
        WHERE date_time >= :from AND date_time < :to
        GROUP BY periodStart, amount_currency
        ORDER BY periodStart
        """,
    )
    suspend fun getExpenseTotalsByPeriod(from: Long, to: Long, bucketMillis: Long): List<PeriodExpenseTotal>

    @Query(
        """
        SELECT (date_time / :bucketMillis * :bucketMillis) AS periodStart,
               category_id AS categoryId,
               amount_currency AS currency,
               COALESCE(SUM(COALESCE(real_amount_minor, amount_minor)), 0) AS totalMinor
        FROM expenses
        WHERE date_time >= :from AND date_time < :to
        GROUP BY periodStart, category_id, amount_currency
        ORDER BY periodStart
        """,
    )
    suspend fun getExpenseCategoryTotalsByPeriod(
        from: Long,
        to: Long,
        bucketMillis: Long,
    ): List<PeriodCategoryExpenseTotal>

    @Query(
        """
        SELECT amount_currency AS currency,
               COALESCE(SUM(COALESCE(real_amount_minor, amount_minor)), 0) AS totalMinor
        FROM expenses
        WHERE date_time >= :from AND date_time < :to
        GROUP BY amount_currency
        """,
    )
    suspend fun getExpenseTotalsByCurrency(from: Long, to: Long): List<CurrencyTotal>

    /**
     * Returns only the most recent [limit] expenses (newest first).
     * Lightweight alternative to [observeAll] for the home dashboard where only
     * the last N rows are displayed — avoids a full-table scan + SQLCipher
     * decryption of every row.
     */
    @Query("SELECT * FROM expenses ORDER BY date_time DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ExpenseEntity>>

    @Upsert
    suspend fun upsert(entity: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: Long)
}
