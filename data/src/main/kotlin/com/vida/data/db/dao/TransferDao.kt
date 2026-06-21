package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.vida.data.db.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the `transfers` table — cross-source money movements.
 *
 * The polymorphic source/destination columns are queried via string comparison on
 * `:sourceType` (same pattern as `ExpenseDao.observeBySource`). For WALLET queries,
 * `:sourceId` is ignored (the wallet is a singleton addressed by `*_wallet_id`).
 */
@Dao
interface TransferDao {

    @Query("SELECT * FROM transfers ORDER BY date_time DESC")
    fun observeAll(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE id = :id")
    suspend fun getById(id: Long): TransferEntity?

    /**
     * Transfers whose **source** (from side) matches [sourceType]/[sourceId].
     * Only rows with `date_time <= asOf` are returned, newest first.
     */
    @Query(
        """
        SELECT * FROM transfers WHERE
            ((source_card_id IS NOT NULL AND source_card_id = :sourceId AND :sourceType = 'CARD') OR
             (source_stash_id IS NOT NULL AND source_stash_id = :sourceId AND :sourceType = 'STASH') OR
             (source_wallet_id IS NOT NULL AND :sourceType = 'WALLET'))
            AND date_time <= :asOf
        ORDER BY date_time DESC
        """,
    )
    fun observeBySource(sourceType: String, sourceId: Long?, asOf: Long): Flow<List<TransferEntity>>

    /**
     * Transfers whose **destination** (to side) matches [sourceType]/[sourceId].
     * Only rows with `date_time <= asOf` are returned, newest first.
     */
    @Query(
        """
        SELECT * FROM transfers WHERE
            ((destination_card_id IS NOT NULL AND destination_card_id = :sourceId AND :sourceType = 'CARD') OR
             (destination_stash_id IS NOT NULL AND destination_stash_id = :sourceId AND :sourceType = 'STASH') OR
             (destination_wallet_id IS NOT NULL AND :sourceType = 'WALLET'))
            AND date_time <= :asOf
        ORDER BY date_time DESC
        """,
    )
    fun observeByDestination(
        sourceType: String,
        sourceId: Long?,
        asOf: Long,
    ): Flow<List<TransferEntity>>

    /**
     * Transfers in which the given source participates on **either side** (from OR to).
     * This backs `TransferRepository.getBySource` which returns a source's full
     * movement history (incoming + outgoing) for the UI.
     */
    @Query(
        """
        SELECT * FROM transfers WHERE
            ((source_card_id IS NOT NULL AND source_card_id = :sourceId AND :sourceType = 'CARD') OR
             (source_stash_id IS NOT NULL AND source_stash_id = :sourceId AND :sourceType = 'STASH') OR
             (source_wallet_id IS NOT NULL AND :sourceType = 'WALLET') OR
             (destination_card_id IS NOT NULL AND destination_card_id = :sourceId AND :sourceType = 'CARD') OR
             (destination_stash_id IS NOT NULL AND destination_stash_id = :sourceId AND :sourceType = 'STASH') OR
             (destination_wallet_id IS NOT NULL AND :sourceType = 'WALLET'))
            AND date_time <= :asOf
        ORDER BY date_time DESC
        """,
    )
    fun observeByParticipant(
        sourceType: String,
        sourceId: Long?,
        asOf: Long,
    ): Flow<List<TransferEntity>>

    /**
     * Wrapped in `@Transaction` for forward-compat: when stored-balance columns are
     * added to source/destination entities, the upsert becomes multi-table (debit
     * source, credit destination, insert transfer). See vida-domain design #93 §7.
     *
     * In PR #3 v1 the body is a single INSERT — the orchestrator handles atomicity
     * via `database.withTransaction { }`.
     */
    @Transaction
    @Upsert
    suspend fun upsert(entity: TransferEntity): Long

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun delete(id: Long)
}
