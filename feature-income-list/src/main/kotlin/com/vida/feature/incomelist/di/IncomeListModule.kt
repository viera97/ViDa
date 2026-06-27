package com.vida.feature.incomelist.di

import com.vida.domain.repository.IncomeRepository
import com.vida.domain.usecase.income.DeleteIncome
import com.vida.domain.usecase.income.SearchIncomes
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object IncomeListModule {

    @Provides
    fun provideSearchIncomes(repo: IncomeRepository): SearchIncomes =
        SearchIncomes(repo)

    @Provides
    fun provideDeleteIncome(repo: IncomeRepository): DeleteIncome =
        DeleteIncome(repo)
}
