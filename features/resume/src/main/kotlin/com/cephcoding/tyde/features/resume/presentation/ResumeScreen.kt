package com.cephcoding.tyde.features.resume.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cephcoding.tyde.core.designsystem.component.TydeCard
import com.cephcoding.tyde.core.designsystem.component.TydeLoadingIndicator
import com.cephcoding.tyde.core.designsystem.component.TydePrimaryButton
import com.cephcoding.tyde.core.designsystem.theme.TydeSpacing
import com.cephcoding.tyde.features.resume.domain.Resume
import org.koin.androidx.compose.koinViewModel

@Composable
fun ResumeRoute(
    onBack: () -> Unit,
    viewModel: ResumeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onFileSelected(context.contentResolver, it) }
    }

    ResumeScreen(
        state = uiState,
        onBack = onBack,
        onUploadClick = { pickerLauncher.launch("application/pdf") },
        onRetryClick = viewModel::onRetry
    )
}

@Composable
private fun ResumeScreen(
    state: ResumeUiState,
    onBack: () -> Unit,
    onUploadClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(TydeSpacing.large)
        ) {
            TextButton(onClick = onBack) {
                Text("Back")
            }

            Spacer(modifier = Modifier.height(TydeSpacing.large))

            if (state.isLoadingResume) {
                TydeLoadingIndicator(modifier = Modifier.fillMaxSize())
                return@Scaffold
            }

            Text(text = "Resume", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(TydeSpacing.large))

            state.resume?.let { resume ->
                ResumeSummaryCard(resume)
                Spacer(modifier = Modifier.height(TydeSpacing.large))
            } ?: run {
                Text(
                    text = "Upload your resume to get started",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(TydeSpacing.large))
            }

            when (val uploadState = state.uploadState) {
                is ResumeUploadState.Idle -> {
                    TydePrimaryButton(
                        text = if (state.resume == null) "Upload Resume" else "Replace Resume",
                        onClick = onUploadClick
                    )
                }

                is ResumeUploadState.Validating -> {
                    Text(text = "Checking your file…", style = MaterialTheme.typography.bodyMedium)
                }

                is ResumeUploadState.Uploading -> {
                    Text(text = "Uploading…", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(TydeSpacing.small))
                    LinearProgressIndicator(
                        progress = { uploadState.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is ResumeUploadState.Success -> {
                    Text(
                        text = "Resume uploaded successfully.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(TydeSpacing.medium))
                    TydePrimaryButton(text = "Replace Resume", onClick = onUploadClick)
                }

                is ResumeUploadState.Error -> {
                    Text(
                        text = resumeErrorMessage(uploadState.error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(TydeSpacing.medium))
                    TydePrimaryButton(text = "Try Again", onClick = onRetryClick)
                }
            }
        }
    }
}

@Composable
private fun ResumeSummaryCard(resume: Resume) {
    TydeCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = resume.originalFileName, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(TydeSpacing.extraSmall))
        Text(text = "Status: ${resume.status.name}", style = MaterialTheme.typography.bodyMedium)
        resume.uploadedAt?.let { uploadedAt ->
            Spacer(modifier = Modifier.height(TydeSpacing.extraSmall))
            Text(
                text = "Uploaded: ${uploadedAt.take(10)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
