package me.sheimi.sgit.fragments

import androidx.compose.runtime.Composable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.manichord.mgit.MainActivity
import com.manichord.mgit.repodetail.StatusChangeType
import com.manichord.mgit.repodetail.StatusFileEntry
import com.manichord.mgit.repodetail.StatusScreen
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.activities.SheimiFragmentActivity.OnBackClickListener
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.repo.tasks.repo.AddToStageTask
import me.sheimi.sgit.repo.tasks.repo.RemoveFromStageTask
import me.sheimi.sgit.repo.tasks.repo.StatusTask
import me.sheimi.sgit.repo.tasks.repo.UnstageAllTask
import org.eclipse.jgit.api.Status

class StatusFragment : RepoDetailFragment() {

    companion object {
        @JvmStatic
        fun newInstance(repo: Repo): StatusFragment {
            val fragment = StatusFragment()
            val bundle = Bundle()
            bundle.putSerializable(Repo.TAG, repo)
            fragment.arguments = bundle
            return fragment
        }
    }

    private var repo: Repo? = null
    private var isLoading by mutableStateOf(true)
    private var isClean by mutableStateOf(false)
    private var stagedFiles by mutableStateOf(emptyList<StatusFileEntry>())
    private var unstagedFiles by mutableStateOf(emptyList<StatusFileEntry>())
    private var conflictingFiles by mutableStateOf(emptyList<String>())

    /** Compose state: true once [setUp] has run, so [Content] knows the fragment is ready. */
    private var isSetUp by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUp(savedInstanceState)
    }

    /** The fragment's non-view setup, done once. The repo screen hosts this fragment headless and
     * draws [Content] in its own composition (so scrolling reaches its collapsing top bar); the
     * file viewer still hosts it as a view, through [onCreateView]. */
    private fun setUp(savedInstanceState: Bundle?) {
        if (isSetUp) return
        // Safe call, not repoDetailActivity (a non-null-typed Java getter) -- on an Activity
        // relaunch (e.g. certain config changes Android doesn't hand to onConfigurationChanged),
        // FragmentManager restores previously-shown fragments during onStart(), before Compose's
        // NavHost has re-run and repopulated MainActivity.currentRepoDetailHost. Matches the
        // pattern CommitsFragment already uses for the same reason.
        (rawActivity as? MainActivity)?.currentRepoDetailHost?.setStatusFragment(this)

        repo = (arguments?.getSerializable(Repo.TAG) as? Repo)
            ?: (savedInstanceState?.getSerializable(Repo.TAG) as? Repo)
        val repo = repo ?: return
        this.repo = repo

        reset()
        isSetUp = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setUp(savedInstanceState)
        // Added headless (no container): the repo screen draws Content() itself
        if (container == null) return null
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    // Qualified: inside ComposeView.apply, a bare Content() is the view's own
                    // Content(), which recurses
                    this@StatusFragment.Content()
                }
            }
        }
    }

    @Composable
    fun Content() {
        if (!isSetUp) return
        StatusScreen(
            isLoading = isLoading,
            isClean = isClean,
            stagedFiles = stagedFiles,
            unstagedFiles = unstagedFiles,
            conflictingFiles = conflictingFiles,
            onStageFile = { path -> stageFile(path) },
            onUnstageFile = { path -> unstageFile(path) },
            onStageAll = { stageAll() },
            onUnstageAll = { unstageAll() },
            onViewStagedDiff = { showDiff("HEAD", "dircache") },
            onViewUnstagedDiff = { showDiff("dircache", "filetree") }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(Repo.TAG, repo)
    }

    private fun showDiff(oldCommit: String, newCommit: String) {
        (rawActivity as MainActivity).openCommitDiff(oldCommit, newCommit, false, repo)
    }

    private fun stageFile(path: String) {
        val repo = repo ?: return
        AddToStageTask(repo, path) { reset() }.executeTask()
    }

    private fun stageAll() {
        val repo = repo ?: return
        AddToStageTask(repo, ".") { reset() }.executeTask()
    }

    private fun unstageFile(path: String) {
        val repo = repo ?: return
        RemoveFromStageTask(repo, path) { reset() }.executeTask()
    }

    private fun unstageAll() {
        val repo = repo ?: return
        UnstageAllTask(repo) { reset() }.executeTask()
    }

    override fun reset() {
        val repo = repo ?: return
        isLoading = true
        val task = StatusTask(repo) { status -> applyStatus(status) }
        task.executeTask()
    }

    private fun applyStatus(status: Status?) {
        if (status == null) {
            isLoading = false
            return
        }
        isClean = !status.hasUncommittedChanges() && status.isClean
        stagedFiles = status.added.map { StatusFileEntry(it, StatusChangeType.ADDED) } +
            status.changed.map { StatusFileEntry(it, StatusChangeType.MODIFIED) } +
            status.removed.map { StatusFileEntry(it, StatusChangeType.REMOVED) }
        unstagedFiles = status.modified.map { StatusFileEntry(it, StatusChangeType.MODIFIED) } +
            status.missing.map { StatusFileEntry(it, StatusChangeType.MISSING) } +
            status.untracked.map { StatusFileEntry(it, StatusChangeType.UNTRACKED) }
        conflictingFiles = status.conflicting.toList()
        isLoading = false
    }

    override fun getOnBackClickListener(): OnBackClickListener? = null
}
