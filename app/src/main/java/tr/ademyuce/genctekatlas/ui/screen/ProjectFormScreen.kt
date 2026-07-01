package tr.ademyuce.genctekatlas.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tr.ademyuce.genctekatlas.data.model.Project
import tr.ademyuce.genctekatlas.data.repository.FirebaseRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFormScreen(
    repository: FirebaseRepository,
    onNavigateBack: () -> Unit
) {
    var ad by remember { mutableStateOf("") }
    var tema by remember { mutableStateOf("") }
    var parkur by remember { mutableStateOf("") }
    var takimAdi by remember { mutableStateOf("") }
    var katilimciIllerStr by remember { mutableStateOf("") }
    var aciklama by remember { mutableStateOf("") }
    var githubLink by remember { mutableStateOf("") }
    var demoLink by remember { mutableStateOf("") }
    var etikKontrol by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val isVibeCoding = tema.trim().lowercase() == "vibe-coding"
    val isGithubValid = githubLink.isEmpty() || githubLink.trim().startsWith("https://github.com/")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yeni Proje") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                MobileScreenHeader(
                    title = "Proje Başvurusu",
                    subtitle = "Proje bilgileri onay sürecine gönderilir."
                )
            }

            item {
                MobileCard {
                    Text("Proje Bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ProjectTextField(value = ad, onValueChange = { ad = it }, label = "Proje Adı *", icon = Icons.Default.Code)
                    ProjectTextField(value = tema, onValueChange = { tema = it }, label = "Tema * (vibe-coding, iot vb.)", icon = Icons.Default.Label)
                    ProjectTextField(value = parkur, onValueChange = { parkur = it }, label = "Kategori / Parkur", icon = Icons.Default.Code)
                    ProjectTextField(value = takimAdi, onValueChange = { takimAdi = it }, label = "Takım Adı *", icon = Icons.Default.Groups)
                    ProjectTextField(value = katilimciIllerStr, onValueChange = { katilimciIllerStr = it }, label = "Katılımcı İller * (Virgülle ayırın)", icon = Icons.Default.LocationOn)
                }
            }

            item {
                MobileCard {
                    Text("Açıklama ve Bağlantılar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ProjectTextField(
                        value = aciklama,
                        onValueChange = { aciklama = it },
                        label = "Proje Açıklaması *",
                        icon = Icons.Default.Description,
                        maxLines = 4
                    )
                    ProjectTextField(
                        value = githubLink,
                        onValueChange = { githubLink = it },
                        label = "GitHub Bağlantısı *",
                        icon = Icons.Default.Link,
                        isError = !isGithubValid
                    )
                    if (!isGithubValid) {
                        Text(
                            text = "GitHub bağlantısı 'https://github.com/' ile başlamalıdır.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    ProjectTextField(value = demoLink, onValueChange = { demoLink = it }, label = "Demo Canlı Bağlantısı", icon = Icons.Default.Link)
                }
            }

            item {
                val cardBorderColor = if (isVibeCoding && !etikKontrol) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorderColor, MobileCardShape),
                    shape = MobileCardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(checked = etikKontrol, onCheckedChange = { etikKontrol = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Etik ve AI Kullanım Onayı" + if (isVibeCoding) " *" else "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVibeCoding && !etikKontrol) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Yapay zeka çıktılarının doğruluğunu kontrol ettiğimi, kodları test ettiğimi ve promptları arşivlediğimi beyan ederim.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
                successMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.primary)
                }

                if (isSubmitting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    FullWidthButton(
                        text = "Proje Başvurusunu Tamamla",
                        icon = Icons.Default.CheckCircle,
                        onClick = {
                            if (ad.isBlank() || tema.isBlank() || takimAdi.isBlank() || katilimciIllerStr.isBlank() || aciklama.isBlank() || githubLink.isBlank()) {
                                errorMessage = "Lütfen zorunlu (*) alanları doldurun."
                                return@FullWidthButton
                            }
                            if (!githubLink.trim().startsWith("https://github.com/")) {
                                errorMessage = "GitHub bağlantısı 'https://github.com/' ile başlamalıdır."
                                return@FullWidthButton
                            }
                            if (isVibeCoding && !etikKontrol) {
                                errorMessage = "Vibe Coding teması için Etik Onay beyanı zorunludur."
                                return@FullWidthButton
                            }

                            coroutineScope.launch {
                                isSubmitting = true
                                errorMessage = null
                                successMessage = null
                                try {
                                    val iller = katilimciIllerStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    val newProject = Project(
                                        ad = ad,
                                        tema = tema,
                                        parkur = parkur,
                                        takimAdi = takimAdi,
                                        katilimciIller = iller,
                                        aciklama = aciklama,
                                        githubLink = githubLink,
                                        demoLink = demoLink,
                                        etikKontrol = etikKontrol
                                    )
                                    repository.addProject(newProject)
                                    successMessage = "Projeniz başarıyla onay sürecine gönderildi."
                                    ad = ""
                                    aciklama = ""
                                    githubLink = ""
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Kayıt gönderilirken hata oluştu."
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun ProjectTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    maxLines: Int = 1,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        maxLines = maxLines,
        singleLine = maxLines == 1,
        isError = isError,
        shape = MobileCardShape
    )
}
