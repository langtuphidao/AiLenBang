package com.example.repositories

import android.content.Context
import android.content.SharedPreferences
import com.example.models.AppSettings
import com.example.models.SelectionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SettingsRepository lưu cấu hình ứng dụng bền vững vào SharedPreferences cục bộ.
 */
class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ai_len_bang_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadFromPrefs())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _activeClassroomId = MutableStateFlow(prefs.getString("active_classroom_id", null))
    val activeClassroomId: StateFlow<String?> = _activeClassroomId.asStateFlow()

    fun setActiveClassroomId(id: String?) {
        prefs.edit().putString("active_classroom_id", id).apply()
        _activeClassroomId.value = id
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        saveToPrefs(updated)
        _settings.value = updated
    }

    fun selectMiniGame(miniGameId: String) {
        updateSettings { current ->
            val updatedRecent = (listOf(miniGameId) + current.recentMiniGameIds)
                .distinct()
                .take(8)
            current.copy(
                selectedMiniGameId = miniGameId,
                recentMiniGameIds = updatedRecent
            )
        }
    }

    fun resetToDefaults() {
        val defaults = AppSettings()
        saveToPrefs(defaults)
        _settings.value = defaults
    }

    private fun loadFromPrefs(): AppSettings {
        return runCatching {
            val modeName = prefs.getString("selection_mode", SelectionMode.NO_REPEAT_IN_CYCLE.name)
                ?: SelectionMode.NO_REPEAT_IN_CYCLE.name
            val mode = runCatching { SelectionMode.valueOf(modeName) }
                .getOrDefault(SelectionMode.NO_REPEAT_IN_CYCLE)
            val recentRaw = prefs.getString(
                "recent_minigames",
                "duck_race,ufo_abduction,butterfly_net,lucky_wheel,rocket_launch"
            ) ?: "duck_race"
            val recentList = recentRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            AppSettings(
                language = prefs.getString("language", "vi") ?: "vi",
                darkMode = prefs.getBoolean("dark_mode", false),
                presentationMode = prefs.getBoolean("presentation_mode", false),
                highQualityEffects = prefs.getBoolean("high_quality_effects", true),
                bgmEnabled = prefs.getBoolean("bgm_enabled", false),
                sfxEnabled = prefs.getBoolean("sfx_enabled", true),
                volume = prefs.getFloat("volume", 0.8f).coerceIn(0f, 1f),
                quietTeachingMode = prefs.getBoolean("quiet_teaching_mode", false),
                selectionMode = mode,
                pickCount = prefs.getInt("pick_count", 1).coerceIn(1, 10),
                animationDurationSeconds = prefs.getInt("anim_duration_sec", 5).coerceIn(3, 12),
                autoStartMiniGame = prefs.getBoolean("auto_start_minigame", false),
                showHistoryOnClassScreen = prefs.getBoolean("show_history_on_class", true),
                confirmBeforeResetCycle = prefs.getBoolean("confirm_reset_cycle", true),
                selectedMiniGameId = prefs.getString("selected_minigame_id", "duck_race") ?: "duck_race",
                recentMiniGameIds = recentList.ifEmpty { listOf("duck_race") }
            )
        }.getOrDefault(AppSettings())
    }

    private fun saveToPrefs(s: AppSettings) {
        prefs.edit()
            .putString("language", s.language)
            .putBoolean("dark_mode", s.darkMode)
            .putBoolean("presentation_mode", s.presentationMode)
            .putBoolean("high_quality_effects", s.highQualityEffects)
            .putBoolean("bgm_enabled", s.bgmEnabled)
            .putBoolean("sfx_enabled", s.sfxEnabled)
            .putFloat("volume", s.volume)
            .putBoolean("quiet_teaching_mode", s.quietTeachingMode)
            .putString("selection_mode", s.selectionMode.name)
            .putInt("pick_count", s.pickCount)
            .putInt("anim_duration_sec", s.animationDurationSeconds)
            .putBoolean("auto_start_minigame", s.autoStartMiniGame)
            .putBoolean("show_history_on_class", s.showHistoryOnClassScreen)
            .putBoolean("confirm_reset_cycle", s.confirmBeforeResetCycle)
            .putString("selected_minigame_id", s.selectedMiniGameId)
            .putString("recent_minigames", s.recentMiniGameIds.joinToString(","))
            .apply()
    }
}
