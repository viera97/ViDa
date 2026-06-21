package com.vida.data.mapper

import com.vida.data.db.entity.StashEntity
import com.vida.domain.model.Stash

object StashMapper {
    fun toDomain(entity: StashEntity): Stash = Stash(
        id = entity.id,
        name = entity.name,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        currency = entity.currency,
    )

    fun toEntity(domain: Stash): StashEntity = StashEntity(
        id = domain.id,
        name = domain.name,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt,
        currency = domain.currency,
    )
}
