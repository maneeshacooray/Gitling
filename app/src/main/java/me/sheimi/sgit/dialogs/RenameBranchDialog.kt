package me.sheimi.sgit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import com.manichord.mgit.MainActivity
import com.manichord.mgit.ui.theme.AppTheme
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
                var branchName by remember { mutableStateOf(Repo.getCommitDisplayName(fromCommit)) }
                var errorRes by remember { mutableStateOf<Int?>(null) }
                val context = LocalContext.current

                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.dialog_rename_branch_title)) },
                        text = {
                            OutlinedTextField(
                                value = branchName,
                                onValueChange = {
                                    branchName = it
                                    errorRes = null
                                },
                                label = { Text(stringResource(R.string.dialog_create_branch_hint)) },
                                singleLine = true,
                                isError = errorRes != null,
                                supportingText = errorRes?.let { res -> { Text(stringResource(res)) } }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val newName = branchName.trim()
                                if (newName.isEmpty()) {
                                    errorRes = R.string.alert_new_branchname_required
                                    return@TextButton
                                }

                                var fail = false
                                try {
                                    when (Repo.getCommitType(fromCommit)) {
                                        Repo.COMMIT_TYPE_HEAD -> {
                                            repo.git.branchRename()
                                                .setOldName(fromCommit)
                                                .setNewName(newName)
                                                .call()
                                        }
                                        Repo.COMMIT_TYPE_TAG -> {
                                            val refs = repo.git.tagList().call()
                                            val tagRef = refs.firstOrNull { it.name == fromCommit }
                                            val tag = tagRef?.let {
                                                RevWalk(repo.git.repository).lookupTag(it.objectId)
                                            }
                                            if (tag == null) {
                                                fail = true
                                            } else {
                                                repo.git.tag()
                                                    .setMessage(tag.fullMessage)
                                                    .setName(newName)
                                                    .setObjectId(tag.`object`)
                                                    .setTagger(tag.taggerIdent)
                                                    .call()
                                                repo.git.tagDelete()
                                                    .setTags(fromCommit)
                                                    .call()
                                            }
                                        }
                                    }
                                } catch (e: StopTaskException) {
                                    fail = true
                                } catch (e: GitAPIException) {
                                    fail = true
                                }

                                if (fail) {
                                    Toast.makeText(context, "can't rename $fromCommit", Toast.LENGTH_LONG).show()
                                }

                                activity.currentBranchChooserViewModel?.refreshList()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(FROM_COMMIT, fromCommit)
    }
}
