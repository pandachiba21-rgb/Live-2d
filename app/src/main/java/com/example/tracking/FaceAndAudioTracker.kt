package com.example.tracking

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PointF
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.FaceDetector
import android.media.MediaRecorder
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real-time VTuber Live2D parameter state driven by CameraX Face Tracking,
 * Microphone Lip-Sync, or Manual Touch Joystick & Sliders.
 */
data class Live2DPoseState(
    val headYaw: Float = 0f,         // -1f (Left) .. +1f (Right)
    val headPitch: Float = 0f,       // -1f (Down) .. +1f (Up)
    val headRoll: Float = 0f,        // -1f (Tilt Left) .. +1f (Tilt Right)
    val leftEyeOpen: Float = 1f,     // 0f (Closed) .. 1.2f (Wide Open)
    val rightEyeOpen: Float = 1f,    // 0f (Closed) .. 1.2f (Wide Open)
    val eyeBallX: Float = 0f,        // -1f .. +1f
    val eyeBallY: Float = 0f,        // -1f .. +1f
    val mouthOpen: Float = 0.08f,    // 0f (Closed) .. 1f (Wide Open)
    val mouthForm: Float = 0.35f,    // -1f (Sad/Frown) .. +1f (Happy Smile)
    val browLeftY: Float = 0f,       // -1f (Angry) .. +1f (Surprised)
    val browRightY: Float = 0f,
    val cheekBlush: Float = 0.15f,   // 0f .. 1f
    val expressionPreset: ExpressionPreset = ExpressionPreset.NEUTRAL,
    val faceDetected: Boolean = false,
    val confidence: Float = 0f,
    val fps: Int = 60,
    val micLevel: Float = 0f
)

enum class ExpressionPreset(
    val labelId: String,
    val emoji: String,
    val mouthForm: Float,
    val eyeScale: Float,
    val browOffset: Float,
    val blush: Float
) {
    NEUTRAL("Netral", "✨", 0.30f, 1.0f, 0.0f, 0.15f),
    HAPPY("Senang", "😊", 0.95f, 0.88f, 0.35f, 0.55f),
    WINK("Kedip", "😉", 0.75f, 1.0f, 0.25f, 0.40f),
    SURPRISED("Kaget", "😲", -0.10f, 1.22f, 0.85f, 0.20f),
    ANGRY("Kesal", "💢", -0.75f, 0.90f, -0.80f, 0.45f),
    SHY("Malu", "😳", 0.20f, 0.92f, -0.25f, 0.95f),
    STAR_EYES("Idol", "🤩", 0.90f, 1.15f, 0.45f, 0.70f)
}

/**
 * CameraX ImageAnalysis.Analyzer that extracts real-time facial geometry, head yaw/pitch/roll,
 * eye openness, and mouth openness from the front camera feed.
 */
