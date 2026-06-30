package com.vida.data.mapper

import com.vida.data.db.entity.ExpenseEntity
import com.vida.data.mapper.util.toColumns
import com.vida.data.mapper.util.toEpochMillis
import com.vida.data.mapper.util.toInstant
import com.vida.data.mapper.util.toMoney
import com.vida.domain.model.Expense
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType

/**
 * Stateless mapper between [Expense] (domain) and [ExpenseEntity] (Room).
 *
 * Money decomposes into `amount_minor` + `amount_currency` (and the nullable
 * `real_amount_*` pair). The polymorphic source maps to exactly one of the three
 * source columns — see [ExpenseEntity] for the invariant.
 */
object ExpenseMapper {

    fun toDomain(entity: ExpenseEntity): Expense {
        val (sourceType, sourceId) = when {
            entity.sourceCardId != null -> SourceType.CARD to entity.sourceCardId
            entity.sourceStashId != null -> SourceType.STASH to entity.sourceStashId
            entity.sourceWalletId != null -> SourceType.WALLET to entity.sourceWalletId
            else -> error("ExpenseEntity id=${entity.id} has no source column set")
        }
        val realAmount: Money? = entity.realAmountMinor?.let { minor ->
            (minor to entity.realAmountCurrency!!).toMoney()
        }
        return Expense(
            id = entity.id,
            categoryId = entity.categoryId,
            amount = (entity.amountMinor to entity.amountCurrency).toMoney(),
            realAmount = realAmount,
            description = entity.description,
            dateTime = entity.dateTime.toInstant(),
            sourceType = sourceType,
            sourceId = sourceId,
            note = entity.note,
        )
    }

    fun toEntity(domain: Expense): ExpenseEntity {
        val (amountMinor, amountCurrency) = domain.amount.toColumns()
        val realAmountMinor: Long? = domain.realAmount?.toColumns()?.first
        val realAmountCurrency: String? = domain.realAmount?.toColumns()?.second
        val (sourceWalletId, sourceCardId, sourceStashId) = when (domain.sourceType) {
            // WALLET now uses the real wallet id from the form (commit 5742918
            // refactored wallets into entities with real row ids). Falls back to
            // null only if the caller passes null (legacy data when wallets
            // were treated as a singleton).
            SourceType.WALLET -> Triple(domain.sourceId, null, null)
            SourceType.CARD -> Triple(null, domain.sourceId, null)
            SourceType.STASH -> Triple(null, null, domain.sourceId)
        }
        return ExpenseEntity(
            id = domain.id,
            categoryId = domain.categoryId,
            amountMinor = amountMinor,
            amountCurrency = amountCurrency,
            realAmountMinor = realAmountMinor,
            realAmountCurrency = realAmountCurrency,
            description = domain.description,
            dateTime = domain.dateTime.toEpochMillis(),
            note = domain.note,
            sourceWalletId = sourceWalletId,
            sourceCardId = sourceCardId,
            sourceStashId = sourceStashId,
        )
    }
}
