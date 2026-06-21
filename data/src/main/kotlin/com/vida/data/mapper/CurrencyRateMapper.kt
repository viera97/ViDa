package com.vida.data.mapper

import com.vida.data.db.entity.CurrencyRateEntity
import com.vida.data.mapper.util.toEpochMillis
import com.vida.data.mapper.util.toInstant
import com.vida.domain.model.CurrencyRate
import java.math.BigDecimal

/**
 * Stateless mapper between [CurrencyRate] (domain) and [CurrencyRateEntity] (Room).
 *
 * [CurrencyRate.rate] (BigDecimal) stores as REAL (Double). [CurrencyRate.updatedAt]
 * (Instant) maps to the `effective_date` column (epoch millis).
 */
object CurrencyRateMapper {

    fun toDomain(entity: CurrencyRateEntity): CurrencyRate = CurrencyRate(
        id = entity.id,
        fromCurrency = entity.fromCurrency,
        toCurrency = entity.toCurrency,
        rate = BigDecimal.valueOf(entity.rate),
        updatedAt = entity.effectiveDate.toInstant(),
    )

    fun toEntity(domain: CurrencyRate): CurrencyRateEntity = CurrencyRateEntity(
        id = domain.id,
        fromCurrency = domain.fromCurrency,
        toCurrency = domain.toCurrency,
        rate = domain.rate.toDouble(),
        effectiveDate = domain.updatedAt.toEpochMillis(),
    )
}
