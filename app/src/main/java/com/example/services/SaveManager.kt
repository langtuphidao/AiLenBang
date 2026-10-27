package com.example.services

import com.example.models.ClassroomEntity
import com.example.models.SelectionHistoryEntity
import com.example.models.StudentEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupPayload(
    val classrooms: List<ClassroomEntity>,
    val students: List<StudentEntity>,
    val history: List<SelectionHistoryEntity>
)

/**
 * Quản lý nhập/xuất danh sách học sinh, xuất CSV lịch sử và sao lưu/khôi phục JSON cục bộ.
 * Có kiểm tra toàn vẹn dữ liệu khi đọc và xử lý an toàn khi dữ liệu lỗi.
 */
object SaveManager {

    /**
     * Phân tích danh sách học sinh từ văn bản nhiều dòng hoặc CSV/TXT.
     * Tự động loại bỏ dòng trống, dấu đầu dòng/số thứ tự thừa và phát hiện tên trùng.
     */
    data class ParsedStudentBatch(
        val cleanNames: List<String>,
        val duplicateNamesInInput: List<String>,
        val existingDuplicatesInClass: List<String>
    )

    fun parseStudentLines(
        rawInput: String,
        existingClassNames: List<String>
    ): ParsedStudentBatch {
        val existingNormalized = existingClassNames.map { it.trim().lowercase(Locale.ROOT) }.toSet()
        val lines = rawInput
            .replace("\r\n", "\n")
            .split("\n", ";")
            .map { line ->
                // Nếu là dòng CSV có dấu phẩy và cột đầu là STT, lấy cột tên phù hợp
                val trimmed = line.trim()
                val csvParts = trimmed.split(",").map { it.trim().trim('"') }.filter { it.isNotEmpty() }
                val candidate = when {
                    csvParts.size >= 2 && csvParts[0].all { it.isDigit() } -> csvParts[1]
                    csvParts.size == 1 -> csvParts[0]
                    else -> trimmed
                }
                // Bỏ số thứ tự đầu dòng kiểu "1. Nguyễn Văn A" hoặc "- Nguyễn Văn A"
                candidate.replace(Regex("^(\\d+[.)\\s-]+|[-*•]+\\s*)"), "").trim()
            }
            .filter { it.isNotEmpty() && !it.equals("họ và tên", ignoreCase = true) && !it.equals("ho va ten", ignoreCase = true) && !it.equals("name", ignoreCase = true) }

        val seenInBatch = mutableSetOf<String>()
        val duplicatesInBatch = mutableListOf<String>()
        val duplicatesWithExisting = mutableListOf<String>()

        for (name in lines) {
            val key = name.lowercase(Locale.ROOT)
            if (!seenInBatch.add(key)) {
                duplicatesInBatch.add(name)
            }
            if (existingNormalized.contains(key)) {
                duplicatesWithExisting.add(name)
            }
        }

        return ParsedStudentBatch(
            cleanNames = lines,
            duplicateNamesInInput = duplicatesInBatch.distinct(),
            existingDuplicatesInClass = duplicatesWithExisting.distinct()
        )
    }

    /**
     * Xuất lịch sử gọi tên ra định dạng CSV chuẩn.
     */
    fun exportHistoryToCsv(records: List<SelectionHistoryEntity>): String {
        val sb = StringBuilder()
        sb.append("ID Luot,Lop,Hoc Sinh,Thoi Gian,Mini-Game,Trang Thai Tra Bai,Che Do Chon,Chu Ky\n")
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        for (r in records) {
            val dateStr = fmt.format(Date(r.calledAt))
            sb.append(escapeCsv(r.turnId)).append(",")
                .append(escapeCsv(r.classroomNameSnapshot)).append(",")
                .append(escapeCsv(r.studentNameSnapshot)).append(",")
                .append(escapeCsv(dateStr)).append(",")
                .append(escapeCsv(r.miniGameNameSnapshot)).append(",")
                .append(escapeCsv(r.statusEnum.labelVi)).append(",")
                .append(escapeCsv(r.selectionModeSnapshot)).append(",")
                .append(r.cycleNumberSnapshot)
                .append("\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    /**
     * Sao lưu toàn bộ dữ liệu lớp, học sinh và lịch sử ra chuỗi JSON có kiểm tra phiên bản.
     */
    fun exportBackupJson(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val classesArr = JSONArray()
        payload.classrooms.forEach { c ->
            classesArr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("gradeNote", c.gradeNote)
                    put("currentCycleNumber", c.currentCycleNumber)
                    put("createdAt", c.createdAt)
                    put("updatedAt", c.updatedAt)
                }
            )
        }
        root.put("classrooms", classesArr)

        val studentsArr = JSONArray()
        payload.students.forEach { s ->
            studentsArr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("classroomId", s.classroomId)
                    put("fullName", s.fullName)
                    put("isActive", s.isActive)
                    put("isEligible", s.isEligible)
                    put("isAbsent", s.isAbsent)
                    put("isManuallyExcluded", s.isManuallyExcluded)
                    put("isInManualSubset", s.isInManualSubset)
                    put("selectedCountInCycle", s.selectedCountInCycle)
                    put("totalSelectedCount", s.totalSelectedCount)
                    if (s.lastSelectedAt != null) put("lastSelectedAt", s.lastSelectedAt)
                    put("displayOrder", s.displayOrder)
                }
            )
        }
        root.put("students", studentsArr)

