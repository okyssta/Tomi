package com.tomi.app.update

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:$packageName".toUri(),
                ),
            )
            finish()
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apk)
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        finish()
    }

    companion object {
        const val EXTRA_APK_PATH = "apk_path"
        const val EXTRA_VERSION = "version"
    }
}
