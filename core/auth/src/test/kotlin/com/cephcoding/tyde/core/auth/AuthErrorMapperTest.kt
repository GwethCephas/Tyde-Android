package com.cephcoding.tyde.core.auth

import com.cephcoding.tyde.core.common.result.AppError
import com.google.firebase.auth.FirebaseAuthException
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMapperTest {

    private fun exceptionWith(errorCode: String): FirebaseAuthException {
        val exception = mockk<FirebaseAuthException>()
        every { exception.errorCode } returns errorCode
        return exception
    }

    @Test
    fun `ERROR_INVALID_EMAIL maps to InvalidEmail`() {
        assertEquals(AppError.InvalidEmail, exceptionWith("ERROR_INVALID_EMAIL").toAppError())
    }

    @Test
    fun `ERROR_WEAK_PASSWORD maps to WeakPassword`() {
        assertEquals(AppError.WeakPassword, exceptionWith("ERROR_WEAK_PASSWORD").toAppError())
    }

    @Test
    fun `ERROR_WRONG_PASSWORD maps to InvalidCredentials`() {
        assertEquals(AppError.InvalidCredentials, exceptionWith("ERROR_WRONG_PASSWORD").toAppError())
    }

    @Test
    fun `ERROR_USER_NOT_FOUND maps to InvalidCredentials`() {
        assertEquals(AppError.InvalidCredentials, exceptionWith("ERROR_USER_NOT_FOUND").toAppError())
    }

    @Test
    fun `ERROR_INVALID_CREDENTIAL maps to InvalidCredentials`() {
        assertEquals(AppError.InvalidCredentials, exceptionWith("ERROR_INVALID_CREDENTIAL").toAppError())
    }

    @Test
    fun `ERROR_EMAIL_ALREADY_IN_USE maps to EmailAlreadyInUse`() {
        assertEquals(AppError.EmailAlreadyInUse, exceptionWith("ERROR_EMAIL_ALREADY_IN_USE").toAppError())
    }

    @Test
    fun `ERROR_TOO_MANY_REQUESTS maps to TooManyRequests`() {
        assertEquals(AppError.TooManyRequests, exceptionWith("ERROR_TOO_MANY_REQUESTS").toAppError())
    }

    @Test
    fun `ERROR_NETWORK_REQUEST_FAILED maps to Network`() {
        assertEquals(AppError.Network, exceptionWith("ERROR_NETWORK_REQUEST_FAILED").toAppError())
    }

    @Test
    fun `unrecognized error code maps to Unknown`() {
        val exception = exceptionWith("ERROR_SOMETHING_NEW")

        val result = exception.toAppError()

        assertEquals(AppError.Unknown(exception), result)
    }
}
