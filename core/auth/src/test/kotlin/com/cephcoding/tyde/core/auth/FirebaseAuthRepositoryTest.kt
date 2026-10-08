package com.cephcoding.tyde.core.auth

import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.core.common.result.AppResult
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirebaseAuthRepositoryTest {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listenerSlot: io.mockk.CapturingSlot<FirebaseAuth.AuthStateListener>
    private lateinit var repository: FirebaseAuthRepository

    @Before
    fun setUp() {
        firebaseAuth = mockk(relaxed = true)
        listenerSlot = slot()
        every { firebaseAuth.addAuthStateListener(capture(listenerSlot)) } returns Unit
        every { firebaseAuth.currentUser } returns null
        repository = FirebaseAuthRepository(firebaseAuth)
    }

    private fun firebaseUser(uid: String = "uid-1", email: String = "user@tyde.app"): FirebaseUser {
        val user = mockk<FirebaseUser>()
        every { user.uid } returns uid
        every { user.email } returns email
        every { user.displayName } returns null
        every { user.photoUrl } returns null
        every { user.isEmailVerified } returns true
        return user
    }

    @Test
    fun `initial auth state is Loading before the listener fires`() {
        assertEquals(AuthState.Loading, repository.authState.value)
    }

    @Test
    fun `listener firing with no current user yields Unauthenticated`() {
        every { firebaseAuth.currentUser } returns null

        listenerSlot.captured.onAuthStateChanged(firebaseAuth)

        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }

    @Test
    fun `listener firing with a current user yields Authenticated`() {
        val user = firebaseUser()
        every { firebaseAuth.currentUser } returns user

        listenerSlot.captured.onAuthStateChanged(firebaseAuth)

        val state = repository.authState.value
        assertTrue(state is AuthState.Authenticated)
        assertEquals("uid-1", (state as AuthState.Authenticated).user.uid)
    }

    @Test
    fun `login success returns the mapped user`() = runTest {
        val user = firebaseUser()
        val authResult = mockk<AuthResult>()
        every { authResult.user } returns user
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)

        val result = repository.login("user@tyde.app", "password123")

        assertEquals(AppResult.Success(user.toTydeUser()), result)
    }

    @Test
    fun `login failure maps the Firebase exception`() = runTest {
        val exception = mockk<FirebaseAuthException>()
        every { exception.errorCode } returns "ERROR_INVALID_CREDENTIAL"
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forException(exception)

        val result = repository.login("user@tyde.app", "wrong-password")

        assertEquals(AppResult.Error(AppError.InvalidCredentials), result)
    }

    @Test
    fun `register success returns the mapped user`() = runTest {
        val user = firebaseUser(uid = "uid-2")
        val authResult = mockk<AuthResult>()
        every { authResult.user } returns user
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)

        val result = repository.register("new@tyde.app", "password123")

        assertEquals(AppResult.Success(user.toTydeUser()), result)
    }

    @Test
    fun `signInWithGoogle success returns the mapped user`() = runTest {
        val user = firebaseUser(uid = "uid-3")
        val authResult = mockk<AuthResult>()
        every { authResult.user } returns user
        every { firebaseAuth.signInWithCredential(any()) } returns Tasks.forResult(authResult)

        val result = repository.signInWithGoogle("id-token")

        assertEquals(AppResult.Success(user.toTydeUser()), result)
    }

    @Test
    fun `logout signs out of Firebase`() {
        repository.logout()

        verify { firebaseAuth.signOut() }
    }
}
