package com.manichord.mgit.dialogs

import android.view.ViewGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.activities.SheimiFragmentActivity.OnEditTextDialogClicked

/**
 * Compose replacement for [me.sheimi.android.activities.SheimiFragmentActivity.showEditTextDialog].
 * The original validated non-empty text via a Toast on confirm; this shows the same validation
 * inline as the text field's error state instead, which doesn't require a separate Activity/
 * Context reference for the Toast.
 */
object EditTextDialog {
    @JvmStatic
    fun show(
        container: ViewGroup,
        title: String,
        hint: String,
        positiveButtonText: String,
        cancelText: String,
        positiveListener: OnEditTextDialogClicked,
        helperText: String? = null
    ) {
        val composeView = ComposeView(container.context)
        container.addView(composeView)
        fun close() = container.removeView(composeView)

        composeView.setContent {
            var text by remember { mutableStateOf("") }
            var showError by remember { mutableStateOf(false) }

            AppTheme {
                AlertDialog(
                    onDismissRequest = { close() },
                    title = { Text(title) },
                    text = {
                        OutlinedTextField(
                            value = text,
                            onValueChange = {
                                text = it
                                showError = false
                            },
                            label = { Text(hint) },
                            singleLine = true,
                            isError = showError,
                            // Long guidance text (e.g. cherry-pick's "copy from commit list")
                            // belongs here, not in the label -- Material3's floating label is
                            // designed to be a single short line when the field is focused and
                            // doesn't wrap, so anything more than a couple words there visibly
                            // overflows past the field's border. supportingText below the field
                            // wraps normally.
                            supportingText = if (showError) {
                                { Text("Required") }
                            } else if (helperText != null) {
                                { Text(helperText) }
                            } else null
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (text.isBlank()) {
                                showError = true
                            } else {
                                close()
                                positiveListener.onClicked(text)
                            }
                        }) {
                            Text(positiveButtonText)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { close() }) { Text(cancelText) }
                    }
                )
            }
        }
    }
}
