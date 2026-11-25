package com.tomi.app.update

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.workDataOf
import androidx.work.WorkerParameters
import com.tomi.app.R
import com.tomi.app.BuildConfig
import com.tomi.app.reminder.ReminderNotifier
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException

class UpdateDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo(0)

    override suspend fun doWork(): Result {
        val apkUrl = inputData.getString(KEY_APK_URL) ?: return Result.failure()
        val apkAssetName = inputData.getString(KEY_APK_ASSET_NAME) ?: return Result.failure()
        val checksumUrl = inputData.getString(KEY_CHECKSUM_URL) ?: return Result.failure()
        val version = inputData.getString(KEY_VERSION) ?: return Result.failure()
        if (!GitHubReleaseUrls.isAllowed(apkUrl, BuildConfig.GITHUB_REPOSITORY) ||
            !GitHubReleaseUrls.isAllowed(checksumUrl, BuildConfig.GITHUB_REPOSITORY) ||
            !Regex("^[A-Za-z0-9._-]+$").matches(apkAssetName) ||
            !Regex("^[A-Za-z0-9._-]+$").matches(version)
        ) {
            return Result.failure()
        }
        val directory = File(applicationContext.cacheDir, "updates")
        val apk = File(directory, "tomi-$version.apk")
        var apkTemporary: File? = null
        var checksumTemporary: File? = null
        var completed = false

        return try {
            apkTemporary = DownloadPolicy.newTemporaryFile(directory, version, "apk")
            checksumTemporary = DownloadPolicy.newTemporaryFile(directory, version, "checksum")
            setForeground(createForegroundInfo(0))
            downloadToFile(checksumUrl, checksumTemporary, DownloadPolicy.MAX_CHECKSUM_BYTES) { }
            val checksumContent = checksumTemporary.readText()
            downloadToFile(apkUrl, apkTemporary, DownloadPolicy.MAX_APK_BYTES) { progress ->
                setForeground(createForegroundInfo(progress))
            }
            if (!ApkVerifier.verify(apkTemporary, checksumContent, apkAssetName)) {
                error("Downloaded APK checksum did not match")
            }
            if (!ApkIdentityVerifier.verify(applicationContext, apkTemporary, BuildConfig.APPLICATION_ID, version)) {
                error("Downloaded APK identity or signature did not match")
            }
            checksumTemporary.delete()
            DownloadPolicy.promote(apkTemporary, apk)
            showReadyToInstall(apk, version)
            completed = true
            Result.success(workDataOf(KEY_VERSION to version))
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            Result.failure(workDataOf(KEY_VERSION to version))
        } finally {
            if (!completed) {
                checksumTemporary?.delete()
                apkTemporary?.delete()
            }
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        NotificationManagerCompat.from(applicationContext).createNotificationChannel(
            NotificationChannel(
                ReminderNotifier.CHANNEL_UPDATES,
                applicationContext.getString(R.string.channel_updates_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = applicationContext.getString(R.string.channel_updates_description)
            },
        )
        val notification = NotificationCompat.Builder(applicationContext, ReminderNotifier.CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_tomi)
            .setContentTitle(applicationContext.getString(R.string.update_downloading_title))
            .setContentText(applicationContext.getString(R.string.update_downloading_progress, progress))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress == 0)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_DOWNLOAD, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_DOWNLOAD, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showReadyToInstall(apk: File, version: String) {
        val manager = NotificationManagerCompat.from(applicationContext)
        manager.createNotificationChannel(
            NotificationChannel(
                ReminderNotifier.CHANNEL_UPDATES,
                applicationContext.getString(R.string.channel_updates_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val installIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_READY,
            Intent(applicationContext, InstallUpdateActivity::class.java).apply {
                putExtra(InstallUpdateActivity.EXTRA_APK_PATH, apk.absolutePath)
                putExtra(InstallUpdateActivity.EXTRA_VERSION, version)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching {
            manager.notify(
                NOTIFICATION_READY,
                NotificationCompat.Builder(applicationContext, ReminderNotifier.CHANNEL_UPDATES)
                    .setSmallIcon(R.drawable.ic_stat_tomi)
                    .setContentTitle(applicationContext.getString(R.string.update_ready_title, version))
                    .setContentText(applicationContext.getString(R.string.update_ready_text))
                    .setContentIntent(installIntent)
                    .setAutoCancel(true)
                    .build(),
            )
        }
    }

    private suspend fun downloadToFile(
        url: String,
        destination: File,
        maximumBytes: Long,
        onProgress: suspend (Int) -> Unit,
    ) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Tomi-Android")
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            val total = connection.contentLength.coerceAtLeast(1)
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        downloaded = DownloadPolicy.acceptedSize(downloaded + read, maximumBytes)
                        output.write(buffer, 0, read)
                        val percent = ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent)
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val KEY_APK_URL = "apk_url"
        const val KEY_APK_ASSET_NAME = "apk_asset_name"
        const val KEY_CHECKSUM_URL = "checksum_url"
        const val KEY_VERSION = UpdateDownloadScheduler.KEY_VERSION
        private const val NOTIFICATION_DOWNLOAD = 4001
        private const val NOTIFICATION_READY = 4002
    }
}
