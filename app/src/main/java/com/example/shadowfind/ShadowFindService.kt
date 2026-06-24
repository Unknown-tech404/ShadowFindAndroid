package com.example.shadowfind

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject

data class ScanResult(
    val totalLinks: Int,
    val internalCount: Int,
    val externalCount: Int,
    val imageCount: Int,
    val scriptCount: Int,
    val emailCount: Int,
    val phoneCount: Int,
    val scanTime: Double,
    val links: List<String>,
    val rawData: String
)

class ShadowFindService(private val context: Context) {
    
    private var py: Python? = null
    
    init {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        py = Python.getInstance()
    }
    
    fun scanWebsite(url: String, threads: Int, depth: Int, username: String, password: String): Result<ScanResult> {
        return try {
            val module = py!!.getModule("shadow_find")
            val result = module.callAttr("scan_android", url, threads, depth, username, password)
            val resultStr = result.toString()
            
            val json = JSONObject(resultStr)
            val success = json.getBoolean("success")
            
            if (success) {
                val data = json.getJSONObject("data")
                val scanResult = ScanResult(
                    totalLinks = data.getInt("total_links"),
                    internalCount = data.getInt("internal_links"),
                    externalCount = data.getInt("external_links"),
                    imageCount = data.getInt("images"),
                    scriptCount = data.getInt("scripts"),
                    emailCount = data.getInt("emails"),
                    phoneCount = data.getInt("phone_numbers"),
                    scanTime = data.getDouble("scan_time"),
                    links = data.getJSONArray("links").let { 
                        (0 until it.length()).map { i -> it.getString(i) }
                    },
                    rawData = resultStr
                )
                Result.success(scanResult)
            } else {
                val error = json.getString("error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
