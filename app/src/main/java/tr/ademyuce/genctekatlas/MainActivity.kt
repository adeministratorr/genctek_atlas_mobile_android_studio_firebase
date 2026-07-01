package tr.ademyuce.genctekatlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import tr.ademyuce.genctekatlas.data.model.User
import tr.ademyuce.genctekatlas.data.repository.FirebaseRepository
import tr.ademyuce.genctekatlas.ui.screen.*
import tr.ademyuce.genctekatlas.ui.theme.AtlasTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: FirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AtlasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(repository)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(repository: FirebaseRepository) {
    val navController = rememberNavController()
    var currentUser by remember { mutableStateOf<User?>(repository.getCurrentUser()) }

    // Track active navigation route for BottomBar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Hide bottom bar on login screen
            if (currentRoute != "login") {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "explore" || currentRoute == "add_event" || currentRoute == "add_project",
                        onClick = {
                            navController.navigate("explore") {
                                popUpTo("explore") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Keşfet") },
                        label = { Text("Keşfet", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "groups",
                        onClick = {
                            navController.navigate("groups") {
                                popUpTo("explore") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Groups, contentDescription = "Gruplar") },
                        label = { Text("Gruplar", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "messages",
                        onClick = {
                            navController.navigate("messages") {
                                popUpTo("explore") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Forum, contentDescription = "Mesajlar") },
                        label = { Text("Mesajlar", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "panel",
                        onClick = {
                            navController.navigate("panel") {
                                popUpTo("explore") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Panel") },
                        label = { Text("Panel", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "profile",
                        onClick = {
                            navController.navigate("profile") {
                                popUpTo("explore") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profil") },
                        label = { Text("Profil", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "explore",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("explore") {
                ExploreScreen(
                    repository = repository,
                    onAddEventClick = {
                        if (currentUser == null) {
                            navController.navigate("login")
                        } else {
                            navController.navigate("add_event")
                        }
                    },
                    onAddProjectClick = {
                        if (currentUser == null) {
                            navController.navigate("login")
                        } else {
                            navController.navigate("add_project")
                        }
                    },
                    onLoginRequired = {
                        navController.navigate("login")
                    },
                    currentUserId = currentUser?.uid.orEmpty()
                )
            }

            composable("login") {
                LoginScreen(
                    repository = repository,
                    onLoginSuccess = { user ->
                        currentUser = user
                        navController.navigate("explore") {
                            popUpTo("explore") { inclusive = false }
                        }
                    }
                )
            }

            composable("add_event") {
                EventFormScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("add_project") {
                ProjectFormScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // --- STITCH BOTTOM BAR SECTIONS ---

            composable("groups") {
                GroupsScreen(repository = repository, currentUser = currentUser)
            }

            composable("messages") {
                MessagesScreen(repository = repository)
            }

            composable("panel") {
                DashboardScreen(repository = repository, currentUser = currentUser)
            }

            composable("profile") {
                ProfileScreen(
                    repository = repository,
                    currentUser = currentUser,
                    onProfileUpdated = { currentUser = it },
                    onLogout = {
                        repository.logout()
                        currentUser = null
                        navController.navigate("explore") {
                            popUpTo("explore") { inclusive = true }
                        }
                    },
                    onLoginRequested = {
                        navController.navigate("login")
                    }
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: FirebaseRepository,
    currentUser: User?,
    onProfileUpdated: (User) -> Unit,
    onLogout: () -> Unit,
    onLoginRequested: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showEditSheet by remember { mutableStateOf(false) }
    var editName by remember(currentUser?.uid) { mutableStateOf(currentUser?.name.orEmpty()) }
    var editCity by remember(currentUser?.uid) { mutableStateOf(currentUser?.city.orEmpty()) }
    var editSchool by remember(currentUser?.uid) { mutableStateOf(currentUser?.school.orEmpty()) }
    var editRole by remember(currentUser?.uid) { mutableStateOf(currentUser?.role.orEmpty()) }
    var profileError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            MobileScreenHeader(
                title = "Profil",
                subtitle = if (currentUser != null) "Hesap ve katılım bilgileri" else "Giriş yaparak profil bilgilerinizi görüntüleyin"
            )
        }

        if (currentUser != null) {
            item {
                MobileCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.name.ifBlank { "Kullanıcı" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    InfoRow(label = "Şehir", value = currentUser.city, icon = Icons.Default.LocationOn)
                    InfoRow(label = "Okul", value = currentUser.school, icon = Icons.Default.School)
                    InfoRow(label = "Rol", value = currentUser.role, icon = Icons.Default.VerifiedUser)
                    InfoRow(label = "Gelişim Puanı", value = currentUser.xp.toString(), icon = Icons.Default.Star)
                }
            }
            item {
                FullWidthOutlinedButton(
                    text = "Profili Düzenle",
                    icon = Icons.Default.Edit,
                    onClick = {
                        editName = currentUser.name
                        editCity = currentUser.city
                        editSchool = currentUser.school
                        editRole = currentUser.role
                        profileError = null
                        showEditSheet = true
                    }
                )
            }
            item {
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MobileCardShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Çıkış Yap")
                }
            }
        } else {
            item {
                EmptyState(
                    title = "Giriş yapılmadı",
                    description = "Profil, etkinlik ve proje işlemleri için giriş yapmanız gerekir.",
                    icon = Icons.Default.AccountCircle
                )
            }
            item {
                FullWidthButton(
                    text = "Giriş Yap",
                    icon = Icons.Default.Login,
                    onClick = onLoginRequested
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showEditSheet && currentUser != null) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            shape = MobileSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MobileScreenHeader("Profil Düzenle", currentUser.email)
                OutlinedTextField(editName, { editName = it }, label = { Text("Ad Soyad") }, modifier = Modifier.fillMaxWidth(), shape = MobileCardShape)
                OutlinedTextField(editCity, { editCity = it }, label = { Text("Şehir") }, modifier = Modifier.fillMaxWidth(), shape = MobileCardShape)
                OutlinedTextField(editSchool, { editSchool = it }, label = { Text("Okul") }, modifier = Modifier.fillMaxWidth(), shape = MobileCardShape)
                OutlinedTextField(editRole, { editRole = it }, label = { Text("Rol") }, modifier = Modifier.fillMaxWidth(), shape = MobileCardShape)
                profileError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                FullWidthButton(
                    text = "Kaydet",
                    icon = Icons.Default.CheckCircle,
                    onClick = {
                        if (editName.isBlank()) {
                            profileError = "Ad soyad boş bırakılamaz."
                            return@FullWidthButton
                        }
                        coroutineScope.launch {
                            try {
                                val updated = repository.updateCurrentUserProfile(
                                    currentUser.copy(
                                        name = editName.trim(),
                                        city = editCity.trim(),
                                        school = editSchool.trim(),
                                        role = editRole.trim().ifBlank { currentUser.role }
                                    )
                                )
                                onProfileUpdated(updated)
                                showEditSheet = false
                            } catch (e: Exception) {
                                profileError = e.message ?: "Profil güncellenemedi."
                            }
                        }
                    }
                )
            }
        }
    }
}
