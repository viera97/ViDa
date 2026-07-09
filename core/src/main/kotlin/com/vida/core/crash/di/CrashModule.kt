package com.vida.core.crash.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.vida.core.crash.AndroidDeviceInfoProvider
import com.vida.core.crash.CrashReportStore
import com.vida.core.crash.CrashReporter
import com.vida.core.crash.DefaultCrashReporter
import com.vida.core.crash.DeviceInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.crashDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "crash_reports"
)

@Module
@InstallIn(SingletonComponent::class)
object CrashModule {

    @Provides
    @Singleton
    fun provideCrashDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.crashDataStore

    @Provides
    @Singleton
    fun provideDeviceInfoProvider(
        @ApplicationContext context: Context,
    ): DeviceInfoProvider = AndroidDeviceInfoProvider(
        packageManager = context.packageManager,
        packageName = context.packageName,
    )

    @Provides
    @Singleton
    fun provideCrashReportStore(
        @ApplicationContext context: Context,
        dataStore: DataStore<Preferences>,
    ): CrashReportStore = CrashReportStore(context, dataStore)

    @Provides
    @Singleton
    fun provideCrashReporter(
        store: CrashReportStore,
        deviceInfoProvider: DeviceInfoProvider,
    ): CrashReporter = DefaultCrashReporter(store, deviceInfoProvider)
}
