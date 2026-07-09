package com.vida.app

import android.app.Application
import com.vida.core.crash.CrashHandler
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class ViDaApplication : Application() {

    @Inject
    lateinit var crashHandler: CrashHandler

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        crashHandler.register()
        // TEMP: test crash — solo la primera vez, después de eso el archivo
        // last_crash.json existe y no crashea de nuevo.
        val crashFile = File(filesDir, "last_crash.json")
        if (!crashFile.exists()) {
            throw RuntimeException("Test crash — probando reporte de errores")
        }
    }
}