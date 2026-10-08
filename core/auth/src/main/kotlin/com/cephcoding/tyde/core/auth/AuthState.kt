package com.cephcoding.tyde.core.auth

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: TydeUser) : AuthState
}
