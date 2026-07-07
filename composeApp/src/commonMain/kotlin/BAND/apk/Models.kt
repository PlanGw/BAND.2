package BAND.apk

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
enum class MessageType {
    TEXT, IMAGE, FILE
}

@Serializable
data class User(
    val id: String,
    val name: String,
    val password: String,
    val tag: String? = null,
    val tagColorArgb: Int = Color.Gray.toArgb(),
    val avatarUrl: String? = null,
    val isGifAvatar: Boolean = false,
    val friendIds: List<String> = emptyList(),
    val bio: String = "",
    val bannerColorArgb: Int = Color(0xFF5865F2).toArgb(),
    val isAdmin: Boolean = false,
    val isTimedOut: Boolean = false,
    val isBanned: Boolean = false,
    val customTags: List<CustomTag> = emptyList(),
    val pronouns: String = "",
    val status: String = "Online",
    val messageColorArgb: Int = Color.White.toArgb()
) {
    val tagColor: Color get() = Color(tagColorArgb)
    val bannerColor: Color get() = Color(bannerColorArgb)
    val messageColor: Color get() = Color(messageColorArgb)
}

@Serializable
data class CustomTag(
    val label: String,
    val colorArgb: Int
) {
    val color: Color get() = Color(colorArgb)
}

@Serializable
data class Message(
    val id: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val chatId: String,
    val type: MessageType = MessageType.TEXT,
    val fileName: String? = null
)

@Serializable
data class GroupDM(
    val id: String,
    val name: String? = null,
    val participantIds: List<String>,
    val iconUrl: String? = null,
    val moderatorIds: List<String> = emptyList()
)

@Serializable
data class ChangelogEntry(
    val id: String,
    val date: Long,
    val title: String,
    val content: String,
    val authorId: String
)

@Serializable
data class UISettings(
    val useCompactMode: Boolean = false,
    val themeColorArgb: Int = Color(0xFF313338).toArgb(),
    val appName: String = "BAND",
    val appIconUri: String? = null,
    val selectedFontName: String = "Default"
) {
    val themeColor: Color get() = Color(themeColorArgb)
}

val INITIAL_CHANGELOGS = listOf(
    ChangelogEntry("1", Clock.System.now().toEpochMilliseconds(), "Welcome to BAND", "Initial release of the BAND messaging app.", "1")
)

val PREMADE_USERS = mutableListOf(
    User(id = "1", name = "PlanA", password = "PlanA_real.tm", tag = "Owner", tagColorArgb = Color(0xFFFF1493).toArgb(), isAdmin = true, bio = "Owner of BAND"),
    User(id = "2", name = "Thor", password = "Th0r_OdinSon_77!#", tag = "Co-Owner", tagColorArgb = Color(0xFF57F287).toArgb(), isAdmin = true, bio = "Co-Owner of BAND")
)

val PREMADE_GROUPS = mutableListOf(
    GroupDM(id = "1", name = "BAND Developers", participantIds = listOf("1", "2"), moderatorIds = listOf("1"))
)
