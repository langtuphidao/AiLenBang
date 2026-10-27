package com.example.repositories

import com.example.models.AnswerStatus
import com.example.models.LockedSelectionTurn
import com.example.models.SelectionHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * HistoryRepository lưu trữ lịch sử các lượt gọi học sinh và quản lý trạng thái trả bài, hoàn tác (Undo).
 */
class HistoryRepository(
    private val historyDao: HistoryDao,
    private val classroomRepository: ClassroomRepository
) {
    val allHistory: Flow<List<SelectionHistoryEntity>> = historyDao.observeAllHistory()

    fun observeHistoryForClassroom(classroomId: String): Flow<List<SelectionHistoryEntity>> =
        historyDao.observeHistoryByClassroom(classroomId)

    /**
     * Ghi nhận kết quả lượt chọn vào lịch sử đúng một lần duy nhất cho mỗi turnId.
     * Lưu snapshot tên học sinh và tên lớp tại thời điểm được gọi để khi xóa/sửa học sinh không ảnh hưởng lịch sử cũ.
     */
    suspend fun recordCommittedTurn(
        lockedTurn: LockedSelectionTurn,
        initialStatus: AnswerStatus = AnswerStatus.PENDING
    ): List<SelectionHistoryEntity> {
        val existing = historyDao.getHistoryByTurnId(lockedTurn.turnId)
        if (existing.isNotEmpty()) {
            return existing
        }

        val now = System.currentTimeMillis()
        val records = lockedTurn.winners.map { student ->
            SelectionHistoryEntity(
                id = UUID.randomUUID().toString(),
                turnId = lockedTurn.turnId,
                classroomId = lockedTurn.classroomId,
                classroomNameSnapshot = lockedTurn.classroomName,
                studentId = student.id,
                studentNameSnapshot = student.fullName,
                calledAt = now,
                miniGameId = lockedTurn.miniGame.id,
                miniGameNameSnapshot = lockedTurn.miniGame.nameVi,
                answerStatus = initialStatus.name,
                selectionModeSnapshot = if (lockedTurn.isManualDirectPick) {
                    "Chạm chọn trực tiếp"
                } else {
                    lockedTurn.selectionMode.titleVi
                },
                cycleNumberSnapshot = lockedTurn.cycleNumber,
                previousSelectedCountInCycle = student.selectedCountInCycle,
                previousLastSelectedAt = student.lastSelectedAt
            )
        }

        historyDao.insertHistoryRecords(records)
        classroomRepository.markStudentsSelected(
            studentIds = lockedTurn.winners.map { it.id },
            timestamp = now
        )
        return records
    }

    /**
     * Cập nhật trạng thái trả bài (Đã trả bài / Chưa trả bài / Vắng mặt / Bỏ qua lượt) cho một lượt.
     */
    suspend fun updateTurnAnswerStatus(turnId: String, status: AnswerStatus) {
        val records = historyDao.getHistoryByTurnId(turnId)
        if (records.isEmpty()) return
        historyDao.updateAnswerStatusByTurnId(turnId, status.name)

        if (status == AnswerStatus.ABSENT) {
            records.forEach { rec ->
                classroomRepository.setStudentAbsent(rec.studentId, true)
                classroomRepository.restoreStudentSelectionState(
                    studentId = rec.studentId,
                    previousCountInCycle = rec.previousSelectedCountInCycle,
                    previousLastSelectedAt = rec.previousLastSelectedAt
                )
            }
        } else if (status == AnswerStatus.SKIPPED) {
            records.forEach { rec ->
                classroomRepository.restoreStudentSelectionState(
                    studentId = rec.studentId,
                    previousCountInCycle = rec.previousSelectedCountInCycle,
                    previousLastSelectedAt = rec.previousLastSelectedAt
                )
            }
        }
    }

    suspend fun updateRecordAnswerStatus(recordId: String, status: AnswerStatus) {
        historyDao.updateAnswerStatusByRecordId(recordId, status.name)
    }

    /**
     * Hoàn tác (Undo) một lượt chọn cụ thể theo turnId:
     * Khôi phục số lần được gọi trong chu kỳ của học sinh và xóa bản ghi lịch sử của lượt đó.
     */
    suspend fun undoTurn(turnId: String): Boolean {
        val records = historyDao.getHistoryByTurnId(turnId)
        if (records.isEmpty()) return false
        for (rec in records) {
            classroomRepository.restoreStudentSelectionState(
                studentId = rec.studentId,
                previousCountInCycle = rec.previousSelectedCountInCycle,
                previousLastSelectedAt = rec.previousLastSelectedAt
            )
        }
        historyDao.deleteByTurnId(turnId)
        return true
    }

    /**
     * Hoàn tác lượt gần nhất của một lớp học.
     */
    suspend fun undoLatestTurnForClassroom(classroomId: String): String? {
        val latest = historyDao.getLatestHistoryForClassroom(classroomId) ?: return null
        val success = undoTurn(latest.turnId)
        return if (success) latest.studentNameSnapshot else null
    }

    suspend fun clearHistoryForClassroom(classroomId: String) {
        historyDao.deleteByClassroomId(classroomId)
    }

    suspend fun clearAllHistory() {
        historyDao.deleteAllHistory()
    }

    suspend fun getAllHistoryOnce(): List<SelectionHistoryEntity> =
        historyDao.getAllHistoryOnce()

    suspend fun replaceAllHistoryFromBackup(records: List<SelectionHistoryEntity>) {
        historyDao.deleteAllHistory()
        if (records.isNotEmpty()) {
            historyDao.insertHistoryRecords(records)
        }
    }
}
