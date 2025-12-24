package com.example.tripexpensetracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CategoryIcon(category: String, modifier: Modifier = Modifier) {
    val (icon, color) = when (category) {
        "Food", "Dinner", "Lunch" -> Pair(Icons.Default.Restaurant, Color(0xFF4CAF50)) // Green
        "Transport", "Uber", "Taxi" -> Pair(Icons.Default.DirectionsCar, Color(0xFF2196F3)) // Blue
        "Hotel", "Lodging" -> Pair(Icons.Default.Hotel, Color(0xFF9C27B0)) // Purple
        "Entertainment", "Movie" -> Pair(Icons.Default.Movie, Color(0xFFFF9800)) // Orange
        "Shopping" -> Pair(Icons.Default.ShoppingCart, Color(0xFFE91E63)) // Pink
        else -> Pair(Icons.Default.ShoppingCart, Color.Gray) // Default
    }

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.2f), CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}
