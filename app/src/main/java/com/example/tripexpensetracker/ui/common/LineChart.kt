package com.example.tripexpensetracker.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LineChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurface
) {
    if (data.isEmpty()) return

    val sortedData = data.toList().sortedBy { it.first } // Ensure data is sorted by date key
    val values = sortedData.map { it.second.toFloat() }
    val labels = sortedData.map { it.first }
    
    val maxVal = values.maxOrNull() ?: 100f
    
    Box(modifier = modifier.height(200.dp).padding(16.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // X-axis spacing
            val spaceX = width / (values.size - 1).coerceAtLeast(1)
            
            // Path
            val path = Path()
            
            values.forEachIndexed { index, value ->
                val x = index * spaceX
                val y = height - (value / maxVal * height)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
                
                // Draw points
                drawCircle(
                    color = lineColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            
            // Draw Line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Fill gradient below? Optional.
        }
        
        // Simple labels (First and Last date)
        if (labels.isNotEmpty()) {
            Text(
                text = labels.first(),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                modifier = Modifier.align(Alignment.BottomStart)
            )
            Text(
                text = labels.last(),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
