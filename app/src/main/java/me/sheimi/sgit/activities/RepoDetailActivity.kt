package me.sheimi.sgit.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.manichord.mgit.repodetail.RepoDetailScreen
import com.manichord.mgit.repodetail.RepoDetailViewModel
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.sgit.R
import me.sheimi.sgit.activities.delegate.RepoOperationDelegate
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.fragments.BaseFragment
import me.sheimi.sgit.fragments.CommitsFragment
import me.sheimi.sgit.fragments.FilesFragment
import me.sheimi.sgit.fragments.StatusFragment
import me.sheimi.sgit.repo.tasks.SheimiAsyncTask.AsyncTaskCallback

class RepoDetailActivity : SheimiFragmentActivity() {

    private val viewModel: RepoDetailViewModel by viewModels()
    private var mRepo: Repo? = null
    private var mRepoDelegate: RepoOperationDelegate? = null

    private var mFilesFragment: FilesFragment? = null
    private var mCommitsFragment: CommitsFragment? = null
    private var mStatusFragment: StatusFragment? = null

    companion object {
        private const val BRANCH_CHOOSE_ACTIVITY = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRepo = intent.getSerializableExtra(Repo.TAG) as? Repo
        if (mRepo == null) {
            finish()
            return
        }

        repoInit()
        viewModel.setRepo(mRepo!!)

        createFragments()

        setContent {
            AppTheme {
                RepoDetailScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onBranchClick = {
                        val intent = Intent(this, BranchChooserActivity::class.java)
                        intent.putExtra(Repo.TAG, mRepo)
                        startActivityForResult(intent, BRANCH_CHOOSE_ACTIVITY)
                    },
                    onOperationClick = { index ->
                        getRepoDelegate().executeAction(index)
                    },
                    filesContent = { FragmentContainer(mFilesFragment!!) },
                    commitsContent = { FragmentContainer(mCommitsFragment!!) },
                    statusContent = { FragmentContainer(mStatusFragment!!) }
                )
            }
        }
    }

    @Composable
    private fun FragmentContainer(fragment: Fragment) {
        AndroidView(
            factory = { context ->
                FrameLayout(context).apply {
                id = View.generateViewId()
                    // Attach fragment if not already attached
                    supportFragmentManager.beginTransaction()
                        .replace(id, fragment)
                        .commitNow()
                }
            },
            update = { /* No-op */ }
        )
    }

    private fun repoInit() {
        mRepo?.updateLatestCommitInfo()
        mRepo?.getRemotes()
    }

    private fun createFragments() {
        mFilesFragment = FilesFragment.newInstance(mRepo)
        mCommitsFragment = CommitsFragment.newInstance(mRepo, null)
        mStatusFragment = StatusFragment.newInstance(mRepo)
    }

    fun getRepoDelegate(): RepoOperationDelegate {
        if (mRepoDelegate == null) {
            mRepoDelegate = RepoOperationDelegate(mRepo, this)
        }
        return mRepoDelegate!!
    }

    fun reset(commitName: String) {
        viewModel.setRepo(mRepo!!) // Refresh
        reset()
    }

    fun reset() {
        mFilesFragment?.reset()
        mCommitsFragment?.reset()
        mStatusFragment?.reset()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            BRANCH_CHOOSE_ACTIVITY -> {
                val branchName = mRepo?.branchName
                if (branchName == null) {
                    showToastMessage(R.string.error_something_wrong)
                    return
                }
                reset(branchName)
            }
        }
    }

    fun closeOperationDrawer() {
        viewModel.setDrawerOpen(false)
    }

    fun error() {
        finish()
        showToastMessage(R.string.error_unknown)
    }

    fun enterDiffActionMode() {
        viewModel.setSelectedTab(1) // COMMITS_FRAGMENT_INDEX
        mCommitsFragment?.enterDiffActionMode()
    }

    // Progress Callback for delegate actions
    public inner class ProgressCallback(private val mInitMsg: Int) : AsyncTaskCallback {
        override fun onPreExecute() {
            viewModel.updateProgress(getString(mInitMsg), "0%", "0/0", 0)
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

    // Legacy overrides for fragments
    fun setFilesFragment(filesFragment: FilesFragment) { mFilesFragment = filesFragment }
    fun getFilesFragment() = mFilesFragment
    fun setCommitsFragment(commitsFragment: CommitsFragment) { mCommitsFragment = commitsFragment }
    fun setStatusFragment(statusFragment: StatusFragment) { mStatusFragment = statusFragment }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                // Fragment back handling logic could go here if needed
                // But Compose and Screen might handle it better via NavBackHandler
            }
        }
        return super.onKeyUp(keyCode, event)
    }
}
