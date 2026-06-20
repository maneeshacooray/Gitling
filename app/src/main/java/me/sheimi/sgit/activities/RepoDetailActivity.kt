package me.sheimi.sgit.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.manichord.mgit.repodetail.RepoDetailScreen
import com.manichord.mgit.repodetail.RepoDetailViewModel
import com.manichord.mgit.ui.components.FragmentHost
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.android.utils.BasicFunctions
import me.sheimi.android.utils.Profile
import me.sheimi.sgit.R
import me.sheimi.sgit.activities.delegate.RepoOperationDelegate
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.fragments.BaseFragment
import me.sheimi.sgit.fragments.CommitsFragment
import me.sheimi.sgit.fragments.FilesFragment
import me.sheimi.sgit.fragments.StatusFragment
import me.sheimi.sgit.repo.tasks.SheimiAsyncTask.AsyncTaskCallback

class RepoDetailActivity : SheimiFragmentActivity() {

    override fun getThemeResource(): Int {
        return if (Profile.getTheme(this) == 1) {
            R.style.DarkAppTheme_NoActionBar
        } else {
            R.style.AppTheme_NoActionBar
        }
    }

    private val viewModel: RepoDetailViewModel by viewModels()
    private var mRepo: Repo? = null
    private var mRepoDelegate: RepoOperationDelegate? = null

    private var mFilesFragment: FilesFragment? = null
    private var mCommitsFragment: CommitsFragment? = null
    private var mStatusFragment: StatusFragment? = null

    private val branchChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val branchName = mRepo?.branchName
        if (branchName == null) {
            showToastMessage(R.string.error_something_wrong)
            return@registerForActivityResult
        }
        reset(branchName)
    }

    companion object {
        private const val BRANCH_CHOOSE_ACTIVITY = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRepo = IntentCompat.getSerializableExtra(intent, Repo.TAG, Repo::class.java)
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
                        branchChooserLauncher.launch(intent)
                    },
                    onOperationClick = { index ->
                        getRepoDelegate().executeAction(index)
                    },
                    filesContent = { FragmentHost(supportFragmentManager, mFilesFragment!!) },
                    commitsContent = { FragmentHost(supportFragmentManager, mCommitsFragment!!) },
                    statusContent = { FragmentHost(supportFragmentManager, mStatusFragment!!) }
                )
            }
        }
    }

    private fun repoInit() {
        mRepo?.updateLatestCommitInfo()
        mRepo?.getRemotes()
    }

    private fun createFragments() {
        mFilesFragment = FilesFragment.newInstance(mRepo!!)
        mCommitsFragment = CommitsFragment.newInstance(mRepo!!, null)
        mStatusFragment = StatusFragment.newInstance(mRepo!!)
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
