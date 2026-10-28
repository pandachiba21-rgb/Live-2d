package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraFront
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.VtuberCharacterEntity
import com.example.ui.VtuberViewModel
import com.example.ui.components.QuickTutorialOverlayDialog
import com.example.ui.components.TutorialGuideScreen
import com.example.ui.screens.Live2DRiggingScreen
import com.example.ui.screens.LiveCameraStageScreen
import com.example.ui.screens.StudioHomeScreen
import com.example.ui.theme.ElectricMagenta
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.LuminaTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoyalViolet

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: VtuberViewModel = viewModel(
                factory = VtuberViewModel.provideFactory(context)
            )
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            LuminaTheme(darkTheme = isDarkTheme) {
                LuminaVtuberApp(viewModel = viewModel)
            }
        }
    }
}

private data class NavTabItem(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun LuminaVtuberApp(viewModel: VtuberViewModel) {
    val context = LocalContext.current
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val selectedCharId by viewModel.selectedCharacterId.collectAsStateWithLifecycle()
    val poseState by viewModel.poseState.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val showMeshOverlay by viewModel.showMeshOverlay.collectAsStateWithLifecycle()
    val selectedRigAnchor by viewModel.selectedRigAnchor.collectAsStateWithLifecycle()
    val isCameraTrackingEnabled by viewModel.isCameraTrackingEnabled.collectAsStateWithLifecycle()
    val isMicLipSyncEnabled by viewModel.isMicLipSyncEnabled.collectAsStateWithLifecycle()
    val isCleanBroadcastMode by viewModel.isCleanBroadcastMode.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var showQuickTutorialDialog by rememberSaveable { mutableStateOf(false) }

    val activeCharacter = remember(characters, selectedCharId) {
        characters.find { it.id == selectedCharId }
            ?: characters.firstOrNull()
            ?: VtuberCharacterEntity(
                id = 1L,
                name = "Celestia Aria",
                tagline = "Cyber-Idol Starlight • Rigging Live2D Penuh",
                imageUri = null
            )
    }

    // Handle system Back press on secondary tabs or Clean Broadcast mode
    BackHandler(enabled = isCleanBroadcastMode || currentTab != 0) {
        if (isCleanBroadcastMode) {
            viewModel.setCleanBroadcastMode(false)
        } else {
            currentTab = 0
        }
    }

    val navItems = listOf(
        NavTabItem(0, "Studio", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_tab_studio"),
        NavTabItem(1, "Rig Live2D", Icons.Filled.Tune, Icons.Outlined.Tune, "nav_tab_rigging"),
        NavTabItem(2, "Live Kamera", Icons.Filled.CameraFront, Icons.Outlined.CameraFront, "nav_tab_live"),
        NavTabItem(3, "Tutorial", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_tab_tutorial")
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (!isCleanBroadcastMode) {
                LuminaTopBar(
                    activeCharacterName = activeCharacter.name,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = viewModel::toggleDarkTheme,
                    onOpenTutorialModal = { showQuickTutorialDialog = true }
                )
            }
        },
        bottomBar = {
            if (!isCleanBroadcastMode) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    navItems.forEach { item ->
                        val selected = currentTab == item.index
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = item.index },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan.copy(alpha = 0.16f)
                            ),
                            modifier = Modifier.testTag(item.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> StudioHomeScreen(
                    characters = characters,
                    activeCharacter = activeCharacter,
                    poseState = poseState,
                    onSelectCharacter = viewModel::selectCharacter,
                    onUpdateCharacter = viewModel::updateCharacterParameters,
                    onImportPhotoCharacter = { uri, name, tagline ->
                        viewModel.importCharacterFromPhoto(context, uri, name, tagline)
                    },
                    onCreatePresetCharacter = viewModel::createNewPresetCharacter,
                    onDeleteCharacter = viewModel::deleteCharacter,
                    onPuppetDrag = viewModel::onPuppetTouchDrag,
                    onCharacterTapped = viewModel::triggerCharacterTapGreeting,
                    onSelectExpressionPreset = viewModel::updateExpressionPreset,
                    onOpenRiggingEditor = { currentTab = 1 },
                    onOpenLiveCamera = { currentTab = 2 },
                    onOpenTutorial = { showQuickTutorialDialog = true }
                )

                1 -> Live2DRiggingScreen(
                    character = activeCharacter,
                    pose = poseState,
                    showMeshOverlay = showMeshOverlay,
                    selectedAnchor = selectedRigAnchor,
                    onToggleMeshOverlay = viewModel::setShowMeshOverlay,
                    onSelectAnchor = viewModel::selectRigAnchor,
                    onMoveAnchor = { anchor, nx, ny ->
                        viewModel.updateCharacterRigAnchor(activeCharacter, anchor, nx, ny)
                    },
                    onResetAnchors = {
                        viewModel.resetCharacterRigAnchors(activeCharacter)
                    },
                    onUpdateCharacterParams = viewModel::updateCharacterParameters,
                    onUpdateManualPose = viewModel::updateManualPose,
                    onSelectExpressionPreset = viewModel::updateExpressionPreset,
                    onResetNeutralPose = viewModel::resetNeutralPose,
                    onPuppetDrag = viewModel::onPuppetTouchDrag,
                    onOpenTutorial = { showQuickTutorialDialog = true }
                )

                2 -> LiveCameraStageScreen(
                    character = activeCharacter,
                    pose = poseState,
                    isCameraTrackingEnabled = isCameraTrackingEnabled,
                    isMicLipSyncEnabled = isMicLipSyncEnabled,
                    isCleanBroadcastMode = isCleanBroadcastMode,
                    onToggleCameraTracking = viewModel::setCameraTrackingEnabled,
                    onToggleMicLipSync = viewModel::setMicLipSyncEnabled,
                    onToggleCleanBroadcastMode = viewModel::setCleanBroadcastMode,
                    onCameraPoseUpdated = viewModel::onCameraFacePoseUpdated,
                    onMicLevelUpdated = viewModel::onMicrophoneLevelUpdated,
                    onSelectExpressionPreset = viewModel::updateExpressionPreset,
                    onUpdateStageBg = { bgId ->
                        viewModel.updateCharacterParameters(activeCharacter.copy(bgPresetId = bgId))
                    },
                    onPuppetDrag = viewModel::onPuppetTouchDrag,
                    onResetNeutralPose = viewModel::resetNeutralPose,
                    onShowStatusMessage = viewModel::showStatusMessage,
                    onOpenTutorial = { showQuickTutorialDialog = true }
                )

                3 -> TutorialGuideScreen(
                    onJumpToTab = { targetTab -> currentTab = targetTab }
                )
            }

            // Floating Status Toast Banner
            AnimatedVisibility(
                visible = statusBannerMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                statusBannerMessage?.let { msg ->
                    Surface(
                        color = Color(0xEE13172B),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonCyan, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = viewModel::clearStatusBanner,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup pesan",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQuickTutorialDialog) {
        QuickTutorialOverlayDialog(
            onDismiss = { showQuickTutorialDialog = false },
            onOpenFullTutorialTab = { targetTab ->
                currentTab = targetTab
            }
        )
    }
}

@Composable
private fun LuminaTopBar(
    activeCharacterName: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenTutorialModal: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(NeonCyan, RoyalViolet, ElectricMagenta))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Lumina Live2D Studio",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Aktif: $activeCharacterName",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Prominent Tutorial Button accessible from ANY screen
                Button(
                    onClick = onOpenTutorialModal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan.copy(alpha = 0.18f),
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("top_bar_tutorial_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Buka Tutorial Teks",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tutorial",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("toggle_theme_button")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = "Ubah Tema Gelap / Terang",
                        tint = if (isDarkTheme) NeonCyan else RoyalViolet
                    )
                }
            }
        }
    }
}
