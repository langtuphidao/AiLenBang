package com.example.repositories

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.models.ClassroomEntity
import com.example.models.SelectionHistoryEntity
import com.example.models.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassroomDao {
    @Query("SELECT * FROM classrooms ORDER BY updatedAt DESC")
    fun observeAllClassrooms(): Flow<List<ClassroomEntity>>

    @Query("SELECT * FROM classrooms ORDER BY updatedAt DESC")
    suspend fun getAllClassroomsOnce(): List<ClassroomEntity>

    @Query("SELECT * FROM classrooms WHERE id = :id LIMIT 1")
    suspend fun getClassroomById(id: String): ClassroomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClassroom(classroom: ClassroomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClassrooms(classrooms: List<ClassroomEntity>)

    @Query("DELETE FROM classrooms WHERE id = :id")
    suspend fun deleteClassroomById(id: String)

    @Query("DELETE FROM classrooms")
    suspend fun deleteAllClassrooms()
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY displayOrder ASC, fullName ASC")
    fun observeAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY displayOrder ASC, fullName ASC")
    suspend fun getAllStudentsOnce(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE classroomId = :classroomId ORDER BY displayOrder ASC, fullName ASC")
    fun observeStudentsByClassroom(classroomId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE classroomId = :classroomId ORDER BY displayOrder ASC, fullName ASC")
    suspend fun getStudentsByClassroomOnce(classroomId: String): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudentById(studentId: String)

    @Query("DELETE FROM students WHERE classroomId = :classroomId")
    suspend fun deleteStudentsByClassroom(classroomId: String)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM selection_history ORDER BY calledAt DESC")
    fun observeAllHistory(): Flow<List<SelectionHistoryEntity>>

    @Query("SELECT * FROM selection_history ORDER BY calledAt DESC")
    suspend fun getAllHistoryOnce(): List<SelectionHistoryEntity>

    @Query("SELECT * FROM selection_history WHERE classroomId = :classroomId ORDER BY calledAt DESC")
    fun observeHistoryByClassroom(classroomId: String): Flow<List<SelectionHistoryEntity>>

    @Query("SELECT * FROM selection_history WHERE turnId = :turnId")
    suspend fun getHistoryByTurnId(turnId: String): List<SelectionHistoryEntity>

    @Query("SELECT * FROM selection_history WHERE classroomId = :classroomId ORDER BY calledAt DESC LIMIT 1")
    suspend fun getLatestHistoryForClassroom(classroomId: String): SelectionHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryRecords(records: List<SelectionHistoryEntity>)

    @Query("UPDATE selection_history SET answerStatus = :status WHERE turnId = :turnId")
    suspend fun updateAnswerStatusByTurnId(turnId: String, status: String)

    @Query("UPDATE selection_history SET answerStatus = :status WHERE id = :recordId")
    suspend fun updateAnswerStatusByRecordId(recordId: String, status: String)

    @Query("DELETE FROM selection_history WHERE turnId = :turnId")
    suspend fun deleteByTurnId(turnId: String)

    @Query("DELETE FROM selection_history WHERE classroomId = :classroomId")
    suspend fun deleteByClassroomId(classroomId: String)

    @Query("DELETE FROM selection_history")
    suspend fun deleteAllHistory()
}

@Database(
    entities = [
        ClassroomEntity::class,
        StudentEntity::class,
        SelectionHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classroomDao(): ClassroomDao
    abstract fun studentDao(): StudentDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_len_bang_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
