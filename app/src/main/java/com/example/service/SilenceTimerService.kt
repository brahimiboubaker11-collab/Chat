package com.example.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SilencePreferences
import com.example.receiver.RestartMonitorReceiver
import com.example.receiver.SilenceActionReceiver
import java.util.Locale

class SilenceTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "nabil_silence_channel"
        const val ALERT_CHANNEL_ID = "nabil_silence_alert_channel"
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002
        private const val TAG = "SilenceTimerService"

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_TOTAL_DURATION = "extra_total_duration"
        const val EXTRA_REASON = "extra_reason"
    }

    private var endTimeMs: Long = 0L
    private var totalDurationMs: Long = 0L
    private var reason: String = "كتم مؤقت"

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    private lateinit var notificationManager: NotificationManager
    private lateinit var preferences: SilencePreferences

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        preferences = SilencePreferences.getInstance(this)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                endTimeMs = intent.getLongExtra(EXTRA_END_TIME, 0L)
                totalDurationMs = intent.getLongExtra(EXTRA_TOTAL_DURATION, 0L)
                reason = intent.getStringExtra(EXTRA_REASON) ?: "كتم مؤقت"

                if (endTimeMs <= System.currentTimeMillis()) {
                    // Already expired, unmute immediately
                    SilenceManager.getInstance(this).unmutePhone(reason)
                    stopForegroundService()
                    return START_NOT_STICKY
                }

                startForegroundNotification()
                startTicker()
            }
            else -> {
                // If restarted by system, restore state from preferences
                val savedEnd = preferences.timerEndTimeMs
                if (savedEnd > System.currentTimeMillis()) {
                    endTimeMs = savedEnd
                    totalDurationMs = preferences.timerTotalDurationMs
                    reason = preferences.timerReason
                    startForegroundNotification()
                    startTicker()
                } else if (savedEnd > 0) {
                    SilenceManager.getInstance(this).unmutePhone("اكتمال الوقت عند إعادة التشغيل")
                    stopForegroundService()
                    return START_NOT_STICKY
                } else {
                    stopForegroundService()
                    return START_NOT_STICKY
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        try {
            val notification = buildOngoingNotification(getFormattedRemainingTime())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in startForegroundNotification: ${e.message}")
        }
    }

    private fun startTicker() {
        updateRunnable?.let { handler.removeCallbacks(it) }

        updateRunnable = object : Runnable {
            override fun run() {
                val remainingMs = endTimeMs - System.currentTimeMillis()
                if (remainingMs <= 0) {
                    // Time is up! Trigger un-silence
                    SilenceManager.getInstance(this@SilenceTimerService).unmutePhone(reason)
                    showCompletionNotification()
                    stopForegroundService()
                } else {
                    // Update notification text
                    try {
                        val notification = buildOngoingNotification(getFormattedRemainingTime())
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating notification: ${e.message}")
                    }
                    handler.postDelayed(this, 1000L)
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun getFormattedRemainingTime(): String {
        val remainingMs = (endTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val totalSecs = remainingMs / 1000L
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    private fun buildOngoingNotification(timeRemainingFormatted: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Unmute Now
        val unmuteIntent = Intent(this, SilenceActionReceiver::class.java).apply {
            action = SilenceActionReceiver.ACTION_UNMUTE_NOW
        }
        val unmutePendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            unmuteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Extend +15 min
        val extendIntent = Intent(this, SilenceActionReceiver::class.java).apply {
            action = SilenceActionReceiver.ACTION_EXTEND_TIMER
        }
        val extendPendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            extendIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Cancel timer without unmuting
        val cancelIntent = Intent(this, SilenceActionReceiver::class.java).apply {
            action = SilenceActionReceiver.ACTION_CANCEL_TIMER
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this,
            3,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = "سيتم إعادة تشغيل الصوت تلقائياً بعد: $timeRemainingFormatted ($reason)"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("نبيل سيلونس - الهاتف صامت مؤقتاً")
            .setContentText(contentText)
            .setSubText(timeRemainingFormatted)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "🔊 إلغاء الكتم الآن", unmutePendingIntent)
            .addAction(0, "➕ +15 د", extendPendingIntent)
            .addAction(0, "⏹️ إيقاف", cancelPendingIntent)
            .build()
    }

    private fun showCompletionNotification() {
        try {
            val openAppIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                this,
                4,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("تم إلغاء كتم الهاتف بنجاح 🔊")
                .setContentText("أعاد نبيل سيلونس تشغيل الرنين إلى وضعه الطبيعي بنجاح.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent)
                .build()

            notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing completion notification: ${e.message}")
        }
    }

    private fun stopForegroundService() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timerChannel = NotificationChannel(
                CHANNEL_ID,
                "مؤقت كتم الصوت (نبيل سيلونس)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار مستمر يوضح الوقت المتبقي لإلغاء الكتم التلقائي"
                setShowBadge(true)
            }

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "تنبيهات إلغاء الكتم",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعار عند اكتمال المؤقت وإعادة تشغيل الصوت"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(timerChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "onTaskRemoved: App swiped. Rescheduling recovery via RestartMonitorReceiver broadcast.")
        val savedEnd = preferences.timerEndTimeMs
        if (savedEnd > System.currentTimeMillis()) {
            try {
                val restartIntent = Intent(applicationContext, RestartMonitorReceiver::class.java).apply {
                    action = RestartMonitorReceiver.ACTION_RESTART_MONITOR
                }
                val restartPendingIntent = PendingIntent.getBroadcast(
                    applicationContext,
                    301,
                    restartIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 1000,
                        restartPendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 1000,
                        restartPendingIntent
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling onTaskRemoved in SilenceTimerService: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
