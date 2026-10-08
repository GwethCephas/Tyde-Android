package com.cephcoding.tyde

import androidx.lifecycle.ViewModel
import com.cephcoding.tyde.core.auth.AuthRepository
import com.cephcoding.tyde.core.auth.AuthState
import kotlinx.coroutines.flow.StateFlow

class AppViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    fun logout() {
        authRepository.logout()
    }
}
