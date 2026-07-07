package BAND.apk

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun App(viewModel: ChatViewModel, imagePicker: ImagePicker) {
    val uiSettings by viewModel.uiSettings
    val activeCall by viewModel.activeCall
    val currentUser by viewModel.currentUser
    
    var currentScreen by remember { mutableStateOf("Main") }

    MaterialTheme(
        colorScheme = if (uiSettings.themeColor.luminance() < 0.5f) darkColorScheme(surface = uiSettings.themeColor) 
                      else lightColorScheme(surface = uiSettings.themeColor)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (currentUser == null) {
                LoginScreen(onLogin = viewModel::login, onRegister = viewModel::register)
            } else {
                when (currentScreen) {
                    "Settings" -> SettingsScreen(viewModel = viewModel, onBack = { currentScreen = "Main" }, imagePicker = imagePicker)
                    "Changelogs" -> ChangelogsScreen(viewModel = viewModel, onBack = { currentScreen = "Main" })
                    "Friends" -> FriendsScreen(viewModel = viewModel, onBack = { currentScreen = "Main" })
                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (uiSettings.appIconUri != null) {
                                AsyncImage(
                                    model = uiSettings.appIconUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            MainAppContent(
                                user = currentUser!!,
                                viewModel = viewModel,
                                onOpenSettings = { currentScreen = "Settings" },
                                onOpenChangelogs = { currentScreen = "Changelogs" },
                                onOpenFriends = { currentScreen = "Friends" },
                                imagePicker = imagePicker
                            )

                            if (activeCall != null) {
                                CallScreen(type = activeCall!!, onEndCall = viewModel::endCall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Boolean, onRegister: (String, String) -> Boolean) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            (if (isRegisterMode) "Create an Account" else "Welcome Back!"),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        if (error) {
            Text((if (isRegisterMode) "Username already exists" else "Invalid credentials"), color = Color.Red)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { 
                val success = if (isRegisterMode) onRegister(name, password) else onLogin(name, password)
                if (!success) error = true
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(if (isRegisterMode) "Register" else "Login")
        }
        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(if (isRegisterMode) "Already have an account? Login" else "Need an account? Register")
        }
    }
}

@Composable
fun CallScreen(type: String, onEndCall: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = when(type) {
                    "voice" -> Icons.Default.Call
                    "video" -> Icons.Default.Videocam
                    else -> Icons.Default.Podcasts
                },
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = when(type) {
                    "voice" -> "Voice Call..."
                    "video" -> "Video Call..."
                    else -> "Streaming..."
                },
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(48.dp))
            FloatingActionButton(
                onClick = onEndCall,
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "End Call")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    user: User,
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit,
    onOpenChangelogs: () -> Unit,
    onOpenFriends: () -> Unit,
    imagePicker: ImagePicker
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedUserForProfile by remember { mutableStateOf<User?>(null) }
    var showParticipantsList by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp).fillMaxHeight(),
                drawerContainerColor = Color(0xFF2B2D31),
                drawerShape = RoundedCornerShape(0.dp)
            ) {
                Sidebar(
                    viewModel = viewModel,
                    onOpenChangelogs = onOpenChangelogs,
                    onOpenSettings = onOpenSettings,
                    onOpenFriends = onOpenFriends,
                    onCloseDrawer = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                val activeGroup by viewModel.activeGroupDM
                TopAppBar(
                    title = { Text(activeGroup?.name ?: viewModel.getParticipantNames(activeGroup ?: GroupDM("", null, emptyList())), color = Color.White, fontSize = 16.sp) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.startCall("voice") }) { Icon(Icons.Default.Call, contentDescription = null, tint = Color.White) }
                        IconButton(onClick = { viewModel.startCall("video") }) { Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White) }
                        IconButton(onClick = { viewModel.startCall("streaming") }) { Icon(Icons.Default.Podcasts, contentDescription = null, tint = Color.White) }
                        IconButton(onClick = { showParticipantsList = !showParticipantsList }) { Icon(Icons.Default.People, contentDescription = "Members", tint = Color.White) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF313338))
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                Column(modifier = Modifier.weight(1f)) {
                    val messages = viewModel.messages
                    LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
                        items(messages) { message ->
                            val sender = viewModel.getUserById(message.senderId)
                            MessageItem(
                                sender = sender,
                                message = message,
                                onClick = { selectedUserForProfile = sender }
                            )
                        }
                    }
                    MessageInput(
                        onSend = { content -> viewModel.sendMessage(content) },
                        onPickFile = { isImage ->
                            imagePicker.pickImage { path ->
                                if (path != null) viewModel.uploadAndSendFile(path, isImage)
                            }
                        },
                        isTimedOut = user.isTimedOut
                    )
                }

                if (showParticipantsList) {
                    val activeGroup by viewModel.activeGroupDM
                    activeGroup?.let { group ->
                        Column(modifier = Modifier.width(200.dp).fillMaxHeight().background(Color(0xFF2B2D31)).padding(8.dp)) {
                            Text("MEMBERS - ${group.participantIds.size}", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                            LazyColumn {
                                items(group.participantIds) { id ->
                                    val member = viewModel.getUserById(id)
                                    member?.let {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { selectedUserForProfile = it }.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = it.avatarUrl ?: "https://via.placeholder.com/32",
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(it.name, color = Color.White, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedUserForProfile != null) {
        UserProfilePopup(
            user = selectedUserForProfile!!,
            currentUser = user,
            viewModel = viewModel,
            onDismiss = { selectedUserForProfile = null }
        )
    }
}

@Composable
fun Sidebar(
    viewModel: ChatViewModel,
    onOpenChangelogs: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFriends: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    val activeGroup by viewModel.activeGroupDM

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("BAND", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(8.dp))
        
        HorizontalDivider(color = Color.Gray.copy(0.2f))
        
        SidebarItem(icon = Icons.Default.Person, label = "Friends", onClick = {
            onOpenFriends()
            onCloseDrawer()
        })
        SidebarItem(icon = Icons.Default.Info, label = "Changelogs", onClick = {
            onOpenChangelogs()
            onCloseDrawer()
        })
        SidebarItem(icon = Icons.Default.Settings, label = "Settings", onClick = {
            onOpenSettings()
            onCloseDrawer()
        })

        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("GROUP DMs", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showCreateGroupDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }

        LazyColumn {
            items(viewModel.groupDMs) { group ->
                val name = group.name ?: viewModel.getParticipantNames(group)
                GroupDMItem(
                    name = name,
                    isActive = group.id == activeGroup?.id,
                    onClick = {
                        viewModel.selectGroupDM(group)
                        onCloseDrawer()
                    }
                )
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(viewModel = viewModel, onDismiss = { showCreateGroupDialog = false })
    }
}

@Composable
fun SidebarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White)
    }
}

@Composable
fun GroupDMItem(name: String, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = if (isActive) Color.White.copy(0.1f) else Color.Transparent
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bgColor).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(name, color = if (isActive) Color.White else Color.Gray, maxLines = 1)
    }
}

@Composable
fun MessageItem(sender: User?, message: Message, onClick: () -> Unit) {
    val isOwner = sender?.tag == "Owner"
    
    val infiniteTransition = rememberInfiniteTransition(label = "RGBTransition")
    val rgbColor by if (isOwner) {
        infiniteTransition.animateColor(
            initialValue = Color.Red,
            targetValue = Color.Red,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 3000
                    Color.Red at 0
                    Color.Yellow at 500
                    Color.Green at 1000
                    Color.Cyan at 1500
                    Color.Blue at 2000
                    Color.Magenta at 2500
                    Color.Red at 3000
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "RGBAnimation"
        )
    } else {
        remember { mutableStateOf(sender?.messageColor ?: Color.LightGray) }
    }

    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp)) {
        AsyncImage(
            model = sender?.avatarUrl ?: "https://via.placeholder.com/40",
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sender?.name ?: "Unknown", color = Color.White, fontWeight = FontWeight.Bold)
                sender?.tag?.let { tag ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = sender.tagColor, shape = RoundedCornerShape(4.dp)) {
                        Text(tag, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp)
                    }
                }
            }
            
            when (message.type) {
                MessageType.IMAGE -> {
                    AsyncImage(
                        model = message.content,
                        contentDescription = "Shared Image",
                        modifier = Modifier.fillMaxWidth(0.7f).heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray),
                        contentScale = ContentScale.Fit
                    )
                }
                MessageType.FILE -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2D31)),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilePresent, contentDescription = null, tint = Color.LightGray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(message.fileName ?: "File", color = Color.LightGray)
                        }
                    }
                }
                else -> {
                    Text(message.content, color = rgbColor)
                }
            }
        }
    }
}

