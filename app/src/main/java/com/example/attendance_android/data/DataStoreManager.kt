package com.example.attendance_android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
    }

    // Write - set flag
    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = isLoggedIn
        }
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_ONBOARDING_COMPLETE] = value
        }
    }

    // Read - returns Flow<Boolean>
    val isLoggedIn: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[IS_LOGGED_IN] ?: false
        }

    val isOnboardingComplete: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[IS_ONBOARDING_COMPLETE] ?: false
        }
}
