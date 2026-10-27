package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.models.AnswerStatus
import com.example.models.AppSettings
import com.example.models.ClassroomEntity
import com.example.models.SelectionHistoryEntity
import com.example.models.SelectionMode
import com.example.services.SaveManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * VIII. MÀN HÌNH LỊCH SỬ VÀ TRẠNG THÁI TRẢ BÀI (HISTORY SCREEN)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    allHistory: List<SelectionHistoryEntity>,
    classrooms: List<ClassroomEntity>,
    studentFilterId: String?,
    onClearStudentFilter: () -> Unit,
    onUpdateRecordStatus: (String, AnswerStatus) -> Unit,
    onUndoTurn: (String) -> Unit,
    onClearHistory: (String?) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<AnswerStatus?>(null) }
    var todayOnly by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }
    var csvPreviewText by remember { mutableStateOf<String?>(null) }

    val filteredHistory = remember(allHistory, selectedClassId, selectedStatus, todayOnly, studentFilterId) {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        allHistory.filter { rec ->
            val matchClass = selectedClassId == null || rec.classroomId == selectedClassId
            val matchStatus = selectedStatus == null || rec.statusEnum == selectedStatus
            val matchToday = !todayOnly || rec.calledAt >= startOfToday
            val matchStudent = studentFilterId == null || rec.studentId == studentFilterId
            matchClass && matchStatus && matchToday && matchStudent
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Lịch sử gọi tên (${filteredHistory.size})",
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
                    IconButton(
                        onClick = {
                            val csv = SaveManager.exportHistoryToCsv(filteredHistory)
                            csvPreviewText = csv
                        },
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Xuất CSV")
                    }
                    IconButton(
                        onClick = { showConfirmDelete = true },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Xóa lịch sử",
                            tint = MaterialTheme.colorScheme.error
                        )
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (studentFilterId != null) {
                    FilledTonalButton(
                        onClick = onClearStudentFilter,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Đang lọc theo 1 học sinh • Bấm để xem tất cả")
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedClassId == null,
                        onClick = { selectedClassId = null },
                        label = { Text("Mọi lớp") }
                    )
                    classrooms.forEach { c ->
                        FilterChip(
                            selected = selectedClassId == c.id,
                            onClick = { selectedClassId = if (selectedClassId == c.id) null else c.id },
                            label = { Text(c.name) }
                        )
                    }
                    FilterChip(
                        selected = todayOnly,
                        onClick = { todayOnly = !todayOnly },
                        label = { Text("Hôm nay") }
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("Mọi trạng thái") }
                    )
                    AnswerStatus.entries.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = if (selectedStatus == st) null else st },
                            label = { Text(st.labelVi) }
                        )
                    }
                }

                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có bản ghi lịch sử nào khớp bộ lọc.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val fmt = remember { SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()) }
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredHistory, key = { it.id }) { rec ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = rec.studentNameSnapshot,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${rec.classroomNameSnapshot} • ${rec.miniGameNameSnapshot} • Chu kỳ #${rec.cycleNumberSnapshot}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${fmt.format(Date(rec.calledAt))} • ${rec.selectionModeSnapshot}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onUndoTurn(rec.turnId) }) {
                                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Hoàn tác lượt này")
                                        }
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        AnswerStatus.entries.forEach { st ->
                                            FilterChip(
                                                selected = rec.statusEnum == st,
                                                onClick = { onUpdateRecordStatus(rec.id, st) },
                                                label = { Text(st.labelVi) }
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

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Xác nhận xóa lịch sử?") },
            text = {
                Text(
                    if (selectedClassId != null) {
                        "Bạn có chắc muốn xóa lịch sử của lớp đang chọn?"
                    } else {
                        "Bạn có chắc muốn xóa toàn bộ lịch sử gọi tên?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDelete = false
                        onClearHistory(selectedClassId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa lịch sử")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    csvPreviewText?.let { csv ->
        AlertDialog(
            onDismissRequest = { csvPreviewText = null },
            title = { Text("Xuất dữ liệu lịch sử CSV") },
            text = {
                OutlinedTextField(
                    value = csv,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "Lich_Su_Ai_Len_Bang.csv")
                            putExtra(Intent.EXTRA_TEXT, csv)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ CSV"))
                        csvPreviewText = null
                    }
                ) {
                    Text("Chia sẻ / Lưu CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { csvPreviewText = null }) {
                    Text("Đóng")
                }
            }
        )
    }
}

/**
 * XI. MÀN HÌNH CÀI ĐẶT & SAO LƯU KHÔI PHỤC (SETTINGS SCREEN)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onResetDefaultSettings: () -> Unit,
    onGenerateBackupJson: suspend () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onOpenAboutAuthor: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    var backupDialogJson by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputJson by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cài đặt & Tùy chỉnh",
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
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Âm thanh & Chế độ yên lặng khi giảng bài
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🔊 Âm thanh & Hiệu ứng",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        SettingSwitchRow(
                            title = "Chế độ yên lặng khi đang giảng bài",
                            subtitle = "Tắt nhanh mọi âm thanh chỉ với 1 chạm",
                            checked = settings.quietTeachingMode,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(quietTeachingMode = v) } }
                        )
                        SettingSwitchRow(
                            title = "Hiệu ứng âm thanh (SFX)",
                            subtitle = "Âm thanh bắt đầu, hồi hộp và pháo hoa công bố tên",
                            checked = settings.sfxEnabled,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(sfxEnabled = v) } }
                        )
                        SettingSwitchRow(
                            title = "Nhạc nền (BGM)",
                            subtitle = "Nhạc nền vui nhộn trong màn hình chờ",
                            checked = settings.bgmEnabled,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(bgmEnabled = v) } }
                        )
                        Text("Âm lượng: ${(settings.volume * 100).toInt()}%")
                        Slider(
                            value = settings.volume,
                            onValueChange = { v -> onUpdateSettings { it.copy(volume = v) } },
                            valueRange = 0f..1f
                        )
                    }
                }

                // 2. Quy tắc chọn học sinh & Hoạt ảnh Mini-Game
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🎲 Quy tắc chọn & Hoạt ảnh Mini-Game",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SelectionMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.selectionMode == mode,
                                    onClick = { onUpdateSettings { it.copy(selectionMode = mode) } },
                                    label = { Text(mode.titleVi) }
                                )
                            }
                        }
                        Text("Thời lượng hoạt ảnh Mini-Game: ${settings.animationDurationSeconds} giây")
                        Slider(
                            value = settings.animationDurationSeconds.toFloat(),
                            onValueChange = { v ->
                                onUpdateSettings { it.copy(animationDurationSeconds = v.toInt().coerceIn(3, 12)) }
                            },
                            valueRange = 3f..12f,
                            steps = 8
                        )
                        SettingSwitchRow(
                            title = "Tự động bắt đầu hoạt ảnh khi vào Mini-Game",
                            subtitle = "Nếu tắt, giáo viên sẽ chủ động bấm nút Bắt đầu",
                            checked = settings.autoStartMiniGame,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(autoStartMiniGame = v) } }
                        )
                        SettingSwitchRow(
                            title = "Xác nhận trước khi đặt lại chu kỳ chọn",
                            subtitle = "Tránh vô tình đặt lại chu kỳ khi đang gọi bài",
                            checked = settings.confirmBeforeResetCycle,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(confirmBeforeResetCycle = v) } }
                        )
                    }
                }

                // 3. Giao diện, Chế độ trình chiếu TV/Máy chiếu
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🖥️ Trình chiếu & Hiển thị",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        SettingSwitchRow(
                            title = "Chế độ trình chiếu (TV / Máy chiếu lớp học)",
                            subtitle = "Phóng đại kích thước chữ và bảng tên để học sinh cuối lớp đọc rõ",
                            checked = settings.presentationMode,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(presentationMode = v) } }
                        )
                        SettingSwitchRow(
                            title = "Giao diện Tối (Bảng đen lớp học)",
                            subtitle = "Chuyển sang tông màu tối dịu mắt khi chiếu trên màn hình lớn",
                            checked = settings.darkMode,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(darkMode = v) } }
                        )
                        SettingSwitchRow(
                            title = "Hiện lịch sử gần đây trên màn hình chính của lớp",
                            subtitle = "Hiển thị 5 học sinh vừa được gọi ngay dưới bảng điều khiển lớp",
                            checked = settings.showHistoryOnClassScreen,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(showHistoryOnClassScreen = v) } }
                        )
                    }
                }

                // 4. Sao lưu & Khôi phục dữ liệu cục bộ (Offline 100%)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "💾 Sao lưu & Khôi phục dữ liệu (Offline)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mọi dữ liệu lớp học, học sinh và lịch sử được lưu cục bộ trên thiết bị, hoàn toàn riêng tư và không gửi lên mạng.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        backupDialogJson = onGenerateBackupJson()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp)
                                    .testTag("backup_data_button"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = "Sao lưu", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sao lưu JSON", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { showRestoreDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp)
                                    .testTag("restore_data_button"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = "Khôi phục", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Khôi phục", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // 5. Giới thiệu tác giả AnonyViet.Com
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "👨‍💻 Tác giả & Đơn vị phát triển: AnonyViet.Com",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ứng dụng \"Ai Lên Bảng?\" được phát triển bởi AnonyViet.Com nhằm mang lại giờ học sôi nổi, công bằng và tràn ngập tiếng cười cho giáo viên & học sinh Việt Nam.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(
                            onClick = onOpenAboutAuthor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("settings_open_about_author_button"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Xem Giới thiệu Tác giả AnonyViet.Com",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onResetDefaultSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("reset_default_settings_button"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("Khôi phục cài đặt mặc định", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    backupDialogJson?.let { json ->
        AlertDialog(
            onDismissRequest = { backupDialogJson = null },
            title = { Text("Bản sao lưu dữ liệu JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sao chép nội dung JSON dưới đây để lưu trữ hoặc chuyển sang thiết bị khác:")
                    OutlinedTextField(
                        value = json,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { backupDialogJson = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Khôi phục dữ liệu từ bản sao lưu") },
            text = {
                OutlinedTextField(
                    value = restoreInputJson,
                    onValueChange = { restoreInputJson = it },
                    label = { Text("Dán chuỗi JSON sao lưu vào đây...") },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDialog = false
                        onRestoreBackupJson(restoreInputJson)
                    }
                ) {
                    Text("Khôi phục ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

/**
 * MÀN HÌNH GIỚI THIỆU TÁC GIẢ ANONYVIET.COM
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAuthorScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Giới thiệu tác giả",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("about_author_back_button")
                    ) {
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
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Thẻ Banner chính AnonyViet.Com
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("author_banner_card"),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🌟 TÁC GIẢ & ĐƠN VỊ PHÁT TRIỂN",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "AnonyViet.Com",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("author_name_title")
                        )
                        Text(
                            text = "Chia sẻ Tri thức • Công nghệ • Phần mềm & Thủ thuật Hữu ích cho Cộng đồng Việt",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Thẻ Giới thiệu về AnonyViet.Com
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "📖 Đôi nét về AnonyViet.Com",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AnonyViet.Com là chuyên trang công nghệ uy tín tại Việt Nam, nơi chia sẻ kiến thức tin học, thủ thuật máy tính – điện thoại, phần mềm tiện ích và các giải pháp công nghệ thiết thực phục vụ học tập, giảng dạy và làm việc mỗi ngày.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Với tinh thần cởi mở và hướng tới cộng đồng, AnonyViet.Com luôn nỗ lực sáng tạo những công cụ đơn giản, trực quan, dễ tiếp cận và hoàn toàn miễn phí cho người dùng Việt.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Thẻ Sứ mệnh của ứng dụng Ai Lên Bảng?
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🎓 Về ứng dụng \"Ai Lên Bảng?\"",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ứng dụng \"Ai Lên Bảng?\" được đội ngũ AnonyViet.Com thiết kế riêng cho quý thầy cô giáo Tiểu học, THCS, THPT, giảng viên và gia sư nhằm biến mỗi lượt gọi tên trả bài hay phát biểu trên lớp thành một trò chơi hào hứng, giảm áp lực tâm lý cho học sinh.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "• Công bằng & Minh bạch 100%: Sử dụng thuật toán chọn ngẫu nhiên chuẩn xác kết hợp khóa lượt chống trùng lặp.\n" +
                                "• Hoạt động Offline hoàn toàn: Không cần kết nối mạng khi giảng dạy trên lớp.\n" +
                                "• Tối ưu trình chiếu: Hỗ trợ cả màn hình dọc trên điện thoại và màn hình ngang chữ lớn khi chiếu lên TV hoặc máy chiếu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Nút mở Website AnonyViet.Com và Quay lại xếp rộng rãi
                Button(
                    onClick = {
                        runCatching {
                            uriHandler.openUri("https://anonyviet.com")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .testTag("visit_anonyviet_website_button"),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Truy cập Website AnonyViet.Com",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text("Quay lại màn hình trước", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Text(
                    text = "Phiên bản 1.0 • Bản quyền phát triển bởi AnonyViet.Com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
