package com.cronwork.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cronwork.core.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val dailyTarget = intPreferencesKey("daily_target_minutes")
        val workDays = stringPreferencesKey("work_days")
        val pauseEnabled = booleanPreferencesKey("pause_enabled")
        val pauseMinutes = intPreferencesKey("pause_minutes")
        val pauseThreshold = intPreferencesKey("pause_threshold_minutes")
        val forgotHours = intPreferencesKey("forgot_reminder_hours")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[Keys.dailyTarget] = s.dailyTargetMinutes
            prefs[Keys.workDays] = s.workDays.map { it.value }.sorted().joinToString(",")
            prefs[Keys.pauseEnabled] = s.pauseEnabled
            prefs[Keys.pauseMinutes] = s.pauseMinutes
            prefs[Keys.pauseThreshold] = s.pauseThresholdMinutes
            prefs[Keys.forgotHours] = s.forgotReminderHours
        }
    }

    private fun Preferences.toSettings(): Settings {
        val d = Settings()
        return Settings(
            dailyTargetMinutes = this[Keys.dailyTarget] ?: d.dailyTargetMinutes,
            workDays = this[Keys.workDays]?.let { str ->
                str.split(",").mapNotNull { it.toIntOrNull()?.let(DayOfWeek::of) }.toSet()
            } ?: d.workDays,
            pauseEnabled = this[Keys.pauseEnabled] ?: d.pauseEnabled,
            pauseMinutes = this[Keys.pauseMinutes] ?: d.pauseMinutes,
            pauseThresholdMinutes = this[Keys.pauseThreshold] ?: d.pauseThresholdMinutes,
            forgotReminderHours = this[Keys.forgotHours] ?: d.forgotReminderHours,
        )
    }
}
