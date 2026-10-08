package com.cephcoding.tyde.features.resume.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ResumeDto(
    val id: String,
    val originalFileName: String,
    val status: String,
    val uploadedAt: String? = null
)
