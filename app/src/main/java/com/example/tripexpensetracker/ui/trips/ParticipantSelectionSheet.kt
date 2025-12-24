package com.example.tripexpensetracker.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tripexpensetracker.data.model.Friend
import com.example.tripexpensetracker.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantSelectionSheet(
    onDismissRequest: () -> Unit,
    viewModel: AddEditTripViewModel
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredFriends by viewModel.filteredFriends.collectAsState()
    val globalResults by viewModel.userSearchResults.collectAsState()
    val suggestedContacts by viewModel.suggestedContacts.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermission by remember { 
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
             val contacts = com.example.tripexpensetracker.ui.common.ContactUtils.getAllContacts(context)
             viewModel.matchContacts(contacts)
        }
    }
    
    // Auto-sync if permission already granted
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
             val contacts = com.example.tripexpensetracker.ui.common.ContactUtils.getAllContacts(context)
             viewModel.matchContacts(contacts)
        }
    }

    var showManualEntry by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .heightIn(max = 600.dp) 
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Add People", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("Search friends, name, or phone") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (showManualEntry) {
                ManualEntryForm(
                    initialName = searchQuery,
                    onAdd = { name, phone -> 
                        viewModel.onAddManualParticipant(name, phone)
                        onDismissRequest() // Close after adding
                    },
                    onCancel = { showManualEntry = false }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 0. Permission / Sync Banner
                    if (!hasPermission) {
                        item {
                            Card(
                                onClick = { permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null) // Ideally use a "Contacts" icon
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Sync Contacts", style = MaterialTheme.typography.titleSmall)
                                        Text("Find friends already on the app", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    
                    // 0.5 Suggested Contacts (Matches)
                    if (suggestedContacts.isNotEmpty() && searchQuery.isEmpty()) {
                         item {
                            Text(
                                text = "Suggested from Contacts",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(suggestedContacts) { user ->
                            UserResultItem(user) {
                                viewModel.onAddUser(user)
                                onDismissRequest()
                            }
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // 1. Friends Section
                    if (filteredFriends.isNotEmpty()) {
                        item {
                            Text(
                                text = "Friends",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(filteredFriends) { friend ->
                            FriendResultItem(friend) {
                                viewModel.onAddFriend(friend)
                                onDismissRequest()
                            }
                        }
                    } else if (searchQuery.isEmpty() && suggestedContacts.isEmpty()) {
                        item {
                            Text("Start typing to search...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // 2. Global Results Section
                    if (globalResults.isNotEmpty()) {
                        item {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "Global Search Results",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(globalResults) { user ->
                            UserResultItem(user) {
                                viewModel.onAddUser(user)
                                onDismissRequest()
                            }
                        }
                    }

                    // Loading Indicator
                    if (isSearching) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    // Empty State / Fallback
                    if (searchQuery.isNotEmpty() && filteredFriends.isEmpty() && globalResults.isEmpty() && !isSearching) {
                        item {
                           Text(
                                text = "No matching friends or users found.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 16.dp)
                           )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manual Entry Button
                Button(
                    onClick = { showManualEntry = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Participant Manually")
                }
            }
        }
    }
}

@Composable
fun ManualEntryForm(
    initialName: String,
    onAdd: (String, String?) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .padding(bottom = 32.dp) // Extra padding for bottom sheet
    ) {
        Text("Add Manual Participant", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name *") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = { onAdd(name, phone) },
                enabled = name.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Add")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendResultItem(friend: Friend, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = friend.name, style = MaterialTheme.typography.titleMedium)
                if (!friend.phoneNumber.isNullOrBlank()) {
                    Text(text = friend.phoneNumber, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (friend.linkedUserId != null) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.CheckCircle, "App User", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserResultItem(user: User, onClick: () -> Unit) {
     Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // Different color to distinguish
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Use a specific icon or badge for "Global Search"
             Icon(Icons.Default.Person, null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = user.displayName ?: "Unknown", style = MaterialTheme.typography.titleMedium)
                Text(text = user.phone, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
