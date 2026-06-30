package com.vida.feature.reports.di

import com.vida.domain.repository.CategoryRepository
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.IncomeRepository
import com.vida.domain.usecase.statistics.GetPeriodReports
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing the [GetPeriodReports] use case for [com.vida.feature.reports.ReportsViewModel].
 *
 * Mirrors [com.vida.feature.statistics.di.StatisticsModule] style: explicit `@Provides`
 * because the use case takes constructor-injected repositories (no auto-binding).
 */
@Module
@InstallIn(ViewModelComponent::class)
object ReportsModule {

    @Provides
    fun provideGetPeriodReports(
        expenseRepository: ExpenseRepository,
        incomeRepository: IncomeRepository,
        categoryRepository: CategoryRepository,
    ): GetPeriodReports = GetPeriodReports(expenseRepository, incomeRepository, categoryRepository)
}
