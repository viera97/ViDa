package com.vida.data.mapper

import com.vida.data.db.entity.RefundEntity
import com.vida.data.mapper.util.toColumns
import com.vida.data.mapper.util.toEpochMillis
import com.vida.data.mapper.util.toInstant
import com.vida.data.mapper.util.toMoney
import com.vida.domain.model.Refund

/**
 * Stateless mapper between [Refund] (domain) and [RefundEntity] (Room).
 *
 * Money decomposes into `amount_minor` + `amount_currency`, same pattern as
 * [ExpenseMapper].
 */
object RefundMapper {

    fun toDomain(entity: RefundEntity): Refund = Refund(
        id = entity.id,
        originalExpenseId = entity.originalExpenseId,
        amount = (entity.amountMinor to entity.amountCurrency).toMoney(),
        reason = entity.reason,
        dateTime = entity.dateTime.toInstant(),
        note = entity.note,
    )

    fun toEntity(domain: Refund): RefundEntity {
        val (minor, currency) = domain.amount.toColumns()
        return RefundEntity(
            id = domain.id,
            originalExpenseId = domain.originalExpenseId,
            amountMinor = minor,
            amountCurrency = currency,
            reason = domain.reason,
            dateTime = domain.dateTime.toEpochMillis(),
            note = domain.note,
        )
    }
}
