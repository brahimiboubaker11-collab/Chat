package com.example.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SilencePreferences
import com.example.receiver.RestartMonitorReceiver
import com.example.receiver.RingerModeReceiver

class SilenceMonitorService : Service() {

    companion object {
        const val MONITOR_CHANNEL_ID = "nabil_silence_monitor_channel"
        const val MONITOR_NOTIFICATION_ID = 2001
        private const val TAG = "SilenceMonitorService"

        const val ACTION_START_MONITOR = "com.example.service.ACTION_START_MONITOR"
        const val ACTION_STOP_MONITOR = "com.example.service.ACTION_STOP_MONITOR"

        fun start(context: Context) {
            val intent = Intent(context, SilenceMonitorService::class.java).apply {
                action = ACTION_START_MONITOR
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed starting SilenceMonitorService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SilenceMonitorService::class.java).apply {
                action = ACTION_STOP_MONITOR
            }
            context.startService(intent)
        }
    }

    private val ringerReceiver = RingerModeReceiver()
    private var contentObserverSystem: ContentObserver? = null
    private var contentObserverGlobal: ContentObserver? = null
    private var isReceiverRegistered = false

    private val handler = Handler(Looper.getMainLooper())
    private var periodicCheckRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerMonitoring()
        startPeriodicCheckLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITOR) {
            stopForegroundService()
            return START_NOT_STICKY
        }

        try {
            val notification = buildMonitorNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                startForeground(MONITOR_NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(MONITOR_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground notification in SilenceMonitorService: ${e.message}")
        }

        // Trigger an immediate check on startup
        RingerModeReceiver.checkAndTriggerAutoSilence(this)

        return START_STICKY
    }

    /**
     * Periodic watchdog loop: checks ringer state every 2 seconds.
     * This provides guaranteed detection even if OEM systems (MIUI, OneUI, EMUI)
     * delay or swallow system broadcast intents.
     */
    private fun startPeriodicCheckLoop() {
        periodicCheckRunnable?.let { handler.removeCallbacks(it) }

        periodicCheckRunnable = object : Runnable {
            override fun run() {
                try {
                    RingerModeReceiver.checkAndTriggerAutoSilence(this@SilenceMonitorService)
                } catch (e: Exception) {
                    Log.e(TAG, "Periodic check error: ${e.message}")
                }
                // Run every 2 seconds
                handler.postDelayed(this, 2000L)
            }
        }
        handler.post(periodicCheckRunnable!!)
    }

    private fun registerMonitoring() {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter().apply {
                    addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                    addAction("android.media.RINGER_MODE_CHANGED")
                    addAction("android.media.VOLUME_CHANGED_ACTION")
                    addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(ringerReceiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    registerReceiver(ringerReceiver, filter)
                }
                isReceiverRegistered = true
                Log.d(TAG, "Registered ringer receiver in SilenceMonitorService")
            } catch (e: Exception) {
                Log.e(TAG, "Error registering receiver: ${e.message}")
            }
        }

        // Register ContentObserver for System Settings (volume, ringer)
        if (contentObserverSystem == null) {
            try {
                contentObserverSystem = object : ContentObserver(handler) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        super.onChange(selfChange, uri)
                        RingerModeReceiver.checkAndTriggerAutoSilence(this@SilenceMonitorService)
                    }
                }
                contentResolver.registerContentObserver(
                    Settings.System.CONTENT_URI,
                    true,
                    contentObserverSystem!!
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error registering System ContentObserver: ${e.message}")
            }
        }

        // Register ContentObserver for Global Settings (e.g., zen_mode / DND)
        if (contentObserverGlobal == null) {
            try {
                contentObserverGlobal = object : ContentObserver(handler) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        super.onChange(selfChange, uri)
                        RingerModeReceiver.checkAndTriggerAutoSilence(this@SilenceMonitorService)
                    }
                }
                contentResolver.registerContentObserver(
                    Settings.Global.CONTENT_URI,
                    true,
                    contentObserverGlobal!!
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error registering Global ContentObserver: ${e.message}")
            }
        }
    }

    private fun buildMonitorNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            200,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prefs = SilencePreferences.getInstance(this)
        val durationMins = prefs.defaultDurationMinutes

        return NotificationCompat.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("نبيل سيلونس - الحماية التلقائية نشطة 🛡️")
            .setContentText("يراقب كتم الهاتف وسيرجع للوضع العام بعد $durationMins دقيقة عند كتمه")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MONITOR_CHANNEL_ID,
                "خدمة مراقبة الصوت في الخلفية",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار خفيف لضمان استمرار نبيل سيلونس بالعمل عند إغلاق التطبيق"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "onTaskRemoved: App swiped from recents. Scheduling restart via AlarmManager Broadcast!")

        // Reschedule monitor service via RestartMonitorReceiver (BroadcastReceiver)
        // This is 100% compliant with Android 12+ background start limits
        try {
            val restartIntent = Intent(applicationContext, RestartMonitorReceiver::class.java).apply {
                action = RestartMonitorReceiver.ACTION_RESTART_MONITOR
            }
            val restartPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                202,
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
            Log.e(TAG, "Error rescheduling monitor service in onTaskRemoved: ${e.message}")
        }
    }

    private fun stopForegroundService() {
        periodicCheckRunnable?.let { handler.removeCallbacks(it) }
        periodicCheckRunnable = null

        try {
            if (isReceiverRegistered) {
                unregisterReceiver(ringerReceiver)
                isReceiverRegistered = false
            }
            contentObserverSystem?.let {
                contentResolver.unregisterContentObserver(it)
                contentObserverSystem = null
            }
            contentObserverGlobal?.let {
                contentResolver.unregisterContentObserver(it)
                contentObserverGlobal = null
            }
        } catch (_: Exception) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopForegroundService()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
