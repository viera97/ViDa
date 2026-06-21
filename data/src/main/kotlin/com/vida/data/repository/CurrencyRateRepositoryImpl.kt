package com.vida.data.repository

import com.vida.data.db.dao.CurrencyRateDao
import com.vida.data.mapper.CurrencyRateMapper
import com.vida.data.mapper.util.toEpochMillis
import com.vida.domain.model.Currency
import com.vida.domain.model.CurrencyRate
import com.vida.domain.repository.CurrencyRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class CurrencyRateRepositoryImpl @Inject constructor(
    private val dao: CurrencyRateDao,
    private val mapper: CurrencyRateMapper,
) : CurrencyRateRepository {

    override fun getAll(): Flow<List<CurrencyRate>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getRate(from: Currency, to: Currency, asOf: Instant): CurrencyRate? =
        dao.getRate(from.code, to.code, asOf.toEpochMillis())?.let(mapper::toDomain)

    override suspend fun getRateHistory(from: Currency, to: Currency): Flow<List<CurrencyRate>> =
        dao.observeRateHistory(from.code, to.code)
            .map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun upsert(rate: CurrencyRate): Long =
        dao.upsert(mapper.toEntity(rate))

    override suspend fun delete(id: Long) = dao.delete(id)
}
