package com.vida.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ViDaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
    }
}