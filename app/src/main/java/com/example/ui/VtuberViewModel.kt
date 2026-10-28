package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.LuminaDatabase
import com.example.data.VtuberCharacterEntity
import com.example.data.VtuberRepository
import com.example.tracking.ExpressionPreset
import com.example.tracking.Live2DPoseState
import com.example.ui.components.RiggingAnchorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class VtuberViewModel(
    private val repository: VtuberRepository
) : ViewModel() {

    val characters: StateFlow<List<VtuberCharacterEntity>> = repository.characters
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _selectedCharacterId = MutableStateFlow<Long?>(null)
    val selectedCharacterId: StateFlow<Long?> = _selectedCharacterId.asStateFlow()

    private val _poseState = MutableStateFlow(Live2DPoseState())
    val poseState: StateFlow<Live2DPoseState> = _poseState.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _showMeshOverlay = MutableStateFlow(true)
    val showMeshOverlay: StateFlow<Boolean> = _showMeshOverlay.asStateFlow()

    private val _selectedRigAnchor = MutableStateFlow<RiggingAnchorType?>(RiggingAnchorType.MOUTH)
    val selectedRigAnchor: StateFlow<RiggingAnchorType?> = _selectedRigAnchor.asStateFlow()

    private val _isCameraTrackingEnabled = MutableStateFlow(true)
    val isCameraTrackingEnabled: StateFlow<Boolean> = _isCameraTrackingEnabled.asStateFlow()

    private val _isMicLipSyncEnabled = MutableStateFlow(false)
    val isMicLipSyncEnabled: StateFlow<Boolean> = _isMicLipSyncEnabled.asStateFlow()

    private val _isCleanBroadcastMode = MutableStateFlow(false)
    val isCleanBroadcastMode: StateFlow<Boolean> = _isCleanBroadcastMode.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultPresets()
        }
    }

    fun selectCharacter(id: Long) {
        _selectedCharacterId.value = id
    }

    fun toggleDarkTheme() {
        _isDarkTheme.update { !it }
    }

    fun setShowMeshOverlay(show: Boolean) {
        _showMeshOverlay.value = show
    }

    fun selectRigAnchor(anchor: RiggingAnchorType) {
        _selectedRigAnchor.value = anchor
    }

    fun setCameraTrackingEnabled(enabled: Boolean) {
        _isCameraTrackingEnabled.value = enabled
        if (!enabled) {
            _poseState.update { it.copy(faceDetected = false, confidence = 0f) }
        }
    }

    fun setMicLipSyncEnabled(enabled: Boolean) {
        _isMicLipSyncEnabled.value = enabled
        if (!enabled) {
            _poseState.update { it.copy(micLevel = 0f) }
        }
    }

    fun setCleanBroadcastMode(clean: Boolean) {
        _isCleanBroadcastMode.value = clean
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun showStatusMessage(message: String) {
        _statusBannerMessage.value = message
    }

    fun updateExpressionPreset(preset: ExpressionPreset) {
        _poseState.update {
            it.copy(
                expressionPreset = preset,
                mouthForm = preset.mouthForm,
                cheekBlush = preset.blush,
                browLeftY = preset.browOffset,
                browRightY = preset.browOffset
            )
        }
    }

    fun triggerCharacterTapGreeting() {
        val nextPreset = when (_poseState.value.expressionPreset) {
            ExpressionPreset.NEUTRAL -> ExpressionPreset.HAPPY
            ExpressionPreset.HAPPY -> ExpressionPreset.WINK
            ExpressionPreset.WINK -> ExpressionPreset.STAR_EYES
            else -> ExpressionPreset.NEUTRAL
        }
        updateExpressionPreset(nextPreset)
    }

    fun onPuppetTouchDrag(deltaYaw: Float, deltaPitch: Float) {
        _poseState.update { state ->
            val newYaw = (state.headYaw + deltaYaw).coerceIn(-1f, 1f)
            val newPitch = (state.headPitch + deltaPitch).coerceIn(-1f, 1f)
            state.copy(
                headYaw = newYaw,
                headPitch = newPitch,
                headRoll = (newYaw * 0.4f).coerceIn(-0.8f, 0.8f),
                eyeBallX = newYaw,
                eyeBallY = -newPitch
            )
        }
    }

    fun updateManualPose(
        yaw: Float? = null,
        pitch: Float? = null,
        roll: Float? = null,
        leftEye: Float? = null,
        rightEye: Float? = null,
        mouthOpen: Float? = null,
        mouthForm: Float? = null,
        blush: Float? = null
    ) {
        _poseState.update { current ->
            current.copy(
                headYaw = yaw ?: current.headYaw,
                headPitch = pitch ?: current.headPitch,
                headRoll = roll ?: current.headRoll,
                leftEyeOpen = leftEye ?: current.leftEyeOpen,
                rightEyeOpen = rightEye ?: current.rightEyeOpen,
                mouthOpen = mouthOpen ?: current.mouthOpen,
                mouthForm = mouthForm ?: current.mouthForm,
                cheekBlush = blush ?: current.cheekBlush
            )
        }
    }

    fun resetNeutralPose() {
        _poseState.update {
            Live2DPoseState(
                expressionPreset = ExpressionPreset.NEUTRAL,
                faceDetected = it.faceDetected,
                fps = it.fps
            )
        }
        showStatusMessage("Posisi kepala & ekspresi direset ke titik tengah (Netral).")
    }

    fun onCameraFacePoseUpdated(
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
    ) {
        if (!_isCameraTrackingEnabled.value) return
        _poseState.update { current ->
            val effectiveMouth = if (_isMicLipSyncEnabled.value && current.micLevel > 0.1f) {
                maxOf(mouthOpen, current.micLevel)
            } else {
                mouthOpen
            }
            current.copy(
                faceDetected = faceDetected,
                headYaw = if (faceDetected) yaw else current.headYaw,
                headPitch = if (faceDetected) pitch else current.headPitch,
                headRoll = if (faceDetected) roll else current.headRoll,
                leftEyeOpen = if (faceDetected) leftEyeOpen else current.leftEyeOpen,
                rightEyeOpen = if (faceDetected) rightEyeOpen else current.rightEyeOpen,
                mouthOpen = if (faceDetected) effectiveMouth else current.mouthOpen,
                mouthForm = if (faceDetected) (smile * 0.6f + current.expressionPreset.mouthForm * 0.4f) else current.mouthForm,
                confidence = confidence,
                fps = fps
            )
        }
    }

    fun onMicrophoneLevelUpdated(mouthOpen: Float, rawAmplitude: Float) {
        if (!_isMicLipSyncEnabled.value) return
        _poseState.update { current ->
            current.copy(
                mouthOpen = mouthOpen,
                micLevel = rawAmplitude
            )
        }
    }

    fun updateCharacterRigAnchor(
        character: VtuberCharacterEntity,
        anchorType: RiggingAnchorType,
        normX: Float,
        normY: Float
    ) {
        val updated = when (anchorType) {
            RiggingAnchorType.LEFT_EYE -> character.copy(leftEyeX = normX, leftEyeY = normY)
            RiggingAnchorType.RIGHT_EYE -> character.copy(rightEyeX = normX, rightEyeY = normY)
            RiggingAnchorType.MOUTH -> character.copy(mouthX = normX, mouthY = normY)
            RiggingAnchorType.HEAD_PIVOT -> character.copy(headPivotX = normX, headPivotY = normY)
        }
        viewModelScope.launch {
            repository.updateCharacter(updated)
        }
    }

    fun resetCharacterRigAnchors(character: VtuberCharacterEntity) {
        val reset = character.copy(
            leftEyeX = 0.37f,
            leftEyeY = 0.43f,
            rightEyeX = 0.63f,
            rightEyeY = 0.43f,
            mouthX = 0.50f,
            mouthY = 0.58f,
            headPivotX = 0.50f,
            headPivotY = 0.68f
        )
        viewModelScope.launch {
            repository.updateCharacter(reset)
            showStatusMessage("Titik Rigging Live2D direset ke posisi standar.")
        }
    }

    fun updateCharacterParameters(updated: VtuberCharacterEntity) {
        viewModelScope.launch {
            repository.updateCharacter(updated)
        }
    }

    fun importCharacterFromPhoto(
        context: Context,
        sourceUri: Uri,
        name: String,
        tagline: String
    ) {
        viewModelScope.launch {
            val savedPath = copyUriToInternalStorage(context, sourceUri)
            val finalUriStr = savedPath ?: sourceUri.toString()
            val newChar = VtuberCharacterEntity(
                name = name.ifBlank { "Avatar Kustomku" },
                tagline = tagline.ifBlank { "Foto Karakter Impor • Rigging Live2D Aktif" },
                imageUri = finalUriStr,
                archetypeId = 3,
                hairColorHex = 0xFF00E5FF,
                eyeColorHex = 0xFFFF2E93,
                outfitColorHex = 0xFF7C4DFF,
                accessoryId = 0
            )
            val newId = repository.saveCharacter(newChar)
            _selectedCharacterId.value = newId
            showStatusMessage("Foto karakter '${newChar.name}' berhasil diimpor! Silakan sesuaikan titik mata & mulut di Editor Live2D.")
        }
    }

    fun createNewPresetCharacter(
        name: String,
        tagline: String,
        archetypeId: Int,
        hairColorHex: Long,
        eyeColorHex: Long,
        outfitColorHex: Long,
        accessoryId: Int
    ) {
        viewModelScope.launch {
            val newChar = VtuberCharacterEntity(
                name = name.ifBlank { "VTuber Baru" },
                tagline = tagline.ifBlank { "Model Live2D Kustom" },
                imageUri = null,
                archetypeId = archetypeId,
                hairColorHex = hairColorHex,
                eyeColorHex = eyeColorHex,
                outfitColorHex = outfitColorHex,
                accessoryId = accessoryId
            )
            val newId = repository.saveCharacter(newChar)
            _selectedCharacterId.value = newId
            showStatusMessage("Karakter '${newChar.name}' berhasil dibuat dan siap digerakkan!")
        }
    }

    fun deleteCharacter(character: VtuberCharacterEntity) {
        viewModelScope.launch {
            repository.deleteCharacter(character.id)
            showStatusMessage("Karakter '${character.name}' telah dihapus.")
        }
    }

    private fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val dir = File(context.filesDir, "vtuber_avatars")
            if (!dir.exists()) dir.mkdirs()
            val destFile = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            Uri.fromFile(destFile).toString()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LuminaDatabase? = null

        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = INSTANCE ?: synchronized(this) {
                        INSTANCE ?: Room.databaseBuilder(
                            context.applicationContext,
                            LuminaDatabase::class.java,
                            "lumina_vtuber_studio.db"
                        ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
                    }
                    return VtuberViewModel(VtuberRepository(db.characterDao())) as T
                }
            }
        }
    }
}
