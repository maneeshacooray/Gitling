@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.manichord.mgit.repodetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.sheimi.sgit.R

/** What the toolbar asks the repo screen to do. */
class RepoToolbarActions(
    val pull: (force: Boolean) -> Unit,
    val push: (pushAll: Boolean, force: Boolean) -> Unit,
    val choosePullRemote: () -> Unit,
    val choosePushRemote: () -> Unit,
    val commit: () -> Unit,
    val more: () -> Unit
)

private enum class ForceConfirm { PULL, PUSH }

/**
 * The everyday git actions one tap away instead of in the operations drawer: Pull and Push as split
 * buttons (one tap pulls from or pushes to origin; the arrow opens the other options), Commit, and
 * More for everything else. Force pull and force push ask first, since they can overwrite work.
 */
@Composable
fun RepoToolbar(actions: RepoToolbarActions, modifier: Modifier = Modifier) {
    var confirm by remember { mutableStateOf<ForceConfirm?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            content = {
                SplitAction(
                    icon = Icons.Default.CloudDownload,
                    label = stringResource(R.string.label_pull),
                    optionsLabel = stringResource(R.string.label_pull_options),
                    onClick = { actions.pull(false) },
                    options = listOf(
                        stringResource(R.string.menu_force_pull) to { confirm = ForceConfirm.PULL },
                        stringResource(R.string.menu_choose_remote) to actions.choosePullRemote
                    )
                )
                FilledIconButton(onClick = actions.commit) {
                    Icon(Icons.Default.Save, contentDescription = stringResource(R.string.label_commit))
                }
                SplitAction(
                    icon = Icons.Default.CloudUpload,
                    label = stringResource(R.string.label_push),
                    optionsLabel = stringResource(R.string.label_push_options),
                    onClick = { actions.push(false, false) },
                    options = listOf(
                        stringResource(R.string.menu_push_all) to { actions.push(true, false) },
                        stringResource(R.string.menu_force_push) to { confirm = ForceConfirm.PUSH },
                        stringResource(R.string.menu_choose_remote) to actions.choosePushRemote
                    )
                )
                IconButton(onClick = actions.more) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = stringResource(R.string.label_more_operations))
                }
            }
        )
    }

    confirm?.let { which ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = {
                Text(stringResource(if (which == ForceConfirm.PULL) R.string.menu_force_pull else R.string.menu_force_push))
            },
            text = {
                Text(stringResource(if (which == ForceConfirm.PULL) R.string.dialog_force_pull_msg else R.string.dialog_force_push_msg))
            },
            confirmButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                    confirm = null
                    if (which == ForceConfirm.PULL) actions.pull(true) else actions.push(false, true)
                }) {
                    Text(stringResource(if (which == ForceConfirm.PULL) R.string.menu_force_pull else R.string.menu_force_push))
                }
            },
            dismissButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { confirm = null }) {
                    Text(stringResource(R.string.label_cancel))
                }
            }
        )
    }
}

/** A split button: the main action on the left, a menu of options behind the arrow on the right. */
@Composable
private fun SplitAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    optionsLabel: String,
    onClick: () -> Unit,
    options: List<Pair<String, () -> Unit>>
) {
    var expanded by remember { mutableStateOf(false) }
    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.TonalLeadingButton(onClick = onClick) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize))
            }
        },
        trailingButton = {
            Box {
                SplitButtonDefaults.TonalTrailingButton(
                    checked = expanded,
                    onCheckedChange = { expanded = it },
                    modifier = Modifier.semantics { contentDescription = optionsLabel }
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(SplitButtonDefaults.TrailingIconSize)
                            .rotate(if (expanded) 180f else 0f)
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (text, action) ->
                        DropdownMenuItem(
                            text = { Text(text) },
                            onClick = {
                                expanded = false
                                action()
                            }
                        )
                    }
                }
            }
        }
    )
}
