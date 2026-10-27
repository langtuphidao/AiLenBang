package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.StudentSelectionService
import com.example.models.AnswerStatus
import com.example.models.AppSettings
import com.example.models.ClassroomEntity
import com.example.models.ClassroomSummary
import com.example.models.LockedSelectionTurn
import com.example.models.MiniGameCatalog
import com.example.models.MiniGameDescriptor
import com.example.models.MiniGameRunState
import com.example.models.SelectionHistoryEntity
import com.example.models.SelectionMode
import com.example.models.SelectionRequestOutcome
import com.example.models.StudentEntity
import com.example.repositories.AppDatabase
import com.example.repositories.ClassroomRepository
import com.example.repositories.HistoryRepository
import com.example.repositories.SettingsRepository
import com.example.services.AudioManager
import com.example.services.BackupPayload
import com.example.services.SaveManager
import com.example.services.SoundEffectType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenDestination {
    HOME,
    CLASS_LIST,
    CLASS_DASHBOARD,
    STUDENT_MANAGER,
    MINIGAME_COLLECTION,
    MINIGAME_PLAY,
    RESULT_REVEAL,
    HISTORY,
    SETTINGS,
    ABOUT_AUTHOR
}

data class CycleExhaustedPrompt(
    val classroomId: String,
    val cycleNumber: Int,
    val eligibleCount: Int,
    val messageVi: String,
    val launchMiniGameAfterReset: Boolean
)

