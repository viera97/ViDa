package com.vida.domain.usecase.balance

import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.repository.CardRepository
import com.vida.domain.repository.StashRepository
import com.vida.domain.repository.WalletRepository
import kotlinx.coroutines.flow.first

/**
 * Returns the computed balance of a single source. Dispatches to the
 * source-type-appropriate repository's reactive observation, taking the first
 * emission for one-shot semantics:
 *
 * - `WALLET` → [WalletRepository.observeBalance]; `sourceId` MUST be non-null
 * - `CARD` → [CardRepository.observeBalance]; `sourceId` MUST be non-null
 * - `STASH` → [StashRepository.observeBalance]; `sourceId` MUST be non-null
 *
 * Real implementations land in `:data` (Room SUM queries per source). The
 * repository uses `Instant.now()` internally as the `asOf` cutoff — historical
 * `asOf` parameter was dropped when the API moved from one-shot to reactive.
 */
class GetBalanceBySource(
    private val cardRepo: CardRepository,
    private val stashRepo: StashRepository,
    private val walletRepo: WalletRepository,
) {
    suspend operator fun invoke(
        sourceType: SourceType,
        sourceId: Long?,
    ): Money = when (sourceType) {
        SourceType.WALLET -> {
            require(sourceId != null) { "sourceId must be set when sourceType is WALLET" }
            walletRepo.observeBalance(sourceId).first()
        }
        SourceType.CARD -> {
            require(sourceId != null) { "sourceId must be set when sourceType is CARD or STASH" }
            cardRepo.observeBalance(sourceId).first()
        }
        SourceType.STASH -> {
            require(sourceId != null) { "sourceId must be set when sourceType is CARD or STASH" }
            stashRepo.observeBalance(sourceId).first()
        }
    }
}
