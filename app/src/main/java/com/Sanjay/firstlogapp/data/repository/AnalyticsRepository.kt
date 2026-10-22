package com.Sanjay.firstlogapp.data.repository

import com.Sanjay.firstlogapp.data.local.DataManager

interface AnalyticsRepository {
    fun readChart(metricId: String, period: String): List<Pair<String, Float>>
}

class StoredAnalyticsRepository(private val storage: DataManager) : AnalyticsRepository {
    override fun readChart(metricId: String, period: String): List<Pair<String, Float>> =
        storage.getChartData(metricId, period)
}
