package com.cephcoding.tyde.features.auth.register

import com.cephcoding.tyde.core.auth.AuthRepository
import com.cephcoding.tyde.core.auth.TydeUser
import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.core.common.result.AppResult
import com.cephcoding.tyde.features.auth.MainDispatcherRule
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
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleUser = TydeUser(
        uid = "uid-1",
        email = "new@tyde.app",
        displayName = null,
        photoUrl = null,
        isEmailVerified = false
    )

    @Test
    fun `onRegisterClick with mismatched passwords sets PasswordMismatch and does not call the repository`() = runTest {
        val repository = mockk<AuthRepository>()
        val viewModel = RegisterViewModel(repository)
        viewModel.onEmailChange("new@tyde.app")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("different")

        viewModel.onRegisterClick()
        advanceUntilIdle()

        assertEquals(AppError.PasswordMismatch, viewModel.uiState.value.error)
        coVerify(exactly = 0) { repository.register(any(), any()) }
    }

    @Test
    fun `onRegisterClick success clears loading and error`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.register("new@tyde.app", "password123") } returns AppResult.Success(sampleUser)
        val viewModel = RegisterViewModel(repository)
        viewModel.onEmailChange("new@tyde.app")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")

        viewModel.onRegisterClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `onRegisterClick failure surfaces the mapped AppError`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.register(any(), any()) } returns AppResult.Error(AppError.EmailAlreadyInUse)
        val viewModel = RegisterViewModel(repository)
        viewModel.onEmailChange("new@tyde.app")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")

        viewModel.onRegisterClick()
        advanceUntilIdle()

        assertEquals(AppError.EmailAlreadyInUse, viewModel.uiState.value.error)
    }

    @Test
    fun `onRegisterClick ignores a second call while the first is in flight`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.register(any(), any()) } coAnswers {
            delay(100)
            AppResult.Success(sampleUser)
        }
        val viewModel = RegisterViewModel(repository)
        viewModel.onEmailChange("new@tyde.app")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")

        viewModel.onRegisterClick()
        runCurrent()
        viewModel.onRegisterClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.register(any(), any()) }
    }
}
