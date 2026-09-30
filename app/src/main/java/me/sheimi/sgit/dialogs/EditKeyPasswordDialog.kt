package me.sheimi.sgit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.manichord.mgit.MainActivity
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.views.SheimiDialogFragment
import me.sheimi.sgit.MGitApplication
import me.sheimi.sgit.R
import timber.log.Timber
import java.io.File

/** Allows editing the stored password for a private key. */
class EditKeyPasswordDialog : SheimiDialogFragment() {

    companion object {
        const val KEY_FILE_EXTRA = "extra_key_file"
    }

    private lateinit var keyFile: File

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        keyFile = File(arguments?.getString(KEY_FILE_EXTRA) ?: "")
        val activity = (requireActivity() as MainActivity).currentPrivateKeyManageHost!!

        return ComposeView(requireContext()).apply {
            setContent {
                val password = rememberTextFieldState()

                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.dialog_edit_key_password_title)) },
                        text = {
                            OutlinedSecureTextField(
                                state = password,
                                label = { Text(stringResource(R.string.label_password)) }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                try {
                                    (activity.applicationContext as MGitApplication)
                                        .securePrefsHelper?.set(keyFile.name, password.text.toString().trim())
                                } catch (e: Exception) {
                                    Timber.e(e)
                                }
                                activity.refreshList()
                                dismiss()
                            }) {
                                Text(stringResource(R.string.label_save))
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
        outState.putString(KEY_FILE_EXTRA, keyFile.absolutePath)
    }
}
