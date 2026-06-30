package com.vida.feature.recurringexpensemanagement.di

import com.vida.domain.repository.CardRepository
import com.vida.domain.repository.CategoryRepository
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.IncomeRepository
import com.vida.domain.repository.RecurringExpenseRepository
import com.vida.domain.repository.RecurringIncomeRepository
import com.vida.domain.repository.StashRepository
import com.vida.domain.repository.WalletRepository
import com.vida.domain.usecase.expense.RecordExpense
import com.vida.domain.usecase.income.RecordIncome
import com.vida.domain.usecase.recurring.AddRecurringExpense
import com.vida.domain.usecase.recurring.AddRecurringIncome
import com.vida.domain.usecase.recurring.DeleteRecurringExpense
import com.vida.domain.usecase.recurring.DeleteRecurringIncome
import com.vida.domain.usecase.recurring.GenerateRecurringExpense
import com.vida.domain.usecase.recurring.GenerateRecurringIncome
import com.vida.domain.usecase.recurring.GetDueRecurringExpenses
import com.vida.domain.usecase.recurring.GetDueRecurringIncomes
import com.vida.domain.usecase.recurring.GetRecurringExpense
import com.vida.domain.usecase.recurring.GetRecurringIncome
import com.vida.domain.usecase.recurring.ListRecurringExpenses
import com.vida.domain.usecase.recurring.ListRecurringIncomes
import com.vida.domain.usecase.recurring.UpdateRecurringExpense
import com.vida.domain.usecase.recurring.UpdateRecurringIncome
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing recurring-expense and recurring-income use cases for ViewModels.
 */
@Module
@InstallIn(ViewModelComponent::class)
object RecurringModule {

    // ── Expense use cases ─────────────────────────────────────────────────────

    @Provides
    fun provideListRecurringExpenses(repo: RecurringExpenseRepository): ListRecurringExpenses =
        ListRecurringExpenses(repo)

    @Provides
    fun provideGetRecurringExpense(repo: RecurringExpenseRepository): GetRecurringExpense =
        GetRecurringExpense(repo)

    @Provides
    fun provideAddRecurringExpense(repo: RecurringExpenseRepository): AddRecurringExpense =
        AddRecurringExpense(repo)

    @Provides
    fun provideUpdateRecurringExpense(repo: RecurringExpenseRepository): UpdateRecurringExpense =
        UpdateRecurringExpense(repo)

    @Provides
    fun provideDeleteRecurringExpense(repo: RecurringExpenseRepository): DeleteRecurringExpense =
        DeleteRecurringExpense(repo)

    @Provides
    fun provideGetDueRecurringExpenses(repo: RecurringExpenseRepository): GetDueRecurringExpenses =
        GetDueRecurringExpenses(repo)

    @Provides
    fun provideGenerateRecurringExpense(repo: RecurringExpenseRepository): GenerateRecurringExpense =
        GenerateRecurringExpense(repo)

    @Provides
    fun provideRecordExpense(
        expenseRepo: ExpenseRepository,
        cardRepo: CardRepository,
        stashRepo: StashRepository,
        walletRepo: WalletRepository,
        categoryRepo: CategoryRepository,
    ): RecordExpense = RecordExpense(expenseRepo, cardRepo, stashRepo, walletRepo, categoryRepo)

    // ── Income use cases ─────────────────────────────────────────────────────

    @Provides
    fun provideListRecurringIncomes(repo: RecurringIncomeRepository): ListRecurringIncomes =
        ListRecurringIncomes(repo)

    @Provides
    fun provideGetRecurringIncome(repo: RecurringIncomeRepository): GetRecurringIncome =
        GetRecurringIncome(repo)

    @Provides
    fun provideAddRecurringIncome(repo: RecurringIncomeRepository): AddRecurringIncome =
        AddRecurringIncome(repo)

    @Provides
    fun provideUpdateRecurringIncome(repo: RecurringIncomeRepository): UpdateRecurringIncome =
        UpdateRecurringIncome(repo)

    @Provides
    fun provideDeleteRecurringIncome(repo: RecurringIncomeRepository): DeleteRecurringIncome =
        DeleteRecurringIncome(repo)

    @Provides
    fun provideGetDueRecurringIncomes(repo: RecurringIncomeRepository): GetDueRecurringIncomes =
        GetDueRecurringIncomes(repo)

    @Provides
    fun provideGenerateRecurringIncome(repo: RecurringIncomeRepository): GenerateRecurringIncome =
        GenerateRecurringIncome(repo)

    @Provides
    fun provideRecordIncome(
        incomeRepo: IncomeRepository,
        cardRepo: CardRepository,
        stashRepo: StashRepository,
        walletRepo: WalletRepository,
    ): RecordIncome = RecordIncome(incomeRepo, cardRepo, stashRepo, walletRepo)
}
