package com.cephcoding.tyde.features.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cephcoding.tyde.core.auth.AuthRepository
import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.core.common.result.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, error = null) }
    }

    fun onRegisterClick() {
        val state = _uiState.value
        if (state.isLoading || state.email.isBlank() || state.password.isBlank()) return

        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(error = AppError.PasswordMismatch) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.register(state.email.trim(), state.password)) {
                is AppResult.Success -> _uiState.update { it.copy(isLoading = false) }
                is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
