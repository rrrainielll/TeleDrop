package com.rrrainielll.teledrop.utils

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.security.MessageDigest

/**
 * Utility for calculating file checksums to enable content-based duplicate detection.
 */
object FileHashUtil {
    
    /**
     * Calculates MD5 checksum of a file from its content URI.
     * 
     * @param context Android context for accessing content resolver
     * @param uri Content URI of the file
     * @return MD5 hash as hex string, or null if calculation fails
     */
    fun calculateMD5(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                calculateMD5FromStream(inputStream)
            }
        } catch (e: Exception) {
            android.util.Log.e("FileHashUtil", "Error calculating checksum for $uri", e)
            null
        }
    }
    
    /**
     * Calculates MD5 checksum from an input stream.
     * Uses buffered reading to handle large files efficiently.
     */
    private fun calculateMD5FromStream(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(8192) // 8KB buffer
        var bytesRead: Int
        
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
