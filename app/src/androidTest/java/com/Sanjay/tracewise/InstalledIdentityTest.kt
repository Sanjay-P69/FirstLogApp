package com.sanjay.tracewise

import android.content.ComponentName
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The rebrand must preserve installation identity and Android component grants. */
@RunWith(AndroidJUnit4::class)
class InstalledIdentityTest {
    @Test
    fun rebrandPreservesInstalledIdAndRegisteredComponents() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val legacyPackage = "com.Sanjay.firstlogapp"
        assertEquals(legacyPackage, context.packageName)
        assertEquals("Tracewise", context.getString(R.string.app_name))
        val manager = context.packageManager
        assertEquals(
            "$legacyPackage.MainActivity",
            manager.getActivityInfo(ComponentName(context, "$legacyPackage.MainActivity"), 0).name
        )
        for (name in listOf("LoggingService", "AppNotificationListenerService")) {
            val component = "$legacyPackage.$name"
            assertEquals(
                component,
                manager.getServiceInfo(ComponentName(context, component), 0).name
            )
        }
        for (name in listOf("BootReceiver", "AutoBackupReceiver")) {
            val component = "$legacyPackage.$name"
            assertEquals(
                component,
                manager.getReceiverInfo(ComponentName(context, component), 0).name
            )
        }
    }
}
