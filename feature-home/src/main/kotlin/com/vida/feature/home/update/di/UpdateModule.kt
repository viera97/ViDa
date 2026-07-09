package com.vida.feature.home.update.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

/**
 * Hilt bindings for the in-app updater.
 *
 * Only the [OkHttpClient] needs an explicit `@Provides` — the rest of the
 * updater ([UpdateManager], [VersionProvider], [ApkInstaller]) all use
 * `@Inject` constructors, so Hilt wires them up automatically.
 *
 * 15s connect + 60s read timeouts are generous enough for the GitHub release
 * metadata endpoint and a 10–30 MB APK on a slow network, without leaving
 * the user hanging on a dead socket.
 */
@Module
@InstallIn(SingletonComponent::class)
object UpdateModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
}
