package com.example.tripexpensetracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat

@Composable
fun BarChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    currencySymbol: String = "₹"
) {
    val maxValue = data.values.maxOrNull() ?: 1.0
    val sortedData = data.toList().sortedByDescending { it.second }

    Column(modifier = modifier) {
        sortedData.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label (Name)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(100.dp),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                // Bar and Value
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fillFraction = if (maxValue > 0) (value / maxValue).toFloat() else 0f
                    
                    Box(
                        modifier = Modifier
                            .height(12.dp)
                            .weight(fillFraction.coerceAtLeast(0.01f)) // Ensure at least tiny bar
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                    
                    // Empty space filler
                    if (fillFraction < 1f) {
                        Spacer(modifier = Modifier.weight(1f - fillFraction))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Value Text
                Text(
                    text = "${currencySymbol}${String.format("%.2f", value)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
