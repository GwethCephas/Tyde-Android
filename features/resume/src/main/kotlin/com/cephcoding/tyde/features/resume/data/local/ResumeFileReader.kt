package com.cephcoding.tyde.features.resume.data.local

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.core.common.result.AppResult

class ResumeFileReader {

    fun read(contentResolver: ContentResolver, uri: Uri): AppResult<SelectedResumeFile> {
        val displayName = queryDisplayName(contentResolver, uri) ?: "resume.pdf"
        val mimeType = contentResolver.getType(uri)
        val looksLikePdf = mimeType == ResumeUploadLimits.ACCEPTED_MIME_TYPE ||
            (mimeType == null && displayName.endsWith(".pdf", ignoreCase = true))
        if (!looksLikePdf) {
            return AppResult.Error(AppError.InvalidFile)
        }

        val reportedSize = querySize(contentResolver, uri)
        if (reportedSize != null && reportedSize > ResumeUploadLimits.MAX_FILE_SIZE_BYTES) {
            return AppResult.Error(AppError.FileTooLarge)
        }

        val bytes = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return AppResult.Error(AppError.InvalidFile)
        } catch (exception: Exception) {
            return AppResult.Error(AppError.InvalidFile)
        }

        if (bytes.isEmpty()) {
            return AppResult.Error(AppError.InvalidFile)
        }
        if (bytes.size > ResumeUploadLimits.MAX_FILE_SIZE_BYTES) {
            return AppResult.Error(AppError.FileTooLarge)
        }

        return AppResult.Success(
            SelectedResumeFile(
                fileName = displayName,
                mimeType = ResumeUploadLimits.ACCEPTED_MIME_TYPE,
                bytes = bytes
            )
        )
    }

    private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return null
    }

    private fun querySize(contentResolver: ContentResolver, uri: Uri): Long? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) {
                return cursor.getLong(index)
            }
        }
        return null
    }
}
