package me.sheimi.sgit.activities

import android.content.Intent
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.manichord.mgit.MainActivity
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit
import timber.log.Timber
import me.sheimi.android.utils.CodeGuesser
import me.sheimi.android.utils.FsUtils
import me.sheimi.android.utils.Profile
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.repo.tasks.repo.CommitDiffTask
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Per-diff-screen state and JS-bridge, formerly a real Activity -- now a plain class constructed
 * by MainActivity's "commitDiff" NavHost route, as part of the single-activity rewrite. Unlike
 * RepoDetailActivity this doesn't need to be a Context (nothing casts to it or uses it to launch
 * Intents) so it's a plain class, not a ContextWrapper -- its few Activity-only needs (starting
 * the share/save-diff intents, toasts, strings) are reached through the wrapped MainActivity.
 */
class CommitDiffActivity(
    private val mainActivity: MainActivity,
    private val oldCommit: String?,
    private val newCommit: String,
    private val showDescription: Boolean,
    private val repo: Repo?
) {
    companion object {
        const val OLD_COMMIT = "old commit"
        const val NEW_COMMIT = "new commit"
        const val SHOW_DESCRIPTION = "show_description"
        private const val JS_INTERFACE = "CodeLoader"

        private const val HTML_TMPL = "<!doctype html>" +
            "<head>" +
            " <script src=\"js/jquery.js\"></script>" +
            " <script src=\"js/highlight.pack.js\"></script>" +
            " <script src=\"js/local_commits_diff.js\"></script>" +
            " <link type=\"text/css\" rel=\"stylesheet\" href=\"css/rainbow.css\" />" +
            " <link type=\"text/css\" rel=\"stylesheet\" href=\"css/local_commits_diff.css\" />" +
            "</head><body></body>"
    }

    private var commit: RevCommit? = null
    private var diffStrs: List<String> = emptyList()
    private var diffEntries: List<DiffEntry> = emptyList()

    var isLoading by mutableStateOf(true)
    private var webView: WebView? = null

    val screenTitle: String = run {
        var title = Repo.getCommitDisplayName(newCommit)
        oldCommit?.let { title += " : " + Repo.getCommitDisplayName(it) }
        mainActivity.getString(R.string.title_activity_commit_diff) + title
    }

    fun setupWebView(view: WebView) {
        if (webView != null) return // AndroidView's factory should only run once; guard anyway
        webView = view
        view.addJavascriptInterface(CodeLoader(), JS_INTERFACE)
        view.settings.javaScriptEnabled = true
        view.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Timber.d(
                    "%s -- From line %d of %s",
                    consoleMessage.message(), consoleMessage.lineNumber(), consoleMessage.sourceId()
                )
                return true
            }
        }
        view.setBackgroundColor(Color.TRANSPARENT)
        view.loadDataWithBaseURL("file:///android_asset/", HTML_TMPL, "text/html", "utf-8", null)
    }

    private fun formatCommitInfo(): String {
        val c = commit ?: return ""
        val committer = c.committerIdent
        val author = c.authorIdent
        return "commit $newCommit\n" +
            "Author:     ${author.name} <${author.emailAddress}>\n" +
            "AuthorDate: ${author.whenAsInstant}\n" +
            "Commit:     ${committer.name} <${committer.emailAddress}>\n" +
            "CommitDate: ${committer.whenAsInstant}\n"
    }

    fun saveDiff(fos: OutputStream) {
        commit?.let { c ->
            fos.write(formatCommitInfo().toByteArray())
            fos.write("\n".toByteArray())
            for (line in c.fullMessage.split("\n")) {
                fos.write("    $line\n".toByteArray())
            }
            fos.write("\n".toByteArray())
        }
        for (str in diffStrs) {
            fos.write(str.toByteArray())
        }
    }

    private fun sharedDiffPathName(): File {
        var fname = newCommit
        oldCommit?.let { fname += "_$it" }
        return File(FsUtils.getExternalDir("diff", true), "$fname.diff")
    }

    fun shareDiff() {
        try {
            val diff = sharedDiffPathName()
            FileOutputStream(diff).use { saveDiff(it) }
            // A raw file:// Uri triggers FileUriExposedException on API 24+ -- share via
            // the app's existing FileProvider instead (already used by FsUtils.openFile()).
            val diffUri = FileProvider.getUriForFile(mainActivity, FsUtils.PROVIDER_AUTHORITY, diff)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/x-patch"
                putExtra(Intent.EXTRA_STREAM, diffUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            mainActivity.startActivity(Intent.createChooser(shareIntent, null))
        } catch (e: IOException) {
            mainActivity.showToastMessage(R.string.alert_file_creation_failure)
        }
    }

    fun launchSaveDiff() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .setType("text/x-patch")
            .putExtra(Intent.EXTRA_TITLE, "${Repo.getCommitDisplayName(newCommit)}.diff")
        mainActivity.launchSaveDiff(intent)
    }

    private inner class CodeLoader {
        @JavascriptInterface
        fun getDiff(index: Int): String = diffStrs[index]

        @JavascriptInterface
        fun haveCommitInfo(): Boolean = commit != null

        @JavascriptInterface
        fun getCommitInfo(): String = formatCommitInfo()

        @JavascriptInterface
        fun getCommitMessage(): String = commit?.fullMessage ?: ""

        @JavascriptInterface
        fun getChangeType(index: Int): String = diffEntries[index].changeType.toString()

        @JavascriptInterface
        fun getOldPath(index: Int): String = diffEntries[index].oldPath

        @JavascriptInterface
        fun getNewPath(index: Int): String = diffEntries[index].newPath

        @JavascriptInterface
        fun getDiffEntries() {
            val effectiveOldCommit = oldCommit ?: "$newCommit^"
            CommitDiffTask(repo, effectiveOldCommit, newCommit, { entries, strs, c ->
                diffEntries = entries
                diffStrs = strs
                commit = c
                isLoading = false
                webView?.loadUrl(CodeGuesser.wrapUrlScript("notifyEntriesReady();"))
            }, showDescription).executeTask()
        }

        @JavascriptInterface
        fun getDiffSize(): Int = diffEntries.size

        @JavascriptInterface
        fun getTheme(): String = Profile.getCodeMirrorTheme(mainActivity.applicationContext)
    }
}
