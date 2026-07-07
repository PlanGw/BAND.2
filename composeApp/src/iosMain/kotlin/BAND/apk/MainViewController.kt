package BAND.apk

import androidx.compose.ui.window.ComposeUIViewController

// In a real app, UserPreferences would be implemented via expect/actual for iOS (e.g. using NSUserDefaults)
fun MainViewController() = ComposeUIViewController {
    // For now, iOS target is just a placeholder to show it's possible
    // We'd need an actual class for UserPreferences on iOS
    // App(ChatViewModel(UserPreferences()))
}
