package com.vida.feature.recurringexpensemanagement.di

import com.vida.domain.repository.CardRepository
import com.vida.domain.repository.CategoryRepository
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.RecurringExpenseRepository
import com.vida.domain.repository.StashRepository
import com.vida.domain.repository.WalletRepository
import com.vida.domain.usecase.expense.RecordExpense
import com.vida.domain.usecase.recurring.AddRecurringExpense
import com.vida.domain.usecase.recurring.DeleteRecurringExpense
import com.vida.domain.usecase.recurring.GenerateRecurringExpense
import com.vida.domain.usecase.recurring.GetDueRecurringExpenses
import com.vida.domain.usecase.recurring.GetRecurringExpense
import com.vida.domain.usecase.recurring.ListRecurringExpenses
import com.vida.domain.usecase.recurring.UpdateRecurringExpense
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing recurring-expense-specific use cases for ViewModels.
 *
 * Cross-module bindings used by [com.vida.feature.recurringexpensemanagement.RecurringListViewModel]:
 * - [com.vida.domain.usecase.category.ListCategories] → [com.vida.feature.expense.di.ExpenseModule]
 * - [com.vida.domain.usecase.card.ListCards] → [com.vida.feature.home.di.HomeModule]
 * - [com.vida.domain.usecase.stash.ListStashes] → [com.vida.feature.home.di.HomeModule]
 */
@Module
@InstallIn(ViewModelComponent::class)
object RecurringModule {

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
}
