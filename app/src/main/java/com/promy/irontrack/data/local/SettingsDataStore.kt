package com.promy.irontrack.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val KEY_WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val KEY_DEFAULT_REST = intPreferencesKey("default_rest_seconds")
    }

    val weightUnit: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_WEIGHT_UNIT] ?: "kg"
    }

    val defaultRestSeconds: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_REST] ?: 90
    }

    suspend fun setWeightUnit(unit: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WEIGHT_UNIT] = unit
        }
    }

    suspend fun setDefaultRestSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_REST] = seconds
        }
    }
}
