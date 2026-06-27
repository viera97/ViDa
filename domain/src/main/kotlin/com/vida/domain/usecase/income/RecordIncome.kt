package com.vida.domain.usecase.income

import com.vida.domain.model.Income
import com.vida.domain.model.SourceType
import com.vida.domain.repository.CardRepository
import com.vida.domain.repository.IncomeRepository
import com.vida.domain.repository.StashRepository
import com.vida.domain.repository.WalletRepository

/**
 * Action use case for recording a new income. Mirrors the pattern of
 * [com.vida.domain.usecase.expense.RecordExpense] — cross-validates the income
 * against the entities it references before persisting.
 *
 * Compared with `AddIncome` (the low-level CRUD primitive): [AddIncome] only
 * re-checks the entity-level invariants (positive amount, non-blank
 * description) and delegates. This use case additionally verifies the
 * destination source exists (`WalletRepository.getById` /
 * `CardRepository.getById` / `StashRepository.getById`).
 *
 * Atomicity (the insert + balance delta) is a `:data` concern —
 * [IncomeRepository.upsert] is wrapped in `withTransaction { }` in the Room
 * layer.
 */
class RecordIncome(
    private val incomeRepo: IncomeRepository,
    private val cardRepo: CardRepository,
    private val stashRepo: StashRepository,
    private val walletRepo: WalletRepository,
) {
    suspend operator fun invoke(income: Income): Long {
        require(income.amount.isPositive()) { "Income amount must be positive" }
        require(income.description.isNotBlank()) { "Income description must not be blank" }

        when (income.sourceType) {
            SourceType.WALLET -> {
                require(income.sourceId != null) { "Wallet income requires non-null sourceId" }
                if (walletRepo.getById(income.sourceId) == null) {
                    throw NoSuchElementException("Wallet ${income.sourceId} not found")
                }
            }
            SourceType.CARD -> {
                require(income.sourceId != null) { "Card income requires non-null sourceId" }
                if (cardRepo.getById(income.sourceId) == null) {
                    throw NoSuchElementException("Card ${income.sourceId} not found")
                }
            }
            SourceType.STASH -> {
                require(income.sourceId != null) { "Stash income requires non-null sourceId" }
                if (stashRepo.getById(income.sourceId) == null) {
                    throw NoSuchElementException("Stash ${income.sourceId} not found")
                }
            }
        }

        return incomeRepo.upsert(income)
    }
}
