package com.gglauncher

import android.app.Application
import com.gglauncher.data.apps.AppRepository
import com.gglauncher.data.prefs.PrefsManager

class GGApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // M0: init leve, nada bloqueante. Prefs/AppRepo são lazy singletons.
        PrefsManager.init(this)
        AppRepository.init(this)
    }
}
