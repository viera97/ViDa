package com.vida.data.mapper

import com.vida.data.db.entity.CategoryEntity
import com.vida.domain.model.Category

/**
 * Stateless mapper between [Category] (domain) and [CategoryEntity] (Room).
 *
 * [Category.isSystem] (Boolean) maps to/from [CategoryEntity.isSystem] (INTEGER 0/1).
 */
object CategoryMapper {

    fun toDomain(entity: CategoryEntity): Category = Category(
        id = entity.id,
        name = entity.name,
        color = entity.color,
        icon = entity.icon,
        isSystem = entity.isSystem != 0,
    )

    fun toEntity(domain: Category): CategoryEntity = CategoryEntity(
        id = domain.id,
        name = domain.name,
        color = domain.color,
        icon = domain.icon,
        isSystem = if (domain.isSystem) 1 else 0,
    )
}
