package tr.ademyuce.genctekatlas.ui.screen

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tr.ademyuce.genctekatlas.data.model.AdminPanelSummary
import tr.ademyuce.genctekatlas.data.model.AnalyticsSummary
import tr.ademyuce.genctekatlas.data.model.AppAnnouncement
import tr.ademyuce.genctekatlas.data.model.AtlasNotification
import tr.ademyuce.genctekatlas.data.model.EventApplication
import tr.ademyuce.genctekatlas.data.model.SchoolProfile
import tr.ademyuce.genctekatlas.data.model.User
import tr.ademyuce.genctekatlas.data.repository.FirebaseRepository

private data class ApprovalItem(
    val id: String,
    val type: String,
    val applicant: String,
    val date: String,
    val city: String,
    val school: String,
    val theme: String,
    val scope: String,
    val title: String
)

private data class PanelTab(val key: String, val title: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    repository: FirebaseRepository,
    currentUser: User?
) {
    val coroutineScope = rememberCoroutineScope()
    val user = currentUser ?: repository.getCurrentUser()
    val role = user?.role.orEmpty()
    val isPrivileged = role in listOf("admin", "coordinator")
    val canManageApplications = role in listOf("admin", "coordinator", "teacher", "principal")

    val tabs = remember(role, user?.school) {
        buildList {
            add(PanelTab("analytics", "Analiz"))
            if (isPrivileged) add(PanelTab("moderation", "Moderasyon"))
            if (isPrivileged) add(PanelTab("management", "Yönetim"))
            if (canManageApplications) add(PanelTab("applications", "Başvurular"))
            if (!user?.school.isNullOrBlank() || role in listOf("teacher", "principal", "student")) add(PanelTab("school", "Okul"))
        }
    }
    var activeTabIndex by remember(tabs) { mutableIntStateOf(0) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val selectedTab = tabs.getOrElse(activeTabIndex) { tabs.first() }

    val summaryFlow = remember(refreshKey) { repository.getAnalyticsSummary() }
    val pendingEventsFlow = remember(refreshKey) { repository.getPendingEvents() }
    val pendingProjectsFlow = remember(refreshKey) { repository.getPendingProjects() }
    val applicationsFlow = remember(refreshKey) { repository.getAllEventApplications() }
    val schoolFlow = remember(refreshKey, user?.school) { repository.getSchoolProfile(user?.school.orEmpty()) }
    val adminSummaryFlow = remember(refreshKey) { repository.getAdminPanelSummary() }

    val summary by summaryFlow.collectAsState(initial = AnalyticsSummary())
    val pendingEvents by pendingEventsFlow.collectAsState(initial = emptyList())
    val pendingProjects by pendingProjectsFlow.collectAsState(initial = emptyList())
    val applications by applicationsFlow.collectAsState(initial = emptyList())
    val schoolProfile by schoolFlow.collectAsState(initial = SchoolProfile(user?.school.orEmpty()))
    val adminSummary by adminSummaryFlow.collectAsState(initial = AdminPanelSummary())
    var expandedItemId by remember { mutableStateOf<String?>(null) }

    val pendingItems = remember(pendingEvents, pendingProjects) {
        buildList {
            pendingEvents.forEach { event ->
                add(
                    ApprovalItem(
                        id = event.id,
                        type = "event",
                        applicant = "Yeni Etkinlik",
                        date = event.tarih.ifEmpty { "Belirtilmemiş" },
                        city = event.il,
                        school = event.kapsam,
                        theme = event.tema,
                        scope = event.durum,
                        title = event.ad
                    )
                )
            }
            pendingProjects.forEach { project ->
                add(
                    ApprovalItem(
                        id = project.id,
                        type = "project",
                        applicant = project.takimAdi,
                        date = "Yeni Proje",
                        city = project.katilimciIller.joinToString(),
                        school = project.parkur,
                        theme = project.tema,
                        scope = "Takım Projesi",
                        title = project.ad
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(selectedTabIndex = activeTabIndex, edgePadding = 0.dp) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = activeTabIndex == index,
                        onClick = { activeTabIndex = index },
                        text = { Text(tab.title, fontWeight = FontWeight.Bold) }
                    )
                }
            }
            when (selectedTab.key) {
                "analytics" -> AnalyticsPanel(summary)
                "moderation" -> ModerationPanel(
                    pendingItems = pendingItems,
                    expandedItemId = expandedItemId,
                    onExpandedChange = { expandedItemId = it },
                    onApprove = { item ->
                        coroutineScope.launch {
                            if (item.type == "event") repository.approveEvent(item.id) else repository.approveProject(item.id)
                            refreshKey++
                            expandedItemId = null
                        }
                    },
                    onReject = { item ->
                        coroutineScope.launch {
                            if (item.type == "event") repository.rejectEvent(item.id) else repository.rejectProject(item.id)
                            refreshKey++
                            expandedItemId = null
                        }
                    }
                )
                "management" -> AdminManagementPanel(
                    repository = repository,
                    summary = adminSummary,
                    onRefresh = { refreshKey++ }
                )
                "applications" -> ApplicationsPanel(
                    applications = applications,
                    onStatus = { application, status ->
                        coroutineScope.launch {
                            repository.updateEventApplicationStatus(application.id, status)
                            refreshKey++
                        }
                    }
                )
                "school" -> SchoolPanel(schoolProfile)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnalyticsPanel(summary: AnalyticsSummary) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            MobileScreenHeader(title = "Analiz", subtitle = "Etkinlik, proje ve katılım özeti")
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Etkinlik", summary.totalEvents.toString(), Icons.Default.CalendarToday)
                MetricCard("Yaklaşan", summary.upcomingEvents.toString(), Icons.Default.CheckCircle)
                MetricCard("Proje", summary.totalProjects.toString(), Icons.Default.Label)
                MetricCard("Başvuru", summary.totalApplications.toString(), Icons.Default.VerifiedUser)
                MetricCard("Grup", summary.totalGroups.toString(), Icons.Default.Groups)
                MetricCard("Öğrenci", summary.totalStudents.toString(), Icons.Default.School)
            }
        }
        item {
            Text("Şehirler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(summary.cityStats.take(12), key = { it.city }) { city ->
            MobileCard {
                Text(city.city, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetaChip("Etkinlik ${city.eventsCount}", Icons.Default.CalendarToday)
                    MetaChip("Proje ${city.projectsCount}", Icons.Default.Label)
                    MetaChip("Başvuru ${city.applicationsCount}", Icons.Default.VerifiedUser)
                    MetaChip("XP ${city.totalXp}", Icons.Default.CheckCircle)
                }
            }
        }
        item {
            Text("Temalar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(summary.themeStats.take(10), key = { it.theme }) { theme ->
            MobileCard {
                Text(theme.theme.readablePanelSlug(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${theme.eventsCount} etkinlik, ${theme.projectsCount} proje", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    MobileCard(modifier = Modifier.width(150.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminManagementPanel(
    repository: FirebaseRepository,
    summary: AdminPanelSummary,
    onRefresh: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var announcementTitle by remember { mutableStateOf("") }
    var announcementMessage by remember { mutableStateOf("") }
    var announcementScope by remember { mutableStateOf("all") }
    var notificationTitle by remember { mutableStateOf("") }
    var notificationMessage by remember { mutableStateOf("") }
    var notificationUserId by remember { mutableStateOf("") }
    var notificationRelatedType by remember { mutableStateOf("") }
    var notificationRelatedId by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            MobileScreenHeader(title = "Yönetim", subtitle = "Duyuru, bildirim ve durum özeti")
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Bekleyen etkinlik", summary.pendingEvents.toString(), Icons.Default.CalendarToday)
                MetricCard("Bekleyen proje", summary.pendingProjects.toString(), Icons.Default.Label)
                MetricCard("Bekleyen başvuru", summary.pendingApplications.toString(), Icons.Default.VerifiedUser)
                MetricCard("Kullanıcı", summary.totalUsers.toString(), Icons.Default.Groups)
                MetricCard("Grup", summary.totalGroups.toString(), Icons.Default.School)
                MetricCard("Duyuru", summary.totalAnnouncements.toString(), Icons.Default.Info)
            }
        }
        item {
            MobileCard {
                Text("Genel duyuru yayınla", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = announcementTitle,
                    onValueChange = { announcementTitle = it },
                    label = { Text("Başlık") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MobileCardShape
                )
                OutlinedTextField(
                    value = announcementMessage,
                    onValueChange = { announcementMessage = it },
                    label = { Text("Duyuru metni") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = MobileCardShape
                )
                OutlinedTextField(
                    value = announcementScope,
                    onValueChange = { announcementScope = it },
                    label = { Text("Kapsam (all, school, city)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MobileCardShape
                )
                Button(
                    onClick = {
                        if (announcementTitle.isBlank() || announcementMessage.isBlank()) return@Button
                        coroutineScope.launch {
                            runCatching {
                                repository.createAnnouncement(
                                    AppAnnouncement(
                                        title = announcementTitle.trim(),
                                        message = announcementMessage.trim(),
                                        scope = announcementScope.trim().ifBlank { "all" }
                                    )
                                )
                            }.onSuccess {
                                announcementTitle = ""
                                announcementMessage = ""
                                announcementScope = "all"
                                feedback = "Duyuru yayınlandı."
                                onRefresh()
                            }.onFailure {
                                feedback = it.message ?: "Duyuru yayınlanamadı."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MobileCardShape
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Duyuruyu Yayınla")
                }
            }
        }
        item {
            MobileCard {
                Text("Bildirim gönder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = notificationTitle,
                    onValueChange = { notificationTitle = it },
                    label = { Text("Başlık") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MobileCardShape
                )
                OutlinedTextField(
                    value = notificationMessage,
                    onValueChange = { notificationMessage = it },
                    label = { Text("Mesaj") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = MobileCardShape
                )
                OutlinedTextField(
                    value = notificationUserId,
                    onValueChange = { notificationUserId = it },
                    label = { Text("Kullanıcı UID (boşsa genel)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MobileCardShape
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = notificationRelatedType,
                        onValueChange = { notificationRelatedType = it },
                        label = { Text("İlişki tipi") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = MobileCardShape
                    )
                    OutlinedTextField(
                        value = notificationRelatedId,
                        onValueChange = { notificationRelatedId = it },
                        label = { Text("İlişki ID") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = MobileCardShape
                    )
                }
                Button(
                    onClick = {
                        if (notificationTitle.isBlank() || notificationMessage.isBlank()) return@Button
                        coroutineScope.launch {
                            runCatching {
                                repository.createNotification(
                                    AtlasNotification(
                                        userId = notificationUserId.trim(),
                                        title = notificationTitle.trim(),
                                        message = notificationMessage.trim(),
                                        type = "admin",
                                        relatedType = notificationRelatedType.trim(),
                                        relatedId = notificationRelatedId.trim()
                                    )
                                )
                            }.onSuccess {
                                notificationTitle = ""
                                notificationMessage = ""
                                notificationUserId = ""
                                notificationRelatedType = ""
                                notificationRelatedId = ""
                                feedback = "Bildirim gönderildi."
                                onRefresh()
                            }.onFailure {
                                feedback = it.message ?: "Bildirim gönderilemedi."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MobileCardShape
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bildirimi Gönder")
                }
            }
        }
        feedback?.let { message ->
            item {
                Text(
                    text = message,
                    color = if (message.contains("gönderildi", ignoreCase = true) || message.contains("yayınlandı", ignoreCase = true)) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModerationPanel(
    pendingItems: List<ApprovalItem>,
    expandedItemId: String?,
    onExpandedChange: (String?) -> Unit,
    onApprove: (ApprovalItem) -> Unit,
    onReject: (ApprovalItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            MobileScreenHeader(title = "Moderasyon", subtitle = "${pendingItems.size} bekleyen etkinlik/proje")
        }
        if (pendingItems.isEmpty()) {
            item {
                EmptyState("Bekleyen kayıt yok", "Yeni etkinlik ve proje kayıtları burada görünür.", Icons.Default.Dashboard)
            }
        } else {
            items(pendingItems, key = { "${it.type}-${it.id}" }) { item ->
                val key = "${item.type}-${item.id}"
                val isExpanded = expandedItemId == key
                MobileCard(modifier = Modifier.clickable { onExpandedChange(if (isExpanded) null else key) }) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MetaChip(if (item.type == "project") "Proje" else "Etkinlik", Icons.Default.Info)
                        MetaChip(item.city.ifBlank { "Konum yok" }, Icons.Default.LocationOn)
                        MetaChip(item.date, Icons.Default.CalendarToday)
                    }
                    AnimatedVisibility(visible = isExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                            InfoRow("Başvuran", item.applicant, Icons.Default.Info)
                            InfoRow("Kapsam", item.school, Icons.Default.Dashboard)
                            InfoRow("Tema", item.theme, Icons.Default.Label)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { onReject(item) }, modifier = Modifier.weight(1f), shape = MobileCardShape) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reddet")
                                }
                                Button(onClick = { onApprove(item) }, modifier = Modifier.weight(1f), shape = MobileCardShape) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Onayla")
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApplicationsPanel(
    applications: List<EventApplication>,
    onStatus: (EventApplication, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            MobileScreenHeader(title = "Başvuru Yönetimi", subtitle = "${applications.size} etkinlik başvurusu")
        }
        if (applications.isEmpty()) {
            item { EmptyState("Başvuru yok", "Etkinlik başvuruları burada listelenir.", Icons.Default.VerifiedUser) }
        } else {
            items(applications, key = { it.id }) { application ->
                MobileCard {
                    Text(application.eventName.ifBlank { application.eventId }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    InfoRow("Öğrenci", application.studentName, Icons.Default.VerifiedUser)
                    InfoRow("Okul", application.school, Icons.Default.School)
                    InfoRow("Durum", application.status, Icons.Default.CheckCircle)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { onStatus(application, "reddedildi") }, modifier = Modifier.weight(1f), shape = MobileCardShape) {
                            Text("Reddet")
                        }
                        Button(onClick = { onStatus(application, "onaylandi") }, modifier = Modifier.weight(1f), shape = MobileCardShape) {
                            Text("Onayla")
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SchoolPanel(profile: SchoolProfile) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            MobileScreenHeader(
                title = profile.schoolName.ifBlank { "Okul Profili" },
                subtitle = "${profile.students.size} öğrenci, ${profile.groups.size} grup, ${profile.events.size} etkinlik"
            )
        }
        item {
            MobileCard {
                InfoRow("İl", profile.city, Icons.Default.LocationOn)
                InfoRow("Öğrenci", profile.students.size.toString(), Icons.Default.School)
                InfoRow("Grup", profile.groups.size.toString(), Icons.Default.Groups)
            }
        }
        item {
            Text("Leaderboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(profile.students.sortedByDescending { it.xp }.take(10), key = { it.uid.ifBlank { it.id } }) { student ->
            MobileCard {
                Text(student.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetaChip("XP ${student.xp}", Icons.Default.CheckCircle)
                    MetaChip(student.grade.ifBlank { "Sınıf yok" }, Icons.Default.School)
                    if (student.isStudentRep) MetaChip("Temsilci", Icons.Default.VerifiedUser)
                }
            }
        }
        item {
            Text("Okul Grupları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(profile.groups.take(8), key = { it.id }) { group ->
            MobileCard {
                Text(group.ad, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(group.aciklama, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

private fun String.readablePanelSlug(): String {
    if (isBlank()) return "Belirtilmemiş"
    return split("-").filter { it.isNotBlank() }.joinToString(" ") { part ->
        part.replaceFirstChar { it.uppercase() }
    }
}
