package com.cephcoding.tyde.features.resume.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.features.resume.R

@Composable
fun resumeErrorMessage(error: AppError): String = when (error) {
    AppError.InvalidFile -> stringResource(R.string.resume_error_invalid_file)
    AppError.FileTooLarge -> stringResource(R.string.resume_error_file_too_large)
    AppError.Network -> stringResource(R.string.resume_error_network)
    AppError.Unauthorized,
    is AppError.Server,
    is AppError.Unknown,
    AppError.InvalidEmail,
    AppError.WeakPassword,
    AppError.InvalidCredentials,
    AppError.EmailAlreadyInUse,
    AppError.TooManyRequests,
    AppError.PasswordMismatch -> stringResource(R.string.resume_error_unknown)
}
