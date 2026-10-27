package com.example.core

import com.example.models.ClassroomEntity
import com.example.models.LockedSelectionTurn
import com.example.models.MiniGameDescriptor
import com.example.models.SelectionMode
import com.example.models.SelectionRequestOutcome
import com.example.models.StudentEntity
import java.security.SecureRandom
import java.util.UUID
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/**
 * Hệ thống trung tâm chọn học sinh ngẫu nhiên (StudentSelectionService).
 *
 * Nguyên tắc kiến trúc:
 * - Mọi mini-game và nút "Chọn người ngay" đều gọi chung service này.
 * - Mini-game không tự ý random, không tự ý thay đổi kết quả đã khóa, không ghi lịch sử trùng.
 * - Hỗ trợ chế độ seed cố định cho môi trường kiểm thử (Unit Test / Statistical Test),
 *   và dùng SecureRandom chuẩn xác trong môi trường thực tế.
 */
class StudentSelectionService(
    private val randomProvider: () -> Random = { SecureRandom().asKotlinRandom() }
) {
    private val lock = Any()
    private var activeTurn: LockedSelectionTurn? = null
    private val committedTurnIds = mutableSetOf<String>()

    fun getActiveTurn(): LockedSelectionTurn? = synchronized(lock) { activeTurn }

    fun isTurnLocked(): Boolean = synchronized(lock) { activeTurn != null }

    /**
     * Lọc danh sách học sinh đủ điều kiện theo chế độ hiện tại.
     */
    fun getEligibleCandidates(
        allStudents: List<StudentEntity>,
        mode: SelectionMode
    ): List<StudentEntity> {
        val basePool = allStudents.filter { it.canBeSelected(mode) }
        return when (mode) {
            SelectionMode.NORMAL_RANDOM,
            SelectionMode.ALLOW_REPEAT,
            SelectionMode.MULTI_PICK,
            SelectionMode.MANUAL_SUBSET -> basePool

            SelectionMode.NO_REPEAT_IN_CYCLE,
            SelectionMode.PRIORITIZE_UNCALLED -> {
                basePool.filter { it.selectedCountInCycle == 0 }
            }
        }
    }

    /**
     * Yêu cầu bắt đầu một lượt chọn học sinh và khóa kết quả trước khi chạy hoạt ảnh.
     */
    fun startSelectionTurn(
        classroom: ClassroomEntity,
        allStudents: List<StudentEntity>,
        mode: SelectionMode,
        requestedCount: Int,
        miniGame: MiniGameDescriptor,
        maxVisualParticipants: Int = 12
    ): SelectionRequestOutcome = synchronized(lock) {
        activeTurn?.let { current ->
            return SelectionRequestOutcome.Busy(
                activeTurnId = current.turnId,
                messageVi = "Đang có một lượt chọn đang chạy. Vui lòng hoàn tất hoặc hủy lượt hiện tại."
            )
        }

        val count = requestedCount.coerceAtLeast(1)

        if (allStudents.isEmpty()) {
            return SelectionRequestOutcome.InvalidPool(
                requestedCount = count,
                availableCount = 0,
                messageVi = "Lớp hiện chưa có học sinh nào. Hãy thêm học sinh trước khi bắt đầu."
            )
        }

        val baseActiveEligible = allStudents.filter { it.canBeSelected(mode) }
        if (baseActiveEligible.isEmpty()) {
            val reason = if (mode == SelectionMode.MANUAL_SUBSET) {
                "Chưa có học sinh nào được đánh dấu trong nhóm chọn thủ công (hoặc tất cả đang vắng mặt)."
            } else {
                "Tất cả học sinh trong lớp hiện đang vắng mặt hoặc nằm trong danh sách tạm loại trừ."
            }
            return SelectionRequestOutcome.InvalidPool(
                requestedCount = count,
                availableCount = 0,
                messageVi = reason
            )
        }

        val candidatePool = when (mode) {
            SelectionMode.NO_REPEAT_IN_CYCLE,
            SelectionMode.PRIORITIZE_UNCALLED -> {
                val uncalled = baseActiveEligible.filter { it.selectedCountInCycle == 0 }
                if (uncalled.isEmpty()) {
                    return SelectionRequestOutcome.CycleExhausted(
                        classroomId = classroom.id,
                        cycleNumber = classroom.currentCycleNumber,
                        eligibleCount = baseActiveEligible.size,
                        messageVi = "Tất cả ${baseActiveEligible.size} học sinh đủ điều kiện đã được gọi trong chu kỳ #${classroom.currentCycleNumber}. Bạn có muốn bắt đầu chu kỳ mới không?"
                    )
                }
                uncalled
            }
            else -> baseActiveEligible
        }

        if (count > candidatePool.size) {
            return SelectionRequestOutcome.InvalidPool(
                requestedCount = count,
                availableCount = candidatePool.size,
                messageVi = "Cần chọn $count học sinh nhưng hiện chỉ còn ${candidatePool.size} học sinh đủ điều kiện trong lượt này. Vui lòng giảm số lượng chọn hoặc đặt lại chu kỳ."
            )
        }

        // Fisher-Yates shuffle thông qua bộ sinh số ngẫu nhiên công bằng
        val rng = randomProvider()
        val shuffledPool = candidatePool.toMutableList()
        for (i in shuffledPool.lastIndex downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = shuffledPool[i]
            shuffledPool[i] = shuffledPool[j]
            shuffledPool[j] = tmp
        }

        val winners = shuffledPool.take(count)

        // Tạo danh sách nhân vật hiển thị trong mini-game (luôn bao gồm tất cả winners)
        val remainingDecoys = (baseActiveEligible.filterNot { candidate ->
            winners.any { w -> w.id == candidate.id }
        }).shuffled(rng)

        val targetVisualCount = maxOf(winners.size, minOf(baseActiveEligible.size, maxVisualParticipants))
        val visualParticipants = (winners + remainingDecoys.take(targetVisualCount - winners.size)).shuffled(rng)

        val locked = LockedSelectionTurn(
            turnId = UUID.randomUUID().toString(),
            classroomId = classroom.id,
            classroomName = classroom.name,
            winners = winners,
            visualParticipants = visualParticipants,
            selectionMode = mode,
            cycleNumber = classroom.currentCycleNumber,
            miniGame = miniGame,
            isManualDirectPick = false
        )
        activeTurn = locked
        return SelectionRequestOutcome.Success(locked)
    }

    /**
     * Chế độ chọn trực tiếp minh bạch (ví dụ: Giáo viên chủ động chạm bắt một cánh bướm cụ thể trong game Vợt Bướm).
     * Đánh dấu rõ isManualDirectPick = true để minh bạch với chế độ random tự động.
     */
    fun lockDirectStudentPick(
        classroom: ClassroomEntity,
        student: StudentEntity,
        allStudents: List<StudentEntity>,
        mode: SelectionMode,
        miniGame: MiniGameDescriptor
    ): SelectionRequestOutcome = synchronized(lock) {
        if (!student.canBeSelected(mode)) {
            return SelectionRequestOutcome.InvalidPool(
                requestedCount = 1,
                availableCount = 0,
                messageVi = "Học sinh ${student.fullName} hiện đang vắng mặt hoặc bị loại trừ."
            )
        }
        val locked = LockedSelectionTurn(
            turnId = UUID.randomUUID().toString(),
            classroomId = classroom.id,
            classroomName = classroom.name,
            winners = listOf(student),
            visualParticipants = allStudents.filter { it.canBeSelected(mode) }.take(12),
            selectionMode = mode,
            cycleNumber = classroom.currentCycleNumber,
            miniGame = miniGame,
            isManualDirectPick = true
        )
        activeTurn = locked
        return SelectionRequestOutcome.Success(locked)
    }

    /**
     * Đánh dấu lượt hiện tại đã được công bố và ghi lịch sử (chống ghi trùng 2 lần khi bấm nhanh).
     * Trả về LockedSelectionTurn nếu đây là lần commit đầu tiên của turnId đó, ngược lại trả về null.
     */
    fun markTurnCommitted(turnId: String): LockedSelectionTurn? = synchronized(lock) {
        if (committedTurnIds.contains(turnId)) {
            return null
        }
        val current = activeTurn
        if (current != null && current.turnId == turnId) {
            committedTurnIds.add(turnId)
            val updated = current.copy(historyCommitted = true)
            activeTurn = null
            return updated
        }
        return null
    }

    /**
     * Hủy lượt chọn đang chạy (ví dụ khi người dùng thoát giữa chừng trước khi công bố kết quả).
     */
    fun cancelActiveTurn(): Boolean = synchronized(lock) {
        val hadActive = activeTurn != null
        activeTurn = null
        hadActive
    }
}
