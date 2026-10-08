package com.cephcoding.tyde.core.auth

import com.cephcoding.tyde.core.common.result.AppError
import com.google.firebase.auth.FirebaseAuthException

fun FirebaseAuthException.toAppError(): AppError = when (errorCode) {
    "ERROR_INVALID_EMAIL" -> AppError.InvalidEmail
    "ERROR_WEAK_PASSWORD" -> AppError.WeakPassword
    "ERROR_WRONG_PASSWORD",
    "ERROR_USER_NOT_FOUND",
    "ERROR_USER_DISABLED",
    "ERROR_INVALID_CREDENTIAL" -> AppError.InvalidCredentials
    "ERROR_EMAIL_ALREADY_IN_USE" -> AppError.EmailAlreadyInUse
    "ERROR_TOO_MANY_REQUESTS" -> AppError.TooManyRequests
    "ERROR_NETWORK_REQUEST_FAILED" -> AppError.Network
    else -> AppError.Unknown(this)
}
