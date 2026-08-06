package com.manichord.mgit.documents

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException
import me.sheimi.sgit.MGitApplication
import me.sheimi.sgit.R
import me.sheimi.sgit.database.RepoDbManager
import me.sheimi.sgit.database.models.Repo
import timber.log.Timber

/**
 * Exposes every repo's working tree as a browsable "Gitling" location in the system Files app
 * and any SAF file picker (issue #49) -- the platform-sanctioned way to let other apps read and
 * edit repo files without Gitling holding any storage permission, and without the repos having
 * to leave the app's own storage.
 *
 * Document ids are "repo:<dbId>" (repo root dir) and "repo:<dbId>/<relative/path>" -- keyed by
 * the repo's stable database id, NOT its folder name, so grants other apps persist keep working
 * across a repo rename. The synthetic top-level id is just "root".
 *
 * Deliberate constraints:
 * - `.git` is hidden from listings and unresolvable by id, so no external app can touch repo
 *   internals through this provider.
 * - All mutations (write-mode open, create, delete, rename) are refused while the repo's
 *   exclusive-task slot is held (see Repo.hasOngoingTask), so an external editor can't race a
 *   checkout/pull/merge that is rewriting the same working tree. The opposite ordering (a git
 *   op starting while an external write is in flight) is not detectable from here and is
 *   accepted, matching what the shared-media-storage option already allows today.
 * - Every method may be invoked by a binder call before MGitApplication.onCreate() has run in
 *   this process (providers are published to the ActivityManager before Application.onCreate),
 *   so nothing is touched in onCreate() and repo enumeration degrades to empty until the app
 *   singleton is ready.
 */
