package com.Sanjay.firstlogapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LoggingService : Service() {

    private lateinit var dataManager: DataManager
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var wifiNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private var audioManager: AudioManager? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null

    // Screen unlock timing and session classification
    private var lastScreenOnTime: Long = 0L
    private var unlockedDuringThisScreenOn: Boolean = false

    // Real-time app usage tracking state
    private var currentForegroundPackage: String? = null
    private var currentSessionStartTime: Long = 0L
    private var tentativePackage: String? = null
    private var tentativeStartTime: Long = 0L
    private var tentativeCounted: Boolean = false

    private val backgroundReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val now = System.currentTimeMillis()
            dataManager.checkAndPerformCycleRollover(now)
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    lastScreenOnTime = now
                    unlockedDuringThisScreenOn = false
                    dataManager.updateMetricState("screen", true, now)
                    dataManager.onScreenTurnedOn(now)
                }
                Intent.ACTION_SCREEN_OFF -> {
                    val sessionDur = if (lastScreenOnTime > 0L) (now - lastScreenOnTime) else 0L
                    if (unlockedDuringThisScreenOn) {
                        val isQuickGlance = sessionDur < 10_000L // < 10s: quick glance, >= 10s: actual usage
                        dataManager.recordUnlockSessionEnd(isQuickGlance, sessionDur, now)
                    } else if (sessionDur > 0L) {
                        dataManager.recordLockScreenGlance(sessionDur, now)
                    }
                    unlockedDuringThisScreenOn = false
                    lastScreenOnTime = 0L
                    dataManager.updateMetricState("screen", false, now)
                    dataManager.onScreenTurnedOff(now)
                    // Finalize active app session when screen turns off
                    if (currentForegroundPackage != null) {
                        val dur = now - currentSessionStartTime
                        if (dur > 0L) {
                            dataManager.recordAppSessionEnd(currentForegroundPackage!!, dur, now)
                        }
                        currentForegroundPackage = null
                        tentativePackage = null
                        tentativeCounted = false
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    unlockedDuringThisScreenOn = true
                    // Classify unlock method based on latency from screen-on
                    val latency = if (lastScreenOnTime > 0L) (now - lastScreenOnTime) else 0L
                    val unlockType = when {
                        lastScreenOnTime == 0L || latency <= 500L -> "fingerprint"
                        latency in 501L..1800L -> "face"
                        else -> "pin"
                    }
                    dataManager.recordUnlock(unlockType, now)
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    updateBatteryInfo(intent)
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    dataManager.handleChargingStarted(now)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    dataManager.handleChargingStopped(now)
                }
                WifiManager.WIFI_STATE_CHANGED_ACTION,
                WifiManager.NETWORK_STATE_CHANGED_ACTION,
                ConnectivityManager.CONNECTIVITY_ACTION,
                Intent.ACTION_AIRPLANE_MODE_CHANGED,
                "android.net.wifi.STATE_CHANGE",
                "android.intent.action.ANY_DATA_CONNECTION_STATE_CHANGED" -> {
                    val isWifi = DeviceStateHelper.isWifiEnabled(this@LoggingService)
                    val isData = DeviceStateHelper.isMobileDataEnabled(this@LoggingService)
                    dataManager.updateMetricState("data", isData, now)
                    dataManager.updateMetricState("wifi", isWifi, now)
                    dataManager.updateMetricState("both_wifi_data", isWifi && isData, now)
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val isBtOn = when (state) {
                        BluetoothAdapter.STATE_ON -> true
                        BluetoothAdapter.STATE_OFF -> false
                        else -> DeviceStateHelper.isBluetoothEnabled(this@LoggingService)
                    }
                    dataManager.updateMetricState("bluetooth", isBtOn, now)
                    if (!isBtOn) {
                        dataManager.onBluetoothDeviceDisconnected(BluetoothCategoryHelper.CAT_BUDS, now)
                        dataManager.onBluetoothDeviceDisconnected(BluetoothCategoryHelper.CAT_WATCH, now)
                        dataManager.onBluetoothDeviceDisconnected(BluetoothCategoryHelper.CAT_AUTO, now)
                        dataManager.onBluetoothDeviceDisconnected(BluetoothCategoryHelper.CAT_OTHER, now)
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    handleBluetoothAclEvent(intent, true, now)
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    handleBluetoothAclEvent(intent, false, now)
                }
                LocationManager.PROVIDERS_CHANGED_ACTION,
                "android.location.PROVIDERS_CHANGED" -> {
                    dataManager.updateMetricState("gps", DeviceStateHelper.isGpsEnabled(this@LoggingService), now)
                }
            }
        }
    }

    private fun handleBluetoothAclEvent(intent: Intent, isConnected: Boolean, now: Long) {
        BluetoothCategoryHelper.handleAclEvent(this, dataManager, intent, isConnected, now)
    }

    private fun updateBatteryInfo(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        dataManager.handleBatteryUpdate(level, scale, status, plugged, System.currentTimeMillis())
    }

    override fun onCreate() {
        super.onCreate()
        dataManager = DataManager(this)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    BluetoothCategoryHelper.checkAllBluetoothConnections(this@LoggingService, dataManager, System.currentTimeMillis())
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    BluetoothCategoryHelper.checkAllBluetoothConnections(this@LoggingService, dataManager, System.currentTimeMillis())
                }
            }
            try {
                audioManager?.registerAudioDeviceCallback(audioDeviceCallback, null)
            } catch (_: Exception) {}
        }

        startForegroundLogging()
        registerReceivers()
        registerWifiNetworkCallback()
        checkInitialStates()
        AutoBackupHelper.scheduleSundayAlarm(this)
        startPeriodicSync()
    }

    private fun registerWifiNetworkCallback() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val wifiRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            wifiNetworkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val now = System.currentTimeMillis()
                    dataManager.checkAndPerformCycleRollover(now)
                    dataManager.updateMetricState("wifi", true, now)
                    val isData = DeviceStateHelper.isMobileDataEnabled(this@LoggingService)
                    dataManager.updateMetricState("both_wifi_data", isData, now)
                }
                override fun onLost(network: Network) {
                    val now = System.currentTimeMillis()
                    dataManager.checkAndPerformCycleRollover(now)
                    val isStillOn = DeviceStateHelper.isWifiEnabled(this@LoggingService)
                    dataManager.updateMetricState("wifi", isStillOn, now)
                    val isData = DeviceStateHelper.isMobileDataEnabled(this@LoggingService)
                    dataManager.updateMetricState("both_wifi_data", isStillOn && isData, now)
                }
            }
            cm?.registerNetworkCallback(wifiRequest, wifiNetworkCallback!!)
        } catch (_: Exception) {}
    }

    private fun startForegroundLogging() {
        val channelId = "firstlog_activity_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Device Activity Tracker",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("FirstLogApp Active")
            .setContentText("Monitoring screen, radio, and call activity 24/7")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, notification)
        }
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction("android.net.wifi.STATE_CHANGE")
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction("android.location.PROVIDERS_CHANGED")
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction("android.intent.action.ANY_DATA_CONNECTION_STATE_CHANGED")
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(backgroundReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(backgroundReceiver, filter)
            }
        } catch (_: Exception) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(backgroundReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    registerReceiver(backgroundReceiver, filter)
                }
            } catch (_: Exception) {}
        }
    }

    private fun checkInitialStates() {
        val now = System.currentTimeMillis()
        dataManager.checkAndPerformCycleRollover(now)
        val isScreenOn = DeviceStateHelper.isScreenInteractive(this)
        val isWifi = DeviceStateHelper.isWifiEnabled(this)
        val isData = DeviceStateHelper.isMobileDataEnabled(this)
        if (isScreenOn) lastScreenOnTime = now

        dataManager.updateMetricState("screen", isScreenOn, now)
        dataManager.updateMetricState("wifi", isWifi, now)
        dataManager.updateMetricState("data", isData, now)
        dataManager.updateMetricState("both_wifi_data", isWifi && isData, now)
        dataManager.updateMetricState("bluetooth", DeviceStateHelper.isBluetoothEnabled(this), now)
        dataManager.updateMetricState("gps", DeviceStateHelper.isGpsEnabled(this), now)

        try {
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryIntent?.let { intent ->
                updateBatteryInfo(intent)
            }
        } catch (_: Exception) {}

        BluetoothCategoryHelper.checkAllBluetoothConnections(this, dataManager, now)
    }

    private fun startPeriodicSync() {
        serviceScope.launch {
            while (isActive) {
                delay(1500L)
                val now = System.currentTimeMillis()
                dataManager.checkAndPerformCycleRollover(now)

                val isScreenOn = DeviceStateHelper.isScreenInteractive(this@LoggingService)
                val isWifi = DeviceStateHelper.isWifiEnabled(this@LoggingService)
                val isData = DeviceStateHelper.isMobileDataEnabled(this@LoggingService)

                dataManager.updateMetricState("screen", isScreenOn, now)
                dataManager.updateMetricState("wifi", isWifi, now)
                dataManager.updateMetricState("data", isData, now)
                dataManager.updateMetricState("both_wifi_data", isWifi && isData, now)
                dataManager.updateMetricState("bluetooth", DeviceStateHelper.isBluetoothEnabled(this@LoggingService), now)
                dataManager.updateMetricState("gps", DeviceStateHelper.isGpsEnabled(this@LoggingService), now)

                BluetoothCategoryHelper.checkAllBluetoothConnections(this@LoggingService, dataManager, now)

                // Real-time app usage tracking (only when screen is interactive!)
                trackForegroundApp(isScreenOn, now)

                // Scheduled Sunday 6:00 AM Auto Backup check
                AutoBackupHelper.checkAndRunSundayAutoBackup(this@LoggingService, dataManager, forceRun = false)
            }
        }
    }

    private fun trackForegroundApp(isScreenOn: Boolean, now: Long) {
        if (!isScreenOn || !AppUsageHelper.hasPermission(this)) {
            if (currentForegroundPackage != null) {
                val dur = now - currentSessionStartTime
                if (dur > 0L) {
                    dataManager.recordAppSessionEnd(currentForegroundPackage!!, dur, now)
                }
                currentForegroundPackage = null
                tentativePackage = null
                tentativeCounted = false
            }
            return
        }

        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val topPkg = AppUsageHelper.getTopForegroundPackage(usm, now)

        if (topPkg != null && !AppUsageHelper.isIgnoredSystemPackage(topPkg, packageName)) {
            val appName = AppUsageHelper.resolveAppName(this, topPkg)
            if (topPkg != currentForegroundPackage) {
                // Switching from one app to another
                if (currentForegroundPackage != null) {
                    val dur = now - currentSessionStartTime
                    if (dur > 0L) {
                        dataManager.recordAppSessionEnd(currentForegroundPackage!!, dur, now)
                    }
                }
                currentForegroundPackage = topPkg
                currentSessionStartTime = now
                tentativePackage = topPkg
                tentativeStartTime = now
                tentativeCounted = false
            } else {
                // User is actively in this app
                val dwell = now - tentativeStartTime
                if (!tentativeCounted && tentativePackage == topPkg && dwell >= 2500L) {
                    // Dwell exceeded 2.5s: Verified user app open!
                    tentativeCounted = true
                    dataManager.recordVerifiedAppOpen(topPkg, appName, now)
                }
                dataManager.addAppUsageTick(topPkg, appName, 1500L, now)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        wifiNetworkCallback?.let {
            try {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                cm?.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioDeviceCallback != null) {
            try {
                audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
            } catch (_: Exception) {}
        }
        try {
            unregisterReceiver(backgroundReceiver)
        } catch (_: Exception) {}

        val now = System.currentTimeMillis()
        val metrics = listOf("screen", "wifi", "bluetooth", "gps", "data", "both_wifi_data")
        for (m in metrics) {
            if (dataManager.isMetricActive(m)) {
                val onTime = dataManager.getMetricLastOnTime(m)
                if (onTime > 0L && now > onTime) {
                    val sessionDur = now - onTime
                    dataManager.addMetricTime(m, sessionDur)
                    dataManager.setMetricLastSessionDuration(m, sessionDur)
                    dataManager.setMetricLastOnTime(m, now)
                }
            }
        }
        if (dataManager.isCharging()) {
            val startTime = dataManager.getLastChargeStartTime()
            if (startTime > 0L && now > startTime) {
                dataManager.addChargingTime(now - startTime)
                dataManager.setLastChargeStartTime(now)
            }
        }
        if (currentForegroundPackage != null) {
            val dur = now - currentSessionStartTime
            if (dur > 0L) {
                dataManager.recordAppSessionEnd(currentForegroundPackage!!, dur, now)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}