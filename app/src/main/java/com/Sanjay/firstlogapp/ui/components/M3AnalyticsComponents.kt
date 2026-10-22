package com.Sanjay.firstlogapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Sanjay.firstlogapp.ui.theme.MintSage
import com.Sanjay.firstlogapp.ui.theme.SoftIndigoPrimary
import com.Sanjay.firstlogapp.ui.theme.WarmCoral
import java.util.Locale

/**
 * Modern M3 Timeframe Selector using SingleChoiceSegmentedButtonRow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3TimeframeSegmentedRow(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selectedOption == option,
                onClick = { onOptionSelected(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selectedOption == option) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Standardized App Usage / Battery Drain List Item adhering to M3 Best-of-Year polish:
 * - 40dp x 40dp rounded squircle app icon with 4dp elevation
 * - Flat list item separated by 8dp spacing (no heavy double borders)
 * - M3 LinearProgressIndicator with rounded caps and proportional impact color
 */
@Composable
fun M3AppUsageListItem(
    appName: String,
    packageName: String,
    durationText: String,
    drainPercentage: Double,
    progress: Float,
    iconComposable: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Proportional impact color: Mint Sage (< 15%), Soft Indigo (15% - 40%), Warm Coral (> 40%)
    val barColor = when {
        drainPercentage > 35.0 -> WarmCoral
        drainPercentage > 15.0 -> SoftIndigoPrimary
        else -> MintSage
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Standardized 40dp squircle with subtle 4dp shadow
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(2.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        iconComposable()
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", drainPercentage)}% battery",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = barColor
                    )
                }
            }

            // Smooth M3 LinearProgressIndicator with rounded caps
            val animatedProgress by animateFloatAsState(
                targetValue = progress.coerceIn(0.04f, 1f),
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "appProgress"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

/**
 * Modern M3 Activity Bar Chart:
 * - Subtle rounded top corners on bars
 * - Fixed interval horizontal time labels (12 AM, 6 AM, 12 PM, 6 PM) to eliminate overlapping
 * - Tap-to-inspect tooltips displaying exact values
 */
@Composable
fun M3ActivityBarChart(
    chartPoints: List<Pair<String, Float>>,
    filterMode: String,
    modifier: Modifier = Modifier
) {
    var selectedBarIndex by remember { mutableStateOf(-1) }
    val maxVal = maxOf(chartPoints.maxOfOrNull { it.second } ?: 1f, 0.01f)
    val scrollState = rememberScrollState()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Selected Tooltip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Tap-to-inspect tooltip
                AnimatedVisibility(
                    visible = selectedBarIndex in chartPoints.indices,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (selectedBarIndex in chartPoints.indices) {
                        val (label, value) = chartPoints[selectedBarIndex]
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = "$label: ${String.format(Locale.getDefault(), "%.1f", value)}h",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            val colWidth = if (filterMode == "Day") 44.dp else 36.dp

            // Horizontally Scrollable Bars
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(130.dp)
                ) {
                    chartPoints.forEachIndexed { index, (label, value) ->
                        val isSelected = selectedBarIndex == index
                        val ratio = (value / maxVal).coerceIn(0.04f, 1f)
                        val animatedRatio by animateFloatAsState(
                            targetValue = ratio,
                            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                            label = "barRatio_$index"
                        )

                        val barColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .width(colWidth)
                                .fillMaxHeight()
                                .clickable { selectedBarIndex = if (isSelected) -1 else index }
                        ) {
                            // Animated vertical bar with rounded top corners
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((animatedRatio * 90).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(barColor)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // X-Axis Scale Labels (Fixed interval readability)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    chartPoints.forEachIndexed { index, (label, _) ->
                        val isSelected = selectedBarIndex == index
                        Box(
                            modifier = Modifier
                                .width(colWidth)
                                .clickable { selectedBarIndex = if (isSelected) -1 else index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
