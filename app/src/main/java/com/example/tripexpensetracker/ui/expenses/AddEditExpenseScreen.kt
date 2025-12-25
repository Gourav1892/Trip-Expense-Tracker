package com.example.tripexpensetracker.ui.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripexpensetracker.data.model.Person
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    tripId: String,
    destinationId: String? = null, // Pre-selected city from navigation
    onNavigateBack: () -> Unit,
    viewModel: AddEditExpenseViewModel = hiltViewModel()
) {
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val suggestedDestinationId by viewModel.suggestedDestinationId.collectAsState()
    // LaunchedEffect removed as ViewModel handles init via SavedStateHandle

    val people by viewModel.people.collectAsState(initial = emptyList())
    val destinations by viewModel.destinations.collectAsState(initial = emptyList())
    val editState by viewModel.editState.collectAsState()

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    val category by viewModel.category.collectAsState()
    var selectedPayer by remember { mutableStateOf<Person?>(null) }

    var expanded by remember { mutableStateOf(false) }
    
    // City selection
    var selectedDestination by remember { mutableStateOf<com.example.tripexpensetracker.data.model.Destination?>(null) }
    var cityExpanded by remember { mutableStateOf(false) }
    
    // Pre-select city when loading from navigation or if suggested by active visit
    // Pre-select city when loading from navigation or if suggested by active visit OR edit state
    LaunchedEffect(destinationId, suggestedDestinationId, destinations, editState) {
        if (destinations.isNotEmpty() && selectedDestination == null) {
            val targetId = editState?.destinationId ?: destinationId ?: suggestedDestinationId
            if (targetId != null) {
                selectedDestination = destinations.find { it.id == targetId }
            }
        }
    }

    var splitType by remember { mutableStateOf(SplitType.EQUAL) }
    // Map of PersonId to Amount String
    var shareAmounts by remember { mutableStateOf(mapOf<String, String>()) }
    // Re-implementing with state selection inside the composable for simplicity
    var selectedForSplit by remember { mutableStateOf(setOf<String>()) }

    // Initialize shares when people load
    LaunchedEffect(people) {
        if (shareAmounts.isEmpty() && people.isNotEmpty()) {
            shareAmounts = people.associate { it.id to "" }
        }
    }

    // Initialize selection when people load (select all by default)
    // Initialize selection when people load (select all by default if NEW expense)
    LaunchedEffect(people, editState) {
        if (selectedForSplit.isEmpty() && people.isNotEmpty() && shareAmounts.isEmpty() && editState == null) {
            // Only default select all if first load AND creating new expense
            selectedForSplit = people.map { it.id }.toSet()
        }
    }

    // Load Edit State
    LaunchedEffect(editState, people) {
        val state = editState
        if (state != null && people.isNotEmpty() && title.isEmpty()) { // Only load once/if empty
             title = state.title
             amount = state.amount.toString()
             
             // Payer
             selectedPayer = people.find { it.id == state.paidBy }
             
             // Content: Shares
             // We need to infer SplitType?
             // Simplest is to assume UNEQUAL if shares exist with arbitrary amounts, or we can check logic?
             // Logic:
             // If all shares equal amount/count -> Equal?
             // If shares have exact amounts -> Unequal.
             // If shares have % -> Percentage.
             // Currently Model `ExpenseShare` only has `amountOwed`. It does NOT store the original split type or %/share_count.
             // This is a limitation. We can only restore as "Fixed Amounts" (UNEQUAL) or try to guess.
             // BEST EFFORT: Load as UNEQUAL (Exact Amounts) because that is the source of truth.
             // This preserves correctness even if user originally used % or Equal.
             
             splitType = SplitType.UNEQUAL
             
             val newShareAmounts = mutableMapOf<String, String>()
             val newSelected = mutableSetOf<String>()
             
             state.shares.forEach { share ->
                 newSelected.add(share.personId)
                 newShareAmounts[share.personId] = share.amountOwed.toString()
             }
             
             // For people NOT in shares, they are unselected
             selectedForSplit = newSelected
             shareAmounts = newShareAmounts
        }
    }


    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AddEditExpenseViewModel.UiState.Error) {
            snackbarHostState.showSnackbar((uiState as AddEditExpenseViewModel.UiState.Error).message)
        }
    }

    val isFormEnabled = people.isNotEmpty() && uiState !is AddEditExpenseViewModel.UiState.Loading


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            val amountValue = amount.toDoubleOrNull()
                            if (title.isBlank() || amountValue == null || selectedPayer == null) {
                                scope.launch { snackbarHostState.showSnackbar("Please fill description, amount and payer") }
                                return@Button
                            }

                            if (selectedForSplit.isEmpty()) {
                                 scope.launch { snackbarHostState.showSnackbar("Please select at least one person to split with") }
                                 return@Button
                            }

                            val sharesMap = mutableMapOf<String, Double>()
                            if (splitType != SplitType.EQUAL) {
                                var currentTotal = 0.0
                                shareAmounts.forEach { (id, value) ->
                                     val v = value.toDoubleOrNull() ?: 0.0
                                     if (selectedForSplit.contains(id)) {
                                         sharesMap[id] = v
                                         currentTotal += v
                                     }
                                }

                                if (splitType == SplitType.UNEQUAL) {
                                    if (kotlin.math.abs(amountValue - currentTotal) > 0.01) {
                                          scope.launch { snackbarHostState.showSnackbar("Split amounts must equal total amount ($amountValue)") }
                                          return@Button
                                    }
                                } else if (splitType == SplitType.PERCENTAGE) {
                                     if (kotlin.math.abs(100.0 - currentTotal) > 0.01) {
                                          scope.launch { snackbarHostState.showSnackbar("Percentages must equal 100%") }
                                          return@Button
                                    }
                                } else if (splitType == SplitType.SHARES) {
                                     if (currentTotal <= 0) {
                                          scope.launch { snackbarHostState.showSnackbar("Total shares must be greater than 0") }
                                          return@Button
                                     }
                                }
                            }

                            viewModel.saveExpense(
                                tripId = tripId, 
                                title = title, 
                                amount = amountValue, 
                                paidByPersonId = selectedPayer!!.id, 
                                splitType = splitType, 
                                shares = sharesMap, 
                                selectedPersonIds = selectedForSplit, 

                                destinationId = selectedDestination?.id ?: ""
                            ) {
                                onNavigateBack()
                            }
                        },
                        enabled = isFormEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState is AddEditExpenseViewModel.UiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Save Expense")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (uiState is AddEditExpenseViewModel.UiState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ... (Existing Empty State Logic)
            if (people.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No participants found. Please add people to the trip before adding expenses.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            


            OutlinedTextField(
                value = title,
                enabled = isFormEnabled,

                onValueChange = { 
                    title = it
                    viewModel.onTitleChanged(it)
                },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amount,
                enabled = isFormEnabled,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                label = { Text("Amount ($currencySymbol)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ExposedDropdownMenuBox implementation
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val payerDisplayName = selectedPayer?.name?.ifBlank { selectedPayer?.phoneNumber } ?: "Select Payer"
                OutlinedTextField(
                    value = if (people.isEmpty()) "Loading..." else payerDisplayName,
                    enabled = people.isNotEmpty() && isFormEnabled,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Paid By") },
                    trailingIcon = { 
                        if (people.isEmpty()) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(), 
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expanded && people.isNotEmpty(),
                    onDismissRequest = { expanded = false }
                ) {
                    people.forEach { person ->
                        DropdownMenuItem(
                            text = { Text(person.name.ifBlank { person.phoneNumber ?: "Unknown" }) },
                            onClick = {
                                selectedPayer = person
                                expanded = false
                            }
                        )
                    }
                }
            }

            // ... (Rest of UI)
            
            // Note: I will need to replace the Button logic at the end specifically.
            // But since I am replacing a chunk, I must be careful.
            // The file is huge. I should do targeted replaces.
            
            // Let's cancel this big replace and do targeted ones.
            // Reason: The previous viewing didn't show the whole file, so I risk overwriting unseen parts incorrectly if I guess.
            // I need to be precise.


            Text(
                text = "Debug: Loaded ${people.size} people",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // City/Destination Selector
            if (destinations.isNotEmpty()) {
                Text("City/Destination:", style = MaterialTheme.typography.labelLarge)
                ExposedDropdownMenuBox(
                    expanded = cityExpanded,
                    onExpandedChange = { cityExpanded = !cityExpanded },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedDestination?.name ?: "Select City (Optional)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("City") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = cityExpanded,
                        onDismissRequest = { cityExpanded = false }
                    ) {
                        // Option to clear selection
                        DropdownMenuItem(
                            text = { Text("No City (General)") },
                            onClick = {
                                selectedDestination = null
                                cityExpanded = false
                            }
                        )
                        destinations.forEach { dest ->
                            DropdownMenuItem(
                                text = { Text(dest.name) },
                                onClick = {
                                    selectedDestination = dest
                                    cityExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Categories
            Text("Category:", style = MaterialTheme.typography.labelLarge)
            val categories = listOf("General", "Food", "Transport", "Lodging", "Entertainment")
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                 items(categories.size) { index ->
                     val cat = categories[index]
                     FilterChip(
                         selected = category == cat,
                         onClick = { viewModel.onCategorySelected(cat) },
                         label = { Text(cat) }
                     )
                 }
            }



            // Split Type Selector
            Text("Split Method:", style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val splitTypes = listOf(SplitType.EQUAL, SplitType.UNEQUAL, SplitType.PERCENTAGE, SplitType.SHARES)
                splitTypes.forEach { type ->
                    val label = when(type) {
                        SplitType.EQUAL -> "Equal"
                        SplitType.UNEQUAL -> "Amount"
                        SplitType.PERCENTAGE -> "%"
                        SplitType.SHARES -> "Shares"
                    }
                    FilterChip(
                        selected = splitType == type,
                        onClick = {
                            splitType = type
                            // Reset amounts when switching types to avoid confusion, or keep?
                            // Better to reset to clear slate
                            shareAmounts = people.associate { it.id to "" }
                        },
                        label = { Text(label) }
                    )
                }
            }



            Spacer(modifier = Modifier.height(16.dp))

            // Select All / Deselect All Row
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable {
                     if (selectedForSplit.size == people.size) {
                         selectedForSplit = emptySet()
                         shareAmounts = emptyMap()
                     } else {
                         selectedForSplit = people.map { it.id }.toSet()
                         // For unequal, we might want to preserve amounts, but simple reset is fine or keep logic simple.
                     }
                }
            ) {
                Checkbox(
                    checked = selectedForSplit.size == people.size && people.isNotEmpty(),
                    onCheckedChange = { checked ->
                        if (checked) {
                            selectedForSplit = people.map { it.id }.toSet()
                        } else {
                            selectedForSplit = emptySet()
                            shareAmounts = emptyMap()
                        }
                    }
                )
                Text("Select All", style = MaterialTheme.typography.bodyMedium)
            }

            Text("Select People:", style = MaterialTheme.typography.titleSmall)

             people.forEach { person ->
                val isSelected = selectedForSplit.contains(person.id)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                         selectedForSplit = if (isSelected) selectedForSplit - person.id else selectedForSplit + person.id

                         if (isSelected) {
                             // If deselecting, remove amount
                             shareAmounts = shareAmounts.toMutableMap().apply { remove(person.id) }
                         }
                    },
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                             selectedForSplit = if (checked) selectedForSplit + person.id else selectedForSplit - person.id
                             if (!checked) {
                                 shareAmounts = shareAmounts.toMutableMap().apply { remove(person.id) }
                             }
                        }
                    )
                    Text(person.name.ifBlank { person.phoneNumber ?: "Unknown" }, modifier = Modifier.weight(1f))

                    if (splitType != SplitType.EQUAL && isSelected) {
                        val label = when(splitType) {
                            SplitType.PERCENTAGE -> "%"
                            SplitType.SHARES -> "Shares"
                            else -> "Amount"
                        }
                        OutlinedTextField(
                            value = shareAmounts[person.id] ?: "",
                            onValueChange = { newValue ->
                                if (newValue.all { char -> char.isDigit() || char == '.' }) {
                                    shareAmounts = shareAmounts.toMutableMap().apply { put(person.id, newValue) }
                                }
                            },
                            label = { Text(label) },
                            modifier = Modifier.width(100.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                }
            }

            if (splitType != SplitType.EQUAL) {
                val currentValues = shareAmounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }

                when (splitType) {
                    SplitType.UNEQUAL -> {
                        val targetAmount = amount.toDoubleOrNull() ?: 0.0
                        val diff = targetAmount - currentValues
                        val color = if (kotlin.math.abs(diff) < 0.01) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        Text(
                            text = "Remaining: ${currencySymbol}${String.format("%.2f", diff)}",
                            color = color,
                            style = MaterialTheme.typography.bodySmall
                        )
                        LinearProgressIndicator(
                            progress = if (targetAmount > 0) (currentValues / targetAmount).toFloat().coerceIn(0f, 1f) else 0f,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            color = color
                        )
                    }
                    SplitType.PERCENTAGE -> {
                         val diff = 100.0 - currentValues
                         val color = if (kotlin.math.abs(diff) < 0.01) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                         Text(
                            text = "Total: $currentValues% (Rem: $diff%)",
                            color = color,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    SplitType.SHARES -> {
                         Text(
                            text = "Total Shares: $currentValues",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    else -> {}
                }
            }


        }
    }
}
