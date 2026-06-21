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
import androidx.compose.ui.res.stringResource
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.views.SheimiDialogFragment
import me.sheimi.sgit.R
import com.manichord.mgit.MainActivity
import me.sheimi.sgit.activities.delegate.actions.RemoveRemoteAction
import me.sheimi.sgit.database.models.Repo
import timber.log.Timber
import java.io.IOException

class RemoveRemoteDialog : SheimiDialogFragment() {

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
                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.dialog_remove_remote_title)) },
                        text = {
                            val transparentColors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            Column {
                                remotes.forEach { remote ->
                                    ListItem(
                                        headlineContent = { Text(remote) },
                                        colors = transparentColors,
                                        modifier = Modifier.clickable {
                                            try {
                                                RemoveRemoteAction.removeRemote(repo, activity, remote)
                                            } catch (e: IOException) {
                                                Timber.e(e)
                                                activity.showMessageDialog(
                                                    R.string.dialog_error_title,
                                                    activity.getString(R.string.error_something_wrong)
                                                )
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
