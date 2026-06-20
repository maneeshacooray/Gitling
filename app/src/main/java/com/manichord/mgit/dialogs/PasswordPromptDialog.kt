package com.manichord.mgit.dialogs

import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.activities.SheimiFragmentActivity.OnPasswordEntered
import me.sheimi.sgit.R

/**
 * Credentials prompt shown by [me.sheimi.android.activities.SheimiFragmentActivity.promptForPassword].
 * That class is Java and can't author Composables directly, so this is invoked imperatively: a
 * ComposeView is added to the activity's content container and removed again once the dialog
 * resolves, the same lifecycle a legacy AlertDialog would have had. Takes the existing
 * [OnPasswordEntered] Java interface directly (rather than Kotlin function types) since a
 * Java lambda calling a void method can't satisfy a Kotlin `() -> Unit` SAM.
 */
object PasswordPromptDialog {
    @JvmStatic
    fun show(
        container: ViewGroup,
        title: String,
        callback: OnPasswordEntered
    ) {
        val composeView = ComposeView(container.context)
        container.addView(composeView)

        fun close() {
            container.removeView(composeView)
        }

        composeView.setContent {
            var username by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var savePassword by remember { mutableStateOf(false) }

            AppTheme {
                AlertDialog(
                    onDismissRequest = {
                        close()
                        callback.onCanceled()
                    },
                    title = { Text(title) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text(stringResource(R.string.label_username)) },
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text(stringResource(R.string.label_password)) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = savePassword, onCheckedChange = { savePassword = it })
                                Text(stringResource(R.string.dialog_prompt_for_password_checkbox))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            close()
                            callback.onClicked(username, password, savePassword)
                        }) {
                            Text(stringResource(R.string.label_done))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            close()
                            callback.onCanceled()
                        }) {
                            Text(stringResource(R.string.label_cancel))
                        }
                    }
                )
            }
        }
    }
}
