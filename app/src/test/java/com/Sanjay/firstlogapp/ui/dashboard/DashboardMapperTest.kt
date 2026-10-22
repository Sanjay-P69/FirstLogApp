package com.Sanjay.firstlogapp.ui.dashboard

import com.Sanjay.firstlogapp.data.model.AppUsageRecord
import com.Sanjay.firstlogapp.data.repository.DashboardSnapshot
import com.Sanjay.firstlogapp.data.repository.TrackedMetricSnapshot
import org.junit.Assert.*
import org.junit.Test

class DashboardMapperTest {
    @Test fun zeroActivityDoesNotInventTimeOrSessions() {
        val state = DashboardSnapshot().toUiState(now = 0)
        assertEquals("00m 00s", state.screen.duration)
        assertEquals("0 sessions · Inactive", state.screen.status)
        assertEquals("0%", state.battery.duration)
        assertTrue(state.apps.isEmpty())
        assertFalse(state.canReadCalls)
        assertFalse(state.canReadApps)
    }

    @Test fun allAppsRemainAvailableBeyondDashboardTopThree() {
        val apps = (1..5).map { AppUsageRecord("app.$it", "App $it", 60_000L * it, launchCount = it) }
        val state = DashboardSnapshot(apps = apps, hasUsagePermission = true).toUiState(now = 0)
        assertEquals(5, state.apps.size)
        assertEquals("15m 00s", state.appUsage)
        assertEquals(5, state.apps.last().launches)
        assertTrue(state.canReadApps)
    }

    @Test fun independentPermissionsAndLiveMeasurementsArePreserved() {
        val state = DashboardSnapshot(
            metrics = listOf(TrackedMetricSnapshot("wifi", 3_600_000, 2, true)),
            wifiSsid = "Home", wifiBytes = 1234, wifiSessions = 2,
            hasCallPermission = true, hasBluetoothPermission = false,
            batteryUsed = 12, unlockCount = 7
        ).toUiState(now = 0)
        val wifi = state.connections.first { it.id == "wifi" }
        assertEquals("1h 00m 00s", wifi.duration)
        assertEquals(1234L, wifi.wifiDataBytes)
        assertEquals("Home", wifi.wifiSsid)
        assertEquals("12%", state.battery.duration)
        assertEquals("7", state.unlocks.duration)
        assertTrue(state.canReadCalls)
        assertFalse(state.canReadBluetooth)
    }
}
