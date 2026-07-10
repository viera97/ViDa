package com.vida.feature.bankmanagement.di

import com.vida.domain.repository.BankRepository
import com.vida.domain.usecase.bank.AddBank
import com.vida.domain.usecase.bank.DeleteBank
import com.vida.domain.usecase.bank.GetBank
import com.vida.domain.usecase.bank.UpdateBank
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing bank-specific use cases for ViewModels.
 *
 * NOTE: [com.vida.domain.usecase.bank.ListBanks] is NOT provided here —
 * it is already provided by the data layer's DI or another module in the
 * same [ViewModelComponent]. Duplicating it would cause a Dagger binding error.
 */
@Module
@InstallIn(ViewModelComponent::class)
object BankModule {

    @Provides
    fun provideAddBank(repo: BankRepository): AddBank =
        AddBank(repo)

    @Provides
    fun provideUpdateBank(repo: BankRepository): UpdateBank =
        UpdateBank(repo)

    @Provides
    fun provideDeleteBank(repo: BankRepository): DeleteBank =
        DeleteBank(repo)

    @Provides
    fun provideGetBank(repo: BankRepository): GetBank =
        GetBank(repo)
}
