package com.example.tripexpensetracker.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage

// Country codes data
data class CountryCode(val country: String, val code: String, val flag: String)

val countryCodes = listOf(
    CountryCode("India", "+91", "🇮🇳"),
    CountryCode("USA", "+1", "🇺🇸"),
    CountryCode("UK", "+44", "🇬🇧"),
    CountryCode("Canada", "+1", "🇨🇦"),
    CountryCode("Australia", "+61", "🇦🇺"),
    CountryCode("UAE", "+971", "🇦🇪"),
    CountryCode("Singapore", "+65", "🇸🇬"),
    CountryCode("Germany", "+49", "🇩🇪"),
    CountryCode("France", "+33", "🇫🇷"),
    CountryCode("Japan", "+81", "🇯🇵"),
)

@Composable
fun CountryCodePicker(
    selectedCode: CountryCode,
    onCodeSelected: (CountryCode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.width(100.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Text("${selectedCode.flag} ${selectedCode.code}")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            countryCodes.forEach { countryCode ->
                DropdownMenuItem(
                    text = { Text("${countryCode.flag} ${countryCode.country} (${countryCode.code})") },
                    onClick = {
                        onCodeSelected(countryCode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onForgotPassword: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    var isLoginPasswordVisible by remember { mutableStateOf(false) }
    var isSignUpPasswordVisible by remember { mutableStateOf(false) }
    var isSignUpConfirmPasswordVisible by remember { mutableStateOf(false) }
    
    // Country code state - default to India
    var selectedCountryCode by remember { mutableStateOf(countryCodes.first()) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val title = if (uiState.isSignUp) {
                        when (uiState.signUpStep) {
                            SignUpStep.PHONE_INPUT -> "Sign Up - Step 1/3"
                            SignUpStep.OTP_INPUT -> "Sign Up - Step 2/4"
                            SignUpStep.NAME_INPUT -> "Sign Up - Step 3/4"
                            SignUpStep.PASSWORD_INPUT -> "Sign Up - Step 4/4"
                        }
                    } else {
                        "Login"
                    }
                    Text(title) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (!uiState.isSignUp) {
                // LOGIN MODE - Phone with country picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CountryCodePicker(
                        selectedCode = selectedCountryCode,
                        onCodeSelected = { selectedCountryCode = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = uiState.phoneNumber,
                        onValueChange = { 
                            viewModel.onPhoneNumberChange(it)
                            viewModel.onCountryCodeChange(selectedCountryCode.code)
                        },
                        label = { Text("Phone Number") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    visualTransformation = if (isLoginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (isLoginPasswordVisible)
                            Icons.Filled.Visibility
                        else
                            Icons.Filled.VisibilityOff

                        IconButton(onClick = { isLoginPasswordVisible = !isLoginPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = if (isLoginPasswordVisible) "Hide Password" else "Show Password")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { 
                        viewModel.onCountryCodeChange(selectedCountryCode.code)
                        viewModel.login() 
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Login")
                }
                
                // Forgot Password Link - highlighted if login error
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = {
                        viewModel.onCountryCodeChange(selectedCountryCode.code)
                        onForgotPassword()
                    }
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = if (uiState.error != null) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary,
                        style = if (uiState.error != null) 
                            MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        else
                            MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // SIGN UP MODE
                when (uiState.signUpStep) {
                    SignUpStep.PHONE_INPUT -> {
                        // Phone input with country picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CountryCodePicker(
                                selectedCode = selectedCountryCode,
                                onCodeSelected = { selectedCountryCode = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = uiState.phoneNumber,
                                onValueChange = {
                                    viewModel.onPhoneNumberChange(it)
                                    viewModel.onCountryCodeChange(selectedCountryCode.code)
                                },
                                label = { Text("Phone Number") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                viewModel.onCountryCodeChange(selectedCountryCode.code)
                                activity?.let { viewModel.sendOtp(it) } 
                            },
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            else Text("Send Verification Code")
                        }
                    }
                    SignUpStep.OTP_INPUT -> {
                        Text("Enter code sent to ${uiState.phoneNumber}")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = uiState.otp,
                            onValueChange = viewModel::onOtpChange,
                            label = { Text("OTP (6 digits)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = viewModel::verifyOtp,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            else Text("Verify & Continue")
                        }
                    }
                    SignUpStep.NAME_INPUT -> {
                        Text("Finish your profile")
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Photo Picker
                        val photoPickerLauncher = rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                        ) { uri ->
                            viewModel.onPhotoSelected(uri)
                        }
                        
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                        ) {
                            if (uiState.photoUri != null) {
                                coil.compose.AsyncImage(
                                    model = uiState.photoUri,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Add Photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Text("Add Profile Photo (Optional)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = viewModel::onNameChange,
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = viewModel::submitName,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            else Text("Next")
                        }
                    }
                    SignUpStep.PASSWORD_INPUT -> {
                        Text("Create a password for your account")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = { Text("Password (min 6 chars)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            visualTransformation = if (isSignUpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (isSignUpPasswordVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff

                                IconButton(onClick = { isSignUpPasswordVisible = !isSignUpPasswordVisible }) {
                                    Icon(imageVector = image, contentDescription = if (isSignUpPasswordVisible) "Hide Password" else "Show Password")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = uiState.confirmPassword,
                            onValueChange = viewModel::onConfirmPasswordChange,
                            label = { Text("Confirm Password") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            visualTransformation = if (isSignUpConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (isSignUpConfirmPasswordVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff

                                IconButton(onClick = { isSignUpConfirmPasswordVisible = !isSignUpConfirmPasswordVisible }) {
                                    Icon(imageVector = image, contentDescription = if (isSignUpConfirmPasswordVisible) "Hide Password" else "Show Password")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                         Button(
                            onClick = viewModel::completeSignUp,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            else Text("Create Account")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = viewModel::toggleMode) {
                Text(
                    text = if (uiState.isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up"
                )
            }
        }
    }
}
