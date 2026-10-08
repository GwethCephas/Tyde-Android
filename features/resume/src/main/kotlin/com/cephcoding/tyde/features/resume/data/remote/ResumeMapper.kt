package com.cephcoding.tyde.features.resume.data.remote

import com.cephcoding.tyde.features.resume.domain.Resume
import com.cephcoding.tyde.features.resume.domain.ResumeStatus

fun ResumeDto.toDomain(): Resume = Resume(
    id = id,
    originalFileName = originalFileName,
    status = ResumeStatus.entries.find { it.name == status } ?: ResumeStatus.UNKNOWN,
    uploadedAt = uploadedAt
)
