package com.example.repositories

import com.example.models.ClassroomEntity
import com.example.models.StudentEntity
import kotlinx.coroutines.flow.Flow
import java.text.Collator
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

/**
 * ClassroomRepository quản lý lớp học và danh sách học sinh.
 * Tách biệt hoàn toàn khỏi giao diện và thuật toán random.
 */
class ClassroomRepository(
    private val classroomDao: ClassroomDao,
    private val studentDao: StudentDao
) {
    val allClassrooms: Flow<List<ClassroomEntity>> = classroomDao.observeAllClassrooms()
    val allStudents: Flow<List<StudentEntity>> = studentDao.observeAllStudents()

    fun observeStudentsForClassroom(classroomId: String): Flow<List<StudentEntity>> =
        studentDao.observeStudentsByClassroom(classroomId)

    suspend fun getStudentsForClassroomOnce(classroomId: String): List<StudentEntity> =
        studentDao.getStudentsByClassroomOnce(classroomId)

    suspend fun getClassroomById(classroomId: String): ClassroomEntity? =
        classroomDao.getClassroomById(classroomId)

    suspend fun createClassroom(name: String, gradeNote: String = ""): ClassroomEntity {
        val cleanName = name.trim().ifEmpty { "Lớp mới" }
        val entity = ClassroomEntity(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            gradeNote = gradeNote.trim(),
            currentCycleNumber = 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        classroomDao.upsertClassroom(entity)
        return entity
    }

    suspend fun renameClassroom(classroomId: String, newName: String, newNote: String) {
        val current = classroomDao.getClassroomById(classroomId) ?: return
        classroomDao.upsertClassroom(
            current.copy(
                name = newName.trim().ifEmpty { current.name },
                gradeNote = newNote.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun duplicateClassroom(sourceClassroomId: String, newName: String? = null): ClassroomEntity? {
        val source = classroomDao.getClassroomById(sourceClassroomId) ?: return null
        val sourceStudents = studentDao.getStudentsByClassroomOnce(sourceClassroomId)
        val copiedClass = ClassroomEntity(
            id = UUID.randomUUID().toString(),
            name = (newName?.trim()?.takeIf { it.isNotEmpty() } ?: "${source.name} (Bản sao)"),
            gradeNote = source.gradeNote,
            currentCycleNumber = 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        classroomDao.upsertClassroom(copiedClass)
        val copiedStudents = sourceStudents.mapIndexed { idx, s ->
            StudentEntity(
                id = UUID.randomUUID().toString(),
                classroomId = copiedClass.id,
                fullName = s.fullName,
                isActive = s.isActive,
                isEligible = s.isEligible,
                isAbsent = false,
                isManuallyExcluded = false,
                isInManualSubset = s.isInManualSubset,
                selectedCountInCycle = 0,
                totalSelectedCount = 0,
                lastSelectedAt = null,
                displayOrder = idx
            )
        }
        if (copiedStudents.isNotEmpty()) {
            studentDao.upsertStudents(copiedStudents)
        }
        return copiedClass
    }

    suspend fun deleteClassroom(classroomId: String) {
        studentDao.deleteStudentsByClassroom(classroomId)
        classroomDao.deleteClassroomById(classroomId)
    }

    suspend fun addStudent(classroomId: String, fullName: String): StudentEntity? {
        val clean = fullName.trim()
        if (clean.isEmpty()) return null
        val existing = studentDao.getStudentsByClassroomOnce(classroomId)
        val maxOrder = existing.maxOfOrNull { it.displayOrder } ?: -1
        val student = StudentEntity(
            id = UUID.randomUUID().toString(),
            classroomId = classroomId,
            fullName = clean,
            displayOrder = maxOrder + 1
        )
        studentDao.upsertStudent(student)
        touchClassroom(classroomId)
        return student
    }

    suspend fun addStudentsBatch(classroomId: String, names: List<String>): List<StudentEntity> {
        val cleanList = names.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanList.isEmpty()) return emptyList()
        val existing = studentDao.getStudentsByClassroomOnce(classroomId)
        val startOrder = (existing.maxOfOrNull { it.displayOrder } ?: -1) + 1
        val created = cleanList.mapIndexed { idx, name ->
            StudentEntity(
                id = UUID.randomUUID().toString(),
                classroomId = classroomId,
                fullName = name,
                displayOrder = startOrder + idx
            )
        }
        studentDao.upsertStudents(created)
        touchClassroom(classroomId)
        return created
    }

    suspend fun updateStudentName(studentId: String, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        val student = studentDao.getStudentById(studentId) ?: return
        studentDao.upsertStudent(student.copy(fullName = clean))
        touchClassroom(student.classroomId)
    }

    suspend fun deleteStudent(studentId: String) {
        val student = studentDao.getStudentById(studentId) ?: return
        studentDao.deleteStudentById(studentId)
        touchClassroom(student.classroomId)
    }

    suspend fun setStudentAbsent(studentId: String, isAbsent: Boolean) {
        val student = studentDao.getStudentById(studentId) ?: return
        studentDao.upsertStudent(student.copy(isAbsent = isAbsent))
    }

    suspend fun setStudentManuallyExcluded(studentId: String, excluded: Boolean) {
        val student = studentDao.getStudentById(studentId) ?: return
        studentDao.upsertStudent(student.copy(isManuallyExcluded = excluded))
    }

    suspend fun setMultipleStudentsExcluded(studentIds: List<String>, excluded: Boolean) {
        val updated = studentIds.mapNotNull { id ->
            studentDao.getStudentById(id)?.copy(isManuallyExcluded = excluded)
        }
        if (updated.isNotEmpty()) {
            studentDao.upsertStudents(updated)
        }
    }

    suspend fun setStudentManualSubset(studentId: String, inSubset: Boolean) {
        val student = studentDao.getStudentById(studentId) ?: return
        studentDao.upsertStudent(student.copy(isInManualSubset = inSubset))
    }

    suspend fun setAllStudentsManualSubset(classroomId: String, inSubset: Boolean) {
        val list = studentDao.getStudentsByClassroomOnce(classroomId)
        studentDao.upsertStudents(list.map { it.copy(isInManualSubset = inSubset) })
    }

    /**
     * Sắp xếp tên theo thứ tự chữ cái tiếng Việt (theo tên chính hoặc toàn bộ họ tên).
     */
    suspend fun sortStudentsAlphabetically(classroomId: String) {
        val collator = Collator.getInstance(Locale("vi", "VN"))
        val students = studentDao.getStudentsByClassroomOnce(classroomId)
        val sorted = students.sortedWith { a, b ->
            val givenA = a.fullName.trim().substringAfterLast(" ")
            val givenB = b.fullName.trim().substringAfterLast(" ")
            val cmp = collator.compare(givenA, givenB)
            if (cmp != 0) cmp else collator.compare(a.fullName, b.fullName)
        }.mapIndexed { idx, s -> s.copy(displayOrder = idx) }
        studentDao.upsertStudents(sorted)
    }

    /**
     * Xáo trộn thứ tự hiển thị trên danh sách mà không làm thay đổi trạng thái hay xác suất chọn.
     */
    suspend fun shuffleDisplayOrder(classroomId: String) {
        val students = studentDao.getStudentsByClassroomOnce(classroomId)
        val shuffled = students.shuffled(Random.Default).mapIndexed { idx, s ->
            s.copy(displayOrder = idx)
        }
        studentDao.upsertStudents(shuffled)
    }

    /**
     * Đặt lại chu kỳ chọn mới cho lớp học (tăng số chu kỳ và đưa selectedCountInCycle về 0).
     */
    suspend fun resetSelectionCycle(classroomId: String, incrementCycleNumber: Boolean = true) {
        val classroom = classroomDao.getClassroomById(classroomId) ?: return
        val students = studentDao.getStudentsByClassroomOnce(classroomId)
        studentDao.upsertStudents(
            students.map { it.copy(selectedCountInCycle = 0) }
        )
        val nextCycle = if (incrementCycleNumber) classroom.currentCycleNumber + 1 else classroom.currentCycleNumber
        classroomDao.upsertClassroom(
            classroom.copy(
                currentCycleNumber = nextCycle,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markStudentsSelected(studentIds: List<String>, timestamp: Long) {
        val updated = studentIds.mapNotNull { id ->
            studentDao.getStudentById(id)?.let { s ->
                s.copy(
                    selectedCountInCycle = s.selectedCountInCycle + 1,
                    totalSelectedCount = s.totalSelectedCount + 1,
                    lastSelectedAt = timestamp
                )
            }
        }
        if (updated.isNotEmpty()) {
            studentDao.upsertStudents(updated)
            touchClassroom(updated.first().classroomId)
        }
    }

    suspend fun restoreStudentSelectionState(
        studentId: String,
        previousCountInCycle: Int,
        previousLastSelectedAt: Long?
    ) {
        val student = studentDao.getStudentById(studentId) ?: return
        studentDao.upsertStudent(
            student.copy(
                selectedCountInCycle = previousCountInCycle.coerceAtLeast(0),
                totalSelectedCount = (student.totalSelectedCount - 1).coerceAtLeast(0),
                lastSelectedAt = previousLastSelectedAt
            )
        )
    }

    /**
     * Tạo lớp học mẫu tùy chọn (có thể xóa hoặc bỏ qua bất kỳ lúc nào).
     */
    suspend fun createOptionalDemoClass(): ClassroomEntity {
        val demoClass = createClassroom(
            name = "Lớp 5A - Demo Trải Nghiệm",
            gradeNote = "Dữ liệu mẫu tùy chọn (có thể xóa bất cứ lúc nào)"
        )
        val demoNames = listOf(
            "Nguyễn Minh An",
            "Trần Bảo Châu",
            "Lê Hoàng Dũng",
            "Phạm Thùy Dương",
            "Hoàng Gia Hân",
            "Vũ Tuấn Kiệt",
            "Đặng Khánh Linh",
            "Bùi Nhật Minh",
            "Đỗ Ngọc Ánh",
            "Ngô Quốc Khánh",
            "Dương Thanh Trúc",
            "Lý Thiên Phúc"
        )
        addStudentsBatch(demoClass.id, demoNames)
        return demoClass
    }

    suspend fun getAllClassroomsOnce(): List<ClassroomEntity> = classroomDao.getAllClassroomsOnce()
    suspend fun getAllStudentsOnce(): List<StudentEntity> = studentDao.getAllStudentsOnce()

    suspend fun replaceAllFromBackup(classrooms: List<ClassroomEntity>, students: List<StudentEntity>) {
        studentDao.deleteAllStudents()
        classroomDao.deleteAllClassrooms()
        classroomDao.upsertClassrooms(classrooms)
        studentDao.upsertStudents(students)
    }

    private suspend fun touchClassroom(classroomId: String) {
        val c = classroomDao.getClassroomById(classroomId) ?: return
        classroomDao.upsertClassroom(c.copy(updatedAt = System.currentTimeMillis()))
    }
}
