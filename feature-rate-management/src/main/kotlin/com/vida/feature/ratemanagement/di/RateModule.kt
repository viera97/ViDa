package com.vida.feature.ratemanagement.di

import com.vida.domain.repository.CurrencyRateRepository
import com.vida.domain.usecase.rate.AddCurrencyRate
import com.vida.domain.usecase.rate.DeleteCurrencyRate
import com.vida.domain.usecase.rate.ListCurrencyRates
import com.vida.domain.usecase.rate.UpdateCurrencyRate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing all four currency-rate use cases for ViewModels.
 *
 * Unlike [com.vida.feature.stashmanagement.di.StashModule], [ListCurrencyRates]
 * is provided here. It is NOT provided by [com.vida.feature.home.di.HomeModule].
 */
@Module
@InstallIn(ViewModelComponent::class)
object RateModule {

    @Provides
    fun provideListCurrencyRates(repo: CurrencyRateRepository): ListCurrencyRates =
        ListCurrencyRates(repo)

    @Provides
    fun provideAddCurrencyRate(repo: CurrencyRateRepository): AddCurrencyRate =
        AddCurrencyRate(repo)

    @Provides
    fun provideUpdateCurrencyRate(repo: CurrencyRateRepository): UpdateCurrencyRate =
        UpdateCurrencyRate(repo)

    @Provides
    fun provideDeleteCurrencyRate(repo: CurrencyRateRepository): DeleteCurrencyRate =
        DeleteCurrencyRate(repo)
}
