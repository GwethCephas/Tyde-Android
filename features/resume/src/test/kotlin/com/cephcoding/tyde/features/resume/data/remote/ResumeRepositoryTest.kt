package com.cephcoding.tyde.features.resume.data.remote

import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.core.common.result.AppResult
import com.cephcoding.tyde.features.resume.data.local.SelectedResumeFile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ResumeRepositoryTest {

    private val api = mockk<ResumeApi>()
    private val repository = ResumeRepositoryImpl(api)

    private val dto = ResumeDto(
        id = "resume-1",
        originalFileName = "resume.pdf",
        status = "UPLOADED",
        uploadedAt = "2026-10-08T15:30:00Z"
    )

    @Test
    fun `getResumes success maps dtos to domain models`() = runTest {
        coEvery { api.getResumes() } returns listOf(dto)

        val result = repository.getResumes()

        assertEquals(AppResult.Success(listOf(dto.toDomain())), result)
    }

    @Test
    fun `getResumes failure maps to AppError`() = runTest {
        val exception = mockk<HttpException>()
        every { exception.code() } returns 500
        coEvery { api.getResumes() } throws exception

        val result = repository.getResumes()

        assertEquals(AppResult.Error(AppError.Server(500)), result)
    }

    @Test
    fun `uploadResume success maps the returned dto to a domain model`() = runTest {
        coEvery { api.uploadResume(any()) } returns dto
        val file = SelectedResumeFile("resume.pdf", "application/pdf", "content".toByteArray())

        val result = repository.uploadResume(file) { }

        assertEquals(AppResult.Success(dto.toDomain()), result)
    }

    @Test
    fun `uploadResume failure maps to AppError`() = runTest {
        val exception = mockk<HttpException>()
        every { exception.code() } returns 413
        coEvery { api.uploadResume(any()) } throws exception
        val file = SelectedResumeFile("resume.pdf", "application/pdf", "content".toByteArray())

        val result = repository.uploadResume(file) { }

        assertEquals(AppResult.Error(AppError.Server(413)), result)
    }
}
