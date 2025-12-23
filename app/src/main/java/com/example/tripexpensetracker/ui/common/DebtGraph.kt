package com.example.tripexpensetracker.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.tripexpensetracker.domain.Debt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.atan2

@Composable
fun DebtGraph(
    debts: List<Debt>,
    personNames: Map<String, String>,
    modifier: Modifier = Modifier
) {
    // Text measurement handling

    // Actually, rememberTextMeasurer is available in newer Compose. 
    // Since we are on 2023.08 BOM / Compose 1.5+, drawing text on Canvas is easier with nativeCanvas for now to be safe against version mismatches, 
    // OR just use basic Composables if possible.
    // Drawing nodes as Composables in a Box is easier than pure Canvas for text.
    // But lines need Canvas.
    // Let's use a hybrid approach: Canvas for lines, Box with offsets for Nodes.
    // BUT to keep it contained in one Composable, pure Canvas is powerful. 
    // Let's stick to Node placement logic.

    // 1. Identify all unique people involved in debts
    val peopleIds = (debts.map { it.fromPersonId } + debts.map { it.toPersonId }).distinct()
    if (peopleIds.isEmpty()) return

    val density = LocalDensity.current
    val nodeRadius = 20.dp
    val graphRadius = 120.dp
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val arrowColor = MaterialTheme.colorScheme.error

    Box(modifier = modifier.height((graphRadius * 2) + (nodeRadius * 4))) {
        Canvas(modifier = Modifier.fillMaxWidth().height((graphRadius * 2) + (nodeRadius * 4))) {
            val center = Offset(size.width / 2, size.height / 2)
            val radiusPx = graphRadius.toPx()
            val nodeRadiusPx = nodeRadius.toPx()

            // Calculate positions
            val angleStep = (2 * PI) / peopleIds.size
            val nodePositions = peopleIds.mapIndexed { index, id ->
                val angle = index * angleStep - (PI / 2) // Start from top
                val x = center.x + radiusPx * cos(angle).toFloat()
                val y = center.y + radiusPx * sin(angle).toFloat()
                id to Offset(x, y)
            }.toMap()

            // Draw Edges (Arrows)
            debts.forEach { debt ->
                val start = nodePositions[debt.fromPersonId] ?: return@forEach
                val end = nodePositions[debt.toPersonId] ?: return@forEach

                // Adjust start/end to be at circle edge, not center
                val angle = atan2(end.y - start.y, end.x - start.x)
                val startAdjusted = Offset(
                    start.x + nodeRadiusPx * cos(angle).toFloat(),
                    start.y + nodeRadiusPx * sin(angle).toFloat()
                )
                val endAdjusted = Offset(
                    end.x - nodeRadiusPx * cos(angle).toFloat(),
                    end.y - nodeRadiusPx * sin(angle).toFloat()
                )

                drawLine(
                    color = arrowColor,
                    start = startAdjusted,
                    end = endAdjusted,
                    strokeWidth = 3.dp.toPx()
                )

                // Draw Arrowhead
                val arrowSize = 10.dp.toPx()
                val arrowPath = Path().apply {
                    moveTo(endAdjusted.x, endAdjusted.y)
                    lineTo(endAdjusted.x - arrowSize, endAdjusted.y - arrowSize / 2)
                    lineTo(endAdjusted.x - arrowSize, endAdjusted.y + arrowSize / 2)
                    close()
                }
                rotate(degrees = Math.toDegrees(angle.toDouble()).toFloat(), pivot = endAdjusted) {
                    drawPath(arrowPath, arrowColor)
                }

                // Draw Amount Label (Middle of line)
                val midPoint = Offset(
                    (startAdjusted.x + endAdjusted.x) / 2,
                    (startAdjusted.y + endAdjusted.y) / 2
                )
                // We'll skip complex text drawing on canvas for now to avoid complexity or use native
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 30f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    drawText("$${debt.amount.toInt()}", midPoint.x, midPoint.y - 10, paint)
                }
            }

            // Draw Nodes
            nodePositions.forEach { (id, pos) ->
                drawCircle(
                    color = primaryColor,
                    radius = nodeRadiusPx,
                    center = pos
                )
                
                // Draw Initials
                val name = personNames[id] ?: "?"
                val initial = name.firstOrNull()?.toString()?.uppercase() ?: "?"
                
                drawContext.canvas.nativeCanvas.apply {
                     val paint = android.graphics.Paint().apply {
                        color = onPrimaryColor.toArgb()
                        textSize = 40f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    // Approx vertical centering
                    drawText(initial, pos.x, pos.y + 15, paint)
                }
                
                // Draw Name below
                drawContext.canvas.nativeCanvas.apply {
                     val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY // Safe color
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawText(name, pos.x, pos.y + nodeRadiusPx + 30, paint)
                }
            }
        }
    }
}
