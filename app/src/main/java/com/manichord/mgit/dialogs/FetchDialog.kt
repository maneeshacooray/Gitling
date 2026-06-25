package com.manichord.mgit.dialogs

import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.sgit.R
import me.sheimi.sgit.activities.RepoDetailActivity
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.repo.tasks.repo.FetchTask

object FetchDialog {
    @JvmStatic
    fun show(container: ViewGroup, repo: Repo, activity: RepoDetailActivity) {
        val remotes = repo.remotes.toTypedArray()
        val composeView = ComposeView(container.context)
        container.addView(composeView)
        fun close() = container.removeView(composeView)

        fun fetch(selected: Array<String>) {
            val task = FetchTask(selected, repo, activity.ProgressCallback(R.string.fetch_msg_init))
            task.executeTask()
        }

        composeView.setContent {
            val checked = remember { mutableStateOf(remotes.associateWith { false }) }

            AppTheme {
                AlertDialog(
                    onDismissRequest = { close() },
                    title = { Text(stringResource(R.string.dialog_fetch_title)) },
                    text = {
                        Column {
                            remotes.forEach { remote ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = checked.value[remote] == true,
                                        onCheckedChange = { isChecked ->
                                            checked.value = checked.value + (remote to isChecked)
                                        }
                                    )
                                    Text(
                                        text = remote,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row {
                            TextButton(onClick = {
                                close()
                                fetch(remotes)
                            }) {
                                Text(stringResource(R.string.dialog_fetch_all_button))
                            }
                            TextButton(onClick = {
                                close()
                                fetch(remotes.filter { checked.value[it] == true }.toTypedArray())
                            }) {
                                Text(stringResource(R.string.dialog_fetch_positive_button))
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { close() }) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}
