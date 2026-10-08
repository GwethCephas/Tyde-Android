package com.cephcoding.tyde.core.network

import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.core.common.result.AppResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiResponseHandlerTest {

    @Test
    fun `successful call returns Success with the value`() = runTest {
        val result = safeApiCall { "ok" }

        assertEquals(AppResult.Success("ok"), result)
    }

    @Test
    fun `IOException maps to Network error`() = runTest {
        val result = safeApiCall { throw IOException() }

        assertEquals(AppResult.Error(AppError.Network), result)
    }

    @Test
    fun `HttpException with 401 maps to Unauthorized`() = runTest {
        val result = safeApiCall { throw unauthorizedHttpException() }

        assertEquals(AppResult.Error(AppError.Unauthorized), result)
    }

    @Test
    fun `HttpException with other code maps to Server error`() = runTest {
        val result = safeApiCall { throw httpException(code = 500) }

        assertEquals(AppResult.Error(AppError.Server(500)), result)
    }

    @Test
    fun `unexpected exception maps to Unknown`() = runTest {
        val thrown = IllegalStateException("boom")

        val result = safeApiCall { throw thrown }

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is AppError.Unknown)
        assertEquals(thrown, (error as AppError.Unknown).throwable)
    }

    private fun unauthorizedHttpException(): HttpException = httpException(code = 401)

    private fun httpException(code: Int): HttpException {
        val response = mockk<Response<*>>()
        every { response.code() } returns code
        every { response.message() } returns ""
        every { response.errorBody() } returns "".toResponseBody("application/json".toMediaType())
        return HttpException(response)
    }
}
