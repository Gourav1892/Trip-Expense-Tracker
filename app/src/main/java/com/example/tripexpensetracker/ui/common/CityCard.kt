package com.example.tripexpensetracker.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tripexpensetracker.data.model.CityStats
import com.example.tripexpensetracker.data.model.Destination
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CityCard(
    destination: Destination,
    stats: CityStats,
    isActive: Boolean = false,
    isLocked: Boolean = false,
    activeCityName: String? = null,
    currencySymbol: String = "₹",
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val cardAlpha = if (isLocked) 0.5f else 1f
    val containerColor = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer
        isLocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .alpha(cardAlpha)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column {
            // Active/Locked badge
            if (isActive || isLocked) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isActive) {
                            Text(
                                "📍 CURRENTLY VISITING",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Locked",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onError
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "End ${activeCityName ?: "current"} visit first",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // City icon/emoji
                Text(
                    if (isLocked) "🔒" else "🏙️",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(end = 16.dp)
                )
                
                // City info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        destination.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Date range
                    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    val startDate = if (destination.startDate > 0) Date(destination.startDate) else null
                    val endDate = if (destination.endDate > 0) Date(destination.endDate) else null
                    
                    if (startDate != null) {
                        val startStr = dateFormat.format(startDate)
                        val dateText = if (endDate != null) {
                             if (dateFormat.format(startDate) == dateFormat.format(endDate)) {
                                 startStr // Same day
                             } else {
                                 "$startStr - ${dateFormat.format(endDate)}"
                             }
                        } else {
                            if (isActive) "Started $startStr" else startStr
                        }
                        
                        Text(
                            dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Fallback to stats if destination dates aren't set (legacy data)
                         stats.dateRange.let { (s, e) ->
                            if (s != null && e != null) {
                                val rangeText = if (dateFormat.format(s) == dateFormat.format(e)) {
                                    dateFormat.format(s)
                                } else {
                                    "${dateFormat.format(s)} - ${dateFormat.format(e)}"
                                }
                                Text(rangeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    // Quick stats
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (stats.expenseCount > 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💸", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    "${stats.expenseCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (stats.activityCount > 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📍", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    "${stats.activityCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Total spending + arrow
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "${currencySymbol}${String.format("%.2f", stats.totalExpenses)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                    )
                    if (stats.topCategory.isNotBlank()) {
                        Text(
                            stats.topCategory,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Menu
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.MoreVert,
                            contentDescription = "City Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            },
                            leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }

                Icon(
                    if (isLocked) Icons.Default.Lock else Icons.Default.ChevronRight,
                    contentDescription = if (isLocked) "Locked" else "Open city",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
