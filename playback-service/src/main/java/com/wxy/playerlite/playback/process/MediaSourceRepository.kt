package com.wxy.playerlite.playback.process

import android.content.Context
import android.net.Uri
import com.wxy.playerlite.player.source.ContentUriSource
import com.wxy.playerlite.player.source.IPlaysource
import com.wxy.playerlite.player.source.LocalFileSource
import java.io.File

internal class MediaSourceRepository(
    private val appContext: Context
) {
    fun createPlayableSource(uri: Uri): IPlaysource? {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val file = File(path)
            if (file.exists() && file.canRead()) {
                return LocalFileSource(file)
            }
            return null
        }
        val isReadable = runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { true } ?: false
        }.getOrDefault(false)
        if (!isReadable) {
            return null
        }
        return ContentUriSource(appContext, uri)
    }

    fun hasPersistedReadPermission(uri: Uri): Boolean {
        if (uri.scheme != "content") {
            return false
        }
        return appContext.contentResolver.persistedUriPermissions.any { permission ->
            permission.isReadPermission && permission.uri == uri
        }
    }
}
