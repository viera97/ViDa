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
    fun provideCrashReportStore(
        @ApplicationContext context: Context,
    ): CrashReportStore {
        val dataStore: DataStore<Preferences> = context.crashDataStore
        return CrashReportStore(context, dataStore)
    }

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
    fun provideCrashReporter(
        store: CrashReportStore,
        deviceInfoProvider: DeviceInfoProvider,
    ): CrashReporter = DefaultCrashReporter(store, deviceInfoProvider)
}
