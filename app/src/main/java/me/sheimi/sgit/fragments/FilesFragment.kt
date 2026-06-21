package me.sheimi.sgit.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.manichord.mgit.MainActivity
import com.manichord.mgit.explorer.FileListContent
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.android.activities.SheimiFragmentActivity.OnBackClickListener
import me.sheimi.android.utils.FsUtils
import me.sheimi.sgit.activities.ViewFileActivity
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.dialogs.RepoFileOperationDialog
import timber.log.Timber
import java.io.File
import java.io.IOException

class FilesFragment : RepoDetailFragment() {

    companion object {
        private const val CURRENT_DIR = "current_dir"

        @JvmStatic
        fun newInstance(repo: Repo): FilesFragment {
            val fragment = FilesFragment()
            val bundle = Bundle()
            bundle.putSerializable(Repo.TAG, repo)
            fragment.arguments = bundle
            return fragment
        }
    }

    private var repo: Repo? = null
    private var rootDir: File? = null
    private var currentDir: File? = null
    // Must be Compose state, not a plain var: the header text below reads this directly, and if
    // the filtered `files` list happens to come out structurally equal between two keystrokes
    // (e.g. "h" and "he" both matching only "helper.js"), mutableStateOf's equality check skips
    // recomposition entirely -- silently leaving the header showing a stale query unless this is
    // independently tracked.
    private var searchQuery by mutableStateOf<String?>(null)
    private var files by mutableStateOf<List<File>>(emptyList())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Safe call, not repoDetailActivity (a non-null-typed Java getter) -- on an Activity
        // relaunch (e.g. certain config changes Android doesn't hand to onConfigurationChanged),
        // FragmentManager restores previously-shown fragments during onStart(), before Compose's
        // NavHost has re-run and repopulated MainActivity.currentRepoDetailHost. Matches the
        // pattern CommitsFragment already uses for the same reason.
        (rawActivity as? MainActivity)?.currentRepoDetailHost?.setFilesFragment(this)

        repo = (arguments?.getSerializable(Repo.TAG) as? Repo)
            ?: (savedInstanceState?.getSerializable(Repo.TAG) as? Repo)
        val repo = repo ?: return ComposeView(requireContext())
        rootDir = repo.dir

        savedInstanceState?.getString(CURRENT_DIR)?.let { path ->
            setCurrentDir(File(path))
        }
        reset()

        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    val dir = currentDir
                    val query = searchQuery
                    FileListContent(
                        currentPath = if (query != null) {
                            "Search results for \"$query\""
                        } else {
                            dir?.let { relativePath(it) } ?: ""
                        },
                        files = files,
                        showUpRow = query == null && (rootDir?.let { dir != null && dir != it } ?: false),
                        onUpClick = { dir?.parentFile?.let { setCurrentDir(it) } },
                        onItemClick = ::onFileClicked,
                        onItemLongClick = ::onFileLongClicked,
                        displayPath = if (query != null) ::relativePath else null
                    )
                }
            }
        }
    }

    private fun relativePath(file: File): String {
        val root = rootDir ?: return file.absolutePath
        return FsUtils.getRelativePath(file, root).ifEmpty { "/" }
    }

    /** Switches the Files tab into search mode: a recursive, flat match by filename across the
     * whole repo (not just the current directory), since that's what's actually useful when
     * looking for a specific file in a repo with many subdirectories. Pass null/blank to return
     * to normal directory browsing at the last-viewed directory. */
    fun setFileSearchQuery(query: String?) {
        searchQuery = query?.takeIf { it.isNotBlank() }
        refreshFiles()
    }

    private fun refreshFiles() {
        val query = searchQuery
        if (query != null) {
            files = rootDir?.let { searchFilesRecursively(it, query) } ?: emptyList()
        } else {
            currentDir?.let { setCurrentDir(it) }
        }
    }

    private fun searchFilesRecursively(root: File, query: String): List<File> {
        val results = mutableListOf<File>()
        fun walk(dir: File) {
            val children = dir.listFiles { f -> f.name != ".git" } ?: return
            for (child in children) {
                if (child.isDirectory) {
                    walk(child)
                } else if (child.name.contains(query, ignoreCase = true)) {
                    results.add(child)
                }
            }
        }
        walk(root)
        return results.sortedBy { it.name }
    }

    private fun onFileClicked(file: File) {
        if (file.isDirectory) {
            setCurrentDir(file)
            return
        }
        val mime = FsUtils.getMimeType(file)
        if (FsUtils.isTextMimeType(mime)) {
            val intent = Intent(activity, ViewFileActivity::class.java)
            intent.putExtra(ViewFileActivity.TAG_FILE_NAME, file.absolutePath)
            intent.putExtra(Repo.TAG, repo)
            // rawActivity directly, not repoDetailActivity -- this is just a plain Activity
            // context/dialog call, no need to route it through the (sometimes
            // not-yet-populated, see onCreateView above) RepoDetailActivity wrapper at all.
            rawActivity.startActivity(intent)
            return
        }
        try {
            FsUtils.openFile(activity as SheimiFragmentActivity, file)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            rawActivity.showMessageDialog(
                me.sheimi.sgit.R.string.dialog_error_title,
                getString(me.sheimi.sgit.R.string.error_can_not_open_file)
            )
        }
    }

    private fun onFileLongClicked(file: File) {
        val dialog = RepoFileOperationDialog()
        val args = Bundle()
        args.putString(RepoFileOperationDialog.FILE_PATH, file.absolutePath)
        dialog.arguments = args
        dialog.show(parentFragmentManager, "repo-file-op-dialog")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(Repo.TAG, repo)
        currentDir?.let { outState.putString(CURRENT_DIR, it.absolutePath) }
    }

    /** Set the directory listing currently being displayed */
    fun setCurrentDir(dir: File) {
        currentDir = dir
        val filtered = dir.listFiles { file -> file.name != ".git" } ?: emptyArray()
        files = filtered.sortedWith(
            compareBy({ !it.isDirectory }, { it.toString() })
        )
    }

    /** If the root dir has previously been set, reset the displayed listing to it */
    fun resetCurrentDir() {
        rootDir?.let { setCurrentDir(it) }
    }

    override fun reset() {
        resetCurrentDir()
    }

    fun newDir(name: String) {
        val dir = currentDir ?: return
        val file = File(dir, name)
        if (file.exists()) {
            showToastMessage(me.sheimi.sgit.R.string.alert_file_exists)
            return
        }
        file.mkdir()
        setCurrentDir(dir)
    }

    /** Create a new file within the currently displayed directory */
    @Throws(IOException::class)
    fun newFile(name: String) {
        val dir = currentDir ?: return
        val file = File(dir, name)
        if (file.exists()) {
            showToastMessage(me.sheimi.sgit.R.string.alert_file_exists)
            return
        }
        file.createNewFile()
        setCurrentDir(dir)
    }

    override fun getOnBackClickListener(): OnBackClickListener {
        return OnBackClickListener {
            val root = rootDir
            val dir = currentDir
            if (root == null || dir == null || root == dir) {
                false
            } else {
                dir.parentFile?.let { setCurrentDir(it) }
                true
            }
        }
    }
}
