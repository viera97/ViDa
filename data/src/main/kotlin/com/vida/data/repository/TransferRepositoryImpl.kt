package com.vida.data.repository

import com.vida.data.db.dao.TransferDao
import com.vida.data.mapper.TransferMapper
import com.vida.data.mapper.util.toEpochMillis
import com.vida.domain.model.SourceType
import com.vida.domain.model.Transfer
import com.vida.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

/**
 * Room-backed [TransferRepository]. Delegates reactive queries to [TransferDao]
 * at SQL level. [upsert] delegates to [TransferOrchestrator.recordTransfer] for
 * atomic recording (source/destination verification + transfer insert in a single
 * transaction).
 */
class TransferRepositoryImpl @Inject constructor(
    private val dao: TransferDao,
    private val mapper: TransferMapper,
    private val orchestrator: TransferOrchestrator,
) : TransferRepository {

    override fun getAll(): Flow<List<Transfer>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): Transfer? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun getBySource(
        sourceType: SourceType,
        sourceId: Long?,
        asOf: Instant,
    ): Flow<List<Transfer>> =
        dao.observeByParticipant(sourceType.name, sourceId, asOf.toEpochMillis())
            .map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun upsert(transfer: Transfer): Long =
        orchestrator.recordTransfer(transfer)

    override suspend fun delete(id: Long) = dao.delete(id)
}
