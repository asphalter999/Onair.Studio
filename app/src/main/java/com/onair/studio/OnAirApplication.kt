package com.onair.studio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OnAirApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
