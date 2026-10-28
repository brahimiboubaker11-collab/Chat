package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.example.data.SilencePreferences
import com.example.service.SilenceManager

class RingerModeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RingerModeReceiver"

        /**
         * Checks the current ringer mode and volume, and automatically starts
         * the silence countdown timer if the phone entered silent or vibrate mode.
         */
        fun checkAndTriggerAutoSilence(context: Context) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val preferences = SilencePreferences.getInstance(context)
            val silenceManager = SilenceManager.getInstance(context)

            val currentMode = audioManager.ringerMode
            val ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)

            Log.d(TAG, "checkAndTriggerAutoSilence: mode=$currentMode, volume=$ringVolume, autoDetect=${preferences.isAutoDetectionEnabled}")

            val isSilent = currentMode == AudioManager.RINGER_MODE_SILENT ||
                    (currentMode == AudioManager.RINGER_MODE_NORMAL && ringVolume == 0)
            val isVibrate = currentMode == AudioManager.RINGER_MODE_VIBRATE

            if (isSilent || (isVibrate && preferences.includeVibrateMode)) {
                if (preferences.isAutoDetectionEnabled) {
                    val activeEnd = preferences.timerEndTimeMs
                    val now = System.currentTimeMillis()

                    // Only trigger if no active timer is currently running
                    if (activeEnd <= now) {
                        val reason = if (isVibrate) "وضع الاهتزاز تلقائي" else "وضع الصامت تلقائي"
                        Log.i(TAG, "Triggering auto-silence timer for ${preferences.defaultDurationMinutes} min. Reason=$reason")
                        silenceManager.startTimerForAutoSilence(preferences.defaultDurationMinutes)
                    }
                }
            } else if (currentMode == AudioManager.RINGER_MODE_NORMAL && ringVolume > 0) {
                // User unmuted the device manually to normal mode
                val activeEnd = preferences.timerEndTimeMs
                if (activeEnd > System.currentTimeMillis()) {
                    Log.i(TAG, "Phone unmuted manually. Cancelling timer.")
                    silenceManager.cancelTimerWithoutUnmuting()
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive: action=$action")

        if (action == AudioManager.RINGER_MODE_CHANGED_ACTION ||
            action == "android.media.VOLUME_CHANGED_ACTION" ||
            action == "android.media.RINGER_MODE_CHANGED"
        ) {
            checkAndTriggerAutoSilence(context)
        } else if (action == Intent.ACTION_BOOT_COMPLETED) {
            val preferences = SilencePreferences.getInstance(context)
            val activeEnd = preferences.timerEndTimeMs
            if (activeEnd > System.currentTimeMillis()) {
                val remaining = activeEnd - System.currentTimeMillis()
                val silenceManager = SilenceManager.getInstance(context)
                silenceManager.startTimerForAutoSilence((remaining / 60000L).toInt().coerceAtLeast(1))
            }
        }
    }
}
