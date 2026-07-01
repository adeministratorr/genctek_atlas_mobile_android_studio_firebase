package tr.ademyuce.genctekatlas.data.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import tr.ademyuce.genctekatlas.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            // Enable Firestore Offline Persistence
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestoreSettings = settings
        }
    }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    var isDemoMode: Boolean = runCatching { FirebaseApp.getInstance() }.isFailure
        private set

    private val demoDirectMessages = mutableListOf<DirectMessage>()
    private val demoGroupMessages = mutableListOf<GroupMessage>()
    private val demoEventApplications = mutableListOf<EventApplication>()
    private val demoEventApplicationCounts = mutableMapOf<String, Int>()
    private val demoNotifications = mutableListOf<AtlasNotification>()
    private val demoAnnouncements = mutableListOf<AppAnnouncement>()
    private val demoGroups = mutableListOf<Group>()
    private val demoGroupTasks = mutableListOf<GroupTask>()
    private val demoGroupAnnouncements = mutableListOf<GroupAnnouncement>()
    private val demoStudents = mutableListOf<StudentProfile>()
    private val demoModeratedEventIds = mutableSetOf<String>()
    private val demoModeratedProjectIds = mutableSetOf<String>()
    private var demoCurrentUser: User? = null
    private var cachedCurrentUser: User? = null

    // Auth actions
    suspend fun login(email: String, password: String): User {
        if (isDemoMode) {
            ensureDemoData()
            val demoUser = if (email == "admin@genctek.org") {
                User(
                    uid = "demo-admin",
                    name = "Demo Admin",
                    email = email,
                    role = "admin",
                    city = "Bursa",
                    school = "Demo Teknoloji Lisesi"
                )
            } else {
                User(
                    uid = "demo-user",
                    name = "Demo Student",
                    email = email,
                    role = "student",
                    city = "Konya",
                    school = "Demo Anadolu Lisesi"
                )
            }
            demoCurrentUser = demoUser
            cachedCurrentUser = demoUser
            return demoUser
        }
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw Exception("User not authenticated")
        val userProfile = getUserProfile(uid)
        cachedCurrentUser = userProfile
        return userProfile
    }

    suspend fun register(user: User, password: String): User {
        if (isDemoMode) {
            ensureDemoData()
            val demoUser = user.copy(uid = "demo-${System.currentTimeMillis()}")
            demoCurrentUser = demoUser
            cachedCurrentUser = demoUser
            return demoUser
        }
        val result = auth.createUserWithEmailAndPassword(user.email, password).await()
        val uid = result.user?.uid ?: throw Exception("User creation failed")
        val finalUser = user.copy(uid = uid)
        firestore.collection("users").document(uid).set(finalUser).await()
        cachedCurrentUser = finalUser
        return finalUser
    }

    private suspend fun getUserProfile(uid: String): User {
        val doc = firestore.collection("users").document(uid).get().await()
        return doc.toObject(User::class.java) ?: throw Exception("User profile not found")
    }

    // Events actions
    fun getEvents(): Flow<List<Event>> = flow {
        if (isDemoMode) {
            emit(getMockEvents().map { event ->
                event.copy(katilimciSayisi = event.katilimciSayisi + (demoEventApplicationCounts[event.id] ?: 0))
            })
            return@flow
        }
        try {
            val snapshot = firestore.collection("events")
                .whereEqualTo("onaylandi", true)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Event::class.java)?.withDocumentId(doc.id)
            }
            emit(list)
        } catch (e: Exception) {
            emit(getMockEvents()) // Fallback to mocks on failure
        }
    }

    suspend fun addEvent(event: Event) {
        if (isDemoMode) return
        val docRef = firestore.collection("events").document()
        val finalEvent = event.copy(id = docRef.id, onaylandi = false)
        docRef.set(finalEvent).await()
    }

    // Projects actions
    fun getProjects(): Flow<List<Project>> = flow {
        if (isDemoMode) {
            emit(getMockProjects())
            return@flow
        }
        try {
            val snapshot = firestore.collection("projects")
                .whereEqualTo("onaylandi", true)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Project::class.java)?.withDocumentId(doc.id)
            }
            emit(list)
        } catch (e: Exception) {
            emit(getMockProjects()) // Fallback to mocks
        }
    }

    suspend fun addProject(project: Project) {
        if (isDemoMode) return
        val docRef = firestore.collection("projects").document()
        val finalProject = project.copy(id = docRef.id, onaylandi = false)
        docRef.set(finalProject).await()
    }

    // Mocks generators
    private fun getMockEvents(): List<Event> {
        return listOf(
            Event(
                id = "mock-e1",
                ad = "Selçuklu MTAL Robotik Kodlama Şenliği",
                tema = "robotik-kodlama",
                format = "Yüz Yüze",
                il = "Konya",
                ilce = "SELÇUKLU",
                aciklama = "Konya Selçuklu Mesleki ve Teknik Anadolu Lisesi bünyesinde düzenlenen Robotik Kodlama Şenliği kapsamında öğrencilerimiz otonom robot projelerini sergilediler.",
                tarih = "2026-04-01",
                katilimciSayisi = 120,
                onaylandi = true,
                durum = "gerceklesti"
            ),
            Event(
                id = "mock-e2",
                ad = "Konya GençTEK Teknolojiye Yolculuk",
                tema = "dijital-yuruyus-stem",
                format = "Yüz Yüze",
                il = "Konya",
                aciklama = "Konya GençTEK koordinatörlüğünde InnoPark'ta düzenlenen girişimcilik eğitimi ve STEM uygulamaları.",
                tarih = "2026-05-21",
                katilimciSayisi = 80,
                onaylandi = true,
                durum = "gerceklesti"
            ),
            Event(
                id = "mock-e3",
                ad = "Bursa YEĞİTEK Çalıştayı",
                tema = "g2s-genc-sektor-bulusmalari",
                format = "Yüz Yüze",
                il = "Bursa",
                aciklama = "YEĞİTEK vizyonu doğrultusunda Bursa ilinde düzenlenen eğitim teknolojileri çalıştayı.",
                tarih = "2026-06-04",
                katilimciSayisi = 120,
                onaylandi = true,
                durum = "gerceklesti"
            ),
            Event(
                id = "mock-e4",
                ad = "GençTek Yaz Yapay Zeka Atölyesi",
                tema = "yapay-zeka",
                format = "Yüz Yüze",
                il = "Konya",
                ilce = "SELÇUKLU",
                aciklama = "Öğrenciler için üretken yapay zeka araçları, etik kullanım ve mini proje geliştirme atölyesi.",
                tarih = "2026-08-12",
                katilimciSayisi = 24,
                ogrenciSiniri = 40,
                baglanti = "https://genctek-atlas.web.app/events/yaz-ai",
                onaylandi = true,
                durum = "duyuru"
            )
        )
    }

    private fun getMockProjects(): List<Project> {
        return listOf(
            Project(
                id = "mock-p1",
                ad = "Bursa Tek Maraton Eğitim Teknolojileri Fikri",
                tema = "tek-maraton",
                parkur = "Tasarım",
                takimAdi = "Uluşehir Mucitleri",
                katilimciIller = listOf("Bursa"),
                aciklama = "Eğitim teknolojilerini sınıf içi etkileşimi artırmak üzere kurgulayan yenilikçi fikir projesi.",
                githubLink = "https://github.com/genctek-atlas/bursa-tek-maraton",
                demoLink = "https://bursa-tek-maraton.web.app",
                etikKontrol = true,
                onaylandi = true
            ),
            Project(
                id = "mock-p2",
                ad = "Yozgat Fikir Maratonu Mobile App",
                tema = "tek-maraton",
                parkur = "Yazılım",
                takimAdi = "Bozok Kodcuları",
                katilimciIller = listOf("Yozgat"),
                aciklama = "Yozgat ilindeki öğrencilere yönelik, ders dışı akran öğrenimini destekleyen mobil uygulama prototipi.",
                githubLink = "https://github.com/genctek-atlas/bozok-asistan",
                etikKontrol = true,
                onaylandi = true
            ),
            Project(
                id = "mock-p3",
                ad = "Yapay Zeka Destekli E-İhracat Optimizasyonu",
                tema = "e-ticaret-ve-e-ihracat",
                parkur = "Yazılım",
                takimAdi = "Ege Lojistik",
                katilimciIller = listOf("İzmir", "Manisa"),
                aciklama = "Genç girişimcilerin yerel ürünlerini yurt dışına satarken lojistik ve gümrük süreçlerini yapay zeka ile optimize eden web arayüzü.",
                githubLink = "https://github.com/genctek-atlas/ege-export-panel",
                demoLink = "https://ege-export.web.app",
                etikKontrol = true,
                onaylandi = true
            )
        )
    }

    fun getCurrentUser(): User? {
        cachedCurrentUser?.let { return it }
        if (isDemoMode) return demoCurrentUser
        val firebaseUser = auth.currentUser ?: return null
        return User(
            uid = firebaseUser.uid,
            name = firebaseUser.displayName ?: "Kullanıcı",
            email = firebaseUser.email ?: "",
            role = "student"
        )
    }

    private suspend fun getAuthenticatedUserProfile(): User? {
        cachedCurrentUser?.let { return it }
        if (isDemoMode) return demoCurrentUser
        val firebaseUser = auth.currentUser ?: return null
        return runCatching { getUserProfile(firebaseUser.uid) }
            .getOrElse {
                User(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "Kullanıcı",
                    email = firebaseUser.email ?: "",
                    role = "student"
                )
            }
            .also { cachedCurrentUser = it }
    }

    fun getThemes(): Flow<List<String>> = flow {
        val fallbackThemes = listOf(
            "Tümü",
            "oyun-tasarimi-egitijam",
            "g2s-genc-sektor-bulusmalari",
            "ritim-ai-yapay-zeka-araclari",
            "dijital-sanatlar",
            "bilisim-hukuku-ve-guvenli-internet",
            "tek-maraton",
            "robot-isletim-sistemi",
            "espor",
            "robot-futbol-ligi",
            "iha-insansiz-hava-araclari",
            "acik-kaynak",
            "e-ticaret-ve-e-ihracat",
            "yapay-zeka",
            "master-tek",
            "dijital-yuruyus-stem",
            "dijital-bagimliliklara-social-fiziksel-alternatif",
            "ctf-bayragi-yakala",
            "vibe-coding",
            "robotik-kodlama"
        )
        if (isDemoMode) {
            emit(fallbackThemes)
            return@flow
        }
        try {
            val snapshot = firestore.collection("themes").get().await()
            val list = snapshot.documents.mapNotNull { it.getString("slug") ?: it.getString("ad") }
            if (list.isEmpty()) {
                emit(fallbackThemes)
            } else {
                emit(listOf("Tümü") + list)
            }
        } catch (e: Exception) {
            emit(fallbackThemes)
        }
    }

    fun getSchools(): Flow<List<String>> = flow {
        val fallbackSchools = listOf("Kadıköy Anadolu Lisesi", "Ankara Fen Lisesi", "İzmir Fen Lisesi")
        if (isDemoMode) {
            emit(fallbackSchools)
            return@flow
        }
        try {
            val snapshot = firestore.collection("custom_schools").get().await()
            val list = snapshot.documents.mapNotNull { it.getString("ad") ?: it.getString("name") }
            if (list.isEmpty()) {
                emit(fallbackSchools)
            } else {
                emit(list)
            }
        } catch (e: Exception) {
            emit(fallbackSchools)
        }
    }

    private fun ensureDemoData() {
        if (demoGroups.isEmpty()) {
            demoGroups += getMockGroups()
        }
        if (demoGroupAnnouncements.isEmpty()) {
            demoGroups.forEach { group ->
                demoGroupAnnouncements += listOf(
                    GroupAnnouncement(
                        id = "a-${group.id}-1",
                        groupId = group.id,
                        text = "${group.ad} haftalık senkronizasyon toplantısı Pazartesi günü saat 19:00'da yapılacaktır.",
                        timestamp = "2026-06-29",
                        authorName = group.olusturanAd,
                        isPinned = true
                    ),
                    GroupAnnouncement(
                        id = "a-${group.id}-2",
                        groupId = group.id,
                        text = "Proje teslimi için son 2 hafta. Bekleyen görevleri güncelleyin.",
                        timestamp = "2026-06-28",
                        authorName = "Koordinasyon"
                    )
                )
            }
        }
        if (demoGroupTasks.isEmpty()) {
            demoGroups.forEach { group ->
                demoGroupTasks += listOf(
                    GroupTask("t-${group.id}-1", group.id, "Ana ekran tasarımı", "Ahmet Y.", "12 Temmuz", "Mobil akışın wireframe taslağı", "todo"),
                    GroupTask("t-${group.id}-2", group.id, "Firebase entegrasyonu", "Elif K.", "15 Temmuz", "Collection izinleri ve demo veri kontrolü", "progress"),
                    GroupTask("t-${group.id}-3", group.id, "Harita katmanı testi", "Mehmet B.", "10 Temmuz", "Şehir seçimi ve fallback testi", "done")
                )
            }
        }
        if (demoStudents.isEmpty()) {
            demoStudents += listOf(
                StudentProfile("s1", "demo-user", "Demo Student", "student@genctek.org", "Konya", "Demo Anadolu Lisesi", "10", "demo-teacher", true, 180, listOf("Harita Kaşifi")),
                StudentProfile("s2", "demo-student-2", "Elif Kaya", "elif@genctek.org", "Bursa", "Demo Teknoloji Lisesi", "11", "demo-teacher", false, 140, listOf("Takım Oyuncusu")),
                StudentProfile("s3", "demo-student-3", "Can Demir", "can@genctek.org", "Konya", "Demo Anadolu Lisesi", "9", "demo-teacher", false, 90)
            )
        }
        if (demoNotifications.isEmpty()) {
            demoNotifications += listOf(
                AtlasNotification("n1", "demo-user", "Başvuru güncellendi", "GençTek Yaz Yapay Zeka Atölyesi başvurunuz beklemede.", "application", "application", "mock-e4", false, System.currentTimeMillis() - 3600000),
                AtlasNotification("n2", "", "Yeni etkinlik", "Konya ilinde yeni bir yapay zeka atölyesi yayınlandı.", "event", "event", "mock-e4", false, System.currentTimeMillis() - 7200000)
            )
        }
        if (demoAnnouncements.isEmpty()) {
            demoAnnouncements += listOf(
                AppAnnouncement("ann1", "GençTek Atlas duyurusu", "Yeni dönem etkinlik başvuruları açıldı.", "all", "GençTek Koordinasyon", System.currentTimeMillis() - 86400000),
                AppAnnouncement("ann2", "Okul temsilcileri", "Okul temsilcileri kendi ekiplerini panelden güncelleyebilir.", "school", "Koordinasyon", System.currentTimeMillis() - 172800000)
            )
        }
        if (demoEventApplications.isEmpty()) {
            demoEventApplications += EventApplication(
                id = "app-demo-1",
                eventId = "mock-e4",
                eventName = "GençTek Yaz Yapay Zeka Atölyesi",
                eventDate = "2026-08-12",
                eventCity = "Konya",
                studentId = "demo-user",
                studentName = "Demo Student",
                studentEmail = "student@genctek.org",
                school = "Demo Anadolu Lisesi",
                city = "Konya",
                status = "beklemede",
                timestamp = System.currentTimeMillis() - 7200000
            )
            demoEventApplicationCounts["mock-e4"] = 1
        }
    }

    // Groups actions
    fun getGroups(): Flow<List<Group>> = flow {
        val user = getCurrentUser()
        val canSeeAll = user?.role in listOf("admin", "coordinator", "principal")
        if (isDemoMode) {
            ensureDemoData()
            emit(demoGroups.visibleFor(user, canSeeAll))
            return@flow
        }
        try {
            val snapshot = firestore.collection("groups").get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Group::class.java)?.withDocumentId(doc.id)
            }
            emit(list.visibleFor(user, canSeeAll))
        } catch (e: Exception) {
            emit(getMockGroups().visibleFor(user, canSeeAll))
        }
    }

    fun getGroupAnnouncements(groupId: String): Flow<List<GroupAnnouncement>> = flow {
        if (isDemoMode) {
            ensureDemoData()
            emit(demoGroupAnnouncements.filter { it.groupId == groupId }.sortedWith(compareByDescending<GroupAnnouncement> { it.isPinned }.thenByDescending { it.timestamp }))
            return@flow
        }
        try {
            val snapshot = firestore.collection("group_announcements")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(GroupAnnouncement::class.java)?.withDocumentId(doc.id)
            }
            emit(list)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getGroupTasks(groupId: String): Flow<List<GroupTask>> = flow {
        if (isDemoMode) {
            ensureDemoData()
            emit(demoGroupTasks.filter { it.groupId == groupId })
            return@flow
        }
        try {
            val snapshot = firestore.collection("group_tasks")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(GroupTask::class.java)?.withDocumentId(doc.id)
            }
            emit(list)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun updateGroupTaskStatus(taskId: String, newStatus: String) {
        if (isDemoMode) {
            ensureDemoData()
            val index = demoGroupTasks.indexOfFirst { it.id == taskId }
            if (index >= 0) demoGroupTasks[index] = demoGroupTasks[index].copy(status = newStatus)
            return
        }
        firestore.collection("group_tasks").document(taskId)
            .update("status", newStatus)
            .await()
    }

    suspend fun createGroup(group: Group): Group {
        val user = getCurrentUser()
        if (!canCurrentUserCreateGroup()) {
            throw IllegalStateException("Çalışma grubu oluşturmak için yönetici yetkisi gerekir.")
        }
        val finalGroup = group.copy(
            id = group.id.ifBlank { "group-${System.currentTimeMillis()}" },
            inviteCode = group.inviteCode.ifBlank { "GT${System.currentTimeMillis().toString().takeLast(4)}" },
            olusturanId = group.olusturanId.ifBlank { user?.uid.orEmpty() },
            olusturanAd = group.olusturanAd.ifBlank { user?.name ?: "Kullanıcı" },
            members = group.members.ifEmpty { listOfNotNull(user?.uid).ifEmpty { listOf("demo-user") } },
            olusturmaTarihi = group.olusturmaTarihi.ifBlank { "2026-06-30" }
        )
        if (isDemoMode) {
            ensureDemoData()
            demoGroups += finalGroup
            return finalGroup
        }
        val docRef = firestore.collection("groups").document()
        val firebaseGroup = finalGroup.copy(id = docRef.id)
        docRef.set(firebaseGroup).await()
        return firebaseGroup
    }

    suspend fun joinGroupByInviteCode(inviteCode: String): Group {
        val userId = getCurrentUser()?.uid ?: throw IllegalStateException("Gruba katılmak için giriş yapmalısınız.")
        if (isDemoMode) {
            ensureDemoData()
            val index = demoGroups.indexOfFirst { it.inviteCode.equals(inviteCode.trim(), ignoreCase = true) }
            if (index < 0) throw IllegalArgumentException("Davet kodu bulunamadı.")
            val group = demoGroups[index]
            val members = (group.members + userId).distinct()
            demoGroups[index] = group.copy(members = members)
            return demoGroups[index]
        }
        val snapshot = firestore.collection("groups")
            .whereEqualTo("inviteCode", inviteCode.trim())
            .get()
            .await()
        val doc = snapshot.documents.firstOrNull() ?: throw IllegalArgumentException("Davet kodu bulunamadı.")
        val group = doc.toObject(Group::class.java)?.withDocumentId(doc.id) ?: throw IllegalStateException("Grup okunamadı.")
        firestore.collection("groups").document(doc.id)
            .update("members", (group.members + userId).distinct())
            .await()
        return group.copy(members = (group.members + userId).distinct())
    }

    suspend fun createGroupAnnouncement(announcement: GroupAnnouncement): GroupAnnouncement {
        val finalAnnouncement = announcement.copy(
            id = announcement.id.ifBlank { "ann-${System.currentTimeMillis()}" },
            timestamp = announcement.timestamp.ifBlank { "2026-06-30" },
            authorName = announcement.authorName.ifBlank { getCurrentUser()?.name ?: "Kullanıcı" }
        )
        if (isDemoMode) {
            ensureDemoData()
            demoGroupAnnouncements += finalAnnouncement
            return finalAnnouncement
        }
        val docRef = firestore.collection("group_announcements").document()
        val firebaseAnnouncement = finalAnnouncement.copy(id = docRef.id)
        docRef.set(firebaseAnnouncement).await()
        return firebaseAnnouncement
    }

    suspend fun updateGroupAnnouncement(announcement: GroupAnnouncement) {
        if (isDemoMode) {
            ensureDemoData()
            val index = demoGroupAnnouncements.indexOfFirst { it.id == announcement.id }
            if (index >= 0) demoGroupAnnouncements[index] = announcement
            return
        }
        firestore.collection("group_announcements").document(announcement.id).set(announcement).await()
    }

    suspend fun deleteGroupAnnouncement(announcementId: String) {
        if (isDemoMode) {
            demoGroupAnnouncements.removeAll { it.id == announcementId }
            return
        }
        firestore.collection("group_announcements").document(announcementId).delete().await()
    }

    suspend fun createGroupTask(task: GroupTask): GroupTask {
        val finalTask = task.copy(id = task.id.ifBlank { "task-${System.currentTimeMillis()}" })
        if (isDemoMode) {
            ensureDemoData()
            demoGroupTasks += finalTask
            return finalTask
        }
        val docRef = firestore.collection("group_tasks").document()
        val firebaseTask = finalTask.copy(id = docRef.id)
        docRef.set(firebaseTask).await()
        return firebaseTask
    }

    suspend fun updateGroupTask(task: GroupTask) {
        if (isDemoMode) {
            ensureDemoData()
            val index = demoGroupTasks.indexOfFirst { it.id == task.id }
            if (index >= 0) demoGroupTasks[index] = task
            return
        }
        firestore.collection("group_tasks").document(task.id).set(task).await()
    }

    suspend fun deleteGroupTask(taskId: String) {
        if (isDemoMode) {
            demoGroupTasks.removeAll { it.id == taskId }
            return
        }
        firestore.collection("group_tasks").document(taskId).delete().await()
    }

    private fun getMockGroups(): List<Group> {
        return listOf(
            Group(
                id = "mock-g1",
                ad = "Siber Güvenlik Çalışma Grubu",
                aciklama = "Bilişim güvenliği ve CTF yarışmalarına hazırlık çalışma grubu.",
                tema = "siber-guvenlik",
                il = "Bursa",
                inviteCode = "SEC881",
                olusturanId = "demo-admin",
                olusturanAd = "Ahmet Yılmaz",
                members = listOf("demo-user", "demo-admin", "demo-student-2")
            ),
            Group(
                id = "mock-g2",
                ad = "Oyun Tasarımı Çalışma Grubu",
                aciklama = "EğitiJAM oyun geliştirme maratonu hazırlık grubu.",
                tema = "oyun-tasarimi",
                il = "Konya",
                inviteCode = "GAME99",
                olusturanId = "demo-admin",
                olusturanAd = "Elif Demir",
                members = listOf("demo-user", "demo-admin", "demo-teacher")
            )
        )
    }

    // Direct Messages actions
    fun getDirectMessages(senderId: String, receiverId: String): Flow<List<DirectMessage>> = flow {
        if (isDemoMode) {
            val sentDemoMessages = demoDirectMessages.filter {
                (it.senderId == senderId && it.receiverId == receiverId) ||
                    (it.senderId == receiverId && it.receiverId == senderId)
            }
            emit((getMockMessages(senderId, receiverId) + sentDemoMessages).sortedBy { it.timestamp })
            return@flow
        }
        try {
            val sent = firestore.collection("direct_messages")
                .whereEqualTo("senderId", senderId)
                .whereEqualTo("receiverId", receiverId)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> doc.toObject(DirectMessage::class.java)?.withDocumentId(doc.id) }

            val received = firestore.collection("direct_messages")
                .whereEqualTo("senderId", receiverId)
                .whereEqualTo("receiverId", senderId)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> doc.toObject(DirectMessage::class.java)?.withDocumentId(doc.id) }

            val merged = (sent + received).sortedBy { it.timestamp }
            emit(merged)
        } catch (e: Exception) {
            emit(getMockMessages(senderId, receiverId))
        }
    }

    suspend fun sendDirectMessage(message: DirectMessage) {
        if (isDemoMode) {
            val finalMsg = message.copy(id = "demo-m-${System.currentTimeMillis()}", timestamp = System.currentTimeMillis())
            demoDirectMessages += finalMsg
            return
        }
        val user = getCurrentUser() ?: throw IllegalStateException("Mesaj göndermek için giriş yapmalısınız.")
        val docRef = firestore.collection("direct_messages").document()
        val finalMsg = message.copy(
            id = docRef.id,
            senderId = user.uid,
            senderName = message.senderName.ifBlank { user.name.ifBlank { user.email } },
            timestamp = System.currentTimeMillis()
        )
        docRef.set(finalMsg).await()
    }

    suspend fun markDirectConversationAsRead(senderId: String, receiverId: String) {
        if (isDemoMode) {
            demoDirectMessages.replaceAll { message ->
                if (message.senderId == receiverId && message.receiverId == senderId) {
                    message.copy(read = true)
                } else {
                    message
                }
            }
            return
        }
        val snapshot = firestore.collection("direct_messages")
            .whereEqualTo("senderId", receiverId)
            .whereEqualTo("receiverId", senderId)
            .get()
            .await()
        snapshot.documents.forEach { doc ->
            firestore.collection("direct_messages").document(doc.id).update("read", true).await()
        }
    }

    private fun getMockMessages(senderId: String, receiverId: String): List<DirectMessage> {
        return listOf(
            DirectMessage("m1", receiverId, senderId, "Projenizde hangi modeli kullanmayı düşünüyorsunuz?", System.currentTimeMillis() - 100000, senderName = "Dr. Mustafa Can", receiverName = "Ben", read = true),
            DirectMessage("m2", senderId, receiverId, "TensorFlow Lite ile mobil uyumlu bir model eğittik hocam.", System.currentTimeMillis() - 50000, senderName = "Ben", receiverName = "Dr. Mustafa Can", read = true),
            DirectMessage("m3", receiverId, senderId, "Harika. Sunum akışı da iyi gidiyor.", System.currentTimeMillis(), senderName = "Dr. Mustafa Can", receiverName = "Ben")
        )
    }

    fun getGroupMessages(groupId: String): Flow<List<GroupMessage>> = flow {
        if (isDemoMode) {
            ensureDemoData()
            val demoSeed = getMockGroupMessages(groupId)
            val sent = demoGroupMessages.filter { it.groupId == groupId }
            emit((demoSeed + sent).distinctBy { it.id }.sortedBy { it.timestamp })
            return@flow
        }
        try {
            val snapshot = firestore.collection("group_messages")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(GroupMessage::class.java)?.withDocumentId(doc.id)
            }.sortedBy { it.timestamp }
            emit(list)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun sendGroupMessage(message: GroupMessage): GroupMessage {
        val user = getCurrentUser() ?: throw IllegalStateException("Mesaj göndermek için giriş yapmalısınız.")
        val finalMessage = message.copy(
            id = message.id.ifBlank { "group-msg-${System.currentTimeMillis()}" },
            senderId = message.senderId.ifBlank { user.uid },
            senderName = message.senderName.ifBlank { user.name.ifBlank { user.email } },
            timestamp = System.currentTimeMillis(),
            readBy = (message.readBy + user.uid).distinct()
        )
        if (isDemoMode) {
            ensureDemoData()
            demoGroupMessages += finalMessage
            return finalMessage
        }
        val docRef = firestore.collection("group_messages").document()
        val firebaseMessage = finalMessage.copy(id = docRef.id)
        docRef.set(firebaseMessage).await()
        return firebaseMessage
    }

    suspend fun markGroupMessagesAsRead(groupId: String) {
        val userId = getCurrentUser()?.uid ?: return
        if (isDemoMode) {
            demoGroupMessages.replaceAll { message ->
                if (message.groupId == groupId && !message.readBy.contains(userId)) {
                    message.copy(readBy = (message.readBy + userId).distinct())
                } else {
                    message
                }
            }
            return
        }
        val snapshot = firestore.collection("group_messages")
            .whereEqualTo("groupId", groupId)
            .get()
            .await()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(GroupMessage::class.java)?.withDocumentId(doc.id)
        }.filterNot { it.readBy.contains(userId) }
            .forEach { message ->
                firestore.collection("group_messages").document(message.id)
                    .update("readBy", (message.readBy + userId).distinct())
                    .await()
            }
    }

    private fun getMockGroupMessages(groupId: String): List<GroupMessage> {
        val now = System.currentTimeMillis()
        return listOf(
            GroupMessage("gm-$groupId-1", groupId, "demo-teacher", "Dr. Mustafa Can", "Bu haftaki sprint hedeflerini güncelledim.", now - 180000),
            GroupMessage("gm-$groupId-2", groupId, "demo-user", "Demo Student", "Görev panosundaki tasarım işini bugün tamamlayacağım.", now - 120000),
            GroupMessage("gm-$groupId-3", groupId, "demo-student-2", "Elif Kaya", "Toplantı notlarını duyuruya ekleyebilir miyiz?", now - 60000)
        )
    }

    fun getChatContacts(): Flow<List<ChatContact>> = flow {
        val current = getCurrentUser()
        if (isDemoMode) {
            ensureDemoData()
            emit(
                listOf(
                    ChatContact("demo-teacher", "Dr. Mustafa Can", "teacher", "Konya", "Demo Anadolu Lisesi", "mustafa@genctek.org"),
                    ChatContact("demo-student-2", "Elif Kaya", "student", "Bursa", "Demo Teknoloji Lisesi", "elif@genctek.org"),
                    ChatContact("demo-coordinator", "GençTek Koordinasyon", "coordinator", current?.city.orEmpty(), current?.school.orEmpty(), "koordinasyon@genctek.org")
                ).filter { it.uid != current?.uid }
            )
            return@flow
        }
        try {
            val snapshot = firestore.collection("users").get().await()
            val contacts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.withDocumentId(doc.id)
            }.filter { it.uid != current?.uid }
                .map { user ->
                    ChatContact(
                        uid = user.uid,
                        name = user.name.ifBlank { user.email },
                        role = user.role,
                        city = user.city,
                        school = user.school,
                        email = user.email
                    )
                }
            emit(contacts)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getNotifications(): Flow<List<AtlasNotification>> = flow {
        val userId = getCurrentUser()?.uid.orEmpty()
        if (isDemoMode) {
            ensureDemoData()
            emit(demoNotifications.filter { it.userId.isBlank() || it.userId == userId }.sortedByDescending { it.timestamp })
            return@flow
        }
        try {
            val targeted = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(AtlasNotification::class.java)?.withDocumentId(it.id) }
            val global = firestore.collection("notifications")
                .whereEqualTo("userId", "")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(AtlasNotification::class.java)?.withDocumentId(it.id) }
            emit((targeted + global).sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getAnnouncements(): Flow<List<AppAnnouncement>> = flow {
        if (isDemoMode) {
            ensureDemoData()
            emit(demoAnnouncements.sortedByDescending { it.timestamp })
            return@flow
        }
        try {
            val snapshot = firestore.collection("announcements").get().await()
            emit(snapshot.documents.mapNotNull { it.toObject(AppAnnouncement::class.java)?.withDocumentId(it.id) }.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun createAnnouncement(announcement: AppAnnouncement): AppAnnouncement {
        requireAdminAccess()
        val user = getCurrentUser()
        val finalAnnouncement = announcement.copy(
            id = announcement.id.ifBlank { "announcement-${System.currentTimeMillis()}" },
            authorName = announcement.authorName.ifBlank { user?.name ?: "Yönetim" },
            timestamp = if (announcement.timestamp == 0L) System.currentTimeMillis() else announcement.timestamp
        )
        if (isDemoMode) {
            ensureDemoData()
            demoAnnouncements += finalAnnouncement
            return finalAnnouncement
        }
        val docRef = firestore.collection("announcements").document()
        val firebaseAnnouncement = finalAnnouncement.copy(id = docRef.id)
        docRef.set(firebaseAnnouncement).await()
        return firebaseAnnouncement
    }

    suspend fun createNotification(notification: AtlasNotification): AtlasNotification {
        requireAdminAccess()
        val finalNotification = notification.copy(
            id = notification.id.ifBlank { "notification-${System.currentTimeMillis()}" },
            timestamp = if (notification.timestamp == 0L) System.currentTimeMillis() else notification.timestamp
        )
        if (isDemoMode) {
            ensureDemoData()
            demoNotifications += finalNotification
            return finalNotification
        }
        val docRef = firestore.collection("notifications").document()
        val firebaseNotification = finalNotification.copy(id = docRef.id)
        docRef.set(firebaseNotification).await()
        return firebaseNotification
    }

    fun getAdminPanelSummary(): Flow<AdminPanelSummary> = flow {
        if (isDemoMode) {
            ensureDemoData()
            emit(
                AdminPanelSummary(
                    pendingEvents = 1 - demoModeratedEventIds.size.coerceAtMost(1),
                    pendingProjects = 1 - demoModeratedProjectIds.size.coerceAtMost(1),
                    pendingApplications = demoEventApplications.count { it.status == "beklemede" },
                    totalUsers = demoStudents.size + 3,
                    totalGroups = demoGroups.size,
                    totalAnnouncements = demoAnnouncements.size
                )
            )
            return@flow
        }
        try {
            val pendingEvents = firestore.collection("events").whereEqualTo("onaylandi", false).get().await().size()
            val pendingProjects = firestore.collection("projects").whereEqualTo("onaylandi", false).get().await().size()
            val applications = firestore.collection("event_applications").get().await().documents
                .mapNotNull { it.toObject(EventApplication::class.java) }
            val usersCount = firestore.collection("users").get().await().size()
            val groupsCount = firestore.collection("groups").get().await().size()
            val announcementsCount = firestore.collection("announcements").get().await().size()
            emit(
                AdminPanelSummary(
                    pendingEvents = pendingEvents,
                    pendingProjects = pendingProjects,
                    pendingApplications = applications.count { it.status == "beklemede" },
                    totalUsers = usersCount,
                    totalGroups = groupsCount,
                    totalAnnouncements = announcementsCount
                )
            )
        } catch (e: Exception) {
            emit(AdminPanelSummary())
        }
    }

    suspend fun markNotificationAsRead(notificationId: String) {
        if (isDemoMode) {
            val index = demoNotifications.indexOfFirst { it.id == notificationId }
            if (index >= 0) demoNotifications[index] = demoNotifications[index].copy(read = true)
            return
        }
        firestore.collection("notifications").document(notificationId).update("read", true).await()
    }

    suspend fun markAllNotificationsAsRead() {
        val notifications = getNotificationsOnce()
        if (isDemoMode) {
            notifications.forEach { markNotificationAsRead(it.id) }
            return
        }
        notifications.forEach { notification ->
            firestore.collection("notifications").document(notification.id).update("read", true).await()
        }
    }

    suspend fun deleteNotification(notificationId: String) {
        if (isDemoMode) {
            demoNotifications.removeAll { it.id == notificationId }
            return
        }
        firestore.collection("notifications").document(notificationId).delete().await()
    }

    private suspend fun getNotificationsOnce(): List<AtlasNotification> {
        val userId = getCurrentUser()?.uid.orEmpty()
        if (isDemoMode) {
            ensureDemoData()
            return demoNotifications.filter { it.userId.isBlank() || it.userId == userId }
        }
        val targeted = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(AtlasNotification::class.java)?.withDocumentId(it.id) }
        val global = firestore.collection("notifications")
            .whereEqualTo("userId", "")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(AtlasNotification::class.java)?.withDocumentId(it.id) }
        return targeted + global
    }

    // Pending approvals / Moderation actions
    fun getPendingEvents(): Flow<List<Event>> = flow {
        if (isDemoMode) {
            emit(listOf(
                Event(id = "p-e1", ad = "Yapay Zeka Okuryazarlığı Semineri", tema = "yapay-zeka", il = "Ankara", aciklama = "Eğitim onay bekliyor.", tarih = "2026-08-01", onaylandi = false)
            ).filterNot { demoModeratedEventIds.contains(it.id) })
            return@flow
        }
        try {
            val snapshot = firestore.collection("events")
                .whereEqualTo("onaylandi", false)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Event::class.java)?.withDocumentId(doc.id)
            }
            emit(list)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getPendingProjects(): Flow<List<Project>> = flow {
        if (isDemoMode) {
            emit(listOf(
                Project(id = "p-p1", ad = "GençTek Atlas Mobil Projesi", tema = "mobil", takimAdi = "Uluşehir Mucitleri", katilimciIller = listOf("Bursa"), aciklama = "Proje onay bekliyor.", onaylandi = false)
            ).filterNot { demoModeratedProjectIds.contains(it.id) })
            return@flow
        }
        try {
            val snapshot = firestore.collection("projects")
                .whereEqualTo("onaylandi", false)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Project::class.java)?.withDocumentId(doc.id)
            }
            emit(list)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun approveEvent(eventId: String) {
        if (isDemoMode) {
            demoModeratedEventIds += eventId
            return
        }
        firestore.collection("events").document(eventId)
            .update("onaylandi", true)
            .await()
    }

    suspend fun rejectEvent(eventId: String) {
        if (isDemoMode) {
            demoModeratedEventIds += eventId
            return
        }
        firestore.collection("events").document(eventId).delete().await()
    }

    suspend fun approveProject(projectId: String) {
        if (isDemoMode) {
            demoModeratedProjectIds += projectId
            return
        }
        firestore.collection("projects").document(projectId)
            .update("onaylandi", true)
            .await()
    }

    suspend fun rejectProject(projectId: String) {
        if (isDemoMode) {
            demoModeratedProjectIds += projectId
            return
        }
        firestore.collection("projects").document(projectId).delete().await()
    }

    fun getMyEventApplications(): Flow<List<EventApplication>> = flow {
        val user = getAuthenticatedUserProfile()
        if (user == null) {
            emit(emptyList())
            return@flow
        }
        if (isDemoMode) {
            emit(demoEventApplications.filter { it.studentId == user.uid })
            return@flow
        }
        try {
            val snapshot = firestore.collection("event_applications")
                .whereEqualTo("studentId", user.uid)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(EventApplication::class.java)?.withDocumentId(doc.id)
            }
            emit(list)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getAllEventApplications(): Flow<List<EventApplication>> = flow {
        if (isDemoMode) {
            ensureDemoData()
            emit(demoEventApplications.sortedByDescending { it.timestamp })
            return@flow
        }
        try {
            val snapshot = firestore.collection("event_applications").get().await()
            emit(snapshot.documents.mapNotNull { it.toObject(EventApplication::class.java)?.withDocumentId(it.id) }.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun updateEventApplicationStatus(applicationId: String, status: String) {
        if (isDemoMode) {
            ensureDemoData()
            val index = demoEventApplications.indexOfFirst { it.id == applicationId }
            if (index >= 0) demoEventApplications[index] = demoEventApplications[index].copy(status = status)
            return
        }
        firestore.collection("event_applications").document(applicationId).update("status", status).await()
    }

    fun getAnalyticsSummary(): Flow<AnalyticsSummary> = flow {
        if (isDemoMode) {
            ensureDemoData()
            val events = getMockEvents().map { it.copy(katilimciSayisi = it.katilimciSayisi + (demoEventApplicationCounts[it.id] ?: 0)) }
            val projects = getMockProjects()
            val applications = demoEventApplications
            emit(buildAnalyticsSummary(events, projects, applications, demoGroups, demoStudents))
            return@flow
        }
        try {
            val events = firestore.collection("events").get().await().documents.mapNotNull { it.toObject(Event::class.java)?.withDocumentId(it.id) }
            val projects = firestore.collection("projects").get().await().documents.mapNotNull { it.toObject(Project::class.java)?.withDocumentId(it.id) }
            val applications = firestore.collection("event_applications").get().await().documents.mapNotNull { it.toObject(EventApplication::class.java)?.withDocumentId(it.id) }
            val groups = firestore.collection("groups").get().await().documents.mapNotNull { it.toObject(Group::class.java)?.withDocumentId(it.id) }
            val students = firestore.collection("students").get().await().documents.mapNotNull { it.toObject(StudentProfile::class.java)?.withDocumentId(it.id) }
            emit(buildAnalyticsSummary(events, projects, applications, groups, students))
        } catch (e: Exception) {
            emit(AnalyticsSummary())
        }
    }

    private fun buildAnalyticsSummary(
        events: List<Event>,
        projects: List<Project>,
        applications: List<EventApplication>,
        groups: List<Group>,
        students: List<StudentProfile>
    ): AnalyticsSummary {
        val cityNames = (events.map { it.il } + projects.flatMap { it.katilimciIller } + applications.map { it.city } + students.map { it.city })
            .map { normalizeCityName(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val cityStats = cityNames.map { city ->
            CityAnalytics(
                city = city,
                eventsCount = events.count { normalizeCityName(it.il) == city },
                projectsCount = projects.count { project -> project.katilimciIller.any { normalizeCityName(it) == city } },
                applicationsCount = applications.count { normalizeCityName(it.city.ifBlank { it.eventCity }) == city },
                totalXp = students.filter { normalizeCityName(it.city) == city }.sumOf { it.xp }
            )
        }.sortedWith(compareByDescending<CityAnalytics> { it.eventsCount + it.projectsCount }.thenBy { it.city })

        val themes = (events.map { it.tema } + projects.map { it.tema }).filter { it.isNotBlank() }.distinct().sorted()
        val themeStats = themes.map { theme ->
            ThemeAnalytics(
                theme = theme,
                eventsCount = events.count { it.tema == theme },
                projectsCount = projects.count { it.tema == theme }
            )
        }.sortedByDescending { it.eventsCount + it.projectsCount }

        return AnalyticsSummary(
            totalEvents = events.size,
            upcomingEvents = events.count { it.durum == "duyuru" },
            totalProjects = projects.size,
            totalApplications = applications.size,
            totalGroups = groups.size,
            totalStudents = students.size,
            cityStats = cityStats,
            themeStats = themeStats
        )
    }

    fun getSchoolProfile(schoolName: String): Flow<SchoolProfile> = flow {
        val normalizedSchool = schoolName.trim()
        if (normalizedSchool.isBlank()) {
            emit(SchoolProfile())
            return@flow
        }
        if (isDemoMode) {
            ensureDemoData()
            val students = demoStudents.filter { it.school.equals(normalizedSchool, ignoreCase = true) }
            val groups = demoGroups.filter { it.schoolId.equals(normalizedSchool, ignoreCase = true) || students.any { student -> it.members.contains(student.uid) } }
            val events = getMockEvents().filter { it.kapsam.equals(normalizedSchool, ignoreCase = true) || it.aciklama.contains(normalizedSchool, ignoreCase = true) }
            emit(SchoolProfile(normalizedSchool, students.firstOrNull()?.city.orEmpty(), students, groups, events))
            return@flow
        }
        try {
            val users = firestore.collection("users")
                .whereEqualTo("school", normalizedSchool)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(User::class.java)?.withDocumentId(it.id) }
            val students = users.map {
                StudentProfile(
                    id = it.uid,
                    uid = it.uid,
                    name = it.name,
                    email = it.email,
                    city = it.city,
                    school = it.school,
                    xp = it.xp,
                    badges = it.badges
                )
            }
            val groups = firestore.collection("groups").get().await().documents
                .mapNotNull { it.toObject(Group::class.java)?.withDocumentId(it.id) }
                .filter { it.schoolId == normalizedSchool || students.any { student -> it.members.contains(student.uid) } }
            val events = firestore.collection("events").get().await().documents
                .mapNotNull { it.toObject(Event::class.java)?.withDocumentId(it.id) }
                .filter { it.kapsam == normalizedSchool || it.aciklama.contains(normalizedSchool, ignoreCase = true) }
            emit(SchoolProfile(normalizedSchool, students.firstOrNull()?.city.orEmpty(), students, groups, events))
        } catch (e: Exception) {
            emit(SchoolProfile(normalizedSchool))
        }
    }

    suspend fun applyToEvent(eventId: String): EventApplication {
        return applyToEvent(Event(id = eventId), EventApplication(eventId = eventId))
    }

    suspend fun applyToEvent(event: Event, application: EventApplication): EventApplication {
        val user = getAuthenticatedUserProfile()
            ?: throw IllegalStateException("Etkinliğe başvurmak için giriş yapmalısınız.")
        val eventId = event.id.ifBlank { application.eventId }
        if (eventId.isBlank()) {
            throw IllegalArgumentException("Etkinlik bilgisi eksik.")
        }

        val now = System.currentTimeMillis()
        val applicationId = "${eventId}_${user.uid}"
        val finalApplication = application.copy(
            id = applicationId,
            eventId = eventId,
            eventName = application.eventName.ifBlank { event.ad },
            eventDate = application.eventDate.ifBlank { event.tarih },
            eventCity = application.eventCity.ifBlank { event.il },
            studentId = user.uid,
            studentName = application.studentName.ifBlank { user.name.ifBlank { "Kullanıcı" } },
            studentEmail = application.studentEmail.ifBlank { user.email },
            school = application.school.ifBlank { user.school },
            city = application.city.ifBlank { user.city.ifBlank { event.il } },
            status = "beklemede",
            timestamp = now
        )

        validateEventApplication(event, finalApplication)

        if (isDemoMode) {
            if (demoEventApplications.any { it.eventId == eventId && it.studentId == user.uid }) {
                throw IllegalStateException("Bu etkinliğe daha önce başvurdunuz.")
            }
            demoEventApplications += finalApplication
            demoEventApplicationCounts[eventId] = (demoEventApplicationCounts[eventId] ?: 0) + 1
            return finalApplication
        }

        val eventRef = firestore.collection("events").document(eventId)
        val applicationRef = firestore.collection("event_applications").document(applicationId)

        firestore.runTransaction { transaction ->
            val existingApplication = transaction.get(applicationRef)
            if (existingApplication.exists()) {
                throw IllegalStateException("Bu etkinliğe daha önce başvurdunuz.")
            }

            val eventSnapshot = transaction.get(eventRef)
            if (!eventSnapshot.exists()) {
                throw IllegalStateException("Etkinlik bulunamadı.")
            }

            val currentCount = eventSnapshot.getLong("katilimciSayisi") ?: 0L
            val limit = eventSnapshot.getLong("ogrenciSiniri")
            if (limit != null && currentCount >= limit) {
                throw IllegalStateException("Etkinlik kontenjanı dolu.")
            }

            transaction.set(applicationRef, finalApplication)
            transaction.update(eventRef, "katilimciSayisi", currentCount + 1)
        }.await()

        return finalApplication
    }

    private fun validateEventApplication(event: Event, application: EventApplication) {
        if (application.studentName.isBlank()) {
            throw IllegalArgumentException("Ad soyad bilgisi boş bırakılamaz.")
        }
        if (application.school.isBlank()) {
            throw IllegalArgumentException("Okul bilgisi boş bırakılamaz.")
        }
        if (event.ogrenciSiniri != null && event.katilimciSayisi >= event.ogrenciSiniri) {
            throw IllegalStateException("Etkinlik kontenjanı dolu.")
        }
        if (event.ilKisitlama && event.il.isNotBlank() && !application.city.equals(event.il, ignoreCase = true)) {
            throw IllegalStateException("Bu etkinliğe yalnızca ${event.il} ilindeki öğrenciler başvurabilir.")
        }
    }

    suspend fun updateCurrentUserProfile(updatedUser: User): User {
        val current = getCurrentUser() ?: throw IllegalStateException("Profil güncellemek için giriş yapmalısınız.")
        val finalUser = updatedUser.copy(uid = current.uid, email = updatedUser.email.ifBlank { current.email })
        if (isDemoMode) {
            demoCurrentUser = finalUser
            cachedCurrentUser = finalUser
            val studentIndex = demoStudents.indexOfFirst { it.uid == finalUser.uid }
            if (studentIndex >= 0) {
                demoStudents[studentIndex] = demoStudents[studentIndex].copy(
                    name = finalUser.name,
                    email = finalUser.email,
                    city = finalUser.city,
                    school = finalUser.school,
                    xp = finalUser.xp,
                    badges = finalUser.badges
                )
            }
            return finalUser
        }
        firestore.collection("users").document(current.uid).set(finalUser).await()
        cachedCurrentUser = finalUser
        return finalUser
    }

    suspend fun registerStudent(student: StudentProfile): StudentProfile {
        val finalStudent = student.copy(
            id = student.id.ifBlank { "student-${System.currentTimeMillis()}" },
            uid = student.uid.ifBlank { "student-${System.currentTimeMillis()}" }
        )
        if (isDemoMode) {
            ensureDemoData()
            demoStudents += finalStudent
            return finalStudent
        }
        val docRef = firestore.collection("students").document(finalStudent.id)
        docRef.set(finalStudent).await()
        return finalStudent
    }

    private fun canCurrentUserCreateGroup(): Boolean {
        val role = getCurrentUser()?.role.orEmpty()
        return role in listOf("admin", "coordinator", "principal")
    }

    private fun requireAdminAccess() {
        val role = getCurrentUser()?.role.orEmpty()
        if (role !in listOf("admin", "coordinator")) {
            throw IllegalStateException("Bu işlem için yönetici yetkisi gerekir.")
        }
    }

    private fun List<Group>.visibleFor(user: User?, canSeeAll: Boolean): List<Group> {
        if (canSeeAll) return this
        val userId = user?.uid.orEmpty()
        if (userId.isBlank()) return emptyList()
        return filter { group ->
            group.members.contains(userId) || group.olusturanId == userId
        }
    }

    private fun normalizeCityName(city: String): String {
        return city.trim().replace(Regex("\\s*\\((Asya|Avrupa)\\)$", RegexOption.IGNORE_CASE), "")
    }

    fun logout() {
        cachedCurrentUser = null
        if (isDemoMode) {
            demoCurrentUser = null
            return
        }
        auth.signOut()
    }

    private fun Event.withDocumentId(documentId: String): Event {
        return if (id.isBlank()) copy(id = documentId) else this
    }

    private fun User.withDocumentId(documentId: String): User {
        return if (uid.isBlank()) copy(uid = documentId) else this
    }

    private fun Project.withDocumentId(documentId: String): Project {
        return if (id.isBlank()) copy(id = documentId) else this
    }

    private fun Group.withDocumentId(documentId: String): Group {
        return if (id.isBlank()) copy(id = documentId) else this
    }

    private fun GroupAnnouncement.withDocumentId(documentId: String): GroupAnnouncement {
        return if (id.isBlank()) copy(id = documentId) else this
    }

    private fun GroupTask.withDocumentId(documentId: String): GroupTask {
        return if (id.isBlank()) copy(id = documentId) else this
    }

    private fun DirectMessage.withDocumentId(documentId: String): DirectMessage {
        return if (id.isBlank()) copy(id = documentId) else this
    }

    private fun GroupMessage.withDocumentId(documentId: String): GroupMessage {
        return if (id.isBlank()) copy(id = documentId) else this
    }

    private fun EventApplication.withDocumentId(documentId: String): EventApplication {
        return if (id.isBlank()) copy(id = documentId) else this
    }

    private fun StudentProfile.withDocumentId(documentId: String): StudentProfile {
        return if (id.isBlank()) copy(id = documentId, uid = uid.ifBlank { documentId }) else this
    }

    private fun AtlasNotification.withDocumentId(documentId: String): AtlasNotification {
        return if (id.isBlank()) copy(id = documentId) else this
    }

    private fun AppAnnouncement.withDocumentId(documentId: String): AppAnnouncement {
        return if (id.isBlank()) copy(id = documentId) else this
    }
}
