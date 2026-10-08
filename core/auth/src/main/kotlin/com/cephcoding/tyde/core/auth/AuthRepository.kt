package com.cephcoding.tyde.core.auth

import com.cephcoding.tyde.core.common.result.AppResult
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun login(email: String, password: String): AppResult<TydeUser>
    suspend fun register(email: String, password: String): AppResult<TydeUser>
    suspend fun signInWithGoogle(idToken: String): AppResult<TydeUser>
    fun logout()
}
