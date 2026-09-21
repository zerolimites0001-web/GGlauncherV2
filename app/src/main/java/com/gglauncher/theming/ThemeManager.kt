package com.gglauncher.theming

import androidx.appcompat.app.AppCompatDelegate
import com.gglauncher.R
import com.gglauncher.data.prefs.PrefsManager

object ThemeManager {
    fun apply(mode: String = PrefsManager.themeMode): Int = when (mode) {
        "amoled" -> R.style.Theme_GGLauncher_Amoled
        "light" -> R.style.Theme_GGLauncher_Light
        "dark" -> R.style.Theme_GGLauncher_Dark
        else -> R.style.Theme_GGLauncher_Dark
    }.also {
        AppCompatDelegate.setDefaultNightMode(
            if (mode == "light") AppCompatDelegate.MODE_NIGHT_NO
            else AppCompatDelegate.MODE_NIGHT_YES
        )
    }
}
