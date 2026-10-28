package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.SilenceLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class SilencePreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nabil_silence_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTO_DETECT = "auto_detect_enabled"
        private const val KEY_DEFAULT_DURATION = "default_duration_min"
        private const val KEY_INCLUDE_VIBRATE = "include_vibrate_mode"
        private const val KEY_RESTORE_VOLUME = "restore_volume_percent"
        private const val KEY_SOUND_ALERT = "sound_alert_on_unmute"
        private const val KEY_VIBRATE_ALERT = "vibrate_alert_on_unmute"
        private const val KEY_TIMER_END = "active_timer_end_ms"
        private const val KEY_TIMER_TOTAL = "active_timer_total_ms"
        private const val KEY_TIMER_REASON = "active_timer_reason"
        private const val KEY_TIMER_TRIGGER = "active_timer_trigger"
        private const val KEY_HISTORY = "history_logs_json"

        @Volatile
        private var INSTANCE: SilencePreferences? = null

        fun getInstance(context: Context): SilencePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SilencePreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _autoDetectFlow = MutableStateFlow(isAutoDetectionEnabled)
    val autoDetectFlow: StateFlow<Boolean> = _autoDetectFlow.asStateFlow()

    private val _defaultDurationFlow = MutableStateFlow(defaultDurationMinutes)
    val defaultDurationFlow: StateFlow<Int> = _defaultDurationFlow.asStateFlow()

    private val _timerEndTimeFlow = MutableStateFlow(timerEndTimeMs)
    val timerEndTimeFlow: StateFlow<Long> = _timerEndTimeFlow.asStateFlow()

    private val _historyFlow = MutableStateFlow(getHistoryLogs())
    val historyFlow: StateFlow<List<SilenceLogEntry>> = _historyFlow.asStateFlow()

    var isAutoDetectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_DETECT, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_DETECT, value).apply()
            _autoDetectFlow.value = value
        }

    var defaultDurationMinutes: Int
        get() = prefs.getInt(KEY_DEFAULT_DURATION, 30)
        set(value) {
            prefs.edit().putInt(KEY_DEFAULT_DURATION, value).apply()
            _defaultDurationFlow.value = value
        }

    var includeVibrateMode: Boolean
        get() = prefs.getBoolean(KEY_INCLUDE_VIBRATE, true)
        set(value) = prefs.edit().putBoolean(KEY_INCLUDE_VIBRATE, value).apply()

    var restoreVolumePercent: Int
        get() = prefs.getInt(KEY_RESTORE_VOLUME, 75)
        set(value) = prefs.edit().putInt(KEY_RESTORE_VOLUME, value.coerceIn(10, 100)).apply()

    var soundAlertOnUnmute: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ALERT, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ALERT, value).apply()

    var vibrateOnUnmute: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE_ALERT, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE_ALERT, value).apply()

    var timerEndTimeMs: Long
        get() = prefs.getLong(KEY_TIMER_END, 0L)
        set(value) {
            prefs.edit().putLong(KEY_TIMER_END, value).apply()
            _timerEndTimeFlow.value = value
        }

    var timerTotalDurationMs: Long
        get() = prefs.getLong(KEY_TIMER_TOTAL, 0L)
        set(value) = prefs.edit().putLong(KEY_TIMER_TOTAL, value).apply()

    var timerReason: String
        get() = prefs.getString(KEY_TIMER_REASON, "كتم مؤقت") ?: "كتم مؤقت"
        set(value) = prefs.edit().putString(KEY_TIMER_REASON, value).apply()

    var timerTriggerType: String
        get() = prefs.getString(KEY_TIMER_TRIGGER, "يدوي") ?: "يدوي"
        set(value) = prefs.edit().putString(KEY_TIMER_TRIGGER, value).apply()

    fun setTimerActive(endTimeMs: Long, totalDurationMs: Long, reason: String, triggerType: String) {
        prefs.edit()
            .putLong(KEY_TIMER_END, endTimeMs)
            .putLong(KEY_TIMER_TOTAL, totalDurationMs)
            .putString(KEY_TIMER_REASON, reason)
            .putString(KEY_TIMER_TRIGGER, triggerType)
            .apply()
        _timerEndTimeFlow.value = endTimeMs
    }

    fun clearTimer() {
        prefs.edit()
            .putLong(KEY_TIMER_END, 0L)
            .putLong(KEY_TIMER_TOTAL, 0L)
            .putString(KEY_TIMER_REASON, "")
            .putString(KEY_TIMER_TRIGGER, "")
            .apply()
        _timerEndTimeFlow.value = 0L
    }

    fun addHistoryLog(entry: SilenceLogEntry) {
        val current = getHistoryLogs().toMutableList()
        current.add(0, entry)
        // Keep last 30 logs
        val trimmed = if (current.size > 30) current.subList(0, 30) else current

        val jsonArray = JSONArray()
        for (item in trimmed) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("durationMinutes", item.durationMinutes)
                put("reason", item.reason)
                put("completedSuccessfully", item.completedSuccessfully)
                put("triggerType", item.triggerType)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
        _historyFlow.value = trimmed
    }

    fun getHistoryLogs(): List<SilenceLogEntry> {
        val jsonStr = prefs.getString(KEY_HISTORY, null) ?: return defaultInitialLogs()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<SilenceLogEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SilenceLogEntry(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        durationMinutes = obj.optInt("durationMinutes", 30),
                        reason = obj.optString("reason", "كتم مؤقت"),
                        completedSuccessfully = obj.optBoolean("completedSuccessfully", true),
                        triggerType = obj.optString("triggerType", "تلقائي")
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultInitialLogs()
        }
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
        _historyFlow.value = emptyList()
    }

    private fun defaultInitialLogs(): List<SilenceLogEntry> {
        val now = System.currentTimeMillis()
        return listOf(
            SilenceLogEntry(
                id = 1L,
                timestamp = now - 3600000L * 4,
                durationMinutes = 30,
                reason = "وقت الصلاة",
                completedSuccessfully = true,
                triggerType = "تلقائي"
            ),
            SilenceLogEntry(
                id = 2L,
                timestamp = now - 3600000L * 14,
                durationMinutes = 60,
                reason = "اجتماع عمل",
                completedSuccessfully = true,
                triggerType = "يدوي"
            )
        )
    }
}
