package com.example.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class SelectionMode(val titleVi: String, val descriptionVi: String) {
    NORMAL_RANDOM(
        "Chế độ A: Ngẫu nhiên thông thường",
        "Mỗi học sinh đủ điều kiện có xác suất được chọn hoàn toàn như nhau."
    ),
    NO_REPEAT_IN_CYCLE(
        "Chế độ B: Không trùng trong chu kỳ",
        "Mỗi học sinh chỉ được gọi tối đa 1 lần cho đến khi hết chu kỳ. Hỏi xác nhận khi sang chu kỳ mới."
    ),
    ALLOW_REPEAT(
        "Chế độ C: Cho phép chọn lặp lại",
        "Mỗi lượt chọn ngẫu nhiên từ toàn bộ học sinh đủ điều kiện, kể cả người đã được gọi."
    ),
    PRIORITIZE_UNCALLED(
        "Chế độ D: Ưu tiên chưa được gọi",
        "Chỉ chọn từ nhóm chưa được gọi trong chu kỳ hiện tại. Khi hết nhóm sẽ hỏi đặt lại chu kỳ."
    ),
    MULTI_PICK(
        "Chế độ E: Chọn nhiều người",
        "Chọn 2, 3 hoặc N học sinh cùng lúc trong một lượt mà không bị trùng nhau."
    ),
    MANUAL_SUBSET(
        "Chế độ F: Danh sách thủ công",
        "Giáo viên đánh dấu một nhóm học sinh cụ thể và chỉ random trong nhóm đó."
    )
}

enum class AnswerStatus(val labelVi: String) {
    PENDING("Chờ xác nhận"),
    ANSWERED("Đã trả bài"),
    NOT_ANSWERED("Chưa trả bài"),
    ABSENT("Vắng mặt"),
    SKIPPED("Bỏ qua lượt")
}

enum class MiniGameCategory(val titleVi: String, val emoji: String) {
    ANIMALS("Động vật", "🦆"),
    SCI_FI("Khoa học viễn tưởng", "🛸"),
    LUCK("May mắn", "🎡"),
    ADVENTURE("Phiêu lưu", "🏴‍☠️"),
    STAGE("Sân khấu", "🎭")
}

enum class MiniGameEngineType {
    DUCK_RACE,
    UFO_ABDUCTION,
    BUTTERFLY_NET,
    LUCKY_WHEEL,
    ROCKET_LAUNCH,
    STAGE_SPOTLIGHT,
    TREASURE_CHEST,
    CLAW_MACHINE
}

data class MiniGameDescriptor(
    val id: String,
    val number: Int,
    val nameVi: String,
    val shortDescriptionVi: String,
    val category: MiniGameCategory,
    val engineType: MiniGameEngineType,
    val iconEmoji: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val isMvpCore: Boolean = false,
    val supportsDirectTapPick: Boolean = false
)

@Entity(tableName = "classrooms")
data class ClassroomEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val gradeNote: String = "",
    val currentCycleNumber: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "students",
    indices = [Index(value = ["classroomId"])]
)
data class StudentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val classroomId: String,
    val fullName: String,
    val isActive: Boolean = true,
    val isEligible: Boolean = true,
    val isAbsent: Boolean = false,
    val isManuallyExcluded: Boolean = false,
    val isInManualSubset: Boolean = false,
    val selectedCountInCycle: Int = 0,
    val totalSelectedCount: Int = 0,
    val lastSelectedAt: Long? = null,
    val displayOrder: Int = 0
) {
    fun canBeSelected(mode: SelectionMode): Boolean {
        if (!isActive || !isEligible || isAbsent || isManuallyExcluded) return false
        if (mode == SelectionMode.MANUAL_SUBSET && !isInManualSubset) return false
        return true
    }
}

