package com.cephcoding.tyde.features.resume.presentation

import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.features.resume.domain.Resume

data class ResumeUiState(
    val isLoadingResume: Boolean = true,
    val resume: Resume? = null,
    val uploadState: ResumeUploadState = ResumeUploadState.Idle
)

sealed interface ResumeUploadState {
    data object Idle : ResumeUploadState
    data object Validating : ResumeUploadState
    data class Uploading(val progress: Float) : ResumeUploadState
    data object Success : ResumeUploadState
    data class Error(val error: AppError) : ResumeUploadState
}

val ResumeUploadState.isBusy: Boolean
    get() = this is ResumeUploadState.Validating || this is ResumeUploadState.Uploading
