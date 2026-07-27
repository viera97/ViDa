package com.vida.feature.currencymanagement.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for the currency feature module.
 *
 * Currency use cases are already provided by the data layer's
 * [com.vida.data.di.DatabaseModule]. This module exists as a marker
 * for future feature-scoped providers.
 */
@Module
@InstallIn(SingletonComponent::class)
object CurrencyModule
