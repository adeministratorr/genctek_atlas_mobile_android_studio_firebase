package tr.ademyuce.genctekatlas.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
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
import tr.ademyuce.genctekatlas.data.model.Event
import tr.ademyuce.genctekatlas.data.repository.FirebaseRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    repository: FirebaseRepository,
    onNavigateBack: () -> Unit
) {
    var ad by remember { mutableStateOf("") }
    var kapsam by remember { mutableStateOf("il") }
    var durum by remember { mutableStateOf("gerceklesti") }
    var tema by remember { mutableStateOf("") }
    var il by remember { mutableStateOf("") }
    var ilce by remember { mutableStateOf("") }
    var tarih by remember { mutableStateOf("") }
    var aciklama by remember { mutableStateOf("") }
    var baglanti by remember { mutableStateOf("") }
    var gorselUrl by remember { mutableStateOf("") }
    var galeriMetni by remember { mutableStateOf("") }
    var ilKisitlama by remember { mutableStateOf(false) }
    var ilceKisitlama by remember { mutableStateOf(false) }
    var ogrenciSiniri by remember { mutableStateOf("") }
    var katilimciSayisi by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yeni Etkinlik") },
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
                Spacer(modifier = Modifier.height(4.dp))
                MobileScreenHeader(
                    title = "Etkinlik Kaydı",
                    subtitle = "Kayıtlar onay sürecine gönderilir."
                )
            }

            item {
                MobileCard {
                    Text("Temel Bilgiler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FormTextField(value = ad, onValueChange = { ad = it }, label = "Etkinlik Adı *", icon = Icons.Default.Info)
                    FormTextField(value = tema, onValueChange = { tema = it }, label = "Tema *", icon = Icons.Default.Label)
                    FormTextField(value = kapsam, onValueChange = { kapsam = it }, label = "Kapsam * (il, ilce, okul, turkiye)", icon = Icons.Default.Info)
                    FormTextField(value = durum, onValueChange = { durum = it }, label = "Durum * (gerceklesti, duyuru)", icon = Icons.Default.CheckCircle)
                }
            }

            item {
                MobileCard {
                    Text("Konum ve Tarih", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FormTextField(value = il, onValueChange = { il = it }, label = "İl *", icon = Icons.Default.LocationOn)
                    FormTextField(value = ilce, onValueChange = { ilce = it }, label = "İlçe", icon = Icons.Default.LocationOn)
                    FormTextField(value = tarih, onValueChange = { tarih = it }, label = "Tarih * (YYYY-MM-DD)", icon = Icons.Default.CalendarToday)
                }
            }

            item {
                MobileCard {
                    Text("Açıklama", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FormTextField(
                        value = aciklama,
                        onValueChange = { aciklama = it },
                        label = "Özet Açıklama *",
                        icon = Icons.Default.Description,
                        maxLines = 4
                    )
                    FormTextField(value = baglanti, onValueChange = { baglanti = it }, label = "Etkinlik Bağlantısı", icon = Icons.Default.Link)
                    FormTextField(value = gorselUrl, onValueChange = { gorselUrl = it }, label = "Kapak Fotoğrafı URL", icon = Icons.Default.Link)
                    FormTextField(
                        value = galeriMetni,
                        onValueChange = { galeriMetni = it },
                        label = "Galeri URL'leri (virgül veya satırla ayırın)",
                        icon = Icons.Default.Link,
                        maxLines = 4
                    )
                }
            }

            if (durum == "duyuru") {
                item {
                    MobileCard {
                        Text("Başvuru Sınırlandırmaları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FormTextField(value = ogrenciSiniri, onValueChange = { ogrenciSiniri = it }, label = "Öğrenci Kontenjan Sınırı", icon = Icons.Default.People)
                        ToggleRow(text = "Sadece bu il ile kısıtla", checked = ilKisitlama, onCheckedChange = { ilKisitlama = it })
                        ToggleRow(text = "Sadece bu ilçe ile kısıtla", checked = ilceKisitlama, onCheckedChange = { ilceKisitlama = it })
                    }
                }
            } else if (durum == "gerceklesti") {
                item {
                    MobileCard {
                        Text("Katılım", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FormTextField(value = katilimciSayisi, onValueChange = { katilimciSayisi = it }, label = "Katılımcı Sayısı *", icon = Icons.Default.People)
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
                        text = "Etkinlik Kaydını Gönder",
                        icon = Icons.Default.CheckCircle,
                        onClick = {
                            if (ad.isBlank() || tema.isBlank() || il.isBlank() || tarih.isBlank() || aciklama.isBlank()) {
                                errorMessage = "Lütfen zorunlu (*) alanları doldurun."
                                return@FullWidthButton
                            }
                            coroutineScope.launch {
                                isSubmitting = true
                                errorMessage = null
                                successMessage = null
                                try {
                                    val limit = ogrenciSiniri.toIntOrNull()
                                    val count = katilimciSayisi.toIntOrNull() ?: 0
                                    val newEvent = Event(
                                        ad = ad,
                                        kapsam = kapsam,
                                        durum = durum,
                                        tema = tema,
                                        il = il,
                                        ilce = ilce,
                                        tarih = tarih,
                                        aciklama = aciklama,
                                        baglanti = baglanti,
                                        gorselUrl = gorselUrl.ifBlank { null },
                                        galeri = galeriMetni
                                            .split(",", "\n")
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }
                                            .distinct(),
                                        ogrenciSiniri = limit,
                                        ilKisitlama = ilKisitlama,
                                        ilceKisitlama = ilceKisitlama,
                                        katilimciSayisi = count
                                    )
                                    repository.addEvent(newEvent)
                                    successMessage = "Etkinlik başarıyla onay sürecine gönderildi."
                                    ad = ""
                                    aciklama = ""
                                    gorselUrl = ""
                                    galeriMetni = ""
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Kayıt gönderilirken hata oluştu."
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        maxLines = maxLines,
        singleLine = maxLines == 1,
        shape = MobileCardShape
    )
}

@Composable
private fun ToggleRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
