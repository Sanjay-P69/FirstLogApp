package com.sanjay.tracewise.ui.notifications

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Sanjay.firstlogapp.AppNotificationListenerService as NotificationService
import com.Sanjay.firstlogapp.NotificationHistoryItem
import com.sanjay.tracewise.R
import com.sanjay.tracewise.ui.apps.AppThumbnailView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCard(
    context: Context,
    thumbnailStyle: String = "vibrant",
    onOpenDialog: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isGranted = remember { NotificationService.isPermissionGranted(context) }
    var grantedState by remember { mutableStateOf(isGranted) }
    val notifications by NotificationService.notifications.collectAsState()
    val history by NotificationService.history.collectAsState()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (grantedState) onOpenDialog()
            }
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF673AB7).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔔", fontSize = 16.sp)
                    }
                    Column {
                        Text(
                            text = "Notifications",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = if (!grantedState) {
                                "Permission needed"
                            } else {
                                "${notifications.size} Active • " +
                                    "${history.size} History today"
                            },
                            fontSize = 11.sp,
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (grantedState) {
                    TextButton(onClick = onOpenDialog) {
                        Text("View Stacks 👉", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!grantedState) {
                Text(
                    text = stringResource(R.string.notification_listener_explanation),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = colors.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                "Cannot open notification settings",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Enable Notification Access",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // ==========================================
                // 1. TOP PORTION: ACTIVE NOTIFICATIONS
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Active Notifications (${notifications.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    if (notifications.isNotEmpty()) {
                        Text(
                            text = "Swipe to dismiss",
                            fontSize = 10.sp,
                            color = colors.outline
                        )
                    }
                }

                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "No active notifications in status bar",
                            fontSize = 11.sp,
                            color = colors.outline
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        notifications.take(3).forEach { notif ->
                            key(notif.key) {
                                val timeStr = SimpleDateFormat(
                                    "hh:mm a",
                                    Locale.getDefault()
                                ).format(Date(notif.postTime))
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.StartToEnd ||
                                            value == SwipeToDismissBoxValue.EndToStart
                                        ) {
                                            val success = NotificationService.markAsRead(
                                                notif.key
                                            )
                                            if (success) {
                                                Toast.makeText(
                                                    context,
                                                    "Notification marked as read",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = true,
                                    enableDismissFromEndToStart = true,
                                    backgroundContent = {
                                        val color = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd,
                                            SwipeToDismissBoxValue.EndToStart ->
                                                Color(
                                                    0xFF2E7D32
                                                ).copy(alpha = 0.25f)
                                            else -> Color.Transparent
                                        }
                                        val alignment = if (dismissState.dismissDirection ==
                                            SwipeToDismissBoxValue.StartToEnd
                                        ) {
                                            Alignment.CenterStart
                                        } else {
                                            Alignment.CenterEnd
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(color)
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = alignment
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    "✓",
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "Mark Read",
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                colors.surface.copy(alpha = 0.7f)
                                            )
                                            .border(
                                                1.dp,
                                                colors.primary.copy(
                                                    alpha = 0.25f
                                                ),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onOpenDialog() }
                                            .padding(8.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(
                                                        6.dp
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    AppThumbnailView(
                                                        packageName = notif.packageName,
                                                        appName = notif.appName,
                                                        style = thumbnailStyle,
                                                        size = 18
                                                    )
                                                    Text(
                                                        text = "${notif.appName} • ${notif.title}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = timeStr,
                                                    fontSize = 10.sp,
                                                    color = colors.outline
                                                )
                                            }
                                            if (notif.content.isNotBlank()) {
                                                Text(
                                                    text = notif.content,
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = colors.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = colors.outlineVariant.copy(alpha = 0.4f)
                )

                // ==========================================
                // 2. BOTTOM PORTION: NOTIFICATION HISTORY TODAY
                // ==========================================
                val allToday = remember(notifications, history) {
                    (
                        history + notifications.map {
                            NotificationHistoryItem(
                                id = it.key,
                                packageName = it.packageName,
                                appName = it.appName,
                                title = it.title,
                                content = it.content,
                                timestamp = it.postTime,
                                category = it.category,
                                canReply = it.canReply
                            )
                        }
                        ).distinctBy { "${it.packageName}_${it.title}_${it.content}" }
                }

                val categories = listOf("Call", "Message", "Gmail", "WhatsApp", "Others")
                val categoryCounts = remember(allToday) {
                    categories.associateWith { cat -> allToday.count { it.category == cat } }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📜 Notification History Today (${history.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val count = categoryCounts[cat] ?: 0
                        val icon = when (cat) {
                            "Call" -> "📞"
                            "Message" -> "💬"
                            "Gmail" -> "✉️"
                            "WhatsApp" -> "🟢"
                            else -> "📦"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    colors.outline.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onOpenDialog() }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("$icon $cat", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (count >
                                                0
                                            ) {
                                                colors.primary
                                            } else {
                                                Color.Gray.copy(alpha = 0.3f)
                                            }
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (count >
                                            0
                                        ) {
                                            colors.onPrimary
                                        } else {
                                            Color.White
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Stacks for Appwise for the day
                val appWiseList = remember(allToday) {
                    allToday.groupBy { it.appName }
                        .toList()
                        .sortedByDescending { it.second.size }
                }

                if (appWiseList.isEmpty()) {
                    Text(
                        text = "No non-persistent notifications recorded today.",
                        fontSize = 11.sp,
                        color = colors.outline
                    )
                } else {
                    Text(
                        text = "App Stacks Today:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(appWiseList) { (appName, items) ->
                            val samplePkg = items.firstOrNull()?.packageName ?: ""
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        colors.primaryContainer.copy(
                                            alpha = 0.7f
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        colors.primary.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onOpenDialog() }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AppThumbnailView(
                                        packageName = samplePkg,
                                        appName = appName,
                                        style = thumbnailStyle,
                                        size = 20
                                    )
                                    Text(
                                        text = appName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onPrimaryContainer
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(colors.primary)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "${items.size}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationDetailDialog(
    context: Context,
    thumbnailStyle: String = "vibrant",
    onDismiss: () -> Unit
) {
    val replyLabel = stringResource(R.string.notification_reply)
    val cancelLabel = stringResource(android.R.string.cancel)
    val colors = MaterialTheme.colorScheme
    val notifications by NotificationService.notifications.collectAsState()
    val history by NotificationService.history.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Active, 1: History
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var replyingKey by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }
    val expandedApps = remember { mutableStateMapOf<String, Boolean>() }

    val categories = listOf("All", "Call", "Message", "Gmail", "WhatsApp", "Others")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Notification Center", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "App-wise stacks & category history for today",
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "⚡ Active (${notifications.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "📜 History (${history.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category chips row: All, Call, Message, Gmail, WhatsApp, Others
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    items(categories) { cat ->
                        val isAll = cat == "All"
                        val isSelected = if (isAll) {
                            selectedCategoryFilter == null
                        } else {
                            selectedCategoryFilter ==
                                cat
                        }
                        val count = if (selectedTab == 0) {
                            if (isAll) {
                                notifications.size
                            } else {
                                notifications.count {
                                    it.category ==
                                        cat
                                }
                            }
                        } else {
                            if (isAll) history.size else history.count { it.category == cat }
                        }
                        val icon = when (cat) {
                            "Call" -> "📞"
                            "Message" -> "💬"
                            "Gmail" -> "✉️"
                            "WhatsApp" -> "🟢"
                            "Others" -> "📦"
                            else -> "⚡"
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryFilter =
                                    if (isAll || selectedCategoryFilter == cat) null else cat
                            },
                            label = { Text("$icon $cat ($count)", fontSize = 10.sp) }
                        )
                    }
                }

                if (selectedTab == 0) {
                    // TAB 0: Active Notifications
                    val filteredActive = remember(notifications, selectedCategoryFilter) {
                        if (selectedCategoryFilter ==
                            null
                        ) {
                            notifications
                        } else {
                            notifications.filter {
                                it.category ==
                                    selectedCategoryFilter
                            }
                        }
                    }

                    if (filteredActive.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No active notifications in this category",
                                fontSize = 12.sp,
                                color = colors.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredActive, key = { it.key }) { item ->
                                val timeStr = SimpleDateFormat(
                                    "hh:mm a",
                                    Locale.getDefault()
                                ).format(Date(item.postTime))
                                val isReplying = replyingKey == item.key

                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.StartToEnd ||
                                            value == SwipeToDismissBoxValue.EndToStart
                                        ) {
                                            val success = NotificationService.markAsRead(
                                                item.key
                                            )
                                            if (success) {
                                                Toast.makeText(
                                                    context,
                                                    "Notification marked as read",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = true,
                                    enableDismissFromEndToStart = true,
                                    backgroundContent = {
                                        val color = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd,
                                            SwipeToDismissBoxValue.EndToStart ->
                                                Color(
                                                    0xFF2E7D32
                                                ).copy(alpha = 0.25f)
                                            else -> Color.Transparent
                                        }
                                        val alignment = if (dismissState.dismissDirection ==
                                            SwipeToDismissBoxValue.StartToEnd
                                        ) {
                                            Alignment.CenterStart
                                        } else {
                                            Alignment.CenterEnd
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(color)
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = alignment
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    "✓",
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "Mark Read",
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = colors.surfaceVariant
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(
                                                        6.dp
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    AppThumbnailView(
                                                        packageName = item.packageName,
                                                        appName = item.appName,
                                                        style = thumbnailStyle,
                                                        size = 24
                                                    )
                                                    Text(
                                                        text = item.appName,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.primary
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(
                                                                colors.primary.copy(
                                                                    alpha = 0.12f
                                                                )
                                                            )
                                                            .padding(
                                                                horizontal = 5.dp,
                                                                vertical = 1.dp
                                                            )
                                                    ) {
                                                        Text(
                                                            item.category,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colors.primary
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = timeStr,
                                                    fontSize = 10.sp,
                                                    color = colors.outline
                                                )
                                            }

                                            if (item.title.isNotBlank()) {
                                                Text(
                                                    text = item.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.onSurface
                                                )
                                            }

                                            if (item.content.isNotBlank()) {
                                                Text(
                                                    text = item.content,
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp,
                                                    color = colors.onSurfaceVariant
                                                )
                                            }

                                            // Actions: Reply & Mark as Read
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (item.canReply) {
                                                    TextButton(
                                                        onClick = {
                                                            if (isReplying) {
                                                                replyingKey = null
                                                                replyText = ""
                                                            } else {
                                                                replyingKey = item.key
                                                                replyText = ""
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(
                                                            horizontal = 8.dp,
                                                            vertical = 2.dp
                                                        )
                                                    ) {
                                                        Text(
                                                            if (isReplying) {
                                                                cancelLabel
                                                            } else {
                                                                replyLabel
                                                            },
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))

                                                OutlinedButton(
                                                    onClick = {
                                                        val success =
                                                            NotificationService.markAsRead(
                                                                item.key
                                                            )
                                                        if (success) {
                                                            Toast.makeText(
                                                                context,
                                                                "Notification marked as read",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(
                                                        horizontal = 8.dp,
                                                        vertical = 2.dp
                                                    ),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text(
                                                        "✔ Mark as Read",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            // Inline reply composer
                                            if (isReplying) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(
                                                        top = 4.dp
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(
                                                        6.dp
                                                    )
                                                ) {
                                                    OutlinedTextField(
                                                        value = replyText,
                                                        onValueChange = { replyText = it },
                                                        placeholder = {
                                                            Text("Type reply...", fontSize = 11.sp)
                                                        },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(
                                                            1f
                                                        ).heightIn(min = 40.dp)
                                                    )
                                                    Button(
                                                        onClick = {
                                                            if (replyText.isNotBlank()) {
                                                                val sent =
                                                                    NotificationService.directReply(
                                                                        context,
                                                                        item.key,
                                                                        replyText.trim()
                                                                    )
                                                                if (sent) {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Reply sent!",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    replyingKey = null
                                                                    replyText = ""
                                                                } else {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Failed to send reply",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }
                                                        },
                                                        enabled = replyText.isNotBlank(),
                                                        contentPadding = PaddingValues(
                                                            horizontal = 10.dp,
                                                            vertical = 4.dp
                                                        ),
                                                        modifier = Modifier.height(38.dp)
                                                    ) {
                                                        Text(
                                                            "Send",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: Today's History - Stacks App-wise for the day!
                    val filteredHistory = remember(history, selectedCategoryFilter) {
                        if (selectedCategoryFilter ==
                            null
                        ) {
                            history
                        } else {
                            history.filter {
                                it.category ==
                                    selectedCategoryFilter
                            }
                        }
                    }

                    if (filteredHistory.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No notification history recorded today for this category.",
                                fontSize = 12.sp,
                                color = colors.outline
                            )
                        }
                    } else {
                        // App-wise stacks
                        val appStacks = remember(filteredHistory) {
                            filteredHistory.groupBy { it.appName }
                                .toList()
                                .sortedByDescending { it.second.size }
                        }

                        LazyColumn(
                            modifier = Modifier.heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${filteredHistory.size} notifications across ${appStacks.size} apps today",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = {
                                            NotificationService.clearHistory(context)
                                            Toast.makeText(
                                                context,
                                                "Today's history cleared",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        contentPadding = PaddingValues(
                                            horizontal = 4.dp,
                                            vertical = 0.dp
                                        )
                                    ) {
                                        Text(
                                            "Clear",
                                            fontSize = 11.sp,
                                            color = colors.error
                                        )
                                    }
                                }
                            }

                            items(appStacks, key = { it.first }) { (appName, items) ->
                                val samplePkg = items.firstOrNull()?.packageName ?: ""
                                val isExpanded = expandedApps[appName] ?: true // default expanded
                                val cat = items.firstOrNull()?.category ?: "Others"

                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = colors.surfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        // Stack Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    expandedApps[appName] = !isExpanded
                                                },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                AppThumbnailView(
                                                    packageName = samplePkg,
                                                    appName = appName,
                                                    style = thumbnailStyle,
                                                    size = 28
                                                )
                                                Column {
                                                    Row(
                                                        verticalAlignment =
                                                        Alignment.CenterVertically,
                                                        horizontalArrangement =
                                                        Arrangement.spacedBy(
                                                            6.dp
                                                        )
                                                    ) {
                                                        Text(
                                                            appName,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(
                                                                    colors.primary.copy(
                                                                        alpha = 0.12f
                                                                    )
                                                                )
                                                                .padding(
                                                                    horizontal = 5.dp,
                                                                    vertical = 1.dp
                                                                )
                                                        ) {
                                                            Text(
                                                                cat,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = colors.primary
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        "${items.size} ${if (items.size == 1) "notification" else "notifications"} today",
                                                        fontSize = 10.sp,
                                                        color = colors.outline
                                                    )
                                                }
                                            }

                                            Text(
                                                if (isExpanded) "▲" else "▼",
                                                fontSize = 12.sp,
                                                color = colors.primary
                                            )
                                        }

                                        // Stacked notification cards when expanded
                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            HorizontalDivider(
                                                color = colors.outlineVariant.copy(
                                                    alpha = 0.5f
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))

                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                items.forEachIndexed { idx, historyItem ->
                                                    if (idx > 0) {
                                                        HorizontalDivider(
                                                            color = colors.outlineVariant.copy(
                                                                alpha = 0.25f
                                                            )
                                                        )
                                                    }
                                                    val timeStr = SimpleDateFormat(
                                                        "hh:mm:ss a",
                                                        Locale.getDefault()
                                                    ).format(Date(historyItem.timestamp))
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 3.dp),
                                                        verticalArrangement = Arrangement.spacedBy(
                                                            2.dp
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement =
                                                            Arrangement.SpaceBetween,
                                                            verticalAlignment =
                                                            Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = historyItem.title.ifBlank {
                                                                    "Notification"
                                                                },
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = colors.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            Text(
                                                                text = timeStr,
                                                                fontSize = 10.sp,
                                                                color = colors.outline
                                                            )
                                                        }
                                                        if (historyItem.content.isNotBlank()) {
                                                            Text(
                                                                text = historyItem.content,
                                                                fontSize = 11.sp,
                                                                softWrap = true,
                                                                color =
                                                                colors.onSurfaceVariant.copy(
                                                                    alpha = 0.85f
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
