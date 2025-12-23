package com.example.tripexpensetracker.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onCurrentPasswordChange(password: String) {
        _uiState.update { it.copy(currentPassword = password) }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password, passwordError = null) }
    }

    fun onConfirmNewPasswordChange(password: String) {
        _uiState.update { it.copy(confirmNewPassword = password, passwordError = null) }
    }

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
            // First re-authenticate
            authRepository.reauthenticate(currentState.currentPassword).collect { reauthResult ->
                if (reauthResult.isSuccess) {
                    // Then update password
                    authRepository.updatePassword(currentState.newPassword).collect { updateResult ->
                        _uiState.update { it.copy(isLoading = false) }
                        if (updateResult.isSuccess) {
                            _eventFlow.emit(UiEvent.ShowSnackbar("Password updated successfully"))
                            _eventFlow.emit(UiEvent.PasswordChangedSuccess)
                        } else {
                            _eventFlow.emit(UiEvent.ShowSnackbar(updateResult.exceptionOrNull()?.message ?: "Update failed"))
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(UiEvent.ShowSnackbar(reauthResult.exceptionOrNull()?.message ?: "Re-authentication failed. Check current password."))
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
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isLoading: Boolean = false,
    val passwordError: String? = null
)
