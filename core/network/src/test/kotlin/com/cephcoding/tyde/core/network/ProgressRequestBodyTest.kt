package com.cephcoding.tyde.core.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressRequestBodyTest {

    @Test
    fun `writeTo reports a final progress of 1 and preserves the written bytes`() {
        val content = ByteArray(50_000) { it.toByte() }
        val delegate = content.toRequestBody("application/pdf".toMediaType())
        val reported = mutableListOf<Float>()

        val progressBody = ProgressRequestBody(delegate) { reported.add(it) }
        val sink = Buffer()
        progressBody.writeTo(sink)

        assertEquals(content.size.toLong(), sink.size)
        assertTrue(reported.isNotEmpty())
        assertEquals(1f, reported.last())
    }

    @Test
    fun `writeTo reports strictly increasing progress`() {
        val content = ByteArray(200_000) { it.toByte() }
        val delegate = content.toRequestBody("application/pdf".toMediaType())
        val reported = mutableListOf<Float>()

        val progressBody = ProgressRequestBody(delegate) { reported.add(it) }
        progressBody.writeTo(Buffer())

        val sorted = reported.sorted()
        assertEquals(sorted, reported)
    }

    @Test
    fun `contentLength and contentType are passed through from the delegate`() {
        val content = "resume bytes".toByteArray()
        val mediaType = "application/pdf".toMediaType()
        val delegate = content.toRequestBody(mediaType)

        val progressBody = ProgressRequestBody(delegate) { }

        assertEquals(delegate.contentLength(), progressBody.contentLength())
        assertEquals(delegate.contentType(), progressBody.contentType())
    }
}
