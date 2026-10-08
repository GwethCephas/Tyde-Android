package com.cephcoding.tyde.features.auth.login

import androidx.credentials.exceptions.GetCredentialCancellationException
import com.cephcoding.tyde.core.auth.AuthRepository
import com.cephcoding.tyde.core.auth.TydeUser
import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.core.common.result.AppResult
import com.cephcoding.tyde.features.auth.MainDispatcherRule
import com.cephcoding.tyde.features.auth.google.GoogleIdTokenRequester
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleUser = TydeUser(
        uid = "uid-1",
        email = "user@tyde.app",
        displayName = null,
        photoUrl = null,
        isEmailVerified = true
    )

    private fun viewModel(
        repository: AuthRepository = mockk(),
        googleIdTokenRequester: GoogleIdTokenRequester = mockk()
    ) = LoginViewModel(repository, googleIdTokenRequester)

    @Test
    fun `initial state is empty and not loading`() {
        val state = viewModel().uiState.value

        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `onLoginClick does nothing when email is blank`() = runTest {
        val repository = mockk<AuthRepository>()
        val viewModel = viewModel(repository)
        viewModel.onPasswordChange("password123")

        viewModel.onLoginClick()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun `onLoginClick success clears loading and error`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.login("user@tyde.app", "password123") } returns AppResult.Success(sampleUser)
        val viewModel = viewModel(repository)
        viewModel.onEmailChange("user@tyde.app")
        viewModel.onPasswordChange("password123")

        viewModel.onLoginClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `onLoginClick failure surfaces the mapped AppError`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.login(any(), any()) } returns AppResult.Error(AppError.InvalidCredentials)
        val viewModel = viewModel(repository)
        viewModel.onEmailChange("user@tyde.app")
        viewModel.onPasswordChange("wrong-password")

        viewModel.onLoginClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(AppError.InvalidCredentials, state.error)
    }

    @Test
    fun `onLoginClick ignores a second call while the first is in flight`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.login(any(), any()) } coAnswers {
            delay(100)
            AppResult.Success(sampleUser)
        }
        val viewModel = viewModel(repository)
        viewModel.onEmailChange("user@tyde.app")
        viewModel.onPasswordChange("password123")

        viewModel.onLoginClick()
        runCurrent()
        viewModel.onLoginClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.login(any(), any()) }
    }

    @Test
    fun `onGoogleSignInClick success clears loading and error`() = runTest {
        val repository = mockk<AuthRepository>()
        val googleIdTokenRequester = mockk<GoogleIdTokenRequester>()
        coEvery { googleIdTokenRequester.requestIdToken(any()) } returns "id-token"
        coEvery { repository.signInWithGoogle("id-token") } returns AppResult.Success(sampleUser)
        val viewModel = viewModel(repository, googleIdTokenRequester)

        viewModel.onGoogleSignInClick(mockk())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `onGoogleSignInClick cancellation clears loading without an error`() = runTest {
        val repository = mockk<AuthRepository>()
        val googleIdTokenRequester = mockk<GoogleIdTokenRequester>()
        coEvery { googleIdTokenRequester.requestIdToken(any()) } throws GetCredentialCancellationException()
        val viewModel = viewModel(repository, googleIdTokenRequester)

        viewModel.onGoogleSignInClick(mockk())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        coVerify(exactly = 0) { repository.signInWithGoogle(any()) }
    }
}
