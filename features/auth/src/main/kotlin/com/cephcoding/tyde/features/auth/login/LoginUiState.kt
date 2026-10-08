package com.cephcoding.tyde.features.auth.login

import com.cephcoding.tyde.core.common.result.AppError

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null
)
