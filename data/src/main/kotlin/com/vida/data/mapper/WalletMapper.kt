package com.vida.data.mapper

import com.vida.data.db.entity.WalletEntity
import com.vida.data.mapper.util.amountMinorUnits
import com.vida.data.mapper.util.fromMinorUnits
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.Wallet

object WalletMapper {
    fun toDomain(entity: WalletEntity): Wallet = Wallet(
        id = entity.id,
        currency = entity.currency,
        name = entity.name,
        balance = Money.fromMinorUnits(entity.balanceMinor, Currency.fromCode(entity.initialBalanceCurrency)),
    )

    fun toEntity(domain: Wallet): WalletEntity = WalletEntity(
        id = domain.id,
        currency = domain.currency,
        name = domain.name,
        balanceMinor = domain.balance.amountMinorUnits(),
        initialBalanceCurrency = domain.balance.currency.code,
    )
}
