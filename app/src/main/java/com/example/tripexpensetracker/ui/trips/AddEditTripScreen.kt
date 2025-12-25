package com.example.tripexpensetracker.ui.trips

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.tripexpensetracker.data.model.Participant
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTripScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTripDetails: (String) -> Unit,
    viewModel: AddEditTripViewModel = hiltViewModel()
) {
    val tripName by viewModel.tripName.collectAsState()
    val budget by viewModel.budget.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val alertThreshold by viewModel.alertThreshold.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    
    var currencyExpanded by remember { mutableStateOf(false) }

    var showFriendSelector by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Contact Picker Logic
    val contactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { contactUri ->
            resolveContact(context, contactUri) { name, phone ->
                if (participants.none { it.name == name && it.phoneNumber == phone }) {
                     val newParticipant = Participant(name = name, phoneNumber = phone)
                     // Check if this contact is an App User
                     viewModel.checkUserRegistration(newParticipant) { updatedParticipant ->
                         val index = participants.indexOf(newParticipant)
                         if (index >= 0) {
                             viewModel.onUpdateParticipant(index, updatedParticipant)
                         } else {
                             // If not in list (first time), just add it.
                             // But wait, the callback is async. If we add it first, we can find it.
                         }
                     }
                     // Add immediately, then update when check returns
                     viewModel.onAddParticipant(newParticipant)
                } else {
                     scope.launch { snackbarHostState.showSnackbar("Participant already added") }
                }
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            contactLauncher.launch(null)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Permission denied. Cannot add from contacts.") }
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AddEditTripViewModel.UiState.Error) {
            snackbarHostState.showSnackbar((uiState as AddEditTripViewModel.UiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.tripId != null) "Edit Trip" else "Add Trip") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState is AddEditTripViewModel.UiState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } 
        
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = tripName,
                enabled = uiState !is AddEditTripViewModel.UiState.Loading,
                onValueChange = { viewModel.onTripNameChanged(it) },
                label = { Text("Trip Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = budget,
                enabled = uiState !is AddEditTripViewModel.UiState.Loading,
                onValueChange = { viewModel.onBudgetChanged(it) },
                label = { Text("Total Budget (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Currency Picker
            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { currencyExpanded = !currencyExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "Currency: $currencyCode",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Trip Currency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = currencyExpanded,
                    onDismissRequest = { currencyExpanded = false }
                ) {
                    listOf("INR", "USD", "EUR", "GBP", "JPY", "AED").forEach { code ->
                        DropdownMenuItem(
                            text = { Text(code) },
                            onClick = {
                                viewModel.onCurrencyChanged(code)
                                currencyExpanded = false
                            }
                        )
                    }
                }
            }

            if (budget.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Alert me at ${(alertThreshold).toInt()}% of budget",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = alertThreshold,
                    onValueChange = { viewModel.onAlertThresholdChanged(it) },
                    valueRange = 50f..100f,
                    steps = 9 // 50, 55, 60... 100? No, steps count. 50 to 100. 
                    // Let's just make it smooth or 5% increments.
                    // 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100 = 11 points = 10 steps.
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text("Participants", style = MaterialTheme.typography.titleMedium)
            
            // Unified "Add People" Button
            Button(
                onClick = { showFriendSelector = true },
                enabled = uiState !is AddEditTripViewModel.UiState.Loading,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add People")
            }

            if (showFriendSelector) {
                ParticipantSelectionSheet(
                    onDismissRequest = { showFriendSelector = false },
                    viewModel = viewModel
                )
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(participants) { index, participant ->
                    val isPending = participant.status == com.example.tripexpensetracker.data.model.Participant.STATUS_INVITED
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPending) 
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = participant.name, 
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isPending) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isPending) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        androidx.compose.material3.Badge(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        ) {
                                            Text("Pending", style = MaterialTheme.typography.labelSmall)
                                        }
                                    } else if (participant.userId != null) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "App User",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (!participant.phoneNumber.isNullOrBlank()) {
                                    Text(text = participant.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(
                                enabled = uiState !is AddEditTripViewModel.UiState.Loading,
                                onClick = { viewModel.onRemoveParticipant(participant) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (tripName.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please enter a trip name") }
                        return@Button
                    }
                    if (participants.isEmpty()) {
                        scope.launch { snackbarHostState.showSnackbar("Please add at least one participant") }
                        return@Button
                    }
                    viewModel.saveTrip {
                        onNavigateBack()
                    }
                },
                enabled = uiState !is AddEditTripViewModel.UiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is AddEditTripViewModel.UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Trip")
                }
            }
        }
    }
}

fun resolveContact(context: Context, contactUri: Uri, onResult: (String, String?) -> Unit) {
    val cursor = context.contentResolver.query(contactUri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val name = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
            
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            val contactId = if (idIndex >= 0) it.getString(idIndex) else null
            
            var phoneNumber: String? = null
            
            val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
            val hasPhone = if (hasPhoneIndex >= 0) it.getString(hasPhoneIndex) else "0"
            
            if (hasPhone == "1" && contactId != null) {
                val phones = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                    null,
                    null
                )
                phones?.use { pCursor ->
                    if (pCursor.moveToFirst()) {
                        val numberIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numberIndex >= 0) {
                            phoneNumber = pCursor.getString(numberIndex)
                            // Basic normalization: remove spaces, dashes
                            phoneNumber = phoneNumber?.replace(Regex("[^0-9+]"), "")
                        }
                    }
                }
            }
            onResult(name, phoneNumber)
        }
    }
}
