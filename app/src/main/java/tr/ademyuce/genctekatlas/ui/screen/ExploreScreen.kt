package tr.ademyuce.genctekatlas.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import tr.ademyuce.genctekatlas.data.model.Event
import tr.ademyuce.genctekatlas.data.model.EventApplication
import tr.ademyuce.genctekatlas.data.model.Project
import tr.ademyuce.genctekatlas.data.repository.FirebaseRepository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    repository: FirebaseRepository,
    onAddEventClick: () -> Unit,
    onAddProjectClick: () -> Unit,
    onLoginRequired: () -> Unit = {},
    currentUserId: String = ""
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCityFilter by remember { mutableStateOf("") }
    var selectedThemeFilter by remember { mutableStateOf("Tümü") }
    var selectedMapMode by remember { mutableStateOf(MapMode.Local) }
    var activeTabState by remember { mutableIntStateOf(0) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var selectedEventDetails by remember { mutableStateOf<Event?>(null) }
    var selectedProjectDetails by remember { mutableStateOf<Project?>(null) }
    var selectedApplicationEvent by remember { mutableStateOf<Event?>(null) }
    var applicationName by remember { mutableStateOf("") }
    var applicationEmail by remember { mutableStateOf("") }
    var applicationSchool by remember { mutableStateOf("") }
    var applicationCity by remember { mutableStateOf("") }
    var applicationPhone by remember { mutableStateOf("") }
    var applicationNote by remember { mutableStateOf("") }
    var applicationError by remember { mutableStateOf<String?>(null) }
    var isApplicationSubmitting by remember { mutableStateOf(false) }
    var eventRefreshKey by remember { mutableIntStateOf(0) }
    var applicationRefreshKey by remember { mutableIntStateOf(0) }
    var notificationRefreshKey by remember { mutableIntStateOf(0) }
    var notificationTabState by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val applicationUserKey = currentUserId.ifBlank { repository.getCurrentUser()?.uid.orEmpty() }
    val eventsFlow = remember(eventRefreshKey) { repository.getEvents() }
    val applicationsFlow = remember(applicationRefreshKey, applicationUserKey) { repository.getMyEventApplications() }
    val notificationsFlow = remember(notificationRefreshKey, applicationUserKey) { repository.getNotifications() }
    val announcementsFlow = remember(notificationRefreshKey) { repository.getAnnouncements() }
    val events by eventsFlow.collectAsState(initial = emptyList())
    val myApplications by applicationsFlow.collectAsState(initial = emptyList())
    val notifications by notificationsFlow.collectAsState(initial = emptyList())
    val announcements by announcementsFlow.collectAsState(initial = emptyList())
    val projects by repository.getProjects().collectAsState(initial = emptyList())
    val themes by repository.getThemes().collectAsState(
        initial = listOf("Tümü", "yapay-zeka", "mobil", "robotik-kodlama", "iot")
    )
    val appliedEventIds = remember(myApplications) { myApplications.map { it.eventId }.toSet() }
    val unreadNotificationCount = remember(notifications, announcements, applicationUserKey) {
        notifications.count { !it.read } + announcements.count { !it.readBy.contains(applicationUserKey) }
    }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("GençTek Atlas", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Etkinlik ve proje keşfi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showNotificationsSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge { Text(unreadNotificationCount.coerceAtMost(9).toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Bildirimler")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ekle")
            }
        }
    ) { innerPadding ->
        val filteredEvents = events.filter {
            (it.ad.contains(searchQuery, ignoreCase = true) ||
                it.il.contains(searchQuery, ignoreCase = true) ||
                it.ilce.contains(searchQuery, ignoreCase = true)) &&
                (selectedCityFilter.isEmpty() || it.il.equals(selectedCityFilter, ignoreCase = true)) &&
                (selectedThemeFilter == "Tümü" || it.tema.equals(selectedThemeFilter, ignoreCase = true))
        }

        val filteredProjects = projects.filter {
            (it.ad.contains(searchQuery, ignoreCase = true) ||
                it.takimAdi.contains(searchQuery, ignoreCase = true)) &&
                (selectedCityFilter.isEmpty() ||
                    it.katilimciIller.any { city -> city.equals(selectedCityFilter, ignoreCase = true) }) &&
                (selectedThemeFilter == "Tümü" || it.tema.equals(selectedThemeFilter, ignoreCase = true))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                MobileScreenHeader(
                    title = "Keşfet",
                    subtitle = "${events.size} etkinlik, ${projects.size} proje"
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Kelime, il veya okul ara") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MobileCardShape
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(themes) { theme ->
                        FilterChip(
                            selected = selectedThemeFilter == theme,
                            onClick = { selectedThemeFilter = theme },
                            label = {
                                Text(
                                    text = theme.readableSlug(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = if (selectedThemeFilter == theme) {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else {
                                null
                            },
                            shape = MobileCardShape
                        )
                    }
                }
            }

            if (selectedCityFilter.isNotEmpty()) {
                item {
                    MobileCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InfoRow(
                                label = "Şehir filtresi",
                                value = selectedCityFilter,
                                icon = Icons.Default.LocationOn,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { selectedCityFilter = "" }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Filtreyi temizle")
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MapModeSelector(
                        selectedMode = selectedMapMode,
                        onModeSelected = { selectedMapMode = it }
                    )
                    when (selectedMapMode) {
                        MapMode.Local -> AtlasMapCard(
                            events = events,
                            projects = projects,
                            selectedCity = selectedCityFilter,
                            onCitySelected = { city -> selectedCityFilter = city }
                        )
                        MapMode.OpenStreetMap -> OsmMapCard(
                            events = events,
                            projects = projects,
                            selectedCity = selectedCityFilter,
                            onCitySelected = { city -> selectedCityFilter = city },
                            onEventSelected = { event -> selectedEventDetails = event },
                            onProjectSelected = { project -> selectedProjectDetails = project }
                        )
                    }
                }
            }

            item {
                TabRow(selectedTabIndex = activeTabState) {
                    Tab(
                        selected = activeTabState == 0,
                        onClick = { activeTabState = 0 },
                        text = { Text("Yaklaşanlar", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTabState == 1,
                        onClick = { activeTabState = 1 },
                        text = { Text("Gerçekleşenler", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTabState == 2,
                        onClick = { activeTabState = 2 },
                        text = { Text("Projeler", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            when (activeTabState) {
                0 -> {
                    val upcoming = filteredEvents.filter { it.durum == "duyuru" }
                    if (upcoming.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Yaklaşan etkinlik yok",
                                description = "Filtreleri değiştirerek diğer kayıtları görüntüleyebilirsiniz.",
                                icon = Icons.Default.CalendarToday
                            )
                        }
                    } else {
                        items(upcoming, key = { it.id }) { event ->
                            EventCard(event = event, onClick = { selectedEventDetails = event })
                        }
                    }
                }
                1 -> {
                    val completed = filteredEvents.filter { it.durum == "gerceklesti" || it.durum.isEmpty() }
                    if (completed.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Gerçekleşen etkinlik yok",
                                description = "Arama veya tema filtresini temizleyerek tekrar deneyin.",
                                icon = Icons.Default.Info
                            )
                        }
                    } else {
                        items(completed, key = { it.id }) { event ->
                            EventCard(event = event, onClick = { selectedEventDetails = event })
                        }
                    }
                }
                2 -> {
                    if (filteredProjects.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Proje bulunamadı",
                                description = "Arama veya şehir filtresini değiştirerek tekrar deneyin.",
                                icon = Icons.Default.Code
                            )
                        }
                    } else {
                        items(filteredProjects, key = { it.id }) { project ->
                            ProjectCard(project = project, onClick = { selectedProjectDetails = project })
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            shape = MobileSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MobileScreenHeader(
                    title = "Yeni kayıt",
                    subtitle = "Etkinlik veya proje başvurusu oluşturun"
                )
                FullWidthButton(
                    text = "Yeni Etkinlik Ekle",
                    icon = Icons.Default.CalendarToday,
                    onClick = {
                        showBottomSheet = false
                        onAddEventClick()
                    }
                )
                FullWidthOutlinedButton(
                    text = "Yeni Proje Ekle",
                    icon = Icons.Default.Code,
                    onClick = {
                        showBottomSheet = false
                        onAddProjectClick()
                    }
                )
            }
        }
    }

    if (showNotificationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            shape = MobileSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MobileScreenHeader(
                    title = "Bildirimler",
                    subtitle = "Bildirimler ve genel duyurular"
                )
                TabRow(selectedTabIndex = notificationTabState) {
                    Tab(
                        selected = notificationTabState == 0,
                        onClick = { notificationTabState = 0 },
                        text = { Text("Bildirimler") }
                    )
                    Tab(
                        selected = notificationTabState == 1,
                        onClick = { notificationTabState = 1 },
                        text = { Text("Duyurular") }
                    )
                }
                if (notificationTabState == 0) {
                    if (notifications.isEmpty()) {
                        EmptyState(
                            title = "Bildirim yok",
                            description = "Yeni bildirimler burada görünecek.",
                            icon = Icons.Default.Notifications
                        )
                    } else {
                        FullWidthOutlinedButton(
                            text = "Tümünü Okundu Yap",
                            icon = Icons.Default.DoneAll,
                            onClick = {
                                coroutineScope.launch {
                                    repository.markAllNotificationsAsRead()
                                    notificationRefreshKey++
                                }
                            }
                        )
                        notifications.forEach { notification ->
                            MobileCard(
                                modifier = Modifier.clickable {
                                    coroutineScope.launch {
                                        repository.markNotificationAsRead(notification.id)
                                        notificationRefreshKey++
                                    }
                                    when (notification.relatedType) {
                                        "event", "application" -> events.find { it.id == notification.relatedId }?.let { selectedEventDetails = it }
                                        "project" -> projects.find { it.id == notification.relatedId }?.let { selectedProjectDetails = it }
                                    }
                                    showNotificationsSheet = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (notification.read) "Okundu" else "Yeni",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (notification.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = notification.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = notification.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.deleteNotification(notification.id)
                                                notificationRefreshKey++
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Bildirim sil")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (announcements.isEmpty()) {
                        EmptyState(
                            title = "Duyuru yok",
                            description = "Genel duyurular burada görünür.",
                            icon = Icons.Default.Info
                        )
                    } else {
                        announcements.forEach { announcement ->
                            MobileCard {
                                Text(
                                    text = announcement.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = announcement.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    MetaChip(text = announcement.scope.ifBlank { "Genel" }, icon = Icons.Default.Label)
                                    MetaChip(text = announcement.authorName.ifBlank { "Koordinasyon" }, icon = Icons.Default.Info)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedEventDetails?.let { event ->
        ModalBottomSheet(
            onDismissRequest = { selectedEventDetails = null },
            shape = MobileSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = event.ad, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                EventPhotoGallery(event = event)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaChip(text = event.il.ifBlank { "Konum yok" }, icon = Icons.Default.LocationOn)
                    MetaChip(text = event.tarih.ifBlank { "Tarih yok" }, icon = Icons.Default.CalendarToday)
                    MetaChip(text = event.tema.readableSlug(), icon = Icons.Default.Label)
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                InfoRow(label = "Konum", value = listOf(event.il, event.ilce).filter { it.isNotBlank() }.joinToString(" - "), icon = Icons.Default.LocationOn)
                InfoRow(label = "Katılımcı", value = "${event.katilimciSayisi} kişi", icon = Icons.Default.Groups)
                event.ogrenciSiniri?.let {
                    InfoRow(label = "Kontenjan", value = "$it kişi", icon = Icons.Default.Info)
                }
                Text(
                    text = "Açıklama",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = event.aciklama.ifEmpty { "Açıklama belirtilmemiş." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.detay.isNotEmpty()) {
                    Text(
                        text = event.detay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (event.baglanti.isNotBlank()) {
                    FullWidthOutlinedButton(
                        text = "Etkinlik Bağlantısını Aç",
                        icon = Icons.Default.OpenInNew,
                        onClick = { openExternalUrl(context, event.baglanti) }
                    )
                }

                if (event.durum == "duyuru") {
                    val alreadyApplied = appliedEventIds.contains(event.id)
                    val isFull = event.ogrenciSiniri?.let { event.katilimciSayisi >= it } == true
                    FullWidthButton(
                        text = when {
                            alreadyApplied -> "Başvurunuz Alındı"
                            isFull -> "Kontenjan Dolu"
                            else -> "Etkinliğe Başvur"
                        },
                        icon = Icons.Default.CheckCircle,
                        enabled = !alreadyApplied && !isFull,
                        onClick = {
                            val currentUser = repository.getCurrentUser()
                            if (currentUser == null) {
                                selectedEventDetails = null
                                onLoginRequired()
                                return@FullWidthButton
                            }
                            applicationName = currentUser.name
                            applicationEmail = currentUser.email
                            applicationSchool = currentUser.school
                            applicationCity = currentUser.city.ifBlank { event.il }
                            applicationPhone = ""
                            applicationNote = ""
                            applicationError = null
                            selectedEventDetails = null
                            selectedApplicationEvent = event
                        }
                    )
                } else {
                    FullWidthOutlinedButton(
                        text = "Etkinlik Gerçekleşti",
                        icon = Icons.Default.CheckCircle,
                        onClick = {},
                        enabled = false
                    )
                }
            }
        }
    }

    selectedApplicationEvent?.let { event ->
        ModalBottomSheet(
            onDismissRequest = {
                if (!isApplicationSubmitting) {
                    selectedApplicationEvent = null
                    applicationError = null
                }
            },
            shape = MobileSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MobileScreenHeader(
                    title = "Etkinlik Başvurusu",
                    subtitle = event.ad
                )
                MobileCard {
                    InfoRow(label = "Etkinlik", value = event.ad, icon = Icons.Default.CalendarToday)
                    InfoRow(label = "Tarih", value = event.tarih, icon = Icons.Default.Info)
                    InfoRow(label = "Konum", value = listOf(event.il, event.ilce).filter { it.isNotBlank() }.joinToString(" - "), icon = Icons.Default.LocationOn)
                }
                OutlinedTextField(
                    value = applicationName,
                    onValueChange = { applicationName = it },
                    label = { Text("Ad Soyad *") },
                    leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MobileCardShape
                )
                OutlinedTextField(
                    value = applicationEmail,
                    onValueChange = { applicationEmail = it },
                    label = { Text("E-posta") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = MobileCardShape
                )
                OutlinedTextField(
                    value = applicationSchool,
                    onValueChange = { applicationSchool = it },
                    label = { Text("Okul *") },
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MobileCardShape
                )
                OutlinedTextField(
                    value = applicationCity,
                    onValueChange = { applicationCity = it },
                    label = { Text("İl *") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MobileCardShape
                )
                OutlinedTextField(
                    value = applicationPhone,
                    onValueChange = { applicationPhone = it },
                    label = { Text("Telefon") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = MobileCardShape
                )
                OutlinedTextField(
                    value = applicationNote,
                    onValueChange = { applicationNote = it },
                    label = { Text("Not") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = MobileCardShape
                )
                if (event.ilKisitlama && event.il.isNotBlank()) {
                    Text(
                        text = "Bu etkinlik yalnızca ${event.il} ilindeki öğrencilerin başvurusuna açıktır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                applicationError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (isApplicationSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    FullWidthButton(
                        text = "Başvuruyu Gönder",
                        icon = Icons.Default.CheckCircle,
                        onClick = {
                            val validationMessage = when {
                                applicationName.isBlank() -> "Ad soyad bilgisi boş bırakılamaz."
                                applicationSchool.isBlank() -> "Okul bilgisi boş bırakılamaz."
                                applicationCity.isBlank() -> "İl bilgisi boş bırakılamaz."
                                event.ilKisitlama && event.il.isNotBlank() && !applicationCity.equals(event.il, ignoreCase = true) ->
                                    "Bu etkinliğe yalnızca ${event.il} ilindeki öğrenciler başvurabilir."
                                else -> null
                            }
                            if (validationMessage != null) {
                                applicationError = validationMessage
                                return@FullWidthButton
                            }

                            coroutineScope.launch {
                                isApplicationSubmitting = true
                                applicationError = null
                                try {
                                    repository.applyToEvent(
                                        event = event,
                                        application = EventApplication(
                                            eventId = event.id,
                                            eventName = event.ad,
                                            eventDate = event.tarih,
                                            eventCity = event.il,
                                            studentName = applicationName.trim(),
                                            studentEmail = applicationEmail.trim(),
                                            school = applicationSchool.trim(),
                                            city = applicationCity.trim(),
                                            phone = applicationPhone.trim(),
                                            note = applicationNote.trim()
                                        )
                                    )
                                    Toast.makeText(context, "Başvurunuz alındı.", Toast.LENGTH_LONG).show()
                                    applicationRefreshKey++
                                    eventRefreshKey++
                                    selectedApplicationEvent = null
                                } catch (e: Exception) {
                                    applicationError = e.message ?: "Başvuru gönderilemedi."
                                } finally {
                                    isApplicationSubmitting = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    selectedProjectDetails?.let { project ->
        ModalBottomSheet(
            onDismissRequest = { selectedProjectDetails = null },
            shape = MobileSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = project.ad, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaChip(text = project.takimAdi.ifBlank { "Takım yok" }, icon = Icons.Default.Groups)
                    MetaChip(text = project.tema.readableSlug(), icon = Icons.Default.Label)
                    MetaChip(text = project.parkur.ifBlank { "Parkur yok" }, icon = Icons.Default.Code)
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                InfoRow(label = "Takım", value = project.takimAdi, icon = Icons.Default.Groups)
                InfoRow(label = "Şehirler", value = project.katilimciIller.joinToString(), icon = Icons.Default.LocationOn)
                Text(
                    text = "Açıklama",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = project.aciklama.ifEmpty { "Açıklama belirtilmemiş." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (project.githubLink.isNotEmpty()) {
                    FullWidthButton(
                        text = "GitHub Deposunu İncele",
                        icon = Icons.Default.OpenInNew,
                        onClick = { openExternalUrl(context, project.githubLink) }
                    )
                }
                if (project.demoLink.isNotEmpty()) {
                    FullWidthOutlinedButton(
                        text = "Canlı Demoyu Görüntüle",
                        icon = Icons.Default.OpenInNew,
                        onClick = { openExternalUrl(context, project.demoLink) }
                    )
                }
            }
        }
    }
}

private enum class MapMode(val label: String) {
    Local("Türkiye"),
    OpenStreetMap("OpenStreetMap")
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MapModeSelector(
    selectedMode: MapMode,
    onModeSelected: (MapMode) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        MapMode.values().forEach { mode ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                label = { Text(mode.label) },
                leadingIcon = {
                    Icon(
                        imageVector = if (selectedMode == mode) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = MobileCardShape
            )
        }
    }
}

@Composable
private fun EventPhotoGallery(event: Event) {
    val photos = remember(event.id, event.gorselUrl, event.galeri) {
        (listOfNotNull(event.gorselUrl?.takeIf { it.isNotBlank() }) + event.galeri.filter { it.isNotBlank() })
            .distinct()
    }
    if (photos.isEmpty()) return

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(photos, key = { it }) { url ->
            AsyncImage(
                model = url,
                contentDescription = event.ad,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 220.dp, height = 132.dp)
                    .clip(MobileCardShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventCard(event: Event, onClick: () -> Unit) {
    MobileCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = event.ad,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = event.aciklama.ifBlank { "Açıklama belirtilmemiş." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MetaChip(text = event.il.ifBlank { "Konum yok" }, icon = Icons.Default.LocationOn)
            MetaChip(text = event.tema.readableSlug(), icon = Icons.Default.Label)
            if (event.durum == "duyuru") {
                MetaChip(text = "Yaklaşan", icon = Icons.Default.CalendarToday)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectCard(project: Project, onClick: () -> Unit) {
    MobileCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = project.ad,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = project.aciklama.ifBlank { "Açıklama belirtilmemiş." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MetaChip(text = project.takimAdi.ifBlank { "Takım yok" }, icon = Icons.Default.Groups)
            MetaChip(text = project.katilimciIller.joinToString().ifBlank { "Şehir yok" }, icon = Icons.Default.LocationOn)
            MetaChip(text = project.tema.readableSlug(), icon = Icons.Default.Label)
        }
    }
}

private fun String.readableSlug(): String {
    if (isBlank()) return "Belirtilmemiş"
    return split("-")
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char -> char.uppercase() }
        }
}

private fun openExternalUrl(context: Context, rawUrl: String) {
    val trimmedUrl = rawUrl.trim()
    if (trimmedUrl.isBlank()) return

    val normalizedUrl = if (
        trimmedUrl.startsWith("http://", ignoreCase = true) ||
        trimmedUrl.startsWith("https://", ignoreCase = true)
    ) {
        trimmedUrl
    } else {
        "https://$trimmedUrl"
    }

    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)))
    }.onFailure {
        Toast.makeText(context, "Bağlantı açılamadı.", Toast.LENGTH_LONG).show()
    }
}
