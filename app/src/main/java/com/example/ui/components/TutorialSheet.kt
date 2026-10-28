package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ElectricMagenta
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoyalViolet

data class TutorialGuideSection(
    val id: String,
    val stepNumber: String,
    val category: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val detailedSteps: List<String>,
    val proTip: String,
    val targetTabIndex: Int
)

val LUMINA_TUTORIAL_SECTIONS = listOf(
    TutorialGuideSection(
        id = "import_photo",
        stepNumber = "01",
        category = "Dasar",
        title = "Impor Foto Karakter & Pembuatan Avatar",
        subtitle = "Cara memasukkan gambar karakter sendiri dari galeri HP dan mengubahnya menjadi VTuber.",
        icon = Icons.Default.PhotoLibrary,
        accentColor = NeonCyan,
        detailedSteps = listOf(
            "Buka tab 'Studio' di pojok kiri bawah aplikasi.",
            "Tekan tombol 'Impor Foto Karakter' untuk memilih ilustrasi anime atau foto karakter dari galeri perangkat Anda (tanpa perlu izin penyimpanan penuh).",
            "Beri nama karakter dan slogan VTuber Anda, atau pilih salah satu dari 3 karakter preset bawaan (Celestia, Kuro Neko-X, atau Luna Sakura).",
            "Anda juga bisa mengganti warna rambut, warna mata, warna kostum, dan aksesoris (Telinga Kucing Cyber, Halo Bintang, Visor Hologram, atau Tanduk Neon) secara instan."
        ),
        proTip = "Gunakan gambar karakter yang menghadap ke depan (front-facing) agar efek kemiringan 2.5D dan penempatan titik mata/mulut terlihat paling natural seperti Live2D Cubism.",
        targetTabIndex = 0
    ),
    TutorialGuideSection(
        id = "live2d_mesh",
        stepNumber = "02",
        category = "Rigging Live2D",
        title = "Cara Pasang Titik Wajah (Mesh Rigging)",
        subtitle = "Menyesuaikan posisi Mata Kiri, Mata Kanan, Mulut, dan Poros Leher di atas foto karakter Anda.",
        icon = Icons.Default.GridOn,
        accentColor = ElectricMagenta,
        detailedSteps = listOf(
            "Masuk ke tab 'Rig Live2D' untuk membuka kanvas editor interaktif.",
            "Aktifkan tombol 'Tampilkan Jaring Mesh Live2D' di bagian atas kanvas.",
            "Anda akan melihat 4 titik node bercahaya: Mata Kiri (Cyan), Mata Kanan (Cyan), Mulut & LipSync (Magenta), dan Poros Leher/Kepala (Hijau).",
            "Sentuh dan geser langsung (drag) titik-titik tersebut di layar agar tepat berada di atas mata, mulut, dan leher pada foto karakter Anda.",
            "Tekan 'Simpan Rigging' untuk menyimpan posisi titik secara permanen."
        ),
        proTip = "Titik 'Poros Leher/Kepala' menentukan pusat putaran saat kepala miring ke kiri atau kanan. Letakkan tepat di bagian tengah dagu atau leher karakter.",
        targetTabIndex = 1
    ),
    TutorialGuideSection(
        id = "physics_sliders",
        stepNumber = "03",
        category = "Rigging Live2D",
        title = "Mengatur Fisika Ayunan Rambut & Pernapasan",
        subtitle = "Membuat rambut dan tubuh karakter bergerak halus secara otomatis seperti model Live2D profesional.",
        icon = Icons.Default.Tune,
        accentColor = RoyalViolet,
        detailedSteps = listOf(
            "Di tab 'Rig Live2D', pilih sub-panel 'Fisika & Parameter'.",
            "Atur slider 'Ayunan Rambut (Hair Sway)' untuk menentukan seberapa lentur rambut mengayun saat kepala menoleh.",
            "Atur 'Pegas Fisika (Spring)' dan 'Kecepatan Napas (Breathing)' agar tubuh karakter naik-turun lembut layaknya bernapas.",
            "Sesuaikan 'Sensitivitas Kedipan Mata' dan 'Gain Lip-Sync Mulut' agar gerakan mulut pas dengan kepekaan suara Anda."
        ),
        proTip = "Nilai Ayunan Rambut 75%–85% dan Kecepatan Napas 1.0x memberikan gerakan paling elegan tanpa terlihat berlebihan saat live.",
        targetTabIndex = 1
    ),
    TutorialGuideSection(
        id = "touch_joystick",
        stepNumber = "04",
        category = "Kontrol Manual",
        title = "Menggerakkan Karakter dengan Sentuhan & Preset Emosi",
        subtitle = "Gerakkan karakter dengan jari kapan saja dan ganti ekspresi wajah dengan sekali sentuh.",
        icon = Icons.Default.TouchApp,
        accentColor = NeonCyan,
        detailedSteps = listOf(
            "Saat jaring mesh dimatikan, sentuh dan geser jari Anda langsung di atas kanvas karakter untuk menolehkan kepala ke segala arah (360° 2.5D).",
            "Gunakan baris tombol 'Preset Ekspresi Cepat' (Netral ✨, Senang 😊, Kedip 😉, Kaget 😲, Kesal 💢, Malu 😳, Idol 🤩) untuk mengubah bentuk mata, alis, dan rona pipi seketika.",
            "Di sub-panel 'Uji Ekspresi Manual', Anda juga dapat menggeser slider Yaw, Pitch, Roll, Bukaan Mulut, dan Senyuman secara presisi."
        ),
        proTip = "Ketuk satu kali pada karakter di kanvas untuk memicu efek sapaan ekspresi instan!",
        targetTabIndex = 1
    ),
    TutorialGuideSection(
        id = "live_camera_tracking",
        stepNumber = "05",
        category = "Live Kamera",
        title = "Sinkronisasi Wajah Real-Time dengan Kamera Depan",
        subtitle = "Mendeteksi wajah, kemiringan kepala, kedipan mata, dan gerakan mulut secara langsung tanpa aplikasi tambahan.",
        icon = Icons.Default.CameraFront,
        accentColor = EmeraldLive,
        detailedSteps = listOf(
            "Buka tab 'Live Kamera' lalu tekan tombol 'Aktifkan Kamera Wajah'. Berikan izin Kamera ketika diminta oleh sistem Android.",
            "Posisikan wajah Anda di depan kamera depan dengan pencahayaan yang cukup. Indikator di pojok atas akan berubah menjadi 'WAJAH TERDETEKSI (LIVE)' lengkap dengan pratinjau kamera kecil (PiP).",
            "Tekan tombol 'Kalibrasi Posisi Netral' sambil menatap lurus ke layar agar posisi kepala Anda saat ini dijadikan titik tengah (0°).",
            "Sekarang setiap kali Anda menoleh, mengangguk, memiringkan kepala, berkedip, atau membuka mulut, avatar VTuber Anda akan bergerak sinkron secara real-time!"
        ),
        proTip = "Jika Anda sedang berada di ruangan gelap atau tidak ingin menyalakan kamera, Anda tetap bisa menggerakkan avatar menggunakan Joystick Layar & tombol ekspresi di bawah layar Live.",
        targetTabIndex = 2
    ),
    TutorialGuideSection(
        id = "mic_and_streaming",
        stepNumber = "06",
        category = "Siaran Live",
        title = "Lip-Sync Suara Mikrofon & Layar Hijau (Chroma Key)",
        subtitle = "Siap dipakai untuk rekaman konten atau live streaming TikTok, YouTube, dan OBS.",
        icon = Icons.Default.Mic,
        accentColor = ElectricMagenta,
        detailedSteps = listOf(
            "Di layar 'Live Kamera', aktifkan tombol 'Lip-Sync Mikrofon' agar mulut karakter otomatis terbuka mengikuti volume dan intonasi suara Anda saat berbicara.",
            "Pilih latar panggung di tombol 'Latar Stage': tersedia 'Cyber Grid', 'Green Screen (Chroma)', 'Blue Screen', 'Nebula', dan 'Sakura Night'.",
            "Tekan tombol 'Mode Layar Penuh (Sembunyikan UI)' di pojok kanan atas untuk menyembunyikan seluruh tombol dan menu, menyisakan avatar bersih untuk direkam layar (Screen Recorder) atau ditangkap ke OBS/aplikasi live."
        ),
        proTip = "Saat berada di Mode Layar Penuh (Clean Feed), cukup ketuk layar di mana saja untuk memunculkan kembali kontrol studio.",
        targetTabIndex = 2
    )
)

