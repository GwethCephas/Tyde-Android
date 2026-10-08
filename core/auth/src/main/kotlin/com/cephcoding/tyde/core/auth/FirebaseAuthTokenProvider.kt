package com.cephcoding.tyde.core.auth

import com.cephcoding.tyde.core.common.auth.AuthTokenProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class FirebaseAuthTokenProvider(
    private val firebaseAuth: FirebaseAuth
) : AuthTokenProvider {

    override suspend fun accessToken(): String? = try {
        firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
    } catch (exception: Exception) {
        null
    }
}
