package com.cephcoding.tyde.features.resume.data.local

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.cephcoding.tyde.core.common.result.AppError
import com.cephcoding.tyde.core.common.result.AppResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ResumeFileReaderTest {

    private val uri = mockk<Uri>()
    private val reader = ResumeFileReader()

    private fun cursorWith(displayName: String?, size: Long?): Cursor {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
        every { cursor.getColumnIndex(OpenableColumns.SIZE) } returns 1
        every { cursor.moveToFirst() } returns true
        every { cursor.getString(0) } returns displayName
        every { cursor.isNull(1) } returns (size == null)
        every { cursor.getLong(1) } returns (size ?: 0L)
        return cursor
    }

    private fun contentResolverFor(
        displayName: String,
        size: Long?,
        mimeType: String?,
        content: ByteArray?
    ): ContentResolver {
        val resolver = mockk<ContentResolver>()
        every { resolver.query(uri, null, null, null, null) } returns cursorWith(displayName, size)
        every { resolver.getType(uri) } returns mimeType
        every { resolver.openInputStream(uri) } returns content?.let { ByteArrayInputStream(it) }
        return resolver
    }

    @Test
    fun `valid pdf is read successfully`() {
        val bytes = "%PDF-1.4 content".toByteArray()
        val resolver = contentResolverFor("resume.pdf", bytes.size.toLong(), "application/pdf", bytes)

        val result = reader.read(resolver, uri)

        assertTrue(result is AppResult.Success)
        val file = (result as AppResult.Success).data
        assertEquals("resume.pdf", file.fileName)
        assertEquals("application/pdf", file.mimeType)
        assertEquals(bytes.size, file.bytes.size)
    }

    @Test
    fun `non-pdf mime type is rejected without reading the stream`() {
        val resolver = contentResolverFor("photo.png", 100L, "image/png", ByteArray(100))

        val result = reader.read(resolver, uri)

        assertEquals(AppResult.Error(AppError.InvalidFile), result)
        verify(exactly = 0) { resolver.openInputStream(any()) }
    }

    @Test
    fun `size reported by the provider exceeding the limit is rejected early`() {
        val oversizedReportedSize = ResumeUploadLimits.MAX_FILE_SIZE_BYTES + 1
        val resolver = contentResolverFor("resume.pdf", oversizedReportedSize, "application/pdf", ByteArray(0))

        val result = reader.read(resolver, uri)

        assertEquals(AppResult.Error(AppError.FileTooLarge), result)
        verify(exactly = 0) { resolver.openInputStream(any()) }
    }

    @Test
    fun `null input stream is rejected as invalid`() {
        val resolver = contentResolverFor("resume.pdf", 10L, "application/pdf", null)

        val result = reader.read(resolver, uri)

        assertEquals(AppResult.Error(AppError.InvalidFile), result)
    }

    @Test
    fun `empty file is rejected as invalid`() {
        val resolver = contentResolverFor("resume.pdf", 0L, "application/pdf", ByteArray(0))

        val result = reader.read(resolver, uri)

        assertEquals(AppResult.Error(AppError.InvalidFile), result)
    }

    @Test
    fun `actual bytes exceeding the limit are rejected even without a reported size`() {
        val oversizedBytes = ByteArray((ResumeUploadLimits.MAX_FILE_SIZE_BYTES + 1).toInt())
        val resolver = contentResolverFor("resume.pdf", null, "application/pdf", oversizedBytes)

        val result = reader.read(resolver, uri)

        assertEquals(AppResult.Error(AppError.FileTooLarge), result)
    }
}
