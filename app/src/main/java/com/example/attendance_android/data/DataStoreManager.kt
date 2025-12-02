package com.example.attendance_android.data


import androidx.datastore.preferences.core.stringPreferencesKey
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.example.attendance_android.utils.Constants
import kotlinx.coroutines.flow.firstOrNull
// inside companion object (add these)


import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.stringPreferencesKey
val EMBEDDING = stringPreferencesKey(Constants.EMBEDDING_PREF_KEY)
val EMBEDDING_MODEL_VERSION = stringPreferencesKey("embedding_model_version")
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

    suspend fun setrollNumber(value: String) {
        context.dataStore.edit { prefs ->
            prefs[RollNumber] = value
        }
    }
    suspend fun setCollege(value: String) {
        context.dataStore.edit { prefs ->
            prefs[COLLEGE] = value
        }
    }

    // Clear all preferences (logout)
    suspend fun clearAllPreferences() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    // Save a FloatArray embedding as Base64 string in DataStore
    suspend fun saveEmbedding(embedding: FloatArray, modelVersion: String = "1") {
        val bytes = floatArrayToByteArray(embedding)
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        context.dataStore.edit { prefs ->
            prefs[EMBEDDING] = encoded
            prefs[EMBEDDING_MODEL_VERSION] = modelVersion
        }
    }

    // Load embedding; returns null if none
    suspend fun loadEmbedding(): FloatArray? {
        val prefs = context.dataStore.data.map { it[EMBEDDING] }.firstOrNull()
        val encoded = prefs ?: return null
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        return byteArrayToFloatArray(bytes)
    }
    private fun floatArrayToByteArray(arr: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(arr.size * 4).order(ByteOrder.nativeOrder())
        arr.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    private fun byteArrayToFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
        val floats = FloatArray(bytes.size / 4)
        for (i in floats.indices) floats[i] = buffer.getFloat()
        return floats
    }


    val name : Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[NAME] ?: ""
        }
    val email: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[EMAIL] ?: ""
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
    val rollNumber: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[RollNumber] ?: ""
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
