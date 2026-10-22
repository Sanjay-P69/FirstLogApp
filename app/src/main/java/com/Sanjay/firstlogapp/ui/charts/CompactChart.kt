package com.Sanjay.firstlogapp.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Sanjay.firstlogapp.R
import androidx.compose.ui.res.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

/** One plot fits the entire selected range; a native slider exposes exact values accessibly. */
@Composable
fun CompactChart(
    points: List<ChartPoint>,
    description: String,
    axisValue: (Double) -> String,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.BARS,
    fixedMaximum: Double? = null
) {
    if (points.isEmpty()) return
    var selected by remember(points) {
        mutableIntStateOf(points.indexOfLast { it.value != null }.coerceAtLeast(0))
    }
    val index = selected.coerceIn(points.indices)
    val maximum = fixedMaximum ?: chartMaximum(points.map { it.value })
    val colors = MaterialTheme.colorScheme
    val selectedLabel = stringResource(R.string.chart_selection)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                Modifier.width(42.dp).height(168.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(axisValue(maximum), style = MaterialTheme.typography.labelSmall)
                Text(axisValue(maximum / 2), style = MaterialTheme.typography.labelSmall)
                Text(axisValue(0.0), style = MaterialTheme.typography.labelSmall)
            }
            Column(Modifier.weight(1f)) {
                Canvas(
                    Modifier.fillMaxWidth().height(168.dp)
                        .semantics { contentDescription = description }
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                val fraction = offset.x / size.width
                                selected = points.indices.minBy { candidate ->
                                    abs((points[candidate].position ?:
                                        ((candidate + 0.5f) / points.size)) - fraction)
                                }
                            }
                        }
                ) {
                    val plotHeight = size.height - 4.dp.toPx()
                    val slot = size.width / points.size
                    listOf(0f, 0.5f, 1f).forEach { fraction ->
                        val y = plotHeight * fraction
                        drawLine(colors.outlineVariant, Offset(0f, y), Offset(size.width, y))
                    }
                    if (style == ChartStyle.BARS) {
                        points.forEachIndexed { pointIndex, point ->
                            val x = slot * pointIndex
                            val value = point.value
                            if (value != null && value > 0) {
                                val height = (value / maximum).coerceIn(0.0, 1.0).toFloat() *
                                    plotHeight
                                drawRect(
                                    if (pointIndex == index) colors.primary else colors.primaryContainer,
                                    Offset(x + slot * 0.15f, plotHeight - height),
                                    Size(slot * 0.7f, height)
                                )
                            } else if (point.state == ChartPointState.NO_RECORD ||
                                point.state == ChartPointState.FUTURE) {
                                drawLine(colors.outlineVariant,
                                    Offset(x + slot * 0.3f, plotHeight + 3.dp.toPx()),
                                    Offset(x + slot * 0.7f, plotHeight + 3.dp.toPx()))
                            }
                        }
                    } else {
                        val path = Path()
                        var continuing = false
                        points.forEachIndexed { pointIndex, point ->
                            val value = point.value
                            if (value == null) {
                                continuing = false
                            } else {
                                val x = (point.position ?: ((pointIndex + 0.5f) / points.size)) *
                                    size.width
                                val y = plotHeight * (1 - value / maximum).toFloat()
                                if (continuing) path.lineTo(x, y) else path.moveTo(x, y)
                                continuing = true
                                drawCircle(colors.primary, if (pointIndex == index) 5.dp.toPx()
                                    else 2.dp.toPx(), Offset(x, y))
                            }
                        }
                        drawPath(path, colors.primary,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    }
                }
                BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min = 28.dp)) {
                    tickIndices(points.size).forEach { tick ->
                        val fraction = points[tick].position ?: ((tick + 0.5f) / points.size)
                        val x = (maxWidth * fraction - 22.dp).coerceIn(0.dp,
                            (maxWidth - 44.dp).coerceAtLeast(0.dp))
                        Text(points[tick].label,
                            Modifier.offset(x = x).width(44.dp),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Text(points[index].description, style = MaterialTheme.typography.titleSmall)
        Text(points[index].valueLabel, style = MaterialTheme.typography.bodyLarge)
        if (points.size > 1) {
            Slider(
                value = index.toFloat(),
                onValueChange = { selected = it.roundToInt().coerceIn(points.indices) },
                valueRange = 0f..points.lastIndex.toFloat(),
                steps = (points.size - 2).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = selectedLabel
                    stateDescription = "${points[index].description}, ${points[index].valueLabel}"
                }
            )
        }
    }
}
