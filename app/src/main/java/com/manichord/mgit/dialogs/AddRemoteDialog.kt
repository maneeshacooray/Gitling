package com.manichord.mgit.dialogs

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
import me.sheimi.sgit.R
import me.sheimi.sgit.activities.RepoDetailActivity
import me.sheimi.sgit.database.models.Repo
import timber.log.Timber
import java.io.IOException

object AddRemoteDialog {
    @JvmStatic
    fun show(container: ViewGroup, activity: RepoDetailActivity, repo: Repo) {
        val composeView = ComposeView(container.context)
        container.addView(composeView)
        fun close() = container.removeView(composeView)

        composeView.setContent {
            val name = rememberTextFieldState()
            val url = rememberTextFieldState()

            AppTheme {
                AlertDialog(
                    onDismissRequest = { close() },
                    title = { Text(stringResource(R.string.dialog_add_remote_title)) },
                    text = {
                        Column {
                            OutlinedTextField(
                                state = name,
                                label = { Text(stringResource(R.string.dialog_add_remote_hint_name)) },
                                lineLimits = TextFieldLineLimits.SingleLine
                            )
                            OutlinedTextField(
                                state = url,
                                label = { Text(stringResource(R.string.dialog_add_remote_hint_url)) },
                                lineLimits = TextFieldLineLimits.SingleLine
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            try {
                                repo.setRemote(name.text.toString(), url.text.toString())
                                repo.updateRemote()
                                activity.showToastMessage(R.string.success_remote_added)
                            } catch (e: IOException) {
                                Timber.e(e)
                                activity.showMessageDialog(
                                    R.string.dialog_error_title,
                                    activity.getString(R.string.error_something_wrong)
                                )
                            }
                            close()
                        }) {
                            Text(stringResource(R.string.dialog_add_remote_positive_label))
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