@Composable
fun TutorialGuideScreen(
    onJumpToTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }

    val categories = listOf("Semua", "Dasar", "Rigging Live2D", "Kontrol Manual", "Live Kamera", "Siaran Live")
    val filteredSections = remember(searchQuery, selectedCategory) {
        LUMINA_TUTORIAL_SECTIONS.filter { section ->
            val matchesCategory = selectedCategory == "Semua" || section.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                section.title.contains(searchQuery, ignoreCase = true) ||
                section.subtitle.contains(searchQuery, ignoreCase = true) ||
                section.detailedSteps.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.6f), ElectricMagenta.copy(alpha = 0.6f))),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Ikon Panduan",
                                tint = NeonCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pusat Tutorial Lengkap Lumina Live2D",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Semua panduan teks langkah demi langkah agar mudah dipahami pemula hingga pro.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari cara impor foto, titik mata, kamera live...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari Tutorial"
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus pencarian")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tutorial_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { category ->
                            val selected = selectedCategory == category
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                    }
                }
            }
        }

        items(filteredSections, key = { it.id }) { section ->
            TutorialSectionCard(
                section = section,
                onTryFeatureClick = { onJumpToTab(section.targetTabIndex) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TutorialSectionCard(
    section: TutorialGuideSection,
    onTryFeatureClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = section.accentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = section.accentColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = section.title,
                                tint = section.accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LANGKAH ${section.stepNumber} • ${section.category.uppercase()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = section.accentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = section.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Detailed step-by-step instructions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                section.detailedSteps.forEachIndexed { idx, stepText ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(section.accentColor.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = section.accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stepText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pro Tip box
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(section.accentColor.copy(alpha = 0.10f))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Tips Penting",
                    tint = section.accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Tips: ${section.proTip}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onTryFeatureClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = section.accentColor.copy(alpha = 0.2f),
                    contentColor = section.accentColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("tutorial_try_${section.id}")
            ) {
                Text("Praktikkan Sekarang")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Quick Tutorial Modal Dialog that appears from ANY screen whenever the user taps
 * the "Tutorial / Bantuan" button in the top bar.
 */
@Composable
fun QuickTutorialOverlayDialog(
    onDismiss: () -> Unit,
    onOpenFullTutorialTab: (Int) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 24.dp)
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(listOf(NeonCyan, ElectricMagenta)),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Panduan Cepat",
                            tint = NeonCyan
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Panduan Teks & Tutorial Lengkap",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Bingung cara pakai? Baca langkah mudah di bawah ini",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_tutorial_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Tutorial"
                        )
                    }
                }

                TutorialGuideScreen(
                    onJumpToTab = { tabIdx ->
                        onDismiss()
                        onOpenFullTutorialTab(tabIdx)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
