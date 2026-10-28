package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.example.data.VtuberCharacterEntity
import com.example.tracking.ExpressionPreset
import com.example.tracking.Live2DPoseState
import com.example.ui.theme.ElectricMagenta
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

enum class RiggingAnchorType(val label: String, val color: Color) {
    LEFT_EYE("Mata Kiri", Color(0xFF00E5FF)),
    RIGHT_EYE("Mata Kanan", Color(0xFF00E5FF)),
    MOUTH("Mulut & LipSync", Color(0xFFFF2E93)),
    HEAD_PIVOT("Poros Leher/Kepala", Color(0xFF00E676))
}

/**
 * Interactive Live2D-style VTuber Canvas Renderer.
 * Supports BOTH:
 * 1) User's Custom Imported Character Photo (with multi-zone 2.5D head tilt, perspective warp,
 *    interactive animated eyelids/pupils/mouth overlay, and hair physics sway)
 * 2) Built-in Multi-Layer Procedural Live2D Anime Archetypes (Back hair, Body/Breathing chest,
 *    Neck, Face contour, Blush, Independent Eye sockets + Irises + Specular highlights + Eyelashes,
 *    Eyebrows, Multi-shape Mouth with tongue/teeth, Front bangs & Side hair strands with spring physics,
 *    and VTuber Accessories).
 *
 * Also renders an interactive Live2D Deformer Mesh Overlay & draggable Rigging Anchor Nodes when
 * `showRiggingMesh` is enabled.
 */
@Composable
fun Live2DAvatarCanvas(
    character: VtuberCharacterEntity,
    pose: Live2DPoseState,
    showRiggingMesh: Boolean = false,
    enableTouchPuppeteer: Boolean = true,
    selectedAnchor: RiggingAnchorType? = null,
    onAnchorSelected: (RiggingAnchorType) -> Unit = {},
    onAnchorMoved: (RiggingAnchorType, Float, Float) -> Unit = { _, _, _ -> },
    onPuppetDrag: (deltaYaw: Float, deltaPitch: Float) -> Unit = { _, _ -> },
    onCharacterTapped: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var customBitmap by remember(character.imageUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(character.imageUri) {
        val uriStr = character.imageUri
        if (!uriStr.isNullOrBlank()) {
            customBitmap = withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(uriStr)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                } catch (_: Exception) {
                    null
                }
            }
        } else {
            customBitmap = null
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "live2d_idle_physics")
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (3200 / character.breathingSpeed.coerceIn(0.4f, 2.5f)).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath_phase"
    )

    val idleBlinkWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_blink"
    )

    // Auto-blink pulse every ~4.2 seconds when not manually closed
    val autoBlinkFactor = if (!pose.faceDetected && idleBlinkWave in 92f..98f) {
        val t = (idleBlinkWave - 95f) / 3f
        abs(t).coerceIn(0.05f, 1f)
    } else {
        1f
    }

    val effectiveYaw = (pose.headYaw * character.headTiltSensitivity).coerceIn(-1.2f, 1.2f)
    val effectivePitch = (pose.headPitch * character.headTiltSensitivity).coerceIn(-1.2f, 1.2f)
    val effectiveRoll = (pose.headRoll * character.headTiltSensitivity).coerceIn(-1.2f, 1.2f)

    val preset = pose.expressionPreset
    val leftEyeOpen = if (preset == ExpressionPreset.WINK) {
        0.08f
    } else {
        (pose.leftEyeOpen * autoBlinkFactor * preset.eyeScale * character.eyeBlinkSensitivity)
            .coerceIn(0.05f, 1.25f)
    }
    val rightEyeOpen = (pose.rightEyeOpen * autoBlinkFactor * preset.eyeScale * character.eyeBlinkSensitivity)
        .coerceIn(0.05f, 1.25f)

    val mouthOpen = (pose.mouthOpen * character.mouthLipSyncGain).coerceIn(0.04f, 1.1f)
    val mouthForm = (pose.mouthForm * 0.5f + preset.mouthForm * 0.5f).coerceIn(-1f, 1f)
    val blushAlpha = maxOf(pose.cheekBlush, preset.blush).coerceIn(0f, 1f)

    val breathOffsetY = sin(breathPhase) * 6f
    val hairPendulum = sin(breathPhase + effectiveYaw * 1.6f) * 14f * character.hairSwayIntensity

    Box(
        modifier = modifier
            .pointerInput(showRiggingMesh, character, enableTouchPuppeteer) {
                if (showRiggingMesh) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val nx = offset.x / w
                            val ny = offset.y / h
                            val anchors = listOf(
                                RiggingAnchorType.LEFT_EYE to Offset(character.leftEyeX, character.leftEyeY),
                                RiggingAnchorType.RIGHT_EYE to Offset(character.rightEyeX, character.rightEyeY),
                                RiggingAnchorType.MOUTH to Offset(character.mouthX, character.mouthY),
                                RiggingAnchorType.HEAD_PIVOT to Offset(character.headPivotX, character.headPivotY)
                            )
                            val nearest = anchors.minByOrNull { (_, pos) ->
                                hypot(nx - pos.x, ny - pos.y)
                            }
                            if (nearest != null && hypot(nx - nearest.second.x, ny - nearest.second.y) < 0.16f) {
                                onAnchorSelected(nearest.first)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val active = selectedAnchor ?: RiggingAnchorType.MOUTH
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val newX = (change.position.x / w).coerceIn(0.15f, 0.85f)
                            val newY = (change.position.y / h).coerceIn(0.15f, 0.88f)
                            onAnchorMoved(active, newX, newY)
                        }
                    )
                } else if (enableTouchPuppeteer) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        onPuppetDrag(
                            (dragAmount.x / (w * 0.35f)),
                            (-dragAmount.y / (h * 0.35f))
                        )
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onCharacterTapped() })
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = min(w, h)

            // 1. Draw Stage Background (Cyber Grid, Chroma Green/Blue, Nebula, Sakura)
            drawStageBackground(character.bgPresetId, w, h, breathPhase)

            val loadedBitmap = customBitmap
            if (loadedBitmap != null) {
                // 2A. Custom Imported Photo Live2D Rigged Renderer
                drawCustomPhotoLive2D(
                    bitmap = loadedBitmap,
                    character = character,
                    w = w,
                    h = h,
                    minDim = minDim,
                    yaw = effectiveYaw,
                    pitch = effectivePitch,
                    roll = effectiveRoll,
                    leftEyeOpen = leftEyeOpen,
                    rightEyeOpen = rightEyeOpen,
                    mouthOpen = mouthOpen,
                    mouthForm = mouthForm,
                    blushAlpha = blushAlpha,
                    breathOffsetY = breathOffsetY,
                    hairPendulum = hairPendulum,
                    preset = preset
                )
            } else {
                // 2B. Procedural Multi-Layer Live2D Anime VTuber Renderer
                drawProceduralAnimeLive2D(
                    character = character,
                    w = w,
                    h = h,
                    minDim = minDim,
                    yaw = effectiveYaw,
                    pitch = effectivePitch,
                    roll = effectiveRoll,
                    leftEyeOpen = leftEyeOpen,
                    rightEyeOpen = rightEyeOpen,
                    mouthOpen = mouthOpen,
                    mouthForm = mouthForm,
                    blushAlpha = blushAlpha,
                    breathOffsetY = breathOffsetY,
                    hairPendulum = hairPendulum,
                    preset = preset
                )
            }

            // 3. If in Live2D Rigging Editor mode, draw Deformer Warp Mesh & Draggable Anchor Nodes
            if (showRiggingMesh) {
                drawLive2DRiggingMeshOverlay(
                    character = character,
                    w = w,
                    h = h,
                    yaw = effectiveYaw,
                    pitch = effectivePitch,
                    selectedAnchor = selectedAnchor
                )
            }
        }
    }
}

