package com.cephcoding.tyde.core.auth

import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.core.common.result.AppResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _authState.value = auth.currentUser?.toTydeUser()
                ?.let(AuthState::Authenticated)
                ?: AuthState.Unauthenticated
        }
    }

    override suspend fun login(email: String, password: String): AppResult<TydeUser> =
        runAuthCatching {
            firebaseAuth.signInWithEmailAndPassword(email, password).await().user!!.toTydeUser()
        }

    override suspend fun register(email: String, password: String): AppResult<TydeUser> =
        runAuthCatching {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await().user!!.toTydeUser()
        }

    override suspend fun signInWithGoogle(idToken: String): AppResult<TydeUser> =
        runAuthCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await().user!!.toTydeUser()
        }

    override fun logout() {
        firebaseAuth.signOut()
    }

    private suspend fun runAuthCatching(block: suspend () -> TydeUser): AppResult<TydeUser> =
        try {
            AppResult.Success(block())
        } catch (exception: FirebaseAuthException) {
            AppResult.Error(exception.toAppError())
        } catch (exception: FirebaseNetworkException) {
            AppResult.Error(AppError.Network)
        } catch (exception: Exception) {
            AppResult.Error(AppError.Unknown(exception))
        }
}