class CameraFaceExpressionAnalyzer(
    private val onFacePoseUpdated: (
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
    ) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastFrameTimestamp = System.currentTimeMillis()
    private var smoothedYaw = 0f
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f
    private var smoothedLeftEye = 1f
    private var smoothedRightEye = 1f
    private var smoothedMouth = 0.08f
    private var smoothedSmile = 0.35f

    // Calibration offsets
    @Volatile var centerYawOffset: Float = 0f
    @Volatile var centerPitchOffset: Float = 0f
    @Volatile var lastRawYaw: Float = 0f
    @Volatile var lastRawPitch: Float = 0f

    fun calibrateNeutral() {
        centerYawOffset = lastRawYaw
        centerPitchOffset = lastRawPitch
    }

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        val dt = max(1L, now - lastFrameTimestamp)
        val currentFps = (1000L / dt).toInt().coerceIn(15, 60)
        lastFrameTimestamp = now

        try {
            val plane = image.planes.firstOrNull()
            if (plane == null) {
                image.close()
                return
            }

            val width = image.width
            val height = image.height
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            // Downsample to 160x120 RGB_565 bitmap for fast real-time Android FaceDetector + regional luma analysis
            val targetW = 160
            val targetH = 120
            val rgb565Bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565)
            val lumaGrid = IntArray(targetW * targetH)

            val rotation = image.imageInfo.rotationDegrees

            for (ty in 0 until targetH) {
                for (tx in 0 until targetW) {
                    // Map target (tx, ty) considering front-camera rotation & mirror
                    val normX = tx.toFloat() / targetW
                    val normY = ty.toFloat() / targetH

                    val (srcX, srcY) = when (rotation) {
                        270 -> Pair(((1f - normY) * (width - 1)).toInt(), ((1f - normX) * (height - 1)).toInt())
                        90 -> Pair((normY * (width - 1)).toInt(), (normX * (height - 1)).toInt())
                        180 -> Pair(((1f - normX) * (width - 1)).toInt(), ((1f - normY) * (height - 1)).toInt())
                        else -> Pair(((1f - normX) * (width - 1)).toInt(), (normY * (height - 1)).toInt())
                    }

                    val index = srcY * rowStride + srcX * pixelStride
                    val yVal = if (index in 0 until buffer.limit()) {
                        buffer.get(index).toInt() and 0xFF
                    } else {
                        128
                    }
                    lumaGrid[ty * targetW + tx] = yVal
                    val color = (0xFF shl 24) or (yVal shl 16) or (yVal shl 8) or yVal
                    rgb565Bitmap.setPixel(tx, ty, color)
                }
            }

            val detector = FaceDetector(targetW, targetH, 1)
            val faces = arrayOfNulls<FaceDetector.Face>(1)
            val foundCount = detector.findFaces(rgb565Bitmap, faces)

            if (foundCount > 0 && faces[0] != null) {
                val face = faces[0]!!
                val midPoint = PointF()
                face.getMidPoint(midPoint)
                val eyesDist = face.eyesDistance().coerceAtLeast(12f)
                val conf = face.confidence().coerceIn(0.3f, 1.0f)

                val rawYaw = ((midPoint.x - targetW * 0.5f) / (targetW * 0.25f)).coerceIn(-1f, 1f)
                val rawPitch = ((targetH * 0.45f - midPoint.y) / (targetH * 0.25f)).coerceIn(-1f, 1f)
                lastRawYaw = rawYaw
                lastRawPitch = rawPitch

                val calibratedYaw = (rawYaw - centerYawOffset).coerceIn(-1f, 1f)
                val calibratedPitch = (rawPitch - centerPitchOffset).coerceIn(-1f, 1f)

                // Estimate head roll & eye blink from left/right eye regional contrast around midpoint
                val leftEyeCenterX = (midPoint.x - eyesDist * 0.5f).toInt().coerceIn(6, targetW - 7)
                val rightEyeCenterX = (midPoint.x + eyesDist * 0.5f).toInt().coerceIn(6, targetW - 7)
                val eyeCenterY = midPoint.y.toInt().coerceIn(6, targetH - 7)

                val leftEyeContrast = computeRegionContrast(lumaGrid, targetW, targetH, leftEyeCenterX, eyeCenterY, 5)
                val rightEyeContrast = computeRegionContrast(lumaGrid, targetW, targetH, rightEyeCenterX, eyeCenterY, 5)

                // Estimate vertical centroid difference between left & right eye dark pupils for head roll
                val leftPupilY = findDarkestYOffset(lumaGrid, targetW, targetH, leftEyeCenterX, eyeCenterY, 5)
                val rightPupilY = findDarkestYOffset(lumaGrid, targetW, targetH, rightEyeCenterX, eyeCenterY, 5)
                val rawRoll = atan2((rightPupilY - leftPupilY).toDouble(), eyesDist.toDouble()).toFloat() * 2.5f

                // Mouth region is ~1.15 * eyesDist below the eyes midpoint
                val mouthCenterX = midPoint.x.toInt().coerceIn(8, targetW - 9)
                val mouthCenterY = (midPoint.y + eyesDist * 1.12f).toInt().coerceIn(8, targetH - 9)
                val mouthOpeningRatio = computeMouthOpening(lumaGrid, targetW, targetH, mouthCenterX, mouthCenterY, (eyesDist * 0.42f).toInt().coerceIn(4, 14))
                val smileSpread = computeMouthWidthSmile(lumaGrid, targetW, targetH, mouthCenterX, mouthCenterY, (eyesDist * 0.55f).toInt().coerceIn(5, 16))

                val targetLeftEye = ((leftEyeContrast - 14f) / 30f).coerceIn(0.05f, 1.15f)
                val targetRightEye = ((rightEyeContrast - 14f) / 30f).coerceIn(0.05f, 1.15f)

                smoothedYaw = lerp(smoothedYaw, calibratedYaw, 0.32f)
                smoothedPitch = lerp(smoothedPitch, calibratedPitch, 0.32f)
                smoothedRoll = lerp(smoothedRoll, rawRoll.coerceIn(-1f, 1f), 0.25f)
                smoothedLeftEye = lerp(smoothedLeftEye, targetLeftEye, 0.40f)
                smoothedRightEye = lerp(smoothedRightEye, targetRightEye, 0.40f)
                smoothedMouth = lerp(smoothedMouth, mouthOpeningRatio, 0.45f)
                smoothedSmile = lerp(smoothedSmile, smileSpread, 0.28f)

                onFacePoseUpdated(
                    true,
                    smoothedYaw,
                    smoothedPitch,
                    smoothedRoll,
                    smoothedLeftEye,
                    smoothedRightEye,
                    smoothedMouth,
                    smoothedSmile,
                    conf,
                    currentFps
                )
            } else {
                // Fallback centroid tracking on skin/face luminance blob if user is closer/further or angled
                val blob = computeFaceBlobMoments(lumaGrid, targetW, targetH)
                if (blob.valid) {
                    lastRawYaw = blob.yaw
                    lastRawPitch = blob.pitch
                    val calYaw = (blob.yaw - centerYawOffset).coerceIn(-1f, 1f)
                    val calPitch = (blob.pitch - centerPitchOffset).coerceIn(-1f, 1f)

                    smoothedYaw = lerp(smoothedYaw, calYaw, 0.25f)
                    smoothedPitch = lerp(smoothedPitch, calPitch, 0.25f)
                    smoothedRoll = lerp(smoothedRoll, blob.roll, 0.20f)
                    smoothedLeftEye = lerp(smoothedLeftEye, blob.leftEyeOpen, 0.35f)
                    smoothedRightEye = lerp(smoothedRightEye, blob.rightEyeOpen, 0.35f)
                    smoothedMouth = lerp(smoothedMouth, blob.mouthOpen, 0.40f)

                    onFacePoseUpdated(
                        true,
                        smoothedYaw,
                        smoothedPitch,
                        smoothedRoll,
                        smoothedLeftEye,
                        smoothedRightEye,
                        smoothedMouth,
                        smoothedSmile,
                        0.72f,
                        currentFps
                    )
                } else {
                    onFacePoseUpdated(
                        false,
                        smoothedYaw * 0.92f,
                        smoothedPitch * 0.92f,
                        smoothedRoll * 0.92f,
                        1f,
                        1f,
                        0.08f,
                        0.35f,
                        0f,
                        currentFps
                    )
                }
            }
        } catch (_: Exception) {
            // Ignore frame errors safely
        } finally {
            image.close()
        }
    }

    private fun lerp(start: Float, stop: Float, fraction: Float): Float {
        return start + (stop - start) * fraction.coerceIn(0f, 1f)
    }

    private fun computeRegionContrast(
        grid: IntArray,
        w: Int,
        h: Int,
        cx: Int,
        cy: Int,
        radius: Int
    ): Float {
        var minLuma = 255
        var maxLuma = 0
        for (dy in -radius..radius) {
            val y = (cy + dy).coerceIn(0, h - 1)
            for (dx in -radius..radius) {
                val x = (cx + dx).coerceIn(0, w - 1)
                val v = grid[y * w + x]
                if (v < minLuma) minLuma = v
                if (v > maxLuma) maxLuma = v
            }
        }
        return (maxLuma - minLuma).toFloat()
    }

    private fun findDarkestYOffset(
        grid: IntArray,
        w: Int,
        h: Int,
        cx: Int,
        cy: Int,
        radius: Int
    ): Float {
        var darkestY = cy
        var minLuma = 255
        for (dy in -radius..radius) {
            val y = (cy + dy).coerceIn(0, h - 1)
            for (dx in -radius..radius) {
                val x = (cx + dx).coerceIn(0, w - 1)
                val v = grid[y * w + x]
                if (v < minLuma) {
                    minLuma = v
                    darkestY = y
                }
            }
        }
        return (darkestY - cy).toFloat()
    }

    private fun computeMouthOpening(
        grid: IntArray,
        w: Int,
        h: Int,
        cx: Int,
        cy: Int,
        radius: Int
    ): Float {
        var sumCenter = 0
        var countCenter = 0
        var sumSurround = 0
        var countSurround = 0

        for (dy in -radius..radius) {
            val y = (cy + dy).coerceIn(0, h - 1)
            for (dx in -radius..radius) {
                val x = (cx + dx).coerceIn(0, w - 1)
                val v = grid[y * w + x]
                if (abs(dy) <= radius / 2 && abs(dx) <= radius / 2) {
                    sumCenter += v
                    countCenter++
                } else {
                    sumSurround += v
                    countSurround++
                }
            }
        }
        val avgCenter = if (countCenter > 0) sumCenter.toFloat() / countCenter else 128f
        val avgSurround = if (countSurround > 0) sumSurround.toFloat() / countSurround else 128f
        val darknessDiff = (avgSurround - avgCenter).coerceAtLeast(0f)
        return (darknessDiff / 42f).coerceIn(0.05f, 1.0f)
    }

    private fun computeMouthWidthSmile(
        grid: IntArray,
        w: Int,
        h: Int,
        cx: Int,
        cy: Int,
        rx: Int
    ): Float {
        val leftCorner = grid[cy.coerceIn(0, h - 1) * w + (cx - rx / 2).coerceIn(0, w - 1)]
        val rightCorner = grid[cy.coerceIn(0, h - 1) * w + (cx + rx / 2).coerceIn(0, w - 1)]
        val upperCheck = grid[(cy - 3).coerceIn(0, h - 1) * w + cx.coerceIn(0, w - 1)]
        val diff = upperCheck - (leftCorner + rightCorner) / 2
        return ((diff + 10) / 45f).coerceIn(-0.4f, 1.0f)
    }

    private data class BlobResult(
        val valid: Boolean,
        val yaw: Float = 0f,
        val pitch: Float = 0f,
        val roll: Float = 0f,
        val leftEyeOpen: Float = 1f,
        val rightEyeOpen: Float = 1f,
        val mouthOpen: Float = 0.1f
    )

    private fun computeFaceBlobMoments(grid: IntArray, w: Int, h: Int): BlobResult {
        var sumX = 0f
        var sumY = 0f
        var totalWeight = 0f

        val startX = (w * 0.15f).toInt()
        val endX = (w * 0.85f).toInt()
        val startY = (h * 0.12f).toInt()
        val endY = (h * 0.88f).toInt()

        for (y in startY until endY) {
            for (x in startX until endX) {
                val luma = grid[y * w + x]
                if (luma in 45..235) {
                    val weight = luma.toFloat()
                    sumX += x * weight
                    sumY += y * weight
                    totalWeight += weight
                }
            }
        }

        if (totalWeight < 1000f) return BlobResult(false)

        val cx = sumX / totalWeight
        val cy = sumY / totalWeight
        val yaw = ((cx - w * 0.5f) / (w * 0.18f)).coerceIn(-1f, 1f)
        val pitch = ((h * 0.48f - cy) / (h * 0.18f)).coerceIn(-1f, 1f)

        val eyeY = (cy - h * 0.08f).toInt().coerceIn(6, h - 7)
        val leftX = (cx - w * 0.10f).toInt().coerceIn(6, w - 7)
        val rightX = (cx + w * 0.10f).toInt().coerceIn(6, w - 7)
        val mouthY = (cy + h * 0.12f).toInt().coerceIn(8, h - 9)

        val leftContrast = computeRegionContrast(grid, w, h, leftX, eyeY, 5)
        val rightContrast = computeRegionContrast(grid, w, h, rightX, eyeY, 5)
        val mouthOpen = computeMouthOpening(grid, w, h, cx.toInt(), mouthY, 7)

        return BlobResult(
            valid = true,
            yaw = yaw,
            pitch = pitch,
            roll = (yaw * 0.35f).coerceIn(-0.6f, 0.6f),
            leftEyeOpen = ((leftContrast - 12f) / 28f).coerceIn(0.1f, 1.1f),
            rightEyeOpen = ((rightContrast - 12f) / 28f).coerceIn(0.1f, 1.1f),
            mouthOpen = mouthOpen
        )
    }
}

