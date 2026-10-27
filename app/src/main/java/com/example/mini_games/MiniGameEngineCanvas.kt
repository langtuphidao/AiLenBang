package com.example.mini_games

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.models.LockedSelectionTurn
import com.example.models.MiniGameEngineType
import com.example.models.MiniGameRunState
import com.example.models.StudentEntity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Bộ dựng đồ họa 2D Canvas cho toàn bộ 30 Mini-Game.
 * Mỗi mini-game có bối cảnh, nhân vật 2D và hiệu ứng riêng biệt theo `lockedTurn.miniGame.id`,
 * kết hợp hệ thống chống tràn chữ & chống đè nhãn (`drawClampedNamePill`).
 */
@Composable
fun MiniGameEngineCanvas(
    lockedTurn: LockedSelectionTurn,
    runState: MiniGameRunState,
    progress: Float, // 0f..1f
    idleTick: Float,
    presentationMode: Boolean,
    onDirectTapStudent: (StudentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val participants = remember(lockedTurn.turnId, lockedTurn.miniGame.id) {
        lockedTurn.visualParticipants.ifEmpty { lockedTurn.winners }
    }
    val primaryWinner = lockedTurn.winners.firstOrNull() ?: participants.firstOrNull()
    val winnerIndex = remember(lockedTurn.turnId, lockedTurn.miniGame.id, participants) {
        participants.indexOfFirst { it.id == primaryWinner?.id }.coerceAtLeast(0)
    }

    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    val badgePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(7, 59, 76)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(lockedTurn.turnId, lockedTurn.miniGame.id, runState) {
                if (lockedTurn.miniGame.supportsDirectTapPick && participants.isNotEmpty()) {
                    detectTapGestures { tapOffset ->
                        val count = participants.size
                        for (i in 0 until count) {
                            val pos = computeButterflyPosition(
                                index = i,
                                count = count,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                                time = if (runState == MiniGameRunState.RUNNING) progress * 6f else idleTick
                            )
                            if (hypot(tapOffset.x - pos.x, tapOffset.y - pos.y) <= 84f) {
                                onDirectTapStudent(participants[i])
                                break
                            }
                        }
                    }
                }
            }
    ) {
        val minDim = min(size.width, size.height)
        val scaleBoost = if (presentationMode) 1.14f else 1.0f
        val adaptiveBadgeSize = (minDim * 0.036f * scaleBoost).coerceIn(20f, 34f)
        val adaptiveWheelTextSize = (minDim * 0.033f * scaleBoost).coerceIn(18f, 30f)

        badgePaint.textSize = adaptiveBadgeSize
        textPaint.textSize = adaptiveWheelTextSize

        val effectiveProgress = if (runState == MiniGameRunState.IDLE) 0f else progress

        when (lockedTurn.miniGame.engineType) {
            MiniGameEngineType.DUCK_RACE -> drawRaceArena(
                lockedTurn = lockedTurn,
                participants = participants,
                winnerIndex = winnerIndex,
                progress = effectiveProgress,
                idleTick = idleTick,
                badgePaint = badgePaint
            )
            MiniGameEngineType.UFO_ABDUCTION -> drawUfoOrRobotArena(
                gameId = lockedTurn.miniGame.id,
                participants = participants,
                winnerIndex = winnerIndex,
                progress = effectiveProgress,
                idleTick = idleTick,
                badgePaint = badgePaint
            )
            MiniGameEngineType.BUTTERFLY_NET -> drawAerialCatchArena(
                gameId = lockedTurn.miniGame.id,
                participants = participants,
                winnerIndex = winnerIndex,
                runState = runState,
                progress = effectiveProgress,
                idleTick = idleTick,
                badgePaint = badgePaint
            )
            MiniGameEngineType.LUCKY_WHEEL -> drawWheelOrBottleArena(
                gameId = lockedTurn.miniGame.id,
                participants = participants,
                winnerIndex = winnerIndex,
                progress = effectiveProgress,
                idleTick = idleTick,
                textPaint = textPaint,
                badgePaint = badgePaint
            )
            MiniGameEngineType.ROCKET_LAUNCH -> drawLaunchOrSportsArena(
                gameId = lockedTurn.miniGame.id,
                participants = participants,
                winnerIndex = winnerIndex,
                progress = effectiveProgress,
                idleTick = idleTick,
                badgePaint = badgePaint
            )
            MiniGameEngineType.STAGE_SPOTLIGHT -> drawStageOrScannerArena(
                gameId = lockedTurn.miniGame.id,
                participants = participants,
                winnerIndex = winnerIndex,
                progress = effectiveProgress,
                idleTick = idleTick,
                badgePaint = badgePaint
            )
            MiniGameEngineType.TREASURE_CHEST -> drawMysteryRevealArena(
                gameId = lockedTurn.miniGame.id,
                participants = participants,
                winnerIndex = winnerIndex,
                progress = effectiveProgress,
                idleTick = idleTick,
                badgePaint = badgePaint
            )
            MiniGameEngineType.CLAW_MACHINE -> drawClawOrFishingArena(
                gameId = lockedTurn.miniGame.id,
                participants = participants,
                winnerIndex = winnerIndex,
                progress = effectiveProgress,
                idleTick = idleTick,
                badgePaint = badgePaint
            )
        }
    }
}

private fun shortenStudentName(fullName: String, maxChars: Int = 16): String {
    val clean = fullName.trim()
    if (clean.length <= maxChars) return clean
    val parts = clean.split(" ").filter { it.isNotEmpty() }
    if (parts.size >= 3) {
        val givenAndMiddle = parts.takeLast(2).joinToString(" ")
        val initials = parts.dropLast(2).joinToString(".") { it.take(1).uppercase() } + "."
        val compact = "$initials $givenAndMiddle"
        if (compact.length <= maxChars) return compact
        if (givenAndMiddle.length <= maxChars) return givenAndMiddle
    }
    return clean.take((maxChars - 1).coerceAtLeast(4)) + "…"
}

/**
 * Vẽ nhãn tên pill có giới hạn chiều rộng tối đa và luôn kẹp trong khung nhìn [minX..maxX]
 * để không bao giờ bị tràn ra ngoài màn hình hoặc đè lên nhân vật.
 */
private fun DrawScope.drawClampedNamePill(
    text: String,
    centerX: Float,
    centerY: Float,
    bgColor: Color,
    paint: Paint,
    maxPillWidth: Float = size.width * 0.44f
) {
    val fontMetrics = paint.fontMetrics
    val textHeight = fontMetrics.descent - fontMetrics.ascent
    val horizontalPad = 22f
    val verticalPad = 12f

    var displayStr = text
    while (displayStr.length > 5 && paint.measureText(displayStr) + horizontalPad > maxPillWidth) {
        displayStr = displayStr.dropLast(2) + "…"
    }

    val pillW = (paint.measureText(displayStr) + horizontalPad).coerceAtMost(maxPillWidth)
    val pillH = textHeight + verticalPad
    val clampedCenterX = centerX.coerceIn(pillW / 2f + 8f, size.width - pillW / 2f - 8f)
    val clampedCenterY = centerY.coerceIn(pillH / 2f + 6f, size.height - pillH / 2f - 6f)

    drawRoundRect(
        color = bgColor,
        topLeft = Offset(clampedCenterX - pillW / 2f, clampedCenterY - pillH / 2f),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
    )

    val baselineY = clampedCenterY - (fontMetrics.ascent + fontMetrics.descent) / 2f
    drawContext.canvas.nativeCanvas.drawText(
        displayStr,
        clampedCenterX,
        baselineY,
        paint
    )
}

/**
 * 1. NHÓM ĐUA TỐC ĐỘ 2D:
 * - duck_race (#1): Đua vịt vàng trên sông xanh
 * - frog_lotus (#2): Ếch xanh nhảy qua các lá sen tới bông sen vàng
 * - penguin_slide (#3): Chim cánh cụt trượt băng Bắc Cực
 * - rabbit_carrot (#4): Thỏ trắng nhảy đồng cỏ tới củ cà rốt khổng lồ
 * - turtle_finish (#5): Rùa biển bò trên bãi cát vàng ra biển
 * - pirate_ship (#19): Tàu cướp biển căng buồm vượt đại dương tới đảo kho báu
 */
