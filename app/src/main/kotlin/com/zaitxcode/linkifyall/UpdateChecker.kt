package com.zaitxcode.linkifyall

import android.content.Context
import android.content.pm.PackageInfo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object UpdateChecker {

    // --- GitHub Config ---
    private const val GITHUB_USER = "zaitxcode"
    private const val GITHUB_REPO = "LinkifyAll"
    // API لجلب آخر إصدار (الأحدث زمنياً)
    private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases?per_page=1"
    private const val DEFAULT_GITHUB_URL = "https://github.com/$GITHUB_USER/$GITHUB_REPO"
    private const val DEFAULT_WEBSITE_URL = "https://zaitxcode.com/linkifyall"

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

    // النطاق يستخدم Job قابل للإلغاء بواسطة المتصل
    private var currentJob: Job? = null

    private fun createScope(): CoroutineScope {
        val job = Job()
        currentJob = job
        return CoroutineScope(SupervisorJob() + Dispatchers.IO + job)
    }

    /**
     * تحقق من وجود تحديث جديد.
     * @param context السياق
     * @param listener مستمع نتائج الفحص
     * @param parentJob Job الخاص بمدير النشاط، يُلغي العملية عند إغلاقه
     */
    fun checkForUpdate(context: Context, parentJob: Job, listener: UpdateListener) {
        // إلغاء العملية السابقة إن وجدت
        currentJob?.cancel()
        
        val scope = if (parentJob != null) {
            CoroutineScope(SupervisorJob(parentJob) + Dispatchers.IO)
        } else {
            createScope()
        }
        
        scope.launch {
            // التحقق من إلغاء العملية بانتظام
            if (!this.isActive) {
                withContext(Dispatchers.Main) { listener.onError("Update check cancelled") }
                return@launch
            }
            try {
                // Fetch latest release from GitHub (HTTPS only)
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.instanceFollowRedirects = false // منع إعادة التوجيه لتجنب SSRF
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
                        
                        val latestVersionName = cleanTagName
                        val currentVersionName = getAppVersionName(context)
                        
                        val updateAvailable = isNewerVersion(latestVersionName, currentVersionName)
                        
                        var apkUrl = ""
                        val assets = latestRelease.getJSONArray("assets")
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.getString("name").endsWith(".apk")) {
                                val rawUrl = asset.getString("browser_download_url")
                                // Security: Validate download URL to prevent SSRF
                                if (isSafeGitHubUrl(rawUrl)) {
                                    apkUrl = rawUrl
                                }
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
    
    /**
     * الحصول على اسم إصدار التطبيق الحالي
     * @param context سياق التطبيق
     * @return رقم الإصدار أو نص فارغ في حالة الخطأ
     */
    private fun getAppVersionName(context: Context): String {
        return try {
            val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * خوارزمية مقارنة إصدارات دلالية (Semantic Versioning)
     * تدعم الإصدارات مثل: 1.0.0, 1.0.0-beta, 1.0.0-rc.1, 1.0.0-alpha.2
     * الترتيب: alpha < beta < rc < final (بدون لاحقة)
     * @param remote الإصدار البعيد (من GitHub)
     * @param current الإصدار الحالي (المثبت)
     * @return true إذا كان الإصدار البعيد أحدث
     */
    private fun isNewerVersion(remote: String, current: String): Boolean {
        val preReleaseOrder = mapOf("alpha" to 0, "beta" to 1, "rc" to 2, "" to 3)

        fun parse(v: String): Pair<List<Int>, Pair<Int, Int>> {
            val cleaned = v.trim().replaceFirst(Regex("^[vV]"), "")
            val dashIdx = cleaned.indexOf('-')
            val baseStr = if (dashIdx >= 0) cleaned.substring(0, dashIdx) else cleaned
            val preStr = if (dashIdx >= 0) cleaned.substring(dashIdx + 1) else ""

            val base = baseStr.split(".").map { it.toIntOrNull() ?: 0 }

            val preMatch = Regex("^([a-zA-Z]+)(\\d*)$").find(preStr)
            val preType = preMatch?.groupValues?.get(1)?.lowercase() ?: ""
            val preNum = preMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val preRank = preReleaseOrder[preType] ?: 3

            return Pair(base, Pair(preRank, preNum))
        }

        val (rBase, rPre) = parse(remote)
        val (cBase, cPre) = parse(current)

        val len = maxOf(rBase.size, cBase.size)
        for (i in 0 until len) {
            val rv = rBase.getOrElse(i) { 0 }
            val cv = cBase.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }

        if (rPre.first != cPre.first) return rPre.first > cPre.first
        return rPre.second > cPre.second
    }

    /**
     * التحقق من أن الرابط ينتمي لنطاقات GitHub الموثوقة فقط
     * يمنع هجمات SSRF عبر روابط تنزيل مزيفة في releases
     */
    private fun isSafeGitHubUrl(url: String): Boolean {
        return try {
            val parsed = URL(url)
            val host = parsed.host?.lowercase() ?: return false
            val protocol = parsed.protocol?.lowercase() ?: return false

            // السماح فقط بـ HTTPS
            if (protocol != "https") return false

            // نطاقات GitHub المسموحة فقط
            val allowedHosts = listOf(
                "github.com",
                "objects.githubusercontent.com",
                "api.github.com",
                "raw.githubusercontent.com"
            )
            allowedHosts.any { host == it || host.endsWith(".$it") }
        } catch (_: Exception) {
            false
        }
    }
}
