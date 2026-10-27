# AI LÊN BẢNG? (Who's Next? Classroom Picker)

Ứng dụng Android chọn ngẫu nhiên học sinh lên bảng trả bài, phát biểu và tham gia hoạt động lớp học thông qua **hệ thống 30 Mini-Game 2D vui nhộn**, công bằng và hoạt động **100% Offline**.

---

## 1. Kiến trúc hệ thống (Modular Architecture)

Dự án được thiết kế tách biệt rõ ràng giữa dữ liệu, thuật toán chọn ngẫu nhiên, điều khiển mini-game và giao diện:

```text
app/src/main/java/com/example/
├── core/
│   └── StudentSelectionService.kt     # Dịch vụ trung tâm chọn ngẫu nhiên (Modes A-F, Fisher-Yates, khóa lượt & chống ghi trùng)
├── models/
│   └── DomainModels.kt                # ClassroomEntity, StudentEntity, SelectionHistoryEntity, SelectionMode, AnswerStatus, MiniGameCatalog (30 games)
├── repositories/
│   ├── AppDatabase.kt                 # Room SQLite Database bền vững cục bộ
│   ├── ClassroomRepository.kt         # Quản lý lớp & 15 thao tác danh sách học sinh
│   ├── HistoryRepository.kt           # Quản lý lịch sử gọi tên, snapshot tên học sinh & Hoàn tác (Undo)
│   └── SettingsRepository.kt          # Lưu trữ cấu hình âm thanh, trình chiếu, quy tắc chọn
├── services/
│   ├── AudioManager.kt                # Tổng hợp hiệu ứng âm thanh PCM 16-bit không bản quyền & Chế độ yên lặng khi giảng bài
│   └── SaveManager.kt                 # Phân tích danh sách hàng loạt, CSV/TXT, xuất CSV lịch sử & Sao lưu/Khôi phục JSON
├── mini_games/
│   └── MiniGameEngineCanvas.kt        # Engine đồ họa 2D cho Đua vịt, UFO hút học sinh, Vợt bướm, Vòng quay, Tên lửa, Sân khấu, Rương kho báu, Máy gắp thú
└── ui/
    ├── MainViewModel.kt               # Điều phối trạng thái ứng dụng
    ├── screens/
    │   ├── HomeAndClassScreens.kt     # Màn hình khởi động, Chọn lớp, Màn hình chính của lớp
    │   ├── StudentManagerScreen.kt    # Quản lý học sinh, dán hàng loạt, lọc & sắp xếp
    │   ├── MiniGamePlayScreen.kt      # Màn hình chạy mini-game (hỗ trợ cả Ngang TV/Máy chiếu & Dọc điện thoại)
    │   ├── ResultAndCollectionScreens.kt # Màn hình công bố kết quả & Bộ sưu tập 30 Mini-Game
    │   └── HistoryAndSettingsScreens.kt  # Lịch sử, xuất CSV, Cài đặt & Sao lưu/Khôi phục
    └── theme/                         # Bảng màu 2D Cartoon & Font chữ tiếng Việt (Baloo 2 + Be Vietnam Pro)
```

---

## 2. Hướng dẫn thêm Mini-Game mới vào kiến trúc hiện có

Mọi mini-game không bao giờ tự random hay tự ghi lịch sử mà nhận `LockedSelectionTurn` từ `StudentSelectionService`:

1. **Khai báo thông tin Mini-Game**: Thêm một phần tử `MiniGameDescriptor` mới vào `MiniGameCatalog.allGames` trong `models/DomainModels.kt`.
2. **Triển khai hoạt ảnh 2D**:
   - Thêm hàm vẽ `DrawScope.drawYourNewArena(...)` trong `mini_games/MiniGameEngineCanvas.kt`.
   - Sử dụng `lockedTurn.visualParticipants` để hiển thị các nhân vật trên màn hình và `lockedTurn.winners` (hoặc `winnerIndex`) để đảm bảo tại `progress == 1.0f`, nhân vật chiến thắng trên màn hình khớp 100% với kết quả đã khóa từ `StudentSelectionService`.
3. **Không cần sửa lại logic chọn học sinh hay lịch sử**: `MiniGamePlayScreen` và `MainViewModel` sẽ tự động khóa lượt, chạy hoạt ảnh, phát âm thanh và ghi lịch sử đúng 1 lần khi hoàn tất.

---

## 3. Hướng dẫn Build APK & Kiểm thử trên Android

### Kiểm thử tự động (Unit Tests & Robolectric Tests)
Chạy toàn bộ 20 kịch bản kiểm thử bắt buộc và kiểm thử thống kê bằng lệnh:

```bash
gradle :app:testDebugUnitTest
```

### Đóng gói file APK cài đặt trực tiếp
1. Chạy lệnh build APK:
   ```bash
   gradle :app:assembleDebug
   ```
2. File APK đầu ra nằm tại:
   `app/build/outputs/apk/debug/app-debug.apk`
3. Cài đặt trên thiết bị Android:
   - Chép file `app-debug.apk` vào điện thoại hoặc máy tính bảng Android (hoặc tải xuống từ menu Export APK của AI Studio).
   - Mở file APK và chọn **Cài đặt (Install)**. Ứng dụng hoạt động hoàn toàn offline và không yêu cầu bất kỳ quyền nhạy cảm nào.
