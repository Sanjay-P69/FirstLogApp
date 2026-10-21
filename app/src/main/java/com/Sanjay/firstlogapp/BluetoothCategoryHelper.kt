package com.Sanjay.firstlogapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import java.util.Locale

object BluetoothCategoryHelper {
    const val CAT_BUDS = "buds"
    const val CAT_WATCH = "watch"
    const val CAT_AUTO = "auto"
    const val CAT_OTHER = "other"

    fun classifyDevice(name: String?, btClass: BluetoothClass?): String {
        val lower = name?.lowercase(Locale.getDefault())?.trim() ?: ""
        if (lower.isBlank()) {
            return CAT_OTHER
        }

        // 1. Android Auto / Car checks
        if (lower.contains("auto") || lower.contains("car") ||
            lower.contains("uconnect") || lower.contains("sync") ||
            lower.contains("infotainment") || lower.contains("mycar") ||
            lower.contains("bt_car") || lower.contains("honda") ||
            lower.contains("hyundai") || lower.contains("toyota") ||
            lower.contains("maruti") || lower.contains("suzuki") ||
            lower.contains("ford") || lower.contains("vw") || lower.contains("volkswagen") ||
            lower.contains("bmw") || lower.contains("audi") || lower.contains("kia") ||
            lower.contains("tata") || lower.contains("mahindra") || lower.contains("nissan") ||
            lower.contains("renault") || lower.contains("skoda") || lower.contains("benz") ||
            lower.contains("mercedes") || lower.contains("mazda") || lower.contains("volvo") ||
            lower.contains("chevrolet") || lower.contains("chevy") || lower.contains("carplay") ||
            lower.contains("handsfree") || lower.contains("carkit")
        ) {
            return CAT_AUTO
        }
        if (btClass != null && (
            btClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
            btClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE
        )) {
            return CAT_AUTO
        }

        // 2. Smartwatch / Fitness Band checks
        if (lower.contains("watch") || lower.contains("band") ||
            lower.contains("fit") || lower.contains("wear") || lower.contains("gear") ||
            lower.contains("garmin") || lower.contains("amazfit") ||
            lower.contains("noise") || lower.contains("fire-boltt") || lower.contains("fireboltt") ||
            lower.contains("boat wave") || lower.contains("boat storm") || lower.contains("boat xtend") ||
            lower.contains("titan") || lower.contains("fastrack") ||
            lower.contains("fossil") || lower.contains("suunto") || lower.contains("polar") ||
            lower.contains("dizo") || lower.contains("honor band") || lower.contains("mi band") ||
            lower.contains("galaxy watch") || lower.contains("pixel watch") || lower.contains("apple watch") ||
            lower.contains("fitbit") || lower.contains("huawei watch") || lower.contains("oppo watch") ||
            lower.contains("realme watch") || lower.contains("redmi watch") || lower.contains("cmf watch") ||
            lower.contains("crossbeats")
        ) {
            return CAT_WATCH
        }
        if (btClass != null && (
            btClass.majorDeviceClass == BluetoothClass.Device.Major.WEARABLE ||
            btClass.deviceClass == BluetoothClass.Device.WEARABLE_WRIST_WATCH
        )) {
            return CAT_WATCH
        }

        // 3. Buds / Headphones / Audio
        if (lower.contains("buds") || lower.contains("airpod") ||
            lower.contains("earbud") || lower.contains("earphone") ||
            lower.contains("headphone") || lower.contains("headset") ||
            lower.contains("audio") || lower.contains("tws") ||
            lower.contains("wh-") || lower.contains("wf-") ||
            lower.contains("tune") || lower.contains("soundcore") ||
            lower.contains("freebuds") || lower.contains("boat") ||
            lower.contains("rockerz") || lower.contains("airdopes") ||
            lower.contains("speaker") || lower.contains("jbl") ||
            lower.contains("bose") || lower.contains("sony") ||
            lower.contains("sennheiser") || lower.contains("realme buds") ||
            lower.contains("oneplus buds") || lower.contains("nord buds") ||
            lower.contains("boult") || lower.contains("hammer") ||
            lower.contains("mivi") || lower.contains("truke") ||
            lower.contains("ptron") || lower.contains("wings") ||
            lower.contains("neckband") || lower.contains("in-ear")
        ) {
            return CAT_BUDS
        }
        if (btClass != null && (
            btClass.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO ||
            btClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
            btClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
            btClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER
        )) {
            return CAT_BUDS
        }

        return CAT_OTHER
    }

    fun getCategoryTitle(cat: String): String = when (cat) {
        CAT_BUDS -> "Buds"
        CAT_WATCH -> "Smartwatch"
        CAT_AUTO -> "Android Auto"
        else -> "Other"
    }

    fun getCategoryIcon(cat: String): String = when (cat) {
        CAT_BUDS -> "🎧"
        CAT_WATCH -> "⌚"
        CAT_AUTO -> "🚗"
        else -> "📱"
    }

