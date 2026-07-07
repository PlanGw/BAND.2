package BAND.apk

import kotlinx.coroutines.flow.Flow

expect class UserPreferences {
    val loggedInUserId: Flow<String?>
    suspend fun saveUserId(userId: String?)
    
    val usersJson: Flow<String?>
    suspend fun saveUsersJson(json: String?)

    val groupsJson: Flow<String?>
    suspend fun saveGroupsJson(json: String?)

    val messagesJson: Flow<String?>
    suspend fun saveMessagesJson(json: String?)

    val changelogsJson: Flow<String?>
    suspend fun saveChangelogsJson(json: String?)
}