data class DuplicateNamesPrompt(
    val classroomId: String,
    val allCleanNames: List<String>,
    val uniqueOnlyNames: List<String>,
    val duplicateNames: List<String>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val classroomRepository = ClassroomRepository(db.classroomDao(), db.studentDao())
    val historyRepository = HistoryRepository(db.historyDao(), classroomRepository)
    val settingsRepository = SettingsRepository(application)
    val selectionService = StudentSelectionService()
    val audioManager = AudioManager()

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    private val _navigationStack = MutableStateFlow(listOf(ScreenDestination.HOME))
    val currentScreen: StateFlow<ScreenDestination> = MutableStateFlow(ScreenDestination.HOME).also { destFlow ->
        viewModelScope.launch {
            _navigationStack.collect { stack ->
                destFlow.value = stack.lastOrNull() ?: ScreenDestination.HOME
            }
        }
    }

    private val _activeTurn = MutableStateFlow<LockedSelectionTurn?>(null)
    val activeTurn: StateFlow<LockedSelectionTurn?> = _activeTurn.asStateFlow()

    private val _miniGameRunState = MutableStateFlow(MiniGameRunState.IDLE)
    val miniGameRunState: StateFlow<MiniGameRunState> = _miniGameRunState.asStateFlow()

    private val _currentTurnAnswerStatus = MutableStateFlow(AnswerStatus.PENDING)
    val currentTurnAnswerStatus: StateFlow<AnswerStatus> = _currentTurnAnswerStatus.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _cycleExhaustedPrompt = MutableStateFlow<CycleExhaustedPrompt?>(null)
    val cycleExhaustedPrompt: StateFlow<CycleExhaustedPrompt?> = _cycleExhaustedPrompt.asStateFlow()

    private val _duplicateNamesPrompt = MutableStateFlow<DuplicateNamesPrompt?>(null)
    val duplicateNamesPrompt: StateFlow<DuplicateNamesPrompt?> = _duplicateNamesPrompt.asStateFlow()

    private val _studentFilterIdForHistory = MutableStateFlow<String?>(null)
    val studentFilterIdForHistory: StateFlow<String?> = _studentFilterIdForHistory.asStateFlow()

    val classrooms: StateFlow<List<ClassroomEntity>> = classroomRepository.allClassrooms
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allStudents: StateFlow<List<StudentEntity>> = classroomRepository.allStudents
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allHistory: StateFlow<List<SelectionHistoryEntity>> = historyRepository.allHistory
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeClassroomId: StateFlow<String?> = settingsRepository.activeClassroomId

    val classroomSummaries: StateFlow<List<ClassroomSummary>> = combine(
        classrooms,
        allStudents
    ) { classList, studentList ->
        classList.map { c ->
            val studentsInClass = studentList.filter { it.classroomId == c.id }
            val activeEligible = studentsInClass.filter { it.isActive && it.isEligible && !it.isAbsent && !it.isManuallyExcluded }
            val calledInCycle = activeEligible.count { it.selectedCountInCycle > 0 }
            val uncalledInCycle = activeEligible.count { it.selectedCountInCycle == 0 }
            val absentOrExcluded = studentsInClass.count { it.isAbsent || it.isManuallyExcluded || !it.isActive }
            val manualSubset = studentsInClass.count { it.isInManualSubset && !it.isAbsent && !it.isManuallyExcluded }
            ClassroomSummary(
                classroom = c,
                totalStudents = studentsInClass.size,
                calledInCycleCount = calledInCycle,
                uncalledInCycleCount = uncalledInCycle,
                absentOrExcludedCount = absentOrExcluded,
                manualSubsetCount = manualSubset
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeClassroomSummary: StateFlow<ClassroomSummary?> = combine(
        classroomSummaries,
        activeClassroomId
    ) { summaries, activeId ->
        summaries.find { it.classroom.id == activeId } ?: summaries.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeClassroomStudents: StateFlow<List<StudentEntity>> = combine(
        allStudents,
        activeClassroomSummary
    ) { students, summary ->
        if (summary == null) emptyList()
        else students.filter { it.classroomId == summary.classroom.id }
            .sortedWith(compareBy<StudentEntity> { it.displayOrder }.thenBy { it.fullName })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedMiniGame: StateFlow<MiniGameDescriptor> = combine(
        settings
    ) { arr ->
        MiniGameCatalog.findById(arr[0].selectedMiniGameId)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        MiniGameCatalog.findById(settingsRepository.settings.value.selectedMiniGameId)
    )

    init {
        viewModelScope.launch {
            settings.collect { s ->
                audioManager.updateSettings(s)
            }
        }
    }

    fun navigateTo(destination: ScreenDestination) {
        if (_navigationStack.value.lastOrNull() == ScreenDestination.MINIGAME_PLAY &&
            destination != ScreenDestination.RESULT_REVEAL
        ) {
            cancelRunningMiniGame()
        }
        if (destination != ScreenDestination.HISTORY) {
            _studentFilterIdForHistory.value = null
        }
        _navigationStack.value = _navigationStack.value + destination
    }

    fun navigateBack(): Boolean {
        val current = _navigationStack.value
        if (current.size <= 1) return false
        if (current.last() == ScreenDestination.MINIGAME_PLAY) {
            cancelRunningMiniGame()
        }
        _navigationStack.value = current.dropLast(1)
        return true
    }

    fun navigateHome() {
        cancelRunningMiniGame()
        _navigationStack.value = listOf(ScreenDestination.HOME)
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    fun selectClassroomAndOpenDashboard(classroomId: String) {
        settingsRepository.setActiveClassroomId(classroomId)
        navigateTo(ScreenDestination.CLASS_DASHBOARD)
    }

    fun selectClassroomForManagement(classroomId: String) {
        settingsRepository.setActiveClassroomId(classroomId)
        navigateTo(ScreenDestination.STUDENT_MANAGER)
    }

    fun createClassroom(name: String, gradeNote: String, openImmediately: Boolean = true) {
        viewModelScope.launch {
            val created = classroomRepository.createClassroom(name, gradeNote)
            settingsRepository.setActiveClassroomId(created.id)
            audioManager.play(SoundEffectType.ACTION_SUCCESS)
            showMessage("Đã tạo lớp \"${created.name}\"")
            if (openImmediately) {
                navigateTo(ScreenDestination.STUDENT_MANAGER)
            }
        }
    }

    fun createOptionalDemoClassroom() {
        viewModelScope.launch {
            val created = classroomRepository.createOptionalDemoClass()
            settingsRepository.setActiveClassroomId(created.id)
            audioManager.play(SoundEffectType.ACTION_SUCCESS)
            showMessage("Đã tạo lớp mẫu \"${created.name}\" với 12 học sinh")
            navigateTo(ScreenDestination.CLASS_DASHBOARD)
        }
    }

    fun renameClassroom(classroomId: String, newName: String, newNote: String) {
        viewModelScope.launch {
            classroomRepository.renameClassroom(classroomId, newName, newNote)
            audioManager.play(SoundEffectType.ACTION_SUCCESS)
            showMessage("Đã cập nhật thông tin lớp")
        }
    }

    fun duplicateClassroom(classroomId: String) {
        viewModelScope.launch {
            val copy = classroomRepository.duplicateClassroom(classroomId)
            if (copy != null) {
                audioManager.play(SoundEffectType.ACTION_SUCCESS)
                showMessage("Đã sao chép sang lớp \"${copy.name}\"")
            }
        }
    }

    fun deleteClassroom(classroomId: String) {
        viewModelScope.launch {
            classroomRepository.deleteClassroom(classroomId)
            audioManager.play(SoundEffectType.WARNING_ALERT)
            showMessage("Đã xóa lớp học")
        }
    }

    fun addSingleStudent(fullName: String, forceAllowDuplicate: Boolean = false) {
        val summary = activeClassroomSummary.value ?: return
        val clean = fullName.trim()
        if (clean.isEmpty()) {
            showMessage("Vui lòng nhập họ và tên học sinh")
            return
        }
        val existingNames = activeClassroomStudents.value.map { it.fullName }
        val parsed = SaveManager.parseStudentLines(clean, existingNames)
        if (!forceAllowDuplicate && parsed.existingDuplicatesInClass.isNotEmpty()) {
            _duplicateNamesPrompt.value = DuplicateNamesPrompt(
                classroomId = summary.classroom.id,
                allCleanNames = parsed.cleanNames,
                uniqueOnlyNames = emptyList(),
                duplicateNames = parsed.existingDuplicatesInClass
            )
            return
        }
        viewModelScope.launch {
            classroomRepository.addStudent(summary.classroom.id, clean)
            audioManager.play(SoundEffectType.ACTION_SUCCESS)
            showMessage("Đã thêm học sinh: $clean")
        }
    }

    fun importStudentsBulkText(rawText: String) {
        val summary = activeClassroomSummary.value ?: return
        val existingNames = activeClassroomStudents.value.map { it.fullName }
        val parsed = SaveManager.parseStudentLines(rawText, existingNames)
        if (parsed.cleanNames.isEmpty()) {
            audioManager.play(SoundEffectType.WARNING_ALERT)
            showMessage("Không tìm thấy tên học sinh hợp lệ (các dòng trống đã được bỏ qua)")
            return
        }

        val allDuplicates = (parsed.duplicateNamesInInput + parsed.existingDuplicatesInClass).distinct()
        if (allDuplicates.isNotEmpty()) {
            val existingSet = existingNames.map { it.trim().lowercase() }.toSet()
            val seen = mutableSetOf<String>()
            val uniqueOnly = parsed.cleanNames.filter { name ->
                val key = name.trim().lowercase()
                !existingSet.contains(key) && seen.add(key)
            }
            _duplicateNamesPrompt.value = DuplicateNamesPrompt(
                classroomId = summary.classroom.id,
                allCleanNames = parsed.cleanNames,
                uniqueOnlyNames = uniqueOnly,
                duplicateNames = allDuplicates
            )
        } else {
            viewModelScope.launch {
                val added = classroomRepository.addStudentsBatch(summary.classroom.id, parsed.cleanNames)
                audioManager.play(SoundEffectType.ACTION_SUCCESS)
                showMessage("Đã thêm ${added.size} học sinh vào lớp ${summary.classroom.name}")
            }
        }
    }

    fun confirmDuplicateNamesImport(keepDuplicates: Boolean) {
        val prompt = _duplicateNamesPrompt.value ?: return
        _duplicateNamesPrompt.value = null
        val namesToInsert = if (keepDuplicates) prompt.allCleanNames else prompt.uniqueOnlyNames
        if (namesToInsert.isEmpty()) {
            showMessage("Đã bỏ qua các tên bị trùng")
            return
        }
        viewModelScope.launch {
            val added = classroomRepository.addStudentsBatch(prompt.classroomId, namesToInsert)
            audioManager.play(SoundEffectType.ACTION_SUCCESS)
            showMessage("Đã thêm ${added.size} học sinh vào danh sách lớp")
        }
    }

    fun dismissDuplicateNamesPrompt() {
        _duplicateNamesPrompt.value = null
    }

    fun updateStudentName(studentId: String, newName: String) {
        viewModelScope.launch {
            classroomRepository.updateStudentName(studentId, newName)
            audioManager.play(SoundEffectType.ACTION_SUCCESS)
            showMessage("Đã cập nhật tên học sinh (lịch sử cũ vẫn giữ nguyên snapshot)")
        }
    }

    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            classroomRepository.deleteStudent(studentId)
            audioManager.play(SoundEffectType.WARNING_ALERT)
            showMessage("Đã xóa học sinh khỏi danh sách lớp")
        }
    }

    fun toggleStudentAbsent(student: StudentEntity) {
        viewModelScope.launch {
            classroomRepository.setStudentAbsent(student.id, !student.isAbsent)
        }
    }

    fun toggleStudentExcluded(student: StudentEntity) {
        viewModelScope.launch {
            classroomRepository.setStudentManuallyExcluded(student.id, !student.isManuallyExcluded)
        }
    }

    fun setBulkStudentsExcluded(studentIds: List<String>, excluded: Boolean) {
        viewModelScope.launch {
            classroomRepository.setMultipleStudentsExcluded(studentIds, excluded)
            showMessage(
                if (excluded) "Đã đưa ${studentIds.size} học sinh vào danh sách tạm loại trừ"
                else "Đã khôi phục ${studentIds.size} học sinh vào danh sách có thể gọi"
            )
        }
    }

    fun toggleStudentManualSubset(student: StudentEntity) {
        viewModelScope.launch {
            classroomRepository.setStudentManualSubset(student.id, !student.isInManualSubset)
        }
    }

    fun setAllStudentsManualSubset(inSubset: Boolean) {
        val summary = activeClassroomSummary.value ?: return
        viewModelScope.launch {
            classroomRepository.setAllStudentsManualSubset(summary.classroom.id, inSubset)
        }
    }

    fun sortStudentsAlphabetically() {
        val summary = activeClassroomSummary.value ?: return
        viewModelScope.launch {
            classroomRepository.sortStudentsAlphabetically(summary.classroom.id)
            audioManager.play(SoundEffectType.ACTION_SUCCESS)
            showMessage("Đã sắp xếp danh sách học sinh theo bảng chữ cái A-Z")
        }
    }

    fun shuffleStudentsDisplayOrder() {
        val summary = activeClassroomSummary.value ?: return
        viewModelScope.launch {
            classroomRepository.shuffleDisplayOrder(summary.classroom.id)
            audioManager.play(SoundEffectType.ACTION_SUCCESS)
            showMessage("Đã xáo trộn thứ tự hiển thị (xác suất chọn vẫn công bằng 100%)")
        }
    }

    fun resetClassroomCycle() {
        val summary = activeClassroomSummary.value ?: return
        viewModelScope.launch {
            classroomRepository.resetSelectionCycle(summary.classroom.id, incrementCycleNumber = true)
            audioManager.play(SoundEffectType.ACTION_SUCCESS)
            showMessage("Đã bắt đầu chu kỳ gọi mới cho lớp ${summary.classroom.name}")
        }
    }

    /**
     * Chọn mini-game từ Bộ sưu tập và quay lại màn hình trước đó (hoặc chuẩn bị lại lượt nếu trước đó là màn hình chơi game).
     */
    fun selectMiniGameFromCollection(game: MiniGameDescriptor) {
        settingsRepository.selectMiniGame(game.id)
        audioManager.play(SoundEffectType.ACTION_SUCCESS)
        val stack = _navigationStack.value
        val previousScreen = if (stack.size >= 2) stack[stack.size - 2] else ScreenDestination.HOME
        if (previousScreen == ScreenDestination.MINIGAME_PLAY && activeClassroomSummary.value != null) {
            openAndPrepareMiniGame(game.id)
        } else {
            _activeTurn.value?.let { currentTurn ->
                if (_miniGameRunState.value == MiniGameRunState.IDLE) {
                    _activeTurn.value = currentTurn.copy(miniGame = game)
                }
            }
            showMessage("Đã chọn mini-game: ${game.nameVi}")
            navigateBack()
        }
    }

    /**
     * Bắt đầu chọn ngay (bỏ qua mini-game, đi thẳng tới màn hình công bố kết quả).
     */
    fun pickStudentImmediately() {
        val summary = activeClassroomSummary.value
        if (summary == null) {
            showMessage("Vui lòng chọn hoặc tạo một lớp học trước")
            navigateTo(ScreenDestination.CLASS_LIST)
            return
        }
        val currentSettings = settingsRepository.settings.value
        val students = activeClassroomStudents.value
        val miniGame = MiniGameCatalog.findById(currentSettings.selectedMiniGameId)
        val pickCount = if (currentSettings.selectionMode == SelectionMode.MULTI_PICK) {
            currentSettings.pickCount.coerceAtLeast(2)
        } else {
            currentSettings.pickCount.coerceAtLeast(1)
        }

        // Hủy lượt đang treo nếu có
        selectionService.cancelActiveTurn()

        when (val outcome = selectionService.startSelectionTurn(
            classroom = summary.classroom,
            allStudents = students,
            mode = currentSettings.selectionMode,
            requestedCount = pickCount,
            miniGame = miniGame
        )) {
            is SelectionRequestOutcome.Success -> {
                _activeTurn.value = outcome.lockedTurn
                _miniGameRunState.value = MiniGameRunState.FINISHED
                commitLockedTurnAndShowResult(outcome.lockedTurn)
            }
            is SelectionRequestOutcome.CycleExhausted -> {
                audioManager.play(SoundEffectType.WARNING_ALERT)
                _cycleExhaustedPrompt.value = CycleExhaustedPrompt(
                    classroomId = outcome.classroomId,
                    cycleNumber = outcome.cycleNumber,
                    eligibleCount = outcome.eligibleCount,
                    messageVi = outcome.messageVi,
                    launchMiniGameAfterReset = false
                )
            }
            is SelectionRequestOutcome.InvalidPool -> {
                audioManager.play(SoundEffectType.WARNING_ALERT)
                showMessage(outcome.messageVi)
            }
            is SelectionRequestOutcome.Busy -> {
                showMessage(outcome.messageVi)
            }
        }
    }

    /**
     * Mở màn hình Mini-Game và khóa lượt chọn từ StudentSelectionService.
     */
    fun openAndPrepareMiniGame(customMiniGameId: String? = null) {
        if (customMiniGameId != null) {
            settingsRepository.selectMiniGame(customMiniGameId)
        }
        val currentSettings = settingsRepository.settings.value
        val targetGameId = customMiniGameId ?: currentSettings.selectedMiniGameId
        val miniGame = MiniGameCatalog.findById(targetGameId)

        val summary = activeClassroomSummary.value
        if (summary == null) {
            showMessage("Đã chọn \"${miniGame.nameVi}\". Vui lòng chọn hoặc tạo lớp học để bắt đầu!")
            navigateTo(ScreenDestination.CLASS_LIST)
            return
        }
        val students = activeClassroomStudents.value
        val pickCount = if (currentSettings.selectionMode == SelectionMode.MULTI_PICK) {
            currentSettings.pickCount.coerceAtLeast(2)
        } else {
            currentSettings.pickCount.coerceAtLeast(1)
        }

        selectionService.cancelActiveTurn()

        when (val outcome = selectionService.startSelectionTurn(
            classroom = summary.classroom,
            allStudents = students,
            mode = currentSettings.selectionMode,
            requestedCount = pickCount,
            miniGame = miniGame
        )) {
            is SelectionRequestOutcome.Success -> {
                _activeTurn.value = outcome.lockedTurn
                _currentTurnAnswerStatus.value = AnswerStatus.PENDING
                _miniGameRunState.value = if (currentSettings.autoStartMiniGame) {
                    audioManager.play(SoundEffectType.START_GAME)
                    MiniGameRunState.RUNNING
                } else {
                    MiniGameRunState.IDLE
                }
                val stack = _navigationStack.value
                when {
                    stack.lastOrNull() == ScreenDestination.MINIGAME_PLAY -> {
                        // Đã ở màn hình chơi game
                    }
                    stack.lastOrNull() == ScreenDestination.MINIGAME_COLLECTION -> {
                        val dropped = stack.dropLast(1)
                        _navigationStack.value = if (dropped.lastOrNull() == ScreenDestination.MINIGAME_PLAY) {
                            dropped
                        } else {
                            dropped + ScreenDestination.MINIGAME_PLAY
                        }
                    }
                    else -> {
                        navigateTo(ScreenDestination.MINIGAME_PLAY)
                    }
                }
            }
            is SelectionRequestOutcome.CycleExhausted -> {
                audioManager.play(SoundEffectType.WARNING_ALERT)
                _cycleExhaustedPrompt.value = CycleExhaustedPrompt(
                    classroomId = outcome.classroomId,
                    cycleNumber = outcome.cycleNumber,
                    eligibleCount = outcome.eligibleCount,
                    messageVi = outcome.messageVi,
                    launchMiniGameAfterReset = true
                )
            }
            is SelectionRequestOutcome.InvalidPool -> {
                audioManager.play(SoundEffectType.WARNING_ALERT)
                showMessage(outcome.messageVi)
            }
            is SelectionRequestOutcome.Busy -> {
                showMessage(outcome.messageVi)
            }
        }
    }

    fun confirmStartNewCycleFromPrompt() {
        val prompt = _cycleExhaustedPrompt.value ?: return
        _cycleExhaustedPrompt.value = null
        viewModelScope.launch {
            classroomRepository.resetSelectionCycle(prompt.classroomId, incrementCycleNumber = true)
            audioManager.play(SoundEffectType.ACTION_SUCCESS)
            showMessage("Đã bắt đầu chu kỳ #${prompt.cycleNumber + 1}")
            if (prompt.launchMiniGameAfterReset) {
                openAndPrepareMiniGame()
            }
        }
    }

    fun dismissCycleExhaustedPrompt() {
        _cycleExhaustedPrompt.value = null
    }

    /**
     * Bắt đầu chạy hoạt ảnh trong màn hình Mini-Game.
     * Chống bấm nhiều lần khi hoạt ảnh đang chạy.
     */
    fun startMiniGameAnimation() {
        if (_miniGameRunState.value == MiniGameRunState.RUNNING ||
            _miniGameRunState.value == MiniGameRunState.REVEALING
        ) {
            return
        }
        if (_activeTurn.value == null) {
            openAndPrepareMiniGame()
            if (_activeTurn.value == null) return
        }
        audioManager.play(SoundEffectType.START_GAME)
        _miniGameRunState.value = MiniGameRunState.RUNNING
    }

    /**
     * Khi giáo viên chủ động chạm bắt một cánh bướm/mục tiêu cụ thể (Chế độ chạm trực tiếp minh bạch).
     */
    fun onDirectButterflyCaught(student: StudentEntity) {
        val summary = activeClassroomSummary.value ?: return
        val currentSettings = settingsRepository.settings.value
        val miniGame = _activeTurn.value?.miniGame ?: MiniGameCatalog.findById(currentSettings.selectedMiniGameId)
        selectionService.cancelActiveTurn()
        val outcome = selectionService.lockDirectStudentPick(
            classroom = summary.classroom,
            student = student,
            allStudents = activeClassroomStudents.value,
            mode = currentSettings.selectionMode,
            miniGame = miniGame
        )
        if (outcome is SelectionRequestOutcome.Success) {
            _activeTurn.value = outcome.lockedTurn
            _miniGameRunState.value = MiniGameRunState.FINISHED
            commitLockedTurnAndShowResult(outcome.lockedTurn)
        }
    }

    /**
     * Hoàn tất hoạt ảnh mini-game hoặc bấm "Dừng / Công bố kết quả" và chuyển sang màn hình kết quả.
     */
    fun finishMiniGameAndRevealResult() {
        val turn = _activeTurn.value ?: return
        _miniGameRunState.value = MiniGameRunState.FINISHED
        commitLockedTurnAndShowResult(turn)
    }

    private fun commitLockedTurnAndShowResult(turn: LockedSelectionTurn) {
        val committed = selectionService.markTurnCommitted(turn.turnId) ?: turn.takeIf { !it.historyCommitted }
        if (committed != null) {
            _activeTurn.value = committed.copy(historyCommitted = true)
            _currentTurnAnswerStatus.value = AnswerStatus.PENDING
            audioManager.play(SoundEffectType.REVEAL_WINNER)
            viewModelScope.launch {
                historyRepository.recordCommittedTurn(committed, AnswerStatus.PENDING)
            }
        }
        if (_navigationStack.value.lastOrNull() == ScreenDestination.MINIGAME_PLAY) {
            _navigationStack.value = _navigationStack.value.dropLast(1) + ScreenDestination.RESULT_REVEAL
        } else if (_navigationStack.value.lastOrNull() != ScreenDestination.RESULT_REVEAL) {
            navigateTo(ScreenDestination.RESULT_REVEAL)
        }
    }

    fun cancelRunningMiniGame() {
        selectionService.cancelActiveTurn()
        _miniGameRunState.value = MiniGameRunState.CANCELLED
        _activeTurn.value = null
    }

    fun markCurrentTurnAnswerStatus(status: AnswerStatus) {
        val turn = _activeTurn.value ?: return
        _currentTurnAnswerStatus.value = status
        viewModelScope.launch {
            historyRepository.updateTurnAnswerStatus(turn.turnId, status)
            audioManager.play(SoundEffectType.ACTION_SUCCESS)
            showMessage("Đã ghi nhận trạng thái: ${status.labelVi}")
        }
    }

    fun undoCurrentTurnFromResultScreen() {
        val turn = _activeTurn.value ?: return
        viewModelScope.launch {
            val undone = historyRepository.undoTurn(turn.turnId)
            if (undone) {
                audioManager.play(SoundEffectType.WARNING_ALERT)
                val names = turn.winners.joinToString(", ") { it.fullName }
                showMessage("Đã hoàn tác lượt gọi của: $names")
                _activeTurn.value = null
                navigateBack()
            }
        }
    }

    fun undoLatestTurnInActiveClassroom() {
        val summary = activeClassroomSummary.value ?: return
        viewModelScope.launch {
            val studentName = historyRepository.undoLatestTurnForClassroom(summary.classroom.id)
            if (studentName != null) {
                audioManager.play(SoundEffectType.WARNING_ALERT)
                showMessage("Đã hoàn tác lượt gọi gần nhất ($studentName)")
            } else {
                showMessage("Chưa có lượt gọi nào trong lớp này để hoàn tác")
            }
        }
    }

    fun viewStudentHistory(studentId: String) {
        _studentFilterIdForHistory.value = studentId
        navigateTo(ScreenDestination.HISTORY)
    }

    fun clearStudentHistoryFilter() {
        _studentFilterIdForHistory.value = null
    }

    fun clearHistory(classroomIdOnly: String? = null) {
        viewModelScope.launch {
            if (classroomIdOnly != null) {
                historyRepository.clearHistoryForClassroom(classroomIdOnly)
                showMessage("Đã xóa lịch sử của lớp")
            } else {
                historyRepository.clearAllHistory()
                showMessage("Đã xóa toàn bộ lịch sử gọi tên")
            }
        }
    }

    suspend fun generateBackupJson(): String {
        val classes = classroomRepository.getAllClassroomsOnce()
        val students = classroomRepository.getAllStudentsOnce()
        val history = historyRepository.getAllHistoryOnce()
        return SaveManager.exportBackupJson(BackupPayload(classes, students, history))
    }

    fun restoreFromBackupJson(json: String) {
        viewModelScope.launch {
            val result = SaveManager.importBackupJson(json)
            result.onSuccess { payload ->
                classroomRepository.replaceAllFromBackup(payload.classrooms, payload.students)
                historyRepository.replaceAllHistoryFromBackup(payload.history)
                payload.classrooms.firstOrNull()?.let {
                    settingsRepository.setActiveClassroomId(it.id)
                }
                audioManager.play(SoundEffectType.ACTION_SUCCESS)
                showMessage("Khôi phục thành công ${payload.classrooms.size} lớp và ${payload.students.size} học sinh!")
            }.onFailure { err ->
                audioManager.play(SoundEffectType.WARNING_ALERT)
                showMessage("Lỗi đọc dữ liệu sao lưu: ${err.message}")
            }
        }
    }
}
