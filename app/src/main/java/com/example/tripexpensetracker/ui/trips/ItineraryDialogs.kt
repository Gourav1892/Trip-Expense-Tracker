package com.example.tripexpensetracker.ui.trips

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.util.*
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDestinationDialog(
    initialName: String = "",
    initialStart: Long? = null,
    initialEnd: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    // Simple dates for MVP: Default to provided or current time
    val start = initialStart ?: System.currentTimeMillis()
    val end = initialEnd ?: (System.currentTimeMillis() + 86400000)

    val isEdit = initialName.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit City" else "Add Destination") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("City Name") },
                    singleLine = true
                )
                Text("Dates defaulted to today/tomorrow for MVP.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, start, end) },
                enabled = name.isNotBlank()
            ) { Text(if (isEdit) "Save" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityDialog(
    destinations: List<com.example.tripexpensetracker.data.model.Destination>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(com.example.tripexpensetracker.data.model.ItineraryItem.TYPE_ACTIVITY) }
    
    // Default time
    val time = System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Activity") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Activity Title") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onConfirm(title, description, time, type) },
                enabled = title.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
