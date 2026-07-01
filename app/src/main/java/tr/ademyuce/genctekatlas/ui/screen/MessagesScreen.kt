package tr.ademyuce.genctekatlas.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import tr.ademyuce.genctekatlas.data.model.ChatContact
import tr.ademyuce.genctekatlas.data.model.DirectMessage
import tr.ademyuce.genctekatlas.data.repository.FirebaseRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(repository: FirebaseRepository) {
    var selectedContactId by remember { mutableStateOf<String?>(null) }
    var selectedContactName by remember { mutableStateOf("") }
    var typedMessage by remember { mutableStateOf("") }
    var messageRefreshKey by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()
    val currentUser = repository.getCurrentUser()
    val canUseMessages = currentUser != null || repository.isDemoMode
    val myUid = currentUser?.uid ?: "demo-user"
    val contacts by repository.getChatContacts().collectAsState(initial = emptyList<ChatContact>())
    val activeMessageFlow = remember(myUid, selectedContactId, messageRefreshKey) {
        selectedContactId?.let { repository.getDirectMessages(myUid, it) } ?: emptyFlow()
    }
    val activeMessages by activeMessageFlow.collectAsState(initial = emptyList<DirectMessage>())

    LaunchedEffect(selectedContactId, messageRefreshKey, myUid) {
        selectedContactId?.let { repository.markDirectConversationAsRead(myUid, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedContactId != null) selectedContactName else "Mesajlar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (selectedContactId != null) {
                        IconButton(onClick = { selectedContactId = null }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Geri")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        if (!canUseMessages) {
            EmptyState(
                title = "Mesajlar için giriş gerekli",
                description = "Sohbet başlatmak ve mesaj göndermek için profilinizle giriş yapın.",
                icon = Icons.Default.Chat,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            )
        } else if (selectedContactId == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    MobileScreenHeader(
                        title = "Gelen Kutusu",
                        subtitle = "Mentorlar ve ekip arkadaşları"
                    )
                }
                if (contacts.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Kişi bulunamadı",
                            description = "Okul, il veya rol bazlı kişiler geldiğinde burada listelenir.",
                            icon = Icons.Default.Person
                        )
                    }
                }
                items(contacts, key = { it.uid }) { contact ->
                    MobileCard(
                        modifier = Modifier.clickable {
                            selectedContactId = contact.uid
                            selectedContactName = contact.name
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ContactAvatar(name = contact.name)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = listOf(contact.role, contact.city, contact.school).filter { it.isNotBlank() }.joinToString(" - "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (activeMessages.isEmpty()) {
                    EmptyState(
                        title = "Mesaj yok",
                        description = "Bu kişiyle konuşma başladığında mesajlar burada görünür.",
                        icon = Icons.Default.Chat,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeMessages, key = { it.id }) { msg ->
                            val isMe = msg.senderId == myUid
                            MessageBubble(
                                message = msg,
                                isMe = isMe,
                                fallbackSenderName = selectedContactName
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .imePadding()
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = typedMessage,
                        onValueChange = { typedMessage = it },
                        placeholder = { Text("Mesajınızı yazın") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = MobileCardShape
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = {
                            if (typedMessage.isNotBlank()) {
                                val newMsg = DirectMessage(
                                    senderId = myUid,
                                    receiverId = selectedContactId!!,
                                    messageText = typedMessage,
                                    senderName = currentUser?.name ?: "Ben",
                                    receiverName = selectedContactName
                                )
                                coroutineScope.launch {
                                    repository.sendDirectMessage(newMsg)
                                    messageRefreshKey += 1
                                    typedMessage = ""
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Gönder",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun MessageBubble(
    message: DirectMessage,
    isMe: Boolean,
    fallbackSenderName: String
) {
    val bubbleColor = if (isMe) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val alignment = if (isMe) Alignment.End else Alignment.Start

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            shape = MobileCardShape,
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isMe) {
                    Text(
                        text = message.senderName.ifEmpty { fallbackSenderName },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(text = message.messageText, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(
            text = if (isMe) {
                if (message.read) "Ben - okundu" else "Ben - gönderildi"
            } else {
                fallbackSenderName
            },
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
