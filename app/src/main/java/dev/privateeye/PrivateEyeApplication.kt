package dev.privateeye

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * PrivateEye Application class
 * Initializes Hilt dependency injection
 */
@HiltAndroidApp
class PrivateEyeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}
