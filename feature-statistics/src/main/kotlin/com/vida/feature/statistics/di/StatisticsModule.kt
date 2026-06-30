package com.vida.feature.statistics.di

import com.vida.domain.repository.CategoryRepository
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.IncomeRepository
import com.vida.domain.usecase.statistics.GetCategoryBreakdown
import com.vida.domain.usecase.statistics.GetCashFlowTrend
import com.vida.domain.usecase.statistics.GetCurrencyComposition
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing statistics use cases for [StatisticsViewModel].
 *
 * Follows the same pattern as [com.vida.feature.ratemanagement.di.RateModule]:
 * each use case is explicitly provided because they take constructor-injected
 * repositories (no auto-binding).
 */
@Module
@InstallIn(ViewModelComponent::class)
object StatisticsModule {

    @Provides
    fun provideGetCategoryBreakdown(
        expenseRepository: ExpenseRepository,
        categoryRepository: CategoryRepository,
    ): GetCategoryBreakdown = GetCategoryBreakdown(expenseRepository, categoryRepository)

    @Provides
    fun provideGetCashFlowTrend(
        expenseRepository: ExpenseRepository,
        incomeRepository: IncomeRepository,
    ): GetCashFlowTrend = GetCashFlowTrend(expenseRepository, incomeRepository)

    @Provides
    fun provideGetCurrencyComposition(
        expenseRepository: ExpenseRepository,
        incomeRepository: IncomeRepository,
    ): GetCurrencyComposition = GetCurrencyComposition(expenseRepository, incomeRepository)
}
