package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.VtuberCharacterEntity
import com.example.tracking.ExpressionPreset
import com.example.tracking.Live2DPoseState
import com.example.ui.components.Live2DAvatarCanvas
import com.example.ui.theme.ElectricMagenta
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoyalViolet

@Composable
fun StudioHomeScreen(
    characters: List<VtuberCharacterEntity>,
    activeCharacter: VtuberCharacterEntity,
    poseState: Live2DPoseState,
    onSelectCharacter: (Long) -> Unit,
    onUpdateCharacter: (VtuberCharacterEntity) -> Unit,
    onImportPhotoCharacter: (Uri, String, String) -> Unit,
    onCreatePresetCharacter: (String, String, Int, Long, Long, Long, Int) -> Unit,
    onDeleteCharacter: (VtuberCharacterEntity) -> Unit,
    onPuppetDrag: (Float, Float) -> Unit,
    onCharacterTapped: () -> Unit,
    onSelectExpressionPreset: (ExpressionPreset) -> Unit,
    onOpenRiggingEditor: () -> Unit,
    onOpenLiveCamera: () -> Unit,
    onOpenTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingImportedUri by remember { mutableStateOf<Uri?>(null) }
    var showCreatePresetDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImportedUri = uri
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Interactive Hero Stage Card with Active VTuber Avatar
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(listOf(NeonCyan, RoyalViolet, ElectricMagenta)),
                        shape = RoundedCornerShape(26.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = NeonCyan.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (activeCharacter.imageUri != null) "FOTO KUSTOM LIVE2D" else "MODEL LIVE2D AKTIF",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NeonCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Geser karakter untuk menoleh",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeCharacter.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activeCharacter.tagline,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenTutorial,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("studio_quick_tutorial_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Buka Tutorial",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tutorial")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Live2D Preview Stage
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(295.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(22.dp)
                            )
                            .testTag("studio_avatar_stage")
                    ) {
                        Live2DAvatarCanvas(
                            character = activeCharacter,
                            pose = poseState,
                            showRiggingMesh = false,
                            enableTouchPuppeteer = true,
                            onPuppetDrag = onPuppetDrag,
                            onCharacterTapped = onCharacterTapped,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Floating telemetry badge
                        Surface(
                            color = Color(0xAA0A0B12),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "YAW ${(poseState.headYaw * 30).toInt()}° • PITCH ${(poseState.headPitch * 25).toInt()}° • ${poseState.expressionPreset.emoji} ${poseState.expressionPreset.labelId}",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Expression Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ExpressionPreset.entries) { preset ->
                            val selected = poseState.expressionPreset == preset
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectExpressionPreset(preset) },
                                label = { Text("${preset.emoji} ${preset.labelId}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricMagenta.copy(alpha = 0.24f),
                                    selectedLabelColor = ElectricMagenta
                                ),
                                modifier = Modifier.testTag("expr_chip_${preset.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Action Buttons: Import Photo, Edit Rigging, Live Camera
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color(0xFF002026)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("import_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Impor Foto",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FilledTonalButton(
                            onClick = onOpenRiggingEditor,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("go_to_rigging_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit Live2D",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = onOpenLiveCamera,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldLive,
                                contentColor = Color(0xFF00210E)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("go_to_live_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraFront,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mulai Live",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 2. Character Roster & Create New Avatar Section
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Koleksi Karakter VTuber (${characters.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Pilih karakter atau tambahkan foto karakter Anda sendiri",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { showCreatePresetDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("create_preset_character_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buat Baru")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(characters, key = { it.id }) { character ->
                    val isSelected = character.id == activeCharacter.id
                    CharacterRosterCard(
                        character = character,
                        isSelected = isSelected,
                        canDelete = characters.size > 1,
                        onSelect = { onSelectCharacter(character.id) },
                        onDelete = { onDeleteCharacter(character) }
                    )
                }
            }
        }

        // 3. Instant Appearance, Colors & Accessory Customizer Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = ElectricMagenta
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Kustomisasi Cepat Gaya & Aksesoris",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ubah warna rambut, mata, aksesoris, dan panggung latar secara real-time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Warna Rambut / Aura Bingkai",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val hairPalette = listOf(
                        0xFF90E0FFL to "Cyan Starlight",
                        0xFFFF80ABL to "Sakura Pink",
                        0xFF282C44L to "Obsidian Night",
                        0xFFB388FFL to "Royal Lavender",
                        0xFFFFD54FL to "Golden Blonde",
                        0xFF69F0AEL to "Cyber Mint"
                    )
                    ColorSwatchRow(
                        palette = hairPalette,
                        selectedHex = activeCharacter.hairColorHex,
                        onSelectColor = { hex ->
                            onUpdateCharacter(activeCharacter.copy(hairColorHex = hex))
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Warna Iris Mata Live2D",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val eyePalette = listOf(
                        0xFFFF2E93L to "Ruby Magenta",
                        0xFF00E5FFL to "Cyber Cyan",
                        0xFFFFD54FL to "Amber Gold",
                        0xFF00E676L to "Emerald Jade",
                        0xFF7C4DFFL to "Amethyst Violet"
                    )
                    ColorSwatchRow(
                        palette = eyePalette,
                        selectedHex = activeCharacter.eyeColorHex,
                        onSelectColor = { hex ->
                            onUpdateCharacter(activeCharacter.copy(eyeColorHex = hex))
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Aksesoris Kepala VTuber",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val accessories = listOf(
                        0 to "Tanpa Aksesoris",
                        1 to "🐱 Telinga Kucing Cyber",
                        2 to "😇 Halo Bintang",
                        3 to "🕶️ Visor Hologram",
                        4 to "😈 Tanduk Neon"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(accessories) { (accId, label) ->
                            FilterChip(
                                selected = activeCharacter.accessoryId == accId,
                                onClick = {
                                    onUpdateCharacter(activeCharacter.copy(accessoryId = accId))
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Latar Panggung & Chroma Key (Untuk Rekaman/Live)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val stages = listOf(
                        0 to "🌌 Cyber Grid",
                        1 to "🟩 Green Screen (OBS)",
                        2 to "🟦 Blue Screen",
                        3 to "🔮 Nebula Stage",
                        4 to "🌸 Sakura Night"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(stages) { (stageId, label) ->
                            FilterChip(
                                selected = activeCharacter.bgPresetId == stageId,
                                onClick = {
                                    onUpdateCharacter(activeCharacter.copy(bgPresetId = stageId))
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalViolet.copy(alpha = 0.25f),
                                    selectedLabelColor = Color(0xFFD6C9FF)
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialog after user picks a photo from Android Photo Picker
    if (pendingImportedUri != null) {
        var customName by remember { mutableStateOf("VTuber Fotoku") }
        var customTagline by remember { mutableStateOf("Karakter Kustom Live2D") }

        AlertDialog(
            onDismissRequest = { pendingImportedUri = null },
            title = {
                Text("Beri Nama Karakter Foto Anda")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Foto berhasil dipilih! Setelah disimpan, Anda dapat langsung menggerakkan karakter atau mengatur titik mata & mulut di menu Rig Live2D.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Nama Karakter VTuber") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customTagline,
                        onValueChange = { customTagline = it },
                        label = { Text("Deskripsi / Slogan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportedUri
                        if (uri != null) {
                            onImportPhotoCharacter(uri, customName, customTagline)
                        }
                        pendingImportedUri = null
                    }
                ) {
                    Text("Simpan & Pasang Rigging")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportedUri = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog to create a new procedural Live2D character
    if (showCreatePresetDialog) {
        var newName by remember { mutableStateOf("Aria Nova") }
        var newTagline by remember { mutableStateOf("Virtual Idol Live2D") }
        var selectedHair by remember { mutableLongStateOf(0xFF90E0FFL) }
        var selectedEye by remember { mutableLongStateOf(0xFFFF2E93L) }
        var selectedAcc by remember { mutableIntStateOf(1) }

        AlertDialog(
            onDismissRequest = { showCreatePresetDialog = false },
            title = { Text("Buat Karakter Live2D Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nama Karakter") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTagline,
                        onValueChange = { newTagline = it },
                        label = { Text("Slogan / Persona") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Pilih Warna Rambut Utama:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    ColorSwatchRow(
                        palette = listOf(
                            0xFF90E0FFL to "Cyan",
                            0xFFFF80ABL to "Pink",
                            0xFFB388FFL to "Ungu",
                            0xFFFFD54FL to "Emas"
                        ),
                        selectedHex = selectedHair,
                        onSelectColor = { selectedHair = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePresetCharacter(
                            newName,
                            newTagline,
                            0,
                            selectedHair,
                            selectedEye,
                            0xFF7C4DFFL,
                            selectedAcc
                        )
                        showCreatePresetDialog = false
                    }
                ) {
                    Text("Buat Karakter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePresetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun CharacterRosterCard(
    character: VtuberCharacterEntity,
    isSelected: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .width(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("character_card_${character.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(character.hairColorHex), Color(character.eyeColorHex))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (character.imageUri != null) "Foto Impor" else "Model Live2D",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus karakter",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = character.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = character.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ColorSwatchRow(
    palette: List<Pair<Long, String>>,
    selectedHex: Long,
    onSelectColor: (Long) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(palette) { (hex, label) ->
            val selected = selectedHex == hex
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) NeonCyan else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { onSelectColor(hex) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(hex)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