        val historyArr = JSONArray()
        payload.history.forEach { h ->
            historyArr.put(
                JSONObject().apply {
                    put("id", h.id)
                    put("turnId", h.turnId)
                    put("classroomId", h.classroomId)
                    put("classroomNameSnapshot", h.classroomNameSnapshot)
                    put("studentId", h.studentId)
                    put("studentNameSnapshot", h.studentNameSnapshot)
                    put("calledAt", h.calledAt)
                    put("miniGameId", h.miniGameId)
                    put("miniGameNameSnapshot", h.miniGameNameSnapshot)
                    put("answerStatus", h.answerStatus)
                    put("selectionModeSnapshot", h.selectionModeSnapshot)
                    put("cycleNumberSnapshot", h.cycleNumberSnapshot)
                    put("previousSelectedCountInCycle", h.previousSelectedCountInCycle)
                    if (h.previousLastSelectedAt != null) put("previousLastSelectedAt", h.previousLastSelectedAt)
                }
            )
        }
        root.put("history", historyArr)

        return root.toString(2)
    }

    /**
     * Khôi phục dữ liệu từ chuỗi JSON sao lưu, kiểm tra tính hợp lệ và báo lỗi rõ ràng nếu dữ liệu hỏng.
     */
    fun importBackupJson(jsonString: String): Result<BackupPayload> = runCatching {
        val root = JSONObject(jsonString.trim())
        if (!root.has("classrooms") || !root.has("students")) {
            throw IllegalArgumentException("Tệp sao lưu không đúng định dạng của Ai Lên Bảng?")
        }

        val classesArr = root.getJSONArray("classrooms")
        val classrooms = mutableListOf<ClassroomEntity>()
        for (i in 0 until classesArr.length()) {
            val obj = classesArr.getJSONObject(i)
            val id = obj.optString("id").trim()
            val name = obj.optString("name").trim()
            if (id.isNotEmpty() && name.isNotEmpty()) {
                classrooms.add(
                    ClassroomEntity(
                        id = id,
                        name = name,
                        gradeNote = obj.optString("gradeNote", ""),
                        currentCycleNumber = obj.optInt("currentCycleNumber", 1).coerceAtLeast(1),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val studentsArr = root.getJSONArray("students")
        val students = mutableListOf<StudentEntity>()
        for (i in 0 until studentsArr.length()) {
            val obj = studentsArr.getJSONObject(i)
            val id = obj.optString("id").trim()
            val classroomId = obj.optString("classroomId").trim()
            val fullName = obj.optString("fullName").trim()
            if (id.isNotEmpty() && classroomId.isNotEmpty() && fullName.isNotEmpty()) {
                students.add(
                    StudentEntity(
                        id = id,
                        classroomId = classroomId,
                        fullName = fullName,
                        isActive = obj.optBoolean("isActive", true),
                        isEligible = obj.optBoolean("isEligible", true),
                        isAbsent = obj.optBoolean("isAbsent", false),
                        isManuallyExcluded = obj.optBoolean("isManuallyExcluded", false),
                        isInManualSubset = obj.optBoolean("isInManualSubset", false),
                        selectedCountInCycle = obj.optInt("selectedCountInCycle", 0).coerceAtLeast(0),
                        totalSelectedCount = obj.optInt("totalSelectedCount", 0).coerceAtLeast(0),
                        lastSelectedAt = if (obj.has("lastSelectedAt") && !obj.isNull("lastSelectedAt")) obj.optLong("lastSelectedAt") else null,
                        displayOrder = obj.optInt("displayOrder", i)
                    )
                )
            }
        }

        val history = mutableListOf<SelectionHistoryEntity>()
        if (root.has("history")) {
            val historyArr = root.getJSONArray("history")
            for (i in 0 until historyArr.length()) {
                val obj = historyArr.getJSONObject(i)
                val id = obj.optString("id").trim()
                val turnId = obj.optString("turnId").trim()
                if (id.isNotEmpty() && turnId.isNotEmpty()) {
                    history.add(
                        SelectionHistoryEntity(
                            id = id,
                            turnId = turnId,
                            classroomId = obj.optString("classroomId", ""),
                            classroomNameSnapshot = obj.optString("classroomNameSnapshot", ""),
                            studentId = obj.optString("studentId", ""),
                            studentNameSnapshot = obj.optString("studentNameSnapshot", ""),
                            calledAt = obj.optLong("calledAt", System.currentTimeMillis()),
                            miniGameId = obj.optString("miniGameId", "duck_race"),
                            miniGameNameSnapshot = obj.optString("miniGameNameSnapshot", "Đua vịt"),
                            answerStatus = obj.optString("answerStatus", "PENDING"),
                            selectionModeSnapshot = obj.optString("selectionModeSnapshot", "NO_REPEAT_IN_CYCLE"),
                            cycleNumberSnapshot = obj.optInt("cycleNumberSnapshot", 1),
                            previousSelectedCountInCycle = obj.optInt("previousSelectedCountInCycle", 0),
                            previousLastSelectedAt = if (obj.has("previousLastSelectedAt") && !obj.isNull("previousLastSelectedAt")) obj.optLong("previousLastSelectedAt") else null
                        )
                    )
                }
            }
        }

        BackupPayload(classrooms = classrooms, students = students, history = history)
    }
}
