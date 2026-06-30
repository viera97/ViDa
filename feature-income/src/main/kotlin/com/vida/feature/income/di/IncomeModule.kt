package com.vida.feature.income.di

import com.vida.domain.repository.IncomeRepository
import com.vida.domain.usecase.income.AddIncome
import com.vida.domain.usecase.income.GetIncome
import com.vida.domain.usecase.income.UpdateIncome
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing income-form-specific use cases.
 *
 * NOTE: [com.vida.domain.usecase.card.ListCards] and
 * [com.vida.domain.usecase.stash.ListStashes] are NOT provided here because
 * they are already provided by
 * [com.vida.feature.home.di.HomeModule] in the same [ViewModelComponent].
 * Both feature modules share the same Dagger component, so bindings from
 * either module are visible to all ViewModels.
 */
@Module
@InstallIn(ViewModelComponent::class)
object IncomeModule {

    @Provides
    fun provideAddIncome(repo: IncomeRepository): AddIncome =
        AddIncome(repo)

    @Provides
    fun provideUpdateIncome(repo: IncomeRepository): UpdateIncome =
        UpdateIncome(repo)

    @Provides
    fun provideGetIncome(repo: IncomeRepository): GetIncome =
        GetIncome(repo)
}
