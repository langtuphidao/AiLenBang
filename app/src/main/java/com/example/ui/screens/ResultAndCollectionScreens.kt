package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.models.AnswerStatus
import com.example.models.LockedSelectionTurn
import com.example.models.MiniGameCatalog
import com.example.models.MiniGameCategory
import com.example.models.MiniGameDescriptor
import kotlin.math.sin

/**
 * 5. MÀN HÌNH CÔNG BỐ KẾT QUẢ (RESULT REVEAL SCREEN)
 * Giữ cố định tên học sinh kích thước lớn trên màn hình, các nút hành động rộng rãi không bị cắt chữ.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultRevealScreen(
    lockedTurn: LockedSelectionTurn?,
    currentAnswerStatus: AnswerStatus,
    onSelectAnswerStatus: (AnswerStatus) -> Unit,
    onCallNextStudentWithMiniGame: () -> Unit,
    onCallNextStudentImmediately: () -> Unit,
    onChangeMiniGame: () -> Unit = {},
    onUndoThisTurn: () -> Unit,
    onBackToClass: () -> Unit
) {
    BackHandler { onBackToClass() }

    if (lockedTurn == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = onBackToClass) {
                Text("Quay về lớp")
            }
        }
        return
    }

    val infinite = rememberInfiniteTransition(label = "confetti")
    val confettiPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF073B4C), Color(0xFF118AB2), Color(0xFF073B4C))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = listOf(
                Color(0xFFFFD166),
                Color(0xFFFF6B35),
                Color(0xFF06D6A0),
                Color(0xFFEF476F),
                Color.White
            )
            for (i in 0 until 32) {
                val x = (size.width * ((i * 37 % 100) / 100f)) + sin(confettiPhase + i) * 28f
                val y = ((size.height * ((i * 23 % 100) / 100f)) + confettiPhase * 90f) % size.height
                drawCircle(
                    color = colors[i % colors.size].copy(alpha = 0.7f),
                    radius = 8f + (i % 4) * 3f,
                    center = Offset(x, y)
                )
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFFFD166)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Chiến thắng",
                        tint = Color(0xFF073B4C)
                    )
                    Text(
                        text = "${lockedTurn.miniGame.iconEmoji} ${lockedTurn.miniGame.nameVi} • ${lockedTurn.classroomName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF073B4C),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Thẻ công bố tên học sinh kích thước cực lớn
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("winner_announcement_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (lockedTurn.isManualDirectPick) {
                            "HỌC SINH ĐƯỢC CHỌN TRỰC TIẾP"
                        } else if (lockedTurn.winners.size > 1) {
                            "DANH SÁCH ${lockedTurn.winners.size} HỌC SINH LÊN BẢNG"
                        } else {
                            "XIN MỜI HỌC SINH LÊN BẢNG!"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFFF6B35),
                        textAlign = TextAlign.Center
                    )

                    lockedTurn.winners.forEachIndexed { index, student ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFF3D6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (lockedTurn.winners.size > 1) {
                                    "${index + 1}. ${student.fullName}"
                                } else {
                                    student.fullName
                                },
                                style = MaterialTheme.typography.displayMedium,
                                color = Color(0xFF073B4C),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 18.dp)
                                    .testTag("winner_name_text_$index")
                            )
                        }
                    }

                    Text(
                        text = "Trạng thái: ${currentAnswerStatus.labelVi} • Chu kỳ #${lockedTurn.cycleNumber}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF3D5A6C),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nhóm nút đánh dấu trạng thái trả bài
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Xác nhận kết quả trả bài của học sinh:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onSelectAnswerStatus(AnswerStatus.ANSWERED) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                                .testTag("status_answered_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentAnswerStatus == AnswerStatus.ANSWERED) {
                                    Color(0xFF06D6A0)
                                } else {
                                    Color(0xFF118AB2)
                                }
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Đã trả bài", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Đã trả bài",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FilledTonalButton(
                            onClick = { onSelectAnswerStatus(AnswerStatus.NOT_ANSWERED) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                                .testTag("status_not_answered_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Chưa trả bài", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Chưa trả bài",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSelectAnswerStatus(AnswerStatus.ABSENT) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("status_absent_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.PersonOff, contentDescription = "Vắng mặt", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Vắng mặt", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = { onSelectAnswerStatus(AnswerStatus.SKIPPED) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("status_skip_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = "Bỏ qua lượt", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bỏ qua lượt", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Xếp 2 nút gọi người tiếp theo toàn chiều ngang để chữ lớn, rõ ràng, không bao giờ bị tràn
            Button(
                onClick = onCallNextStudentWithMiniGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("result_next_minigame_button"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B35)
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Gọi người tiếp theo")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GỌI NGƯỜI TIẾP THEO (CHƠI MINI-GAME)",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onCallNextStudentImmediately,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("result_next_instant_button"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD166),
                    contentColor = Color(0xFF073B4C)
                )
            ) {
                Icon(Icons.Default.Bolt, contentDescription = "Gọi nhanh")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GỌI NHANH NGƯỜI TIẾP THEO (BỎ QUA GAME)",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onChangeMiniGame,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("result_change_game_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("🎮 Đổi game", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onUndoThisTurn,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("result_undo_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Hoàn tác", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hoàn tác", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                FilledTonalButton(
                    onClick = onBackToClass,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("result_back_to_class_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Về lớp", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Về lớp", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * 6. BỘ SƯU TẬP 30 MINI-GAME (MINIGAME COLLECTION SCREEN)
 * Bố cục thẻ 2 tầng thông thoáng: Tầng trên là Icon + Tên + Mô tả rộng hết chiều ngang,
 * tầng dưới là 2 nút Chọn & Chơi ngay rõ ràng, không ép chữ vào cột hẹp.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MiniGameCollectionScreen(
    selectedMiniGameId: String,
    recentMiniGameIds: List<String>,
    onSelectAndReturn: (MiniGameDescriptor) -> Unit,
    onPlayNow: (MiniGameDescriptor) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedCategory by remember { mutableStateOf<MiniGameCategory?>(null) }
    var showMvpOnly by remember { mutableStateOf(false) }

    val gamesToShow = remember(selectedCategory, showMvpOnly) {
        MiniGameCatalog.allGames.filter { game ->
            val catMatch = selectedCategory == null || game.category == selectedCategory
            val mvpMatch = !showMvpOnly || game.isMvpCore
            catMatch && mvpMatch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bộ sưu tập 30 Mini-Game 2D",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null && !showMvpOnly,
                        onClick = {
                            selectedCategory = null
                            showMvpOnly = false
                        },
                        label = { Text("Tất cả (30)") }
                    )
                    FilterChip(
                        selected = showMvpOnly,
                        onClick = { showMvpOnly = !showMvpOnly },
                        label = { Text("⭐ 5 Game Chủ Đạo MVP") }
                    )
                    MiniGameCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = if (selectedCategory == cat) null else cat
                            },
                            label = { Text("${cat.emoji} ${cat.titleVi}") }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(gamesToShow, key = { it.id }) { game ->
                        val isCurrent = game.id == selectedMiniGameId
                        val isRecent = recentMiniGameIds.contains(game.id)
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { onSelectAndReturn(game) }
                                .testTag("minigame_item_${game.id}"),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(game.primaryColorHex),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = game.iconEmoji,
                                                style = MaterialTheme.typography.headlineMedium
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "#${game.number}. ${game.nameVi}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (game.isMvpCore) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFFFD166)
                                                ) {
                                                    Text(
                                                        text = "MVP",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = Color(0xFF073B4C),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            if (isRecent) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    Text(
                                                        text = "Gần đây",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = game.shortDescriptionVi,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = { onSelectAndReturn(game) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 46.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = if (isCurrent) "✓ Đang chọn mặc định" else "Chọn game này",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Button(
                                        onClick = { onPlayNow(game) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 46.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Chơi ngay", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Chơi ngay",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