@Composable
fun MessageInput(onSend: (String) -> Unit, onPickFile: (Boolean) -> Unit, isTimedOut: Boolean) {
    var text by remember { mutableStateOf("") }
    Surface(color = Color(0xFF383A40), modifier = Modifier.padding(8.dp), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onPickFile(false) }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Attach File", tint = Color.Gray)
            }
            IconButton(onClick = { onPickFile(true) }) {
                Icon(Icons.Default.Image, contentDescription = "Attach Image", tint = Color.Gray)
            }
            TextField(
                value = text,
                onValueChange = { if (!isTimedOut) text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (isTimedOut) "You are timed out" else "Message...", color = Color.Gray) },
                enabled = !isTimedOut,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            IconButton(onClick = { if (text.isNotBlank()) { onSend(text); text = "" } }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = if (text.isBlank()) Color.Gray else Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ChatViewModel, onBack: () -> Unit, imagePicker: ImagePicker) {
    val user by viewModel.currentUser
    val uiSettings by viewModel.uiSettings
    var selectedTab by remember { mutableStateOf("Account") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(250.dp).fillMaxHeight(),
                drawerContainerColor = Color(0xFF2B2D31),
                drawerShape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    Text("Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(8.dp))
                    HorizontalDivider(color = Color.Gray.copy(0.2f))
                    SettingsTabItem("Account", selectedTab == "Account") { selectedTab = "Account"; scope.launch { drawerState.close() } }
                    SettingsTabItem("Security", selectedTab == "Security") { selectedTab = "Security"; scope.launch { drawerState.close() } }
                    SettingsTabItem("Voice & Video", selectedTab == "Voice & Video") { selectedTab = "Voice & Video"; scope.launch { drawerState.close() } }
                    SettingsTabItem("Appearance", selectedTab == "Appearance") { selectedTab = "Appearance"; scope.launch { drawerState.close() } }
                    SettingsTabItem("Customization", selectedTab == "Customization") { selectedTab = "Customization"; scope.launch { drawerState.close() } }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = viewModel::logout, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), modifier = Modifier.fillMaxWidth()) {
                        Text("Logout")
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings - $selectedTab", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White) }
                    },
                    actions = {
                        IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = "Close Settings", tint = Color.White) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF313338))
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                when (selectedTab) {
                    "Account" -> AccountSettings(user!!, viewModel, imagePicker)
                    "Appearance" -> AppearanceSettings(uiSettings, viewModel)
                    "Customization" -> CustomizationSettings(uiSettings, viewModel, imagePicker)
                    else -> Text("Placeholder for $selectedTab", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SettingsTabItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (isSelected) Color.White else Color.Gray,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(if (isSelected) Color.White.copy(0.1f) else Color.Transparent).clickable(onClick = onClick).padding(12.dp),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
fun AccountSettings(user: User, viewModel: ChatViewModel, imagePicker: ImagePicker) {
    var bio by remember { mutableStateOf(user.bio) }
    var pronouns by remember { mutableStateOf(user.pronouns) }
    var avatarUrl by remember { mutableStateOf(user.avatarUrl ?: "") }
    var messageColor by remember { mutableStateOf(user.messageColor) }

    Text("Account Info", style = MaterialTheme.typography.headlineSmall, color = Color.White)
    Spacer(modifier = Modifier.height(16.dp))
    
    AsyncImage(
        model = if (avatarUrl.isEmpty()) "https://via.placeholder.com/80" else avatarUrl,
        contentDescription = "Avatar",
        modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.Gray).clickable { 
            imagePicker.pickImage { path -> if (path != null) avatarUrl = path }
        },
        contentScale = ContentScale.Crop
    )
    Text("Click avatar to pick image", color = Color.Gray, fontSize = 12.sp)

    Spacer(modifier = Modifier.height(16.dp))
    TextField(value = pronouns, onValueChange = { pronouns = it }, label = { Text("Pronouns") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    TextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())
    
    Spacer(modifier = Modifier.height(16.dp))
    Text("Message Text Color", color = Color.White)
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        listOf(Color.White, Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta).forEach { color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .padding(4.dp)
                    .background(color, CircleShape)
                    .clickable { messageColor = color }
                    .then(if (messageColor == color) Modifier.background(Color.Gray.copy(alpha = 0.5f), CircleShape) else Modifier)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = { viewModel.updateProfile(bio, avatarUrl, user.bannerColor, pronouns, messageColor) }) {
        Text("Save Profile")
    }
}

@Composable
fun AppearanceSettings(settings: UISettings, viewModel: ChatViewModel) {
    Text("Appearance", style = MaterialTheme.typography.headlineSmall, color = Color.White)
    Spacer(modifier = Modifier.height(16.dp))
    Text("Theme Color", color = Color.White)
    Row {
        ColorOption(Color(0xFF313338)) { viewModel.updateUISettings(settings.copy(themeColorArgb = it.toArgb())) }
        ColorOption(Color(0xFF5865F2)) { viewModel.updateUISettings(settings.copy(themeColorArgb = it.toArgb())) }
        ColorOption(Color.Black) { viewModel.updateUISettings(settings.copy(themeColorArgb = it.toArgb())) }
    }
}

@Composable
fun CustomizationSettings(settings: UISettings, viewModel: ChatViewModel, imagePicker: ImagePicker) {
    var name by remember { mutableStateOf(settings.appName) }
    var bgUri by remember { mutableStateOf(settings.appIconUri ?: "") }

    Text("Client Customization", style = MaterialTheme.typography.headlineSmall, color = Color.White)
    Spacer(modifier = Modifier.height(16.dp))
    TextField(value = name, onValueChange = { name = it }, label = { Text("App Name") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(16.dp))
    
    Button(onClick = { 
        imagePicker.pickImage { path -> if (path != null) bgUri = path }
    }) {
        Text("Pick Background Image")
    }
    if (bgUri.isNotEmpty()) {
        Text("Current Path: $bgUri", color = Color.Gray, fontSize = 12.sp)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = { viewModel.updateUISettings(settings.copy(appName = name, appIconUri = if (bgUri.isBlank()) null else bgUri)) }) {
        Text("Apply Changes")
    }
}

@Composable
fun ColorOption(color: Color, onClick: (Color) -> Unit) {
    Box(modifier = Modifier.size(40.dp).padding(4.dp).background(color, CircleShape).clickable { onClick(color) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogsScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val currentUser by viewModel.currentUser
    val logs = viewModel.changelogs
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Changelogs", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
                },
                actions = {
                    if (currentUser?.tag == "Owner" || currentUser?.tag == "Co-Owner") {
                        IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF313338))
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            items(logs) { log ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF383A40))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(log.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(log.content, color = Color.LightGray)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Changelog") },
            text = {
                Column {
                    TextField(value = title, onValueChange = { title = it }, placeholder = { Text("Title") })
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(value = content, onValueChange = { content = it }, placeholder = { Text("Content") }, modifier = Modifier.height(100.dp))
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.addChangelog(title, content); showAddDialog = false }) { Text("Post") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val currentUser by viewModel.currentUser
    val users = viewModel.getOtherUsers()
    var selectedUserForProfile by remember { mutableStateOf<User?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF313338))
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("ADD FRIENDS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(users) { user ->
                    val isFriend = currentUser?.friendIds?.contains(user.id) == true
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedUserForProfile = user },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = user.avatarUrl ?: "https://via.placeholder.com/40",
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(if (isFriend) "Friend" else "Not Friend", color = Color.Gray, fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.toggleFriend(user.id) }) {
                            Icon(
                                imageVector = if (isFriend) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = if (isFriend) Color.Red else Color.Green
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedUserForProfile != null) {
        UserProfilePopup(
            user = selectedUserForProfile!!,
            currentUser = currentUser!!,
            viewModel = viewModel,
            onDismiss = { selectedUserForProfile = null }
        )
    }
}

@Composable
fun UserProfilePopup(user: User, currentUser: User, viewModel: ChatViewModel, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22))
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(user.bannerColor))
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        AsyncImage(
                            model = user.avatarUrl ?: "https://via.placeholder.com/80",
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.Gray).offset(y = (-40).dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.offset(y = (-10).dp)) {
                            Text(user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(user.pronouns, color = Color.Gray)
                        }
                    }
                    
                    Text("ABOUT ME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(user.bio, color = Color.White)
                    
                    if (user.customTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ROLES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            user.customTags.forEach { tag ->
                                Surface(color = tag.color, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(2.dp)) {
                                    Text(tag.label, modifier = Modifier.padding(4.dp), color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (currentUser.id != user.id) {
                        val isFriend = currentUser.friendIds.contains(user.id)
                        Button(
                            onClick = { viewModel.toggleFriend(user.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isFriend) Color.Gray else Color(0xFF5865F2))
                        ) {
                            Icon(if (isFriend) Icons.Default.PersonRemove else Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isFriend) "Remove Friend" else "Add Friend")
                        }
                    }

                    if (currentUser.isAdmin && currentUser.id != user.id) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("MODERATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.timeoutUser(user.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (user.isTimedOut) "Untimeout" else "Timeout")
                            }
                            
                            val activeGroup by viewModel.activeGroupDM
                            if (activeGroup != null) {
                                Button(
                                    onClick = { viewModel.removeUserFromGroup(activeGroup!!.id, user.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Kick")
                                }
                            }

                            if (currentUser.tag == "Owner") {
                                Button(
                                    onClick = { viewModel.banUser(user.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.Red),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (user.isBanned) "Unban" else "Ban")
                                }
                            }
                        }
                    }
                    
                    if ((currentUser.tag == "Owner" || currentUser.tag == "Co-Owner") && currentUser.id != user.id) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.giveAdmin(user.id) }, modifier = Modifier.fillMaxWidth()) { Text("Make Admin") }
                        
                        var showAddTagDialog by remember { mutableStateOf(false) }
                        Button(onClick = { showAddTagDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Add Custom Tag") }
                        
                        if (showAddTagDialog) {
                            var tagLabel by remember { mutableStateOf("") }
                            AlertDialog(
                                onDismissRequest = { showAddTagDialog = false },
                                title = { Text("Add Custom Tag") },
                                text = { TextField(value = tagLabel, onValueChange = { tagLabel = it }, placeholder = { Text("Tag Label") }) },
                                confirmButton = {
                                    Button(onClick = { viewModel.addCustomTag(user.id, tagLabel, Color.Cyan); showAddTagDialog = false }) { Text("Add") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGroupDialog(viewModel: ChatViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
        text = {
            Column {
                TextField(value = name, onValueChange = { name = it }, placeholder = { Text("Group Name") })
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select Members:")
                viewModel.getOtherUsers().forEach { other ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                        if (selectedIds.contains(other.id)) selectedIds.remove(other.id) else selectedIds.add(other.id)
                    }) {
                        Checkbox(checked = selectedIds.contains(other.id), onCheckedChange = null)
                        Text(other.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.createGroupDM(name, selectedIds); onDismiss() }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

interface ImagePicker {
    fun pickImage(onImagePicked: (String?) -> Unit)
}


