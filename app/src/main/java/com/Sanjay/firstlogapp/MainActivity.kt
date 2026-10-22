package com.Sanjay.firstlogapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Sanjay.firstlogapp.data.local.DataManager
import com.Sanjay.firstlogapp.data.repository.AndroidDashboardRepository
import com.Sanjay.firstlogapp.ui.dashboard.DashboardViewModel
import com.Sanjay.firstlogapp.ui.navigation.MainNavigationHost
import com.Sanjay.firstlogapp.ui.theme.FirstLogAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Application wiring only. LoggingService owns device monitoring independently. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = DataManager(applicationContext)
        val service = Intent(this, LoggingService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
        val factory = DashboardViewModel.Factory(AndroidDashboardRepository(applicationContext, storage))
        setContent {
            val viewModel: DashboardViewModel = viewModel(factory = factory)
            val state by viewModel.state.collectAsStateWithLifecycle()
            var theme by rememberSaveable { mutableStateOf(storage.getThemeMode()) }
            var thumbnails by rememberSaveable { mutableStateOf(storage.getThumbnailStyle()) }
            val scope = rememberCoroutineScope()
            val dark = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
            FirstLogAppTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainNavigationHost(
                        state = state, dataManager = storage,
                        currentThemeMode = theme,
                        onThemeChange = { mode ->
                            theme = mode
                            scope.launch(Dispatchers.IO) { storage.setThemeMode(mode) }
                        },
                        currentThumbnailStyle = thumbnails,
                        onThumbnailStyleChange = { style ->
                            thumbnails = style
                            scope.launch(Dispatchers.IO) { storage.setThumbnailStyle(style) }
                        },
                        onForceResetToday = {
                            scope.launch {
                                withContext(Dispatchers.IO) { storage.forceResetCycleNow() }
                                viewModel.refresh()
                            }
                        },
                        onRefresh = { viewModel.refresh() }
                    )
                }
            }
        }
    }
}
