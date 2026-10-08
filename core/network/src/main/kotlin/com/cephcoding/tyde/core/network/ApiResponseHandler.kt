package com.cephcoding.tyde.core.network

import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.core.common.result.AppResult
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> {
    return try {
        AppResult.Success(block())
    } catch (exception: IOException) {
        AppResult.Error(AppError.Network)
    } catch (exception: HttpException) {
        val error = if (exception.code() == 401) {
            AppError.Unauthorized
        } else {
            AppError.Server(exception.code())
        }
        AppResult.Error(error)
    } catch (exception: Exception) {
        AppResult.Error(AppError.Unknown(exception))
    }
}