private fun DrawScope.drawRaceArena(
    lockedTurn: LockedSelectionTurn,
    participants: List<StudentEntity>,
    winnerIndex: Int,
    progress: Float,
    idleTick: Float,
    badgePaint: Paint
) {
    val w = size.width
    val h = size.height
    val gameId = lockedTurn.miniGame.id

    val bgColors = when (gameId) {
        "frog_lotus" -> listOf(Color(0xFF0A4D46), Color(0xFF126E63), Color(0xFF1B8A7B))
        "penguin_slide" -> listOf(Color(0xFFCAF0F8), Color(0xFF90E0EF), Color(0xFF48CAE4))
        "rabbit_carrot" -> listOf(Color(0xFF2D6A4F), Color(0xFF40916C), Color(0xFF52B788))
        "turtle_finish" -> listOf(Color(0xFFD4A373), Color(0xFFE9C46A), Color(0xFF2A9D8F))
        "pirate_ship" -> listOf(Color(0xFF03045E), Color(0xFF023E8A), Color(0xFF0077B6))
        else -> listOf(Color(0xFF0077B6), Color(0xFF0096C7), Color(0xFF00B4D8))
    }

    drawRect(brush = Brush.verticalGradient(colors = bgColors))

    val count = participants.size.coerceAtLeast(1)
    val topMargin = h * 0.05f
    val bottomMargin = h * 0.05f
    val trackHeight = (h - topMargin - bottomMargin) / count

    val laneFontSize = (trackHeight * 0.42f).coerceIn(17f, 28f)
    badgePaint.textSize = laneFontSize

    val startX = w * 0.10f
    val finishX = w * 0.88f

    // Vạch xuất phát
    drawLine(
        color = Color.White.copy(alpha = 0.65f),
        start = Offset(startX, topMargin),
        end = Offset(startX, h - bottomMargin),
        strokeWidth = 5f
    )

    // Vạch đích tùy biến theo từng trò chơi
    when (gameId) {
        "frog_lotus" -> {
            // Đích đến là hàng bông sen vàng nở rộ
            for (i in 0 until count) {
                val cy = topMargin + (i + 0.5f) * trackHeight
                drawCircle(color = Color(0xFF52B788), radius = 18f, center = Offset(finishX + 14f, cy))
                drawCircle(color = Color(0xFFFFB703), radius = 11f, center = Offset(finishX + 14f, cy))
                drawCircle(color = Color(0xFFFB8500), radius = 5f, center = Offset(finishX + 14f, cy))
            }
        }
        "rabbit_carrot" -> {
            // Đích đến là hàng củ cà rốt cam tươi
            for (i in 0 until count) {
                val cy = topMargin + (i + 0.5f) * trackHeight
                val carrotPath = Path().apply {
                    moveTo(finishX, cy - 10f)
                    lineTo(finishX + 30f, cy)
                    lineTo(finishX, cy + 10f)
                    close()
                }
                drawPath(path = carrotPath, color = Color(0xFFFB5607))
                drawCircle(color = Color(0xFF2B9348), radius = 7f, center = Offset(finishX - 4f, cy))
            }
        }
        "pirate_ship" -> {
            // Đích đến là Đảo Kho Báu vàng
            drawRoundRect(
                color = Color(0xFFFFD166),
                topLeft = Offset(finishX, topMargin),
                size = Size(34f, h - topMargin - bottomMargin),
                cornerRadius = CornerRadius(16f, 16f)
            )
            for (i in 0 until count) {
                val cy = topMargin + (i + 0.5f) * trackHeight
                drawCircle(color = Color(0xFFD90429), radius = 8f, center = Offset(finishX + 17f, cy))
            }
        }
        else -> {
            val checkerCount = 16
            val checkerH = (h - topMargin - bottomMargin) / checkerCount
            for (c in 0 until checkerCount) {
                drawRect(
                    color = if (c % 2 == 0) Color.White else Color(0xFFD90429),
                    topLeft = Offset(finishX, topMargin + c * checkerH),
                    size = Size(16f, checkerH)
                )
                drawRect(
                    color = if (c % 2 == 1) Color.White else Color(0xFFD90429),
                    topLeft = Offset(finishX + 16f, topMargin + c * checkerH),
                    size = Size(16f, checkerH)
                )
            }
        }
    }

    val winnerSet = lockedTurn.winners.map { it.id }.toSet()
    val spriteScale = (trackHeight / 56f).coerceIn(0.55f, 1.15f)

    for (i in 0 until count) {
        val laneY = topMargin + i * trackHeight + trackHeight * 0.5f
        if (i > 0) {
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(12f, topMargin + i * trackHeight),
                end = Offset(w - 12f, topMargin + i * trackHeight),
                strokeWidth = 2f
            )
        }

        // Vẽ các lá sen dọc đường cho game Ếch nhảy ao sen
        if (gameId == "frog_lotus") {
            for (padIdx in 1..4) {
                val padX = startX + (finishX - startX) * (padIdx / 5f)
                drawOval(
                    color = Color(0xFF40916C).copy(alpha = 0.55f),
                    topLeft = Offset(padX - 18f * spriteScale, laneY + 4f * spriteScale),
                    size = Size(36f * spriteScale, 14f * spriteScale)
                )
            }
        }

        val student = participants[i]
        val isWinner = winnerSet.contains(student.id) || i == winnerIndex

        val seedPhase = (i * 1.73f) + 0.5f
        val surgeWave = sin(progress * PI.toFloat() * 3f + seedPhase) * 0.08f * (1f - progress)
        val finalRatio = if (isWinner) 1.0f else (0.70f + (i % 4) * 0.055f)
        val currentRatio = if (progress <= 0.001f) {
            0f
        } else {
            val base = progress * finalRatio + surgeWave
            if (isWinner && progress > 0.82f) {
                val t = (progress - 0.82f) / 0.18f
                (base * (1f - t) + 1.0f * t).coerceIn(0f, 1.01f)
            } else {
                base.coerceIn(0f, if (isWinner) 1.0f else 0.90f)
            }
        }

        val xPos = startX + (finishX - startX) * currentRatio
        val hopBounce = when (gameId) {
            "frog_lotus", "rabbit_carrot" -> -abs(sin((idleTick * 6f + progress * 18f) + i)) * (14f * spriteScale)
            else -> sin((idleTick * 5f) + i) * (3.5f * spriteScale)
        }
        val cy = laneY + hopBounce
        val bodyW = 46f * spriteScale

        // Vẽ nhân vật riêng biệt cho từng trò chơi
        when (gameId) {
            "frog_lotus" -> {
                // Chú ếch xanh lá với 2 mắt to tròn
                drawOval(
                    color = if (isWinner && progress > 0.88f) Color(0xFF80ED99) else Color(0xFF38B000),
                    topLeft = Offset(xPos - 22f * spriteScale, cy - 14f * spriteScale),
                    size = Size(44f * spriteScale, 28f * spriteScale)
                )
                drawCircle(color = Color.White, radius = 7f * spriteScale, center = Offset(xPos + 8f * spriteScale, cy - 14f * spriteScale))
                drawCircle(color = Color(0xFF073B4C), radius = 3.5f * spriteScale, center = Offset(xPos + 9f * spriteScale, cy - 14f * spriteScale))
            }
            "penguin_slide" -> {
                // Chim cánh cụt trượt băng bụng trắng lưng đen & khăn quàng đỏ
                drawOval(
                    color = Color(0xFF1D3557),
                    topLeft = Offset(xPos - 24f * spriteScale, cy - 13f * spriteScale),
                    size = Size(48f * spriteScale, 26f * spriteScale)
                )
                drawOval(
                    color = Color.White,
                    topLeft = Offset(xPos - 16f * spriteScale, cy - 6f * spriteScale),
                    size = Size(32f * spriteScale, 16f * spriteScale)
                )
                drawRoundRect(
                    color = Color(0xFFE63946),
                    topLeft = Offset(xPos + 4f * spriteScale, cy - 12f * spriteScale),
                    size = Size(8f * spriteScale, 20f * spriteScale),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawRoundRect(
                    color = Color(0xFFFFB703),
                    topLeft = Offset(xPos + 22f * spriteScale, cy - 5f * spriteScale),
                    size = Size(10f * spriteScale, 6f * spriteScale),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
            "rabbit_carrot" -> {
                // Chú thỏ trắng tai dài hồng
                drawCircle(
                    color = Color.White,
                    radius = 7f * spriteScale,
                    center = Offset(xPos - 20f * spriteScale, cy + 2f * spriteScale)
                )
                drawOval(
                    color = Color.White,
                    topLeft = Offset(xPos - 18f * spriteScale, cy - 12f * spriteScale),
                    size = Size(38f * spriteScale, 24f * spriteScale)
                )
                drawOval(
                    color = Color.White,
                    topLeft = Offset(xPos + 4f * spriteScale, cy - 30f * spriteScale),
                    size = Size(10f * spriteScale, 22f * spriteScale)
                )
                drawOval(
                    color = Color(0xFFFF758F),
                    topLeft = Offset(xPos + 6f * spriteScale, cy - 27f * spriteScale),
                    size = Size(6f * spriteScale, 16f * spriteScale)
                )
                drawCircle(color = Color(0xFFFF4D6D), radius = 3.5f * spriteScale, center = Offset(xPos + 16f * spriteScale, cy - 4f * spriteScale))
            }
            "turtle_finish" -> {
                // Chú rùa mai xanh lục đậm & đầu xanh nhạt
                drawCircle(
                    color = Color(0xFF95D5B2),
                    radius = 9f * spriteScale,
                    center = Offset(xPos + 22f * spriteScale, cy)
                )
                drawOval(
                    color = Color(0xFF2D6A4F),
                    topLeft = Offset(xPos - 22f * spriteScale, cy - 15f * spriteScale),
                    size = Size(40f * spriteScale, 30f * spriteScale)
                )
                drawOval(
                    color = Color(0xFF52B788),
                    topLeft = Offset(xPos - 15f * spriteScale, cy - 10f * spriteScale),
                    size = Size(26f * spriteScale, 20f * spriteScale)
                )
            }
            "pirate_ship" -> {
                // Tàu cướp biển có thân gỗ và cánh buồm trắng
                val hullPath = Path().apply {
                    moveTo(xPos - 24f * spriteScale, cy)
                    lineTo(xPos + 26f * spriteScale, cy)
                    lineTo(xPos + 16f * spriteScale, cy + 14f * spriteScale)
                    lineTo(xPos - 18f * spriteScale, cy + 14f * spriteScale)
                    close()
                }
                drawPath(path = hullPath, color = Color(0xFF7F4F24))
                val sailPath = Path().apply {
                    moveTo(xPos, cy - 24f * spriteScale)
                    lineTo(xPos + 20f * spriteScale, cy - 2f * spriteScale)
                    lineTo(xPos - 12f * spriteScale, cy - 2f * spriteScale)
                    close()
                }
                drawPath(path = sailPath, color = Color.White)
                drawRect(
                    color = Color(0xFFD90429),
                    topLeft = Offset(xPos - 8f * spriteScale, cy - 28f * spriteScale),
                    size = Size(12f * spriteScale, 6f * spriteScale)
                )
            }
            else -> {
                // Chú vịt vàng (duck_race)
                val bodyH = 30f * spriteScale
                val bodyColor = if (isWinner && progress > 0.88f) Color(0xFFFFD166) else Color(0xFFFFB703)
                drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(xPos - bodyW / 2f, cy - bodyH / 2f),
                    size = Size(bodyW, bodyH),
                    cornerRadius = CornerRadius(bodyH / 2f, bodyH / 2f)
                )
                drawCircle(
                    color = Color(0xFFFFD166),
                    radius = 13f * spriteScale,
                    center = Offset(xPos + bodyW * 0.32f, cy - bodyH * 0.35f)
                )
                drawRoundRect(
                    color = Color(0xFFFB5607),
                    topLeft = Offset(xPos + bodyW * 0.50f, cy - bodyH * 0.45f),
                    size = Size(12f * spriteScale, 7f * spriteScale),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
        }

        val label = "${i + 1}. ${shortenStudentName(student.fullName, 15)}"
        val maxLabelW = (w * 0.38f).coerceAtLeast(130f)
        val estWidth = (badgePaint.measureText(label) + 24f).coerceAtMost(maxLabelW)
        val labelCenterX = if (xPos < w * 0.45f) {
            xPos + bodyW * 0.8f + estWidth / 2f + 10f
        } else {
            xPos - bodyW * 0.6f - estWidth / 2f - 10f
        }

        drawClampedNamePill(
            text = label,
            centerX = labelCenterX,
            centerY = laneY,
            bgColor = if (isWinner && progress > 0.88f) Color(0xFFFFD166) else Color.White.copy(alpha = 0.94f),
            paint = badgePaint,
            maxPillWidth = maxLabelW
        )
    }
}

/**
 * 2. NHÓM QUÉT TỪ TRÊN CAO:
 * - ufo_abduction (#7): Đĩa bay UFO chiếu tia sáng hút học sinh lên tàu
 * - robot_scanner (#9): Robot khổng lồ chiếu 2 mắt tia hồng ngoại khóa mục tiêu
 */
private fun DrawScope.drawUfoOrRobotArena(
    gameId: String,
    participants: List<StudentEntity>,
    winnerIndex: Int,
    progress: Float,
    idleTick: Float,
    badgePaint: Paint
) {
    val w = size.width
    val h = size.height
    val isRobot = gameId == "robot_scanner"

    drawRect(
        brush = Brush.verticalGradient(
            colors = if (isRobot) {
                listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF3A506B))
            } else {
                listOf(Color(0xFF10002B), Color(0xFF240046), Color(0xFF3C096C))
            }
        )
    )
    drawRect(
        color = if (isRobot) Color(0xFF1F2833) else Color(0xFF1B4332),
        topLeft = Offset(0f, h * 0.72f),
        size = Size(w, h * 0.28f)
    )

    val count = participants.size.coerceAtLeast(1)
    val slotWidth = (w * 0.86f) / count
    val baseStartX = w * 0.07f + slotWidth * 0.5f
    val groundY = h * 0.76f
    val winnerX = baseStartX + winnerIndex * slotWidth

    val scannerX = when {
        progress <= 0.001f -> w * 0.5f + sin(idleTick * 2f) * (w * 0.25f)
        progress < 0.65f -> {
            val t = progress / 0.65f
            val sweep = sin(t * PI.toFloat() * 3.5f) * (w * 0.36f)
            (w * 0.5f + sweep).coerceIn(w * 0.12f, w * 0.88f)
        }
        else -> {
            val t = ((progress - 0.65f) / 0.35f).coerceIn(0f, 1f)
            val fromX = w * 0.5f + sin(PI.toFloat() * 3.5f) * (w * 0.36f)
            fromX + (winnerX - fromX) * t
        }
    }
    val topY = h * 0.17f + sin(idleTick * 4f) * 6f

    if (progress > 0.01f) {
        val beamColor = if (progress > 0.68f) Color(0xFF06D6A0) else if (isRobot) Color(0xFFFF006E) else Color(0xFF4CC9F0)
        val beamPath = Path().apply {
            moveTo(scannerX - 32f, topY + 20f)
            lineTo(scannerX + 32f, topY + 20f)
            lineTo(scannerX + 105f, groundY + 30f)
            lineTo(scannerX - 105f, groundY + 30f)
            close()
        }
        drawPath(
            path = beamPath,
            brush = Brush.verticalGradient(
                colors = listOf(beamColor.copy(alpha = 0.68f), beamColor.copy(alpha = 0.10f))
            )
        )
    }

    val drawOrder = participants.indices.sortedBy { if (it == winnerIndex) 1 else 0 }
    for (i in drawOrder) {
        val student = participants[i]
        val x = baseStartX + i * slotWidth
        val isLockedWinner = (i == winnerIndex && progress > 0.68f)
        val liftProgress = if (isLockedWinner && !isRobot) ((progress - 0.68f) / 0.32f).coerceIn(0f, 1f) else 0f
        val rowOffset = if (!isLockedWinner && i % 2 == 1) 16f else 0f
        val y = (groundY + rowOffset) - liftProgress * (groundY - (topY + 48f))

        val avatarColor = if (isLockedWinner) Color(0xFFFFD166) else Color(0xFF48CAE4)
        drawCircle(
            color = avatarColor,
            radius = if (isLockedWinner) 22f else 16f,
            center = Offset(x, y - 28f)
        )
        drawRoundRect(
            color = avatarColor,
            topLeft = Offset(x - 14f, y - 10f),
            size = Size(28f, 26f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        if (isRobot && isLockedWinner) {
            drawCircle(
                color = Color(0xFF06D6A0),
                radius = 40f,
                center = Offset(x, y - 16f),
                style = Stroke(width = 5f)
            )
        }

        val tier = i % 3
        val labelY = if (isLockedWinner) {
            y - 62f
        } else {
            h * 0.81f + tier * (h * 0.062f)
        }
        val maxAllowedWidth = if (isLockedWinner) w * 0.65f else (slotWidth * 2.6f).coerceIn(110f, w * 0.42f)
        drawClampedNamePill(
            text = shortenStudentName(student.fullName, if (isLockedWinner) 20 else 12),
            centerX = x,
            centerY = labelY,
            bgColor = if (isLockedWinner) Color(0xFFFFD166) else Color.White.copy(alpha = 0.92f),
            paint = badgePaint,
            maxPillWidth = maxAllowedWidth
        )
    }

    if (isRobot) {
        // Vẽ Đầu Robot AI khổng lồ với 2 mắt laser và ăng-ten
        drawLine(
            color = Color(0xFF4CC9F0),
            start = Offset(scannerX, topY - 56f),
            end = Offset(scannerX, topY - 34f),
            strokeWidth = 6f
        )
        drawCircle(color = Color(0xFFFF006E), radius = 9f, center = Offset(scannerX, topY - 60f))
        drawRoundRect(
            color = Color(0xFF4361EE),
            topLeft = Offset(scannerX - 66f, topY - 34f),
            size = Size(132f, 68f),
            cornerRadius = CornerRadius(18f, 18f)
        )
        drawRoundRect(
            color = Color(0xFF073B4C),
            topLeft = Offset(scannerX - 50f, topY - 20f),
            size = Size(100f, 32f),
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawCircle(color = Color(0xFFFFBE0B), radius = 10f, center = Offset(scannerX - 24f, topY - 4f))
        drawCircle(color = Color(0xFFFFBE0B), radius = 10f, center = Offset(scannerX + 24f, topY - 4f))
    } else {
        // Đĩa bay UFO
        drawOval(
            color = Color(0xFF4CC9F0),
            topLeft = Offset(scannerX - 48f, topY - 34f),
            size = Size(96f, 46f)
        )
        drawOval(
            color = Color(0xFFB5179E),
            topLeft = Offset(scannerX - 84f, topY - 10f),
            size = Size(168f, 38f)
        )
        for (lightIdx in -2..2) {
            drawCircle(
                color = Color(0xFFFFD166),
                radius = 6f,
                center = Offset(scannerX + lightIdx * 28f, topY + 9f)
            )
        }
    }
}

/**
 * 3. NHÓM BAY LƯỢN TRÊN KHÔNG:
 * - butterfly_net (#6): Vợt bắt bướm trong vườn hoa
 * - zero_gravity (#11): Phi hành gia bay lơ lửng ngoài vũ trụ
 * - balloon_pop (#15): Những quả bóng bay đầy màu sắc phát nổ tung pháo giấy
 */
private fun computeButterflyPosition(
    index: Int,
    count: Int,
    width: Float,
    height: Float,
    time: Float
): Offset {
    val isPortrait = height > width
    val cols = when {
        count <= 4 -> 2
        isPortrait -> 2
        count <= 9 -> 3
        else -> 4
    }
    val rows = ((count + cols - 1) / cols).coerceAtLeast(1)
    val col = index % cols
    val row = index / cols

    val cellW = (width * 0.88f) / cols
    val cellH = (height * 0.82f) / rows
    val baseX = width * 0.06f + cellW * (col + 0.5f)
    val baseY = height * 0.08f + cellH * (row + 0.45f)

    val orbitX = sin(time * 2.0f + index * 1.3f) * (cellW * 0.14f)
    val orbitY = cos(time * 1.6f + index * 1.7f) * (cellH * 0.14f)
    return Offset(baseX + orbitX, baseY + orbitY)
}

private fun DrawScope.drawAerialCatchArena(
    gameId: String,
    participants: List<StudentEntity>,
    winnerIndex: Int,
    runState: MiniGameRunState,
    progress: Float,
    idleTick: Float,
    badgePaint: Paint
) {
    val w = size.width
    val h = size.height
    val isPortrait = h > w

    val bgBrush = when (gameId) {
        "zero_gravity" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF03045E), Color(0xFF10002B), Color(0xFF240046))
        )
        "balloon_pop" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF48CAE4), Color(0xFF90E0EF), Color(0xFFADE8F4))
        )
        else -> Brush.verticalGradient(
            colors = listOf(Color(0xFF90E0EF), Color(0xFFCAF0F8), Color(0xFFB7E4C7))
        )
    }
    drawRect(brush = bgBrush)

    if (gameId == "zero_gravity") {
        // Vẽ bầu trời sao vũ trụ
        for (s in 0 until 24) {
            val sx = (w * ((s * 41 % 100) / 100f))
            val sy = (h * ((s * 67 % 100) / 100f))
            drawCircle(color = Color.White.copy(alpha = 0.6f), radius = 3f, center = Offset(sx, sy))
        }
    }

    val count = participants.size.coerceAtLeast(1)
    val cols = if (count <= 4 || isPortrait) 2 else if (count <= 9) 3 else 4
    val maxPillW = ((w * 0.86f) / cols) * 0.92f
    val animTime = if (runState == MiniGameRunState.RUNNING) progress * 6f else idleTick

    val palette = listOf(
        Color(0xFFFF6B35),
        Color(0xFFEF476F),
        Color(0xFF8338EC),
        Color(0xFF06D6A0),
        Color(0xFFFFB703),
        Color(0xFF118AB2)
    )

    var winnerPos = Offset(w * 0.5f, h * 0.5f)

    for (i in 0 until count) {
        val pos = computeButterflyPosition(i, count, w, h, animTime)
        if (i == winnerIndex) {
            winnerPos = pos
        }
        val color = palette[i % palette.size]
        val isPoppedWinner = (gameId == "balloon_pop" && i == winnerIndex && progress > 0.85f)

        when (gameId) {
            "zero_gravity" -> {
                // Phi hành gia mặc đồ trắng với kính mũ vàng/xanh
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(pos.x - 16f, pos.y - 10f),
                    size = Size(32f, 28f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawCircle(color = Color.White, radius = 17f, center = Offset(pos.x, pos.y - 20f))
                drawRoundRect(
                    color = Color(0xFF4CC9F0),
                    topLeft = Offset(pos.x - 11f, pos.y - 26f),
                    size = Size(22f, 12f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
            "balloon_pop" -> {
                if (isPoppedWinner) {
                    // Hiệu ứng pháo giấy nổ tung khi trúng bóng
                    for (ray in 0 until 8) {
                        val angle = ray * (PI.toFloat() / 4f)
                        drawCircle(
                            color = palette[ray % palette.size],
                            radius = 7f,
                            center = Offset(pos.x + cos(angle) * 28f, pos.y - 12f + sin(angle) * 28f)
                        )
                    }
                } else {
                    // Quả bóng bay căng tròn và dây buộc
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(pos.x, pos.y + 8f),
                        end = Offset(pos.x, pos.y + 28f),
                        strokeWidth = 3f
                    )
                    drawOval(
                        color = color,
                        topLeft = Offset(pos.x - 20f, pos.y - 34f),
                        size = Size(40f, 46f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f),
                        radius = 6f,
                        center = Offset(pos.x - 7f, pos.y - 20f)
                    )
                }
            }
            else -> {
                // Cánh bướm đập cánh mềm mại
                val wingFlap = (0.65f + 0.35f * abs(sin(idleTick * 8f + i)))
                drawOval(
                    color = color,
                    topLeft = Offset(pos.x - 28f * wingFlap, pos.y - 20f),
                    size = Size(26f * wingFlap, 34f)
                )
                drawOval(
                    color = color,
                    topLeft = Offset(pos.x + 2f, pos.y - 20f),
                    size = Size(26f * wingFlap, 34f)
                )
                drawRoundRect(
                    color = Color(0xFF073B4C),
                    topLeft = Offset(pos.x - 3.5f, pos.y - 18f),
                    size = Size(7f, 32f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }

        drawClampedNamePill(
            text = shortenStudentName(participants[i].fullName, 16),
            centerX = pos.x,
            centerY = pos.y + 34f,
            bgColor = if (i == winnerIndex && progress > 0.85f) Color(0xFFFFD166) else Color.White.copy(alpha = 0.95f),
            paint = badgePaint,
            maxPillWidth = maxPillW
        )
    }

    if (progress > 0.05f) {
        val catcherPos = if (progress < 0.72f) {
            val sweepAngle = progress * PI.toFloat() * 4f
            Offset(
                w * 0.5f + cos(sweepAngle) * (w * 0.26f),
                h * 0.48f + sin(sweepAngle * 1.5f) * (h * 0.20f)
            )
        } else {
            val t = ((progress - 0.72f) / 0.28f).coerceIn(0f, 1f)
            val startCatch = Offset(w * 0.5f, h * 0.48f)
            Offset(
                startCatch.x + (winnerPos.x - startCatch.x) * t,
                startCatch.y + (winnerPos.y - startCatch.y) * t
            )
        }

        when (gameId) {
            "zero_gravity" -> {
                // Vòng khoang đón phi hành gia phát sáng
                drawCircle(
                    color = Color(0xFF06D6A0),
                    radius = 52f,
                    center = catcherPos,
                    style = Stroke(width = 6f)
                )
            }
            "balloon_pop" -> {
                // Phi tiêu vàng bay tới làm nổ bóng
                drawCircle(
                    color = Color(0xFFFFD166),
                    radius = 14f,
                    center = catcherPos
                )
                drawCircle(
                    color = Color(0xFFD90429),
                    radius = 6f,
                    center = catcherPos
                )
            }
            else -> {
                // Vợt bắt bướm
                drawLine(
                    color = Color(0xFF8D5524),
                    start = catcherPos,
                    end = Offset(catcherPos.x + 80f, catcherPos.y + 80f),
                    strokeWidth = 10f
                )
                drawCircle(
                    color = Color(0xFFFFD166),
                    radius = 48f,
                    center = catcherPos,
                    style = Stroke(width = 7f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.32f),
                    radius = 44f,
                    center = catcherPos
                )
            }
        }
    }
}

/**
 * 4. NHÓM VÒNG QUAY & CHAI XOAY:
 * - lucky_wheel (#13): Vòng quay may mắn chia múi màu
 * - spin_bottle (#17): Chai thủy tinh xoay giữa vòng tròn học sinh
 */
private fun DrawScope.drawWheelOrBottleArena(
    gameId: String,
    participants: List<StudentEntity>,
    winnerIndex: Int,
    progress: Float,
    idleTick: Float,
    textPaint: Paint,
    badgePaint: Paint
) {
    val w = size.width
    val h = size.height
    val count = participants.size.coerceAtLeast(1)

    if (gameId == "spin_bottle") {
        // Bối cảnh mặt bàn gỗ ấm áp và các học sinh ngồi thành vòng tròn quanh chiếc chai
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF582F0E), Color(0xFF333D29)),
                center = Offset(w / 2f, h / 2f),
                radius = maxOf(w, h) * 0.7f
            )
        )

        val center = Offset(w / 2f, h * 0.45f)
        val ringRadius = min(w, h * 0.80f) * 0.36f
        val angleStep = 360f / count
        val targetAngle = winnerIndex * angleStep - 90f
        val eased = 1f - (1f - progress) * (1f - progress) * (1f - progress)
        val bottleAngle = if (progress <= 0.001f) {
            idleTick * 25f
        } else {
            360f * 6f * eased + targetAngle * eased
        }

        // Vẽ vòng tròn học sinh xung quanh
        for (i in 0 until count) {
            val angleRad = (i * angleStep - 90f) * (PI.toFloat() / 180f)
            val sx = center.x + cos(angleRad) * ringRadius
            val sy = center.y + sin(angleRad) * ringRadius
            val isTarget = i == winnerIndex && progress > 0.92f

            drawCircle(
                color = if (isTarget) Color(0xFFFFD166) else Color(0xFF06D6A0),
                radius = if (isTarget) 22f else 16f,
                center = Offset(sx, sy)
            )
            drawClampedNamePill(
                text = shortenStudentName(participants[i].fullName, 11),
                centerX = sx,
                centerY = sy + 26f,
                bgColor = if (isTarget) Color(0xFFFFD166) else Color.White.copy(alpha = 0.92f),
                paint = badgePaint,
                maxPillWidth = (w * 0.34f).coerceAtLeast(100f)
            )
        }

        // Vẽ chiếc chai thủy tinh xanh ngọc xoay ở tâm
        rotate(degrees = bottleAngle + 90f, pivot = center) {
            // Thân chai
            drawRoundRect(
                color = Color(0xFF2EC4B6),
                topLeft = Offset(center.x - 18f, center.y - 25f),
                size = Size(36f, 74f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            // Cổ chai hướng về phía người được chọn (hướng lên trên khi angle = -90)
            drawRoundRect(
                color = Color(0xFF06D6A0),
                topLeft = Offset(center.x - 8f, center.y - 68f),
                size = Size(16f, 46f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Nắp chai vàng
            drawCircle(
                color = Color(0xFFFFD166),
                radius = 10f,
                center = Offset(center.x, center.y - 70f)
            )
        }

        val winner = participants[winnerIndex]
        drawClampedNamePill(
            text = if (progress >= 0.92f) "🍾 Mũi chai chỉ: ${winner.fullName}" else "🍾 Chai đang xoay...",
            centerX = w / 2f,
            centerY = h * 0.91f,
            bgColor = if (progress >= 0.92f) Color(0xFFFFD166) else Color.White,
            paint = badgePaint,
            maxPillWidth = w * 0.88f
        )
        return
    }

    // Vòng quay may mắn (#13)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF1D3557), Color(0xFF073B4C)),
            center = Offset(w / 2f, h / 2f),
            radius = maxOf(w, h) * 0.7f
        )
    )

    val sweepPerSlice = 360f / count
    val center = Offset(w / 2f, h * 0.45f)
    val radius = min(w, h * 0.82f) * 0.38f

    textPaint.textSize = (radius * (if (count > 8) 0.11f else 0.135f)).coerceIn(15f, 26f)

    val targetModAngle = 270f - (winnerIndex * sweepPerSlice + sweepPerSlice / 2f)
    val eased = 1f - (1f - progress) * (1f - progress) * (1f - progress)
    val totalRotation = if (progress <= 0.001f) {
        idleTick * 12f
    } else {
        (360f * 6f + targetModAngle) * eased
    }

    val sliceColors = listOf(
        Color(0xFFFF6B35),
        Color(0xFF06D6A0),
        Color(0xFF118AB2),
        Color(0xFFFFB703),
        Color(0xFFEF476F),
        Color(0xFF8338EC)
    )

    drawCircle(
        color = Color(0xFFFFD166),
        radius = radius + 12f,
        center = center
    )

    for (i in 0 until count) {
        val startAngle = totalRotation + i * sweepPerSlice
        drawArc(
            color = sliceColors[i % sliceColors.size],
            startAngle = startAngle,
            sweepAngle = sweepPerSlice,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f)
        )
        drawArc(
            color = Color.White.copy(alpha = 0.5f),
            startAngle = startAngle,
            sweepAngle = sweepPerSlice,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = 2.5f)
        )

        val midAngleDeg = startAngle + sweepPerSlice / 2f
        rotate(degrees = midAngleDeg, pivot = center) {
            val label = shortenStudentName(participants[i].fullName, 11)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                center.x + radius * 0.58f,
                center.y + (textPaint.textSize * 0.35f),
                textPaint
            )
        }
    }

    drawCircle(color = Color.White, radius = radius * 0.14f, center = center)
    drawCircle(color = Color(0xFFFF6B35), radius = radius * 0.09f, center = center)

    val pointerPath = Path().apply {
        moveTo(center.x - 20f, center.y - radius - 22f)
        lineTo(center.x + 20f, center.y - radius - 22f)
        lineTo(center.x, center.y - radius + 16f)
        close()
    }
    drawPath(path = pointerPath, color = Color(0xFFFFD166))

    val normalizedPointerAngle = ((270f - (totalRotation % 360f)) + 720f) % 360f
    val activeIdx = if (progress >= 0.98f) {
        winnerIndex
    } else {
        ((normalizedPointerAngle / sweepPerSlice).toInt()).coerceIn(0, count - 1)
    }
    val activeStudent = participants[activeIdx]

    drawClampedNamePill(
        text = "🎯 ${activeStudent.fullName}",
        centerX = w / 2f,
        centerY = h * 0.90f,
        bgColor = if (progress >= 0.95f) Color(0xFFFFD166) else Color.White,
        paint = badgePaint,
        maxPillWidth = w * 0.86f
    )
}

