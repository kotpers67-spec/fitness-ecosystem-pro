package com.athleteapp.pro.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
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
    val name: String?,
    val body: String?,
    val assets: List<GitHubAsset>?
)

data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long
)

data class AthleteUpdateCheckResult(
    val isUpdateAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String?
)

class AthleteUpdateService(private val context: Context) {

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

    suspend fun checkForUpdates(): Result<AthleteUpdateCheckResult> = withContext(Dispatchers.IO) {
        try {
            val currentVersionName = getCurrentVersionName()

            // 1. Primary: check our own server /api/version (always has the real latest)
            val serverEndpoints = listOf(
                "https://fitness-ecosystem-pro.onrender.com/api/version",
                "https://fitness-ecosystem-pro.onrender.com/api/version"
            )
            for (apiUrl in serverEndpoints) {
                try {
                    val url = URL(apiUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.setRequestProperty("Accept", "application/json")
                    conn.setRequestProperty("User-Agent", "AthletePro-App")
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000

                    if (conn.responseCode == 200) {
                        val json = conn.inputStream.bufferedReader().readText()
                        val parsed = gson.fromJson(json, com.google.gson.JsonObject::class.java)
                        val athleteObj = parsed.getAsJsonObject("athlete")
                        val latestVersion = athleteObj?.get("version")?.asString ?: parsed.get("latest")?.asString ?: continue
                        val downloadUrl = athleteObj?.get("url")?.asString ?: continue
                        val changelog = athleteObj?.get("changelog")?.asString ?: "Доступно обновление v$latestVersion"

                        val isNewer = isVersionNewer(latestVersion, currentVersionName)
                        return@withContext Result.success(
                            AthleteUpdateCheckResult(
                                isUpdateAvailable = isNewer,
                                currentVersion = currentVersionName,
                                latestVersion = latestVersion,
                                releaseNotes = changelog,
                                downloadUrl = downloadUrl
                            )
                        )
                    }
                } catch (_: Exception) {}
                break
            }

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
                    conn.setRequestProperty("User-Agent", "AthletePro-App")
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000

                    if (conn.responseCode == 200) {
                        val json = conn.inputStream.bufferedReader().readText()
                        val release = gson.fromJson(json, GitHubRelease::class.java)
                        val cleanTag = release.tag_name.removePrefix("v").trim()
                        val apkAsset = release.assets?.find { it.name.contains("athlete", ignoreCase = true) && it.name.endsWith(".apk") }
                            ?: release.assets?.find { it.name.endsWith(".apk") }

                        val downloadUrl = apkAsset?.browser_download_url
                            ?: "https://fitness-ecosystem-pro.onrender.com/releases/athlete-pro-v$cleanTag.apk"

                        val isNewer = isVersionNewer(cleanTag, currentVersionName)
                        return@withContext Result.success(
                            AthleteUpdateCheckResult(
                                isUpdateAvailable = isNewer,
                                currentVersion = currentVersionName,
                                latestVersion = cleanTag,
                                releaseNotes = release.body ?: "Доступно официальное обновление Athlete Pro v$cleanTag",
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
                AthleteUpdateCheckResult(
                    isUpdateAvailable = isFallbackNewer,
                    currentVersion = currentVersionName,
                    latestVersion = fallbackVersion,
                    releaseNotes = "Официальный релиз Athlete Pro v$fallbackVersion доступен для загрузки.",
                    downloadUrl = "https://fitness-ecosystem-pro.onrender.com/releases/athlete-pro-v$fallbackVersion.apk"
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
        val candidateUrls = mutableListOf<String>()
        if (downloadUrl.isNotBlank()) candidateUrls.add(downloadUrl)
        candidateUrls.add("https://fitness-ecosystem-pro.onrender.com/releases/athlete-pro-v2.0.1.apk")
        candidateUrls.add("https://github.com/kotpers67-spec/fitness-ecosystem-pro/releases/download/v2.0.1/athlete-pro-v2.0.1.apk")

        val targetFile = File(context.cacheDir, "AthletePro_Update.apk")

        for (candidate in candidateUrls.distinct()) {
            try {
                if (targetFile.exists()) targetFile.delete()

                var currentUrl = candidate
                var redirects = 0
                var conn: HttpURLConnection? = null

                while (redirects < 10) {
                    val url = URL(currentUrl)
                    conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 20000
                    conn.readTimeout = 120000
                    conn.instanceFollowRedirects = true
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; AthletePro)")

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
                            val buffer = ByteArray(32 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                        }
                    }
                    if (targetFile.length() > 2000000L && isValidZipApk(targetFile)) {
                        return@withContext targetFile
                    } else {
                        targetFile.delete()
                    }
                }
            } catch (_: Exception) {}
        }
        null
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
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val apkFile = downloadApkDirectly(downloadUrl)
            if (apkFile != null && isValidZipApk(apkFile)) {
                withContext(Dispatchers.Main) {
                    launchApkInstallation(apkFile)
                }
            } else {
                // Background download manager fallback (no browser redirect)
                try {
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    if (dm != null) {
                        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                            setTitle("Обновление Athlete Pro")
                            setDescription("Фоновая загрузка APK...")
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "AthletePro_Update.apk")
                            setAllowedOverMetered(true)
                            setAllowedOverRoaming(true)
                        }
                        dm.enqueue(request)
                    }
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
