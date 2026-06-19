package com.manichord.mgit.util

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract

/**
 * Document tree pickers (ActivityResultContracts.OpenDocumentTree) return a content:// SAF
 * URI, not a filesystem path -- but Repo/JGit need a real java.io.File path. MGit holds
 * MANAGE_EXTERNAL_STORAGE, so for the primary volume we can resolve the tree's document ID
 * straight to its absolute path. Other volumes (e.g. an SD card) aren't resolvable this way;
 * returns null for those so callers can fall back to an error message instead of silently
 * constructing a broken path out of the raw URI string.
 */
fun resolvePrimaryVolumePath(uri: Uri): String? {
    val docId = DocumentsContract.getTreeDocumentId(uri)
    val parts = docId.split(":", limit = 2)
    val volume = parts.getOrNull(0) ?: return null
    if (!volume.equals("primary", ignoreCase = true)) return null
    val relativePath = parts.getOrElse(1) { "" }
    val base = Environment.getExternalStorageDirectory().absolutePath
    return if (relativePath.isEmpty()) base else "$base/$relativePath"
}
