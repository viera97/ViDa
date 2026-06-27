package com.vida.feature.expense.di

import com.vida.domain.repository.CategoryRepository
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.WalletRepository
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.expense.AddExpense
import com.vida.domain.usecase.expense.GetExpense
import com.vida.domain.usecase.expense.UpdateExpense
import com.vida.domain.usecase.wallet.GetWallet
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing expense-form-specific use cases.
 *
 * NOTE: [ListCards] and [ListStashes] are NOT provided here because they are
 * already provided by [com.vida.feature.home.di.HomeModule] in the same
 * [ViewModelComponent]. Both feature modules share the same Dagger component,
 * so bindings from either module are visible to all ViewModels.
 */
@Module
@InstallIn(ViewModelComponent::class)
object ExpenseModule {

    @Provides
    fun provideAddExpense(repo: ExpenseRepository): AddExpense =
        AddExpense(repo)

    @Provides
    fun provideUpdateExpense(repo: ExpenseRepository): UpdateExpense =
        UpdateExpense(repo)

    @Provides
    fun provideGetExpense(repo: ExpenseRepository): GetExpense =
        GetExpense(repo)

    @Provides
    fun provideListCategories(repo: CategoryRepository): ListCategories =
        ListCategories(repo)

    @Provides
    fun provideGetWallet(repo: WalletRepository): GetWallet =
        GetWallet(repo)
}
