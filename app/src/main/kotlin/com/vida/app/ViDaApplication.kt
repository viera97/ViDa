package com.vida.app

import android.app.Application
import com.vida.core.crash.CrashHandler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ViDaApplication : Application() {

    @Inject
    lateinit var crashHandler: CrashHandler

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        crashHandler.register()
    }
}