class GitlingDocumentsProvider : DocumentsProvider() {

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        result.newRow().apply {
            add(Root.COLUMN_ROOT_ID, ROOT_ID)
            add(Root.COLUMN_DOCUMENT_ID, ROOT_DOC_ID)
            add(Root.COLUMN_TITLE, context!!.getString(R.string.app_name))
            add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
            add(
                Root.COLUMN_FLAGS,
                Root.FLAG_LOCAL_ONLY or Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_IS_CHILD
            )
        }
        return result
    }

    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        if (documentId == ROOT_DOC_ID) {
            addRootDocRow(result)
        } else {
            val resolved = resolve(documentId)
            addFileRow(result, documentId, resolved)
        }
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String, projection: Array<String>?, sortOrder: String?
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        if (parentDocumentId == ROOT_DOC_ID) {
            for (repo in listRepos()) {
                val dir = repo.dir
                if (dir.isDirectory) {
                    addFileRow(result, repoDocId(repo), Resolved(repo, dir), displayName = repo.localPath)
                }
            }
            return result
        }
        val resolved = resolve(parentDocumentId)
        val children = resolved.file.listFiles() ?: return result
        for (child in children.sortedBy { it.name.lowercase() }) {
            if (child.name == GIT_DIR) continue
            addFileRow(result, childDocId(parentDocumentId, child.name), Resolved(resolved.repo, child))
        }
        return result
    }

    override fun openDocument(
        documentId: String, mode: String, signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val resolved = resolve(documentId)
        if (mode.contains('w') || mode.contains('t')) {
            failIfRepoBusy(resolved.repo)
        }
        return ParcelFileDescriptor.open(resolved.file, ParcelFileDescriptor.parseMode(mode))
    }

    override fun createDocument(
        parentDocumentId: String, mimeType: String, displayName: String
    ): String {
        val parent = resolve(parentDocumentId)
        failIfRepoBusy(parent.repo)
        requireSafeName(displayName)
        var target = File(parent.file, displayName)
        // Match other providers' behaviour when the name is taken: uniquify, don't clobber.
        var attempt = 1
        while (target.exists()) {
            target = File(parent.file, "$displayName (${attempt++})")
        }
        val ok = if (mimeType == Document.MIME_TYPE_DIR) target.mkdir() else target.createNewFile()
        if (!ok) throw FileNotFoundException("Could not create $displayName")
        return childDocId(parentDocumentId, target.name)
    }

    override fun deleteDocument(documentId: String) {
        val resolved = resolve(documentId)
        if (resolved.isRepoRoot) {
            // Deleting a whole repo belongs in Gitling's own UI, where it also cleans up the
            // database entry -- a file manager must not be able to do it as a plain rm -rf.
            throw UnsupportedOperationException("Repos can only be deleted from within Gitling")
        }
        failIfRepoBusy(resolved.repo)
        if (!resolved.file.deleteRecursively()) {
            throw FileNotFoundException("Could not delete ${resolved.file.name}")
        }
    }

    override fun renameDocument(documentId: String, displayName: String): String {
        val resolved = resolve(documentId)
        if (resolved.isRepoRoot) {
            throw UnsupportedOperationException("Repos can only be renamed from within Gitling")
        }
        failIfRepoBusy(resolved.repo)
        requireSafeName(displayName)
        val target = File(resolved.file.parentFile, displayName)
        if (target.exists()) throw FileNotFoundException("$displayName already exists")
        if (!resolved.file.renameTo(target)) {
            throw FileNotFoundException("Could not rename ${resolved.file.name}")
        }
        return documentId.substringBeforeLast('/') + "/" + displayName
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        if (parentDocumentId == ROOT_DOC_ID) return documentId != ROOT_DOC_ID
        return documentId.startsWith("$parentDocumentId/")
    }

    override fun getDocumentType(documentId: String): String {
        if (documentId == ROOT_DOC_ID) return Document.MIME_TYPE_DIR
        return mimeType(resolve(documentId).file)
    }

    // ------------------------------------------------------------------ internals

    /** A resolved document id: the repo it belongs to and the actual file, already validated
     * to be inside the repo's working tree and not under `.git`. */
    private class Resolved(val repo: Repo, val file: File) {
        val isRepoRoot: Boolean get() = file == repo.dir
    }

    private fun resolve(documentId: String): Resolved {
        val body = documentId.removePrefix(REPO_PREFIX)
        if (body == documentId) throw FileNotFoundException("Unknown document id: $documentId")
        val repoId = body.substringBefore('/').toIntOrNull()
            ?: throw FileNotFoundException("Bad document id: $documentId")
        val repo = listRepos().find { it.getID() == repoId }
            ?: throw FileNotFoundException("No such repo: $repoId")
        val relative = body.substringAfter('/', "")
        val repoDir = repo.dir
        val file = if (relative.isEmpty()) repoDir else File(repoDir, relative)
        // Canonical containment check defeats ".." traversal in ids handed to us by other
        // apps; the .git check keeps repo internals unreachable even by a crafted direct id.
        val canonical = file.canonicalFile
        val canonicalRoot = repoDir.canonicalFile
        if (canonical != canonicalRoot && !canonical.startsWith(canonicalRoot)) {
            throw FileNotFoundException("Outside repo: $documentId")
        }
        if (canonical != canonicalRoot &&
            canonical.relativeTo(canonicalRoot).path.split(File.separatorChar).any { it == GIT_DIR }
        ) {
            // Blocks any `.git` segment at any depth, not just the repo root's own -- a
            // submodule's nested .git is exactly as off-limits as the top-level one.
            throw FileNotFoundException("Outside repo: $documentId")
        }
        if (!file.exists()) throw FileNotFoundException("Not found: $documentId")
        return Resolved(repo, file)
    }

    /** All resolvable repos, or empty if the app singleton isn't initialized yet (see class
     * doc) or the database can't be read for any other reason -- the provider must never take
     * the Files app down with it. */
    private fun listRepos(): List<Repo> {
        return try {
            Repo.getRepoList(MGitApplication.getContext(), RepoDbManager.queryAllRepo())
                .filter { !it.isExternal }
        } catch (e: Exception) {
            Timber.w(e, "documents provider queried before app init or with unreadable db")
            emptyList()
        }
    }

    private fun failIfRepoBusy(repo: Repo) {
        if (repo.hasOngoingTask()) {
            throw IllegalStateException("A git operation is running on this repo; try again shortly")
        }
    }

    private fun addRootDocRow(cursor: MatrixCursor) {
        cursor.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, ROOT_DOC_ID)
            add(Document.COLUMN_DISPLAY_NAME, context!!.getString(R.string.app_name))
            add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
            add(Document.COLUMN_FLAGS, 0)
            add(Document.COLUMN_SIZE, null)
            add(Document.COLUMN_LAST_MODIFIED, null)
        }
    }

    private fun addFileRow(
        cursor: MatrixCursor, documentId: String, resolved: Resolved, displayName: String? = null
    ) {
        val file = resolved.file
        val flags = if (file.isDirectory) {
            if (resolved.isRepoRoot) {
                Document.FLAG_DIR_SUPPORTS_CREATE
            } else {
                Document.FLAG_DIR_SUPPORTS_CREATE or Document.FLAG_SUPPORTS_DELETE or
                    Document.FLAG_SUPPORTS_RENAME
            }
        } else {
            Document.FLAG_SUPPORTS_WRITE or Document.FLAG_SUPPORTS_DELETE or
                Document.FLAG_SUPPORTS_RENAME
        }
        cursor.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, documentId)
            add(Document.COLUMN_DISPLAY_NAME, displayName ?: file.name)
            add(Document.COLUMN_MIME_TYPE, mimeType(file))
            add(Document.COLUMN_FLAGS, flags)
            add(Document.COLUMN_SIZE, if (file.isFile) file.length() else null)
            add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
        }
    }

    private fun mimeType(file: File): String {
        if (file.isDirectory) return Document.MIME_TYPE_DIR
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }

    private fun requireSafeName(displayName: String) {
        if (displayName.contains('/') || displayName == "." || displayName == ".." ||
            displayName == GIT_DIR
        ) {
            throw IllegalArgumentException("Invalid name: $displayName")
        }
    }

    companion object {
        private const val ROOT_ID = "gitling"
        private const val ROOT_DOC_ID = "root"
        private const val REPO_PREFIX = "repo:"
        private const val GIT_DIR = ".git"

        private fun repoDocId(repo: Repo) = REPO_PREFIX + repo.getID()

        private fun childDocId(parentDocId: String, name: String) = "$parentDocId/$name"

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_TITLE,
            Root.COLUMN_ICON, Root.COLUMN_FLAGS
        )

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE,
            Document.COLUMN_FLAGS, Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED
        )
    }
}