private fun DrawScope.drawStageBackground(bgPresetId: Int, w: Float, h: Float, phase: Float) {
    when (bgPresetId) {
        1 -> {
            // Chroma Green Screen for OBS / Streaming crop
            drawRect(color = Color(0xFF00FF00), size = Size(w, h))
        }
        2 -> {
            // Chroma Blue Screen for Streaming
            drawRect(color = Color(0xFF0047FF), size = Size(w, h))
        }
        3 -> {
            // Nebula Starlight Stage
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2B1654), Color(0xFF120D2B), Color(0xFF090A12)),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = maxOf(w, h) * 0.75f
                ),
                size = Size(w, h)
            )
            drawCyberGridLines(w, h, Color(0x28FF2E93), phase)
        }
        4 -> {
            // Sakura Night Studio
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2C122E), Color(0xFF161026), Color(0xFF0B0C14))
                ),
                size = Size(w, h)
            )
            drawCyberGridLines(w, h, Color(0x28FF80AB), phase)
        }
        else -> {
            // Default Cyber-Neon Studio Grid
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF192242), Color(0xFF0F1222), Color(0xFF080911)),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = maxOf(w, h) * 0.7f
                ),
                size = Size(w, h)
            )
            drawCyberGridLines(w, h, Color(0x2400E5FF), phase)
        }
    }
}

private fun DrawScope.drawCyberGridLines(w: Float, h: Float, gridColor: Color, phase: Float) {
    val step = 48f
    var x = 0f
    while (x <= w) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1f
        )
        x += step
    }
    var y = 0f
    while (y <= h) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f
        )
        y += step
    }
    // Glowing halo behind avatar
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(gridColor.copy(alpha = 0.25f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.45f),
            radius = min(w, h) * (0.38f + sin(phase) * 0.02f)
        ),
        radius = min(w, h) * 0.40f,
        center = Offset(w * 0.5f, h * 0.45f)
    )
}

