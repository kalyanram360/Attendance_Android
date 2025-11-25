package com.example.attendance_android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.stringPreferencesKey
private val Context.dataStore by preferencesDataStore("user_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
        val IS_STUDENT = booleanPreferencesKey("is_student")

        val ROLE = stringPreferencesKey("role")
        val NAME = stringPreferencesKey("name")
        val EMAIL = stringPreferencesKey("email")
        val COLLEGE = stringPreferencesKey("college")
        val RollNumber = stringPreferencesKey("roll_number")

        val BRANCH = stringPreferencesKey("branch")
        val SECTION = stringPreferencesKey("section")
        val YEAR = stringPreferencesKey("year")



    }

    // Write - set flag
    suspend fun setBranch(value: String) {
        context.dataStore.edit { prefs ->
            prefs[BRANCH] = value
        }
    }
    suspend fun setSection(value: String) {
        context.dataStore.edit { prefs ->
            prefs[SECTION] = value
        }
    }
    suspend fun setYear(value: String) {
        context.dataStore.edit { prefs ->
            prefs[YEAR] = value
        }
    }

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
    suspend fun setStudent(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_STUDENT] = value
        }
    }

    suspend fun setRole(value: String) {
        context.dataStore.edit { prefs ->
            prefs[ROLE] = value
        }
    }
    suspend fun setName(value: String) {
        context.dataStore.edit { prefs ->
            prefs[NAME] = value
        }
    }
    suspend fun setEmail(value: String) {
        context.dataStore.edit { prefs ->
            prefs[EMAIL] = value
        }
    }

    suspend fun RollNumber(value: String) {
        context.dataStore.edit { prefs ->
            prefs[RollNumber] = value
        }
    }
    suspend fun setCollege(value: String) {
        context.dataStore.edit { prefs ->
            prefs[COLLEGE] = value
        }
    }


    // Read - returns Flow<Boolean>
    val branch: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[BRANCH] ?: ""
        }
    val section: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[SECTION] ?: ""
        }
    val year: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[YEAR] ?: ""
        }


    val isLoggedIn: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[IS_LOGGED_IN] ?: false
        }

    val isOnboardingComplete: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[IS_ONBOARDING_COMPLETE] ?: false
        }
    val isStudent : Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[IS_STUDENT] ?: false
        }
    val userRole: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[ROLE] ?: ""
        }


}
