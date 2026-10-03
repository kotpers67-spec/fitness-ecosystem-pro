package com.athleteapp.pro.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId == -1L) return

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor != null && cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    val localUriString = if (uriIndex >= 0) cursor.getString(uriIndex) else null

                    val apkUri: Uri? = if (localUriString != null) {
                        val file = File(Uri.parse(localUriString).path ?: "")
                        if (file.exists()) {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        } else {
                            downloadManager.getUriForDownloadedFile(downloadId)
                        }
                    } else {
                        downloadManager.getUriForDownloadedFile(downloadId)
                    }

                    if (apkUri != null) {
                        launchInstallIntent(context, apkUri)
                    }
                }
                cursor.close()
            }
        }
    }

    private fun launchInstallIntent(context: Context, apkUri: Uri) {
        try {
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (_: Exception) {}
    }
}
