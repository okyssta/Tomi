package com.tomi.app.update

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class InstallUpdateActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra(EXTRA_APK_PATH)
        val apk = path?.let(::File)
        if (apk == null || !apk.exists()) {
            finish()
            return
        }
        if (!packageManager.canRequestPackageInstalls()) {
            if (!openSettings(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:$packageName".toUri()))) {
                showError("Allow Tomi to install updates in system settings, then try again.")
            }
            finish()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apk)
            openSettings(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        } catch (error: Exception) {
            Log.e(TAG, "Could not launch the package installer", error)
            showError("The update could not be opened. Download the APK again and retry.")
        }
        finish()
    }

    private fun openSettings(intent: Intent): Boolean = runCatching {
        startActivity(intent)
    }.isSuccess

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "TomiInstallUpdate"
        const val EXTRA_APK_PATH = InstallUpdateContract.EXTRA_APK_PATH
        const val EXTRA_VERSION = InstallUpdateContract.EXTRA_VERSION
    }
}
