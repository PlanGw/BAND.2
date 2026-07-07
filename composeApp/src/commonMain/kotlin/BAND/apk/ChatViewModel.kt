package BAND.apk

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class ChatViewModel(private val userPreferences: UserPreferences) : ViewModel() {
    private val serverIp = "192.168.254.141" 
    private val serverUrl = "http://$serverIp:8080"
    private val wsUrl = "ws://$serverIp:8080/chat"

    private val client = HttpClient {
        install(ContentNegotiation) { json() }
        install(WebSockets) { contentConverter = KotlinxWebsocketSerializationConverter(Json) }
        install(Logging) { level = LogLevel.INFO }
        defaultRequest { url(serverUrl) }
    }

    private val _users = mutableStateListOf<User>()
    val users: List<User> get() = _users

    private val _groupDMs = mutableStateListOf<GroupDM>()
    val groupDMs: List<GroupDM> get() = _groupDMs

    private val _messages = mutableStateListOf<Message>()
    val messages: List<Message> get() = _messages

    private val _changelogs = mutableStateListOf<ChangelogEntry>()
    val changelogs: List<ChangelogEntry> get() = _changelogs

    private val _currentUser = mutableStateOf<User?>(null)
    val currentUser: State<User?> get() = _currentUser

    private val _activeGroupDM = mutableStateOf<GroupDM?>(null)
    val activeGroupDM: State<GroupDM?> get() = _activeGroupDM

    private val _uiSettings = mutableStateOf(UISettings())
    val uiSettings: State<UISettings> get() = _uiSettings

    private val _activeCall = mutableStateOf<String?>(null)
    val activeCall: State<String?> get() = _activeCall

    private var chatSession: DefaultClientWebSocketSession? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val userList: List<User> = client.get("/users").body()
                _users.clear()
                _users.addAll(userList)

                val groupList: List<GroupDM> = client.get("/groups").body()
                _groupDMs.clear()
                _groupDMs.addAll(groupList)

                val userId = userPreferences.loggedInUserId.first()
                if (userId != null) {
                    val user = _users.find { it.id == userId }
                    if (user != null) {
                        _currentUser.value = user
                        connectToChat(user.id)
                    }
                }
            } catch (e: Exception) {
                println("Failed to load data from server: ${e.message}")
            }
        }
    }

    private fun connectToChat(userId: String) {
        viewModelScope.launch {
            try {
                client.webSocket("$wsUrl/$userId") {
                    chatSession = this
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val msg = Json.decodeFromString<Message>(frame.readText())
                            if (_activeGroupDM.value?.id == msg.chatId) {
                                _messages.add(msg)
                                _messages.sortBy { it.timestamp }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("WebSocket error: ${e.message}")
            }
        }
    }

    fun login(name: String, password: String): Boolean {
        viewModelScope.launch {
            try {
                val response = client.post("/login") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("name" to name, "password" to password))
                }
                if (response.status == HttpStatusCode.OK) {
                    val user: User = response.body()
                    _currentUser.value = user
                    userPreferences.saveUserId(user.id)
                    connectToChat(user.id)
                }
            } catch (e: Exception) {
                println("Login failed: ${e.message}")
            }
        }
        return true
    }

    fun register(name: String, password: String): Boolean {
        viewModelScope.launch {
            try {
                val newUser = User(id = "", name = name, password = password)
                val response = client.post("/register") {
                    contentType(ContentType.Application.Json)
                    setBody(newUser)
                }
                if (response.status == HttpStatusCode.OK) {
                    val user: User = response.body()
                    _currentUser.value = user
                    userPreferences.saveUserId(user.id)
                    connectToChat(user.id)
                }
            } catch (e: Exception) {
                println("Registration failed: ${e.message}")
            }
        }
        return true
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.saveUserId(null)
            _currentUser.value = null
            chatSession?.close()
        }
    }

    fun selectGroupDM(group: GroupDM) {
        _activeGroupDM.value = group
        _messages.clear()
    }

    fun createGroupDM(name: String, participantIds: List<String>) {
        val user = _currentUser.value ?: return
        val finalParticipants = participantIds.toMutableList()
        if (!finalParticipants.contains(user.id)) finalParticipants.add(user.id)
        
        val newGroup = GroupDM(
            id = "",
            name = if (name.isBlank()) null else name,
            participantIds = finalParticipants,
            moderatorIds = listOf(user.id)
        )
        viewModelScope.launch {
            try {
                val response = client.post("/groups") {
                    contentType(ContentType.Application.Json)
                    setBody(newGroup)
                }
                if (response.status == HttpStatusCode.OK) {
                    val group: GroupDM = response.body()
                    _groupDMs.add(group)
                    selectGroupDM(group)
                }
            } catch (e: Exception) {
                println("Group creation failed: ${e.message}")
            }
        }
    }

    fun sendMessage(content: String, type: MessageType = MessageType.TEXT, fileName: String? = null) {
        val user = _currentUser.value ?: return
        val activeGroup = _activeGroupDM.value ?: return
        
        viewModelScope.launch {
            val message = Message(
                id = "",
                senderId = user.id,
                content = content,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                chatId = activeGroup.id,
                type = type,
                fileName = fileName
            )
            chatSession?.sendSerialized(message)
        }
    }

    fun uploadAndSendFile(localPath: String, isImage: Boolean) {
        sendMessage("File: $localPath", if (isImage) MessageType.IMAGE else MessageType.FILE, localPath.substringAfterLast("/"))
    }

    fun updateProfile(bio: String, avatarUrl: String?, bannerColor: Color, pronouns: String, messageColor: Color = Color.White) {
        val user = _currentUser.value ?: return
        val updated = user.copy(bio = bio, avatarUrl = avatarUrl, bannerColorArgb = bannerColor.toArgb(), pronouns = pronouns, messageColorArgb = messageColor.toArgb())
        viewModelScope.launch {
            try {
                val response = client.patch("/users/${user.id}") {
                    contentType(ContentType.Application.Json)
                    setBody(updated)
                }
                if (response.status == HttpStatusCode.OK) {
                    _currentUser.value = response.body()
                }
            } catch (e: Exception) {
                println("Profile update failed: ${e.message}")
            }
        }
    }

    fun toggleFriend(targetUserId: String) {
        val user = _currentUser.value ?: return
        val updatedFriends = user.friendIds.toMutableList()
        if (updatedFriends.contains(targetUserId)) updatedFriends.remove(targetUserId)
        else updatedFriends.add(targetUserId)
        
        val updated = user.copy(friendIds = updatedFriends)
        viewModelScope.launch {
            try {
                client.patch("/users/${user.id}") {
                    contentType(ContentType.Application.Json)
                    setBody(updated)
                }
            } catch (e: Exception) {}
        }
    }

    fun timeoutUser(userId: String) {
        val target = _users.find { it.id == userId } ?: return
        val updated = target.copy(isTimedOut = !target.isTimedOut)
        viewModelScope.launch {
            try {
                client.patch("/users/$userId") {
                    contentType(ContentType.Application.Json)
                    setBody(updated)
                }
            } catch (e: Exception) {}
        }
    }

    fun banUser(userId: String) {
        val target = _users.find { it.id == userId } ?: return
        val updated = target.copy(isBanned = !target.isBanned)
        viewModelScope.launch {
            try {
                client.patch("/users/$userId") {
                    contentType(ContentType.Application.Json)
                    setBody(updated)
                }
            } catch (e: Exception) {}
        }
    }

    fun giveAdmin(userId: String) {
        val target = _users.find { it.id == userId } ?: return
        val updated = target.copy(isAdmin = true, tag = "Admin", tagColorArgb = Color.Red.toArgb())
        viewModelScope.launch {
            try {
                client.patch("/users/$userId") {
                    contentType(ContentType.Application.Json)
                    setBody(updated)
                }
            } catch (e: Exception) {}
        }
    }

    fun addCustomTag(userId: String, label: String, color: Color) {
        val target = _users.find { it.id == userId } ?: return
        val newTag = CustomTag(label, color.toArgb())
        val updated = target.copy(customTags = target.customTags + newTag)
        viewModelScope.launch {
            try {
                client.patch("/users/$userId") {
                    contentType(ContentType.Application.Json)
                    setBody(updated)
                }
            } catch (e: Exception) {}
        }
    }

    fun addChangelog(title: String, content: String) {
        // ... Similar POST logic for changelogs if added to server
    }

    fun updateUISettings(newSettings: UISettings) { _uiSettings.value = newSettings }
    fun startCall(type: String) { _activeCall.value = type }
    fun endCall() { _activeCall.value = null }
    fun getUserById(id: String): User? = _users.find { it.id == id }
    fun getOtherUsers(): List<User> = _users.filter { it.id != _currentUser.value?.id }
    fun getParticipantNames(group: GroupDM): String = _users.filter { group.participantIds.contains(it.id) }.joinToString(", ") { it.name }
    fun removeUserFromGroup(groupId: String, userId: String) {}
}
