package com.vida.data.mapper

import com.vida.data.db.entity.CardEntity
import com.vida.data.mapper.util.amountMinorUnits
import com.vida.data.mapper.util.fromMinorUnits
import com.vida.domain.model.Card
import com.vida.domain.model.CardNumber
import com.vida.domain.model.Currency
import com.vida.domain.model.Money

object CardMapper {
    fun toDomain(entity: CardEntity): Card = Card(
        id = entity.id,
        number = CardNumber.fromFirst6Last4(
            entity.maskedNumber.substring(0, 6),
            entity.maskedNumber.substring(12, 16),
        ),
        bank = entity.bank,
        type = entity.type,
        currency = entity.currency,
        note = entity.note,
        expirationDate = entity.expirationDate,
        balance = Money.fromMinorUnits(entity.balanceMinor, Currency.fromCode(entity.initialBalanceCurrency)),
    )

    fun toEntity(domain: Card): CardEntity = CardEntity(
        id = domain.id,
        maskedNumber = domain.number.masked,
        bank = domain.bank,
        type = domain.type,
        currency = domain.currency,
        note = domain.note,
        expirationDate = domain.expirationDate,
        balanceMinor = domain.balance.amountMinorUnits(),
        initialBalanceCurrency = domain.balance.currency.code,
    )
}
