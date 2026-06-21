package me.sheimi.sgit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.views.SheimiDialogFragment
import me.sheimi.sgit.R
import com.manichord.mgit.MainActivity
import me.sheimi.sgit.activities.delegate.actions.PushAction
import me.sheimi.sgit.database.models.Repo

class PushDialog : SheimiDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val repo = arguments?.getSerializable(Repo.TAG) as Repo
        val activity = (requireActivity() as MainActivity).currentRepoDetailHost!!
        val remotes = repo.remotes.toList()

        return ComposeView(requireContext()).apply {
            setContent {
                var pushAll by remember { mutableStateOf(false) }
                var forcePush by remember { mutableStateOf(false) }

                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.dialog_push_repo_title)) },
                        text = {
                            val transparentColors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = pushAll, onCheckedChange = { pushAll = it })
                                    Text(stringResource(R.string.dialog_push_is_push_all))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = forcePush, onCheckedChange = { forcePush = it })
                                    Text(stringResource(R.string.dialog_push_is_force_push))
                                }
                                remotes.forEach { remote ->
                                    ListItem(
                                        headlineContent = { Text(remote) },
                                        colors = transparentColors,
                                        modifier = Modifier.clickable {
                                            PushAction.push(repo, activity, remote, pushAll, forcePush)
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
