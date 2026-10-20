@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.manichord.mgit.dialogs

import androidx.compose.material3.ButtonDefaults
import android.content.DialogInterface
import android.view.ViewGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.ComposeView
import com.manichord.mgit.ui.theme.AppTheme

/**
 * Compose replacement for [me.sheimi.android.activities.SheimiFragmentActivity]'s
 * showMessageDialog helpers (previously MaterialAlertDialogBuilder with a forced generic
 * Material3 dialog theme, not the app's actual brand colors). Takes the existing
 * DialogInterface.OnClickListener Java interface directly -- a Java lambda calling a void
 * method can't satisfy a Kotlin () -> Unit SAM. Callers never reference the DialogInterface
 * argument (confirmed across call sites), but a no-op stub is passed rather than null to stay
 * safe regardless.
 */
object MessageDialog {
    private val noOpDialogInterface = object : DialogInterface {
        override fun cancel() {}
        override fun dismiss() {}
    }

    @JvmStatic
    fun show(
        container: ViewGroup,
        title: String,
        message: String,
        positiveButtonText: String,
        negativeButtonText: String,
        positiveListener: DialogInterface.OnClickListener,
        negativeListener: DialogInterface.OnClickListener
    ) {
        val composeView = ComposeView(container.context)
        container.addView(composeView)
        fun close() = container.removeView(composeView)

        composeView.setContent {
            AppTheme {
                AlertDialog(
                    onDismissRequest = {
                        close()
                        negativeListener.onClick(noOpDialogInterface, DialogInterface.BUTTON_NEGATIVE)
                    },
                    title = { Text(title) },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                            close()
                            positiveListener.onClick(noOpDialogInterface, DialogInterface.BUTTON_POSITIVE)
                        }) {
                            Text(positiveButtonText)
                        }
                    },
                    dismissButton = {
                        TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                            close()
                            negativeListener.onClick(noOpDialogInterface, DialogInterface.BUTTON_NEGATIVE)
                        }) {
                            Text(negativeButtonText)
                        }
                    }
                )
            }
        }
    }

    @JvmStatic
    fun showSingleButton(
        container: ViewGroup,
        title: String,
        message: String,
        positiveButtonText: String,
        positiveListener: DialogInterface.OnClickListener
    ) {
        val composeView = ComposeView(container.context)
        container.addView(composeView)
        fun close() = container.removeView(composeView)

        composeView.setContent {
            AppTheme {
                AlertDialog(
                    onDismissRequest = { close() },
                    title = { Text(title) },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                            close()
                            positiveListener.onClick(noOpDialogInterface, DialogInterface.BUTTON_POSITIVE)
                        }) {
                            Text(positiveButtonText)
                        }
                    }
                )
            }
        }
    }
}
