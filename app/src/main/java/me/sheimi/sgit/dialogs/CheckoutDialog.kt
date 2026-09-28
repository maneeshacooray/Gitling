package me.sheimi.sgit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        val activity = (requireActivity() as MainActivity).currentRepoDetailHost
        if (activity == null) {
            // A DialogFragment can be recreated by the FragmentManager's own state
            // restoration (e.g. after process death) before Compose has recomposed
            // "repoDetail" and re-set currentRepoDetailHost -- bail out rather than crash.
            dismiss()
            return ComposeView(requireContext())
        }
        val message = getString(R.string.dialog_comfirm_checkout_commit_msg) +
            " " + Repo.getCommitDisplayName(commit)

        return ComposeView(requireContext()).apply {
            setContent {
                val newBranchName = rememberTextFieldState()

                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.dialog_comfirm_checkout_commit_title)) },
                        text = {
                            Column {
                                Text(message)
                                OutlinedTextField(
                                    state = newBranchName,
                                    label = { Text(stringResource(R.string.label_new_branch_name)) },
                                    lineLimits = TextFieldLineLimits.SingleLine
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                activity.getRepoDelegate().checkoutCommit(commit, newBranchName.text.toString().trim())
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
