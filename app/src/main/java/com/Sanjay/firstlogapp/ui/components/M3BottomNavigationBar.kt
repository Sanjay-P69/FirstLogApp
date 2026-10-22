package com.Sanjay.firstlogapp.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.Sanjay.firstlogapp.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class M3NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Material 3 Navigation Bar with tonal elevation, soft pill active indicator,
 * and strict edge-to-edge system bar padding.
 */
@Composable
fun M3BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        M3NavItem(stringResource(R.string.today_tab), Icons.Filled.Home, Icons.Outlined.Home),
        M3NavItem(stringResource(R.string.trends_tab), Icons.Filled.BarChart, Icons.Outlined.BarChart),
        M3NavItem(stringResource(R.string.settings_tab), Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selectedTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}


/** Expanded-width sibling using the same destinations and selection contract. */
@Composable
fun M3NavigationRail(selectedTab: Int, onTabSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        M3NavItem(stringResource(R.string.today_tab), Icons.Filled.Home, Icons.Outlined.Home),
        M3NavItem(stringResource(R.string.trends_tab), Icons.Filled.BarChart, Icons.Outlined.BarChart),
        M3NavItem(stringResource(R.string.settings_tab), Icons.Filled.Settings, Icons.Outlined.Settings)
    )
    androidx.compose.material3.NavigationRail(modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        items.forEachIndexed { index, item ->
            androidx.compose.material3.NavigationRailItem(selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(if (selectedTab == index) item.selectedIcon else item.unselectedIcon, null) },
                label = { Text(item.title) })
        }
    }
}
