package com.vida.feature.onboarding.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vida.feature.onboarding.preferences.WizardPreferences
import com.vida.feature.onboarding.preferences.onboardingDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the wizard preferences DataStore and wrapper.
 *
 * The DataStore is built lazily by the `Context.onboardingDataStore` delegate
 * declared in [com.vida.feature.onboarding.preferences.WizardPreferences];
 * Hilt just hands the singleton instance to [WizardPreferences].
 */
@Module
@InstallIn(SingletonComponent::class)
object OnboardingModule {

    @Provides
    @Singleton
    fun provideOnboardingDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.onboardingDataStore

    @Provides
    @Singleton
    fun provideWizardPreferences(
        dataStore: DataStore<Preferences>,
    ): WizardPreferences = WizardPreferences(dataStore)
}
