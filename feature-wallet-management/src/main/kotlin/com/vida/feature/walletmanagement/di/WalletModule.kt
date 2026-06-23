package com.vida.feature.walletmanagement.di

import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.WalletRepository
import com.vida.domain.usecase.expense.GetExpensesBySource
import com.vida.domain.usecase.wallet.UpdateWallet
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing wallet-specific use cases for
 * [com.vida.feature.walletmanagement.WalletViewModel].
 *
 * NOTE: [com.vida.domain.usecase.wallet.GetWallet] is intentionally NOT provided
 * here — it is already provided by [com.vida.feature.expense.di.ExpenseModule] in
 * the same [ViewModelComponent]. Duplicating it would cause a Dagger binding error.
 *
 * NOTE: [com.vida.domain.usecase.wallet.GetWalletBalance] is provided by
 * [com.vida.feature.home.di.HomeModule], also in [ViewModelComponent].
 */
@Module
@InstallIn(ViewModelComponent::class)
object WalletModule {

    @Provides
    fun provideUpdateWallet(repo: WalletRepository): UpdateWallet =
        UpdateWallet(repo)

    @Provides
    fun provideGetExpensesBySource(repo: ExpenseRepository): GetExpensesBySource =
        GetExpensesBySource(repo)
}
