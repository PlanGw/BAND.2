package BAND.apk

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

actual class UserPreferences(private val context: Context) {
    companion object {
        val LOGGED_IN_USER_ID = stringPreferencesKey("logged_in_user_id")
        val USERS_JSON = stringPreferencesKey("users_json")
        val GROUPS_JSON = stringPreferencesKey("groups_json")
        val MESSAGES_JSON = stringPreferencesKey("messages_json")
        val CHANGELOGS_JSON = stringPreferencesKey("changelogs_json")
    }

    actual val loggedInUserId: Flow<String?> = context.dataStore.data.map { it[LOGGED_IN_USER_ID] }
    actual suspend fun saveUserId(userId: String?) {
        context.dataStore.edit { if (userId == null) it.remove(LOGGED_IN_USER_ID) else it[LOGGED_IN_USER_ID] = userId }
    }

    actual val usersJson: Flow<String?> = context.dataStore.data.map { it[USERS_JSON] }
    actual suspend fun saveUsersJson(json: String?) {
        context.dataStore.edit { if (json == null) it.remove(USERS_JSON) else it[USERS_JSON] = json }
    }

    actual val groupsJson: Flow<String?> = context.dataStore.data.map { it[GROUPS_JSON] }
    actual suspend fun saveGroupsJson(json: String?) {
        context.dataStore.edit { if (json == null) it.remove(GROUPS_JSON) else it[GROUPS_JSON] = json }
    }

    actual val messagesJson: Flow<String?> = context.dataStore.data.map { it[MESSAGES_JSON] }
    actual suspend fun saveMessagesJson(json: String?) {
        context.dataStore.edit { if (json == null) it.remove(MESSAGES_JSON) else it[MESSAGES_JSON] = json }
    }

    actual val changelogsJson: Flow<String?> = context.dataStore.data.map { it[CHANGELOGS_JSON] }
    actual suspend fun saveChangelogsJson(json: String?) {
        context.dataStore.edit { if (json == null) it.remove(CHANGELOGS_JSON) else it[CHANGELOGS_JSON] = json }
    }
}
