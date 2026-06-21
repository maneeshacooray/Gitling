package me.sheimi.sgit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.manichord.mgit.tasks.repo.UpdateIndexTask
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.views.SheimiDialogFragment
import me.sheimi.sgit.R
import com.manichord.mgit.MainActivity
import me.sheimi.sgit.repo.tasks.repo.DeleteFileFromRepoTask.DeleteOperationType

class RepoFileOperationDialog : SheimiDialogFragment() {

    companion object {
        const val FILE_PATH = "file path"
        private const val ADD_TO_STAGE = 0
        private const val CHECKOUT_FILE = 1
        private const val DELETE = 2
        private const val REMOVE_CACHED = 3
        private const val REMOVE_FORCE = 4
        private const val MAKE_EXECUTABLE = 5
        private const val MAKE_NOT_EXECUTABLE = 6
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val filePath = arguments?.getString(FILE_PATH) ?: ""
        val activity = (requireActivity() as MainActivity).currentRepoDetailHost!!

        fun showRemoveFileMessageDialog(
            dialogTitle: Int,
            dialogMsg: Int,
            dialogPositiveButton: Int,
            deleteOperationType: DeleteOperationType
        ) {
            showMessageDialog(dialogTitle, dialogMsg, dialogPositiveButton) { _, _ ->
                activity.getRepoDelegate().deleteFileFromRepo(filePath, deleteOperationType)
            }
        }

        return ComposeView(requireContext()).apply {
            setContent {
                val options = stringArrayResource(R.array.repo_file_operations)

                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.dialog_title_you_want_to)) },
                        text = {
                            val transparentColors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            Column {
                                options.forEachIndexed { index, option ->
                                    ListItem(
                                        headlineContent = { Text(option) },
                                        colors = transparentColors,
                                        modifier = Modifier.clickable {
                                            when (index) {
                                                ADD_TO_STAGE -> activity.getRepoDelegate().addToStage(filePath)
                                                CHECKOUT_FILE -> activity.getRepoDelegate().checkoutFile(filePath)
                                                DELETE -> showRemoveFileMessageDialog(
                                                    R.string.dialog_file_delete,
                                                    R.string.dialog_file_delete_msg,
                                                    R.string.label_delete,
                                                    DeleteOperationType.DELETE
                                                )
                                                REMOVE_CACHED -> showRemoveFileMessageDialog(
                                                    R.string.dialog_file_remove_cached,
                                                    R.string.dialog_file_remove_cached_msg,
                                                    R.string.label_delete,
                                                    DeleteOperationType.REMOVE_CACHED
                                                )
                                                REMOVE_FORCE -> showRemoveFileMessageDialog(
                                                    R.string.dialog_file_remove_force,
                                                    R.string.dialog_file_remove_force_msg,
                                                    R.string.label_delete,
                                                    DeleteOperationType.REMOVE_FORCE
                                                )
                                                MAKE_EXECUTABLE, MAKE_NOT_EXECUTABLE -> {
                                                    val newExecutableState = index == MAKE_EXECUTABLE
                                                    activity.getRepoDelegate().updateIndex(
                                                        filePath,
                                                        UpdateIndexTask.calculateNewMode(newExecutableState)
                                                    )
                                                }
                                            }
                                            dismiss()
                                        }
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { dismiss() }) {
                                Text(stringResource(R.string.label_cancel))
                            }
                        }
                    )
                }
            }
        }
    }
}
