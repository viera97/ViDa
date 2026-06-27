package com.vida.data.mapper

import com.vida.data.db.entity.IncomeEntity
import com.vida.data.mapper.util.toColumns
import com.vida.data.mapper.util.toEpochMillis
import com.vida.data.mapper.util.toInstant
import com.vida.data.mapper.util.toMoney
import com.vida.domain.model.Income
import com.vida.domain.model.SourceType

/**
 * Stateless mapper between [Income] (domain) and [IncomeEntity] (Room).
 *
 * Money decomposes into `amount_minor` + `amount_currency`. The polymorphic
 * destination source maps to exactly one of the three destination columns —
 * see [IncomeEntity] for the invariant.
 */
object IncomeMapper {

    fun toDomain(entity: IncomeEntity): Income {
        val (sourceType, sourceId) = when {
            entity.destinationCardId != null -> SourceType.CARD to entity.destinationCardId
            entity.destinationStashId != null -> SourceType.STASH to entity.destinationStashId
            entity.destinationWalletId != null -> SourceType.WALLET to entity.destinationWalletId
            else -> error("IncomeEntity id=${entity.id} has no destination column set")
        }
        return Income(
            id = entity.id,
            amount = (entity.amountMinor to entity.amountCurrency).toMoney(),
            description = entity.description,
            dateTime = entity.dateTime.toInstant(),
            sourceType = sourceType,
            sourceId = sourceId,
            note = entity.note,
        )
    }

    fun toEntity(domain: Income): IncomeEntity {
        val (amountMinor, amountCurrency) = domain.amount.toColumns()
        val (destinationWalletId, destinationCardId, destinationStashId) = when (domain.sourceType) {
            // WALLET now uses the real wallet id from the form (commit 5742918
            // refactored wallets into entities with real row ids). Falls back to
            // null only if the caller passes null (legacy data when wallets
            // were treated as a singleton).
            SourceType.WALLET -> Triple(domain.sourceId, null, null)
            SourceType.CARD -> Triple(null, domain.sourceId, null)
            SourceType.STASH -> Triple(null, null, domain.sourceId)
        }
        return IncomeEntity(
            id = domain.id,
            amountMinor = amountMinor,
            amountCurrency = amountCurrency,
            description = domain.description,
            dateTime = domain.dateTime.toEpochMillis(),
            note = domain.note,
            destinationWalletId = destinationWalletId,
            destinationCardId = destinationCardId,
            destinationStashId = destinationStashId,
        )
    }
}
