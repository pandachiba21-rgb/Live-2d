package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.VtuberCharacterEntity
import com.example.tracking.CameraFaceExpressionAnalyzer
import com.example.tracking.ExpressionPreset
import com.example.tracking.Live2DPoseState
import com.example.tracking.MicrophoneLipSyncEngine
import com.example.ui.components.Live2DAvatarCanvas
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricMagenta
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoyalViolet
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun LiveCameraStageScreen(
    character: VtuberCharacterEntity,
    pose: Live2DPoseState,
    isCameraTrackingEnabled: Boolean,
    isMicLipSyncEnabled: Boolean,
    isCleanBroadcastMode: Boolean,
    onToggleCameraTracking: (Boolean) -> Unit,
    onToggleMicLipSync: (Boolean) -> Unit,
    onToggleCleanBroadcastMode: (Boolean) -> Unit,
    onCameraPoseUpdated: (
        faceDetected: Boolean,
        yaw: Float,
        pitch: Float,
        roll: Float,
        leftEyeOpen: Float,
        rightEyeOpen: Float,
        mouthOpen: Float,
        smile: Float,
        confidence: Float,
        fps: Int
    ) -> Unit,
    onMicLevelUpdated: (Float, Float) -> Unit,
    onSelectExpressionPreset: (ExpressionPreset) -> Unit,
    onUpdateStageBg: (Int) -> Unit,
    onPuppetDrag: (Float, Float) -> Unit,
    onResetNeutralPose: () -> Unit,
    onShowStatusMessage: (String) -> Unit,
    onOpenTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            onToggleCameraTracking(true)
            onShowStatusMessage("Kamera wajah diaktifkan! Hadapkan wajah Anda ke layar.")
        } else {
            onShowStatusMessage("Izin kamera ditolak. Anda tetap bisa menggerakkan avatar dengan sentuhan jari.")
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            onToggleMicLipSync(true)
            onShowStatusMessage("Lip-Sync Mikrofon aktif! Mulut karakter akan mengikuti suara Anda.")
        } else {
            onShowStatusMessage("Izin mikrofon tidak diberikan.")
        }
    }

    val faceAnalyzer = remember {
        CameraFaceExpressionAnalyzer(onFacePoseUpdated = onCameraPoseUpdated)
    }

    val micEngine = remember {
        MicrophoneLipSyncEngine(
            scope = coroutineScope,
            onAudioLevel = onMicLevelUpdated
        )
    }

    DisposableEffect(hasAudioPermission, isMicLipSyncEnabled) {
        if (hasAudioPermission && isMicLipSyncEnabled) {
            micEngine.start()
        } else {
            micEngine.stop()
        }
        onDispose {
            micEngine.stop()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("live_camera_stage_root")
    ) {
        // Full-Screen Live2D VTuber Stage Canvas
        Live2DAvatarCanvas(
            character = character,
            pose = pose,
            showRiggingMesh = false,
            enableTouchPuppeteer = true,
            onPuppetDrag = onPuppetDrag,
            onCharacterTapped = {
                if (isCleanBroadcastMode) {
                    onToggleCleanBroadcastMode(false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top HUD: Camera PiP Preview + Real-Time Face Sync Status + Clean Mode Button
        if (!isCleanBroadcastMode) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Left Telemetry & Status Pill
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xCC0C0E1A)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = if (pose.faceDetected && isCameraTrackingEnabled) EmeraldLive else NeonCyan.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            !hasCameraPermission || !isCameraTrackingEnabled -> AmberWarning
                                            pose.faceDetected -> EmeraldLive
                                            else -> ElectricMagenta
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    !hasCameraPermission -> "IZIN KAMERA BELUM AKTIF"
                                    !isCameraTrackingEnabled -> "MODE SENTUHAN MANUAL"
                                    pose.faceDetected -> "WAJAH SINKRON REAL-TIME (${pose.fps} FPS)"
                                    else -> "MENCARI WAJAH DI KAMERA..."
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Kepala X:${(pose.headYaw * 30).roundToInt()}° Y:${(pose.headPitch * 25).roundToInt()}° Z:${(pose.headRoll * 25).roundToInt()}° • Mulut:${(pose.mouthOpen * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )

                        if (isMicLipSyncEnabled && hasAudioPermission) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = ElectricMagenta,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                LinearProgressIndicator(
                                    progress = { pose.micLevel.coerceIn(0f, 1f) },
                                    color = ElectricMagenta,
                                    trackColor = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .height(6.dp)
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right Camera PiP Preview + Clean Broadcast Button
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasCameraPermission && isCameraTrackingEnabled) {
                        Box(
                            modifier = Modifier
                                .size(width = 104.dp, height = 138.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .border(
                                    width = 2.dp,
                                    color = if (pose.faceDetected) EmeraldLive else NeonCyan,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .testTag("camera_pip_preview")
                        ) {
                            CameraXFrontPreview(
                                analyzer = faceAnalyzer,
                                lifecycleOwner = lifecycleOwner,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color(0xAA000000),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = if (pose.faceDetected) "FACE LOCK" else "SCANNING",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (pose.faceDetected) EmeraldLive else Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = Color(0xCC0C0E1A),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            IconButton(
                                onClick = onOpenTutorial,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Bantuan Live",
                                    tint = NeonCyan
                                )
                            }
                        }

                        Surface(
                            color = Color(0xCC0C0E1A),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            IconButton(
                                onClick = {
                                    onToggleCleanBroadcastMode(true)
                                    onShowStatusMessage("Mode Layar Bersih (Clean Stream) aktif. Ketuk karakter untuk menampilkan kontrol.")
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("clean_broadcast_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Mode Layar Penuh Tanpa UI",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Control Dock for Live VTuber Streaming
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xE6101322)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .align(Alignment.BottomCenter)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Permission Prompt Banner if Camera not yet granted
                    if (!hasCameraPermission) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Aktifkan Deteksi Wajah Kamera Real-Time",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Izinkan kamera depan agar ekspresi wajah, kedipan mata, dan kepala sinkron otomatis dengan avatar.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = Color(0xFF002026)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("grant_camera_permission_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraFront,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Izinkan Kamera", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Row 1: Expression Hotkeys for Live Streaming
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ExpressionPreset.entries) { preset ->
                            FilterChip(
                                selected = pose.expressionPreset == preset,
                                onClick = { onSelectExpressionPreset(preset) },
                                label = { Text("${preset.emoji} ${preset.labelId}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricMagenta.copy(alpha = 0.28f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Row 2: Stage Background / Green Screen Chroma Switcher
                    val stages = listOf(
                        0 to "🌌 Grid",
                        1 to "🟩 Green Screen",
                        2 to "🟦 Blue Screen",
                        3 to "🔮 Nebula",
                        4 to "🌸 Sakura"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(stages) { (bgId, name) ->
                            FilterChip(
                                selected = character.bgPresetId == bgId,
                                onClick = { onUpdateStageBg(bgId) },
                                label = { Text(name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalViolet.copy(alpha = 0.32f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                    }

                    // Row 3: Camera Toggle, Microphone Lip-Sync Toggle, and Neutral Face Calibration
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                if (!hasCameraPermission) {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    onToggleCameraTracking(!isCameraTrackingEnabled)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasCameraPermission && isCameraTrackingEnabled) EmeraldLive else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (hasCameraPermission && isCameraTrackingEnabled) Color(0xFF00210E) else Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("toggle_camera_tracking_button")
                        ) {
                            Icon(
                                imageVector = if (hasCameraPermission && isCameraTrackingEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasCameraPermission && isCameraTrackingEnabled) "Kamera ON" else "Kamera OFF",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                if (!hasAudioPermission) {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    onToggleMicLipSync(!isMicLipSyncEnabled)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("toggle_mic_lipsync_button")
                        ) {
                            Icon(
                                imageVector = if (hasAudioPermission && isMicLipSyncEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = null,
                                tint = if (hasAudioPermission && isMicLipSyncEnabled) ElectricMagenta else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasAudioPermission && isMicLipSyncEnabled) "Mic ON" else "Mic LipSync",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                faceAnalyzer.calibrateNeutral()
                                onResetNeutralPose()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("calibrate_neutral_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "Kalibrasi Wajah Netral",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kalibrasi")
                        }
                    }
                }
            }
        } else {
            // Subtle floating exit button in top-right corner during Clean Broadcast Mode
            Surface(
                color = Color(0x66000000),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = { onToggleCleanBroadcastMode(false) },
                    modifier = Modifier.testTag("exit_clean_broadcast_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Tampilkan Kontrol Studio",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraXFrontPreview(
    analyzer: CameraFaceExpressionAnalyzer,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, analyzer)
                    }

                val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis
                )
            } catch (_: Exception) {
                // Handle camera binding gracefully on emulators without physical webcam
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {}
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
