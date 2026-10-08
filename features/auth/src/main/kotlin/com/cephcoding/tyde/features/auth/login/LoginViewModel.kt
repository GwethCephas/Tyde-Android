package com.cephcoding.tyde.features.auth.login

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cephcoding.tyde.core.auth.AuthRepository
import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.core.common.result.AppResult
import com.cephcoding.tyde.features.auth.google.GoogleIdTokenRequester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val googleIdTokenRequester: GoogleIdTokenRequester
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (state.isLoading || state.email.isBlank() || state.password.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(state.email.trim(), state.password)) {
                is AppResult.Success -> _uiState.update { it.copy(isLoading = false) }
                is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    fun onGoogleSignInClick(context: Context) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val idToken = googleIdTokenRequester.requestIdToken(context)
                when (val result = authRepository.signInWithGoogle(idToken)) {
                    is AppResult.Success -> _uiState.update { it.copy(isLoading = false) }
                    is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error) }
                }
            } catch (exception: GetCredentialCancellationException) {
                _uiState.update { it.copy(isLoading = false) }
            } catch (exception: Exception) {
                _uiState.update { it.copy(isLoading = false, error = AppError.Unknown(exception)) }
            }
        }
    }
}