@Entity(
    tableName = "selection_history",
    indices = [
        Index(value = ["classroomId"]),
        Index(value = ["studentId"]),
        Index(value = ["turnId"])
    ]
)
data class SelectionHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val turnId: String,
    val classroomId: String,
    val classroomNameSnapshot: String,
    val studentId: String,
    val studentNameSnapshot: String,
    val calledAt: Long,
    val miniGameId: String,
    val miniGameNameSnapshot: String,
    val answerStatus: String = AnswerStatus.PENDING.name,
    val selectionModeSnapshot: String,
    val cycleNumberSnapshot: Int,
    val previousSelectedCountInCycle: Int = 0,
    val previousLastSelectedAt: Long? = null
) {
    val statusEnum: AnswerStatus
        get() = runCatching { AnswerStatus.valueOf(answerStatus) }.getOrDefault(AnswerStatus.PENDING)
}

data class ClassroomSummary(
    val classroom: ClassroomEntity,
    val totalStudents: Int,
    val calledInCycleCount: Int,
    val uncalledInCycleCount: Int,
    val absentOrExcludedCount: Int,
    val manualSubsetCount: Int
)

data class AppSettings(
    val language: String = "vi",
    val darkMode: Boolean = false,
    val presentationMode: Boolean = false,
    val highQualityEffects: Boolean = true,
    val bgmEnabled: Boolean = false,
    val sfxEnabled: Boolean = true,
    val volume: Float = 0.8f,
    val quietTeachingMode: Boolean = false,
    val selectionMode: SelectionMode = SelectionMode.NO_REPEAT_IN_CYCLE,
    val pickCount: Int = 1,
    val animationDurationSeconds: Int = 5,
    val autoStartMiniGame: Boolean = false,
    val showHistoryOnClassScreen: Boolean = true,
    val confirmBeforeResetCycle: Boolean = true,
    val selectedMiniGameId: String = "duck_race",
    val recentMiniGameIds: List<String> = listOf(
        "duck_race",
        "ufo_abduction",
        "butterfly_net",
        "lucky_wheel",
        "rocket_launch"
    )
)

enum class MiniGameRunState {
    IDLE,
    RUNNING,
    REVEALING,
    FINISHED,
    CANCELLED,
    ERROR
}

data class LockedSelectionTurn(
    val turnId: String = UUID.randomUUID().toString(),
    val classroomId: String,
    val classroomName: String,
    val winners: List<StudentEntity>,
    val visualParticipants: List<StudentEntity>,
    val selectionMode: SelectionMode,
    val cycleNumber: Int,
    val miniGame: MiniGameDescriptor,
    val isManualDirectPick: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val historyCommitted: Boolean = false
)

sealed class SelectionRequestOutcome {
    data class Success(val lockedTurn: LockedSelectionTurn) : SelectionRequestOutcome()
    data class CycleExhausted(
        val classroomId: String,
        val cycleNumber: Int,
        val eligibleCount: Int,
        val messageVi: String
    ) : SelectionRequestOutcome()
    data class InvalidPool(
        val requestedCount: Int,
        val availableCount: Int,
        val messageVi: String
    ) : SelectionRequestOutcome()
    data class Busy(val activeTurnId: String, val messageVi: String) : SelectionRequestOutcome()
}

