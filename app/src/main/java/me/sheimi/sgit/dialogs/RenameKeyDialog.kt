package me.sheimi.sgit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
import androidx.compose.ui.res.stringResource
import com.manichord.mgit.MainActivity
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.views.SheimiDialogFragment
import me.sheimi.sgit.R
import me.sheimi.sgit.ssh.PrivateKeyUtils
import timber.log.Timber
import java.io.File

class RenameKeyDialog : SheimiDialogFragment() {

    companion object {
        const val FROM_PATH = "from path"
    }

    private lateinit var fromFile: File

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fromPath = arguments?.getString(FROM_PATH) ?: ""
        fromFile = File(fromPath)
        val activity = (requireActivity() as MainActivity).currentPrivateKeyManageHost!!

        return ComposeView(requireContext()).apply {
            setContent {
                var filename by remember { mutableStateOf(fromFile.name) }
                var errorRes by remember { mutableStateOf<Int?>(null) }

                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.dialog_rename_key_title)) },
                        text = {
                            OutlinedTextField(
                                value = filename,
                                onValueChange = {
                                    filename = it
                                    errorRes = null
                                },
                                label = { Text(stringResource(R.string.label_new_file_name)) },
                                singleLine = true,
                                isError = errorRes != null,
                                supportingText = errorRes?.let { res -> { Text(stringResource(res)) } }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val newFilename = filename.trim()
                                when {
                                    newFilename.isEmpty() -> {
                                        errorRes = R.string.alert_new_filename_required
                                    }
                                    newFilename.contains("/") -> {
                                        errorRes = R.string.alert_filename_format
                                    }
                                    File(fromFile.parentFile, newFilename).exists() -> {
                                        errorRes = R.string.alert_file_exists
                                    }
                                    else -> {
                                        val file = File(fromFile.parentFile, newFilename)
                                        fromFile.renameTo(file)
                                        try {
                                            PrivateKeyUtils.getPublicKey(fromFile).renameTo(PrivateKeyUtils.getPublicKey(file))
                                        } catch (e: Exception) {
                                            Timber.e(e)
                                        }
                                        activity.refreshList()
                                        dismiss()
                                    }
                                }
                            }) {
                                Text(stringResource(R.string.label_rename))
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
        outState.putString(FROM_PATH, fromFile.absolutePath)
    }
}
