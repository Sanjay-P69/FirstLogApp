package com.Sanjay.firstlogapp.data.device

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.telephony.TelephonyManager

object DeviceStateHelper {
    fun isWifiEnabled(context: Context): Boolean {
        // 1. ConnectivityManager: active network or any network has TRANSPORT_WIFI
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeNet = cm.activeNetwork
                if (activeNet != null) {
                    val caps = cm.getNetworkCapabilities(activeNet)
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return true
                    }
                }
                for (net in cm.allNetworks) {
                    val caps = cm.getNetworkCapabilities(net)
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return true
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. WifiManager connected supplicant state check
        try {
            val wifiMgr = (context.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
                ?: (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
            if (wifiMgr != null && wifiMgr.isWifiEnabled) {
                val info = wifiMgr.connectionInfo
                if (info != null && info.networkId != -1) {
                    val suppState = info.supplicantState
                    if (suppState == android.net.wifi.SupplicantState.COMPLETED) {
                        val bssid = info.bssid
                        if (bssid != null && bssid != "02:00:00:00:00:00" && bssid != "00:00:00:00:00:00") {
                            return true
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return false
    }

    fun isMobileDataEnabled(context: Context): Boolean {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        // 1. Primary check: TelephonyManager.isDataEnabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                telephony?.let {
                    return it.isDataEnabled
                }
            } catch (_: SecurityException) {
                // Permission can be revoked between the check and the platform query.
            }
        }

        // 2. TelephonyManager dataState check (DATA_CONNECTED = 2)
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                telephony?.dataState == TelephonyManager.DATA_CONNECTED) {
                return true
            }
        } catch (_: SecurityException) {}

        // 3. ConnectivityManager active network with TRANSPORT_CELLULAR & NET_CAPABILITY_INTERNET
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork
            if (activeNet != null) {
                val caps = cm.getNetworkCapabilities(activeNet)
                if (caps != null &&
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                ) {
                    return true
                }
            }
        } catch (_: Exception) {}

        return false
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        val btAdapter = try {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                ?: BluetoothAdapter.getDefaultAdapter()
        } catch (_: Exception) {
            null
        }
        return try {
            btAdapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        }
    }

    fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return try {
            lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        } catch (_: Exception) {
            false
        }
    }

    fun isScreenInteractive(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isInteractive == true
    }
}
