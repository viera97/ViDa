package com.vida.data.repository

import com.vida.data.db.dao.CategoryDao
import com.vida.data.mapper.CategoryMapper
import com.vida.domain.model.Category
import com.vida.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao,
    private val mapper: CategoryMapper,
) : CategoryRepository {

    override fun getAll(): Flow<List<Category>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): Category? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun upsert(category: Category): Long =
        dao.upsert(mapper.toEntity(category))

    override suspend fun delete(id: Long) = dao.delete(id)
}
