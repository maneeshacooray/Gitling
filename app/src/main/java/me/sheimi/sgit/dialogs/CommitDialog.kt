package me.sheimi.sgit.dialogs

import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.utils.Profile
import me.sheimi.sgit.R
import me.sheimi.sgit.activities.RepoDetailActivity
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.exception.StopTaskException
import me.sheimi.sgit.repo.tasks.SheimiAsyncTask.AsyncTaskPostCallback
import me.sheimi.sgit.repo.tasks.repo.CommitChangesTask
import java.util.Locale

private data class Author(val name: String, val email: String) {
    private val keywords = (name + " " + email).lowercase(Locale.ROOT).split(Regex(" |\\.|-|_|@")).filter { it.isNotEmpty() }

    fun displayString() = "$name <$email>"

    fun matches(constraint: String): Boolean {
        val lower = constraint.lowercase(Locale.ROOT)
        if (email.lowercase(Locale.ROOT).startsWith(lower)) return true
        if (name.lowercase(Locale.ROOT).startsWith(lower)) return true
        return lower.split(Regex(" |\\.|-|_|@")).filter { it.isNotEmpty() }.all { part ->
            keywords.any { it.startsWith(part) }
        }
    }
}

/** Compose replacement for [me.sheimi.sgit.activities.delegate.actions.CommitAction]'s commit dialog. */
object CommitDialog {
    @JvmStatic
    fun show(container: ViewGroup, repo: Repo, activity: RepoDetailActivity) {
        val composeView = ComposeView(container.context)
        container.addView(composeView)
        fun close() = container.removeView(composeView)

        val authors = try {
            val seen = LinkedHashSet<Author>()
            repo.git.log().setMaxCount(500).call().forEach { commit ->
                val ident = commit.authorIdent
                seen.add(Author(ident.name, ident.emailAddress))
            }
            seen
        } catch (e: StopTaskException) {
            linkedSetOf()
        } catch (e: Exception) {
            linkedSetOf()
        }.toMutableSet()

        val profileUsername = Profile.getUsername(activity.applicationContext)
        val profileEmail = Profile.getEmail(activity.applicationContext)
        if (!profileUsername.isNullOrEmpty() && !profileEmail.isNullOrEmpty()) {
            authors.add(Author(profileUsername, profileEmail))
        }
        val authorList = authors.toList().sortedWith(compareBy({ it.name }, { it.email }))

        composeView.setContent {
            var commitMsg by remember { mutableStateOf("") }
            var authorText by remember { mutableStateOf("") }
            var isAmend by remember { mutableStateOf(false) }
            var autoStage by remember { mutableStateOf(true) }
            var authorMenuExpanded by remember { mutableStateOf(false) }
            var commitMsgError by remember { mutableStateOf<String?>(null) }
            var authorError by remember { mutableStateOf<String?>(null) }

            val filteredAuthors = if (authorText.isEmpty()) authorList else authorList.filter { it.matches(authorText) }

            AppTheme {
                AlertDialog(
                    onDismissRequest = { close() },
                    title = { Text(stringResource(R.string.dialog_commit_title)) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = commitMsg,
                                onValueChange = {
                                    commitMsg = it
                                    commitMsgError = null
                                },
                                label = { Text(stringResource(R.string.dialog_commit_msg_hint)) },
                                isError = commitMsgError != null,
                                supportingText = commitMsgError?.let { msg -> { Text(msg) } }
                            )
                            OutlinedTextField(
                                value = authorText,
                                onValueChange = {
                                    authorText = it
                                    authorError = null
                                    authorMenuExpanded = true
                                },
                                label = { Text(stringResource(R.string.dialog_commit_author_hint)) },
                                singleLine = true,
                                isError = authorError != null,
                                supportingText = authorError?.let { msg -> { Text(msg) } }
                            )
                            DropdownMenu(
                                expanded = authorMenuExpanded && filteredAuthors.isNotEmpty(),
                                onDismissRequest = { authorMenuExpanded = false }
                            ) {
                                filteredAuthors.forEach { author ->
                                    DropdownMenuItem(
                                        text = { Text(author.displayString()) },
                                        onClick = {
                                            authorText = author.displayString()
                                            authorMenuExpanded = false
                                        }
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = autoStage, onCheckedChange = { autoStage = it })
                                Text(stringResource(R.string.dialog_commit_auto_stage))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isAmend,
                                    onCheckedChange = {
                                        isAmend = it
                                        commitMsg = if (it) repo.lastCommitFullMsg ?: "" else ""
                                    }
                                )
                                Text(stringResource(R.string.dialog_commit_is_amend))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val msg = commitMsg.trim()
                            val author = authorText.trim()
                            var authorName: String? = null
                            var authorEmail: String? = null

                            if (msg.isEmpty()) {
                                commitMsgError = activity.getString(R.string.error_no_commit_msg)
                                return@TextButton
                            }
                            if (author.isNotEmpty()) {
                                val ltIdx = author.indexOf('<')
                                if (!author.endsWith(">") || ltIdx == -1) {
                                    authorError = activity.getString(R.string.error_invalid_author)
                                    return@TextButton
                                }
                                authorName = author.substring(0, ltIdx).trim()
                                authorEmail = author.substring(ltIdx + 1, author.length - 1)
                            }

                            val task = CommitChangesTask(
                                repo, msg, isAmend, autoStage, authorName, authorEmail,
                                object : AsyncTaskPostCallback {
                                    override fun onPostExecute(isSuccess: Boolean?) {
                                        activity.reset()
                                    }
                                }
                            )
                            task.executeTask()
                            close()
                        }) {
                            Text(stringResource(R.string.dialog_commit_positive_label))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { close() }) {
                            Text(stringResource(R.string.label_cancel))
                        }
                    }
                )
            }
        }
    }
}
