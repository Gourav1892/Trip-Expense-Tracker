package com.example.tripexpensetracker.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    radiusOuter: Dp = 90.dp,
    chartBarWidth: Dp = 20.dp,
    animDuration: Int = 1000
) {
    val totalSum = data.values.sum()
    val floatValue = mutableListOf<Float>()

    // To set the value of each Arc according to
    // the value given in the data, we have used a simple math
    // formula: (value / totalSum) * 360
    data.values.forEachIndexed { index, values ->
        floatValue.add(index, 360 * values.toFloat() / totalSum.toFloat())
    }

    // Default colors if not enough unique colors provided
    val colors = listOf(
        Color(0xFFE57373), // Red
        Color(0xFF81C784), // Green
        Color(0xFF64B5F6), // Blue
        Color(0xFFFFD54F), // Yellow
        Color(0xFFBA68C8), // Purple
        Color(0xFF4DB6AC), // Teal
        Color(0xFFFF8A65), // Deep Orange
        Color(0xFFA1887F)  // Brown
    )

    var lastValue = 0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) { 
        Box(
            modifier = Modifier.size(radiusOuter * 2),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(radiusOuter * 2f)
            ) {
                lastValue = -90f

                data.values.forEachIndexed { index, value ->
                    drawArc(
                        color = colors.getOrElse(index) { Color.Gray },
                        startAngle = lastValue,
                        sweepAngle = floatValue[index],
                        useCenter = false,
                        style = Stroke(chartBarWidth.toPx()),
                        size = Size(size.width, size.height) // Use implicit size of canvas for bounds? 
                        // Actually Stroke style draws centered on the outline. 
                        // It's cleaner to just fill the available size or define explicit bounds.
                        // Let's rely on Modifier size.
                    )
                    lastValue += floatValue[index]
                }
            }
            
            // Inner text showing total
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = java.text.NumberFormat.getCurrencyInstance().format(totalSum),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        val keys = data.keys.toList()
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            keys.forEachIndexed { index, key ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = colors.getOrElse(index) { Color.Gray })
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$key (${data[key]?.toInt()})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable // Quick shim for FlowRow if using older compose
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Basic shim implementation using existing FlowRow from Foundation if available in BOM 2024+
    // Since we reverted to 2023.08, FlowRow might be experimental or not available in foundation.layout.
    // We will use a simple wrapped Column/Row logic or just check if it exists.
    // Actually, let's use a simpler implementation: A vertically scrolling list of legends isn't great.
    // Let's use the official one if available:
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
