package com.rrrainielll.teledrop.utils

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer

/**
 * A custom RequestBody wrapper that tracks upload progress and reports it via callback.
 * Calculates upload percentage, speed, and estimated time remaining.
 */
class ProgressRequestBody(
    private val requestBody: RequestBody,
    private val onProgress: (uploadedBytes: Long, totalBytes: Long, percent: Int, speedBps: Long, etaSeconds: Long) -> Unit
) : RequestBody() {

    private var startTime: Long = 0
    private var lastUpdateTime: Long = 0
    private var lastUploadedBytes: Long = 0
    
    // For smoothing speed calculations
    private val speedHistory = mutableListOf<Long>()
    private val maxSpeedHistorySize = 5

    override fun contentType(): MediaType? = requestBody.contentType()

    override fun contentLength(): Long = requestBody.contentLength()

    override fun writeTo(sink: BufferedSink) {
        startTime = System.currentTimeMillis()
        lastUpdateTime = startTime
        lastUploadedBytes = 0

        val countingSink = CountingSink(sink)
        val bufferedSink = countingSink.buffer()

        requestBody.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
        private var bytesWritten: Long = 0
        private val totalBytes = contentLength()

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount

            val currentTime = System.currentTimeMillis()
            
            // Update at most every 200ms to avoid excessive callbacks
            if (currentTime - lastUpdateTime >= 200 || bytesWritten == totalBytes) {
                val timeSinceStart = currentTime - startTime
                val timeSinceLastUpdate = currentTime - lastUpdateTime
                
                // Calculate percentage
                val percent = if (totalBytes > 0) {
                    ((bytesWritten * 100) / totalBytes).toInt().coerceIn(0, 100)
                } else {
                    0
                }

                // Calculate instantaneous speed (bytes per second)
                val bytesInInterval = bytesWritten - lastUploadedBytes
                val instantSpeed = if (timeSinceLastUpdate > 0) {
                    (bytesInInterval * 1000) / timeSinceLastUpdate
                } else {
                    0L
                }
                
                // Add to speed history for smoothing
                speedHistory.add(instantSpeed)
                if (speedHistory.size > maxSpeedHistorySize) {
                    speedHistory.removeAt(0)
                }
                
                // Calculate average speed
                val avgSpeed = if (speedHistory.isNotEmpty()) {
                    speedHistory.average().toLong()
                } else {
                    0L
                }

                // Calculate ETA (estimated time of arrival)
                val remainingBytes = totalBytes - bytesWritten
                val etaSeconds = if (avgSpeed > 0) {
                    remainingBytes / avgSpeed
                } else {
                    0L
                }

                // Report progress
                onProgress(bytesWritten, totalBytes, percent, avgSpeed, etaSeconds)

                lastUpdateTime = currentTime
                lastUploadedBytes = bytesWritten
            }
        }
    }
}
