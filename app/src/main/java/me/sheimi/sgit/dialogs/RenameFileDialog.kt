package me.sheimi.sgit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import com.manichord.mgit.ui.components.onUserTextChange
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.views.SheimiDialogFragment
import me.sheimi.sgit.R
import java.io.File

/** Renames a file or directory within a repo's working tree -- plain filesystem rename, not a
 * git operation, so (like NewFileAction/NewDirAction) it acts directly on FilesFragment rather
 * than going through RepoOperationDelegate/a RepoOpTask. Modeled on RenameKeyDialog. */
class RenameFileDialog : SheimiDialogFragment() {

    companion object {
        const val FILE_PATH = "file path"
    }

    private lateinit var fromFile: File

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fromPath = arguments?.getString(FILE_PATH) ?: ""
        fromFile = File(fromPath)
        val activity = (requireActivity() as MainActivity).currentRepoDetailHost
        if (activity == null) {
            // Same race as RepoFileOperationDialog: state restoration can recreate this dialog
            // before currentRepoDetailHost is set again.
            dismiss()
            return ComposeView(requireContext())
        }

        return ComposeView(requireContext()).apply {
            setContent {
                val filename = rememberTextFieldState(fromFile.name)
                var errorRes by remember { mutableStateOf<Int?>(null) }

                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.dialog_rename_file_title)) },
                        text = {
                            OutlinedTextField(
                                state = filename,
                                inputTransformation = onUserTextChange { errorRes = null },
                                label = { Text(stringResource(R.string.label_new_file_name)) },
                                lineLimits = TextFieldLineLimits.SingleLine,
                                isError = errorRes != null,
                                supportingText = errorRes?.let { res -> { Text(stringResource(res)) } }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val newFilename = filename.text.toString().trim()
                                val parent = fromFile.parentFile
                                when {
                                    newFilename.isEmpty() -> {
                                        errorRes = R.string.alert_new_filename_required
                                    }
                                    newFilename.contains("/") -> {
                                        errorRes = R.string.alert_filename_format
                                    }
                                    parent == null -> {
                                        dismiss()
                                    }
                                    newFilename != fromFile.name && File(parent, newFilename).exists() -> {
                                        errorRes = R.string.alert_file_exists
                                    }
                                    else -> {
                                        val newFile = File(parent, newFilename)
                                        fromFile.renameTo(newFile)
                                        // Refresh the listing in place -- FilesFragment.reset()
                                        // navigates back to the repo root rather than staying in
                                        // the current directory, which would be surprising here.
                                        activity.getFilesFragment()?.setCurrentDir(parent)
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
        outState.putString(FILE_PATH, fromFile.absolutePath)
    }
}
