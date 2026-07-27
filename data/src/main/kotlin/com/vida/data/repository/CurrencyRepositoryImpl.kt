package com.vida.data.repository

import com.vida.data.db.dao.CurrencyDao
import com.vida.data.mapper.CurrencyMapper
import com.vida.domain.model.CurrencyInfo
import com.vida.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CurrencyRepositoryImpl @Inject constructor(
    private val dao: CurrencyDao,
    private val mapper: CurrencyMapper,
) : CurrencyRepository {

    override fun getAll(): Flow<List<CurrencyInfo>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): CurrencyInfo? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun getByCode(code: String): CurrencyInfo? =
        dao.getByCode(code)?.let(mapper::toDomain)

    override suspend fun upsert(currency: CurrencyInfo): Long =
        dao.upsert(mapper.toEntity(currency))

    override suspend fun delete(id: Long) = dao.delete(id)
}
