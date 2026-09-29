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
import com.manichord.mgit.ui.components.onUserTextChange
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.GitConfig

/**
 * Repo-local git user.name/user.email config. The original used two-way data binding that
 * wrote through to disk on every keystroke (GitConfig's setters call StoredConfig.save()
 * synchronously) -- same behavior here, just via onValueChange instead of @={...} binding.
 */
object RepoConfigDialog {
    @JvmStatic
    fun show(container: ViewGroup, gitConfig: GitConfig) {
        val composeView = ComposeView(container.context)
        container.addView(composeView)
        fun close() = container.removeView(composeView)

        composeView.setContent {
            val userName = rememberTextFieldState(gitConfig.userName ?: "")
            val userEmail = rememberTextFieldState(gitConfig.userEmail ?: "")

            AppTheme {
                AlertDialog(
                    onDismissRequest = { close() },
                    text = {
                        Column {
                            OutlinedTextField(
                                state = userName,
                                inputTransformation = onUserTextChange { gitConfig.userName = it },
                                label = { Text(stringResource(R.string.label_git_name_per_repo)) },
                                lineLimits = TextFieldLineLimits.SingleLine
                            )
                            OutlinedTextField(
                                state = userEmail,
                                inputTransformation = onUserTextChange { gitConfig.userEmail = it },
                                label = { Text(stringResource(R.string.label_git_email_per_repo)) },
                                lineLimits = TextFieldLineLimits.SingleLine
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { close() }) {
                            Text(stringResource(R.string.label_done))
                        }
                    }
                )
            }
        }
    }
}
