package com.tomi.app.backup.drive

import android.net.Uri
import java.io.InputStream

interface CloudBackupStore {
    fun persist(treeUri: Uri): String
    fun release(treeUri: Uri)
    fun write(treeUri: Uri, content: String)
    fun openInput(treeUri: Uri): InputStream
}
