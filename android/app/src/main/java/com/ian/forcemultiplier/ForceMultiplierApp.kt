package com.ian.forcemultiplier

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ForceMultiplierApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}
