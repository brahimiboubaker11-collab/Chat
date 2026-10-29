package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SilencePreferences
import com.example.model.PresetOption
import com.example.model.RingerStatus
import com.example.model.SilenceLogEntry
import com.example.service.SilenceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SilenceViewModel(application: Application) : AndroidViewModel(application) {

    private val silenceManager = SilenceManager.getInstance(application)
    private val preferences = SilencePreferences.getInstance(application)

    private val _ringerStatus = MutableStateFlow(silenceManager.getCurrentRingerStatus())
    val ringerStatus: StateFlow<RingerStatus> = _ringerStatus.asStateFlow()

    private val _volumePercent = MutableStateFlow(silenceManager.getCurrentVolumePercent())
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    private val _hasDndPermission = MutableStateFlow(silenceManager.hasDndAccess())
    val hasDndPermission: StateFlow<Boolean> = _hasDndPermission.asStateFlow()

    val isAutoDetectEnabled = preferences.autoDetectFlow
    val defaultDurationMinutes = preferences.defaultDurationFlow
    val historyLogs = preferences.historyFlow

    private val _isIgnoringBattery = MutableStateFlow(silenceManager.isIgnoringBatteryOptimizations())
    val isIgnoringBattery: StateFlow<Boolean> = _isIgnoringBattery.asStateFlow()

    private val _restoreVolumePercent = MutableStateFlow(preferences.restoreVolumePercent)
    val restoreVolumePercent: StateFlow<Int> = _restoreVolumePercent.asStateFlow()

    private val _soundAlertOnUnmute = MutableStateFlow(preferences.soundAlertOnUnmute)
    val soundAlertOnUnmute: StateFlow<Boolean> = _soundAlertOnUnmute.asStateFlow()

    private val _vibrateOnUnmute = MutableStateFlow(preferences.vibrateOnUnmute)
    val vibrateOnUnmute: StateFlow<Boolean> = _vibrateOnUnmute.asStateFlow()

    private val _includeVibrateMode = MutableStateFlow(preferences.includeVibrateMode)
    val includeVibrateMode: StateFlow<Boolean> = _includeVibrateMode.asStateFlow()

    private val _timerEndTime = MutableStateFlow(preferences.timerEndTimeMs)
    val timerEndTime: StateFlow<Long> = _timerEndTime.asStateFlow()

    private val _timerTotalDuration = MutableStateFlow(preferences.timerTotalDurationMs)
    val timerTotalDuration: StateFlow<Long> = _timerTotalDuration.asStateFlow()

    private val _timerReason = MutableStateFlow(preferences.timerReason)
    val timerReason: StateFlow<String> = _timerReason.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _messageEvent = MutableStateFlow<String?>(null)
    val messageEvent: StateFlow<String?> = _messageEvent.asStateFlow()

    private var countdownJob: Job? = null

    val presets = listOf(
        PresetOption("prayer", "وقت الصلاة", 30, "كتم مؤقت لأداء الصلاة بتركيز", "🕌"),
        PresetOption("meeting", "اجتماع عمل", 45, "للاجتماعات والمقابلات الهامة", "💼"),
        PresetOption("study", "دراسة / محاضرة", 60, "جلسة تركيز ومذاكرة مكثفة", "📚"),
        PresetOption("movie", "سينما / حدث", 120, "فيلم سينمائي أو حدث طويل", "🎬"),
        PresetOption("nap", "قيلولة / راحة", 90, "راحة هادئة بدون إزعاج الرنين", "☕"),
        PresetOption("sleep", "نوم عميق", 480, "8 ساعات نوم هانئ", "🌙")
    )

    init {
        refreshState()
        startTicker()
    }

    fun refreshState() {
        _ringerStatus.value = silenceManager.getCurrentRingerStatus()
        _volumePercent.value = silenceManager.getCurrentVolumePercent()
        _hasDndPermission.value = silenceManager.hasDndAccess()
        _isIgnoringBattery.value = silenceManager.isIgnoringBatteryOptimizations()
        _timerEndTime.value = preferences.timerEndTimeMs
        _timerTotalDuration.value = preferences.timerTotalDurationMs
        _timerReason.value = preferences.timerReason
    }

    private fun startTicker() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                val end = preferences.timerEndTimeMs
                val now = System.currentTimeMillis()
                if (end > now) {
                    _remainingSeconds.value = ((end - now) / 1000L).coerceAtLeast(0L)
                    _timerEndTime.value = end
                    _timerTotalDuration.value = preferences.timerTotalDurationMs
                    _timerReason.value = preferences.timerReason
                } else {
                    _remainingSeconds.value = 0L
                    _timerEndTime.value = 0L
                }

                val currentStatus = silenceManager.getCurrentRingerStatus()
                val currentVol = silenceManager.getCurrentVolumePercent()
                _ringerStatus.value = currentStatus
                _volumePercent.value = currentVol
                _hasDndPermission.value = silenceManager.hasDndAccess()

                // Real-time automatic detection of silent or vibrate mode
                val isMuted = currentStatus == RingerStatus.SILENT ||
                        (currentStatus == RingerStatus.VIBRATE && preferences.includeVibrateMode) ||
                        (currentStatus == RingerStatus.NORMAL && currentVol == 0)

                if (isMuted && end <= now && preferences.isAutoDetectionEnabled) {
                    silenceManager.startTimerForAutoSilence(preferences.defaultDurationMinutes)
                    val durMinutes = preferences.defaultDurationMinutes
                    val statusLabel = if (currentStatus == RingerStatus.VIBRATE) "وضع الاهتزاز" else "وضع الصامت"
                    _messageEvent.value = "تم اكتشاف $statusLabel! بدأ نبيل سيلونس المؤقت تلقائياً ($durMinutes دقيقة)"
                } else if (!isMuted && end > now) {
                    // If user manually restored normal ring volume
                    val elapsed = now - (end - preferences.timerTotalDurationMs)
                    if (elapsed > 3000L) {
                        silenceManager.cancelTimerWithoutUnmuting()
                        _messageEvent.value = "تمت استعادة الوضع العام يدوياً"
                    }
                }

                delay(1000L)
            }
        }
    }

    fun unmuteNow() {
        val success = silenceManager.unmutePhone("إلغاء يدوي من التطبيق")
        if (success) {
            _messageEvent.value = "تم إلغاء كتم الصوت وإعادة تشغيل الرنين بنجاح 🔊"
        } else {
            _messageEvent.value = "يرجى التحقق من إذن عدم الإزعاج لإلغاء الكتم"
        }
        refreshState()
    }

    fun muteForPreset(preset: PresetOption) {
        silenceManager.muteAndStartTimer(
            durationMinutes = preset.minutes,
            reason = preset.title,
            triggerType = "يدوي"
        )
        _messageEvent.value = "تم تفعيل وضع الصامت لمدة ${preset.minutes} دقيقة (${preset.title})"
        refreshState()
    }

    fun muteCustom(minutes: Int, reason: String = "مدة مخصصة") {
        silenceManager.muteAndStartTimer(
            durationMinutes = minutes,
            reason = reason,
            triggerType = "يدوي"
        )
        _messageEvent.value = "تم تفعيل وضع الصامت لمدة $minutes دقيقة"
        refreshState()
    }

    fun testQuickTenSeconds() {
        // Sets a quick 10-second mute test so the user can verify the countdown and auto-unmute
        val totalMs = 10 * 1000L
        val endMs = System.currentTimeMillis() + totalMs
        preferences.setTimerActive(endMs, totalMs, "تجربة سريعة (10 ثوانٍ)", "يدوي")
        try {
            val audioManager = getApplication<Application>().getSystemService(Application.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.ringerMode = android.media.AudioManager.RINGER_MODE_SILENT
        } catch (_: Exception) {}
        val intent = android.content.Intent(getApplication(), com.example.service.SilenceTimerService::class.java).apply {
            action = com.example.service.SilenceTimerService.ACTION_START
            putExtra(com.example.service.SilenceTimerService.EXTRA_END_TIME, endMs)
            putExtra(com.example.service.SilenceTimerService.EXTRA_TOTAL_DURATION, totalMs)
            putExtra(com.example.service.SilenceTimerService.EXTRA_REASON, "تجربة سريعة (10 ثوانٍ)")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
        _messageEvent.value = "بدأت التجربة السريعة! سيتم إلغاء الكتم تلقائياً بعد 10 ثوانٍ"
        refreshState()
    }

    fun extendTimer(minutes: Int = 15) {
        silenceManager.extendTimer(minutes)
        _messageEvent.value = "تمت إضافة $minutes دقيقة إضافية إلى المؤقت"
        refreshState()
    }

    fun adjustActiveTimer(minutesDelta: Int) {
        silenceManager.adjustTimer(minutesDelta)
        val text = if (minutesDelta > 0) "تمت إضافة $minutesDelta دقيقة إلى المؤقت" else "تم إنقاص ${-minutesDelta} دقائق من المؤقت"
        _messageEvent.value = text
        refreshState()
    }

    fun cancelTimer() {
        silenceManager.cancelTimerWithoutUnmuting()
        _messageEvent.value = "تم إلغاء المؤقت (سيبقى الهاتف في وضعه الحالي)"
        refreshState()
    }

    fun updateAutoDetect(enabled: Boolean) {
        preferences.isAutoDetectionEnabled = enabled
        _messageEvent.value = if (enabled) "تم تفعيل المراقبة التلقائية للصامت" else "تم إيقاف المراقبة التلقائية"
    }

    fun updateDefaultDuration(minutes: Int) {
        preferences.defaultDurationMinutes = minutes
    }

    fun updateRestoreVolume(percent: Int) {
        preferences.restoreVolumePercent = percent
        _restoreVolumePercent.value = percent
    }

    fun updateSoundAlert(enabled: Boolean) {
        preferences.soundAlertOnUnmute = enabled
        _soundAlertOnUnmute.value = enabled
    }

    fun updateVibrateAlert(enabled: Boolean) {
        preferences.vibrateOnUnmute = enabled
        _vibrateOnUnmute.value = enabled
    }

    fun updateIncludeVibrate(enabled: Boolean) {
        preferences.includeVibrateMode = enabled
        _includeVibrateMode.value = enabled
    }

    fun clearHistory() {
        preferences.clearHistory()
        _messageEvent.value = "تم مسح سجل العمليات"
    }

    fun clearMessage() {
        _messageEvent.value = null
    }

    fun getDndSettingsIntent() = silenceManager.getDndSettingsIntent()
    fun getBatteryOptimizationIntent() = silenceManager.getBatteryOptimizationIntent()
}
