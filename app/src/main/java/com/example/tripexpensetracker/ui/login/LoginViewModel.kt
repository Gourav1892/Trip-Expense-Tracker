package com.example.tripexpensetracker.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    // For Sign Up Flow
    private var verificationId: String? = null

    fun onPhoneNumberChange(number: String) {
        // Only store the local number (without country code)
        _uiState.value = _uiState.value.copy(phoneNumber = number.replace(Regex("[^0-9]"), ""), error = null)
    }
    
    fun onCountryCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(countryCode = code, error = null)
    }

    fun onOtpChange(otp: String) {
        _uiState.value = _uiState.value.copy(otp = otp, error = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }
    
    fun onConfirmPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = password, error = null)
    }
    
    fun toggleMode() {
        val currentMode = _uiState.value.isSignUp
        _uiState.value = LoginUiState(isSignUp = !currentMode)
    }
    
    // Get full phone with country code
    private fun getFullPhoneNumber(): String {
        val code = _uiState.value.countryCode
        val number = _uiState.value.phoneNumber
        return "$code$number"
    }

    // Step 1: Send OTP (Sign Up)
    fun sendOtp(activity: android.app.Activity) {
        val phone = getFullPhoneNumber()
        if (_uiState.value.phoneNumber.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter phone number")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        authRepository.sendVerificationCode(
            phoneNumber = phone,
            activity = activity,
            onCodeSent = { vId, _ ->
                verificationId = vId
                _uiState.value = _uiState.value.copy(isLoading = false, signUpStep = SignUpStep.OTP_INPUT)
            },
            onVerificationCompleted = { cred ->
                 verifyOtpWithCredential(cred)
            },
            onVerificationFailed = { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        )
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name, error = null)
    }

    // Step 2: Verify OTP (Sign Up)
    fun verifyOtp() {
        val otp = _uiState.value.otp
        val vId = verificationId
        if (otp.isBlank() || vId == null) {
            _uiState.value = _uiState.value.copy(error = "Invalid OTP")
            return
        }
        val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(vId, otp)
        verifyOtpWithCredential(credential)
    }

    private fun verifyOtpWithCredential(credential: com.google.firebase.auth.PhoneAuthCredential) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            authRepository.signInWithCredential(credential).collect { result ->
                result.onSuccess {
                    // Phone Verified, move to Name Input
                    _uiState.value = _uiState.value.copy(isLoading = false, signUpStep = SignUpStep.NAME_INPUT)
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }
    
    // Step 2.5: Submit Name
    fun submitName() {
        val name = _uiState.value.name
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your name")
            return
        }
        // Move to Password
        _uiState.value = _uiState.value.copy(signUpStep = SignUpStep.PASSWORD_INPUT)
    }

    // Step 3: Set Password (Sign Up)
    fun completeSignUp() {
        val pass = _uiState.value.password
        val confirm = _uiState.value.confirmPassword
        val phone = getFullPhoneNumber()
        val name = _uiState.value.name
        val photoUri = _uiState.value.photoUri

        if (pass != confirm) {
            _uiState.value = _uiState.value.copy(error = "Passwords do not match")
            return
        }
        if (pass.length < 6) {
             _uiState.value = _uiState.value.copy(error = "Password too short")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            authRepository.linkPassword(phone, pass, name, photoUri).collect { result ->
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    // Login Flow
    fun login() {
        val phone = getFullPhoneNumber()
        val password = _uiState.value.password
        
        if (_uiState.value.phoneNumber.isBlank() || password.isBlank()) {
               _uiState.value = _uiState.value.copy(error = "Fill all fields")
               return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            authRepository.login(phone, password).collect { result ->
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }
    fun onPhotoSelected(uri: android.net.Uri?) {
        _uiState.value = _uiState.value.copy(photoUri = uri, error = null)
    }
}

enum class SignUpStep { PHONE_INPUT, OTP_INPUT, NAME_INPUT, PASSWORD_INPUT }

data class LoginUiState(
    val phoneNumber: String = "",
    val countryCode: String = "+91", // India default
    val otp: String = "",         // For Sign Up
    val name: String = "",        // For Sign Up
    val photoUri: android.net.Uri? = null, // For Sign Up
    val password: String = "",
    val confirmPassword: String = "", // For Sign Up
    val isSignUp: Boolean = false,
    val signUpStep: SignUpStep = SignUpStep.PHONE_INPUT,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)

