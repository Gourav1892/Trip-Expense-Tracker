package com.example.tripexpensetracker.ui.profile

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.repository.AuthRepository
import com.google.firebase.auth.PhoneAuthCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PasswordChangeStep {
    REQUEST_OTP,   // User enters phone and requests OTP
    VERIFY_OTP,    // User enters OTP to verify identity
    NEW_PASSWORD   // User enters new password
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()
    
    private var verificationId: String? = null

    init {
        // Initialize with empty phone - user needs to enter it for forgot password
    }

    fun onPhoneNumberChange(phone: String) {
        // Only store digits
        _uiState.update { it.copy(phoneNumber = phone.replace(Regex("[^0-9]"), ""), error = null) }
    }
    
    fun onCountryCodeChange(code: String) {
        _uiState.update { it.copy(countryCode = code, error = null) }
    }

    fun onOtpChange(otp: String) {
        _uiState.update { it.copy(otp = otp, error = null) }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password, passwordError = null) }
    }

    fun onConfirmNewPasswordChange(password: String) {
        _uiState.update { it.copy(confirmNewPassword = password, passwordError = null) }
    }

    // Step 1: Send OTP to user's phone
    fun sendOtp(activity: Activity) {
        val localPhone = _uiState.value.phoneNumber
        val countryCode = _uiState.value.countryCode
        if (localPhone.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your phone number") }
            return
        }
        
        val fullPhone = "$countryCode$localPhone"

        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.sendVerificationCode(
            phoneNumber = fullPhone,
            activity = activity,
            onCodeSent = { vId, _ ->
                verificationId = vId
                _uiState.update { it.copy(isLoading = false, step = PasswordChangeStep.VERIFY_OTP) }
            },
            onVerificationCompleted = { credential ->
                // Auto-verification - proceed directly
                verifyOtpWithCredential(credential)
            },
            onVerificationFailed = { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Verification failed") }
            }
        )
    }

    // Step 2: Verify OTP
    fun verifyOtp() {
        val otp = _uiState.value.otp
        val vId = verificationId
        
        if (otp.isBlank()) {
            _uiState.update { it.copy(error = "Enter OTP") }
            return
        }
        if (vId == null) {
            _uiState.update { it.copy(error = "OTP expired. Request again.") }
            return
        }
        
        val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(vId, otp)
        verifyOtpWithCredential(credential)
    }
    
    private fun verifyOtpWithCredential(credential: PhoneAuthCredential) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.reauthenticateWithCredential(credential).collect { result ->
                result.onSuccess {
                    // OTP Verified, move to new password step
                    _uiState.update { it.copy(isLoading = false, step = PasswordChangeStep.NEW_PASSWORD) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "OTP verification failed") }
                }
            }
        }
    }

    // Step 3: Set new password
    fun changePassword() {
        val currentState = _uiState.value
        
        if (currentState.newPassword != currentState.confirmNewPassword) {
            _uiState.update { it.copy(passwordError = "Passwords do not match") }
            return
        }

        if (currentState.newPassword.length < 6) {
             _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            authRepository.updatePassword(currentState.newPassword).collect { updateResult ->
                _uiState.update { it.copy(isLoading = false) }
                if (updateResult.isSuccess) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Password updated successfully"))
                    _eventFlow.emit(UiEvent.PasswordChangedSuccess)
                } else {
                    _eventFlow.emit(UiEvent.ShowSnackbar(updateResult.exceptionOrNull()?.message ?: "Update failed"))
                }
            }
        }
    }

    sealed class UiEvent {
        object PasswordChangedSuccess : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}

data class ChangePasswordUiState(
    val phoneNumber: String = "",
    val countryCode: String = "+91", // India default
    val otp: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val step: PasswordChangeStep = PasswordChangeStep.REQUEST_OTP,
    val isLoading: Boolean = false,
    val error: String? = null,
    val passwordError: String? = null
)

