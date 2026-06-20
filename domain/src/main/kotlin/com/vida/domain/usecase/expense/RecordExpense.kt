package com.vida.domain.usecase.expense

import com.vida.domain.model.Expense
import com.vida.domain.model.SourceType
import com.vida.domain.repository.CardRepository
import com.vida.domain.repository.CategoryRepository
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.StashRepository
import com.vida.domain.repository.WalletRepository

/**
 * Action use case for recording a new expense. This is the **rich** path the
 * UI uses when the user taps "Save" on the expense dialog — it cross-validates
 * the expense against the other entities it references before persisting.
 *
 * Compared with `AddExpense` (the low-level CRUD primitive): [AddExpense]
 * only re-checks the entity-level invariants (positive amount, non-blank
 * description) and delegates. [RecordExpense] additionally requires:
 *
 * - the referenced [com.vida.domain.model.Category] exists
 *   (`CategoryRepository.getById`)
 * - the referenced source exists (`CardRepository.getById` /
 *   `StashRepository.getById` / `WalletRepository.get()`)
 *
 * The wallet source is assumed to be seeded at app init (it is a singleton,
 * Q1 locked) — [WalletRepository.get] throws if it is not, which is the
 * correct failure mode.
 *
 * **Balance gating is intentionally NOT performed here** (Q-PR2b-2 closed):
 * balances are computed at read time (Q7), so an expense may be recorded even
 * when the resulting computed balance is negative (credit-card scenarios).
 * A future `HasSufficientBalance` use case can warn via the UI without
 * changing this contract.
 *
 * Atomicity (the single-statement insert) is a `:data` concern —
 * [ExpenseRepository.upsert] is wrapped in `withTransaction { }` in the Room
 * layer.
 */
class RecordExpense(
    private val expenseRepo: ExpenseRepository,
    private val cardRepo: CardRepository,
    private val stashRepo: StashRepository,
    private val walletRepo: WalletRepository,
    private val categoryRepo: CategoryRepository,
) {
    suspend operator fun invoke(expense: Expense): Long {
        require(expense.amount.isPositive()) { "Expense amount must be positive" }
        require(expense.description.isNotBlank()) { "Expense description must not be blank" }

        if (categoryRepo.getById(expense.categoryId) == null) {
            throw NoSuchElementException("Category ${expense.categoryId} not found")
        }

        when (expense.sourceType) {
            SourceType.WALLET -> {
                // Singleton: walletRepo.get() throws if not seeded. That's the correct signal.
                walletRepo.get()
            }
            SourceType.CARD -> {
                require(expense.sourceId != null) { "Card expense requires non-null sourceId" }
                if (cardRepo.getById(expense.sourceId) == null) {
                    throw NoSuchElementException("Card ${expense.sourceId} not found")
                }
            }
            SourceType.STASH -> {
                require(expense.sourceId != null) { "Stash expense requires non-null sourceId" }
                if (stashRepo.getById(expense.sourceId) == null) {
                    throw NoSuchElementException("Stash ${expense.sourceId} not found")
                }
            }
        }

        return expenseRepo.upsert(expense)
    }
}
