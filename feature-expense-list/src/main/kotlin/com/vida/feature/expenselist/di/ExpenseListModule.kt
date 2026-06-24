package com.vida.feature.expenselist.di

import com.vida.domain.repository.CardRepository
import com.vida.domain.repository.CategoryRepository
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.RefundRepository
import com.vida.domain.repository.StashRepository
import com.vida.domain.repository.WalletRepository
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.category.ListCategories
import com.vida.domain.usecase.expense.DeleteExpense
import com.vida.domain.usecase.expense.GetExpense
import com.vida.domain.usecase.expense.SearchExpenses
import com.vida.domain.usecase.refund.AddRefund
import com.vida.domain.usecase.refund.DeleteRefund
import com.vida.domain.usecase.refund.GetRefundsByOriginalExpense
import com.vida.domain.usecase.refund.UpdateRefund
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.GetWallet
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing expense-list-specific use cases.
 *
 * NOTE: [ListCards], [ListStashes], [ListCategories], and [GetWallet] are
 * also provided by [com.vida.feature.home.di.HomeModule] in the same
 * [ViewModelComponent]. Duplicate bindings in the same component will cause
 * a Dagger compilation error, so we only provide the use cases NOT already
 * provided by other feature modules.
 *
 * [SearchExpenses], [GetExpense], and [DeleteExpense] are new for this
 * feature and MUST be provided here.
 */
@Module
@InstallIn(ViewModelComponent::class)
object ExpenseListModule {

    @Provides
    fun provideSearchExpenses(repo: ExpenseRepository): SearchExpenses =
        SearchExpenses(repo)

    @Provides
    fun provideGetExpense(repo: ExpenseRepository): GetExpense =
        GetExpense(repo)

    @Provides
    fun provideDeleteExpense(repo: ExpenseRepository): DeleteExpense =
        DeleteExpense(repo)

    @Provides
    fun provideGetRefundsByOriginalExpense(repo: RefundRepository): GetRefundsByOriginalExpense =
        GetRefundsByOriginalExpense(repo)

    @Provides
    fun provideAddRefund(repo: RefundRepository): AddRefund =
        AddRefund(repo)

    @Provides
    fun provideUpdateRefund(repo: RefundRepository): UpdateRefund =
        UpdateRefund(repo)

    @Provides
    fun provideDeleteRefund(repo: RefundRepository): DeleteRefund =
        DeleteRefund(repo)
}
