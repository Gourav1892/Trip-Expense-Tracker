package com.example.tripexpensetracker.ui.common

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/**
 * Confirmation dialog for ending a city visit
 */
@Composable
fun EndCityVisitDialog(
    cityName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("🏙️", style = MaterialTheme.typography.headlineMedium) },
        title = { 
            Text(
                "End Visit to $cityName?",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Text("You can always come back later. Any expenses and activities you've added will be saved.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("End Visit")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Stay")
            }
        }
    )
}
