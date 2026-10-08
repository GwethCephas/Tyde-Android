package com.cephcoding.tyde.core.common.result

sealed class AppError {
    data object Network : AppError()
    data class Server(val code: Int) : AppError()
    data object Unauthorized : AppError()
    data class Unknown(val throwable: Throwable) : AppError()
    data object InvalidEmail : AppError()
    data object WeakPassword : AppError()
    data object InvalidCredentials : AppError()
    data object EmailAlreadyInUse : AppError()
    data object TooManyRequests : AppError()
    data object PasswordMismatch : AppError()
    data object InvalidFile : AppError()
    data object FileTooLarge : AppError()
}
