package com.cephcoding.tyde.features.resume.domain

data class Resume(
    val id: String,
    val originalFileName: String,
    val status: ResumeStatus,
    val uploadedAt: String?
)

enum class ResumeStatus {
    UPLOADED,
    PROCESSING,
    READY,
    FAILED,
    UNKNOWN
}
