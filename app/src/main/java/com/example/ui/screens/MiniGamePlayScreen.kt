package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mini_games.MiniGameEngineCanvas
import com.example.models.AppSettings
import com.example.models.LockedSelectionTurn
import com.example.models.MiniGameRunState
import com.example.models.StudentEntity
import com.example.services.AudioManager
import com.example.services.SoundEffectType
import kotlinx.coroutines.delay

@Composable
fun MiniGamePlayScreen(
    lockedTurn: LockedSelectionTurn?,
    runState: MiniGameRunState,
    settings: AppSettings,
    audioManager: AudioManager,
    onStartGame: () -> Unit,
    onStopAndRevealNow: () -> Unit,
    onReplayTurn: () -> Unit,
    onChangeMiniGame: () -> Unit = {},
    onDirectTapPick: (StudentEntity) -> Unit,
    onBackToClass: () -> Unit
) {
    BackHandler {
        onBackToClass()
    }

    if (lockedTurn == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Chưa có lượt chọn nào được khởi tạo.",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackToClass) {
                    Text("Quay về lớp")
                }
            }
        }
        return
    }

    val progressAnim = remember(lockedTurn.turnId, lockedTurn.miniGame.id) { Animatable(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "idle_loop")
    val idleTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_tick"
    )

    LaunchedEffect(lockedTurn.turnId, lockedTurn.miniGame.id, runState) {
        if (runState == MiniGameRunState.RUNNING) {
            val durationMs = (settings.animationDurationSeconds.coerceIn(3, 12) * 1000)
            progressAnim.snapTo(0f)
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
            )
            delay(280L)
            onStopAndRevealNow()
        } else if (runState == MiniGameRunState.IDLE) {
            progressAnim.snapTo(0f)
        }
    }

    LaunchedEffect(progressAnim.value, runState) {
        if (runState == MiniGameRunState.RUNNING && progressAnim.value > 0.05f && progressAnim.value < 0.92f) {
            audioManager.play(SoundEffectType.SUSPENSE_TICK)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        val isLandscape = maxWidth > maxHeight
        val isNarrowPortrait = maxWidth < 380.dp

        if (isLandscape) {
            // Bố cục ngang tối ưu trình chiếu TV / Máy chiếu:
            // Canvas bên trái (68%), cột nút bấm xếp dọc bên phải (32%) rộng rãi, không bao giờ ép 2 nút vào 1 hàng hẹp!
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.68f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniGameHeaderBar(
                        lockedTurn = lockedTurn,
                        runState = runState,
                        onChangeMiniGame = onChangeMiniGame
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        MiniGameEngineCanvas(
                            lockedTurn = lockedTurn,
                            runState = runState,
                            progress = progressAnim.value,
                            idleTick = idleTick,
                            presentationMode = settings.presentationMode,
                            onDirectTapStudent = onDirectTapPick,
                            modifier = Modifier.testTag("minigame_canvas")
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(0.32f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    MiniGameControlPanel(
                        lockedTurn = lockedTurn,
                        runState = runState,
                        progress = progressAnim.value,
                        stackButtonsVertically = true,
                        onStartGame = onStartGame,
                        onStopAndRevealNow = onStopAndRevealNow,
                        onReplayTurn = onReplayTurn,
                        onChangeMiniGame = onChangeMiniGame,
                        onBackToClass = onBackToClass,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    )
                }
            }
        } else {
            // Bố cục dọc trên điện thoại: Thanh tiêu đề nằm tách biệt phía trên Canvas nên không che bất kỳ nhân vật nào
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniGameHeaderBar(
                    lockedTurn = lockedTurn,
                    runState = runState,
                    onChangeMiniGame = onChangeMiniGame
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    MiniGameEngineCanvas(
                        lockedTurn = lockedTurn,
                        runState = runState,
                        progress = progressAnim.value,
                        idleTick = idleTick,
                        presentationMode = settings.presentationMode,
                        onDirectTapStudent = onDirectTapPick,
                        modifier = Modifier.testTag("minigame_canvas")
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    MiniGameControlPanel(
                        lockedTurn = lockedTurn,
                        runState = runState,
                        progress = progressAnim.value,
                        stackButtonsVertically = isNarrowPortrait,
                        onStartGame = onStartGame,
                        onStopAndRevealNow = onStopAndRevealNow,
                        onReplayTurn = onReplayTurn,
                        onChangeMiniGame = onChangeMiniGame,
                        onBackToClass = onBackToClass,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Thanh thông tin nằm tách biệt phía trên khu vực Canvas để không bao giờ che khuất nhân vật hoặc tên học sinh.
 */
@Composable
private fun MiniGameHeaderBar(
    lockedTurn: LockedSelectionTurn,
    runState: MiniGameRunState,
    onChangeMiniGame: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF073B4C)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${lockedTurn.miniGame.iconEmoji} ${lockedTurn.miniGame.nameVi} (#${lockedTurn.miniGame.number}) • ${lockedTurn.classroomName}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val statusLabel = when (runState) {
                MiniGameRunState.IDLE -> if (lockedTurn.miniGame.supportsDirectTapPick) {
                    "Chạm bướm / Bắt đầu"
                } else {
                    "Sẵn sàng"
                }
                MiniGameRunState.RUNNING -> "Đang chạy..."
                else -> "Hoàn tất"
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFFFD166)
            ) {
                Text(
                    text = statusLabel,
                    color = Color(0xFF073B4C),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniGameControlPanel(
    lockedTurn: LockedSelectionTurn,
    runState: MiniGameRunState,
    progress: Float,
    stackButtonsVertically: Boolean,
    onStartGame: () -> Unit,
    onStopAndRevealNow: () -> Unit,
    onReplayTurn: () -> Unit,
    onChangeMiniGame: () -> Unit,
    onBackToClass: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = runState == MiniGameRunState.RUNNING

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isRunning) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        if (lockedTurn.miniGame.supportsDirectTapPick) {
            Text(
                text = "💡 Bấm \"Bắt đầu\" để random tự động, hoặc chạm trực tiếp vào một chú bướm trên màn hình!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (stackButtonsVertically) {
            Button(
                onClick = onStartGame,
                enabled = !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("minigame_start_button"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Bắt đầu")
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRunning) "Đang chạy..." else "Bắt đầu",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = onStopAndRevealNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("minigame_reveal_now_button"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(
                    if (isRunning) Icons.Default.Stop else Icons.Default.Bolt,
                    contentDescription = "Chọn ngay / Công bố"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRunning) "Công bố ngay" else "Chọn ngay",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onReplayTurn,
                enabled = !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("minigame_replay_button"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Chơi lại", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Đổi lượt mới", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            OutlinedButton(
                onClick = onChangeMiniGame,
                enabled = !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("minigame_switch_game_button"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text("🎮 Đổi Mini-Game khác", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            OutlinedButton(
                onClick = onBackToClass,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("minigame_back_button"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay về lớp",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Quay về lớp", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStartGame,
                    enabled = !isRunning,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("minigame_start_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Bắt đầu", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "Đang chạy..." else "Bắt đầu",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = onStopAndRevealNow,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("minigame_reveal_now_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Stop else Icons.Default.Bolt,
                        contentDescription = "Chọn ngay / Công bố",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "Công bố ngay" else "Chọn ngay",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onReplayTurn,
                    enabled = !isRunning,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("minigame_replay_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Chơi lại", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Đổi lượt", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onChangeMiniGame,
                    enabled = !isRunning,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("minigame_switch_game_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Text("🎮 Đổi game", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onBackToClass,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("minigame_back_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay về lớp",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Về lớp", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
