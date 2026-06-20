package me.sheimi.sgit.activities

import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.manichord.mgit.branchchooser.BranchChooserScreen
import com.manichord.mgit.branchchooser.BranchChooserViewModel
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.android.utils.BasicFunctions
import me.sheimi.android.utils.Profile
import androidx.core.content.IntentCompat
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.dialogs.RenameBranchDialog
import me.sheimi.sgit.repo.tasks.repo.CheckoutTask
import me.sheimi.sgit.repo.tasks.SheimiAsyncTask
import android.widget.Toast

class BranchChooserActivity : SheimiFragmentActivity() {

    override fun getThemeResource(): Int {
        return if (Profile.getTheme(this) == 1) {
            R.style.DarkAppTheme_NoActionBar
        } else {
            R.style.AppTheme_NoActionBar
        }
    }

    private val viewModel: BranchChooserViewModel by viewModels()
    private var mRepo: Repo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRepo = IntentCompat.getSerializableExtra(intent, Repo.TAG, Repo::class.java)
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
        com.manichord.mgit.dialogs.MessageDialog.show(
            findViewById(android.R.id.content),
            getString(R.string.dialog_branch_delete) + " " + commitName,
            getString(R.string.dialog_branch_delete_msg),
            getString(R.string.label_delete),
            getString(R.string.label_cancel),
            DialogInterface.OnClickListener { _, _ -> deleteBranch(commitName) },
            DialogInterface.OnClickListener { _, _ -> }
        )
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
