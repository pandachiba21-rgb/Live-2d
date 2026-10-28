package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.VtuberCharacterEntity
import com.example.tracking.ExpressionPreset
import com.example.tracking.Live2DPoseState
import com.example.ui.components.Live2DAvatarCanvas
import com.example.ui.components.RiggingAnchorType
import com.example.ui.theme.ElectricMagenta
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoyalViolet
import kotlin.math.roundToInt

@Composable
fun Live2DRiggingScreen(
    character: VtuberCharacterEntity,
    pose: Live2DPoseState,
    showMeshOverlay: Boolean,
    selectedAnchor: RiggingAnchorType?,
    onToggleMeshOverlay: (Boolean) -> Unit,
    onSelectAnchor: (RiggingAnchorType) -> Unit,
    onMoveAnchor: (RiggingAnchorType, Float, Float) -> Unit,
    onResetAnchors: () -> Unit,
    onUpdateCharacterParams: (VtuberCharacterEntity) -> Unit,
    onUpdateManualPose: (
        yaw: Float?,
        pitch: Float?,
        roll: Float?,
        leftEye: Float?,
        rightEye: Float?,
        mouthOpen: Float?,
        mouthForm: Float?,
        blush: Float?
    ) -> Unit,
    onSelectExpressionPreset: (ExpressionPreset) -> Unit,
    onResetNeutralPose: () -> Unit,
    onPuppetDrag: (Float, Float) -> Unit,
    onOpenTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Titik Mesh Wajah", "Fisika Live2D", "Uji Parameter Manual")

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Control Strip + Live2D Rigging Canvas
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Tampilkan Jaring Mesh & Titik Wajah",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (showMeshOverlay) {
                                        "Mode Edit Titik: Geser titik mata, mulut, & leher di kanvas"
                                    } else {
                                        "Mode Uji Gerak: Geser jari di kanvas untuk menolehkan karakter"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = showMeshOverlay,
                            onCheckedChange = onToggleMeshOverlay,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonCyan
                            ),
                            modifier = Modifier.testTag("toggle_mesh_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(310.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = 1.5.dp,
                                color = if (showMeshOverlay) NeonCyan.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .testTag("rigging_canvas_box")
                    ) {
                        Live2DAvatarCanvas(
                            character = character,
                            pose = pose,
                            showRiggingMesh = showMeshOverlay,
                            enableTouchPuppeteer = !showMeshOverlay,
                            selectedAnchor = selectedAnchor,
                            onAnchorSelected = onSelectAnchor,
                            onAnchorMoved = onMoveAnchor,
                            onPuppetDrag = onPuppetDrag,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Helper instruction banner inside canvas
                        Surface(
                            color = Color(0xCC0A0B12),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (showMeshOverlay) {
                                    "Geser titik ${selectedAnchor?.label ?: "Mulut"} agar pas dengan foto karakter Anda"
                                } else {
                                    "Sentuh & geser layar untuk menguji kemiringan 2.5D Live2D"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sub-navigation tabs inside Rigging Editor
                    TabRow(
                        selectedTabIndex = selectedSubTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = NeonCyan,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    ) {
                        subTabs.forEachIndexed { idx, title ->
                            Tab(
                                selected = selectedSubTab == idx,
                                onClick = { selectedSubTab = idx },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                modifier = Modifier.testTag("rigging_subtab_$idx")
                            )
                        }
                    }
                }
            }
        }

        // 2. Dynamic Panel Content based on Selected Sub-Tab
        item {
            when (selectedSubTab) {
                0 -> AnchorNodesInspectorCard(
                    character = character,
                    selectedAnchor = selectedAnchor,
                    onSelectAnchor = {
                        onToggleMeshOverlay(true)
                        onSelectAnchor(it)
                    },
                    onResetAnchors = onResetAnchors,
                    onOpenTutorial = onOpenTutorial
                )
                1 -> PhysicsAndDeformerCard(
                    character = character,
                    onUpdateCharacterParams = onUpdateCharacterParams
                )
                else -> ManualParameterInspectorCard(
                    pose = pose,
                    onUpdateManualPose = onUpdateManualPose,
                    onSelectExpressionPreset = onSelectExpressionPreset,
                    onResetNeutralPose = onResetNeutralPose
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AnchorNodesInspectorCard(
    character: VtuberCharacterEntity,
    selectedAnchor: RiggingAnchorType?,
    onSelectAnchor: (RiggingAnchorType) -> Unit,
    onResetAnchors: () -> Unit,
    onOpenTutorial: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Penempatan Titik Rigging Wajah (Deformer Nodes)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pilih salah satu bagian wajah di bawah, lalu geser titiknya langsung pada gambar di atas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onOpenTutorial,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cara Pakai")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RiggingAnchorType.entries) { anchor ->
                    val isSelected = selectedAnchor == anchor
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectAnchor(anchor) },
                        label = { Text(anchor.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = anchor.color.copy(alpha = 0.24f),
                            selectedLabelColor = anchor.color
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Coordinate readout table
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(12.dp)
            ) {
                AnchorCoordRow("Mata Kiri (Left Eye)", character.leftEyeX, character.leftEyeY, NeonCyan)
                AnchorCoordRow("Mata Kanan (Right Eye)", character.rightEyeX, character.rightEyeY, NeonCyan)
                AnchorCoordRow("Mulut & LipSync (Mouth)", character.mouthX, character.mouthY, ElectricMagenta)
                AnchorCoordRow("Poros Leher/Kepala (Pivot)", character.headPivotX, character.headPivotY, EmeraldLive)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onResetAnchors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_anchors_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Posisi Titik ke Standar")
            }
        }
    }
}

@Composable
private fun AnchorCoordRow(label: String, x: Float, y: Float, color: Color) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "X: ${(x * 100).roundToInt()}%  •  Y: ${(y * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
private fun PhysicsAndDeformerCard(
    character: VtuberCharacterEntity,
    onUpdateCharacterParams: (VtuberCharacterEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = RoyalViolet
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Fisika Pegas Rambut & Kepekaan Live2D",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Atur kelenturan ayunan rambut, napas, serta kepekaan kedipan mata & mulut",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ParameterSliderRow(
                title = "Ayunan Rambut (Hair Pendulum Sway)",
                valueText = "${(character.hairSwayIntensity * 100).roundToInt()}%",
                value = character.hairSwayIntensity,
                valueRange = 0f..1.5f,
                accentColor = NeonCyan,
                onValueChange = {
                    onUpdateCharacterParams(character.copy(hairSwayIntensity = it))
                }
            )

            ParameterSliderRow(
                title = "Kelenturan Pegas Fisika (Spring Damping)",
                valueText = "${(character.physicsSpring * 100).roundToInt()}%",
                value = character.physicsSpring,
                valueRange = 0.2f..1.4f,
                accentColor = RoyalViolet,
                onValueChange = {
                    onUpdateCharacterParams(character.copy(physicsSpring = it))
                }
            )

            ParameterSliderRow(
                title = "Kecepatan Pernapasan (Breathing Cycle)",
                valueText = "${(character.breathingSpeed * 10).roundToInt() / 10f}x",
                value = character.breathingSpeed,
                valueRange = 0.4f..2.2f,
                accentColor = EmeraldLive,
                onValueChange = {
                    onUpdateCharacterParams(character.copy(breathingSpeed = it))
                }
            )

            ParameterSliderRow(
                title = "Sensitivitas Kemiringan Kepala (3D Tilt)",
                valueText = "${(character.headTiltSensitivity * 100).roundToInt()}%",
                value = character.headTiltSensitivity,
                valueRange = 0.4f..1.8f,
                accentColor = NeonCyan,
                onValueChange = {
                    onUpdateCharacterParams(character.copy(headTiltSensitivity = it))
                }
            )

            ParameterSliderRow(
                title = "Kepekaan Bukaan Mulut & Lip-Sync",
                valueText = "${(character.mouthLipSyncGain * 100).roundToInt()}%",
                value = character.mouthLipSyncGain,
                valueRange = 0.5f..2.0f,
                accentColor = ElectricMagenta,
                onValueChange = {
                    onUpdateCharacterParams(character.copy(mouthLipSyncGain = it))
                }
            )
        }
    }
}

@Composable
private fun ManualParameterInspectorCard(
    pose: Live2DPoseState,
    onUpdateManualPose: (
        yaw: Float?,
        pitch: Float?,
        roll: Float?,
        leftEye: Float?,
        rightEye: Float?,
        mouthOpen: Float?,
        mouthForm: Float?,
        blush: Float?
    ) -> Unit,
    onSelectExpressionPreset: (ExpressionPreset) -> Unit,
    onResetNeutralPose: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inspektor Parameter Live2D Manual",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Gerakkan setiap bagian wajah secara detail seperti di Live2D Cubism Viewer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onResetNeutralPose,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("reset_pose_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset 0°")
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExpressionPreset.entries) { preset ->
                    FilterChip(
                        selected = pose.expressionPreset == preset,
                        onClick = { onSelectExpressionPreset(preset) },
                        label = { Text("${preset.emoji} ${preset.labelId}") }
                    )
                }
            }

            ParameterSliderRow(
                title = "ParamAngleX (Menoleh Kiri / Kanan)",
                valueText = "${(pose.headYaw * 30).roundToInt()}°",
                value = pose.headYaw,
                valueRange = -1f..1f,
                accentColor = NeonCyan,
                onValueChange = { onUpdateManualPose(it, null, null, null, null, null, null, null) }
            )

            ParameterSliderRow(
                title = "ParamAngleY (Mendongak / Menunduk)",
                valueText = "${(pose.headPitch * 25).roundToInt()}°",
                value = pose.headPitch,
                valueRange = -1f..1f,
                accentColor = NeonCyan,
                onValueChange = { onUpdateManualPose(null, it, null, null, null, null, null, null) }
            )

            ParameterSliderRow(
                title = "ParamAngleZ (Miring Kepala)",
                valueText = "${(pose.headRoll * 25).roundToInt()}°",
                value = pose.headRoll,
                valueRange = -1f..1f,
                accentColor = RoyalViolet,
                onValueChange = { onUpdateManualPose(null, null, it, null, null, null, null, null) }
            )

            ParameterSliderRow(
                title = "ParamEyeLOpen (Bukaan Mata Kiri)",
                valueText = "${(pose.leftEyeOpen * 100).roundToInt()}%",
                value = pose.leftEyeOpen,
                valueRange = 0.05f..1.2f,
                accentColor = EmeraldLive,
                onValueChange = { onUpdateManualPose(null, null, null, it, null, null, null, null) }
            )

            ParameterSliderRow(
                title = "ParamEyeROpen (Bukaan Mata Kanan)",
                valueText = "${(pose.rightEyeOpen * 100).roundToInt()}%",
                value = pose.rightEyeOpen,
                valueRange = 0.05f..1.2f,
                accentColor = EmeraldLive,
                onValueChange = { onUpdateManualPose(null, null, null, null, it, null, null, null) }
            )

            ParameterSliderRow(
                title = "ParamMouthOpenY (Bukaan Mulut)",
                valueText = "${(pose.mouthOpen * 100).roundToInt()}%",
                value = pose.mouthOpen,
                valueRange = 0.04f..1f,
                accentColor = ElectricMagenta,
                onValueChange = { onUpdateManualPose(null, null, null, null, null, it, null, null) }
            )

            ParameterSliderRow(
                title = "ParamMouthForm (Bentuk Senyum / Sedih)",
                valueText = "${(pose.mouthForm * 100).roundToInt()}%",
                value = pose.mouthForm,
                valueRange = -1f..1f,
                accentColor = ElectricMagenta,
                onValueChange = { onUpdateManualPose(null, null, null, null, null, null, it, null) }
            )

            ParameterSliderRow(
                title = "ParamCheek (Rona Pipi / Blush)",
                valueText = "${(pose.cheekBlush * 100).roundToInt()}%",
                value = pose.cheekBlush,
                valueRange = 0f..1f,
                accentColor = Color(0xFFFF80AB),
                onValueChange = { onUpdateManualPose(null, null, null, null, null, null, null, it) }
            )
        }
    }
}

@Composable
private fun ParameterSliderRow(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor
            )
        )
    }
}
