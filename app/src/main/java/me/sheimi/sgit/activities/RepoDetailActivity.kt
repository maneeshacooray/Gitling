package me.sheimi.sgit.activities

import android.content.ContextWrapper
import android.content.DialogInterface
import android.view.View
import androidx.fragment.app.FragmentManager
import com.manichord.mgit.repodetail.RepoDetailViewModel
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.android.activities.SheimiFragmentActivity.OnEditTextDialogClicked
import me.sheimi.sgit.R
import me.sheimi.sgit.activities.delegate.RepoOperationDelegate
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.fragments.CommitsFragment
import me.sheimi.sgit.fragments.FilesFragment
import me.sheimi.sgit.fragments.StatusFragment
import me.sheimi.sgit.repo.tasks.SheimiAsyncTask.AsyncTaskCallback

/**
 * Per-repo-screen state and operation dispatch, formerly a real Activity -- now a plain class
 * wrapping the single hosting MainActivity, as part of the single-activity rewrite. Keeps its
 * original name/package/public API so the ~35 dialogs/Action classes that reference it by type
 * (RepoOperationDelegate and every *Action.java, plus PushDialog/MergeDialog/etc.) don't need to
 * change at all -- they already only ever called methods on it, never relied on it actually
 * being an Activity subclass. Extending ContextWrapper (rather than plain Any) means it's still
 * usable anywhere a Context is needed (e.g. `Intent(rawActivity, OtherActivity::class.java)`)
 * without every call site needing to reach through to the wrapped Activity explicitly.
 */
class RepoDetailActivity(
    private val mainActivity: SheimiFragmentActivity,
    val repo: Repo,
    val viewModel: RepoDetailViewModel
) : ContextWrapper(mainActivity) {

    private var mRepoDelegate: RepoOperationDelegate? = null
    private var mFilesFragment: FilesFragment? = null
    private var mCommitsFragment: CommitsFragment? = null
    private var mStatusFragment: StatusFragment? = null

    init {
        repo.updateLatestCommitInfo()
        repo.remotes
        viewModel.setRepo(repo)
    }

    fun getRepoDelegate(): RepoOperationDelegate {
        if (mRepoDelegate == null) {
            mRepoDelegate = RepoOperationDelegate(repo, this)
        }
        return mRepoDelegate!!
    }

    fun reset(commitName: String) {
        viewModel.setRepo(repo) // Refresh
        reset()
    }

    fun reset() {
        mFilesFragment?.reset()
        mCommitsFragment?.reset()
        mStatusFragment?.reset()
    }

    fun closeOperationDrawer() {
        viewModel.setDrawerOpen(false)
    }

    fun error() {
        mainActivity.showToastMessage(R.string.error_unknown)
    }

    fun enterDiffActionMode() {
        viewModel.setSelectedTab(1) // COMMITS_FRAGMENT_INDEX
        mCommitsFragment?.enterDiffActionMode()
    }

    fun setFilesFragment(filesFragment: FilesFragment) { mFilesFragment = filesFragment }
    fun getFilesFragment() = mFilesFragment
    fun setCommitsFragment(commitsFragment: CommitsFragment) { mCommitsFragment = commitsFragment }
    fun setStatusFragment(statusFragment: StatusFragment) { mStatusFragment = statusFragment }

    fun searchFiles(query: String?) = mFilesFragment?.setFileSearchQuery(query)
    fun searchCommits(query: String?) = mCommitsFragment?.setFilter(query)

    // Delegation surface for the handful of Activity-only (non-Context) members the 35
    // dependent files call on this -- everything else (getApplicationContext, getString,
    // startActivity, etc.) comes for free from ContextWrapper.
    fun getSupportFragmentManager(): FragmentManager = mainActivity.supportFragmentManager
    fun finish() = mainActivity.finish()
    fun <T : View> findViewById(id: Int): T = mainActivity.findViewById(id)

    fun showMessageDialog(title: Int, msg: Int, positiveBtn: Int, positiveListener: DialogInterface.OnClickListener) =
        mainActivity.showMessageDialog(title, msg, positiveBtn, positiveListener)

    fun showMessageDialog(title: Int, msg: String, positiveBtn: Int, positiveListener: DialogInterface.OnClickListener) =
        mainActivity.showMessageDialog(title, msg, positiveBtn, positiveListener)

    fun showMessageDialog(
        title: Int,
        msg: String,
        positiveBtn: Int,
        negativeBtn: Int,
        positiveListener: DialogInterface.OnClickListener,
        negativeListener: DialogInterface.OnClickListener
    ) = mainActivity.showMessageDialog(title, msg, positiveBtn, negativeBtn, positiveListener, negativeListener)

    fun showMessageDialog(title: Int, msg: String) = mainActivity.showMessageDialog(title, msg)

    fun showToastMessage(resId: Int) = mainActivity.showToastMessage(resId)
    fun showToastMessage(msg: String) = mainActivity.showToastMessage(msg)

    fun showEditTextDialog(title: Int, hint: Int, positiveBtn: Int, positiveListener: OnEditTextDialogClicked) =
        mainActivity.showEditTextDialog(title, hint, positiveBtn, positiveListener)

    fun showEditTextDialog(title: Int, hint: Int, positiveBtn: Int, positiveListener: OnEditTextDialogClicked, helperText: Int) =
        mainActivity.showEditTextDialog(title, hint, positiveBtn, positiveListener, helperText)

    // Progress Callback for delegate actions
    inner class ProgressCallback(private val mInitMsg: Int) : AsyncTaskCallback {
        override fun onPreExecute() {
            viewModel.updateProgress(mainActivity.getString(mInitMsg), "0%", "0/0", 0)
        }

        override fun onProgressUpdate(vararg progress: String) {
            viewModel.updateProgress(progress[0], progress[1], progress[2], progress[3].toInt())
        }

        override fun onPostExecute(isSuccess: Boolean?) {
            viewModel.hideProgress()
            reset()
        }

        override fun doInBackground(vararg params: Void?): Boolean {
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                return false
            }
            return true
        }
    }
}
