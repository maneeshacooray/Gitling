package me.sheimi.sgit.activities

import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.manichord.mgit.branchchooser.BranchChooserScreen
import com.manichord.mgit.branchchooser.BranchChooserViewModel
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.dialogs.RenameBranchDialog
import me.sheimi.sgit.repo.tasks.repo.CheckoutTask
import me.sheimi.sgit.repo.tasks.SheimiAsyncTask
import android.app.AlertDialog
import android.widget.Toast

class BranchChooserActivity : SheimiFragmentActivity() {

    private val viewModel: BranchChooserViewModel by viewModels()
    private var mRepo: Repo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRepo = intent.getSerializableExtra(Repo.TAG) as? Repo
        if (mRepo == null) {
            finish()
            return
        }

        viewModel.setRepo(mRepo!!)

        setContent {
            AppTheme {
                BranchChooserScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onBranchClick = { checkoutBranch(it) },
                    onRenameClick = { showRenameDialog(it) },
                    onDeleteClick = { showDeleteDialog(it) }
                )
            }
        }
    }

    fun refreshList() {
        viewModel.refreshList()
    }

    private fun checkoutBranch(commitName: String) {
        val checkoutTask = CheckoutTask(mRepo, commitName, null, object : SheimiAsyncTask.AsyncTaskPostCallback {
            override fun onPostExecute(isSuccess: Boolean?) {
                finish()
            }
        })
        checkoutTask.executeTask()
    }

    private fun showRenameDialog(commitName: String) {
        val pathArg = Bundle()
        pathArg.putString(RenameBranchDialog.FROM_COMMIT, commitName)
        pathArg.putSerializable(Repo.TAG, mRepo)
        val rbd = RenameBranchDialog()
        rbd.arguments = pathArg
        rbd.show(supportFragmentManager, "rename-dialog")
    }

    private fun showDeleteDialog(commitName: String) {
        AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(getString(R.string.dialog_branch_delete) + " " + commitName)
            .setMessage(R.string.dialog_branch_delete_msg)
            .setPositiveButton(R.string.label_delete) { _, _ ->
                deleteBranch(commitName)
            }
            .setNegativeButton(R.string.label_cancel, null)
            .show()
    }

    private fun deleteBranch(commitName: String) {
        val commitType = Repo.getCommitType(commitName)
        try {
            when (commitType) {
                Repo.COMMIT_TYPE_HEAD -> {
                    mRepo?.git?.branchDelete()?.setBranchNames(commitName)?.setForce(true)?.call()
                }
                Repo.COMMIT_TYPE_TAG -> {
                    mRepo?.git?.tagDelete()?.setTags(commitName)?.call()
                }
            }
            viewModel.refreshList()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.cannot_delete_branch, commitName), Toast.LENGTH_LONG).show()
        }
    }
}
