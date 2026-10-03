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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
    private val currentVersionName = "1.0.0"
    private val repoOwner = "kotpe"
    private val repoName = "trainer-app-android"

    suspend fun checkForUpdates(): Result<UpdateCheckResult> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "TrainerPro-App")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().readText()
                val release = gson.fromJson(json, GitHubRelease::class.java)
                val cleanTag = release.tag_name.removePrefix("v").trim()
                val apkAsset = release.assets.find { it.name.endsWith(".apk") }

                val isNewer = isVersionNewer(cleanTag, currentVersionName)
                Result.success(
                    UpdateCheckResult(
                        isUpdateAvailable = isNewer,
                        currentVersion = currentVersionName,
                        latestVersion = cleanTag,
                        releaseNotes = release.body ?: "Новая версия доступна",
                        downloadUrl = apkAsset?.browser_download_url
                    )
                )
            } else {
                // If repo not yet public/has no releases, report up-to-date
                Result.success(
                    UpdateCheckResult(
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

    fun downloadAndInstallApk(downloadUrl: String) {
        try {
            val targetFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "TrainerPro_Update.apk")
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val uri = Uri.parse(downloadUrl)
            val request = DownloadManager.Request(uri)
                .setTitle("Trainer Pro Update")
                .setDescription("Загрузка новой версии приложения...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "TrainerPro_Update.apk")
                .setMimeType("application/vnd.android.package-archive")

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                    if (id == downloadId) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (_: Exception) {}
                        launchApkInstallation(targetFile)
                    }
                }
            }

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            // Fallback to browser download if DownloadManager fails
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        }
    }

    private fun launchApkInstallation(apkFile: File) {
        try {
            if (!apkFile.exists()) return

            // Check REQUEST_INSTALL_PACKAGES permission on Android 8.0+ (Oreo) and Android 14+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(manageIntent)
                    // Continue to attempt install flow
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
            // Fallback if FileProvider fails
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
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
