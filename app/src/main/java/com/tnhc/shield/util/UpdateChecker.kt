package com.tnhc.shield.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val releaseUrl: String,
    val downloadUrl: String?,
    val releaseNotes: String?
)

object UpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/The-No-Hands-company/Adblocker/releases/latest"

    suspend fun checkForUpdates(currentVersion: String, client: OkHttpClient): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Shield-Android-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("GitHub API returned ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val json = JSONObject(body)
                val tagName = json.getString("tag_name").trimStart('v')
                val htmlUrl = json.optString("html_url", "https://github.com/The-No-Hands-company/Adblocker/releases")
                val notes = json.optString("body", "")

                var apkDownloadUrl: String? = null
                if (json.has("assets")) {
                    val assets = json.getJSONArray("assets")
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkDownloadUrl = asset.optString("browser_download_url", null)
                            break
                        }
                    }
                }

                val hasUpdate = isNewerVersion(tagName, currentVersion.trimStart('v'))
                Result.success(
                    UpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersion = tagName,
                        releaseUrl = htmlUrl,
                        downloadUrl = apkDownloadUrl,
                        releaseNotes = notes
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split('.').mapNotNull { it.toIntOrNull() }
        val currentParts = current.split('.').mapNotNull { it.toIntOrNull() }

        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
