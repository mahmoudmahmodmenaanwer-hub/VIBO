package com.example.data.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val key: String, val labelAr: String) {
    SYSTEM("system", "تلقائي (النظام)"),
    DARK("dark", "الوضع الليلي"),
    LIGHT("light", "الوضع النهاري");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: DARK
    }
}

private val Context.viboDataStore: DataStore<Preferences> by preferencesDataStore(name = "vibo_prefs")

class ThemePreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.viboDataStore.data.map { prefs ->
        ThemeMode.fromKey(prefs[themeKey])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.viboDataStore.edit { prefs ->
            prefs[themeKey] = mode.key
        }
    }
}
