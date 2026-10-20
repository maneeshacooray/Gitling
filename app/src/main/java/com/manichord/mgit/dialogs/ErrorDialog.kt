@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.manichord.mgit.dialogs

import androidx.compose.material3.ButtonDefaults
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.views.SheimiDialogFragment
import me.sheimi.sgit.BuildConfig
import me.sheimi.sgit.R
import timber.log.Timber

/**
 * Error dialog shown by BasicFunctions.showError/showException.
 *
 * Its title, message and error details are kept in the fragment arguments, not plain fields: the
 * FragmentManager recreates a showing dialog with its arguments whenever the activity is
 * recreated (dark mode or language switching, the app being restored after Android killed it),
 * and plain fields came back as 0, so getString(0) crashed the app with
 * "Resources$NotFoundException: String resource ID #0x0".
 */
class ErrorDialog : SheimiDialogFragment() {

    companion object {
        private const val ARG_ERROR_RES = "error_res"
        private const val ARG_ERROR_TITLE_RES = "error_title_res"
        private const val ARG_DETAILS = "details"
    }

    /** Only used for debug logging; not restored when the dialog is recreated. */
    private var mThrowable: Throwable? = null

    @StringRes
    var errorTitleRes: Int = 0
        get() = args().getInt(ARG_ERROR_TITLE_RES).takeIf { it != 0 } ?: R.string.dialog_error_title
        set(value) {
            field = value
            args().putInt(ARG_ERROR_TITLE_RES, value)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val errorRes = args().getInt(ARG_ERROR_RES)
        val details = args().getString(ARG_DETAILS).orEmpty()
        val title = getString(errorTitleRes)
        val message = listOfNotNull(
            errorRes.takeIf { it != 0 }?.let { getString(it) },
            details.takeIf { it.isNotEmpty() }
        ).joinToString("\n")

        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(title) },
                        text = { Text(message) },
                        confirmButton = {
                            TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                                if (BuildConfig.DEBUG) {
                                    if (mThrowable != null) {
                                        Timber.e(mThrowable)
                                    } else if (errorRes != 0) {
                                        Timber.e(getString(errorRes))
                                    }
                                }
                                dismiss()
                            }) {
                                Text(stringResource(R.string.label_ok))
                            }
                        }
                    )
                }
            }
        }
    }

    fun setThrowable(throwable: Throwable?) {
        mThrowable = throwable
        args().putString(ARG_DETAILS, (throwable as? Exception)?.message)
    }

    fun setErrorRes(@StringRes errorRes: Int) {
        args().putInt(ARG_ERROR_RES, errorRes)
    }

    private fun args(): Bundle = arguments ?: Bundle().also { arguments = it }
}