/**
 * 5. NHÓM PHÓNG TÊN LỬA, THỂ THAO & PHÁO HOA:
 * - rocket_launch (#8): Tên lửa vũ trụ phóng lên quỹ đạo
 * - student_bowling (#21): Bóng bowling lăn trên sàn gỗ làm đổ 10 ki-lô
 * - penalty_kick (#22): Sút phạt bóng đá vòng cung vào khung thành
 * - hot_air_balloon (#24): Khinh khí cầu sặc sỡ bay lên trời xanh
 * - fireworks_call (#28): Pháo hoa rực sáng bầu trời đêm
 */
private fun DrawScope.drawLaunchOrSportsArena(
    gameId: String,
    participants: List<StudentEntity>,
    winnerIndex: Int,
    progress: Float,
    idleTick: Float,
    badgePaint: Paint
) {
    val w = size.width
    val h = size.height
    val count = participants.size.coerceAtLeast(1)
    val scannedIndex = if (progress < 0.55f) {
        ((progress * 25f + idleTick * 2f).toInt()) % count
    } else {
        winnerIndex
    }
    val currentStudent = participants.getOrElse(scannedIndex) { participants.first() }
    val launchPhase = ((progress - 0.55f) / 0.45f).coerceIn(0f, 1f)

    when (gameId) {
        "student_bowling" -> {
            // Sàn gỗ Bowling
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF7F4F24), Color(0xFFB08968), Color(0xFFDDA15E))
                )
            )
            // Hai rãnh biên
            drawRect(color = Color(0xFF3D405B), topLeft = Offset(0f, 0f), size = Size(w * 0.16f, h))
            drawRect(color = Color(0xFF3D405B), topLeft = Offset(w * 0.84f, 0f), size = Size(w * 0.16f, h))

            // 10 Ki-lô Bowling ở phía trên
            val pinSpread = if (progress > 0.72f) ((progress - 0.72f) / 0.28f) * 90f else 0f
            for (p in -2..2) {
                val px = w * 0.5f + p * (28f + pinSpread)
                val py = h * 0.22f - abs(p) * 10f - pinSpread * 0.4f
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(px - 10f, py - 24f),
                    size = Size(20f, 48f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawRect(
                    color = Color(0xFFD90429),
                    topLeft = Offset(px - 10f, py - 8f),
                    size = Size(20f, 6f)
                )
            }

            // Quả bóng Bowling lăn từ dưới lên
            val ballY = h * 0.76f - progress * (h * 0.54f)
            val ballX = w * 0.5f + sin(progress * PI.toFloat()) * 24f
            drawCircle(color = Color(0xFF1D3557), radius = 34f, center = Offset(ballX, ballY))
            drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 6f, center = Offset(ballX - 8f, ballY - 8f))

            drawClampedNamePill(
                text = "🎳 Strike: ${currentStudent.fullName}",
                centerX = w / 2f,
                centerY = h * 0.88f,
                bgColor = if (progress >= 0.72f) Color(0xFFFFD166) else Color.White,
                paint = badgePaint,
                maxPillWidth = w * 0.86f
            )
        }

        "penalty_kick" -> {
            // Sân cỏ bóng đá và khung thành trắng
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B4332), Color(0xFF2D6A4F), Color(0xFF40916C))
                )
            )
            // Khung thành ở phía trên
            val goalLeft = w * 0.20f
            val goalRight = w * 0.80f
            val goalTop = h * 0.12f
            val goalBottom = h * 0.34f
            drawRect(
                color = Color.White,
                topLeft = Offset(goalLeft, goalTop),
                size = Size(goalRight - goalLeft, goalBottom - goalTop),
                style = Stroke(width = 8f)
            )

            // Quả bóng đá sút cong vào góc cao khung thành
            val ballX = w * 0.5f + sin(progress * PI.toFloat()) * (w * 0.18f)
            val ballY = h * 0.76f - progress * (h * 0.54f)
            drawCircle(color = Color.White, radius = 26f, center = Offset(ballX, ballY))
            drawCircle(color = Color(0xFF073B4C), radius = 10f, center = Offset(ballX, ballY))

            drawClampedNamePill(
                text = if (progress >= 0.80f) "⚽ VÀOOO! ${currentStudent.fullName}" else "⚽ Đang sút: ${currentStudent.fullName}",
                centerX = w / 2f,
                centerY = h * 0.88f,
                bgColor = if (progress >= 0.80f) Color(0xFFFFD166) else Color.White,
                paint = badgePaint,
                maxPillWidth = w * 0.88f
            )
        }

        "hot_air_balloon" -> {
            // Bầu trời xanh ban ngày và mây trắng
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0096C7), Color(0xFF48CAE4), Color(0xFFCAF0F8))
                )
            )
            val balloonY = h * 0.62f - launchPhase * (h * 0.34f)
            val balloonX = w * 0.5f + sin(idleTick * 2f) * 16f

            // Giỏ mây phía dưới khinh khí cầu
            drawRoundRect(
                color = Color(0xFF9C6644),
                topLeft = Offset(balloonX - 24f, balloonY + 54f),
                size = Size(48f, 34f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            // Vòm khinh khí cầu sặc sỡ
            drawOval(
                color = Color(0xFFFF6B35),
                topLeft = Offset(balloonX - 64f, balloonY - 78f),
                size = Size(128f, 124f)
            )
            drawOval(
                color = Color(0xFFFFD166),
                topLeft = Offset(balloonX - 36f, balloonY - 78f),
                size = Size(72f, 124f)
            )

            drawClampedNamePill(
                text = "🎈 ${currentStudent.fullName}",
                centerX = w / 2f,
                centerY = h * 0.88f,
                bgColor = if (progress >= 0.55f) Color(0xFFFFD166) else Color.White,
                paint = badgePaint,
                maxPillWidth = w * 0.88f
            )
        }

        "fireworks_call" -> {
            // Bầu trời đêm rực sáng pháo hoa
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF03045E), Color(0xFF10002B), Color(0xFF240046))
                )
            )
            val cx = w * 0.5f
            val cy = h * 0.38f
            if (progress < 0.50f) {
                val shellY = h * 0.85f - (progress / 0.50f) * (h * 0.47f)
                drawCircle(color = Color(0xFFFFD166), radius = 10f, center = Offset(cx, shellY))
            } else {
                val burst = ((progress - 0.50f) / 0.50f).coerceIn(0.1f, 1f)
                val maxR = min(w, h) * 0.34f * burst
                val colors = listOf(Color(0xFFFFD166), Color(0xFFFF006E), Color(0xFF00F5D4), Color(0xFFFFBE0B))
                for (ring in 1..3) {
                    val r = maxR * (ring / 3f)
                    for (ray in 0 until 12) {
                        val a = ray * (PI.toFloat() / 6f) + ring * 0.2f
                        drawCircle(
                            color = colors[(ray + ring) % colors.size],
                            radius = 8f,
                            center = Offset(cx + cos(a) * r, cy + sin(a) * r)
                        )
                    }
                }
            }

            drawClampedNamePill(
                text = "🎆 ${currentStudent.fullName}",
                centerX = w / 2f,
                centerY = h * 0.86f,
                bgColor = if (progress >= 0.50f) Color(0xFFFFD166) else Color.White,
                paint = badgePaint,
                maxPillWidth = w * 0.88f
            )
        }

        else -> {
            // Tên lửa phóng (#8)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF03045E), Color(0xFF023E8A), Color(0xFF0077B6))
                )
            )
            val rocketX = w * 0.5f + if (progress in 0.35f..0.55f) sin(progress * 120f) * 6f else 0f
            val rocketBaseY = h * 0.64f - (launchPhase * launchPhase * h * 0.44f)

            if (progress > 0.3f) {
                drawOval(
                    color = Color(0xFFFFB703),
                    topLeft = Offset(rocketX - 24f, rocketBaseY + 48f),
                    size = Size(48f, 80f)
                )
                drawOval(
                    color = Color(0xFFFB5607),
                    topLeft = Offset(rocketX - 15f, rocketBaseY + 56f),
                    size = Size(30f, 54f)
                )
            }

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(rocketX - 36f, rocketBaseY - 70f),
                size = Size(72f, 130f),
                cornerRadius = CornerRadius(32f, 32f)
            )
            val nosePath = Path().apply {
                moveTo(rocketX, rocketBaseY - 115f)
                lineTo(rocketX - 36f, rocketBaseY - 55f)
                lineTo(rocketX + 36f, rocketBaseY - 55f)
                close()
            }
            drawPath(path = nosePath, color = Color(0xFFEF476F))
            drawCircle(
                color = Color(0xFF4CC9F0),
                radius = 18f,
                center = Offset(rocketX, rocketBaseY - 16f)
            )

            drawClampedNamePill(
                text = "🚀 ${currentStudent.fullName}",
                centerX = w / 2f,
                centerY = h * 0.87f,
                bgColor = if (progress >= 0.55f) Color(0xFFFFD166) else Color.White,
                paint = badgePaint,
                maxPillWidth = w * 0.88f
            )
        }
    }
}

