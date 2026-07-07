package BAND.apk

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class UserPreferences {
    actual val loggedInUserId: Flow<String?> = flowOf(null)
    actual suspend fun saveUserId(userId: String?) {
        // Placeholder for iOS implementation (e.g. NSUserDefaults)
    }
}
