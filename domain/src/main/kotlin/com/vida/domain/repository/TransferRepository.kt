package com.vida.domain.repository

import com.vida.domain.model.SourceType
import com.vida.domain.model.Transfer
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Persistence contract for [Transfer] aggregates. Implemented in `:data` (Room).
 *
 * Query helpers:
 * - [getBySource] returns transfers in which the given source participates on
 *   either side — i.e. transfers whose `fromType/fromId` match OR whose
 *   `toType/toId` match. This is the query the UI uses to render a source's
 *   "movement history" (incoming + outgoing) without a join at the view layer.
 * - `sourceId == null` selects wallet-sourced transfers.
 */
interface TransferRepository {
    fun getAll(): Flow<List<Transfer>>
    suspend fun getById(id: Long): Transfer?
    suspend fun getBySource(
        sourceType: SourceType,
        sourceId: Long?,
        asOf: Instant = Instant.now(),
    ): Flow<List<Transfer>>
    suspend fun upsert(transfer: Transfer): Long
    suspend fun delete(id: Long)
}
