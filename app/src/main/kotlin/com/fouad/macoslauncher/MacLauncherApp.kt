package com.maclauncher
 
import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
 
class="text-[#D7BA7D]">@HiltAndroidApp
class MacLauncherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.i(class="text-[#CE9178]">"MacOS Launcher v3 initialized")
    }
}