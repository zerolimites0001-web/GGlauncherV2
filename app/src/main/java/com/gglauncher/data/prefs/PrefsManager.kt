package com.gglauncher.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "gg_prefs")

object PrefsManager {

    private lateinit var appContext: Context

    private val THEME = stringPreferencesKey("theme_mode")

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    var themeMode: String
        get() = runBlocking {
            appContext.dataStore.data.map { it[THEME] ?: "system" }.first()
        }
        set(value) = runBlocking {
            appContext.dataStore.edit { it[THEME] = value }
        }
}
