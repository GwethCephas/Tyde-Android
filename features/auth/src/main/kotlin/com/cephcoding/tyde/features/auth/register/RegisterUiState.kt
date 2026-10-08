package com.cephcoding.tyde.features.auth.register

import com.cephcoding.tyde.core.common.result.AppError

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null
)
