package tr.ademyuce.genctekatlas.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tr.ademyuce.genctekatlas.data.model.Group
import tr.ademyuce.genctekatlas.data.model.GroupAnnouncement
import tr.ademyuce.genctekatlas.data.model.GroupMessage
import tr.ademyuce.genctekatlas.data.model.GroupTask
import tr.ademyuce.genctekatlas.data.model.User
import tr.ademyuce.genctekatlas.data.repository.FirebaseRepository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupsScreen(repository: FirebaseRepository, currentUser: User? = null) {
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var activeTabState by remember { mutableIntStateOf(0) }
    var kanbanTabState by remember { mutableIntStateOf(0) }
    var taskRefreshKey by remember { mutableIntStateOf(0) }
    var groupRefreshKey by remember { mutableIntStateOf(0) }
    var showCreateGroupSheet by remember { mutableStateOf(false) }
    var showJoinGroupSheet by remember { mutableStateOf(false) }
    var showCreateAnnouncementSheet by remember { mutableStateOf(false) }
    var showCreateTaskSheet by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var groupTheme by remember { mutableStateOf("") }
    var groupCity by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var announcementText by remember { mutableStateOf("") }
    var taskTitle by remember { mutableStateOf("") }
    var taskAssignee by remember { mutableStateOf("") }
    var taskDueDate by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var typedGroupMessage by remember { mutableStateOf("") }
    var groupMessageRefreshKey by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val user = currentUser ?: repository.getCurrentUser()
    val canCreateGroup = user?.role in listOf("admin", "coordinator", "principal")
    val groupsFlow = remember(groupRefreshKey) { repository.getGroups() }
    val groups by groupsFlow.collectAsState(initial = emptyList<Group>())
    val selectedGroup = groups.find { it.id == selectedGroupId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedGroup?.ad ?: "Çalışma Grupları", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (selectedGroupId != null) {
                        IconButton(onClick = { selectedGroupId = null }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Geri")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        },
        floatingActionButton = {
            val showFab = (selectedGroupId == null && canCreateGroup) || (selectedGroupId != null && activeTabState != 2)
            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        if (selectedGroupId == null) {
                            showCreateGroupSheet = true
                        } else if (activeTabState == 0) {
                            showCreateAnnouncementSheet = true
                        } else if (activeTabState == 1) {
                            showCreateTaskSheet = true
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ekle")
                }
            }
        }
    ) { innerPadding ->
        if (selectedGroupId == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    MobileScreenHeader(
                        title = "Gruplarım",
                        subtitle = "${groups.size} çalışma grubu"
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        if (canCreateGroup) {
                            Button(
                                onClick = { showCreateGroupSheet = true },
                                modifier = Modifier.weight(1f),
                                shape = MobileCardShape
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("Oluştur")
                            }
                        }
                        Button(
                            onClick = { showJoinGroupSheet = true },
                            modifier = Modifier.weight(1f),
                            shape = MobileCardShape
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null)
                            Text("Katıl")
                        }
                    }
                }
                if (groups.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Grup bulunamadı",
                            description = "Üye olduğunuz çalışma grupları burada görünür.",
                            icon = Icons.Default.Groups
                        )
                    }
                } else {
                    items(groups, key = { it.id }) { group ->
                        GroupCard(group = group, onClick = { selectedGroupId = group.id })
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        } else {
            val announcements by repository.getGroupAnnouncements(selectedGroupId!!).collectAsState(initial = emptyList<GroupAnnouncement>())
            val taskFlow = remember(selectedGroupId, taskRefreshKey) {
                repository.getGroupTasks(selectedGroupId!!)
            }
            val groupMessagesFlow = remember(selectedGroupId, groupMessageRefreshKey) {
                repository.getGroupMessages(selectedGroupId!!)
            }
            val tasks by taskFlow.collectAsState(initial = emptyList<GroupTask>())
            val groupMessages by groupMessagesFlow.collectAsState(initial = emptyList<GroupMessage>())

            LaunchedEffect(selectedGroupId, activeTabState, groupMessageRefreshKey) {
                if (activeTabState == 2) {
                    selectedGroupId?.let { repository.markGroupMessagesAsRead(it) }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                TabRow(selectedTabIndex = activeTabState) {
                    Tab(
                        selected = activeTabState == 0,
                        onClick = { activeTabState = 0 },
                        text = { Text("Duyurular", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTabState == 1,
                        onClick = { activeTabState = 1 },
                        text = { Text("Görev Panosu", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTabState == 2,
                        onClick = { activeTabState = 2 },
                        text = { Text("Sohbet", fontWeight = FontWeight.Bold) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (activeTabState == 0) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (announcements.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "Duyuru yok",
                                    description = "Bu gruba ait duyurular burada listelenir.",
                                    icon = Icons.Default.Info
                                )
                            }
                        } else {
                            items(announcements, key = { it.id }) { announcement ->
                                MobileCard {
                                    InfoRow(label = "Duyuru", value = announcement.text, icon = Icons.Default.Info)
                                    InfoRow(label = "Tarih", value = announcement.timestamp, icon = Icons.Default.CalendarToday)
                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    repository.deleteGroupAnnouncement(announcement.id)
                                                    taskRefreshKey += 1
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Duyuruyu sil")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (activeTabState == 1) {
                    ScrollableTabRow(
                        selectedTabIndex = kanbanTabState,
                        edgePadding = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(selected = kanbanTabState == 0, onClick = { kanbanTabState = 0 }, text = { Text("Yapılacak", fontSize = 12.sp) })
                        Tab(selected = kanbanTabState == 1, onClick = { kanbanTabState = 1 }, text = { Text("Yapılıyor", fontSize = 12.sp) })
                        Tab(selected = kanbanTabState == 2, onClick = { kanbanTabState = 2 }, text = { Text("Tamamlandı", fontSize = 12.sp) })
                    }

                    val statusFilter = when (kanbanTabState) {
                        0 -> "todo"
                        1 -> "progress"
                        else -> "done"
                    }
                    val filteredTasks = tasks.filter { it.status == statusFilter }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 12.dp)
                    ) {
                        if (filteredTasks.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "Görev yok",
                                    description = "Bu sütunda görev bulunmamaktadır.",
                                    icon = Icons.Default.TaskAlt
                                )
                            }
                        } else {
                            items(filteredTasks, key = { it.id }) { task ->
                                MobileCard {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        MetaChip(text = task.assignee.ifBlank { "Sorumlu yok" }, icon = Icons.Default.Groups)
                                        MetaChip(text = task.dueDate.ifBlank { "Tarih yok" }, icon = Icons.Default.CalendarToday)
                                    }
                                    if (task.description.isNotBlank()) {
                                        Text(
                                            text = task.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    repository.deleteGroupTask(task.id)
                                                    taskRefreshKey += 1
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Görevi sil")
                                        }
                                        if (task.status == "todo") {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        repository.updateGroupTaskStatus(task.id, "progress")
                                                        taskRefreshKey += 1
                                                    }
                                                },
                                                shape = MobileCardShape
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                                Text("Başla")
                                            }
                                        } else if (task.status == "progress") {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        repository.updateGroupTaskStatus(task.id, "done")
                                                        taskRefreshKey += 1
                                                    }
                                                },
                                                shape = MobileCardShape
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                                Text("Tamamla")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (groupMessages.isEmpty()) {
                            EmptyState(
                                title = "Grup sohbeti boş",
                                description = "Bu gruptaki mesajlaşma başladığında burada görünür.",
                                icon = Icons.Default.Chat,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(groupMessages, key = { it.id }) { message ->
                                    GroupMessageBubble(
                                        message = message,
                                        isMe = message.senderId == user?.uid
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                                .imePadding()
                                .navigationBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = typedGroupMessage,
                                onValueChange = { typedGroupMessage = it },
                                placeholder = { Text("Gruba mesaj yazın") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MobileCardShape
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            IconButton(
                                onClick = {
                                    val messageText = typedGroupMessage.trim()
                                    if (messageText.isBlank()) return@IconButton
                                    coroutineScope.launch {
                                        repository.sendGroupMessage(
                                            GroupMessage(
                                                groupId = selectedGroupId!!,
                                                messageText = messageText
                                            )
                                        )
                                        typedGroupMessage = ""
                                        groupMessageRefreshKey += 1
                                    }
                                },
                                enabled = user != null,
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Gönder", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupSheet && canCreateGroup) {
        ModalBottomSheet(onDismissRequest = { showCreateGroupSheet = false }, shape = MobileSheetShape) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MobileScreenHeader("Grup Oluştur", "Yeni çalışma grubu başlatın")
                OutlinedTextField(groupName, { groupName = it }, label = { Text("Grup adı") }, modifier = Modifier.fillMaxWidth(), shape = MobileCardShape)
                OutlinedTextField(groupDescription, { groupDescription = it }, label = { Text("Açıklama") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = MobileCardShape)
                OutlinedTextField(groupTheme, { groupTheme = it }, label = { Text("Tema") }, modifier = Modifier.fillMaxWidth(), shape = MobileCardShape)
                OutlinedTextField(groupCity, { groupCity = it }, label = { Text("İl") }, modifier = Modifier.fillMaxWidth(), shape = MobileCardShape)
                FullWidthButton(
                    text = "Grubu Oluştur",
                    icon = Icons.Default.Add,
                    onClick = {
                        if (groupName.isBlank()) return@FullWidthButton
                        coroutineScope.launch {
                            val group = repository.createGroup(
                                Group(
                                    ad = groupName.trim(),
                                    aciklama = groupDescription.trim(),
                                    tema = groupTheme.trim(),
                                    il = groupCity.trim()
                                )
                            )
                            selectedGroupId = group.id
                            groupRefreshKey += 1
                            showCreateGroupSheet = false
                            groupName = ""
                            groupDescription = ""
                            groupTheme = ""
                            groupCity = ""
                        }
                    }
                )
            }
        }
    }

    if (showJoinGroupSheet) {
        ModalBottomSheet(onDismissRequest = { showJoinGroupSheet = false }, shape = MobileSheetShape) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MobileScreenHeader("Gruba Katıl", "Davet kodunu girin")
                OutlinedTextField(inviteCode, { inviteCode = it }, label = { Text("Davet kodu") }, modifier = Modifier.fillMaxWidth(), shape = MobileCardShape)
                FullWidthButton(
                    text = "Katıl",
                    icon = Icons.Default.Groups,
                    onClick = {
                        if (inviteCode.isBlank()) return@FullWidthButton
                        coroutineScope.launch {
                            val group = repository.joinGroupByInviteCode(inviteCode)
                            selectedGroupId = group.id
                            groupRefreshKey += 1
                            showJoinGroupSheet = false
                            inviteCode = ""
                        }
                    }
                )
            }
        }
    }

    selectedGroupId?.let { groupId ->
        if (showCreateAnnouncementSheet) {
            ModalBottomSheet(onDismissRequest = { showCreateAnnouncementSheet = false }, shape = MobileSheetShape) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MobileScreenHeader("Duyuru Ekle", selectedGroup?.ad.orEmpty())
                    OutlinedTextField(announcementText, { announcementText = it }, label = { Text("Duyuru metni") }, modifier = Modifier.fillMaxWidth(), minLines = 4, shape = MobileCardShape)
                    FullWidthButton(
                        text = "Duyuruyu Yayınla",
                        icon = Icons.Default.Info,
                        onClick = {
                            if (announcementText.isBlank()) return@FullWidthButton
                            coroutineScope.launch {
                                repository.createGroupAnnouncement(GroupAnnouncement(groupId = groupId, text = announcementText.trim()))
                                announcementText = ""
                                showCreateAnnouncementSheet = false
                                taskRefreshKey += 1
                            }
                        }
                    )
                }
            }
        }

        if (showCreateTaskSheet) {
            ModalBottomSheet(onDismissRequest = { showCreateTaskSheet = false }, shape = MobileSheetShape) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MobileScreenHeader("Görev Ekle", selectedGroup?.ad.orEmpty())
                    OutlinedTextField(taskTitle, { taskTitle = it }, label = { Text("Görev başlığı") }, modifier = Modifier.fillMaxWidth(), shape = MobileCardShape)
                    OutlinedTextField(taskAssignee, { taskAssignee = it }, label = { Text("Sorumlu") }, modifier = Modifier.fillMaxWidth(), shape = MobileCardShape)
                    OutlinedTextField(taskDueDate, { taskDueDate = it }, label = { Text("Son tarih") }, modifier = Modifier.fillMaxWidth(), shape = MobileCardShape)
                    OutlinedTextField(taskDescription, { taskDescription = it }, label = { Text("Açıklama") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = MobileCardShape)
                    FullWidthButton(
                        text = "Görevi Ekle",
                        icon = Icons.Default.TaskAlt,
                        onClick = {
                            if (taskTitle.isBlank()) return@FullWidthButton
                            coroutineScope.launch {
                                repository.createGroupTask(
                                    GroupTask(
                                        groupId = groupId,
                                        title = taskTitle.trim(),
                                        assignee = taskAssignee.trim(),
                                        dueDate = taskDueDate.trim(),
                                        description = taskDescription.trim()
                                    )
                                )
                                taskTitle = ""
                                taskAssignee = ""
                                taskDueDate = ""
                                taskDescription = ""
                                showCreateTaskSheet = false
                                taskRefreshKey += 1
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupCard(group: Group, onClick: () -> Unit) {
    MobileCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = group.ad,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = group.aciklama.ifBlank { "Açıklama belirtilmemiş." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MetaChip(text = group.il.ifBlank { "Konum yok" }, icon = Icons.Default.LocationOn)
            MetaChip(text = group.tema.ifBlank { "Tema yok" }, icon = Icons.Default.Label)
        }
    }
}

@Composable
private fun GroupMessageBubble(message: GroupMessage, isMe: Boolean) {
    Column(
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        MobileCard(modifier = Modifier.fillMaxWidth(if (isMe) 0.84f else 0.92f)) {
            if (!isMe) {
                Text(
                    text = message.senderName.ifBlank { "Grup üyesi" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = message.messageText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isMe) "Ben" else message.senderName.ifBlank { "Üye" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
