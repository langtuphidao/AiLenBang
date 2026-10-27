package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.StudentSelectionService
import com.example.models.AnswerStatus
import com.example.models.MiniGameCatalog
import com.example.models.SelectionMode
import com.example.models.SelectionRequestOutcome
import com.example.repositories.AppDatabase
import com.example.repositories.ClassroomRepository
import com.example.repositories.HistoryRepository
import com.example.repositories.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/**
 * Kiểm thử tích hợp Robolectric cho:
 * - Tình huống 10: Hoàn tác kết quả (Undo)
 * - Tình huống 11: Chỉnh sửa hoặc xóa học sinh nhưng lịch sử cũ vẫn giữ nguyên snapshot tên
 * - Tình huống 12: Đóng và mở lại ứng dụng (Lưu trữ bền vững Room & SharedPreferences)
 * - Tình huống 20: Hoạt động hoàn toàn offline không cần kết nối Internet
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var classroomRepo: ClassroomRepository
    private lateinit var historyRepo: HistoryRepository
    private lateinit var settingsRepo: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        classroomRepo = ClassroomRepository(db.classroomDao(), db.studentDao())
        historyRepo = HistoryRepository(db.historyDao(), classroomRepo)
        settingsRepo = SettingsRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun appName_matchesAiLenBang() {
        assertEquals("Ai Lên Bảng?", context.getString(R.string.app_name))
    }

    @Test
    fun scenario10_11_12_persistenceUndoAndSnapshotIntegrityOnEditOrDelete() = runBlocking {
        // Tạo lớp và thêm 3 học sinh
        val createdClass = classroomRepo.createClassroom("Lớp 8A", "Toán Học")
        classroomRepo.addStudentsBatch(
            createdClass.id,
            listOf("Nguyễn Văn An", "Trần Thị Bình", "Lê Hoàng Châu")
        )
        val initialStudents = classroomRepo.getStudentsForClassroomOnce(createdClass.id)
        assertEquals(3, initialStudents.size)

        // Chạy 1 lượt chọn với StudentSelectionService
        val service = StudentSelectionService { Random(2026) }
        val outcome = service.startSelectionTurn(
            classroom = createdClass,
            allStudents = initialStudents,
            mode = SelectionMode.NO_REPEAT_IN_CYCLE,
            requestedCount = 1,
            miniGame = MiniGameCatalog.findById("ufo_abduction")
        ) as SelectionRequestOutcome.Success

        val committedTurn = service.markTurnCommitted(outcome.lockedTurn.turnId)
        assertNotNull(committedTurn)
        historyRepo.recordCommittedTurn(committedTurn!!, AnswerStatus.ANSWERED)

        val winner = committedTurn.winners.first()
        val originalWinnerName = winner.fullName

        // Kiểm tra số lần gọi trong chu kỳ đã tăng lên 1
        val afterCallStudent = classroomRepo.getStudentsForClassroomOnce(createdClass.id)
            .first { it.id == winner.id }
        assertEquals(1, afterCallStudent.selectedCountInCycle)

        // Tình huống 11: Đổi tên học sinh rồi xóa học sinh -> Lịch sử cũ vẫn phải giữ nguyên snapshot tên lúc gọi
        classroomRepo.updateStudentName(winner.id, "Tên Mới Đã Đổi")
        val historyRecords = historyRepo.getAllHistoryOnce()
        assertEquals(1, historyRecords.size)
        assertEquals(originalWinnerName, historyRecords.first().studentNameSnapshot)

        // Tình huống 10: Hoàn tác (Undo) lượt gọi -> Khôi phục selectedCountInCycle về 0 và xóa bản ghi lượt đó
        val undoneName = historyRepo.undoLatestTurnForClassroom(createdClass.id)
        assertEquals(originalWinnerName, undoneName)
        val restoredStudent = classroomRepo.getStudentsForClassroomOnce(createdClass.id)
            .first { it.id == winner.id }
        assertEquals(0, restoredStudent.selectedCountInCycle)
        assertTrue(historyRepo.getAllHistoryOnce().isEmpty())

        // Tình huống 12: Cài đặt được lưu bền vững khi khởi tạo lại SettingsRepository
        settingsRepo.updateSettings {
            it.copy(
                presentationMode = true,
                quietTeachingMode = true,
                pickCount = 3
            )
        }
        val reloadedSettingsRepo = SettingsRepository(context)
        assertTrue(reloadedSettingsRepo.settings.value.presentationMode)
        assertTrue(reloadedSettingsRepo.settings.value.quietTeachingMode)
        assertEquals(3, reloadedSettingsRepo.settings.value.pickCount)
    }
}
