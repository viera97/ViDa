package com.vida.domain.usecase

import com.vida.domain.model.Currency
import com.vida.domain.model.CurrencyRate
import com.vida.domain.model.Money
import com.vida.domain.repository.CurrencyRateRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class ConvertCurrencyTest {

    private val now: Instant = Instant.parse("2026-06-19T12:00:00Z")
    private val oneUSD: Money = Money(BigDecimal.ONE, Currency.USD)
    private val rateUSDToCUP: CurrencyRate = CurrencyRate(
        fromCurrency = "USD",
        toCurrency = "CUP",
        rate = BigDecimal("420"),
        updatedAt = now,
    )

    @Test
    fun `same currency short-circuits without consulting the repo`() = runTest {
        val repo = mockk<CurrencyRateRepository>()
        val convert = ConvertCurrency(repo)
        val result = convert(oneUSD, Currency.USD, now)
        assertEquals(oneUSD, result)
    }

    @Test
    fun `available rate produces converted Money`() = runTest {
        val repo = mockk<CurrencyRateRepository>()
        coEvery { repo.getRate("USD", "CUP", now) } returns rateUSDToCUP
        val convert = ConvertCurrency(repo)
        val result = convert(oneUSD, Currency.CUP, now)
        assertEquals(Money(BigDecimal("420.00"), Currency.CUP), result)
    }

    @Test
    fun `missing rate returns null`() = runTest {
        val repo = mockk<CurrencyRateRepository>()
        coEvery { repo.getRate("USD", "CUP", now) } returns null
        val convert = ConvertCurrency(repo)
        val result = convert(oneUSD, Currency.CUP, now)
        assertNull(result)
    }
}