/**
 * Real-time Microphone Lip-Sync Analyzer using Android AudioRecord.
 * Converts voice amplitude & vowel energy into smooth Live2D mouth opening.
 */
class MicrophoneLipSyncEngine(
    private val scope: CoroutineScope,
    private val onAudioLevel: (mouthOpen: Float, rawAmplitude: Float) -> Unit
) {
    private var recordingJob: Job? = null
    private var smoothedLevel = 0.05f

    @SuppressLint("MissingPermission")
    fun start() {
        if (recordingJob?.isActive == true) return
        recordingJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBufferSize <= 0) return@launch

            val audioRecord = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize * 2
                )
            } catch (_: Exception) {
                return@launch
            }

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release()
                return@launch
            }

            val buffer = ShortArray(512)
            try {
                audioRecord.startRecording()
                while (isActive) {
                    val readCount = audioRecord.read(buffer, 0, buffer.size)
                    if (readCount > 0) {
                        var sumSquares = 0.0
                        for (i in 0 until readCount) {
                            val sample = buffer[i].toDouble() / 32768.0
                            sumSquares += sample * sample
                        }
                        val rms = sqrt(sumSquares / readCount).toFloat()
                        val normalized = (rms * 6.5f).coerceIn(0f, 1f)
                        smoothedLevel = smoothedLevel + (normalized - smoothedLevel) * 0.45f
                        onAudioLevel(smoothedLevel.coerceIn(0.04f, 1f), normalized)
                    }
                    delay(20L)
                }
            } catch (_: Exception) {
                // Ignore audio stop exceptions
            } finally {
                try {
                    audioRecord.stop()
                } catch (_: Exception) {}
                audioRecord.release()
            }
        }
    }

    fun stop() {
        recordingJob?.cancel()
        recordingJob = null
    }
}
