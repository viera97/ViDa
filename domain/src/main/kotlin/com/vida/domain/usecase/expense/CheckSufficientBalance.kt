package com.vida.domain.usecase.expense

import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.repository.CardRepository
import com.vida.domain.repository.StashRepository
import com.vida.domain.repository.WalletRepository
import kotlinx.coroutines.flow.first

/**
 * Checks whether the given [amount] is available in the selected source.
 *
 * Returns [Result.Sufficient] when the source's current balance covers the
 * expense, [Result.Insufficient] with both figures when it does not.
 *
 * **Currency assumption**: the [amount] currency must match the source's
 * balance currency — callers SHOULD verify this before invoking this use
 * case (e.g. via [com.vida.feature.expense.ExpenseFormViewModel.computeMismatchError]).
 */
class CheckSufficientBalance(
    private val walletRepo: WalletRepository,
    private val cardRepo: CardRepository,
    private val stashRepo: StashRepository,
) {
    sealed class Result {
        data object Sufficient : Result()
        data class Insufficient(val balance: Money, val needed: Money) : Result()
    }

    /**
     * @throws IllegalArgumentException if [sourceId] is null for CARD or STASH.
     */
    suspend operator fun invoke(
        sourceType: SourceType,
        sourceId: Long?,
        amount: Money,
    ): Result {
        val balance: Money = when (sourceType) {
            SourceType.WALLET -> walletRepo.observeBalance(sourceId ?: 1L).first()
            SourceType.CARD -> {
                require(sourceId != null) { "Card expense requires non-null sourceId" }
                cardRepo.observeBalance(sourceId).first()
            }
            SourceType.STASH -> {
                require(sourceId != null) { "Stash expense requires non-null sourceId" }
                stashRepo.observeBalance(sourceId).first()
            }
        }
        return if (amount > balance) Result.Insufficient(balance, amount)
        else Result.Sufficient
    }
}
