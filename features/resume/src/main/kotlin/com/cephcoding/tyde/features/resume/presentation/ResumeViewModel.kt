package com.cephcoding.tyde.features.resume.presentation

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cephcoding.tyde.core.common.coroutines.DispatcherProvider
import com.cephcoding.tyde.core.common.result.AppResult
import com.cephcoding.tyde.features.resume.data.local.ResumeFileReader
import com.cephcoding.tyde.features.resume.data.local.SelectedResumeFile
import com.cephcoding.tyde.features.resume.domain.ResumeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResumeViewModel(
    private val resumeRepository: ResumeRepository,
    private val resumeFileReader: ResumeFileReader,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResumeUiState())
    val uiState: StateFlow<ResumeUiState> = _uiState.asStateFlow()

    private var lastSelectedFile: SelectedResumeFile? = null

    init {
        loadResumes()
    }

    private fun loadResumes() {
        viewModelScope.launch {
            when (val result = resumeRepository.getResumes()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isLoadingResume = false, resume = result.data.firstOrNull())
                }
                is AppResult.Error -> _uiState.update { it.copy(isLoadingResume = false) }
            }
        }
    }

    fun onFileSelected(contentResolver: ContentResolver, uri: Uri) {
        if (_uiState.value.uploadState.isBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(uploadState = ResumeUploadState.Validating) }
            when (val fileResult = withContext(dispatcherProvider.io) { resumeFileReader.read(contentResolver, uri) }) {
                is AppResult.Error -> _uiState.update { it.copy(uploadState = ResumeUploadState.Error(fileResult.error)) }
                is AppResult.Success -> {
                    lastSelectedFile = fileResult.data
                    upload(fileResult.data)
                }
            }
        }
    }

    fun onRetry() {
        if (_uiState.value.uploadState.isBusy) return
        val file = lastSelectedFile ?: return
        viewModelScope.launch { upload(file) }
    }

    private suspend fun upload(file: SelectedResumeFile) {
        _uiState.update { it.copy(uploadState = ResumeUploadState.Uploading(0f)) }
        val result = resumeRepository.uploadResume(file) { progress ->
            _uiState.update { it.copy(uploadState = ResumeUploadState.Uploading(progress)) }
        }
        when (result) {
            is AppResult.Success -> _uiState.update {
                it.copy(resume = result.data, uploadState = ResumeUploadState.Success)
            }
            is AppResult.Error -> _uiState.update { it.copy(uploadState = ResumeUploadState.Error(result.error)) }
        }
    }
}
