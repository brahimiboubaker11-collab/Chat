package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.example.receiver.RingerModeReceiver
import com.example.service.SilenceTimerService

class NabilSilenceApp : Application() {

    companion object {
        private const val TAG = "NabilSilenceApp"
    }

    private val ringerReceiver = RingerModeReceiver()
    private var contentObserver: ContentObserver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        registerDynamicRingerReceiver()
        registerAudioContentObserver()
    }

    private fun registerDynamicRingerReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction("android.media.RINGER_MODE_CHANGED")
                addAction("android.media.VOLUME_CHANGED_ACTION")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(ringerReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(ringerReceiver, filter)
            }
            Log.d(TAG, "Successfully registered dynamic RingerModeReceiver")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering dynamic receiver: ${e.message}")
        }
    }

    private fun registerAudioContentObserver() {
        try {
            val handler = Handler(Looper.getMainLooper())
            contentObserver = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    Log.d(TAG, "Audio or system settings changed at URI: $uri")
                    RingerModeReceiver.checkAndTriggerAutoSilence(this@NabilSilenceApp)
                }
            }

            // Observe system settings and ringer mode changes
            contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                contentObserver!!
            )
            Log.d(TAG, "Successfully registered Audio ContentObserver")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering ContentObserver: ${e.message}")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val timerChannel = NotificationChannel(
                SilenceTimerService.CHANNEL_ID,
                "خدمة مؤقت نبيل سيلونس",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار تفاعلي ومستمر لعرض الوقت المتبقي للكتم التلقائي"
                setShowBadge(true)
            }

            val alertChannel = NotificationChannel(
                SilenceTimerService.ALERT_CHANNEL_ID,
                "تنبيهات إلغاء الكتم",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعار عند انتهاء وقت الكتم واستعادة الصوت الطبيعي"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(timerChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
