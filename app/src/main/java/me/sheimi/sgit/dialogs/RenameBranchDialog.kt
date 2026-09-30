package me.sheimi.sgit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.manichord.mgit.MainActivity
import com.manichord.mgit.ui.components.onUserTextChange
import com.manichord.mgit.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.exception.StopTaskException
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.revwalk.RevTag
import org.eclipse.jgit.revwalk.RevWalk

class RenameBranchDialog : DialogFragment() {

    companion object {
        const val FROM_COMMIT = "from path"
    }

    private lateinit var fromCommit: String
    private lateinit var repo: Repo

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = arguments
        fromCommit = args?.getString(FROM_COMMIT) ?: ""
        repo = args?.getSerializable(Repo.TAG) as Repo
        val activity = requireActivity() as MainActivity

        return ComposeView(requireContext()).apply {
            setContent {
                val branchName = rememberTextFieldState(Repo.getCommitDisplayName(fromCommit))
                var errorRes by remember { mutableStateOf<Int?>(null) }

                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.dialog_rename_branch_title)) },
                        text = {
                            OutlinedTextField(
                                state = branchName,
                                inputTransformation = onUserTextChange { errorRes = null },
                                label = { Text(stringResource(R.string.dialog_create_branch_hint)) },
                                lineLimits = TextFieldLineLimits.SingleLine,
                                isError = errorRes != null,
                                supportingText = errorRes?.let { res -> { Text(stringResource(res)) } }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val newName = branchName.text.toString().trim()
                                if (newName.isEmpty()) {
                                    errorRes = R.string.alert_new_branchname_required
                                    return@TextButton
                                }

                                // Off the main thread: JGit writes a reflog entry, and with no
                                // git email configured it builds a default one from the device
                                // hostname -- a network lookup Android forbids on the main
                                // thread. That crashed the app mid-rename, leaving HEAD pointing
                                // at a branch ref that no longer existed. Uses the activity's
                                // scope since this dialog is dismissed straight away.
                                val from = fromCommit
                                activity.lifecycleScope.launch {
                                    val renamed = withContext(Dispatchers.IO) { renameRef(from, newName) }
                                    if (!renamed) {
                                        Toast.makeText(activity, "can't rename $from", Toast.LENGTH_LONG).show()
                                    }
                                    activity.currentBranchChooserViewModel?.refreshList()
                                }
                                dismiss()
                            }) {
                                Text(stringResource(R.string.label_rename))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { dismiss() }) {
                                Text(stringResource(R.string.label_cancel))
                            }
                        }
                    )
                }
            }
        }
    }

    /** Renames a branch or tag; returns false on failure. Does disk (and possibly network) I/O. */
    private fun renameRef(fromCommit: String, newName: String): Boolean {
        return try {
            when (Repo.getCommitType(fromCommit)) {
                Repo.COMMIT_TYPE_HEAD -> {
                    repo.git.branchRename()
                        .setOldName(fromCommit)
                        .setNewName(newName)
                        .call()
                    true
                }
                Repo.COMMIT_TYPE_TAG -> {
                    val refs = repo.git.tagList().call()
                    val tagRef = refs.firstOrNull { it.name == fromCommit }
                    val tag = tagRef?.let {
                        RevWalk(repo.git.repository).lookupTag(it.objectId)
                    } ?: return false
                    repo.git.tag()
                        .setMessage(tag.fullMessage)
                        .setName(newName)
                        .setObjectId(tag.`object`)
                        .setTagger(tag.taggerIdent)
                        .call()
                    repo.git.tagDelete()
                        .setTags(fromCommit)
                        .call()
                    true
                }
                else -> true
            }
        } catch (e: StopTaskException) {
            false
        } catch (e: GitAPIException) {
            false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(FROM_COMMIT, fromCommit)
    }
}
