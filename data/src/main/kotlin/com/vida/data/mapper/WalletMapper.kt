package com.vida.data.mapper

import com.vida.data.db.entity.WalletEntity
import com.vida.domain.model.Wallet

object WalletMapper {
    fun toDomain(entity: WalletEntity): Wallet = Wallet(
        id = entity.id,
        currency = entity.currency,
    )

    fun toEntity(domain: Wallet): WalletEntity = WalletEntity(
        id = domain.id,
        currency = domain.currency,
    )
}
