package com.example.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import com.example.data.SilencePreferences
import com.example.model.RingerStatus
import com.example.model.SilenceLogEntry

class SilenceManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val prefs = SilencePreferences.getInstance(context)

    companion object {
        private const val TAG = "SilenceManager"

        @Volatile
        private var INSTANCE: SilenceManager? = null

        fun getInstance(context: Context): SilenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SilenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Checks if Do Not Disturb (DND) / Notification Policy access is granted.
     * Required on Android 7+ (API 24+) to change ringer mode out of Silent.
     */
    fun hasDndAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
    }

    /**
     * Intent to open system DND access settings.
     */
    fun getDndSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Gets current ringer status.
     */
    fun getCurrentRingerStatus(): RingerStatus {
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> RingerStatus.NORMAL
            AudioManager.RINGER_MODE_VIBRATE -> RingerStatus.VIBRATE
            AudioManager.RINGER_MODE_SILENT -> RingerStatus.SILENT
            else -> RingerStatus.UNKNOWN
        }
    }

    /**
     * Current ring volume percentage (0 - 100).
     */
    fun getCurrentVolumePercent(): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        if (max <= 0) return 0
        val current = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        return ((current.toFloat() / max.toFloat()) * 100).toInt()
    }

    /**
     * Restores the phone ringer mode to NORMAL and sets preferred volume.
     */
    fun unmutePhone(reason: String = "إلغاء الكتم التلقائي"): Boolean {
        Log.d(TAG, "Unmuting phone: $reason")
        var ringerModeChanged = false

        // 1. Attempt setting ringerMode to NORMAL
        try {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            ringerModeChanged = true
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException changing ringerMode directly (DND permission needed): ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error changing ringerMode: ${e.message}")
        }

        // 2. Calculate and set preferred volume
        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val targetPercent = prefs.restoreVolumePercent
            val targetLevel = ((targetPercent / 100f) * maxVol).toInt().coerceIn(1, maxVol)

            audioManager.setStreamVolume(AudioManager.STREAM_RING, targetLevel, AudioManager.FLAG_SHOW_UI)
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetLevel, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Could not set stream volume directly: ${e.message}")
        }

        // 3. Feedback: vibration
        if (prefs.vibrateOnUnmute) {
            triggerVibration()
        }

        // 4. Feedback: audio chime
        if (prefs.soundAlertOnUnmute) {
            playUnmuteChime()
        }

        // 5. Log session if a timer was active
        try {
            val totalDur = prefs.timerTotalDurationMs
            val durMin = if (totalDur > 0) (totalDur / 60000L).toInt() else prefs.defaultDurationMinutes
            val trigger = prefs.timerTriggerType

            prefs.addHistoryLog(
                SilenceLogEntry(
                    durationMinutes = durMin,
                    reason = if (prefs.timerReason.isNotEmpty()) prefs.timerReason else reason,
                    completedSuccessfully = true,
                    triggerType = if (trigger.isNotEmpty()) trigger else "تلقائي"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error logging history: ${e.message}")
        }

        // 6. Stop service and clear timer state
        stopTimerService()
        return ringerModeChanged || hasDndAccess()
    }

    /**
     * Starts silencing the phone for a specific duration in minutes.
     */
    fun muteAndStartTimer(durationMinutes: Int, reason: String = "كتم يدوي", triggerType: String = "يدوي", toVibrateOnly: Boolean = false) {
        val totalMs = durationMinutes * 60 * 1000L
        val endMs = System.currentTimeMillis() + totalMs

        try {
            if (toVibrateOnly) {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            } else {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not set ringer mode without DND permission: ${e.message}")
        }

        prefs.setTimerActive(endMs, totalMs, reason, triggerType)
        startTimerService(endMs, totalMs, reason)
    }

    /**
     * Starts the countdown timer service when phone goes silent automatically.
     */
    fun startTimerForAutoSilence(durationMinutes: Int = prefs.defaultDurationMinutes) {
        val totalMs = durationMinutes * 60 * 1000L
        val endMs = System.currentTimeMillis() + totalMs
        val isVibrate = audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE
        val reason = if (isVibrate) "وضع الاهتزاز تلقائي" else "وضع الصامت تلقائي"

        prefs.setTimerActive(endMs, totalMs, reason, "تلقائي")
        startTimerService(endMs, totalMs, reason)
    }

    /**
     * Extends the active timer by additional minutes (e.g. +15 min).
     */
    fun extendTimer(addMinutes: Int = 15) {
        adjustTimer(addMinutes)
    }

    /**
     * Adjusts active timer by adding or subtracting minutes (can be positive or negative).
     * If timer expires after adjustment, unmutes the phone.
     */
    fun adjustTimer(minutesDelta: Int) {
        val currentEnd = prefs.timerEndTimeMs
        val now = System.currentTimeMillis()
        val baseTime = if (currentEnd > now) currentEnd else now
        val newEnd = baseTime + (minutesDelta * 60 * 1000L)

        if (newEnd <= now) {
            unmutePhone("اكتمال الوقت بعد التعديل")
        } else {
            val deltaMs = minutesDelta * 60 * 1000L
            val newTotal = (prefs.timerTotalDurationMs + deltaMs).coerceAtLeast(60000L)
            prefs.setTimerActive(newEnd, newTotal, prefs.timerReason, prefs.timerTriggerType)
            startTimerService(newEnd, newTotal, prefs.timerReason)
        }
    }

    /**
     * Cancels active timer without unmuting (user wants to remain silent indefinitely).
     */
    fun cancelTimerWithoutUnmuting() {
        prefs.clearTimer()
        stopTimerService()
    }

    private fun startTimerService(endMs: Long, totalMs: Long, reason: String) {
        val intent = Intent(context, SilenceTimerService::class.java).apply {
            action = SilenceTimerService.ACTION_START
            putExtra(SilenceTimerService.EXTRA_END_TIME, endMs)
            putExtra(SilenceTimerService.EXTRA_TOTAL_DURATION, totalMs)
            putExtra(SilenceTimerService.EXTRA_REASON, reason)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopTimerService() {
        prefs.clearTimer()
        val intent = Intent(context, SilenceTimerService::class.java).apply {
            action = SilenceTimerService.ACTION_STOP
        }
        context.startService(intent)
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 200), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 200), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 150, 100, 200), -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun playUnmuteChime() {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context, alertUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone.play()
        } catch (e: Exception) {
            Log.e(TAG, "Chime failed: ${e.message}")
        }
    }
}
