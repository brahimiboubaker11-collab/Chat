package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.service.SilenceManager

class AlarmUnmuteReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_UNMUTE = "com.example.ACTION_ALARM_UNMUTE"
        private const val TAG = "AlarmUnmuteReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "AlarmManager triggered exact unmute! Unmuting device now.")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NabilSilence:AlarmUnmuteWakeLock"
        )

        try {
            wakeLock.acquire(10000L) // 10 seconds max wake lock

            val reason = intent.getStringExtra("reason") ?: "اكتمال الوقت المحدد"
            val silenceManager = SilenceManager.getInstance(context)
            silenceManager.unmutePhone(reason)

        } catch (e: Exception) {
            Log.e(TAG, "Error in AlarmUnmuteReceiver: ${e.message}")
        } finally {
            try {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            } catch (_: Exception) {}
        }
    }
}
