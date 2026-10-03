package com.athleteapp.pro.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val repoOwner = "santiyastudio-lgtm"
    private val repoName = "fitness-ecosystem-pro"

    private fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.1"
        } catch (_: Exception) {
            "1.0.1"
        }
    }

    suspend fun checkForUpdates(): Result<AthleteUpdateCheckResult> = withContext(Dispatchers.IO) {
        try {
            val currentVersionName = getCurrentVersionName()

            // 1. Приоритетно проверяем обновление через Google Диск (AES-256)
            try {
                val endpoint = com.athleteapp.pro.data.sync.CloudSecurityManager.getEndpointUrl()
                val secretKey = com.athleteapp.pro.data.sync.CloudSecurityManager.getSecretKey()
                val encodedKey = java.net.URLEncoder.encode(secretKey, "UTF-8")
                val gDriveUrl = "$endpoint?key=$encodedKey"

                val rawCloud = httpGet(gDriveUrl)
                val decryptedJson = com.athleteapp.pro.data.sync.CloudSecurityManager.decryptPayload(rawCloud)

                if (decryptedJson.isNotBlank() && decryptedJson != "{}") {
                    val root = com.google.gson.JsonParser.parseString(decryptedJson).asJsonObject
                    if (root.has("updates")) {
                        val updatesNode = root.getAsJsonObject("updates")
                        val remoteVersion = updatesNode.get("athleteVersion")?.asString?.removePrefix("v")?.trim() ?: ""
                        val downloadUrl = updatesNode.get("athleteUrl")?.asString
                        val notes = updatesNode.get("notes")?.asString ?: "Новое обновление Athlete Pro доступно на Google Диске"

                        if (remoteVersion.isNotBlank()) {
                            val isNewer = isVersionNewer(remoteVersion, currentVersionName)
                            return@withContext Result.success(
                                AthleteUpdateCheckResult(
                                    isUpdateAvailable = isNewer,
                                    currentVersion = currentVersionName,
                                    latestVersion = remoteVersion,
                                    releaseNotes = notes,
                                    downloadUrl = downloadUrl
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // В случае ошибки на Google Диске используем резервный GitHub API
            }

            // 2. Резервный источник: GitHub API
            val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "AthletePro-App")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().readText()
                val release = gson.fromJson(json, GitHubRelease::class.java)
                val cleanTag = release.tag_name.removePrefix("v").trim()
                val apkAsset = release.assets?.find { it.name.contains("athlete", ignoreCase = true) && it.name.endsWith(".apk") }
                    ?: release.assets?.find { it.name.endsWith(".apk") }

                val isNewer = isVersionNewer(cleanTag, currentVersionName)
                Result.success(
                    AthleteUpdateCheckResult(
                        isUpdateAvailable = isNewer,
                        currentVersion = currentVersionName,
                        latestVersion = cleanTag,
                        releaseNotes = release.body ?: "Доступно обновление приложения Athlete Pro",
                        downloadUrl = apkAsset?.browser_download_url
                    )
                )
            } else {
                Result.success(
                    AthleteUpdateCheckResult(
                        isUpdateAvailable = false,
                        currentVersion = currentVersionName,
                        latestVersion = currentVersionName,
                        releaseNotes = "Установлена актуальная версия ($currentVersionName)",
                        downloadUrl = null
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun httpGet(urlStr: String): String {
        var currentUrl = urlStr
        var redirects = 0
        while (redirects < 5) {
            val conn = URL(currentUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"

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

    fun downloadAndInstallApk(downloadUrl: String) {
        try {
            val uri = Uri.parse(downloadUrl)
            val request = DownloadManager.Request(uri)
                .setTitle("Athlete Pro Update")
                .setDescription("Загрузка обновления Athlete Pro...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AthletePro_Update.apk")
                .setMimeType("application/vnd.android.package-archive")

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        }
    }

    private fun isVersionNewer(remote: String, local: String): Boolean {
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
