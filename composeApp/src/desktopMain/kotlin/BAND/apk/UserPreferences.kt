package BAND.apk

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

actual class UserPreferences {
    private val dataStore = PreferenceDataStoreFactory.create {
        File(System.getProperty("user.home"), ".band_user_prefs.preferences_pb")
    }

    companion object {
        val LOGGED_IN_USER_ID = stringPreferencesKey("logged_in_user_id")
    }

    actual val loggedInUserId: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[LOGGED_IN_USER_ID]
        }

    actual suspend fun saveUserId(userId: String?) {
        dataStore.edit { preferences ->
            if (userId == null) {
                preferences.remove(LOGGED_IN_USER_ID)
            } else {
                preferences[LOGGED_IN_USER_ID] = userId
            }
        }
    }
}
