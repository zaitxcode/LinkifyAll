package com.mohamedzaitoon.linkifyall

import android.content.Context
import android.content.pm.PackageInfo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    // --- GitHub Config ---
    private const val GITHUB_USER = "mohamed-zaitoon"
    private const val GITHUB_REPO = "LinkifyAll"
    // API لجلب آخر إصدار (الأحدث زمنياً)
    private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases?per_page=1"
    private const val DEFAULT_GITHUB_URL = "https://github.com/$GITHUB_USER/$GITHUB_REPO"
    private const val DEFAULT_WEBSITE_URL = "https://linkifyall.mohamedzaitoon.com"

    // --- GitHub API Keys (Simulating Remote Config locally or via simple fetch if needed, but here we stick to direct GitHub API for version check) ---
    // Since we removed Firebase, we will rely on GitHub Releases for version info too if possible,
    // or keep a local constant for now. For a robust solution without backend, checking GitHub releases for tag name is best.
    // However, GitHub API returns tag name (e.g., v0.5.0). We need to parse it.
    // To keep it simple and robust without Firebase: We will check the latest release tag from GitHub API.

    data class ConfigResult(
        val githubUrl: String,
        val websiteUrl: String,
        val updateAvailable: Boolean = false,
        val latestVersionName: String = "",
        val downloadUrl: String = "",
        val releaseNotes: String = ""
    )

    interface UpdateListener {
        fun onConfigFetched(result: ConfigResult)
        fun onError(error: String)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun checkForUpdate(context: Context, listener: UpdateListener) {
        scope.launch {
            try {
                val currentCode = getAppVersionCode(context)
                
                // Fetch latest release from GitHub
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "LinkifyAll-App")
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val releases = JSONArray(response)
                    
                    if (releases.length() > 0) {
                        val latestRelease = releases.getJSONObject(0)
                        val tagName = latestRelease.getString("tag_name") // e.g., "v0.5.0"
                        val body = latestRelease.optString("body", "") // Release notes
                        
                        // Parse version from tag (remove 'v' prefix if present)
                        val cleanTagName = tagName.replaceFirst("^v", "", ignoreCase = true)
                        
                        // Note: Comparing string versions is not ideal, but without a backend to provide integer versionCode,
                        // we rely on tag name. For robust comparison, one might need to parse semver or rely on a separate version file.
                        // Here we assume the user checks visually or we try to parse if possible. 
                        // To keep it simple and working like before: we will just check if tag name differs from current versionName?
                        // Or better: Just provide the info and let the user decide, OR try to parse versionCode if stored in release assets/meta.
                        
                        // Since we removed Firebase Remote Config which held the integer versionCode,
                        // we can't do a strict integer comparison easily unless we parse the tag or have a convention.
                        // Let's assume the tag name matches versionName. 
                        // To maintain "Update Available" logic, we might need to fetch a version code from somewhere.
                        // Alternative: Use GitHub Releases API to get the latest tag, and compare with current versionName.
                        
                        val latestVersionName = cleanTagName
                        val currentVersionName = getAppVersionName(context)
                        
                        val updateAvailable = latestVersionName != currentVersionName
                        
                        var apkUrl = ""
                        val assets = latestRelease.getJSONArray("assets")
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.getString("name").endsWith(".apk")) {
                                apkUrl = asset.getString("browser_download_url")
                                break
                            }
                        }
                        
                        if (apkUrl.isEmpty()) {
                             apkUrl = DEFAULT_GITHUB_URL
                        }

                        withContext(Dispatchers.Main) {
                            listener.onConfigFetched(
                                ConfigResult(
                                    githubUrl = DEFAULT_GITHUB_URL,
                                    websiteUrl = DEFAULT_WEBSITE_URL,
                                    updateAvailable = updateAvailable,
                                    latestVersionName = latestVersionName,
                                    downloadUrl = apkUrl,
                                    releaseNotes = body
                                )
                            )
                        }
                    } else {
                        withContext(Dispatchers.Main) { listener.onError("No releases found") }
                    }
                } else {
                    withContext(Dispatchers.Main) { listener.onError("GitHub API Error: ${connection.responseCode}") }
                }
                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { listener.onError(e.message ?: "Unknown error") }
            }
        }
    }
    
    private fun getAppVersionName(context: Context): String {
        return try {
            val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // دالة لجلب رابط الـ APK من GitHub API
    private fun fetchDynamicUrlFromGitHub(callback: (String?, String?) -> Unit) {
        scope.launch {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                // Headers لتجنب الحظر
                connection.setRequestProperty("User-Agent", "LinkifyAll-App")
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                try {
                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val releases = JSONArray(response)

                        if (releases.length() > 0) {
                            val latestJson = releases.getJSONObject(0)
                            val assets = latestJson.getJSONArray("assets")

                            var apkUrl = ""
                            // البحث عن أول ملف ينتهي بـ .apk
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                if (asset.getString("name").endsWith(".apk")) {
                                    apkUrl = asset.getString("browser_download_url")
                                    break
                                }
                            }

                            // العودة بالنتيجة للـ Main Thread
                            withContext(Dispatchers.Main) {
                                if (apkUrl.isNotEmpty()) callback(apkUrl, null)
                                else callback(null, "No APK found in release")
                            }
                        } else {
                            withContext(Dispatchers.Main) { callback(null, "No releases found") }
                        }
                    } else {
                        withContext(Dispatchers.Main) { callback(null, "GitHub Error: ${connection.responseCode}") }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback(null, e.message) }
            }
        }
    }

    private fun getAppVersionCode(context: Context): Long {
        return try {
            val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.longVersionCode
        } catch (e: Exception) { -1L }
    }
}
