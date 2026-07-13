package me.sheimi.sgit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.views.SheimiDialogFragment
import me.sheimi.sgit.R
import com.manichord.mgit.MainActivity
import me.sheimi.sgit.database.models.Repo

class MergeDialog : SheimiDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val repo = arguments?.getSerializable(Repo.TAG) as? Repo
        val activity = (requireActivity() as MainActivity).currentRepoDetailHost
        if (repo == null || activity == null) {
            // A DialogFragment can be recreated by the FragmentManager's own state
            // restoration (e.g. after process death) before Compose has recomposed
            // "repoDetail" and re-set currentRepoDetailHost -- bail out rather than crash.
            dismiss()
            return ComposeView(requireContext())
        }
        val currentBranchDisplayName = repo.currentDisplayName
        val branches = repo.localBranches.filter {
            Repo.getCommitDisplayName(it.name) != currentBranchDisplayName
        }

        return ComposeView(requireContext()).apply {
            setContent {
                val ffOptions = stringArrayResource(R.array.merge_ff_type)
                var selectedFf by remember { mutableStateOf(ffOptions[0]) }
                var ffMenuExpanded by remember { mutableStateOf(false) }
                var autoCommit by remember { mutableStateOf(false) }

                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.dialog_merge_title)) },
                        text = {
                            val transparentColors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            Column {
                                OutlinedTextField(
                                    value = selectedFf,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { ffMenuExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.clickable { ffMenuExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = ffMenuExpanded,
                                    onDismissRequest = { ffMenuExpanded = false }
                                ) {
                                    ffOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                selectedFf = option
                                                ffMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = autoCommit, onCheckedChange = { autoCommit = it })
                                    Text(stringResource(R.string.dialog_merge_checkbox))
                                }
                                branches.forEach { branch ->
                                    val icon = if (Repo.getCommitType(branch.name) == Repo.COMMIT_TYPE_TAG) {
                                        Icons.Default.Tag
                                    } else {
                                        Icons.Default.AccountTree
                                    }
                                    ListItem(
                                        headlineContent = { Text(Repo.getCommitDisplayName(branch.name)) },
                                        leadingContent = {
                                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        colors = transparentColors,
                                        modifier = Modifier.clickable {
                                            activity.getRepoDelegate().mergeBranch(branch, selectedFf, autoCommit)
                                            dismiss()
                                        }
                                    )
                                }
                            }
                        },
                        confirmButton = {}
                    )
                }
            }
        }
    }
}
