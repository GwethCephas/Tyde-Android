package com.cephcoding.tyde.features.resume.domain

import com.cephcoding.tyde.core.common.result.AppResult
import com.cephcoding.tyde.features.resume.data.local.SelectedResumeFile

interface ResumeRepository {
    suspend fun getResumes(): AppResult<List<Resume>>
    suspend fun uploadResume(file: SelectedResumeFile, onProgress: (Float) -> Unit): AppResult<Resume>
}
