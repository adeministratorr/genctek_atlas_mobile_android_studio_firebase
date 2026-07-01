package tr.ademyuce.genctekatlas.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "student", // student, teacher, principal, coordinator, admin
    val city: String = "",
    val school: String = "",
    val xp: Int = 0,
    val badges: List<String> = emptyList()
)

data class StudentProfile(
    val id: String = "",
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val city: String = "",
    val school: String = "",
    val grade: String = "",
    val teacherId: String = "",
    val isStudentRep: Boolean = false,
    val xp: Int = 0,
    val badges: List<String> = emptyList()
)

data class Event(
    val id: String = "",
    val ad: String = "",
    val tema: String = "",
    val format: String = "", // online, face-to-face
    val il: String = "",
    val ilce: String = "",
    val kapsam: String = "il", // il | ilce | okul | turkiye
    val durum: String = "gerceklesti", // gerceklesti | duyuru
    val duyuruEtkinlikId: String? = null,
    val ogrenciSiniri: Int? = null,
    val ilKisitlama: Boolean = false,
    val ilceKisitlama: Boolean = false,
    val detay: String = "",
    val tarih: String = "",
    val katilimciSayisi: Int = 0,
    val aciklama: String = "",
    val baglanti: String = "",
    val gorselUrl: String? = null,
    val galeri: List<String> = emptyList(),
    val enlem: Double? = null,
    val boylam: Double? = null,
    val onaylandi: Boolean = false
)

data class Project(
    val id: String = "",
    val ad: String = "",
    val tema: String = "",
    val parkur: String = "",
    val takimAdi: String = "",
    val katilimciIller: List<String> = emptyList(),
    val aciklama: String = "",
    val githubLink: String = "",
    val demoLink: String = "",
    val gorselUrl: String? = null,
    val promptDosyaUrl: String? = null,
    val etikKontrol: Boolean = false,
    val onaylandi: Boolean = false
)

data class DirectMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    val timestamp: Long = 0L,
    val senderName: String = "",
    val receiverName: String = "",
    val read: Boolean = false
)

data class GroupMessage(
    val id: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val messageText: String = "",
    val timestamp: Long = 0L,
    val readBy: List<String> = emptyList()
)

data class Group(
    val id: String = "",
    val ad: String = "",
    val aciklama: String = "",
    val tema: String = "",
    val il: String = "",
    val schoolId: String = "",
    val inviteCode: String = "",
    val olusturanId: String = "",
    val olusturanAd: String = "",
    val olusturmaTarihi: String = "",
    val members: List<String> = emptyList()
)

data class GroupTask(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val assignee: String = "",
    val dueDate: String = "",
    val description: String = "",
    val status: String = "todo" // todo, progress, done
)

data class GroupAnnouncement(
    val id: String = "",
    val groupId: String = "",
    val text: String = "",
    val timestamp: String = "",
    val authorName: String = "",
    val isPinned: Boolean = false
)

data class EventApplication(
    val id: String = "",
    val eventId: String = "",
    val eventName: String = "",
    val eventDate: String = "",
    val eventCity: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val school: String = "",
    val city: String = "",
    val phone: String = "",
    val note: String = "",
    val status: String = "beklemede",
    val timestamp: Long = 0L
)

data class AtlasNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "info",
    val relatedType: String = "",
    val relatedId: String = "",
    val read: Boolean = false,
    val timestamp: Long = 0L
)

data class AppAnnouncement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val scope: String = "all",
    val authorName: String = "",
    val timestamp: Long = 0L,
    val readBy: List<String> = emptyList()
)

data class ChatContact(
    val uid: String = "",
    val name: String = "",
    val role: String = "",
    val city: String = "",
    val school: String = "",
    val email: String = ""
)

data class CityAnalytics(
    val city: String = "",
    val eventsCount: Int = 0,
    val projectsCount: Int = 0,
    val applicationsCount: Int = 0,
    val totalXp: Int = 0
)

data class ThemeAnalytics(
    val theme: String = "",
    val eventsCount: Int = 0,
    val projectsCount: Int = 0
)

data class AnalyticsSummary(
    val totalEvents: Int = 0,
    val upcomingEvents: Int = 0,
    val totalProjects: Int = 0,
    val totalApplications: Int = 0,
    val totalGroups: Int = 0,
    val totalStudents: Int = 0,
    val cityStats: List<CityAnalytics> = emptyList(),
    val themeStats: List<ThemeAnalytics> = emptyList()
)

data class SchoolProfile(
    val schoolName: String = "",
    val city: String = "",
    val students: List<StudentProfile> = emptyList(),
    val groups: List<Group> = emptyList(),
    val events: List<Event> = emptyList()
)

data class AdminPanelSummary(
    val pendingEvents: Int = 0,
    val pendingProjects: Int = 0,
    val pendingApplications: Int = 0,
    val totalUsers: Int = 0,
    val totalGroups: Int = 0,
    val totalAnnouncements: Int = 0
)