/**
 * 6. NHÓM SÂN KHẤU, CỔNG DỊCH CHUYỂN, MÁY QUÉT LASER, THANG MÁY & GAMESHOW:
 * - stage_spotlight (#25): Đèn sân khấu hội tụ
 * - portal_gate (#10): Cổng dịch chuyển không gian xoáy tròn
 * - laser_scanner (#12): Chùm tia laser neon quét ngang dọc
 * - secret_elevator (#29): Thang máy nhảy tầng và mở cửa đón học sinh
 * - gameshow_reveal (#30): Bảng điện tử Gameshow truyền hình
 */
private fun DrawScope.drawStageOrScannerArena(
    gameId: String,
    participants: List<StudentEntity>,
    winnerIndex: Int,
    progress: Float,
    idleTick: Float,
    badgePaint: Paint
) {
    val w = size.width
    val h = size.height
    val isPortrait = h > w
    val count = participants.size.coerceAtLeast(1)

    val highlightedIndex = when {
        progress <= 0.001f -> ((idleTick * 2f).toInt()) % count
        progress < 0.78f -> ((progress * 28f).toInt()) % count
        else -> winnerIndex
    }

    if (gameId == "secret_elevator") {
        // Thang máy bí mật (#29) nhảy số tầng rồi mở 2 cánh cửa bạc
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1D3557), Color(0xFF457B9D))
            )
        )
        val cx = w * 0.5f
        val cy = h * 0.52f
        val elevW = min(w * 0.74f, 340f)
        val elevH = min(h * 0.64f, 320f)
        val isOpened = progress >= 0.75f
        val openOffset = if (isOpened) ((progress - 0.75f) / 0.25f) * (elevW * 0.42f) else 0f

        // Lòng thang máy sáng vàng
        drawRoundRect(
            color = Color(0xFFFFF3D6),
            topLeft = Offset(cx - elevW / 2f, cy - elevH / 2f),
            size = Size(elevW, elevH),
            cornerRadius = CornerRadius(18f, 18f)
        )
        // Hai cánh cửa thang máy trượt sang 2 bên
        val doorW = (elevW / 2f) - openOffset
        if (doorW > 4f) {
            drawRect(
                color = Color(0xFF8D99AE),
                topLeft = Offset(cx - elevW / 2f, cy - elevH / 2f),
                size = Size(doorW, elevH)
            )
            drawRect(
                color = Color(0xFF8D99AE),
                topLeft = Offset(cx + openOffset, cy - elevH / 2f),
                size = Size(doorW, elevH)
            )
        }

        val currentStudent = participants[highlightedIndex]
        drawClampedNamePill(
            text = "🛗 Tầng ${highlightedIndex + 1}: ${currentStudent.fullName}",
            centerX = cx,
            centerY = if (isOpened) cy else cy - elevH / 2f - 32f,
            bgColor = if (isOpened) Color(0xFFFFD166) else Color.White,
            paint = badgePaint,
            maxPillWidth = w * 0.88f
        )
        return
    }

    val bgColors = when (gameId) {
        "portal_gate" -> listOf(Color(0xFF240046), Color(0xFF3C096C), Color(0xFF5A189A))
        "laser_scanner" -> listOf(Color(0xFF050517), Color(0xFF0B132B), Color(0xFF1C2541))
        "gameshow_reveal" -> listOf(Color(0xFF3A0CA3), Color(0xFF4361EE), Color(0xFF4CC9F0))
        else -> listOf(Color(0xFF1A1423), Color(0xFF3D314A), Color(0xFF684756))
    }
    drawRect(brush = Brush.verticalGradient(colors = bgColors))

    val cols = when {
        count <= 4 -> 2
        isPortrait -> 2
        count <= 9 -> 3
        else -> 4
    }
    val rows = ((count + cols - 1) / cols).coerceAtLeast(1)
    val cellW = (w * 0.90f) / cols
    val cellH = (h * 0.82f) / rows

    var spotlitCenter = Offset(w / 2f, h / 2f)

    for (i in 0 until count) {
        val col = i % cols
        val row = i / cols
        val cx = w * 0.05f + cellW * (col + 0.5f)
        val cy = h * 0.10f + cellH * (row + 0.5f)
        val isSpotlit = i == highlightedIndex
        if (isSpotlit) {
            spotlitCenter = Offset(cx, cy)
        }

        if (isSpotlit) {
            when (gameId) {
                "portal_gate" -> {
                    drawCircle(
                        color = Color(0xFF00F5D4).copy(alpha = 0.45f),
                        radius = min(cellW, cellH) * 0.48f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color(0xFFF72585),
                        radius = min(cellW, cellH) * 0.48f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 5f)
                    )
                }
                "gameshow_reveal" -> {
                    drawRoundRect(
                        color = Color(0xFFFFBE0B).copy(alpha = 0.45f),
                        topLeft = Offset(cx - cellW * 0.46f, cy - 30f),
                        size = Size(cellW * 0.92f, 60f),
                        cornerRadius = CornerRadius(18f, 18f)
                    )
                }
                else -> {
                    val beam = Path().apply {
                        moveTo(w * 0.5f, 0f)
                        lineTo(cx - cellW * 0.44f, cy + 26f)
                        lineTo(cx + cellW * 0.44f, cy + 26f)
                        close()
                    }
                    drawPath(
                        path = beam,
                        color = Color(0xFFFFD166).copy(alpha = 0.28f)
                    )
                }
            }
        }

        drawClampedNamePill(
            text = shortenStudentName(participants[i].fullName, 16),
            centerX = cx,
            centerY = cy,
            bgColor = if (isSpotlit) Color(0xFFFFD166) else Color.White.copy(alpha = 0.88f),
            paint = badgePaint,
            maxPillWidth = cellW * 0.92f
        )
    }

    if (gameId == "laser_scanner" && progress > 0.01f) {
        // Tia laser ngang & dọc quét qua mục tiêu đang sáng
        drawLine(
            color = Color(0xFF00F5D4),
            start = Offset(0f, spotlitCenter.y),
            end = Offset(w, spotlitCenter.y),
            strokeWidth = 4f
        )
        drawLine(
            color = Color(0xFFF15BB5),
            start = Offset(spotlitCenter.x, 0f),
            end = Offset(spotlitCenter.x, h),
            strokeWidth = 4f
        )
    }
}

