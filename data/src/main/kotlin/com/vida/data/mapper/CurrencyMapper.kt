package com.vida.data.mapper

import com.vida.data.db.entity.CurrencyEntity
import com.vida.domain.model.CurrencyInfo

/**
 * Stateless mapper between [CurrencyInfo] (domain) and [CurrencyEntity] (Room).
 *
 * [CurrencyInfo.isSystem] (Boolean) maps to/from [CurrencyEntity.isSystem] (INTEGER 0/1).
 */
object CurrencyMapper {

    fun toDomain(entity: CurrencyEntity): CurrencyInfo = CurrencyInfo(
        id = entity.id,
        name = entity.name,
        code = entity.code,
        isSystem = entity.isSystem != 0,
    )

    fun toEntity(domain: CurrencyInfo): CurrencyEntity = CurrencyEntity(
        id = domain.id,
        name = domain.name,
        code = domain.code,
        isSystem = if (domain.isSystem) 1 else 0,
    )
}
