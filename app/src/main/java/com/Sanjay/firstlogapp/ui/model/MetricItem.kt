package com.Sanjay.firstlogapp.ui.model


data class MetricItem(
    val id: String,
    val title: String,
    val duration: String,
    val status: String,
    val iconResId: Int = 0,
    // Extra custom payload for specialized cards
    val bothWifiDataTime: String? = null,
    val bothWifiDataCount: Int = 0,
    val unlockFingerprint: Int = 0,
    val unlockFace: Int = 0,
    val unlockPin: Int = 0,
    val btBudsDur: String? = null,
    val btWatchDur: String? = null,
    val btAutoDur: String? = null,
    val connectedCallsCount: Int = 0,
    val connectedCallsDuration: String? = null,
    val missedCallsCount: Int = 0,
    val wifiSsid: String? = null,
    val wifiDataBytes: Long = 0L,
    val wifiSessionCount: Int = 0,
    val mobileDataBytes: Long = 0L,
    val mobileSessionCount: Int = 0,
    val chargingSessionsCount: Int = 0,
    val screenOnDrainRate: Double = 0.0,
    val actualUsageCount: Int = 0,
    val quickGlanceCount: Int = 0
)
