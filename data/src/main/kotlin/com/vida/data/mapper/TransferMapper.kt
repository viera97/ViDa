package com.vida.data.mapper

import com.vida.data.db.entity.TransferEntity
import com.vida.data.mapper.util.toColumns
import com.vida.data.mapper.util.toEpochMillis
import com.vida.data.mapper.util.toInstant
import com.vida.data.mapper.util.toMoney
import com.vida.domain.model.SourceType
import com.vida.domain.model.Transfer

/**
 * Stateless mapper between [Transfer] (domain) and [TransferEntity] (Room).
 *
 * Money decomposes into `amount_minor` + `amount_currency`. The polymorphic source
 * and destination each map to exactly one of three nullable columns — see
 * [TransferEntity] for the invariant.
 *
 * The domain `Transfer` model uses `fromType`/`fromId` and `toType`/`toId` naming;
 * the entity uses `source_*`/`destination_*` column naming.
 */
object TransferMapper {

    fun toDomain(entity: TransferEntity): Transfer {
        val (fromType, fromId) = decodeTriplet(
            entity.sourceWalletId,
            entity.sourceCardId,
            entity.sourceStashId,
            "TransferEntity id=${entity.id} source",
        )
        val (toType, toId) = decodeTriplet(
            entity.destinationWalletId,
            entity.destinationCardId,
            entity.destinationStashId,
            "TransferEntity id=${entity.id} destination",
        )
        return Transfer(
            id = entity.id,
            fromType = fromType,
            fromId = fromId,
            toType = toType,
            toId = toId,
            amount = (entity.amountMinor to entity.amountCurrency).toMoney(),
            dateTime = entity.dateTime.toInstant(),
            note = entity.note,
        )
    }

    fun toEntity(domain: Transfer): TransferEntity {
        val (amountMinor, amountCurrency) = domain.amount.toColumns()
        val (sourceWalletId, sourceCardId, sourceStashId) = encodeTriplet(
            domain.fromType,
            domain.fromId,
        )
        val (destinationWalletId, destinationCardId, destinationStashId) = encodeTriplet(
            domain.toType,
            domain.toId,
        )
        return TransferEntity(
            id = domain.id,
            amountMinor = amountMinor,
            amountCurrency = amountCurrency,
            dateTime = domain.dateTime.toEpochMillis(),
            note = domain.note,
            sourceWalletId = sourceWalletId,
            sourceCardId = sourceCardId,
            sourceStashId = sourceStashId,
            destinationWalletId = destinationWalletId,
            destinationCardId = destinationCardId,
            destinationStashId = destinationStashId,
        )
    }

    private fun decodeTriplet(
        walletId: Long?,
        cardId: Long?,
        stashId: Long?,
        label: String,
    ): Pair<SourceType, Long> = when {
        cardId != null -> SourceType.CARD to cardId
        stashId != null -> SourceType.STASH to stashId
        walletId != null -> SourceType.WALLET to walletId
        else -> error("$label has no column set")
    }

    private fun encodeTriplet(
        type: SourceType,
        id: Long,
    ): Triple<Long?, Long?, Long?> = when (type) {
        SourceType.WALLET -> Triple(id, null, null)
        SourceType.CARD -> Triple(null, id, null)
        SourceType.STASH -> Triple(null, null, id)
    }
}
