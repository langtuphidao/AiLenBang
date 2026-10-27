package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.screens.AboutAuthorScreen
import com.example.ui.screens.ClassListScreen
import com.example.ui.screens.ClassroomDashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MiniGameCollectionScreen
import com.example.ui.screens.MiniGamePlayScreen
import com.example.ui.screens.ResultRevealScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudentManagerScreen
import com.example.ui.theme.AiLenBangTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            AiLenBangApp(viewModel = mainViewModel)
        }
    }
}

@Composable
fun AiLenBangApp(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val summaries by viewModel.classroomSummaries.collectAsStateWithLifecycle()
    val activeSummary by viewModel.activeClassroomSummary.collectAsStateWithLifecycle()
    val activeStudents by viewModel.activeClassroomStudents.collectAsStateWithLifecycle()
    val selectedMiniGame by viewModel.selectedMiniGame.collectAsStateWithLifecycle()
    val activeTurn by viewModel.activeTurn.collectAsStateWithLifecycle()
    val miniGameRunState by viewModel.miniGameRunState.collectAsStateWithLifecycle()
    val currentTurnAnswerStatus by viewModel.currentTurnAnswerStatus.collectAsStateWithLifecycle()
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val classrooms by viewModel.classrooms.collectAsStateWithLifecycle()
    val cycleExhaustedPrompt by viewModel.cycleExhaustedPrompt.collectAsStateWithLifecycle()
    val duplicateNamesPrompt by viewModel.duplicateNamesPrompt.collectAsStateWithLifecycle()
    val studentFilterId by viewModel.studentFilterIdForHistory.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    AiLenBangTheme(
        darkTheme = settings.darkMode,
        presentationMode = settings.presentationMode
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    ScreenDestination.HOME -> HomeScreen(
                        activeClassroomSummary = activeSummary,
                        selectedMiniGame = selectedMiniGame,
                        onStartClicked = {
                            if (activeSummary != null) {
                                viewModel.navigateTo(ScreenDestination.CLASS_DASHBOARD)
                            } else {
                                viewModel.navigateTo(ScreenDestination.CLASS_LIST)
                            }
                        },
                        onManageClassesClicked = {
                            viewModel.navigateTo(ScreenDestination.CLASS_LIST)
                        },
                        onHistoryClicked = {
                            viewModel.navigateTo(ScreenDestination.HISTORY)
                        },
                        onMiniGameCollectionClicked = {
                            viewModel.navigateTo(ScreenDestination.MINIGAME_COLLECTION)
                        },
                        onSettingsClicked = {
                            viewModel.navigateTo(ScreenDestination.SETTINGS)
                        },
                        onAboutAuthorClicked = {
                            viewModel.navigateTo(ScreenDestination.ABOUT_AUTHOR)
                        },
                        onCreateDemoClassClicked = {
                            viewModel.createOptionalDemoClassroom()
                        }
                    )

                    ScreenDestination.CLASS_LIST -> ClassListScreen(
                        summaries = summaries,
                        onStartClassroom = { classId ->
                            viewModel.selectClassroomAndOpenDashboard(classId)
                        },
                        onManageRoster = { classId ->
                            viewModel.selectClassroomForManagement(classId)
                        },
                        onCreateClassroom = { name, note ->
                            viewModel.createClassroom(name, note, openImmediately = true)
                        },
                        onRenameClassroom = { id, name, note ->
                            viewModel.renameClassroom(id, name, note)
                        },
                        onDuplicateClassroom = { id ->
                            viewModel.duplicateClassroom(id)
                        },
                        onDeleteClassroom = { id ->
                            viewModel.deleteClassroom(id)
                        },
                        onCreateDemoClassroom = {
                            viewModel.createOptionalDemoClassroom()
                        },
                        onBack = { viewModel.navigateBack() }
                    )

                    ScreenDestination.CLASS_DASHBOARD -> {
                        val classHistory = remember(allHistory, activeSummary) {
                            val cid = activeSummary?.classroom?.id
                            if (cid == null) emptyList()
                            else allHistory.filter { it.classroomId == cid }
                        }
                        ClassroomDashboardScreen(
                            summary = activeSummary,
                            selectedMiniGame = selectedMiniGame,
                            settings = settings,
                            recentHistory = classHistory,
                            onPickImmediately = { viewModel.pickStudentImmediately() },
                            onPlayMiniGame = { viewModel.openAndPrepareMiniGame() },
                            onChangeMiniGame = { viewModel.navigateTo(ScreenDestination.MINIGAME_COLLECTION) },
                            onQuickSelectMiniGame = { gameId ->
                                viewModel.settingsRepository.selectMiniGame(gameId)
                            },
                            onOpenStudentRoster = { viewModel.navigateTo(ScreenDestination.STUDENT_MANAGER) },
                            onOpenHistory = { viewModel.navigateTo(ScreenDestination.HISTORY) },
                            onUndoLastTurn = { viewModel.undoLatestTurnInActiveClassroom() },
                            onResetCycle = { viewModel.resetClassroomCycle() },
                            onChangeSelectionMode = { mode ->
                                viewModel.settingsRepository.updateSettings { it.copy(selectionMode = mode) }
                            },
                            onChangePickCount = { count ->
                                viewModel.settingsRepository.updateSettings { it.copy(pickCount = count) }
                            },
                            onSwitchClass = { viewModel.navigateTo(ScreenDestination.CLASS_LIST) },
                            onBack = { viewModel.navigateBack() }
                        )
                    }

                    ScreenDestination.STUDENT_MANAGER -> StudentManagerScreen(
                        summary = activeSummary,
                        students = activeStudents,
                        duplicatePrompt = duplicateNamesPrompt,
                        onAddSingleStudent = { name -> viewModel.addSingleStudent(name) },
                        onBulkImportText = { raw -> viewModel.importStudentsBulkText(raw) },
                        onConfirmDuplicateImport = { keep -> viewModel.confirmDuplicateNamesImport(keep) },
                        onDismissDuplicateImport = { viewModel.dismissDuplicateNamesPrompt() },
                        onUpdateStudentName = { id, name -> viewModel.updateStudentName(id, name) },
                        onDeleteStudent = { id -> viewModel.deleteStudent(id) },
                        onToggleAbsent = { s -> viewModel.toggleStudentAbsent(s) },
                        onToggleExcluded = { s -> viewModel.toggleStudentExcluded(s) },
                        onBulkExcludeStudents = { ids, excl -> viewModel.setBulkStudentsExcluded(ids, excl) },
                        onToggleManualSubset = { s -> viewModel.toggleStudentManualSubset(s) },
                        onSetAllManualSubset = { inSub -> viewModel.setAllStudentsManualSubset(inSub) },
                        onSortAlphabetically = { viewModel.sortStudentsAlphabetically() },
                        onShuffleDisplayOrder = { viewModel.shuffleStudentsDisplayOrder() },
                        onResetCycle = { viewModel.resetClassroomCycle() },
                        onViewStudentHistory = { sid -> viewModel.viewStudentHistory(sid) },
                        onBack = { viewModel.navigateBack() }
                    )

                    ScreenDestination.MINIGAME_COLLECTION -> MiniGameCollectionScreen(
                        selectedMiniGameId = settings.selectedMiniGameId,
                        recentMiniGameIds = settings.recentMiniGameIds,
                        onSelectAndReturn = { game ->
                            viewModel.selectMiniGameFromCollection(game)
                        },
                        onPlayNow = { game ->
                            viewModel.openAndPrepareMiniGame(game.id)
                        },
                        onBack = { viewModel.navigateBack() }
                    )

                    ScreenDestination.MINIGAME_PLAY -> MiniGamePlayScreen(
                        lockedTurn = activeTurn,
                        runState = miniGameRunState,
                        settings = settings,
                        audioManager = viewModel.audioManager,
                        onStartGame = { viewModel.startMiniGameAnimation() },
                        onStopAndRevealNow = { viewModel.finishMiniGameAndRevealResult() },
                        onReplayTurn = { viewModel.openAndPrepareMiniGame() },
                        onChangeMiniGame = { viewModel.navigateTo(ScreenDestination.MINIGAME_COLLECTION) },
                        onDirectTapPick = { student -> viewModel.onDirectButterflyCaught(student) },
                        onBackToClass = { viewModel.navigateBack() }
                    )

                    ScreenDestination.RESULT_REVEAL -> ResultRevealScreen(
                        lockedTurn = activeTurn,
                        currentAnswerStatus = currentTurnAnswerStatus,
                        onSelectAnswerStatus = { st -> viewModel.markCurrentTurnAnswerStatus(st) },
                        onCallNextStudentWithMiniGame = { viewModel.openAndPrepareMiniGame() },
                        onCallNextStudentImmediately = { viewModel.pickStudentImmediately() },
                        onChangeMiniGame = { viewModel.navigateTo(ScreenDestination.MINIGAME_COLLECTION) },
                        onUndoThisTurn = { viewModel.undoCurrentTurnFromResultScreen() },
                        onBackToClass = { viewModel.navigateBack() }
                    )

                    ScreenDestination.HISTORY -> HistoryScreen(
                        allHistory = allHistory,
                        classrooms = classrooms,
                        studentFilterId = studentFilterId,
                        onClearStudentFilter = { viewModel.clearStudentHistoryFilter() },
                        onUpdateRecordStatus = { recId, st ->
                            viewModel.markCurrentTurnAnswerStatus(st)
                        },
                        onUndoTurn = { turnId ->
                            viewModel.undoCurrentTurnFromResultScreen()
                        },
                        onClearHistory = { cid -> viewModel.clearHistory(cid) },
                        onBack = { viewModel.navigateBack() }
                    )

                    ScreenDestination.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onUpdateSettings = { transform -> viewModel.settingsRepository.updateSettings(transform) },
                        onResetDefaultSettings = {
                            viewModel.settingsRepository.resetToDefaults()
                            viewModel.showMessage("Đã khôi phục cài đặt mặc định")
                        },
                        onGenerateBackupJson = { viewModel.generateBackupJson() },
                        onRestoreBackupJson = { json -> viewModel.restoreFromBackupJson(json) },
                        onOpenAboutAuthor = {
                            viewModel.navigateTo(ScreenDestination.ABOUT_AUTHOR)
                        },
                        onBack = { viewModel.navigateBack() }
                    )

                    ScreenDestination.ABOUT_AUTHOR -> AboutAuthorScreen(
                        onBack = { viewModel.navigateBack() }
                    )
                }

                // Hộp thoại xác nhận đặt lại chu kỳ mới khi toàn bộ học sinh đủ điều kiện đã được gọi
                cycleExhaustedPrompt?.let { prompt ->
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissCycleExhaustedPrompt() },
                        title = { Text("Đã hoàn tất chu kỳ #${prompt.cycleNumber}") },
                        text = { Text(prompt.messageVi) },
                        confirmButton = {
                            Button(onClick = { viewModel.confirmStartNewCycleFromPrompt() }) {
                                Text("Bắt đầu chu kỳ mới")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.dismissCycleExhaustedPrompt() }) {
                                Text("Để sau")
                            }
                        }
                    )
                }
            }
        }
    }
}