    fun isBudsConnected(context: Context): Boolean {
        if (!DeviceStateHelper.isBluetoothEnabled(context)) return false
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val audioDevs = try { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } catch (_: Exception) { emptyArray() }
            for (dev in audioDevs) {
                val type = dev.type
                val isBtAudio = type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET) ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && type == AudioDeviceInfo.TYPE_HEARING_AID)
                if (isBtAudio) {
                    val pName = dev.productName?.toString() ?: ""
                    if (classifyDevice(pName, null) == CAT_BUDS) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun checkAllBluetoothConnections(context: Context, dataManager: DataManager, now: Long = System.currentTimeMillis()) {
        if (!DeviceStateHelper.isBluetoothEnabled(context)) {
            dataManager.onBluetoothDeviceDisconnected(CAT_BUDS, now)
            dataManager.onBluetoothDeviceDisconnected(CAT_WATCH, now)
            dataManager.onBluetoothDeviceDisconnected(CAT_AUTO, now)
            dataManager.onBluetoothDeviceDisconnected(CAT_OTHER, now)
            return
        }

        val connectedCategories = mutableSetOf<String>()
        val categoryDeviceNames = mutableMapOf<String, String>()

        // 1. AudioManager devices - strictly checks active audio routing to Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val audioDevs = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
                for (dev in audioDevs) {
                    val type = dev.type
                    val isBtAudio = type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                                type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                                type == AudioDeviceInfo.TYPE_BLE_BROADCAST
                            )) ||
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && type == AudioDeviceInfo.TYPE_HEARING_AID)
                    if (isBtAudio) {
                        val pName = dev.productName?.toString() ?: ""
                        val cat = classifyDevice(pName, null)
                        if (cat == CAT_AUTO) {
                            connectedCategories.add(CAT_AUTO)
                            categoryDeviceNames[CAT_AUTO] = if (pName.isNotBlank()) pName else "Android Auto"
                        } else if (cat == CAT_BUDS) {
                            connectedCategories.add(CAT_BUDS)
                            categoryDeviceNames[CAT_BUDS] = if (pName.isNotBlank()) pName else "Audio Buds"
                        } else if (cat == CAT_WATCH) {
                            connectedCategories.add(CAT_WATCH)
                            categoryDeviceNames[CAT_WATCH] = if (pName.isNotBlank()) pName else "Smartwatch"
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. BluetoothManager GATT devices (Smartwatches & Fitness Bands ONLY - never buds)
        try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val gattDevices = bm?.getConnectedDevices(android.bluetooth.BluetoothProfile.GATT) ?: emptyList()
            for (dev in gattDevices) {
                val name = try { dev.name } catch (_: SecurityException) { null }
                val btClass = try { dev.bluetoothClass } catch (_: SecurityException) { null }
                val cat = classifyDevice(name, btClass)
                if (cat == CAT_WATCH || cat == CAT_OTHER) {
                    val targetCat = if (cat == CAT_OTHER) CAT_WATCH else cat
                    connectedCategories.add(targetCat)
                    if (name != null) categoryDeviceNames[targetCat] = name
                }
            }
        } catch (_: SecurityException) {} catch (_: Exception) {}

        // 3. BluetoothAdapter bonded devices isConnected reflection (Wearables & Android Auto)
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled) {
                val isConnectedMethod = BluetoothDevice::class.java.getMethod("isConnected")
                val bonded = try { adapter.bondedDevices } catch (_: SecurityException) { null } ?: emptySet()
                for (dev in bonded) {
                    val isConn = try {
                        isConnectedMethod.invoke(dev) as? Boolean ?: false
                    } catch (_: Exception) { false }
                    if (isConn) {
                        val name = try { dev.name } catch (_: SecurityException) { null }
                        val btClass = try { dev.bluetoothClass } catch (_: SecurityException) { null }
                        val cat = classifyDevice(name, btClass)
                        if (cat == CAT_WATCH || cat == CAT_AUTO) {
                            connectedCategories.add(cat)
                            if (name != null) categoryDeviceNames[cat] = name
                        }
                    }
                }
            }
        } catch (_: SecurityException) {} catch (_: Exception) {}

        // Synchronize active states with dataManager
        val allTrackedCats = listOf(CAT_BUDS, CAT_WATCH, CAT_AUTO, CAT_OTHER)
        for (cat in allTrackedCats) {
            if (connectedCategories.contains(cat)) {
                if (!dataManager.isBtCategoryActive(cat)) {
                    val dName = categoryDeviceNames[cat] ?: getCategoryTitle(cat)
                    dataManager.onBluetoothDeviceConnected(cat, dName, now)
                }
            } else {
                if (dataManager.isBtCategoryActive(cat)) {
                    dataManager.onBluetoothDeviceDisconnected(cat, now)
                }
            }
        }
    }

    fun handleAclEvent(context: Context, dataManager: DataManager, intent: Intent, isConnected: Boolean, now: Long = System.currentTimeMillis()) {
        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        val nameExtra = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
            ?: intent.getStringExtra("android.bluetooth.device.extra.NAME")
        val name = nameExtra ?: try { device?.name } catch (_: SecurityException) { null }
        val btClass = try { device?.bluetoothClass } catch (_: SecurityException) { null }
        val category = classifyDevice(name, btClass)
        if (isConnected) {
            if (category == CAT_BUDS) {
                // If it's buds, only mark connected if audio routing is confirmed
                if (isBudsConnected(context)) {
                    dataManager.onBluetoothDeviceConnected(category, name ?: getCategoryTitle(category), now)
                }
            } else {
                dataManager.onBluetoothDeviceConnected(category, name ?: getCategoryTitle(category), now)
            }
        } else {
            dataManager.onBluetoothDeviceDisconnected(category, now)
        }
        checkAllBluetoothConnections(context, dataManager, now)
    }
}