private fun DrawScope.drawCustomPhotoLive2D(
    bitmap: ImageBitmap,
    character: VtuberCharacterEntity,
    w: Float,
    h: Float,
    minDim: Float,
    yaw: Float,
    pitch: Float,
    roll: Float,
    leftEyeOpen: Float,
    rightEyeOpen: Float,
    mouthOpen: Float,
    mouthForm: Float,
    blushAlpha: Float,
    breathOffsetY: Float,
    hairPendulum: Float,
    preset: ExpressionPreset
) {
    val cx = w * 0.5f + yaw * minDim * 0.06f
    val cy = h * 0.50f - pitch * minDim * 0.05f + breathOffsetY
    val cardSize = minDim * 0.78f

    val pivotX = w * character.headPivotX
    val pivotY = h * character.headPivotY

    // Multi-layer 2.5D parallax & spring deformation of the imported character photo
    withTransform({
        translate(left = yaw * 18f, top = -pitch * 14f + breathOffsetY)
        rotate(degrees = roll * 14f + yaw * 5f, pivot = Offset(pivotX, pivotY))
        scale(
            scaleX = 1f - abs(yaw) * 0.04f + sin(breathOffsetY * 0.1f) * 0.01f,
            scaleY = 1f - abs(pitch) * 0.03f,
            pivot = Offset(pivotX, pivotY)
        )
    }) {
        val dstLeft = (cx - cardSize * 0.5f).toInt()
        val dstTop = (cy - cardSize * 0.5f).toInt()
        val dstSize = cardSize.toInt()

        // Outer neon aura frame around imported character
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(character.hairColorHex), Color(character.eyeColorHex))
            ),
            topLeft = Offset(dstLeft - 6f, dstTop - 6f),
            size = Size(dstSize + 12f, dstSize + 12f),
            cornerRadius = CornerRadius(36f, 36f),
            style = Stroke(width = 4f)
        )

        val clipRounded = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = Rect(Offset(dstLeft.toFloat(), dstTop.toFloat()), Size(dstSize.toFloat(), dstSize.toFloat())),
                    cornerRadius = CornerRadius(32f, 32f)
                )
            )
        }

        clipPath(clipRounded) {
            drawImage(
                image = bitmap,
                dstOffset = androidx.compose.ui.unit.IntOffset(dstLeft, dstTop),
                dstSize = androidx.compose.ui.unit.IntSize(dstSize, dstSize)
            )
        }

        // Overlay Live2D rigged facial expressions at the user-configured anchor positions!
        val leftEyeCenter = Offset(w * character.leftEyeX + yaw * 8f, h * character.leftEyeY - pitch * 6f)
        val rightEyeCenter = Offset(w * character.rightEyeX + yaw * 8f, h * character.rightEyeY - pitch * 6f)
        val mouthCenter = Offset(w * character.mouthX + yaw * 6f, h * character.mouthY - pitch * 5f)
        val eyeRadius = minDim * 0.048f

        // Cheek blush overlay on photo
        if (blushAlpha > 0.1f) {
            drawOval(
                color = Color(0xFFFF4081).copy(alpha = blushAlpha * 0.45f),
                topLeft = Offset(leftEyeCenter.x - eyeRadius * 1.1f, leftEyeCenter.y + eyeRadius * 0.9f),
                size = Size(eyeRadius * 1.8f, eyeRadius * 0.85f)
            )
            drawOval(
                color = Color(0xFFFF4081).copy(alpha = blushAlpha * 0.45f),
                topLeft = Offset(rightEyeCenter.x - eyeRadius * 0.7f, rightEyeCenter.y + eyeRadius * 0.9f),
                size = Size(eyeRadius * 1.8f, eyeRadius * 0.85f)
            )
        }

        // Dynamic Live2D Eye Rigging on Photo
        drawRiggedAnimeEye(
            center = leftEyeCenter,
            radius = eyeRadius,
            openRatio = leftEyeOpen,
            gazeX = yaw,
            gazeY = -pitch,
            irisColor = Color(character.eyeColorHex),
            isLeft = true,
            starEye = preset == ExpressionPreset.STAR_EYES
        )
        drawRiggedAnimeEye(
            center = rightEyeCenter,
            radius = eyeRadius,
            openRatio = rightEyeOpen,
            gazeX = yaw,
            gazeY = -pitch,
            irisColor = Color(character.eyeColorHex),
            isLeft = false,
            starEye = preset == ExpressionPreset.STAR_EYES
        )

        // Dynamic Live2D Mouth & Lip-Sync on Photo
        drawRiggedAnimeMouth(
            center = mouthCenter,
            baseWidth = minDim * 0.085f,
            mouthOpen = mouthOpen,
            mouthForm = mouthForm
        )

        // Accessory Overlay on Photo
        drawVtuberAccessory(
            accessoryId = character.accessoryId,
            headTop = Offset(cx + hairPendulum * 0.25f, dstTop + cardSize * 0.12f),
            eyeMid = Offset((leftEyeCenter.x + rightEyeCenter.x) * 0.5f, (leftEyeCenter.y + rightEyeCenter.y) * 0.5f),
            headRadius = cardSize * 0.34f,
            accentColor = Color(character.outfitColorHex)
        )
    }
}

