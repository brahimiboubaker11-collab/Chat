package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.data.SilencePreferences
import com.example.service.SilenceManager
import com.example.service.SilenceMonitorService

/**
 * Triggered by AlarmManager or System Events to keep or revive
 * the background monitor and timer services even if swiped from recents.
 */
class RestartMonitorReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_RESTART_MONITOR = "com.example.ACTION_RESTART_MONITOR"
        private const val TAG = "RestartMonitorReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(TAG, "RestartMonitorReceiver woke up via AlarmManager/System! Action: ${intent?.action}")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NabilSilence:RestartWakeLock"
        )

        try {
            wakeLock?.acquire(5000L) // 5 seconds wake lock

            // 1. Ensure monitor service is running
            SilenceMonitorService.start(context)

            // 2. Check active timer
            val prefs = SilencePreferences.getInstance(context)
            val endTime = prefs.timerEndTimeMs
            val now = System.currentTimeMillis()

            if (endTime > now) {
                // Timer is still active, restore countdown service
                val totalMs = prefs.timerTotalDurationMs
                val reason = prefs.timerReason
                val serviceIntent = Intent(context, com.example.service.SilenceTimerService::class.java).apply {
                    action = com.example.service.SilenceTimerService.ACTION_START
                    putExtra(com.example.service.SilenceTimerService.EXTRA_END_TIME, endTime)
                    putExtra(com.example.service.SilenceTimerService.EXTRA_TOTAL_DURATION, totalMs)
                    putExtra(com.example.service.SilenceTimerService.EXTRA_REASON, reason)
                }
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Could not restart SilenceTimerService: ${e.message}")
                }
            } else if (endTime > 0 && endTime <= now) {
                // Timer expired while app was dead -> unmute now!
                SilenceManager.getInstance(context).unmutePhone("اكتمال الوقت بعد إعادة التشغيل")
            } else {
                // Check if current ringer mode is silent/vibrate right now
                RingerModeReceiver.checkAndTriggerAutoSilence(context)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in RestartMonitorReceiver: ${e.message}")
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (_: Exception) {}
        }
    }
}
