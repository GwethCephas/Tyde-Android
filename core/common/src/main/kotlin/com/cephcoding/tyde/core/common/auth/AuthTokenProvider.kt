package com.cephcoding.tyde.core.common.auth

interface AuthTokenProvider {
    suspend fun accessToken(): String?
}