private fun DrawScope.drawProceduralAnimeLive2D(
    character: VtuberCharacterEntity,
    w: Float,
    h: Float,
    minDim: Float,
    yaw: Float,
    pitch: Float,
    roll: Float,
    leftEyeOpen: Float,
    rightEyeOpen: Float,
    mouthOpen: Float,
    mouthForm: Float,
    blushAlpha: Float,
    breathOffsetY: Float,
    hairPendulum: Float,
    preset: ExpressionPreset
) {
    val hairColor = Color(character.hairColorHex)
    val hairShadow = Color(
        red = (hairColor.red * 0.62f).coerceIn(0f, 1f),
        green = (hairColor.green * 0.62f).coerceIn(0f, 1f),
        blue = (hairColor.blue * 0.72f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val irisColor = Color(character.eyeColorHex)
    val outfitColor = Color(character.outfitColorHex)
    val skinColor = Color(0xFFFFF0E8)
    val skinShadow = Color(0xFFF5D5C8)

    val centerX = w * 0.5f
    val centerY = h * 0.48f + breathOffsetY
    val headRadius = minDim * 0.23f

    val headShiftX = yaw * headRadius * 0.24f
    val headShiftY = -pitch * headRadius * 0.18f

    // 1. Back Hair Layer (Parallax opposite to head turn + spring sway)
    withTransform({
        translate(left = -headShiftX * 0.45f + hairPendulum * 0.7f, top = -headShiftY * 0.3f)
        rotate(degrees = roll * 8f, pivot = Offset(centerX, centerY))
    }) {
        val backHairPath = Path().apply {
            moveTo(centerX - headRadius * 1.15f, centerY - headRadius * 0.4f)
            cubicTo(
                centerX - headRadius * 1.45f, centerY + headRadius * 0.9f,
                centerX - headRadius * 1.25f + hairPendulum, centerY + headRadius * 1.85f,
                centerX - headRadius * 0.75f + hairPendulum, centerY + headRadius * 2.15f
            )
            lineTo(centerX + headRadius * 0.75f + hairPendulum, centerY + headRadius * 2.15f)
            cubicTo(
                centerX + headRadius * 1.25f + hairPendulum, centerY + headRadius * 1.85f,
                centerX + headRadius * 1.45f, centerY + headRadius * 0.9f,
                centerX + headRadius * 1.15f, centerY - headRadius * 0.4f
            )
            close()
        }
        drawPath(
            path = backHairPath,
            brush = Brush.verticalGradient(listOf(hairColor, hairShadow))
        )
    }

    // 2. Torso / Idol Jacket & Neck Layer
    val shoulderY = centerY + headRadius * 1.08f
    val bodyPath = Path().apply {
        moveTo(centerX - headRadius * 1.05f, h)
        quadraticTo(
            centerX - headRadius * 0.95f, shoulderY + headRadius * 0.15f,
            centerX - headRadius * 0.35f, shoulderY
        )
        lineTo(centerX + headRadius * 0.35f, shoulderY)
        quadraticTo(
            centerX + headRadius * 0.95f, shoulderY + headRadius * 0.15f,
            centerX + headRadius * 1.05f, h
        )
        close()
    }
    drawPath(
        path = bodyPath,
        brush = Brush.verticalGradient(
            colors = listOf(outfitColor, Color(0xFF14172B))
        )
    )
    // Glowing Cyber Collar Trim
    drawLine(
        color = NeonCyan,
        start = Offset(centerX - headRadius * 0.45f, shoulderY + headRadius * 0.15f),
        end = Offset(centerX, shoulderY + headRadius * 0.55f),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = NeonCyan,
        start = Offset(centerX + headRadius * 0.45f, shoulderY + headRadius * 0.15f),
        end = Offset(centerX, shoulderY + headRadius * 0.55f),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )

    // Neck
    drawRoundRect(
        color = skinShadow,
        topLeft = Offset(centerX - headRadius * 0.22f + headShiftX * 0.3f, centerY + headRadius * 0.68f),
        size = Size(headRadius * 0.44f, headRadius * 0.52f),
        cornerRadius = CornerRadius(18f, 18f)
    )

    // 3. Head & Facial Rigging Layer (Rotates around Neck Pivot)
    val pivot = Offset(w * character.headPivotX, h * character.headPivotY)
    withTransform({
        translate(left = headShiftX, top = headShiftY)
        rotate(degrees = roll * 18f + yaw * 4f, pivot = pivot)
    }) {
        // Anime Face Contour with Chin
        val facePath = Path().apply {
            moveTo(centerX - headRadius * 0.92f, centerY - headRadius * 0.35f)
            cubicTo(
                centerX - headRadius * 0.96f, centerY + headRadius * 0.35f,
                centerX - headRadius * 0.55f, centerY + headRadius * 0.82f,
                centerX + yaw * 6f, centerY + headRadius * 0.95f
            )
            cubicTo(
                centerX + headRadius * 0.55f, centerY + headRadius * 0.82f,
                centerX + headRadius * 0.96f, centerY + headRadius * 0.35f,
                centerX + headRadius * 0.92f, centerY - headRadius * 0.35f
            )
            cubicTo(
                centerX + headRadius * 0.85f, centerY - headRadius * 1.05f,
                centerX - headRadius * 0.85f, centerY - headRadius * 1.05f,
                centerX - headRadius * 0.92f, centerY - headRadius * 0.35f
            )
            close()
        }
        drawPath(
            path = facePath,
            brush = Brush.verticalGradient(listOf(skinColor, skinShadow))
        )

        // Compute dynamic Eye & Mouth positions from character rigging anchors
        val leftEyePos = Offset(
            w * character.leftEyeX + yaw * headRadius * 0.14f,
            h * character.leftEyeY - pitch * headRadius * 0.10f
        )
        val rightEyePos = Offset(
            w * character.rightEyeX + yaw * headRadius * 0.14f,
            h * character.rightEyeY - pitch * headRadius * 0.10f
        )
        val mouthPos = Offset(
            w * character.mouthX + yaw * headRadius * 0.10f,
            h * character.mouthY - pitch * headRadius * 0.08f
        )
        val eyeRadius = headRadius * 0.24f

        // Cheek Blush + Anime Blush Lines
        if (blushAlpha > 0.05f) {
            val blushColor = Color(0xFFFF4D8D).copy(alpha = (blushAlpha * 0.55f).coerceIn(0f, 0.75f))
            drawOval(
                color = blushColor,
                topLeft = Offset(leftEyePos.x - eyeRadius * 1.15f, leftEyePos.y + eyeRadius * 0.95f),
                size = Size(eyeRadius * 1.9f, eyeRadius * 0.85f)
            )
            drawOval(
                color = blushColor,
                topLeft = Offset(rightEyePos.x - eyeRadius * 0.75f, rightEyePos.y + eyeRadius * 0.95f),
                size = Size(eyeRadius * 1.9f, eyeRadius * 0.85f)
            )
        }

        // Nose subtle anime shadow
        val noseX = (leftEyePos.x + rightEyePos.x) * 0.5f + yaw * 5f
        val noseY = (leftEyePos.y + mouthPos.y) * 0.5f
        drawLine(
            color = Color(0xFFD9A696),
            start = Offset(noseX, noseY - 5f),
            end = Offset(noseX - 3f, noseY + 4f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        // Left & Right Rigged Anime Eyes
        drawRiggedAnimeEye(
            center = leftEyePos,
            radius = eyeRadius,
            openRatio = leftEyeOpen,
            gazeX = yaw,
            gazeY = -pitch,
            irisColor = irisColor,
            isLeft = true,
            starEye = preset == ExpressionPreset.STAR_EYES
        )
        drawRiggedAnimeEye(
            center = rightEyePos,
            radius = eyeRadius,
            openRatio = rightEyeOpen,
            gazeX = yaw,
            gazeY = -pitch,
            irisColor = irisColor,
            isLeft = false,
            starEye = preset == ExpressionPreset.STAR_EYES
        )

        // Expressive Eyebrows
        val browOffset = preset.browOffset * 10f
        drawAnimeEyebrow(
            center = Offset(leftEyePos.x, leftEyePos.y - eyeRadius * 1.25f - browOffset),
            width = eyeRadius * 2.1f,
            angle = if (preset == ExpressionPreset.ANGRY) 16f else if (preset == ExpressionPreset.SHY) -14f else -4f,
            color = hairShadow
        )
        drawAnimeEyebrow(
            center = Offset(rightEyePos.x, rightEyePos.y - eyeRadius * 1.25f - browOffset),
            width = eyeRadius * 2.1f,
            angle = if (preset == ExpressionPreset.ANGRY) -16f else if (preset == ExpressionPreset.SHY) 14f else 4f,
            color = hairShadow
        )

        // Rigged Mouth with Lip-Sync
        drawRiggedAnimeMouth(
            center = mouthPos,
            baseWidth = headRadius * 0.36f,
            mouthOpen = mouthOpen,
            mouthForm = mouthForm
        )

        // 4. Front Hair Bangs & Side Physics Strands
        val bangsPath = Path().apply {
            val topY = centerY - headRadius * 1.08f
            moveTo(centerX - headRadius * 1.02f, centerY - headRadius * 0.1f)
            quadraticTo(
                centerX - headRadius * 0.95f, topY,
                centerX, topY - headRadius * 0.06f
            )
            quadraticTo(
                centerX + headRadius * 0.95f, topY,
                centerX + headRadius * 1.02f, centerY - headRadius * 0.1f
            )
            // Sculpted anime bangs peaks
            lineTo(centerX + headRadius * 0.72f + hairPendulum * 0.25f, centerY - headRadius * 0.18f)
            lineTo(centerX + headRadius * 0.42f + hairPendulum * 0.3f, centerY - headRadius * 0.48f)
            lineTo(centerX + headRadius * 0.12f + hairPendulum * 0.35f, centerY - headRadius * 0.12f)
            lineTo(centerX - headRadius * 0.18f + hairPendulum * 0.3f, centerY - headRadius * 0.50f)
            lineTo(centerX - headRadius * 0.52f + hairPendulum * 0.25f, centerY - headRadius * 0.16f)
            lineTo(centerX - headRadius * 0.80f + hairPendulum * 0.2f, centerY - headRadius * 0.42f)
            close()
        }
        drawPath(
            path = bangsPath,
            brush = Brush.verticalGradient(listOf(hairColor, hairShadow))
        )

        // Side Hair locks with spring physics
        val leftLockPath = Path().apply {
            moveTo(centerX - headRadius * 0.95f, centerY - headRadius * 0.35f)
            cubicTo(
                centerX - headRadius * 1.18f, centerY + headRadius * 0.35f,
                centerX - headRadius * 1.05f + hairPendulum * 0.8f, centerY + headRadius * 1.10f,
                centerX - headRadius * 0.72f + hairPendulum * 1.1f, centerY + headRadius * 1.45f
            )
            quadraticTo(
                centerX - headRadius * 0.78f + hairPendulum * 0.5f, centerY + headRadius * 0.55f,
                centerX - headRadius * 0.78f, centerY - headRadius * 0.20f
            )
            close()
        }
        drawPath(leftLockPath, brush = Brush.verticalGradient(listOf(hairColor, hairShadow)))

        val rightLockPath = Path().apply {
            moveTo(centerX + headRadius * 0.95f, centerY - headRadius * 0.35f)
            cubicTo(
                centerX + headRadius * 1.18f, centerY + headRadius * 0.35f,
                centerX + headRadius * 1.05f + hairPendulum * 0.8f, centerY + headRadius * 1.10f,
                centerX + headRadius * 0.72f + hairPendulum * 1.1f, centerY + headRadius * 1.45f
            )
            quadraticTo(
                centerX + headRadius * 0.78f + hairPendulum * 0.5f, centerY + headRadius * 0.55f,
                centerX + headRadius * 0.78f, centerY - headRadius * 0.20f
            )
            close()
        }
        drawPath(rightLockPath, brush = Brush.verticalGradient(listOf(hairColor, hairShadow)))

        // Hair Halo Highlight (Anime Angel Ring)
        drawArc(
            color = Color.White.copy(alpha = 0.42f),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(centerX - headRadius * 0.68f, centerY - headRadius * 0.82f),
            size = Size(headRadius * 1.36f, headRadius * 0.55f),
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )

        // 5. VTuber Accessory Layer
        drawVtuberAccessory(
            accessoryId = character.accessoryId,
            headTop = Offset(centerX, centerY - headRadius * 0.98f),
            eyeMid = Offset((leftEyePos.x + rightEyePos.x) * 0.5f, (leftEyePos.y + rightEyePos.y) * 0.5f),
            headRadius = headRadius,
            accentColor = outfitColor
        )
    }
}

private fun DrawScope.drawRiggedAnimeEye(
    center: Offset,
    radius: Float,
    openRatio: Float,
    gazeX: Float,
    gazeY: Float,
    irisColor: Color,
    isLeft: Boolean,
    starEye: Boolean
) {
    val clampedOpen = openRatio.coerceIn(0.05f, 1.25f)
    if (clampedOpen < 0.18f) {
        // Happy / Wink closed eye arc
        val closedPath = Path().apply {
            moveTo(center.x - radius, center.y)
            quadraticTo(center.x, center.y + radius * 0.45f, center.x + radius, center.y)
        }
        drawPath(
            path = closedPath,
            color = Color(0xFF1E192B),
            style = Stroke(width = 5.5f, cap = StrokeCap.Round)
        )
        return
    }

    val eyeH = radius * 1.55f * clampedOpen
    val eyeW = radius * 2.05f

    // Sclera (Eye White)
    drawOval(
        color = Color(0xFFFAFBFD),
        topLeft = Offset(center.x - eyeW * 0.5f, center.y - eyeH * 0.5f),
        size = Size(eyeW, eyeH)
    )

    // Iris + Pupil shifted by gaze
    val irisShiftX = gazeX * radius * 0.28f
    val irisShiftY = gazeY * radius * 0.22f
    val irisCenter = Offset(center.x + irisShiftX, center.y + irisShiftY)
    val irisW = radius * 1.28f
    val irisH = (radius * 1.38f * clampedOpen).coerceAtMost(eyeH * 0.92f)

    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1A142C), irisColor, irisColor.copy(alpha = 0.85f))
        ),
        topLeft = Offset(irisCenter.x - irisW * 0.5f, irisCenter.y - irisH * 0.5f),
        size = Size(irisW, irisH)
    )

    // Inner Dark Pupil
    val pupilR = irisW * 0.26f
    drawCircle(
        color = Color(0xFF120E24),
        radius = pupilR,
        center = irisCenter
    )

    // Specular Starlight Highlights
    drawCircle(
        color = Color.White,
        radius = radius * 0.24f,
        center = Offset(irisCenter.x - radius * 0.25f, irisCenter.y - radius * 0.25f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = radius * 0.12f,
        center = Offset(irisCenter.x + radius * 0.24f, irisCenter.y + radius * 0.22f)
    )

    if (starEye) {
        val starPath = Path().apply {
            val sx = irisCenter.x
            val sy = irisCenter.y
            val sr = radius * 0.34f
            moveTo(sx, sy - sr)
            lineTo(sx + sr * 0.28f, sy - sr * 0.28f)
            lineTo(sx + sr, sy)
            lineTo(sx + sr * 0.28f, sy + sr * 0.28f)
            lineTo(sx, sy + sr)
            lineTo(sx - sr * 0.28f, sy + sr * 0.28f)
            lineTo(sx - sr, sy)
            lineTo(sx - sr * 0.28f, sy - sr * 0.28f)
            close()
        }
        drawPath(starPath, color = Color(0xFFFFF59D))
    }

    // Thick Upper Eyelash Curve
    val lashPath = Path().apply {
        val wingDir = if (isLeft) -1f else 1f
        moveTo(center.x - eyeW * 0.54f, center.y - eyeH * 0.12f)
        quadraticTo(
            center.x, center.y - eyeH * 0.68f,
            center.x + eyeW * 0.54f, center.y - eyeH * 0.12f
        )
        lineTo(center.x + wingDir * eyeW * 0.64f, center.y - eyeH * 0.28f)
    }
    drawPath(
        path = lashPath,
        color = Color(0xFF191528),
        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawAnimeEyebrow(
    center: Offset,
    width: Float,
    angle: Float,
    color: Color
) {
    withTransform({
        rotate(degrees = angle, pivot = center)
    }) {
        val path = Path().apply {
            moveTo(center.x - width * 0.5f, center.y)
            quadraticTo(center.x, center.y - 6f, center.x + width * 0.5f, center.y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawRiggedAnimeMouth(
    center: Offset,
    baseWidth: Float,
    mouthOpen: Float,
    mouthForm: Float
) {
    val w = baseWidth * (0.9f + mouthForm * 0.22f)
    val openH = (baseWidth * 0.95f * mouthOpen).coerceIn(3f, baseWidth * 1.05f)

    if (mouthOpen < 0.14f) {
        // Closed smile / cat-mouth / frown curve
        val curveY = mouthForm * 12f
        val linePath = Path().apply {
            moveTo(center.x - w * 0.5f, center.y - curveY * 0.3f)
            quadraticTo(center.x, center.y + curveY, center.x + w * 0.5f, center.y - curveY * 0.3f)
        }
        drawPath(
            path = linePath,
            color = Color(0xFF5D2742),
            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
        )
    } else {
        // Open Rigged Anime Mouth with Cavity, Tongue, and Upper Teeth
        val mouthPath = Path().apply {
            moveTo(center.x - w * 0.5f, center.y - openH * 0.25f)
            quadraticTo(
                center.x, center.y - openH * 0.42f + mouthForm * 4f,
                center.x + w * 0.5f, center.y - openH * 0.25f
            )
            cubicTo(
                center.x + w * 0.46f, center.y + openH * 0.65f,
                center.x - w * 0.46f, center.y + openH * 0.65f,
                center.x - w * 0.5f, center.y - openH * 0.25f
            )
            close()
        }
        drawPath(mouthPath, color = Color(0xFF6B1D3B))

        clipPath(mouthPath) {
            // Tongue
            drawOval(
                color = Color(0xFFFF6E8E),
                topLeft = Offset(center.x - w * 0.38f, center.y + openH * 0.02f),
                size = Size(w * 0.76f, openH * 0.72f)
            )
            // Upper Teeth Bar
            drawRect(
                color = Color.White,
                topLeft = Offset(center.x - w * 0.42f, center.y - openH * 0.42f),
                size = Size(w * 0.84f, openH * 0.24f)
            )
        }

        drawPath(
            path = mouthPath,
            color = Color(0xFF3E1226),
            style = Stroke(width = 3.5f, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.drawVtuberAccessory(
    accessoryId: Int,
    headTop: Offset,
    eyeMid: Offset,
    headRadius: Float,
    accentColor: Color
) {
    when (accessoryId) {
        1 -> {
            // Cyber Cat Ears Headset
            val leftEar = Path().apply {
                moveTo(headTop.x - headRadius * 0.85f, headTop.y + headRadius * 0.32f)
                lineTo(headTop.x - headRadius * 0.98f, headTop.y - headRadius * 0.38f)
                lineTo(headTop.x - headRadius * 0.36f, headTop.y + headRadius * 0.06f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(headTop.x + headRadius * 0.85f, headTop.y + headRadius * 0.32f)
                lineTo(headTop.x + headRadius * 0.98f, headTop.y - headRadius * 0.38f)
                lineTo(headTop.x + headRadius * 0.36f, headTop.y + headRadius * 0.06f)
                close()
            }
            drawPath(leftEar, color = Color(0xFF1A1E36))
            drawPath(leftEar, color = NeonCyan, style = Stroke(width = 4f))
            drawPath(rightEar, color = Color(0xFF1A1E36))
            drawPath(rightEar, color = ElectricMagenta, style = Stroke(width = 4f))
        }
        2 -> {
            // Starlight Floating Halo
            drawOval(
                color = NeonCyan,
                topLeft = Offset(headTop.x - headRadius * 0.65f, headTop.y - headRadius * 0.28f),
                size = Size(headRadius * 1.3f, headRadius * 0.26f),
                style = Stroke(width = 5f)
            )
        }
        3 -> {
            // Futuristic Holo Visor Glasses
            val visorW = headRadius * 1.55f
            val visorH = headRadius * 0.36f
            drawRoundRect(
                color = accentColor.copy(alpha = 0.38f),
                topLeft = Offset(eyeMid.x - visorW * 0.5f, eyeMid.y - visorH * 0.5f),
                size = Size(visorW, visorH),
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = NeonCyan,
                topLeft = Offset(eyeMid.x - visorW * 0.5f, eyeMid.y - visorH * 0.5f),
                size = Size(visorW, visorH),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 3f)
            )
        }
        4 -> {
            // Neon Devil Horns
            val leftHorn = Path().apply {
                moveTo(headTop.x - headRadius * 0.55f, headTop.y + headRadius * 0.15f)
                quadraticTo(
                    headTop.x - headRadius * 0.85f, headTop.y - headRadius * 0.35f,
                    headTop.x - headRadius * 0.40f, headTop.y - headRadius * 0.45f
                )
                quadraticTo(
                    headTop.x - headRadius * 0.50f, headTop.y - headRadius * 0.10f,
                    headTop.x - headRadius * 0.30f, headTop.y + headRadius * 0.10f
                )
                close()
            }
            val rightHorn = Path().apply {
                moveTo(headTop.x + headRadius * 0.55f, headTop.y + headRadius * 0.15f)
                quadraticTo(
                    headTop.x + headRadius * 0.85f, headTop.y - headRadius * 0.35f,
                    headTop.x + headRadius * 0.40f, headTop.y - headRadius * 0.45f
                )
                quadraticTo(
                    headTop.x + headRadius * 0.50f, headTop.y - headRadius * 0.10f,
                    headTop.x + headRadius * 0.30f, headTop.y + headRadius * 0.10f
                )
                close()
            }
            drawPath(leftHorn, color = ElectricMagenta)
            drawPath(rightHorn, color = ElectricMagenta)
        }
    }
}

private fun DrawScope.drawLive2DRiggingMeshOverlay(
    character: VtuberCharacterEntity,
    w: Float,
    h: Float,
    yaw: Float,
    pitch: Float,
    selectedAnchor: RiggingAnchorType?
) {
    // Draw triangulated Live2D Warp Mesh Grid over character area
    val cols = 6
    val rows = 7
    val left = w * 0.16f
    val right = w * 0.84f
    val top = h * 0.12f
    val bottom = h * 0.86f

    val meshPoints = Array(rows) { r ->
        Array(cols) { c ->
            val fx = c.toFloat() / (cols - 1)
            val fy = r.toFloat() / (rows - 1)
            val centerFactor = 1f - hypot(fx - 0.5f, fy - 0.5f).coerceIn(0f, 0.7f)
            val x = left + fx * (right - left) + yaw * 24f * centerFactor
            val y = top + fy * (bottom - top) - pitch * 18f * centerFactor
            Offset(x, y)
        }
    }

    val meshColor = Color(0x6600E5FF)
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val p = meshPoints[r][c]
            if (c + 1 < cols) {
                drawLine(meshColor, p, meshPoints[r][c + 1], strokeWidth = 1.5f)
            }
            if (r + 1 < rows) {
                drawLine(meshColor, p, meshPoints[r + 1][c], strokeWidth = 1.5f)
            }
            if (c + 1 < cols && r + 1 < rows) {
                drawLine(Color(0x337C4DFF), p, meshPoints[r + 1][c + 1], strokeWidth = 1f)
            }
        }
    }

    // Draw bone links connecting Head Pivot to Mouth and Eyes
    val leftEyePt = Offset(w * character.leftEyeX, h * character.leftEyeY)
    val rightEyePt = Offset(w * character.rightEyeX, h * character.rightEyeY)
    val mouthPt = Offset(w * character.mouthX, h * character.mouthY)
    val pivotPt = Offset(w * character.headPivotX, h * character.headPivotY)

    val boneColor = Color(0xCCFF2E93)
    drawLine(boneColor, pivotPt, mouthPt, strokeWidth = 3f)
    drawLine(boneColor, mouthPt, leftEyePt, strokeWidth = 2.5f)
    drawLine(boneColor, mouthPt, rightEyePt, strokeWidth = 2.5f)
    drawLine(boneColor, leftEyePt, rightEyePt, strokeWidth = 2f)

    val anchors = listOf(
        RiggingAnchorType.LEFT_EYE to leftEyePt,
        RiggingAnchorType.RIGHT_EYE to rightEyePt,
        RiggingAnchorType.MOUTH to mouthPt,
        RiggingAnchorType.HEAD_PIVOT to pivotPt
    )

    anchors.forEach { (type, pt) ->
        val isSelected = type == selectedAnchor
        val outerR = if (isSelected) 22f else 15f
        drawCircle(
            color = type.color.copy(alpha = 0.28f),
            radius = outerR * 1.4f,
            center = pt
        )
        drawCircle(
            color = Color(0xFF0A0B12),
            radius = outerR,
            center = pt
        )
        drawCircle(
            color = type.color,
            radius = outerR,
            center = pt,
            style = Stroke(width = if (isSelected) 4.5f else 3f)
        )
        drawCircle(
            color = Color.White,
            radius = 5f,
            center = pt
        )
    }
}
