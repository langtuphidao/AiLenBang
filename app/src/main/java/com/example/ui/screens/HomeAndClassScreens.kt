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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.models.AppSettings
import com.example.models.ClassroomSummary
import com.example.models.MiniGameCatalog
import com.example.models.MiniGameDescriptor
import com.example.models.SelectionHistoryEntity
import com.example.models.SelectionMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 1. MÀN HÌNH KHỞI ĐỘNG (HOME SCREEN)
 */
@Composable
fun HomeScreen(
    activeClassroomSummary: ClassroomSummary?,
    selectedMiniGame: MiniGameDescriptor,
    onStartClicked: () -> Unit,
    onManageClassesClicked: () -> Unit,
    onHistoryClicked: () -> Unit,
    onMiniGameCollectionClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onAboutAuthorClicked: () -> Unit,
    onCreateDemoClassClicked: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "mascot_anim")
    val bounceOffset by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_bounce"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        val isCompactPhone = maxWidth < 380.dp

        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Hero với Mascot Cú Giáo Sư 2D Cartoon
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFF073B4C)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFF6B35).copy(alpha = 0.25f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onAboutAuthorClicked)
                    ) {
                        Text(
                            text = "✨ Tác giả: AnonyViet.Com",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFD166),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    MascotOwlCartoonCanvas(
                        bounceY = bounceOffset,
                        modifier = Modifier.size(124.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "AI LÊN BẢNG?",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color(0xFFFFD166),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Chọn Ngẫu Nhiên Học Sinh Bằng Mini-Game 2D Vui Nhộn",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (activeClassroomSummary != null) {
                                "Lớp: ${activeClassroomSummary.classroom.name} (${activeClassroomSummary.totalStudents} HS) • ${selectedMiniGame.iconEmoji} ${selectedMiniGame.nameVi}"
                            } else {
                                "Chưa có lớp học • Bấm Bắt đầu hoặc Quản lý lớp để tạo ngay!"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF90E0EF),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Nút BẮT ĐẦU chính (Co giãn chiều cao tự nhiên, không bao giờ cắt chữ)
            Button(
                onClick = onStartClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .testTag("home_start_button"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Bắt đầu",
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (activeClassroomSummary != null) {
                        "BẮT ĐẦU (${activeClassroomSummary.classroom.name})"
                    } else {
                        "BẮT ĐẦU NGAY"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Lưới 2x2 cho 4 chức năng chính (tự động xếp 1 cột nếu màn hình quá hẹp)
            if (isCompactPhone) {
                HomeMenuCardButton(
                    title = "Quản lý lớp",
                    subtitle = "Danh sách lớp & học sinh",
                    icon = Icons.Default.Groups,
                    accentColor = Color(0xFF118AB2),
                    testTag = "home_manage_classes_button",
                    onClick = onManageClassesClicked,
                    modifier = Modifier.fillMaxWidth()
                )
                HomeMenuCardButton(
                    title = "Kho 30 Mini-game",
                    subtitle = "30 trò chơi 2D vui nhộn",
                    icon = Icons.Default.SportsEsports,
                    accentColor = Color(0xFF06D6A0),
                    testTag = "home_minigame_collection_button",
                    onClick = onMiniGameCollectionClicked,
                    modifier = Modifier.fillMaxWidth()
                )
                HomeMenuCardButton(
                    title = "Lịch sử gọi bài",
                    subtitle = "Theo dõi lượt gọi & trả bài",
                    icon = Icons.Default.History,
                    accentColor = Color(0xFF8338EC),
                    testTag = "home_history_button",
                    onClick = onHistoryClicked,
                    modifier = Modifier.fillMaxWidth()
                )
                HomeMenuCardButton(
                    title = "Cài đặt",
                    subtitle = "Âm thanh, chế độ & sao lưu",
                    icon = Icons.Default.Settings,
                    accentColor = Color(0xFFEF476F),
                    testTag = "home_settings_button",
                    onClick = onSettingsClicked,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeMenuCardButton(
                        title = "Quản lý lớp",
                        subtitle = "Danh sách lớp & HS",
                        icon = Icons.Default.Groups,
                        accentColor = Color(0xFF118AB2),
                        testTag = "home_manage_classes_button",
                        onClick = onManageClassesClicked,
                        modifier = Modifier.weight(1f)
                    )
                    HomeMenuCardButton(
                        title = "Kho Mini-game",
                        subtitle = "30 trò chơi 2D vui nhộn",
                        icon = Icons.Default.SportsEsports,
                        accentColor = Color(0xFF06D6A0),
                        testTag = "home_minigame_collection_button",
                        onClick = onMiniGameCollectionClicked,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeMenuCardButton(
                        title = "Lịch sử",
                        subtitle = "Lượt gọi & trả bài",
                        icon = Icons.Default.History,
                        accentColor = Color(0xFF8338EC),
                        testTag = "home_history_button",
                        onClick = onHistoryClicked,
                        modifier = Modifier.weight(1f)
                    )
                    HomeMenuCardButton(
                        title = "Cài đặt",
                        subtitle = "Âm thanh & sao lưu",
                        icon = Icons.Default.Settings,
                        accentColor = Color(0xFFEF476F),
                        testTag = "home_settings_button",
                        onClick = onSettingsClicked,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Menu Giới thiệu tác giả AnonyViet.Com
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onAboutAuthorClicked)
                    .testTag("home_about_author_button"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF6B35).copy(alpha = 0.16f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Giới thiệu tác giả AnonyViet.Com",
                                tint = Color(0xFFFF6B35),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Giới thiệu tác giả: AnonyViet.Com",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Thông tin nhà phát triển, sứ mệnh giáo dục & cộng đồng AnonyViet.Com",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (activeClassroomSummary == null) {
                OutlinedButton(
                    onClick = onCreateDemoClassClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("home_create_demo_button"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.School, contentDescription = "Tạo lớp mẫu")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dùng thử nhanh với Lớp 5A Mẫu (12 HS)")
                }
            }
        }
    }
}

@Composable
private fun MascotOwlCartoonCanvas(
    bounceY: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f + bounceY

        drawCircle(
            color = Color(0xFFFFD166).copy(alpha = 0.22f),
            radius = size.minDimension * 0.48f,
            center = Offset(cx, cy)
        )

        drawOval(
            color = Color(0xFFFF6B35),
            topLeft = Offset(cx - 44f, cy - 34f),
            size = Size(88f, 92f)
        )
        drawOval(
            color = Color(0xFFFFEDD8),
            topLeft = Offset(cx - 28f, cy - 6f),
            size = Size(56f, 54f)
        )
        drawCircle(color = Color.White, radius = 18f, center = Offset(cx - 18f, cy - 16f))
        drawCircle(color = Color.White, radius = 18f, center = Offset(cx + 18f, cy - 16f))
        drawCircle(color = Color(0xFF073B4C), radius = 8f, center = Offset(cx - 16f, cy - 16f))
        drawCircle(color = Color(0xFF073B4C), radius = 8f, center = Offset(cx + 16f, cy - 16f))

        val beak = Path().apply {
            moveTo(cx - 8f, cy - 10f)
            lineTo(cx + 8f, cy - 10f)
            lineTo(cx, cy + 4f)
            close()
        }
        drawPath(path = beak, color = Color(0xFFFFB703))

        drawRoundRect(
            color = Color(0xFF06D6A0),
            topLeft = Offset(cx - 38f, cy - 48f),
            size = Size(76f, 12f),
            cornerRadius = CornerRadius(6f, 6f)
        )
    }
}

@Composable
private fun HomeMenuCardButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .heightIn(min = 128.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.16f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 2. MÀN HÌNH CHỌN LỚP (CLASS LIST SCREEN)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassListScreen(
    summaries: List<ClassroomSummary>,
    onStartClassroom: (String) -> Unit,
    onManageRoster: (String) -> Unit,
    onCreateClassroom: (String, String) -> Unit,
    onRenameClassroom: (String, String, String) -> Unit,
    onDuplicateClassroom: (String) -> Unit,
    onDeleteClassroom: (String) -> Unit,
    onCreateDemoClassroom: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingSummary by remember { mutableStateOf<ClassroomSummary?>(null) }
    var deletingSummary by remember { mutableStateOf<ClassroomSummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quản lý & Chọn lớp học",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("class_list_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_classroom_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm lớp mới")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Thêm lớp", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (summaries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = "Chưa có lớp",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Chưa có lớp học nào",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hãy tạo lớp học đầu tiên của bạn để thêm danh sách học sinh và bắt đầu quay số bằng các mini-game vui nhộn!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .testTag("empty_create_class_button"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tạo lớp đầu tiên")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tạo lớp học đầu tiên")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onCreateDemoClassroom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("Tạo lớp mẫu 5A (12 học sinh) để thử ngay")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(summaries, key = { it.classroom.id }) { item ->
                        ClassroomItemCard(
                            summary = item,
                            onStart = { onStartClassroom(item.classroom.id) },
                            onManageStudents = { onManageRoster(item.classroom.id) },
                            onEdit = { editingSummary = item },
                            onDuplicate = { onDuplicateClassroom(item.classroom.id) },
                            onDelete = { deletingSummary = item }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(84.dp)) }
                }
            }
        }
    }

    if (showCreateDialog) {
        ClassroomFormDialog(
            title = "Tạo lớp học mới",
            initialName = "",
            initialNote = "",
            confirmLabel = "Tạo lớp",
            onConfirm = { name, note ->
                showCreateDialog = false
                onCreateClassroom(name, note)
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    editingSummary?.let { target ->
        ClassroomFormDialog(
            title = "Đổi tên / Sửa thông tin lớp",
            initialName = target.classroom.name,
            initialNote = target.classroom.gradeNote,
            confirmLabel = "Lưu thay đổi",
            onConfirm = { name, note ->
                val id = target.classroom.id
                editingSummary = null
                onRenameClassroom(id, name, note)
            },
            onDismiss = { editingSummary = null }
        )
    }

    deletingSummary?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingSummary = null },
            title = { Text("Xác nhận xóa lớp học") },
            text = {
                Text(
                    "Bạn có chắc chắn muốn xóa lớp \"${target.classroom.name}\" (${target.totalStudents} học sinh)? Lịch sử cũ vẫn được lưu snapshot an toàn."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.classroom.id
                        deletingSummary = null
                        onDeleteClassroom(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa lớp")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSummary = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun ClassroomItemCard(
    summary: ClassroomSummary,
    onStart: () -> Unit,
    onManageStudents: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("classroom_card_${summary.classroom.id}"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = summary.classroom.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Chu kỳ #${summary.classroom.currentCycleNumber}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (summary.classroom.gradeNote.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = summary.classroom.gradeNote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Sửa tên lớp")
                    }
                    IconButton(onClick = onDuplicate) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Sao chép lớp")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Xóa lớp",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Các chỉ số trong lớp với nhãn ngắn gọn 1 dòng không bao giờ bị ép chữ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPill(
                    label = "Sĩ số",
                    value = "${summary.totalStudents}",
                    color = Color(0xFF118AB2),
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Đã gọi",
                    value = "${summary.calledInCycleCount}",
                    color = Color(0xFF06D6A0),
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Chưa gọi",
                    value = "${summary.uncalledInCycleCount}",
                    color = Color(0xFFFF6B35),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("start_class_${summary.classroom.id}"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Bắt đầu", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Vào lớp",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FilledTonalButton(
                    onClick = onManageStudents,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("manage_roster_${summary.classroom.id}"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Groups, contentDescription = "Quản lý danh sách", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Danh sách HS",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ClassroomFormDialog(
    title: String,
    initialName: String,
    initialNote: String,
    confirmLabel: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var note by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên lớp (VD: Lớp 6A1, Toán 9B)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("classroom_name_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú môn học / năm học (Tùy chọn)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.ifBlank { "Lớp mới" }, note) },
                modifier = Modifier.testTag("confirm_classroom_dialog_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

/**
 * 3. MÀN HÌNH CHÍNH CỦA LỚP (CLASSROOM DASHBOARD SCREEN)
 * Thiết kế lại lưới thống kê 2x2 rộng rãi, nút bấm lớn chống tràn chữ và thanh chọn số lượng rõ ràng.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClassroomDashboardScreen(
    summary: ClassroomSummary?,
    selectedMiniGame: MiniGameDescriptor,
    settings: AppSettings,
    recentHistory: List<SelectionHistoryEntity>,
    onPickImmediately: () -> Unit,
    onPlayMiniGame: () -> Unit,
    onChangeMiniGame: () -> Unit,
    onQuickSelectMiniGame: (String) -> Unit = {},
    onOpenStudentRoster: () -> Unit,
    onOpenHistory: () -> Unit,
    onUndoLastTurn: () -> Unit,
    onResetCycle: () -> Unit,
    onChangeSelectionMode: (SelectionMode) -> Unit,
    onChangePickCount: (Int) -> Unit,
    onSwitchClass: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showResetConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = summary?.classroom?.name ?: "Màn hình lớp học",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    TextButton(onClick = onSwitchClass) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Đổi lớp")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đổi lớp")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (summary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = onSwitchClass) {
                    Text("Chọn hoặc tạo lớp học")
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Thẻ tổng quan thống kê lớp học (Lưới 2x2 rộng rãi thay vì 4 cột hẹp)
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = summary.classroom.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Chu kỳ #${summary.classroom.currentCycleNumber} • ${settings.selectionMode.titleVi}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    if (settings.confirmBeforeResetCycle) {
                                        showResetConfirm = true
                                    } else {
                                        onResetCycle()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Đặt lại chu kỳ", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chu kỳ mới", maxLines = 1)
                            }
                        }

                        // Lưới 2x2 rộng rãi cho 4 chỉ số thống kê
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatPill(
                                label = "Tổng sĩ số lớp",
                                value = "${summary.totalStudents}",
                                color = Color(0xFF118AB2),
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                label = "Chưa được gọi",
                                value = "${summary.uncalledInCycleCount}",
                                color = Color(0xFFFF6B35),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatPill(
                                label = "Đã gọi trong chu kỳ",
                                value = "${summary.calledInCycleCount}",
                                color = Color(0xFF06D6A0),
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                label = "Vắng mặt / Loại trừ",
                                value = "${summary.absentOrExcludedCount}",
                                color = Color(0xFFEF476F),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Thẻ Mini-game hiện tại + Nút Chơi Mini-game & Chọn người ngay
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFF073B4C)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(selectedMiniGame.primaryColorHex),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = selectedMiniGame.iconEmoji,
                                        style = MaterialTheme.typography.headlineLarge
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mini-game: ${selectedMiniGame.nameVi}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFFFFD166)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = selectedMiniGame.shortDescriptionVi,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }

                        // Thanh chọn nhanh các Mini-Game nổi bật ngay trên màn hình lớp
                        val quickGames = remember(settings.recentMiniGameIds) {
                            val featuredIds = (settings.recentMiniGameIds + listOf(
                                "duck_race",
                                "ufo_abduction",
                                "butterfly_net",
                                "lucky_wheel",
                                "rocket_launch",
                                "treasure_chest",
                                "claw_machine",
                                "stage_spotlight"
                            )).distinct().take(6)
                            featuredIds.map { MiniGameCatalog.findById(it) }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Đổi nhanh Mini-Game:",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickGames.forEach { qGame ->
                                    val isSelected = qGame.id == selectedMiniGame.id
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color(0xFFFFD166) else Color.White.copy(alpha = 0.14f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onQuickSelectMiniGame(qGame.id) }
                                            .testTag("quick_minigame_${qGame.id}")
                                    ) {
                                        Text(
                                            text = "${qGame.iconEmoji} ${qGame.nameVi}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF073B4C) else Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = onChangeMiniGame,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp)
                                .testTag("change_minigame_button"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Extension, contentDescription = "Đổi mini-game", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Xem tất cả 30 Mini-Game", fontWeight = FontWeight.Bold)
                        }

                        // Hai nút hành động chính xếp toàn chiều ngang hoặc 2 cột thoáng đãng
                        Button(
                            onClick = onPlayMiniGame,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 58.dp)
                                .testTag("play_minigame_button"),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.SportsEsports, contentDescription = "Chơi mini-game", modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "CHƠI MINI-GAME (${selectedMiniGame.nameVi.uppercase()})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onPickImmediately,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .testTag("pick_immediately_button"),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF06D6A0),
                                contentColor = Color(0xFF073B4C)
                            )
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = "Chọn người ngay", modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CHỌN NGƯỜI NGAY (BỎ QUA HOẠT ẢNH)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Tùy chọn nhanh Chế độ chọn (A - F) và Số lượng người gọi
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Chế độ chọn ngẫu nhiên",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectionMode.entries.forEach { mode ->
                                val selected = settings.selectionMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = { onChangeSelectionMode(mode) },
                                    label = {
                                        Text(
                                            text = mode.titleVi,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        Text(
                            text = "Số lượng học sinh gọi mỗi lượt:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 2, 3, 4, 5).forEach { count ->
                                val active = settings.pickCount == count
                                if (active) {
                                    Button(
                                        onClick = { onChangePickCount(count) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 44.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("$count HS", fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onChangePickCount(count) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 44.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("$count", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3 nút quản trị lớp: Danh sách lớp, Lịch sử, Hoàn tác lượt gần nhất
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenStudentRoster,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .testTag("manage_students_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = "Danh sách lớp", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Danh sách lớp", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FilledTonalButton(
                        onClick = onOpenHistory,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .testTag("class_history_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Lịch sử", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lịch sử lớp", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                OutlinedButton(
                    onClick = onUndoLastTurn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("undo_latest_button"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Hoàn tác lượt gần nhất")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hoàn tác lượt gọi gần nhất", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                // Lịch sử gần đây trên màn hình chính
                if (settings.showHistoryOnClassScreen && recentHistory.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Các lượt gọi gần đây trong lớp",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val fmt = remember { SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()) }
                            recentHistory.take(5).forEach { rec ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${rec.studentNameSnapshot} (${rec.miniGameNameSnapshot})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${rec.statusEnum.labelVi} • ${fmt.format(Date(rec.calledAt))}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Đặt lại chu kỳ gọi mới?") },
            text = {
                Text("Tất cả học sinh đủ điều kiện trong lớp sẽ được đặt lại trạng thái chưa gọi cho chu kỳ mới.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetCycle()
                    }
                ) {
                    Text("Bắt đầu chu kỳ mới")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
