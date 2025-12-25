package com.example.tripexpensetracker.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.tripexpensetracker.data.model.Expense
import com.example.tripexpensetracker.data.model.ExpenseShare
import com.example.tripexpensetracker.data.model.Person
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ExpenseDetailsDialog(
    expense: Expense,
    shares: List<ExpenseShare>,
    people: List<Person>, // To Map IDs to Names
    currencySymbol: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(expense.title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Info
                Column {
                    Text("Total Amount", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "$currencySymbol${String.format("%.2f", expense.amount)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Category", style = MaterialTheme.typography.labelMedium)
                        Text(expense.category, style = MaterialTheme.typography.bodyLarge)
                    }
                    Column {
                        Text("Date", style = MaterialTheme.typography.labelMedium)
                        Text(dateFormatter.format(expense.date), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Divider()

                // Paid By
                val payerName = people.find { it.id == expense.paidByPersonId }?.name ?: "Unknown"
                Column {
                    Text("Paid By", style = MaterialTheme.typography.labelMedium)
                    Text(payerName, style = MaterialTheme.typography.bodyLarge)
                }

                Divider()

                // Split Details
                Text("Shared With", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (shares.isEmpty()) {
                        Text("Loading shares...", style = MaterialTheme.typography.bodySmall)
                    } else {
                        shares.forEach { share ->
                            val personName = people.find { it.id == share.personId }?.name ?: "Unknown"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(personName, style = MaterialTheme.typography.bodyMedium)
                                Text("$currencySymbol${String.format("%.2f", share.amountOwed)}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onEdit) {
                Text("Edit Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
