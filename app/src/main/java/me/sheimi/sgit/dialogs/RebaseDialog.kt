package me.sheimi.sgit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.views.SheimiDialogFragment
import me.sheimi.sgit.R
import com.manichord.mgit.MainActivity
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.repo.tasks.SheimiAsyncTask.AsyncTaskPostCallback
import me.sheimi.sgit.repo.tasks.repo.RebaseTask

class RebaseDialog : SheimiDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val repo = arguments?.getSerializable(Repo.TAG) as? Repo
        val activity = (requireActivity() as MainActivity).currentRepoDetailHost
        if (repo == null || activity == null) {
            // A DialogFragment can be recreated by the FragmentManager's own state
            // restoration (e.g. after process death) before Compose has recomposed
            // "repoDetail" and re-set currentRepoDetailHost -- bail out rather than crash.
            dismiss()
            return ComposeView(requireContext())
        }
        val currentBranchName = repo.branchName
        val branches = repo.branches.filter { it != currentBranchName }

        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.dialog_rebase_title)) },
                        text = {
                            val transparentColors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            Column {
                                branches.forEach { branch ->
                                    val icon = if (Repo.getCommitType(branch) == Repo.COMMIT_TYPE_TAG) {
                                        Icons.Default.Tag
                                    } else {
                                        Icons.Default.AccountTree
                                    }
                                    ListItem(
                                        headlineContent = { Text(Repo.getCommitDisplayName(branch)) },
                                        leadingContent = {
                                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        colors = transparentColors,
                                        modifier = Modifier.clickable {
                                            val task = RebaseTask(repo, branch, object : AsyncTaskPostCallback {
                                                override fun onPostExecute(isSuccess: Boolean?) {
                                                    activity.reset()
                                                }
                                            })
                                            task.executeTask()
                                            dismiss()
                                        }
                                    )
                                }
                            }
                        },
                        confirmButton = {}
                    )
                }
            }
        }
    }
}
