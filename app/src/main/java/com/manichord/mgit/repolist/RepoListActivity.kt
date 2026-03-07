package com.manichord.mgit.repolist

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.manichord.mgit.clone.CloneViewModel
import com.manichord.mgit.transport.MGitHttpConnectionFactory
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.sgit.MGitApplication
import me.sheimi.sgit.R
import me.sheimi.sgit.activities.RepoDetailActivity
import me.sheimi.sgit.activities.UserSettingsActivity
import me.sheimi.sgit.activities.explorer.ExploreFileActivity
import me.sheimi.sgit.activities.explorer.ImportRepositoryActivity
import me.sheimi.sgit.adapters.RepoListAdapter
import me.sheimi.sgit.database.RepoDbManager
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.repo.tasks.repo.CloneTask
import me.sheimi.sgit.ssh.PrivateKeyUtils
import timber.log.Timber
import java.io.File
import java.net.MalformedURLException
import java.net.URL

class RepoListActivity : SheimiFragmentActivity() {

    private lateinit var mRepoListAdapter: RepoListAdapter
    private lateinit var cloneViewModel: CloneViewModel
    private lateinit var viewModel: RepoListViewModel

    companion object {
        private const val REQUEST_IMPORT_REPO = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[RepoListViewModel::class.java]
        cloneViewModel = ViewModelProvider(this)[CloneViewModel::class.java]

        PrivateKeyUtils.migratePrivateKeys()
        initUpdatedSSL()

        mRepoListAdapter = RepoListAdapter(this)
        mRepoListAdapter.queryAllRepo()

        setContent {
            RepoListComposeContent(
                activity = this,
                cloneViewModel = cloneViewModel,
                adapter = mRepoListAdapter,
                onCloneClick = { cloneRepo() },
                onCancelCloneViewClick = { hideCloneView() }
            )
        }

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        try {
            val remoteRepoUrl = URL(uri.scheme, uri.host, uri.port ?: -1, uri.path)
            val remoteUrl = remoteRepoUrl.toString()
            var repoName = remoteUrl.substring(remoteUrl.lastIndexOf("/") + 1)
            val repoUrlBuilder = StringBuilder(remoteUrl)

            if (!remoteUrl.lowercase().endsWith(getString(R.string.git_extension))) {
                repoUrlBuilder.append(getString(R.string.git_extension))
            } else {
                repoName = repoName.substring(0, repoName.lastIndexOf('.'))
            }

            val repositoriesWithSameRemote = Repo.getRepoList(this, RepoDbManager.searchRepo(remoteUrl))

            if (repositoriesWithSameRemote.isNotEmpty()) {
                showToastMessage(R.string.repository_already_present)
                val detailIntent = Intent(this, RepoDetailActivity::class.java).apply {
                    putExtra(Repo.TAG, repositoriesWithSameRemote[0])
                }
                startActivity(detailIntent)
            } else if (Repo.getDir((applicationContext as MGitApplication).prefenceHelper, repoName).exists()) {
                cloneViewModel.remoteUrl = repoUrlBuilder.toString()
                showCloneView()
            } else {
                val cloningStatus = getString(R.string.cloning)
                val mRepo = Repo.createRepo(repoName, repoUrlBuilder.toString(), cloningStatus)
                CloneTask(mRepo, true, cloningStatus, null).executeTask()
            }
        } catch (e: MalformedURLException) {
            showToastMessage(R.string.invalid_url)
            Timber.e(e)
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestRequiredPermissions(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun initUpdatedSSL() {
        MGitHttpConnectionFactory.install()
        Timber.i("Installed custom HTTPS factory")
    }

    private fun cloneRepo() {
        if (cloneViewModel.validate()) {
            hideCloneView()
            cloneViewModel.cloneRepo()
        }
    }

    private fun showCloneView() {
        cloneViewModel.show(true)
    }

    private fun hideCloneView() {
        cloneViewModel.show(false)
        // Keyboard hiding is handled by TopAppBar/BottomSheet focus in Compose usually,
        // but keeping it for safety if needed via ViewHelper
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return

        when (requestCode) {
            REQUEST_IMPORT_REPO -> {
                val path = data.extras?.getString(ExploreFileActivity.RESULT_PATH) ?: return
                val file = File(path)
                val dotGit = File(file, Repo.DOT_GIT_DIR)
                if (!dotGit.exists()) {
                    showToastMessage(getString(R.string.error_no_repository))
                    return
                }

                showMessageDialog(
                    R.string.dialog_comfirm_import_repo_title,
                    getString(R.string.dialog_comfirm_import_repo_msg),
                    R.string.label_import,
                    { _, _ ->
                        // Import logic
                        val args = Bundle().apply { putString("from_path", path) }
                        // Need to import dialog - would eventually be a Compose screen
                        // For now we still use the Fragment dialog
                    }
                )
            }
        }
    }
}
