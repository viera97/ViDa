package com.vida.domain.usecase.balance

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.repository.CardRepository
import com.vida.domain.repository.StashRepository
import com.vida.domain.repository.WalletRepository
import com.vida.domain.usecase.ConvertCurrency
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Total balance across every source (wallet + every card + every stash), converted
 * to [Currency.CUP].
 *
 * Source rules:
 *
 * - Each source's balance is queried in its native currency via the matching repo's
 *   `getBalance` (PR #1 stub pattern — the `:data` Room SUM lands later).
 * - Each balance is then converted to CUP via [convertCurrency].
 * - Sources whose conversion returns `null` (no rate configured for their currency
 *   at [clock]) are DROPPED from the total rather than failing the whole query.
 * - When every source is unrated, OR no source exists at all, the result is
 *   [Money.ZERO_CUP].
 */
class GetTotalBalance(
    private val cardRepo: CardRepository,
    private val stashRepo: StashRepository,
    private val walletRepo: WalletRepository,
    private val convertCurrency: ConvertCurrency,
    private val clock: () -> Instant = { Instant.now() },
) {
    suspend operator fun invoke(): Money {
        val now: Instant = clock()
        val cards = cardRepo.getAll().first()
        val stashes = stashRepo.getAll().first()
        val wallet = runCatching { walletRepo.get() }.getOrNull()

        var total: Money = Money.ZERO_CUP
        if (wallet != null) {
            val balance = walletRepo.getBalance(now)
            total += convertCurrency(balance, Currency.CUP, now) ?: Money.ZERO_CUP
        }
        cards.forEach { card ->
            val balance = cardRepo.getBalance(card.id, now)
            total += convertCurrency(balance, Currency.CUP, now) ?: Money.ZERO_CUP
        }
        stashes.forEach { stash ->
            val balance = stashRepo.getBalance(stash.id, now)
            total += convertCurrency(balance, Currency.CUP, now) ?: Money.ZERO_CUP
        }
        return total
    }
}