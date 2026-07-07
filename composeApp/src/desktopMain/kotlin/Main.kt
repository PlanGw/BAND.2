import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import BAND.apk.App
import BAND.apk.ChatViewModel
import BAND.apk.UserPreferences

fun main() = application {
    val viewModel = ChatViewModel(UserPreferences())
    Window(onCloseRequest = ::exitApplication, title = "BAND") {
        App(viewModel)
    }
}
