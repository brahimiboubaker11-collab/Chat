package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SilencePreferences
import com.example.service.SilenceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("نبيل سيلونس", appName)
    }

    @Test
    fun `test preferences persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SilencePreferences.getInstance(context)

        prefs.defaultDurationMinutes = 45
        assertEquals(45, prefs.defaultDurationMinutes)

        prefs.isAutoDetectionEnabled = true
        assertTrue(prefs.isAutoDetectionEnabled)

        prefs.defaultDurationMinutes = 180
        assertEquals(180, prefs.defaultDurationMinutes)

        prefs.restoreVolumePercent = 80
        assertEquals(80, prefs.restoreVolumePercent)
    }

    @Test
    fun `test silence manager initialization`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SilenceManager.getInstance(context)
        assertNotNull(manager)
        val status = manager.getCurrentRingerStatus()
        assertNotNull(status)
    }
}
