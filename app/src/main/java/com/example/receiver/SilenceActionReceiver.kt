package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.service.SilenceManager

class SilenceActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_UNMUTE_NOW = "com.example.ACTION_UNMUTE_NOW"
        const val ACTION_EXTEND_TIMER = "com.example.ACTION_EXTEND_TIMER"
        const val ACTION_CANCEL_TIMER = "com.example.ACTION_CANCEL_TIMER"
        private const val TAG = "SilenceActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val manager = SilenceManager.getInstance(context)
        when (intent.action) {
            ACTION_UNMUTE_NOW -> {
                Log.d(TAG, "Unmute now requested from notification")
                manager.unmutePhone("إلغاء يدوي من الإشعار")
            }
            ACTION_EXTEND_TIMER -> {
                Log.d(TAG, "Extend timer requested from notification")
                manager.extendTimer(15)
            }
            ACTION_CANCEL_TIMER -> {
                Log.d(TAG, "Cancel timer requested from notification")
                manager.cancelTimerWithoutUnmuting()
            }
        }
    }
}
