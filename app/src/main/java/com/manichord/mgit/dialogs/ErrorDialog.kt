package com.manichord.mgit.dialogs

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

class ErrorDialog : SheimiDialogFragment() {
    private var mThrowable: Throwable? = null
    @StringRes
    private var mErrorRes: Int = 0
    @StringRes
    var errorTitleRes: Int = 0
        get() = if (field != 0) field else R.string.dialog_error_title

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val details = (mThrowable as? Exception)?.message ?: ""
        val title = getString(errorTitleRes)
        val message = getString(mErrorRes) + "\n" + details

        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(title) },
                        text = { Text(message) },
                        confirmButton = {
                            TextButton(onClick = {
                                if (BuildConfig.DEBUG) {
                                    if (mThrowable != null) {
                                        Timber.e(mThrowable)
                                    } else if (mErrorRes != 0) {
                                        Timber.e(getString(mErrorRes))
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
    }

    fun setErrorRes(@StringRes errorRes: Int) {
        mErrorRes = errorRes
    }
}
