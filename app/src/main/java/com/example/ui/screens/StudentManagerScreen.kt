package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.models.ClassroomSummary
import com.example.models.StudentEntity
import com.example.ui.DuplicateNamesPrompt

/**
 * IV. MÀN HÌNH QUẢN LÝ DANH SÁCH HỌC SINH (STUDENT MANAGER SCREEN)
 * Bố cục tối ưu UI/UX: Tên học sinh rộng rãi, thanh công cụ tự động xuống dòng (FlowRow),
 * không có nút bấm nào bị tràn chữ hay che chữ.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudentManagerScreen(
    summary: ClassroomSummary?,
    students: List<StudentEntity>,
    duplicatePrompt: DuplicateNamesPrompt?,
    onAddSingleStudent: (String) -> Unit,
    onBulkImportText: (String) -> Unit,
    onConfirmDuplicateImport: (Boolean) -> Unit,
    onDismissDuplicateImport: () -> Unit,
    onUpdateStudentName: (String, String) -> Unit,
    onDeleteStudent: (String) -> Unit,
    onToggleAbsent: (StudentEntity) -> Unit,
    onToggleExcluded: (StudentEntity) -> Unit,
    onBulkExcludeStudents: (List<String>, Boolean) -> Unit,
    onToggleManualSubset: (StudentEntity) -> Unit,
    onSetAllManualSubset: (Boolean) -> Unit,
    onSortAlphabetically: () -> Unit,
    onShuffleDisplayOrder: () -> Unit,
    onResetCycle: () -> Unit,
    onViewStudentHistory: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var newStudentName by remember { mutableStateOf("") }
    var showBulkPasteDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var deletingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    val selectedStudentIds = remember { mutableStateListOf<String>() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                }
            }.getOrNull()?.let { content ->
                onBulkImportText(content)
            }
        }
    }

    val filteredStudents = remember(students, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) students
        else students.filter { it.fullName.lowercase().contains(q) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (summary != null) {
                            "Danh sách ${summary.classroom.name} (${students.size} HS)"
                        } else {
                            "Danh sách học sinh"
                        },
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
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Hàng thêm nhanh 1 học sinh
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newStudentName,
                        onValueChange = { newStudentName = it },
                        label = { Text("Thêm họ và tên học sinh...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("single_student_name_input")
                    )
                    Button(
                        onClick = {
                            if (newStudentName.isNotBlank()) {
                                onAddSingleStudent(newStudentName)
                                newStudentName = ""
                            }
                        },
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .testTag("add_student_button"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Thêm", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // Hai nút nhập danh sách rộng rãi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showBulkPasteDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("bulk_paste_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Dán danh sách", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dán nhiều dòng", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("text/*") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("import_csv_txt_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Nhập CSV/TXT", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nhập CSV / TXT", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Thanh công cụ sắp xếp, xáo trộn, khôi phục chu kỳ
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = onSortAlphabetically,
                        label = { Text("Sắp xếp A-Z") },
                        leadingIcon = {
                            Icon(Icons.Default.SortByAlpha, contentDescription = "Sắp xếp A-Z", modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.testTag("sort_az_button")
                    )
                    AssistChip(
                        onClick = onShuffleDisplayOrder,
                        label = { Text("Xáo trộn hiển thị") },
                        leadingIcon = {
                            Icon(Icons.Default.Shuffle, contentDescription = "Xáo trộn", modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.testTag("shuffle_display_button")
                    )
                    AssistChip(
                        onClick = onResetCycle,
                        label = { Text("Đặt lại chu kỳ") },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = "Đặt lại chu kỳ", modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.testTag("reset_cycle_button")
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") },
                    label = { Text("Tìm kiếm học sinh theo tên...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_search_input")
                )

                if (selectedStudentIds.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Đã chọn ${selectedStudentIds.size} học sinh:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        onBulkExcludeStudents(selectedStudentIds.toList(), true)
                                        selectedStudentIds.clear()
                                    }
                                ) {
                                    Text("Tạm loại trừ")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onBulkExcludeStudents(selectedStudentIds.toList(), false)
                                        selectedStudentIds.clear()
                                    }
                                ) {
                                    Text("Bỏ loại trừ")
                                }
                                TextButton(onClick = { selectedStudentIds.clear() }) {
                                    Text("Bỏ chọn")
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        val isChecked = selectedStudentIds.contains(student.id)
                        StudentRowCard(
                            student = student,
                            isCheckedForBulk = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedStudentIds.add(student.id)
                                else selectedStudentIds.remove(student.id)
                            },
                            onToggleAbsent = { onToggleAbsent(student) },
                            onToggleExcluded = { onToggleExcluded(student) },
                            onToggleManualSubset = { onToggleManualSubset(student) },
                            onEdit = { editingStudent = student },
                            onDelete = { deletingStudent = student },
                            onViewHistory = { onViewStudentHistory(student.id) }
                        )
                    }
                }
            }
        }
    }

    if (showBulkPasteDialog) {
        BulkPasteStudentsDialog(
            onConfirmPaste = { rawText ->
                showBulkPasteDialog = false
                onBulkImportText(rawText)
            },
            onDismiss = { showBulkPasteDialog = false }
        )
    }

    duplicatePrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = onDismissDuplicateImport,
            title = { Text("Phát hiện tên học sinh trùng") },
            text = {
                Text(
                    "Phát hiện ${prompt.duplicateNames.size} tên trùng (${prompt.duplicateNames.take(5).joinToString(", ")}). " +
                        "Nếu lớp có 2 học sinh trùng tên thật, bạn có thể chọn \"Giữ tất cả\". Hoặc chọn \"Chỉ thêm tên chưa trùng\"."
                )
            },
            confirmButton = {
                Button(onClick = { onConfirmDuplicateImport(true) }) {
                    Text("Giữ tất cả (${prompt.allCleanNames.size})")
                }
            },
            dismissButton = {
                Row {
                    if (prompt.uniqueOnlyNames.isNotEmpty()) {
                        TextButton(onClick = { onConfirmDuplicateImport(false) }) {
                            Text("Chỉ thêm tên mới (${prompt.uniqueOnlyNames.size})")
                        }
                    }
                    TextButton(onClick = onDismissDuplicateImport) {
                        Text("Hủy")
                    }
                }
            }
        )
    }

    editingStudent?.let { target ->
        var updatedName by remember(target.id) { mutableStateOf(target.fullName) }
        AlertDialog(
            onDismissRequest = { editingStudent = null },
            title = { Text("Sửa tên học sinh") },
            text = {
                OutlinedTextField(
                    value = updatedName,
                    onValueChange = { updatedName = it },
                    label = { Text("Họ và tên") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        editingStudent = null
                        onUpdateStudentName(id, updatedName)
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingStudent = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    deletingStudent?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingStudent = null },
            title = { Text("Xóa học sinh?") },
            text = {
                Text("Bạn muốn xóa \"${target.fullName}\" khỏi danh sách lớp? Lịch sử các lượt gọi trước đây của học sinh này vẫn được bảo toàn.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        deletingStudent = null
                        onDeleteStudent(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingStudent = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudentRowCard(
    student: StudentEntity,
    isCheckedForBulk: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onToggleAbsent: () -> Unit,
    onToggleExcluded: () -> Unit,
    onToggleManualSubset: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewHistory: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_card_${student.id}"),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = isCheckedForBulk,
                        onCheckedChange = onCheckedChange
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Chu kỳ này: ${student.selectedCountInCycle} lần • Tổng: ${student.totalSelectedCount} lần",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, contentDescription = "Lịch sử học sinh")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Sửa tên")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Xóa học sinh",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = student.isAbsent,
                    onClick = onToggleAbsent,
                    label = { Text(if (student.isAbsent) "Đang vắng mặt" else "Có mặt") },
                    leadingIcon = {
                        Icon(
                            if (student.isAbsent) Icons.Default.PersonOff else Icons.Default.CheckCircle,
                            contentDescription = "Trạng thái có mặt",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                FilterChip(
                    selected = student.isManuallyExcluded,
                    onClick = onToggleExcluded,
                    label = { Text(if (student.isManuallyExcluded) "Đang tạm loại trừ" else "Đủ điều kiện") }
                )
                FilterChip(
                    selected = student.isInManualSubset,
                    onClick = onToggleManualSubset,
                    label = { Text("Nhóm thủ công (Chế độ F)") },
                    leadingIcon = {
                        Icon(Icons.Default.Checklist, contentDescription = "Nhóm thủ công", modifier = Modifier.size(16.dp))
                    }
                )
            }
        }
    }
}

@Composable
private fun BulkPasteStudentsDialog(
    onConfirmPaste: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var rawText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dán danh sách học sinh hàng loạt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Mỗi dòng 1 họ tên học sinh. Các dòng trống và số thứ tự đầu dòng (VD: 1. Nguyễn Văn A) sẽ được tự động làm sạch.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Danh sách học sinh...") },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bulk_paste_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPaste(rawText) },
                modifier = Modifier.testTag("confirm_bulk_paste_button")
            ) {
                Text("Nhập danh sách")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
