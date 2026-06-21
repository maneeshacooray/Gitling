package me.sheimi.sgit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.views.SheimiDialogFragment
import me.sheimi.sgit.R
import com.manichord.mgit.MainActivity
import me.sheimi.sgit.database.models.Repo

class CheckoutDialog : SheimiDialogFragment() {

    companion object {
        const val BASE_COMMIT = "base commit"
    }

    private var commit: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        commit = arguments?.getString(BASE_COMMIT) ?: ""
        val activity = (requireActivity() as MainActivity).currentRepoDetailHost!!
        val message = getString(R.string.dialog_comfirm_checkout_commit_msg) +
            " " + Repo.getCommitDisplayName(commit)

        return ComposeView(requireContext()).apply {
            setContent {
                var newBranchName by remember { mutableStateOf("") }

                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.dialog_comfirm_checkout_commit_title)) },
                        text = {
                            Column {
                                Text(message)
                                OutlinedTextField(
                                    value = newBranchName,
                                    onValueChange = { newBranchName = it },
                                    label = { Text(stringResource(R.string.label_new_branch_name)) },
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                activity.getRepoDelegate().checkoutCommit(commit, newBranchName.trim())
                                dismiss()
                            }) {
                                Text(stringResource(R.string.label_checkout))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                activity.getRepoDelegate().checkoutCommit(commit)
                                dismiss()
                            }) {
                                Text(stringResource(R.string.label_anonymous_checkout))
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BASE_COMMIT, commit)
    }
}
