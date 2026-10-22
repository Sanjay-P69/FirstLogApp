package com.Sanjay.firstlogapp.ui.dashboard

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.Sanjay.firstlogapp.data.repository.DashboardSnapshot
import com.Sanjay.firstlogapp.ui.theme.FirstLogAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DashboardContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun deniedCallAccessOffersAnActionInsteadOfFakeCallCounts() {
        var received: DashboardAction? = null
        compose.setContent {
            FirstLogAppTheme(dynamicColor = false) {
                DashboardContent(DashboardSnapshot().toUiState(), { received = it })
            }
        }
        // Lazy grid items are composed on demand; find the action by scrolling the grid.
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Enable call tracking"))
        compose.onNodeWithText("Enable call tracking").performClick()
        compose.runOnIdle { assertEquals(DashboardAction.GrantCalls, received) }
    }

    @Test
    fun dailySummaryEmitsAnActionWithoutStartingAService() {
        var received: DashboardAction? = null
        compose.setContent {
            FirstLogAppTheme(dynamicColor = false) {
                DashboardContent(DashboardSnapshot().toUiState(), { received = it })
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Daily summary"))
        compose.onNodeWithText("Daily summary").performClick()
        compose.runOnIdle { assertEquals(DashboardAction.OpenSummary, received) }
    }
}
