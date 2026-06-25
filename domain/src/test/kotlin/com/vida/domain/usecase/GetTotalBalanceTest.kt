package com.vida.domain.usecase

import com.vida.domain.model.Card
import com.vida.domain.model.CardNumber
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import com.vida.domain.model.CurrencyRate
import com.vida.domain.model.Money
import com.vida.domain.model.Stash
import com.vida.domain.model.Wallet
import com.vida.domain.repository.CardRepository
import com.vida.domain.repository.CurrencyRateRepository
import com.vida.domain.repository.StashRepository
import com.vida.domain.repository.WalletRepository
import com.vida.domain.usecase.balance.GetTotalBalance
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class GetTotalBalanceTest {

    private val now: Instant = Instant.parse("2026-06-19T12:00:00Z")

    private fun newTotal(
        cardRepo: CardRepository,
        stashRepo: StashRepository,
        walletRepo: WalletRepository,
        rateRepo: CurrencyRateRepository,
    ): GetTotalBalance {
        val convert = ConvertCurrency(rateRepo)
        return GetTotalBalance(cardRepo, stashRepo, walletRepo, convert) { now }
    }

    @Test
    fun `empty sources return ZERO_CUP`() = runTest {
        val cardRepo = mockk<CardRepository>()
        val stashRepo = mockk<StashRepository>()
        val walletRepo = mockk<WalletRepository>()
        val rateRepo = mockk<CurrencyRateRepository>()

        coEvery { cardRepo.getAll() } returns flowOf(emptyList<Card>())
        coEvery { stashRepo.getAll() } returns flowOf(emptyList<Stash>())
        coEvery { walletRepo.getAll() } returns flowOf(emptyList<Wallet>())

        val total = newTotal(cardRepo, stashRepo, walletRepo, rateRepo).invoke()
        assertEquals(Money.ZERO_CUP, total)
    }

    @Test
    fun `wallet in USD plus USD-to-CUP rate totals correctly`() = runTest {
        val cardRepo = mockk<CardRepository>()
        val stashRepo = mockk<StashRepository>()
        val walletRepo = mockk<WalletRepository>()
        val rateRepo = mockk<CurrencyRateRepository>()

        coEvery { cardRepo.getAll() } returns flowOf(emptyList<Card>())
        coEvery { stashRepo.getAll() } returns flowOf(emptyList<Stash>())
        coEvery { walletRepo.getAll() } returns flowOf(listOf(Wallet(id = 1L, currency = Currency.USD)))
        coEvery { walletRepo.getBalance(1L, now) } returns Money(BigDecimal.ONE, Currency.USD)
        coEvery { rateRepo.getRate(Currency.USD, Currency.CUP, now) } returns
            CurrencyRate(
                fromCurrency = Currency.USD,
                toCurrency = Currency.CUP,
                rate = BigDecimal("420"),
                updatedAt = now,
            )

        val total = newTotal(cardRepo, stashRepo, walletRepo, rateRepo).invoke()
        // 1 USD * 420 = 420 CUP (HALF_EVEN scale 2)
        assertEquals(Money(BigDecimal("420.00"), Currency.CUP), total)
    }

    @Test
    fun `source whose currency has no rate is dropped from total`() = runTest {
        val cardRepo = mockk<CardRepository>()
        val stashRepo = mockk<StashRepository>()
        val walletRepo = mockk<WalletRepository>()
        val rateRepo = mockk<CurrencyRateRepository>()

        val mlcCard = Card(
            id = 7L,
            number = CardNumber.fromFull("1234567890123456"),
            bank = "BANDEC",
            type = CardType.DEBIT,
            currency = Currency.MLC,
            expirationDate = LocalDate.now().plusYears(2),
        )
        coEvery { cardRepo.getAll() } returns flowOf(listOf(mlcCard))
        coEvery { stashRepo.getAll() } returns flowOf(emptyList<Stash>())
        coEvery { walletRepo.getAll() } returns flowOf(listOf(Wallet(id = 1L, currency = Currency.CUP)))
        coEvery { walletRepo.getBalance(1L, now) } returns Money(BigDecimal("100.00"), Currency.CUP)
        coEvery { cardRepo.getBalance(7L, now) } returns Money(BigDecimal("50"), Currency.MLC)
        coEvery { rateRepo.getRate(Currency.MLC, Currency.CUP, now) } returns null

        val total = newTotal(cardRepo, stashRepo, walletRepo, rateRepo).invoke()
        // wallet contributes 100 CUP; MLC card dropped because no rate
        assertEquals(Money(BigDecimal("100.00"), Currency.CUP), total)
    }
}