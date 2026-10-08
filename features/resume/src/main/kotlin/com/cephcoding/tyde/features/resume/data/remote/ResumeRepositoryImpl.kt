package com.cephcoding.tyde.features.resume.data.remote

import com.cephcoding.tyde.core.common.result.AppResult
import com.cephcoding.tyde.core.common.result.map
import com.cephcoding.tyde.core.network.ProgressRequestBody
import com.cephcoding.tyde.core.network.safeApiCall
import com.cephcoding.tyde.features.resume.data.local.SelectedResumeFile
import com.cephcoding.tyde.features.resume.domain.Resume
import com.cephcoding.tyde.features.resume.domain.ResumeRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ResumeRepositoryImpl(
    private val resumeApi: ResumeApi
) : ResumeRepository {

    override suspend fun getResumes(): AppResult<List<Resume>> =
        safeApiCall { resumeApi.getResumes() }.map { dtos -> dtos.map { it.toDomain() } }

    override suspend fun uploadResume(
        file: SelectedResumeFile,
        onProgress: (Float) -> Unit
    ): AppResult<Resume> {
        val requestBody = file.bytes.toRequestBody(file.mimeType.toMediaType())
        val progressBody = ProgressRequestBody(requestBody, onProgress)
        val part = MultipartBody.Part.createFormData("file", file.fileName, progressBody)
        return safeApiCall { resumeApi.uploadResume(part) }.map { it.toDomain() }
    }
}