/**
 * 7. NHÓM RƯƠNG KHO BÁU, HỘP QUÀ, LẬT THẺ, MŨ ẢO THUẬT & ĐẬP PIÑATA:
 * - treasure_chest (#14): Rương kho báu vàng
 * - mystery_gift (#16): Hộp quà thắt nơ đỏ
 * - secret_card (#18): Lật thẻ bài ma thuật
 * - magic_hat (#26): Mũ ảo thuật gia tung sao
 * - pinata_smash (#27): Đập Piñata tung kẹo ngọt
 */
private fun DrawScope.drawMysteryRevealArena(
    gameId: String,
    participants: List<StudentEntity>,
    winnerIndex: Int,
    progress: Float,
    idleTick: Float,
    badgePaint: Paint
) {
    val w = size.width
    val h = size.height
    val bgColors = when (gameId) {
        "mystery_gift" -> listOf(Color(0xFF1982C4), Color(0xFF8AC926))
        "secret_card" -> listOf(Color(0xFF240046), Color(0xFF6A4C93))
        "magic_hat" -> listOf(Color(0xFF10002B), Color(0xFF3C096C))
        "pinata_smash" -> listOf(Color(0xFFFB5607), Color(0xFFFF006E))
        else -> listOf(Color(0xFF2B2D42), Color(0xFF8D99AE))
    }
    drawRect(brush = Brush.verticalGradient(colors = bgColors))

    val cx = w * 0.5f + if (progress in 0.05f..0.7f) sin(progress * 95f) * 12f else 0f
    val cy = h * 0.54f
    val isOpened = progress >= 0.7f
    val winner = participants.getOrElse(winnerIndex) { participants.first() }

    if (isOpened) {
        drawCircle(
            color = Color(0xFFFFD166).copy(alpha = 0.45f),
            radius = min(w, h) * 0.34f,
            center = Offset(cx, cy - 40f)
        )
    }

    val lidOffsetY = if (isOpened) -68f else 0f

    when (gameId) {
        "mystery_gift" -> {
            // Hộp quà xanh lá thắt nơ đỏ
            drawRoundRect(
                color = Color(0xFF06D6A0),
                topLeft = Offset(cx - 86f, cy - 40f),
                size = Size(172f, 120f),
                cornerRadius = CornerRadius(18f, 18f)
            )
            drawRect(color = Color(0xFFEF476F), topLeft = Offset(cx - 16f, cy - 40f), size = Size(32f, 120f))
            drawRoundRect(
                color = Color(0xFF118AB2),
                topLeft = Offset(cx - 94f, cy - 72f + lidOffsetY),
                size = Size(188f, 40f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawCircle(color = Color(0xFFEF476F), radius = 20f, center = Offset(cx - 18f, cy - 82f + lidOffsetY))
            drawCircle(color = Color(0xFFEF476F), radius = 20f, center = Offset(cx + 18f, cy - 82f + lidOffsetY))
        }

        "secret_card" -> {
            // Tấm thẻ bài ma thuật xoay và lật mặt vàng
            val cardW = 180f * abs(cos(if (isOpened) 0f else progress * PI.toFloat() * 5f)).coerceAtLeast(0.18f)
            drawRoundRect(
                color = if (isOpened) Color(0xFFFFD166) else Color(0xFF4CC9F0),
                topLeft = Offset(cx - cardW / 2f, cy - 110f),
                size = Size(cardW, 210f),
                cornerRadius = CornerRadius(22f, 22f)
            )
        }

        "magic_hat" -> {
            // Chiếc mũ ảo thuật gia màu đen với dải ruy-băng tím và tai thỏ trắng
            if (isOpened) {
                drawOval(
                    color = Color.White,
                    topLeft = Offset(cx - 28f, cy - 135f),
                    size = Size(22f, 64f)
                )
                drawOval(
                    color = Color.White,
                    topLeft = Offset(cx + 6f, cy - 135f),
                    size = Size(22f, 64f)
                )
            }
            drawRoundRect(
                color = Color(0xFF1A1423),
                topLeft = Offset(cx - 72f, cy - 76f),
                size = Size(144f, 110f),
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRect(color = Color(0xFFF72585), topLeft = Offset(cx - 72f, cy + 8f), size = Size(144f, 18f))
            drawRoundRect(
                color = Color(0xFF1A1423),
                topLeft = Offset(cx - 112f, cy + 24f),
                size = Size(224f, 24f),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }

        "pinata_smash" -> {
            // Ngựa giấy Piñata treo trên dây đung đưa và tung kẹo
            drawLine(color = Color.White, start = Offset(cx, 0f), end = Offset(cx, cy - 50f), strokeWidth = 5f)
            if (isOpened) {
                val candyColors = listOf(Color(0xFFFFBE0B), Color(0xFF00F5D4), Color(0xFF8338EC), Color.White)
                for (c in 0 until 12) {
                    val a = c * (PI.toFloat() / 6f)
                    drawCircle(
                        color = candyColors[c % candyColors.size],
                        radius = 12f,
                        center = Offset(cx + cos(a) * 95f, cy + sin(a) * 65f)
                    )
                }
            } else {
                drawRoundRect(
                    color = Color(0xFFFFBE0B),
                    topLeft = Offset(cx - 76f, cy - 45f),
                    size = Size(152f, 90f),
                    cornerRadius = CornerRadius(28f, 28f)
                )
                drawRoundRect(
                    color = Color(0xFF00F5D4),
                    topLeft = Offset(cx + 36f, cy - 90f),
                    size = Size(44f, 60f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
            }
        }

        else -> {
            // Rương kho báu (#14)
            drawRoundRect(
                color = Color(0xFF9C6644),
                topLeft = Offset(cx - 100f, cy - 36f),
                size = Size(200f, 110f),
                cornerRadius = CornerRadius(20f, 20f)
            )
            drawRect(
                color = Color(0xFFFFB703),
                topLeft = Offset(cx - 100f, cy - 8f),
                size = Size(200f, 18f)
            )
            drawRoundRect(
                color = Color(0xFFB08968),
                topLeft = Offset(cx - 106f, cy - 82f + lidOffsetY),
                size = Size(212f, 54f),
                cornerRadius = CornerRadius(22f, 22f)
            )
        }
    }

    val displayStudent = if (isOpened) {
        winner
    } else {
        participants[((progress * 20f + idleTick).toInt()).coerceAtLeast(0) % participants.size.coerceAtLeast(1)]
    }
    val cardY = if (isOpened) cy - 135f else cy + 125f
    drawClampedNamePill(
        text = "✨ ${displayStudent.fullName}",
        centerX = cx,
        centerY = cardY,
        bgColor = if (isOpened) Color(0xFFFFD166) else Color.White,
        paint = badgePaint,
        maxPillWidth = w * 0.86f
    )
}

/**
 * 8. NHÓM MÁY GẮP THÚ & CÂU CÁ GỌI TÊN:
 * - claw_machine (#23): Càng gắp thú bông trong tủ kính
 * - fishing_pick (#20): Thả cần câu xuống mặt hồ câu chú cá vàng mang tên học sinh
 */
private fun DrawScope.drawClawOrFishingArena(
    gameId: String,
    participants: List<StudentEntity>,
    winnerIndex: Int,
    progress: Float,
    idleTick: Float,
    badgePaint: Paint
) {
    val w = size.width
    val h = size.height
    val isFishing = gameId == "fishing_pick"

    drawRect(
        brush = Brush.verticalGradient(
            colors = if (isFishing) {
                listOf(Color(0xFF90E0EF), Color(0xFF0077B6), Color(0xFF023E8A))
            } else {
                listOf(Color(0xFF2B2D42), Color(0xFF48CAE4), Color(0xFF90E0EF))
            }
        )
    )

    val count = participants.size.coerceAtLeast(1)
    val slotW = (w * 0.86f) / count
    val baseStartX = w * 0.07f + slotW * 0.5f
    val bottomY = h * 0.72f
    val winnerX = baseStartX + winnerIndex * slotW

    val clawX = when {
        progress <= 0.001f -> w * 0.5f + sin(idleTick * 2f) * (w * 0.25f)
        progress < 0.5f -> w * 0.5f + sin(progress * PI.toFloat() * 4f) * (w * 0.34f)
        else -> {
            val t = ((progress - 0.5f) / 0.25f).coerceIn(0f, 1f)
            val fromX = w * 0.5f
            fromX + (winnerX - fromX) * t
        }
    }

    val clawY = when {
        progress < 0.5f -> h * 0.16f
        progress < 0.75f -> {
            val t = (progress - 0.5f) / 0.25f
            h * 0.16f + t * (bottomY - 36f - h * 0.16f)
        }
        else -> {
            val t = (progress - 0.75f) / 0.25f
            (bottomY - 36f) - t * (bottomY - 36f - h * 0.24f)
        }
    }

    if (isFishing) {
        // Cần câu cá từ góc trên bên phải vươn ra và dây câu + phao câu đỏ trắng
        drawLine(
            color = Color(0xFF7F4F24),
            start = Offset(w, 0f),
            end = Offset(clawX, h * 0.10f),
            strokeWidth = 12f
        )
        drawLine(
            color = Color.White,
            start = Offset(clawX, h * 0.10f),
            end = Offset(clawX, clawY),
            strokeWidth = 3.5f
        )
        drawCircle(color = Color(0xFFD90429), radius = 14f, center = Offset(clawX, clawY))
        drawCircle(color = Color.White, radius = 7f, center = Offset(clawX, clawY + 6f))
    } else {
        // Càng máy gắp thú
        drawLine(
            color = Color(0xFF073B4C),
            start = Offset(clawX, 0f),
            end = Offset(clawX, clawY),
            strokeWidth = 7f
        )
        drawCircle(
            color = Color(0xFFFF6B35),
            radius = 20f,
            center = Offset(clawX, clawY)
        )
    }

    val drawOrder = participants.indices.sortedBy { if (it == winnerIndex) 1 else 0 }
    for (i in drawOrder) {
        val x = baseStartX + i * slotW
        val isPicked = i == winnerIndex && progress >= 0.75f
        val tierOffset = (i % 3) * (h * 0.065f)
        val y = if (isPicked) clawY + 42f else (bottomY + tierOffset)

        if (isFishing) {
            // Vẽ chú cá vàng bơi dưới hồ
            drawOval(
                color = if (isPicked) Color(0xFFFFD166) else Color(0xFFFF9F1C),
                topLeft = Offset(x - 24f, y - 42f),
                size = Size(48f, 26f)
            )
        } else {
            // Vẽ thú bông tròn dễ thương
            drawCircle(
                color = if (isPicked) Color(0xFFFFD166) else Color(0xFFFF758F),
                radius = 20f,
                center = Offset(x, y - 30f)
            )
        }

        val maxW = if (isPicked) w * 0.72f else (slotW * 2.6f).coerceIn(110f, w * 0.42f)
        drawClampedNamePill(
            text = shortenStudentName(participants[i].fullName, if (isPicked) 20 else 12),
            centerX = x,
            centerY = y,
            bgColor = if (isPicked) Color(0xFFFFD166) else Color.White.copy(alpha = 0.94f),
            paint = badgePaint,
            maxPillWidth = maxW
        )
    }
}
