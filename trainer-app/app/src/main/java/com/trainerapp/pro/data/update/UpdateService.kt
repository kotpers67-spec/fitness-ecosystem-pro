package com.trainerapp.pro.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val body: String,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long
)

data class UpdateCheckResult(
    val isUpdateAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String?
)

class UpdateService(private val context: Context) {

    private val gson = Gson()
    private val repoOwner = "kotpers67-spec"
    private val repoName = "fitness-ecosystem-pro"

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "2.0.1"
        } catch (_: Exception) {
            "2.0.1"
        }
    }

    suspend fun checkForUpdates(): Result<UpdateCheckResult> = withContext(Dispatchers.IO) {
        try {
            val currentVersionName = getCurrentVersionName()

            // 1. Primary: check our own server /api/version (always has the real latest)
            try {
                val url = URL("https://fitness-ecosystem-pro.onrender.com/api/version")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "TrainerPro-App")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                if (conn.responseCode == 200) {
                    val json = conn.inputStream.bufferedReader().readText()
                    val parsed = gson.fromJson(json, com.google.gson.JsonObject::class.java)
                    val trainerObj = parsed.getAsJsonObject("trainer")
                    val latestVersion = trainerObj?.get("version")?.asString ?: parsed.get("latest")?.asString
                    val downloadUrl = trainerObj?.get("url")?.asString
                    val changelog = trainerObj?.get("changelog")?.asString ?: "Доступно обновление v$latestVersion"

                    if (latestVersion != null && downloadUrl != null) {
                        val isNewer = isVersionNewer(latestVersion, currentVersionName)
                        return@withContext Result.success(
                            UpdateCheckResult(
                                isUpdateAvailable = isNewer,
                                currentVersion = currentVersionName,
                                latestVersion = latestVersion,
                                releaseNotes = changelog,
                                downloadUrl = downloadUrl
                            )
                        )
                    }
                }
            } catch (_: Exception) {}

            // 2. Secondary: Official GitHub Releases API
            val apiEndpoints = listOf(
                "https://api.github.com/repos/$repoOwner/$repoName/releases/latest",
                "https://api.github.com/repos/$repoOwner/render-auth-bot/releases/latest"
            )

            for (apiUrl in apiEndpoints) {
                try {
                    val url = URL(apiUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.setRequestProperty("Accept", "application/vnd.github+json")
                    conn.setRequestProperty("User-Agent", "TrainerPro-App")
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000

                    if (conn.responseCode == 200) {
                        val json = conn.inputStream.bufferedReader().readText()
                        val release = gson.fromJson(json, GitHubRelease::class.java)
                        val cleanTag = release.tag_name.removePrefix("v").trim()
                        val apkAsset = release.assets.find { it.name.contains("trainer", ignoreCase = true) && it.name.endsWith(".apk") }
                            ?: release.assets.find { it.name.endsWith(".apk") }

                        val downloadUrl = apkAsset?.browser_download_url
                            ?: "https://fitness-ecosystem-pro.onrender.com/releases/trainer-pro-v$cleanTag.apk"

                        val isNewer = isVersionNewer(cleanTag, currentVersionName)
                        return@withContext Result.success(
                            UpdateCheckResult(
                                isUpdateAvailable = isNewer,
                                currentVersion = currentVersionName,
                                latestVersion = cleanTag,
                                releaseNotes = release.body ?: "Доступно официальное обновление Trainer Pro v$cleanTag",
                                downloadUrl = downloadUrl
                            )
                        )
                    }
                } catch (_: Exception) {}
            }

            // 3. Static fallback
            val fallbackVersion = "2.0.1"
            val isFallbackNewer = isVersionNewer(fallbackVersion, currentVersionName)
            Result.success(
                UpdateCheckResult(
                    isUpdateAvailable = isFallbackNewer,
                    currentVersion = currentVersionName,
                    latestVersion = fallbackVersion,
                    releaseNotes = "Официальный релиз Trainer Pro v$fallbackVersion доступен для загрузки.",
                    downloadUrl = "https://fitness-ecosystem-pro.onrender.com/releases/trainer-pro-v$fallbackVersion.apk"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun httpGet(urlStr: String): String {
        var currentUrl = urlStr
        var redirects = 0
        while (redirects < 8) {
            val conn = URL(currentUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 25000
            conn.readTimeout = 25000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")

            val code = conn.responseCode
            if (code in 300..308) {
                val loc = conn.getHeaderField("Location") ?: break
                currentUrl = loc
                redirects++
                continue
            }

            return if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                ""
            }
        }
        return ""
    }

    suspend fun downloadApkDirectly(downloadUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(context.cacheDir, "TrainerPro_Update.apk")
            if (targetFile.exists()) targetFile.delete()

            var currentUrl = downloadUrl
            var redirects = 0
            var conn: HttpURLConnection? = null

            while (redirects < 8) {
                val url = URL(currentUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")

                val code = conn.responseCode
                if (code in 300..308) {
                    val loc = conn.getHeaderField("Location") ?: break
                    currentUrl = loc
                    redirects++
                    continue
                }
                break
            }

            if (conn != null && conn.responseCode in 200..299) {
                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (targetFile.length() > 2000000L && isValidZipApk(targetFile)) {
                    return@withContext targetFile
                } else {
                    targetFile.delete()
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun isValidZipApk(file: File): Boolean {
        return try {
            if (!file.exists()) return false
            java.io.FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                val read = fis.read(header)
                read == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun downloadAndInstallApk(downloadUrl: String) {
        // Fallback or explicit trigger: try direct HTTP download first, then launch installer
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val apkFile = downloadApkDirectly(downloadUrl)
            if (apkFile != null && isValidZipApk(apkFile)) {
                withContext(Dispatchers.Main) {
                    launchApkInstallation(apkFile)
                }
            } else {
                // Fallback to browser or DownloadManager
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(browserIntent)
                } catch (_: Exception) {}
            }
        }
    }

    fun launchApkInstallation(apkFile: File) {
        try {
            if (!apkFile.exists() || !isValidZipApk(apkFile)) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(manageIntent)
                }
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    fun isVersionNewer(remote: String, local: String): Boolean {
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val lParts = local.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(rParts.size, lParts.size)) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
