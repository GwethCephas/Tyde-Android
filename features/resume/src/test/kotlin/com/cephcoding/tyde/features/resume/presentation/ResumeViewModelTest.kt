package com.cephcoding.tyde.features.resume.presentation

import android.content.ContentResolver
import android.net.Uri
import com.cephcoding.tyde.core.common.coroutines.DispatcherProvider
import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.core.common.result.AppResult
import com.cephcoding.tyde.features.resume.MainDispatcherRule
import com.cephcoding.tyde.features.resume.data.local.ResumeFileReader
import com.cephcoding.tyde.features.resume.data.local.SelectedResumeFile
import com.cephcoding.tyde.features.resume.domain.Resume
import com.cephcoding.tyde.features.resume.domain.ResumeRepository
import com.cephcoding.tyde.features.resume.domain.ResumeStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResumeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleFile = SelectedResumeFile("resume.pdf", "application/pdf", "content".toByteArray())
    private val sampleResume = Resume("resume-1", "resume.pdf", ResumeStatus.UPLOADED, "2026-10-08T15:30:00Z")

    private fun TestScope.viewModel(
        repository: ResumeRepository = mockk(),
        reader: ResumeFileReader = mockk()
    ): ResumeViewModel {
        val testDispatcherProvider = object : DispatcherProvider {
            override val main = UnconfinedTestDispatcher(testScheduler)
            override val io = UnconfinedTestDispatcher(testScheduler)
            override val default = UnconfinedTestDispatcher(testScheduler)
        }
        return ResumeViewModel(repository, reader, testDispatcherProvider)
    }

    @Test
    fun `initial load success populates the most recent resume`() = runTest {
        val repository = mockk<ResumeRepository>()
        coEvery { repository.getResumes() } returns AppResult.Success(listOf(sampleResume))

        val viewModel = viewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingResume)
        assertEquals(sampleResume, state.resume)
    }

    @Test
    fun `initial load failure leaves the screen in the empty state, not an error state`() = runTest {
        val repository = mockk<ResumeRepository>()
        coEvery { repository.getResumes() } returns AppResult.Error(AppError.Network)

        val viewModel = viewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingResume)
        assertNull(state.resume)
        assertEquals(ResumeUploadState.Idle, state.uploadState)
    }

    @Test
    fun `onFileSelected validation failure surfaces the AppError without calling the repository`() = runTest {
        val repository = mockk<ResumeRepository>()
        val reader = mockk<ResumeFileReader>()
        coEvery { repository.getResumes() } returns AppResult.Success(emptyList())
        coEvery { reader.read(any(), any()) } returns AppResult.Error(AppError.InvalidFile)

        val viewModel = viewModel(repository, reader)
        advanceUntilIdle()
        viewModel.onFileSelected(mockk<ContentResolver>(), mockk<Uri>())
        advanceUntilIdle()

        assertEquals(ResumeUploadState.Error(AppError.InvalidFile), viewModel.uiState.value.uploadState)
        coVerify(exactly = 0) { repository.uploadResume(any(), any()) }
    }

    @Test
    fun `onFileSelected success updates the resume and clears the upload state`() = runTest {
        val repository = mockk<ResumeRepository>()
        val reader = mockk<ResumeFileReader>()
        coEvery { repository.getResumes() } returns AppResult.Success(emptyList())
        coEvery { reader.read(any(), any()) } returns AppResult.Success(sampleFile)
        coEvery { repository.uploadResume(sampleFile, any()) } returns AppResult.Success(sampleResume)

        val viewModel = viewModel(repository, reader)
        advanceUntilIdle()
        viewModel.onFileSelected(mockk<ContentResolver>(), mockk<Uri>())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(sampleResume, state.resume)
        assertEquals(ResumeUploadState.Success, state.uploadState)
    }

    @Test
    fun `onFileSelected upload failure surfaces the AppError`() = runTest {
        val repository = mockk<ResumeRepository>()
        val reader = mockk<ResumeFileReader>()
        coEvery { repository.getResumes() } returns AppResult.Success(emptyList())
        coEvery { reader.read(any(), any()) } returns AppResult.Success(sampleFile)
        coEvery { repository.uploadResume(sampleFile, any()) } returns AppResult.Error(AppError.Network)

        val viewModel = viewModel(repository, reader)
        advanceUntilIdle()
        viewModel.onFileSelected(mockk<ContentResolver>(), mockk<Uri>())
        advanceUntilIdle()

        assertEquals(ResumeUploadState.Error(AppError.Network), viewModel.uiState.value.uploadState)
    }

    @Test
    fun `onRetry re-uploads the previously selected file without re-reading it`() = runTest {
        val repository = mockk<ResumeRepository>()
        val reader = mockk<ResumeFileReader>()
        coEvery { repository.getResumes() } returns AppResult.Success(emptyList())
        coEvery { reader.read(any(), any()) } returns AppResult.Success(sampleFile)
        coEvery { repository.uploadResume(sampleFile, any()) } returnsMany listOf(
            AppResult.Error(AppError.Network),
            AppResult.Success(sampleResume)
        )

        val viewModel = viewModel(repository, reader)
        advanceUntilIdle()
        viewModel.onFileSelected(mockk<ContentResolver>(), mockk<Uri>())
        advanceUntilIdle()
        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(sampleResume, viewModel.uiState.value.resume)
        assertEquals(ResumeUploadState.Success, viewModel.uiState.value.uploadState)
        coVerify(exactly = 1) { reader.read(any(), any()) }
        coVerify(exactly = 2) { repository.uploadResume(sampleFile, any()) }
    }

    @Test
    fun `onFileSelected ignores a second call while the first is uploading`() = runTest {
        val repository = mockk<ResumeRepository>()
        val reader = mockk<ResumeFileReader>()
        coEvery { repository.getResumes() } returns AppResult.Success(emptyList())
        coEvery { reader.read(any(), any()) } returns AppResult.Success(sampleFile)
        coEvery { repository.uploadResume(sampleFile, any()) } coAnswers {
            delay(100)
            AppResult.Success(sampleResume)
        }

        val viewModel = viewModel(repository, reader)
        advanceUntilIdle()
        viewModel.onFileSelected(mockk<ContentResolver>(), mockk<Uri>())
        runCurrent()
        viewModel.onFileSelected(mockk<ContentResolver>(), mockk<Uri>())
        advanceUntilIdle()

        coVerify(exactly = 1) { reader.read(any(), any()) }
        coVerify(exactly = 1) { repository.uploadResume(sampleFile, any()) }
    }
}
