package com.vida.domain.usecase.transfer

import com.vida.domain.model.SourceType
import com.vida.domain.model.Transfer
import com.vida.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Reactive stream of every transfer in which the given source participates on
 * either side (incoming + outgoing) on or before [asOf]. Pass `sourceId = null`
 * for the wallet.
 */
class GetTransfersBySource(private val repo: TransferRepository) {
    suspend operator fun invoke(
        sourceType: SourceType,
        sourceId: Long?,
        asOf: Instant = Instant.now(),
    ): Flow<List<Transfer>> = repo.getBySource(sourceType, sourceId, asOf)
}