object MiniGameCatalog {
    val allGames: List<MiniGameDescriptor> = listOf(
        // A. ĐỘNG VẬT (1-6)
        MiniGameDescriptor(
            id = "duck_race",
            number = 1,
            nameVi = "Đua vịt",
            shortDescriptionVi = "Các chú vịt mang tên học sinh bơi đua sôi động về vạch đích.",
            category = MiniGameCategory.ANIMALS,
            engineType = MiniGameEngineType.DUCK_RACE,
            iconEmoji = "🦆",
            primaryColorHex = 0xFFFFB703,
            secondaryColorHex = 0xFF219EBC,
            isMvpCore = true
        ),
        MiniGameDescriptor(
            id = "frog_lotus",
            number = 2,
            nameVi = "Ếch nhảy ao sen",
            shortDescriptionVi = "Những chú ếch xanh bật nhảy qua các lá sen tiến tới bông sen vàng.",
            category = MiniGameCategory.ANIMALS,
            engineType = MiniGameEngineType.DUCK_RACE,
            iconEmoji = "🐸",
            primaryColorHex = 0xFF06D6A0,
            secondaryColorHex = 0xFF118AB2
        ),
        MiniGameDescriptor(
            id = "penguin_slide",
            number = 3,
            nameVi = "Chim cánh cụt trượt băng",
            shortDescriptionVi = "Đường trượt băng lấp lánh nơi các chú chim cánh cụt lướt về đích.",
            category = MiniGameCategory.ANIMALS,
            engineType = MiniGameEngineType.DUCK_RACE,
            iconEmoji = "🐧",
            primaryColorHex = 0xFF4CC9F0,
            secondaryColorHex = 0xFF3A0CA3
        ),
        MiniGameDescriptor(
            id = "rabbit_carrot",
            number = 4,
            nameVi = "Thỏ đua cà rốt",
            shortDescriptionVi = "Đàn thỏ trắng bật nhảy về phía củ cà rốt khổng lồ cuối đồng cỏ.",
            category = MiniGameCategory.ANIMALS,
            engineType = MiniGameEngineType.DUCK_RACE,
            iconEmoji = "🐰",
            primaryColorHex = 0xFFFF758F,
            secondaryColorHex = 0xFF52B788
        ),
        MiniGameDescriptor(
            id = "turtle_finish",
            number = 5,
            nameVi = "Rùa bò về đích",
            shortDescriptionVi = "Cuộc đua chậm mà chắc đầy kịch tính ở những mét cuối cùng.",
            category = MiniGameCategory.ANIMALS,
            engineType = MiniGameEngineType.DUCK_RACE,
            iconEmoji = "🐢",
            primaryColorHex = 0xFF74C69D,
            secondaryColorHex = 0xFF40916C
        ),
        MiniGameDescriptor(
            id = "butterfly_net",
            number = 6,
            nameVi = "Vợt bướm",
            shortDescriptionVi = "Cánh bướm rực rỡ bay lượn — hỗ trợ cả Bắt Ngẫu Nhiên lẫn Chạm Trực Tiếp minh bạch!",
            category = MiniGameCategory.ANIMALS,
            engineType = MiniGameEngineType.BUTTERFLY_NET,
            iconEmoji = "🦋",
            primaryColorHex = 0xFFFF6B35,
            secondaryColorHex = 0xFF8338EC,
            isMvpCore = true,
            supportsDirectTapPick = true
        ),

        // B. KHOA HỌC VIỄN TƯỞNG (7-12)
        MiniGameDescriptor(
            id = "ufo_abduction",
            number = 7,
            nameVi = "UFO hút học sinh",
            shortDescriptionVi = "Đĩa bay ngoài hành tinh chiếu tia sáng rà soát sân trường và kéo người được chọn lên tàu.",
            category = MiniGameCategory.SCI_FI,
            engineType = MiniGameEngineType.UFO_ABDUCTION,
            iconEmoji = "🛸",
            primaryColorHex = 0xFF7209B7,
            secondaryColorHex = 0xFF4CC9F0,
            isMvpCore = true
        ),
        MiniGameDescriptor(
            id = "rocket_launch",
            number = 8,
            nameVi = "Tên lửa phóng",
            shortDescriptionVi = "Radar vũ trụ khóa mục tiêu, đếm ngược và phóng tên lửa mang tên học sinh lên quỹ đạo.",
            category = MiniGameCategory.SCI_FI,
            engineType = MiniGameEngineType.ROCKET_LAUNCH,
            iconEmoji = "🚀",
            primaryColorHex = 0xFFF72585,
            secondaryColorHex = 0xFF3A0CA3,
            isMvpCore = true
        ),
        MiniGameDescriptor(
            id = "robot_scanner",
            number = 9,
            nameVi = "Robot quét mục tiêu",
            shortDescriptionVi = "Robot thông minh chiếu tia hồng ngoại quét qua danh sách và khóa mục tiêu.",
            category = MiniGameCategory.SCI_FI,
            engineType = MiniGameEngineType.UFO_ABDUCTION,
            iconEmoji = "🤖",
            primaryColorHex = 0xFF4361EE,
            secondaryColorHex = 0xFF4CC9F0
        ),
        MiniGameDescriptor(
            id = "portal_gate",
            number = 10,
            nameVi = "Cổng dịch chuyển",
            shortDescriptionVi = "Vòng xoáy không gian mở ra và đưa nhân vật được chọn bước lên sân khấu.",
            category = MiniGameCategory.SCI_FI,
            engineType = MiniGameEngineType.STAGE_SPOTLIGHT,
            iconEmoji = "🌀",
            primaryColorHex = 0xFF560BAD,
            secondaryColorHex = 0xFFB5179E
        ),
        MiniGameDescriptor(
            id = "zero_gravity",
            number = 11,
            nameVi = "Phi hành gia mất trọng lực",
            shortDescriptionVi = "Các phi hành gia bay lơ lửng giữa trạm vũ trụ trước khi cửa khoang đón người được chọn.",
            category = MiniGameCategory.SCI_FI,
            engineType = MiniGameEngineType.BUTTERFLY_NET,
            iconEmoji = "🧑‍🚀",
            primaryColorHex = 0xFF3F37C9,
            secondaryColorHex = 0xFF4895EF
        ),
        MiniGameDescriptor(
            id = "laser_scanner",
            number = 12,
            nameVi = "Máy quét laser",
            shortDescriptionVi = "Chùm tia laser neon quét tốc độ cao qua bảng tên và dừng tại người chiến thắng.",
            category = MiniGameCategory.SCI_FI,
            engineType = MiniGameEngineType.STAGE_SPOTLIGHT,
            iconEmoji = "⚡",
            primaryColorHex = 0xFF00BBF9,
            secondaryColorHex = 0xFFF15BB5
        ),

        // C. MAY MẮN (13-18)
        MiniGameDescriptor(
            id = "lucky_wheel",
            number = 13,
            nameVi = "Vòng quay may mắn",
            shortDescriptionVi = "Vòng quay chia đều các ô tên học sinh, giảm tốc mượt mà và dừng chuẩn xác tại kim chỉ.",
            category = MiniGameCategory.LUCK,
            engineType = MiniGameEngineType.LUCKY_WHEEL,
            iconEmoji = "🎡",
            primaryColorHex = 0xFFEF476F,
            secondaryColorHex = 0xFFFFD166,
            isMvpCore = true
        ),
        MiniGameDescriptor(
            id = "treasure_chest",
            number = 14,
            nameVi = "Rương kho báu",
            shortDescriptionVi = "Chiếc rương vàng rung lắc rồi bật nắp tung ánh hào quang cùng tên học sinh.",
            category = MiniGameCategory.LUCK,
            engineType = MiniGameEngineType.TREASURE_CHEST,
            iconEmoji = "👑",
            primaryColorHex = 0xFFFFB703,
            secondaryColorHex = 0xFFFB8500
        ),
        MiniGameDescriptor(
            id = "balloon_pop",
            number = 15,
            nameVi = "Nổ bóng bay",
            shortDescriptionVi = "Những quả bóng bay đầy màu sắc bay lên và phát nổ tung pháo giấy hé lộ tên.",
            category = MiniGameCategory.LUCK,
            engineType = MiniGameEngineType.BUTTERFLY_NET,
            iconEmoji = "🎈",
            primaryColorHex = 0xFFFF595E,
            secondaryColorHex = 0xFFFFCA3A
        ),
        MiniGameDescriptor(
            id = "mystery_gift",
            number = 16,
            nameVi = "Hộp quà bí ẩn",
            shortDescriptionVi = "Mở hộp quà thắt nơ rực rỡ để khám phá học sinh may mắn của lượt này.",
            category = MiniGameCategory.LUCK,
            engineType = MiniGameEngineType.TREASURE_CHEST,
            iconEmoji = "🎁",
            primaryColorHex = 0xFF8AC926,
            secondaryColorHex = 0xFF1982C4
        ),
        MiniGameDescriptor(
            id = "spin_bottle",
            number = 17,
            nameVi = "Chai xoay gọi tên",
            shortDescriptionVi = "Chiếc chai xoay tròn giữa vòng tròn học sinh và dừng mũi chỉ đúng người được chọn.",
            category = MiniGameCategory.LUCK,
            engineType = MiniGameEngineType.LUCKY_WHEEL,
            iconEmoji = "🍾",
            primaryColorHex = 0xFF06D6A0,
            secondaryColorHex = 0xFF118AB2
        ),
        MiniGameDescriptor(
            id = "secret_card",
            number = 18,
            nameVi = "Lật thẻ bí mật",
            shortDescriptionVi = "Tráo các tấm thẻ bài ma thuật trên bàn và lật mở tấm thẻ mang tên người được gọi.",
            category = MiniGameCategory.LUCK,
            engineType = MiniGameEngineType.TREASURE_CHEST,
            iconEmoji = "🃏",
            primaryColorHex = 0xFF6A4C93,
            secondaryColorHex = 0xFFFFCA3A
        ),

        // D. PHIÊU LƯU (19-24)
        MiniGameDescriptor(
            id = "pirate_ship",
            number = 19,
            nameVi = "Tàu cướp biển",
            shortDescriptionVi = "Con tàu vượt sóng tiến về hòn đảo kho báu mang tên học sinh được chọn.",
            category = MiniGameCategory.ADVENTURE,
            engineType = MiniGameEngineType.DUCK_RACE,
            iconEmoji = "🏴‍☠️",
            primaryColorHex = 0xFFE63946,
            secondaryColorHex = 0xFF1D3557
        ),
        MiniGameDescriptor(
            id = "fishing_pick",
            number = 20,
            nameVi = "Câu cá gọi tên",
            shortDescriptionVi = "Thả cần câu xuống mặt hồ trong xanh và kéo lên chú cá vàng mang tên học sinh.",
            category = MiniGameCategory.ADVENTURE,
            engineType = MiniGameEngineType.CLAW_MACHINE,
            iconEmoji = "🎣",
            primaryColorHex = 0xFF0096C7,
            secondaryColorHex = 0xFF90E0EF
        ),
        MiniGameDescriptor(
            id = "student_bowling",
            number = 21,
            nameVi = "Bowling học sinh",
            shortDescriptionVi = "Quả bóng bowling lăn mạnh trên đường băng và strike đúng vị trí mục tiêu.",
            category = MiniGameCategory.ADVENTURE,
            engineType = MiniGameEngineType.ROCKET_LAUNCH,
            iconEmoji = "🎳",
            primaryColorHex = 0xFFD90429,
            secondaryColorHex = 0xFF2B2D42
        ),
        MiniGameDescriptor(
            id = "penalty_kick",
            number = 22,
            nameVi = "Sút bóng trúng tên",
            shortDescriptionVi = "Cú sút phạt vẽ đường cong tuyệt đẹp bay thẳng vào góc khung thành mang tên học sinh.",
            category = MiniGameCategory.ADVENTURE,
            engineType = MiniGameEngineType.ROCKET_LAUNCH,
            iconEmoji = "⚽",
            primaryColorHex = 0xFF2EC4B6,
            secondaryColorHex = 0xFFFF9F1C
        ),
        MiniGameDescriptor(
            id = "claw_machine",
            number = 23,
            nameVi = "Máy gắp thú",
            shortDescriptionVi = "Càng gắp di chuyển ngang dọc rồi hạ xuống gắp trúng nhân vật mang tên học sinh.",
            category = MiniGameCategory.ADVENTURE,
            engineType = MiniGameEngineType.CLAW_MACHINE,
            iconEmoji = "🕹️",
            primaryColorHex = 0xFFFF4D6D,
            secondaryColorHex = 0xFFC9184A
        ),
        MiniGameDescriptor(
            id = "hot_air_balloon",
            number = 24,
            nameVi = "Khinh khí cầu",
            shortDescriptionVi = "Khinh khí cầu rực rỡ bay vút lên bầu trời xanh cùng biểu ngữ tên học sinh.",
            category = MiniGameCategory.ADVENTURE,
            engineType = MiniGameEngineType.ROCKET_LAUNCH,
            iconEmoji = "🎈",
            primaryColorHex = 0xFFF4A261,
            secondaryColorHex = 0xFF2A9D8F
        ),

        // E. SÂN KHẤU (25-30)
        MiniGameDescriptor(
            id = "stage_spotlight",
            number = 25,
            nameVi = "Đèn sân khấu",
            shortDescriptionVi = "Ánh đèn spotlight sân khấu lia qua các vị trí trong khán phòng rồi hội tụ tại người được chọn.",
            category = MiniGameCategory.STAGE,
            engineType = MiniGameEngineType.STAGE_SPOTLIGHT,
            iconEmoji = "🔦",
            primaryColorHex = 0xFFFFBE0B,
            secondaryColorHex = 0xFF3A86FF
        ),
        MiniGameDescriptor(
            id = "magic_hat",
            number = 26,
            nameVi = "Mũ ảo thuật",
            shortDescriptionVi = "Chiếc mũ ảo thuật gia tung sao lấp lánh và rút ra tấm thẻ tên bất ngờ.",
            category = MiniGameCategory.STAGE,
            engineType = MiniGameEngineType.TREASURE_CHEST,
            iconEmoji = "🎩",
            primaryColorHex = 0xFF7209B7,
            secondaryColorHex = 0xFFF72585
        ),
        MiniGameDescriptor(
            id = "pinata_smash",
            number = 27,
            nameVi = "Đập piñata",
            shortDescriptionVi = "Chú ngựa giấy piñata đầy màu sắc tung kẹo ngọt và băng rôn tên học sinh.",
            category = MiniGameCategory.STAGE,
            engineType = MiniGameEngineType.TREASURE_CHEST,
            iconEmoji = "🪅",
            primaryColorHex = 0xFFFF006E,
            secondaryColorHex = 0xFFFB5607
        ),
        MiniGameDescriptor(
            id = "fireworks_call",
            number = 28,
            nameVi = "Pháo hoa gọi tên",
            shortDescriptionVi = "Pháo hoa rực sáng trên bầu trời đêm và xếp thành tên học sinh lên bảng.",
            category = MiniGameCategory.STAGE,
            engineType = MiniGameEngineType.ROCKET_LAUNCH,
            iconEmoji = "🎆",
            primaryColorHex = 0xFF8338EC,
            secondaryColorHex = 0xFFFFBE0B
        ),
        MiniGameDescriptor(
            id = "secret_elevator",
            number = 29,
            nameVi = "Thang máy bí mật",
            shortDescriptionVi = "Thang máy nhảy số qua các tầng lầu rồi mở cửa đón học sinh được chọn.",
            category = MiniGameCategory.STAGE,
            engineType = MiniGameEngineType.STAGE_SPOTLIGHT,
            iconEmoji = "🛗",
            primaryColorHex = 0xFF0077B6,
            secondaryColorHex = 0xFF00B4D8
        ),
        MiniGameDescriptor(
            id = "gameshow_reveal",
            number = 30,
            nameVi = "Gameshow gọi tên",
            shortDescriptionVi = "Sân khấu truyền hình rực rỡ ánh đèn và bảng điện tử công bố người lên bảng.",
            category = MiniGameCategory.STAGE,
            engineType = MiniGameEngineType.STAGE_SPOTLIGHT,
            iconEmoji = "🏆",
            primaryColorHex = 0xFFFF9F1C,
            secondaryColorHex = 0xFFE71D36
        )
    )

    fun findById(id: String): MiniGameDescriptor =
        allGames.find { it.id == id } ?: allGames.first()
}
