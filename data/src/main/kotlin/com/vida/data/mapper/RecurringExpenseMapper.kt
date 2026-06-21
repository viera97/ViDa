package com.vida.data.mapper

import com.vida.data.db.entity.RecurringExpenseEntity
import com.vida.data.mapper.util.toColumns
import com.vida.data.mapper.util.toMoney
import com.vida.domain.model.Currency
import com.vida.domain.model.Frequency
import com.vida.domain.model.RecurringExpense
import com.vida.domain.model.SourceType
import java.time.LocalDate

/**
 * Stateless mapper between [RecurringExpense] (domain) and [RecurringExpenseEntity] (Room).
 *
 * Money decomposes into `amount_minor` + `amount_currency`. The domain `currency`
 * field is reconstructed from `amount_currency` (they are always equal per the
 * domain invariant — no separate `currency_code` column is stored). [Frequency] is
 * stored as its enum name (String). [isActive] maps to/from INTEGER (1/0).
 * `startDate`, `endDate`, and `lastGeneratedDate` are stored as epoch-day integers.
 *
 * The polymorphic source maps to exactly one of three nullable columns — same
 * pattern as `ExpenseMapper`.
 */
object RecurringExpenseMapper {

    fun toDomain(entity: RecurringExpenseEntity): RecurringExpense {
        val (sourceType, sourceId) = when {
            entity.sourceCardId != null -> SourceType.CARD to entity.sourceCardId
            entity.sourceStashId != null -> SourceType.STASH to entity.sourceStashId
            entity.sourceWalletId != null -> SourceType.WALLET to null
            else -> error("RecurringExpenseEntity id=${entity.id} has no source column set")
        }
        val amount = (entity.amountMinor to entity.amountCurrency).toMoney()
        return RecurringExpense(
            id = entity.id,
            amount = amount,
            currency = Currency.fromCode(entity.amountCurrency),
            categoryId = entity.categoryId,
            sourceType = sourceType,
            sourceId = sourceId,
            description = entity.description,
            frequency = Frequency.valueOf(entity.frequency),
            startDate = LocalDate.ofEpochDay(entity.startDate),
            endDate = entity.endDate?.let(LocalDate::ofEpochDay),
            lastGeneratedDate = entity.lastGeneratedDate?.let(LocalDate::ofEpochDay),
            isActive = entity.isActive == 1,
        )
    }

    fun toEntity(domain: RecurringExpense): RecurringExpenseEntity {
        val (amountMinor, amountCurrency) = domain.amount.toColumns()
        val (sourceWalletId, sourceCardId, sourceStashId) = when (domain.sourceType) {
            SourceType.WALLET -> Triple(1L, null, null)
            SourceType.CARD -> Triple(null, domain.sourceId, null)
            SourceType.STASH -> Triple(null, null, domain.sourceId)
        }
        return RecurringExpenseEntity(
            id = domain.id,
            categoryId = domain.categoryId,
            amountMinor = amountMinor,
            amountCurrency = amountCurrency,
            description = domain.description,
            frequency = domain.frequency.name,
            startDate = domain.startDate.toEpochDay(),
            endDate = domain.endDate?.toEpochDay(),
            lastGeneratedDate = domain.lastGeneratedDate?.toEpochDay(),
            isActive = if (domain.isActive) 1 else 0,
            sourceWalletId = sourceWalletId,
            sourceCardId = sourceCardId,
            sourceStashId = sourceStashId,
        )
    }
}
