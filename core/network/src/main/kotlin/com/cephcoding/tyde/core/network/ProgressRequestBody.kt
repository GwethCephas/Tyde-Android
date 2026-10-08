package com.cephcoding.tyde.core.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer

class ProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (Float) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        var lastReportedPercent = -1
        val progressSink = object : ForwardingSink(sink) {
            var bytesWritten = 0L

            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                if (total <= 0L) return
                bytesWritten += byteCount
                val percent = ((bytesWritten * 100) / total).toInt().coerceIn(0, 100)
                if (percent != lastReportedPercent) {
                    lastReportedPercent = percent
                    onProgress(percent / 100f)
                }
            }
        }
        val bufferedProgressSink = progressSink.buffer()
        delegate.writeTo(bufferedProgressSink)
        bufferedProgressSink.flush()
    }
}
