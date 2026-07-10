package com.vida.data.mapper

import com.vida.data.db.entity.BankEntity
import com.vida.domain.model.Bank

/**
 * Stateless mapper between [Bank] (domain) and [BankEntity] (Room).
 *
 * [Bank.isSystem] (Boolean) maps to/from [BankEntity.isSystem] (INTEGER 0/1).
 */
object BankMapper {

    fun toDomain(entity: BankEntity): Bank = Bank(
        id = entity.id,
        name = entity.name,
        color = entity.color,
        isSystem = entity.isSystem != 0,
    )

    fun toEntity(domain: Bank): BankEntity = BankEntity(
        id = domain.id,
        name = domain.name,
        color = domain.color,
        isSystem = if (domain.isSystem) 1 else 0,
    )
}
