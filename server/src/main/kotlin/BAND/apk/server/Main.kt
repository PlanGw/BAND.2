package BAND.apk.server

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Serializable
enum class MessageType { TEXT, IMAGE, FILE }

@Serializable
data class User(
    val id: String,
    val name: String,
    val password: String,
    val tag: String? = null,
    val tagColorArgb: Int = -7829368,
    val avatarUrl: String? = null,
    val isGifAvatar: Boolean = false,
    val friendIds: List<String> = emptyList(),
    val bio: String = "",
    val bannerColorArgb: Int = -10984206,
    val isAdmin: Boolean = false,
    val isTimedOut: Boolean = false,
    val isBanned: Boolean = false,
    val customTags: List<CustomTag> = emptyList(),
    val pronouns: String = "",
    val status: String = "Offline",
    val messageColorArgb: Int = -1
)

@Serializable
data class CustomTag(val label: String, val colorArgb: Int)

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

val usersFile = File("users.json")
val messagesFile = File("messages.json")
val groupsFile = File("groups.json")

val users = ConcurrentHashMap<String, User>()
val messages = mutableListOf<Message>()
val groups = ConcurrentHashMap<String, GroupDM>()
val sessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

fun main() {
    loadData()
    // Add premade users if empty
    if (users.isEmpty()) {
        val p1 = User(id = "1", name = "PlanA", password = "PlanA_real.tm", tag = "Owner", tagColorArgb = -60013, isAdmin = true, bio = "Owner of BAND")
        val p2 = User(id = "2", name = "Thor", password = "Th0r_OdinSon_77!#", tag = "Co-Owner", tagColorArgb = -11013497, isAdmin = true, bio = "Co-Owner of BAND")
        users[p1.id] = p1
        users[p2.id] = p2
        saveData()
    }

    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) { json() }
        install(WebSockets) {
            pingPeriodMillis = 15000
            timeoutMillis = 15000
        }
        
        routing {
            post("/register") {
                val user = call.receive<User>()
                if (users.values.any { it.name == user.name }) {
                    call.respond(io.ktor.http.HttpStatusCode.Conflict, "Username taken")
                } else {
                    val newUser = user.copy(id = UUID.randomUUID().toString())
                    users[newUser.id] = newUser
                    saveData()
                    call.respond(newUser)
                }
            }
            
            post("/login") {
                val credentials = call.receive<Map<String, String>>()
                val name = credentials["name"]
                val pass = credentials["password"]
                val user = users.values.find { it.name == name && it.password == pass }
                if (user != null) call.respond(user) else call.respond(io.ktor.http.HttpStatusCode.Unauthorized)
            }
            
            get("/users") { call.respond(users.values.toList()) }
            get("/groups") { call.respond(groups.values.toList()) }

            post("/groups") {
                val group = call.receive<GroupDM>()
                val newGroup = group.copy(id = "group_${System.currentTimeMillis()}")
                groups[newGroup.id] = newGroup
                saveData()
                call.respond(newGroup)
            }

            patch("/users/{id}") {
                val id = call.parameters["id"] ?: return@patch
                val updates = call.receive<User>()
                val existing = users[id] ?: return@patch
                val updated = existing.copy(
                    bio = updates.bio,
                    avatarUrl = updates.avatarUrl,
                    bannerColorArgb = updates.bannerColorArgb,
                    pronouns = updates.pronouns,
                    messageColorArgb = updates.messageColorArgb,
                    isAdmin = updates.isAdmin,
                    tag = updates.tag,
                    tagColorArgb = updates.tagColorArgb,
                    customTags = updates.customTags,
                    isTimedOut = updates.isTimedOut,
                    isBanned = updates.isBanned,
                    friendIds = updates.friendIds
                )
                users[id] = updated
                saveData()
                call.respond(updated)
            }
            
            webSocket("/chat/{userId}") {
                val userId = call.parameters["userId"] ?: return@webSocket
                sessions[userId] = this
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            val msg = Json.decodeFromString<Message>(text)
                            val finalMsg = msg.copy(id = UUID.randomUUID().toString(), timestamp = System.currentTimeMillis())
                            messages.add(finalMsg)
                            saveData()
                            
                            // Broadcast to participants
                            val group = groups[msg.chatId]
                            group?.participantIds?.forEach { participantId ->
                                sessions[participantId]?.send(Json.encodeToString(finalMsg))
                            }
                        }
                    }
                } finally {
                    sessions.remove(userId)
                }
            }
        }
    }.start(wait = true)
}

fun loadData() {
    try {
        if (usersFile.exists()) users.putAll(Json.decodeFromString<List<User>>(usersFile.readText()).associateBy { it.id })
        if (groupsFile.exists()) groups.putAll(Json.decodeFromString<List<GroupDM>>(groupsFile.readText()).associateBy { it.id })
        if (messagesFile.exists()) messages.addAll(Json.decodeFromString<List<Message>>(messagesFile.readText()))
    } catch (e: Exception) {
        println("Error loading data: ${e.message}")
    }
}

fun saveData() {
    usersFile.writeText(Json.encodeToString(users.values.toList()))
    groupsFile.writeText(Json.encodeToString(groups.values.toList()))
    messagesFile.writeText(Json.encodeToString(messages))
}
