package com.rrrainielll.teledrop.utils

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility class for gathering device information for registration notifications.
 */
object DeviceInfoHelper {
    
    /**
     * Returns the device name (manufacturer + model).
     * Example: "Samsung Galaxy S21"
     */
    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
        val model = Build.MODEL
        
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }
    
    /**
     * Returns the Android version info.
     * Example: "Android 14 (API 34)"
     */
    fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }
    
    /**
     * Fetches the public IP address using ipify.org.
     * Returns null if the request fails.
     */
    suspend fun getPublicIpAddress(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.ipify.org?format=json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                json.optString("ip", null)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets approximate location (city, country) from IP address using ip-api.com.
     * Returns "Unknown" if the request fails.
     */
    suspend fun getLocationFromIp(ip: String?): String = withContext(Dispatchers.IO) {
        if (ip.isNullOrBlank()) return@withContext "Unknown"
        
        try {
            val url = URL("http://ip-api.com/json/$ip?fields=status,city,regionName,country")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                
                if (json.optString("status") == "success") {
                    val city = json.optString("city", "")
                    val region = json.optString("regionName", "")
                    val country = json.optString("country", "")
                    
                    buildString {
                        if (city.isNotBlank()) append(city)
                        if (region.isNotBlank() && region != city) {
                            if (isNotBlank()) append(", ")
                            append(region)
                        }
                        if (country.isNotBlank()) {
                            if (isNotBlank()) append(", ")
                            append(country)
                        }
                    }.ifBlank { "Unknown" }
                } else {
                    "Unknown"
                }
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Returns the current date and time formatted for display.
     * Example: "2026-01-05 00:45:00 (UTC+8)"
     */
    fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timezone = TimeZone.getDefault()
        val offsetHours = timezone.rawOffset / (1000 * 60 * 60)
        val offsetSign = if (offsetHours >= 0) "+" else ""
        
        return "${dateFormat.format(Date())} (UTC$offsetSign$offsetHours)"
    }
}
