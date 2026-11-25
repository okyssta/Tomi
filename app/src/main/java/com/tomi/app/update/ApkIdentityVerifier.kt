package com.tomi.app.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

object ApkIdentityVerifier {
    const val EXPECTED_CERTIFICATE_SHA256 = "fee392206a600443c5ad6fa469a81e44dc378bd651d41971c8f888f6bdf935d8"

    fun verify(
        context: Context,
        apk: File,
        expectedPackage: String,
        expectedVersionName: String,
    ): Boolean = runCatching {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES or
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    PackageManager.GET_SIGNATURES
                } else {
                    0
                }
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: return@runCatching false
        if (info.packageName != expectedPackage || info.versionName != expectedVersionName) return@runCatching false

        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.toList().orEmpty()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.toList().orEmpty()
        }
        signatures.isNotEmpty() && signatures.all { signature ->
            val digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            digest.joinToString("") { "%02x".format(it) }.equals(EXPECTED_CERTIFICATE_SHA256, ignoreCase = true)
        }
    }.getOrDefault(false)
}
