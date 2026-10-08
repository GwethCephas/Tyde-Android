package com.cephcoding.tyde.features.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cephcoding.tyde.core.common.result.AppError

@Composable
fun authErrorMessage(error: AppError): String = when (error) {
    AppError.InvalidEmail -> stringResource(R.string.auth_error_invalid_email)
    AppError.WeakPassword -> stringResource(R.string.auth_error_weak_password)
    AppError.InvalidCredentials -> stringResource(R.string.auth_error_invalid_credentials)
    AppError.EmailAlreadyInUse -> stringResource(R.string.auth_error_email_already_in_use)
    AppError.TooManyRequests -> stringResource(R.string.auth_error_too_many_requests)
    AppError.Network -> stringResource(R.string.auth_error_network)
    AppError.PasswordMismatch -> stringResource(R.string.auth_error_password_mismatch)
    AppError.Unauthorized,
    is AppError.Server,
    is AppError.Unknown,
    AppError.InvalidFile,
    AppError.FileTooLarge -> stringResource(R.string.auth_error_unknown)
}
