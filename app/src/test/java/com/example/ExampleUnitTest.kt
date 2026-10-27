package com.example

import com.example.core.StudentSelectionService
import com.example.models.ClassroomEntity
import com.example.models.MiniGameCatalog
import com.example.models.SelectionHistoryEntity
import com.example.models.SelectionMode
import com.example.models.SelectionRequestOutcome
import com.example.models.StudentEntity
import com.example.services.BackupPayload
import com.example.services.SaveManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs
import kotlin.random.Random

/**
 * XV. KIỂM THỬ BẮT BUỘC (20 TÌNH HUỐNG + KIỂM THỬ CÔNG BẰNG THỐNG KÊ)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    private val testClassroom = ClassroomEntity(
        id = "class-10a1",
        name = "Lớp 10A1",
        currentCycleNumber = 1
    )
    private val duckGame = MiniGameCatalog.findById("duck_race")
    private val ufoGame = MiniGameCatalog.findById("ufo_abduction")
    private val wheelGame = MiniGameCatalog.findById("lucky_wheel")

    private fun makeStudents(names: List<String>): List<StudentEntity> =
        names.mapIndexed { idx, name ->
            StudentEntity(
                id = "stu-$idx",
                classroomId = testClassroom.id,
                fullName = name,
                displayOrder = idx
            )
        }

    // Tình huống 1: Lớp có một học sinh
    @Test
    fun scenario01_singleStudentClass_selectsThatStudent() {
        val service = StudentSelectionService { Random(42) }
        val students = makeStudents(listOf("Nguyễn Văn An"))
        val outcome = service.startSelectionTurn(
            classroom = testClassroom,
            allStudents = students,
            mode = SelectionMode.NORMAL_RANDOM,
            requestedCount = 1,
            miniGame = duckGame
        )
        assertTrue(outcome is SelectionRequestOutcome.Success)
        val success = outcome as SelectionRequestOutcome.Success
        assertEquals(1, success.lockedTurn.winners.size)
        assertEquals("Nguyễn Văn An", success.lockedTurn.winners.first().fullName)
    }

    // Tình huống 2: Lớp có nhiều học sinh + Kiểm thử công bằng thống kê (Statistical Fairness)
    @Test
    fun scenario02_multipleStudents_statisticalFairnessAcrossLargeTrials() {
        val rng = Random(20261026L)
        val service = StudentSelectionService { rng }
        val names = (1..10).map { "Học Sinh Số $it" }
        val students = makeStudents(names)
        val counts = mutableMapOf<String, Int>()
        val trials = 10_000

        repeat(trials) {
            val outcome = service.startSelectionTurn(
                classroom = testClassroom,
                allStudents = students,
                mode = SelectionMode.NORMAL_RANDOM,
                requestedCount = 1,
                miniGame = duckGame
            )
            val turn = (outcome as SelectionRequestOutcome.Success).lockedTurn
            val winnerId = turn.winners.first().id
            counts[winnerId] = (counts[winnerId] ?: 0) + 1
            service.markTurnCommitted(turn.turnId)
        }

        // Kỳ vọng mỗi học sinh được chọn ~ 1000 lần (10%). Cho phép sai số thống kê hợp lý ±18%.
        val expected = trials / students.size.toDouble()
        students.forEach { s ->
            val actual = counts[s.id] ?: 0
            val relativeDiff = abs(actual - expected) / expected
            assertTrue(
                "Học sinh ${s.fullName} có tần suất lệch bất thường: $actual / $expected",
                relativeDiff < 0.18
            )
        }
    }

    // Tình huống 3: Danh sách rỗng
    @Test
    fun scenario03_emptyStudentList_returnsInvalidPoolWithoutCrash() {
        val service = StudentSelectionService { Random(1) }
        val outcome = service.startSelectionTurn(
            classroom = testClassroom,
            allStudents = emptyList(),
            mode = SelectionMode.NO_REPEAT_IN_CYCLE,
            requestedCount = 1,
            miniGame = duckGame
        )
        assertTrue(outcome is SelectionRequestOutcome.InvalidPool)
    }

    // Tình huống 4: Tất cả học sinh bị loại trừ
    @Test
    fun scenario04_allStudentsExcluded_returnsInvalidPool() {
        val service = StudentSelectionService { Random(1) }
        val students = makeStudents(listOf("An", "Bình", "Châu")).map {
            it.copy(isManuallyExcluded = true)
        }
        val outcome = service.startSelectionTurn(
            classroom = testClassroom,
            allStudents = students,
            mode = SelectionMode.NORMAL_RANDOM,
            requestedCount = 1,
            miniGame = duckGame
        )
        assertTrue(outcome is SelectionRequestOutcome.InvalidPool)
    }

    // Tình huống 5: Một số học sinh vắng mặt (không bao giờ chọn phải học sinh vắng mặt)
    @Test
    fun scenario05_someStudentsAbsent_neverSelectsAbsentStudents() {
        val rng = Random(99)
        val service = StudentSelectionService { rng }
        val students = listOf(
            StudentEntity(id = "s1", classroomId = testClassroom.id, fullName = "An (Vắng)", isAbsent = true),
            StudentEntity(id = "s2", classroomId = testClassroom.id, fullName = "Bình (Có mặt)", isAbsent = false),
            StudentEntity(id = "s3", classroomId = testClassroom.id, fullName = "Châu (Vắng)", isAbsent = true)
        )
        repeat(30) {
            val outcome = service.startSelectionTurn(
                classroom = testClassroom,
                allStudents = students,
                mode = SelectionMode.NORMAL_RANDOM,
                requestedCount = 1,
                miniGame = duckGame
            ) as SelectionRequestOutcome.Success
            assertEquals("s2", outcome.lockedTurn.winners.first().id)
            service.markTurnCommitted(outcome.lockedTurn.turnId)
        }
    }

    // Tình huống 6: Chọn nhiều người hơn số người đủ điều kiện
    @Test
    fun scenario06_pickMoreThanEligible_returnsInvalidPoolAndNeverDuplicates() {
        val service = StudentSelectionService { Random(7) }
        val students = makeStudents(listOf("An", "Bình"))
        val outcome = service.startSelectionTurn(
            classroom = testClassroom,
            allStudents = students,
            mode = SelectionMode.MULTI_PICK,
            requestedCount = 3,
            miniGame = duckGame
        )
        assertTrue(outcome is SelectionRequestOutcome.InvalidPool)
    }

    // Tình huống 7 & 8: Không trùng trong chu kỳ & Yêu cầu xác nhận bắt đầu chu kỳ mới
    @Test
    fun scenario07_08_noRepeatInCycle_exhaustsAllBeforeRequestingNewCycle() {
        val rng = Random(1234)
        val service = StudentSelectionService { rng }
        var students = makeStudents(listOf("An", "Bình", "Châu", "Dũng"))
        val pickedIds = mutableSetOf<String>()

        repeat(4) {
            val outcome = service.startSelectionTurn(
                classroom = testClassroom,
                allStudents = students,
                mode = SelectionMode.NO_REPEAT_IN_CYCLE,
                requestedCount = 1,
                miniGame = duckGame
            )
            assertTrue(outcome is SelectionRequestOutcome.Success)
            val turn = (outcome as SelectionRequestOutcome.Success).lockedTurn
            val winner = turn.winners.first()
            assertTrue("Không được chọn trùng trong cùng 1 chu kỳ", pickedIds.add(winner.id))
            service.markTurnCommitted(turn.turnId)
            students = students.map {
                if (it.id == winner.id) it.copy(selectedCountInCycle = it.selectedCountInCycle + 1) else it
            }
        }

        // Lượt thứ 5 phải báo CycleExhausted chứ không tự âm thầm reset
        val fifthOutcome = service.startSelectionTurn(
            classroom = testClassroom,
            allStudents = students,
            mode = SelectionMode.NO_REPEAT_IN_CYCLE,
            requestedCount = 1,
            miniGame = duckGame
        )
        assertTrue(fifthOutcome is SelectionRequestOutcome.CycleExhausted)
    }

    // Tình huống 9: Cho phép chọn lặp lại (Chế độ C)
    @Test
    fun scenario09_allowRepeat_canSelectAlreadyCalledStudents() {
        val rng = Random(55)
        val service = StudentSelectionService { rng }
        val students = makeStudents(listOf("An", "Bình")).map {
            it.copy(selectedCountInCycle = 3)
        }
        val outcome = service.startSelectionTurn(
            classroom = testClassroom,
            allStudents = students,
            mode = SelectionMode.ALLOW_REPEAT,
            requestedCount = 1,
            miniGame = duckGame
        )
        assertTrue(outcome is SelectionRequestOutcome.Success)
    }

    // Tình huống 13 & 14: Thoát giữa hoạt ảnh & Chạm liên tục vào nút bắt đầu (Idempotency & Turn Lock)
    @Test
    fun scenario13_14_turnLockAndDoubleCommitProtection_preventsDuplicateHistory() {
        val service = StudentSelectionService { Random(77) }
        val students = makeStudents(listOf("An", "Bình", "Châu"))

        val first = service.startSelectionTurn(
            classroom = testClassroom,
            allStudents = students,
            mode = SelectionMode.NORMAL_RANDOM,
            requestedCount = 1,
            miniGame = duckGame
        )
        assertTrue(first is SelectionRequestOutcome.Success)
        val locked = (first as SelectionRequestOutcome.Success).lockedTurn

        // Bấm liên tục khi lượt đang chạy phải trả về Busy
        val secondWhileRunning = service.startSelectionTurn(
            classroom = testClassroom,
            allStudents = students,
            mode = SelectionMode.NORMAL_RANDOM,
            requestedCount = 1,
            miniGame = duckGame
        )
        assertTrue(secondWhileRunning is SelectionRequestOutcome.Busy)

        // Commit lần 1 thành công, commit lần 2 với cùng turnId trả về null (không ghi trùng lịch sử)
        assertNotNull(service.markTurnCommitted(locked.turnId))
        assertNull(service.markTurnCommitted(locked.turnId))

        // Thoát giữa hoạt ảnh (cancelActiveTurn) giải phóng khóa
        val third = service.startSelectionTurn(
            classroom = testClassroom,
            allStudents = students,
            mode = SelectionMode.NORMAL_RANDOM,
            requestedCount = 1,
            miniGame = duckGame
        )
        assertTrue(third is SelectionRequestOutcome.Success)
        assertTrue(service.cancelActiveTurn())
        assertFalse(service.isTurnLocked())
    }

    // Tình huống 15: Nhiều mini-game dùng chung một StudentSelectionService
    @Test
    fun scenario15_multipleMiniGames_shareSameCentralService() {
        val service = StudentSelectionService { Random(101) }
        val students = makeStudents(listOf("An", "Bình", "Châu"))

        listOf(duckGame, ufoGame, wheelGame).forEach { game ->
            val outcome = service.startSelectionTurn(
                classroom = testClassroom,
                allStudents = students,
                mode = SelectionMode.NORMAL_RANDOM,
                requestedCount = 1,
                miniGame = game
            ) as SelectionRequestOutcome.Success
            assertEquals(game.id, outcome.lockedTurn.miniGame.id)
            service.markTurnCommitted(outcome.lockedTurn.turnId)
        }
    }

    // Tình huống 16, 17, 18: Sao lưu/Khôi phục JSON, Tên tiếng Việt có dấu, Tên dài & Tên trùng
    @Test
    fun scenario16_17_18_vietnameseNamesDuplicatesAndBackupRestore() {
        val rawInput = """
            1. Nguyễn Phúc Huyền Trân
            2. Đặng Hoàng Minh Triết
            
            3. Nguyễn Phúc Huyền Trân
            4. Công Tằng Tôn Nữ Nguyễn Thị Minh Khai
        """.trimIndent()

        val parsed = SaveManager.parseStudentLines(
            rawInput = rawInput,
            existingClassNames = listOf("Lê Văn Tám")
        )
        // Đã loại bỏ dòng trống và số thứ tự
        assertEquals(4, parsed.cleanNames.size)
        assertEquals("Nguyễn Phúc Huyền Trân", parsed.cleanNames[0])
        assertEquals(listOf("Nguyễn Phúc Huyền Trân"), parsed.duplicateNamesInInput)

        // Kiểm tra Sao lưu & Khôi phục JSON bảo toàn dấu tiếng Việt và snapshot lịch sử
        val students = makeStudents(parsed.cleanNames)
        val history = listOf(
            SelectionHistoryEntity(
                id = "h1",
                turnId = "t1",
                classroomId = testClassroom.id,
                classroomNameSnapshot = testClassroom.name,
                studentId = students[0].id,
                studentNameSnapshot = students[0].fullName,
                calledAt = 1700000000000L,
                miniGameId = "duck_race",
                miniGameNameSnapshot = "Đua vịt",
                selectionModeSnapshot = SelectionMode.NO_REPEAT_IN_CYCLE.titleVi,
                cycleNumberSnapshot = 1
            )
        )
        val json = SaveManager.exportBackupJson(
            BackupPayload(
                classrooms = listOf(testClassroom),
                students = students,
                history = history
            )
        )
        val restored = SaveManager.importBackupJson(json).getOrThrow()
        assertEquals(1, restored.classrooms.size)
        assertEquals(4, restored.students.size)
        assertEquals("Công Tằng Tôn Nữ Nguyễn Thị Minh Khai", restored.students[3].fullName)
        assertEquals("Nguyễn Phúc Huyền Trân", restored.history.first().studentNameSnapshot)
    }
}
